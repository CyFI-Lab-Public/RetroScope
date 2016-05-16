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

// Dual engine test

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <OMXAL/OpenMAXAL.h>

int main(int argc, char **argv)
{
    XAresult xaResult;
    XAObjectItf xaEngineObject;

    SLresult slResult;
    SLObjectItf slEngineObject;

    printf("xaCreateEngine\n");
    xaResult = xaCreateEngine(&xaEngineObject, 0, NULL, 0, NULL, NULL);
    printf("xaResult = %d\n", xaResult);
    assert(XA_RESULT_SUCCESS == xaResult);
    printf("xaEngineObject = %p\n", xaEngineObject);

    printf("realize xaEngineObject\n");
    xaResult = (*xaEngineObject)->Realize(xaEngineObject, XA_BOOLEAN_FALSE);
    printf("xaResult = %d\n", xaResult);

    printf("GetInterface for XA_IID_ENGINE\n");
    XAEngineItf xaEngineEngine;
    xaResult = (*xaEngineObject)->GetInterface(xaEngineObject, XA_IID_ENGINE, &xaEngineEngine);
    printf("xaResult = %d\n", xaResult);
    printf("xaEngineEngine = %p\n", xaEngineEngine);
    assert(XA_RESULT_SUCCESS == xaResult);

    printf("slCreateEngine\n");
    slResult = slCreateEngine(&slEngineObject, 0, NULL, 0, NULL, NULL);
    printf("slResult = %d\n", slResult);
    assert(SL_RESULT_SUCCESS == slResult);
    printf("slEngineObject = %p\n", slEngineObject);

    printf("realize slEngineObject\n");
    slResult = (*slEngineObject)->Realize(slEngineObject, SL_BOOLEAN_FALSE);
    printf("slResult = %d\n", slResult);

    printf("GetInterface for SL_IID_ENGINE\n");
    SLEngineItf slEngineEngine;
    slResult = (*slEngineObject)->GetInterface(slEngineObject, SL_IID_ENGINE, &slEngineEngine);
    printf("slResult = %d\n", slResult);
    printf("slEngineEngine = %p\n", slEngineEngine);
    assert(SL_RESULT_SUCCESS == slResult);

    printf("destroying xaEngineObject\n");
    (*xaEngineObject)->Destroy(xaEngineObject);

    printf("destroying slEngineObject\n");
    (*slEngineObject)->Destroy(slEngineObject);

    printf("exit\n");
    return EXIT_SUCCESS;
}
