/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef ANDROID_VIDEOEDITOR_PLAYER_H
#define ANDROID_VIDEOEDITOR_PLAYER_H

#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>
#include "M4xVSS_API.h"
#include "VideoEditorMain.h"
#include "VideoEditorTools.h"
#include "VideoEditorAudioPlayer.h"
#include "NativeWindowRenderer.h"

namespace android {

struct PreviewPlayer;

class VideoEditorPlayer : public MediaPlayerInterface {
    public:
    class VeAudioOutput: public MediaPlayerBase::AudioSink
    {
    public:
                                VeAudioOutput();
        virtual                 ~VeAudioOutput();

        virtual bool            ready() const { return mTrack != NULL; }
        virtual bool            realtime() const { return true; }
        virtual ssize_t         bufferSize() const;
        virtual ssize_t         frameCount() const;
        virtual ssize_t         channelCount() const;
        virtual ssize_t         frameSize() const;
        virtual uint32_t        latency() const;
        virtual float           msecsPerFrame() const;
        virtual status_t        getPosition(uint32_t *position) const;
        virtual status_t        getFramesWritten(uint32_t*) const;
        virtual int             getSessionId() const;

        virtual status_t        open(
                uint32_t sampleRate, int channelCount, audio_channel_mask_t channelMask,
                audio_format_t format, int bufferCount,
                AudioCallback cb, void *cookie, audio_output_flags_t flags,
                const audio_offload_info_t *offloadInfo);

        virtual status_t        start();
        virtual ssize_t         write(const void* buffer, size_t size);
        virtual void            stop();
        virtual void            flush();
        virtual void            pause();
        virtual void            close();
        void setAudioStreamType(audio_stream_type_t streamType) { mStreamType = streamType; }
        virtual audio_stream_type_t getAudioStreamType() const { return mStreamType; }
                void            setVolume(float left, float right);
        virtual status_t        dump(int fd,const Vector<String16>& args) const;

        static bool             isOnEmulator();
        static int              getMinBufferCount();
    private:
        static void             setMinBufferCount();
        static void             CallbackWrapper(
                int event, void *me, void *info);

        sp<AudioTrack>          mTrack;
        AudioCallback           mCallback;
        void *                  mCallbackCookie;
        audio_stream_type_t     mStreamType;
        float                   mLeftVolume;
        float                   mRightVolume;
        float                   mMsecsPerFrame;
        uint32_t                mLatency;
        int                     mSessionId;
        static bool             mIsOnEmulator;
        static int              mMinBufferCount; // 12 for emulator; otherwise 4

        public:
        uint32_t                mNumFramesWritten;
        void                    snoopWrite(const void*, size_t);
    };

public:
    VideoEditorPlayer(NativeWindowRenderer* renderer);
    virtual ~VideoEditorPlayer();

    virtual status_t initCheck();

    virtual status_t setDataSource(
            const char *url, const KeyedVector<String8, String8> *headers);

    virtual status_t setDataSource(int fd, int64_t offset, int64_t length);
    virtual status_t setVideoSurface(const sp<Surface> &surface);
    virtual status_t setVideoSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer);
    virtual status_t prepare();
    virtual status_t prepareAsync();
    virtual status_t start();
    virtual status_t stop();
    virtual status_t pause();
    virtual bool isPlaying();
    virtual status_t seekTo(int msec);
    virtual status_t getCurrentPosition(int *msec);
    virtual status_t getDuration(int *msec);
    virtual status_t reset();
    virtual status_t setLooping(int loop);
    virtual player_type playerType();
    virtual status_t invoke(const Parcel &request, Parcel *reply);
    virtual void setAudioSink(const sp<AudioSink> &audioSink);
    virtual void acquireLock();
    virtual void releaseLock();
    virtual status_t setParameter(int key, const Parcel &request);
    virtual status_t getParameter(int key, Parcel *reply);

    virtual status_t getMetadata(
                        const media::Metadata::Filter& ids, Parcel *records);

    virtual status_t loadEffectsSettings(
                         M4VSS3GPP_EffectSettings* pEffectSettings, int nEffects);

    virtual status_t loadAudioMixSettings(
                         M4xVSS_AudioMixingSettings* pAudioMixSettings);

    virtual status_t setAudioMixPCMFileHandle(
                         M4OSA_Context pAudioMixPCMFileHandle);

    virtual status_t setAudioMixStoryBoardParam(
                         M4OSA_UInt32 x, M4OSA_UInt32 y, M4OSA_UInt32 z);

    virtual status_t setPlaybackBeginTime(uint32_t msec);
    virtual status_t setPlaybackEndTime(uint32_t msec);
    virtual status_t setStoryboardStartTime(uint32_t msec);
    virtual status_t setProgressCallbackInterval(uint32_t cbInterval);

    virtual status_t setMediaRenderingMode(M4xVSS_MediaRendering mode,
                          M4VIDEOEDITING_VideoFrameSize outputVideoSize);

    virtual status_t resetJniCallbackTimeStamp();
    virtual status_t setImageClipProperties(uint32_t width, uint32_t height);
    virtual status_t readFirstVideoFrame();
    virtual status_t getLastRenderedTimeMs(uint32_t *lastRenderedTimeMs);

    status_t setAudioPlayer(VideoEditorAudioPlayer *audioPlayer);
private:
    PreviewPlayer       *mPlayer;
    sp<VeAudioOutput>    mVeAudioSink;

    VideoEditorPlayer(const VideoEditorPlayer &);
    VideoEditorPlayer &operator=(const VideoEditorPlayer &);
};

}  // namespace android

#endif  // ANDROID_VIDEOEDITOR_PLAYER_H
