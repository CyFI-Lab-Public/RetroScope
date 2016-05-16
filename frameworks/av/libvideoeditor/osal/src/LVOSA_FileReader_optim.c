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
 * @file         M4OSA_FileReader_optim.c
 * @brief
 * @note         This file implements functions to manipulate filesystem access
 ******************************************************************************
*/

/** Addition of Trace ID **/
#include "M4OSA_CoreID.h"
#include "M4OSA_Error.h"

#ifdef M4TRACE_ID
#undef M4TRACE_ID
#endif
#define M4TRACE_ID    M4OSA_FILE_READER


#include "M4OSA_FileCommon.h"
#include "M4OSA_FileReader.h"
#include "M4OSA_FileWriter.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"

#include "LVOSA_FileReader_optim.h"

#define M4OSA_READER_OPTIM_USE_OSAL_IF
#ifndef M4OSA_READER_OPTIM_USE_OSAL_IF
    #include "M4OSA_FileAccess.h"
#endif

#define M4ERR_CHECK_NULL_RETURN_VALUE(retval, pointer) if ((pointer) == M4OSA_NULL) return (retval);




/**
 ******************************************************************************
 * File reader cache buffers parameters (size, number of buffers, etc)
 ******************************************************************************
*/
#define M4OSA_READBUFFER_SIZE    1024*16
#define M4OSA_READBUFFER_NB        2
#define M4OSA_READBUFFER_NONE    -1
#define M4OSA_EOF               -1

#define MAX_FILLS_SINCE_LAST_ACCESS    M4OSA_READBUFFER_NB*2

/**
 ******************************************************************************
 * structure    M4OSA_FileReader_Buffer
 * @brief       This structure defines the File reader Buffers context (private)
 ******************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8      data;        /**< buffer data */
    M4OSA_FilePosition  size;        /**< size of the buffer */
    M4OSA_FilePosition  filepos;    /**< position in the file where the buffer starts */
    M4OSA_FilePosition  remain;        /**< data amount not already copied from buffer */
    M4OSA_UInt32        nbFillSinceLastAcess;    /**< To know since how many time we didn't use this buffer */
} M4OSA_FileReader_Buffer_optim;

/**
 ******************************************************************************
 * structure    M4OSA_FileReader_Context
 * @brief       This structure defines the File reader context (private)
 * @note        This structure is used for all File Reader calls to store the context
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Bool              IsOpened;       /**< Micro state machine */
    M4OSA_FileAttribute     FileAttribute;  /**< Opening mode */
    M4OSA_FilePosition         readFilePos;    /**< Effective position of the GFL read pointer */
    M4OSA_FilePosition         absolutePos;    /**< Virtual position for next reading */
    M4OSA_FilePosition         fileSize;        /**< Size of the file */

    M4OSA_FileReader_Buffer_optim buffer[M4OSA_READBUFFER_NB];  /**< Read buffers */

    M4OSA_Void*             aFileDesc;  /**< File descriptor */

#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_FileReadPointer*     FS;            /**< Filesystem interface */
#else
    M4OSA_FileSystem_FctPtr *FS;        /**< Filesystem interface */
#endif

} M4OSA_FileReader_Context_optim;

/* __________________________________________________________ */
/*|                                                          |*/
/*|    Global function for handling low level read access    |*/
/*|__________________________________________________________|*/

static M4OSA_FileReadPointer* gv_NXPSW_READOPT_lowLevelFunctions;

M4OSA_ERR NXPSW_FileReaderOptim_init(M4OSA_Void *lowLevel_functionPointers, M4OSA_Void *optimized_functionPointers)
{
    M4OSA_FileReadPointer* lowLevel_fp  = (M4OSA_FileReadPointer*) lowLevel_functionPointers;
    M4OSA_FileReadPointer* optimized_fp = (M4OSA_FileReadPointer*) optimized_functionPointers;

    //Set the optimized functions, to be called by the user
    optimized_fp->openRead  = M4OSA_fileReadOpen_optim;
    optimized_fp->readData  = M4OSA_fileReadData_optim;
    optimized_fp->seek      = M4OSA_fileReadSeek_optim;
    optimized_fp->closeRead = M4OSA_fileReadClose_optim;
    optimized_fp->setOption = M4OSA_fileReadSetOption_optim;
    optimized_fp->getOption = M4OSA_fileReadGetOption_optim;


    return M4NO_ERROR;
}

M4OSA_ERR NXPSW_FileReaderOptim_cleanUp()
{

    gv_NXPSW_READOPT_lowLevelFunctions = M4OSA_NULL;

    return M4NO_ERROR;
}


