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
 * @file         M4OSA_FileReader.c
 * @author       Cedric Lecoutre (cedric.lecoutre@philips.com)
 *               Laurent Fay (laurent.fay@philips.com)
 * @par Org:     Philips Digital Systems Laboratories - Paris (PDSL-P)
 * @brief        File reader for Android
 * @note         This file implements functions to read a file.
 ************************************************************************
*/


#include "M4OSA_Debug.h"
#include "M4OSA_FileCommon_priv.h"
#include "M4OSA_FileReader.h"
#include "M4OSA_FileReader_priv.h"
#include "M4OSA_Memory.h"

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
#include "M4OSA_Semaphore.h"
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */


/**
************************************************************************
* @brief      This function opens the provided URL and returns its context.
*             If an error occured, the context is set to NULL.
* @param      context: (OUT) Context of the core file reader
* @param      url: (IN) URL of the input file
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
M4OSA_ERR M4OSA_fileReadOpen(M4OSA_Context* pContext, M4OSA_Void* pFileDescriptor,
                             M4OSA_UInt32 fileModeAccess)
{
    M4OSA_TRACE1_3("M4OSA_fileReadOpen : pC = 0x%p  fd = 0x%p  mode = %lu",
                                     pContext, pFileDescriptor, fileModeAccess);

    return M4OSA_fileCommonOpen(M4OSA_FILE_READER, pContext,
                                               pFileDescriptor, fileModeAccess);
}

/**
************************************************************************
* @brief      This function reads the 'size' bytes in the core file reader
*             (selected by its 'context') and writes the data to the 'data'
*             pointer.
* @note       If 'size' byte cannot be read in the core file reader, 'size'
*             parameter is updated to match the correct
* @note       number of read bytes.
* @param      context: (IN/OUT) Context of the core file reader
* @param      buffer: (OUT) Data pointer of the read data
* @param      size: (IN/OUT) Size of the data to read (in bytes)
* @return     M4NO_ERROR: there is no error
* @return     M4ERR_PARAMETER: at least one parameter is NULL
* @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
* @return     M4ERR_ALLOC: there is no more memory available
* @return     M4WAR_NO_DATA_YET: there is no enough data to fill the 'data'
*             buffer, so the size parameter has been updated.
************************************************************************
*/
M4OSA_ERR M4OSA_fileReadData(M4OSA_Context pContext, M4OSA_MemAddr8 data,
                                                            M4OSA_UInt32* pSize)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_ERR    err = M4NO_ERROR;
    M4OSA_Int32    uiSizeRead;

    M4OSA_TRACE2_2("M4OSA_fileReadData : data = 0x%p  size = %lu",
                                    data, (M4OSA_NULL != pSize) ? (*pSize) : 0);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                  "M4OSA_fileReadData: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == data, M4ERR_PARAMETER,
                                      "M4OSA_fileReadData: data is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pSize, M4ERR_PARAMETER,
                                     "M4OSA_fileReadData: pSize is M4OSA_NULL");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context,
      M4ERR_BAD_CONTEXT, "M4OSA_fileReadData: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if(M4OSA_kDescRWAccess == pFileContext->m_DescrModeAccess) /* read write */
    {
        uiSizeRead = fread(data, sizeof(M4OSA_Char), *pSize,
                                                       pFileContext->file_desc);
        if(-1 == uiSizeRead)
        {
            /* handle is invalid, or the file is not open for reading, or the file is locked */
            *pSize = 0;
            err = M4ERR_BAD_CONTEXT;
        }
        else
        {
            pFileContext->read_position = pFileContext->read_position + uiSizeRead;
            if ((M4OSA_UInt32)uiSizeRead < *pSize)
            {
                *pSize = uiSizeRead;
                /* This is the end of file */
                pFileContext->b_is_end_of_file = M4OSA_TRUE;
                err = M4WAR_NO_DATA_YET;
            }
            else
            {
                *pSize = uiSizeRead;
            }
        }

        return err;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if(pFileContext->current_seek != SeekRead)
    {
        /* fseek to the last read position */
        err = M4OSA_fileCommonSeek(pContext, M4OSA_kFileSeekBeginning,
                                                &(pFileContext->read_position));
        if(M4OSA_ERR_IS_ERROR(err))
        {
            M4OSA_DEBUG(err, "M4OSA_fileReadData: M4OSA_fileCommonSeek");

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return err;
        }

        pFileContext->current_seek = SeekRead;
    }

    /* Read data */
    uiSizeRead = fread(data, sizeof(M4OSA_Char), *pSize,
                                                       pFileContext->file_desc);
    if(-1 == uiSizeRead)
    {
        /* handle is invalid, or the file is not open for reading,
         or the file is locked */
        *pSize = 0;
        err = M4ERR_BAD_CONTEXT;
    }
    else
    {
        pFileContext->read_position = pFileContext->read_position + uiSizeRead;
        if ((M4OSA_UInt32)uiSizeRead < *pSize)
        {
            *pSize = uiSizeRead;

            /* This is the end of file */
            pFileContext->b_is_end_of_file = M4OSA_TRUE;

            err = M4WAR_NO_DATA_YET;
        }
        else
        {
            *pSize = uiSizeRead;
        }
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */


    return err;
}


/**
************************************************************************
 * @brief      This function seeks at the provided position in the core file
 *             reader (selected by its 'context'). The position is related to
 *             the seekMode parameter it can be either from the beginning, from
 *             the end or from the current postion. To support large file
 *             access (more than 2GBytes), the position is provided on a 64
 *             bits.
 * @note       If this function returns an error the current position pointer
 *             in the file must not change. Else the current
 *             position pointer must be updated.
 * @param      context: (IN/OUT) Context of the core file reader
 * @param      seekMode: (IN) Seek access mode
 * @param      position: (IN/OUT) Position in the file
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 * @return     M4ERR_FILE_INVALID_POSITION: the position cannot be reached
 ************************************************************************
*/

M4OSA_ERR M4OSA_fileReadSeek(M4OSA_Context pContext, M4OSA_FileSeekAccessMode seekMode,
                             M4OSA_FilePosition* pPosition)
{
    M4OSA_FileContext* pFileContext = (M4OSA_FileContext*)pContext;
    M4OSA_ERR err;

    M4OSA_TRACE2_2("M4OSA_fileReadSeek : mode = %d  pos = %lu", seekMode,
                                  (pPosition != M4OSA_NULL) ? (*pPosition) : 0);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                  "M4OSA_fileReadSeek: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(0 == seekMode, M4ERR_PARAMETER,
                                           "M4OSA_fileReadSeek: seekMode is 0");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pPosition, M4ERR_PARAMETER,
                                 "M4OSA_fileReadSeek: pPosition is M4OSA_NULL");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context,
      M4ERR_BAD_CONTEXT, "M4OSA_fileReadSeek: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if (M4OSA_kDescRWAccess == pFileContext->m_DescrModeAccess)
    {
         M4OSA_UInt32    SeekModeOption;
         /* Go to the desired position */
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
            M4OSA_TRACE1_0("M4OSA_fileReadSeek: END WITH ERROR !!! (CONVERION ERROR FOR THE SEEK MODE)");
            return M4ERR_PARAMETER;
        }

        /**
         * Go to the desired position */
        err = fseek(pFileContext->file_desc, *pPosition, SeekModeOption);
        if(err != 0)
        {
            /* converts the error to PSW format*/
            err=((M4OSA_UInt32)(M4_ERR)<<30)+(((M4OSA_FILE_WRITER)&0x003FFF)<<16)+(M4OSA_Int16)(err);
            M4OSA_TRACE1_1("M4OSA_FileReadSeek error:%x",err);
        }
        else
        {
            return M4NO_ERROR;
        }

        /* Return without error */
        return err;
    }


