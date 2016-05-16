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
#undef M4OSA_TRACE_LEVEL
#define M4OSA_TRACE_LEVEL 1

#include "VideoEditorBuffer.h"
#include "utils/Log.h"

#define VIDEOEDITOR_BUFFEPOOL_MAX_NAME_SIZE 40

#define VIDEOEDITOR_SAFE_FREE(p) \
{ \
    if(M4OSA_NULL != p) \
    { \
        free(p); \
        p = M4OSA_NULL; \
    } \
}

/**
 ************************************************************************
 M4OSA_ERR VIDEOEDITOR_BUFFER_allocatePool(VIDEOEDITOR_BUFFER_Pool** ppool,
 *                                         M4OSA_UInt32 nbBuffers)
 * @brief   Allocate a pool of nbBuffers buffers
 *
 * @param   ppool      : IN The buffer pool to create
 * @param   nbBuffers  : IN The number of buffers in the pool
 * @param   poolName   : IN a name given to the pool
 * @return  Error code
 ************************************************************************
*/
M4OSA_ERR VIDEOEDITOR_BUFFER_allocatePool(VIDEOEDITOR_BUFFER_Pool** ppool,
        M4OSA_UInt32 nbBuffers, M4OSA_Char* poolName)
{
    M4OSA_ERR lerr = M4NO_ERROR;
    VIDEOEDITOR_BUFFER_Pool* pool;
    M4OSA_UInt32 index;

    ALOGV("VIDEOEDITOR_BUFFER_allocatePool : ppool = 0x%x nbBuffers = %d ",
        ppool, nbBuffers);

    pool = M4OSA_NULL;
    pool = (VIDEOEDITOR_BUFFER_Pool*)M4OSA_32bitAlignedMalloc(
            sizeof(VIDEOEDITOR_BUFFER_Pool), VIDEOEDITOR_BUFFER_EXTERNAL,
            (M4OSA_Char*)("VIDEOEDITOR_BUFFER_allocatePool: pool"));
    if (M4OSA_NULL == pool)
    {
        lerr = M4ERR_ALLOC;
        goto VIDEOEDITOR_BUFFER_allocatePool_Cleanup;
    }

    ALOGV("VIDEOEDITOR_BUFFER_allocatePool : Allocating Pool buffers");
    pool->pNXPBuffer = M4OSA_NULL;
    pool->pNXPBuffer = (VIDEOEDITOR_BUFFER_Buffer*)M4OSA_32bitAlignedMalloc(
                            sizeof(VIDEOEDITOR_BUFFER_Buffer)*nbBuffers,
                            VIDEOEDITOR_BUFFER_EXTERNAL,
                            (M4OSA_Char*)("BUFFER_allocatePool: pNXPBuffer"));
    if(M4OSA_NULL == pool->pNXPBuffer)
    {
        lerr = M4ERR_ALLOC;
        goto VIDEOEDITOR_BUFFER_allocatePool_Cleanup;
    }

    for (index = 0; index < nbBuffers; index++)
    {
        pool->pNXPBuffer[index].pData = M4OSA_NULL;
    }

    ALOGV("VIDEOEDITOR_BUFFER_allocatePool : Allocating Pool name buffer");
    pool->poolName = M4OSA_NULL;
    pool->poolName = (M4OSA_Char*)M4OSA_32bitAlignedMalloc(
        VIDEOEDITOR_BUFFEPOOL_MAX_NAME_SIZE,VIDEOEDITOR_BUFFER_EXTERNAL,
        (M4OSA_Char*)("VIDEOEDITOR_BUFFER_allocatePool: poolname"));
    if(pool->poolName == M4OSA_NULL)
    {
        lerr = M4ERR_ALLOC;
        goto VIDEOEDITOR_BUFFER_allocatePool_Cleanup;
    }

    ALOGV("VIDEOEDITOR_BUFFER_allocatePool : Assigning Pool name buffer");

    memset((void *)pool->poolName, 0,VIDEOEDITOR_BUFFEPOOL_MAX_NAME_SIZE);
    memcpy((void *)pool->poolName, (void *)poolName,
        VIDEOEDITOR_BUFFEPOOL_MAX_NAME_SIZE-1);

    pool->NB = nbBuffers;

VIDEOEDITOR_BUFFER_allocatePool_Cleanup:
    if(M4NO_ERROR != lerr)
    {
        VIDEOEDITOR_SAFE_FREE(pool->pNXPBuffer);
        VIDEOEDITOR_SAFE_FREE(pool->poolName);
        VIDEOEDITOR_SAFE_FREE(pool);
    }
    *ppool = pool;
    ALOGV("VIDEOEDITOR_BUFFER_allocatePool END");

    return lerr;
}

