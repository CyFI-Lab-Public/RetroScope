/*
 * Copyright (C) 2010 The Android Open Source Project
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

#include <cutils/memory.h>

#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

using namespace android;

namespace android {

int main(int argc, char** argv)
{
    // set up the thread-pool
    sp<ProcessState> proc(ProcessState::self());
    ProcessState::self()->startThreadPool();

    // create a client to surfaceflinger
    sp<SurfaceComposerClient> client = new SurfaceComposerClient();
    
    sp<SurfaceControl> surfaceControl = client->createSurface(String8("resize"),
            160, 240, PIXEL_FORMAT_RGB_565, 0);

    sp<Surface> surface = surfaceControl->getSurface();

    SurfaceComposerClient::openGlobalTransaction();
    surfaceControl->setLayer(100000);
    SurfaceComposerClient::closeGlobalTransaction();

    ANativeWindow_Buffer outBuffer;
    surface->lock(&outBuffer, NULL);
    ssize_t bpr = outBuffer.stride * bytesPerPixel(outBuffer.format);
    android_memset16((uint16_t*)outBuffer.bits, 0xF800, bpr*outBuffer.height);
    surface->unlockAndPost();

    surface->lock(&outBuffer);
    android_memset16((uint16_t*)outBuffer.bits, 0x07E0, bpr*outBuffer.height);
    surface->unlockAndPost();

    SurfaceComposerClient::openGlobalTransaction();
    surfaceControl->setSize(320, 240);
    SurfaceComposerClient::closeGlobalTransaction();

    
    IPCThreadState::self()->joinThreadPool();
    
    return 0;
}
