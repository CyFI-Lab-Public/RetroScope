/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef ANDROID_AUDIO_TRACK_SHARED_H
#define ANDROID_AUDIO_TRACK_SHARED_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/threads.h>
#include <utils/Log.h>
#include <utils/RefBase.h>
#include <media/nbaio/roundup.h>
#include <media/SingleStateQueue.h>
#include <private/media/StaticAudioTrackState.h>

namespace android {

// ----------------------------------------------------------------------------

// for audio_track_cblk_t::mFlags
#define CBLK_UNDERRUN   0x01 // set by server immediately on output underrun, cleared by client
#define CBLK_FORCEREADY 0x02 // set: track is considered ready immediately by AudioFlinger,
                             // clear: track is ready when buffer full
#define CBLK_INVALID    0x04 // track buffer invalidated by AudioFlinger, need to re-create
#define CBLK_DISABLED   0x08 // output track disabled by AudioFlinger due to underrun,
                             // need to re-start.  Unlike CBLK_UNDERRUN, this is not set
                             // immediately, but only after a long string of underruns.
// 0x10 unused
#define CBLK_LOOP_CYCLE 0x20 // set by server each time a loop cycle other than final one completes
#define CBLK_LOOP_FINAL 0x40 // set by server when the final loop cycle completes
#define CBLK_BUFFER_END 0x80 // set by server when the position reaches end of buffer if not looping
#define CBLK_OVERRUN   0x100 // set by server immediately on input overrun, cleared by client
#define CBLK_INTERRUPT 0x200 // set by client on interrupt(), cleared by client in obtainBuffer()
#define CBLK_STREAM_END_DONE 0x400 // set by server on render completion, cleared by client

//EL_FIXME 20 seconds may not be enough and must be reconciled with new obtainBuffer implementation
#define MAX_RUN_OFFLOADED_TIMEOUT_MS 20000 //assuming upto a maximum of 20 seconds of offloaded

struct AudioTrackSharedStreaming {
    // similar to NBAIO MonoPipe
    // in continuously incrementing frame units, take modulo buffer size, which must be a power of 2
    volatile int32_t mFront;    // read by server
    volatile int32_t mRear;     // write by client
    volatile int32_t mFlush;    // incremented by client to indicate a request to flush;
                                // server notices and discards all data between mFront and mRear
    volatile uint32_t mUnderrunFrames;  // server increments for each unavailable but desired frame
};

typedef SingleStateQueue<StaticAudioTrackState> StaticAudioTrackSingleStateQueue;

struct AudioTrackSharedStatic {
    StaticAudioTrackSingleStateQueue::Shared
                    mSingleStateQueue;
    size_t          mBufferPosition;    // updated asynchronously by server,
                                        // "for entertainment purposes only"
};

// ----------------------------------------------------------------------------

// Important: do not add any virtual methods, including ~
struct audio_track_cblk_t
{
                // Since the control block is always located in shared memory, this constructor
                // is only used for placement new().  It is never used for regular new() or stack.
                            audio_track_cblk_t();
                /*virtual*/ ~audio_track_cblk_t() { }

                friend class Proxy;
                friend class ClientProxy;
                friend class AudioTrackClientProxy;
                friend class AudioRecordClientProxy;
                friend class ServerProxy;
                friend class AudioTrackServerProxy;
                friend class AudioRecordServerProxy;

    // The data members are grouped so that members accessed frequently and in the same context
    // are in the same line of data cache.

                uint32_t    mServer;    // Number of filled frames consumed by server (mIsOut),
                                        // or filled frames provided by server (!mIsOut).
                                        // It is updated asynchronously by server without a barrier.
                                        // The value should be used "for entertainment purposes only",
                                        // which means don't make important decisions based on it.

                size_t      frameCount_;    // used during creation to pass actual track buffer size
                                            // from AudioFlinger to client, and not referenced again
                                            // FIXME remove here and replace by createTrack() in/out
                                            // parameter
                                            // renamed to "_" to detect incorrect use

    volatile    int32_t     mFutex;     // event flag: down (P) by client,
                                        // up (V) by server or binderDied() or interrupt()
#define CBLK_FUTEX_WAKE 1               // if event flag bit is set, then a deferred wake is pending

private:

                size_t      mMinimum;       // server wakes up client if available >= mMinimum

                // Channel volumes are fixed point U4.12, so 0x1000 means 1.0.
                // Left channel is in [0:15], right channel is in [16:31].
                // Always read and write the combined pair atomically.
                // For AudioTrack only, not used by AudioRecord.
                uint32_t    mVolumeLR;