M4OSA_ERR NXPSW_FileReaderOptim_getLowLevelFunctions(M4OSA_Void **FS)
{
    M4OSA_FileReadPointer** pFunctionsPointer = (M4OSA_FileReadPointer**) FS;
    *pFunctionsPointer = gv_NXPSW_READOPT_lowLevelFunctions;
    return M4NO_ERROR;
}


/* __________________________________________________________ */
/*|                                                          |*/
/*|        Buffer handling functions for Read access         |*/
/*|__________________________________________________________|*/

/**************************************************************/
M4OSA_ERR M4OSA_FileReader_BufferInit(M4OSA_FileReader_Context_optim* apContext)
/**************************************************************/
{
    M4OSA_UInt8 i;

    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        apContext->buffer[i].data = M4OSA_NULL;
        apContext->buffer[i].size = 0;
        apContext->buffer[i].filepos = 0;
        apContext->buffer[i].remain = 0;
    }

    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        apContext->buffer[i].data = (M4OSA_MemAddr8) M4OSA_32bitAlignedMalloc(M4OSA_READBUFFER_SIZE, 
            M4OSA_FILE_READER, (M4OSA_Char *)"M4OSA_FileReader_BufferInit");
        M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_ALLOC, apContext->buffer[i].data);
    }

    return M4NO_ERROR;
}

/**************************************************************/
M4OSA_Void M4OSA_FileReader_BufferFree(M4OSA_FileReader_Context_optim* apContext)
/**************************************************************/
{
    M4OSA_Int8 i;

    for(i=0; i<M4OSA_READBUFFER_NB; i++)
        if(apContext->buffer[i].data != M4OSA_NULL)
            free(apContext->buffer[i].data);
}

/**************************************************************/
M4OSA_FilePosition M4OSA_FileReader_BufferCopy(M4OSA_FileReader_Context_optim* apContext,
                                               M4OSA_Int8 i, M4OSA_FilePosition pos,
                                               M4OSA_FilePosition size, M4OSA_MemAddr8 pData)
/**************************************************************/
{
    M4OSA_FilePosition copysize;
    M4OSA_FilePosition offset;

    if(apContext->buffer[i].size == M4OSA_EOF) return M4OSA_EOF;

    if(   (pos < apContext->buffer[i].filepos)
       || (pos > (apContext->buffer[i].filepos + apContext->buffer[i].size - 1)) )
    {
        return 0; /* nothing copied */
    }

    offset = pos - apContext->buffer[i].filepos;

    copysize = apContext->buffer[i].size - offset;
    copysize = (size < copysize) ? size : copysize;

    memcpy((void *)pData, (void *)(apContext->buffer[i].data + offset), copysize);

    apContext->buffer[i].remain -= copysize;
    apContext->buffer[i].nbFillSinceLastAcess = 0;

    return copysize;
}

/**************************************************************/
M4OSA_ERR M4OSA_FileReader_BufferFill(M4OSA_FileReader_Context_optim* apContext,
                                       M4OSA_Int8 i, M4OSA_FilePosition pos)
/**************************************************************/
{
    M4OSA_FilePosition     gridPos;
    M4OSA_FilePosition    tempPos;
    M4OSA_UInt32        bufferSize;
    M4OSA_FilePosition     diff;
    M4OSA_FilePosition     size;
    M4OSA_ERR             err = M4NO_ERROR;
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR             errno = M4NO_ERROR;
    M4OSA_UInt32         fileReadSize = 0;
    M4OSA_FilePosition     fileSeekPosition = 0;
#else
    M4OSA_Int32         ret_val;
    M4OSA_UInt16         errno;
#endif

    M4OSA_TRACE3_4("BufferFill  i = %d  pos = %ld  read = %ld  old = %ld", i, pos,
                              apContext->readFilePos, apContext->buffer[i].filepos);

    /* Avoid cycling statement because of EOF */
    if(pos >= apContext->fileSize)
        return M4WAR_NO_MORE_AU;

    /* Relocate to absolute postion if necessary */
    bufferSize = M4OSA_READBUFFER_SIZE;
    tempPos = (M4OSA_FilePosition) (pos / bufferSize);
    gridPos = tempPos * M4OSA_READBUFFER_SIZE;
    diff = gridPos - apContext->readFilePos;

    if(diff != 0)
    {
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
        fileSeekPosition = diff;
        errno = apContext->FS->seek(apContext->aFileDesc, M4OSA_kFileSeekCurrent,
                                    &fileSeekPosition);
        apContext->readFilePos = gridPos;

        if(M4NO_ERROR != errno)
        {
            err = errno;
            M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR1 = 0x%x", err);
            return err;
        }

#else
        ret_val = apContext->FS->pFctPtr_Seek(apContext->aFileDesc, diff,
                                               M4OSA_kFileSeekCurrent, &errno);
        apContext->readFilePos = gridPos;

        if(ret_val != 0)
        {
            err = M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_READER, errno);
            M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR1 = 0x%x", err);
            return err;
        }
#endif /*M4OSA_READER_OPTIM_USE_OSAL_IF*/
    }

    apContext->buffer[i].filepos = apContext->readFilePos;

    /* Read Data */
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    fileReadSize = M4OSA_READBUFFER_SIZE;
    errno = apContext->FS->readData(apContext->aFileDesc,
                      (M4OSA_MemAddr8)apContext->buffer[i].data, &fileReadSize);

    size = (M4OSA_FilePosition)fileReadSize;
    if ((M4NO_ERROR != errno)&&(M4WAR_NO_DATA_YET != errno))
    {
        apContext->buffer[i].size = M4OSA_EOF;
        apContext->buffer[i].remain = 0;

        err = errno;
        M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR2 = 0x%x", err);
        return err;
    }
