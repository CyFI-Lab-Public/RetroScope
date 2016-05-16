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
* \file  phHciNfc_Sequence.c                                                  *
* \brief State Machine Implementation for the HCI Management and              *
* and the Function Sequence for a particular State                            *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun  8 09:33:46 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.85 $                                                           *
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/

/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcCompId.h>
#include <phNfcConfig.h>
#include <phHciNfc.h>
#include <phHciNfc_Sequence.h>
#include <phHciNfc_AdminMgmt.h>
#include <phHciNfc_IDMgmt.h>
#include <phHciNfc_LinkMgmt.h>
#include <phHciNfc_DevMgmt.h>
#include <phHciNfc_PollingLoop.h>
#include <phHciNfc_RFReader.h>
#include <phHciNfc_RFReaderA.h>
#include <phHciNfc_Emulation.h>
#ifdef ENABLE_P2P
#include <phHciNfc_NfcIPMgmt.h>
#endif
#include <phHciNfc_SWP.h>
#include <phHciNfc_WI.h>
#include <phOsalNfc.h>

/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

/* Address Definitions for HAL Configuration */
#define NFC_ADDRESS_HAL_CONF            0x9FD0U


/*
################################################################################
********************** Structure/Enumeration Definitions ***********************
################################################################################
*/


#ifdef VALIDATE_FSM

typedef struct phHciNfc_sFsm
{
    phHciNfc_eState_t from_state;
    phHciNfc_eState_t to_state;
    uint8_t           valid;
}phHciNfc_sFsm_t;

static phHciNfc_sFsm_t phHciNfc_Valid_Fsm[] = {
    {hciState_Reset,        hciState_Initialise ,   TRUE},
        /*  {hciState_Reset,        hciState_Config,            FALSE}, */
    {hciState_Initialise,   hciState_Config,            TRUE},
    {hciState_Initialise,   hciState_Release,       TRUE},
    {hciState_Config,       hciState_Connect,       TRUE},
    {hciState_Config,       hciState_Release,       TRUE},
    {hciState_Connect,      hciState_Activate,      TRUE},
    {hciState_Connect,      hciState_Transact,      TRUE},
    {hciState_Connect,      hciState_Disconnect,    TRUE},
    {hciState_Disconnect,   hciState_Config,            TRUE},
    /*  {hciState_Disconnect,   hciState_Release,       TRUE}, */
    {hciState_Reset,        hciState_Initialise,    TRUE},
};

#endif


/*
################################################################################
************************* Function Prototype Declaration ***********************
################################################################################
*/


static
NFCSTATUS
phHciNfc_Config_Sequence(
                         phHciNfc_sContext_t        *psHciContext,
                         void                   *pHwRef
                         );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Connect_Sequence function sequence selects the  
 *  discovered target for performing the transaction.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI target selection sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

static
NFCSTATUS
phHciNfc_Transact_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Info_Sequence function sequence selects the  
 *  discovered target for performing the transaction.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI target selection sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

static
NFCSTATUS
phHciNfc_Info_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                   );

static
NFCSTATUS
phHciNfc_Test_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            NFCSTATUS               test_status,
                            uint8_t                 *pdata,
                            uint8_t                 length
                         );

#ifdef HCI_FSM_RESET

static
void
phHciNfc_FSM_Reset(
                        phHciNfc_sContext_t *psHciContext
                    );

#endif

static
NFCSTATUS
phHciNfc_IO_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            NFCSTATUS               test_status,
                            uint8_t                 *pdata,
                            uint8_t                 length
                    );

static
NFCSTATUS
phHciNfc_Pending_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                          );


/*
################################################################################
***************************** Function Definitions *****************************
################################################################################
*/

