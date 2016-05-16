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
* \file  phHciNfc_Emulation.h                                                 *
* \brief HCI emulation management routines.                                   *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:26 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.10 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_EMULATION_H
#define PHHCINFC_EMULATION_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_Emulation.h
*
*/
/*@{*/
#define PHHCINFC_EMULATION_FILEREVISION "$Revision: 1.10 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_EMULATION_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/
/* Connectivity Gate Command Support */
#define PRO_HOST_REQUEST            (0x10U)

/* Connectivity Gate Event Support */
#define EVT_CONNECTIVITY            (0x10U)
#define EVT_END_OF_TRANSACTION      (0x11U)
#define EVT_TRANSACTION             (0x12U)
#define EVT_OPERATION_ENDED         (0x13U)

#define TRANSACTION_MIN_LEN         (0x03U)
#define TRANSACTION_AID             (0x81U)
#define TRANSACTION_PARAM           (0x82U)

#define HOST_CE_MODE_ENABLE         (0x02U)
#define HOST_CE_MODE_DISABLE        (0xFFU)

#define NXP_PIPE_CONNECTIVITY       (0x60U)


/* Card Emulation Gate Events */
#define CE_EVT_NFC_SEND_DATA        (0x10U)
#define CE_EVT_NFC_FIELD_ON         (0x11U)
#define CE_EVT_NFC_DEACTIVATED      (0x12U)
#define CE_EVT_NFC_ACTIVATED        (0x13U)
#define CE_EVT_NFC_FIELD_OFF        (0x14U)

/*
******************** Enumeration and Structure Definition **********************
*/



/*
*********************** Function Prototype Declaration *************************
*/

extern
NFCSTATUS
phHciNfc_Uicc_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipe_id,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                        );

extern
NFCSTATUS
phHciNfc_EmuMgmt_Update_Seq(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     seq_type
                        );

extern
NFCSTATUS
phHciNfc_EmuMgmt_Initialise(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                        );

extern
NFCSTATUS
phHciNfc_EmuMgmt_Release(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                        );


extern
NFCSTATUS
phHciNfc_Emulation_Cfg (
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef, 
                        phHciNfc_eConfigType_t  cfg_type
                    );

extern
NFCSTATUS
phHciNfc_Uicc_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                        );

extern
NFCSTATUS
phHciNfc_Uicc_Connect_Status(
                               phHciNfc_sContext_t      *psHciContext,
                               void                 *pHwRef
                      );

extern
void
phHciNfc_Uicc_Connectivity(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                        );


#endif /* PHHCINFC_EMULATION_H */
