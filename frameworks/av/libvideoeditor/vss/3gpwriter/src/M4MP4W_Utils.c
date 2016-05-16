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
******************************************************************************
 * @file    M4MP4W_Utils.c
 * @brief   Utilities and private functions for the MP4 writer
******************************************************************************
*/

#include "NXPSW_CompilerSwitches.h"

#ifndef _M4MP4W_USE_CST_MEMORY_WRITER

#include "M4MP4W_Utils.h"
#include "M4OSA_Error.h"
#include "M4MP4W_Types.h"

#define ERR_CHECK(exp, err) if (!(exp)) { return err; }

/*******************************************************************************/
M4OSA_ERR M4MP4W_putByte(M4OSA_UChar c, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context)
/*******************************************************************************/
{
    M4OSA_ERR err = fileFunction->writeData(context, (M4OSA_MemAddr8)&c, 1);
    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_putBE16(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context)
/*******************************************************************************/
{
    M4OSA_ERR err;
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 8), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)val, fileFunction, context);
    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_putBE24(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context)
/*******************************************************************************/
{
    M4OSA_ERR err;
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 16), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 8), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)val, fileFunction, context);
    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_putBE32(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context)
/*******************************************************************************/
{
    M4OSA_ERR err;
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 24), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 16), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)(val >> 8), fileFunction, context);
    ERR_CHECK(err == M4NO_ERROR, err);
    err = M4MP4W_putByte((M4OSA_UChar)val, fileFunction, context);
    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_putBlock(const M4OSA_UChar* Block, M4OSA_UInt32 size,
                           M4OSA_FileWriterPointer* fileFunction, M4OSA_Context context)
/*******************************************************************************/
{
    M4OSA_ERR err = fileFunction->writeData(context, (M4OSA_MemAddr8)Block, size);
    return err;
}

/*******************************************************************************/
void M4MP4W_convertInt32BE(M4OSA_UInt32* valPtr)
/*******************************************************************************/
{
    M4OSA_UChar a, b;
    M4OSA_UChar* c = (M4OSA_UChar*)valPtr;
    a       = *(c);
    b       = *(c+1);
    *(c)   = *(c+3);
    *(c+1) = *(c+2);
    *(c+2) = b;
    *(c+3) = a;
}

/*******************************************************************************/
void M4MP4W_table32ToBE(M4OSA_UInt32* tab, M4OSA_UInt32 nb)
/*******************************************************************************/
{
    M4OSA_UInt32 i;
    for (i=0; i<nb; i++)
        M4MP4W_convertInt32BE(&(tab)[i]);
}

