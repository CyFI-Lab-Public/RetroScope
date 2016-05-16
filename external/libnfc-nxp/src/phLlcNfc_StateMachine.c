/*
 * Copyright (C) 2010 NXP Semiconductors
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

/*!
* \file  phLlcNfc_StateMachine.c
* \brief Llc state machine implemenatation.
*
* Project: NFC-FRI-1.1
*
* $Date: Fri Apr 17 09:17:48 2009 $
* $Author: ing02260 $
* $Revision: 1.8 $
* $Aliases: NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*************************** Includes *******************************/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phOsalNfc.h>
#include <phNfcInterface.h>
#include <phLlcNfc_DataTypes.h>
#include <phLlcNfc_Frame.h>

/*********************** End of includes ****************************/

/***************************** Macros *******************************/

/************************ End of macros *****************************/

/*********************** Local functions ****************************/

/******************** End of Local functions ************************/

/********************** Global variables ****************************/

/******************** End of Global Variables ***********************/

NFCSTATUS 
phLlcNfc_H_ChangeState(
    phLlcNfc_Context_t  *psLlcCtxt, 
    phLlcNfc_State_t    changeStateTo
)
{
    NFCSTATUS   result = PHNFCSTVAL(CID_NFC_LLC, 
                                    NFCSTATUS_INVALID_STATE);
    if ((NULL != psLlcCtxt) && 
        (changeStateTo != phLlcNfc_Uninitialise_State))
    {
        switch(psLlcCtxt->state)
        {
            case phLlcNfc_Uninitialise_State:
            {
                if (phLlcNfc_Initialising_State == changeStateTo)
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_Initialising_State:
            {
                if (phLlcNfc_Initialised_State == changeStateTo)
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_Initialised_State:
            {
                if (changeStateTo > phLlcNfc_Initialising_State)
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_ReceiveWait_State:
            {
                if (changeStateTo >= phLlcNfc_Initialised_State)
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_Sending_State:
            {
                if ((phLlcNfc_Initialised_State == changeStateTo) || 
                    (phLlcNfc_Sending_State == changeStateTo))
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_Receiving_State:
            {
                if ((phLlcNfc_Initialised_State == changeStateTo) || 
                    (phLlcNfc_Sending_State == changeStateTo))
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                break;
            }
            case phLlcNfc_Resend_State:
            {
                if (phLlcNfc_Initialised_State == changeStateTo)
                {
                    result = NFCSTATUS_SUCCESS;
                    psLlcCtxt->state = changeStateTo;
                }
                if (phLlcNfc_Sending_State == changeStateTo)
                {
                    result = NFCSTATUS_SUCCESS;
                    if (0 != psLlcCtxt->s_frameinfo.s_send_store.winsize_cnt)
                    {
                        psLlcCtxt->state = changeStateTo;
                    }
                }
                break;
            }
            default:
            {
                /* Error scenario: There cant be any change in the state, 
                    while LLC is in these states */
                result = PHNFCSTVAL(CID_NFC_LLC, 
                                    NFCSTATUS_INVALID_FORMAT);
                break;
            }
        }
    }
    if ((NULL != psLlcCtxt) && 
        (phLlcNfc_Uninitialise_State == changeStateTo))
    {
        psLlcCtxt->state = changeStateTo;
    }
    return result;
}
