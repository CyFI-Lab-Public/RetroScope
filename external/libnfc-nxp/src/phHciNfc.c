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
* =========================================================================== *
*                                                                             *
*                                                                             *
* \file  phHciNfc.c                                                           *
* \brief HCI Interface Source for the HCI Management.                         *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Thu Apr 22 17:49:47 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.90 $                                                           *
* $Aliases: NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $              
*                                                                             *
* =========================================================================== *
*/


/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcConfig.h>
#include <phNfcCompId.h>
#include <phNfcIoctlCode.h>
#include <phHciNfc.h>
#include <phHciNfc_Sequence.h>
#include <phHciNfc_RFReader.h>
#include <phHciNfc_LinkMgmt.h>
#ifdef ENABLE_P2P
#include <phHciNfc_NfcIPMgmt.h>
#endif
#include <phHciNfc_Emulation.h>
#include <phHciNfc_SWP.h>
#include <phHciNfc_DevMgmt.h>
#include <phOsalNfc.h>

/**/

/*
*************************** Static Function Declaration **************************
*/


static
NFCSTATUS
phHciNfc_Config_Emulation (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_sEmulationCfg_t           *pEmulationConfig

                        );


/*
*************************** Function Definitions **************************
*/


/*!
 * \brief Initialises the HCI Interface
 *
 * This function initialises the resources for the HCI Command and
 * Response Mechanism
 */

 NFCSTATUS
 phHciNfc_Initialise (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHciNfc_Init_t                 init_mode,
                        phHal_sHwConfig_t               *pHwConfig,
                        pphNfcIF_Notification_CB_t       pHalNotify,
                        void                            *psContext,
                        phNfcLayer_sCfg_t               *psHciLayerCfg
                     )
{
    phHciNfc_sContext_t *psHciContext = NULL;
    phNfcIF_sReference_t hciReference = { NULL, 0, 0 };
    phNfcIF_sCallBack_t  if_callback = { NULL, NULL, NULL, NULL };
    phNfc_sLowerIF_t    *plower_if = NULL;
    NFCSTATUS            status = NFCSTATUS_SUCCESS;
    uint8_t              lower_index=0;

    if( (NULL == psHciHandle) || (NULL == pHwRef) || (NULL == pHalNotify)
        || (NULL== psContext) || (NULL == psHciLayerCfg) || (NULL == pHwConfig)
    )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL != *(phHciNfc_sContext_t **)psHciHandle )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_ALREADY_INITIALISED );
    }
    else
    {
        /* Create the memory for HCI Context */
        psHciContext = (phHciNfc_sContext_t *) 
                        phOsalNfc_GetMemory(sizeof(phHciNfc_sContext_t));

        if(psHciContext != NULL)
        {
            (void)memset((void *)psHciContext,0,
                                            sizeof(phHciNfc_sContext_t));

            psHciContext->hci_state.cur_state = hciState_Reset;
            psHciContext->hci_mode = hciMode_Reset;
            psHciContext->p_hw_ref = pHwRef;
            psHciContext->host_rf_type = phHal_eUnknown_DevType;
            HCI_PRINT("HCI Initialisation in Progress.... \n");

#ifdef ESTABLISH_SESSION
            /*(void)memcpy(((phHal_sHwReference_t *)pHwRef)->session_id,
                DEFAULT_SESSION, (sizeof(DEFAULT_SESSION) > 0x01) ? 
                  sizeof(DEFAULT_SESSION):
                    sizeof(((phHal_sHwReference_t *)pHwRef)->session_id));*/
            (void)memcpy(pHwConfig->session_id,
                DEFAULT_SESSION, ((sizeof(DEFAULT_SESSION) > 0x01)
                                    && (sizeof(DEFAULT_SESSION) <= 0x08 )) ? 
                  sizeof(DEFAULT_SESSION):
                    sizeof(pHwConfig->session_id));
#endif
            HCI_DEBUG("Sizeof Default Session %u\n",sizeof(DEFAULT_SESSION));
            psHciContext->p_upper_notify = pHalNotify;
            psHciContext->p_upper_context = psContext;

            if_callback.pif_ctxt = psHciContext ;
            if_callback.send_complete = &phHciNfc_Send_Complete;
            if_callback.receive_complete= &phHciNfc_Receive_Complete;
            if_callback.notify = &phHciNfc_Notify_Event;
            plower_if = hciReference.plower_if = &(psHciContext->lower_interface);
            *((phHciNfc_sContext_t **)psHciHandle) = psHciContext;
            psHciContext->init_mode = init_mode;
            psHciContext->p_hci_layer = psHciLayerCfg ;
            lower_index = psHciLayerCfg->layer_index - 1;

            if(NULL != psHciLayerCfg->layer_next->layer_registry)
            {
                status = psHciLayerCfg->layer_next->layer_registry(
                                        &hciReference, if_callback, 
                                        (void *)&psHciLayerCfg[lower_index]);
                HCI_DEBUG("HCI Lower Layer Register, Status = %02X\n",status);
            }
            if( (NFCSTATUS_SUCCESS == status) && (NULL != plower_if->init) )
            {
                status = phHciNfc_FSM_Update ( psHciContext,
                                        hciState_Initialise
                                        );
                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = ADMIN_INIT_SEQ;
                    psHciContext->target_release = FALSE;
                    psHciContext->config_type = POLL_LOOP_CFG;
                    psHciContext->p_config_params = pHwConfig ;
                    status = plower_if->init((void *)plower_if->pcontext, 
                                            (void *)psHciContext->p_hw_ref);
                    HCI_DEBUG("HCI Lower Layer Initialisation, Status = %02X\n",status);
                    if( NFCSTATUS_PENDING != status )
                    {
                        /* Roll Back the State Machine to its Original State */
                        phHciNfc_FSM_Rollback ( psHciContext );
                    }
                }
                else
                {
                    /* TODO: Handle Initialisation in the Invalid State */
                }
            }/* End of Lower Layer Init */
        } /* End of Status Check for Memory */
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);

            HCI_PRINT("HCI Context Memory Allocation Failed\n");
        }

    }
    return status;
}


