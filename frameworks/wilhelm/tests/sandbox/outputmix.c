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

// output mix interface tests

#include <SLES/OpenSLES.h>
#include "OpenSLESUT.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv)
{
    // create engine
    SLObjectItf engineObject;
    SLresult result;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    printf("Engine object %p\n", engineObject);
    // realize engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    // get engine interface
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);
    // query supported interfaces on output mix object ID and display their IDs
    SLuint32 numSupportedInterfaces;
    result = (*engineEngine)->QueryNumSupportedInterfaces(engineEngine, SL_OBJECTID_OUTPUTMIX,
            &numSupportedInterfaces);
    assert(SL_RESULT_SUCCESS == result);
    printf("Output mix supports %u interfaces:\n", numSupportedInterfaces);
    SLuint32 i;
    for (i = 0; i < numSupportedInterfaces; ++i) {
        SLInterfaceID interfaceID;
        result = (*engineEngine)->QuerySupportedInterfaces(engineEngine, SL_OBJECTID_OUTPUTMIX, i,
                &interfaceID);
        assert(SL_RESULT_SUCCESS == result);
        printf(" [%u] = ", i);
        slesutPrintIID(interfaceID);
    }
    // create output mix, with no place to put the new object
    result = (*engineEngine)->CreateOutputMix(engineEngine, NULL, 0, NULL, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    // create output mix, requesting no explicit interfaces
    SLObjectItf outputMixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    printf("Output mix object %p\n", outputMixObject);
    // get object interface before realization
    SLObjectItf outputMixObject2;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OBJECT, &outputMixObject2);
    assert(SL_RESULT_SUCCESS == result);
    assert(outputMixObject2 == outputMixObject);
    // get any other interface before realization should fail
    SLOutputMixItf outputMixOutputMix;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OUTPUTMIX,
            &outputMixOutputMix);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);
    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    // get each expected implicit interface
    outputMixObject2 = NULL;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OBJECT, &outputMixObject2);
    assert(SL_RESULT_SUCCESS == result);
    assert(outputMixObject2 == outputMixObject);
    SLDynamicInterfaceManagementItf outputMixDynamicInterfaceManagement;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_DYNAMICINTERFACEMANAGEMENT,
            &outputMixDynamicInterfaceManagement);
    assert((SL_RESULT_SUCCESS == result) || (SL_RESULT_FEATURE_UNSUPPORTED) == result);
    if (SL_RESULT_SUCCESS == result) {
        printf("Output mix supports dynamic interface management\n");
    } else {
        printf("Output mix does not support dynamic interface management\n");
    }
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OUTPUTMIX,
            &outputMixOutputMix);
    assert(SL_RESULT_SUCCESS == result);
    // get explicit and optional interfaces should fail since not requested at creation
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
            &outputMixEnvironmentalReverb);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLEqualizerItf outputMixEqualizer;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_EQUALIZER,
            &outputMixEqualizer);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLPresetReverbItf outputMixPresetReverb;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_PRESETREVERB,
            &outputMixPresetReverb);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLVirtualizerItf outputMixVirtualizer;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VIRTUALIZER,
            &outputMixVirtualizer);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLVolumeItf outputMixVolume;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VOLUME,
            &outputMixVolume);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLBassBoostItf outputMixBassBoost;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_BASSBOOST,
            &outputMixBassBoost);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    SLVisualizationItf outputMixVisualization;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VISUALIZATION,
            &outputMixVisualization);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);
    // re-create output mix, this time requesting implicit interfaces as "hard" requirements (must
    // be there), and explicit interfaces as "soft" requirements (OK if not available)
    SLInterfaceID ids[10] = {SL_IID_OBJECT, SL_IID_DYNAMICINTERFACEMANAGEMENT, SL_IID_OUTPUTMIX,
            SL_IID_ENVIRONMENTALREVERB, SL_IID_EQUALIZER, SL_IID_PRESETREVERB, SL_IID_VIRTUALIZER,
            SL_IID_VOLUME, SL_IID_BASSBOOST, SL_IID_VISUALIZATION};
    SLboolean req[10] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_FALSE /*SL_BOOLEAN_TRUE*/, SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE/*FALSE*/, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE,
            SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 10, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    printf("Output mix object %p\n", outputMixObject);
    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    // get implicit interfaces
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OBJECT,
            &outputMixObject2);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_DYNAMICINTERFACEMANAGEMENT,
            &outputMixDynamicInterfaceManagement);
    assert((SL_RESULT_SUCCESS == result) || (SL_RESULT_FEATURE_UNSUPPORTED) == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_OUTPUTMIX,
            &outputMixOutputMix);
    assert(SL_RESULT_SUCCESS == result);
    // get explicit and optional interfaces should succeed since they were requested at creation
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
            &outputMixEnvironmentalReverb);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_EQUALIZER,
            &outputMixEqualizer);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_PRESETREVERB,
            &outputMixPresetReverb);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VIRTUALIZER,
            &outputMixVirtualizer);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VOLUME,
            &outputMixVolume);
    assert((SL_RESULT_SUCCESS == result) || (SL_RESULT_FEATURE_UNSUPPORTED) == result);
    if (SL_RESULT_SUCCESS == result) {
        printf("Output mix supports volume\n");
    } else {
        printf("Output mix does not support volume\n");
    }
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_BASSBOOST,
            &outputMixBassBoost);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_VISUALIZATION,
            &outputMixVisualization);
    assert((SL_RESULT_SUCCESS == result) || (SL_RESULT_FEATURE_UNSUPPORTED) == result);
    if (SL_RESULT_SUCCESS == result) {
        printf("Output mix supports visualization\n");
    } else {
        printf("Output mix does not support visualization\n");
    }
    // use the OutputMix interface on output mix object, in order to get code coverage
    SLint32 numDevices = 1;
    SLuint32 deviceIDs[1];
    result = (*outputMixOutputMix)->GetDestinationOutputDeviceIDs(outputMixOutputMix, &numDevices,
            deviceIDs);
    assert(SL_RESULT_SUCCESS == result);
    assert(1 == numDevices);
    assert(SL_DEFAULTDEVICEID_AUDIOOUTPUT == deviceIDs[0]);
    result = (*outputMixOutputMix)->RegisterDeviceChangeCallback(outputMixOutputMix, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixOutputMix)->ReRoute(outputMixOutputMix, 1, deviceIDs);
    assert(SL_RESULT_SUCCESS == result);
    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);
    // destroy engine
    (*engineObject)->Destroy(engineObject);
    return EXIT_SUCCESS;
}
