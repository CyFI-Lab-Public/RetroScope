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
 * \file  phLibNfc_initiator.h
 *
 * Project: NFC-FRI 1.1
 *
 * $Workfile:: phLibNfc_1.1.h  $
 * $Modtime::         $
 * $Author: ing07299 $
 * $Revision: 1.13 $
 *
 */
#ifndef PHLIBNFC_INITIATOR_H
#define PHLIBNFC_INITIATOR_H

typedef struct phLibNfc_NfcIpInfo
{
    phNfc_sData_t                *p_recv_data;
    uint32_t                     recv_index;
     /*NFC-IP Call back & it's context*/
    pphLibNfc_RspCb_t            pClientNfcIpCfgCb;
    void                         *pClientNfcIpCfgCntx;  
    /*NFC-IP send callback and its context*/
    pphLibNfc_RspCb_t            pClientNfcIpTxCb;
    void                         *pClientNfcIpTxCntx;

    /*NFC-IP receive callback and its context*/
    pphLibNfc_Receive_RspCb_t    pClientNfcIpRxCb;
    void                         *pClientNfcIpRxCntx;
    /*Store the role of remote device*/
    phHal4Nfc_TransactInfo_t     TransactInfoRole;

    /*NFC IP remote initator handle */
    uint32_t                     Rem_Initiator_Handle;

}phLibNfc_NfcIpInfo_t;


#endif


