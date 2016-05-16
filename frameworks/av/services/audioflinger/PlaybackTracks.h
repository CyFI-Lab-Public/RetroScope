/*
**
** Copyright 2012, The Android Open Source Project
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

#ifndef INCLUDING_FROM_AUDIOFLINGER_H
    #error This header file should only be included from AudioFlinger.h
#endif

// playback track
class Track : public TrackBase, public VolumeProvider {
public:
                        Track(  PlaybackThread *thread,
                                const sp<Client>& client,
                                audio_stream_type_t streamType,
                                uint32_t sampleRate,
                                audio_format_t format,
                                audio_channel_mask_t channelMask,
                                size_t frameCount,
                                const sp<IMemory>& sharedBuffer,
                                int sessionId,
                                int uid,
                                IAudioFlinger::track_flags_t flags);
    virtual             ~Track();

    static  void        appendDumpHeader(String8& result);
            void        dump(char* buffer, size_t size);
    virtual status_t    start(AudioSystem::sync_event_t event =
                                    AudioSystem::SYNC_EVENT_NONE,
                             int triggerSession = 0);
    virtual void        stop();
            void        pause();

            void        flush();
            void        destroy();
            int         name() const { return mName; }

    virtual uint32_t    sampleRate() const;

            audio_stream_type_t streamType() const {
                return mStreamType;
            }
            bool        isOffloaded() const { return (mFlags & IAudioFlinger::TRACK_OFFLOAD) != 0; }
            status_t    setParameters(const String8& keyValuePairs);
            status_t    attachAuxEffect(int EffectId);
            void        setAuxBuffer(int EffectId, int32_t *buffer);
            int32_t     *auxBuffer() const { return mAuxBuffer; }
            void        setMainBuffer(int16_t *buffer) { mMainBuffer = buffer; }
            int16_t     *mainBuffer() const { return mMainBuffer; }
            int         auxEffectId() const { return mAuxEffectId; }
    virtual status_t    getTimestamp(AudioTimestamp& timestamp);
            void        signal();

// implement FastMixerState::VolumeProvider interface
    virtual uint32_t    getVolumeLR();

    virtual status_t    setSyncEvent(const sp<SyncEvent>& event);

protected:
    // for numerous
    friend class PlaybackThread;
    friend class MixerThread;
    friend class DirectOutputThread;
    friend class OffloadThread;

                        Track(const Track&);
                        Track& operator = (const Track&);

    // AudioBufferProvider interface
    virtual status_t getNextBuffer(AudioBufferProvider::Buffer* buffer,
                                   int64_t pts = kInvalidPTS);
    // releaseBuffer() not overridden

    // ExtendedAudioBufferProvider interface
    virtual size_t framesReady() const;
    virtual size_t framesReleased() const;

    bool isPausing() const { return mState == PAUSING; }
    bool isPaused() const { return mState == PAUSED; }
    bool isResuming() const { return mState == RESUMING; }
    bool isReady() const;
    void setPaused() { mState = PAUSED; }
    void reset();

    bool isOutputTrack() const {
        return (mStreamType == AUDIO_STREAM_CNT);
    }

    sp<IMemory> sharedBuffer() const { return mSharedBuffer; }

    // framesWritten is cumulative, never reset, and is shared all tracks
    // audioHalFrames is derived from output latency
    // FIXME parameters not needed, could get them from the thread
    bool presentationComplete(size_t framesWritten, size_t audioHalFrames);

public:
    void triggerEvents(AudioSystem::sync_event_t type);
    void invalidate();
    bool isInvalid() const { return mIsInvalid; }
    virtual bool isTimedTrack() const { return false; }
    bool isFastTrack() const { return (mFlags & IAudioFlinger::TRACK_FAST) != 0; }
    int fastIndex() const { return mFastIndex; }

protected:

    // FILLED state is used for suppressing volume ramp at begin of playing
    enum {FS_INVALID, FS_FILLING, FS_FILLED, FS_ACTIVE};
    mutable uint8_t     mFillingUpStatus;
    int8_t              mRetryCount;

    // see comment at AudioFlinger::PlaybackThread::Track::~Track for why this can't be const
    sp<IMemory>         mSharedBuffer;

    bool                mResetDone;
    const audio_stream_type_t mStreamType;
    int                 mName;      // track name on the normal mixer,
                                    // allocated statically at track creation time,
                                    // and is even allocated (though unused) for fast tracks
                                    // FIXME don't allocate track name for fast tracks
    int16_t             *mMainBuffer;
    int32_t             *mAuxBuffer;
    int                 mAuxEffectId;
    bool                mHasVolumeController;
    size_t              mPresentationCompleteFrames; // number of frames written to the
                                    // audio HAL when this track will be fully rendered
                                    // zero means not monitoring
private:
    IAudioFlinger::track_flags_t mFlags;

    // The following fields are only for fast tracks, and should be in a subclass
    int                 mFastIndex; // index within FastMixerState::mFastTracks[];
                                    // either mFastIndex == -1 if not isFastTrack()
                                    // or 0 < mFastIndex < FastMixerState::kMaxFast because
                                    // index 0 is reserved for normal mixer's submix;
                                    // index is allocated statically at track creation time
                                    // but the slot is only used if track is active
    FastTrackUnderruns  mObservedUnderruns; // Most recently observed value of
                                    // mFastMixerDumpState.mTracks[mFastIndex].mUnderruns
    volatile float      mCachedVolume;  // combined master volume and stream type volume;
                                        // 'volatile' means accessed without lock or
                                        // barrier, but is read/written atomically
    bool                mIsInvalid; // non-resettable latch, set by invalidate()
    AudioTrackServerProxy*  mAudioTrackServerProxy;
    bool                mResumeToStopping; // track was paused in stopping state.
};  // end of Track

class TimedTrack : public Track {
  public:
    static sp<TimedTrack> create(PlaybackThread *thread,
                                 const sp<Client>& client,
                                 audio_stream_type_t streamType,
                                 uint32_t sampleRate,
                                 audio_format_t format,
                                 audio_channel_mask_t channelMask,
                                 size_t frameCount,
                                 const sp<IMemory>& sharedBuffer,
                                 int sessionId,
                                 int uid);
    virtual ~TimedTrack();

    class TimedBuffer {
      public:
        TimedBuffer();
        TimedBuffer(const sp<IMemory>& buffer, int64_t pts);
        const sp<IMemory>& buffer() const { return mBuffer; }
        int64_t pts() const { return mPTS; }
        uint32_t position() const { return mPosition; }
        void setPosition(uint32_t pos) { mPosition = pos; }
      private:
        sp<IMemory> mBuffer;
        int64_t     mPTS;
        uint32_t    mPosition;
    };

    // Mixer facing methods.
    virtual bool isTimedTrack() const { return true; }
    virtual size_t framesReady() const;

    // AudioBufferProvider interface
    virtual status_t getNextBuffer(AudioBufferProvider::Buffer* buffer,
                                   int64_t pts);
    virtual void releaseBuffer(AudioBufferProvider::Buffer* buffer);

    // Client/App facing methods.
    status_t    allocateTimedBuffer(size_t size,
                                    sp<IMemory>* buffer);
    status_t    queueTimedBuffer(const sp<IMemory>& buffer,
                                 int64_t pts);
    status_t    setMediaTimeTransform(const LinearTransform& xform,
                                      TimedAudioTrack::TargetTimeline target);

  private:
    TimedTrack(PlaybackThread *thread,
               const sp<Client>& client,
               audio_stream_type_t streamType,
               uint32_t sampleRate,
               audio_format_t format,
               audio_channel_mask_t channelMask,
               size_t frameCount,
               const sp<IMemory>& sharedBuffer,
               int sessionId,
               int uid);

    void timedYieldSamples_l(AudioBufferProvider::Buffer* buffer);
    void timedYieldSilence_l(uint32_t numFrames,
                             AudioBufferProvider::Buffer* buffer);
    void trimTimedBufferQueue_l();
    void trimTimedBufferQueueHead_l(const char* logTag);
    void updateFramesPendingAfterTrim_l(const TimedBuffer& buf,
                                        const char* logTag);

    uint64_t            mLocalTimeFreq;
    LinearTransform     mLocalTimeToSampleTransform;
    LinearTransform     mMediaTimeToSampleTransform;
    sp<MemoryDealer>    mTimedMemoryDealer;

    Vector<TimedBuffer> mTimedBufferQueue;
    bool                mQueueHeadInFlight;
    bool                mTrimQueueHeadOnRelease;
    uint32_t            mFramesPendingInQueue;

    uint8_t*            mTimedSilenceBuffer;
    uint32_t            mTimedSilenceBufferSize;
    mutable Mutex       mTimedBufferQueueLock;
    bool                mTimedAudioOutputOnTime;
    CCHelper            mCCHelper;

    Mutex               mMediaTimeTransformLock;
    LinearTransform     mMediaTimeTransform;
    bool                mMediaTimeTransformValid;
    TimedAudioTrack::TargetTimeline mMediaTimeTransformTarget;
};


// playback track, used by DuplicatingThread
class OutputTrack : public Track {
public:

    class Buffer : public AudioBufferProvider::Buffer {
    public:
        int16_t *mBuffer;
    };

                        OutputTrack(PlaybackThread *thread,
                                DuplicatingThread *sourceThread,
                                uint32_t sampleRate,
                                audio_format_t format,
                                audio_channel_mask_t channelMask,
                                size_t frameCount,
                                int uid);
    virtual             ~OutputTrack();

    virtual status_t    start(AudioSystem::sync_event_t event =
                                    AudioSystem::SYNC_EVENT_NONE,
                             int triggerSession = 0);
    virtual void        stop();
            bool        write(int16_t* data, uint32_t frames);
            bool        bufferQueueEmpty() const { return mBufferQueue.size() == 0; }
            bool        isActive() const { return mActive; }
    const wp<ThreadBase>& thread() const { return mThread; }

private:

    status_t            obtainBuffer(AudioBufferProvider::Buffer* buffer,
                                     uint32_t waitTimeMs);
    void                clearBufferQueue();

    // Maximum number of pending buffers allocated by OutputTrack::write()
    static const uint8_t kMaxOverFlowBuffers = 10;

    Vector < Buffer* >          mBufferQueue;
    AudioBufferProvider::Buffer mOutBuffer;
    bool                        mActive;
    DuplicatingThread* const mSourceThread; // for waitTimeMs() in write()
    AudioTrackClientProxy*      mClientProxy;
};  // end of OutputTrack