/*!
 * \brief Release of the HCI Interface .
 *
 * This function Closes all the open pipes and frees all the resources used by
 * HCI Layer
 */

 NFCSTATUS
 phHciNfc_Release (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    pphNfcIF_Notification_CB_t      pHalReleaseCB,
                    void                            *psContext
                  )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)  
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    /* This Scenario Forces the HCI and the lower layers 
     * to release its Resources 
     */
    else if ( NULL == pHalReleaseCB )
    {
        /* Release the lower layer Resources */
        phHciNfc_Release_Lower( psHciContext, pHwRef );
        /* Release the HCI layer Resources */
        phHciNfc_Release_Resources( &psHciContext );
    }
    else if ( NULL == psContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        HCI_PRINT("HCI Release in Progress.... \n");
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Release );
        if ((NFCSTATUS_SUCCESS == status)
#ifdef NXP_HCI_SHUTDOWN_OVERRIDE
            || (NFCSTATUS_INVALID_STATE == PHNFCSTATUS(status))
#endif
            )
        {
            psHciContext->p_upper_notify = pHalReleaseCB;
            psHciContext->p_upper_context = psContext;
            /* psHciContext->hci_seq = EMULATION_REL_SEQ;*/
            /* psHciContext->hci_seq = READER_MGMT_REL_SEQ; */
            if (HCI_SELF_TEST != psHciContext->init_mode)
            {
                psHciContext->hci_seq = PL_STOP_SEQ;
            }
            else
            {
                psHciContext->hci_seq = ADMIN_REL_SEQ;
            }

#ifdef NXP_HCI_SHUTDOWN_OVERRIDE
            if (NFCSTATUS_SUCCESS != status)
            {
                psHciContext->hci_state.next_state = (uint8_t) hciState_Release;
                status = NFCSTATUS_PENDING;
            }
            else
#endif
            {
                status = phHciNfc_Release_Sequence(psHciContext,pHwRef);
            }

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }

    }

    return status;
}