#else
    size = apContext->FS->pFctPtr_Read(apContext->aFileDesc,
        (M4OSA_UInt8 *)apContext->buffer[i].data, M4OSA_READBUFFER_SIZE, &errno);
    if(size == -1)
    {
        apContext->buffer[i].size = M4OSA_EOF;
        apContext->buffer[i].remain = 0;

        err = M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_READER, errno);
        M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR2 = 0x%x", err);
        return err;
    }
#endif

    apContext->buffer[i].size = size;
    apContext->buffer[i].remain = size;
    apContext->buffer[i].nbFillSinceLastAcess = 0;

    /* Retrieve current position */
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    errno = apContext->FS->getOption(apContext->aFileDesc,
                                     M4OSA_kFileReadGetFilePosition,
                                     (M4OSA_DataOption*) &apContext->readFilePos);

    if (M4NO_ERROR != errno)
    {
        err = errno;
        M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR3 = 0x%x", err);
    }
    else if(   (apContext->buffer[i].size >= 0)
       && (apContext->buffer[i].size < M4OSA_READBUFFER_SIZE) )
    {
        err = M4WAR_NO_DATA_YET;
        M4OSA_TRACE2_0("M4OSA_FileReader_BufferFill returns NO DATA YET");
        return err;
    }
#else
    apContext->readFilePos = apContext->FS->pFctPtr_Tell(apContext->aFileDesc, &errno);

    if(   (apContext->buffer[i].size >= 0)
       && (apContext->buffer[i].size < M4OSA_READBUFFER_SIZE) )
    {
        err = M4WAR_NO_DATA_YET;
        M4OSA_TRACE1_1("M4OSA_FileReader_BufferFill ERR3 = 0x%x", err);
        return err;
    }
#endif /*M4OSA_READER_OPTIM_USE_OSAL_IF*/

    /* Return without error */
    return M4NO_ERROR;
}

/**************************************************************/
M4OSA_Int8 M4OSA_FileReader_BufferMatch(M4OSA_FileReader_Context_optim* apContext,
                                        M4OSA_FilePosition pos)
/**************************************************************/
{
    M4OSA_Int8 i;


    /* Select the buffer which matches with given pos */
    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        if(   (pos >= apContext->buffer[i].filepos)
           && (pos < (apContext->buffer[i].filepos + apContext->buffer[i].size)) )
        {
            return i;
        }
    }
    return M4OSA_READBUFFER_NONE;
}

/**************************************************************/
M4OSA_Int8 M4OSA_FileReader_BufferSelect(M4OSA_FileReader_Context_optim* apContext,
                                         M4OSA_Int8 current_i)
/**************************************************************/
{
    M4OSA_Int8 i,j;
    M4OSA_FilePosition min_amount,max_amount;
    M4OSA_Int8 min_i,max_count;

    /* update nbFillSinceLastAcess field */
    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        apContext->buffer[i].nbFillSinceLastAcess ++;
    }

    /* Plan A : Scan for empty buffer */
    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        if(apContext->buffer[i].remain == 0)
        {
            return i;
        }
    }

    max_count = M4OSA_READBUFFER_NB;
    max_amount = MAX_FILLS_SINCE_LAST_ACCESS;

    /* Plan B : Scan for dead buffer */
    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        if(apContext->buffer[i].nbFillSinceLastAcess >= (M4OSA_UInt32) max_amount)
        {
            max_amount = apContext->buffer[i].nbFillSinceLastAcess;
            max_count = i;
        }
    }
    if(max_count<M4OSA_READBUFFER_NB)
    {
        M4OSA_TRACE2_2("DEAD BUFFER: %d, %d",max_count,apContext->buffer[max_count].nbFillSinceLastAcess);
        return max_count;
    }

    min_i = current_i;
    min_amount = M4OSA_READBUFFER_SIZE;

    /* Select the buffer which is the most "empty" */
    for(i=0; i<M4OSA_READBUFFER_NB; i++)
    {
        j = (i+current_i)%M4OSA_READBUFFER_NB;

        if(apContext->buffer[j].remain < min_amount)
        {
            min_amount = apContext->buffer[j].remain;
            min_i = j;
        }
    }

    return min_i;

}

