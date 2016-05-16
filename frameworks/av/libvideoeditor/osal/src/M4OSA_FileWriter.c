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
 ************************************************************************
 * @file         M4OSA_FileWriter.c
 * @brief        File writer for Android
 * @note         This file implements functions to write in a file.
 ************************************************************************
*/

#include "M4OSA_Debug.h"
#include "M4OSA_FileCommon_priv.h"
#include "M4OSA_FileWriter.h"
#include "M4OSA_FileWriter_priv.h"
#include "M4OSA_Memory.h"

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
#include "M4OSA_Semaphore.h"
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

/**
 ************************************************************************
 * @brief      This function opens the provided URL and returns its context.
 *             If an error occured, the context is set to NULL.
 * @param      pContext: (OUT) Context of the core file writer
 * @param      pUrl: (IN) URL of the input file
 * @param      fileModeAccess: (IN) File mode access
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_ALLOC: there is no more memory available
 * @return     M4ERR_NOT_IMPLEMENTED: the URL does not match with the supported
 *             file
 * @return     M4ERR_FILE_NOT_FOUND: the file cannot be found
 * @return     M4ERR_FILE_LOCKED: the file is locked by an other
 *             application/process
 * @return     M4ERR_FILE_BAD_MODE_ACCESS: the file mode access is not correct
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileWriteOpen(M4OSA_Context* pContext, M4OSA_Void* pUrl,
                              M4OSA_UInt32 fileModeAccess)
{
    M4OSA_TRACE1_3("M4OSA_fileWriteOpen : pC = 0x%p  fd = 0x%p  mode = %d",
                                                pContext, pUrl, fileModeAccess);

    return M4OSA_fileCommonOpen(M4OSA_FILE_WRITER, pContext, pUrl,
                                fileModeAccess);
}


/**
 ************************************************************************
 * @brief      This function writes the 'size' bytes stored at 'data' memory
 *             in the file selected by its context.
 * @note       The caller is responsible for allocating/de-allocating the
 *             memory for 'data' parameter.
 * @note       Moreover the data pointer must be allocated to store at least
 *             'size' bytes.
 * @param      pContext: (IN/OUT) Context of the core file reader
 * @param      buffer: (IN) Data pointer of the write data
 * @param      size: (IN) Size of the data to write (in bytes)
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileWriteData(M4OSA_Context pContext, M4OSA_MemAddr8 data,
                              M4OSA_UInt32 uiSize)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_ERR err;
    M4OSA_UInt32 uiSizeWrite;

    M4OSA_TRACE2_2("M4OSA_fileWriteData : data = 0x%p  size = %lu", data,
                                                                        uiSize);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                 "M4OSA_fileWriteData: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == data, M4ERR_PARAMETER,
                                     "M4OSA_fileWriteData: data is M4OSA_NULL");
    M4OSA_DEBUG_IF2(0 == uiSize, M4ERR_PARAMETER,
                                            "M4OSA_fileWriteData: uiSize is 0");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context,
                                  M4ERR_BAD_CONTEXT,
                                  "M4OSA_fileWriteData: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if (M4OSA_kDescRWAccess == pFileContext->m_DescrModeAccess)
    {
        M4OSA_UInt32    WriteSize;
        err = M4NO_ERROR;
        WriteSize = fwrite((void *)data,1, uiSize, pFileContext->file_desc);
        if(WriteSize != uiSize)
        {
            /* converts the error to PSW format*/
            err = ((M4OSA_UInt32)(M4_ERR)<<30)+(((M4OSA_FILE_WRITER)&0x003FFF)<<16)+(M4OSA_Int16)(WriteSize);
            M4OSA_TRACE1_1("M4OSA_FileWriteData error:%x",err);
        }
        fflush(pFileContext->file_desc);

        pFileContext->write_position = pFileContext->write_position + WriteSize;

        /* Update the file size */
        if(pFileContext->write_position > pFileContext->file_size)
        {
            pFileContext->file_size = pFileContext->write_position;
        }
        return err;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if(pFileContext->current_seek != SeekWrite)
    {
        /* fseek to the last read position */
        err = M4OSA_fileCommonSeek(pContext, M4OSA_kFileSeekBeginning,
            &(pFileContext->write_position));

        if(M4OSA_ERR_IS_ERROR(err))
        {
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */
            M4OSA_DEBUG(err, "M4OSA_fileWriteData: M4OSA_fileCommonSeek");
            return err;
        }

        pFileContext->current_seek = SeekWrite;
    }

    /* Write data */
    uiSizeWrite = fwrite(data, sizeof(M4OSA_Char), uiSize, pFileContext->file_desc);

    if(uiSizeWrite == (M4OSA_UInt32)-1)
    {
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
        M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

        /* An error occured */

        M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_fileWriteData: fwrite failed");
        return M4ERR_BAD_CONTEXT;
    }

    pFileContext->write_position = pFileContext->write_position + uiSizeWrite;

    /* Update the file size */
    if(pFileContext->write_position > pFileContext->file_size)
    {
        pFileContext->file_size = pFileContext->write_position;
    }

    if((M4OSA_UInt32)uiSizeWrite < uiSize)
    {
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
        M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileWriteData");
        return M4ERR_ALLOC;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function seeks at the provided position in the core file
 *             writer (selected by its 'context'). The position is related to
 *             the seekMode parameter it can be either from the beginning,
 *             from the end or from the current postion. To support large file
 *             access (more than 2GBytes), the position is provided on a 64
 *             bits.
 * @note       If this function returns an error the current position pointer
 *             in the file must not change. Else the current position pointer
 *             must be updated.
 * @param      pContext: (IN/OUT) Context of the core file reader
 * @param      seekMode: (IN) Seek access mode
 * @param      position: (IN/OUT) Position in the file
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 * @return     M4ERR_FILE_INVALID_POSITION: the position cannot be reached
 ************************************************************************
                              */
M4OSA_ERR M4OSA_fileWriteSeek(M4OSA_Context pContext, M4OSA_FileSeekAccessMode seekMode,
                              M4OSA_FilePosition* pPosition)
{
    M4OSA_FileContext* pFileContext = (M4OSA_FileContext*)pContext;
    M4OSA_ERR err;

    M4OSA_TRACE2_2("M4OSA_fileWriteSeek : mode = %d  pos = %lu",
                        seekMode, (M4OSA_NULL != pPosition) ? (*pPosition) : 0);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                 "M4OSA_fileWriteSeek: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(0 == seekMode, M4ERR_PARAMETER,
                                          "M4OSA_fileWriteSeek: seemMode is 0");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pPosition, M4ERR_PARAMETER,
                                "M4OSA_fileWriteSeek: pPosition is M4OSA_NULL");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context, M4ERR_BAD_CONTEXT,
                        "M4OSA_fileWriteSeek: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if (M4OSA_kDescRWAccess == pFileContext->m_DescrModeAccess) /* read write */
    {
         M4OSA_UInt32    SeekModeOption;
        /*The position for the seek mode between the SHP and the OSAl part are different */
        if (M4OSA_kFileSeekBeginning == seekMode)
        {
            SeekModeOption = SEEK_SET;
        }
        else if (M4OSA_kFileSeekEnd == seekMode)
        {
            SeekModeOption = SEEK_END;
        }
        else if (M4OSA_kFileSeekCurrent == seekMode)
        {
            SeekModeOption = SEEK_CUR;
        }
        else
        {
            M4OSA_TRACE1_0("M4OSA_fileWriteSeek: END WITH ERROR !!! (CONVERION ERROR FOR THE SEEK MODE) ");
            return M4ERR_PARAMETER;
        }

        /**
         * Go to the desired position */
        err = fseek(pFileContext->file_desc,*pPosition,SeekModeOption);
        if(err != 0)
        {
            /* converts the error to PSW format*/
            err=((M4OSA_UInt32)(M4_ERR)<<30)+(((M4OSA_FILE_WRITER)&0x003FFF)<<16)+(M4OSA_Int16)(err);
            M4OSA_TRACE1_1("M4OSA_FileWriteSeek error:%x",err);
        }
        else
        {
            return M4NO_ERROR;
        }

        return err;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    err = M4OSA_fileCommonSeek(pContext, seekMode, pPosition);

    if(M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_DEBUG(err, "M4OSA_fileWriteSeek: M4OSA_fileCommonSeek");

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
        M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

        return err;
    }

    pFileContext->write_position = *pPosition;

    pFileContext->current_seek = SeekWrite;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function asks the core file writer to close the file
 *             (associated to the context).
 * @note       The context of the core file writer is freed.
 * @param      pContext: (IN/OUT) Context of the core file writer
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
************************************************************************
*/

M4OSA_ERR M4OSA_fileWriteClose(M4OSA_Context pContext)
{
    M4OSA_FileContext* pFileContext = (M4OSA_FileContext*)pContext;

    M4OSA_TRACE1_1("M4OSA_fileWriteClose : pC = 0x%p", pContext);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                "M4OSA_fileWriteClose: pContext is M4OSA_NULL");

    return M4OSA_fileCommonClose(M4OSA_FILE_WRITER, pContext);
}


