/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "AudioMixer"
//#define LOG_NDEBUG 0

#include "Configuration.h"
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Log.h>

#include <cutils/bitops.h>
#include <cutils/compiler.h>
#include <utils/Debug.h>

#include <system/audio.h>

#include <audio_utils/primitives.h>
#include <common_time/local_clock.h>
#include <common_time/cc_helper.h>

#include <media/EffectsFactoryApi.h>

#include "AudioMixer.h"

namespace android {

// ----------------------------------------------------------------------------
AudioMixer::DownmixerBufferProvider::DownmixerBufferProvider() : AudioBufferProvider(),
        mTrackBufferProvider(NULL), mDownmixHandle(NULL)
{
}

AudioMixer::DownmixerBufferProvider::~DownmixerBufferProvider()
{
    ALOGV("AudioMixer deleting DownmixerBufferProvider (%p)", this);
    EffectRelease(mDownmixHandle);
}

status_t AudioMixer::DownmixerBufferProvider::getNextBuffer(AudioBufferProvider::Buffer *pBuffer,
        int64_t pts) {
    //ALOGV("DownmixerBufferProvider::getNextBuffer()");
    if (this->mTrackBufferProvider != NULL) {
        status_t res = mTrackBufferProvider->getNextBuffer(pBuffer, pts);
        if (res == OK) {
            mDownmixConfig.inputCfg.buffer.frameCount = pBuffer->frameCount;
            mDownmixConfig.inputCfg.buffer.raw = pBuffer->raw;
            mDownmixConfig.outputCfg.buffer.frameCount = pBuffer->frameCount;
            mDownmixConfig.outputCfg.buffer.raw = mDownmixConfig.inputCfg.buffer.raw;
            // in-place so overwrite the buffer contents, has been set in prepareTrackForDownmix()
            //mDownmixConfig.outputCfg.accessMode = EFFECT_BUFFER_ACCESS_WRITE;

            res = (*mDownmixHandle)->process(mDownmixHandle,
                    &mDownmixConfig.inputCfg.buffer, &mDownmixConfig.outputCfg.buffer);
            //ALOGV("getNextBuffer is downmixing");
        }
        return res;
    } else {
        ALOGE("DownmixerBufferProvider::getNextBuffer() error: NULL track buffer provider");
        return NO_INIT;
    }
}

void AudioMixer::DownmixerBufferProvider::releaseBuffer(AudioBufferProvider::Buffer *pBuffer) {
    //ALOGV("DownmixerBufferProvider::releaseBuffer()");
    if (this->mTrackBufferProvider != NULL) {
        mTrackBufferProvider->releaseBuffer(pBuffer);
    } else {
        ALOGE("DownmixerBufferProvider::releaseBuffer() error: NULL track buffer provider");
    }
}


// ----------------------------------------------------------------------------
bool AudioMixer::isMultichannelCapable = false;

effect_descriptor_t AudioMixer::dwnmFxDesc;

// Ensure mConfiguredNames bitmask is initialized properly on all architectures.
// The value of 1 << x is undefined in C when x >= 32.

AudioMixer::AudioMixer(size_t frameCount, uint32_t sampleRate, uint32_t maxNumTracks)
    :   mTrackNames(0), mConfiguredNames((maxNumTracks >= 32 ? 0 : 1 << maxNumTracks) - 1),
        mSampleRate(sampleRate)
{
    // AudioMixer is not yet capable of multi-channel beyond stereo
    COMPILE_TIME_ASSERT_FUNCTION_SCOPE(2 == MAX_NUM_CHANNELS);

    ALOG_ASSERT(maxNumTracks <= MAX_NUM_TRACKS, "maxNumTracks %u > MAX_NUM_TRACKS %u",
            maxNumTracks, MAX_NUM_TRACKS);

    // AudioMixer is not yet capable of more than 32 active track inputs
    ALOG_ASSERT(32 >= MAX_NUM_TRACKS, "bad MAX_NUM_TRACKS %d", MAX_NUM_TRACKS);

    // AudioMixer is not yet capable of multi-channel output beyond stereo
    ALOG_ASSERT(2 == MAX_NUM_CHANNELS, "bad MAX_NUM_CHANNELS %d", MAX_NUM_CHANNELS);

    LocalClock lc;

    pthread_once(&sOnceControl, &sInitRoutine);

    mState.enabledTracks= 0;
    mState.needsChanged = 0;
    mState.frameCount   = frameCount;
    mState.hook         = process__nop;
    mState.outputTemp   = NULL;
    mState.resampleTemp = NULL;
    mState.mLog         = &mDummyLog;
    // mState.reserved

    // FIXME Most of the following initialization is probably redundant since
    // tracks[i] should only be referenced if (mTrackNames & (1 << i)) != 0
    // and mTrackNames is initially 0.  However, leave it here until that's verified.
    track_t* t = mState.tracks;
    for (unsigned i=0 ; i < MAX_NUM_TRACKS ; i++) {
        t->resampler = NULL;
        t->downmixerBufferProvider = NULL;
        t++;
    }

    // find multichannel downmix effect if we have to play multichannel content
    uint32_t numEffects = 0;
    int ret = EffectQueryNumberEffects(&numEffects);
    if (ret != 0) {
        ALOGE("AudioMixer() error %d querying number of effects", ret);
        return;
    }
    ALOGV("EffectQueryNumberEffects() numEffects=%d", numEffects);

    for (uint32_t i = 0 ; i < numEffects ; i++) {
        if (EffectQueryEffect(i, &dwnmFxDesc) == 0) {
            ALOGV("effect %d is called %s", i, dwnmFxDesc.name);
            if (memcmp(&dwnmFxDesc.type, EFFECT_UIID_DOWNMIX, sizeof(effect_uuid_t)) == 0) {
                ALOGI("found effect \"%s\" from %s",
                        dwnmFxDesc.name, dwnmFxDesc.implementor);
                isMultichannelCapable = true;
                break;
            }
        }
    }
    ALOGE_IF(!isMultichannelCapable, "unable to find downmix effect");
}

AudioMixer::~AudioMixer()
{
    track_t* t = mState.tracks;
    for (unsigned i=0 ; i < MAX_NUM_TRACKS ; i++) {
        delete t->resampler;
        delete t->downmixerBufferProvider;
        t++;
    }
    delete [] mState.outputTemp;
    delete [] mState.resampleTemp;
}

void AudioMixer::setLog(NBLog::Writer *log)
{
    mState.mLog = log;
}

int AudioMixer::getTrackName(audio_channel_mask_t channelMask, int sessionId)
{
    uint32_t names = (~mTrackNames) & mConfiguredNames;
    if (names != 0) {
        int n = __builtin_ctz(names);
        ALOGV("add track (%d)", n);
        mTrackNames |= 1 << n;
        // assume default parameters for the track, except where noted below
        track_t* t = &mState.tracks[n];
        t->needs = 0;
        t->volume[0] = UNITY_GAIN;
        t->volume[1] = UNITY_GAIN;
        // no initialization needed
        // t->prevVolume[0]
        // t->prevVolume[1]
        t->volumeInc[0] = 0;
        t->volumeInc[1] = 0;
        t->auxLevel = 0;
        t->auxInc = 0;
        // no initialization needed
        // t->prevAuxLevel
        // t->frameCount
        t->channelCount = 2;
        t->enabled = false;
        t->format = 16;
        t->channelMask = AUDIO_CHANNEL_OUT_STEREO;
        t->sessionId = sessionId;
        // setBufferProvider(name, AudioBufferProvider *) is required before enable(name)
        t->bufferProvider = NULL;
        t->buffer.raw = NULL;
        // no initialization needed
        // t->buffer.frameCount
        t->hook = NULL;
        t->in = NULL;
        t->resampler = NULL;
        t->sampleRate = mSampleRate;
        // setParameter(name, TRACK, MAIN_BUFFER, mixBuffer) is required before enable(name)
        t->mainBuffer = NULL;
        t->auxBuffer = NULL;
        t->downmixerBufferProvider = NULL;

        status_t status = initTrackDownmix(&mState.tracks[n], n, channelMask);
        if (status == OK) {
            return TRACK0 + n;
        }
        ALOGE("AudioMixer::getTrackName(0x%x) failed, error preparing track for downmix",
                channelMask);
    }
    return -1;
}

void AudioMixer::invalidateState(uint32_t mask)
{
    if (mask) {
        mState.needsChanged |= mask;
        mState.hook = process__validate;
    }
 }

status_t AudioMixer::initTrackDownmix(track_t* pTrack, int trackNum, audio_channel_mask_t mask)
{
    uint32_t channelCount = popcount(mask);
    ALOG_ASSERT((channelCount <= MAX_NUM_CHANNELS_TO_DOWNMIX) && channelCount);
    status_t status = OK;
    if (channelCount > MAX_NUM_CHANNELS) {
        pTrack->channelMask = mask;
        pTrack->channelCount = channelCount;
        ALOGV("initTrackDownmix(track=%d, mask=0x%x) calls prepareTrackForDownmix()",
                trackNum, mask);
        status = prepareTrackForDownmix(pTrack, trackNum);
    } else {
        unprepareTrackForDownmix(pTrack, trackNum);
    }
    return status;
}

void AudioMixer::unprepareTrackForDownmix(track_t* pTrack, int trackName) {
    ALOGV("AudioMixer::unprepareTrackForDownmix(%d)", trackName);

    if (pTrack->downmixerBufferProvider != NULL) {
        // this track had previously been configured with a downmixer, delete it
        ALOGV(" deleting old downmixer");
        pTrack->bufferProvider = pTrack->downmixerBufferProvider->mTrackBufferProvider;
        delete pTrack->downmixerBufferProvider;
        pTrack->downmixerBufferProvider = NULL;
    } else {
        ALOGV(" nothing to do, no downmixer to delete");
    }
}

status_t AudioMixer::prepareTrackForDownmix(track_t* pTrack, int trackName)
{
    ALOGV("AudioMixer::prepareTrackForDownmix(%d) with mask 0x%x", trackName, pTrack->channelMask);

    // discard the previous downmixer if there was one
    unprepareTrackForDownmix(pTrack, trackName);

    DownmixerBufferProvider* pDbp = new DownmixerBufferProvider();
    int32_t status;

    if (!isMultichannelCapable) {
        ALOGE("prepareTrackForDownmix(%d) fails: mixer doesn't support multichannel content",
                trackName);
        goto noDownmixForActiveTrack;
    }

    if (EffectCreate(&dwnmFxDesc.uuid,
            pTrack->sessionId /*sessionId*/, -2 /*ioId not relevant here, using random value*/,
            &pDbp->mDownmixHandle/*pHandle*/) != 0) {
        ALOGE("prepareTrackForDownmix(%d) fails: error creating downmixer effect", trackName);
        goto noDownmixForActiveTrack;
    }

    // channel input configuration will be overridden per-track
    pDbp->mDownmixConfig.inputCfg.channels = pTrack->channelMask;
    pDbp->mDownmixConfig.outputCfg.channels = AUDIO_CHANNEL_OUT_STEREO;
    pDbp->mDownmixConfig.inputCfg.format = AUDIO_FORMAT_PCM_16_BIT;
    pDbp->mDownmixConfig.outputCfg.format = AUDIO_FORMAT_PCM_16_BIT;
    pDbp->mDownmixConfig.inputCfg.samplingRate = pTrack->sampleRate;
    pDbp->mDownmixConfig.outputCfg.samplingRate = pTrack->sampleRate;
    pDbp->mDownmixConfig.inputCfg.accessMode = EFFECT_BUFFER_ACCESS_READ;
    pDbp->mDownmixConfig.outputCfg.accessMode = EFFECT_BUFFER_ACCESS_WRITE;
    // input and output buffer provider, and frame count will not be used as the downmix effect
    // process() function is called directly (see DownmixerBufferProvider::getNextBuffer())
    pDbp->mDownmixConfig.inputCfg.mask = EFFECT_CONFIG_SMP_RATE | EFFECT_CONFIG_CHANNELS |
            EFFECT_CONFIG_FORMAT | EFFECT_CONFIG_ACC_MODE;
    pDbp->mDownmixConfig.outputCfg.mask = pDbp->mDownmixConfig.inputCfg.mask;

    {// scope for local variables that are not used in goto label "noDownmixForActiveTrack"
        int cmdStatus;
        uint32_t replySize = sizeof(int);

        // Configure and enable downmixer
        status = (*pDbp->mDownmixHandle)->command(pDbp->mDownmixHandle,
                EFFECT_CMD_SET_CONFIG /*cmdCode*/, sizeof(effect_config_t) /*cmdSize*/,
                &pDbp->mDownmixConfig /*pCmdData*/,
                &replySize /*replySize*/, &cmdStatus /*pReplyData*/);
        if ((status != 0) || (cmdStatus != 0)) {
            ALOGE("error %d while configuring downmixer for track %d", status, trackName);
            goto noDownmixForActiveTrack;
        }
        replySize = sizeof(int);
        status = (*pDbp->mDownmixHandle)->command(pDbp->mDownmixHandle,
                EFFECT_CMD_ENABLE /*cmdCode*/, 0 /*cmdSize*/, NULL /*pCmdData*/,
                &replySize /*replySize*/, &cmdStatus /*pReplyData*/);
        if ((status != 0) || (cmdStatus != 0)) {
            ALOGE("error %d while enabling downmixer for track %d", status, trackName);
            goto noDownmixForActiveTrack;
        }

        // Set downmix type
        // parameter size rounded for padding on 32bit boundary
        const int psizePadded = ((sizeof(downmix_params_t) - 1)/sizeof(int) + 1) * sizeof(int);
        const int downmixParamSize =
                sizeof(effect_param_t) + psizePadded + sizeof(downmix_type_t);
        effect_param_t * const param = (effect_param_t *) malloc(downmixParamSize);
        param->psize = sizeof(downmix_params_t);
        const downmix_params_t downmixParam = DOWNMIX_PARAM_TYPE;
        memcpy(param->data, &downmixParam, param->psize);
        const downmix_type_t downmixType = DOWNMIX_TYPE_FOLD;
        param->vsize = sizeof(downmix_type_t);
        memcpy(param->data + psizePadded, &downmixType, param->vsize);

        status = (*pDbp->mDownmixHandle)->command(pDbp->mDownmixHandle,
                EFFECT_CMD_SET_PARAM /* cmdCode */, downmixParamSize/* cmdSize */,
                param /*pCmndData*/, &replySize /*replySize*/, &cmdStatus /*pReplyData*/);

        free(param);

        if ((status != 0) || (cmdStatus != 0)) {
            ALOGE("error %d while setting downmix type for track %d", status, trackName);
            goto noDownmixForActiveTrack;
        } else {
            ALOGV("downmix type set to %d for track %d", (int) downmixType, trackName);
        }
    }// end of scope for local variables that are not used in goto label "noDownmixForActiveTrack"

    // initialization successful:
    // - keep track of the real buffer provider in case it was set before
    pDbp->mTrackBufferProvider = pTrack->bufferProvider;
    // - we'll use the downmix effect integrated inside this
    //    track's buffer provider, and we'll use it as the track's buffer provider
    pTrack->downmixerBufferProvider = pDbp;
    pTrack->bufferProvider = pDbp;

    return NO_ERROR;

noDownmixForActiveTrack:
    delete pDbp;
    pTrack->downmixerBufferProvider = NULL;
    return NO_INIT;
}

void AudioMixer::deleteTrackName(int name)
{
    ALOGV("AudioMixer::deleteTrackName(%d)", name);
    name -= TRACK0;
    ALOG_ASSERT(uint32_t(name) < MAX_NUM_TRACKS, "bad track name %d", name);
    ALOGV("deleteTrackName(%d)", name);
    track_t& track(mState.tracks[ name ]);
    if (track.enabled) {
        track.enabled = false;
        invalidateState(1<<name);
    }
    // delete the resampler
    delete track.resampler;
    track.resampler = NULL;
    // delete the downmixer
    unprepareTrackForDownmix(&mState.tracks[name], name);

    mTrackNames &= ~(1<<name);
}

void AudioMixer::enable(int name)
{
    name -= TRACK0;
    ALOG_ASSERT(uint32_t(name) < MAX_NUM_TRACKS, "bad track name %d", name);
    track_t& track = mState.tracks[name];

    if (!track.enabled) {
        track.enabled = true;
        ALOGV("enable(%d)", name);
        invalidateState(1 << name);
    }
}

void AudioMixer::disable(int name)
{
    name -= TRACK0;
    ALOG_ASSERT(uint32_t(name) < MAX_NUM_TRACKS, "bad track name %d", name);
    track_t& track = mState.tracks[name];

    if (track.enabled) {
        track.enabled = false;
        ALOGV("disable(%d)", name);
        invalidateState(1 << name);
    }
}

void AudioMixer::setParameter(int name, int target, int param, void *value)
{
    name -= TRACK0;
    ALOG_ASSERT(uint32_t(name) < MAX_NUM_TRACKS, "bad track name %d", name);
    track_t& track = mState.tracks[name];

    int valueInt = (int)value;
    int32_t *valueBuf = (int32_t *)value;

    switch (target) {

    case TRACK:
        switch (param) {
        case CHANNEL_MASK: {
            audio_channel_mask_t mask = (audio_channel_mask_t) value;
            if (track.channelMask != mask) {
                uint32_t channelCount = popcount(mask);
                ALOG_ASSERT((channelCount <= MAX_NUM_CHANNELS_TO_DOWNMIX) && channelCount);
                track.channelMask = mask;
                track.channelCount = channelCount;
                // the mask has changed, does this track need a downmixer?
                initTrackDownmix(&mState.tracks[name], name, mask);
                ALOGV("setParameter(TRACK, CHANNEL_MASK, %x)", mask);
                invalidateState(1 << name);
            }
            } break;
        case MAIN_BUFFER:
            if (track.mainBuffer != valueBuf) {
                track.mainBuffer = valueBuf;
                ALOGV("setParameter(TRACK, MAIN_BUFFER, %p)", valueBuf);
                invalidateState(1 << name);
            }
            break;
        case AUX_BUFFER:
            if (track.auxBuffer != valueBuf) {
                track.auxBuffer = valueBuf;
                ALOGV("setParameter(TRACK, AUX_BUFFER, %p)", valueBuf);
                invalidateState(1 << name);
            }
            break;
        case FORMAT:
            ALOG_ASSERT(valueInt == AUDIO_FORMAT_PCM_16_BIT);
            break;
        // FIXME do we want to support setting the downmix type from AudioFlinger?
        //         for a specific track? or per mixer?
        /* case DOWNMIX_TYPE:
            break          */
        default:
            LOG_FATAL("bad param");
        }
        break;

    case RESAMPLE:
        switch (param) {
        case SAMPLE_RATE:
            ALOG_ASSERT(valueInt > 0, "bad sample rate %d", valueInt);
            if (track.setResampler(uint32_t(valueInt), mSampleRate)) {
                ALOGV("setParameter(RESAMPLE, SAMPLE_RATE, %u)",
                        uint32_t(valueInt));
                invalidateState(1 << name);
            }
            break;
        case RESET:
            track.resetResampler();
            invalidateState(1 << name);
            break;
        case REMOVE:
            delete track.resampler;
            track.resampler = NULL;
            track.sampleRate = mSampleRate;
            invalidateState(1 << name);
            break;
        default:
            LOG_FATAL("bad param");
        }
        break;

    case RAMP_VOLUME:
    case VOLUME:
        switch (param) {
        case VOLUME0:
        case VOLUME1:
            if (track.volume[param-VOLUME0] != valueInt) {
                ALOGV("setParameter(VOLUME, VOLUME0/1: %04x)", valueInt);
                track.prevVolume[param-VOLUME0] = track.volume[param-VOLUME0] << 16;
                track.volume[param-VOLUME0] = valueInt;
                if (target == VOLUME) {
                    track.prevVolume[param-VOLUME0] = valueInt << 16;
                    track.volumeInc[param-VOLUME0] = 0;
                } else {
                    int32_t d = (valueInt<<16) - track.prevVolume[param-VOLUME0];
                    int32_t volInc = d / int32_t(mState.frameCount);
                    track.volumeInc[param-VOLUME0] = volInc;
                    if (volInc == 0) {
                        track.prevVolume[param-VOLUME0] = valueInt << 16;
                    }
                }
                invalidateState(1 << name);
            }
            break;
        case AUXLEVEL:
            //ALOG_ASSERT(0 <= valueInt && valueInt <= MAX_GAIN_INT, "bad aux level %d", valueInt);
            if (track.auxLevel != valueInt) {
                ALOGV("setParameter(VOLUME, AUXLEVEL: %04x)", valueInt);
                track.prevAuxLevel = track.auxLevel << 16;
                track.auxLevel = valueInt;
                if (target == VOLUME) {
                    track.prevAuxLevel = valueInt << 16;
                    track.auxInc = 0;
                } else {
                    int32_t d = (valueInt<<16) - track.prevAuxLevel;
                    int32_t volInc = d / int32_t(mState.frameCount);
                    track.auxInc = volInc;
                    if (volInc == 0) {
                        track.prevAuxLevel = valueInt << 16;
                    }
                }
                invalidateState(1 << name);
            }
            break;
        default:
            LOG_FATAL("bad param");
        }
        break;

    default:
        LOG_FATAL("bad target");
    }
}

bool AudioMixer::track_t::setResampler(uint32_t value, uint32_t devSampleRate)
{
    if (value != devSampleRate || resampler != NULL) {
        if (sampleRate != value) {
            sampleRate = value;
            if (resampler == NULL) {
                ALOGV("creating resampler from track %d Hz to device %d Hz", value, devSampleRate);
                AudioResampler::src_quality quality;
                // force lowest quality level resampler if use case isn't music or video
                // FIXME this is flawed for dynamic sample rates, as we choose the resampler
                // quality level based on the initial ratio, but that could change later.
                // Should have a way to distinguish tracks with static ratios vs. dynamic ratios.
                if (!((value == 44100 && devSampleRate == 48000) ||
                      (value == 48000 && devSampleRate == 44100))) {
                    quality = AudioResampler::LOW_QUALITY;
                } else {
                    quality = AudioResampler::DEFAULT_QUALITY;
                }
                resampler = AudioResampler::create(
                        format,
                        // the resampler sees the number of channels after the downmixer, if any
                        downmixerBufferProvider != NULL ? MAX_NUM_CHANNELS : channelCount,
                        devSampleRate, quality);
                resampler->setLocalTimeFreq(sLocalTimeFreq);
            }
            return true;
        }
    }
    return false;
}

inline
void AudioMixer::track_t::adjustVolumeRamp(bool aux)
{
    for (uint32_t i=0 ; i<MAX_NUM_CHANNELS ; i++) {
        if (((volumeInc[i]>0) && (((prevVolume[i]+volumeInc[i])>>16) >= volume[i])) ||
            ((volumeInc[i]<0) && (((prevVolume[i]+volumeInc[i])>>16) <= volume[i]))) {
            volumeInc[i] = 0;
            prevVolume[i] = volume[i]<<16;
        }
    }
    if (aux) {
        if (((auxInc>0) && (((prevAuxLevel+auxInc)>>16) >= auxLevel)) ||
            ((auxInc<0) && (((prevAuxLevel+auxInc)>>16) <= auxLevel))) {
            auxInc = 0;
            prevAuxLevel = auxLevel<<16;
        }
    }
}

size_t AudioMixer::getUnreleasedFrames(int name) const
{
    name -= TRACK0;
    if (uint32_t(name) < MAX_NUM_TRACKS) {
        return mState.tracks[name].getUnreleasedFrames();
    }
    return 0;
}

void AudioMixer::setBufferProvider(int name, AudioBufferProvider* bufferProvider)
{
    name -= TRACK0;
    ALOG_ASSERT(uint32_t(name) < MAX_NUM_TRACKS, "bad track name %d", name);

    if (mState.tracks[name].downmixerBufferProvider != NULL) {
        // update required?
        if (mState.tracks[name].downmixerBufferProvider->mTrackBufferProvider != bufferProvider) {
            ALOGV("AudioMixer::setBufferProvider(%p) for downmix", bufferProvider);
            // setting the buffer provider for a track that gets downmixed consists in:
            //  1/ setting the buffer provider to the "downmix / buffer provider" wrapper
            //     so it's the one that gets called when the buffer provider is needed,
            mState.tracks[name].bufferProvider = mState.tracks[name].downmixerBufferProvider;
            //  2/ saving the buffer provider for the track so the wrapper can use it
            //     when it downmixes.
            mState.tracks[name].downmixerBufferProvider->mTrackBufferProvider = bufferProvider;
        }
    } else {
        mState.tracks[name].bufferProvider = bufferProvider;
    }
}


void AudioMixer::process(int64_t pts)
{
    mState.hook(&mState, pts);
}


void AudioMixer::process__validate(state_t* state, int64_t pts)
{
    ALOGW_IF(!state->needsChanged,
        "in process__validate() but nothing's invalid");

    uint32_t changed = state->needsChanged;
    state->needsChanged = 0; // clear the validation flag

    // recompute which tracks are enabled / disabled
    uint32_t enabled = 0;
    uint32_t disabled = 0;
    while (changed) {
        const int i = 31 - __builtin_clz(changed);
        const uint32_t mask = 1<<i;
        changed &= ~mask;
        track_t& t = state->tracks[i];
        (t.enabled ? enabled : disabled) |= mask;
    }
    state->enabledTracks &= ~disabled;
    state->enabledTracks |=  enabled;

    // compute everything we need...
    int countActiveTracks = 0;
    bool all16BitsStereoNoResample = true;
    bool resampling = false;
    bool volumeRamp = false;
    uint32_t en = state->enabledTracks;
    while (en) {
        const int i = 31 - __builtin_clz(en);
        en &= ~(1<<i);

        countActiveTracks++;
        track_t& t = state->tracks[i];
        uint32_t n = 0;
        n |= NEEDS_CHANNEL_1 + t.channelCount - 1;
        n |= NEEDS_FORMAT_16;
        n |= t.doesResample() ? NEEDS_RESAMPLE_ENABLED : NEEDS_RESAMPLE_DISABLED;
        if (t.auxLevel != 0 && t.auxBuffer != NULL) {
            n |= NEEDS_AUX_ENABLED;
        }

        if (t.volumeInc[0]|t.volumeInc[1]) {
            volumeRamp = true;
        } else if (!t.doesResample() && t.volumeRL == 0) {
            n |= NEEDS_MUTE_ENABLED;
        }
        t.needs = n;

        if ((n & NEEDS_MUTE__MASK) == NEEDS_MUTE_ENABLED) {
            t.hook = track__nop;
        } else {
            if ((n & NEEDS_AUX__MASK) == NEEDS_AUX_ENABLED) {
                all16BitsStereoNoResample = false;
            }
            if ((n & NEEDS_RESAMPLE__MASK) == NEEDS_RESAMPLE_ENABLED) {
                all16BitsStereoNoResample = false;
                resampling = true;
                t.hook = track__genericResample;
                ALOGV_IF((n & NEEDS_CHANNEL_COUNT__MASK) > NEEDS_CHANNEL_2,
                        "Track %d needs downmix + resample", i);
            } else {
                if ((n & NEEDS_CHANNEL_COUNT__MASK) == NEEDS_CHANNEL_1){
                    t.hook = track__16BitsMono;
                    all16BitsStereoNoResample = false;
                }
                if ((n & NEEDS_CHANNEL_COUNT__MASK) >= NEEDS_CHANNEL_2){
                    t.hook = track__16BitsStereo;
                    ALOGV_IF((n & NEEDS_CHANNEL_COUNT__MASK) > NEEDS_CHANNEL_2,
                            "Track %d needs downmix", i);
                }
            }
        }
    }

    // select the processing hooks
    state->hook = process__nop;
    if (countActiveTracks) {
        if (resampling) {
            if (!state->outputTemp) {
                state->outputTemp = new int32_t[MAX_NUM_CHANNELS * state->frameCount];
            }
            if (!state->resampleTemp) {
                state->resampleTemp = new int32_t[MAX_NUM_CHANNELS * state->frameCount];
            }
            state->hook = process__genericResampling;
        } else {
            if (state->outputTemp) {
                delete [] state->outputTemp;
                state->outputTemp = NULL;
            }
            if (state->resampleTemp) {
                delete [] state->resampleTemp;
                state->resampleTemp = NULL;
            }
            state->hook = process__genericNoResampling;
            if (all16BitsStereoNoResample && !volumeRamp) {
                if (countActiveTracks == 1) {
                    state->hook = process__OneTrack16BitsStereoNoResampling;
                }
            }
        }
    }

    ALOGV("mixer configuration change: %d activeTracks (%08x) "
        "all16BitsStereoNoResample=%d, resampling=%d, volumeRamp=%d",
        countActiveTracks, state->enabledTracks,
        all16BitsStereoNoResample, resampling, volumeRamp);

   state->hook(state, pts);

    // Now that the volume ramp has been done, set optimal state and
    // track hooks for subsequent mixer process
    if (countActiveTracks) {
        bool allMuted = true;
        uint32_t en = state->enabledTracks;
        while (en) {
            const int i = 31 - __builtin_clz(en);
            en &= ~(1<<i);
            track_t& t = state->tracks[i];
            if (!t.doesResample() && t.volumeRL == 0)
            {
                t.needs |= NEEDS_MUTE_ENABLED;
                t.hook = track__nop;
            } else {
                allMuted = false;
            }
        }
        if (allMuted) {
            state->hook = process__nop;
        } else if (all16BitsStereoNoResample) {
            if (countActiveTracks == 1) {
                state->hook = process__OneTrack16BitsStereoNoResampling;
            }
        }
    }
}


void AudioMixer::track__genericResample(track_t* t, int32_t* out, size_t outFrameCount,
        int32_t* temp, int32_t* aux)
{
    t->resampler->setSampleRate(t->sampleRate);

    // ramp gain - resample to temp buffer and scale/mix in 2nd step
    if (aux != NULL) {
        // always resample with unity gain when sending to auxiliary buffer to be able
        // to apply send level after resampling
        // TODO: modify each resampler to support aux channel?
        t->resampler->setVolume(UNITY_GAIN, UNITY_GAIN);
        memset(temp, 0, outFrameCount * MAX_NUM_CHANNELS * sizeof(int32_t));
        t->resampler->resample(temp, outFrameCount, t->bufferProvider);
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1]|t->auxInc)) {
            volumeRampStereo(t, out, outFrameCount, temp, aux);
        } else {
            volumeStereo(t, out, outFrameCount, temp, aux);
        }
    } else {
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1])) {
            t->resampler->setVolume(UNITY_GAIN, UNITY_GAIN);
            memset(temp, 0, outFrameCount * MAX_NUM_CHANNELS * sizeof(int32_t));
            t->resampler->resample(temp, outFrameCount, t->bufferProvider);
            volumeRampStereo(t, out, outFrameCount, temp, aux);
        }

        // constant gain
        else {
            t->resampler->setVolume(t->volume[0], t->volume[1]);
            t->resampler->resample(out, outFrameCount, t->bufferProvider);
        }
    }
}

