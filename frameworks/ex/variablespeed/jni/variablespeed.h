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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_VARIABLESPEED_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_VARIABLESPEED_H_

#include <jni.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>

#include <integral_types.h>
#include <utils/threads.h>

#include <profile_timer.h>
#include <decode_buffer.h>

#include <queue>
#include <stack>

namespace video_editing {
  class SolaTimeScaler;
}

// This is the audio engine class.
// It forms the bulk  of the variablespeed library.
// It should not be used directly, but rather used indirectly from the java
// native methods.
class AudioEngine {
 public:
  AudioEngine(size_t targetFrames, float windowDuration,
      float windowOverlapDuration, size_t maxPlayBufferCount,
      float initialRate, size_t decodeInitialSize, size_t decodeMaxSize,
      size_t startPositionMillis, int audioStreamType);
  virtual ~AudioEngine();

  bool PlayUri(const char* uri);
  bool PlayFileDescriptor(int fd, int64 offset, int64 length);
  void SetVariableSpeed(float speed);
  void RequestStart();
  void RequestStop();
  int GetCurrentPosition();
  int GetTotalDuration();

  void DecodingBufferQueueCallback(
      SLAndroidSimpleBufferQueueItf queueItf, void *context);
  void DecodingEventCallback(SLPlayItf caller, SLuint32 event);
  void PrefetchEventCallback(SLPrefetchStatusItf caller, SLuint32 event);
  void PlayingBufferQueueCallback();

  static AudioEngine* GetEngine();
  static void SetEngine(AudioEngine* engine);
  static void DeleteEngine();

 private:
  bool PlayFromThisSource(const SLDataSource& audioSrc);
  void EnqueueMoreAudioIfNecessary(SLAndroidSimpleBufferQueueItf bufferQueue);
  bool EnqueueNextBufferOfAudio(SLAndroidSimpleBufferQueueItf bufferQueue);
  void PrefetchDurationSampleRateAndChannels(
      SLPlayItf playItf, SLPrefetchStatusItf prefetchItf);
  video_editing::SolaTimeScaler* GetTimeScaler();
  bool Finished();
  bool GetWasStartRequested();
  bool GetWasStopRequested();
  void ClearRequestStart();
  void SetEndOfDecoderReached();
  bool GetEndOfDecoderReached();
  bool DecodeBufferTooFull();
  void ClearDecodeBuffer();
  bool IsDecodeBufferEmpty();
  bool GetHasReachedPlayingBuffersLimit();
  bool HasSampleRateAndChannels();
  SLuint32 GetSLSampleRate();
  SLuint32 GetSLChannels();
  size_t GetChannelCount();

  // The single global audio engine instance.
  static AudioEngine* audioEngine_;

  // Protects access to the shared decode buffer.
  android::Mutex decodeBufferLock_;
  // Buffer into which we put the audio data as we decode.
  // Protected by decodeBufferLock_.
  DecodeBuffer decodeBuffer_;

  // Protects access to the playingBuffers_ and freeBuffers_.
  android::Mutex playBufferLock_;
  // The buffers we're using for playback.
  std::queue<int16*> playingBuffers_;
  std::stack<int16*> freeBuffers_;

  // The time scaler.
  video_editing::SolaTimeScaler* timeScaler_;

  // The frame buffer, used for converting between PCM data and float for
  // time scaler.
  float* floatBuffer_;
  float* injectBuffer_;

  // Required when we create the audio player.
  // Set during the first callback from the decoder.
  // Guarded by callbackLock_.
  SLuint32 mSampleRate;
  SLuint32 mChannels;

  size_t targetFrames_;
  float windowDuration_;
  float windowOverlapDuration_;
  size_t maxPlayBufferCount_;
  float initialRate_;
  size_t startPositionMillis_;
  // The type of audio stream as defined by the STREAM_XXX constants in
  // android.media.AudioManager. These constant values actually match the
  // corresponding SL_ANDROID_STREAM_XXX constants defined by
  // include/SLES/OpenSLES_AndroidConfiguration.h
  int audioStreamType_;

  // The prefetch callback signal, for letting the prefetch callback method
  // indicate when it is done.
  android::Mutex prefetchLock_;
  android::Condition prefetchCondition_;

  // Protects access to the CallbackContext object.
  // I don't believe this to be necessary, I think that it's thread-confined,
  // but it also won't do any harm.
  android::Mutex callbackLock_;

  // Protects access to the shared member variables below.
  android::Mutex lock_;
  // Protected by lock_.
  // Stores the total duration of the track.
  SLmillisecond totalDurationMs_;
  // Protected by lock_.
  // Stores the current position of the decoder head.
  SLmillisecond decoderCurrentPosition_;
  // Protected by lock_.
  // Set externally via RequestStart(), this determines when we begin to
  // playback audio.
  // Until this is set to true, our audio player will remain stopped.
  bool startRequested_;
  // Protected by lock_.
  // Set externally via RequestStop(), this tells us top stop playing
  // and therefore shut everything down.
  bool stopRequested_;
  // Protected by lock_.
  // This is set to true once we reach the end of the decoder stream.
  bool finishedDecoding_;

  DISALLOW_COPY_AND_ASSIGN(AudioEngine);
};

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_VARIABLESPEED_H_
