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

// base for record and playback
class TrackBase : public ExtendedAudioBufferProvider, public RefBase {

public:
    enum track_state {
        IDLE,
        FLUSHED,
        STOPPED,
        // next 2 states are currently used for fast tracks
        // and offloaded tracks only
        STOPPING_1,     // waiting for first underrun
        STOPPING_2,     // waiting for presentation complete
        RESUMING,
        ACTIVE,
        PAUSING,
        PAUSED
    };

                        TrackBase(ThreadBase *thread,
                                const sp<Client>& client,
                                uint32_t sampleRate,
                                audio_format_t format,
                                audio_channel_mask_t channelMask,
                                size_t frameCount,
                                const sp<IMemory>& sharedBuffer,
                                int sessionId,
                                int uid,
                                bool isOut);
    virtual             ~TrackBase();

    virtual status_t    start(AudioSystem::sync_event_t event,
                             int triggerSession) = 0;
    virtual void        stop() = 0;
            sp<IMemory> getCblk() const { return mCblkMemory; }
            audio_track_cblk_t* cblk() const { return mCblk; }
            int         sessionId() const { return mSessionId; }
            int         uid() const { return mUid; }
    virtual status_t    setSyncEvent(const sp<SyncEvent>& event);

protected:
                        TrackBase(const TrackBase&);
                        TrackBase& operator = (const TrackBase&);

    // AudioBufferProvider interface
    virtual status_t getNextBuffer(AudioBufferProvider::Buffer* buffer, int64_t pts) = 0;
    virtual void releaseBuffer(AudioBufferProvider::Buffer* buffer);

    // ExtendedAudioBufferProvider interface is only needed for Track,
    // but putting it in TrackBase avoids the complexity of virtual inheritance
    virtual size_t  framesReady() const { return SIZE_MAX; }

    audio_format_t format() const { return mFormat; }

    uint32_t channelCount() const { return mChannelCount; }

    audio_channel_mask_t channelMask() const { return mChannelMask; }

    virtual uint32_t sampleRate() const { return mSampleRate; }

    // Return a pointer to the start of a contiguous slice of the track buffer.
    // Parameter 'offset' is the requested start position, expressed in
    // monotonically increasing frame units relative to the track epoch.
    // Parameter 'frames' is the requested length, also in frame units.
    // Always returns non-NULL.  It is the caller's responsibility to
    // verify that this will be successful; the result of calling this
    // function with invalid 'offset' or 'frames' is undefined.
    void* getBuffer(uint32_t offset, uint32_t frames) const;

    bool isStopped() const {
        return (mState == STOPPED || mState == FLUSHED);
    }

    // for fast tracks and offloaded tracks only
    bool isStopping() const {
        return mState == STOPPING_1 || mState == STOPPING_2;
    }
    bool isStopping_1() const {
        return mState == STOPPING_1;
    }
    bool isStopping_2() const {
        return mState == STOPPING_2;
    }

    bool isTerminated() const {
        return mTerminated;
    }

    void terminate() {
        mTerminated = true;
    }

    bool isOut() const { return mIsOut; }
                                    // true for Track and TimedTrack, false for RecordTrack,
                                    // this could be a track type if needed later

    const wp<ThreadBase> mThread;
    /*const*/ sp<Client> mClient;   // see explanation at ~TrackBase() why not const
    sp<IMemory>         mCblkMemory;
    audio_track_cblk_t* mCblk;
    void*               mBuffer;    // start of track buffer, typically in shared memory
                                    // except for OutputTrack when it is in local memory
    // we don't really need a lock for these
    track_state         mState;
    const uint32_t      mSampleRate;    // initial sample rate only; for tracks which
                        // support dynamic rates, the current value is in control block
    const audio_format_t mFormat;
    const audio_channel_mask_t mChannelMask;
    const uint32_t      mChannelCount;
    const size_t        mFrameSize; // AudioFlinger's view of frame size in shared memory,
                                    // where for AudioTrack (but not AudioRecord),
                                    // 8-bit PCM samples are stored as 16-bit
    const size_t        mFrameCount;// size of track buffer given at createTrack() or
                                    // openRecord(), and then adjusted as needed

    const int           mSessionId;
    int                 mUid;
    Vector < sp<SyncEvent> >mSyncEvents;
    const bool          mIsOut;
    ServerProxy*        mServerProxy;
    const int           mId;
    sp<NBAIO_Sink>      mTeeSink;
    sp<NBAIO_Source>    mTeeSource;
    bool                mTerminated;
};
