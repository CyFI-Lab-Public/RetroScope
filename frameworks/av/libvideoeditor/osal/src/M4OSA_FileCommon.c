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
 * @file         M4OSA_FileCommon.c
 * @brief        File common for Android
 * @note         This file implements functions used by both the file writer
 *               and file reader.
 ************************************************************************
*/

#ifndef USE_STAGEFRIGHT_CODECS
#error "USE_STAGEFRIGHT_CODECS is not defined"
#endif /*USE_STAGEFRIGHT_CODECS*/

#ifdef UTF_CONVERSION
#include <string.h>
#endif /*UTF_CONVERSION*/

#include <sys/stat.h>
#include <errno.h>

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
#include "M4OSA_Semaphore.h"
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */


#include "M4OSA_Debug.h"
#include "M4OSA_FileCommon.h"
#include "M4OSA_FileCommon_priv.h"
#include "M4OSA_Memory.h"
#include "M4OSA_CharStar.h"

/**
 ************************************************************************
 * @brief      This function opens the provided URL and returns its context.
 *             If an error occured, the context is set to NULL.
 * @param      core_id: (IN) Core ID of the caller (M4OSA_FILE_READER or M4OSA_FILE_WRITER)
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
M4OSA_ERR M4OSA_fileCommonOpen(M4OSA_UInt16 core_id, M4OSA_Context* pContext,
                               M4OSA_Char* pUrl, M4OSA_FileModeAccess fileModeAccess)
{

    M4OSA_Int32 i            = 0;
    M4OSA_Int32 iMode        = 0;
    M4OSA_Int32 iSize        = 0;
    M4OSA_Int32 iSavePos    = 0;

    M4OSA_Char  mode[4]            = "";
    M4OSA_Char* pReadString        = (M4OSA_Char*)"r";
    M4OSA_Char* pWriteString    = (M4OSA_Char*)"w";
    M4OSA_Char* pAppendString    = (M4OSA_Char*)"a";
    M4OSA_Char* pBinaryString    = (M4OSA_Char*)"b";
    M4OSA_Char* pPlusString        = (M4OSA_Char*)"+";

    M4OSA_ERR err = M4NO_ERROR;

    FILE* pFileHandler = M4OSA_NULL;
    M4OSA_FileContext *pFileContext    = M4OSA_NULL;


#ifdef UTF_CONVERSION
    /*FB: to test the UTF16->UTF8 conversion into Video Artist*/
    /*Convert the URL from UTF16 to UTF8*/
    M4OSA_Void* tempConversionBuf;
    M4OSA_UInt32 tempConversionSize = 1000;

    tempConversionBuf = (M4OSA_Char*)M4OSA_32bitAlignedMalloc(tempConversionSize +1, 0, "conversion buf");
    if(tempConversionBuf == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Error when allocating conversion buffer\n");
        return M4ERR_PARAMETER;
    }
    M4OSA_ToUTF8_OSAL(pUrl, tempConversionBuf, &tempConversionSize);
    ((M4OSA_Char*)tempConversionBuf)[tempConversionSize ] = '\0';

    printf("file open %s\n", tempConversionBuf);
