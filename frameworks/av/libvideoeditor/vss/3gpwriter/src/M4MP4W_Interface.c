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
 * @file    M4MP4W_Interface.c
 * @brief    3GPP file writer interface
 * @note    This implementation follows the common interface defined
 *          in "M4WRITER_common.h".
 ******************************************************************************
*/

#include "NXPSW_CompilerSwitches.h"

/**
 * OSAL includes */
#include "M4OSA_Types.h"            /**< OSAL basic types definiton */
#include "M4OSA_FileWriter.h"        /**< Include for OSAL file accesses implementation */
#include "M4OSA_Memory.h"            /**< Include for OSAL memory accesses implementation */
#include "M4OSA_Debug.h"            /**< OSAL debug tools */

/**
 * Writer includes */
#include "M4WRITER_common.h"        /**< Definition of the writer common interface that
                                          this module follows */

#ifdef _M4MP4W_USE_CST_MEMORY_WRITER
#include "M4MP4W_Types_CstMem.h"    /**< MP4/3GP core writer types */
#include "M4MP4W_Writer_CstMem.h"    /**< MP4/3GP core writer functions */
#else
#include "M4MP4W_Types.h"            /**< MP4/3GP core writer types */
#include "M4MP4W_Writer.h"            /**< MP4/3GP core writer functions */
#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */

/**
 * Specific errors for this module */
#define M4WRITER_3GP_ERR_UNSUPPORTED_STREAM_TYPE \
                M4OSA_ERR_CREATE(M4_ERR, M4WRITER_3GP, 0x000001)


