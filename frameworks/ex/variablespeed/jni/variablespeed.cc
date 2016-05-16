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

#include <variablespeed.h>

#include <unistd.h>
#include <stdlib.h>

#include <sola_time_scaler.h>
#include <ring_buffer.h>

#include <hlogging.h>

#include <vector>

#include <sys/system_properties.h>

// ****************************************************************************
// Constants, utility methods, structures and other miscellany used throughout
// this file.

namespace {

// These variables are used to determine the size of the buffer queue used by
// the decoder.
// This is not the same as the large buffer used to hold the uncompressed data
// - for that see the member variable decodeBuffer_.
// The choice of 1152 corresponds to the number of samples per mp3 frame, so is
// a good choice of size for a decoding buffer in the absence of other
// information (we don't know exactly what formats we will be working with).
const size_t kNumberOfBuffersInQueue = 4;
const size_t kNumberOfSamplesPerBuffer = 1152;
const size_t kBufferSizeInBytes = 2 * kNumberOfSamplesPerBuffer;
const size_t kSampleSizeInBytes = 4;

// When calculating play buffer size before pushing to audio player.
const size_t kNumberOfBytesPerInt16 = 2;

// How long to sleep during the main play loop and the decoding callback loop.
// In due course this should be replaced with the better signal and wait on
// condition rather than busy-looping.
const int kSleepTimeMicros = 1000;

// Used in detecting errors with the OpenSL ES framework.
const SLuint32 kPrefetchErrorCandidate =
    SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE;

// Structure used when we perform a decoding callback.
typedef struct CallbackContext_ {
  // Pointer to local storage buffers for decoded audio data.
  int8_t* pDataBase;
  // Pointer to the current buffer within local storage.
  int8_t* pData;
  // Used to read the sample rate and channels from the decoding stream during
  // the first decoding callback.
  SLMetadataExtractionItf decoderMetadata;
  // The play interface used for reading duration.
  SLPlayItf playItf;
} CallbackContext;

// Local storage for decoded audio data.
int8_t pcmData[kNumberOfBuffersInQueue * kBufferSizeInBytes];

#define CheckSLResult(message, result) \
    CheckSLResult_Real(message, result, __LINE__)

// Helper function for debugging - checks the OpenSL result for success.
void CheckSLResult_Real(const char* message, SLresult result, int line) {
  // This can be helpful when debugging.
  // LOGD("sl result %d for %s", result, message);
  if (SL_RESULT_SUCCESS != result) {
    LOGE("slresult was %d at %s file variablespeed line %d",
        static_cast<int>(result), message, line);
  }
  CHECK(SL_RESULT_SUCCESS == result);
}

// Whether logging should be enabled. Only used if LOG_OPENSL_API_CALL is
// defined to use it.
bool gLogEnabled = false;
// The property to set in order to enable logging.
const char *const kLogTagVariableSpeed = "log.tag.VariableSpeed";

bool ShouldLog() {
  char buffer[PROP_VALUE_MAX];
  __system_property_get(kLogTagVariableSpeed, buffer);
  return strlen(buffer) > 0;
}

}  // namespace

// ****************************************************************************
// Static instance of audio engine, and methods for getting, setting and
// deleting it.

// The single global audio engine instance.
AudioEngine* AudioEngine::audioEngine_ = NULL;
android::Mutex publishEngineLock_;

AudioEngine* AudioEngine::GetEngine() {
  android::Mutex::Autolock autoLock(publishEngineLock_);
  if (audioEngine_ == NULL) {
    LOGE("you haven't initialized the audio engine");
    CHECK(false);
    return NULL;
  }
  return audioEngine_;
}

void AudioEngine::SetEngine(AudioEngine* engine) {
  if (audioEngine_ != NULL) {
    LOGE("you have already set the audio engine");
    CHECK(false);
    return;
  }
  audioEngine_ = engine;
}

void AudioEngine::DeleteEngine() {
  if (audioEngine_ == NULL) {
    LOGE("you haven't initialized the audio engine");
    CHECK(false);
    return;
  }
  delete audioEngine_;
  audioEngine_ = NULL;
}

// ****************************************************************************
// The callbacks from the engine require static callback functions.
// Here are the static functions - they just delegate to instance methods on
// the engine.

