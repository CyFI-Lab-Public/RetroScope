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

#ifndef VE_AUDIO_PLAYER_H_
#define VE_AUDIO_PLAYER_H_

#include <media/MediaPlayerInterface.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/TimeSource.h>
#include <utils/threads.h>

#include "M4xVSS_API.h"
#include "VideoEditorMain.h"
#include "M4OSA_FileReader.h"
#include "VideoEditorBGAudioProcessing.h"


namespace android {

class MediaSource;
class AudioTrack;
class PreviewPlayer;

class VideoEditorAudioPlayer : public TimeSource {
public:
    enum {
        REACHED_EOS,
        SEEK_COMPLETE
    };

    VideoEditorAudioPlayer(const sp<MediaPlayerBase::AudioSink> &audioSink,
        PreviewPlayer *audioObserver = NULL);

    ~VideoEditorAudioPlayer();

    // Return time in us.
    int64_t getRealTimeUs();

    // Returns the timestamp of the last buffer played (in us).
    int64_t getMediaTimeUs();

    // Returns true iff a mapping is established, i.e. the AudioPlayerBase
    // has played at least one frame of audio.
    bool getMediaTimeMapping(int64_t *realtime_us, int64_t *mediatime_us);

    status_t start(bool sourceAlreadyStarted = false);
    void pause(bool playPendingSamples = false);
    status_t resume();
    status_t seekTo(int64_t time_us);
    bool isSeeking();
    bool reachedEOS(status_t *finalStatus);

    void setAudioMixSettings(M4xVSS_AudioMixingSettings* pAudioMixSettings);
    void setAudioMixPCMFileHandle(M4OSA_Context pBGAudioPCMFileHandle);
    void setAudioMixStoryBoardSkimTimeStamp(
        M4OSA_UInt32 pBGAudioStoryBoardSkimTimeStamp,
        M4OSA_UInt32 pBGAudioCurrentMediaBeginCutTS,
        M4OSA_UInt32 pBGAudioCurrentMediaVolumeVal);

    void setObserver(PreviewPlayer *observer);
    void setSource(const sp<MediaSource> &source);
    sp<MediaSource> getSource();

    bool isStarted();
private:

    M4xVSS_AudioMixingSettings *mAudioMixSettings;
    VideoEditorBGAudioProcessing *mAudioProcess;

    M4OSA_Context mBGAudioPCMFileHandle;
    int64_t mBGAudioPCMFileLength;
    int64_t mBGAudioPCMFileTrimmedLength;
    int64_t mBGAudioPCMFileDuration;
    int64_t mBGAudioPCMFileSeekPoint;
    int64_t mBGAudioPCMFileOriginalSeekPoint;
    int64_t mBGAudioStoryBoardSkimTimeStamp;
    int64_t mBGAudioStoryBoardCurrentMediaBeginCutTS;
    int64_t mBGAudioStoryBoardCurrentMediaVolumeVal;

    sp<MediaSource> mSource;
    sp<AudioTrack> mAudioTrack;

    MediaBuffer *mInputBuffer;

    int mSampleRate;
    int64_t mLatencyUs;
    size_t mFrameSize;

    Mutex mLock;
    int64_t mNumFramesPlayed;

    int64_t mPositionTimeMediaUs;
    int64_t mPositionTimeRealUs;

    bool mSeeking;
    bool mReachedEOS;
    status_t mFinalStatus;
    int64_t mSeekTimeUs;

    bool mStarted;

    bool mIsFirstBuffer;
    status_t mFirstBufferResult;
    MediaBuffer *mFirstBuffer;

    sp<MediaPlayerBase::AudioSink> mAudioSink;
    PreviewPlayer *mObserver;

    static void AudioCallback(int event, void *user, void *info);
    void AudioCallback(int event, void *info);
    size_t fillBuffer(void *data, size_t size);
    static size_t AudioSinkCallback(
            MediaPlayerBase::AudioSink *audioSink,
            void *data, size_t size, void *me,
            MediaPlayerBase::AudioSink::cb_event_t event);

    void reset();
    void clear();
    int64_t getRealTimeUs_l();
    void setPrimaryTrackVolume(
            M4OSA_Int16 *data, M4OSA_UInt32 size, M4OSA_Float volLevel);

    VideoEditorAudioPlayer(const VideoEditorAudioPlayer &);
    VideoEditorAudioPlayer &operator=(const VideoEditorAudioPlayer &);
};

}  // namespace android

#endif  // VE_AUDIO_PLAYER_H_