#endif /*UTF CONVERSION*/

    M4OSA_TRACE3_4("M4OSA_fileCommonOpen\t\tM4OSA_UInt16 %d\tM4OSA_Context* 0x%x\t"
        "M4OSA_Char* %s\tfileModeAccess %d", core_id, pContext, pUrl, fileModeAccess);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext,    M4ERR_PARAMETER,    "M4OSA_fileCommonOpen: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pUrl,        M4ERR_PARAMETER,    "M4OSA_fileCommonOpen: pUrl  is M4OSA_NULL");
    M4OSA_DEBUG_IF2(0 == fileModeAccess,    M4ERR_PARAMETER,    "M4OSA_fileCommonOpen: fileModeAccess is 0");

    /* Read mode not set for the reader */
    M4OSA_DEBUG_IF1((M4OSA_FILE_READER == core_id) && !(fileModeAccess & M4OSA_kFileRead),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileRead");

    /* Read mode not set for the reader */
    M4OSA_DEBUG_IF1((M4OSA_FILE_READER == core_id) && !(fileModeAccess & M4OSA_kFileRead),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileRead");

    /* M4OSAfileReadOpen cannot be used with Write file mode access */
    M4OSA_DEBUG_IF1((M4OSA_FILE_READER == core_id) && (fileModeAccess & M4OSA_kFileWrite),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileWrite");

    /* Append and Create flags cannot be used with Read */
    M4OSA_DEBUG_IF1((M4OSA_FILE_READER == core_id) && (fileModeAccess & M4OSA_kFileAppend),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileAppend");

    M4OSA_DEBUG_IF1((M4OSA_FILE_READER == core_id) && (fileModeAccess & M4OSA_kFileCreate),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileCreate");

    /* Write mode not set for the writer */
    M4OSA_DEBUG_IF1((M4OSA_FILE_WRITER == core_id) && !(fileModeAccess & M4OSA_kFileWrite),
        M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: M4OSA_kFileWrite");

    /* Create flag necessary for opening file */
    if ((fileModeAccess & M4OSA_kFileRead) &&
        (fileModeAccess & M4OSA_kFileWrite)&&(fileModeAccess & M4OSA_kFileCreate))
    {
        strncat((char *)mode, (const char *)pWriteString, (size_t)1);
        strncat((char *)mode, (const char *)pPlusString, (size_t)1);
    }
    else
    {
        if(fileModeAccess & M4OSA_kFileAppend)
        {
            strncat((char *)mode, (const char *)pAppendString, (size_t)1);
        }
        else if(fileModeAccess & M4OSA_kFileRead)
        {
            strncat((char *)mode, (const char *)pReadString, (size_t)1);
        }
        else if(fileModeAccess & M4OSA_kFileWrite)
        {
            strncat((char *)mode, (const char *)pWriteString, (size_t)1);
        }

        if((fileModeAccess & M4OSA_kFileRead)&&(fileModeAccess & M4OSA_kFileWrite))
        {
            strncat((char *)mode,(const char *)pPlusString, (size_t)1);
        }
    }

    if(!(fileModeAccess & M4OSA_kFileIsTextMode))
    {
        strncat((char *)mode, (const char *)pBinaryString,(size_t)1);
    }

    /*Open the file*/

#ifdef UTF_CONVERSION
    /*Open the converted path*/
    pFileHandler = fopen((const char *)tempConversionBuf, (const char *)mode);
    /*Free the temporary decoded buffer*/
    free(tempConversionBuf);
#else /* UTF_CONVERSION */
    pFileHandler = fopen((const char *)pUrl, (const char *)mode);
#endif /* UTF_CONVERSION */

    if (M4OSA_NULL == pFileHandler)
    {
        switch(errno)
        {
        case ENOENT:
            {
                M4OSA_DEBUG(M4ERR_FILE_NOT_FOUND, "M4OSA_fileCommonOpen: No such file or directory");
                M4OSA_TRACE1_1("File not found: %s", pUrl);
                return M4ERR_FILE_NOT_FOUND;
            }
        case EACCES:
            {
                M4OSA_DEBUG(M4ERR_FILE_LOCKED, "M4OSA_fileCommonOpen: Permission denied");
                return M4ERR_FILE_LOCKED;
            }
         case EINVAL:
         {
            M4OSA_DEBUG(M4ERR_FILE_BAD_MODE_ACCESS, "M4OSA_fileCommonOpen: Invalid Argument");
            return M4ERR_FILE_BAD_MODE_ACCESS;
         }
        case EMFILE:
         case ENOSPC:
        case ENOMEM:
            {
                M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileCommonOpen: Too many open files");
                return M4ERR_ALLOC;
            }
        default:
            {
                M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_fileCommonOpen");
                return M4ERR_NOT_IMPLEMENTED;
            }
        }
    }

    /* Allocate the file context */
    pFileContext = (M4OSA_FileContext*) M4OSA_32bitAlignedMalloc(sizeof(M4OSA_FileContext),
                    core_id, (M4OSA_Char*)"M4OSA_fileCommonOpen: file context");
    if (M4OSA_NULL == pFileContext)
    {
        fclose(pFileHandler);
        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileCommonOpen");
        return M4ERR_ALLOC;
    }

    pFileContext->file_desc        = pFileHandler;
    pFileContext->access_mode    = fileModeAccess;
    pFileContext->current_seek    = SeekNone;
    pFileContext->b_is_end_of_file    = M4OSA_FALSE;

    /**
     * Note: Never use this expression "i = (value1 == value2) ? x: y;"
     * because that doens't compile on other platforms (ADS for example)
     * Use: if(value1 == value2)
     *        { i= x; ..etc
     */
    pFileContext->coreID_write = 0;
    pFileContext->coreID_read = 0;
    pFileContext->m_DescrModeAccess = M4OSA_kDescNoneAccess;

    if (M4OSA_FILE_READER == core_id)
    {
        pFileContext->coreID_read = core_id;
        pFileContext->m_DescrModeAccess = M4OSA_kDescReadAccess;
    }
    else if (M4OSA_FILE_WRITER == core_id)
    {
        pFileContext->coreID_write = core_id;
        pFileContext->m_DescrModeAccess = M4OSA_kDescWriteAccess;
    }

    pFileContext->read_position = 0;
    pFileContext->write_position = 0;

    /* Allocate the memory to store the URL string */
    pFileContext->url_name = (M4OSA_Char*) M4OSA_32bitAlignedMalloc(strlen((const char *)pUrl)+1,
                        core_id, (M4OSA_Char*)"M4OSA_fileCommonOpen: URL name");
    if (M4OSA_NULL == pFileContext->url_name)
    {
        fclose(pFileHandler);
        free(pFileContext);
        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileCommonOpen");
        return M4ERR_ALLOC;
    }
    M4OSA_chrNCopy(pFileContext->url_name, pUrl, strlen((const char *)pUrl)+1);

    /* Get the file name */
    err = M4OSA_fileCommonGetFilename(pUrl, &pFileContext->file_name);
    if(M4NO_ERROR != err)
    {
        fclose(pFileHandler);
        free(pFileContext->url_name);
        free(pFileContext);
        M4OSA_DEBUG(err, "M4OSA_fileCommonOpen");
        return err;
    }

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreOpen(&(pFileContext->semaphore_context), 1); /* Allocate the semaphore */
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */



#ifdef USE_STAGEFRIGHT_CODECS
    // Workaround for file system bug on Stingray/Honeycomb where a file re-created will keep
    // the original file's size filled with 0s. Do not seek to the end to avoid ill effects
    if(fileModeAccess & M4OSA_kFileAppend) {
        /* Get the file size */
        iSavePos = ftell(pFileHandler);            /*    1- Check the first position */
        fseek(pFileHandler, 0, SEEK_END);        /*    2- Go to the end of the file*/
        iSize = ftell(pFileHandler);            /*    3- Check the file size        */
        fseek(pFileHandler, iSavePos, SEEK_SET);/*    4- go to the first position */
    } else {
        iSize = 0;
    }
#else /* USE_STAGEFRIGHT_CODECS */
    /* Get the file size */
    iSavePos = ftell(pFileHandler);            /*    1- Check the first position */
    fseek(pFileHandler, 0, SEEK_END);        /*    2- Go to the end of the file*/
    iSize = ftell(pFileHandler);            /*    3- Check the file size        */
    fseek(pFileHandler, iSavePos, SEEK_SET);/*    4- go to the first position */
#endif /* USE_STAGEFRIGHT_CODECS */



    /* Warning possible overflow if the file is higher than 2GBytes */
    pFileContext->file_size = iSize;

    *pContext = pFileContext;

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function convert from UTF16 to UTF8
 * @param      pBufferIn: (IN) UTF16 input path
 * @param      pBufferOut: (OUT) UTF8 output path
 * @param      bufferOutSize: (IN/OUT) size of the output path
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: the output path size is not enough to contain
 *               the decoded path
 ************************************************************************
*/
#ifdef UTF_CONVERSION
M4OSA_ERR M4OSA_ToUTF8_OSAL (M4OSA_Void   *pBufferIn, M4OSA_UInt8  *pBufferOut,
                                                    M4OSA_UInt32 *bufferOutSize)
{
    M4OSA_UInt16 i;
    wchar_t      *w_str = (wchar_t *) pBufferIn;
    M4OSA_UInt32 len, size_needed, size_given;
    if (pBufferIn == NULL)
    {
        *pBufferOut=NULL;
        *bufferOutSize=1;
    }
    else
    {
        len         = wcslen(w_str);
        size_needed = len+1;
        size_given  = *bufferOutSize;

       *bufferOutSize=size_needed;
        if (size_given < size_needed )
        {
            return M4ERR_PARAMETER;
        }
        else
        {
            for (i=0; i<len; i++)
            {
                pBufferOut[i]=(M4OSA_UInt8)w_str[i];
            }
            pBufferOut[len]=0;
        }
    }
    return M4NO_ERROR;
}
#endif /*UTF CONVERSION*/

/**
 ************************************************************************
 * @brief      This function seeks at the provided position.
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
M4OSA_ERR M4OSA_fileCommonSeek(M4OSA_Context pContext,
                               M4OSA_FileSeekAccessMode seekMode,
                               M4OSA_FilePosition* pFilePos)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_FilePosition fpos_current;
    M4OSA_FilePosition fpos_seek;
    M4OSA_FilePosition fpos_null = 0;
    M4OSA_FilePosition fpos_neg_un = -1;
    M4OSA_FilePosition fpos_file_size;
    M4OSA_FilePosition fpos_seek_from_beginning;

    M4OSA_TRACE3_3("M4OSA_fileCommonSeek\t\tM4OSA_Context 0x%x\t M4OSA_FileSeekAccessMode %d\tM4OSA_FilePosition* 0x%x",
        pContext, seekMode, pFilePos);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER, "M4OSA_fileCommonSeek");
    M4OSA_DEBUG_IF2(0 == seekMode, M4ERR_PARAMETER, "M4OSA_fileCommonSeek");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFilePos, M4ERR_PARAMETER, "M4OSA_fileCommonSeek");

    fpos_file_size = pFileContext->file_size;

    if(SeekRead == pFileContext->current_seek)
    {
        fpos_current = pFileContext->read_position;
    }
    else if(SeekWrite == pFileContext->current_seek)
    {
        fpos_current = pFileContext->write_position;
    }
    else
    {
        fpos_current = 0;
    }

    switch(seekMode)
    {
    case M4OSA_kFileSeekCurrent:
        {
            fpos_seek = *pFilePos;
            break;
        }
    case M4OSA_kFileSeekBeginning:
        {
            fpos_seek = *pFilePos - fpos_current;
            break;
        }
    case M4OSA_kFileSeekEnd:
        {
            fpos_seek = *pFilePos + fpos_file_size - fpos_current;
            break;
        }
    default:
        {
            return M4ERR_PARAMETER;
        }
    }

    fpos_seek_from_beginning = fpos_current + fpos_seek;

    if(fseek(pFileContext->file_desc, fpos_seek, SEEK_CUR) != 0)
    {
        switch(errno)
        {
        case EINVAL:
            {
            /* meaning the value for origin is invalid or the position
                specified by offset is before the beginning of the file */
                return M4ERR_FILE_INVALID_POSITION;
            }

        case EBADF:
        default:
            {
                return M4ERR_BAD_CONTEXT;/* file handle is invalid */
            }
        }
    }

    /* Set the returned position from the beginning of the file */
    *pFilePos = fpos_seek_from_beginning;

    /* SEEK done, reset end of file value */
    pFileContext->b_is_end_of_file = M4OSA_FALSE;

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function asks to close the file (associated to the context)
 * @note       The context of the core file reader/writer is freed.
 * @param      context: (IN/OUT) Context of the core file reader
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 ************************************************************************
*/

M4OSA_ERR M4OSA_fileCommonClose(M4OSA_UInt16 core_id, M4OSA_Context pContext)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_Int32 i32_err_code=0;

    M4OSA_TRACE3_2("M4OSA_fileCommonClose\tM4OSA_UInt16 %d\tM4OSA_Context 0x%x",
                                                             core_id, pContext);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext,
              M4ERR_PARAMETER, "M4OSA_fileCommonClose: pContext is M4OSA_NULL");
#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileContext->semaphore_context, M4ERR_BAD_CONTEXT,
                     "M4OSA_fileCommonClose: semaphore_context is M4OSA_NULL");
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    free(pFileContext->url_name);
    pFileContext->url_name = M4OSA_NULL;

    free(pFileContext->file_name);
    pFileContext->file_name = M4OSA_NULL;

    i32_err_code = fclose(pFileContext->file_desc);

    pFileContext->file_desc = M4OSA_NULL;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
    M4OSA_semaphoreClose(pFileContext->semaphore_context);/* free the semaphore */
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */

    free(pFileContext);

    if (i32_err_code != 0)
    {
        M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_fileCommonClose");
        return M4ERR_BAD_CONTEXT;
    }

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function gets the file attributes (associated to the
 *             context)
 * @param      context: (IN) Context of the core file reader
 * @param      attribute: (OUT) The file attribute (allocated by the caller)
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileCommonGetAttribute(M4OSA_Context pContext, M4OSA_FileAttribute* pAttribute)
{

    M4OSA_FileContext* fileContext = pContext;

    struct stat TheStat;

    M4OSA_TRACE3_2("M4OSA_fileCommonGetAttribute\tM4OSA_Context 0x%x\t"
        "M4OSA_FileAttribute* 0x%x", pContext, pAttribute);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext,        M4ERR_PARAMETER, "M4OSA_fileCommonGetAttribute");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pAttribute,    M4ERR_PARAMETER, "M4OSA_fileCommonGetAttribute");

    if(stat((char*)fileContext->url_name, &TheStat) != 0)
    {
        M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_fileCommonGetAttribute");
        return M4ERR_BAD_CONTEXT;
    }

    pAttribute->creationDate.time = (M4OSA_Time)TheStat.st_ctime;
    pAttribute->lastAccessDate.time = (M4OSA_Time)TheStat.st_atime;
    pAttribute->modifiedDate.time = (M4OSA_Time)TheStat.st_mtime;

    pAttribute->creationDate.timeScale = 1;
    pAttribute->lastAccessDate.timeScale = 1;
    pAttribute->modifiedDate.timeScale = 1;

    pAttribute->creationDate.referenceYear = 1970;
    pAttribute->lastAccessDate.referenceYear = 1970;
    pAttribute->modifiedDate.referenceYear = 1970;

    pAttribute->modeAccess = fileContext->access_mode;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief      This function gets the file URL (associated to the context).
 * @note
 * @param      context: (IN) Context of the core file reader
 * @param      url: (OUT) The buffer containing the URL (allocated by
 *             M4OSA_fileCommonGetURL)
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_ALLOC: there is no more memory available
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileCommonGetURL(M4OSA_Context pContext, M4OSA_Char** pUrl)
{
    M4OSA_FileContext* pFileContext = pContext;
    M4OSA_UInt32    uiLength;

    M4OSA_TRACE3_2("M4OSA_fileCommonGetURL\tM4OSA_Context 0x%x\tM4OSA_Char** 0x%x",
                    pContext, pUrl);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext,    M4ERR_PARAMETER,
                              "M4OSA_fileCommonGetURL: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pUrl,    M4ERR_PARAMETER,
                                  "M4OSA_fileCommonGetURL: pUrl is M4OSA_NULL");

    uiLength = strlen((const char *)pFileContext->url_name)+1;

    /* Allocate the memory to store the url_name */
    *pUrl = (M4OSA_Char*)M4OSA_32bitAlignedMalloc(uiLength, M4OSA_FILE_COMMON,
                                    (M4OSA_Char*)"M4OSA_fileCommonGetURL: url");
    if(M4OSA_NULL == *pUrl)
    {
        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileCommonGetURL");
        return M4ERR_ALLOC;
    }

    M4OSA_chrNCopy(*pUrl, pFileContext->url_name, uiLength);

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief      This function gets a string containing the file name associated
 *             to the input URL.
 * @note       The user should not forget to delete the output string using
 *             M4OSA_strDestroy
 * @param      pUrl:            (IN) The buffer containing the URL
 * @param      pFileName:    (OUT) The string containing the URL. It is
 *                            allocated inside this function
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_NOT_IMPLEMENTED: the URL does not match with the supported
 *             file
 * @return     M4ERR_ALLOC: there is no more memory available
 ************************************************************************