void AudioMixer::track__nop(track_t* t, int32_t* out, size_t outFrameCount, int32_t* temp,
        int32_t* aux)
{
}

void AudioMixer::volumeRampStereo(track_t* t, int32_t* out, size_t frameCount, int32_t* temp,
        int32_t* aux)
{
    int32_t vl = t->prevVolume[0];
    int32_t vr = t->prevVolume[1];
    const int32_t vlInc = t->volumeInc[0];
    const int32_t vrInc = t->volumeInc[1];

    //ALOGD("[0] %p: inc=%f, v0=%f, v1=%d, final=%f, count=%d",
    //        t, vlInc/65536.0f, vl/65536.0f, t->volume[0],
    //       (vl + vlInc*frameCount)/65536.0f, frameCount);

    // ramp volume
    if (CC_UNLIKELY(aux != NULL)) {
        int32_t va = t->prevAuxLevel;
        const int32_t vaInc = t->auxInc;
        int32_t l;
        int32_t r;

        do {
            l = (*temp++ >> 12);
            r = (*temp++ >> 12);
            *out++ += (vl >> 16) * l;
            *out++ += (vr >> 16) * r;
            *aux++ += (va >> 17) * (l + r);
            vl += vlInc;
            vr += vrInc;
            va += vaInc;
        } while (--frameCount);
        t->prevAuxLevel = va;
    } else {
        do {
            *out++ += (vl >> 16) * (*temp++ >> 12);
            *out++ += (vr >> 16) * (*temp++ >> 12);
            vl += vlInc;
            vr += vrInc;
        } while (--frameCount);
    }
    t->prevVolume[0] = vl;
    t->prevVolume[1] = vr;
    t->adjustVolumeRamp(aux != NULL);
}