NFCSTATUS 
phHciNfc_FSM_Validate( 
                      phHciNfc_sContext_t *psHciContext,  
                      phHciNfc_eState_t state,  
                      uint8_t validate_type
                    )
{
    NFCSTATUS           status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
    phHciNfc_eState_t   cur_state = (phHciNfc_eState_t) 
        psHciContext->hci_state.cur_state;

    switch(validate_type)
    {
        case NFC_FSM_CURRENT:
        {
            if( cur_state == (uint8_t) state )
            {
                status = NFCSTATUS_SUCCESS;
            }
            break;
        }
        case NFC_FSM_NEXT:
        {
            phHciNfc_eState_t   next_state = state;
            switch (cur_state)
            {
                case hciState_Reset:
                {
                    switch(next_state)
                    {
                        /* Specifies the Starting of the init Sequence */
                        case hciState_Initialise:
                        /* Initialise to Perform Test on 
                           the Antenna/SWP Link */
                        case hciState_Test:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Initialise:
                {
                    switch(next_state)
                    {
                        /* Discovery Resume after connect failure */
                        case hciState_Initialise:
                        /* Configuring the Discovery/Emulation */
                        case hciState_Config:
                        /* Configuring the Memory */
                        case hciState_IO:
                        /* Occurence of the Tag Discovered Event */
                        case hciState_Select:
                        /* Occurence of the Target Activated Event */
                        case hciState_Listen:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Test:
                {
                    if ((hciState_Test == next_state )
                        || (hciState_IO == next_state)
                        || (hciState_Release == next_state))
                    {
                        /*  Next Test/Reset Sequence */
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }
                case hciState_Select:
                {
                    switch(next_state)
                    {
                        /* Restart the Wheel */
                        case hciState_Initialise:
                            /* Select the next Tag in the Field or 
                             * already Selected Tag Again 
                             */
                        /* Configuring the Memory */
                        case hciState_IO:
                        case hciState_Select:
                        /* Configuring the Discovery/Emulation */
                        case hciState_Config:
                            /* Re-Activate the Target or 
                             * Discover the next target 
                             */
                        case hciState_Reactivate:
                        /* Connect the Discovered Target */
                        case hciState_Connect:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Connect:
                {
                    switch(next_state)
                    {
                        /* Disabling the Tag Discovery */
                        case hciState_Initialise:
                            /* Configuring the Discovery/Emulation */
                            /* This should not be allowed if the target
                            * is connected.
                            */
                        /* Configuring the Memory */
                        case hciState_IO:
                        case hciState_Config:
                            /* Re-Activate the Target or 
                             * Discover the next target 
                             */
                        case hciState_Reactivate:
                        /* Intermediate Transceive State */
                        case hciState_Transact:
                        /* Intermediate Presence Check State */
                        case hciState_Presence:
                        /* Disconnect the Target Connected */
                        case hciState_Disconnect:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Listen:
                {
                    switch(next_state)
                    {
                        /* Releasing from the Emulation/Target Mode */
                        case hciState_Initialise:
                        /* Occurence of the Tag Discovered Event 
                        * after the Disconnect Operation
                        */
                        case hciState_Select:
                        /* Configuring the Memory */
                        case hciState_IO:
                        /* Configuring the Discovery/Emulation */
                        case hciState_Config:
                        /* Intermediate Transceive State */
                        case hciState_Transact:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Reactivate:
                {
                    switch(next_state)
                    {
                        /* Restart/Discovery after the Target is removed
                         * after Reactivation.
                        */
                        /* case hciState_Initialise: */
                        /* Re-Connect the Re-Activated Target */
                        case hciState_Connect:
                        /* Configuring the Memory */
                        case hciState_IO:
                        /* Configuring the Discovery/Emulation */
                        case hciState_Config:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                case hciState_Disconnect:
                {
                    switch(next_state)
                    {
                        /* Discovery Resume after connect failure 
                           after the disconnect */
                        case hciState_Initialise:
                        /* Configuring the Memory */
                        case hciState_IO:
                        /* Configuring the Discovery/Emulation */
                        case hciState_Config:
                        /* Occurence of the Tag Discovered Event 
                        * after the Disconnect Operation
                        */
                        case hciState_Select:
                        /* Occurence of the Target Activated Event */
                        case hciState_Listen:
                        /* Specifies the Starting of the Release Sequence */
                        case hciState_Release:
                        {
                            status = NFCSTATUS_SUCCESS;
                            break;
                        }
                        default:
                        {
                            break;
                    }
                    }
                    break;
                }
#ifdef USE_M5
                case hciState_Presence:
                case hciState_Transact:
                case hciState_Release:
                {
                    break;
                }
#endif
                /* case phHciNfc_Unknown: */
                default:
                {
                    /* status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE); */
                    break;
                }
            } /* End of State Validation Switch */
            if( NFC_FSM_IN_PROGRESS == psHciContext->hci_state.transition )
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_BUSY);
            }
            break;
        }
        default:
        {
            HCI_DEBUG("State Validate Type:%x is Unknown/Incorrect \n",
                                                            validate_type);
            break;
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_FSM_Update(
                    phHciNfc_sContext_t *psHciContext,
                    phHciNfc_eState_t   next_state
                    )
{
    NFCSTATUS       status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);

    HCI_DEBUG(" HCI: Current State --> %02u \n", 
                            psHciContext->hci_state.cur_state );
    HCI_DEBUG(" HCI: Transition Before FSM Update --> %02u \n", 
                    psHciContext->hci_state.transition );
    HCI_DEBUG(" HCI: Next State Before FSM Update --> %02u \n", 
                            psHciContext->hci_state.next_state );

    status = phHciNfc_FSM_Validate(psHciContext, next_state, NFC_FSM_NEXT );
    if(NFCSTATUS_SUCCESS == status)
    {
        psHciContext->hci_state.next_state = (uint8_t) next_state;
        psHciContext->hci_state.transition = NFC_FSM_IN_PROGRESS;
        psHciContext->response_pending = FALSE;
        HCI_DEBUG(" HCI: Next State After FSM Update --> %02u \n", 
                                psHciContext->hci_state.next_state );
    }
    else
    {
        HCI_DEBUG(" HCI: FSM - Invalid next state --> %02u \n", 
                                next_state );
    }

    return status;
}


NFCSTATUS
phHciNfc_FSM_Complete(
                        phHciNfc_sContext_t *psHciContext
                    )
{
    NFCSTATUS       status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);

    HCI_DEBUG("HCI: In Function: %s \n", __FUNCTION__);

    HCI_DEBUG(" HCI: Transition Before FSM Complete --> %02u \n", 
                    psHciContext->hci_state.transition );
 
    HCI_DEBUG(" HCI: Current State Before FSM Complete --> %02u \n", 
                            psHciContext->hci_state.cur_state );

    HCI_DEBUG(" HCI: Next State Before FSM Complete  --> %02u \n", 
                            psHciContext->hci_state.next_state );

    if( (NFC_FSM_IN_PROGRESS == psHciContext->hci_state.transition)
      )
    {
        psHciContext->hci_state.cur_state = 
                                    psHciContext->hci_state.next_state ;
        psHciContext->hci_state.transition = NFC_FSM_COMPLETE ;
        psHciContext->hci_state.next_state = (uint8_t) hciState_Unknown ;
        /* Reset the HCI Sequence */
        psHciContext->response_pending = FALSE;
        psHciContext->hci_seq = HCI_INVALID_SEQ;
        status = NFCSTATUS_SUCCESS;
    }

    HCI_DEBUG(" HCI: Current State After FSM Complete --> %02u \n", 
                            psHciContext->hci_state.cur_state );

    return status;
}

void
phHciNfc_FSM_Rollback(
                        phHciNfc_sContext_t *psHciContext
                    )
{

    HCI_DEBUG("HCI: %s: transition=%02u, cur_state=%02u, next_state=%02u\n",
            __func__,
            psHciContext->hci_state.transition,
            psHciContext->hci_state.cur_state,
            psHciContext->hci_state.next_state);






    if( (NFC_FSM_IN_PROGRESS  == psHciContext->hci_state.transition)
      )
    {
        psHciContext->hci_state.transition = NFC_FSM_COMPLETE ;
        psHciContext->hci_state.next_state = (uint8_t) hciState_Unknown ;
        /* Reset the HCI Sequence */
        psHciContext->hci_seq = HCI_INVALID_SEQ;
        psHciContext->response_pending = FALSE;
    }
}

#ifdef HCI_FSM_RESET
static
void
phHciNfc_FSM_Reset(
                        phHciNfc_sContext_t *psHciContext
                    )
{

    if( (hciState_Reset  != psHciContext->hci_state.cur_state )
      )
    {
        psHciContext->hci_state.cur_state = (uint8_t) hciState_Initialise ;
        psHciContext->hci_state.transition = NFC_FSM_COMPLETE ;
        psHciContext->hci_state.next_state = (uint8_t) hciState_Unknown ;
        /* Reset the HCI Sequence */
        psHciContext->hci_seq = HCI_INVALID_SEQ;
    }

}
#endif



static
NFCSTATUS
phHciNfc_Pending_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                          )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    PHNFC_UNUSED_VARIABLE(status);

    HCI_DEBUG("HCI: psHciContext->target_release --> %s \n",
            (psHciContext->target_release)?"TRUE":"FALSE");
    if(TRUE == psHciContext->target_release)
    {
#ifdef SW_RELEASE_TARGET
        status = phHciNfc_ReaderMgmt_Deselect( 
            psHciContext, pHwRef, phHal_eISO14443_A_PICC, TRUE);
        if(NFCSTATUS_PENDING == status )
        {
            psHciContext->target_release = FALSE ;
        }
    }
    else
    {
        status = psHciContext->error_status;
#else
      psHciContext->target_release = FALSE ;
#endif
    }

    return status;
}


void
phHciNfc_Error_Sequence(
                                void            *psContext,
                                void            *pHwRef,
                                NFCSTATUS       error_status,
                                void            *pdata,
                                uint8_t         length
                        )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t *psHciContext = (phHciNfc_sContext_t *)psContext;

    PHNFC_UNUSED_VARIABLE(status);

    HCI_DEBUG("HCI: In Function: %s \n",
        __FUNCTION__);

    HCI_DEBUG ("HCI : Error Status : %04X\n", error_status);

    HCI_DEBUG(" HCI: Current HCI State --> %02u \n", 
                            psHciContext->hci_state.cur_state );
    HCI_DEBUG(" HCI: Next HCI State --> %02u \n", 
                            psHciContext->hci_state.next_state );


    if ( NFC_FSM_IN_PROGRESS == psHciContext->hci_state.transition )
    {
        switch(psHciContext->hci_state.next_state)
        {
            case hciState_Initialise:
            {
                if (hciState_Reset == psHciContext->hci_state.cur_state)
                {
                    phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                    phHciNfc_Release_Lower( psHciContext, pHwRef );
                    /* Release all the resources and 
                    * Notify the Receive Error Scenario to the Upper Layer 
                    */
                    comp_info.status = error_status ;
                    phHciNfc_Release_Notify (psHciContext, pHwRef,
                        NFC_NOTIFY_INIT_FAILED, &comp_info);
                }
                else if (hciState_Config == psHciContext->hci_state.cur_state)
                {
                    /* Notify the Poll/Emulation Configure failure to the upper layer */

                    phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                    comp_info.status = error_status ;

                    psHciContext->error_status = error_status;
                    status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                    /* Rollback the FSM as the Poll/Emulation configuration Failed */
                    phHciNfc_FSM_Rollback(psHciContext);
                    psHciContext->error_status = NFCSTATUS_SUCCESS;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_CONFIG_ERROR, &comp_info);
                }
                else
                {

                    /* Notify the Poll Configure failure to the upper layer */
                    phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};


                    psHciContext->error_status = error_status;
                    status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                    /* Rollback the FSM as the Poll Disable Failed */
                    phHciNfc_FSM_Rollback(psHciContext);
                    comp_info.status = error_status ;
                    psHciContext->error_status = NFCSTATUS_SUCCESS;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_ERROR, &comp_info);
                }
                break;
            }
            case hciState_Test:
            {
                status = phHciNfc_Test_Sequence( psHciContext, pHwRef , error_status, 
                                                            (uint8_t *)pdata, length );
                break;
            }
            case hciState_IO:
            {
                status = phHciNfc_IO_Sequence( psHciContext, pHwRef , error_status, 
                                                            (uint8_t *)pdata, length );
                break;
            }
            case hciState_Config:
            {
                /* Notify the Configure failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                psHciContext->error_status = error_status;
                status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                /* Rollback the FSM as the Poll Failed */
                phHciNfc_FSM_Rollback(psHciContext);
                comp_info.status = psHciContext->error_status ;
                psHciContext->error_status = NFCSTATUS_SUCCESS;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_CONFIG_ERROR, &comp_info);
                break;
            }
            case hciState_Select:
            {
                /* Notify the Configure failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                /* Rollback the FSM as the Target Discovery Failed */
                phHciNfc_FSM_Rollback(psHciContext);
                status = phHciNfc_ReaderMgmt_Update_Sequence(
                                                psHciContext, INFO_SEQ );
                comp_info.status = error_status ;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_DISCOVERY_ERROR, &comp_info);

#if 0
                /* Polling Wheel will be restarted by the upper layer 
                 * to Rediscover again */
                if(NFCSTATUS_SUCCESS == status)
                {
                    status = phHciNfc_ReaderMgmt_Deselect( 
                        psHciContext, pHwRef, phHal_eISO14443_A_PICC, FALSE);
                }
                phHciNfc_FSM_Rollback(psHciContext);
#endif
                break;
            }
            case hciState_Transact:
                /* Notify the Transceive failure to the upper layer */
            {
                phNfc_sTransactionInfo_t        transact_info={FALSE,0,NULL,NULL,0};

                /* Rollback the FSM as the Transceive Failed */
                phHciNfc_FSM_Rollback(psHciContext);
                transact_info.status = error_status;
                transact_info.buffer = NULL;
                transact_info.length = FALSE;
                psHciContext->p_xchg_info = NULL ;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_TRANSCEIVE_ERROR, &transact_info);
                break;

            }
            case hciState_Connect:
            {
                /* Notify the General failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                /* psHciContext->host_rf_type = phHal_eUnknown_DevType; */
                status = phHciNfc_ReaderMgmt_Update_Sequence(
                                                psHciContext, INFO_SEQ );
                psHciContext->p_target_info = NULL;
                psHciContext->hci_state.cur_state = hciState_Select;
                phHciNfc_FSM_Rollback(psHciContext);
                comp_info.status = error_status ;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_CONNECT_FAILED, &comp_info);
                break;
            }
            case hciState_Reactivate:
            {
                /* Notify the General failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                /* psHciContext->host_rf_type = phHal_eUnknown_DevType; 
                status = phHciNfc_ReaderMgmt_Update_Sequence(
                                                psHciContext, INFO_SEQ );
                psHciContext->p_target_info = NULL;
                psHciContext->hci_state.cur_state = hciState_Select;  */
                phHciNfc_FSM_Rollback(psHciContext);
                comp_info.status = error_status ;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_CONNECT_FAILED, &comp_info);
                break;
            }
            case hciState_Presence:
            {
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                /* Roll Back to Connect State as Presence Check is Complete */
                phHciNfc_FSM_Rollback(psHciContext);

                /* Initialisation Complete Notification to the Upper Layer */
                comp_info.status = error_status;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                             NFC_NOTIFY_ERROR, &comp_info);
                HCI_PRINT(" HCI Remote Target Removed from the Field. \n");
                break;
            }
            /* Notify the Connect or Disconnect failure to the upper layer */
            case hciState_Disconnect:
            {
                /* Notify the General failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                phHciNfc_FSM_Rollback(psHciContext);
                comp_info.status = error_status ;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                    psHciContext->p_upper_context, pHwRef,
                    NFC_NOTIFY_DISCONNECT_FAILED, &comp_info);
                break;
            }
            case hciState_Release:
            {
#ifdef NXP_HCI_SHUTDOWN_OVERRIDE
                status = phHciNfc_Release_Sequence(psHciContext ,pHwRef);
#else
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                phHciNfc_Release_Lower( psHciContext, pHwRef );
                /* Release all the resources and 
                * Notify the Receive Error Scenario to the Upper Layer 
                */
                comp_info.status = error_status ;
                phHciNfc_Release_Notify (psHciContext, pHwRef,
                    NFC_NOTIFY_DEINIT_FAILED, &comp_info);
#endif
                break;
            }
            default:
            {
                /* Notify the General failure to the upper layer */
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                phHciNfc_FSM_Rollback(psHciContext);
                comp_info.status = error_status ;
                psHciContext->error_status = error_status;
                status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                if (NFCSTATUS_PENDING != status)
                {
                    psHciContext->error_status = NFCSTATUS_SUCCESS;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_ERROR, &comp_info);
                }
                break;
            }

        } /* End of the Processing of HCI State*/
    }
    else
    {
        /* Notify the General failure to the upper layer */
        phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};
        phHciNfc_FSM_Rollback(psHciContext);
        comp_info.status = error_status ;
        /* Disable the Notification to the Upper Layer */
        if(NFCSTATUS_BOARD_COMMUNICATION_ERROR
                            == PHNFCSTATUS(error_status))
        {
            phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_ERROR, &comp_info);
        }
        else
        {
            psHciContext->error_status = error_status;
            status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
            if (NFCSTATUS_PENDING != status)
            {
                psHciContext->error_status = NFCSTATUS_SUCCESS;
            }
        }
    }
    return;
}



