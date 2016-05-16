/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef COM_ANDROID_GALLERY3D_PHOTOEDITOR_JNI_EGL_FENSE_H
#define COM_ANDROID_GALLERY3D_PHOTOEDITOR_JNI_EGL_FENSE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_android_gallery3d_photoeditor_FilterStack_nativeEglSetFenceAndWait(JNIEnv* env,
                                                                            jobject thiz);
#ifdef __cplusplus
}
#endif

#endif  /* COM_ANDROID_GALLERY3D_PHOTOEDITOR_JNI_EGL_FENSE_H */