void AudioMixer::volumeStereo(track_t* t, int32_t* out, size_t frameCount, int32_t* temp,
        int32_t* aux)
{
    const int16_t vl = t->volume[0];
    const int16_t vr = t->volume[1];

    if (CC_UNLIKELY(aux != NULL)) {
        const int16_t va = t->auxLevel;
        do {
            int16_t l = (int16_t)(*temp++ >> 12);
            int16_t r = (int16_t)(*temp++ >> 12);
            out[0] = mulAdd(l, vl, out[0]);
            int16_t a = (int16_t)(((int32_t)l + r) >> 1);
            out[1] = mulAdd(r, vr, out[1]);
            out += 2;
            aux[0] = mulAdd(a, va, aux[0]);
            aux++;
        } while (--frameCount);
    } else {
        do {
            int16_t l = (int16_t)(*temp++ >> 12);
            int16_t r = (int16_t)(*temp++ >> 12);
            out[0] = mulAdd(l, vl, out[0]);
            out[1] = mulAdd(r, vr, out[1]);
            out += 2;
        } while (--frameCount);
    }
}

void AudioMixer::track__16BitsStereo(track_t* t, int32_t* out, size_t frameCount, int32_t* temp,
        int32_t* aux)
{
    const int16_t *in = static_cast<const int16_t *>(t->in);

    if (CC_UNLIKELY(aux != NULL)) {
        int32_t l;
        int32_t r;
        // ramp gain
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1]|t->auxInc)) {
            int32_t vl = t->prevVolume[0];
            int32_t vr = t->prevVolume[1];
            int32_t va = t->prevAuxLevel;
            const int32_t vlInc = t->volumeInc[0];
            const int32_t vrInc = t->volumeInc[1];
            const int32_t vaInc = t->auxInc;
            // ALOGD("[1] %p: inc=%f, v0=%f, v1=%d, final=%f, count=%d",
            //        t, vlInc/65536.0f, vl/65536.0f, t->volume[0],
            //        (vl + vlInc*frameCount)/65536.0f, frameCount);

            do {
                l = (int32_t)*in++;
                r = (int32_t)*in++;
                *out++ += (vl >> 16) * l;
                *out++ += (vr >> 16) * r;
                *aux++ += (va >> 17) * (l + r);
                vl += vlInc;
                vr += vrInc;
                va += vaInc;
            } while (--frameCount);

            t->prevVolume[0] = vl;
            t->prevVolume[1] = vr;
            t->prevAuxLevel = va;
            t->adjustVolumeRamp(true);
        }

        // constant gain
        else {
            const uint32_t vrl = t->volumeRL;
            const int16_t va = (int16_t)t->auxLevel;
            do {
                uint32_t rl = *reinterpret_cast<const uint32_t *>(in);
                int16_t a = (int16_t)(((int32_t)in[0] + in[1]) >> 1);
                in += 2;
                out[0] = mulAddRL(1, rl, vrl, out[0]);
                out[1] = mulAddRL(0, rl, vrl, out[1]);
                out += 2;
                aux[0] = mulAdd(a, va, aux[0]);
                aux++;
            } while (--frameCount);
        }
    } else {
        // ramp gain
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1])) {
            int32_t vl = t->prevVolume[0];
            int32_t vr = t->prevVolume[1];
            const int32_t vlInc = t->volumeInc[0];
            const int32_t vrInc = t->volumeInc[1];

            // ALOGD("[1] %p: inc=%f, v0=%f, v1=%d, final=%f, count=%d",
            //        t, vlInc/65536.0f, vl/65536.0f, t->volume[0],
            //        (vl + vlInc*frameCount)/65536.0f, frameCount);

            do {
                *out++ += (vl >> 16) * (int32_t) *in++;
                *out++ += (vr >> 16) * (int32_t) *in++;
                vl += vlInc;
                vr += vrInc;
            } while (--frameCount);

            t->prevVolume[0] = vl;
            t->prevVolume[1] = vr;
            t->adjustVolumeRamp(false);
        }

        // constant gain
        else {
            const uint32_t vrl = t->volumeRL;
            do {
                uint32_t rl = *reinterpret_cast<const uint32_t *>(in);
                in += 2;
                out[0] = mulAddRL(1, rl, vrl, out[0]);
                out[1] = mulAddRL(0, rl, vrl, out[1]);
                out += 2;
            } while (--frameCount);
        }
    }
    t->in = in;
}