NFCSTATUS
phHciNfc_Resume_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                          )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    HCI_DEBUG("HCI: %s: cur_state=%02u, next_state=%02u",
        __FUNCTION__,
        psHciContext->hci_state.cur_state,
        psHciContext->hci_state.next_state);




    switch(psHciContext->hci_state.next_state)
    {
        /* Process the Admin Gate Response based on the HCI State */
        case hciState_Initialise:
        {
            switch (psHciContext->hci_state.cur_state)
            {
                /* Initialise State after Power on */
                case hciState_Reset:
                {
                    status = phHciNfc_Initialise_Sequence(psHciContext ,pHwRef);
                    break;
                }
                /* Initialise State after Power on */
                case hciState_Config:
                {
                    status = phHciNfc_Config_Sequence(psHciContext ,pHwRef);
                    break;
                }
                /* Discovery Resume after connect failure */
                case hciState_Initialise:
                case hciState_Select:
                case hciState_Connect:
                {
                    phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                    /* Update to the Intialise state as the discovery wheel is 
                     * restarted.
                     */
                    status = phHciNfc_FSM_Complete(psHciContext);

                    psHciContext->host_rf_type = phHal_eUnknown_DevType;
                    psHciContext->p_target_info = NULL;
                    psHciContext->p_xchg_info = NULL;

                    /* Initialisation Complete Notification to the Upper Layer */
                    comp_info.status = status;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                psHciContext->p_upper_context, pHwRef,
                                NFC_NOTIFY_POLL_RESTARTED , &comp_info);
                    HCI_PRINT(" HCI Remote Target Still Present in the Field. \n");
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
                    break;
                }
            }
            break;
        }
        case hciState_Release:
        {
            status = phHciNfc_Release_Sequence(psHciContext ,pHwRef);
            break;
        }
        case hciState_Config:
        {
            status = phHciNfc_Config_Sequence(psHciContext ,pHwRef);
            break;
        }
        case hciState_Listen:
        case hciState_Select:
        {
            status = phHciNfc_Info_Sequence( psHciContext, pHwRef );
            break;
        }
        case hciState_Reactivate:
        case hciState_Connect:
        {
            status = phHciNfc_Connect_Sequence( psHciContext, pHwRef );
            break;
        }
        case hciState_Transact:
        {
            status = phHciNfc_Transact_Sequence( 
                                            psHciContext, pHwRef );
            break;
        }
        case hciState_Presence:
        {
            phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

            /* Roll Back to Connect State as Presence Check is Complete */
            phHciNfc_FSM_Rollback(psHciContext);

            /* Initialisation Complete Notification to the Upper Layer */
            comp_info.status = NFCSTATUS_SUCCESS;
            phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_TARGET_PRESENT , &comp_info);
            HCI_PRINT(" HCI Remote Target Still Present in the Field. \n");
            break;
        }
        case hciState_Disconnect:
        {
            status = phHciNfc_Disconnect_Sequence( psHciContext, pHwRef );
            break;
        }
        case hciState_Test:
        {
            status = phHciNfc_Test_Sequence( psHciContext, pHwRef , status, NULL, 0 );
            break;
        }
        case hciState_IO:
        {
            status = phHciNfc_IO_Sequence( psHciContext, pHwRef , status, NULL, 0 );
            break;
        }
        case hciState_Unknown:
        {
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
            break;
        }
    } /* End of the Processing of HCI State*/

    return status;
}


