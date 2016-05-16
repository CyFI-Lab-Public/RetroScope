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

class ThreadBase : public Thread {
public:

#include "TrackBase.h"

    enum type_t {
        MIXER,              // Thread class is MixerThread
        DIRECT,             // Thread class is DirectOutputThread
        DUPLICATING,        // Thread class is DuplicatingThread
        RECORD,             // Thread class is RecordThread
        OFFLOAD             // Thread class is OffloadThread
    };

    ThreadBase(const sp<AudioFlinger>& audioFlinger, audio_io_handle_t id,
                audio_devices_t outDevice, audio_devices_t inDevice, type_t type);
    virtual             ~ThreadBase();

    void dumpBase(int fd, const Vector<String16>& args);
    void dumpEffectChains(int fd, const Vector<String16>& args);

    void clearPowerManager();

    // base for record and playback
    enum {
        CFG_EVENT_IO,
        CFG_EVENT_PRIO
    };

    class ConfigEvent {
    public:
        ConfigEvent(int type) : mType(type) {}
        virtual ~ConfigEvent() {}

                 int type() const { return mType; }

        virtual  void dump(char *buffer, size_t size) = 0;

    private:
        const int mType;
    };

    class IoConfigEvent : public ConfigEvent {
    public:
        IoConfigEvent(int event, int param) :
            ConfigEvent(CFG_EVENT_IO), mEvent(event), mParam(event) {}
        virtual ~IoConfigEvent() {}

                int event() const { return mEvent; }
                int param() const { return mParam; }

        virtual  void dump(char *buffer, size_t size) {
            snprintf(buffer, size, "IO event: event %d, param %d\n", mEvent, mParam);
        }

    private:
        const int mEvent;
        const int mParam;
    };

    class PrioConfigEvent : public ConfigEvent {
    public:
        PrioConfigEvent(pid_t pid, pid_t tid, int32_t prio) :
            ConfigEvent(CFG_EVENT_PRIO), mPid(pid), mTid(tid), mPrio(prio) {}
        virtual ~PrioConfigEvent() {}

                pid_t pid() const { return mPid; }
                pid_t tid() const { return mTid; }
                int32_t prio() const { return mPrio; }

        virtual  void dump(char *buffer, size_t size) {
            snprintf(buffer, size, "Prio event: pid %d, tid %d, prio %d\n", mPid, mTid, mPrio);
        }

    private:
        const pid_t mPid;
        const pid_t mTid;
        const int32_t mPrio;
    };


    class PMDeathRecipient : public IBinder::DeathRecipient {
    public:
                    PMDeathRecipient(const wp<ThreadBase>& thread) : mThread(thread) {}
        virtual     ~PMDeathRecipient() {}

        // IBinder::DeathRecipient
        virtual     void        binderDied(const wp<IBinder>& who);

    private:
                    PMDeathRecipient(const PMDeathRecipient&);
                    PMDeathRecipient& operator = (const PMDeathRecipient&);

        wp<ThreadBase> mThread;
    };

    virtual     status_t    initCheck() const = 0;

                // static externally-visible
                type_t      type() const { return mType; }
                audio_io_handle_t id() const { return mId;}

                // dynamic externally-visible
                uint32_t    sampleRate() const { return mSampleRate; }
                uint32_t    channelCount() const { return mChannelCount; }
                audio_channel_mask_t channelMask() const { return mChannelMask; }
                audio_format_t format() const { return mFormat; }
                // Called by AudioFlinger::frameCount(audio_io_handle_t output) and effects,
                // and returns the [normal mix] buffer's frame count.
    virtual     size_t      frameCount() const = 0;
                size_t      frameSize() const { return mFrameSize; }

    // Should be "virtual status_t requestExitAndWait()" and override same
    // method in Thread, but Thread::requestExitAndWait() is not yet virtual.
                void        exit();
    virtual     bool        checkForNewParameters_l() = 0;
    virtual     status_t    setParameters(const String8& keyValuePairs);
    virtual     String8     getParameters(const String8& keys) = 0;
    virtual     void        audioConfigChanged_l(int event, int param = 0) = 0;
                void        sendIoConfigEvent(int event, int param = 0);
                void        sendIoConfigEvent_l(int event, int param = 0);
                void        sendPrioConfigEvent_l(pid_t pid, pid_t tid, int32_t prio);
                void        processConfigEvents();

                // see note at declaration of mStandby, mOutDevice and mInDevice
                bool        standby() const { return mStandby; }
                audio_devices_t outDevice() const { return mOutDevice; }
                audio_devices_t inDevice() const { return mInDevice; }

    virtual     audio_stream_t* stream() const = 0;

