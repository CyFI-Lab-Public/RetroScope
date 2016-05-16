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
* \file  phLlcNfc_Interface.h
* \brief Interface for both LLC and transport layer
*
* Project: NFC-FRI-1.1
*
* $Date: Thu Sep 11 12:19:29 2008 $
* $Author: ing02260 $
* $Revision: 1.9 $
* $Aliases: NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK849_PACK1_PREP1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

#ifndef PHLLCNFC_INTERFACE_H
#define PHLLCNFC_INTERFACE_H

/**
*  \name LLC TL NFC interface
*
* File: \ref phLlcTlNfc_Interface.h
*
*/
/*@{*/
#define PH_LLCNFC_INTERFACE_FILEREVISION "$Revision: 1.9 $" /**< \ingroup grp_hal_nfc_llc */
#define PH_LLCNFC_INTERFACE_FILEALIASES "$Aliases: NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK849_PACK1_PREP1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"    /**< \ingroup grp_hal_nfc_llc */
/*@}*/
/*************************** Includes *******************************/

/*********************** End of includes ****************************/

/***************************** Macros *******************************/
#define PH_LLCNFC_READWAIT_OFF          0
#define PH_LLCNFC_READWAIT_ON           1

/************************ End of macros *****************************/

/********************** Callback functions **************************/

/******************* End of Callback functions **********************/

/********************* Structures and enums *************************/

/****************** End of structures and enums *********************/

/******************** Function declarations *************************/

NFCSTATUS 
phLlcNfc_Interface_Register(
    phLlcNfc_Context_t          *psLlcCtxt, 
    phNfcLayer_sCfg_t           *psIFConfig
);

NFCSTATUS 
phLlcNfc_Interface_Init(
    phLlcNfc_Context_t      *psLlcCtxt
);

NFCSTATUS 
phLlcNfc_Interface_Read(
    phLlcNfc_Context_t      *psLlcCtxt, 
    uint8_t                 readWaitOn, 
    uint8_t                 *pLlcBuffer, 
    uint32_t                llcBufferLength
);

NFCSTATUS 
phLlcNfc_Interface_Write(
    phLlcNfc_Context_t      *psLlcCtxt,
    uint8_t             *pLlcBuffer, 
    uint32_t            llcBufferLength
);

/**
* \ingroup grp_hal_nfc_llc
*
* \brief \b Release function
*
* \copydoc page_reg Release all the variables of the LLC component, that has been 
*      initialised in \b phLlcNfc_Init function (Synchronous function).
*
* \param[in] pContext          LLC context is provided by the upper layer. The LLC 
*                              context earlier was given to the upper layer through the
*                              \ref phLlcNfc_Register function
* \param[in] pLinkInfo         Link information of the hardware
*
* \retval NFCSTATUS_PENDING            If the command is yet to be processed.
* \retval NFCSTATUS_INVALID_PARAMETER  At least one parameter of the function is invalid.
* \retval Other errors                 Errors related to the lower layers
*
*/
extern 
NFCSTATUS 
phLlcNfc_Release(
                 void    *pContext, 
                 void    *pLinkInfo
                 );
/****************** End of Function declarations ********************/
#endif /* PHLLCTLNFC_INTERFACE_H */