#if 0
/*!
 * \brief  Interface to Starts the RF Device Discovery. 
 *
 * This function Starts the Discovery Wheel.
 */


 NFCSTATUS
 phHciNfc_Start_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef
                     )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if ( (NULL == psHciHandle) 
        || (NULL == pHwRef)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        status = phHciNfc_ReaderMgmt_Enable_Discovery( psHciContext, pHwRef ); 
    }

    return status;
}


/*!
 * \brief  Interface to Stop the RF Device Discovery. 
 *
 * This function Stops the Discovery Wheel.
 */


 NFCSTATUS
 phHciNfc_Stop_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef
                     )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if ( (NULL == psHciHandle) 
        || (NULL == pHwRef)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        status = phHciNfc_ReaderMgmt_Disable_Discovery( psHciContext, pHwRef ); 
    }

    return status;
}


#endif

/*!
 * \brief  Interface to Configure the Device With the appropriate
 * Configuration Parameters .
 *
 * This function configures the Devices with the provided
 * configuration attributes.
 */


 NFCSTATUS
 phHciNfc_Configure (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eConfigType_t             config_type,
                        phHal_uConfig_t                 *pConfig
                     )
 {
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == pConfig)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        switch(config_type)
        {
            case NFC_P2P_CONFIG:
            {
#ifdef ENABLE_P2P
                phHciNfc_sContext_t  *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);
                status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );

                if (NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->config_type = NFC_GENERAL_CFG;
                    psHciContext->p_config_params = &(pConfig->nfcIPConfig);
                    psHciContext->hci_seq = INITIATOR_GENERAL_SEQ;
                    status = phHciNfc_NfcIP_SetATRInfo( psHciHandle,
                                            pHwRef, NFCIP_INITIATOR, 
                                                    &(pConfig->nfcIPConfig));
                    if( NFCSTATUS_PENDING != status )
                    {
                        /* Roll Back the State Machine to its Original State */
                        phHciNfc_FSM_Rollback ( psHciContext );
                    }
                    else
                    {
                        psHciContext->hci_seq = TARGET_GENERAL_SEQ;
                    }
                }
#else
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);

#endif
                break;
            }
            case NFC_EMULATION_CONFIG:
            {
                status = phHciNfc_Config_Emulation( psHciHandle,
                                            pHwRef, &(pConfig->emuConfig));
                break;
            }
            case NFC_SE_PROTECTION_CONFIG:
            {
                phHciNfc_sContext_t  *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);
                status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );

                if (NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->config_type = SWP_PROTECT_CFG;
                    psHciContext->p_config_params = &(pConfig->protectionConfig);
                    psHciContext->hci_seq = HCI_END_SEQ;
                    status = phHciNfc_SWP_Protection( psHciHandle,
                                pHwRef, pConfig->protectionConfig.mode);
                    if( NFCSTATUS_PENDING != status )
                    {
                        /* Roll Back the State Machine to its Original State */
                        phHciNfc_FSM_Rollback ( psHciContext );
                    }
                }
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                break;
            } 
        }/* End of the Configuration Switch */
    }

    return status;
 }


/*!
 * \brief  Interface to Configure the RF Device Discovery using 
 * HCI Polling Loop Gate .
 *
 * This function configures the HCI Polling Loop Gate with the provided
 * configuration attributes.
 */

 NFCSTATUS
 phHciNfc_Config_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_sADD_Cfg_t                *pPollConfig
                     )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == pPollConfig)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        HCI_PRINT("HCI Poll Configuration .... \n");
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );

        if (NFCSTATUS_SUCCESS == status)
        {
#if 0
            if(pPollConfig->PollDevInfo.PollEnabled)
            {
                psHciContext->hci_seq = PL_DURATION_SEQ;
            }
            else
            {
                psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;
                /* psHciContext->hci_seq = (pPollConfig->NfcIP_Mode != 0 )?
                                                    PL_CONFIG_PHASE_SEQ:
                                                        READER_DISABLE_SEQ; */
            }
#endif
            psHciContext->hci_seq = PL_DURATION_SEQ;
            psHciContext->config_type = POLL_LOOP_CFG;
            psHciContext->p_config_params = pPollConfig;
            status = phHciNfc_PollLoop_Sequence( psHciContext, pHwRef );

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }
    }
    return status;
}