static void PlayingBufferQueueCb(SLAndroidSimpleBufferQueueItf, void*) {
  AudioEngine::GetEngine()->PlayingBufferQueueCallback();
}

static void PrefetchEventCb(SLPrefetchStatusItf caller, void*, SLuint32 event) {
  AudioEngine::GetEngine()->PrefetchEventCallback(caller, event);
}

static void DecodingBufferQueueCb(SLAndroidSimpleBufferQueueItf queueItf,
    void *context) {
  AudioEngine::GetEngine()->DecodingBufferQueueCallback(queueItf, context);
}

static void DecodingEventCb(SLPlayItf caller, void*, SLuint32 event) {
  AudioEngine::GetEngine()->DecodingEventCallback(caller, event);
}

// ****************************************************************************
// Macros for making working with OpenSL easier.

// Log based on the value of a property.
#define LOG_OPENSL_API_CALL(string) (gLogEnabled && LOGV(string))

// The regular macro: log an api call, make the api call, check the result.
#define OpenSL(obj, method, ...) \
{ \
  LOG_OPENSL_API_CALL("OpenSL " #method "(" #obj ", " #__VA_ARGS__ ")"); \
  SLresult result = (*obj)->method(obj, __VA_ARGS__); \
  CheckSLResult("OpenSL " #method "(" #obj ", " #__VA_ARGS__ ")", result); \
}

// Special case call for api call that has void return value, can't be checked.
#define VoidOpenSL(obj, method) \
{ \
  LOG_OPENSL_API_CALL("OpenSL (void) " #method "(" #obj ")"); \
  (*obj)->method(obj); \
}

// Special case for api call with checked result but takes no arguments.
#define OpenSL0(obj, method) \
{ \
  LOG_OPENSL_API_CALL("OpenSL " #method "(" #obj ")"); \
  SLresult result = (*obj)->method(obj); \
  CheckSLResult("OpenSL " #method "(" #obj ")", result); \
}

// Special case for api call whose result we want to store, not check.
// We have to encapsulate the two calls in braces, so that this expression
// evaluates to the last expression not the first.
#define ReturnOpenSL(obj, method, ...) \
( \
    LOG_OPENSL_API_CALL("OpenSL (int) " \
        #method "(" #obj ", " #__VA_ARGS__ ")"), \
    (*obj)->method(obj, __VA_ARGS__) \
) \

// ****************************************************************************
// Static utility methods.

// Set the audio stream type for the player.
//
// Must be called before it is realized.
//
// The caller must have requested the SL_IID_ANDROIDCONFIGURATION interface when
// creating the player.
static void setAudioStreamType(SLObjectItf audioPlayer, SLint32 audioStreamType) {
  SLAndroidConfigurationItf playerConfig;
  OpenSL(audioPlayer, GetInterface, SL_IID_ANDROIDCONFIGURATION, &playerConfig);
  // The STREAM_XXX constants defined by android.media.AudioManager match the
  // corresponding SL_ANDROID_STREAM_XXX constants defined by
  // include/SLES/OpenSLES_AndroidConfiguration.h, so we can just pass the
  // value across.
  OpenSL(playerConfig, SetConfiguration, SL_ANDROID_KEY_STREAM_TYPE,
         &audioStreamType, sizeof(audioStreamType));
}

// Must be called with callbackLock_ held.
static void ReadSampleRateAndChannelCount(CallbackContext *pContext,
    SLuint32 *sampleRateOut, SLuint32 *channelsOut) {
  SLMetadataExtractionItf decoderMetadata = pContext->decoderMetadata;
  SLuint32 itemCount;
  OpenSL(decoderMetadata, GetItemCount, &itemCount);
  SLuint32 i, keySize, valueSize;
  SLMetadataInfo *keyInfo, *value;
  for (i = 0; i < itemCount; ++i) {
    keyInfo = value = NULL;
    keySize = valueSize = 0;
    OpenSL(decoderMetadata, GetKeySize, i, &keySize);
    keyInfo = static_cast<SLMetadataInfo*>(malloc(keySize));
    if (keyInfo) {
      OpenSL(decoderMetadata, GetKey, i, keySize, keyInfo);
      if (keyInfo->encoding == SL_CHARACTERENCODING_ASCII
          || keyInfo->encoding == SL_CHARACTERENCODING_UTF8) {
        OpenSL(decoderMetadata, GetValueSize, i, &valueSize);
        value = static_cast<SLMetadataInfo*>(malloc(valueSize));
        if (value) {
          OpenSL(decoderMetadata, GetValue, i, valueSize, value);
          if (strcmp((char*) keyInfo->data, ANDROID_KEY_PCMFORMAT_SAMPLERATE) == 0) {
            SLuint32 sampleRate = *(reinterpret_cast<SLuint32*>(value->data));
            LOGD("sample Rate: %d", sampleRate);
            *sampleRateOut = sampleRate;
          } else if (strcmp((char*) keyInfo->data, ANDROID_KEY_PCMFORMAT_NUMCHANNELS) == 0) {
            SLuint32 channels = *(reinterpret_cast<SLuint32*>(value->data));
            LOGD("channels: %d", channels);
            *channelsOut = channels;
          }
          free(value);
        }
      }
      free(keyInfo);
    }
  }
}

// Must be called with callbackLock_ held.
static void RegisterCallbackContextAndAddEnqueueBuffersToDecoder(
    SLAndroidSimpleBufferQueueItf decoderQueue, CallbackContext* context) {
  // Register a callback on the decoder queue, so that we will be called
  // throughout the decoding process (and can then extract the decoded audio
  // for the next bit of the pipeline).
  OpenSL(decoderQueue, RegisterCallback, DecodingBufferQueueCb, context);

  // Enqueue buffers to map the region of memory allocated to store the
  // decoded data.
  for (size_t i = 0; i < kNumberOfBuffersInQueue; i++) {
    OpenSL(decoderQueue, Enqueue, context->pData, kBufferSizeInBytes);
    context->pData += kBufferSizeInBytes;
  }
  context->pData = context->pDataBase;
}

// ****************************************************************************
// Constructor and Destructor.

AudioEngine::AudioEngine(size_t targetFrames, float windowDuration,
    float windowOverlapDuration, size_t maxPlayBufferCount, float initialRate,
    size_t decodeInitialSize, size_t decodeMaxSize, size_t startPositionMillis,
    int audioStreamType)
    : decodeBuffer_(decodeInitialSize, decodeMaxSize),
      playingBuffers_(), freeBuffers_(), timeScaler_(NULL),
      floatBuffer_(NULL), injectBuffer_(NULL),
      mSampleRate(0), mChannels(0),
      targetFrames_(targetFrames),
      windowDuration_(windowDuration),
      windowOverlapDuration_(windowOverlapDuration),
      maxPlayBufferCount_(maxPlayBufferCount), initialRate_(initialRate),
      startPositionMillis_(startPositionMillis),
      audioStreamType_(audioStreamType),
      totalDurationMs_(0), decoderCurrentPosition_(0), startRequested_(false),
      stopRequested_(false), finishedDecoding_(false) {
  // Determine whether we should log calls.
  gLogEnabled = ShouldLog();
}

AudioEngine::~AudioEngine() {
  // destroy the time scaler
  if (timeScaler_ != NULL) {
    delete timeScaler_;
    timeScaler_ = NULL;
  }

  // delete all outstanding playing and free buffers
  android::Mutex::Autolock autoLock(playBufferLock_);
  while (playingBuffers_.size() > 0) {
    delete[] playingBuffers_.front();
    playingBuffers_.pop();
  }
  while (freeBuffers_.size() > 0) {
    delete[] freeBuffers_.top();
    freeBuffers_.pop();
  }

  delete[] floatBuffer_;
  floatBuffer_ = NULL;
  delete[] injectBuffer_;
  injectBuffer_ = NULL;
}

// ****************************************************************************
// Regular AudioEngine class methods.

void AudioEngine::SetVariableSpeed(float speed) {
  // TODO: Mutex for shared time scaler accesses.
  if (HasSampleRateAndChannels()) {
    GetTimeScaler()->set_speed(speed);
  } else {
    // This is being called at a point where we have not yet processed enough
    // data to determine the sample rate and number of channels.
    // Ignore the call.  See http://b/5140693.
    LOGD("set varaible speed called, sample rate and channels not ready yet");
  }
}

void AudioEngine::RequestStart() {
  android::Mutex::Autolock autoLock(lock_);
  startRequested_ = true;
}

void AudioEngine::ClearRequestStart() {
  android::Mutex::Autolock autoLock(lock_);
  startRequested_ = false;
}

bool AudioEngine::GetWasStartRequested() {
  android::Mutex::Autolock autoLock(lock_);
  return startRequested_;
}

void AudioEngine::RequestStop() {
  android::Mutex::Autolock autoLock(lock_);
  stopRequested_ = true;
}

int AudioEngine::GetCurrentPosition() {
  android::Mutex::Autolock autoLock(decodeBufferLock_);
  double result = decodeBuffer_.GetTotalAdvancedCount();
  // TODO: This is horrible, but should be removed soon once the outstanding
  // issue with get current position on decoder is fixed.
  android::Mutex::Autolock autoLock2(callbackLock_);
  return static_cast<int>(
      (result * 1000) / mSampleRate / mChannels + startPositionMillis_);
}

int AudioEngine::GetTotalDuration() {
  android::Mutex::Autolock autoLock(lock_);
  return static_cast<int>(totalDurationMs_);
}

video_editing::SolaTimeScaler* AudioEngine::GetTimeScaler() {
  if (timeScaler_ == NULL) {
    CHECK(HasSampleRateAndChannels());
    android::Mutex::Autolock autoLock(callbackLock_);
    timeScaler_ = new video_editing::SolaTimeScaler();
    timeScaler_->Init(mSampleRate, mChannels, initialRate_, windowDuration_,
        windowOverlapDuration_);
  }
  return timeScaler_;
}

bool AudioEngine::EnqueueNextBufferOfAudio(
    SLAndroidSimpleBufferQueueItf audioPlayerQueue) {
  size_t channels;
  {
    android::Mutex::Autolock autoLock(callbackLock_);
    channels = mChannels;
  }
  size_t frameSizeInBytes = kSampleSizeInBytes * channels;
  size_t frameCount = 0;
  while (frameCount < targetFrames_) {
    size_t framesLeft = targetFrames_ - frameCount;
    // If there is data already in the time scaler, retrieve it.
    if (GetTimeScaler()->available() > 0) {
      size_t retrieveCount = min(GetTimeScaler()->available(), framesLeft);
      int count = GetTimeScaler()->RetrieveSamples(
          floatBuffer_ + frameCount * channels, retrieveCount);
      if (count <= 0) {
        LOGD("error: count was %d", count);
        break;
      }
      frameCount += count;
      continue;
    }
    // If there is no data in the time scaler, then feed some into it.
    android::Mutex::Autolock autoLock(decodeBufferLock_);
    size_t framesInDecodeBuffer =
        decodeBuffer_.GetSizeInBytes() / frameSizeInBytes;
    size_t framesScalerCanHandle = GetTimeScaler()->input_limit();
    size_t framesToInject = min(framesInDecodeBuffer,
        min(targetFrames_, framesScalerCanHandle));
    if (framesToInject <= 0) {
      // No more frames left to inject.
      break;
    }
    for (size_t i = 0; i < framesToInject * channels; ++i) {
      injectBuffer_[i] = decodeBuffer_.GetAtIndex(i);
    }
    int count = GetTimeScaler()->InjectSamples(injectBuffer_, framesToInject);
    if (count <= 0) {
      LOGD("error: count was %d", count);
      break;
    }
    decodeBuffer_.AdvanceHeadPointerShorts(count * channels);
  }
  if (frameCount <= 0) {
    // We must have finished playback.
    if (GetEndOfDecoderReached()) {
      // If we've finished decoding, clear the buffer - so we will terminate.
      ClearDecodeBuffer();
    }
    return false;
  }

  // Get a free playing buffer.
  int16* playBuffer;
  {
    android::Mutex::Autolock autoLock(playBufferLock_);
    if (freeBuffers_.size() > 0) {
      // If we have a free buffer, recycle it.
      playBuffer = freeBuffers_.top();
      freeBuffers_.pop();
    } else {
      // Otherwise allocate a new one.
      playBuffer = new int16[targetFrames_ * channels];
    }
  }

  // Try to play the buffer.
  for (size_t i = 0; i < frameCount * channels; ++i) {
    playBuffer[i] = floatBuffer_[i];
  }
  size_t sizeOfPlayBufferInBytes =
      frameCount * channels * kNumberOfBytesPerInt16;
  SLresult result = ReturnOpenSL(audioPlayerQueue, Enqueue, playBuffer,
      sizeOfPlayBufferInBytes);
  if (result == SL_RESULT_SUCCESS) {
    android::Mutex::Autolock autoLock(playBufferLock_);
    playingBuffers_.push(playBuffer);
  } else {
    LOGE("could not enqueue audio buffer");
    delete[] playBuffer;
  }

  return (result == SL_RESULT_SUCCESS);
}

bool AudioEngine::GetEndOfDecoderReached() {
  android::Mutex::Autolock autoLock(lock_);
  return finishedDecoding_;
}

void AudioEngine::SetEndOfDecoderReached() {
  android::Mutex::Autolock autoLock(lock_);
  finishedDecoding_ = true;
}

bool AudioEngine::PlayFileDescriptor(int fd, int64 offset, int64 length) {
  SLDataLocator_AndroidFD loc_fd = {
      SL_DATALOCATOR_ANDROIDFD, fd, offset, length };
  SLDataFormat_MIME format_mime = {
      SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED };
  SLDataSource audioSrc = { &loc_fd, &format_mime };
  return PlayFromThisSource(audioSrc);
}

bool AudioEngine::PlayUri(const char* uri) {
  // Source of audio data for the decoding
  SLDataLocator_URI decUri = { SL_DATALOCATOR_URI,
      const_cast<SLchar*>(reinterpret_cast<const SLchar*>(uri)) };
  SLDataFormat_MIME decMime = {
      SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED };
  SLDataSource decSource = { &decUri, &decMime };
  return PlayFromThisSource(decSource);
}

bool AudioEngine::IsDecodeBufferEmpty() {
  android::Mutex::Autolock autoLock(decodeBufferLock_);
  return decodeBuffer_.GetSizeInBytes() <= 0;
}

void AudioEngine::ClearDecodeBuffer() {
  android::Mutex::Autolock autoLock(decodeBufferLock_);
  decodeBuffer_.Clear();
}

static size_t ReadDuration(SLPlayItf playItf) {
  SLmillisecond durationInMsec = SL_TIME_UNKNOWN;
  OpenSL(playItf, GetDuration, &durationInMsec);
  if (durationInMsec == SL_TIME_UNKNOWN) {
    LOGE("can't get duration");
    return 0;
  }
  LOGD("duration: %d", static_cast<int>(durationInMsec));
  return durationInMsec;
}

static size_t ReadPosition(SLPlayItf playItf) {
  SLmillisecond positionInMsec = SL_TIME_UNKNOWN;
  OpenSL(playItf, GetPosition, &positionInMsec);
  if (positionInMsec == SL_TIME_UNKNOWN) {
    LOGE("can't get position");
    return 0;
  }
  LOGW("decoder position: %d", static_cast<int>(positionInMsec));
  return positionInMsec;
}

static void CreateAndRealizeEngine(SLObjectItf &engine,
    SLEngineItf &engineInterface) {
  SLEngineOption EngineOption[] = { {
      SL_ENGINEOPTION_THREADSAFE, SL_BOOLEAN_TRUE } };
  SLresult result = slCreateEngine(&engine, 1, EngineOption, 0, NULL, NULL);
  CheckSLResult("create engine", result);
  OpenSL(engine, Realize, SL_BOOLEAN_FALSE);
  OpenSL(engine, GetInterface, SL_IID_ENGINE, &engineInterface);
}

SLuint32 AudioEngine::GetSLSampleRate() {
  android::Mutex::Autolock autoLock(callbackLock_);
  return mSampleRate * 1000;
}

SLuint32 AudioEngine::GetSLChannels() {
  android::Mutex::Autolock autoLock(callbackLock_);
  switch (mChannels) {
    case 2:
      return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    case 1:
      return SL_SPEAKER_FRONT_CENTER;
    default:
      LOGE("unknown channels %d, using 2", mChannels);
      return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
  }
}

SLuint32 AudioEngine::GetChannelCount() {
  android::Mutex::Autolock autoLock(callbackLock_);
  return mChannels;
}

static void CreateAndRealizeAudioPlayer(SLuint32 slSampleRate,
    size_t channelCount, SLuint32 slChannels, SLint32 audioStreamType, SLObjectItf &outputMix,
    SLObjectItf &audioPlayer, SLEngineItf &engineInterface) {
  // Define the source and sink for the audio player: comes from a buffer queue
  // and goes to the output mix.
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
      SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
  SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, channelCount, slSampleRate,
      SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
      slChannels, SL_BYTEORDER_LITTLEENDIAN};
  SLDataSource playingSrc = {&loc_bufq, &format_pcm};
  SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMix};
  SLDataSink audioSnk = {&loc_outmix, NULL};

  // Create the audio player, which will play from the buffer queue and send to
  // the output mix.
  const size_t playerInterfaceCount = 2;
  const SLInterfaceID iids[playerInterfaceCount] = {
      SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_ANDROIDCONFIGURATION };
  const SLboolean reqs[playerInterfaceCount] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
  OpenSL(engineInterface, CreateAudioPlayer, &audioPlayer, &playingSrc,
      &audioSnk, playerInterfaceCount, iids, reqs);
  setAudioStreamType(audioPlayer, audioStreamType);
  OpenSL(audioPlayer, Realize, SL_BOOLEAN_FALSE);
}

bool AudioEngine::HasSampleRateAndChannels() {
  android::Mutex::Autolock autoLock(callbackLock_);
  return mChannels != 0 && mSampleRate != 0;
}

bool AudioEngine::PlayFromThisSource(const SLDataSource& audioSrc) {
  ClearDecodeBuffer();

  SLObjectItf engine;
  SLEngineItf engineInterface;
  CreateAndRealizeEngine(engine, engineInterface);

  // Define the source and sink for the decoding player: comes from the source
  // this method was called with, is sent to another buffer queue.
  SLDataLocator_AndroidSimpleBufferQueue decBuffQueue;
  decBuffQueue.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
  decBuffQueue.numBuffers = kNumberOfBuffersInQueue;
  // A valid value seems required here but is currently ignored.
  SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_44_1,
      SL_PCMSAMPLEFORMAT_FIXED_16, 16,
      SL_SPEAKER_FRONT_LEFT, SL_BYTEORDER_LITTLEENDIAN};
  SLDataSink decDest = { &decBuffQueue, &pcm };

  // Create the decoder with the given source and sink.
  const size_t decoderInterfaceCount = 5;
  SLObjectItf decoder;
  const SLInterfaceID decodePlayerInterfaces[decoderInterfaceCount] = {
      SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_PREFETCHSTATUS, SL_IID_SEEK,
      SL_IID_METADATAEXTRACTION, SL_IID_ANDROIDCONFIGURATION };
  const SLboolean decodePlayerRequired[decoderInterfaceCount] = {
      SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
  SLDataSource sourceCopy(audioSrc);
  OpenSL(engineInterface, CreateAudioPlayer, &decoder, &sourceCopy, &decDest,
      decoderInterfaceCount, decodePlayerInterfaces, decodePlayerRequired);
  // Not sure if this is necessary, but just in case.
  setAudioStreamType(decoder, audioStreamType_);
  OpenSL(decoder, Realize, SL_BOOLEAN_FALSE);

  // Get the play interface from the decoder, and register event callbacks.
  // Get the buffer queue, prefetch and seek interfaces.
  SLPlayItf decoderPlay = NULL;
  SLAndroidSimpleBufferQueueItf decoderQueue = NULL;
  SLPrefetchStatusItf decoderPrefetch = NULL;
  SLSeekItf decoderSeek = NULL;
  SLMetadataExtractionItf decoderMetadata = NULL;
  OpenSL(decoder, GetInterface, SL_IID_PLAY, &decoderPlay);
  OpenSL(decoderPlay, SetCallbackEventsMask, SL_PLAYEVENT_HEADATEND);
  OpenSL(decoderPlay, RegisterCallback, DecodingEventCb, NULL);
  OpenSL(decoder, GetInterface, SL_IID_PREFETCHSTATUS, &decoderPrefetch);
  OpenSL(decoder, GetInterface, SL_IID_SEEK, &decoderSeek);
  OpenSL(decoder, GetInterface, SL_IID_METADATAEXTRACTION, &decoderMetadata);
  OpenSL(decoder, GetInterface, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
      &decoderQueue);

  // Initialize the callback structure, used during the decoding.
  CallbackContext callbackContext;
  {
    android::Mutex::Autolock autoLock(callbackLock_);
    callbackContext.pDataBase = pcmData;
    callbackContext.pData = pcmData;
    callbackContext.decoderMetadata = decoderMetadata;
    callbackContext.playItf = decoderPlay;
    RegisterCallbackContextAndAddEnqueueBuffersToDecoder(
        decoderQueue, &callbackContext);
  }

  // Initialize the callback for prefetch errors, if we can't open the
  // resource to decode.
  OpenSL(decoderPrefetch, SetCallbackEventsMask, kPrefetchErrorCandidate);
  OpenSL(decoderPrefetch, RegisterCallback, PrefetchEventCb, &decoderPrefetch);

  // Seek to the start position.
  OpenSL(decoderSeek, SetPosition, startPositionMillis_, SL_SEEKMODE_ACCURATE);

  // Start decoding immediately.
  OpenSL(decoderPlay, SetPlayState, SL_PLAYSTATE_PLAYING);

  // These variables hold the audio player and its output.
  // They will only be constructed once the decoder has invoked the callback,
  // and given us the correct sample rate, number of channels and duration.
  SLObjectItf outputMix = NULL;
  SLObjectItf audioPlayer = NULL;
  SLPlayItf audioPlayerPlay = NULL;
  SLAndroidSimpleBufferQueueItf audioPlayerQueue = NULL;

  // The main loop - until we're told to stop: if there is audio data coming
  // out of the decoder, feed it through the time scaler.
  // As it comes out of the time scaler, feed it into the audio player.
  while (!Finished()) {
    if (GetWasStartRequested() && HasSampleRateAndChannels()) {
      // Build the audio player.
      // TODO: What happens if I maliciously call start lots of times?
      floatBuffer_ = new float[targetFrames_ * mChannels];
      injectBuffer_ = new float[targetFrames_ * mChannels];
      OpenSL(engineInterface, CreateOutputMix, &outputMix, 0, NULL, NULL);
      OpenSL(outputMix, Realize, SL_BOOLEAN_FALSE);
      CreateAndRealizeAudioPlayer(GetSLSampleRate(), GetChannelCount(),
          GetSLChannels(), audioStreamType_, outputMix, audioPlayer,
          engineInterface);
      OpenSL(audioPlayer, GetInterface, SL_IID_PLAY, &audioPlayerPlay);
      OpenSL(audioPlayer, GetInterface, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
          &audioPlayerQueue);
      OpenSL(audioPlayerQueue, RegisterCallback, PlayingBufferQueueCb, NULL);
      ClearRequestStart();
      OpenSL(audioPlayerPlay, SetPlayState, SL_PLAYSTATE_PLAYING);
    }
    EnqueueMoreAudioIfNecessary(audioPlayerQueue);
    usleep(kSleepTimeMicros);
  }

  // Delete the audio player and output mix, iff they have been created.
  if (audioPlayer != NULL) {
    OpenSL(audioPlayerPlay, SetPlayState, SL_PLAYSTATE_STOPPED);
    OpenSL0(audioPlayerQueue, Clear);
    OpenSL(audioPlayerQueue, RegisterCallback, NULL, NULL);
    VoidOpenSL(audioPlayer, AbortAsyncOperation);
    VoidOpenSL(audioPlayer, Destroy);
    VoidOpenSL(outputMix, Destroy);
    audioPlayer = NULL;
    audioPlayerPlay = NULL;
    audioPlayerQueue = NULL;
    outputMix = NULL;
  }

  // Delete the decoder.
  OpenSL(decoderPlay, SetPlayState, SL_PLAYSTATE_STOPPED);
  OpenSL(decoderPrefetch, RegisterCallback, NULL, NULL);
  // This is returning slresult 13 if I do no playback.
  // Repro is to comment out all before this line, and all after enqueueing
  // my buffers.
  // OpenSL0(decoderQueue, Clear);
  OpenSL(decoderQueue, RegisterCallback, NULL, NULL);
  decoderSeek = NULL;
  decoderPrefetch = NULL;
  decoderQueue = NULL;
  OpenSL(decoderPlay, RegisterCallback, NULL, NULL);
  VoidOpenSL(decoder, AbortAsyncOperation);
  VoidOpenSL(decoder, Destroy);
  decoderPlay = NULL;

  // Delete the engine.
  VoidOpenSL(engine, Destroy);
  engineInterface = NULL;

  return true;
}

bool AudioEngine::Finished() {
  if (GetWasStopRequested()) {
    return true;
  }
  android::Mutex::Autolock autoLock(playBufferLock_);
  return playingBuffers_.size() <= 0 &&
      IsDecodeBufferEmpty() &&
      GetEndOfDecoderReached();
}

bool AudioEngine::GetWasStopRequested() {
  android::Mutex::Autolock autoLock(lock_);
  return stopRequested_;
}

bool AudioEngine::GetHasReachedPlayingBuffersLimit() {
  android::Mutex::Autolock autoLock(playBufferLock_);
  return playingBuffers_.size() >= maxPlayBufferCount_;
}

void AudioEngine::EnqueueMoreAudioIfNecessary(
    SLAndroidSimpleBufferQueueItf audioPlayerQueue) {
  bool keepEnqueueing = true;
  while (audioPlayerQueue != NULL &&
         !GetWasStopRequested() &&
         !IsDecodeBufferEmpty() &&
         !GetHasReachedPlayingBuffersLimit() &&
         keepEnqueueing) {
    keepEnqueueing = EnqueueNextBufferOfAudio(audioPlayerQueue);
  }
}

bool AudioEngine::DecodeBufferTooFull() {
  android::Mutex::Autolock autoLock(decodeBufferLock_);
  return decodeBuffer_.IsTooLarge();
}

// ****************************************************************************
// Code for handling the static callbacks.

void AudioEngine::PlayingBufferQueueCallback() {
  // The head playing buffer is done, move it to the free list.
  android::Mutex::Autolock autoLock(playBufferLock_);
  if (playingBuffers_.size() > 0) {
    freeBuffers_.push(playingBuffers_.front());
    playingBuffers_.pop();
  }
}

void AudioEngine::PrefetchEventCallback(
    SLPrefetchStatusItf caller, SLuint32 event) {
  // If there was a problem during decoding, then signal the end.
  SLpermille level = 0;
  SLuint32 status;
  OpenSL(caller, GetFillLevel, &level);
  OpenSL(caller, GetPrefetchStatus, &status);
  if ((kPrefetchErrorCandidate == (event & kPrefetchErrorCandidate)) &&
      (level == 0) &&
      (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
    LOGI("prefetcheventcallback error while prefetching data");
    SetEndOfDecoderReached();
  }
  if (SL_PREFETCHSTATUS_SUFFICIENTDATA == event) {
    // android::Mutex::Autolock autoLock(prefetchLock_);
    // prefetchCondition_.broadcast();
  }
}

void AudioEngine::DecodingBufferQueueCallback(
    SLAndroidSimpleBufferQueueItf queueItf, void *context) {
  if (GetWasStopRequested()) {
    return;
  }

  CallbackContext *pCntxt;
  {
    android::Mutex::Autolock autoLock(callbackLock_);
    pCntxt = reinterpret_cast<CallbackContext*>(context);
  }
  {
    android::Mutex::Autolock autoLock(decodeBufferLock_);
    decodeBuffer_.AddData(pCntxt->pData, kBufferSizeInBytes);
  }

  if (!HasSampleRateAndChannels()) {
    android::Mutex::Autolock autoLock(callbackLock_);
    ReadSampleRateAndChannelCount(pCntxt, &mSampleRate, &mChannels);
  }

  {
    android::Mutex::Autolock autoLock(lock_);
    if (totalDurationMs_ == 0) {
      totalDurationMs_ = ReadDuration(pCntxt->playItf);
    }
    // TODO: This isn't working, it always reports zero.
    // ReadPosition(pCntxt->playItf);
  }

  OpenSL(queueItf, Enqueue, pCntxt->pData, kBufferSizeInBytes);

  // Increase data pointer by buffer size
  pCntxt->pData += kBufferSizeInBytes;
  if (pCntxt->pData >= pCntxt->pDataBase +
      (kNumberOfBuffersInQueue * kBufferSizeInBytes)) {
    pCntxt->pData = pCntxt->pDataBase;
  }

  // If we get too much data into the decoder,
  // sleep until the playback catches up.
  while (!GetWasStopRequested() && DecodeBufferTooFull()) {
    usleep(kSleepTimeMicros);
  }
}

void AudioEngine::DecodingEventCallback(SLPlayItf, SLuint32 event) {
  if (SL_PLAYEVENT_HEADATEND & event) {
    SetEndOfDecoderReached();
  }
}