/**************************************************************/
M4OSA_ERR M4OSA_FileReader_CalculateSize(M4OSA_FileReader_Context_optim* apContext)
/**************************************************************/
{
    M4OSA_ERR            err = M4NO_ERROR;
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR            errno = M4NO_ERROR;
#else
    M4OSA_Int32          ret_val;
    M4OSA_UInt16         errno;
#endif

    /* go to the end of file*/
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    errno = apContext->FS->getOption(apContext->aFileDesc, M4OSA_kFileReadGetFileSize,
                                        (M4OSA_DataOption*) &apContext->fileSize);
    if (M4NO_ERROR != errno)
    {
        err = errno;
        M4OSA_TRACE1_1("M4OSA_FileReader_CalculateSize ERR = 0x%x", err);
    }
#else
    ret_val = apContext->FS->pFctPtr_Seek(apContext->aFileDesc, 0, M4OSA_kFileSeekEnd, &errno);

    if (ret_val != 0)
    {
        apContext->readFilePos = M4OSA_EOF;
        err = M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_READER, errno);
        M4OSA_TRACE1_1("M4OSA_FileReader_CalculateSize ERR = 0x%x", err);
    }
    else
    {
        /* Retrieve size of the file */
        apContext->fileSize = apContext->FS->pFctPtr_Tell(apContext->aFileDesc, &errno);
        apContext->readFilePos = apContext->fileSize;
    }
#endif /*M4OSA_READER_OPTIM_USE_OSAL_IF*/

    return err;
}


/* __________________________________________________________ */
/*|                                                          |*/
/*|                   OSAL filesystem API                    |*/
/*|__________________________________________________________|*/

/**
******************************************************************************
* @brief       This method opens the provided fileDescriptor and returns its context.
* @param       pContext:       (OUT) File reader context.
* @param       pFileDescriptor :       (IN) File Descriptor of the input file.
* @param       FileModeAccess :        (IN) File mode access.
* @return      M4NO_ERROR: there is no error
* @return      M4ERR_PARAMETER pContext or fileDescriptor is NULL
* @return      M4ERR_ALLOC     there is no more memory available
* @return      M4ERR_FILE_BAD_MODE_ACCESS      the file mode access is not correct (it must be either isTextMode or read)
* @return      M4ERR_FILE_NOT_FOUND The file can not be opened.
******************************************************************************
*/
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR M4OSA_fileReadOpen_optim(M4OSA_Context* pContext,
                                       M4OSA_Void* pFileDescriptor,
                                       M4OSA_UInt32 FileModeAccess)
#else
    M4OSA_ERR M4OSA_fileReadOpen_optim(M4OSA_Context* pContext,
                                       M4OSA_Void* pFileDescriptor,
                                       M4OSA_UInt32 FileModeAccess,
                                       M4OSA_FileSystem_FctPtr *FS)
#endif
{
    M4OSA_FileReader_Context_optim* apContext = M4OSA_NULL;

    M4OSA_ERR   err       = M4NO_ERROR;
    M4OSA_Void* aFileDesc = M4OSA_NULL;
    M4OSA_Bool  buffers_allocated = M4OSA_FALSE;
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR errno = M4NO_ERROR;
#else
    M4OSA_UInt16 errno;
#endif /*M4OSA_READER_OPTIM_USE_OSAL_IF*/

    M4OSA_TRACE2_3("M4OSA_fileReadOpen_optim p = 0x%p fd = %s mode = %lu", pContext,
                                                   pFileDescriptor, FileModeAccess);

    /*      Check input parameters */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext);
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pFileDescriptor);

    *pContext = M4OSA_NULL;

    /*      Allocate memory for the File reader context. */
    apContext = (M4OSA_FileReader_Context_optim *)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_FileReader_Context_optim),
                                      M4OSA_FILE_READER, (M4OSA_Char *)"M4OSA_FileReader_Context_optim");

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_ALLOC, apContext);

    /* Set filesystem interface */
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF

    /*Set the optimized functions, to be called by the user*/

    apContext->FS = (M4OSA_FileReadPointer*) M4OSA_32bitAlignedMalloc(sizeof(M4OSA_FileReadPointer),
                                       M4OSA_FILE_READER, (M4OSA_Char *)"M4OSA_FileReaderOptim_init");
    if (M4OSA_NULL==apContext->FS)
    {
        M4OSA_TRACE1_0("M4OSA_FileReaderOptim_init - ERROR : allocation failed");
        return M4ERR_ALLOC;
    }
    apContext->FS->openRead  = M4OSA_fileReadOpen;
    apContext->FS->readData  = M4OSA_fileReadData;
    apContext->FS->seek      = M4OSA_fileReadSeek;
    apContext->FS->closeRead = M4OSA_fileReadClose;
    apContext->FS->setOption = M4OSA_fileReadSetOption;
    apContext->FS->getOption = M4OSA_fileReadGetOption;