                sp<EffectHandle> createEffect_l(
                                    const sp<AudioFlinger::Client>& client,
                                    const sp<IEffectClient>& effectClient,
                                    int32_t priority,
                                    int sessionId,
                                    effect_descriptor_t *desc,
                                    int *enabled,
                                    status_t *status);
                void disconnectEffect(const sp< EffectModule>& effect,
                                      EffectHandle *handle,
                                      bool unpinIfLast);

                // return values for hasAudioSession (bit field)
                enum effect_state {
                    EFFECT_SESSION = 0x1,   // the audio session corresponds to at least one
                                            // effect
                    TRACK_SESSION = 0x2     // the audio session corresponds to at least one
                                            // track
                };

                // get effect chain corresponding to session Id.
                sp<EffectChain> getEffectChain(int sessionId);
                // same as getEffectChain() but must be called with ThreadBase mutex locked
                sp<EffectChain> getEffectChain_l(int sessionId) const;
                // add an effect chain to the chain list (mEffectChains)
    virtual     status_t addEffectChain_l(const sp<EffectChain>& chain) = 0;
                // remove an effect chain from the chain list (mEffectChains)
    virtual     size_t removeEffectChain_l(const sp<EffectChain>& chain) = 0;
                // lock all effect chains Mutexes. Must be called before releasing the
                // ThreadBase mutex before processing the mixer and effects. This guarantees the
                // integrity of the chains during the process.
                // Also sets the parameter 'effectChains' to current value of mEffectChains.
                void lockEffectChains_l(Vector< sp<EffectChain> >& effectChains);
                // unlock effect chains after process
                void unlockEffectChains(const Vector< sp<EffectChain> >& effectChains);
                // get a copy of mEffectChains vector
                Vector< sp<EffectChain> > getEffectChains_l() const { return mEffectChains; };
                // set audio mode to all effect chains
                void setMode(audio_mode_t mode);
                // get effect module with corresponding ID on specified audio session
                sp<AudioFlinger::EffectModule> getEffect(int sessionId, int effectId);
                sp<AudioFlinger::EffectModule> getEffect_l(int sessionId, int effectId);
                // add and effect module. Also creates the effect chain is none exists for
                // the effects audio session
                status_t addEffect_l(const sp< EffectModule>& effect);
                // remove and effect module. Also removes the effect chain is this was the last
                // effect
                void removeEffect_l(const sp< EffectModule>& effect);
                // detach all tracks connected to an auxiliary effect
    virtual     void detachAuxEffect_l(int effectId) {}
                // returns either EFFECT_SESSION if effects on this audio session exist in one
                // chain, or TRACK_SESSION if tracks on this audio session exist, or both
                virtual uint32_t hasAudioSession(int sessionId) const = 0;
                // the value returned by default implementation is not important as the
                // strategy is only meaningful for PlaybackThread which implements this method
                virtual uint32_t getStrategyForSession_l(int sessionId) { return 0; }

                // suspend or restore effect according to the type of effect passed. a NULL
                // type pointer means suspend all effects in the session
                void setEffectSuspended(const effect_uuid_t *type,
                                        bool suspend,
                                        int sessionId = AUDIO_SESSION_OUTPUT_MIX);
                // check if some effects must be suspended/restored when an effect is enabled
                // or disabled
                void checkSuspendOnEffectEnabled(const sp<EffectModule>& effect,
                                                 bool enabled,
                                                 int sessionId = AUDIO_SESSION_OUTPUT_MIX);
                void checkSuspendOnEffectEnabled_l(const sp<EffectModule>& effect,
                                                   bool enabled,
                                                   int sessionId = AUDIO_SESSION_OUTPUT_MIX);

                virtual status_t    setSyncEvent(const sp<SyncEvent>& event) = 0;
                virtual bool        isValidSyncEvent(const sp<SyncEvent>& event) const = 0;


    mutable     Mutex                   mLock;

protected:

                // entry describing an effect being suspended in mSuspendedSessions keyed vector
                class SuspendedSessionDesc : public RefBase {
                public:
                    SuspendedSessionDesc() : mRefCount(0) {}

                    int mRefCount;          // number of active suspend requests
                    effect_uuid_t mType;    // effect type UUID
                };

                void        acquireWakeLock(int uid = -1);
                void        acquireWakeLock_l(int uid = -1);
                void        releaseWakeLock();
                void        releaseWakeLock_l();
                void        updateWakeLockUids(const SortedVector<int> &uids);
                void        updateWakeLockUids_l(const SortedVector<int> &uids);
                void        getPowerManager_l();
                void setEffectSuspended_l(const effect_uuid_t *type,
                                          bool suspend,
                                          int sessionId);
                // updated mSuspendedSessions when an effect suspended or restored
                void        updateSuspendedSessions_l(const effect_uuid_t *type,
                                                      bool suspend,
                                                      int sessionId);
                // check if some effects must be suspended when an effect chain is added
                void checkSuspendOnAddEffectChain_l(const sp<EffectChain>& chain);

