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

#include <SLES/OpenSLES.h>
#include "OpenSLESUT.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv)
{
    printf("Get number of available engine interfaces\n");
    SLresult result;
    SLuint32 numSupportedInterfaces = 12345;
    result = slQueryNumSupportedEngineInterfaces(&numSupportedInterfaces);
    assert(SL_RESULT_SUCCESS == result);
    result = slQueryNumSupportedEngineInterfaces(NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);

    printf("Engine number of supported interfaces %u\n", numSupportedInterfaces);
    SLInterfaceID *engine_ids = calloc(numSupportedInterfaces+1, sizeof(SLInterfaceID));
    assert(engine_ids != NULL);
    SLboolean *engine_req = calloc(numSupportedInterfaces+1, sizeof(SLboolean));
    assert(engine_req != NULL);

    printf("Display the ID of each available interface\n");
    SLuint32 index;
    for (index = 0; index < numSupportedInterfaces + 1; ++index) {
        SLInterfaceID interfaceID;
        memset(&interfaceID, 0x55, sizeof(interfaceID));
        result = slQuerySupportedEngineInterfaces(index, &interfaceID);
        if (index < numSupportedInterfaces) {
            assert(SL_RESULT_SUCCESS == result);
            printf("interface[%u] ", index);
            slesutPrintIID(interfaceID);
            engine_ids[index] = interfaceID;
            engine_req[index] = SL_BOOLEAN_TRUE;
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
        }
        result = slQuerySupportedEngineInterfaces(index, NULL);
        assert(SL_RESULT_PARAMETER_INVALID == result);
    }

    printf("Create an engine and request all available interfaces\n");
    SLObjectItf engineObject;
    if (0 < numSupportedInterfaces) {
        printf("Create engine with numSupportedInterfaces > 0 but NULL pointers\n");
        result = slCreateEngine(&engineObject, 0, NULL, numSupportedInterfaces, engine_ids, NULL);
        assert(SL_RESULT_PARAMETER_INVALID == result);
        assert(NULL == engineObject);
        result = slCreateEngine(&engineObject, 0, NULL, numSupportedInterfaces, NULL, engine_req);
        assert(SL_RESULT_PARAMETER_INVALID == result);
        assert(NULL == engineObject);
    }

    printf("Create engine with no place to return the new engine object\n");
    result = slCreateEngine(NULL, 0, NULL, numSupportedInterfaces, engine_ids, engine_req);
    assert(SL_RESULT_PARAMETER_INVALID == result);

    printf("Create engine with NULL interface pointer\n");
    SLInterfaceID null_id[1] = {NULL};
    SLboolean null_req[1] = {SL_BOOLEAN_FALSE};
    result = slCreateEngine(&engineObject, 0, NULL, 1, null_id, null_req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == engineObject);

    printf("Create an engine with numOptions > 0 but NULL pointer\n");
    result = slCreateEngine(&engineObject, 1, NULL, 0, NULL, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == engineObject);
    SLEngineOption options[2];
    options[0].feature = 0x12345;
    options[0].data = 0;

    printf("Create engine with non-sensical option\n");
    result = slCreateEngine(&engineObject, 1, options, 0, NULL, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == engineObject);

    printf("Create an engine and require non-sensical volume interface\n");
    engine_ids[numSupportedInterfaces] = SL_IID_VOLUME;
    engine_req[numSupportedInterfaces] = SL_BOOLEAN_TRUE;
    result = slCreateEngine(&engineObject, 0, NULL, numSupportedInterfaces+1, engine_ids,
            engine_req);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == engineObject);

    printf("Create an engine and politely request a non-sensical interface with options\n");
    engine_req[numSupportedInterfaces] = SL_BOOLEAN_FALSE;
    options[0].feature = SL_ENGINEOPTION_THREADSAFE;
    options[0].data = (SLuint32) SL_BOOLEAN_TRUE;
    options[1].feature = SL_ENGINEOPTION_LOSSOFCONTROL;
    options[1].data = (SLuint32) SL_BOOLEAN_FALSE;
    result = slCreateEngine(&engineObject, 2, options, numSupportedInterfaces+1, engine_ids,
            engine_req);
    assert(SL_RESULT_SUCCESS == result);
    printf("Engine object %p\n", engineObject);

    printf("Get each available interface before realization\n");
    for (index = 0; index < numSupportedInterfaces; ++index) {
        void *interface = NULL;
        // Use the interface ID as returned by slQuerySupportedEngineInterfaces
        result = (*engineObject)->GetInterface(engineObject, engine_ids[index], &interface);
        assert(SL_RESULT_SUCCESS == result || SL_RESULT_PRECONDITIONS_VIOLATED == result);
        if (SL_RESULT_SUCCESS == result) {
            printf("interface available pre-realize: ");
            slesutPrintIID(engine_ids[index]);
        }
    }

    printf("Destroy engine before realization\n");
    (*engineObject)->Destroy(engineObject);

    printf("Create engine again\n");
    result = slCreateEngine(&engineObject, 0, NULL, numSupportedInterfaces, engine_ids, engine_req);
    assert(SL_RESULT_SUCCESS == result);
    printf("Engine object %p\n", engineObject);

    printf("Realize the engine\n");
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    printf("Get each available interface after realization\n");
    for (index = 0; index < numSupportedInterfaces; ++index) {
        void *interface = NULL;
        result = (*engineObject)->GetInterface(engineObject, engine_ids[index], &interface);
        assert(SL_RESULT_SUCCESS == result);
        printf("interface[%u] %p\n", index, interface);
        // Use a copy of the interface ID to make sure lookup is not purely relying on address
        void *interface_again = NULL;
        struct SLInterfaceID_ copy = *engine_ids[index];
        result = (*engineObject)->GetInterface(engineObject, &copy, &interface_again);
        assert(SL_RESULT_SUCCESS == result);
        // Calling GetInterface multiple times should return the same interface
        assert(interface_again == interface);
    }

    SLObjectItf engineObject2;
#if 0
    printf("Create too many engines\n");
    result = slCreateEngine(&engineObject2, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_RESOURCE_ERROR == result);
    assert(NULL == engineObject2);
#endif

    printf("Destroy engine\n");
    (*engineObject)->Destroy(engineObject);

    printf("Now should be able to create another engine\n");
    result = slCreateEngine(&engineObject2, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);

    printf("Exit without destroying engine -- examine log for expected error message\n");
    free(engine_ids);
    free(engine_req);
    return EXIT_SUCCESS;
}