/*!
 * \brief  Interface to Restart the RF Device Discovery. 
 *
 * This function restarts the Discovery Wheel.
 */


 NFCSTATUS
 phHciNfc_Restart_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        uint8_t                         re_poll
                     )
{
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHal_eRemDevType_t     target_type = phHal_eUnknown_DevType;

    if ( (NULL == psHciHandle) 
        || (NULL == pHwRef)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;

        /* To be back in the Poll State to Re-Poll the Target */
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Initialise );
        if (NFCSTATUS_SUCCESS == status)
        {
            switch (psHciContext->host_rf_type)
            {
                case phHal_eISO14443_A_PCD:
                {
                    target_type = phHal_eISO14443_A_PICC;
                    break;
                }
                case phHal_eNfcIP1_Initiator:
                {
                    target_type = phHal_eNfcIP1_Target;
                    break;
                }
#ifdef TYPE_B
                case phHal_eISO14443_B_PCD:
                {
                    target_type = phHal_eISO14443_B_PICC;
                    break;
                }
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PCD:
                {
                    target_type = phHal_eFelica_PICC;
                    break;
                }
#endif
#ifdef TYPE_JEWEL
                case phHal_eJewel_PCD:
                {
                    target_type = phHal_eJewel_PICC;
                    break;
                }
#endif
#ifdef  TYPE_ISO15693
                case phHal_eISO15693_PCD:
                {
                    target_type = phHal_eISO15693_PICC;
                    break;
                }
#endif /* #ifdef    TYPE_ISO15693 */
#ifndef TYPE_B
                case phHal_eISO14443_B_PCD:
#endif
#ifndef TYPE_FELICA
                case phHal_eFelica_PCD:
#endif
#ifndef TYPE_JEWEL
                case phHal_eJewel_PCD:
#endif
#ifndef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PCD:
#endif
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    break;
                }
            }/* End of the Remote Target Type Switch */
            if( NFCSTATUS_SUCCESS == status )
            {
                status = phHciNfc_ReaderMgmt_Deselect( 
                    psHciContext, pHwRef, target_type, re_poll);
                if( NFCSTATUS_PENDING != status )
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                }
                else
                {
                    psHciContext->host_rf_type = phHal_eUnknown_DevType;
                }
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }
    }

    return status;
}



