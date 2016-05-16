/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// <IMPORTANT_WARNING>
// Design rules for threadLoop() are given in the comments at section "Fast mixer thread" of
// StateQueue.h.  In particular, avoid library and system calls except at well-known points.
// The design rules are only for threadLoop(), and don't apply to FastMixerDumpState methods.
// </IMPORTANT_WARNING>

#define LOG_TAG "FastMixer"
//#define LOG_NDEBUG 0

#define ATRACE_TAG ATRACE_TAG_AUDIO

#include "Configuration.h"
#include <sys/atomics.h>
#include <time.h>
#include <utils/Log.h>
#include <utils/Trace.h>
#include <system/audio.h>
#ifdef FAST_MIXER_STATISTICS
#include <cpustats/CentralTendencyStatistics.h>
#ifdef CPU_FREQUENCY_STATISTICS
#include <cpustats/ThreadCpuUsage.h>
#endif
#endif
#include "AudioMixer.h"
#include "FastMixer.h"

#define FAST_HOT_IDLE_NS     1000000L   // 1 ms: time to sleep while hot idling
#define FAST_DEFAULT_NS    999999999L   // ~1 sec: default time to sleep
#define MIN_WARMUP_CYCLES          2    // minimum number of loop cycles to wait for warmup
#define MAX_WARMUP_CYCLES         10    // maximum number of loop cycles to wait for warmup

#define FCC_2                       2   // fixed channel count assumption