#else
    apContext->FS = FS;
#endif

    /* Verify access mode */
    if (   ((FileModeAccess & M4OSA_kFileAppend) != 0)
        || ((FileModeAccess & M4OSA_kFileRead) == 0))
    {
        err = M4ERR_FILE_BAD_MODE_ACCESS;
        goto cleanup;
    }

    /* Open file in read mode */
    if((FileModeAccess & M4OSA_kFileCreate) != 0)
    {
        err = M4ERR_FILE_BAD_MODE_ACCESS;
    }
    else
    {
        if ((FileModeAccess & M4OSA_kFileRead))
        {
            /* File is opened in read only*/
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
            errno = apContext->FS->openRead(&aFileDesc, pFileDescriptor, FileModeAccess);

            if ((aFileDesc == M4OSA_NULL)||(M4NO_ERROR != errno))
            {
                /* converts the error to PSW format*/
                err = errno;
                M4OSA_TRACE2_1("M4OSA_fileReadOpen_optim ERR1 = 0x%x", err);
                apContext->IsOpened = M4OSA_FALSE;
            }
#else
            aFileDesc = apContext->FS->pFctPtr_Open(pFileDescriptor, FileModeAccess, &errno);

            if (aFileDesc == M4OSA_NULL)
            {
                /* converts the error to PSW format*/
                err = M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_READER, errno);
                M4OSA_TRACE2_1("M4OSA_fileReadOpen_optim ERR1 = 0x%x", err);
                apContext->IsOpened = M4OSA_FALSE;
            }
#endif

            else
            {
                apContext->IsOpened = M4OSA_TRUE;
            }
        }
        else
        {
            err = M4ERR_FILE_BAD_MODE_ACCESS;
        }
    }

    if (M4NO_ERROR != err) goto cleanup;

    /* Allocate buffers */
    err = M4OSA_FileReader_BufferInit(apContext);
    buffers_allocated = M4OSA_TRUE;

    if (M4NO_ERROR != err) goto cleanup;

    /* Initialize parameters */
    apContext->fileSize = 0;
    apContext->absolutePos = 0;
    apContext->readFilePos = 0;

    /* Retrieve the File Descriptor*/
    apContext->aFileDesc = aFileDesc;

    /* Retrieve the File mode Access */
    apContext->FileAttribute.modeAccess = (M4OSA_FileModeAccess) FileModeAccess;

    /*Retrieve the File reader context */
    *pContext= (M4OSA_Context)apContext;

    /* Compute file size */
    err = M4OSA_FileReader_CalculateSize(apContext);

    if (M4NO_ERROR != err) goto cleanup;

    return M4NO_ERROR;

cleanup:

    /* free context */
    if (M4OSA_NULL != apContext)
    {
        if(buffers_allocated == M4OSA_TRUE)
        {
            M4OSA_FileReader_BufferFree(apContext);
        }

        free( apContext);
        *pContext = M4OSA_NULL;
    }

    M4OSA_TRACE2_1 ("M4OSA_fileReadOpen_optim: returns error 0x%0x", err)
    return err;
}