/*!
 * \brief  Interface to Configure the device to emulation as 
 * the tag, smart tag or p2p target .
 *
 * This function configures the HCI Polling Loop Gate with the provided
 * configuration attributes.
 */

 static
 NFCSTATUS
 phHciNfc_Config_Emulation (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_sEmulationCfg_t           *pEmulationCfg
                     )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == pEmulationCfg)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;

        HCI_PRINT("HCI Configure Emulation .... \n");
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );

        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = EMULATION_CONFIG_SEQ;
            psHciContext->p_config_params = pEmulationCfg;
            switch( pEmulationCfg->emuType )
            {
                case NFC_SMARTMX_EMULATION:
                {
                    psHciContext->config_type = SMX_WI_CFG;
                    status = phHciNfc_Emulation_Cfg(psHciContext, 
                                pHwRef, SMX_WI_CFG);
                    break;
                }
                case NFC_UICC_EMULATION:
                {
                    psHciContext->config_type = UICC_SWP_CFG;
                    psHciContext->hci_seq = EMULATION_CONFIG_SEQ;
                    (void)phHciNfc_SWP_Update_Sequence(
                                        psHciContext, CONFIG_SEQ );
                    status = phHciNfc_EmulationCfg_Sequence(
                                                psHciContext, pHwRef);
                    break;
                }
                case NFC_HOST_CE_A_EMULATION:
                case NFC_HOST_CE_B_EMULATION:
#if defined(HOST_EMULATION)
                {
                    if(TRUE == pEmulationCfg->config.
                        hostEmuCfg_A.enableEmulation)
                    {
                        psHciContext->hci_seq = ADMIN_CE_SEQ;
                    }
                    status = phHciNfc_EmulationCfg_Sequence(
                                                psHciContext, pHwRef);
                    break;
                }
#endif
                default:
                {
                    break;
                }

            } /* End of Config Switch */
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }
    }

    return status;
}
 
 NFCSTATUS
 phHciNfc_Switch_SwpMode (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eSWP_Mode_t               swp_mode /* ,
                        void                            *pSwpCfg */
                     )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;

        HCI_PRINT("HCI SWP Switch .... ");
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );
        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->config_type = SWP_EVT_CFG;
            status = phHciNfc_SWP_Configure_Mode( psHciContext, pHwRef ,
                                                       (uint8_t) swp_mode );

            /* Send the Success Status as this is an event */
            status = ((status == NFCSTATUS_SUCCESS)?
                    NFCSTATUS_PENDING : status);

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );

                HCI_PRINT(" Execution Error \n");
            }
            else
            {
                HCI_PRINT(" Successful \n");
            }
        }
        else
        {
            HCI_PRINT(" Not allowed - Invalid State \n");
            /* TODO: Return appropriate Error */
        }
    }

    return status;
}



/*!
 * \brief  Interface to Switch the Mode of the SmartMx from Virtual/Wired 
 * to the other mode.
 *
 * This function switches the mode of the SmartMX connected through WI(S2C)
 * Interface to virtual/wired mode.
 */


 NFCSTATUS
 phHciNfc_Switch_SmxMode (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eSmartMX_Mode_t           smx_mode,
                        phHal_sADD_Cfg_t                *pPollConfig
                    )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == pPollConfig)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;

        HCI_PRINT("HCI Smart MX Mode Switch .... \n");
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Config );

        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = READER_DISABLE_SEQ;
            if ( (eSmartMx_Wired == psHciContext->smx_mode)
                && ( hciState_Connect == psHciContext->hci_state.cur_state)
                &&( eSmartMx_Wired != smx_mode)
                )
            {
                /* Workaround: For Wired Mode Disconnect
                   All the statemachine updates should be done only with the
                   Statemachine API and should not be overridden.
                 */
                 psHciContext->hci_state.cur_state = hciState_Disconnect;
            }
            psHciContext->config_type = SMX_WI_MODE;
            psHciContext->smx_mode = smx_mode;
            psHciContext->p_config_params = pPollConfig;
            status = phHciNfc_SmartMx_Mode_Sequence( psHciContext, pHwRef );
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }
    }

    return status;
}


/*!
 * \brief  Interface to Select the Next Remote Target Discovered during the
 * discovery sequence using the particular HCI Reader Gate .
 * 
 *
 * This function Selects and Activates the next Remote Target 
 * Detected using the particular HCI Reader Gate. 
 */



NFCSTATUS
phHciNfc_Select_Next_Target (
                    void                            *psHciHandle,
                    void                            *pHwRef
                    )
{
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Select );
        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = READER_SELECT_SEQ;
            status = phHciNfc_ReaderMgmt_Activate_Next( psHciContext, pHwRef );
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }
    }

    return status;

}


