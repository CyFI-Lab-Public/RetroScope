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

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <SLES/OpenSLES.h>
#include "OpenSLESUT.h"

int main(int arg, char **argv)
{
    SLresult result;

    printf("Create engine\n");
    SLObjectItf engineObject;
    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);
    SLuint32 i;
    // loop through both valid and invalid object IDs
    SLuint32 objectID;
    // Test object IDs from one less than the first valid object
    // ID, up to one more than the last valid object ID. This way
    // we can test for both valid and invalid object IDs at both
    // ends. If more objects are added, be sure to update the macros.
#define FIRST_VALID SL_OBJECTID_ENGINE
#define LAST_VALID  SL_OBJECTID_METADATAEXTRACTOR
    for (objectID = FIRST_VALID - 1; objectID <= LAST_VALID + 1; ++objectID) {
        printf("object ID %x", objectID);
        const char *string = slesutObjectIDToString(objectID);
        if (NULL != string)
            printf(" (%s)", string);
        printf(":\n");
        result = (*engineEngine)->QueryNumSupportedInterfaces(engineEngine, objectID, NULL);
        assert(SL_RESULT_PARAMETER_INVALID == result);
        SLuint32 numSupportedInterfaces = 12345;
        result = (*engineEngine)->QueryNumSupportedInterfaces(engineEngine, objectID,
                &numSupportedInterfaces);
        SLInterfaceID interfaceID;
        if (SL_RESULT_FEATURE_UNSUPPORTED == result) {
            printf("  unsupported\n");
            result = (*engineEngine)->QuerySupportedInterfaces(engineEngine, objectID, 0,
                &interfaceID);
            assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
            assert(NULL == interfaceID);
            continue;
        }
        assert(SL_RESULT_SUCCESS == result);
        printf("numSupportedInterfaces %u\n", numSupportedInterfaces);
        for (i = 0; i < numSupportedInterfaces + 1; ++i) {
            result = (*engineEngine)->QuerySupportedInterfaces(engineEngine, objectID, i, NULL);
            assert(SL_RESULT_PARAMETER_INVALID == result);
            result = (*engineEngine)->QuerySupportedInterfaces(engineEngine, objectID, i,
                    &interfaceID);
            if (i < numSupportedInterfaces) {
                assert(SL_RESULT_SUCCESS == result);
                printf("    interface %u ", i);
                slesutPrintIID(interfaceID);
            } else {
                assert(SL_RESULT_PARAMETER_INVALID == result);
            }
        }
    }
    // query number of extensions
    result = (*engineEngine)->QueryNumSupportedExtensions(engineEngine, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    SLuint32 numExtensions = 0x12345;
    result = (*engineEngine)->QueryNumSupportedExtensions(engineEngine, &numExtensions);
    assert(SL_RESULT_SUCCESS == result);
    printf("numExtensions = %u\n", numExtensions);
    // query names of the extensions
    for (i = 0; i < numExtensions + 1; ++i) {
        SLchar extensionName[32];
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, extensionName, NULL);
        assert(SL_RESULT_PARAMETER_INVALID == result);
        SLint16 nameLength = -1;
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, NULL, &nameLength);
        if (i < numExtensions) {
            assert(SL_RESULT_SUCCESS == result);
            printf("    extension[%u] length = %u\n", i, nameLength);
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
            assert(0 == nameLength);
        }
        memset(extensionName, 'X', sizeof(extensionName));
        nameLength = -1;
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, extensionName,
                &nameLength);
        if (i < numExtensions) {
            assert(SL_RESULT_BUFFER_INSUFFICIENT == result);
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
        }
        assert('X' == extensionName[0]);
        nameLength = 0;
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, extensionName,
                &nameLength);
        if (i < numExtensions) {
            assert(SL_RESULT_BUFFER_INSUFFICIENT == result);
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
        }
        assert('X' == extensionName[0]);
        nameLength = 1;
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, extensionName,
                &nameLength);
        if (i < numExtensions) {
            assert(SL_RESULT_BUFFER_INSUFFICIENT == result);
            assert('\0' == extensionName[0]);
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
        }
        assert('X' == extensionName[1]);
        nameLength = sizeof(extensionName);
        result = (*engineEngine)->QuerySupportedExtension(engineEngine, i, extensionName,
                &nameLength);
        if (i < numExtensions) {
            assert(SL_RESULT_SUCCESS == result);
            assert((1 <= nameLength) && (nameLength <= (SLint16) sizeof(extensionName)));
            printf("    extension[%u] = \"%.*s\"\n", i, nameLength, extensionName);
        } else {
            assert(SL_RESULT_PARAMETER_INVALID == result);
            assert(0 == nameLength);
        }
    }
    // check if extension is supported
    SLboolean isSupported = SL_BOOLEAN_TRUE;
    result = (*engineEngine)->IsExtensionSupported(engineEngine, NULL, &isSupported);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(SL_BOOLEAN_FALSE == isSupported);
    SLchar *unsupportedExt = (SLchar *) "fish";
    result = (*engineEngine)->IsExtensionSupported(engineEngine, unsupportedExt, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    isSupported = SL_BOOLEAN_TRUE;
    result = (*engineEngine)->IsExtensionSupported(engineEngine, unsupportedExt, &isSupported);
    assert(SL_RESULT_SUCCESS == result);
    assert(SL_BOOLEAN_FALSE == isSupported);
    SLchar *supportedExt;
#ifdef ANDROID
    // whereas the implementation uses PLATFORM_SDK_VERSION, use a hard-coded value here
    // so that we're actually testing for a particular expected value
    supportedExt = (SLchar *) "ANDROID_SDK_LEVEL_13";
#else
    supportedExt = (SLchar *) "WILHELM_DESKTOP";
#endif
    isSupported = SL_BOOLEAN_FALSE;
    result = (*engineEngine)->IsExtensionSupported(engineEngine, supportedExt, &isSupported);
    assert(SL_RESULT_SUCCESS == result);
    assert(SL_BOOLEAN_TRUE == isSupported);
    // create an extension object with no place to put the new object
    result = (*engineEngine)->CreateExtensionObject(engineEngine, NULL, NULL, 0x123, 0, NULL, NULL);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    // create an extension object, which is unsupported
    SLObjectItf extensionObject;
    result = (*engineEngine)->CreateExtensionObject(engineEngine, &extensionObject, NULL, 0x123, 0,
            NULL, NULL);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == extensionObject);
    // destroy engine
    (*engineObject)->Destroy(engineObject);
    return EXIT_SUCCESS;
}