NFCSTATUS
phHciNfc_Initialise_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    static  uint8_t             config = 0;

    PHNFC_UNUSED_VARIABLE(config);

    switch(psHciContext->hci_seq)
    {
        case ADMIN_INIT_SEQ:
        {
            status = phHciNfc_Admin_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
#ifdef ESTABLISH_SESSION
                if( hciMode_Session == psHciContext->hci_mode)
                {
                    /* TODO: Initialise Link Management 
                            Gate Resources */
                    NFCSTATUS info_status = NFCSTATUS_SUCCESS;
                    PHNFC_UNUSED_VARIABLE(info_status);
                    info_status = phHciNfc_IDMgmt_Update_Sequence( 
                                                psHciContext, INFO_SEQ );

                    if(NFCSTATUS_SUCCESS == info_status)
                    {
                        psHciContext->hci_seq = PL_STOP_SEQ;
                    }
                    else
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                        status = PHNFCSTVAL(CID_NFC_HCI,
                                    NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                }
                else
#endif
                {
                    psHciContext->hci_seq = LINK_MGMT_INIT_SEQ;
                }
            }
            break;
        }
        case LINK_MGMT_INIT_SEQ:
        {
            status = phHciNfc_LinkMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = IDENTITY_INIT_SEQ;
            }
            break;
        }
        case IDENTITY_INIT_SEQ:
        {
            status = phHciNfc_IDMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = DEV_INIT_SEQ;
            }
            break;
        }
        case DEV_INIT_SEQ:
        {
            status = phHciNfc_DevMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                if (HCI_SELF_TEST != psHciContext->init_mode)
                {
                    psHciContext->hci_seq = PL_INIT_SEQ;
                }
                else
                {
#if defined( ESTABLISH_SESSION )
                    NFCSTATUS info_status = NFCSTATUS_SUCCESS;
                    PHNFC_UNUSED_VARIABLE(info_status);
                    info_status = phHciNfc_IDMgmt_Update_Sequence( 
                                                psHciContext, INFO_SEQ );

                    if(NFCSTATUS_SUCCESS == info_status)
                    {
#if ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
                        psHciContext->hci_seq = DEV_HAL_INFO_SEQ;
#else
                        psHciContext->hci_seq = IDENTITY_INFO_SEQ;
#endif /* #if ( NXP_HAL_MEM_INFO_SIZE > 0x00U ) */
                    }
                    else
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                        status = PHNFCSTVAL(CID_NFC_HCI,
                                    NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
#elif ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
                    psHciContext->hci_seq = DEV_HAL_INFO_SEQ;
#else
                    psHciContext->hci_seq = HCI_END_SEQ;
#endif /* #ifdef ESTABLISH_SESSION */
                }

            }
            break;
        }
        case PL_INIT_SEQ:
        {
            status = phHciNfc_PollLoop_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                NFCSTATUS reset_status = NFCSTATUS_SUCCESS;
                PHNFC_UNUSED_VARIABLE(reset_status);
                reset_status = phHciNfc_ReaderMgmt_Update_Sequence(
                                                    psHciContext, RESET_SEQ );
                psHciContext->hci_seq = READER_MGMT_INIT_SEQ;
            }
            break;
        }
        case READER_MGMT_INIT_SEQ:
        {
            status = phHciNfc_ReaderMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                NFCSTATUS reset_status = NFCSTATUS_SUCCESS;
                PHNFC_UNUSED_VARIABLE(reset_status);
                reset_status =  phHciNfc_EmuMgmt_Update_Seq( 
                                                psHciContext, RESET_SEQ );
                psHciContext->hci_seq = EMULATION_INIT_SEQ;
            }
            break;
        }
        case EMULATION_INIT_SEQ:
        {
            status = phHciNfc_EmuMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
#if defined( ESTABLISH_SESSION )
                psHciContext->hci_seq = ADMIN_SESSION_SEQ;
#elif ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
                psHciContext->hci_seq = DEV_HAL_INFO_SEQ;
#else
                psHciContext->hci_seq = HCI_END_SEQ;
#endif
            }
            break;
        }
#ifdef ESTABLISH_SESSION
        case ADMIN_SESSION_SEQ:
        {
            status = phHciNfc_Admin_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
#if ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
                psHciContext->hci_seq = DEV_HAL_INFO_SEQ;
#else
                psHciContext->hci_seq = IDENTITY_INFO_SEQ;
#endif /* #if ( NXP_HAL_MEM_INFO_SIZE > 0x00U ) */
            }
            break;
        }
        case PL_STOP_SEQ:
        {
            status = phHciNfc_ReaderMgmt_Disable_Discovery( 
                                                psHciContext, pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
#if defined( SW_AUTO_ACTIVATION )
                psHciContext->hci_seq = READER_SW_AUTO_SEQ;
#elif ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
                psHciContext->hci_seq = DEV_HAL_INFO_SEQ;
#else
                psHciContext->hci_seq = IDENTITY_INFO_SEQ;
#endif /* #if ( NXP_HAL_MEM_INFO_SIZE > 0x00U ) */
            }
            break;
        }
#ifdef SW_AUTO_ACTIVATION
        case READER_SW_AUTO_SEQ:
        {
            uint8_t     activate_enable = FALSE;
            uint8_t     rdr_enable = TRUE;

            status = phHciNfc_ReaderA_Update_Info( 
                    psHciContext, HCI_READER_A_ENABLE, 
                                            &rdr_enable);
            if(status == NFCSTATUS_SUCCESS)
            {
                status = phHciNfc_ReaderA_Auto_Activate( psHciContext,
                                                pHwRef, activate_enable );
                if(status == NFCSTATUS_SUCCESS)
                {
                    psHciContext->hci_seq = IDENTITY_INFO_SEQ;
                }
            }
            break;
        }
#endif
        /* fall through */
        case IDENTITY_INFO_SEQ:
        {
            status = phHciNfc_IDMgmt_Info_Sequence( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                if ((HCI_SELF_TEST != psHciContext->init_mode)
                    /* && ( TRUE == ((phHal_sHwReference_t *)pHwRef)->se_detect ) */
                    && (HCI_CUSTOM_INIT != psHciContext->init_mode)
                    && (HCI_NFC_DEVICE_TEST != psHciContext->init_mode))
                {
                    NFCSTATUS info_status = NFCSTATUS_SUCCESS;
                    PHNFC_UNUSED_VARIABLE(info_status);
                    info_status = phHciNfc_EmuMgmt_Update_Seq( 
                                                psHciContext, INFO_SEQ );

                    if(NFCSTATUS_SUCCESS == info_status)
                    {
                        psHciContext->hci_seq = EMULATION_SWP_SEQ;
                    }
                }
                else
                {
                    psHciContext->hci_seq = HCI_END_SEQ;
                }
            }
            break;
        }
        case EMULATION_SWP_SEQ:
        {
            status = phHciNfc_EmuMgmt_Initialise( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = HCI_END_SEQ;
            }
            break;
        }
#endif /* #ifdef ESTABLISH_SESSION */

#if ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
        case DEV_HAL_INFO_SEQ:
        {
            static uint8_t      mem_index = 0;
            status = phHciNfc_DevMgmt_Get_Info(psHciContext, pHwRef,
                        (NFC_ADDRESS_HAL_CONF + mem_index),
                            (psHciContext->hal_mem_info + mem_index));
            if(NFCSTATUS_PENDING == status)
            {
                mem_index++;
                if (NXP_HAL_MEM_INFO_SIZE <= mem_index )
                {
                    NFCSTATUS info_status = NFCSTATUS_SUCCESS;
                    PHNFC_UNUSED_VARIABLE(info_status);
                    info_status = phHciNfc_IDMgmt_Update_Sequence(
                                                psHciContext, INFO_SEQ );
                    mem_index = 0;
                    psHciContext->hci_seq = IDENTITY_INFO_SEQ;
                    /* psHciContext->hci_seq =
                            (HCI_SELF_TEST != psHciContext->init_mode)?
                                    IDENTITY_INFO_SEQ : HCI_END_SEQ; */
                }
            }
            break;
        }
#endif /* #if ( NXP_HAL_MEM_INFO_SIZE > 0x00U ) */
        case HCI_END_SEQ:
        {
            phHal_sMemInfo_t    *p_mem_info =
                    (phHal_sMemInfo_t *) ( psHciContext->hal_mem_info );
            if (
                (HCI_SELF_TEST == psHciContext->init_mode )
                || (HCI_NFC_DEVICE_TEST == psHciContext->init_mode )
                )
            {
                psHciContext->hci_state.next_state
                    = (uint8_t) hciState_Test;
            }
            status = phHciNfc_FSM_Complete ( psHciContext );
#ifdef UICC_CONNECTIVITY_PATCH
            phHciNfc_Uicc_Connectivity( psHciContext, pHwRef );
#endif /* #ifdef UICC_CONNECTIVITY_PATCH */

#if ( NXP_HAL_MEM_INFO_SIZE > 0x00U )
            if(NXP_FW_UPLOAD_SUCCESS != p_mem_info->fw_magic )
            {
                status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_FAILED );
            }
#endif /* #if ( NXP_HAL_MEM_INFO_SIZE > 0x00U ) */

            /* Initialisation Complete Notification to the Upper Layer */
            if(NFCSTATUS_SUCCESS == status)
            {
                phNfc_sCompletionInfo_t     comp_info={FALSE,0, NULL};

                comp_info.status = status;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                                NFC_NOTIFY_INIT_COMPLETED, &comp_info);
                HCI_PRINT("HCI Initialisation Completed \n");
            }
            else
            {
                pphNfcIF_Notification_CB_t  p_upper_notify = psHciContext->p_upper_notify;
                void                        *pcontext = psHciContext->p_upper_context;
                phNfc_sCompletionInfo_t     comp_info;


                phHciNfc_Release_Lower( psHciContext, pHwRef );
                phHciNfc_Release_Resources( &psHciContext );
                 /* Notify the Failure to the Upper Layer */
                comp_info.status = status;
                phHciNfc_Notify( p_upper_notify, pcontext, pHwRef,
                                NFC_NOTIFY_INIT_FAILED, &comp_info);
                HCI_PRINT("HCI FSM Initialisation Error \n");
            }
            break;
        }
        default:
            break;
    }

    return status;
}