/*!
 * \brief  Interface to Connect the Remote Target Discovered during the
 * discovery sequence using the particular HCI Reader Gate .
 * 
 *
 * This function connects the Remote Target Detected using the particular 
 * HCI Reader Gate with the appropriate configuration setup.
 */


 NFCSTATUS
 phHciNfc_Connect (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_target_info
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;
    /* phHal_eRemDevType_t  target_type = phHal_eUnknown_DevType; */

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == p_target_info)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Connect );
        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = READER_SELECT_SEQ;
            switch (p_target_info->RemDevType)
            {
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eMifare_PICC:
                case phHal_eISO14443_3A_PICC:
#ifdef ENABLE_P2P
                case phHal_eNfcIP1_Target:
#endif
#ifdef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifdef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
#ifdef  TYPE_ISO15693
                case phHal_eISO15693_PICC:
#endif /* #ifdef    TYPE_ISO15693 */

                {
                    psHciContext->p_target_info = p_target_info;
                    status = phHciNfc_ReaderMgmt_Select( 
                                    psHciContext, pHwRef,
                                    p_target_info->RemDevType );
                    break;
                }
#ifndef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PICC:
#endif
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
            }/* End of the Remote Target Type Switch */
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }

    } /* End of the HCI Handle Validation */

    return status;
}


/*!
 * \brief  Interface to Reactivate the Remote Targets Discovered during the
 * discovery sequence using the particular HCI Reader Gate .
 * 
 *
 * This function reactivates the Remote Target Detected using the particular 
 * HCI Reader Gate with the appropriate configuration setup.
 */


 NFCSTATUS
 phHciNfc_Reactivate (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_target_info
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;
    /* phHal_eRemDevType_t  target_type = phHal_eUnknown_DevType; */

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == p_target_info)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Reactivate );
        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = READER_REACTIVATE_SEQ;
            switch (p_target_info->RemDevType)
            {
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eMifare_PICC:
                case phHal_eISO14443_3A_PICC:
                {
                    psHciContext->host_rf_type = phHal_eISO14443_A_PCD;
                    break;
                }
                case phHal_eNfcIP1_Target:
                {
                    psHciContext->host_rf_type = phHal_eNfcIP1_Initiator;
                    break;
                }
#ifdef TYPE_B
                case phHal_eISO14443_4B_PICC:
                case phHal_eISO14443_B_PICC:
                {
                    psHciContext->host_rf_type = phHal_eISO14443_B_PCD;
                    break;
                }
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PICC:
                {
                    psHciContext->host_rf_type = phHal_eFelica_PCD;
                    break;
                }
#endif
#ifdef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PICC:
#endif
                    /* Reactivate for Jewel is not Supported */
                case phHal_eJewel_PICC:
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
            }/* End of the Remote Target Type Switch */
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }

        if(NFCSTATUS_SUCCESS == status )
        {
            psHciContext->p_target_info = p_target_info;
            status = phHciNfc_ReaderMgmt_Reactivate( 
                            psHciContext, pHwRef, p_target_info->RemDevType );
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    } /* End of the HCI Handle Validation */


    return status;
}


/*!
 * \brief  Interface to Disconnect the selected target.
 *
 * This function disconnects the remote target selected.
 */


 NFCSTATUS
 phHciNfc_Disconnect (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint8_t                         re_poll
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;
    phHal_eRemDevType_t  target_type = phHal_eUnknown_DevType;
    /* phHal_eSmartMX_Mode_t smx_mode = (phHal_eSmartMX_Mode_t)type; */
    static  uint8_t      repoll=0;


    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || ( NULL == psHciContext->p_target_info)
    )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        repoll = re_poll;
        psHciContext->p_config_params = &repoll;
        /* psHciContext->hci_seq = HCI_END_SEQ; */

        /* To be back in the Poll State to Re-Poll the Target */
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Disconnect );
        if (NFCSTATUS_SUCCESS == status)
        {
            psHciContext->hci_seq = READER_UICC_DISPATCH_SEQ;
            target_type = psHciContext->p_target_info->RemDevType;
            switch (target_type)
            {
                case phHal_eMifare_PICC:
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eISO14443_3A_PICC:
                case phHal_eNfcIP1_Target:
#ifdef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifdef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
#ifdef  TYPE_ISO15693
                case phHal_eISO15693_PICC:
#endif /* #ifdef    TYPE_ISO15693 */

                {
                    status = phHciNfc_Disconnect_Sequence( 
                                    psHciContext, pHwRef );
                    break;
                }
#ifndef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifndef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifndef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
#ifndef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PICC:
#endif
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    break;
                }
            }/* End of the Remote Target Type Switch */
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            /* TODO: Return appropriate Error */
        }
    } /* End of the HCI Handle Validation */

    return status;
}