                String16 getWakeLockTag();

    virtual     void        preExit() { }

    friend class AudioFlinger;      // for mEffectChains

                const type_t            mType;

                // Used by parameters, config events, addTrack_l, exit
                Condition               mWaitWorkCV;

                const sp<AudioFlinger>  mAudioFlinger;

                // updated by PlaybackThread::readOutputParameters() or
                // RecordThread::readInputParameters()
                uint32_t                mSampleRate;
                size_t                  mFrameCount;       // output HAL, direct output, record
                audio_channel_mask_t    mChannelMask;
                uint32_t                mChannelCount;
                size_t                  mFrameSize;
                audio_format_t          mFormat;

                // Parameter sequence by client: binder thread calling setParameters():
                //  1. Lock mLock
                //  2. Append to mNewParameters
                //  3. mWaitWorkCV.signal
                //  4. mParamCond.waitRelative with timeout
                //  5. read mParamStatus
                //  6. mWaitWorkCV.signal
                //  7. Unlock
                //
                // Parameter sequence by server: threadLoop calling checkForNewParameters_l():
                // 1. Lock mLock
                // 2. If there is an entry in mNewParameters proceed ...
                // 2. Read first entry in mNewParameters
                // 3. Process
                // 4. Remove first entry from mNewParameters
                // 5. Set mParamStatus
                // 6. mParamCond.signal
                // 7. mWaitWorkCV.wait with timeout (this is to avoid overwriting mParamStatus)
                // 8. Unlock
                Condition               mParamCond;
                Vector<String8>         mNewParameters;
                status_t                mParamStatus;

                // vector owns each ConfigEvent *, so must delete after removing
                Vector<ConfigEvent *>     mConfigEvents;

                // These fields are written and read by thread itself without lock or barrier,
                // and read by other threads without lock or barrier via standby() , outDevice()
                // and inDevice().
                // Because of the absence of a lock or barrier, any other thread that reads
                // these fields must use the information in isolation, or be prepared to deal
                // with possibility that it might be inconsistent with other information.
                bool                    mStandby;   // Whether thread is currently in standby.
                audio_devices_t         mOutDevice;   // output device
                audio_devices_t         mInDevice;    // input device
                audio_source_t          mAudioSource; // (see audio.h, audio_source_t)

                const audio_io_handle_t mId;
                Vector< sp<EffectChain> > mEffectChains;

                static const int        kNameLength = 16;   // prctl(PR_SET_NAME) limit
                char                    mName[kNameLength];
                sp<IPowerManager>       mPowerManager;
                sp<IBinder>             mWakeLockToken;
                const sp<PMDeathRecipient> mDeathRecipient;
                // list of suspended effects per session and per type. The first vector is
                // keyed by session ID, the second by type UUID timeLow field
                KeyedVector< int, KeyedVector< int, sp<SuspendedSessionDesc> > >
                                        mSuspendedSessions;
                static const size_t     kLogSize = 4 * 1024;
                sp<NBLog::Writer>       mNBLogWriter;
};

// --- PlaybackThread ---
class PlaybackThread : public ThreadBase {
public:

#include "PlaybackTracks.h"

    enum mixer_state {
        MIXER_IDLE,             // no active tracks
        MIXER_TRACKS_ENABLED,   // at least one active track, but no track has any data ready
        MIXER_TRACKS_READY,      // at least one active track, and at least one track has data
        MIXER_DRAIN_TRACK,      // drain currently playing track
        MIXER_DRAIN_ALL,        // fully drain the hardware
        // standby mode does not have an enum value
        // suspend by audio policy manager is orthogonal to mixer state
    };

    // retry count before removing active track in case of underrun on offloaded thread:
    // we need to make sure that AudioTrack client has enough time to send large buffers
//FIXME may be more appropriate if expressed in time units. Need to revise how underrun is handled
    // for offloaded tracks
    static const int8_t kMaxTrackRetriesOffload = 20;

    PlaybackThread(const sp<AudioFlinger>& audioFlinger, AudioStreamOut* output,
                   audio_io_handle_t id, audio_devices_t device, type_t type);
    virtual             ~PlaybackThread();

                void        dump(int fd, const Vector<String16>& args);

    // Thread virtuals
    virtual     status_t    readyToRun();
    virtual     bool        threadLoop();

    // RefBase
    virtual     void        onFirstRef();

protected:
    // Code snippets that were lifted up out of threadLoop()
    virtual     void        threadLoop_mix() = 0;
    virtual     void        threadLoop_sleepTime() = 0;
    virtual     ssize_t     threadLoop_write();
    virtual     void        threadLoop_drain();
    virtual     void        threadLoop_standby();
    virtual     void        threadLoop_exit();
    virtual     void        threadLoop_removeTracks(const Vector< sp<Track> >& tracksToRemove);