*/
M4OSA_ERR M4OSA_fileCommonGetFilename(M4OSA_Char* pUrl, M4OSA_Char** pFileName)
{
    M4OSA_Int32 i            = 0;
    M4OSA_Int32 iUrlLen        = 0;
    M4OSA_Int32 FileNameLen = 0;

    M4OSA_Char* ptrUrl        = M4OSA_NULL;
    M4OSA_Char* ptrFilename    = M4OSA_NULL;

    M4OSA_TRACE3_2("M4OSA_fileCommonGetURL\tM4OSA_Char* %s\tM4OSA_Char** 0x%x",
                                                               pUrl, pFileName);

    M4OSA_DEBUG_IF2(M4OSA_NULL == pUrl,    M4ERR_PARAMETER,
                             "M4OSA_fileCommonGetFilename: pUrl is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_NULL == pFileName,    M4ERR_PARAMETER,
                        "M4OSA_fileCommonGetFilename: pFileName is M4OSA_NULL");

    *pFileName = M4OSA_NULL;

    /*Parse URL*/
    iUrlLen = strlen((const char *)pUrl);
    for(i=iUrlLen-1; i>=0; i--)
    {
        if (pUrl[i] != '\\' && pUrl[i] != '/')
        {
            FileNameLen++;
        }
        else
        {
            break; /* find the beginning of the file name */
        }
    }

    ptrFilename = (M4OSA_Char*) M4OSA_32bitAlignedMalloc(FileNameLen+1, M4OSA_FILE_COMMON,
                    (M4OSA_Char*)"M4OSA_fileCommonGetFilename: Filename string");
    if (ptrFilename == M4OSA_NULL)
    {
        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_fileCommonGetFilename");
        return M4ERR_ALLOC;
    }

    ptrUrl = pUrl + (iUrlLen - FileNameLen);
    M4OSA_chrNCopy(ptrFilename, ptrUrl, FileNameLen+1);

    *pFileName = ptrFilename;

    return M4NO_ERROR;
}

