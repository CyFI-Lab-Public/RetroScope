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

// Test dynamic interface management

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include <SLES/OpenSLES.h>
#ifdef ANDROID
#include <SLES/OpenSLES_Android.h>
#endif

int main(int argc, char **argv)
{
    if (argc != 1) {
        fprintf(stderr, "usage: %s\n", argv[0]);
        return EXIT_FAILURE;
    }

    SLresult result;
    SLObjectItf engineObject;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    SLEngineItf engineEngine;
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);

    // create output mix
    SLObjectItf outputMixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);

    // get the dynamic interface management interface for output mix, before realize
    SLDynamicInterfaceManagementItf outputMixDIM;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_DYNAMICINTERFACEMANAGEMENT,
            &outputMixDIM);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);
    assert(NULL == outputMixDIM);

    // realize output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // get the dynamic interface management interface for output mix, after realize
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_DYNAMICINTERFACEMANAGEMENT,
            &outputMixDIM);
    assert(SL_RESULT_SUCCESS == result);
    assert(NULL != outputMixDIM);

    // register callback
    result = (*outputMixDIM)->RegisterCallback(outputMixDIM, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);

    // get environmental reverb interface, before add or resume
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
            &outputMixEnvironmentalReverb);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == outputMixEnvironmentalReverb);

    // resume environmental reverb interface
    result = (*outputMixDIM)->ResumeInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB,
            SL_BOOLEAN_FALSE);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);

    // get environmental reverb interface, after resume but before add
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
            &outputMixEnvironmentalReverb);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == outputMixEnvironmentalReverb);

    // add environmental reverb interface
    result = (*outputMixDIM)->AddInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB,
            SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // get environmental reverb interface, after add
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
            &outputMixEnvironmentalReverb);
    assert(SL_RESULT_SUCCESS == result);
    assert(NULL != outputMixEnvironmentalReverb);

    // add environmental reverb interface again
    result = (*outputMixDIM)->AddInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB,
            SL_BOOLEAN_FALSE);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);

    // resume environmental reverb interface
    result = (*outputMixDIM)->ResumeInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB,
            SL_BOOLEAN_FALSE);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);

    // remove environmental reverb interface (FIXME not yet implemented)
    result = (*outputMixDIM)->RemoveInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB);
    assert((SL_RESULT_SUCCESS == result) || (SL_RESULT_FEATURE_UNSUPPORTED == result));

    // FIXME once remove is implemented we can try this
    if (SL_RESULT_SUCCESS == result) {
        result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                &outputMixEnvironmentalReverb);
        assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);
        assert(NULL == outputMixEnvironmentalReverb);
        result = (*outputMixDIM)->RemoveInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB);
        assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);
        result = (*outputMixDIM)->AddInterface(outputMixDIM, SL_IID_ENVIRONMENTALREVERB,
                SL_BOOLEAN_FALSE);
        assert(SL_RESULT_SUCCESS == result);
        result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                &outputMixEnvironmentalReverb);
        assert(SL_RESULT_SUCCESS == result);
        assert(NULL != outputMixEnvironmentalReverb);
    }

    // get non-sensical play interface, before add
    SLPlayItf outputMixPlay;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_PLAY, &outputMixPlay);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == outputMixPlay);

    // add play interface
    result = (*outputMixDIM)->AddInterface(outputMixDIM, SL_IID_PLAY, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);

    // get play interface should still fail
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_PLAY, &outputMixPlay);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == outputMixPlay);

    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);

    // destroy engine
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