void AudioMixer::track__16BitsMono(track_t* t, int32_t* out, size_t frameCount, int32_t* temp,
        int32_t* aux)
{
    const int16_t *in = static_cast<int16_t const *>(t->in);

    if (CC_UNLIKELY(aux != NULL)) {
        // ramp gain
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1]|t->auxInc)) {
            int32_t vl = t->prevVolume[0];
            int32_t vr = t->prevVolume[1];
            int32_t va = t->prevAuxLevel;
            const int32_t vlInc = t->volumeInc[0];
            const int32_t vrInc = t->volumeInc[1];
            const int32_t vaInc = t->auxInc;

            // ALOGD("[2] %p: inc=%f, v0=%f, v1=%d, final=%f, count=%d",
            //         t, vlInc/65536.0f, vl/65536.0f, t->volume[0],
            //         (vl + vlInc*frameCount)/65536.0f, frameCount);

            do {
                int32_t l = *in++;
                *out++ += (vl >> 16) * l;
                *out++ += (vr >> 16) * l;
                *aux++ += (va >> 16) * l;
                vl += vlInc;
                vr += vrInc;
                va += vaInc;
            } while (--frameCount);

            t->prevVolume[0] = vl;
            t->prevVolume[1] = vr;
            t->prevAuxLevel = va;
            t->adjustVolumeRamp(true);
        }
        // constant gain
        else {
            const int16_t vl = t->volume[0];
            const int16_t vr = t->volume[1];
            const int16_t va = (int16_t)t->auxLevel;
            do {
                int16_t l = *in++;
                out[0] = mulAdd(l, vl, out[0]);
                out[1] = mulAdd(l, vr, out[1]);
                out += 2;
                aux[0] = mulAdd(l, va, aux[0]);
                aux++;
            } while (--frameCount);
        }
    } else {
        // ramp gain
        if (CC_UNLIKELY(t->volumeInc[0]|t->volumeInc[1])) {
            int32_t vl = t->prevVolume[0];
            int32_t vr = t->prevVolume[1];
            const int32_t vlInc = t->volumeInc[0];
            const int32_t vrInc = t->volumeInc[1];

            // ALOGD("[2] %p: inc=%f, v0=%f, v1=%d, final=%f, count=%d",
            //         t, vlInc/65536.0f, vl/65536.0f, t->volume[0],
            //         (vl + vlInc*frameCount)/65536.0f, frameCount);

            do {
                int32_t l = *in++;
                *out++ += (vl >> 16) * l;
                *out++ += (vr >> 16) * l;
                vl += vlInc;
                vr += vrInc;
            } while (--frameCount);

            t->prevVolume[0] = vl;
            t->prevVolume[1] = vr;
            t->adjustVolumeRamp(false);
        }
        // constant gain
        else {
            const int16_t vl = t->volume[0];
            const int16_t vr = t->volume[1];
            do {
                int16_t l = *in++;
                out[0] = mulAdd(l, vl, out[0]);
                out[1] = mulAdd(l, vr, out[1]);
                out += 2;
            } while (--frameCount);
        }
    }
    t->in = in;
}

