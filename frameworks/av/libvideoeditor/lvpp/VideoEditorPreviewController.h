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

#ifndef ANDROID_VE_PREVIEWCONTROLLER_H
#define ANDROID_VE_PREVIEWCONTROLLER_H

#include "VideoEditorPlayer.h"
#include "VideoEditorTools.h"

namespace android {

// Callback mechanism from PreviewController to Jni  */
typedef void (*jni_progress_callback_fct)(void* cookie, M4OSA_UInt32 msgType, void *argc);

struct Surface;
struct PreviewRenderer;

class VideoEditorPreviewController {

public:
    VideoEditorPreviewController();
    ~VideoEditorPreviewController();

    M4OSA_ERR loadEditSettings(
            M4VSS3GPP_EditSettings* pSettings,
            M4xVSS_AudioMixingSettings* bgmSettings);

    M4OSA_ERR setSurface(const sp<Surface>& surface);

    M4OSA_ERR startPreview(
            M4OSA_UInt32 fromMS, M4OSA_Int32 toMs,
            M4OSA_UInt16 callBackAfterFrameCount,
            M4OSA_Bool loop) ;

    M4OSA_UInt32 stopPreview();

    M4OSA_ERR renderPreviewFrame(
            const sp<Surface>& surface,
            VideoEditor_renderPreviewFrameStr* pFrameInfo,
            VideoEditorCurretEditInfo *pCurrEditInfo);

    M4OSA_ERR clearSurface(
            const sp<Surface>& surface,
            VideoEditor_renderPreviewFrameStr* pFrameInfo);

    M4OSA_Void setJniCallback(
            void* cookie,
            jni_progress_callback_fct callbackFct);

    status_t setPreviewFrameRenderingMode(
            M4xVSS_MediaRendering mode,
            M4VIDEOEDITING_VideoFrameSize outputVideoSize);

private:
    enum {
        kTotalNumPlayerInstances = 2,
        kPreviewThreadStackSize = 65536,
    };

    typedef enum {
        VePlayerIdle = 0,
        VePlayerBusy,
        VePlayerAutoStop
    } PlayerState;

    typedef enum {
        OVERLAY_UPDATE = 0,
        OVERLAY_CLEAR
    } OverlayState;

    sp<VideoEditorPlayer> mVePlayer[kTotalNumPlayerInstances];
    int mCurrentPlayer;  // player instance currently being used
    sp<Surface>  mSurface;
    mutable Mutex mLock;
    M4OSA_Context mThreadContext;
    PlayerState mPlayerState;
    M4OSA_Bool    mPrepareReqest;
    M4VSS3GPP_ClipSettings **mClipList;
    M4OSA_UInt32 mNumberClipsInStoryBoard;
    M4OSA_UInt32 mNumberClipsToPreview;
    M4OSA_UInt32 mStartingClipIndex;
    M4OSA_Bool mPreviewLooping;
    M4OSA_UInt32 mCallBackAfterFrameCnt;
    M4VSS3GPP_EffectSettings* mEffectsSettings;
    M4OSA_UInt32 mNumberEffects;
    M4OSA_Int32 mCurrentClipNumber;
    M4OSA_UInt32 mClipTotalDuration;
    M4OSA_UInt32 mCurrentVideoEffect;
    M4xVSS_AudioMixingSettings* mBackgroundAudioSetting;
    M4OSA_Context mAudioMixPCMFileHandle;
    PreviewRenderer *mTarget;
    M4OSA_Context mJniCookie;
    jni_progress_callback_fct mJniCallback;
    VideoEditor_renderPreviewFrameStr mFrameStr;
    M4OSA_UInt32 mCurrentPlayedDuration;
    M4OSA_UInt32 mCurrentClipDuration;
    M4VIDEOEDITING_VideoFrameSize mOutputVideoSize;
    M4OSA_UInt32 mFirstPreviewClipBeginTime;
    M4OSA_UInt32 mLastPreviewClipEndTime;
    M4OSA_UInt32 mVideoStoryBoardTimeMsUptoFirstPreviewClip;
    OverlayState mOverlayState;
    int mActivePlayerIndex;

    M4xVSS_MediaRendering mRenderingMode;
    uint32_t mOutputVideoWidth;
    uint32_t mOutputVideoHeight;
    bool bStopThreadInProgress;
    M4OSA_Context mSemThreadWait;
    bool mIsFiftiesEffectStarted;

    sp<VideoEditorPlayer::VeAudioOutput> mVEAudioSink;
    VideoEditorAudioPlayer *mVEAudioPlayer;
    NativeWindowRenderer* mNativeWindowRenderer;

    M4VIFI_UInt8*  mFrameRGBBuffer;
    M4VIFI_UInt8*  mFrameYUVBuffer;
    mutable Mutex mLockSem;


    static M4OSA_ERR preparePlayer(void* param, int playerInstance, int index);
    static M4OSA_ERR threadProc(M4OSA_Void* param);
    static void notify(void* cookie, int msg, int ext1, int ext2);

    void setVideoEffectType(M4VSS3GPP_VideoEffectType type, M4OSA_Bool enable);

    M4OSA_ERR applyVideoEffect(
            M4OSA_Void * dataPtr, M4OSA_UInt32 colorFormat,
            M4OSA_UInt32 videoWidth, M4OSA_UInt32 videoHeight,
            M4OSA_UInt32 timeMs, M4OSA_Void* outPtr);

    M4OSA_ERR doImageRenderingMode(
            M4OSA_Void * dataPtr,
            M4OSA_UInt32 colorFormat, M4OSA_UInt32 videoWidth,
            M4OSA_UInt32 videoHeight, M4OSA_Void* outPtr);

    // Don't call me!
    VideoEditorPreviewController(const VideoEditorPreviewController &);
    VideoEditorPreviewController &operator=(
            const VideoEditorPreviewController &);
};

}

#endif // ANDROID_VE_PREVIEWCONTROLLER_H