/**
******************************************************************************
* @brief       This method reads the 'size' bytes in the core file reader (selected by its 'context')
*                      and writes the data to the 'data' pointer. If 'size' byte can not be read in the core file reader,
*                      'size' parameter is updated to match the correct number of read bytes.
* @param       pContext:       (IN) File reader context.
* @param       pData : (OUT) Data pointer of the read data.
* @param       pSize : (INOUT) Size of the data to read (in byte).
* @return      M4NO_ERROR: there is no error
* @return      M4ERR_PARAMETER pSize, fileDescriptor or pData is NULL
* @return      M4ERR_ALLOC     there is no more memory available
* @return      M4ERR_BAD_CONTEXT       provided context is not a valid one.
******************************************************************************
*/
M4OSA_ERR M4OSA_fileReadData_optim(M4OSA_Context pContext,M4OSA_MemAddr8 pData,
                                                            M4OSA_UInt32* pSize)
{
    M4OSA_FileReader_Context_optim* apContext =
                                     (M4OSA_FileReader_Context_optim*) pContext;

    M4OSA_ERR err;
    M4OSA_FilePosition aSize;
    M4OSA_FilePosition copiedSize;
    M4OSA_Int8 selected_buffer, current_buffer;

    M4OSA_TRACE3_3("M4OSA_fileReadData_optim p = 0x%p  d = 0x%p  s = %lu",
                                                       pContext, pData, *pSize);

    /* Check input parameters */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_BAD_CONTEXT, apContext);
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pData);
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pSize);

    if (apContext->IsOpened != M4OSA_TRUE)
    {
        return M4ERR_BAD_CONTEXT;
    }

    /* Prevent reading beyond EOF */
    if((*pSize > 0) && (apContext->absolutePos >= apContext->fileSize))
    {
        copiedSize = 0;
        err = M4WAR_NO_MORE_AU;
        goto cleanup;
    }

    /* Check if data can be read from a buffer */
    /* If not, fill one according to quantized positions */
    copiedSize = 0;
    err = M4NO_ERROR;

    selected_buffer = M4OSA_FileReader_BufferMatch(apContext, apContext->absolutePos);

    if(selected_buffer == M4OSA_READBUFFER_NONE)
    {
        selected_buffer = M4OSA_FileReader_BufferSelect(apContext, 0);
        err = M4OSA_FileReader_BufferFill(apContext, selected_buffer,
                                                        apContext->absolutePos);
    }

    if(err != M4NO_ERROR)
    {
        if(err == M4WAR_NO_DATA_YET)
        {
            if (*pSize <= (M4OSA_UInt32)apContext->buffer[selected_buffer].size)
            {
                err = M4NO_ERROR;
            }
            else
            {
                copiedSize = (M4OSA_UInt32)apContext->buffer[selected_buffer].size;
                /*copy the content into pData*/
                M4OSA_FileReader_BufferCopy(apContext, selected_buffer,
                                     apContext->absolutePos, copiedSize, pData);
                goto cleanup;
            }
        }
        else
        {
            goto cleanup;
        }
    }

    M4OSA_TRACE3_3("read  size = %lu  buffer = %d  pos = %ld", *pSize,
                                       selected_buffer, apContext->absolutePos);

    /* Copy buffer into pData */
    while(((M4OSA_UInt32)copiedSize < *pSize) && (err == M4NO_ERROR))
    {
        aSize = M4OSA_FileReader_BufferCopy(apContext, selected_buffer,
                                            apContext->absolutePos+copiedSize,
                                            *pSize-copiedSize, pData+copiedSize);
        copiedSize += aSize;

        if(aSize == 0)
        {
            err = M4WAR_NO_DATA_YET;
        }
        else
        {
            if((M4OSA_UInt32)copiedSize < *pSize)
            {
                current_buffer = selected_buffer;
                selected_buffer = M4OSA_FileReader_BufferMatch(apContext,
                                             apContext->absolutePos+copiedSize);

                if(selected_buffer == M4OSA_READBUFFER_NONE)
                {
                    selected_buffer = M4OSA_FileReader_BufferSelect(apContext,
                                                                current_buffer);
                    err = M4OSA_FileReader_BufferFill(apContext, selected_buffer,
                                             apContext->absolutePos+copiedSize);

                    if(err != M4NO_ERROR)
                    {
                        if(err == M4WAR_NO_DATA_YET)
                        {
                            /*If we got all the data that we wanted, we should return no error*/
                            if ((*pSize-copiedSize) <= (M4OSA_UInt32)apContext->buffer[selected_buffer].size)
                            {
                                err = M4NO_ERROR;
                            }
                            /*If we did not get enough data, we will return NO_DATA_YET*/

                            /*copy the data read*/
                            aSize = M4OSA_FileReader_BufferCopy(apContext, selected_buffer,
                                                               apContext->absolutePos+copiedSize,
                                                               *pSize-copiedSize, pData+copiedSize);
                            copiedSize += aSize;

                            /*we reached end of file, so stop trying to read*/
                            goto cleanup;
                        }
                        if (err == M4WAR_NO_MORE_AU)
                        {
                            err = M4WAR_NO_DATA_YET;

                            /*copy the data read*/
                            aSize = M4OSA_FileReader_BufferCopy(apContext, selected_buffer,
                                                             apContext->absolutePos+copiedSize,
                                                             *pSize-copiedSize, pData+copiedSize);
                            copiedSize += aSize;

                            /*we reached end of file, so stop trying to read*/
                            goto cleanup;

                        }
                        else
                        {
                            goto cleanup;
                        }
                    }
                }
            }
        }
    }

