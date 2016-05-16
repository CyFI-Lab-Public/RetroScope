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
 * @file   M4MCS_API.c
 * @brief  MCS implementation (Video Compressor Service)
 * @note   This file implements the API and the processing of the MCS
 *************************************************************************
 **/

/****************/
/*** Includes ***/
/****************/

/**
 * OSAL headers */
#include "M4OSA_Memory.h"   /**< OSAL memory management */
#include "M4OSA_Debug.h"    /**< OSAL debug management */

/* Our headers */
#include "M4MCS_API.h"
#include "M4MCS_ErrorCodes.h"
#include "M4MCS_InternalTypes.h"
#include "M4MCS_InternalConfig.h"
#include "M4MCS_InternalFunctions.h"

/* Common headers (for aac) */
#include "M4_Common.h"

#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
#include "M4VD_EXTERNAL_Interface.h"
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */



/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intCheckAudioEffects(M4MCS_InternalContext* pContext)
 * @brief    Check if an effect has to be applied currently
 * @note    It is called by the stepEncoding function
 * @param    pContext    (IN) MCS internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_intCheckAudioEffects(M4MCS_InternalContext* pC)
{
    M4OSA_Int8 *pActiveEffectNumber = &(pC->pActiveEffectNumber);

    *pActiveEffectNumber = -1;

    if(pC->ReaderAudioAU.m_CTS > pC->uiBeginCutTime
    && pC->ReaderAudioAU.m_CTS < pC->uiEndCutTime)
    {
        M4OSA_UInt32 outputRelatedTime = 0;
        M4OSA_UInt8 uiEffectIndex = 0;
        outputRelatedTime =
        (M4OSA_UInt32)(pC->ReaderAudioAU.m_CTS  - pC->uiBeginCutTime + 0.5);

        for(uiEffectIndex=0; uiEffectIndex<pC->nbEffects; uiEffectIndex++)
        {
            if ((outputRelatedTime >=
                (M4OSA_UInt32)(pC->pEffects[uiEffectIndex].uiStartTime)) &&
                (outputRelatedTime <
                (M4OSA_UInt32)(pC->pEffects[uiEffectIndex].uiStartTime +\
                pC->pEffects[uiEffectIndex].uiDuration)))
            {
                *pActiveEffectNumber = uiEffectIndex;
                uiEffectIndex = pC->nbEffects;
            }
        }
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_editAudioEffectFct_FadeIn()
 * @brief    Apply audio effect FadeIn to pPCMdata
 * @param   pC            (IN/OUT) Internal edit context
 * @param    pPCMdata    (IN/OUT) Input and Output PCM audio data
 * @param    uiPCMsize    (IN)     Size of pPCMdata
 * @param    pProgress    (IN)     Effect progress
 * @return    M4NO_ERROR:             No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_editAudioEffectFct_FadeIn(  M4OSA_Void *pFunctionContext,
                                            M4OSA_Int16 *pPCMdata,
                                            M4OSA_UInt32 uiPCMsize,
                                            M4MCS_ExternalProgress *pProgress)
{
    /* we will cast each Int16 sample into this Int32 variable */
    M4OSA_Int32 i32sample;

    /**
     * Sanity check */
    if(pProgress->uiProgress > 1000)
    {
        pProgress->uiProgress = 1000;
    }

    /**
     * From buffer size (bytes) to number of sample (int16): divide by two */
    uiPCMsize >>= 1;

    /**
     * Loop on samples */
    while (uiPCMsize-->0) /**< decrementing to optimize */
    {
        i32sample = *pPCMdata;
        i32sample *= pProgress->uiProgress;
        i32sample /= 1000;
        *pPCMdata++ = (M4OSA_Int16)i32sample;
    }

    /**
     *    Return */
    M4OSA_TRACE3_0("M4MCS_editAudioEffectFct_FadeIn: returning M4NO_ERROR");
    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_editAudioEffectFct_FadeOut()
 * @brief    Apply audio effect FadeIn to pPCMdata
 * @param    pC            (IN/OUT) Internal edit context
 * @param    pPCMdata    (IN/OUT) Input and Output PCM audio data
 * @param    uiPCMsize    (IN)     Size of pPCMdata
 * @param    pProgress    (IN)     Effect progress
 * @return   M4NO_ERROR:             No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_editAudioEffectFct_FadeOut( M4OSA_Void *pFunctionContext,
                                            M4OSA_Int16 *pPCMdata,
                                            M4OSA_UInt32 uiPCMsize,
                                            M4MCS_ExternalProgress *pProgress)
{
    /* we will cast each Int16 sample into this Int32 variable */
    M4OSA_Int32 i32sample;

    /**
     * Sanity check */
    if(pProgress->uiProgress > 1000)
    {
        pProgress->uiProgress = 1000;
    }
    pProgress->uiProgress = 1000 - pProgress->uiProgress;

    /**
     * From buffer size (bytes) to number of sample (int16): divide by two */
    uiPCMsize >>= 1;

    /**
     * Loop on samples */
    while (uiPCMsize-->0) /**< decrementing to optimize */
    {
        i32sample = *pPCMdata;
        i32sample *= pProgress->uiProgress;
        i32sample /= 1000;
        *pPCMdata++ = (M4OSA_Int16)i32sample;
    }

    /**
     *    Return */
    M4OSA_TRACE3_0("M4MCS_editAudioEffectFct_FadeOut: returning M4NO_ERROR");
    return M4NO_ERROR;
}