NFCSTATUS
phHciNfc_Release_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    switch(psHciContext->hci_seq)
    {
        case PL_STOP_SEQ:
        {
            status = phHciNfc_ReaderMgmt_Disable_Discovery( 
                                                psHciContext, pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                (void)phHciNfc_EmuMgmt_Update_Seq( 
                                    psHciContext, REL_SEQ );
                psHciContext->hci_seq = EMULATION_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case EMULATION_REL_SEQ:
        {
            status = phHciNfc_EmuMgmt_Release( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                (void)phHciNfc_DevMgmt_Update_Sequence( 
                                    psHciContext, REL_SEQ );
                psHciContext->hci_seq = ADMIN_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case DEV_REL_SEQ:
        {
            NFCSTATUS info_status = NFCSTATUS_SUCCESS;
            PHNFC_UNUSED_VARIABLE(info_status);
            info_status = phHciNfc_DevMgmt_Update_Sequence(
                                            psHciContext, REL_SEQ );
            status = phHciNfc_DevMgmt_Release( psHciContext, pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = HCI_END_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case READER_MGMT_REL_SEQ:
        {
            status = phHciNfc_ReaderMgmt_Release( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = PL_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case PL_REL_SEQ:
        {
            status = phHciNfc_PollLoop_Release( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = IDENTITY_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case IDENTITY_REL_SEQ:
        {
            status = phHciNfc_IDMgmt_Release( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = LINK_MGMT_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case LINK_MGMT_REL_SEQ:
        {
            status = phHciNfc_LinkMgmt_Release( psHciContext,pHwRef );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = ADMIN_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case ADMIN_REL_SEQ:
        {
            /*  Admin Management Release Sequence */
            status = phHciNfc_Admin_Release( psHciContext,pHwRef, phHciNfc_TerminalHostID );
            if(NFCSTATUS_SUCCESS == status)
            {
                psHciContext->hci_seq = DEV_REL_SEQ;
                status = NFCSTATUS_PENDING;
            }
            break;
        }
        case HCI_END_SEQ:
        {
            pphNfcIF_Notification_CB_t  p_upper_notify = 
                                            psHciContext->p_upper_notify;
            phNfc_sLowerIF_t            *plower_if = 
                                            &(psHciContext->lower_interface);
            void                        *pcontext = 
                                                psHciContext->p_upper_context;
            phNfc_sCompletionInfo_t     comp_info;


            status = plower_if->release((void *)plower_if->pcontext,
                                            (void *)pHwRef);

            phHciNfc_Release_Resources( &psHciContext );
            /* De-Initialisation Complete Notification to the Upper Layer */
            comp_info.status = status;
            phHciNfc_Notify(p_upper_notify, pcontext, pHwRef,
                                    NFC_NOTIFY_DEINIT_COMPLETED, &comp_info);

            HCI_PRINT("HCI Release Completed \n");
            break;
        }
        default:
        {
            /* psHciContext->hci_seq = HCI_END_SEQ; */
            break;
        }
    }

    return status;
}


static
NFCSTATUS
phHciNfc_Config_Sequence(
                           phHciNfc_sContext_t      *psHciContext,
                           void                 *pHwRef
                        )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phNfc_sCompletionInfo_t     comp_info = {FALSE,0,NULL};

    switch(psHciContext->config_type)
    {
        case POLL_LOOP_CFG:
        {
            status = phHciNfc_PollLoop_Sequence( psHciContext, pHwRef );
            break;
        }
        case SMX_WI_MODE:
        {
            status = phHciNfc_SmartMx_Mode_Sequence( psHciContext, pHwRef );
            break;
        }
#ifdef ENABLE_P2P
        case NFC_GENERAL_CFG:
        {
            if(TARGET_GENERAL_SEQ == psHciContext->hci_seq)
            {
                status = phHciNfc_NfcIP_SetATRInfo( psHciContext,
                                        pHwRef, NFCIP_TARGET, 
                                        psHciContext->p_config_params);
                    if( NFCSTATUS_PENDING != status )
                    {
                        /* Roll Back the State Machine to its Original State */
                        phHciNfc_FSM_Rollback ( psHciContext );
                    }
                    else
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                    }
            }
            else
            {
                status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                if (NFCSTATUS_PENDING != status)
                {
                    /* Roll Back to its Current State as Configuration is Complete */
                    phHciNfc_FSM_Rollback(psHciContext); 

                    HCI_PRINT(" NFC-IP(P2P) Configuration Completed. \n");
                    comp_info.status = status;
                    psHciContext->error_status = NFCSTATUS_SUCCESS;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                    psHciContext->p_upper_context, pHwRef,
                                        NFC_NOTIFY_CONFIG_SUCCESS , &comp_info);
                }
            }
            break;
        }
#endif
        case SWP_PROTECT_CFG:
        case SWP_EVT_CFG:
        case SMX_WI_CFG:
        {
            /* Roll Back to its Current State as Configuration is Complete */
            phHciNfc_FSM_Rollback(psHciContext); 

            HCI_DEBUG(" %s Configuration Completed. \n",
                  ((SMX_WI_CFG == psHciContext->config_type)?
                        "SmartMX" : "SWP Event/Protection"));

            comp_info.status = status;
            phHciNfc_Notify(psHciContext->p_upper_notify,
                                psHciContext->p_upper_context, pHwRef,
                                NFC_NOTIFY_CONFIG_SUCCESS, &comp_info);
            break;
        }
        case NFC_TARGET_CFG:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
            break;
        }
        case UICC_SWP_CFG:
#if 0
        {
            phHal_sEmulationCfg_t   *p_emulation_cfg = 
                                        (phHal_sEmulationCfg_t * )
                                                psHciContext->p_config_params;
            if (NULL != p_emulation_cfg)
            {
                phHal_sUiccEmuCfg_t   *uicc_config = 
                                    &p_emulation_cfg->config.uiccEmuCfg;
                if( TRUE == uicc_config->enableUicc )
                {
                    status = phHciNfc_Uicc_Connect_Status(psHciContext,pHwRef);
                    if( NFCSTATUS_PENDING == status )
                    {
                        break;
                    } /* Or Else Fall through to notify the above layer */
                }
            }
        }
#endif
        /* fall through */
        case NFC_CE_A_CFG:
        case NFC_CE_B_CFG:
        {
            status = phHciNfc_EmulationCfg_Sequence( psHciContext, pHwRef );
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);       
            break;
        }
    }
    
    return status;
}


NFCSTATUS
phHciNfc_PollLoop_Sequence(
                           phHciNfc_sContext_t      *psHciContext,
                           void                 *pHwRef
                           )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    phHal_sADD_Cfg_t    *p_poll_config = (phHal_sADD_Cfg_t * )
        psHciContext->p_config_params;
    if (NULL != p_poll_config)
    {
        uint8_t speed = 
                p_poll_config->NfcIP_Mode;
        uint8_t targetSpeed =
                p_poll_config->NfcIP_Target_Mode;
        switch(psHciContext->hci_seq)
        {
            case PL_DURATION_SEQ:
            {
                status = phHciNfc_PollLoop_Cfg( psHciContext, pHwRef,
                    (uint8_t)PL_DURATION , NULL);
                if(NFCSTATUS_SUCCESS == status)
                {
#if defined (ENABLE_P2P) && defined (TARGET_SPEED)
                    psHciContext->hci_seq = TARGET_SPEED_SEQ;
#elif defined (ENABLE_P2P) && defined (INITIATOR_SPEED)
                    psHciContext->hci_seq = INITIATOR_SPEED_SEQ;
#elif defined (ENABLE_P2P) &&  defined (NFCIP_TGT_DISABLE_CFG)
                    psHciContext->hci_seq = PL_TGT_DISABLE_SEQ;
#else
                    psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;  
#endif

                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#if defined (ENABLE_P2P) && defined (TARGET_SPEED)
            case TARGET_SPEED_SEQ:
            {
#define NFCIP_ACTIVE_SHIFT	0x03U
#define NFCIP_PASSIVE_MASK	0x07U
                uint8_t mode = targetSpeed;
                HCI_DEBUG("Setting target mode to 0x%02X", mode);
                status = 
                    phHciNfc_NfcIP_SetMode( psHciContext, pHwRef, NFCIP_TARGET,
                     (uint8_t) mode );
                if(NFCSTATUS_PENDING == status)
                {
#if defined (INITIATOR_SPEED)
                    psHciContext->hci_seq = INITIATOR_SPEED_SEQ;
#elif defined (NFCIP_TGT_DISABLE_CFG)
                    psHciContext->hci_seq = PL_TGT_DISABLE_SEQ;
#else
                    psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;  
#endif
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#endif
#if defined (ENABLE_P2P) && defined (INITIATOR_SPEED)
            case INITIATOR_SPEED_SEQ:
            {
                HCI_DEBUG("Setting initiator mode to 0x%02X", speed);
                status = 
                    phHciNfc_NfcIP_SetMode( psHciContext, pHwRef, NFCIP_INITIATOR,
                     (uint8_t) (speed & DEFAULT_NFCIP_INITIATOR_MODE_SUPPORT));
                if(NFCSTATUS_PENDING == status)
                {
#if defined (NFCIP_TGT_DISABLE_CFG)
                    psHciContext->hci_seq = PL_TGT_DISABLE_SEQ;
#else
                    psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;  
#endif
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#endif
#if defined (ENABLE_P2P) && defined (NFCIP_TGT_DISABLE_CFG)
            case PL_TGT_DISABLE_SEQ:
            {
                /* Configure the Polling Loop Target Disable Parameter */
                status = phHciNfc_PollLoop_Cfg( psHciContext, pHwRef,
                (uint8_t)PL_DISABLE_TARGET, &p_poll_config->NfcIP_Tgt_Disable );
                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;  
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#endif
            case PL_CONFIG_PHASE_SEQ:
            {
                phHal_sPollDevInfo_t *p_poll_info = 
                                &(p_poll_config->PollDevInfo.PollCfgInfo);

                p_poll_info->EnableIso14443A = 
                    ( (p_poll_info->EnableIso14443A)
                                    || ( speed & (uint8_t)phHal_ePassive106 ) 
                                    );
                p_poll_info->EnableFelica212 = 
                            ( (p_poll_info->EnableFelica212)
                                    || ( speed & (uint8_t)phHal_ePassive212 ) 
                                    );
                p_poll_info->EnableFelica424 = 
                            ( (p_poll_info->EnableFelica424)
                                    || ( speed & (uint8_t)phHal_ePassive424 ) 
                                    );
                /* Configure the Polling Loop Gate Parameters */
                status = phHciNfc_PollLoop_Cfg( psHciContext, pHwRef,
                    (uint8_t)PL_RD_PHASES, NULL );
                if(NFCSTATUS_SUCCESS == status)
                {
                    if(((~(PL_RD_PHASES_DISABLE)) & 
                            p_poll_config->PollDevInfo.PollEnabled)!= 0)
                    {
                        psHciContext->hci_seq = READER_ENABLE_SEQ;
                    }
                    else
                    {
                        /* psHciContext->hci_seq = READER_DISABLE_SEQ; */
                        psHciContext->hci_seq = HCI_END_SEQ;
                    }
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case READER_ENABLE_SEQ:
            {
                status = 
                    phHciNfc_ReaderMgmt_Enable_Discovery( 
                    psHciContext, pHwRef );
                if(NFCSTATUS_SUCCESS == status)
                {
                    /* psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ; */
                    psHciContext->hci_seq = HCI_END_SEQ;  
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case READER_DISABLE_SEQ:
            {
                status = phHciNfc_ReaderMgmt_Disable_Discovery( 
                    psHciContext, pHwRef );

                if(NFCSTATUS_SUCCESS == status)
                {
                    if((~(PL_RD_PHASES_DISABLE) & 
                            p_poll_config->PollDevInfo.PollEnabled)!= 0)
                    {
                        psHciContext->hci_seq = PL_DURATION_SEQ;
                    }
                    else
                    {
#if defined (ENABLE_P2P) && defined (INITIATOR_SPEED)
                        psHciContext->hci_seq = INITIATOR_SPEED_SEQ;
#elif defined (ENABLE_P2P) &&  defined (NFCIP_TGT_DISABLE_CFG)
                        psHciContext->hci_seq = PL_TGT_DISABLE_SEQ;
#else
                        psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;  
#endif
                        /* psHciContext->hci_seq = HCI_END_SEQ; */
                    }
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case HCI_END_SEQ:
            {
                phNfc_sCompletionInfo_t     comp_info;
                status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                if (NFCSTATUS_PENDING != status)
                {
                    /* status = phHciNfc_FSM_Complete ( psHciContext );*/
                    phHciNfc_FSM_Rollback ( psHciContext );
                    /* Poll Configuration Notification to the Upper Layer */
                    if((~(PL_RD_PHASES_DISABLE) & 
                            p_poll_config->PollDevInfo.PollEnabled)!= 0)
                    {
                        comp_info.status = status;
                        phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_POLL_ENABLED, &comp_info);
                    } 
                    else
                    {
                        comp_info.status = status;
                        phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_POLL_DISABLED, &comp_info);
                    }
                    HCI_PRINT("HCI Discovery Configuration Completed \n");
                }
                break;
            }
            default:
            {
                /* psHciContext->hci_seq = HCI_END_SEQ; */
                break;
            }
        }/* End of the Poll Sequence Switch */
    }/* End of the Poll Config info Check */

    return status;
}


NFCSTATUS
phHciNfc_EmulationCfg_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static phNfc_sCompletionInfo_t      comp_info = {FALSE,0,NULL};
#if defined(HOST_EMULATION)
    phHciNfc_GateID_t       ce_gate = phHciNfc_UnknownGate;
#endif  /* #ifdef HOST_EMULATION */
    phHal_sEmulationCfg_t   *p_emulation_cfg = (phHal_sEmulationCfg_t * )
                                        psHciContext->p_config_params;
#ifdef UICC_SESSION_RESET
    uint8_t                 uicc_clear_pipes = FALSE;
#endif


    if (NULL != p_emulation_cfg)
    {
#if defined(HOST_EMULATION)
        if(NFC_HOST_CE_A_EMULATION == p_emulation_cfg->emuType)
        {
            psHciContext->config_type = NFC_CE_A_CFG;
            if (NULL == psHciContext->p_ce_a_info)
            {
                ce_gate = phHciNfc_CETypeAGate;
            }
        }
        else if (NFC_HOST_CE_B_EMULATION == p_emulation_cfg->emuType)
        {
            psHciContext->config_type = NFC_CE_B_CFG;
            if (NULL == psHciContext->p_ce_b_info)
            {
                ce_gate = phHciNfc_CETypeBGate;
            }
        }
#ifdef UICC_SESSION_RESET
        else if ((NFC_UICC_EMULATION == p_emulation_cfg->emuType)
            &&(FALSE == p_emulation_cfg->config.uiccEmuCfg.enableUicc)
            )
        {
            uicc_clear_pipes = TRUE;
        }
#endif
        else
        {
            ;
        }
#endif  /* #ifdef HOST_EMULATION */

        switch(psHciContext->hci_seq)
        {
#if defined(HOST_EMULATION)
            case ADMIN_CE_SEQ:
            {
                if(phHciNfc_UnknownGate != ce_gate)
                {
                    status = phHciNfc_Admin_CE_Init(psHciContext, pHwRef, ce_gate);
                }
                else
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_NOT_ALLOWED);
                }

                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = EMULATION_CONFIG_SEQ;
                    /* psHciContext->hci_seq = HCI_END_SEQ; */
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#endif
            case EMULATION_CONFIG_SEQ:
            {
                status = phHciNfc_Emulation_Cfg(psHciContext, pHwRef, 
                                                psHciContext->config_type);
                if(NFCSTATUS_SUCCESS == status)
                {
                    /* psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ; */
#ifdef UICC_SESSION_RESET
                    if(UICC_SWP_CFG == psHciContext->config_type)
                    {
                        psHciContext->hci_seq = ADMIN_REL_SEQ;
                    }
                    else
#endif /* UICC_SESSION_RESET */
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                    }
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
#ifdef UICC_SESSION_RESET
            case ADMIN_REL_SEQ:
            {
                if (TRUE == uicc_clear_pipes)
                {
                    /*  Admin Management UICC Release Sequence */
                    status = phHciNfc_Admin_Release( psHciContext,pHwRef, phHciNfc_UICCHostID );
                    if(NFCSTATUS_SUCCESS == status)
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                        if (UICC_SWP_CFG == psHciContext->config_type)
                        {
                            (void)phHciNfc_SWP_Update_Sequence(psHciContext, 
                                                                        CONFIG_SEQ );
                        }
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
            }
#endif /* UICC_SESSION_RESET */
            /* fall through */
            case HCI_END_SEQ:
            {
                phHciNfc_FSM_Rollback(psHciContext); 

                HCI_PRINT(" Emulation Configuration Completed. \n");

                comp_info.status = status;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                                 psHciContext->p_upper_context, pHwRef,
                                   NFC_NOTIFY_CONFIG_SUCCESS, &comp_info);
                break;
            }
            default:
            {
                /* psHciContext->hci_seq = HCI_END_SEQ; */
                break;
            }
        }
        /*
            NFC_CE_A_CFG;
            NFC_CE_B_CFG; */

    }/* End of the Emulation Config info Check */
    
    return status;
}


NFCSTATUS
phHciNfc_SmartMx_Mode_Sequence(
                           phHciNfc_sContext_t      *psHciContext,
                           void                     *pHwRef
                          )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    phHal_sADD_Cfg_t    *p_poll_config = (phHal_sADD_Cfg_t * )
        psHciContext->p_config_params;
    phNfc_sCompletionInfo_t     comp_info = {FALSE,0,NULL};
    if (NULL != p_poll_config)
    {
        switch(psHciContext->hci_seq)
        {
            case READER_DISABLE_SEQ:
            {
                status = phHciNfc_ReaderMgmt_Disable_Discovery( 
                    psHciContext, pHwRef );
                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = EMULATION_CONFIG_SEQ;
                    /* psHciContext->hci_seq = HCI_END_SEQ; */
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case EMULATION_CONFIG_SEQ:
            {
                status = phHciNfc_WI_Configure_Mode( 
                    psHciContext, pHwRef,psHciContext->smx_mode );
                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ;
                    /* psHciContext->hci_seq = HCI_END_SEQ; */
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case PL_CONFIG_PHASE_SEQ:
            {
                /* Configure the Polling Loop Gate Parameters */
                status = phHciNfc_PollLoop_Cfg( psHciContext, pHwRef,
                    (uint8_t)PL_RD_PHASES, NULL );
                if(NFCSTATUS_SUCCESS == status)
                {
                    psHciContext->hci_seq = READER_ENABLE_SEQ;
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case READER_ENABLE_SEQ:
            {
                status = 
                    phHciNfc_ReaderMgmt_Enable_Discovery( 
                    psHciContext, pHwRef );
                if(NFCSTATUS_SUCCESS == status)
                {
                    /* psHciContext->hci_seq = PL_CONFIG_PHASE_SEQ; */
                    psHciContext->hci_seq = HCI_END_SEQ;  
                    status = NFCSTATUS_PENDING;
                }
                break;
            }
            case HCI_END_SEQ:
            {
                status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
                if (NFCSTATUS_PENDING != status)
                {
                    /* status = phHciNfc_FSM_Complete ( psHciContext );*/
                    phHciNfc_FSM_Rollback ( psHciContext );
                    if( hciState_Disconnect == psHciContext->hci_state.cur_state)
                    {
                        psHciContext->host_rf_type = phHal_eUnknown_DevType;
                        psHciContext->p_target_info = NULL;
                        psHciContext->p_xchg_info = NULL;
                    }
                    /* Poll Configuration Notification to the Upper Layer */
                    if((~(PL_RD_PHASES_DISABLE) & 
                            p_poll_config->PollDevInfo.PollEnabled)!= 0)
                    {
                        comp_info.status = status;
                        phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_POLL_ENABLED, &comp_info);
                    } 
                    else
                    {
                        comp_info.status = status;
                        phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_POLL_DISABLED, &comp_info);
                    }
                    HCI_PRINT("HCI Discovery Configuration Completed \n");
                }
                break;
            }
            default:
            {
                /* psHciContext->hci_seq = HCI_END_SEQ; */
                break;
            }
        }/* End of the Poll Sequence Switch */
    }/* End of the Poll Config info Check */

    return status;
}


NFCSTATUS
phHciNfc_Connect_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    static phNfc_sCompletionInfo_t      comp_info = {FALSE,0,NULL};
    phHal_eRemDevType_t         target_type = phHal_eUnknown_DevType;

    if( NULL != psHciContext->p_target_info )
    {

        target_type = psHciContext->p_target_info->RemDevType;
        switch(psHciContext->hci_seq)
        {
            case READER_REACTIVATE_SEQ:
            {
                /* Complete the Reactivate Sequence and notify the HAL */
                status = phHciNfc_FSM_Complete ( psHciContext );
                /* Reactivate Complete Notification to the Upper Layer */
                if(NFCSTATUS_SUCCESS == status)
                {
                    comp_info.status = status;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                    psHciContext->p_upper_context, pHwRef,
                                    NFC_NOTIFY_TARGET_REACTIVATED , &comp_info);
                    HCI_PRINT(" HCI Remote Target Reactivated. \n");
                }
                else
                {
                    comp_info.status = status;
                    phHciNfc_FSM_Rollback ( psHciContext );
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_ERROR , &comp_info);
                    HCI_PRINT("HCI FSM Invalid Selection State \n");
                    HCI_PRINT("HCI Remote Target Reactivation Failed \n");
                }
                break;
            }
            case READER_SELECT_SEQ:
            {
                /* If the Target is Mifare then it should fall through */
                if(( phHal_eMifare_PICC != target_type ) 
                    &&(phHal_eISO14443_3A_PICC != target_type)
#ifdef TYPE_B
                    &&  ( phHal_eISO14443_B_PICC != target_type )
                    &&  ( phHal_eISO14443_4B_PICC != target_type )
#endif
#ifdef TYPE_FELICA
                    &&  ( phHal_eFelica_PICC != target_type )
#endif
#ifdef TYPE_JEWEL
                    &&  ( phHal_eJewel_PICC != target_type )
#endif /* #ifdef TYPE_JEWEL */
#ifdef TYPE_ISO15693
                    &&  ( phHal_eISO15693_PICC != target_type )
#endif /* #ifdef TYPE_ISO15693 */

                    )
                {
                    status = phHciNfc_ReaderMgmt_Info_Sequence( psHciContext, pHwRef );
                    if(NFCSTATUS_SUCCESS == status)
                    {
                        psHciContext->hci_seq = HCI_END_SEQ;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
            }
            /* fall through */
            case HCI_END_SEQ:
            {
                /* Complete the Connect Sequence and notify the HAL */
                status = phHciNfc_FSM_Complete ( psHciContext );
                /* Connection Complete Notification to the Upper Layer */
                if(NFCSTATUS_SUCCESS == status)
                {
                    /* Invalidate the previously polled RF Reader Type */
                    /* psHciContext->host_rf_type = phHal_eInvalidRFType;*/
                    comp_info.status = status;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                    psHciContext->p_upper_context, pHwRef,
                                    NFC_NOTIFY_TARGET_CONNECTED , &comp_info);
                    HCI_PRINT(" HCI Remote Target Selected for Transaction. \n");
                }
                else
                {
                    comp_info.status = status;
                    /* phHciNfc_FSM_Rollback ( psHciContext ); */
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_ERROR , &comp_info);
                    HCI_PRINT("HCI FSM Invalid Selection State \n");
                    HCI_PRINT("HCI Remote Target Selection Failed \n");
                }
                break;
            }
            default:
            {
                HCI_PRINT("\t Invalid HCI Connect Sequence \n");
                /* psHciContext->hci_seq = HCI_END_SEQ; */
                break;
            }
        }/* End of the Connect Sequence Switch */
    }

    return status;
}


NFCSTATUS
phHciNfc_Disconnect_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    static phNfc_sCompletionInfo_t      comp_info = {FALSE, 0 , NULL};
    phHal_eRemDevType_t         target_type = phHal_eUnknown_DevType;
    uint8_t             re_poll = 0;

    if( NULL != psHciContext->p_target_info )
    {

        target_type = psHciContext->p_target_info->RemDevType;
        switch(psHciContext->hci_seq)
        {
            case READER_UICC_DISPATCH_SEQ:
            {
                status = phHciNfc_ReaderMgmt_UICC_Dispatch( 
                                psHciContext, pHwRef, target_type );
                psHciContext->hci_seq = READER_DESELECT_SEQ;
                if(NFCSTATUS_PENDING == status)
                {
                    break;
                }
            }
            /* fall through */
            case READER_DESELECT_SEQ:
            {
                re_poll = (uint8_t) ( NULL != psHciContext->p_config_params )?
                                *((uint8_t *)psHciContext->p_config_params):FALSE;
                status = phHciNfc_ReaderMgmt_Deselect( 
                                psHciContext, pHwRef, target_type, re_poll);
                if(NFCSTATUS_PENDING == status)
                {
                    psHciContext->hci_seq = HCI_END_SEQ;
                    psHciContext->p_config_params = NULL;
                }
                break;
            }
            case HCI_END_SEQ:
            {
                /* Complete the Disconnect Sequence and notify the HAL */
                status = phHciNfc_FSM_Complete ( psHciContext );
                /* Disconnect Notification to the Upper Layer */
                if(NFCSTATUS_SUCCESS == status)
                {
                    /* Invalidate the previously polled RF Reader Type */
                    psHciContext->host_rf_type = phHal_eUnknown_DevType;
                    psHciContext->p_target_info = NULL;
                    psHciContext->p_xchg_info = NULL;
                    comp_info.status = status;
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                    psHciContext->p_upper_context, pHwRef,
                                    NFC_NOTIFY_TARGET_DISCONNECTED , &comp_info);
                    HCI_PRINT(" HCI Remote Target De-Selected. \n");
                }
                else
                {
                    comp_info.status = status;
                    /* phHciNfc_FSM_Rollback ( psHciContext ); */
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_ERROR , &comp_info);
                    HCI_PRINT("HCI FSM Invalid De-Selection State \n");
                    HCI_PRINT("HCI Remote Target De-Selection Failed \n");
                }

                break;
            }
            default:
            {
                HCI_PRINT("\t Invalid HCI Connect Sequence \n");
                /* psHciContext->hci_seq = HCI_END_SEQ; */
                break;
            }
        }/* End of the Connect Sequence Switch */
    }

    return status;
}


static
NFCSTATUS
phHciNfc_Transact_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    static phNfc_sTransactionInfo_t transact_info = {FALSE,0,NULL,NULL,0};

    pphNfcIF_Notification_CB_t  p_upper_notify = psHciContext->p_upper_notify;
    void                        *pcontext = psHciContext->p_upper_context;
    uint8_t                     transact_result = NFC_NOTIFY_ERROR;

    /* Roll Back to Connect State as Transceive is Complete */
    phHciNfc_FSM_Rollback(psHciContext); 

    switch (psHciContext->host_rf_type)
    {
        case phHal_eISO14443_A_PCD:
#ifdef TYPE_B
        case phHal_eISO14443_B_PCD:
#endif
        case phHal_eISO14443_BPrime_PCD:
#ifdef TYPE_FELICA
        case phHal_eFelica_PCD:
#endif
#ifdef TYPE_ISO15693
        case phHal_eISO15693_PCD:
#endif
        {
            if(ZERO != psHciContext->rx_index)
            {
                transact_info.status = NFCSTATUS_SUCCESS;
                transact_info.buffer = 
                                &psHciContext->recv_buffer[psHciContext->rx_index];
                transact_info.length = 
                                psHciContext->rx_total - psHciContext->rx_index;
                transact_result = NFC_NOTIFY_TRANSCEIVE_COMPLETED;
            }
            else
            {
                transact_info.status = NFCSTATUS_FAILED;
                transact_result = NFC_NOTIFY_TRANSCEIVE_ERROR;
            }
            HCI_PRINT(" HCI Transceive operation Completed. \n");
            psHciContext->p_xchg_info = NULL ;
            break;
        }
#ifdef TYPE_JEWEL
        /* fall through */
        case phHal_eJewel_PCD:
#endif
        {
            transact_info.status = NFCSTATUS_SUCCESS;
            transact_info.buffer = 
                            &psHciContext->recv_buffer[psHciContext->rx_index];
            transact_info.length = 
                            psHciContext->rx_total - psHciContext->rx_index;
            transact_result = NFC_NOTIFY_TRANSCEIVE_COMPLETED;
            HCI_PRINT(" HCI Transceive operation Completed. \n");
            psHciContext->p_xchg_info = NULL ;
            break;
        }
#if defined(ENABLE_P2P)
        case phHal_eNfcIP1_Initiator:
        case phHal_eNfcIP1_Target:
#endif
        {
            HCI_PRINT(" HCI Send operation Completed. \n");
            transact_info.status = NFCSTATUS_SUCCESS;
            transact_result = NFC_NOTIFY_SEND_COMPLETED;
            break;
        }
        case phHal_eUnknown_DevType:
        default:
        {
            transact_info.status  = 
                        PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
            break;
        }

    }
    /* Notify the Transceive Completion to the Upper layer */
    phHciNfc_Notify( p_upper_notify, pcontext , pHwRef,
                    transact_result, &transact_info);

    return (NFCSTATUS)NFCSTATUS_SUCCESS;
}

static
NFCSTATUS
phHciNfc_Info_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    HCI_DEBUG(" HCI: Info Sequence Entry --> Reader Type : %02X \n",
                                            psHciContext->host_rf_type);
    switch (psHciContext->host_rf_type)
    {
        case phHal_eISO14443_A_PCD:
#ifdef TYPE_B
        case phHal_eISO14443_B_PCD:
#endif
        case phHal_eISO14443_BPrime_PCD:
#ifdef TYPE_FELICA
        case phHal_eFelica_PCD:
#endif
#ifdef TYPE_JEWEL
        case phHal_eJewel_PCD:
#endif
#ifdef TYPE_ISO15693
        case phHal_eISO15693_PCD:
#endif
        {
            /* To update the select sequence to retrieve
            * the target information using the reader type.
            */
            status = phHciNfc_ReaderMgmt_Info_Sequence( psHciContext, pHwRef );
            break;
        }
#if defined(ENABLE_P2P)
        case phHal_eNfcIP1_Initiator:
        case phHal_eNfcIP1_Target:
        {
            status = phHciNfc_NfcIP_Info_Sequence( psHciContext, pHwRef );
            break;
        }
#endif
        case phHal_eUnknown_DevType:
        default:
        {
            status  = 
                    PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
            break;
        }

    }
    return status;
}

static
NFCSTATUS
phHciNfc_Test_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            NFCSTATUS               test_status,
                            uint8_t                 *pdata,
                            uint8_t                 length
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    static phNfc_sCompletionInfo_t      comp_info = {0};
    static phNfc_sData_t test_result= {NULL,0};

    /* Complete the Test Sequence and notify the HAL */
    status = phHciNfc_FSM_Complete ( psHciContext );
    /* Test Results to the Upper Layer */
    if(NFCSTATUS_SUCCESS == status)
    {
        comp_info.status = test_status;
        if ( (NULL != pdata) && (length >= HCP_HEADER_LEN) )
        {
            test_result.buffer = ( pdata + HCP_HEADER_LEN);
            test_result.length = length - HCP_HEADER_LEN;
        }
        else
        {
            status = phHciNfc_DevMgmt_Get_Test_Result( 
                                        psHciContext, &test_result );
        }
        comp_info.info = &test_result;
        phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_NOTIFY_RESULT , &comp_info);
        HCI_DEBUG(" HCI System Test Completed : Status = %u\n", test_status);
    }
    else
    {
        comp_info.status = status;
        phHciNfc_FSM_Rollback ( psHciContext );
        phHciNfc_Notify(psHciContext->p_upper_notify,
            psHciContext->p_upper_context, pHwRef,
            NFC_NOTIFY_ERROR , &comp_info);
        HCI_PRINT("HCI FSM Invalid Test State \n");
    }

    return status;
}

static
NFCSTATUS
phHciNfc_IO_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            NFCSTATUS               io_status,
                            uint8_t                 *pdata,
                            uint8_t                 length
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    static phNfc_sCompletionInfo_t      comp_info = {0};

    /* To remove "warning (VS 4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(pdata);
    PHNFC_UNUSED_VARIABLE(length);
    /* Complete the Test Sequence and notify the HAL */
    phHciNfc_FSM_Rollback ( psHciContext );
    /* Test Results to the Upper Layer */
    comp_info.status = io_status;
    status = phHciNfc_Pending_Sequence(psHciContext, pHwRef );
    if(NFCSTATUS_SUCCESS == io_status)
    {
        phHciNfc_Notify(psHciContext->p_upper_notify,
                        psHciContext->p_upper_context, pHwRef,
                        NFC_IO_SUCCESS , &comp_info);
        HCI_PRINT(" HCI System IO Successful. \n");
    }
    else
    {
        phHciNfc_Notify(psHciContext->p_upper_notify,
            psHciContext->p_upper_context, pHwRef,
            NFC_IO_ERROR , &comp_info);
        HCI_PRINT("HCI IO Error \n");
    }
    return status;
}



#ifdef OTHER_TAGS

NFCSTATUS
phHciNfc_Activate_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    return status;
}


#endif