// no-op case
void AudioMixer::process__nop(state_t* state, int64_t pts)
{
    uint32_t e0 = state->enabledTracks;
    size_t bufSize = state->frameCount * sizeof(int16_t) * MAX_NUM_CHANNELS;
    while (e0) {
        // process by group of tracks with same output buffer to
        // avoid multiple memset() on same buffer
        uint32_t e1 = e0, e2 = e0;
        int i = 31 - __builtin_clz(e1);
        {
            track_t& t1 = state->tracks[i];
            e2 &= ~(1<<i);
            while (e2) {
                i = 31 - __builtin_clz(e2);
                e2 &= ~(1<<i);
                track_t& t2 = state->tracks[i];
                if (CC_UNLIKELY(t2.mainBuffer != t1.mainBuffer)) {
                    e1 &= ~(1<<i);
                }
            }
            e0 &= ~(e1);

            memset(t1.mainBuffer, 0, bufSize);
        }

        while (e1) {
            i = 31 - __builtin_clz(e1);
            e1 &= ~(1<<i);
            {
                track_t& t3 = state->tracks[i];
                size_t outFrames = state->frameCount;
                while (outFrames) {
                    t3.buffer.frameCount = outFrames;
                    int64_t outputPTS = calculateOutputPTS(
                        t3, pts, state->frameCount - outFrames);
                    t3.bufferProvider->getNextBuffer(&t3.buffer, outputPTS);
                    if (t3.buffer.raw == NULL) break;
                    outFrames -= t3.buffer.frameCount;
                    t3.bufferProvider->releaseBuffer(&t3.buffer);
                }
            }
        }
    }
}