#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    if(pFileContext->current_seek != SeekRead)
    {

        /* fseek to the last read position */
        err = M4OSA_fileCommonSeek(pContext, M4OSA_kFileSeekBeginning,
                                                &(pFileContext->read_position));
        if(M4OSA_ERR_IS_ERROR(err))
        {
            M4OSA_DEBUG(err, "M4OSA_fileReadData: M4OSA_fileCommonSeek");

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return err;
        }

        pFileContext->current_seek = SeekRead;
    }

    err = M4OSA_fileCommonSeek(pContext, seekMode, pPosition);
    if(M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_DEBUG(err, "M4OSA_fileReadData: M4OSA_fileCommonSeek");
    }
    else
    {
        pFileContext->read_position = *pPosition;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    return err;
}


/**
 ************************************************************************
 * @brief      This function asks the core file reader to close the file
 *             (associated to the context).
 * @note       The context of the core file reader is freed.
 * @param      pContext: (IN/OUT) Context of the core file reader
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileReadClose(M4OSA_Context pContext)
{
    M4OSA_FileContext* pFileContext = (M4OSA_FileContext*)pContext;

    M4OSA_TRACE1_1("M4OSA_fileReadClose : pC = 0x%p", pContext);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                 "M4OSA_fileReadClose: pContext is M4OSA_NULL");

    if(M4OSA_FILE_WRITER == pFileContext->coreID_write)
    {
        return M4NO_ERROR;
    }

    return M4OSA_fileCommonClose(M4OSA_FILE_READER, pContext);
}




/**
 ************************************************************************
 * @brief      This function asks the core file reader to return the value
 *             associated with the optionID. The caller is responsible for
 *             allocating/de-allocating the memory of the value field.
 * @note       'value' must be cast according to the type related to the
 *             optionID As the caller is responsible for
 *             allocating/de-allocating the 'value' field, the callee must copy
 *             this field to its internal variable.
 * @param      pContext: (IN/OUT) Context of the core file reader
 * @param      pOptionID: (IN) ID of the option
 * @param      pOptionValue: (OUT) Value of the option
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_BAD_OPTION_ID: the optionID is not a valid one
 * @return     M4ERR_WRITE_ONLY: this option is a write only one
 * @return     M4ERR_NOT_IMPLEMENTED: this option is not implemented
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileReadGetOption(M4OSA_Context pContext, M4OSA_FileReadOptionID optionID,
                                  M4OSA_DataOption* pOptionValue)
{
    M4OSA_FileContext* pFileContext = pContext;

    M4OSA_TRACE2_1("M4OSA_fileReadGetOption : option = 0x%x", optionID);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                             "M4OSA_fileReadGetOption: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(optionID == 0, M4ERR_PARAMETER,
                                      "M4OSA_fileReadGetOption: optionID is 0");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pOptionValue, M4ERR_PARAMETER,
                         "M4OSA_fileReadGetOption: pOptionValue is M4OSA_NULL");

    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_FILE_READER),
                                M4ERR_BAD_OPTION_ID, "M4OSA_fileReadGetOption");
    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_READABLE(optionID),
                                   M4ERR_WRITE_ONLY, "M4OSA_fileReadGetOption");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context,
                                  M4ERR_BAD_CONTEXT,
                                  "M4OSA_fileReadGetOption: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    switch(optionID)
    {
#if(M4OSA_OPTIONID_FILE_READ_GET_FILE_POSITION == M4OSA_TRUE)
    case M4OSA_kFileReadGetFilePosition:
        {
            M4OSA_FilePosition* pPosition = (M4OSA_FilePosition*)pOptionValue;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            *pPosition = pFileContext->read_position;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_READ_GET_FILE_POSITION*/