/**
 ************************************************************************
 M4OSA_ERR VIDEOEDITOR_BUFFER_freePool(VIDEOEDITOR_BUFFER_Pool* ppool)
 * @brief   Deallocate a buffer pool
 *
 * @param   ppool      : IN The buffer pool to free
 * @return  Error code
 ************************************************************************
*/
M4OSA_ERR VIDEOEDITOR_BUFFER_freePool(VIDEOEDITOR_BUFFER_Pool* ppool)
{
    M4OSA_ERR err;
    M4OSA_UInt32  j = 0;

    ALOGV("VIDEOEDITOR_BUFFER_freePool : ppool = 0x%x", ppool);

    err = M4NO_ERROR;

    for (j = 0; j < ppool->NB; j++)
    {
        if(M4OSA_NULL != ppool->pNXPBuffer[j].pData)
        {
            free(ppool->pNXPBuffer[j].pData);
            ppool->pNXPBuffer[j].pData = M4OSA_NULL;
        }
    }

    if(ppool != M4OSA_NULL)
    {
        SAFE_FREE(ppool->pNXPBuffer);
        SAFE_FREE(ppool->poolName);
        SAFE_FREE(ppool);
    }

    return(err);
}

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
        VIDEOEDITOR_BUFFER_Buffer** pNXPBuffer)
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_Bool bFound = M4OSA_FALSE;
    M4OSA_UInt32 i, ibuf;

    ALOGV("VIDEOEDITOR_BUFFER_getBuffer from %s in state=%d",
        ppool->poolName, desiredState);

    ibuf = 0;

    for (i=0; i < ppool->NB; i++)
    {
        bFound = (ppool->pNXPBuffer[i].state == desiredState);
        if (bFound)
        {
            ibuf = i;
            break;
        }
    }

    if(!bFound)
    {
        ALOGV("VIDEOEDITOR_BUFFER_getBuffer No buffer available in state %d",
            desiredState);
        *pNXPBuffer = M4OSA_NULL;
        return M4ERR_NO_BUFFER_AVAILABLE;
    }

    /* case where a buffer has been found */
    *pNXPBuffer = &(ppool->pNXPBuffer[ibuf]);

    ALOGV("VIDEOEDITOR_BUFFER_getBuffer: idx = %d", ibuf);

    return(err);
}

M4OSA_ERR VIDEOEDITOR_BUFFER_initPoolBuffers(VIDEOEDITOR_BUFFER_Pool* pool,
    M4OSA_UInt32 lSize)
{
    M4OSA_ERR     err = M4NO_ERROR;
    M4OSA_UInt32  index, j;

    /**
     * Initialize all the buffers in the pool */
    for(index = 0; index < pool->NB; index++)
    {
        pool->pNXPBuffer[index].pData = M4OSA_NULL;
        pool->pNXPBuffer[index].pData = (M4OSA_Void*)M4OSA_32bitAlignedMalloc(
            lSize, VIDEOEDITOR_BUFFER_EXTERNAL,
            (M4OSA_Char*)("BUFFER_initPoolBuffers: Buffer data"));
        if(M4OSA_NULL == pool->pNXPBuffer[index].pData)
        {
            for (j = 0; j < index; j++)
            {
                if(M4OSA_NULL != pool->pNXPBuffer[j].pData)
                {
                    free(pool->pNXPBuffer[j].pData);
                    pool->pNXPBuffer[j].pData = M4OSA_NULL;
                }
            }
            err = M4ERR_ALLOC;
            return err;
        }
        pool->pNXPBuffer[index].size = 0;
        pool->pNXPBuffer[index].state = VIDEOEDITOR_BUFFER_kEmpty;
        pool->pNXPBuffer[index].idx = index;
        pool->pNXPBuffer[index].buffCTS = -1;
    }
    return err;
}

M4OSA_ERR VIDEOEDITOR_BUFFER_getOldestBuffer(VIDEOEDITOR_BUFFER_Pool *pool,
        VIDEOEDITOR_BUFFER_State desiredState,
        VIDEOEDITOR_BUFFER_Buffer** pNXPBuffer)
{
    M4OSA_ERR     err = M4NO_ERROR;
    M4OSA_UInt32  index, j;
    M4_MediaTime  candidateTimeStamp = (M4_MediaTime)0x7ffffff;
    M4OSA_Bool    bFound = M4OSA_FALSE;

    *pNXPBuffer = M4OSA_NULL;
    for(index = 0; index< pool->NB; index++)
    {
        if(pool->pNXPBuffer[index].state == desiredState)
        {
            if(pool->pNXPBuffer[index].buffCTS <= candidateTimeStamp)
            {
                bFound = M4OSA_TRUE;
                candidateTimeStamp = pool->pNXPBuffer[index].buffCTS;
                    *pNXPBuffer = &(pool->pNXPBuffer[index]);
            }
        }
    }
    if(M4OSA_FALSE == bFound)
    {
        ALOGV("VIDEOEDITOR_BUFFER_getOldestBuffer WARNING no buffer available");
        err = M4ERR_NO_BUFFER_AVAILABLE;
    }
    return err;
}
