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
 * \file  phLibNfc_Internal.h
 *
 * Project: NFC-FRI 1.1
 *
 * $Workfile:: phLibNfc_Internal.h  $
 * $Modtime::         $
 * $Author: ing07385 $
 * $Revision: 1.26 $
 *
 */
#ifndef PHLIBNFC_IN_H
#define PHLIBNFC_IN_H
#include <phNfcStatus.h>
#include <phNfcCompId.h>
#include <phNfcHalTypes.h>
#include <phNfcInterface.h>
#include <phNfcConfig.h>
#include <phDbgTrace.h>
#include <phHal4Nfc.h>
#include <phFriNfc_NdefMap.h>
#include <phFriNfc_OvrHal.h>
#include <phFriNfc_SmtCrdFmt.h>
#include <phFriNfc_Llcp.h>
#include <phFriNfc_LlcpTransport.h>
#include <phOsalNfc_Timer.h>
#include <phLibNfc_SE.h>
#include <phFriNfc_NdefReg.h>
#include <phLibNfc.h>
#include <phLibNfc_initiator.h>
#include <phLibNfc_ndef_raw.h>
#include <phNfcLlcpTypes.h>

/**Maximum number of Records.Presently set to a realistic value of 128 
   Configurable upto 1K*/
#define    MAX_NO_OF_RECORDS    128U
#define    CHK_NDEF_NOT_DONE   0x02U

typedef struct phLibNfc_status
{
    unsigned                    RlsCb_status : 1;
    unsigned                    DiscEnbl_status : 1;
    unsigned                    Connect_status : 1;
    unsigned                    TransProg_status : 1;
    unsigned                    RelsProg_status : 1;
    unsigned                    GenCb_pending_status : 1;
    unsigned                    Shutdown_pending_status : 1;
    unsigned                    Discovery_pending_status : 1;

}Status_t;
typedef enum phLibNfc_State{
    eLibNfcHalStateShutdown   = 0x00,  /**< closed*/
    eLibNfcHalInitInProgress,
    eLibNfcHalInited,
    eLibNfcHalShutdownInProgress,
    eLibNfcHalStateInitandIdle,
    eLibNfcHalStateConfigReady ,
    eLibNfcHalStateConnect,
    eLibNfcHalStateTransaction,
    eLibNfcHalStatePresenceChk,
    eLibNfcHalStateRelease,
    eLibNfcHalStateInvalid
} phLibNfc_State_t;




typedef struct phLibNfc_Hal_CB_Info
{
    /*Init call back & its context*/
    pphLibNfc_RspCb_t              pClientInitCb;
    void                           *pClientInitCntx;
    /*Shutdown call back & its context*/
    pphLibNfc_RspCb_t              pClientShutdownCb;
    void                           *pClientShtdwnCntx;
    /*Connect call back & its context*/
    pphLibNfc_ConnectCallback_t    pClientConnectCb;
    void                           *pClientConCntx;
    /*DisConnect call back & its context*/
    pphLibNfc_DisconnectCallback_t pClientDisConnectCb;
    void                           *pClientDConCntx;

    /*Transceive Call back & it's context*/
    pphLibNfc_TransceiveCallback_t pClientTransceiveCb;
    void                           *pClientTranseCntx;
    /*Check Ndef Call back & it's context*/
    pphLibNfc_ChkNdefRspCb_t       pClientCkNdefCb;
    void                           *pClientCkNdefCntx;
    /*Read Ndef Call back & it's context*/
    pphLibNfc_RspCb_t              pClientRdNdefCb;
    void                           *pClientRdNdefCntx;
    /*Write Ndef Call back & it's context*/
    pphLibNfc_RspCb_t              pClientWrNdefCb;
    void                           *pClientWrNdefCntx;


    /*Discover Call back & it's context*/
    pphLibNfc_RspCb_t              pClientDisConfigCb;
    void                           *pClientDisCfgCntx;

    /*Presence check Call back & it's context*/
    pphLibNfc_RspCb_t              pClientPresChkCb;
    void                           *pClientPresChkCntx;

    /*Register notification Call back & it's context*/
    phLibNfc_NtfRegister_RspCb_t   pClientNtfRegRespCB;
    void                           *pClientNtfRegRespCntx;

    /*Ndef Notification CB*/
    pphLibNfc_Ndef_Search_RspCb_t   pClientNdefNtfRespCb;
    void                           *pClientNdefNtfRespCntx;

    /*LLCP Check CB*/
    pphLibNfc_ChkLlcpRspCb_t       pClientLlcpCheckRespCb;
    void                           *pClientLlcpCheckRespCntx;

    /*LLCP Link CB*/
    pphLibNfc_LlcpLinkStatusCb_t   pClientLlcpLinkCb;
    void                           *pClientLlcpLinkCntx;

    /*LLCP service discovery*/
    pphLibNfc_RspCb_t              pClientLlcpDiscoveryCb;
    void                           *pClientLlcpDiscoveryCntx;

}phLibNfc_Hal_CB_Info_t;