/*!
 * \brief  Interface to exchange the data to/from 
 * the selected target.
 * 
 * This function sends and receives the data to/from 
 * the selected remote target.
 */

 NFCSTATUS
 phHciNfc_Exchange_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_target_info,
                    phHciNfc_XchgInfo_t             *p_xchg_info
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == p_target_info)
        || (NULL == p_xchg_info)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (p_target_info != psHciContext->p_target_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_REMOTE_DEVICE);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Transact ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            switch (p_target_info->RemDevType)
            {
                case phHal_eMifare_PICC:
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eISO14443_3A_PICC:
#ifdef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifdef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
#ifdef  TYPE_ISO15693
                case phHal_eISO15693_PICC:
#endif /* #ifdef    TYPE_ISO15693 */
                {
                    psHciContext->p_xchg_info = p_xchg_info;
                    status = phHciNfc_ReaderMgmt_Exchange_Data( 
                                    psHciContext, pHwRef, p_xchg_info );
                    break;
                }
#ifndef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifndef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifndef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
                case phHal_eNfcIP1_Target:
#ifndef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PICC:
#endif
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    break;
                }

            }/* End of the Remote Target Type Switch */
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }
    } /* End of the HCI Handle Validation */

    return status;
}

/*!
 * \brief  Interface to Send the data to/from 
 * the selected NfcIP.
 * 
 * This function sends and receives the data to/from 
 * the selected remote target.
 */

 NFCSTATUS
 phHciNfc_Send_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_remote_dev_info,
                    phHciNfc_XchgInfo_t             *p_send_param
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        || (NULL == p_send_param)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Transact ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            switch (psHciContext->host_rf_type)
            {
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eISO14443_4B_PICC:
                {
                    break;
                }
#ifdef ENABLE_P2P
                case phHal_eNfcIP1_Initiator:
                {
                    if (p_remote_dev_info != 
                                    psHciContext->p_target_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                    }
                    else
                    {
                        psHciContext->p_xchg_info = p_send_param;
                        status = phHciNfc_NfcIP_Send_Data( psHciContext, 
                                                    pHwRef, p_send_param );
                    }
                    break;
                }
                case phHal_eNfcIP1_Target:
                {
                    psHciContext->p_xchg_info = p_send_param;
                    status = phHciNfc_NfcIP_Send_Data( psHciContext, 
                                                pHwRef, p_send_param );
                    break;
                }
#endif
#ifdef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PCD:
                case phHal_eFelica_PCD:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
#endif
                case phHal_eUnknown_DevType:
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    break;
                }

            }/* End of the Remote Target Type Switch */
#if defined( ENABLE_P2P ) || defined (TYPE_B_PRIME)
            if( NFCSTATUS_PENDING != status )
#endif
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }
    } /* End of the HCI Handle Validation */

    return status;

 }

#if 0
    
/*!
 * \brief  Interface to Send the data from 
 * the selected NfcIP.
 * 
 * This function sends and receives the data to/from 
 * the selected remote target.
 */

 NFCSTATUS
 phHciNfc_Receive_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint8_t                         *p_data,
                    uint8_t                         length
                 )
 {
    phHciNfc_sContext_t  *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciHandle) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if (NFCSTATUS_SUCCESS == status)
        {
            status = phHciNfc_Receive(psHciHandle, pHwRef, p_data, length);
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }
    return status;

 }

#endif

 /*!
 * \brief  Interface to Check for the presence of
 * the selected target in the field .
 * 
 * This function checks the presence of the 
 * the selected remote target in the field .
 */