                // prepareTracks_l reads and writes mActiveTracks, and returns
                // the pending set of tracks to remove via Vector 'tracksToRemove'.  The caller
                // is responsible for clearing or destroying this Vector later on, when it
                // is safe to do so. That will drop the final ref count and destroy the tracks.
    virtual     mixer_state prepareTracks_l(Vector< sp<Track> > *tracksToRemove) = 0;
                void        removeTracks_l(const Vector< sp<Track> >& tracksToRemove);

                void        writeCallback();
                void        resetWriteBlocked(uint32_t sequence);
                void        drainCallback();
                void        resetDraining(uint32_t sequence);

    static      int         asyncCallback(stream_callback_event_t event, void *param, void *cookie);

    virtual     bool        waitingAsyncCallback();
    virtual     bool        waitingAsyncCallback_l();
    virtual     bool        shouldStandby_l();


    // ThreadBase virtuals
    virtual     void        preExit();

public:

    virtual     status_t    initCheck() const { return (mOutput == NULL) ? NO_INIT : NO_ERROR; }

                // return estimated latency in milliseconds, as reported by HAL
                uint32_t    latency() const;
                // same, but lock must already be held
                uint32_t    latency_l() const;

                void        setMasterVolume(float value);
                void        setMasterMute(bool muted);

                void        setStreamVolume(audio_stream_type_t stream, float value);
                void        setStreamMute(audio_stream_type_t stream, bool muted);

                float       streamVolume(audio_stream_type_t stream) const;

                sp<Track>   createTrack_l(
                                const sp<AudioFlinger::Client>& client,
                                audio_stream_type_t streamType,
                                uint32_t sampleRate,
                                audio_format_t format,
                                audio_channel_mask_t channelMask,
                                size_t frameCount,
                                const sp<IMemory>& sharedBuffer,
                                int sessionId,
                                IAudioFlinger::track_flags_t *flags,
                                pid_t tid,
                                int uid,
                                status_t *status);

                AudioStreamOut* getOutput() const;
                AudioStreamOut* clearOutput();
                virtual audio_stream_t* stream() const;

                // a very large number of suspend() will eventually wraparound, but unlikely
                void        suspend() { (void) android_atomic_inc(&mSuspended); }
                void        restore()
                                {
                                    // if restore() is done without suspend(), get back into
                                    // range so that the next suspend() will operate correctly
                                    if (android_atomic_dec(&mSuspended) <= 0) {
                                        android_atomic_release_store(0, &mSuspended);
                                    }
                                }
                bool        isSuspended() const
                                { return android_atomic_acquire_load(&mSuspended) > 0; }

    virtual     String8     getParameters(const String8& keys);
    virtual     void        audioConfigChanged_l(int event, int param = 0);
                status_t    getRenderPosition(size_t *halFrames, size_t *dspFrames);
                int16_t     *mixBuffer() const { return mMixBuffer; };

    virtual     void detachAuxEffect_l(int effectId);
                status_t attachAuxEffect(const sp<AudioFlinger::PlaybackThread::Track> track,
                        int EffectId);
                status_t attachAuxEffect_l(const sp<AudioFlinger::PlaybackThread::Track> track,
                        int EffectId);

                virtual status_t addEffectChain_l(const sp<EffectChain>& chain);
                virtual size_t removeEffectChain_l(const sp<EffectChain>& chain);
                virtual uint32_t hasAudioSession(int sessionId) const;
                virtual uint32_t getStrategyForSession_l(int sessionId);


                virtual status_t setSyncEvent(const sp<SyncEvent>& event);
                virtual bool     isValidSyncEvent(const sp<SyncEvent>& event) const;

                // called with AudioFlinger lock held
                        void     invalidateTracks(audio_stream_type_t streamType);

    virtual     size_t      frameCount() const { return mNormalFrameCount; }

                // Return's the HAL's frame count i.e. fast mixer buffer size.
                size_t      frameCountHAL() const { return mFrameCount; }

                status_t         getTimestamp_l(AudioTimestamp& timestamp);

protected:
    // updated by readOutputParameters()
    size_t                          mNormalFrameCount;  // normal mixer and effects

    int16_t*                        mMixBuffer;         // frame size aligned mix buffer
    int8_t*                         mAllocMixBuffer;    // mixer buffer allocation address

    // suspend count, > 0 means suspended.  While suspended, the thread continues to pull from
    // tracks and mix, but doesn't write to HAL.  A2DP and SCO HAL implementations can't handle
    // concurrent use of both of them, so Audio Policy Service suspends one of the threads to
    // workaround that restriction.
    // 'volatile' means accessed via atomic operations and no lock.
    volatile int32_t                mSuspended;