/**
 ******************************************************************************
 * structure    M4WRITER_3GP_InternalContext
 * @brief        This structure defines the writer context (private)
 * @note        This structure is used for all writer calls to store the context
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Context    pMP4Context;    /**< MP4 writer context */
    M4OSA_UInt32    maxAUsizes;        /**< the maximum AU size possible */
} M4WRITER_3GP_InternalContext;


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_openWrite(M4WRITER_Context* pContext, void* pWhat,
 *                                   M4OSA_FileWriterPointer* pFileWriterPointer)
 * @brief    Open a writer session.
 * @note
 * @param    pContext:     (OUT) Execution context of the 3GP writer, allocated by this function.
 * @param    outputFileDescriptor (IN)  Descriptor of the output file to create.
 * @param    fileWriterFunction     (IN)  Pointer to structure containing the set of OSAL
 *                                       file write functions.
 * @param    tempFileDescriptor     (IN)  Descriptor of the temporary file to open
 *                                        (NULL if not used)
 * @param    fileReaderFunction     (IN)  Pointer to structure containing the set of OSAL file read
 *                                      functions (NULL if not used)
 * @return    M4NO_ERROR:  there is no error
 * @return    M4ERR_ALLOC: there is no more available memory
 * @return    M4ERR_PARAMETER: pContext or pFilePtrFct is M4OSA_NULL (debug only)
 * @return    any error returned by the MP4 core writer openWrite (Its coreID is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_openWrite( M4WRITER_Context* pContext,
                                  void* outputFileDescriptor,
                                  M4OSA_FileWriterPointer* pFileWriterPointer,
                                  void* tempFileDescriptor,
                                  M4OSA_FileReadPointer* pFileReaderPointer )
{
    M4WRITER_3GP_InternalContext* apContext;
    M4OSA_ERR err;

    M4OSA_TRACE1_0("M4WRITER_3GP_openWrite");

    /**
     *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_openWrite: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileWriterPointer),M4ERR_PARAMETER,
         "M4WRITER_3GP_openWrite: pFileWriterPointer is M4OSA_NULL");

    /**
     *    Allocate memory for the context */
    *pContext=M4OSA_NULL;
    apContext = (M4WRITER_3GP_InternalContext*)M4OSA_32bitAlignedMalloc(
                    sizeof(M4WRITER_3GP_InternalContext),
                    M4WRITER_3GP,
                    (M4OSA_Char *)"M4WRITER_3GP_InternalContext");

    if (M4OSA_NULL == apContext)
    {
        M4OSA_TRACE1_0("M4WRITER_3GP_openWrite:\
             unable to allocate context, returning M4ERR_ALLOC");
        return (M4OSA_ERR)M4ERR_ALLOC;
    }

    /**
     *    Reset context variables */
    apContext->pMP4Context = M4OSA_NULL;
    apContext->maxAUsizes = 0;

    /**
     *    Return the writer context */
    *pContext = (M4WRITER_Context *)apContext;

    /**
     *    Launch the openWrite of the MP4 writer */
    M4OSA_TRACE3_0("M4WRITER_3GP_openWrite: calling M4MP4W_openWrite()");

    err = M4MP4W_openWrite(&apContext->pMP4Context, outputFileDescriptor,
            pFileWriterPointer, tempFileDescriptor, pFileReaderPointer );

    if (M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_TRACE1_1("M4WRITER_3GP_openWrite: "
                       "M4MP4W_openWrite returns error 0x%x", err);
    }

    M4OSA_TRACE2_1("M4WRITER_3GP_openWrite: returning 0x%x", err);

    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_startWriting(M4WRITER_Context pContext)
 * @brief    Indicates to the writer that the setup session is ended and that
 *          we will start to write.
 * @note
 * @param     pContext:   (IN) Execution context of the 3GP writer,
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only)
 * @return    any error returned by the MP4 core writer startWriting (Its
 *            coreID is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_startWriting(M4WRITER_Context pContext)
{
    M4WRITER_3GP_InternalContext* apContext =
                (M4WRITER_3GP_InternalContext*)pContext;

    M4OSA_ERR err;

    M4OSA_TRACE1_1("M4WRITER_3GP_startWriting: pContext=0x%x", pContext);

    /**
     *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_startWriting: pContext is M4OSA_NULL");

    /**
     *    Call the MP4 core writer */
    M4OSA_TRACE3_0("M4WRITER_3GP_startWriting: calling M4MP4W_startWriting()");
    err = M4MP4W_startWriting(apContext->pMP4Context);
    if (M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_TRACE1_1("M4MP4W_startWriting returns error 0x%x", err);
    }

    M4OSA_TRACE2_1("M4WRITER_3GP_startWriting: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_addStream(
 *     M4WRITER_Context pContext,
 *     M4SYS_StreamDescription *pStreamDescription)
 * @brief     Add a stream (audio or video).
 * @note      Decoder specific info properties are correctly set before calling
 *            the core writer add function
 * @param     pContext:   (IN) Execution context of the 3GP writer,
 * @param     streamDescription:    (IN) stream description.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext or pStreamDescription is M4OSA_NULL
 *            (debug only)
 * @return    any error returned by the MP4 core writer addStream
 *            (Its coreID is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_addStream(M4WRITER_Context pContext,
                                 M4SYS_StreamDescription* pStreamDescription)
{
    M4WRITER_3GP_InternalContext *apContext =
        (M4WRITER_3GP_InternalContext *)pContext;

    M4OSA_ERR err;
    M4WRITER_StreamVideoInfos *pVideoInfo = M4OSA_NULL;
    M4WRITER_StreamAudioInfos *pAudioInfo = M4OSA_NULL;
    M4MP4W_StreamIDsize sizeValue;

    M4OSA_TRACE1_2("M4WRITER_3GP_addStream: pContext=0x%x, "
                   "pStreamDescription=0x%x",
                   pContext, pStreamDescription);

    /**
     *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_addStream: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pStreamDescription),M4ERR_PARAMETER,
         "M4WRITER_3GP_addStream: pStreamDescription is M4OSA_NULL");

    /**
     *    Adapt audio/video stream infos */
    switch (pStreamDescription->streamType)
    {
        case M4SYS_kMPEG_4:
        case M4SYS_kH264:
        case M4SYS_kH263:
            M4OSA_TRACE3_1("M4WRITER_3GP_addStream: "
                    "adding a Video stream (streamType=0x%x)",
                    pStreamDescription->streamType);
            /**
             *    Common descriptions */
            pStreamDescription->streamID = VideoStreamID;    /**< The only values checked by our
                                                                  core writer are streamID */
            pStreamDescription->timeScale = 1000;            /**< and timeScale */

/* Not recommended for video editing -> write explicitely the 'bitr' box into 'd263' */
/* Rem : it is REL 5 of 3gpp documentation */
//            /**
//             * Average bit-rate must not be set in H263 to be compatible with Platform4 */
//            if (M4SYS_kH263 == pStreamDescription->streamType)
//            {
//                pStreamDescription->averageBitrate = -1;
//            }

            /**
             *    Decoder specific info */
            pVideoInfo = (M4WRITER_StreamVideoInfos *)pStreamDescription->decoderSpecificInfo;
            pStreamDescription->decoderSpecificInfoSize = pVideoInfo->Header.Size;
            pStreamDescription->decoderSpecificInfo = (M4OSA_MemAddr32)pVideoInfo->Header.pBuf;
            M4OSA_TRACE3_2("M4WRITER_3GP_addStream: Video: DSI=0x%x, DSIsize=%d",
                 pVideoInfo->Header.pBuf, pVideoInfo->Header.Size);
            break;

        case M4SYS_kAMR:
        case M4SYS_kAMR_WB:
        case M4SYS_kAAC:
        case M4SYS_kEVRC:
            M4OSA_TRACE3_1("M4WRITER_3GP_addStream: adding an Audio stream (streamType=0x%x)",
                 pStreamDescription->streamType);
            /**
             *    Common descriptions */
            pStreamDescription->streamID = AudioStreamID;    /**< The only value checked by our
                                                                 core writer is streamID */

            /**
             *    Decoder specific info */
            pAudioInfo = (M4WRITER_StreamAudioInfos *)pStreamDescription->decoderSpecificInfo;
            pStreamDescription->decoderSpecificInfoSize = pAudioInfo->Header.Size;
            pStreamDescription->decoderSpecificInfo = (M4OSA_MemAddr32)pAudioInfo->Header.pBuf;
            M4OSA_TRACE3_2("M4WRITER_3GP_addStream: Audio: DSI=0x%x, DSIsize=%d",
                 pAudioInfo->Header.pBuf, pAudioInfo->Header.Size);
            break;

        default:
            M4OSA_TRACE1_1("M4WRITER_3GP_addStream:\
                 returning M4WRITER_3GP_ERR_UNSUPPORTED_STREAM_TYPE (streamType=0x%x)",
                     pStreamDescription->streamType);
            return (M4OSA_ERR)M4WRITER_3GP_ERR_UNSUPPORTED_STREAM_TYPE;
            break;
    }

    /**
     *    Call the MP4 core writer */
    M4OSA_TRACE3_0("M4WRITER_3GP_addStream: calling M4MP4W_addStream()");
    err = M4MP4W_addStream(apContext->pMP4Context,pStreamDescription);
    if (M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_TRACE1_1("M4WRITER_3GP_addStream: M4MP4W_addStream returns error 0x%x", err);
        M4OSA_TRACE1_1("M4WRITER_3GP_addStream: returning 0x%x", err);
        return (err);
    }

    /**
     *    For Video, set the M4MP4W_trackSize Option */
    switch (pStreamDescription->streamType)
    {
        case M4SYS_kMPEG_4:
        case M4SYS_kH264:
        case M4SYS_kH263:
            sizeValue.streamID = VideoStreamID;
            sizeValue.height = (M4OSA_UInt16)(pVideoInfo->height);
            sizeValue.width  = (M4OSA_UInt16)(pVideoInfo->width);
            M4OSA_TRACE3_2("M4WRITER_3GP_addStream: Video: height=%d, width=%d",
                 sizeValue.height, sizeValue.width);

            M4OSA_TRACE3_0("M4WRITER_3GP_addStream: calling M4MP4W_setOption(M4MP4W_trackSize)");
            err = M4MP4W_setOption( apContext->pMP4Context, M4MP4W_trackSize,
                 (M4OSA_DataOption)&sizeValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4WRITER_3GP_addStream: M4MP4W_setOption returns error 0x%x",
                     err);
            }
            break;
        default:
            break;
    }

    M4OSA_TRACE2_1("M4WRITER_3GP_addStream: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_closeWrite(M4WRITER_Context pContext)
 * @brief    Close the writer. The context is freed here.
 * @note
 * @param     pContext:   (IN) Execution context of the 3GP writer,
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only)
 * @return    any error returned by the MP4 core writer closeWrite (Its coreID
 *            is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_closeWrite(M4WRITER_Context pContext)
{
    M4WRITER_3GP_InternalContext* apContext=(M4WRITER_3GP_InternalContext*)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    M4OSA_TRACE1_1("M4WRITER_3GP_closeWrite called with pContext=0x%x", pContext);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_closeWrite: pContext is M4OSA_NULL");

    /**
     *    Call the MP4 core writer */
    if (M4OSA_NULL != apContext->pMP4Context)
    {
        M4OSA_TRACE3_0("M4WRITER_3GP_closeWrite: calling M4MP4W_closeWrite()");
        err = M4MP4W_closeWrite(apContext->pMP4Context);
        if (M4OSA_ERR_IS_ERROR(err))
        {
            M4OSA_TRACE1_1("M4WRITER_3GP_closeWrite: M4MP4W_closeWrite returns error 0x%x", err);
        }
    }

    /**
     *    Deallocate our own context */
    free(apContext);

    M4OSA_TRACE2_1("M4WRITER_3GP_closeWrite: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_setOption(
 *        M4WRITER_Context pContext, M4OSA_UInt32 optionID,
 *        M4OSA_DataOption optionValue)
 * @brief     This function asks the writer to set the value associated with
 *            the optionID. The caller is responsible for allocating/
 *            de-allocating the memory of the value field.
 * @note      The options handled by the component depend on the implementation
 *            of the component.
 * @param     pContext:     (IN) Execution context of the 3GP writer,
 * @param     pptionId:     (IN) ID of the option to set.
 * @param     OptionValue : (IN) Value of the option to set.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only)
 * @return    M4ERR_BAD_OPTION_ID: the ID of the option is not valid.
 * @return    any error returned by the MP4 core writer setOption (Its coreID
 *            is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_setOption(
        M4WRITER_Context pContext, M4OSA_UInt32 optionID,
        M4OSA_DataOption optionValue)
{
    M4WRITER_3GP_InternalContext* apContext =
            (M4WRITER_3GP_InternalContext*)pContext;

    M4OSA_ERR err = M4NO_ERROR;
    M4MP4W_memAddr memval;
    M4SYS_StreamIDValue optval;

    M4OSA_TRACE2_3("M4WRITER_3GP_setOption: pContext=0x%x, optionID=0x%x,\
         optionValue=0x%x", pContext, optionID, optionValue);

    /**
     *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL==apContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_setOption: pContext is M4OSA_NULL");

    switch (optionID)
    {
        /**
         *    Maximum Access Unit size */
        case M4WRITER_kMaxAUSize:
            M4OSA_TRACE2_0("setting M4WRITER_kMaxAUSize option");
            err = M4MP4W_setOption(
                    apContext->pMP4Context,M4MP4W_maxAUsize, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_maxAUsize) "
                               "returns error 0x%x", err);
            }
            break;
        /**
         *    Maximum chunck size */
        case M4WRITER_kMaxChunckSize:
            M4OSA_TRACE2_0("setting M4WRITER_kMaxChunckSize option");
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_maxChunkSize, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_maxChunkSize)\
                     returns error 0x%x", err);
            }
            break;
        /**
         *    File string signature */
        case M4WRITER_kEmbeddedString:
            M4OSA_TRACE2_0("setting M4WRITER_kEmbeddedString option");
            /* The given M4OSA_DataOption must actually
               be a text string */
            memval.addr = (M4OSA_MemAddr32)optionValue;
            /**< this is max string size copied by the core */
            memval.size = 16;
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_embeddedString, &memval);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_embeddedString)\
                     returns error 0x%x", err);
            }
            break;
        /**
         *    File integration tag */
        case M4WRITER_kIntegrationTag:
            M4OSA_TRACE2_0("setting M4WRITER_kIntegrationTag option");
            /* The given M4OSA_DataOption must actually
               be a text string */
            memval.addr = (M4OSA_MemAddr32)optionValue;
            /**< this is max string size copied by the core */
            memval.size = strlen((const char *)optionValue);
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_integrationTag, &memval);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_integrationTag)"
                               " returns error 0x%x", err);
            }
            break;
        /**
         *    File version signature */
        case M4WRITER_kEmbeddedVersion:
            M4OSA_TRACE2_0("setting M4WRITER_kEmbeddedVersion option");
            /* The given M4OSA_DataOption must actually
               be a version number */

            /**< Here 0 means both streams */
            optval.streamID = 0;
            /**< version number */
            optval.value = *(M4OSA_UInt32*)optionValue;
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_CamcoderVersion, &optval);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_CamcoderVersion)"
                               " returns error 0x%x", err);
            }
            break;
        /**
         *    Some options are read-only */
        case M4WRITER_kFileSize:
        case M4WRITER_kFileSizeAudioEstimated:
            M4OSA_TRACE2_1("trying to set a read-only option! (ID=0x%x)",
                    optionID);
            return (M4OSA_ERR)M4ERR_READ_ONLY;
            break;
        /**
         *    Maximum filesize limitation */
        case M4WRITER_kMaxFileSize:
            M4OSA_TRACE2_0("setting M4WRITER_kMaxFileSize option");
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_maxFileSize, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_maxFileSize)\
                     returns error 0x%x", err);
            }
            break;

        /**
         *    Maximum file duration limitation */
        case M4WRITER_kMaxFileDuration:
            M4OSA_TRACE2_0("setting M4WRITER_kMaxFileDuration option");
            err = M4MP4W_setOption(
                apContext->pMP4Context,M4MP4W_maxFileDuration, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4WRITER_kMaxFileDuration)"
                               " returns error 0x%x", err);
            }
            break;

        /**
         *    Set 'ftyp' atom */
        case M4WRITER_kSetFtypBox:
            M4OSA_TRACE2_0("setting M4WRITER_kSetFtypBox option");
            err = M4MP4W_setOption(
                apContext->pMP4Context, M4MP4W_setFtypBox, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_setFtypBox)\
                     returns error 0x%x", err);
            }
            break;

        /**
         *    Decoder Specific Info */
        case M4WRITER_kDSI:
            M4OSA_TRACE2_0("setting M4WRITER_kDSI option");
            err = M4MP4W_setOption(
                apContext->pMP4Context, M4MP4W_DSI, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_DSI)\
                     returns error 0x%x", err);
            }
            break;
        /*+ H.264 Trimming  */
        case M4WRITER_kMUL_PPS_SPS:
            M4OSA_TRACE2_0("setting M4WRITER_kMUL_PPS_SPS option");
            err = M4MP4W_setOption(
                apContext->pMP4Context, M4MP4W_MUL_PPS_SPS, optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_setOption(M4MP4W_DSI)\
                     returns error 0x%x", err);
            }
            break;
        /*- H.264 Trimming  */

        /**
         *    Unknown option */
        default:
            M4OSA_TRACE2_1("trying to set an unknown option!\
                 (optionID=0x%x)", optionID);
            return (M4OSA_ERR)M4ERR_BAD_OPTION_ID;
            break;
    }

    M4OSA_TRACE3_1("M4WRITER_3GP_setOption: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_getOption(
 *     M4WRITER_Context pContext, M4OSA_UInt32 optionID,
 *     M4OSA_DataOption optionValue)
 * @brief     This function asks the writer to return the value associated with
 *            the optionID. The caller is responsible for allocating/
 *            de-allocating the memory of the value field.
 * @note      The options handled by the component depend on the implementation
 *            of the component.
 * @param     pContext:     (IN) Execution context of the 3GP writer,
 * @param     OptionId:      (IN) Id of the option to get.
 * @param     pOptionValue: (OUT) Value of the option to get.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only)
 * @return    M4ERR_BAD_OPTION_ID: the ID of the option is not valid.
 * @return    M4ERR_NOT_IMPLEMENTED: This option is not implemented yet.
 * @return    any error returned by the MP4 core writer getOption (Its coreID
 *            is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_getOption(
        M4WRITER_Context pContext, M4OSA_UInt32 optionID,
        M4OSA_DataOption optionValue)
{
    M4WRITER_3GP_InternalContext* apContext =
            (M4WRITER_3GP_InternalContext*)pContext;

    M4OSA_ERR err;

    M4OSA_TRACE2_3("M4WRITER_3GP_getOption: pContext=0x%x, optionID=0x%x,\
         optionValue=0x%x", pContext, optionID, optionValue);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext),M4ERR_PARAMETER,
         "M4WRITER_3GP_getOption: pContext is M4OSA_NULL");

    switch (optionID)
    {
        /**
         *    Maximum Access Unit size */
        case M4WRITER_kMaxAUSize:
            M4OSA_TRACE2_0("getting M4WRITER_kMaxAUSize option");
            err = M4MP4W_getOption(apContext->pMP4Context,M4MP4W_maxAUsize,
                (M4OSA_DataOption*)&optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_getOption(M4MP4W_maxAUsize)"
                               " returns error 0x%x", err);
            }
            break;
        /**
         *    Maximum chunck size */
        case M4WRITER_kMaxChunckSize:
            M4OSA_TRACE2_0("getting M4WRITER_kMaxChunckSize option");
            err = M4MP4W_getOption(apContext->pMP4Context,M4MP4W_maxChunkSize,
                (M4OSA_DataOption*)&optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_getOption(M4MP4W_maxChunkSize)\
                     returns error 0x%x", err);
            }
            break;
        /**
         *    The file size option */
        case M4WRITER_kFileSize:
            M4OSA_TRACE2_0("getting M4WRITER_kFileSize option");
            /* get the current file size */
            err = M4MP4W_getCurrentFileSize(
                apContext->pMP4Context, (M4OSA_UInt32*)optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_getCurrentFileSize"
                               " returns error 0x%x", err);
            }
            break;
        /**
         *    The file size with audio option has its own function call
              in the MP4 core writer */
        case M4WRITER_kFileSizeAudioEstimated:
            M4OSA_TRACE2_0("getting M4WRITER_kFileSizeAudioEstimated option");
            /* get the current file size ... */
            err = M4MP4W_getCurrentFileSize(
                apContext->pMP4Context, (M4OSA_UInt32*)optionValue);
            if (M4OSA_ERR_IS_ERROR(err))
            {
                M4OSA_TRACE1_1("M4MP4W_getCurrentFileSize"
                               " returns error 0x%x", err);
            }
            //no more needed 3gp writer has its own mecanism
            ///* ... add the estimated next max AU size */
            //*((M4OSA_UInt32*)optionValue) += apContext->maxAUsizes;
            break;
        /**
         *    Unknown option */
        default:
            M4OSA_TRACE2_1("trying to get an unknown option!\
                 (optionID=0x%x)", optionID);
            return    (M4OSA_ERR)M4ERR_BAD_OPTION_ID;
            break;
    }

    M4OSA_TRACE3_1("M4WRITER_3GP_getOption: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_startAU(
 *          M4WRITER_Context pContext, M4SYS_StreamID streamID,
 *          M4SYS_AccessUnit* pAU)
 * @brief     Prepare an Access Unit to be ready to store data
 * @note
 * @param     pContext: (IN) Execution context of the 3GP writer,
 * @param     streamID: (IN) Id of the stream to which the Access Unit
 *            is related.
 * @param     pAU:      (IN/OUT) Access Unit to be prepared.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext or pAU is M4OSA_NULL (debug only)
 * @return    M4ERR_BAD_STREAM_ID: streamID is not VideoStreamID nor
 *            AudioStreamID (debug only)
 * @return    any error returned by the MP4 core writer startAU (Its coreID
 *            is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_startAU(
        M4WRITER_Context pContext, M4SYS_StreamID streamID,
        M4SYS_AccessUnit* pAU)
{
    M4WRITER_3GP_InternalContext* apContext =
            (M4WRITER_3GP_InternalContext*)pContext;

    M4OSA_ERR err;

    M4OSA_TRACE2_3("M4WRITER_3GP_startAU: pContext=0x%x, streamID=%d, pAU=0x%x",
         pContext, streamID, pAU);

    /**
     *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext), M4ERR_PARAMETER,
         "M4WRITER_3GP_startAU: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pAU), M4ERR_PARAMETER,
         "M4WRITER_3GP_startAU: pAU is M4OSA_NULL");
    M4OSA_DEBUG_IF2(
         ((VideoStreamID != streamID) && (AudioStreamID != streamID)),
         M4ERR_BAD_STREAM_ID,
         "M4WRITER_3GP_processAU: Wrong streamID");

    /**
     * Call the MP4 writer */
    M4OSA_TRACE3_0("M4WRITER_3GP_startAU: calling M4MP4W_startAU()");
    err = M4MP4W_startAU(apContext->pMP4Context, streamID, pAU);
    if (M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_TRACE1_1("M4MP4W_startAU returns error 0x%x", err);
    }

    M4OSA_TRACE3_2("AU: dataAddress=0x%x, size=%d",
         pAU->dataAddress, pAU->size);

    /* Convert oversize to a request toward VES automaton */
    if (M4WAR_MP4W_OVERSIZE == err)
    {
        err = M4WAR_WRITER_STOP_REQ;
    }

    M4OSA_TRACE3_1("M4WRITER_3GP_startAU: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_processAU(
 *          M4WRITER_Context pContext, M4SYS_StreamID streamID,
 *          M4SYS_AccessUnit* pAU)
 * @brief     Write an Access Unit
 * @note
 * @param     pContext: (IN) Execution context of the 3GP writer,
 * @param     streamID: (IN) Id of the stream to which the Access Unit
 *            is related.
 * @param     pAU:      (IN/OUT) Access Unit to be written
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext or pAU is M4OSA_NULL (debug only)
 * @return    M4ERR_BAD_STREAM_ID: streamID is not VideoStreamID nor
 *            AudioStreamID (debug only)
 * @return    any error returned by the MP4 core writer processAU
 *            (Its coreID is M4MP4_WRITER)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_processAU(
        M4WRITER_Context pContext, M4SYS_StreamID streamID,
        M4SYS_AccessUnit* pAU)
{
    M4WRITER_3GP_InternalContext* apContext =
        (M4WRITER_3GP_InternalContext*)pContext;

    M4OSA_ERR err;

    M4OSA_TRACE2_3("M4WRITER_3GP_processAU: "
                   "pContext=0x%x, streamID=%d, pAU=0x%x",
                    pContext, streamID, pAU);

    /**
     *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == apContext), M4ERR_PARAMETER,
         "M4WRITER_3GP_processAU: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pAU), M4ERR_PARAMETER,
         "M4WRITER_3GP_processAU: pAU is M4OSA_NULL");
    M4OSA_DEBUG_IF2(
         ((VideoStreamID != streamID) && (AudioStreamID != streamID)),
         M4ERR_BAD_STREAM_ID,
         "M4WRITER_3GP_processAU: Wrong streamID");

    M4OSA_TRACE3_4("M4WRITER_3GP_processAU: AU: "
         "dataAddress=0x%x, size=%d, CTS=%d, nbFrag=%d",
         pAU->dataAddress, pAU->size, (M4OSA_UInt32)pAU->CTS, pAU->nbFrag);

    if(pAU->size > apContext->maxAUsizes)
    {
        apContext->maxAUsizes = pAU->size;
    }
    /**
     * Call the MP4 writer */
    M4OSA_TRACE3_0("M4WRITER_3GP_processAU: calling M4MP4W_processAU()");
    err = M4MP4W_processAU(apContext->pMP4Context, streamID, pAU);
    if (M4OSA_ERR_IS_ERROR(err))
    {
        M4OSA_TRACE1_1("M4MP4W_processAU returns error 0x%x", err);
    }

    /* Convert oversize to a request toward VES automaton */
    if(M4WAR_MP4W_OVERSIZE == err)
    {
        err = M4WAR_WRITER_STOP_REQ;
    }

    M4OSA_TRACE3_1("M4WRITER_3GP_processAU: returning 0x%x", err);
    return err;
}


/******************************************************************************
 * M4OSA_ERR M4WRITER_3GP_getInterfaces(
 *      M4WRITER_OutputFileType* Type,
 *      M4WRITER_GlobalInterface** SrcGlobalInterface,
 *      M4WRITER_DataInterface** SrcDataInterface)
 * @brief     Get the 3GPP writer common interface
 * @note      Retrieves the set of functions needed to use the 3GPP writer.
 *            It follows the common writer interface.
 * @param     Type: (OUT) return the type of this writer. Will always be
 *            M4WRITER_k3GPP.
 * @param     SrcGlobalInterface: (OUT) Main set of function to use this
 *            3GPP writer
 * @param     SrcDataInterface:   (OUT) Set of function related to datas
 *            to use this 3GPP writer
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_ALLOC: there is no more available memory
 * @return    M4ERR_PARAMETER: At least one of the parameters is M4OSA_NULL
 *            (debug only)
 ******************************************************************************
*/
M4OSA_ERR M4WRITER_3GP_getInterfaces(
        M4WRITER_OutputFileType* Type,
        M4WRITER_GlobalInterface** SrcGlobalInterface,
        M4WRITER_DataInterface** SrcDataInterface)
{
    M4WRITER_GlobalInterface *pGlobal;
    M4WRITER_DataInterface *pData;

    M4OSA_TRACE2_3("M4WRITER_3GP_getInterfaces: "
         "Type=0x%x, SrcGlobalInterface=0x%x,\
         SrcDataInterface=0x%x", Type, SrcGlobalInterface, SrcDataInterface);

    /**
     *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == Type), M4ERR_PARAMETER,
         "M4WRITER_3GP_getInterfaces: Type is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == SrcGlobalInterface), M4ERR_PARAMETER,
         "M4WRITER_3GP_getInterfaces: SrcGlobalInterface is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == SrcDataInterface), M4ERR_PARAMETER,
         "M4WRITER_3GP_getInterfaces: SrcDataInterface is M4OSA_NULL");

    /**
     *    Set the output type */
    *Type = M4WRITER_k3GPP;

    /**
     *    Allocate the global interface structure */
    pGlobal = (M4WRITER_GlobalInterface*)M4OSA_32bitAlignedMalloc(
                sizeof(M4WRITER_GlobalInterface),
                M4WRITER_3GP, (M4OSA_Char *)"M4WRITER_GlobalInterface");
    if (M4OSA_NULL == pGlobal)
    {
        M4OSA_TRACE1_0("unable to allocate M4WRITER_GlobalInterface,\
             returning M4ERR_ALLOC");
        *SrcGlobalInterface = M4OSA_NULL;
        *SrcDataInterface = M4OSA_NULL;
        return (M4OSA_ERR)M4ERR_ALLOC;
    }

    /**
     *    Allocate the data interface structure */
    pData =
        (M4WRITER_DataInterface *)M4OSA_32bitAlignedMalloc(sizeof(M4WRITER_DataInterface),
        M4WRITER_3GP, (M4OSA_Char *)"M4WRITER_DataInterface");
    if (M4OSA_NULL == pData)
    {
        M4OSA_TRACE1_0("unable to allocate M4WRITER_DataInterface,\
             returning M4ERR_ALLOC");
        free(pGlobal);
        *SrcGlobalInterface = M4OSA_NULL;
        *SrcDataInterface = M4OSA_NULL;
        return (M4OSA_ERR)M4ERR_ALLOC;
    }

    /**
     *    Fill the global interface structure */
    pGlobal->pFctOpen = M4WRITER_3GP_openWrite;
    pGlobal->pFctAddStream = M4WRITER_3GP_addStream;
    pGlobal->pFctStartWriting = M4WRITER_3GP_startWriting;
    pGlobal->pFctCloseWrite = M4WRITER_3GP_closeWrite;
    pGlobal->pFctSetOption = M4WRITER_3GP_setOption;
    pGlobal->pFctGetOption = M4WRITER_3GP_getOption;

    /**
     *    Fill the data interface structure */
    pData->pStartAU = M4WRITER_3GP_startAU;
    pData->pProcessAU = M4WRITER_3GP_processAU;

    /**
     *    Set the return values */
    *SrcGlobalInterface = pGlobal;
    *SrcDataInterface = pData;

    M4OSA_TRACE2_0("M4WRITER_3GP_getInterfaces: returning M4NO_ERROR");
    return M4NO_ERROR;
}