NFCSTATUS
phHciNfc_Presence_Check (
                    void                            *psHciHandle,
                    void                            *pHwRef
                    )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);
    phHal_eRemDevType_t     target_type = phHal_eUnknown_DevType;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Presence ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            target_type = psHciContext->p_target_info->RemDevType;
            switch (target_type)
            {
                case phHal_eISO14443_A_PICC:
                case phHal_eMifare_PICC:
                case phHal_eISO14443_4A_PICC:
                case phHal_eISO14443_3A_PICC:
#ifdef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifdef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifdef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
#ifdef  TYPE_ISO15693
                case phHal_eISO15693_PICC:
#endif /* #ifdef    TYPE_ISO15693 */
#ifdef ENABLE_P2P
                case phHal_eNfcIP1_Target:
#endif
                {
                    status = phHciNfc_ReaderMgmt_Presence_Check( 
                                            psHciContext, pHwRef );
                    break;
                }
#ifdef TYPE_B_PRIME
                case phHal_eISO14443_BPrime_PICC:
#endif
#ifndef TYPE_B
                case phHal_eISO14443_B_PICC:
                case phHal_eISO14443_4B_PICC:
#endif
#ifndef TYPE_FELICA
                case phHal_eFelica_PICC:
#endif
#ifndef TYPE_JEWEL
                case phHal_eJewel_PICC:
#endif
                case phHal_eUnknown_DevType:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
                default:
                {
                    /* Roll Back the State Machine to its Original State */
                    phHciNfc_FSM_Rollback ( psHciContext );
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    break;
                }

            }/* End of the Remote Target Type Switch */
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
        }
    } /* End of the HCI Handle Validation */

    return status;
}

 NFCSTATUS
 phHciNfc_PRBS_Test (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint32_t                        test_type,
                    phNfc_sData_t                   *test_param
                 )
 {
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        || (test_type != DEVMGMT_PRBS_TEST)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_IO ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            status = phHciNfc_DevMgmt_Test(psHciContext, pHwRef,
                (uint8_t)(test_type & DEVMGMT_TEST_MASK), test_param);
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }
    return status;
 }


 NFCSTATUS
 phHciNfc_System_Test (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint32_t                        test_type,
                    phNfc_sData_t                   *test_param
                 )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);
    static phNfc_sData_t test_result;
    static uint8_t       gpio_status = 0;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_Test ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            if (test_type != NFC_GPIO_READ)
            {
                status = phHciNfc_DevMgmt_Test(psHciContext, pHwRef,
                    (uint8_t)(test_type & DEVMGMT_TEST_MASK), test_param);
            }
            else
            {
                test_result.buffer = &gpio_status;
                status = phHciNfc_DevMgmt_Get_Info(psHciContext, pHwRef,
                    (uint16_t)NFC_GPIO_READ, test_result.buffer);

            }
            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }

     return status;
}


 NFCSTATUS
 phHciNfc_System_Configure (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint32_t                        config_type,
                    uint8_t                         config_value
                 )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_IO ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            status = phHciNfc_DevMgmt_Configure(psHciContext, pHwRef,
                (uint16_t)config_type, config_value);

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }
 return status;
}

NFCSTATUS
 phHciNfc_System_Get_Info(
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint32_t                        config_type,
                    uint8_t                         *p_config_value
                 )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        || (NULL == p_config_value)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_IO ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            status = phHciNfc_DevMgmt_Get_Info(psHciContext, pHwRef,
                (uint16_t)config_type, p_config_value);

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }

 return status;
}


 NFCSTATUS
 phHciNfc_Get_Link_Status(
                    void                            *psHciHandle,
                    void                            *pHwRef
                 )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->p_hw_ref = pHwRef;
        status = phHciNfc_FSM_Update ( psHciContext, hciState_IO ); 
        if (NFCSTATUS_SUCCESS == status)
        {
            status = phHciNfc_LinkMgmt_Open(psHciContext, pHwRef);

            if( NFCSTATUS_PENDING != status )
            {
                /* Roll Back the State Machine to its Original State */
                phHciNfc_FSM_Rollback ( psHciContext );
            }
        }
    }

 return status;
}