/**
 ************************************************************************
 * @brief      This function flushes the stream associated to the context.
 * @param      pContext: (IN/OUT) Context of the core file writer
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileWriteFlush(M4OSA_Context pContext)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_ERR    err = M4NO_ERROR;

    M4OSA_TRACE2_1("M4OSA_fileWriteFlush : pC = 0x%p", pContext);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                "M4OSA_fileWriteFlush: pcontext is M4OSA_NULL");

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context, M4ERR_BAD_CONTEXT,
                       "M4OSA_fileWriteFlush: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if (fflush(pFileContext->file_desc) != 0)
    {
        err = M4ERR_BAD_CONTEXT;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    return err;
}


/**
 ************************************************************************
 * @brief      This function asks the core file writer to return the value
 *             associated with the optionID.
 *             The caller is responsible for allocating/de-allocating the
 *             memory of the value field.
 * @note       'value' must be cast according to the type related to the
 *             optionID
 *             As the caller is responsible for allocating/de-allocating the
 *             'value' field, the callee must copy this field
 *             to its internal variable.
 * @param      pContext: (IN/OUT) Context of the core file writer
 * @param      optionID: (IN) ID of the option
 * @param      value: (OUT) Value of the option
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_BAD_OPTION_ID: the optionID is not a valid one
 * @return     M4ERR_WRITE_ONLY: this option is a write only one
 * @return     M4ERR_NOT_IMPLEMENTED: this option is not implemented
************************************************************************
*/

