/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef JNIDEFINES_H
#define JNIDEFINES_H


#include <jni.h>
#include <string.h>
#include <android/log.h>

#define LOGV(msg...) __android_log_print(ANDROID_LOG_VERBOSE, "Native_JPEGStream", msg)
#define LOGE(msg...) __android_log_print(ANDROID_LOG_ERROR, "Native_JPEGStream", msg)
#define LOGW(msg...) __android_log_print(ANDROID_LOG_WARN, "Native_JPEGStream", msg)

#endif // JNIDEFINES_H