/*******************************************************************************/
void* M4MP4W_realloc(M4OSA_MemAddr32 ptr, M4OSA_UInt32 oldSize, M4OSA_UInt32 newSize)
/*******************************************************************************/
{
    M4OSA_MemAddr32 ptr2 = (M4OSA_MemAddr32)M4OSA_32bitAlignedMalloc(newSize, M4MP4_WRITER,
                                                          (M4OSA_Char *)"realloc");
    if (M4OSA_NULL != ptr2)
    {
        memcpy((void *)ptr2, (void *)ptr, oldSize);
    }
    free(ptr);
    return ptr2;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_freeContext(M4OSA_Context context)
/*******************************************************************************/
{
#ifdef _M4MP4W_MOOV_FIRST
    M4OSA_UInt32 i;
#endif /*_M4MP4W_MOOV_FIRST*/
    M4MP4W_Mp4FileData* mMp4FileDataPtr = (M4MP4W_Mp4FileData*)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    /*freeContext is now called after closeWrite*/
    ERR_CHECK( mMp4FileDataPtr->state == M4MP4W_closed, M4ERR_STATE);
    mMp4FileDataPtr->state = M4MP4W_closed;

    if (mMp4FileDataPtr->audioTrackPtr != M4OSA_NULL)
    {
        /*delete also other chunks if any*/
        /*for (i=0; i<=mMp4FileDataPtr->audioTrackPtr->currentChunk; i++)*/

#ifdef _M4MP4W_MOOV_FIRST
        for (i=0; i<=mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk; i++)
        {
            free(mMp4FileDataPtr->audioTrackPtr->Chunk[i]);
        }
#else
        if ((M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->Chunk) &&
             (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->Chunk[0]))
        {
            free(mMp4FileDataPtr->audioTrackPtr->Chunk[0]);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable)
        {
            free(mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable);
        }
#endif /*_M4MP4W_MOOV_FIRST*/

        /*now dynamic*/
        if (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->Chunk)
        {
            free(mMp4FileDataPtr->audioTrackPtr->Chunk);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->chunkSizeTable)
        {
            free(mMp4FileDataPtr->audioTrackPtr->chunkSizeTable);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable)
        {
            free(mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable)
        {
            free(mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable);
        }

        if (mMp4FileDataPtr->audioTrackPtr->TABLE_STTS != M4OSA_NULL)
        {
            free(mMp4FileDataPtr->audioTrackPtr->TABLE_STTS);
        }

        if (mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ != M4OSA_NULL)
        {
            free(mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ);
        }

        if (mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL)
        {
            free(mMp4FileDataPtr->audioTrackPtr->DSI);
            mMp4FileDataPtr->audioTrackPtr->DSI = M4OSA_NULL;
        }

        free(mMp4FileDataPtr->audioTrackPtr);
        mMp4FileDataPtr->audioTrackPtr = M4OSA_NULL;
    }
    if (mMp4FileDataPtr->videoTrackPtr != M4OSA_NULL)
    {
        /*delete also other chunks if any*/
        /*for (i=0; i<=mMp4FileDataPtr->videoTrackPtr->currentChunk; i++)*/

#ifdef _M4MP4W_MOOV_FIRST
        for (i=0; i<=mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk; i++)
        {
            free(mMp4FileDataPtr->videoTrackPtr->Chunk[i]);
        }
#else
        if ((M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->Chunk) &&
             (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->Chunk[0]))
        {
            free(mMp4FileDataPtr->videoTrackPtr->Chunk[0]);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable)
        {
            free(mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable);
        }
#endif /*_M4MP4W_MOOV_FIRST*/

        /*now dynamic*/
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->Chunk)
        {
            free(mMp4FileDataPtr->videoTrackPtr->Chunk);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->chunkSizeTable)
        {
            free(mMp4FileDataPtr->videoTrackPtr->chunkSizeTable);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable)
        {
            free(mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable)
        {
            free(mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable);
        }

        if (mMp4FileDataPtr->videoTrackPtr->DSI != M4OSA_NULL)
        {
            free(mMp4FileDataPtr->videoTrackPtr->DSI);
            mMp4FileDataPtr->videoTrackPtr->DSI = M4OSA_NULL;
        }

        /*now dynamic*/
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->TABLE_STTS)
        {
            free(mMp4FileDataPtr->videoTrackPtr->TABLE_STTS);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ)
        {
            free(mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ);
        }
        if (M4OSA_NULL != mMp4FileDataPtr->videoTrackPtr->TABLE_STSS)
        {
            free(mMp4FileDataPtr->videoTrackPtr->TABLE_STSS);
        }

        free(mMp4FileDataPtr->videoTrackPtr);
        mMp4FileDataPtr->videoTrackPtr = M4OSA_NULL;
    }

    if (mMp4FileDataPtr->embeddedString != M4OSA_NULL)
    {
        free(mMp4FileDataPtr->embeddedString);
        mMp4FileDataPtr->embeddedString = M4OSA_NULL;
    }

    free(mMp4FileDataPtr);

    return M4NO_ERROR;
}

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
/*******************************************************************************/
M4OSA_Void M4MP4W_put32_Hi(M4OSA_UInt32* tab, M4OSA_UInt16 Hi)
/*******************************************************************************/
{
    *tab &= 0xFFFF;
    *tab |= Hi<<16;
}

/*******************************************************************************/
M4OSA_Void M4MP4W_put32_Lo(M4OSA_UInt32* tab, M4OSA_UInt16 Lo)
/*******************************************************************************/
{
    *tab &= 0xFFFF0000;
    *tab |= Lo;
}

/*******************************************************************************/
M4OSA_UInt16 M4MP4W_get32_Hi(M4OSA_UInt32* tab)
/*******************************************************************************/
{
    return (*tab >> 16) & 0xFFFF;
}

/*******************************************************************************/
M4OSA_UInt16 M4MP4W_get32_Lo(M4OSA_UInt32* tab)
/*******************************************************************************/
{
    return *tab & 0xFFFF;
}
#endif

#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */

