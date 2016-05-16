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
 * \file  phLibNfc_SE.h
 *
 * Project: NFC-FRI 1.1
 *
 * $Workfile:: phLibNfc_1.1.h  $
 * $Modtime::         $
 * $Author: ing07299 $
 * $Revision: 1.14 $
 *
 */
#ifndef PHLIBNFC_SE_H
#define PHLIBNFC_SE_H

#define LIBNFC_SE_INVALID_HANDLE 0
#define LIBNFC_SE_SUPPORTED      2
#define LIBNFC_SE_BASE_HANDLE    0xABCDEF

#define LIBNFC_SE_SMARTMX_INDEX  0
#define LIBNFC_SE_UICC_INDEX     1

#define PAUSE_PHASE     0x0824  /*Indicates the Pause phase duration*/
#define EMULATION_PHASE 0x5161  /*Indicates the Emulation phase duration*/

typedef struct phLibNfc_SeCallbackInfo
{
    /* SE set mode callback and its context */
    pphLibNfc_SE_SetModeRspCb_t         pSEsetModeCb;
    void                                *pSEsetModeCtxt; 
    /* Store SE discovery notification callback and its context */
    pphLibNfc_SE_NotificationCb_t       pSeListenerNtfCb;
    void                                *pSeListenerCtxt;

}phLibNfc_SECallbackInfo_t;

/*SE State */
typedef enum {
            phLibNfc_eSeInvalid   = 0x00,
            phLibNfc_eSeInit,
            phLibNfc_eSeReady,
            phLibNfc_eSeVirtual,
            phLibNfc_eSeWired
}phLibNfc_SeState_t;


/* Context for secured element */
typedef struct phLibNfc_SeCtxt
{

    /* UICC Status in Virtual Mode */
    uint8_t                         uUiccActivate;   

    /* SMX Status in Virtual Mode */
    uint8_t                         uSmxActivate;

    /* Count of the Secure Elements Present */
    uint8_t                         uSeCount;
    
    /* Se Temp handle */
    phLibNfc_Handle                 hSetemp;

    /*Current SE state*/
    phLibNfc_SeState_t              eSE_State;

    /*Current SE Mode */

    phLibNfc_eSE_ActivationMode     eActivatedMode;

    /* SE callback information */
    phLibNfc_SECallbackInfo_t       sSeCallabackInfo;

}phLibNfc_SeCtxt_t;

extern phLibNfc_SE_List_t sSecuredElementInfo[PHLIBNFC_MAXNO_OF_SE];



#endif