cleanup :

    /* Update the new position of the pointer */
    apContext->absolutePos = apContext->absolutePos + copiedSize;

    if((err != M4NO_ERROR)&&(err!=M4WAR_NO_DATA_YET))
    {
        M4OSA_TRACE2_3("M4OSA_fileReadData_optim size = %ld  copied = %ld  err = 0x%x",
                                                           *pSize, copiedSize, err);
    }

    /* Effective copied size must be returned */
    *pSize = copiedSize;


    /* Read is done */
    return err;
}

/**
******************************************************************************
* @brief       This method seeks at the provided position in the core file reader (selected by its 'context').
*              The position is related to the seekMode parameter it can be either :
*              From the beginning (position MUST be positive) : end position = position
*              From the end (position MUST be negative) : end position = file size + position
*              From the current position (signed offset) : end position = current position + position.
* @param       pContext:       (IN) File reader context.
* @param       SeekMode :      (IN) Seek access mode.
* @param       pPosition :     (IN) Position in the file.
* @return      M4NO_ERROR: there is no error
* @return      M4ERR_PARAMETER Seekmode or fileDescriptor is NULL
* @return      M4ERR_ALLOC     there is no more memory available
* @return      M4ERR_BAD_CONTEXT       provided context is not a valid one.
* @return      M4ERR_FILE_INVALID_POSITION the position cannot be reached.
******************************************************************************
*/
M4OSA_ERR M4OSA_fileReadSeek_optim( M4OSA_Context pContext, M4OSA_FileSeekAccessMode SeekMode,
                                                              M4OSA_FilePosition* pPosition)
{
    M4OSA_FileReader_Context_optim* apContext = (M4OSA_FileReader_Context_optim*) pContext;
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_TRACE3_3("M4OSA_fileReadSeek_optim p = 0x%p mode = %d pos = %d", pContext,
                                                             SeekMode, *pPosition);

    /* Check input parameters */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_BAD_CONTEXT, apContext);
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pPosition);
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, SeekMode);

    if (apContext->IsOpened != M4OSA_TRUE)
    {
        return M4ERR_BAD_CONTEXT;       /*< The context can not be correct */
    }

    /* Go to the desired position */
    switch(SeekMode)
    {
        case M4OSA_kFileSeekBeginning :
            if(*pPosition < 0) {
                return M4ERR_PARAMETER; /**< Bad SeekAcess mode */
            }
            apContext->absolutePos = *pPosition;
            *pPosition = apContext->absolutePos;
            break;

        case M4OSA_kFileSeekEnd :
            if(*pPosition > 0) {
                return M4ERR_PARAMETER; /**< Bad SeekAcess mode */
            }
            apContext->absolutePos = apContext->fileSize + *pPosition;
            *pPosition = apContext->absolutePos;
            break;

        case M4OSA_kFileSeekCurrent :
            if(((apContext->absolutePos + *pPosition) > apContext->fileSize) ||
                ((apContext->absolutePos + *pPosition) < 0)){
                return M4ERR_PARAMETER; /**< Bad SeekAcess mode */
            }
            apContext->absolutePos = apContext->absolutePos + *pPosition;
            *pPosition = apContext->absolutePos;
            break;

        default :
            err = M4ERR_PARAMETER; /**< Bad SeekAcess mode */
            break;
    }

    /* Return without error */
    return err;
}

/**
******************************************************************************
* @brief       This method asks the core file reader to close the file
*              (associated to the context) and also frees the context.
* @param       pContext:       (IN) File reader context.
* @return      M4NO_ERROR: there is no error
* @return      M4ERR_BAD_CONTEXT       provided context is not a valid one.
******************************************************************************
*/
M4OSA_ERR M4OSA_fileReadClose_optim(M4OSA_Context pContext)
{
    M4OSA_FileReader_Context_optim* apContext = (M4OSA_FileReader_Context_optim*) pContext;

    M4OSA_ERR err = M4NO_ERROR;
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    M4OSA_ERR errno = M4NO_ERROR;
#else
    M4OSA_UInt16 errno;
#endif

    M4OSA_TRACE2_1("M4OSA_fileReadClose_optim p = 0x%p", pContext );

    /* Check input parameters */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_BAD_CONTEXT, apContext);

    if (apContext->IsOpened != M4OSA_TRUE)
    {
        return M4ERR_BAD_CONTEXT;       /**< The context can not be correct */
    }

    /* buffer */
    M4OSA_FileReader_BufferFree(apContext);

    /* Close the file */
