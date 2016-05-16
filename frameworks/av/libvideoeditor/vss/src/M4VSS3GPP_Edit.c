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
 * @file    M4VSS3GPP_Edit.c
 * @brief    Video Studio Service 3GPP edit API implementation.
 * @note
 ******************************************************************************
 */

/****************/
/*** Includes ***/
/****************/

#include "NXPSW_CompilerSwitches.h"
/**
 * Our headers */
#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_InternalTypes.h"
#include "M4VSS3GPP_InternalFunctions.h"
#include "M4VSS3GPP_InternalConfig.h"
#include "M4VSS3GPP_ErrorCodes.h"


/**
 * OSAL headers */
#include "M4OSA_Memory.h"   /**< OSAL memory management */
#include "M4OSA_Debug.h"    /**< OSAL debug management */
#include "M4OSA_CharStar.h" /**< OSAL string management */

#ifdef WIN32
#include "string.h"         /**< for strcpy (Don't want to get dependencies
                                 with M4OSA_String...) */

#endif                      /* WIN32 */
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
#include "M4VD_EXTERNAL_Interface.h"
#endif

/************************************************************************/
/* Static local functions                                               */
/************************************************************************/
static M4OSA_ERR M4VSS3GPP_intClipSettingsSanityCheck(
    M4VSS3GPP_ClipSettings *pClip );
static M4OSA_ERR M4VSS3GPP_intTransitionSettingsSanityCheck(
    M4VSS3GPP_TransitionSettings *pTransition );
static M4OSA_Void M4VSS3GPP_intFreeSettingsList(
    M4VSS3GPP_InternalEditContext *pC );
static M4OSA_ERR
M4VSS3GPP_intCreateMP3OutputFile( M4VSS3GPP_InternalEditContext *pC,
                                 M4OSA_Void *pOutputFile );
static M4OSA_ERR M4VSS3GPP_intSwitchToNextClip(
    M4VSS3GPP_InternalEditContext *pC );
static M4OSA_ERR
M4VSS3GPP_intComputeOutputVideoAndAudioDsi( M4VSS3GPP_InternalEditContext *pC,
                                           M4OSA_UInt8 uiMasterClip );