                uint32_t    mSampleRate;    // AudioTrack only: client's requested sample rate in Hz
                                            // or 0 == default. Write-only client, read-only server.

                // client write-only, server read-only
                uint16_t    mSendLevel;      // Fixed point U4.12 so 0x1000 means 1.0

                uint16_t    mPad2;           // unused

public:

    volatile    int32_t     mFlags;         // combinations of CBLK_*

                // Cache line boundary (32 bytes)

public:
                union {
                    AudioTrackSharedStreaming   mStreaming;
                    AudioTrackSharedStatic      mStatic;
                    int                         mAlign[8];
                } u;

                // Cache line boundary (32 bytes)
};

// ----------------------------------------------------------------------------

// Proxy for shared memory control block, to isolate callers from needing to know the details.
// There is exactly one ClientProxy and one ServerProxy per shared memory control block.
// The proxies are located in normal memory, and are not multi-thread safe within a given side.
class Proxy : public RefBase {
protected:
    Proxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount, size_t frameSize, bool isOut,
            bool clientInServer);
    virtual ~Proxy() { }

public:
    struct Buffer {
        size_t  mFrameCount;            // number of frames available in this buffer
        void*   mRaw;                   // pointer to first frame
        size_t  mNonContig;             // number of additional non-contiguous frames available
    };

protected:
    // These refer to shared memory, and are virtual addresses with respect to the current process.
    // They may have different virtual addresses within the other process.
    audio_track_cblk_t* const   mCblk;  // the control block
    void* const     mBuffers;           // starting address of buffers

    const size_t    mFrameCount;        // not necessarily a power of 2
    const size_t    mFrameSize;         // in bytes
    const size_t    mFrameCountP2;      // mFrameCount rounded to power of 2, streaming mode
    const bool      mIsOut;             // true for AudioTrack, false for AudioRecord
    const bool      mClientInServer;    // true for OutputTrack, false for AudioTrack & AudioRecord
    bool            mIsShutdown;        // latch set to true when shared memory corruption detected
    size_t          mUnreleased;        // unreleased frames remaining from most recent obtainBuffer
};

// ----------------------------------------------------------------------------

// Proxy seen by AudioTrack client and AudioRecord client
class ClientProxy : public Proxy {
protected:
    ClientProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount, size_t frameSize,
            bool isOut, bool clientInServer);
    virtual ~ClientProxy() { }

public:
    static const struct timespec kForever;
    static const struct timespec kNonBlocking;

    // Obtain a buffer with filled frames (reading) or empty frames (writing).
    // It is permitted to call obtainBuffer() multiple times in succession, without any intervening
    // calls to releaseBuffer().  In that case, the final obtainBuffer() is the one that effectively
    // sets or extends the unreleased frame count.
    // On entry:
    //  buffer->mFrameCount should be initialized to maximum number of desired frames,
    //      which must be > 0.
    //  buffer->mNonContig is unused.
    //  buffer->mRaw is unused.
    //  requested is the requested timeout in local monotonic delta time units:
    //      NULL or &kNonBlocking means non-blocking (zero timeout).
    //      &kForever means block forever (infinite timeout).
    //      Other values mean a specific timeout in local monotonic delta time units.
    //  elapsed is a pointer to a location that will hold the total local monotonic time that
    //      elapsed while blocked, or NULL if not needed.
    // On exit:
    //  buffer->mFrameCount has the actual number of contiguous available frames,
    //      which is always 0 when the return status != NO_ERROR.
    //  buffer->mNonContig is the number of additional non-contiguous available frames.
    //  buffer->mRaw is a pointer to the first available frame,
    //      or NULL when buffer->mFrameCount == 0.
    // The return status is one of:
    //  NO_ERROR    Success, buffer->mFrameCount > 0.
    //  WOULD_BLOCK Non-blocking mode and no frames are available.
    //  TIMED_OUT   Timeout occurred before any frames became available.
    //              This can happen even for infinite timeout, due to a spurious wakeup.
    //              In this case, the caller should investigate and then re-try as appropriate.
    //  DEAD_OBJECT Server has died or invalidated, caller should destroy this proxy and re-create.
    //  -EINTR      Call has been interrupted.  Look around to see why, and then perhaps try again.
    //  NO_INIT     Shared memory is corrupt.
    // Assertion failure on entry, if buffer == NULL or buffer->mFrameCount == 0.
    status_t    obtainBuffer(Buffer* buffer, const struct timespec *requested = NULL,
            struct timespec *elapsed = NULL);

    // Release (some of) the frames last obtained.
    // On entry, buffer->mFrameCount should have the number of frames to release,
    // which must (cumulatively) be <= the number of frames last obtained but not yet released.
    // buffer->mRaw is ignored, but is normally same pointer returned by last obtainBuffer().
    // It is permitted to call releaseBuffer() multiple times to release the frames in chunks.
    // On exit:
    //  buffer->mFrameCount is zero.
    //  buffer->mRaw is NULL.
    void        releaseBuffer(Buffer* buffer);

    // Call after detecting server's death
    void        binderDied();

    // Call to force an obtainBuffer() to return quickly with -EINTR
    void        interrupt();

    size_t      getPosition() {
        return mEpoch + mCblk->mServer;
    }

    void        setEpoch(size_t epoch) {
        mEpoch = epoch;
    }

    void        setMinimum(size_t minimum) {
        mCblk->mMinimum = minimum;
    }

    // Return the number of frames that would need to be obtained and released
    // in order for the client to be aligned at start of buffer
    virtual size_t  getMisalignment();

    size_t      getEpoch() const {
        return mEpoch;
    }

    size_t      getFramesFilled();