    // FIXME overflows every 6+ hours at 44.1 kHz stereo 16-bit samples
    // mFramesWritten would be better, or 64-bit even better
    size_t                          mBytesWritten;
private:
    // mMasterMute is in both PlaybackThread and in AudioFlinger.  When a
    // PlaybackThread needs to find out if master-muted, it checks it's local
    // copy rather than the one in AudioFlinger.  This optimization saves a lock.
    bool                            mMasterMute;
                void        setMasterMute_l(bool muted) { mMasterMute = muted; }
protected:
    SortedVector< wp<Track> >       mActiveTracks;  // FIXME check if this could be sp<>
    SortedVector<int>               mWakeLockUids;
    int                             mActiveTracksGeneration;
    wp<Track>                       mLatestActiveTrack; // latest track added to mActiveTracks

    // Allocate a track name for a given channel mask.
    //   Returns name >= 0 if successful, -1 on failure.
    virtual int             getTrackName_l(audio_channel_mask_t channelMask, int sessionId) = 0;
    virtual void            deleteTrackName_l(int name) = 0;

    // Time to sleep between cycles when:
    virtual uint32_t        activeSleepTimeUs() const;      // mixer state MIXER_TRACKS_ENABLED
    virtual uint32_t        idleSleepTimeUs() const = 0;    // mixer state MIXER_IDLE
    virtual uint32_t        suspendSleepTimeUs() const = 0; // audio policy manager suspended us
    // No sleep when mixer state == MIXER_TRACKS_READY; relies on audio HAL stream->write()
    // No sleep in standby mode; waits on a condition

    // Code snippets that are temporarily lifted up out of threadLoop() until the merge
                void        checkSilentMode_l();

    // Non-trivial for DUPLICATING only
    virtual     void        saveOutputTracks() { }
    virtual     void        clearOutputTracks() { }

    // Cache various calculated values, at threadLoop() entry and after a parameter change
    virtual     void        cacheParameters_l();

    virtual     uint32_t    correctLatency_l(uint32_t latency) const;

private:

    friend class AudioFlinger;      // for numerous

    PlaybackThread(const Client&);
    PlaybackThread& operator = (const PlaybackThread&);

    status_t    addTrack_l(const sp<Track>& track);
    bool        destroyTrack_l(const sp<Track>& track);
    void        removeTrack_l(const sp<Track>& track);
    void        broadcast_l();

    void        readOutputParameters();

    virtual void dumpInternals(int fd, const Vector<String16>& args);
    void        dumpTracks(int fd, const Vector<String16>& args);

    SortedVector< sp<Track> >       mTracks;
    // mStreamTypes[] uses 1 additional stream type internally for the OutputTrack used by
    // DuplicatingThread
    stream_type_t                   mStreamTypes[AUDIO_STREAM_CNT + 1];
    AudioStreamOut                  *mOutput;

    float                           mMasterVolume;
    nsecs_t                         mLastWriteTime;
    int                             mNumWrites;
    int                             mNumDelayedWrites;
    bool                            mInWrite;

    // FIXME rename these former local variables of threadLoop to standard "m" names
    nsecs_t                         standbyTime;
    size_t                          mixBufferSize;

    // cached copies of activeSleepTimeUs() and idleSleepTimeUs() made by cacheParameters_l()
    uint32_t                        activeSleepTime;
    uint32_t                        idleSleepTime;

    uint32_t                        sleepTime;

    // mixer status returned by prepareTracks_l()
    mixer_state                     mMixerStatus; // current cycle
                                                  // previous cycle when in prepareTracks_l()
    mixer_state                     mMixerStatusIgnoringFastTracks;
                                                  // FIXME or a separate ready state per track

    // FIXME move these declarations into the specific sub-class that needs them
    // MIXER only
    uint32_t                        sleepTimeShift;

    // same as AudioFlinger::mStandbyTimeInNsecs except for DIRECT which uses a shorter value
    nsecs_t                         standbyDelay;

    // MIXER only
    nsecs_t                         maxPeriod;

    // DUPLICATING only
    uint32_t                        writeFrames;