// generic code without resampling
void AudioMixer::process__genericNoResampling(state_t* state, int64_t pts)
{
    int32_t outTemp[BLOCKSIZE * MAX_NUM_CHANNELS] __attribute__((aligned(32)));

    // acquire each track's buffer
    uint32_t enabledTracks = state->enabledTracks;
    uint32_t e0 = enabledTracks;
    while (e0) {
        const int i = 31 - __builtin_clz(e0);
        e0 &= ~(1<<i);
        track_t& t = state->tracks[i];
        t.buffer.frameCount = state->frameCount;
        t.bufferProvider->getNextBuffer(&t.buffer, pts);
        t.frameCount = t.buffer.frameCount;
        t.in = t.buffer.raw;
        // t.in == NULL can happen if the track was flushed just after having
        // been enabled for mixing.
        if (t.in == NULL)
            enabledTracks &= ~(1<<i);
    }

    e0 = enabledTracks;
    while (e0) {
        // process by group of tracks with same output buffer to
        // optimize cache use
        uint32_t e1 = e0, e2 = e0;
        int j = 31 - __builtin_clz(e1);
        track_t& t1 = state->tracks[j];
        e2 &= ~(1<<j);
        while (e2) {
            j = 31 - __builtin_clz(e2);
            e2 &= ~(1<<j);
            track_t& t2 = state->tracks[j];
            if (CC_UNLIKELY(t2.mainBuffer != t1.mainBuffer)) {
                e1 &= ~(1<<j);
            }
        }
        e0 &= ~(e1);
        // this assumes output 16 bits stereo, no resampling
        int32_t *out = t1.mainBuffer;
        size_t numFrames = 0;
        do {
            memset(outTemp, 0, sizeof(outTemp));
            e2 = e1;
            while (e2) {
                const int i = 31 - __builtin_clz(e2);
                e2 &= ~(1<<i);
                track_t& t = state->tracks[i];
                size_t outFrames = BLOCKSIZE;
                int32_t *aux = NULL;
                if (CC_UNLIKELY((t.needs & NEEDS_AUX__MASK) == NEEDS_AUX_ENABLED)) {
                    aux = t.auxBuffer + numFrames;
                }
                while (outFrames) {
                    size_t inFrames = (t.frameCount > outFrames)?outFrames:t.frameCount;
                    if (inFrames) {
                        t.hook(&t, outTemp + (BLOCKSIZE-outFrames)*MAX_NUM_CHANNELS, inFrames,
                                state->resampleTemp, aux);
                        t.frameCount -= inFrames;
                        outFrames -= inFrames;
                        if (CC_UNLIKELY(aux != NULL)) {
                            aux += inFrames;
                        }
                    }
                    if (t.frameCount == 0 && outFrames) {
                        t.bufferProvider->releaseBuffer(&t.buffer);
                        t.buffer.frameCount = (state->frameCount - numFrames) -
                                (BLOCKSIZE - outFrames);
                        int64_t outputPTS = calculateOutputPTS(
                            t, pts, numFrames + (BLOCKSIZE - outFrames));
                        t.bufferProvider->getNextBuffer(&t.buffer, outputPTS);
                        t.in = t.buffer.raw;
                        if (t.in == NULL) {
                            enabledTracks &= ~(1<<i);
                            e1 &= ~(1<<i);
                            break;
                        }
                        t.frameCount = t.buffer.frameCount;
                    }
                }
            }
            ditherAndClamp(out, outTemp, BLOCKSIZE);
            out += BLOCKSIZE;
            numFrames += BLOCKSIZE;
        } while (numFrames < state->frameCount);
    }

    // release each track's buffer
    e0 = enabledTracks;
    while (e0) {
        const int i = 31 - __builtin_clz(e0);
        e0 &= ~(1<<i);
        track_t& t = state->tracks[i];
        t.bufferProvider->releaseBuffer(&t.buffer);
    }
}


