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
* \file  phHciNfc_WI .h                                                       *
* \brief HCI wired interface gate Management Routines.                        *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Jan 16 10:33:47 2009 $                                           *
* $Author: ravindrau $                                                         *
* $Revision: 1.11 $                                                            *
* $Aliases: NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/
#ifndef PHHCINFC_WI_H
#define PHHCINFC_WI_H
/*@}*/
/**
 *  \name HCI
 *
 * File: \ref phHciNfc_WI.h
 *
 */
/*@{*/
#define PHHCINFC_WIRED_FILEREVISION "$Revision: 1.11 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_WIREDINTERFACE_FILEALIASES  "$Aliases: NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/****************************** Header File Inclusion *****************************/
#include <phHciNfc_Generic.h>
#include <phHciNfc_Emulation.h>

/******************************* Macro Definitions ********************************/

/******************** Enumeration and Structure Definition ***********************/


/* enable /disable notifications */
typedef enum phHciNfc_WI_Events{
    eDisableEvents, 
    eEnableEvents
} phHciNfc_WI_Events_t;

typedef enum phHciNfc_WI_Seq{
    eWI_PipeOpen        = 0x00U,
    eWI_SetDefaultMode,
    eWI_PipeClose
} phHciNfc_WI_Seq_t;

/* Information structure for  WI  Gate */
typedef struct phHciNfc_WI_Info{

    /* Pointer to WI gate pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    /*  WI gate pipe Identifier */
    uint8_t                         pipe_id;
    /*  Application ID of the Transaction performed */
    uint8_t                         aid[MAX_AID_LEN];
    /*  Default info */
    uint8_t                         default_type;
    /* Current WI gate Internal Sequence type   */
    phHciNfc_WI_Seq_t               current_seq;
    /*Current WI gate next Sequence ID          */
    phHciNfc_WI_Seq_t               next_seq;
    
} phHciNfc_WI_Info_t;

/************************ Function Prototype Declaration *************************/
/*!
 * \brief Allocates the resources required for  WI gate management.
 *
 * This function Allocates necessary resources as requiered by WI gate management
 *
 * \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *
 * \retval NFCSTATUS_SUCCESS           Function execution is successful
 *
 * \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
 */
extern
NFCSTATUS
phHciNfc_WI_Init_Resources(phHciNfc_sContext_t   *psHciContext);

/**
* \ingroup grp_hci_nfc
*
* \brief Allocates the resources required for  WI gate management.
*
* This function Allocates necessary resources as requiered by WI gate management
*
* \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/

extern
NFCSTATUS   
phHciNfc_WIMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );
/**
* \ingroup grp_hci_nfc
*
* \brief Allocates the resources required for  WI gate management.
*
* This function Allocates necessary resources as requiered by WI gate management
*
* \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_WI_Update_PipeInfo(
                                  phHciNfc_sContext_t     *psHciContext,
                                  uint8_t                 pipeID,
                                  phHciNfc_Pipe_Info_t    *pPipeInfo
                                  );

/**
* \ingroup grp_hci_nfc
*
* \brief Allocates the resources required for  WI gate management.
*
* This function Allocates necessary resources as requiered by WI gate management
*
* \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_WI_Configure_Mode(
                                  void                *psHciHandle,
                                  void                *pHwRef,
                                  phHal_eSmartMX_Mode_t   cfg_Mode
                          );

extern
NFCSTATUS
phHciNfc_WI_Configure_Notifications(
                                    void        *psHciHandle,
                                    void        *pHwRef,
                                    phHciNfc_WI_Events_t eNotification
                                );

extern
NFCSTATUS
phHciNfc_WI_Get_PipeID(
                       phHciNfc_sContext_t        *psHciContext,
                       uint8_t                    *ppipe_id
                   );

extern 
NFCSTATUS
phHciNfc_WI_Configure_Default(
                              void                  *psHciHandle,
                              void                  *pHwRef,
                              uint8_t               enable_type
                          );

extern 
NFCSTATUS
phHciNfc_WI_Get_Default(
                        void                  *psHciHandle,
                        void                  *pHwRef
                        );


#endif /* #ifndef PHHCINFC_WI_H */

