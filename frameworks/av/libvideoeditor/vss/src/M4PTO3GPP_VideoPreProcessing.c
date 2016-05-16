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
 * @file    M4PTO3GPP_VideoPreProcessing.c
 * @brief   Picture to 3gpp Service video preprocessing management.
 ******************************************************************************
 */

/**
 *    OSAL Debug utilities */
#include "M4OSA_Debug.h"

/**
 *    OSAL Memory management */
#include "M4OSA_Memory.h"

/**
 *    Definition of the M4PTO3GPP internal context */
#include "M4PTO3GPP_InternalTypes.h"

/**
 *    Definition of the M4PTO3GPP errors */
#include "M4PTO3GPP_ErrorCodes.h"

/* If time increment is too low then we have an infinite alloc loop into M4ViEncCaptureFrame() */
/* Time increment should match 30 fps maximum */
#define M4PTO3GPP_MIN_TIME_INCREMENT 33.3333334


/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_applyVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
 *                                 M4VIFI_ImagePlane* pPlaneOut)
 * @brief    Call an external callback to get the picture to encode
 * @note    It is called by the video encoder
 * @param    pContext    (IN) VPP context, which actually is the M4PTO3GPP internal context
 *                            in our case
 * @param    pPlaneIn    (IN) Contains the image
 * @param    pPlaneOut    (IN/OUT) Pointer to an array of 3 planes that will contain the
 *                        output YUV420 image read with the m_pPictureCallbackFct
 * @return    M4NO_ERROR:    No error
 * @return    Any error returned by an underlaying module
 ******************************************************************************
 */
/******************************************************/
M4OSA_ERR M4PTO3GPP_applyVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
                             M4VIFI_ImagePlane* pPlaneOut)
/******************************************************/
{
    M4OSA_ERR    err;
    M4OSA_Double mtDuration;
    M4OSA_UInt32 i;

    /*** NOTE ***/
    /* It's OK to get pPlaneIn == M4OSA_NULL here                        */
    /* since it has been given NULL in the pFctEncode() call.            */
    /* It's because we use the M4PTO3GPP internal context to            */
    /* transmit the encoder input data.                                    */
    /* The input data is the image read from the m_pPictureCallbackFct    */

    /**
     *    The VPP context is actually the M4PTO3GPP context! */
    M4PTO3GPP_InternalContext *pC = (M4PTO3GPP_InternalContext*)(pContext);

    /**
    *  Get the picture to encode */
    if (M4OSA_FALSE == pC->m_bLastInternalCallBack)
    {
        err = pC->m_Params.pPictureCallbackFct(pC->m_Params.pPictureCallbackCtxt, pPlaneOut,
             &mtDuration);

        /* In case of error when getting YUV to encode (ex: error when decoding a JPEG) */
        if((M4NO_ERROR != err) && (((M4OSA_UInt32)M4PTO3GPP_WAR_LAST_PICTURE) != err))
        {
            return err;
        }

        /**
         * If end of encoding is asked by the size limitation system,
         * we must end the encoding the same way that when it is asked by the
         * picture callback (a.k.a. the integrator).
         * Thus we simulate the LastPicture code return: */
        if (M4OSA_TRUE == pC->m_IsLastPicture)
        {
            err = M4PTO3GPP_WAR_LAST_PICTURE;
        }

        if(((M4OSA_UInt32)M4PTO3GPP_WAR_LAST_PICTURE) == err)
        {
            pC->m_bLastInternalCallBack = M4OSA_TRUE; /* Toggle flag for the final call of the CB*/
            pC->m_IsLastPicture         = M4OSA_TRUE; /* To stop the encoder */
            pC->pSavedPlane             = pPlaneOut;  /* Save the last YUV plane ptr */
            pC->uiSavedDuration         = (M4OSA_UInt32)mtDuration; /* Save the last duration */
        }
    }
    else
    {
        /**< Not necessary here because the last frame duration is set to the-last-but-one by
                the light writer */
        /**< Only necessary for pC->m_mtNextCts below...*/
        mtDuration = pC->uiSavedDuration;


        /** Copy the last YUV plane into the current one
         * (the last pic is splited due to the callback extra-call... */
        for (i=0; i<3; i++)
        {
            memcpy((void *)pPlaneOut[i].pac_data,
                 (void *)pC->pSavedPlane[i].pac_data,
                     pPlaneOut[i].u_stride * pPlaneOut[i].u_height);
        }
    }

    /* TimeIncrement should be 30 fps maximum */
    if(mtDuration < M4PTO3GPP_MIN_TIME_INCREMENT)
    {
        mtDuration = M4PTO3GPP_MIN_TIME_INCREMENT;
    }

    pC->m_mtNextCts += mtDuration;

    M4OSA_TRACE3_0("M4PTO3GPP_applyVPP: returning M4NO_ERROR");
    return M4NO_ERROR;
}