#ifdef M4OSA_READER_OPTIM_USE_OSAL_IF
    errno = apContext->FS->closeRead(apContext->aFileDesc);

    if (M4NO_ERROR != errno)
    {
        /* converts the error to PSW format*/
        err = errno;
        M4OSA_TRACE2_1("M4OSA_fileReadClose_optim ERR1 = 0x%x", err);
    }
#else
    aRet_Val = apContext->FS->pFctPtr_Close(apContext->aFileDesc, &errno);

    if (aRet_Val != 0)
    {
        /* converts the error to PSW format*/
        err = M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_READER, errno);
        M4OSA_TRACE2_1("M4OSA_fileReadClose_optim ERR1 = 0x%x", err);
    }
#endif /*M4OSA_READER_OPTIM_USE_OSAL_IF*/

    apContext->IsOpened = M4OSA_FALSE;

    //>>>> GLM20090212 : set the low level function statically
    if (apContext->FS != M4OSA_NULL)
    {
        free( apContext->FS);
    }
    //<<<< GLM20090212 : set the low level function statically

    /* Free the context */
    free(apContext);

    /* Return without error */
    return err;
}

/**
******************************************************************************
* @brief       This is a dummy function required to maintain function pointer
*              structure.
* @note        This is a dummy function required to maintain function pointer
*              structure.
* @param       pContext:       (IN) Execution context.
* @param       OptionId :      (IN) Id of the option to set.
* @param       OptionValue :   (IN) Value of the option.
* @return      M4NO_ERROR: there is no error
******************************************************************************
*/
M4OSA_ERR M4OSA_fileReadSetOption_optim(M4OSA_Context pContext,
                                        M4OSA_FileReadOptionID OptionID,
                                        M4OSA_DataOption OptionValue)
{
    M4OSA_ERR err = M4NO_ERROR;
    return err;
}

/**
******************************************************************************
* @brief       This method asks the core file reader to return the value associated
*              with the optionID.The caller is responsible for allocating/de-allocating
*              the memory of the value field.
* @note        The options handled by the component depend on the implementation
*                                                               of the component.
* @param       pContext:       (IN) Execution context.
* @param       OptionId :      (IN) Id of the option to set.
* @param       pOptionValue :  (OUT) Value of the option.
* @return      M4NO_ERROR: there is no error
* @return      M4ERR_BAD_CONTEXT       pContext is NULL
* @return      M4ERR_BAD_OPTION_ID the option id is not valid.
* @return      M4ERR_NOT_IMPLEMENTED The option is not implemented yet.
******************************************************************************
*/
M4OSA_ERR M4OSA_fileReadGetOption_optim(M4OSA_Context pContext,
                                        M4OSA_FileReadOptionID OptionID,
                                        M4OSA_DataOption* pOptionValue)
{
    M4OSA_FileReader_Context_optim* apContext = (M4OSA_FileReader_Context_optim*) pContext;
    M4OSA_ERR err = M4NO_ERROR;

    /*  Check input parameters */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_BAD_CONTEXT, apContext);

    if (apContext->IsOpened != M4OSA_TRUE)
    {
        return M4ERR_BAD_CONTEXT;       /**< The context can not be correct */
    }

    /* Get the desired option if it is avalaible */
    switch(OptionID)
    {
        /* Get File Size */
        case M4OSA_kFileReadGetFileSize:/**< Get size of the file, limited to 32 bit size */

            (*(M4OSA_UInt32 *)pOptionValue) = apContext->fileSize;
            break;

        /* Check End of file Occurs */
        case M4OSA_kFileReadIsEOF :     /**< See if we are at the end of the file */

            (*(M4OSA_Bool *)pOptionValue) = (apContext->absolutePos >= apContext->fileSize) ? M4OSA_TRUE : M4OSA_FALSE;
            break;

        /* Get File Position */
        case M4OSA_kFileReadGetFilePosition :   /**< Get file position */

            *(M4OSA_FilePosition *)pOptionValue = apContext->absolutePos;
            break;

        /* Get Attribute */
        case M4OSA_kFileReadGetFileAttribute :  /**< Get the file attribute = access mode */

            (*(M4OSA_FileAttribute *)pOptionValue).modeAccess = apContext->FileAttribute.modeAccess;
            break;

        default:
            /**< Bad option ID */
            err = M4ERR_BAD_OPTION_ID;
            break;
    }

    /*Return without error */
    return err;
}