#if(M4OSA_OPTIONID_FILE_READ_IS_EOF == M4OSA_TRUE)
    case M4OSA_kFileReadIsEOF:
        {
            M4OSA_Bool* bIsEndOfFile = (M4OSA_Bool*)pOptionValue;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            *bIsEndOfFile = pFileContext->b_is_end_of_file;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_READ_IS_EOF*/


#if(M4OSA_OPTIONID_FILE_READ_GET_FILE_SIZE == M4OSA_TRUE)
    case M4OSA_kFileReadGetFileSize:
        {
            M4OSA_FilePosition* pPosition = (M4OSA_FilePosition*)pOptionValue;
            M4OSA_Int32 iSavePos    = 0;
            M4OSA_Int32 iSize        = 0;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphoreWait(pFileContext->semaphore_context, M4OSA_WAIT_FOREVER);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */
            /**
            * Bugfix: update the file size.
            * When a file is in read mode, may be another application is writing in.
            * So, we have to update the file size */
            iSavePos = ftell(pFileContext->file_desc);            /*1- Check the first position */
            fseek(pFileContext->file_desc, 0, SEEK_END);        /*2- Go to the end of the file */
            iSize = ftell(pFileContext->file_desc);                /*3- Check the file size*/
            fseek(pFileContext->file_desc, iSavePos, SEEK_SET);    /*4- go to the first position*/
            pFileContext->file_size = iSize;

            *pPosition = pFileContext->file_size;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
            M4OSA_semaphorePost(pFileContext->semaphore_context);
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

            return M4NO_ERROR;
        }
#endif /*M4OSA_OPTIONID_FILE_READ_GET_FILE_SIZE*/

#if(M4OSA_OPTIONID_FILE_READ_GET_FILE_ATTRIBUTE == M4OSA_TRUE)
    case M4OSA_kFileReadGetFileAttribute:
        {
            return M4OSA_fileCommonGetAttribute(pContext,
                                            (M4OSA_FileAttribute*)pOptionValue);
        }
#endif /*M4OSA_OPTIONID_FILE_READ_GET_FILE_ATTRIBUTE*/

#if(M4OSA_OPTIONID_FILE_READ_GET_URL == M4OSA_TRUE)
    case M4OSA_kFileReadGetURL:
        {
            return M4OSA_fileCommonGetURL(pContext, (M4OSA_Char**)pOptionValue);
        }
#endif /*M4OSA_OPTIONID_FILE_READ_GET_URL*/

        case M4OSA_kFileReadLockMode:
        {
            *(M4OSA_UInt32*)pOptionValue = pFileContext->m_uiLockMode;
            return M4NO_ERROR;
        }
    }

    M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_fileReadGetOption");

    return M4ERR_NOT_IMPLEMENTED;
}