private:
    size_t      mEpoch;
};

// ----------------------------------------------------------------------------

// Proxy used by AudioTrack client, which also includes AudioFlinger::PlaybackThread::OutputTrack
class AudioTrackClientProxy : public ClientProxy {
public:
    AudioTrackClientProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize, bool clientInServer = false)
        : ClientProxy(cblk, buffers, frameCount, frameSize, true /*isOut*/,
          clientInServer) { }
    virtual ~AudioTrackClientProxy() { }

    // No barriers on the following operations, so the ordering of loads/stores
    // with respect to other parameters is UNPREDICTABLE. That's considered safe.

    // caller must limit to 0.0 <= sendLevel <= 1.0
    void        setSendLevel(float sendLevel) {
        mCblk->mSendLevel = uint16_t(sendLevel * 0x1000);
    }

    // caller must limit to 0 <= volumeLR <= 0x10001000
    void        setVolumeLR(uint32_t volumeLR) {
        mCblk->mVolumeLR = volumeLR;
    }

    void        setSampleRate(uint32_t sampleRate) {
        mCblk->mSampleRate = sampleRate;
    }

    virtual void flush();

    virtual uint32_t    getUnderrunFrames() const {
        return mCblk->u.mStreaming.mUnderrunFrames;
    }

    bool        clearStreamEndDone();   // and return previous value

    bool        getStreamEndDone() const;

    status_t    waitStreamEndDone(const struct timespec *requested);
};

class StaticAudioTrackClientProxy : public AudioTrackClientProxy {
public:
    StaticAudioTrackClientProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize);
    virtual ~StaticAudioTrackClientProxy() { }

    virtual void    flush();

#define MIN_LOOP    16  // minimum length of each loop iteration in frames
            void    setLoop(size_t loopStart, size_t loopEnd, int loopCount);
            size_t  getBufferPosition();

    virtual size_t  getMisalignment() {
        return 0;
    }

    virtual uint32_t    getUnderrunFrames() const {
        return 0;
    }

private:
    StaticAudioTrackSingleStateQueue::Mutator   mMutator;
    size_t          mBufferPosition;    // so that getBufferPosition() appears to be synchronous
};

// ----------------------------------------------------------------------------

// Proxy used by AudioRecord client
class AudioRecordClientProxy : public ClientProxy {
public:
    AudioRecordClientProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize)
        : ClientProxy(cblk, buffers, frameCount, frameSize,
            false /*isOut*/, false /*clientInServer*/) { }
    ~AudioRecordClientProxy() { }
};

// ----------------------------------------------------------------------------

// Proxy used by AudioFlinger server
class ServerProxy : public Proxy {
protected:
    ServerProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount, size_t frameSize,
            bool isOut, bool clientInServer);