M4OSA_ERR M4OSA_fileWriteGetOption(M4OSA_Context pContext, M4OSA_OptionID optionID,
                                   M4OSA_DataOption* pOptionValue)
{
    M4OSA_FileContext* pFileContext = pContext;

    M4OSA_TRACE2_1("M4OSA_fileWriteGetOption : option = 0x%x", optionID);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                            "M4OSA_fileWriteGetOption: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(optionID == 0, M4ERR_PARAMETER, "M4OSA_fileWriteGetOption");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pOptionValue, M4ERR_PARAMETER,
                         "M4OSA_fileWriteGetOption: pOtionValue is M4OSA_NULL");

    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_FILE_WRITER),
                               M4ERR_BAD_OPTION_ID, "M4OSA_fileWriteGetOption");
    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_READABLE(optionID), M4ERR_WRITE_ONLY,
                                                    "M4OSA_fileWriteGetOption");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context, M4ERR_BAD_CONTEXT,
                   "M4OSA_fileWriteGetOption: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    switch(optionID)
    {
#if(M4OSA_OPTIONID_FILE_WRITE_GET_FILE_POSITION == M4OSA_TRUE)
    case M4OSA_kFileWriteGetFilePosition:
        {
            M4OSA_FilePosition* position = (M4OSA_FilePosition*)pOptionValue;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            *position = pFileContext->write_position;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_WRITE_GET_FILE_POSITION*/

#if(M4OSA_OPTIONID_FILE_WRITE_GET_FILE_SIZE == M4OSA_TRUE)
    case M4OSA_kFileWriteGetFileSize:
        {
            M4OSA_FilePosition* position = (M4OSA_FilePosition*)pOptionValue;

            if(M4OSA_kDescRWAccess == pFileContext->m_DescrModeAccess)
            {
                M4OSA_Int32 iSavePos    = 0;
                M4OSA_Int32 iSize        = 0;

                iSavePos = ftell(pFileContext->file_desc);            /*1- Check the first position */
                fseek(pFileContext->file_desc, 0, SEEK_END);        /*2- Go to the end of the file */
                *position = ftell(pFileContext->file_desc);            /*3- Check the file size*/
                fseek(pFileContext->file_desc, iSavePos, SEEK_SET);    /*4- go to the first position*/
                return M4NO_ERROR;
            }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            *position = pFileContext->file_size;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_WRITE_GET_FILE_SIZE*/

#if(M4OSA_OPTIONID_FILE_WRITE_GET_URL == M4OSA_TRUE)
    case M4OSA_kFileWriteGetURL:
        {
            return M4OSA_fileCommonGetURL (pContext, (M4OSA_Char**)pOptionValue);
        }
#endif /*M4OSA_OPTIONID_FILE_WRITE_GET_URL*/

#if(M4OSA_OPTIONID_FILE_WRITE_GET_FILE_ATTRIBUTE == M4OSA_TRUE)
    case M4OSA_kFileWriteGetAttribute:
        {
            return M4OSA_fileCommonGetAttribute(pContext,
                (M4OSA_FileAttribute*)pOptionValue);
        }
#endif /*M4OSA_OPTIONID_FILE_WRITE_GET_FILE_ATTRIBUTE*/

#if(M4OSA_OPTIONID_FILE_WRITE_GET_READER_CONTEXT == M4OSA_TRUE)
    case M4OSA_kFileWriteGetReaderContext:
        {
            M4OSA_FileModeAccess access = pFileContext->access_mode;

            M4OSA_DEBUG_IF1(!(access & M4OSA_kFileRead), M4ERR_BAD_CONTEXT,
                "M4OSA_fileWriteGetOption: M4OSA_kFileRead");

            M4OSA_DEBUG_IF1(!(access & M4OSA_kFileWrite), M4ERR_BAD_CONTEXT,
                "M4OSA_fileWriteGetOption: M4OSA_kFileWrite");

            pFileContext->coreID_read = M4OSA_FILE_READER;

            *pOptionValue = pContext;

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_WRITE_GET_READER_CONTEXT*/

    case M4OSA_kFileWriteLockMode:
        {
            *(M4OSA_UInt32*)pOptionValue = pFileContext->m_uiLockMode;
            return M4NO_ERROR;
        }

    }

    M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_fileWriteGetOption");

    return M4ERR_NOT_IMPLEMENTED;
}


/**
************************************************************************
* @brief      This function asks the core file writer to set the value
*             associated with the optionID.
*             The caller is responsible for allocating/de-allocating the
*             memory of the value field.
* @note       As the caller is responsible for allocating/de-allocating the
*             'value' field, the callee must copy this field to its internal
*             variable.
* @param      pContext: (IN/OUT) Context of the core file writer
* @param      optionID: (IN) ID of the option
* @param      value: (IN) Value of the option
* @return     M4NO_ERROR: there is no error
* @return     M4ERR_PARAMETER: at least one parameter is NULL
* @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
* @return     M4ERR_BAD_OPTION_ID: the optionID is not a valid one
* @return     M4ERR_READ_ONLY: this option is a read only one
* @return     M4ERR_NOT_IMPLEMENTED: this option is not implemented
************************************************************************
*/

M4OSA_ERR M4OSA_fileWriteSetOption(M4OSA_Context pContext,
                                   M4OSA_OptionID optionID,
                                   M4OSA_DataOption optionValue)
{
    M4OSA_FileContext* pFileContext = pContext;

    M4OSA_TRACE2_1("M4OSA_fileWriteSetOption : option = 0x%x", optionID);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                                    "M4OSA_fileWriteSetOption");

    M4OSA_DEBUG_IF2(0 == optionID, M4ERR_PARAMETER, "M4OSA_fileWriteSetOption");

    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_FILE_WRITER),
        M4ERR_BAD_OPTION_ID, "M4OSA_fileWriteSetOption");

    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_WRITABLE(optionID), M4ERR_READ_ONLY,
                                                     "M4OSA_fileReadSetOption");

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context, M4ERR_BAD_CONTEXT,
                   "M4OSA_fileWriteSetOption: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    switch(optionID)
    {
        case M4OSA_kFileWriteLockMode:
        {
            pFileContext->m_uiLockMode = (M4OSA_UInt32)*(M4OSA_UInt32*)optionValue;
            return M4NO_ERROR;
        }

        case M4OSA_kFileWriteDescMode:
        {
            pFileContext->m_DescrModeAccess = (M4OSA_Int32)*(M4OSA_Int32*)optionValue;
            return M4NO_ERROR;
        }

        default:
            return M4ERR_NOT_IMPLEMENTED;
    }

    return M4ERR_NOT_IMPLEMENTED;
}

