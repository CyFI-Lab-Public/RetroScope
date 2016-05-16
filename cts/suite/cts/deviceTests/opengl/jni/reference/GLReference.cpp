/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
#include <jni.h>

#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <graphics/GLUtils.h>
#include <graphics/Renderer.h>

#include "ReferenceRenderer.h"

extern "C" JNIEXPORT jboolean JNICALL
Java_com_android_cts_opengl_reference_GLGameActivity_startBenchmark(
        JNIEnv* env, jclass clazz, jobject assetManager, jobject surface, jint numFrames,
        jdoubleArray setUpTimes, jdoubleArray updateTimes, jdoubleArray renderTimes) {

    GLUtils::setEnvAndAssetManager(env, assetManager);

    if (numFrames > (ReferenceRenderer::FRAMES_PER_SCENE * ReferenceRenderer::NUM_SCENES)) {
        return false;
    }

    ReferenceRenderer* renderer = new ReferenceRenderer(ANativeWindow_fromSurface(env, surface));

    bool success = renderer->setUp();
    env->SetDoubleArrayRegion(
            setUpTimes, 0, ReferenceRenderer::NUM_SETUP_TIMES, renderer->mSetUpTimes);

    double updates[numFrames];
    double renders[numFrames];
    for (int i = 0; i < numFrames && success; i++) {
        double t0 = GLUtils::currentTimeMillis();
        success = renderer->update(i);
        double t1 = GLUtils::currentTimeMillis();
        success = success && renderer->draw();
        double t2 = GLUtils::currentTimeMillis();
        updates[i] = t1 - t0;
        renders[i] = t2 - t1;
    }

    env->SetDoubleArrayRegion(updateTimes, 0, numFrames, updates);
    env->SetDoubleArrayRegion(renderTimes, 0, numFrames, renders);

    success = renderer->tearDown() && success;
    delete renderer;
    renderer = NULL;
    return success;
}