namespace android {

// Fast mixer thread
bool FastMixer::threadLoop()
{
    static const FastMixerState initial;
    const FastMixerState *previous = &initial, *current = &initial;
    FastMixerState preIdle; // copy of state before we went into idle
    struct timespec oldTs = {0, 0};
    bool oldTsValid = false;
    long slopNs = 0;    // accumulated time we've woken up too early (> 0) or too late (< 0)
    long sleepNs = -1;  // -1: busy wait, 0: sched_yield, > 0: nanosleep
    int fastTrackNames[FastMixerState::kMaxFastTracks]; // handles used by mixer to identify tracks
    int generations[FastMixerState::kMaxFastTracks];    // last observed mFastTracks[i].mGeneration
    unsigned i;
    for (i = 0; i < FastMixerState::kMaxFastTracks; ++i) {
        fastTrackNames[i] = -1;
        generations[i] = 0;
    }
    NBAIO_Sink *outputSink = NULL;
    int outputSinkGen = 0;
    AudioMixer* mixer = NULL;
    short *mixBuffer = NULL;
    enum {UNDEFINED, MIXED, ZEROED} mixBufferState = UNDEFINED;
    NBAIO_Format format = Format_Invalid;
    unsigned sampleRate = 0;
    int fastTracksGen = 0;
    long periodNs = 0;      // expected period; the time required to render one mix buffer
    long underrunNs = 0;    // underrun likely when write cycle is greater than this value
    long overrunNs = 0;     // overrun likely when write cycle is less than this value
    long forceNs = 0;       // if overrun detected, force the write cycle to take this much time
    long warmupNs = 0;      // warmup complete when write cycle is greater than to this value
    FastMixerDumpState dummyDumpState, *dumpState = &dummyDumpState;
    bool ignoreNextOverrun = true;  // used to ignore initial overrun and first after an underrun
#ifdef FAST_MIXER_STATISTICS
    struct timespec oldLoad = {0, 0};    // previous value of clock_gettime(CLOCK_THREAD_CPUTIME_ID)
    bool oldLoadValid = false;  // whether oldLoad is valid
    uint32_t bounds = 0;
    bool full = false;      // whether we have collected at least mSamplingN samples
#ifdef CPU_FREQUENCY_STATISTICS
    ThreadCpuUsage tcu;     // for reading the current CPU clock frequency in kHz
#endif
#endif
    unsigned coldGen = 0;   // last observed mColdGen
    bool isWarm = false;    // true means ready to mix, false means wait for warmup before mixing
    struct timespec measuredWarmupTs = {0, 0};  // how long did it take for warmup to complete
    uint32_t warmupCycles = 0;  // counter of number of loop cycles required to warmup
    NBAIO_Sink* teeSink = NULL; // if non-NULL, then duplicate write() to this non-blocking sink
    NBLog::Writer dummyLogWriter, *logWriter = &dummyLogWriter;
    uint32_t totalNativeFramesWritten = 0;  // copied to dumpState->mFramesWritten

    // next 2 fields are valid only when timestampStatus == NO_ERROR
    AudioTimestamp timestamp;
    uint32_t nativeFramesWrittenButNotPresented = 0;    // the = 0 is to silence the compiler
    status_t timestampStatus = INVALID_OPERATION;

    for (;;) {

        // either nanosleep, sched_yield, or busy wait
        if (sleepNs >= 0) {
            if (sleepNs > 0) {
                ALOG_ASSERT(sleepNs < 1000000000);
                const struct timespec req = {0, sleepNs};
                nanosleep(&req, NULL);
            } else {
                sched_yield();
            }
        }
        // default to long sleep for next cycle
        sleepNs = FAST_DEFAULT_NS;

        // poll for state change
        const FastMixerState *next = mSQ.poll();
        if (next == NULL) {
            // continue to use the default initial state until a real state is available
            ALOG_ASSERT(current == &initial && previous == &initial);
            next = current;
        }

        FastMixerState::Command command = next->mCommand;
        if (next != current) {

            // As soon as possible of learning of a new dump area, start using it
            dumpState = next->mDumpState != NULL ? next->mDumpState : &dummyDumpState;
            teeSink = next->mTeeSink;
            logWriter = next->mNBLogWriter != NULL ? next->mNBLogWriter : &dummyLogWriter;
            if (mixer != NULL) {
                mixer->setLog(logWriter);
            }

            // We want to always have a valid reference to the previous (non-idle) state.
            // However, the state queue only guarantees access to current and previous states.
            // So when there is a transition from a non-idle state into an idle state, we make a
            // copy of the last known non-idle state so it is still available on return from idle.
            // The possible transitions are:
            //  non-idle -> non-idle    update previous from current in-place
            //  non-idle -> idle        update previous from copy of current
            //  idle     -> idle        don't update previous
            //  idle     -> non-idle    don't update previous
            if (!(current->mCommand & FastMixerState::IDLE)) {
                if (command & FastMixerState::IDLE) {
                    preIdle = *current;
                    current = &preIdle;
                    oldTsValid = false;
#ifdef FAST_MIXER_STATISTICS
                    oldLoadValid = false;
#endif
                    ignoreNextOverrun = true;
                }
                previous = current;
            }
            current = next;
        }
#if !LOG_NDEBUG
        next = NULL;    // not referenced again
#endif

        dumpState->mCommand = command;

        switch (command) {
        case FastMixerState::INITIAL:
        case FastMixerState::HOT_IDLE:
            sleepNs = FAST_HOT_IDLE_NS;
            continue;
        case FastMixerState::COLD_IDLE:
            // only perform a cold idle command once
            // FIXME consider checking previous state and only perform if previous != COLD_IDLE
            if (current->mColdGen != coldGen) {
                int32_t *coldFutexAddr = current->mColdFutexAddr;
                ALOG_ASSERT(coldFutexAddr != NULL);
                int32_t old = android_atomic_dec(coldFutexAddr);
                if (old <= 0) {
                    __futex_syscall4(coldFutexAddr, FUTEX_WAIT_PRIVATE, old - 1, NULL);
                }
                int policy = sched_getscheduler(0);
                if (!(policy == SCHED_FIFO || policy == SCHED_RR)) {
                    ALOGE("did not receive expected priority boost");
                }
                // This may be overly conservative; there could be times that the normal mixer
                // requests such a brief cold idle that it doesn't require resetting this flag.
                isWarm = false;
                measuredWarmupTs.tv_sec = 0;
                measuredWarmupTs.tv_nsec = 0;
                warmupCycles = 0;
                sleepNs = -1;
                coldGen = current->mColdGen;
#ifdef FAST_MIXER_STATISTICS
                bounds = 0;
                full = false;
#endif
                oldTsValid = !clock_gettime(CLOCK_MONOTONIC, &oldTs);
                timestampStatus = INVALID_OPERATION;
            } else {
                sleepNs = FAST_HOT_IDLE_NS;
            }
            continue;
        case FastMixerState::EXIT:
            delete mixer;
            delete[] mixBuffer;
            return false;
        case FastMixerState::MIX:
        case FastMixerState::WRITE:
        case FastMixerState::MIX_WRITE:
            break;
        default:
            LOG_FATAL("bad command %d", command);
        }

        // there is a non-idle state available to us; did the state change?
        size_t frameCount = current->mFrameCount;
        if (current != previous) {

            // handle state change here, but since we want to diff the state,
            // we're prepared for previous == &initial the first time through
            unsigned previousTrackMask;

            // check for change in output HAL configuration
            NBAIO_Format previousFormat = format;
            if (current->mOutputSinkGen != outputSinkGen) {
                outputSink = current->mOutputSink;
                outputSinkGen = current->mOutputSinkGen;
                if (outputSink == NULL) {
                    format = Format_Invalid;
                    sampleRate = 0;
                } else {
                    format = outputSink->format();
                    sampleRate = Format_sampleRate(format);
                    ALOG_ASSERT(Format_channelCount(format) == FCC_2);
                }
                dumpState->mSampleRate = sampleRate;
            }

            if ((format != previousFormat) || (frameCount != previous->mFrameCount)) {
                // FIXME to avoid priority inversion, don't delete here
                delete mixer;
                mixer = NULL;
                delete[] mixBuffer;
                mixBuffer = NULL;
                if (frameCount > 0 && sampleRate > 0) {
                    // FIXME new may block for unbounded time at internal mutex of the heap
                    //       implementation; it would be better to have normal mixer allocate for us
                    //       to avoid blocking here and to prevent possible priority inversion
                    mixer = new AudioMixer(frameCount, sampleRate, FastMixerState::kMaxFastTracks);
                    mixBuffer = new short[frameCount * FCC_2];
                    periodNs = (frameCount * 1000000000LL) / sampleRate;    // 1.00
                    underrunNs = (frameCount * 1750000000LL) / sampleRate;  // 1.75
                    overrunNs = (frameCount * 500000000LL) / sampleRate;    // 0.50
                    forceNs = (frameCount * 950000000LL) / sampleRate;      // 0.95
                    warmupNs = (frameCount * 500000000LL) / sampleRate;     // 0.50
                } else {
                    periodNs = 0;
                    underrunNs = 0;
                    overrunNs = 0;
                    forceNs = 0;
                    warmupNs = 0;
                }
                mixBufferState = UNDEFINED;
#if !LOG_NDEBUG
                for (i = 0; i < FastMixerState::kMaxFastTracks; ++i) {
                    fastTrackNames[i] = -1;
                }
#endif
                // we need to reconfigure all active tracks
                previousTrackMask = 0;
                fastTracksGen = current->mFastTracksGen - 1;
                dumpState->mFrameCount = frameCount;
            } else {
                previousTrackMask = previous->mTrackMask;
            }

            // check for change in active track set
            unsigned currentTrackMask = current->mTrackMask;
            dumpState->mTrackMask = currentTrackMask;
            if (current->mFastTracksGen != fastTracksGen) {
                ALOG_ASSERT(mixBuffer != NULL);
                int name;

                // process removed tracks first to avoid running out of track names
                unsigned removedTracks = previousTrackMask & ~currentTrackMask;
                while (removedTracks != 0) {
                    i = __builtin_ctz(removedTracks);
                    removedTracks &= ~(1 << i);
                    const FastTrack* fastTrack = &current->mFastTracks[i];
                    ALOG_ASSERT(fastTrack->mBufferProvider == NULL);
                    if (mixer != NULL) {
                        name = fastTrackNames[i];
                        ALOG_ASSERT(name >= 0);
                        mixer->deleteTrackName(name);
                    }
#if !LOG_NDEBUG
                    fastTrackNames[i] = -1;
#endif
                    // don't reset track dump state, since other side is ignoring it
                    generations[i] = fastTrack->mGeneration;
                }

                // now process added tracks
                unsigned addedTracks = currentTrackMask & ~previousTrackMask;
                while (addedTracks != 0) {
                    i = __builtin_ctz(addedTracks);
                    addedTracks &= ~(1 << i);
                    const FastTrack* fastTrack = &current->mFastTracks[i];
                    AudioBufferProvider *bufferProvider = fastTrack->mBufferProvider;
                    ALOG_ASSERT(bufferProvider != NULL && fastTrackNames[i] == -1);
                    if (mixer != NULL) {
                        // calling getTrackName with default channel mask and a random invalid
                        //   sessionId (no effects here)
                        name = mixer->getTrackName(AUDIO_CHANNEL_OUT_STEREO, -555);
                        ALOG_ASSERT(name >= 0);
                        fastTrackNames[i] = name;
                        mixer->setBufferProvider(name, bufferProvider);
                        mixer->setParameter(name, AudioMixer::TRACK, AudioMixer::MAIN_BUFFER,
                                (void *) mixBuffer);
                        // newly allocated track names default to full scale volume
                        if (fastTrack->mSampleRate != 0 && fastTrack->mSampleRate != sampleRate) {
                            mixer->setParameter(name, AudioMixer::RESAMPLE,
                                    AudioMixer::SAMPLE_RATE, (void*) fastTrack->mSampleRate);
                        }
                        mixer->setParameter(name, AudioMixer::TRACK, AudioMixer::CHANNEL_MASK,
                                (void *) fastTrack->mChannelMask);
                        mixer->enable(name);
                    }
                    generations[i] = fastTrack->mGeneration;
                }

                // finally process (potentially) modified tracks; these use the same slot
                // but may have a different buffer provider or volume provider
                unsigned modifiedTracks = currentTrackMask & previousTrackMask;
                while (modifiedTracks != 0) {
                    i = __builtin_ctz(modifiedTracks);
                    modifiedTracks &= ~(1 << i);
                    const FastTrack* fastTrack = &current->mFastTracks[i];
                    if (fastTrack->mGeneration != generations[i]) {
                        // this track was actually modified
                        AudioBufferProvider *bufferProvider = fastTrack->mBufferProvider;
                        ALOG_ASSERT(bufferProvider != NULL);
                        if (mixer != NULL) {
                            name = fastTrackNames[i];
                            ALOG_ASSERT(name >= 0);
                            mixer->setBufferProvider(name, bufferProvider);
                            if (fastTrack->mVolumeProvider == NULL) {
                                mixer->setParameter(name, AudioMixer::VOLUME, AudioMixer::VOLUME0,
                                        (void *)0x1000);
                                mixer->setParameter(name, AudioMixer::VOLUME, AudioMixer::VOLUME1,
                                        (void *)0x1000);
                            }
                            if (fastTrack->mSampleRate != 0 &&
                                    fastTrack->mSampleRate != sampleRate) {
                                mixer->setParameter(name, AudioMixer::RESAMPLE,
                                        AudioMixer::SAMPLE_RATE, (void*) fastTrack->mSampleRate);
                            } else {
                                mixer->setParameter(name, AudioMixer::RESAMPLE,
                                        AudioMixer::REMOVE, NULL);
                            }
                            mixer->setParameter(name, AudioMixer::TRACK, AudioMixer::CHANNEL_MASK,
                                    (void *) fastTrack->mChannelMask);
                            // already enabled
                        }
                        generations[i] = fastTrack->mGeneration;
                    }
                }

                fastTracksGen = current->mFastTracksGen;

                dumpState->mNumTracks = popcount(currentTrackMask);
            }

#if 1   // FIXME shouldn't need this
            // only process state change once
            previous = current;
#endif
        }

        // do work using current state here
        if ((command & FastMixerState::MIX) && (mixer != NULL) && isWarm) {
            ALOG_ASSERT(mixBuffer != NULL);
            // for each track, update volume and check for underrun
            unsigned currentTrackMask = current->mTrackMask;
            while (currentTrackMask != 0) {
                i = __builtin_ctz(currentTrackMask);
                currentTrackMask &= ~(1 << i);
                const FastTrack* fastTrack = &current->mFastTracks[i];

                // Refresh the per-track timestamp
                if (timestampStatus == NO_ERROR) {
                    uint32_t trackFramesWrittenButNotPresented;
                    uint32_t trackSampleRate = fastTrack->mSampleRate;
                    // There is currently no sample rate conversion for fast tracks currently
                    if (trackSampleRate != 0 && trackSampleRate != sampleRate) {
                        trackFramesWrittenButNotPresented =
                                ((int64_t) nativeFramesWrittenButNotPresented * trackSampleRate) /
                                sampleRate;
                    } else {
                        trackFramesWrittenButNotPresented = nativeFramesWrittenButNotPresented;
                    }
                    uint32_t trackFramesWritten = fastTrack->mBufferProvider->framesReleased();
                    // Can't provide an AudioTimestamp before first frame presented,
                    // or during the brief 32-bit wraparound window
                    if (trackFramesWritten >= trackFramesWrittenButNotPresented) {
                        AudioTimestamp perTrackTimestamp;
                        perTrackTimestamp.mPosition =
                                trackFramesWritten - trackFramesWrittenButNotPresented;
                        perTrackTimestamp.mTime = timestamp.mTime;
                        fastTrack->mBufferProvider->onTimestamp(perTrackTimestamp);
                    }
                }

                int name = fastTrackNames[i];
                ALOG_ASSERT(name >= 0);
                if (fastTrack->mVolumeProvider != NULL) {
                    uint32_t vlr = fastTrack->mVolumeProvider->getVolumeLR();
                    mixer->setParameter(name, AudioMixer::VOLUME, AudioMixer::VOLUME0,
                            (void *)(vlr & 0xFFFF));
                    mixer->setParameter(name, AudioMixer::VOLUME, AudioMixer::VOLUME1,
                            (void *)(vlr >> 16));
                }
                // FIXME The current implementation of framesReady() for fast tracks
                // takes a tryLock, which can block
                // up to 1 ms.  If enough active tracks all blocked in sequence, this would result
                // in the overall fast mix cycle being delayed.  Should use a non-blocking FIFO.
                size_t framesReady = fastTrack->mBufferProvider->framesReady();
                if (ATRACE_ENABLED()) {
                    // I wish we had formatted trace names
                    char traceName[16];
                    strcpy(traceName, "fRdy");
                    traceName[4] = i + (i < 10 ? '0' : 'A' - 10);
                    traceName[5] = '\0';
                    ATRACE_INT(traceName, framesReady);
                }
                FastTrackDump *ftDump = &dumpState->mTracks[i];
                FastTrackUnderruns underruns = ftDump->mUnderruns;
                if (framesReady < frameCount) {
                    if (framesReady == 0) {
                        underruns.mBitFields.mEmpty++;
                        underruns.mBitFields.mMostRecent = UNDERRUN_EMPTY;
                        mixer->disable(name);
                    } else {
                        // allow mixing partial buffer
                        underruns.mBitFields.mPartial++;
                        underruns.mBitFields.mMostRecent = UNDERRUN_PARTIAL;
                        mixer->enable(name);
                    }
                } else {
                    underruns.mBitFields.mFull++;
                    underruns.mBitFields.mMostRecent = UNDERRUN_FULL;
                    mixer->enable(name);
                }
                ftDump->mUnderruns = underruns;
                ftDump->mFramesReady = framesReady;
            }

            int64_t pts;
            if (outputSink == NULL || (OK != outputSink->getNextWriteTimestamp(&pts)))
                pts = AudioBufferProvider::kInvalidPTS;

            // process() is CPU-bound
            mixer->process(pts);
            mixBufferState = MIXED;
        } else if (mixBufferState == MIXED) {
            mixBufferState = UNDEFINED;
        }
        bool attemptedWrite = false;
        //bool didFullWrite = false;    // dumpsys could display a count of partial writes
        if ((command & FastMixerState::WRITE) && (outputSink != NULL) && (mixBuffer != NULL)) {
            if (mixBufferState == UNDEFINED) {
                memset(mixBuffer, 0, frameCount * FCC_2 * sizeof(short));
                mixBufferState = ZEROED;
            }
            if (teeSink != NULL) {
                (void) teeSink->write(mixBuffer, frameCount);
            }
            // FIXME write() is non-blocking and lock-free for a properly implemented NBAIO sink,
            //       but this code should be modified to handle both non-blocking and blocking sinks
            dumpState->mWriteSequence++;
            ATRACE_BEGIN("write");
            ssize_t framesWritten = outputSink->write(mixBuffer, frameCount);
            ATRACE_END();
            dumpState->mWriteSequence++;
            if (framesWritten >= 0) {
                ALOG_ASSERT((size_t) framesWritten <= frameCount);
                totalNativeFramesWritten += framesWritten;
                dumpState->mFramesWritten = totalNativeFramesWritten;
                //if ((size_t) framesWritten == frameCount) {
                //    didFullWrite = true;
                //}
            } else {
                dumpState->mWriteErrors++;
            }
            attemptedWrite = true;
            // FIXME count # of writes blocked excessively, CPU usage, etc. for dump

            timestampStatus = outputSink->getTimestamp(timestamp);
            if (timestampStatus == NO_ERROR) {
                uint32_t totalNativeFramesPresented = timestamp.mPosition;
                if (totalNativeFramesPresented <= totalNativeFramesWritten) {
                    nativeFramesWrittenButNotPresented =
                        totalNativeFramesWritten - totalNativeFramesPresented;
                } else {
                    // HAL reported that more frames were presented than were written
                    timestampStatus = INVALID_OPERATION;
                }
            }
        }

        // To be exactly periodic, compute the next sleep time based on current time.
        // This code doesn't have long-term stability when the sink is non-blocking.
        // FIXME To avoid drift, use the local audio clock or watch the sink's fill status.
        struct timespec newTs;
        int rc = clock_gettime(CLOCK_MONOTONIC, &newTs);
        if (rc == 0) {
            //logWriter->logTimestamp(newTs);
            if (oldTsValid) {
                time_t sec = newTs.tv_sec - oldTs.tv_sec;
                long nsec = newTs.tv_nsec - oldTs.tv_nsec;
                ALOGE_IF(sec < 0 || (sec == 0 && nsec < 0),
                        "clock_gettime(CLOCK_MONOTONIC) failed: was %ld.%09ld but now %ld.%09ld",
                        oldTs.tv_sec, oldTs.tv_nsec, newTs.tv_sec, newTs.tv_nsec);
                if (nsec < 0) {
                    --sec;
                    nsec += 1000000000;
                }
                // To avoid an initial underrun on fast tracks after exiting standby,
                // do not start pulling data from tracks and mixing until warmup is complete.
                // Warmup is considered complete after the earlier of:
                //      MIN_WARMUP_CYCLES write() attempts and last one blocks for at least warmupNs
                //      MAX_WARMUP_CYCLES write() attempts.
                // This is overly conservative, but to get better accuracy requires a new HAL API.
                if (!isWarm && attemptedWrite) {
                    measuredWarmupTs.tv_sec += sec;
                    measuredWarmupTs.tv_nsec += nsec;
                    if (measuredWarmupTs.tv_nsec >= 1000000000) {
                        measuredWarmupTs.tv_sec++;
                        measuredWarmupTs.tv_nsec -= 1000000000;
                    }
                    ++warmupCycles;
                    if ((nsec > warmupNs && warmupCycles >= MIN_WARMUP_CYCLES) ||
                            (warmupCycles >= MAX_WARMUP_CYCLES)) {
                        isWarm = true;
                        dumpState->mMeasuredWarmupTs = measuredWarmupTs;
                        dumpState->mWarmupCycles = warmupCycles;
                    }
                }
                sleepNs = -1;
                if (isWarm) {
                    if (sec > 0 || nsec > underrunNs) {
                        ATRACE_NAME("underrun");
                        // FIXME only log occasionally
                        ALOGV("underrun: time since last cycle %d.%03ld sec",
                                (int) sec, nsec / 1000000L);
                        dumpState->mUnderruns++;
                        ignoreNextOverrun = true;
                    } else if (nsec < overrunNs) {
                        if (ignoreNextOverrun) {
                            ignoreNextOverrun = false;
                        } else {
                            // FIXME only log occasionally
                            ALOGV("overrun: time since last cycle %d.%03ld sec",
                                    (int) sec, nsec / 1000000L);
                            dumpState->mOverruns++;
                        }
                        // This forces a minimum cycle time. It:
                        //  - compensates for an audio HAL with jitter due to sample rate conversion
                        //  - works with a variable buffer depth audio HAL that never pulls at a
                        //    rate < than overrunNs per buffer.
                        //  - recovers from overrun immediately after underrun
                        // It doesn't work with a non-blocking audio HAL.
                        sleepNs = forceNs - nsec;
                    } else {
                        ignoreNextOverrun = false;
                    }
                }
#ifdef FAST_MIXER_STATISTICS
                if (isWarm) {
                    // advance the FIFO queue bounds
                    size_t i = bounds & (dumpState->mSamplingN - 1);
                    bounds = (bounds & 0xFFFF0000) | ((bounds + 1) & 0xFFFF);
                    if (full) {
                        bounds += 0x10000;
                    } else if (!(bounds & (dumpState->mSamplingN - 1))) {
                        full = true;
                    }
                    // compute the delta value of clock_gettime(CLOCK_MONOTONIC)
                    uint32_t monotonicNs = nsec;
                    if (sec > 0 && sec < 4) {
                        monotonicNs += sec * 1000000000;
                    }
                    // compute raw CPU load = delta value of clock_gettime(CLOCK_THREAD_CPUTIME_ID)
                    uint32_t loadNs = 0;
                    struct timespec newLoad;
                    rc = clock_gettime(CLOCK_THREAD_CPUTIME_ID, &newLoad);
                    if (rc == 0) {
                        if (oldLoadValid) {
                            sec = newLoad.tv_sec - oldLoad.tv_sec;
                            nsec = newLoad.tv_nsec - oldLoad.tv_nsec;
                            if (nsec < 0) {
                                --sec;
                                nsec += 1000000000;
                            }
                            loadNs = nsec;
                            if (sec > 0 && sec < 4) {
                                loadNs += sec * 1000000000;
                            }
                        } else {
                            // first time through the loop
                            oldLoadValid = true;
                        }
                        oldLoad = newLoad;
                    }
#ifdef CPU_FREQUENCY_STATISTICS
                    // get the absolute value of CPU clock frequency in kHz
                    int cpuNum = sched_getcpu();
                    uint32_t kHz = tcu.getCpukHz(cpuNum);
                    kHz = (kHz << 4) | (cpuNum & 0xF);
#endif
                    // save values in FIFO queues for dumpsys
                    // these stores #1, #2, #3 are not atomic with respect to each other,
                    // or with respect to store #4 below
                    dumpState->mMonotonicNs[i] = monotonicNs;
                    dumpState->mLoadNs[i] = loadNs;
#ifdef CPU_FREQUENCY_STATISTICS
                    dumpState->mCpukHz[i] = kHz;
#endif
                    // this store #4 is not atomic with respect to stores #1, #2, #3 above, but
                    // the newest open & oldest closed halves are atomic with respect to each other
                    dumpState->mBounds = bounds;
                    ATRACE_INT("cycle_ms", monotonicNs / 1000000);
                    ATRACE_INT("load_us", loadNs / 1000);
                }
#endif
            } else {
                // first time through the loop
                oldTsValid = true;
                sleepNs = periodNs;
                ignoreNextOverrun = true;
            }
            oldTs = newTs;
        } else {
            // monotonic clock is broken
            oldTsValid = false;
            sleepNs = periodNs;
        }


    }   // for (;;)

    // never return 'true'; Thread::_threadLoop() locks mutex which can result in priority inversion
}

FastMixerDumpState::FastMixerDumpState(
#ifdef FAST_MIXER_STATISTICS
        uint32_t samplingN
#endif
        ) :
    mCommand(FastMixerState::INITIAL), mWriteSequence(0), mFramesWritten(0),
    mNumTracks(0), mWriteErrors(0), mUnderruns(0), mOverruns(0),
    mSampleRate(0), mFrameCount(0), /* mMeasuredWarmupTs({0, 0}), */ mWarmupCycles(0),
    mTrackMask(0)
#ifdef FAST_MIXER_STATISTICS
    , mSamplingN(0), mBounds(0)
#endif
{
    mMeasuredWarmupTs.tv_sec = 0;
    mMeasuredWarmupTs.tv_nsec = 0;
#ifdef FAST_MIXER_STATISTICS
    increaseSamplingN(samplingN);
#endif
}

#ifdef FAST_MIXER_STATISTICS
void FastMixerDumpState::increaseSamplingN(uint32_t samplingN)
{
    if (samplingN <= mSamplingN || samplingN > kSamplingN || roundup(samplingN) != samplingN) {
        return;
    }
    uint32_t additional = samplingN - mSamplingN;
    // sample arrays aren't accessed atomically with respect to the bounds,
    // so clearing reduces chance for dumpsys to read random uninitialized samples
    memset(&mMonotonicNs[mSamplingN], 0, sizeof(mMonotonicNs[0]) * additional);
    memset(&mLoadNs[mSamplingN], 0, sizeof(mLoadNs[0]) * additional);
#ifdef CPU_FREQUENCY_STATISTICS
    memset(&mCpukHz[mSamplingN], 0, sizeof(mCpukHz[0]) * additional);
#endif
    mSamplingN = samplingN;
}
#endif

FastMixerDumpState::~FastMixerDumpState()
{
}

// helper function called by qsort()
static int compare_uint32_t(const void *pa, const void *pb)
{
    uint32_t a = *(const uint32_t *)pa;
    uint32_t b = *(const uint32_t *)pb;
    if (a < b) {
        return -1;
    } else if (a > b) {
        return 1;
    } else {
        return 0;
    }
}

void FastMixerDumpState::dump(int fd) const
{
    if (mCommand == FastMixerState::INITIAL) {
        fdprintf(fd, "FastMixer not initialized\n");
        return;
    }
#define COMMAND_MAX 32
    char string[COMMAND_MAX];
    switch (mCommand) {
    case FastMixerState::INITIAL:
        strcpy(string, "INITIAL");
        break;
    case FastMixerState::HOT_IDLE:
        strcpy(string, "HOT_IDLE");
        break;
    case FastMixerState::COLD_IDLE:
        strcpy(string, "COLD_IDLE");
        break;
    case FastMixerState::EXIT:
        strcpy(string, "EXIT");
        break;
    case FastMixerState::MIX:
        strcpy(string, "MIX");
        break;
    case FastMixerState::WRITE:
        strcpy(string, "WRITE");
        break;
    case FastMixerState::MIX_WRITE:
        strcpy(string, "MIX_WRITE");
        break;
    default:
        snprintf(string, COMMAND_MAX, "%d", mCommand);
        break;
    }
    double measuredWarmupMs = (mMeasuredWarmupTs.tv_sec * 1000.0) +
            (mMeasuredWarmupTs.tv_nsec / 1000000.0);
    double mixPeriodSec = (double) mFrameCount / (double) mSampleRate;
    fdprintf(fd, "FastMixer command=%s writeSequence=%u framesWritten=%u\n"
                 "          numTracks=%u writeErrors=%u underruns=%u overruns=%u\n"
                 "          sampleRate=%u frameCount=%u measuredWarmup=%.3g ms, warmupCycles=%u\n"
                 "          mixPeriod=%.2f ms\n",
                 string, mWriteSequence, mFramesWritten,
                 mNumTracks, mWriteErrors, mUnderruns, mOverruns,
                 mSampleRate, mFrameCount, measuredWarmupMs, mWarmupCycles,
                 mixPeriodSec * 1e3);
#ifdef FAST_MIXER_STATISTICS
    // find the interval of valid samples
    uint32_t bounds = mBounds;
    uint32_t newestOpen = bounds & 0xFFFF;
    uint32_t oldestClosed = bounds >> 16;
    uint32_t n = (newestOpen - oldestClosed) & 0xFFFF;
    if (n > mSamplingN) {
        ALOGE("too many samples %u", n);
        n = mSamplingN;
    }
    // statistics for monotonic (wall clock) time, thread raw CPU load in time, CPU clock frequency,
    // and adjusted CPU load in MHz normalized for CPU clock frequency
    CentralTendencyStatistics wall, loadNs;
#ifdef CPU_FREQUENCY_STATISTICS
    CentralTendencyStatistics kHz, loadMHz;
    uint32_t previousCpukHz = 0;
#endif
    // Assuming a normal distribution for cycle times, three standard deviations on either side of
    // the mean account for 99.73% of the population.  So if we take each tail to be 1/1000 of the
    // sample set, we get 99.8% combined, or close to three standard deviations.
    static const uint32_t kTailDenominator = 1000;
    uint32_t *tail = n >= kTailDenominator ? new uint32_t[n] : NULL;
    // loop over all the samples
    for (uint32_t j = 0; j < n; ++j) {
        size_t i = oldestClosed++ & (mSamplingN - 1);
        uint32_t wallNs = mMonotonicNs[i];
        if (tail != NULL) {
            tail[j] = wallNs;
        }
        wall.sample(wallNs);
        uint32_t sampleLoadNs = mLoadNs[i];
        loadNs.sample(sampleLoadNs);
#ifdef CPU_FREQUENCY_STATISTICS
        uint32_t sampleCpukHz = mCpukHz[i];
        // skip bad kHz samples
        if ((sampleCpukHz & ~0xF) != 0) {
            kHz.sample(sampleCpukHz >> 4);
            if (sampleCpukHz == previousCpukHz) {
                double megacycles = (double) sampleLoadNs * (double) (sampleCpukHz >> 4) * 1e-12;
                double adjMHz = megacycles / mixPeriodSec;  // _not_ wallNs * 1e9
                loadMHz.sample(adjMHz);
            }
        }
        previousCpukHz = sampleCpukHz;
#endif
    }
    fdprintf(fd, "Simple moving statistics over last %.1f seconds:\n", wall.n() * mixPeriodSec);
    fdprintf(fd, "  wall clock time in ms per mix cycle:\n"
                 "    mean=%.2f min=%.2f max=%.2f stddev=%.2f\n",
                 wall.mean()*1e-6, wall.minimum()*1e-6, wall.maximum()*1e-6, wall.stddev()*1e-6);
    fdprintf(fd, "  raw CPU load in us per mix cycle:\n"
                 "    mean=%.0f min=%.0f max=%.0f stddev=%.0f\n",
                 loadNs.mean()*1e-3, loadNs.minimum()*1e-3, loadNs.maximum()*1e-3,
                 loadNs.stddev()*1e-3);
#ifdef CPU_FREQUENCY_STATISTICS
    fdprintf(fd, "  CPU clock frequency in MHz:\n"
                 "    mean=%.0f min=%.0f max=%.0f stddev=%.0f\n",
                 kHz.mean()*1e-3, kHz.minimum()*1e-3, kHz.maximum()*1e-3, kHz.stddev()*1e-3);
    fdprintf(fd, "  adjusted CPU load in MHz (i.e. normalized for CPU clock frequency):\n"
                 "    mean=%.1f min=%.1f max=%.1f stddev=%.1f\n",
                 loadMHz.mean(), loadMHz.minimum(), loadMHz.maximum(), loadMHz.stddev());
#endif
    if (tail != NULL) {
        qsort(tail, n, sizeof(uint32_t), compare_uint32_t);
        // assume same number of tail samples on each side, left and right
        uint32_t count = n / kTailDenominator;
        CentralTendencyStatistics left, right;
        for (uint32_t i = 0; i < count; ++i) {
            left.sample(tail[i]);
            right.sample(tail[n - (i + 1)]);
        }
        fdprintf(fd, "Distribution of mix cycle times in ms for the tails (> ~3 stddev outliers):\n"
                     "  left tail: mean=%.2f min=%.2f max=%.2f stddev=%.2f\n"
                     "  right tail: mean=%.2f min=%.2f max=%.2f stddev=%.2f\n",
                     left.mean()*1e-6, left.minimum()*1e-6, left.maximum()*1e-6, left.stddev()*1e-6,
                     right.mean()*1e-6, right.minimum()*1e-6, right.maximum()*1e-6,
                     right.stddev()*1e-6);
        delete[] tail;
    }
#endif
    // The active track mask and track states are updated non-atomically.
    // So if we relied on isActive to decide whether to display,
    // then we might display an obsolete track or omit an active track.
    // Instead we always display all tracks, with an indication
    // of whether we think the track is active.
    uint32_t trackMask = mTrackMask;
    fdprintf(fd, "Fast tracks: kMaxFastTracks=%u activeMask=%#x\n",
            FastMixerState::kMaxFastTracks, trackMask);
    fdprintf(fd, "Index Active Full Partial Empty  Recent Ready\n");
    for (uint32_t i = 0; i < FastMixerState::kMaxFastTracks; ++i, trackMask >>= 1) {
        bool isActive = trackMask & 1;
        const FastTrackDump *ftDump = &mTracks[i];
        const FastTrackUnderruns& underruns = ftDump->mUnderruns;
        const char *mostRecent;
        switch (underruns.mBitFields.mMostRecent) {
        case UNDERRUN_FULL:
            mostRecent = "full";
            break;
        case UNDERRUN_PARTIAL:
            mostRecent = "partial";
            break;
        case UNDERRUN_EMPTY:
            mostRecent = "empty";
            break;
        default:
            mostRecent = "?";
            break;
        }
        fdprintf(fd, "%5u %6s %4u %7u %5u %7s %5u\n", i, isActive ? "yes" : "no",
                (underruns.mBitFields.mFull) & UNDERRUN_MASK,
                (underruns.mBitFields.mPartial) & UNDERRUN_MASK,
                (underruns.mBitFields.mEmpty) & UNDERRUN_MASK,
                mostRecent, ftDump->mFramesReady);
    }
}

}   // namespace android