typedef struct phLibNfc_NdefInfo
{
    bool_t                       NdefContinueRead;
    uint32_t                     NdefActualSize,
                                 AppWrLength;
    phFriNfc_NdefMap_t           *psNdefMap;
    uint16_t                     NdefSendRecvLen;
    uint16_t                     NdefDataCount;
    phNfc_sData_t                *psUpperNdefMsg;
    uint32_t                     NdefReadTimerId,
                                 NdefLength;
    uint8_t                      is_ndef ;
    phFriNfc_sNdefSmtCrdFmt_t    *ndef_fmt ;
    phLibNfc_Last_Call_t         eLast_Call;
    uint32_t                     Chk_Ndef_Timer_Id;


    /*Format Ndef Call back & it's context*/
    pphLibNfc_RspCb_t            pClientNdefFmtCb;
    void                        *pClientNdefFmtCntx;
    phLibNfc_Ndef_SrchType_t    *pNdef_NtfSrch_Type;

}phLibNfc_NdefInfo_t;

typedef struct phLibNfc_NdefRecInfo
{
    phFriNfc_NdefReg_CbParam_t  CbParam;
    phFriNfc_NdefReg_t          NdefReg;
    uint8_t                     *NdefTypes_array[100];
    phFriNfc_NdefRecord_t       RecordsExtracted;
    uint8_t                     ChunkedRecordsarray[MAX_NO_OF_RECORDS];
    uint32_t                    NumberOfRecords;
    uint8_t                     IsChunked[MAX_NO_OF_RECORDS];
    uint32_t                    NumberOfRawRecords;
    uint8_t                     *RawRecords[MAX_NO_OF_RECORDS];
    phFriNfc_NdefReg_Cb_t       *NdefCb;
    phNfc_sData_t               ndef_message;
}phLibNfc_NdefRecInfo_t;

typedef struct phLibNfc_LlcpInfo
{
   /* Local parameters for LLC, given upon config
    * and used upon detection.
    */
   phLibNfc_Llcp_sLinkParameters_t sLocalParams;

   /* LLCP compliance flag */
   bool_t bIsLlcp;

   /* Monitor structure for LLCP Transport */
   phFriNfc_LlcpTransport_t sLlcpTransportContext;

   /* Monitor structure for LLCP LLC */
   phFriNfc_Llcp_t sLlcpContext;

   /* LLC Rx buffer */
   uint8_t pRxBuffer[PHFRINFC_LLCP_PDU_HEADER_MAX + PHFRINFC_LLCP_MIU_DEFAULT + PHFRINFC_LLCP_MIUX_MAX];

   /* LLC Tx buffer */
   uint8_t pTxBuffer[PHFRINFC_LLCP_PDU_HEADER_MAX + PHFRINFC_LLCP_MIU_DEFAULT + PHFRINFC_LLCP_MIUX_MAX];

} phLibNfc_LlcpInfo_t;

typedef struct phLibNfc_LibContext
{
    phHal_sHwReference_t         *psHwReference;
    Status_t                     status;
    phHal_sEmulationCfg_t        sCardEmulCfg;
    phLibNfc_SeCtxt_t            sSeContext;
    phNfc_sState_t               LibNfcState;

    phHal_sDevInputParam_t       *psDevInputParam;

    phLibNfc_NdefInfo_t          ndef_cntx;
    phLibNfc_NfcIpInfo_t         sNfcIp_Context;

    phFriNfc_OvrHal_t            *psOverHalCtxt;
    phLibNfc_Registry_Info_t     RegNtfType;
    uint8_t                      dev_cnt;

    /*To re configure the discovery wheel*/
    phLibNfc_sADD_Cfg_t          sADDconfig;
    uint32_t                     Connected_handle,
                                 Discov_handle[MAX_REMOTE_DEVICES];

    /* To store the previous connected handle in case of Multiple protocol tags */
    uint32_t Prev_Connected_handle;

    /*Call back function pointers */

    phLibNfc_eDiscoveryConfigMode_t        eLibNfcCfgMode;

    phHal4Nfc_DiscoveryInfo_t      *psDiscInfo;

    phLibNfc_eReleaseType_t        ReleaseType;
    /**Transaction Related Info */
    phLibNfc_sTransceiveInfo_t     *psTransInfo;
    phLibNfc_sTransceiveInfo_t     *psBufferedAuth;
    uint8_t                        LastTrancvSuccess;
    phLibNfc_RemoteDevList_t       psRemoteDevList[MAX_REMOTE_DEVICES];
    /*To Call back function pointers & Client context*/
    phLibNfc_Hal_CB_Info_t           CBInfo;

    /*Ndef RTD search Info*/
    phLibNfc_NdefRecInfo_t    phLib_NdefRecCntx;

    /*LLCP Info*/
    phLibNfc_LlcpInfo_t          llcp_cntx;

    /* Pointer to Lib context */
} phLibNfc_LibContext_t,*pphLibNfc_LibContext_t;

extern void phLibNfc_Pending_Shutdown(void);
extern pphLibNfc_LibContext_t gpphLibContext;
extern NFCSTATUS
phLibNfc_UpdateNextState(
                         pphLibNfc_LibContext_t psNfcHalCtxt,
                         phLibNfc_State_t        next_state
                         );

extern void
phLibNfc_UpdateCurState(
                        NFCSTATUS      status,
                        pphLibNfc_LibContext_t psNfcHalCtxt
                        );

extern void
phLibNfc_Reconnect_Mifare_Cb (
                    void                            *pContext,
                    phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                    NFCSTATUS                       status);


#endif