static M4OSA_Void M4VSS3GPP_intComputeOutputAverageVideoBitrate(
    M4VSS3GPP_InternalEditContext *pC );

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_GetVersion()
 * @brief    Get the VSS 3GPP version.
 * @note    Can be called anytime. Do not need any context.
 * @param    pVersionInfo        (OUT) Pointer to a version info structure
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pVersionInfo is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_GetVersion( M4_VersionInfo *pVersionInfo )
{
    M4OSA_TRACE3_1("M4VSS3GPP_GetVersion called with pVersionInfo=0x%x",
        pVersionInfo);

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pVersionInfo), M4ERR_PARAMETER,
        "M4VSS3GPP_GetVersion: pVersionInfo is M4OSA_NULL");

    pVersionInfo->m_major = M4VSS_VERSION_MAJOR;
    pVersionInfo->m_minor = M4VSS_VERSION_MINOR;
    pVersionInfo->m_revision = M4VSS_VERSION_REVISION;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editInit()
 * @brief    Initializes the VSS 3GPP edit operation (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the VSS 3GPP edit context to allocate
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editInit( M4VSS3GPP_EditContext *pContext,
                             M4OSA_FileReadPointer *pFileReadPtrFct,
                             M4OSA_FileWriterPointer *pFileWritePtrFct )
{
    M4VSS3GPP_InternalEditContext *pC;
    M4OSA_ERR err;
    M4OSA_UInt32 i;

    M4OSA_TRACE3_3(
        "M4VSS3GPP_editInit called with pContext=0x%x, \
        pFileReadPtrFct=0x%x, pFileWritePtrFct=0x%x",
        pContext, pFileReadPtrFct, pFileWritePtrFct);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_editInit: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileReadPtrFct), M4ERR_PARAMETER,
        "M4VSS3GPP_editInit: pFileReadPtrFct is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileWritePtrFct), M4ERR_PARAMETER,
        "M4VSS3GPP_editInit: pFileWritePtrFct is M4OSA_NULL");

    /**
    * Allocate the VSS context and return it to the user */
    pC = (M4VSS3GPP_InternalEditContext
        *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_InternalEditContext),
        M4VSS3GPP, (M4OSA_Char *)"M4VSS3GPP_InternalContext");
    *pContext = pC;
        /* Inialization of context Variables */
    memset((void *)pC, 0,sizeof(M4VSS3GPP_InternalEditContext));

    if( M4OSA_NULL == pC )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_editInit(): unable to allocate M4VSS3GPP_InternalContext,\
            returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }


    /* Init the context. */
    pC->uiClipNumber = 0;
    pC->pClipList = M4OSA_NULL;
    pC->pTransitionList = M4OSA_NULL;
    pC->pEffectsList = M4OSA_NULL;
    pC->pActiveEffectsList = M4OSA_NULL;
    pC->pActiveEffectsList1 = M4OSA_NULL;
    pC->bClip1ActiveFramingEffect = M4OSA_FALSE;
    pC->bClip2ActiveFramingEffect = M4OSA_FALSE;
    pC->uiCurrentClip = 0;
    pC->pC1 = M4OSA_NULL;
    pC->pC2 = M4OSA_NULL;
    pC->yuv1[0].pac_data = pC->yuv1[1].pac_data = pC->
        yuv1[2].pac_data = M4OSA_NULL;
    pC->yuv2[0].pac_data = pC->yuv2[1].pac_data = pC->
        yuv2[2].pac_data = M4OSA_NULL;
    pC->yuv3[0].pac_data = pC->yuv3[1].pac_data = pC->
        yuv3[2].pac_data = M4OSA_NULL;
    pC->yuv4[0].pac_data = pC->yuv4[1].pac_data = pC->
        yuv4[2].pac_data = M4OSA_NULL;
    pC->bClip1AtBeginCut = M4OSA_FALSE;
    pC->iClip1ActiveEffect = 0;
    pC->iClip2ActiveEffect = 0;
    pC->bTransitionEffect = M4OSA_FALSE;
    pC->bSupportSilence = M4OSA_FALSE;

    /**
    * Init PC->ewc members */
    // Decorrelate input and output encoding timestamp to handle encoder prefetch
    pC->ewc.dInputVidCts  = 0.0;
    pC->ewc.dOutputVidCts = 0.0;
    pC->ewc.dATo = 0.0;
    pC->ewc.iOutputDuration = 0;
    pC->ewc.VideoStreamType = M4SYS_kVideoUnknown;
    pC->ewc.uiVideoBitrate = 0;
    pC->ewc.uiVideoWidth = 0;
    pC->ewc.uiVideoHeight = 0;
    pC->ewc.uiVideoTimeScale = 0;
    pC->ewc.bVideoDataPartitioning = M4OSA_FALSE;
    pC->ewc.pVideoOutputDsi = M4OSA_NULL;
    pC->ewc.uiVideoOutputDsiSize = 0;
    pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
    pC->ewc.uiNbChannels = 1;
    pC->ewc.uiAudioBitrate = 0;
    pC->ewc.uiSamplingFrequency = 0;
    pC->ewc.pAudioOutputDsi = M4OSA_NULL;
    pC->ewc.uiAudioOutputDsiSize = 0;
    pC->ewc.pAudioEncCtxt = M4OSA_NULL;
    pC->ewc.pAudioEncDSI.infoSize = 0;
    pC->ewc.pAudioEncDSI.pInfo = M4OSA_NULL;
    pC->ewc.uiSilencePcmSize = 0;
    pC->ewc.pSilenceFrameData = M4OSA_NULL;
    pC->ewc.uiSilenceFrameSize = 0;
    pC->ewc.iSilenceFrameDuration = 0;
    pC->ewc.scale_audio = 0.0;
    pC->ewc.pEncContext = M4OSA_NULL;
    pC->ewc.pDummyAuBuffer = M4OSA_NULL;
    pC->ewc.iMpeg4GovOffset = 0;
    pC->ewc.VppError = 0;
    pC->ewc.encoderState = M4VSS3GPP_kNoEncoder;
    pC->ewc.p3gpWriterContext = M4OSA_NULL;
    pC->ewc.uiVideoMaxAuSize = 0;
    pC->ewc.uiAudioMaxAuSize = 0;
    /**
    * Keep the OSAL file functions pointer set in our context */
    pC->pOsaFileReadPtr = pFileReadPtrFct;
    pC->pOsaFileWritPtr = pFileWritePtrFct;

    /*
    * Reset pointers for media and codecs interfaces */

    err = M4VSS3GPP_clearInterfaceTables(&pC->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    /*
    *  Call the media and codecs subscription module */
    err = M4VSS3GPP_subscribeMediaAndCodec(&pC->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    /**
    * Update main state automaton */
    pC->State = M4VSS3GPP_kEditState_CREATED;
    pC->Vstate = M4VSS3GPP_kEditVideoState_READ_WRITE;
    pC->Astate = M4VSS3GPP_kEditAudioState_READ_WRITE;
    /* The flag is set to false at the beginning of every clip */
    pC->m_bClipExternalHasStarted = M4OSA_FALSE;

    pC->bIsMMS = M4OSA_FALSE;

    pC->iInOutTimeOffset = 0;
    pC->bEncodeTillEoF = M4OSA_FALSE;
    pC->nbActiveEffects = 0;
    pC->nbActiveEffects1 = 0;
    pC->bIssecondClip = M4OSA_FALSE;
    pC->m_air_context = M4OSA_NULL;
    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_editInit(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editCreateClipSettings()
 * @brief    Allows filling a clip settings structure with default values
 *
 * @note    WARNING: pClipSettings->Effects[ ] will be allocated in this function.
 *                   pClipSettings->pFile      will be allocated in this function.
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   pFile               (IN) Clip file name
 * @param   filePathSize        (IN) Clip path size (needed for UTF 16 conversion)
 * @param    nbEffects           (IN) Nb of effect settings to allocate
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR
M4VSS3GPP_editCreateClipSettings( M4VSS3GPP_ClipSettings *pClipSettings,
                                 M4OSA_Void *pFile, M4OSA_UInt32 filePathSize,
                                 M4OSA_UInt8 nbEffects )
{
    M4OSA_UInt8 uiFx;

    M4OSA_TRACE3_1(
        "M4VSS3GPP_editCreateClipSettings called with pClipSettings=0x%p",
        pClipSettings);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettings), M4ERR_PARAMETER,
        "M4VSS3GPP_editCreateClipSettings: pClipSettings is NULL");

    /**
    * Set the clip settings to default */
    pClipSettings->pFile = M4OSA_NULL;        /**< no file */
    pClipSettings->FileType =
        M4VIDEOEDITING_kFileType_Unsupported; /**< undefined */

    if( M4OSA_NULL != pFile )
    {
        //pClipSettings->pFile = (M4OSA_Char*) M4OSA_32bitAlignedMalloc(strlen(pFile)+1, M4VSS3GPP,
        // "pClipSettings->pFile");
        /*FB: add clip path size because of utf 16 conversion*/
        pClipSettings->pFile =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(filePathSize + 1, M4VSS3GPP,
            (M4OSA_Char *)"pClipSettings->pFile");

        if( M4OSA_NULL == pClipSettings->pFile )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editCreateClipSettings : ERROR allocating filename");
            return M4ERR_ALLOC;
        }
        //memcpy(pClipSettings->pFile, pFile, strlen(pFile)+1);
        /*FB: add clip path size because of utf 16 conversion*/
        memcpy((void *)pClipSettings->pFile, (void *)pFile, filePathSize + 1);
    }

    /*FB: add file path size to support UTF16 conversion*/
    pClipSettings->filePathSize = filePathSize + 1;
    /**/
    pClipSettings->ClipProperties.bAnalysed = M4OSA_FALSE;
    pClipSettings->ClipProperties.FileType = 0;
    pClipSettings->ClipProperties.Version[0] = 0;
    pClipSettings->ClipProperties.Version[1] = 0;
    pClipSettings->ClipProperties.Version[2] = 0;
    pClipSettings->ClipProperties.uiClipDuration = 0;

    pClipSettings->uiBeginCutTime = 0; /**< no begin cut */
    pClipSettings->uiEndCutTime = 0;   /**< no end cut */
    pClipSettings->ClipProperties.bSetImageData = M4OSA_FALSE;

    /**
    * Reset video characteristics */
    pClipSettings->ClipProperties.VideoStreamType = M4VIDEOEDITING_kNoneVideo;
    pClipSettings->ClipProperties.uiClipVideoDuration = 0;
    pClipSettings->ClipProperties.uiVideoBitrate = 0;
    pClipSettings->ClipProperties.uiVideoMaxAuSize = 0;
    pClipSettings->ClipProperties.uiVideoWidth = 0;
    pClipSettings->ClipProperties.uiVideoHeight = 0;
    pClipSettings->ClipProperties.uiVideoTimeScale = 0;
    pClipSettings->ClipProperties.fAverageFrameRate = 0.0;
    pClipSettings->ClipProperties.uiVideoProfile =
        M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
    pClipSettings->ClipProperties.uiVideoLevel =
        M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
    pClipSettings->ClipProperties.bMPEG4dataPartition = M4OSA_FALSE;
    pClipSettings->ClipProperties.bMPEG4rvlc = M4OSA_FALSE;
    pClipSettings->ClipProperties.bMPEG4resynchMarker = M4OSA_FALSE;

    /**
    * Reset audio characteristics */
    pClipSettings->ClipProperties.AudioStreamType = M4VIDEOEDITING_kNoneAudio;
    pClipSettings->ClipProperties.uiClipAudioDuration = 0;
    pClipSettings->ClipProperties.uiAudioBitrate = 0;
    pClipSettings->ClipProperties.uiAudioMaxAuSize = 0;
    pClipSettings->ClipProperties.uiNbChannels = 0;
    pClipSettings->ClipProperties.uiSamplingFrequency = 0;
    pClipSettings->ClipProperties.uiExtendedSamplingFrequency = 0;
    pClipSettings->ClipProperties.uiDecodedPcmSize = 0;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_editSetDefaultSettings(): returning M4NO_ERROR");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editDuplicateClipSettings()
 * @brief    Duplicates a clip settings structure, performing allocations if required
 *
 * @param    pClipSettingsDest    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param    pClipSettingsOrig    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   bCopyEffects        (IN) Flag to know if we have to duplicate effects
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR
M4VSS3GPP_editDuplicateClipSettings( M4VSS3GPP_ClipSettings *pClipSettingsDest,
                                    M4VSS3GPP_ClipSettings *pClipSettingsOrig,
                                    M4OSA_Bool bCopyEffects )
{
    M4OSA_UInt8 uiFx;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_editDuplicateClipSettings called with dest=0x%p src=0x%p",
        pClipSettingsDest, pClipSettingsOrig);

    /* Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettingsDest), M4ERR_PARAMETER,
        "M4VSS3GPP_editDuplicateClipSettings: pClipSettingsDest is NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettingsOrig), M4ERR_PARAMETER,
        "M4VSS3GPP_editDuplicateClipSettings: pClipSettingsOrig is NULL");

    /* Copy plain structure */
    memcpy((void *)pClipSettingsDest,
        (void *)pClipSettingsOrig, sizeof(M4VSS3GPP_ClipSettings));

    /* Duplicate filename */
    if( M4OSA_NULL != pClipSettingsOrig->pFile )
    {
        //pClipSettingsDest->pFile =
        // (M4OSA_Char*) M4OSA_32bitAlignedMalloc(strlen(pClipSettingsOrig->pFile)+1, M4VSS3GPP,
        // "pClipSettingsDest->pFile");
        /*FB: clip path size is needed for utf 16 conversion*/
        /*FB 2008/10/16: bad allocation size which raises a crash*/
        pClipSettingsDest->pFile =
            (M4OSA_Char *)M4OSA_32bitAlignedMalloc(pClipSettingsOrig->filePathSize + 1,
            M4VSS3GPP, (M4OSA_Char *)"pClipSettingsDest->pFile");

        if( M4OSA_NULL == pClipSettingsDest->pFile )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editDuplicateClipSettings : ERROR allocating filename");
            return M4ERR_ALLOC;
        }
        /*FB: clip path size is needed for utf 16 conversion*/
        //memcpy(pClipSettingsDest->pFile, pClipSettingsOrig->pFile,
        // strlen(pClipSettingsOrig->pFile)+1);
        /*FB 2008/10/16: bad allocation size which raises a crash*/
        memcpy((void *)pClipSettingsDest->pFile, (void *)pClipSettingsOrig->pFile,
            pClipSettingsOrig->filePathSize/*+1*/);
        ( (M4OSA_Char
            *)pClipSettingsDest->pFile)[pClipSettingsOrig->filePathSize] = '\0';
    }

    /* Duplicate effects */
    /* Return with no error */

    M4OSA_TRACE3_0(
        "M4VSS3GPP_editDuplicateClipSettings(): returning M4NO_ERROR");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editFreeClipSettings()
 * @brief    Free the pointers allocated in the ClipSetting structure (pFile, Effects).
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editFreeClipSettings(
    M4VSS3GPP_ClipSettings *pClipSettings )
{
    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettings), M4ERR_PARAMETER,
        "M4VSS3GPP_editFreeClipSettings: pClipSettings is NULL");

    /* free filename */
    if( M4OSA_NULL != pClipSettings->pFile )
    {
        free(pClipSettings->pFile);
        pClipSettings->pFile = M4OSA_NULL;
    }

    /* free effects settings */
    /*    if(M4OSA_NULL != pClipSettings->Effects)
    {
    free(pClipSettings->Effects);
    pClipSettings->Effects = M4OSA_NULL;
    pClipSettings->nbEffects = 0;
    } RC */

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editOpen()
 * @brief     Set the VSS input and output files.
 * @note      It opens the input file, but the output file may not be created yet.
 * @param     pContext           (IN) VSS edit context
 * @param     pSettings           (IN) Edit settings
 * @return    M4NO_ERROR:       No error
 * @return    M4ERR_PARAMETER:  At least one parameter is M4OSA_NULL (debug only)
 * @return    M4ERR_STATE:      VSS is not in an appropriate state for this function to be called
 * @return    M4ERR_ALLOC:      There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editOpen( M4VSS3GPP_EditContext pContext,
                             M4VSS3GPP_EditSettings *pSettings )
{
    M4VSS3GPP_InternalEditContext *pC =
        (M4VSS3GPP_InternalEditContext *)pContext;

    M4OSA_ERR err;
    M4OSA_Int32 i;
    M4VIDEOEDITING_FileType outputFileType =
        M4VIDEOEDITING_kFileType_Unsupported; /**< 3GPP or MP3 (we don't do AMR output) */
    M4OSA_UInt32 uiC1duration, uiC2duration;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_editOpen called with pContext=0x%x, pSettings=0x%x",
        pContext, pSettings);

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_editOpen: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pSettings), M4ERR_PARAMETER,
        "M4VSS3GPP_editOpen: pSettings is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pSettings->pClipList), M4ERR_PARAMETER,
        "M4VSS3GPP_editOpen: pSettings->pClipList is M4OSA_NULL");
    M4OSA_DEBUG_IF2(( pSettings->uiClipNumber > 1)
        && (M4OSA_NULL == pSettings->pTransitionList), M4ERR_PARAMETER,
        "M4VSS3GPP_editOpen: pSettings->pTransitionList is M4OSA_NULL");

    /**
    * Check state automaton */
    if( ( pC->State != M4VSS3GPP_kEditState_CREATED)
        && (pC->State != M4VSS3GPP_kEditState_CLOSED) )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_editOpen: State error (0x%x)! Returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /**
    * Free any previously allocated internal settings list */
    M4VSS3GPP_intFreeSettingsList(pC);

    /**
    * Copy the user settings in our context */
    pC->uiClipNumber = pSettings->uiClipNumber;

    /**
    * Copy the clip list */
    pC->pClipList =
        (M4VSS3GPP_ClipSettings *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_ClipSettings)
        * pC->uiClipNumber, M4VSS3GPP, (M4OSA_Char *)"pC->pClipList");

    if( M4OSA_NULL == pC->pClipList )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_editOpen: unable to allocate pC->Settings.pClipList,\
            returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    for ( i = 0; i < pSettings->uiClipNumber; i++ )
    {
        M4VSS3GPP_editDuplicateClipSettings(&(pC->pClipList[i]),
            pSettings->pClipList[i], M4OSA_TRUE);
    }

    /**
    * Copy effects list RC */

    /*FB bug fix 19.03.2008 if the number of effects is 0 -> crash*/
    if( pSettings->nbEffects > 0 )
    {
        pC->nbEffects = pSettings->nbEffects;
        pC->pEffectsList = (M4VSS3GPP_EffectSettings
            *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_EffectSettings) * pC->nbEffects,
            M4VSS3GPP, (M4OSA_Char *)"pC->pEffectsList");

        if( M4OSA_NULL == pC->pEffectsList )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editOpen: unable to allocate pC->pEffectsList, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        for ( i = 0; i < pC->nbEffects; i++ )
        {
            memcpy((void *) &(pC->pEffectsList[i]),
                (void *) &(pSettings->Effects[i]),
                sizeof(M4VSS3GPP_EffectSettings));
        }

        /**
        * Allocate active effects list RC */
        pC->pActiveEffectsList =
            (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_UInt8) * pC->nbEffects,
            M4VSS3GPP, (M4OSA_Char *)"pC->pActiveEffectsList");

        if( M4OSA_NULL == pC->pActiveEffectsList )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editOpen: unable to allocate pC->pActiveEffectsList,\
                returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        /**
         * Allocate active effects list */
        pC->pActiveEffectsList1 =
            (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_UInt8) * pC->nbEffects,
            M4VSS3GPP, (M4OSA_Char *)"pC->pActiveEffectsList");
        if (M4OSA_NULL == pC->pActiveEffectsList1)
        {
            M4OSA_TRACE1_0("M4VSS3GPP_editOpen: unable to allocate pC->pActiveEffectsList, \
                           returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

    }
    else
    {
        pC->nbEffects = 0;
        pC->nbActiveEffects = 0;
        pC->nbActiveEffects1 = 0;
        pC->pEffectsList = M4OSA_NULL;
        pC->pActiveEffectsList = M4OSA_NULL;
        pC->pActiveEffectsList1 = M4OSA_NULL;
        pC->bClip1ActiveFramingEffect = M4OSA_FALSE;
        pC->bClip2ActiveFramingEffect = M4OSA_FALSE;
    }

    /**
    * Test the clip analysis data, if it is not provided, analyse the clips by ourselves. */
    for ( i = 0; i < pC->uiClipNumber; i++ )
    {
        if( M4OSA_FALSE == pC->pClipList[i].ClipProperties.bAnalysed )
        {
            /**< Analysis not provided by the integrator */
            err = M4VSS3GPP_editAnalyseClip(pC->pClipList[i].pFile,
                pC->pClipList[i].FileType, &pC->pClipList[i].ClipProperties,
                pC->pOsaFileReadPtr);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editOpen: M4VSS3GPP_editAnalyseClip returns 0x%x!",
                    err);
                return err;
            }
        }
    }

    /**
    * Check clip compatibility */
    for ( i = 0; i < pC->uiClipNumber; i++ )
    {
        if (pC->pClipList[i].FileType !=M4VIDEOEDITING_kFileType_ARGB8888) {
            /**
            * Check all the clips are compatible with VSS 3GPP */
            err = M4VSS3GPP_intCheckClipCompatibleWithVssEditing(
                &pC->pClipList[i].ClipProperties);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_2(
                    "M4VSS3GPP_editOpen:\
                    M4VSS3GPP_intCheckClipCompatibleWithVssEditing(%d) returns 0x%x!",
                    i, err);
                return err;
            }
        }

        /**
        * Check the master clip versus all the other ones.
        (including master clip with itself, else variables for master clip
        are not properly setted) */
        if(pC->pClipList[i].FileType != M4VIDEOEDITING_kFileType_ARGB8888) {

            err = M4VSS3GPP_editCheckClipCompatibility(
                &pC->pClipList[pSettings->uiMasterClip].ClipProperties,
                &pC->pClipList[i].ClipProperties);
            /* in case of warning regarding audio incompatibility,
                editing continues */
            if( M4OSA_ERR_IS_ERROR(err) )
            {
                M4OSA_TRACE1_2(
                    "M4VSS3GPP_editOpen: M4VSS3GPP_editCheckClipCompatibility \
                        (%d) returns 0x%x!", i, err);
                return err;
            }
        } else {
            pC->pClipList[i].ClipProperties.bAudioIsCompatibleWithMasterClip =
             M4OSA_FALSE;
        }
    }
    /* Search audio tracks that cannot be edited :
    *   - delete all audio effects for the clip
    *   - if master clip is editable let the transition
    (bad track will be replaced later with silence)
    *   - if master clip is not editable switch to a dummy transition (only copy/paste) */
    for ( i = 0; i < pC->uiClipNumber; i++ )
    {
        if( M4OSA_FALSE == pC->pClipList[i].ClipProperties.bAudioIsEditable )
        {
            M4OSA_UInt8 uiFx;

            for ( uiFx = 0; uiFx < pC->nbEffects; uiFx++ )
            {
                pC->pEffectsList[uiFx].AudioEffectType
                    = M4VSS3GPP_kAudioEffectType_None;
            }

            if( ( i < (pC->uiClipNumber - 1))
                && (M4OSA_NULL != pSettings->pTransitionList[i])
                && (M4OSA_FALSE == pC->pClipList[pSettings->
                uiMasterClip].ClipProperties.bAudioIsEditable) )
            {
                pSettings->pTransitionList[i]->AudioTransitionType
                    = M4VSS3GPP_kAudioTransitionType_None;
            }
        }
    }

    /**
    * We add a transition of duration 0 at the end of the last clip.
    * It will suppress a whole bunch a test latter in the processing... */
    pC->pTransitionList = (M4VSS3GPP_TransitionSettings
        *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_TransitionSettings)
        * (pC->uiClipNumber), M4VSS3GPP, (M4OSA_Char *)"pC->pTransitionList");

    if( M4OSA_NULL == pC->pTransitionList )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_editOpen: unable to allocate pC->Settings.pTransitionList,\
            returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /**< copy transition settings */
    for ( i = 0; i < (pSettings->uiClipNumber - 1); i++ )
    {
        memcpy((void *) &(pC->pTransitionList[i]),
            (void *)pSettings->pTransitionList[i],
            sizeof(M4VSS3GPP_TransitionSettings));
    }

    /**< We fill the last "dummy" transition */
    pC->pTransitionList[pC->uiClipNumber - 1].uiTransitionDuration = 0;
    pC->pTransitionList[pC->uiClipNumber
        - 1].VideoTransitionType = M4VSS3GPP_kVideoTransitionType_None;
    pC->pTransitionList[pC->uiClipNumber
        - 1].AudioTransitionType = M4VSS3GPP_kAudioTransitionType_None;

    /**
    * Avoid weird clip settings */
    for ( i = 0; i < pSettings->uiClipNumber; i++ )
    {
        if (pC->pClipList[i].FileType !=M4VIDEOEDITING_kFileType_ARGB8888) {
            err = M4VSS3GPP_intClipSettingsSanityCheck(&pC->pClipList[i]);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editOpen: M4VSS3GPP_intClipSettingsSanityCheck returns 0x%x!",
                    err);
                return err;
            }
        }
    }

    for ( i = 0; i < (pSettings->uiClipNumber - 1); i++ )
    {
        if (pC->pTransitionList[i].uiTransitionDuration != 0) {
             if (pC->pClipList[i].FileType == M4VIDEOEDITING_kFileType_ARGB8888) {
                 pC->pClipList[i].uiBeginCutTime = 0;
                 pC->pClipList[i].uiEndCutTime =
                     pC->pTransitionList[i].uiTransitionDuration;
             }

             if (pC->pClipList[i+1].FileType == M4VIDEOEDITING_kFileType_ARGB8888) {
                 pC->pClipList[i+1].uiBeginCutTime = 0;
                 pC->pClipList[i+1].uiEndCutTime =
                     pC->pTransitionList[i].uiTransitionDuration;
             }
        } else {

             if (pC->pClipList[i].FileType == M4VIDEOEDITING_kFileType_ARGB8888) {
                 pC->pClipList[i].uiEndCutTime =
                     pC->pClipList[i].uiEndCutTime - pC->pClipList[i].uiBeginCutTime;
                 pC->pClipList[i].uiBeginCutTime = 0;
             }

             if (pC->pClipList[i+1].FileType == M4VIDEOEDITING_kFileType_ARGB8888) {
                 pC->pClipList[i+1].uiEndCutTime =
                     pC->pClipList[i+1].uiEndCutTime - pC->pClipList[i+1].uiBeginCutTime;
                 pC->pClipList[i+1].uiBeginCutTime = 0;
             }

        }

        /**
        * Maximum transition duration between clip n and clip n+1 is the duration
        * of the shortest clip */
        if( 0 == pC->pClipList[i].uiEndCutTime )
        {
            uiC1duration = pC->pClipList[i].ClipProperties.uiClipVideoDuration;
        }
        else
        {
            /**< duration of clip n is the end cut time */
            uiC1duration = pC->pClipList[i].uiEndCutTime;
        }

        /**< Substract begin cut */
        uiC1duration -= pC->pClipList[i].uiBeginCutTime;

        /**< Check that the transition is shorter than clip n */
        if( pC->pTransitionList[i].uiTransitionDuration > uiC1duration )
        {
            pC->pTransitionList[i].uiTransitionDuration = uiC1duration - 1;
        }

        if( 0 == pC->pClipList[i + 1].uiEndCutTime )
        {
            uiC2duration =
                pC->pClipList[i + 1].ClipProperties.uiClipVideoDuration;
        }
        else
        {
            /**< duration of clip n+1 is the end cut time */
            uiC2duration = pC->pClipList[i + 1].uiEndCutTime;
        }

        /**< Substract begin cut */
        uiC2duration -= pC->pClipList[i + 1].uiBeginCutTime;

        /**< Check that the transition is shorter than clip n+1 */
        if( pC->pTransitionList[i].uiTransitionDuration > uiC2duration )
        {
            pC->pTransitionList[i].uiTransitionDuration = uiC2duration - 1;
        }

        /**
        * Avoid weird transition settings */
        err =
            M4VSS3GPP_intTransitionSettingsSanityCheck(&pC->pTransitionList[i]);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editOpen: M4VSS3GPP_intClipSettingsSanityCheck returns 0x%x!",
                err);
            return err;
        }

        /**
        * Check that two transitions are not overlapping
          (no overlapping possible for first clip) */
        if( i > 0 )
        {
            /**
            * There is a transition overlap if the sum of the duration of
              two consecutive transitions
            * is higher than the duration of the clip in-between. */
            if( ( pC->pTransitionList[i - 1].uiTransitionDuration
                + pC->pTransitionList[i].uiTransitionDuration) >= uiC1duration )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editOpen: Overlapping transitions on clip %d,\
                    returning M4VSS3GPP_ERR_OVERLAPPING_TRANSITIONS",
                    i);
                return M4VSS3GPP_ERR_OVERLAPPING_TRANSITIONS;
            }
        }
    }

    /**
    * Output clip duration */
    for ( i = 0; i < pC->uiClipNumber; i++ )
    {
        /**
        * Compute the sum of the clip duration */
        if( 0 == pC->pClipList[i].uiEndCutTime )
        {
            pC->ewc.iOutputDuration +=
                pC->
                pClipList[
                    i].ClipProperties.
                        uiClipVideoDuration; /* Only video track duration is important to
                                             avoid deviation if audio track is longer */
        }
        else
        {
            pC->ewc.iOutputDuration +=
                pC->pClipList[i].uiEndCutTime; /**< Add end cut */
        }

        pC->ewc.iOutputDuration -=
            pC->pClipList[i].uiBeginCutTime; /**< Remove begin cut */

        /**
        * Remove the duration of the transition (it is counted twice) */
        pC->ewc.iOutputDuration -= pC->pTransitionList[i].uiTransitionDuration;
    }

    /* Get video properties from output properties */

    /* Get output width and height */
    switch(pC->xVSS.outputVideoSize) {
        case M4VIDEOEDITING_kSQCIF:
            pC->ewc.uiVideoWidth = 128;
            pC->ewc.uiVideoHeight = 96;
            break;
        case M4VIDEOEDITING_kQQVGA:
            pC->ewc.uiVideoWidth = 160;
            pC->ewc.uiVideoHeight = 120;
            break;
        case M4VIDEOEDITING_kQCIF:
            pC->ewc.uiVideoWidth = 176;
            pC->ewc.uiVideoHeight = 144;
            break;
        case M4VIDEOEDITING_kQVGA:
            pC->ewc.uiVideoWidth = 320;
            pC->ewc.uiVideoHeight = 240;
            break;
        case M4VIDEOEDITING_kCIF:
            pC->ewc.uiVideoWidth = 352;
            pC->ewc.uiVideoHeight = 288;
            break;
        case M4VIDEOEDITING_kVGA:
            pC->ewc.uiVideoWidth = 640;
            pC->ewc.uiVideoHeight = 480;
            break;
            /* +PR LV5807 */
        case M4VIDEOEDITING_kWVGA:
            pC->ewc.uiVideoWidth = 800;
            pC->ewc.uiVideoHeight = 480;
            break;
        case M4VIDEOEDITING_kNTSC:
            pC->ewc.uiVideoWidth = 720;
            pC->ewc.uiVideoHeight = 480;
            break;
            /* -PR LV5807 */
            /* +CR Google */
        case M4VIDEOEDITING_k640_360:
            pC->ewc.uiVideoWidth = 640;
            pC->ewc.uiVideoHeight = 360;
            break;

        case M4VIDEOEDITING_k854_480:
            pC->ewc.uiVideoWidth = M4ENCODER_854_480_Width;
            pC->ewc.uiVideoHeight = 480;
            break;

        case M4VIDEOEDITING_k1280_720:
            pC->ewc.uiVideoWidth = 1280;
            pC->ewc.uiVideoHeight = 720;
            break;
        case M4VIDEOEDITING_k1080_720:
            pC->ewc.uiVideoWidth = M4ENCODER_1080_720_Width;

            pC->ewc.uiVideoHeight = 720;
            break;
        case M4VIDEOEDITING_k960_720:
            pC->ewc.uiVideoWidth = 960;
            pC->ewc.uiVideoHeight = 720;
            break;
        case M4VIDEOEDITING_k1920_1080:
            pC->ewc.uiVideoWidth = 1920;
            pC->ewc.uiVideoHeight = 1088; // need to be multiples of 16
            break;

        default: /* If output video size is not given, we take QCIF size */
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editOpen: no output video size given, default to QCIF!");
            pC->ewc.uiVideoWidth = 176;
            pC->ewc.uiVideoHeight = 144;
            pC->xVSS.outputVideoSize = M4VIDEOEDITING_kQCIF;
            break;
    }

    pC->ewc.uiVideoTimeScale        = 30;
    pC->ewc.bVideoDataPartitioning  = 0;
    /* Set output video profile and level */
    pC->ewc.outputVideoProfile = pC->xVSS.outputVideoProfile;
    pC->ewc.outputVideoLevel = pC->xVSS.outputVideoLevel;

    switch(pC->xVSS.outputVideoFormat) {
        case M4VIDEOEDITING_kH263:
            pC->ewc.VideoStreamType = M4SYS_kH263;
            break;
        case M4VIDEOEDITING_kMPEG4:
            pC->ewc.VideoStreamType = M4SYS_kMPEG_4;
            break;
        case M4VIDEOEDITING_kH264:
            pC->ewc.VideoStreamType = M4SYS_kH264;
            break;
        default:
            pC->ewc.VideoStreamType = M4SYS_kVideoUnknown;
            break;
    }

    /**
    * Copy the audio properties of the master clip to the output properties */
    pC->ewc.uiNbChannels =
        pC->pClipList[pSettings->uiMasterClip].ClipProperties.uiNbChannels;
    pC->ewc.uiAudioBitrate =
        pC->pClipList[pSettings->uiMasterClip].ClipProperties.uiAudioBitrate;
    pC->ewc.uiSamplingFrequency = pC->pClipList[pSettings->
        uiMasterClip].ClipProperties.uiSamplingFrequency;
    pC->ewc.uiSilencePcmSize =
        pC->pClipList[pSettings->uiMasterClip].ClipProperties.uiDecodedPcmSize;
    pC->ewc.scale_audio = pC->ewc.uiSamplingFrequency / 1000.0;

    switch( pC->pClipList[pSettings->uiMasterClip].ClipProperties.AudioStreamType )
    {
        case M4VIDEOEDITING_kAMR_NB:
            pC->ewc.AudioStreamType = M4SYS_kAMR;
            pC->ewc.pSilenceFrameData =
                (M4OSA_UInt8 *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048;
            pC->ewc.uiSilenceFrameSize =
                M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
            pC->ewc.iSilenceFrameDuration =
                M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION;
            pC->bSupportSilence = M4OSA_TRUE;
            break;

        case M4VIDEOEDITING_kAAC:
        case M4VIDEOEDITING_kAACplus:
        case M4VIDEOEDITING_keAACplus:
            pC->ewc.AudioStreamType = M4SYS_kAAC;

            if( pC->ewc.uiNbChannels == 1 )
            {
                pC->ewc.pSilenceFrameData =
                    (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_MONO;
                pC->ewc.uiSilenceFrameSize = M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                pC->bSupportSilence = M4OSA_TRUE;
            }
            else
            {
                pC->ewc.pSilenceFrameData =
                    (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_STEREO;
                pC->ewc.uiSilenceFrameSize =
                    M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                pC->bSupportSilence = M4OSA_TRUE;
            }
            pC->ewc.iSilenceFrameDuration =
                1024; /* AAC is always 1024/Freq sample duration */
            break;

        case M4VIDEOEDITING_kMP3:
            pC->ewc.AudioStreamType = M4SYS_kMP3;
            pC->ewc.pSilenceFrameData = M4OSA_NULL;
            pC->ewc.uiSilenceFrameSize = 0;
            pC->ewc.iSilenceFrameDuration = 0;
            /* Special case, mp3 core reader return a time in ms */
            pC->ewc.scale_audio = 1.0;
            break;

        case M4VIDEOEDITING_kEVRC:
            pC->ewc.AudioStreamType = M4SYS_kEVRC;
            pC->ewc.pSilenceFrameData = M4OSA_NULL;
            pC->ewc.uiSilenceFrameSize = 0;
            pC->ewc.iSilenceFrameDuration = 160; /* EVRC frames are 20 ms at 8000 Hz
                                             (makes it easier to factorize amr and evrc code) */
            break;

        default:
            pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
            break;
    }

    for (i=0; i<pC->uiClipNumber; i++) {
        if (pC->pClipList[i].bTranscodingRequired == M4OSA_FALSE) {
            /** If not transcoded in Analysis phase, check
             * if transcoding required now
             */
            if ((pC->pClipList[i].ClipProperties.VideoStreamType !=
                  pC->xVSS.outputVideoFormat)||
                  (pC->pClipList[i].ClipProperties.uiVideoWidth !=
                   pC->ewc.uiVideoWidth) ||
                  (pC->pClipList[i].ClipProperties.uiVideoHeight !=
                   pC->ewc.uiVideoHeight) ||
                  (pC->pClipList[i].ClipProperties.VideoStreamType ==
                   M4VIDEOEDITING_kH264) ||
                  (pC->pClipList[i].ClipProperties.VideoStreamType ==
                   M4VIDEOEDITING_kMPEG4 &&
                   pC->pClipList[i].ClipProperties.uiVideoTimeScale !=
                    pC->ewc.uiVideoTimeScale)) {
                pC->pClipList[i].bTranscodingRequired = M4OSA_TRUE;
            }
        } else {
            /** If bTranscodingRequired is true, it means the clip has
             * been transcoded in Analysis phase.
             */
            pC->pClipList[i].bTranscodingRequired = M4OSA_FALSE;
        }
    }
    /**
    * We produce a 3gpp file, unless it is mp3 */
    if( M4VIDEOEDITING_kMP3 == pC->
        pClipList[pSettings->uiMasterClip].ClipProperties.AudioStreamType )
        outputFileType = M4VIDEOEDITING_kFileType_MP3;
    else
        outputFileType = M4VIDEOEDITING_kFileType_3GPP;

    /**
    * Beware, a null duration would lead to a divide by zero error (better safe than sorry...) */
    if( 0 == pC->ewc.iOutputDuration )
    {
        pC->ewc.iOutputDuration = 1;
    }

    /**
    * Open first clip */
    pC->uiCurrentClip = 0;

    // Decorrelate input and output encoding timestamp to handle encoder prefetch
    pC->ewc.dInputVidCts  = 0.0;
    pC->ewc.dOutputVidCts = 0.0;
    pC->ewc.dATo = 0.0;

    err = M4VSS3GPP_intSwitchToNextClip(pC);
    /* RC: to know when a file has been processed */
    if( M4NO_ERROR != err && err != M4VSS3GPP_WAR_SWITCH_CLIP )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_editOpen: M4VSS3GPP_intSwitchToNextClip() returns 0x%x!",
            err);
        return err;
    }

    /**
    * Do the video stuff in 3GPP Audio/Video case */
    if( M4VIDEOEDITING_kFileType_3GPP == outputFileType )
    {
        /**
        * Compute the Decoder Specific Info for the output video and audio streams */
        err = M4VSS3GPP_intComputeOutputVideoAndAudioDsi(pC,
            pSettings->uiMasterClip);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editOpen: M4VSS3GPP_intComputeOutputVideoAndAudioDsi() returns 0x%x!",
                err);
            return err;
        }

        /**
        * Compute the time increment for the transition file */
        switch( pSettings->videoFrameRate )
        {
            case M4VIDEOEDITING_k5_FPS:
                pC->dOutputFrameDuration = 1000.0 / 5.0;
                break;

            case M4VIDEOEDITING_k7_5_FPS:
                pC->dOutputFrameDuration = 1000.0 / 7.5;
                break;

            case M4VIDEOEDITING_k10_FPS:
                pC->dOutputFrameDuration = 1000.0 / 10.0;
                break;

            case M4VIDEOEDITING_k12_5_FPS:
                pC->dOutputFrameDuration = 1000.0 / 12.5;
                break;

            case M4VIDEOEDITING_k15_FPS:
                pC->dOutputFrameDuration = 1000.0 / 15.0;
                break;

            case M4VIDEOEDITING_k20_FPS:
                pC->dOutputFrameDuration = 1000.0 / 20.0;
                break;

            case M4VIDEOEDITING_k25_FPS:
                pC->dOutputFrameDuration = 1000.0 / 25.0;
                break;

            case M4VIDEOEDITING_k30_FPS:
                pC->dOutputFrameDuration = 1000.0 / 30.0;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editOpen(): invalid videoFrameRate (0x%x),\
                    returning M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE",
                    pSettings->videoFrameRate);
                return M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE;
        }

        if( M4SYS_kMPEG_4 == pC->ewc.VideoStreamType )
        {
            M4OSA_UInt32 uiAlpha;
            /**
            * MPEG-4 case.
            * Time scale of the transition encoder must be the same than the
            * timescale of the input files.
            * So the frame duration must be compatible with this time scale,
            * but without beeing too short.
            * For that, we must compute alpha (integer) so that:
            *             (alpha x 1000)/EncoderTimeScale > MinFrameDuration
            **/

            uiAlpha = (M4OSA_UInt32)(( pC->dOutputFrameDuration
                * pC->ewc.uiVideoTimeScale) / 1000.0 + 0.5);

            if( uiAlpha > 0 )
            {
                pC->dOutputFrameDuration =
                    ( uiAlpha * 1000.0) / pC->ewc.uiVideoTimeScale;
            }
        }
        else if( M4SYS_kH263 == pC->ewc.VideoStreamType )
        {
            switch( pSettings->videoFrameRate )
            {
                case M4VIDEOEDITING_k12_5_FPS:
                case M4VIDEOEDITING_k20_FPS:
                case M4VIDEOEDITING_k25_FPS:
                    M4OSA_TRACE1_0(
                        "M4VSS3GPP_editOpen(): invalid videoFrameRate for H263,\
                        returning M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE");
                    return M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE;
               default:
                  break;
            }
        }
    }

    /**
    * Create the MP3 output file */
    if( M4VIDEOEDITING_kFileType_MP3 == outputFileType )
    {
        M4READER_Buffer mp3tagBuffer;
        err = M4VSS3GPP_intCreateMP3OutputFile(pC, pSettings->pOutputFile);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editOpen: M4VSS3GPP_intCreateMP3OutputFile returns 0x%x",
                err);
            return err;
        }

        /* The ID3v2 tag could be at any place in the mp3 file                             */
        /* The mp3 reader only checks few bytes in the beginning of
           stream to look for a ID3v2 tag  */
        /* It means that if the ID3v2 tag is not at the beginning of the file the reader do
        as there is no these metadata */

        /* Retrieve the data of the ID3v2 Tag */
        err = pC->pC1->ShellAPI.m_pReader->m_pFctGetOption(
            pC->pC1->pReaderContext, M4READER_kOptionID_Mp3Id3v2Tag,
            (M4OSA_DataOption) &mp3tagBuffer);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1("M4VSS3GPP_editOpen: M4MP3R_getOption returns 0x%x",
                err);
            return err;
        }

        /* Write the data of the ID3v2 Tag in the output file */
        if( 0 != mp3tagBuffer.m_uiBufferSize )
        {
            err = pC->pOsaFileWritPtr->writeData(pC->ewc.p3gpWriterContext,
                (M4OSA_MemAddr8)mp3tagBuffer.m_pData, mp3tagBuffer.m_uiBufferSize);

            /**
            * Free before the error checking anyway */
            free(mp3tagBuffer.m_pData);

            /**
            * Error checking */
            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editOpen: WriteData(ID3v2Tag) returns 0x%x",
                    err);
                return err;
            }

            mp3tagBuffer.m_uiBufferSize = 0;
            mp3tagBuffer.m_pData = M4OSA_NULL;
        }
    }
    /**
    * Create the 3GPP output file */
    else if( M4VIDEOEDITING_kFileType_3GPP == outputFileType )
    {
        pC->ewc.uiVideoBitrate = pSettings->xVSS.outputVideoBitrate;

        /**
        * 11/12/2008 CR3283 MMS use case in VideoArtist: Set max output file size if needed */
        if( pC->bIsMMS == M4OSA_TRUE )
        {
            err = M4VSS3GPP_intCreate3GPPOutputFile(&pC->ewc, &pC->ShellAPI,
                pC->pOsaFileWritPtr, pSettings->pOutputFile,
                pC->pOsaFileReadPtr, pSettings->pTemporaryFile,
                pSettings->xVSS.outputFileSize);
        }
        else
        {
            err = M4VSS3GPP_intCreate3GPPOutputFile(&pC->ewc, &pC->ShellAPI,
                pC->pOsaFileWritPtr, pSettings->pOutputFile,
                pC->pOsaFileReadPtr, pSettings->pTemporaryFile, 0);
        }

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editOpen: M4VSS3GPP_intCreate3GPPOutputFile returns 0x%x",
                err);
            return err;
        }
    }
    /**
    * Default error case */
    else
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_editOpen: invalid outputFileType = 0x%x,\
            returning M4VSS3GPP_ERR_OUTPUT_FILE_TYPE_ERROR",
            outputFileType);
        return
            M4VSS3GPP_ERR_OUTPUT_FILE_TYPE_ERROR; /**< this is an internal error code
                                                  unknown to the user */
    }

    /**
    * Initialize state */
    if( M4SYS_kMP3 == pC->ewc.AudioStreamType )
    {
        /**
        * In the MP3 case we use a special audio state */
        pC->State = M4VSS3GPP_kEditState_MP3_JUMP;
    }
    else
    {
        /**
        * We start with the video processing */
        pC->State = M4VSS3GPP_kEditState_VIDEO;
    }

    /**
    * Initialize state.
    * The first clip is independant to the "virtual previous clips",
    * so it's like if we where in Read/Write mode before it. */
    pC->Vstate = M4VSS3GPP_kEditVideoState_READ_WRITE;
    pC->Astate = M4VSS3GPP_kEditAudioState_READ_WRITE;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_editOpen(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editStep()
 * @brief    Perform one step of editing.
 * @note
 * @param     pContext           (IN) VSS 3GPP edit context
 * @param     pProgress          (OUT) Progress percentage (0 to 100) of the editing operation
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:   pContext is M4OSA_NULL (debug only)
 * @return    M4ERR_STATE:       VSS 3GPP is not in an appropriate state for this
 *                               function to be called
 * @return    M4VSS3GPP_WAR_EDITING_DONE: Edition is done, user should now call
 *            M4VSS3GPP_editClose()
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editStep( M4VSS3GPP_EditContext pContext,
                             M4OSA_UInt8 *pProgress )
{
    M4VSS3GPP_InternalEditContext *pC =
        (M4VSS3GPP_InternalEditContext *)pContext;
    M4OSA_UInt32 uiProgressAudio, uiProgressVideo, uiProgress;
    M4OSA_ERR err;

    M4OSA_TRACE3_1("M4VSS3GPP_editStep called with pContext=0x%x", pContext);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_editStep: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pProgress), M4ERR_PARAMETER,
        "M4VSS3GPP_editStep: pProgress is M4OSA_NULL");

    /**
    * Check state automaton and select correct processing */
    switch( pC->State )
    {
        case M4VSS3GPP_kEditState_VIDEO:
            err = M4VSS3GPP_intEditStepVideo(pC);
            break;

        case M4VSS3GPP_kEditState_AUDIO:
            err = M4VSS3GPP_intEditStepAudio(pC);
            break;

        case M4VSS3GPP_kEditState_MP3:
            err = M4VSS3GPP_intEditStepMP3(pC);
            break;

        case M4VSS3GPP_kEditState_MP3_JUMP:
            err = M4VSS3GPP_intEditJumpMP3(pC);
            break;

        default:
            M4OSA_TRACE1_0(
                "M4VSS3GPP_editStep(): invalid internal state (0x%x), returning M4ERR_STATE");
            return M4ERR_STATE;
    }

    /**
    * Compute progress.
    * We do the computing with 32bits precision because in some (very) extreme case, we may get
    * values higher than 256 (...) */
    uiProgressAudio =
        ( (M4OSA_UInt32)(pC->ewc.dATo * 100)) / pC->ewc.iOutputDuration;
    // Decorrelate input and output encoding timestamp to handle encoder prefetch
    uiProgressVideo = ((M4OSA_UInt32)(pC->ewc.dInputVidCts * 100)) / pC->ewc.iOutputDuration;

    uiProgress = uiProgressAudio + uiProgressVideo;

    if( ( pC->ewc.AudioStreamType != M4SYS_kAudioUnknown)
        && (pC->ewc.VideoStreamType != M4SYS_kVideoUnknown) )
        uiProgress /= 2;

    /**
    * Sanity check */
    if( uiProgress > 100 )
    {
        *pProgress = 100;
    }
    else
    {
        *pProgress = (M4OSA_UInt8)uiProgress;
    }

    /**
    * Return the error */
    M4OSA_TRACE3_1("M4VSS3GPP_editStep(): returning 0x%x", err);
    return err;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editClose()
 * @brief    Finish the VSS edit operation.
 * @note    The output 3GPP file is ready to be played after this call
 * @param    pContext           (IN) VSS edit context
 * @return    M4NO_ERROR:       No error
 * @return    M4ERR_PARAMETER:  pContext is M4OSA_NULL (debug only)
 * @return    M4ERR_STATE:      VSS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editClose( M4VSS3GPP_EditContext pContext )
{
    M4VSS3GPP_InternalEditContext *pC =
        (M4VSS3GPP_InternalEditContext *)pContext;
    M4OSA_ERR err;
    M4OSA_ERR returnedError = M4NO_ERROR;
    M4OSA_UInt32 lastCTS;

    M4OSA_TRACE3_1("M4VSS3GPP_editClose called with pContext=0x%x", pContext);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_editClose: pContext is M4OSA_NULL");

    /**
    * Check state automaton.
    * In "theory", we should not authorize closing if we are in CREATED state.
    * But in practice, in case the opening failed, it may have been partially done.
    * In that case we have to free some opened ressources by calling Close. */
    if( M4VSS3GPP_kEditState_CLOSED == pC->State )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_editClose: Wrong state (0x%x), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /**
    * There may be an encoder to destroy */
    err = M4VSS3GPP_intDestroyVideoEncoder(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_editClose: M4VSS3GPP_editDestroyVideoEncoder() returns 0x%x!",
            err);
        /**< We do not return the error here because we still have stuff to free */
        returnedError = err;
    }

    /**
    * Close the output file */
    if( M4SYS_kMP3 == pC->ewc.AudioStreamType )
    {
        /**
        * MP3 case */
        if( M4OSA_NULL != pC->ewc.p3gpWriterContext )
        {
            err = pC->pOsaFileWritPtr->closeWrite(pC->ewc.p3gpWriterContext);
            pC->ewc.p3gpWriterContext = M4OSA_NULL;
        }
    }
    else
    {
        /**
        * Close the output 3GPP clip, if it has been opened */
        if( M4OSA_NULL != pC->ewc.p3gpWriterContext )
        {
            /* Update last Video CTS */
            lastCTS = pC->ewc.iOutputDuration;

            err = pC->ShellAPI.pWriterGlobalFcts->pFctSetOption(
                pC->ewc.p3gpWriterContext,
                (M4OSA_UInt32)M4WRITER_kMaxFileDuration, &lastCTS);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editClose: SetOption(M4WRITER_kMaxFileDuration) returns 0x%x",
                    err);
            }

            err = pC->ShellAPI.pWriterGlobalFcts->pFctCloseWrite(
                pC->ewc.p3gpWriterContext);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_editClose: pFctCloseWrite(OUT) returns 0x%x!",
                    err);
                /**< We do not return the error here because we still have stuff to free */
                if( M4NO_ERROR
                    == returnedError ) /**< we return the first error that happened */
                {
                    returnedError = err;
                }
            }
            pC->ewc.p3gpWriterContext = M4OSA_NULL;
        }
    }

    /**
    * Free the output video DSI, if it has been created */
    if( M4OSA_NULL != pC->ewc.pVideoOutputDsi )
    {
        free(pC->ewc.pVideoOutputDsi);
        pC->ewc.pVideoOutputDsi = M4OSA_NULL;
    }

    /**
    * Free the output audio DSI, if it has been created */
    if( M4OSA_NULL != pC->ewc.pAudioOutputDsi )
    {
        free(pC->ewc.pAudioOutputDsi);
        pC->ewc.pAudioOutputDsi = M4OSA_NULL;
    }

    /**
    * Close clip1, if needed */
    if( M4OSA_NULL != pC->pC1 )
    {
        err = M4VSS3GPP_intClipCleanUp(pC->pC1);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editClose: M4VSS3GPP_intClipCleanUp(C1) returns 0x%x!",
                err);
            /**< We do not return the error here because we still have stuff to free */
            if( M4NO_ERROR
                == returnedError ) /**< we return the first error that happened */
            {
                returnedError = err;
            }
        }
        pC->pC1 = M4OSA_NULL;
    }

    /**
    * Close clip2, if needed */
    if( M4OSA_NULL != pC->pC2 )
    {
        err = M4VSS3GPP_intClipCleanUp(pC->pC2);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editClose: M4VSS3GPP_intClipCleanUp(C2) returns 0x%x!",
                err);
            /**< We do not return the error here because we still have stuff to free */
            if( M4NO_ERROR
                == returnedError ) /**< we return the first error that happened */
            {
                returnedError = err;
            }
        }
        pC->pC2 = M4OSA_NULL;
    }

    /**
    * Free the temporary YUV planes */
    if( M4OSA_NULL != pC->yuv1[0].pac_data )
    {
        free(pC->yuv1[0].pac_data);
        pC->yuv1[0].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv1[1].pac_data )
    {
        free(pC->yuv1[1].pac_data);
        pC->yuv1[1].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv1[2].pac_data )
    {
        free(pC->yuv1[2].pac_data);
        pC->yuv1[2].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv2[0].pac_data )
    {
        free(pC->yuv2[0].pac_data);
        pC->yuv2[0].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv2[1].pac_data )
    {
        free(pC->yuv2[1].pac_data);
        pC->yuv2[1].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv2[2].pac_data )
    {
        free(pC->yuv2[2].pac_data);
        pC->yuv2[2].pac_data = M4OSA_NULL;
    }

    /* RC */
    if( M4OSA_NULL != pC->yuv3[0].pac_data )
    {
        free(pC->yuv3[0].pac_data);
        pC->yuv3[0].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv3[1].pac_data )
    {
        free(pC->yuv3[1].pac_data);
        pC->yuv3[1].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv3[2].pac_data )
    {
        free(pC->yuv3[2].pac_data);
        pC->yuv3[2].pac_data = M4OSA_NULL;
    }

    /* RC */
    if( M4OSA_NULL != pC->yuv4[0].pac_data )
    {
        free(pC->yuv4[0].pac_data);
        pC->yuv4[0].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv4[1].pac_data )
    {
        free(pC->yuv4[1].pac_data);
        pC->yuv4[1].pac_data = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->yuv4[2].pac_data )
    {
        free(pC->yuv4[2].pac_data);
        pC->yuv4[2].pac_data = M4OSA_NULL;
    }

    /**
    * RC Free effects list */
    if( pC->pEffectsList != M4OSA_NULL )
    {
        free(pC->pEffectsList);
        pC->pEffectsList = M4OSA_NULL;
    }

    /**
    * RC Free active effects list */
    if( pC->pActiveEffectsList != M4OSA_NULL )
    {
        free(pC->pActiveEffectsList);
        pC->pActiveEffectsList = M4OSA_NULL;
    }
    /**
     *  Free active effects list */
    if(pC->pActiveEffectsList1 != M4OSA_NULL)
    {
        free(pC->pActiveEffectsList1);
        pC->pActiveEffectsList1 = M4OSA_NULL;
    }
    if(pC->m_air_context != M4OSA_NULL) {
        free(pC->m_air_context);
        pC->m_air_context = M4OSA_NULL;
    }
    /**
    * Update state automaton */
    pC->State = M4VSS3GPP_kEditState_CLOSED;

    /**
    * Return with no error */
    M4OSA_TRACE3_1("M4VSS3GPP_editClose(): returning 0x%x", returnedError);
    return returnedError;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editCleanUp()
 * @brief    Free all resources used by the VSS edit operation.
 * @note    The context is no more valid after this call
 * @param    pContext            (IN) VSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editCleanUp( M4VSS3GPP_EditContext pContext )
{
    M4OSA_ERR err;
    M4VSS3GPP_InternalEditContext *pC =
        (M4VSS3GPP_InternalEditContext *)pContext;

    M4OSA_TRACE3_1("M4VSS3GPP_editCleanUp called with pContext=0x%x", pContext);

    /**
    *    Check input parameter */
    if( M4OSA_NULL == pContext )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_editCleanUp(): pContext is M4OSA_NULL, returning M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    /**
    * Close, if needed.
    * In "theory", we should not close if we are in CREATED state.
    * But in practice, in case the opening failed, it may have been partially done.
    * In that case we have to free some opened ressources by calling Close. */
    if( M4VSS3GPP_kEditState_CLOSED != pC->State )
    {
        M4OSA_TRACE3_0("M4VSS3GPP_editCleanUp(): calling M4VSS3GPP_editClose");
        err = M4VSS3GPP_editClose(pC);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editCleanUp(): M4VSS3GPP_editClose returns 0x%x",
                err);
        }
    }

    /**
    * Free the video encoder dummy AU */
    if( M4OSA_NULL != pC->ewc.pDummyAuBuffer )
    {
        free(pC->ewc.pDummyAuBuffer);
        pC->ewc.pDummyAuBuffer = M4OSA_NULL;
    }

    /**
    * Free the Audio encoder context */
    if( M4OSA_NULL != pC->ewc.pAudioEncCtxt )
    {
        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctClose(
            pC->ewc.pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editCleanUp: pAudioEncoderGlobalFcts->pFctClose returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctCleanUp(
            pC->ewc.pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_editCleanUp: pAudioEncoderGlobalFcts->pFctCleanUp returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        pC->ewc.pAudioEncCtxt = M4OSA_NULL;
    }

    /**
    * Free the shells interfaces */
    M4VSS3GPP_unRegisterAllWriters(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllEncoders(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllReaders(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllDecoders(&pC->ShellAPI);

    /**
    * Free the settings copied in the internal context */
    M4VSS3GPP_intFreeSettingsList(pC);

    /**
    * Finally, Free context */
    free(pC);
    pC = M4OSA_NULL;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_editCleanUp(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

#ifdef WIN32
/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_GetErrorMessage()
 * @brief    Return a string describing the given error code
 * @note    The input string must be already allocated (and long enough!)
 * @param    err                (IN) Error code to get the description from
 * @param    sMessage        (IN/OUT) Allocated string in which the description will be copied
 * @return    M4NO_ERROR:        Input error is from the VSS3GPP module
 * @return    M4ERR_PARAMETER:Input error is not from the VSS3GPP module
 ******************************************************************************
 */

M4OSA_ERR M4VSS3GPP_GetErrorMessage( M4OSA_ERR err, M4OSA_Char *sMessage )
{
    switch( err )
    {
        case M4VSS3GPP_WAR_EDITING_DONE:
            strcpy(sMessage, "M4VSS3GPP_WAR_EDITING_DONE");
            break;

        case M4VSS3GPP_WAR_END_OF_AUDIO_MIXING:
            strcpy(sMessage, "M4VSS3GPP_WAR_END_OF_AUDIO_MIXING");
            break;

        case M4VSS3GPP_WAR_END_OF_EXTRACT_PICTURE:
            strcpy(sMessage, "M4VSS3GPP_WAR_END_OF_EXTRACT_PICTURE");
            break;

        case M4VSS3GPP_ERR_INVALID_FILE_TYPE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_FILE_TYPE");
            break;

        case M4VSS3GPP_ERR_INVALID_EFFECT_KIND:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_EFFECT_KIND");
            break;

        case M4VSS3GPP_ERR_INVALID_VIDEO_EFFECT_TYPE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_VIDEO_EFFECT_TYPE");
            break;

        case M4VSS3GPP_ERR_INVALID_AUDIO_EFFECT_TYPE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_AUDIO_EFFECT_TYPE");
            break;

        case M4VSS3GPP_ERR_INVALID_VIDEO_TRANSITION_TYPE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_VIDEO_TRANSITION_TYPE");
            break;

        case M4VSS3GPP_ERR_INVALID_AUDIO_TRANSITION_TYPE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_AUDIO_TRANSITION_TYPE");
            break;

        case M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_VIDEO_ENCODING_FRAME_RATE");
            break;

        case M4VSS3GPP_ERR_EXTERNAL_EFFECT_NULL:
            strcpy(sMessage, "M4VSS3GPP_ERR_EXTERNAL_EFFECT_NULL");
            break;

        case M4VSS3GPP_ERR_EXTERNAL_TRANSITION_NULL:
            strcpy(sMessage, "M4VSS3GPP_ERR_EXTERNAL_TRANSITION_NULL");
            break;

        case M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_DURATION:
            strcpy(sMessage, "M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_DURATION");
            break;

        case M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_END_CUT:
            strcpy(sMessage, "M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_END_CUT");
            break;

        case M4VSS3GPP_ERR_OVERLAPPING_TRANSITIONS:
            strcpy(sMessage, "M4VSS3GPP_ERR_OVERLAPPING_TRANSITIONS");
            break;

        case M4VSS3GPP_ERR_INVALID_3GPP_FILE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_3GPP_FILE");
            break;

        case M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT");
            break;

        case M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT");
            break;

        case M4VSS3GPP_ERR_AMR_EDITING_UNSUPPORTED:
            strcpy(sMessage, "M4VSS3GPP_ERR_AMR_EDITING_UNSUPPORTED");
            break;

        case M4VSS3GPP_ERR_INPUT_VIDEO_AU_TOO_LARGE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INPUT_VIDEO_AU_TOO_LARGE");
            break;

        case M4VSS3GPP_ERR_INPUT_AUDIO_AU_TOO_LARGE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INPUT_AUDIO_AU_TOO_LARGE");
            break;

        case M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU:
            strcpy(sMessage, "M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU");
            break;

        case M4VSS3GPP_ERR_ENCODER_ACCES_UNIT_ERROR:
            strcpy(sMessage, "M4VSS3GPP_ERR_ENCODER_ACCES_UNIT_ERROR");
            break;

        case M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT");
            break;

        case M4VSS3GPP_ERR_EDITING_UNSUPPORTED_H263_PROFILE:
            strcpy(sMessage, "M4VSS3GPP_ERR_EDITING_UNSUPPORTED_H263_PROFILE");
            break;

        case M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_PROFILE:
            strcpy(sMessage, "M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_PROFILE");
            break;

        case M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_RVLC:
            strcpy(sMessage, "M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_RVLC");
            break;

        case M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT");
            break;

        case M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_STREAM_IN_FILE:
            strcpy(sMessage,
                "M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_STREAM_IN_FILE");
            break;

        case M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_VIDEO_STREAM_IN_FILE:
            strcpy(sMessage,
                "M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_VIDEO_STREAM_IN_FILE");
            break;

        case M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION:
            strcpy(sMessage, "M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION");
            break;

        case M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FORMAT");
            break;

        case M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FRAME_SIZE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FRAME_SIZE");
            break;

        case M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_TIME_SCALE:
            strcpy(sMessage, "M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_TIME_SCALE");
            break;

        case M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_DATA_PARTITIONING:
            strcpy(sMessage,
                "M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_DATA_PARTITIONING");
            break;

        case M4VSS3GPP_ERR_UNSUPPORTED_MP3_ASSEMBLY:
            strcpy(sMessage, "M4VSS3GPP_ERR_UNSUPPORTED_MP3_ASSEMBLY");
            break;

        case M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_STREAM_TYPE:
            strcpy(sMessage, "M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_STREAM_TYPE");
            break;

        case M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_NB_OF_CHANNELS:
            strcpy(sMessage, "M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_NB_OF_CHANNELS");
            break;

        case M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_SAMPLING_FREQUENCY:
            strcpy(sMessage,
                "M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_SAMPLING_FREQUENCY");
            break;

        case M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE:
            strcpy(sMessage, "M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE");
            break;

        case M4VSS3GPP_ERR_ADDVOLUME_EQUALS_ZERO:
            strcpy(sMessage, "M4VSS3GPP_ERR_ADDVOLUME_EQUALS_ZERO");
            break;

        case M4VSS3GPP_ERR_ADDCTS_HIGHER_THAN_VIDEO_DURATION:
            strcpy(sMessage, "M4VSS3GPP_ERR_ADDCTS_HIGHER_THAN_VIDEO_DURATION");
            break;

        case M4VSS3GPP_ERR_UNDEFINED_AUDIO_TRACK_FILE_FORMAT:
            strcpy(sMessage, "M4VSS3GPP_ERR_UNDEFINED_AUDIO_TRACK_FILE_FORMAT");
            break;

        case M4VSS3GPP_ERR_UNSUPPORTED_ADDED_AUDIO_STREAM:
            strcpy(sMessage, "M4VSS3GPP_ERR_UNSUPPORTED_ADDED_AUDIO_STREAM");
            break;

        case M4VSS3GPP_ERR_AUDIO_MIXING_UNSUPPORTED:
            strcpy(sMessage, "M4VSS3GPP_ERR_AUDIO_MIXING_UNSUPPORTED");
            break;

        case M4VSS3GPP_ERR_FEATURE_UNSUPPORTED_WITH_AUDIO_TRACK:
            strcpy(sMessage,
                "M4VSS3GPP_ERR_FEATURE_UNSUPPORTED_WITH_AUDIO_TRACK");
            break;

        case M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED:
            strcpy(sMessage, "M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED");
            break;

        case M4VSS3GPP_ERR_INPUT_CLIP_IS_NOT_A_3GPP:
            strcpy(sMessage, "M4VSS3GPP_ERR_INPUT_CLIP_IS_NOT_A_3GPP");
            break;

        case M4VSS3GPP_ERR_BEGINLOOP_HIGHER_ENDLOOP:
            strcpy(sMessage, "M4VSS3GPP_ERR_BEGINLOOP_HIGHER_ENDLOOP");
            break;

        case M4VSS3GPP_ERR_H263_PROFILE_NOT_SUPPORTED:
            strcpy(sMessage, "M4VSS3GPP_ERR_H263_PROFILE_NOT_SUPPORTED");
            break;

        case M4VSS3GPP_ERR_NO_SUPPORTED_VIDEO_STREAM_IN_FILE:
            strcpy(sMessage, "M4VSS3GPP_ERR_NO_SUPPORTED_VIDEO_STREAM_IN_FILE");
            break;

        default: /**< Not a VSS3GPP error */
            strcpy(sMessage, "");
            return M4ERR_PARAMETER;
    }
    return M4NO_ERROR;
}

#endif /* WIN32 */

/********************************************************/
/********************************************************/
/********************************************************/
/****************   STATIC FUNCTIONS   ******************/
/********************************************************/
/********************************************************/
/********************************************************/

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipSettingsSanityCheck()
 * @brief    Simplify the given clip settings
 * @note    This function may modify the given structure
 * @param   pClip    (IN/OUT) Clip settings
 * @return    M4NO_ERROR:            No error
 * @return    M4VSS3GPP_ERR_EXTERNAL_EFFECT_NULL:
 ******************************************************************************
 */

static M4OSA_ERR M4VSS3GPP_intClipSettingsSanityCheck(
    M4VSS3GPP_ClipSettings *pClip )
{
    M4OSA_UInt8 uiFx;
    M4OSA_UInt32
        uiClipActualDuration; /**< the clip duration once the cuts are done */
    M4OSA_UInt32 uiDuration;
    M4VSS3GPP_EffectSettings *pFx;

    /**
    * If begin cut is too far, return an error */
    uiDuration = pClip->ClipProperties.uiClipDuration;

    if( pClip->uiBeginCutTime > uiDuration )
    {
        M4OSA_TRACE1_2(
            "M4VSS3GPP_intClipSettingsSanityCheck: %d > %d,\
            returning M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_DURATION",
            pClip->uiBeginCutTime, uiDuration);
        return M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_DURATION;
    }

    /**
    * If end cut is too far, set to zero (it means no end cut) */
    if( pClip->uiEndCutTime > uiDuration )
    {
        pClip->uiEndCutTime = 0;
    }

    /**
    * Compute actual clip duration (once cuts are done) */
    if( 0 == pClip->uiEndCutTime )
    {
        /**
        * No end cut */
        uiClipActualDuration = uiDuration - pClip->uiBeginCutTime;
    }
    else
    {
        if( pClip->uiBeginCutTime >= pClip->uiEndCutTime )
        {
            M4OSA_TRACE1_2(
                "M4VSS3GPP_intClipSettingsSanityCheck: %d > %d,\
                returning M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_END_CUT",
                pClip->uiBeginCutTime, pClip->uiEndCutTime);
            return M4VSS3GPP_ERR_BEGIN_CUT_LARGER_THAN_END_CUT;
        }
        uiClipActualDuration = pClip->uiEndCutTime - pClip->uiBeginCutTime;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intTransitionSettingsSanityCheck()
 * @brief    Simplify the given transition settings
 * @note     This function may modify the given structure
 * @param    pTransition    (IN/OUT) Transition settings
 * @return    M4NO_ERROR:            No error
 * @return    M4VSS3GPP_ERR_EXTERNAL_TRANSITION_NULL:
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intTransitionSettingsSanityCheck(
    M4VSS3GPP_TransitionSettings *pTransition )
{
    /**
    * No transition */
    if( 0 == pTransition->uiTransitionDuration )
    {
        pTransition->VideoTransitionType = M4VSS3GPP_kVideoTransitionType_None;
        pTransition->AudioTransitionType = M4VSS3GPP_kAudioTransitionType_None;
    }
    else if( ( M4VSS3GPP_kVideoTransitionType_None
        == pTransition->VideoTransitionType)
        && (M4VSS3GPP_kAudioTransitionType_None
        == pTransition->AudioTransitionType) )
    {
        pTransition->uiTransitionDuration = 0;
    }

    /**
    * Check external transition function is set */
    if( ( pTransition->VideoTransitionType
        >= M4VSS3GPP_kVideoTransitionType_External)
        && (M4OSA_NULL == pTransition->ExtVideoTransitionFct) )
    {
        return M4VSS3GPP_ERR_EXTERNAL_TRANSITION_NULL;
    }

    /**
    * Set minimal transition duration */
    if( ( pTransition->uiTransitionDuration > 0)
        && (pTransition->uiTransitionDuration
        < M4VSS3GPP_MINIMAL_TRANSITION_DURATION) )
    {
        pTransition->uiTransitionDuration =
            M4VSS3GPP_MINIMAL_TRANSITION_DURATION;
    }
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intFreeSettingsList()
 * @brief    Free the settings copied in the internal context
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_Void M4VSS3GPP_intFreeSettingsList(
    M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_UInt32 i;

    /**
    * Free the settings list */
    if( M4OSA_NULL != pC->pClipList )
    {
        for ( i = 0; i < pC->uiClipNumber; i++ )
        {
            M4VSS3GPP_editFreeClipSettings(&(pC->pClipList[i]));
        }

        free(pC->pClipList);
        pC->pClipList = M4OSA_NULL;
    }

    /**
    * Free the transition list */
    if( M4OSA_NULL != pC->pTransitionList )
    {
        free(pC->pTransitionList);
        pC->pTransitionList = M4OSA_NULL;
    }
}
/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreateMP3OutputFile()
 * @brief        Creates and prepare the output MP file
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_ERR
M4VSS3GPP_intCreateMP3OutputFile( M4VSS3GPP_InternalEditContext *pC,
                                 M4OSA_Void *pOutputFile )
{
    M4OSA_ERR err;

    err =
        pC->pOsaFileWritPtr->openWrite(&pC->ewc.p3gpWriterContext, pOutputFile,
        M4OSA_kFileWrite);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intCreateMP3OutputFile: WriteOpen returns 0x%x!", err);
        return err;
    }

    return M4NO_ERROR;
}
/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreate3GPPOutputFile()
 * @brief   Creates and prepare the output MP3 file
 * @note    Creates the writer, Creates the output file, Adds the streams,
           Readies the writing process
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR
M4VSS3GPP_intCreate3GPPOutputFile( M4VSS3GPP_EncodeWriteContext *pC_ewc,
                                  M4VSS3GPP_MediaAndCodecCtxt *pC_ShellAPI,
                                  M4OSA_FileWriterPointer *pOsaFileWritPtr,
                                  M4OSA_Void *pOutputFile,
                                  M4OSA_FileReadPointer *pOsaFileReadPtr,
                                  M4OSA_Void *pTempFile,
                                  M4OSA_UInt32 maxOutputFileSize )
{
    M4OSA_ERR err;
    M4OSA_UInt32 uiVersion;
    M4SYS_StreamIDValue temp;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_intCreate3GPPOutputFile called with pC_ewc=0x%x, pOutputFile=0x%x",
        pC_ewc, pOutputFile);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pC_ewc), M4ERR_PARAMETER,
        "M4VSS3GPP_intCreate3GPPOutputFile: pC_ewc is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pOutputFile), M4ERR_PARAMETER,
        "M4VSS3GPP_intCreate3GPPOutputFile: pOutputFile is M4OSA_NULL");

    /* Set writer */
    err =
        M4VSS3GPP_setCurrentWriter(pC_ShellAPI, M4VIDEOEDITING_kFileType_3GPP);
    M4ERR_CHECK_RETURN(err);

    /**
    * Create the output file */
    err = pC_ShellAPI->pWriterGlobalFcts->pFctOpen(&pC_ewc->p3gpWriterContext,
        pOutputFile, pOsaFileWritPtr, pTempFile, pOsaFileReadPtr);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intCreate3GPPOutputFile: pWriterGlobalFcts->pFctOpen returns 0x%x!",
            err);
        return err;
    }

    /**
    * Set the signature option of the writer */
    err =
        pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(pC_ewc->p3gpWriterContext,
        M4WRITER_kEmbeddedString, (M4OSA_DataOption)"NXP-SW : VSS    ");

    if( ( M4NO_ERROR != err) && (((M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
        != err) ) /* this option may not be implemented by some writers */
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intCreate3GPPOutputFile:\
            pWriterGlobalFcts->pFctSetOption(M4WRITER_kEmbeddedString) returns 0x%x!",
            err);
        return err;
    }

    /*11/12/2008 CR3283 MMS use case for VideoArtist:
    Set the max output file size option in the writer so that the output file will be
    smaller than the given file size limitation*/
    if( maxOutputFileSize > 0 )
    {
        err = pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(
            pC_ewc->p3gpWriterContext,
            M4WRITER_kMaxFileSize, &maxOutputFileSize);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                writer set option M4WRITER_kMaxFileSize returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Set the version option of the writer */
    uiVersion =
        (M4VIDEOEDITING_VERSION_MAJOR * 100 + M4VIDEOEDITING_VERSION_MINOR * 10
        + M4VIDEOEDITING_VERSION_REVISION);
    err =
        pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(pC_ewc->p3gpWriterContext,
        M4WRITER_kEmbeddedVersion, (M4OSA_DataOption) &uiVersion);

    if( ( M4NO_ERROR != err) && (((M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
        != err) ) /* this option may not be implemented by some writers */
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intCreate3GPPOutputFile:\
            pWriterGlobalFcts->pFctSetOption(M4WRITER_kEmbeddedVersion) returns 0x%x!",
            err);
        return err;
    }

    if( M4SYS_kVideoUnknown != pC_ewc->VideoStreamType )
    {
        /**
        * Set the video stream properties */
        pC_ewc->WriterVideoStreamInfo.height = pC_ewc->uiVideoHeight;
        pC_ewc->WriterVideoStreamInfo.width = pC_ewc->uiVideoWidth;
        pC_ewc->WriterVideoStreamInfo.fps =
            0.0; /**< Not used by the shell/core writer */
        pC_ewc->WriterVideoStreamInfo.Header.pBuf =
            pC_ewc->pVideoOutputDsi; /**< Previously computed output DSI */
        pC_ewc->WriterVideoStreamInfo.Header.Size = pC_ewc->
            uiVideoOutputDsiSize; /**< Previously computed output DSI size */

        pC_ewc->WriterVideoStream.streamType = pC_ewc->VideoStreamType;

        switch( pC_ewc->VideoStreamType )
        {
            case M4SYS_kMPEG_4:
            case M4SYS_kH263:
            case M4SYS_kH264:
                /**< We HAVE to put a value here... */
                pC_ewc->WriterVideoStream.averageBitrate =
                    pC_ewc->uiVideoBitrate;
                pC_ewc->WriterVideoStream.maxBitrate = pC_ewc->uiVideoBitrate;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intCreate3GPPOutputFile: unknown input video format (0x%x),\
                    returning M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT!",
                    pC_ewc->VideoStreamType);
                return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT;
        }

        pC_ewc->WriterVideoStream.streamID = M4VSS3GPP_WRITER_VIDEO_STREAM_ID;
        pC_ewc->WriterVideoStream.timeScale =
            0; /**< Not used by the shell/core writer */
        pC_ewc->WriterVideoStream.profileLevel =
            0; /**< Not used by the shell/core writer */
        pC_ewc->WriterVideoStream.duration =
            0; /**< Not used by the shell/core writer */

        pC_ewc->WriterVideoStream.decoderSpecificInfoSize =
            sizeof(M4WRITER_StreamVideoInfos);
        pC_ewc->WriterVideoStream.decoderSpecificInfo =
            (M4OSA_MemAddr32) &(pC_ewc->WriterVideoStreamInfo);

        /**
        * Add the video stream */
        err = pC_ShellAPI->pWriterGlobalFcts->pFctAddStream(
            pC_ewc->p3gpWriterContext, &pC_ewc->WriterVideoStream);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctAddStream(video) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Update AU properties for video stream */
        pC_ewc->WriterVideoAU.attribute = AU_RAP;
        pC_ewc->WriterVideoAU.CTS = 0;
        pC_ewc->WriterVideoAU.DTS = 0;    /** Reset time */
        pC_ewc->WriterVideoAU.frag = M4OSA_NULL;
        pC_ewc->WriterVideoAU.nbFrag = 0; /** No fragment */
        pC_ewc->WriterVideoAU.size = 0;
        pC_ewc->WriterVideoAU.dataAddress = M4OSA_NULL;
        pC_ewc->WriterVideoAU.stream = &(pC_ewc->WriterVideoStream);

        /**
        * Set the writer max video AU size */
        pC_ewc->uiVideoMaxAuSize = (M4OSA_UInt32)(1.5F
            *(M4OSA_Float)(pC_ewc->WriterVideoStreamInfo.width
            * pC_ewc->WriterVideoStreamInfo.height)
            * M4VSS3GPP_VIDEO_MIN_COMPRESSION_RATIO);
        temp.streamID = M4VSS3GPP_WRITER_VIDEO_STREAM_ID;
        temp.value = pC_ewc->uiVideoMaxAuSize;
        err = pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(
            pC_ewc->p3gpWriterContext, (M4OSA_UInt32)M4WRITER_kMaxAUSize,
            (M4OSA_DataOption) &temp);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, video) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Set the writer max video chunk size */
        temp.streamID = M4VSS3GPP_WRITER_VIDEO_STREAM_ID;
        temp.value = (M4OSA_UInt32)(pC_ewc->uiVideoMaxAuSize \
            * M4VSS3GPP_VIDEO_AU_SIZE_TO_CHUNCK_SIZE_RATIO); /**< from max AU size to
                                                                  max Chunck size */
        err = pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(
            pC_ewc->p3gpWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxChunckSize,
            (M4OSA_DataOption) &temp);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, video) returns 0x%x!",
                err);
            return err;
        }
    }

    if( M4SYS_kAudioUnknown != pC_ewc->AudioStreamType )
    {
        M4WRITER_StreamAudioInfos streamAudioInfo;

        streamAudioInfo.nbSamplesPerSec = 0; /**< unused by our shell writer */
        streamAudioInfo.nbBitsPerSample = 0; /**< unused by our shell writer */
        streamAudioInfo.nbChannels = 1;      /**< unused by our shell writer */

        if( pC_ewc->pAudioOutputDsi != M4OSA_NULL )
        {
            /* If we copy the stream from the input, we copy its DSI */
            streamAudioInfo.Header.Size = pC_ewc->uiAudioOutputDsiSize;
            streamAudioInfo.Header.pBuf = pC_ewc->pAudioOutputDsi;
        }
        else
        {
            /* Writer will put a default DSI */
            streamAudioInfo.Header.Size = 0;
            streamAudioInfo.Header.pBuf = M4OSA_NULL;
        }

        pC_ewc->WriterAudioStream.streamID = M4VSS3GPP_WRITER_AUDIO_STREAM_ID;
        pC_ewc->WriterAudioStream.streamType = pC_ewc->AudioStreamType;
        pC_ewc->WriterAudioStream.duration =
            0; /**< Not used by the shell/core writer */
        pC_ewc->WriterAudioStream.profileLevel =
            0; /**< Not used by the shell/core writer */
        pC_ewc->WriterAudioStreamInfo.nbSamplesPerSec =
            pC_ewc->uiSamplingFrequency;
        pC_ewc->WriterAudioStream.timeScale = pC_ewc->uiSamplingFrequency;
        pC_ewc->WriterAudioStreamInfo.nbChannels =
            (M4OSA_UInt16)pC_ewc->uiNbChannels;
        pC_ewc->WriterAudioStreamInfo.nbBitsPerSample =
            0; /**< Not used by the shell/core writer */

        /**
        * Add the audio stream */
        switch( pC_ewc->AudioStreamType )
        {
            case M4SYS_kAMR:
                pC_ewc->WriterAudioStream.averageBitrate =
                    0; /**< It is not used by the shell, the DSI is taken into account instead */
                pC_ewc->WriterAudioStream.maxBitrate =
                    0; /**< Not used by the shell/core writer */
                break;

            case M4SYS_kAAC:
                pC_ewc->WriterAudioStream.averageBitrate =
                    pC_ewc->uiAudioBitrate;
                pC_ewc->WriterAudioStream.maxBitrate = pC_ewc->uiAudioBitrate;
                break;

            case M4SYS_kEVRC:
                pC_ewc->WriterAudioStream.averageBitrate =
                    0; /**< It is not used by the shell, the DSI is taken into account instead */
                pC_ewc->WriterAudioStream.maxBitrate =
                    0; /**< Not used by the shell/core writer */
                break;

            case M4SYS_kMP3: /**< there can't be MP3 track in 3GPP file -> error */
            default:
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intCreate3GPPOutputFile: unknown output audio format (0x%x),\
                    returning M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT!",
                    pC_ewc->AudioStreamType);
                return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT;
        }

        /**
        * Our writer shell interface is a little tricky: we put M4WRITER_StreamAudioInfos
        in the DSI pointer... */
        pC_ewc->WriterAudioStream.decoderSpecificInfo =
            (M4OSA_MemAddr32) &streamAudioInfo;

        /**
        * Link the AU and the stream */
        pC_ewc->WriterAudioAU.stream = &(pC_ewc->WriterAudioStream);
        pC_ewc->WriterAudioAU.dataAddress = M4OSA_NULL;
        pC_ewc->WriterAudioAU.size = 0;
        pC_ewc->WriterAudioAU.CTS =
            -pC_ewc->iSilenceFrameDuration; /** Reset time */
        pC_ewc->WriterAudioAU.DTS = 0;
        pC_ewc->WriterAudioAU.attribute = 0;
        pC_ewc->WriterAudioAU.nbFrag = 0; /** No fragment */
        pC_ewc->WriterAudioAU.frag = M4OSA_NULL;

        err = pC_ShellAPI->pWriterGlobalFcts->pFctAddStream(
            pC_ewc->p3gpWriterContext, &pC_ewc->WriterAudioStream);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctAddStream(audio) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Set the writer max audio AU size */
        pC_ewc->uiAudioMaxAuSize = M4VSS3GPP_AUDIO_MAX_AU_SIZE;
        temp.streamID = M4VSS3GPP_WRITER_AUDIO_STREAM_ID;
        temp.value = pC_ewc->uiAudioMaxAuSize;
        err = pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(
            pC_ewc->p3gpWriterContext, (M4OSA_UInt32)M4WRITER_kMaxAUSize,
            (M4OSA_DataOption) &temp);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, audio) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Set the writer max audio chunck size */
        temp.streamID = M4VSS3GPP_WRITER_AUDIO_STREAM_ID;
        temp.value = M4VSS3GPP_AUDIO_MAX_CHUNCK_SIZE;
        err = pC_ShellAPI->pWriterGlobalFcts->pFctSetOption(
            pC_ewc->p3gpWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxChunckSize,
            (M4OSA_DataOption) &temp);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCreate3GPPOutputFile:\
                pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, audio) returns 0x%x!",
                err);
            return err;
        }
    }

    /**
    * All streams added, we're now ready to write */
    err = pC_ShellAPI->pWriterGlobalFcts->pFctStartWriting(
        pC_ewc->p3gpWriterContext);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intCreate3GPPOutputFile:\
            pWriterGlobalFcts->pFctStartWriting() returns 0x%x!",
            err);
        return err;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intCreate3GPPOutputFile(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR  M4VSS3GPP_intComputeOutputVideoAndAudioDsi()
 * @brief    Generate a H263 or MPEG-4 decoder specific info compatible with all input video
 *            tracks. Copy audio dsi from master clip.
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_ERR
M4VSS3GPP_intComputeOutputVideoAndAudioDsi( M4VSS3GPP_InternalEditContext *pC,
                                           M4OSA_UInt8 uiMasterClip )
{
    M4OSA_Int32 iResynchMarkerDsiIndex;
    M4_StreamHandler *pStreamForDsi;
    M4VSS3GPP_ClipContext *pClip;
    M4OSA_ERR err;
    M4OSA_UInt32 i;
    M4DECODER_MPEG4_DecoderConfigInfo DecConfigInfo;
    M4DECODER_VideoSize dummySize;
    M4OSA_Bool bGetDSiFromEncoder = M4OSA_FALSE;

    M4ENCODER_Header *encHeader;
    M4SYS_StreamIDmemAddr streamHeader;

    pStreamForDsi = M4OSA_NULL;
    pClip = M4OSA_NULL;

    /**
    * H263 case */
    if( M4SYS_kH263 == pC->ewc.VideoStreamType )
    {
        /**
        * H263 output DSI is always 7 bytes */
        pC->ewc.uiVideoOutputDsiSize = 7;
        pC->ewc.pVideoOutputDsi =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->ewc.uiVideoOutputDsiSize,
            M4VSS3GPP, (M4OSA_Char *)"pC->ewc.pVideoOutputDsi (H263)");

        if( M4OSA_NULL == pC->ewc.pVideoOutputDsi )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intComputeOutputVideoAndAudioDsi():\
                unable to allocate pVideoOutputDsi (H263), returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        /**
        * (We override the input vendor info.
        * At least we know that nothing special will be tried with PHLP-stamped
          edited streams...) */
        pC->ewc.pVideoOutputDsi[0] = 'P';
        pC->ewc.pVideoOutputDsi[1] = 'H';
        pC->ewc.pVideoOutputDsi[2] = 'L';
        pC->ewc.pVideoOutputDsi[3] = 'P';

        /**
        * Decoder version is 0 */
        pC->ewc.pVideoOutputDsi[4] = 0;

        /**
        * Level is the sixth byte in the DSI */
        pC->ewc.pVideoOutputDsi[5] = pC->xVSS.outputVideoLevel;

        /**
        * Profile is the seventh byte in the DSI*/
        pC->ewc.pVideoOutputDsi[6] = pC->xVSS.outputVideoProfile;
    }

    /**
    * MPEG-4 case */
    else if( M4SYS_kMPEG_4 == pC->ewc.VideoStreamType ||
        M4SYS_kH264 == pC->ewc.VideoStreamType) {

        /* For MPEG4 and H.264 encoder case
        * Fetch the DSI from the shell video encoder, and feed it to the writer before
        closing it. */

        M4OSA_TRACE1_0(
            "M4VSS3GPP_intComputeOutputVideoAndAudioDsi: get DSI for H264 stream");

        if( M4OSA_NULL == pC->ewc.pEncContext )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intComputeOutputVideoAndAudioDsi: pC->ewc.pEncContext is NULL");
            err = M4VSS3GPP_intCreateVideoEncoder(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    M4VSS3GPP_intCreateVideoEncoder returned error 0x%x",
                    err);
            }
        }

        if( M4OSA_NULL != pC->ewc.pEncContext )
        {
            err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctGetOption(
                pC->ewc.pEncContext, M4ENCODER_kOptionID_EncoderHeader,
                (M4OSA_DataOption) &encHeader);

            if( ( M4NO_ERROR != err) || (M4OSA_NULL == encHeader->pBuf) )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    failed to get the encoder header (err 0x%x)",
                    err);
                M4OSA_TRACE1_2(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi: encHeader->pBuf=0x%x, size=0x%x",
                    encHeader->pBuf, encHeader->Size);
            }
            else
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                     send DSI for video stream to 3GP writer");

                /**
                * Allocate and copy the new DSI */
                pC->ewc.pVideoOutputDsi =
                    (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(encHeader->Size, M4VSS3GPP,
                    (M4OSA_Char *)"pC->ewc.pVideoOutputDsi (H264)");

                if( M4OSA_NULL == pC->ewc.pVideoOutputDsi )
                {
                    M4OSA_TRACE1_0(
                        "M4VSS3GPP_intComputeOutputVideoAndAudioDsi():\
                         unable to allocate pVideoOutputDsi, returning M4ERR_ALLOC");
                    return M4ERR_ALLOC;
                }
                pC->ewc.uiVideoOutputDsiSize = (M4OSA_UInt16)encHeader->Size;
                memcpy((void *)pC->ewc.pVideoOutputDsi, (void *)encHeader->pBuf,
                    encHeader->Size);
            }

            err = M4VSS3GPP_intDestroyVideoEncoder(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    M4VSS3GPP_intDestroyVideoEncoder returned error 0x%x",
                    err);
            }
        }
        else
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                pC->ewc.pEncContext is NULL, cannot get the DSI");
        }
    }

    pStreamForDsi = M4OSA_NULL;
    pClip = M4OSA_NULL;

    /* Compute Audio DSI */
    if( M4SYS_kAudioUnknown != pC->ewc.AudioStreamType )
    {
        if( uiMasterClip == 0 )
        {
            /* Clip is already opened */
            pStreamForDsi = &(pC->pC1->pAudioStream->m_basicProperties);
        }
        else
        {
            /**
            * We can use the fast open mode to get the DSI */
            err = M4VSS3GPP_intClipInit(&pClip, pC->pOsaFileReadPtr);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    M4VSS3GPP_intClipInit() returns 0x%x!",
                    err);

                if( pClip != M4OSA_NULL )
                {
                    M4VSS3GPP_intClipCleanUp(pClip);
                }
                return err;
            }

            err = M4VSS3GPP_intClipOpen(pClip, &pC->pClipList[uiMasterClip],
                M4OSA_FALSE, M4OSA_TRUE, M4OSA_TRUE);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    M4VSS3GPP_intClipOpen() returns 0x%x!",
                    err);
                M4VSS3GPP_intClipCleanUp(pClip);
                return err;
            }

            pStreamForDsi = &(pClip->pAudioStream->m_basicProperties);
        }

        /**
        * Allocate and copy the new DSI */
        pC->ewc.pAudioOutputDsi = (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(
            pStreamForDsi->m_decoderSpecificInfoSize,
            M4VSS3GPP, (M4OSA_Char *)"pC->ewc.pAudioOutputDsi");

        if( M4OSA_NULL == pC->ewc.pAudioOutputDsi )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intComputeOutputVideoAndAudioDsi():\
                unable to allocate pAudioOutputDsi, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->ewc.uiAudioOutputDsiSize =
            (M4OSA_UInt16)pStreamForDsi->m_decoderSpecificInfoSize;
        memcpy((void *)pC->ewc.pAudioOutputDsi,
            (void *)pStreamForDsi->m_pDecoderSpecificInfo,
            pC->ewc.uiAudioOutputDsiSize);

        /**
        * If a clip has been temporarily opened to get its DSI, close it */
        if( M4OSA_NULL != pClip )
        {
            err = M4VSS3GPP_intClipCleanUp(pClip);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intComputeOutputVideoAndAudioDsi:\
                    M4VSS3GPP_intClipCleanUp() returns 0x%x!",
                    err);
                return err;
            }
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intComputeOutputVideoAndAudioDsi(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intSwitchToNextClip()
 * @brief    Switch from the current clip to the next one
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intSwitchToNextClip(
    M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;

    if( M4OSA_NULL != pC->pC1 )
    {
        if (M4OSA_NULL != pC->pC1->m_pPreResizeFrame) {
            if (M4OSA_NULL != pC->pC1->m_pPreResizeFrame[0].pac_data) {
                free(pC->pC1->m_pPreResizeFrame[0].pac_data);
                pC->pC1->m_pPreResizeFrame[0].pac_data = M4OSA_NULL;
            }
            if (M4OSA_NULL != pC->pC1->m_pPreResizeFrame[1].pac_data) {
                free(pC->pC1->m_pPreResizeFrame[1].pac_data);
                pC->pC1->m_pPreResizeFrame[1].pac_data = M4OSA_NULL;
            }
            if (M4OSA_NULL != pC->pC1->m_pPreResizeFrame[2].pac_data) {
                free(pC->pC1->m_pPreResizeFrame[2].pac_data);
                pC->pC1->m_pPreResizeFrame[2].pac_data = M4OSA_NULL;
            }
            free(pC->pC1->m_pPreResizeFrame);
            pC->pC1->m_pPreResizeFrame = M4OSA_NULL;
        }
        /**
        * Close the current first clip */
        err = M4VSS3GPP_intClipCleanUp(pC->pC1);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intSwitchToNextClip: M4VSS3GPP_intClipCleanUp(C1) returns 0x%x!",
                err);
            return err;
        }

        /**
        *  increment clip counter */
        pC->uiCurrentClip++;
    }

    /**
    * Check if we reached the last clip */
    if( pC->uiCurrentClip >= pC->uiClipNumber )
    {
        pC->pC1 = M4OSA_NULL;
        pC->State = M4VSS3GPP_kEditState_FINISHED;

        M4OSA_TRACE1_0(
            "M4VSS3GPP_intSwitchToNextClip:\
            M4VSS3GPP_intClipClose(C1) returns M4VSS3GPP_WAR_EDITING_DONE");
        return M4VSS3GPP_WAR_EDITING_DONE;
    }

    /**
    * If the next clip has already be opened, set it as first clip */
    if( M4OSA_NULL != pC->pC2 )
    {
        pC->pC1 = pC->pC2;
        if(M4OSA_NULL != pC->pC2->m_pPreResizeFrame) {
            pC->pC1->m_pPreResizeFrame = pC->pC2->m_pPreResizeFrame;
        }
        pC->pC2 = M4OSA_NULL;
        pC->bClip1ActiveFramingEffect = pC->bClip2ActiveFramingEffect;
        pC->bClip2ActiveFramingEffect = M4OSA_FALSE;
    }
    /**
    * else open it */
    else
    {
        err = M4VSS3GPP_intOpenClip(pC, &pC->pC1,
            &pC->pClipList[pC->uiCurrentClip]);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intSwitchToNextClip: M4VSS3GPP_intOpenClip() returns 0x%x!",
                err);
            return err;
        }

        /**
        * If the second clip has not been opened yet,
          that means that there has been no transition.
        * So both output video and audio times are OK.
        * So we can set both video2 and audio offsets */

        /**
        * Add current video output CTS to the clip video offset */

        // Decorrelate input and output encoding timestamp to handle encoder prefetch
        pC->pC1->iVoffset += (M4OSA_UInt32)pC->ewc.dInputVidCts;
        /**
        * Add current audio output CTS to the clip audio offset */
        pC->pC1->iAoffset +=
            (M4OSA_UInt32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

        /**
        * 2005-03-24: BugFix for audio-video synchro:
        * There may be a portion of the duration of an audio AU of desynchro at each assembly.
        * It leads to an audible desynchro when there are a lot of clips assembled.
        * This bug fix allows to resynch the audio track when the delta is higher
        * than one audio AU duration.
        * We Step one AU in the second clip and we change the audio offset accordingly. */
        if( ( pC->pC1->iAoffset
            - (M4OSA_Int32)(pC->pC1->iVoffset *pC->pC1->scale_audio + 0.5))
        > pC->ewc.iSilenceFrameDuration )
        {
            /**
            * Advance one AMR frame */
            err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC1);

            if( M4OSA_ERR_IS_ERROR(err) )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intSwitchToNextClip:\
                    M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",
                    err);
                return err;
            }
            /**
            * Update audio offset accordingly*/
            pC->pC1->iAoffset -= pC->ewc.iSilenceFrameDuration;
        }
    }

    /**
    * Init starting state for this clip processing */
    if( M4SYS_kMP3 == pC->ewc.AudioStreamType )
    {
        /**
        * In the MP3 case we use a special audio state */
        pC->State = M4VSS3GPP_kEditState_MP3_JUMP;
    }
    else
    {
        /**
        * We start with the video processing */
        pC->State = M4VSS3GPP_kEditState_VIDEO;

        if( pC->Vstate != M4VSS3GPP_kEditVideoState_TRANSITION )
        {
            /* if not a transition then reset previous video state */
            pC->Vstate = M4VSS3GPP_kEditVideoState_READ_WRITE;
        }
    }
    /* The flags are set to false at the beginning of every clip */
    pC->m_bClipExternalHasStarted = M4OSA_FALSE;
    pC->bEncodeTillEoF = M4OSA_FALSE;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intSwitchToNextClip(): returning M4NO_ERROR");
    /* RC: to know when a file has been processed */
    return M4VSS3GPP_WAR_SWITCH_CLIP;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intReachedEndOfVideo()
 * @brief    Do what to do when the end of a clip video track is reached
 * @note    If there is audio on the current clip, process it, else switch to the next clip
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intReachedEndOfVideo( M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;

    /**
    * Video is done for this clip, now we do the audio */
    if( M4SYS_kAudioUnknown != pC->ewc.AudioStreamType )
    {
        pC->State = M4VSS3GPP_kEditState_AUDIO;
    }
    else
    {
        /**
        * Clip done, do the next one */
        err = M4VSS3GPP_intSwitchToNextClip(pC);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intReachedEndOfVideo: M4VSS3GPP_intSwitchToNextClip() returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intReachedEndOfVideo(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intReachedEndOfAudio()
 * @brief    Do what to do when the end of a clip audio track is reached
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intReachedEndOfAudio( M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;

    /**
    * Clip done, do the next one */
    err = M4VSS3GPP_intSwitchToNextClip(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intReachedEndOfAudio: M4VSS3GPP_intSwitchToNextClip() returns 0x%x",
            err);
        return err;
    }

    /**
    * Start with the video */
    if( M4SYS_kVideoUnknown != pC->ewc.VideoStreamType )
    {
        pC->State = M4VSS3GPP_kEditState_VIDEO;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intReachedEndOfAudio(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intOpenClip()
 * @brief    Open next clip
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intOpenClip( M4VSS3GPP_InternalEditContext *pC,
                                M4VSS3GPP_ClipContext ** hClip,
                                M4VSS3GPP_ClipSettings *pClipSettings )
{
    M4OSA_ERR err;
    M4VSS3GPP_ClipContext *pClip; /**< shortcut */
    M4VIDEOEDITING_ClipProperties *pClipProperties = M4OSA_NULL;
    M4OSA_Int32 iCts;
    M4OSA_UInt32 i;

    M4OSA_TRACE2_1("M4VSS3GPP_intOpenClip: \"%s\"",
        (M4OSA_Char *)pClipSettings->pFile);

    err = M4VSS3GPP_intClipInit(hClip, pC->pOsaFileReadPtr);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intOpenClip: M4VSS3GPP_intClipInit() returns 0x%x!",
            err);

        if( *hClip != M4OSA_NULL )
        {
            M4VSS3GPP_intClipCleanUp(*hClip);
        }
        return err;
    }

    /**
    * Set shortcut */
    pClip = *hClip;

    if (pClipSettings->FileType == M4VIDEOEDITING_kFileType_ARGB8888 ) {
        pClipProperties = &pClipSettings->ClipProperties;
        pClip->pSettings = pClipSettings;
        pClip->iEndTime = pClipSettings->uiEndCutTime;
    }

    err = M4VSS3GPP_intClipOpen(pClip, pClipSettings,
              M4OSA_FALSE, M4OSA_FALSE, M4OSA_FALSE);
    if (M4NO_ERROR != err) {
        M4OSA_TRACE1_1("M4VSS3GPP_intOpenClip: \
            M4VSS3GPP_intClipOpen() returns 0x%x!", err);
        M4VSS3GPP_intClipCleanUp(pClip);
        *hClip = M4OSA_NULL;
        return err;
    }

    if (pClipSettings->FileType != M4VIDEOEDITING_kFileType_ARGB8888 ) {
        pClipProperties = &pClip->pSettings->ClipProperties;
    }

    /**
    * Copy common 'silence frame stuff' to ClipContext */
    pClip->uiSilencePcmSize = pC->ewc.uiSilencePcmSize;
    pClip->pSilenceFrameData = pC->ewc.pSilenceFrameData;
    pClip->uiSilenceFrameSize = pC->ewc.uiSilenceFrameSize;
    pClip->iSilenceFrameDuration = pC->ewc.iSilenceFrameDuration;
    pClip->scale_audio = pC->ewc.scale_audio;

    pClip->iAudioFrameCts = -pClip->iSilenceFrameDuration; /* Reset time */

    /**
    * If the audio track is not compatible with the output audio format,
    * we remove it. So it will be replaced by silence */
    if( M4OSA_FALSE == pClipProperties->bAudioIsCompatibleWithMasterClip )
    {
        M4VSS3GPP_intClipDeleteAudioTrack(pClip);
    }

    /**
    * Actual begin cut */
    if( 0 == pClipSettings->uiBeginCutTime )
    {
        pClip->iVoffset = 0;
        pClip->iAoffset = 0;
        pClip->iActualVideoBeginCut = 0;
        pClip->iActualAudioBeginCut = 0;
    }
    else if(pClipSettings->FileType != M4VIDEOEDITING_kFileType_ARGB8888) {
        if( M4SYS_kVideoUnknown != pC->ewc.VideoStreamType )
        {
            /**
            * Jump the video to the target begin cut to get the actual begin cut value */
            pClip->iActualVideoBeginCut =
                (M4OSA_Int32)pClipSettings->uiBeginCutTime;
            iCts = pClip->iActualVideoBeginCut;

            err = pClip->ShellAPI.m_pReader->m_pFctJump(pClip->pReaderContext,
                (M4_StreamHandler *)pClip->pVideoStream, &iCts);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intOpenClip: m_pFctJump(V) returns 0x%x!", err);
                return err;
            }

            /**
            * Update clip offset with the video begin cut */
            pClip->iVoffset = -pClip->iActualVideoBeginCut;
        }

        if( M4SYS_kAudioUnknown != pC->ewc.AudioStreamType )
        {
            /**
            * Jump the audio to the video actual begin cut */
            if( M4VIDEOEDITING_kMP3 != pClipProperties->AudioStreamType )
            {
                pClip->iActualAudioBeginCut = pClip->iActualVideoBeginCut;
                iCts = (M4OSA_Int32)(pClip->iActualAudioBeginCut
                    * pClip->scale_audio + 0.5);

                err = M4VSS3GPP_intClipJumpAudioAt(pClip, &iCts);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intOpenClip: M4VSS3GPP_intClipJumpAudioAt(A) returns 0x%x!",
                        err);
                    return err;
                }
                /**
                * Update clip offset with the audio begin cut */
                pClip->iAoffset = -iCts;
            }
            else
            {
                /**
                * For the MP3, the jump is not done because of the VBR,
                  it could be not enough accurate */
                pClip->iActualAudioBeginCut =
                    (M4OSA_Int32)pClipSettings->uiBeginCutTime;
            }
        }
    }

    if( M4SYS_kVideoUnknown != pC->ewc.VideoStreamType )
    {
        if ((pClipSettings->FileType != M4VIDEOEDITING_kFileType_ARGB8888 )) {

            /**
            * Read the first Video AU of the clip */
            err = pClip->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
                pClip->pReaderContext,
                (M4_StreamHandler *)pClip->pVideoStream, &pClip->VideoAU);

            if( M4WAR_NO_MORE_AU == err )
            {
                /**
                * If we (already!) reach the end of the clip, we filter the error.
                * It will be correctly managed at the first step. */
                err = M4NO_ERROR;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1("M4VSS3GPP_intOpenClip: \
                    m_pReaderDataIt->m_pFctGetNextAu() returns 0x%x!", err);
                return err;
            }
        } else {
            pClipProperties->uiVideoWidth  = pClipProperties->uiStillPicWidth;
            pClipProperties->uiVideoHeight = pClipProperties->uiStillPicHeight;
        }
        /* state check not to allocate buffer during save start */


        /******************************/
        /* Video resize management   */
        /******************************/
        /**
        * If the input clip is a rotate video or the output resolution is different
        * from the input resolution, then the video frame needs to be rotated
        * or resized, force to resize mode */
        if (((M4OSA_UInt32)pC->ewc.uiVideoWidth !=
                 pClipProperties->uiVideoWidth) ||
            ((M4OSA_UInt32)pC->ewc.uiVideoHeight !=
                 pClipProperties->uiVideoHeight) ||
            pClipProperties->videoRotationDegrees != 0) {

            if (pClip->m_pPreResizeFrame == M4OSA_NULL) {
                /**
                * Allocate the intermediate video plane that will
                  receive the decoded image before resizing */
                pClip->m_pPreResizeFrame =
                 (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(
                     3*sizeof(M4VIFI_ImagePlane), M4VSS3GPP,
                     (M4OSA_Char *)"pPreResizeFrame");
                if (M4OSA_NULL == pClip->m_pPreResizeFrame) {
                    M4OSA_TRACE1_0("M4MCS_intPrepareVideoEncoder(): \
                        unable to allocate m_pPreResizeFrame");
                    return M4ERR_ALLOC;
                }

                pClip->m_pPreResizeFrame[0].pac_data = M4OSA_NULL;
                pClip->m_pPreResizeFrame[1].pac_data = M4OSA_NULL;
                pClip->m_pPreResizeFrame[2].pac_data = M4OSA_NULL;

                /**
                * Allocate the Y plane */
                pClip->m_pPreResizeFrame[0].u_topleft = 0;
                pClip->m_pPreResizeFrame[0].u_width  =
                    pClipProperties->uiVideoWidth;
                pClip->m_pPreResizeFrame[0].u_height =
                    pClipProperties->uiVideoHeight;
                pClip->m_pPreResizeFrame[0].u_stride =
                    pClip->m_pPreResizeFrame[0].u_width;

                pClip->m_pPreResizeFrame[0].pac_data =
                 (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc (
                   pClip->m_pPreResizeFrame[0].u_stride * pClip->m_pPreResizeFrame[0].u_height,
                   M4MCS, (M4OSA_Char *)"m_pPreResizeFrame[0].pac_data");
                if (M4OSA_NULL == pClip->m_pPreResizeFrame[0].pac_data) {
                    M4OSA_TRACE1_0("M4MCS_intPrepareVideoEncoder(): \
                        unable to allocate m_pPreResizeFrame[0].pac_data");
                    free(pClip->m_pPreResizeFrame);
                    return M4ERR_ALLOC;
                }

                /**
                * Allocate the U plane */
                pClip->m_pPreResizeFrame[1].u_topleft = 0;
                pClip->m_pPreResizeFrame[1].u_width  =
                    pClip->m_pPreResizeFrame[0].u_width >> 1;
                pClip->m_pPreResizeFrame[1].u_height =
                    pClip->m_pPreResizeFrame[0].u_height >> 1;
                pClip->m_pPreResizeFrame[1].u_stride =
                    pClip->m_pPreResizeFrame[1].u_width;

                pClip->m_pPreResizeFrame[1].pac_data =
                 (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc (
                   pClip->m_pPreResizeFrame[1].u_stride * pClip->m_pPreResizeFrame[1].u_height,
                   M4MCS, (M4OSA_Char *)"m_pPreResizeFrame[1].pac_data");
                if (M4OSA_NULL == pClip->m_pPreResizeFrame[1].pac_data) {
                    M4OSA_TRACE1_0("M4MCS_intPrepareVideoEncoder(): \
                        unable to allocate m_pPreResizeFrame[1].pac_data");
                    free(pClip->m_pPreResizeFrame[0].pac_data);
                    free(pClip->m_pPreResizeFrame);
                    return M4ERR_ALLOC;
                }

                /**
                * Allocate the V plane */
                pClip->m_pPreResizeFrame[2].u_topleft = 0;
                pClip->m_pPreResizeFrame[2].u_width =
                    pClip->m_pPreResizeFrame[1].u_width;
                pClip->m_pPreResizeFrame[2].u_height =
                    pClip->m_pPreResizeFrame[1].u_height;
                pClip->m_pPreResizeFrame[2].u_stride =
                    pClip->m_pPreResizeFrame[2].u_width;

                pClip->m_pPreResizeFrame[2].pac_data =
                 (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc (
                   pClip->m_pPreResizeFrame[2].u_stride * pClip->m_pPreResizeFrame[2].u_height,
                   M4MCS, (M4OSA_Char *)"m_pPreResizeFrame[2].pac_data");
                if (M4OSA_NULL == pClip->m_pPreResizeFrame[2].pac_data) {
                    M4OSA_TRACE1_0("M4MCS_intPrepareVideoEncoder(): \
                        unable to allocate m_pPreResizeFrame[2].pac_data");
                    free(pClip->m_pPreResizeFrame[0].pac_data);
                    free(pClip->m_pPreResizeFrame[1].pac_data);
                    free(pClip->m_pPreResizeFrame);
                    return M4ERR_ALLOC;
                }
            }
        }

        /**
        * The video is currently in reading mode */
        pClip->Vstatus = M4VSS3GPP_kClipStatus_READ;
    }

    if( ( M4SYS_kAudioUnknown != pC->ewc.AudioStreamType)
        && (M4VIDEOEDITING_kMP3 != pClipProperties->AudioStreamType) )
    {
        /**
        * Read the first Audio AU of the clip */
        err = M4VSS3GPP_intClipReadNextAudioFrame(pClip);

        if( M4OSA_ERR_IS_ERROR(err) )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intOpenClip: M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",
                err);
            return err;
        }

        /**
        * The audio is currently in reading mode */
        pClip->Astatus = M4VSS3GPP_kClipStatus_READ;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intOpenClip(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR  M4VSS3GPP_intComputeOutputAverageVideoBitrate()
 * @brief    Average bitrate of the output file, computed from input bitrates,
 *          durations, transitions and cuts.
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_Void M4VSS3GPP_intComputeOutputAverageVideoBitrate(
    M4VSS3GPP_InternalEditContext *pC )
{
    M4VSS3GPP_ClipSettings *pCS_0, *pCS_1, *pCS_2;
    M4VSS3GPP_TransitionSettings *pT0, *pT2;
    M4OSA_Int32 i;

    M4OSA_UInt32 t0_duration, t2_duration;
    M4OSA_UInt32 t0_bitrate, t2_bitrate;
    M4OSA_UInt32 c1_duration;

    M4OSA_UInt32 total_duration;
    M4OSA_UInt32 total_bitsum;

    total_duration = 0;
    total_bitsum = 0;

    /* Loop on the number of clips */
    for ( i = 0; i < pC->uiClipNumber; i++ )
    {
        pCS_1 = &pC->pClipList[i];

        t0_duration = 0;
        t0_bitrate = pCS_1->ClipProperties.uiVideoBitrate;
        t2_duration = 0;
        t2_bitrate = pCS_1->ClipProperties.uiVideoBitrate;

        /* Transition with the previous clip */
        if( i > 0 )
        {
            pCS_0 = &pC->pClipList[i - 1];
            pT0 = &pC->pTransitionList[i - 1];

            if( pT0->VideoTransitionType
                != M4VSS3GPP_kVideoTransitionType_None )
            {
                t0_duration = pT0->uiTransitionDuration;

                if( pCS_0->ClipProperties.uiVideoBitrate > t0_bitrate )
                {
                    t0_bitrate = pCS_0->ClipProperties.uiVideoBitrate;
                }
            }
        }

        /* Transition with the next clip */
        if( i < pC->uiClipNumber - 1 )
        {
            pCS_2 = &pC->pClipList[i + 1];
            pT2 = &pC->pTransitionList[i];

            if( pT2->VideoTransitionType
                != M4VSS3GPP_kVideoTransitionType_None )
            {
                t2_duration = pT2->uiTransitionDuration;

                if( pCS_2->ClipProperties.uiVideoBitrate > t2_bitrate )
                {
                    t2_bitrate = pCS_2->ClipProperties.uiVideoBitrate;
                }
            }
        }

        /* Check for cut times */
        if( pCS_1->uiEndCutTime > 0 )
            c1_duration = pCS_1->uiEndCutTime;
        else
            c1_duration = pCS_1->ClipProperties.uiClipVideoDuration;

        if( pCS_1->uiBeginCutTime > 0 )
            c1_duration -= pCS_1->uiBeginCutTime;

        c1_duration -= t0_duration + t2_duration;

        /* Compute bitsum and duration */
        total_duration += c1_duration + t0_duration / 2 + t2_duration / 2;

        total_bitsum +=
            c1_duration * (pCS_1->ClipProperties.uiVideoBitrate / 1000)
            + (t0_bitrate / 1000) * t0_duration / 2
            + (t2_bitrate / 1000) * t2_duration / 2;
    }

    pC->ewc.uiVideoBitrate = ( total_bitsum / total_duration) * 1000;
}

