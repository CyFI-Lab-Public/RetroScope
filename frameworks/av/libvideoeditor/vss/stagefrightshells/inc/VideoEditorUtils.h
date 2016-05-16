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
/**
*************************************************************************
* @file   VideoEditorUtils.cpp
* @brief  StageFright shell Utilities
*************************************************************************
*/
#ifndef ANDROID_UTILS_H_
#define ANDROID_UTILS_H_

/*******************
 *     HEADERS     *
 *******************/

#include "M4OSA_Debug.h"

#include "utils/Log.h"
#include <utils/RefBase.h>
#include <utils/threads.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

/**
 *************************************************************************
 * VIDEOEDITOR_CHECK(test, errCode)
 * @note This macro displays an error message and goes to function cleanUp label
 *       if the test fails.
 *************************************************************************
 */
#define VIDEOEDITOR_CHECK(test, errCode) \
{ \
    if( !(test) ) { \
        ALOGV("!!! %s (L%d) check failed : " #test ", yields error 0x%.8x", \
            __FILE__, __LINE__, errCode); \
        err = (errCode); \
        goto cleanUp; \
    } \
}

/**
 *************************************************************************
 * SAFE_FREE(p)
 * @note This macro calls free and makes sure the pointer is set to NULL.
 *************************************************************************
 */
#define SAFE_FREE(p) \
{ \
    if(M4OSA_NULL != (p)) { \
        free((p)) ; \
        (p) = M4OSA_NULL ; \
    } \
}

/**
 *************************************************************************
 * SAFE_MALLOC(p, type, count, comment)
 * @note This macro allocates a buffer, checks for success and fills the buffer
 *       with 0.
 *************************************************************************
 */
#define SAFE_MALLOC(p, type, count, comment) \
{ \
    (p) = (type*)M4OSA_32bitAlignedMalloc(sizeof(type)*(count), 0xFF,(M4OSA_Char*)comment);\
    VIDEOEDITOR_CHECK(M4OSA_NULL != (p), M4ERR_ALLOC); \
    memset((void *)(p), 0,sizeof(type)*(count)); \
}


    /********************
     *    UTILITIES     *
     ********************/


namespace android {

/*--------------------------*/
/* DISPLAY METADATA CONTENT */
/*--------------------------*/
void displayMetaData(const sp<MetaData> meta);

// Build the AVC codec spcific info from the StageFright encoders output
status_t buildAVCCodecSpecificData(uint8_t **outputData, size_t *outputSize,
        const uint8_t *data, size_t size, MetaData *param);

}//namespace android


#endif //ANDROID_UTILS_H_
