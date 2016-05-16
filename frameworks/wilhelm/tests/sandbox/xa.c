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

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <OMXAL/OpenMAXAL.h>
#include <OMXAL/OpenMAXAL_Android.h>

int main(int argc, char **argv)
{
    XAresult result;
    XAObjectItf engineObject;
    printf("xaCreateEngine\n");
    result = xaCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    printf("result = %d\n", result);
    assert(XA_RESULT_SUCCESS == result);
    printf("engineObject = %p\n", engineObject);
    printf("realize\n");
    result = (*engineObject)->Realize(engineObject, XA_BOOLEAN_FALSE);
    printf("result = %d\n", result);
    printf("GetInterface for ENGINE\n");
    XAEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, XA_IID_ENGINE, &engineEngine);
    printf("result = %d\n", result);
    printf("engineEngine = %p\n", engineEngine);
    assert(XA_RESULT_SUCCESS == result);

    XAObjectItf outputMixObject;
    printf("CreateOutputMix");
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    printf("result = %d, outputMixObject=%p\n", result, outputMixObject);
    result = (*outputMixObject)->Realize(outputMixObject, XA_BOOLEAN_FALSE);
    printf("result = %d\n", result);

    XAObjectItf deviceObject;
    printf("CreateCameraDevice\n");
    result = (*engineEngine)->CreateCameraDevice(engineEngine, &deviceObject,
            XA_DEFAULTDEVICEID_CAMERA, 0, NULL, NULL);
    printf("result = %d, deviceObject=%p\n", result, deviceObject);

    printf("CreateRadioDevice\n");
    result = (*engineEngine)->CreateRadioDevice(engineEngine, &deviceObject, 0, NULL, NULL);
    printf("result = %d, deviceObject=%p\n", result, deviceObject);

    printf("CreateLEDDevice\n");
    result = (*engineEngine)->CreateLEDDevice(engineEngine, &deviceObject, XA_DEFAULTDEVICEID_LED,
            0, NULL, NULL);
    printf("result = %d, deviceObject=%p\n", result, deviceObject);

    printf("CreateVibraDevice\n");
    result = (*engineEngine)->CreateVibraDevice(engineEngine, &deviceObject,
            XA_DEFAULTDEVICEID_VIBRA, 0, NULL, NULL);
    printf("result = %d, deviceObject=%p\n", result, deviceObject);

    printf("CreateMediaPlayer\n");
    XAObjectItf playerObject;
#if 1
    XADataLocator_AndroidBufferQueue locABQ;
    memset(&locABQ, 0, sizeof(locABQ));
    locABQ.locatorType = XA_DATALOCATOR_ANDROIDBUFFERQUEUE;
#else
    XADataLocator_URI locUri;
    locUri.locatorType = XA_DATALOCATOR_URI;
    locUri.URI = (XAchar *) "/sdcard/hello.wav";
#endif
    XADataFormat_MIME fmtMime;
    fmtMime.formatType = XA_DATAFORMAT_MIME;
    fmtMime.mimeType = NULL;
    fmtMime.containerType = XA_CONTAINERTYPE_UNSPECIFIED;
    XADataSource dataSrc;
#if 1
    dataSrc.pLocator = &locABQ;
#else
    dataSrc.pLocator = &locUri;
#endif
    dataSrc.pFormat = &fmtMime;
    XADataSink audioSnk;
    XADataLocator_OutputMix locOM;
    locOM.locatorType = XA_DATALOCATOR_OUTPUTMIX;
    locOM.outputMix = outputMixObject;
    audioSnk.pLocator = &locOM;
    audioSnk.pFormat = NULL;
    XADataLocator_NativeDisplay locND;
    locND.locatorType = XA_DATALOCATOR_NATIVEDISPLAY;
    locND.hWindow = NULL;
    locND.hDisplay = NULL;
    XADataSink imageVideoSink;
    imageVideoSink.pLocator = &locND;
    imageVideoSink.pFormat = NULL;
    result = (*engineEngine)->CreateMediaPlayer(engineEngine, &playerObject, &dataSrc, NULL,
            &audioSnk, &imageVideoSink, NULL, NULL, 0, NULL, NULL);
    printf("result = %d, playerObject=%p\n", result, playerObject);
    result = (*playerObject)->Realize(playerObject, XA_BOOLEAN_FALSE);
    printf("result = %d\n", result);

    printf("GetInterface for PLAY\n");
    XAPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, XA_IID_PLAY, &playerPlay);
    printf("result = %d\n", result);
    printf("playerPlay = %p\n", playerPlay);
    assert(XA_RESULT_SUCCESS == result);

    printf("SetPlayState to PLAYING\n");
    result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PLAYING);
    printf("result = %d\n", result);
    assert(XA_RESULT_SUCCESS == result);

    printf("destroying media player\n");
    (*playerObject)->Destroy(playerObject);

    printf("destroying output mix\n");
    (*outputMixObject)->Destroy(outputMixObject);

    printf("destroying engine\n");
    (*engineObject)->Destroy(engineObject);
    printf("exit\n");

    return EXIT_SUCCESS;
}