    size_t                          mBytesRemaining;
    size_t                          mCurrentWriteLength;
    bool                            mUseAsyncWrite;
    // mWriteAckSequence contains current write sequence on bits 31-1. The write sequence is
    // incremented each time a write(), a flush() or a standby() occurs.
    // Bit 0 is set when a write blocks and indicates a callback is expected.
    // Bit 0 is reset by the async callback thread calling resetWriteBlocked(). Out of sequence
    // callbacks are ignored.
    uint32_t                        mWriteAckSequence;
    // mDrainSequence contains current drain sequence on bits 31-1. The drain sequence is
    // incremented each time a drain is requested or a flush() or standby() occurs.
    // Bit 0 is set when the drain() command is called at the HAL and indicates a callback is
    // expected.
    // Bit 0 is reset by the async callback thread calling resetDraining(). Out of sequence
    // callbacks are ignored.
    uint32_t                        mDrainSequence;
    // A condition that must be evaluated by prepareTrack_l() has changed and we must not wait
    // for async write callback in the thread loop before evaluating it
    bool                            mSignalPending;
    sp<AsyncCallbackThread>         mCallbackThread;

private:
    // The HAL output sink is treated as non-blocking, but current implementation is blocking
    sp<NBAIO_Sink>          mOutputSink;
    // If a fast mixer is present, the blocking pipe sink, otherwise clear
    sp<NBAIO_Sink>          mPipeSink;
    // The current sink for the normal mixer to write it's (sub)mix, mOutputSink or mPipeSink
    sp<NBAIO_Sink>          mNormalSink;
#ifdef TEE_SINK
    // For dumpsys
    sp<NBAIO_Sink>          mTeeSink;
    sp<NBAIO_Source>        mTeeSource;
#endif
    uint32_t                mScreenState;   // cached copy of gScreenState
    static const size_t     kFastMixerLogSize = 4 * 1024;
    sp<NBLog::Writer>       mFastMixerNBLogWriter;
public:
    virtual     bool        hasFastMixer() const = 0;
    virtual     FastTrackUnderruns getFastTrackUnderruns(size_t fastIndex) const
                                { FastTrackUnderruns dummy; return dummy; }

protected:
                // accessed by both binder threads and within threadLoop(), lock on mutex needed
                unsigned    mFastTrackAvailMask;    // bit i set if fast track [i] is available
    virtual     void        flushOutput_l();

private:
    // timestamp latch:
    //  D input is written by threadLoop_write while mutex is unlocked, and read while locked
    //  Q output is written while locked, and read while locked
    struct {
        AudioTimestamp  mTimestamp;
        uint32_t        mUnpresentedFrames;
    } mLatchD, mLatchQ;
    bool mLatchDValid;  // true means mLatchD is valid, and clock it into latch at next opportunity
    bool mLatchQValid;  // true means mLatchQ is valid
};

class MixerThread : public PlaybackThread {
public:
    MixerThread(const sp<AudioFlinger>& audioFlinger,
                AudioStreamOut* output,
                audio_io_handle_t id,
                audio_devices_t device,
                type_t type = MIXER);
    virtual             ~MixerThread();

    // Thread virtuals

    virtual     bool        checkForNewParameters_l();
    virtual     void        dumpInternals(int fd, const Vector<String16>& args);

protected:
    virtual     mixer_state prepareTracks_l(Vector< sp<Track> > *tracksToRemove);
    virtual     int         getTrackName_l(audio_channel_mask_t channelMask, int sessionId);
    virtual     void        deleteTrackName_l(int name);
    virtual     uint32_t    idleSleepTimeUs() const;
    virtual     uint32_t    suspendSleepTimeUs() const;
    virtual     void        cacheParameters_l();

    // threadLoop snippets
    virtual     ssize_t     threadLoop_write();
    virtual     void        threadLoop_standby();
    virtual     void        threadLoop_mix();
    virtual     void        threadLoop_sleepTime();
    virtual     void        threadLoop_removeTracks(const Vector< sp<Track> >& tracksToRemove);
    virtual     uint32_t    correctLatency_l(uint32_t latency) const;

                AudioMixer* mAudioMixer;    // normal mixer
private:
                // one-time initialization, no locks required
                FastMixer*  mFastMixer;         // non-NULL if there is also a fast mixer
                sp<AudioWatchdog> mAudioWatchdog; // non-0 if there is an audio watchdog thread

                // contents are not guaranteed to be consistent, no locks required
                FastMixerDumpState mFastMixerDumpState;
#ifdef STATE_QUEUE_DUMP
                StateQueueObserverDump mStateQueueObserverDump;
                StateQueueMutatorDump  mStateQueueMutatorDump;
#endif
                AudioWatchdogDump mAudioWatchdogDump;

                // accessible only within the threadLoop(), no locks required
                //          mFastMixer->sq()    // for mutating and pushing state
                int32_t     mFastMixerFutex;    // for cold idle

public:
    virtual     bool        hasFastMixer() const { return mFastMixer != NULL; }
    virtual     FastTrackUnderruns getFastTrackUnderruns(size_t fastIndex) const {
                              ALOG_ASSERT(fastIndex < FastMixerState::kMaxFastTracks);
                              return mFastMixerDumpState.mTracks[fastIndex].mUnderruns;
                            }
};

class DirectOutputThread : public PlaybackThread {
public:

    DirectOutputThread(const sp<AudioFlinger>& audioFlinger, AudioStreamOut* output,
                       audio_io_handle_t id, audio_devices_t device);
    virtual                 ~DirectOutputThread();