// generic code with resampling
void AudioMixer::process__genericResampling(state_t* state, int64_t pts)
{
    // this const just means that local variable outTemp doesn't change
    int32_t* const outTemp = state->outputTemp;
    const size_t size = sizeof(int32_t) * MAX_NUM_CHANNELS * state->frameCount;

    size_t numFrames = state->frameCount;

    uint32_t e0 = state->enabledTracks;
    while (e0) {
        // process by group of tracks with same output buffer
        // to optimize cache use
        uint32_t e1 = e0, e2 = e0;
        int j = 31 - __builtin_clz(e1);
        track_t& t1 = state->tracks[j];
        e2 &= ~(1<<j);
        while (e2) {
            j = 31 - __builtin_clz(e2);
            e2 &= ~(1<<j);
            track_t& t2 = state->tracks[j];
            if (CC_UNLIKELY(t2.mainBuffer != t1.mainBuffer)) {
                e1 &= ~(1<<j);
            }
        }
        e0 &= ~(e1);
        int32_t *out = t1.mainBuffer;
        memset(outTemp, 0, size);
        while (e1) {
            const int i = 31 - __builtin_clz(e1);
            e1 &= ~(1<<i);
            track_t& t = state->tracks[i];
            int32_t *aux = NULL;
            if (CC_UNLIKELY((t.needs & NEEDS_AUX__MASK) == NEEDS_AUX_ENABLED)) {
                aux = t.auxBuffer;
            }

            // this is a little goofy, on the resampling case we don't
            // acquire/release the buffers because it's done by
            // the resampler.
            if ((t.needs & NEEDS_RESAMPLE__MASK) == NEEDS_RESAMPLE_ENABLED) {
                t.resampler->setPTS(pts);
                t.hook(&t, outTemp, numFrames, state->resampleTemp, aux);
            } else {

                size_t outFrames = 0;

                while (outFrames < numFrames) {
                    t.buffer.frameCount = numFrames - outFrames;
                    int64_t outputPTS = calculateOutputPTS(t, pts, outFrames);
                    t.bufferProvider->getNextBuffer(&t.buffer, outputPTS);
                    t.in = t.buffer.raw;
                    // t.in == NULL can happen if the track was flushed just after having
                    // been enabled for mixing.
                    if (t.in == NULL) break;

                    if (CC_UNLIKELY(aux != NULL)) {
                        aux += outFrames;
                    }
                    t.hook(&t, outTemp + outFrames*MAX_NUM_CHANNELS, t.buffer.frameCount,
                            state->resampleTemp, aux);
                    outFrames += t.buffer.frameCount;
                    t.bufferProvider->releaseBuffer(&t.buffer);
                }
            }
        }
        ditherAndClamp(out, outTemp, numFrames);
    }
}