/**
 ************************************************************************
 * @fn         M4OSA_ERR M4OSA_fileReadSetOption (M4OSA_Context context,
 *                       M4OSA_OptionID optionID, M4OSA_DataOption optionValue))
 * @brief      This function asks the core file reader to set the value associated with the optionID.
 *             The caller is responsible for allocating/de-allocating the memory of the value field.
 * @note       As the caller is responsible for allocating/de-allocating the 'value' field, the callee must copy this field
 *             to its internal variable.
 * @param      pContext: (IN/OUT) Context of the core file reader
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
M4OSA_ERR M4OSA_fileReadSetOption(M4OSA_Context pContext,
                                  M4OSA_FileReadOptionID optionID,
                                  M4OSA_DataOption optionValue)
{
    M4OSA_FileContext* pFileContext = pContext;

    M4OSA_TRACE2_1("M4OSA_fileReadSetOption : option = 0x%x", optionID);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                             "M4OSA_fileReadSetOption: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(0 == optionID, M4ERR_PARAMETER,
                                                     "M4OSA_fileReadSetOption");
    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_FILE_READER),
                                M4ERR_BAD_OPTION_ID, "M4OSA_fileReadSetOption");

    M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_WRITABLE(optionID),
                                    M4ERR_READ_ONLY, "M4OSA_fileReadSetOption");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context,
                                  M4ERR_BAD_CONTEXT,
                                  "M4OSA_fileReadSetOption: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    switch(optionID)
    {
        case M4OSA_kFileReadLockMode:
        {
            pFileContext->m_uiLockMode= (M4OSA_UInt32)*(M4OSA_UInt32*)optionValue;
            return M4NO_ERROR;
        }
        default:
            M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_fileReadSetOption");
            return M4ERR_NOT_IMPLEMENTED;
    }
}