    // Thread virtuals

    virtual     bool        checkForNewParameters_l();

protected:
    virtual     int         getTrackName_l(audio_channel_mask_t channelMask, int sessionId);
    virtual     void        deleteTrackName_l(int name);
    virtual     uint32_t    activeSleepTimeUs() const;
    virtual     uint32_t    idleSleepTimeUs() const;
    virtual     uint32_t    suspendSleepTimeUs() const;
    virtual     void        cacheParameters_l();

    // threadLoop snippets
    virtual     mixer_state prepareTracks_l(Vector< sp<Track> > *tracksToRemove);
    virtual     void        threadLoop_mix();
    virtual     void        threadLoop_sleepTime();

    // volumes last sent to audio HAL with stream->set_volume()
    float mLeftVolFloat;
    float mRightVolFloat;

    DirectOutputThread(const sp<AudioFlinger>& audioFlinger, AudioStreamOut* output,
                        audio_io_handle_t id, uint32_t device, ThreadBase::type_t type);
    void processVolume_l(Track *track, bool lastTrack);

    // prepareTracks_l() tells threadLoop_mix() the name of the single active track
    sp<Track>               mActiveTrack;
public:
    virtual     bool        hasFastMixer() const { return false; }
};

class OffloadThread : public DirectOutputThread {
public:

    OffloadThread(const sp<AudioFlinger>& audioFlinger, AudioStreamOut* output,
                        audio_io_handle_t id, uint32_t device);
    virtual                 ~OffloadThread() {};

protected:
    // threadLoop snippets
    virtual     mixer_state prepareTracks_l(Vector< sp<Track> > *tracksToRemove);
    virtual     void        threadLoop_exit();
    virtual     void        flushOutput_l();

    virtual     bool        waitingAsyncCallback();
    virtual     bool        waitingAsyncCallback_l();
    virtual     bool        shouldStandby_l();

private:
                void        flushHw_l();

private:
    bool        mHwPaused;
    bool        mFlushPending;
    size_t      mPausedWriteLength;     // length in bytes of write interrupted by pause
    size_t      mPausedBytesRemaining;  // bytes still waiting in mixbuffer after resume
    wp<Track>   mPreviousTrack;         // used to detect track switch
};

class AsyncCallbackThread : public Thread {
public:

    AsyncCallbackThread(const wp<PlaybackThread>& playbackThread);

    virtual             ~AsyncCallbackThread();

    // Thread virtuals
    virtual bool        threadLoop();

    // RefBase
    virtual void        onFirstRef();

            void        exit();
            void        setWriteBlocked(uint32_t sequence);
            void        resetWriteBlocked();
            void        setDraining(uint32_t sequence);
            void        resetDraining();

private:
    const wp<PlaybackThread>   mPlaybackThread;
    // mWriteAckSequence corresponds to the last write sequence passed by the offload thread via
    // setWriteBlocked(). The sequence is shifted one bit to the left and the lsb is used
    // to indicate that the callback has been received via resetWriteBlocked()
    uint32_t                   mWriteAckSequence;
    // mDrainSequence corresponds to the last drain sequence passed by the offload thread via
    // setDraining(). The sequence is shifted one bit to the left and the lsb is used
    // to indicate that the callback has been received via resetDraining()
    uint32_t                   mDrainSequence;
    Condition                  mWaitWorkCV;
    Mutex                      mLock;
};

class DuplicatingThread : public MixerThread {
public:
    DuplicatingThread(const sp<AudioFlinger>& audioFlinger, MixerThread* mainThread,
                      audio_io_handle_t id);
    virtual                 ~DuplicatingThread();

    // Thread virtuals
                void        addOutputTrack(MixerThread* thread);
                void        removeOutputTrack(MixerThread* thread);
                uint32_t    waitTimeMs() const { return mWaitTimeMs; }
protected:
    virtual     uint32_t    activeSleepTimeUs() const;

private:
                bool        outputsReady(const SortedVector< sp<OutputTrack> > &outputTracks);
protected:
    // threadLoop snippets
    virtual     void        threadLoop_mix();
    virtual     void        threadLoop_sleepTime();
    virtual     ssize_t     threadLoop_write();
    virtual     void        threadLoop_standby();
    virtual     void        cacheParameters_l();

private:
    // called from threadLoop, addOutputTrack, removeOutputTrack
    virtual     void        updateWaitTime_l();
protected:
    virtual     void        saveOutputTracks();
    virtual     void        clearOutputTracks();
private:

                uint32_t    mWaitTimeMs;
    SortedVector < sp<OutputTrack> >  outputTracks;
    SortedVector < sp<OutputTrack> >  mOutputTracks;
public:
    virtual     bool        hasFastMixer() const { return false; }
};


