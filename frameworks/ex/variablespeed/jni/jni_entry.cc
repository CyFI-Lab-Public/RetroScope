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

#include <stdlib.h>
#include <assert.h>

#include <jni.h>
#include <variablespeed.h>

// Quick #define to make sure I get all the JNI method calls right.
#define JNI_METHOD(x, y) \
JNIEXPORT y JNICALL \
Java_com_android_ex_variablespeed_VariableSpeedNative_##x

class MethodLog {
 public:
  explicit MethodLog(const char* name) : name_(name) {
    LOGV("+ %s", name);
  }
  virtual ~MethodLog() {
    LOGV("- %s", name_);
  }

 private:
  const char* name_;
};

extern "C" {
JNI_METHOD(playFileDescriptor, void) (JNIEnv*, jclass, int fd, jlong offset,
    jlong length) {
  MethodLog _("playFileDescriptor");
  AudioEngine::GetEngine()->PlayFileDescriptor(fd, offset, length);
}

JNI_METHOD(playUri, void) (JNIEnv* env, jclass, jstring uri) {
  MethodLog _("playUri");
  const char* utf8 = env->GetStringUTFChars(uri, NULL);
  CHECK(NULL != utf8);
  AudioEngine::GetEngine()->PlayUri(utf8);
}

JNI_METHOD(setVariableSpeed, void) (JNIEnv*, jclass, float speed) {
  MethodLog _("setVariableSpeed");
  AudioEngine::GetEngine()->SetVariableSpeed(speed);
}

JNI_METHOD(startPlayback, void) (JNIEnv*, jclass) {
  MethodLog _("startPlayback");
  AudioEngine::GetEngine()->RequestStart();
}

JNI_METHOD(stopPlayback, void) (JNIEnv*, jclass) {
  MethodLog _("stopPlayback");
  AudioEngine::GetEngine()->RequestStop();
}

JNI_METHOD(getCurrentPosition, int) (JNIEnv*, jclass) {
  return AudioEngine::GetEngine()->GetCurrentPosition();
}

JNI_METHOD(getTotalDuration, int) (JNIEnv*, jclass) {
  return AudioEngine::GetEngine()->GetTotalDuration();
}

JNI_METHOD(initializeEngine, void) (JNIEnv*, jclass,
    int targetFrames, float windowDuration,
    float windowOverlapDuration, size_t maxPlayBufferCount,
    float initialRate, size_t decodeInitialSize, size_t decodeMaxSize,
    size_t startPositionMillis, int audioStreamType) {
  MethodLog _("initializeEngine");
  AudioEngine::SetEngine(new AudioEngine(targetFrames,
      windowDuration, windowOverlapDuration, maxPlayBufferCount, initialRate,
      decodeInitialSize, decodeMaxSize, startPositionMillis, audioStreamType));
}

JNI_METHOD(shutdownEngine, void) (JNIEnv*, jclass) {
  MethodLog _("shutdownEngine");
  AudioEngine::DeleteEngine();
}
}  // extern "C"