// one track, 16 bits stereo without resampling is the most common case
void AudioMixer::process__OneTrack16BitsStereoNoResampling(state_t* state,
                                                           int64_t pts)
{
    // This method is only called when state->enabledTracks has exactly
    // one bit set.  The asserts below would verify this, but are commented out
    // since the whole point of this method is to optimize performance.
    //ALOG_ASSERT(0 != state->enabledTracks, "no tracks enabled");
    const int i = 31 - __builtin_clz(state->enabledTracks);
    //ALOG_ASSERT((1 << i) == state->enabledTracks, "more than 1 track enabled");
    const track_t& t = state->tracks[i];

    AudioBufferProvider::Buffer& b(t.buffer);

    int32_t* out = t.mainBuffer;
    size_t numFrames = state->frameCount;

    const int16_t vl = t.volume[0];
    const int16_t vr = t.volume[1];
    const uint32_t vrl = t.volumeRL;
    while (numFrames) {
        b.frameCount = numFrames;
        int64_t outputPTS = calculateOutputPTS(t, pts, out - t.mainBuffer);
        t.bufferProvider->getNextBuffer(&b, outputPTS);
        const int16_t *in = b.i16;

        // in == NULL can happen if the track was flushed just after having
        // been enabled for mixing.
        if (in == NULL || ((unsigned long)in & 3)) {
            memset(out, 0, numFrames*MAX_NUM_CHANNELS*sizeof(int16_t));
            ALOGE_IF(((unsigned long)in & 3), "process stereo track: input buffer alignment pb: "
                                              "buffer %p track %d, channels %d, needs %08x",
                    in, i, t.channelCount, t.needs);
            return;
        }
        size_t outFrames = b.frameCount;

        if (CC_UNLIKELY(uint32_t(vl) > UNITY_GAIN || uint32_t(vr) > UNITY_GAIN)) {
            // volume is boosted, so we might need to clamp even though
            // we process only one track.
            do {
                uint32_t rl = *reinterpret_cast<const uint32_t *>(in);
                in += 2;
                int32_t l = mulRL(1, rl, vrl) >> 12;
                int32_t r = mulRL(0, rl, vrl) >> 12;
                // clamping...
                l = clamp16(l);
                r = clamp16(r);
                *out++ = (r<<16) | (l & 0xFFFF);
            } while (--outFrames);
        } else {
            do {
                uint32_t rl = *reinterpret_cast<const uint32_t *>(in);
                in += 2;
                int32_t l = mulRL(1, rl, vrl) >> 12;
                int32_t r = mulRL(0, rl, vrl) >> 12;
                *out++ = (r<<16) | (l & 0xFFFF);
            } while (--outFrames);
        }
        numFrames -= b.frameCount;
        t.bufferProvider->releaseBuffer(&b);
    }
}

#if 0
// 2 tracks is also a common case
// NEVER used in current implementation of process__validate()
// only use if the 2 tracks have the same output buffer
void AudioMixer::process__TwoTracks16BitsStereoNoResampling(state_t* state,
                                                            int64_t pts)
{
    int i;
    uint32_t en = state->enabledTracks;

    i = 31 - __builtin_clz(en);
    const track_t& t0 = state->tracks[i];
    AudioBufferProvider::Buffer& b0(t0.buffer);

    en &= ~(1<<i);
    i = 31 - __builtin_clz(en);
    const track_t& t1 = state->tracks[i];
    AudioBufferProvider::Buffer& b1(t1.buffer);

    const int16_t *in0;
    const int16_t vl0 = t0.volume[0];
    const int16_t vr0 = t0.volume[1];
    size_t frameCount0 = 0;

    const int16_t *in1;
    const int16_t vl1 = t1.volume[0];
    const int16_t vr1 = t1.volume[1];
    size_t frameCount1 = 0;

    //FIXME: only works if two tracks use same buffer
    int32_t* out = t0.mainBuffer;
    size_t numFrames = state->frameCount;
    const int16_t *buff = NULL;


    while (numFrames) {

        if (frameCount0 == 0) {
            b0.frameCount = numFrames;
            int64_t outputPTS = calculateOutputPTS(t0, pts,
                                                   out - t0.mainBuffer);
            t0.bufferProvider->getNextBuffer(&b0, outputPTS);
            if (b0.i16 == NULL) {
                if (buff == NULL) {
                    buff = new int16_t[MAX_NUM_CHANNELS * state->frameCount];
                }
                in0 = buff;
                b0.frameCount = numFrames;
            } else {
                in0 = b0.i16;
            }
            frameCount0 = b0.frameCount;
        }
        if (frameCount1 == 0) {
            b1.frameCount = numFrames;
            int64_t outputPTS = calculateOutputPTS(t1, pts,
                                                   out - t0.mainBuffer);
            t1.bufferProvider->getNextBuffer(&b1, outputPTS);
            if (b1.i16 == NULL) {
                if (buff == NULL) {
                    buff = new int16_t[MAX_NUM_CHANNELS * state->frameCount];
                }
                in1 = buff;
                b1.frameCount = numFrames;
            } else {
                in1 = b1.i16;
            }
            frameCount1 = b1.frameCount;
        }

        size_t outFrames = frameCount0 < frameCount1?frameCount0:frameCount1;

        numFrames -= outFrames;
        frameCount0 -= outFrames;
        frameCount1 -= outFrames;

        do {
            int32_t l0 = *in0++;
            int32_t r0 = *in0++;
            l0 = mul(l0, vl0);
            r0 = mul(r0, vr0);
            int32_t l = *in1++;
            int32_t r = *in1++;
            l = mulAdd(l, vl1, l0) >> 12;
            r = mulAdd(r, vr1, r0) >> 12;
            // clamping...
            l = clamp16(l);
            r = clamp16(r);
            *out++ = (r<<16) | (l & 0xFFFF);
        } while (--outFrames);

        if (frameCount0 == 0) {
            t0.bufferProvider->releaseBuffer(&b0);
        }
        if (frameCount1 == 0) {
            t1.bufferProvider->releaseBuffer(&b1);
        }
    }

    delete [] buff;
}
#endif

int64_t AudioMixer::calculateOutputPTS(const track_t& t, int64_t basePTS,
                                       int outputFrameIndex)
{
    if (AudioBufferProvider::kInvalidPTS == basePTS)
        return AudioBufferProvider::kInvalidPTS;

    return basePTS + ((outputFrameIndex * sLocalTimeFreq) / t.sampleRate);
}

/*static*/ uint64_t AudioMixer::sLocalTimeFreq;
/*static*/ pthread_once_t AudioMixer::sOnceControl = PTHREAD_ONCE_INIT;

/*static*/ void AudioMixer::sInitRoutine()
{
    LocalClock lc;
    sLocalTimeFreq = lc.getLocalFreq();
}

// ----------------------------------------------------------------------------
}; // namespace android