public:
    virtual ~ServerProxy() { }

    // Obtain a buffer with filled frames (writing) or empty frames (reading).
    // It is permitted to call obtainBuffer() multiple times in succession, without any intervening
    // calls to releaseBuffer().  In that case, the final obtainBuffer() is the one that effectively
    // sets or extends the unreleased frame count.
    // Always non-blocking.
    // On entry:
    //  buffer->mFrameCount should be initialized to maximum number of desired frames,
    //      which must be > 0.
    //  buffer->mNonContig is unused.
    //  buffer->mRaw is unused.
    //  ackFlush is true iff being called from Track::start to acknowledge a pending flush.
    // On exit:
    //  buffer->mFrameCount has the actual number of contiguous available frames,
    //      which is always 0 when the return status != NO_ERROR.
    //  buffer->mNonContig is the number of additional non-contiguous available frames.
    //  buffer->mRaw is a pointer to the first available frame,
    //      or NULL when buffer->mFrameCount == 0.
    // The return status is one of:
    //  NO_ERROR    Success, buffer->mFrameCount > 0.
    //  WOULD_BLOCK No frames are available.
    //  NO_INIT     Shared memory is corrupt.
    virtual status_t    obtainBuffer(Buffer* buffer, bool ackFlush = false);

    // Release (some of) the frames last obtained.
    // On entry, buffer->mFrameCount should have the number of frames to release,
    // which must (cumulatively) be <= the number of frames last obtained but not yet released.
    // It is permitted to call releaseBuffer() multiple times to release the frames in chunks.
    // buffer->mRaw is ignored, but is normally same pointer returned by last obtainBuffer().
    // On exit:
    //  buffer->mFrameCount is zero.
    //  buffer->mRaw is NULL.
    virtual void        releaseBuffer(Buffer* buffer);

protected:
    size_t      mAvailToClient; // estimated frames available to client prior to releaseBuffer()
    int32_t     mFlush;         // our copy of cblk->u.mStreaming.mFlush, for streaming output only
};

// Proxy used by AudioFlinger for servicing AudioTrack
class AudioTrackServerProxy : public ServerProxy {
public:
    AudioTrackServerProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize, bool clientInServer = false)
        : ServerProxy(cblk, buffers, frameCount, frameSize, true /*isOut*/, clientInServer) { }
protected:
    virtual ~AudioTrackServerProxy() { }

public:
    // return value of these methods must be validated by the caller
    uint32_t    getSampleRate() const { return mCblk->mSampleRate; }
    uint16_t    getSendLevel_U4_12() const { return mCblk->mSendLevel; }
    uint32_t    getVolumeLR() const { return mCblk->mVolumeLR; }

    // estimated total number of filled frames available to server to read,
    // which may include non-contiguous frames
    virtual size_t      framesReady();

    // Currently AudioFlinger will call framesReady() for a fast track from two threads:
    // FastMixer thread, and normal mixer thread.  This is dangerous, as the proxy is intended
    // to be called from at most one thread of server, and one thread of client.
    // As a temporary workaround, this method informs the proxy implementation that it
    // should avoid doing a state queue poll from within framesReady().
    // FIXME Change AudioFlinger to not call framesReady() from normal mixer thread.
    virtual void        framesReadyIsCalledByMultipleThreads() { }

    bool     setStreamEndDone();    // and return previous value

    // Add to the tally of underrun frames, and inform client of underrun
    virtual void        tallyUnderrunFrames(uint32_t frameCount);

    // Return the total number of frames which AudioFlinger desired but were unavailable,
    // and thus which resulted in an underrun.
    virtual uint32_t    getUnderrunFrames() const { return mCblk->u.mStreaming.mUnderrunFrames; }

    // Return the total number of frames that AudioFlinger has obtained and released
    virtual size_t      framesReleased() const { return mCblk->mServer; }
};

class StaticAudioTrackServerProxy : public AudioTrackServerProxy {
public:
    StaticAudioTrackServerProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize);
protected:
    virtual ~StaticAudioTrackServerProxy() { }

public:
    virtual size_t      framesReady();
    virtual void        framesReadyIsCalledByMultipleThreads();
    virtual status_t    obtainBuffer(Buffer* buffer, bool ackFlush);
    virtual void        releaseBuffer(Buffer* buffer);
    virtual void        tallyUnderrunFrames(uint32_t frameCount);
    virtual uint32_t    getUnderrunFrames() const { return 0; }

private:
    ssize_t             pollPosition(); // poll for state queue update, and return current position
    StaticAudioTrackSingleStateQueue::Observer  mObserver;
    size_t              mPosition;  // server's current play position in frames, relative to 0
    size_t              mEnd;       // cached value computed from mState, safe for asynchronous read
    bool                mFramesReadyIsCalledByMultipleThreads;
    StaticAudioTrackState   mState;
};

// Proxy used by AudioFlinger for servicing AudioRecord
class AudioRecordServerProxy : public ServerProxy {
public:
    AudioRecordServerProxy(audio_track_cblk_t* cblk, void *buffers, size_t frameCount,
            size_t frameSize)
        : ServerProxy(cblk, buffers, frameCount, frameSize, false /*isOut*/,
            false /*clientInServer*/) { }
protected:
    virtual ~AudioRecordServerProxy() { }
};

// ----------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_AUDIO_TRACK_SHARED_H
