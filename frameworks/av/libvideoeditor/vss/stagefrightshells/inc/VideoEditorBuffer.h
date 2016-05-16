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
* @file   VideoEditorBuffer.c
* @brief  StageFright shell Buffer
*************************************************************************
*/
#ifndef   VIDEOEDITOR_BUFFER_H
#define   VIDEOEDITOR_BUFFER_H

#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"
#include "M4OSA_Memory.h"
#include "M4OSA_CharStar.h"
#include "M4_Utils.h"

#include "LV_Macros.h"

/*--- Core id for VIDEOEDITOR Buffer allocations  ---*/
#define VIDEOEDITOR_BUFFER_EXTERNAL 0x012F

/* ----- errors  -----*/
#define M4ERR_NO_BUFFER_AVAILABLE \
    M4OSA_ERR_CREATE(M4_ERR,VIDEOEDITOR_BUFFER_EXTERNAL,0x000001)
#define M4ERR_NO_BUFFER_MATCH \
    M4OSA_ERR_CREATE(M4_ERR,VIDEOEDITOR_BUFFER_EXTERNAL,0x000002)

typedef enum {
    VIDEOEDITOR_BUFFER_kEmpty = 0,
    VIDEOEDITOR_BUFFER_kFilled,
} VIDEOEDITOR_BUFFER_State;

/**
 ************************************************************************
 * Structure    LVOMX_BUFFER_Buffer
 * @brief       One OMX Buffer and data related to it
 ************************************************************************
*/
typedef struct {
    M4OSA_Void* pData;              /**< Pointer to the data*/
    M4OSA_UInt32 size;
    VIDEOEDITOR_BUFFER_State state; /**< Buffer state */
    M4OSA_UInt32 idx;               /**< Index of the buffer inside the pool */
    M4_MediaTime    buffCTS;        /**< Time stamp of the buffer */
} VIDEOEDITOR_BUFFER_Buffer;

/**
 ************************************************************************
 * Structure    LVOMX_BUFFER_Pool
 * @brief       Structure to manage buffers
 ************************************************************************
*/
typedef struct {
    VIDEOEDITOR_BUFFER_Buffer* pNXPBuffer;
    M4OSA_UInt32 NB;
    M4OSA_Char* poolName;
} VIDEOEDITOR_BUFFER_Pool;

#ifdef __cplusplus
extern "C"
{
#endif //__cplusplus

/**
 ************************************************************************
 M4OSA_ERR VIDEOEDITOR_BUFFER_allocatePool(VIDEOEDITOR_BUFFER_Pool** ppool,
 *         M4OSA_UInt32 nbBuffers)
 * @brief   Allocate a pool of nbBuffers buffers
 *
 * @param   ppool      : IN The buffer pool to create
 * @param   nbBuffers  : IN The number of buffers in the pool
 * @param   poolName   : IN a name given to the pool
 * @return  Error code
 ************************************************************************
*/
M4OSA_ERR VIDEOEDITOR_BUFFER_allocatePool(VIDEOEDITOR_BUFFER_Pool** ppool,
        M4OSA_UInt32 nbBuffers, M4OSA_Char* poolName);

/**
 ************************************************************************
 M4OSA_ERR VIDEOEDITOR_BUFFER_freePool(LVOMX_BUFFER_Pool* ppool)
 * @brief   Deallocate a buffer pool
 *
 * @param   ppool      : IN The buffer pool to free
 * @return  Error code
 ************************************************************************
*/
M4OSA_ERR VIDEOEDITOR_BUFFER_freePool(VIDEOEDITOR_BUFFER_Pool* ppool);

/**
 ************************************************************************
 M4OSA_ERR VIDEOEDITOR_BUFFER_getBuffer(VIDEOEDITOR_BUFFER_Pool* ppool,
 *         VIDEOEDITOR_BUFFER_Buffer** pNXPBuffer)
 * @brief   Returns a buffer in a given state
 *
 * @param   ppool      : IN The buffer pool
 * @param   desiredState : IN The buffer state
 * @param   pNXPBuffer : IN The selected buffer
 * @return  Error code
 ************************************************************************
*/
M4OSA_ERR VIDEOEDITOR_BUFFER_getBuffer(VIDEOEDITOR_BUFFER_Pool* ppool,
        VIDEOEDITOR_BUFFER_State desiredState,
        VIDEOEDITOR_BUFFER_Buffer** pNXPBuffer);


M4OSA_ERR VIDEOEDITOR_BUFFER_initPoolBuffers(VIDEOEDITOR_BUFFER_Pool* ppool,
        M4OSA_UInt32 lSize);

M4OSA_ERR VIDEOEDITOR_BUFFER_getOldestBuffer(VIDEOEDITOR_BUFFER_Pool *pool,
        VIDEOEDITOR_BUFFER_State desiredState,
        VIDEOEDITOR_BUFFER_Buffer** pNXPBuffer);

#ifdef __cplusplus
}
#endif //__cplusplus
#endif /*VIDEOEDITOR_BUFFER_H*/