// record thread
class RecordThread : public ThreadBase, public AudioBufferProvider
                        // derives from AudioBufferProvider interface for use by resampler
{
public:

#include "RecordTracks.h"

            RecordThread(const sp<AudioFlinger>& audioFlinger,
                    AudioStreamIn *input,
                    uint32_t sampleRate,
                    audio_channel_mask_t channelMask,
                    audio_io_handle_t id,
                    audio_devices_t outDevice,
                    audio_devices_t inDevice
#ifdef TEE_SINK
                    , const sp<NBAIO_Sink>& teeSink
#endif
                    );
            virtual     ~RecordThread();

    // no addTrack_l ?
    void        destroyTrack_l(const sp<RecordTrack>& track);
    void        removeTrack_l(const sp<RecordTrack>& track);

    void        dumpInternals(int fd, const Vector<String16>& args);
    void        dumpTracks(int fd, const Vector<String16>& args);

    // Thread virtuals
    virtual bool        threadLoop();
    virtual status_t    readyToRun();

    // RefBase
    virtual void        onFirstRef();

    virtual status_t    initCheck() const { return (mInput == NULL) ? NO_INIT : NO_ERROR; }
            sp<AudioFlinger::RecordThread::RecordTrack>  createRecordTrack_l(
                    const sp<AudioFlinger::Client>& client,
                    uint32_t sampleRate,
                    audio_format_t format,
                    audio_channel_mask_t channelMask,
                    size_t frameCount,
                    int sessionId,
                    int uid,
                    IAudioFlinger::track_flags_t *flags,
                    pid_t tid,
                    status_t *status);

            status_t    start(RecordTrack* recordTrack,
                              AudioSystem::sync_event_t event,
                              int triggerSession);

            // ask the thread to stop the specified track, and
            // return true if the caller should then do it's part of the stopping process
            bool        stop(RecordTrack* recordTrack);

            void        dump(int fd, const Vector<String16>& args);
            AudioStreamIn* clearInput();
            virtual audio_stream_t* stream() const;

    // AudioBufferProvider interface
    virtual status_t    getNextBuffer(AudioBufferProvider::Buffer* buffer, int64_t pts);
    virtual void        releaseBuffer(AudioBufferProvider::Buffer* buffer);

    virtual bool        checkForNewParameters_l();
    virtual String8     getParameters(const String8& keys);
    virtual void        audioConfigChanged_l(int event, int param = 0);
            void        readInputParameters();
    virtual unsigned int  getInputFramesLost();

    virtual status_t addEffectChain_l(const sp<EffectChain>& chain);
    virtual size_t removeEffectChain_l(const sp<EffectChain>& chain);
    virtual uint32_t hasAudioSession(int sessionId) const;

            // Return the set of unique session IDs across all tracks.
            // The keys are the session IDs, and the associated values are meaningless.
            // FIXME replace by Set [and implement Bag/Multiset for other uses].
            KeyedVector<int, bool> sessionIds() const;

    virtual status_t setSyncEvent(const sp<SyncEvent>& event);
    virtual bool     isValidSyncEvent(const sp<SyncEvent>& event) const;

    static void syncStartEventCallback(const wp<SyncEvent>& event);
           void handleSyncStartEvent(const sp<SyncEvent>& event);

    virtual size_t      frameCount() const { return mFrameCount; }
            bool        hasFastRecorder() const { return false; }

private:
            void clearSyncStartEvent();

            // Enter standby if not already in standby, and set mStandby flag
            void standby();

            // Call the HAL standby method unconditionally, and don't change mStandby flag
            void inputStandBy();

            AudioStreamIn                       *mInput;
            SortedVector < sp<RecordTrack> >    mTracks;
            // mActiveTrack has dual roles:  it indicates the current active track, and
            // is used together with mStartStopCond to indicate start()/stop() progress
            sp<RecordTrack>                     mActiveTrack;
            Condition                           mStartStopCond;

            // updated by RecordThread::readInputParameters()
            AudioResampler                      *mResampler;
            // interleaved stereo pairs of fixed-point signed Q19.12
            int32_t                             *mRsmpOutBuffer;
            int16_t                             *mRsmpInBuffer; // [mFrameCount * mChannelCount]
            size_t                              mRsmpInIndex;
            size_t                              mBufferSize;    // stream buffer size for read()
            const uint32_t                      mReqChannelCount;
            const uint32_t                      mReqSampleRate;
            ssize_t                             mBytesRead;
            // sync event triggering actual audio capture. Frames read before this event will
            // be dropped and therefore not read by the application.
            sp<SyncEvent>                       mSyncStartEvent;
            // number of captured frames to drop after the start sync event has been received.
            // when < 0, maximum frames to drop before starting capture even if sync event is
            // not received
            ssize_t                             mFramestoDrop;

            // For dumpsys
            const sp<NBAIO_Sink>                mTeeSink;
};
