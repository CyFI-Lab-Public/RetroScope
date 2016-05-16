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

/**
 * \file  phFriNfc_OvrHalCmd.h
 * \brief Overlapped HAL
 *
 * Project: NFC-FRI
 *
 * $Date: Fri Oct  5 10:09:57 2007 $
 * $Author: frq05303 $
 * $Revision: 1.1 $
 * $Aliases: NFC_FRI1.1_WK826_PREP1,NFC_FRI1.1_WK826_R1,NFC_FRI1.1_WK826_R2,NFC_FRI1.1_WK830_PREP1,NFC_FRI1.1_WK830_PREP2,NFC_FRI1.1_WK830_R5_1,NFC_FRI1.1_WK830_R5_2,NFC_FRI1.1_WK830_R5_3,NFC_FRI1.1_WK832_PREP1,NFC_FRI1.1_WK832_PRE2,NFC_FRI1.1_WK832_PREP2,NFC_FRI1.1_WK832_PREP3,NFC_FRI1.1_WK832_R5_1,NFC_FRI1.1_WK832_R6_1,NFC_FRI1.1_WK834_PREP1,NFC_FRI1.1_WK834_PREP2,NFC_FRI1.1_WK834_R7_1,NFC_FRI1.1_WK836_PREP1,NFC_FRI1.1_WK836_R8_1,NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1 $
 *
 */

#ifndef PHFRINFC_OVRHALCMD_H
#define PHFRINFC_OVRHALCMD_H

#include <phNfcHalTypes.h>

/**
 *  \name Overlapped HAL
 *
 * File: \ref phFriNfc_OvrHalCmd.h
 *
 */
/*@{*/
#define PH_FRINFC_OVRHALCMD_FILEREVISION "$Revision: 1.1 $" /** \ingroup grp_file_attributes */
#define PH_FRINFC_OVRHALCMD_FILEALIASES  "$Aliases: NFC_FRI1.1_WK826_PREP1,NFC_FRI1.1_WK826_R1,NFC_FRI1.1_WK826_R2,NFC_FRI1.1_WK830_PREP1,NFC_FRI1.1_WK830_PREP2,NFC_FRI1.1_WK830_R5_1,NFC_FRI1.1_WK830_R5_2,NFC_FRI1.1_WK830_R5_3,NFC_FRI1.1_WK832_PREP1,NFC_FRI1.1_WK832_PRE2,NFC_FRI1.1_WK832_PREP2,NFC_FRI1.1_WK832_PREP3,NFC_FRI1.1_WK832_R5_1,NFC_FRI1.1_WK832_R6_1,NFC_FRI1.1_WK834_PREP1,NFC_FRI1.1_WK834_PREP2,NFC_FRI1.1_WK834_R7_1,NFC_FRI1.1_WK836_PREP1,NFC_FRI1.1_WK836_R8_1,NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1 $"      /** \ingroup grp_file_attributes */
/*@}*/

/** \defgroup grp_ovr_hal_cmd Overlapped HAL Command List
 *  \ingroup grp_fri_nfc_ovr_hal
 *  These are the command definitions for the Overlapped HAL. They are used internally by the
 *  implementation of the component.
 */
/*@{*/
#define PH_FRINFC_OVRHALCMD_NUL             (0)     /**< \brief We're in NO command */

#define PH_FRINFC_OVRHALCMD_ENU             (1)     /**< \brief Enumerate */
#define PH_FRINFC_OVRHALCMD_OPE             (2)     /**< \brief Open */
#define PH_FRINFC_OVRHALCMD_CLO             (3)     /**< \brief Close */
#define PH_FRINFC_OVRHALCMD_GDC             (4)     /**< \brief Get Dev Caps */
#define PH_FRINFC_OVRHALCMD_POL             (5)     /**< \brief Poll */
#define PH_FRINFC_OVRHALCMD_CON             (6)     /**< \brief Connect */
#define PH_FRINFC_OVRHALCMD_DIS             (7)     /**< \brief Disconnect */
#define PH_FRINFC_OVRHALCMD_TRX             (8)     /**< \brief Transceive */
#define PH_FRINFC_OVRHALCMD_STM             (9)     /**< \brief Start Target Mode */
#define PH_FRINFC_OVRHALCMD_SND             (10)     /**< \brief Send */
#define PH_FRINFC_OVRHALCMD_RCV             (11)    /**< \brief Receive */
#define PH_FRINFC_OVRHALCMD_IOC             (12)    /**< \brief IOCTL */

#define PH_FRINFC_OVRHALCMD_TST             (255)   /**< \brief OVR HAL test-related command */


/** \brief Parameter compound internally used for testing purpose
 *
 */
typedef struct phFriNfc_OvrHalCmdVoid
{
    void      *Div;
    NFCSTATUS  Status;
    uint32_t   Delay;
} phFriNfc_OvrHalCmdVoid_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Enumerate
 *
 */
typedef struct phFriNfc_OvrHalCmdEnu
{
    phHal_sHwReference_t           *HwReference;
    uint8_t                        *pNbrOfDevDetected;
} phFriNfc_OvrHalCmdEnu_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Open
 *
 */
typedef struct phFriNfc_OvrHalCmdOpe
{
    phHal_sHwReference_t *psHwReference;
} phFriNfc_OvrHalCmdOpe_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Close
 *
 */
typedef struct phFriNfc_OvrHalCmdClo
{
    phHal_sHwReference_t *psHwReference;
} phFriNfc_OvrHalCmdClo_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_GetDeviceCapabilities
 *
 */
typedef struct phFriNfc_OvrHalCmdGdc
{
    phHal_sHwReference_t            *psHwReference;
    phHal_sDeviceCapabilities_t     *psDevCapabilities;
} phFriNfc_OvrHalCmdGdc_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Poll
 *
 */
typedef struct phFriNfc_OvrHalCmdPol
{
    phHal_sHwReference_t           *psHwReference;
    phHal_eOpModes_t               *OpModes;
    phHal_sRemoteDevInformation_t  *psRemoteDevInfoList;
    uint8_t                        *NbrOfRemoteDev;
    phHal_sDevInputParam_t         *psDevInputParam;
} phFriNfc_OvrHalCmdPol_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Connect
 *
 */
typedef struct phFriNfc_OvrHalCmdCon
{
    phHal_sHwReference_t          *psHwReference;
    phHal_eOpModes_t               OpMode;
    phHal_sRemoteDevInformation_t *psRemoteDevInfo;
    phHal_sDevInputParam_t        *psDevInputParam;
} phFriNfc_OvrHalCmdCon_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Disconnect
 *
 */
typedef struct phFriNfc_OvrHalCmdDis
{
    phHal_sHwReference_t            *psHwReference;
    phHal_sRemoteDevInformation_t   *psRemoteDevInfo;
} phFriNfc_OvrHalCmdDis_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Transceive
 *
 */
typedef struct phFriNfc_OvrHalCmdTrx
{
    phHal_sHwReference_t           *psHwReference;
    phHal_sRemoteDevInformation_t  *psRemoteDevInfo;
    phHal_uCmdList_t                Cmd;
    phHal_sDepAdditionalInfo_t     *psDepAdditionalInfo;
    uint8_t                        *pSendBuf;
    uint16_t                        SendLength;
    uint8_t                        *pRecvBuf;
    uint16_t                       *pRecvLength;
} phFriNfc_OvrHalCmdTrx_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_StartTargetMode
 *
 */
typedef struct phFriNfc_OvrHalCmdStm
{
    phHal_sHwReference_t       *psHwReference;
    phHal_sTargetInfo_t        *pTgInfo;
    phHal_eOpModes_t           *OpModes;
    uint8_t                    *pConnectionReq;
    uint8_t                    *pConnectionReqBufLength;
} phFriNfc_OvrHalCmdStm_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Receive
 *
 */
typedef struct phFriNfc_OvrHalCmdRcv
{
    phHal_sHwReference_t        *psHwReference;
    phHal_sDepAdditionalInfo_t  *psDepAdditionalInfo;
    uint8_t                     *pRecvBuf;
    uint16_t                    *pRecvLength;
} phFriNfc_OvrHalCmdRcv_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Send
 *
 */
typedef struct phFriNfc_OvrHalCmdSnd
{
    phHal_sHwReference_t        *psHwReference;
    phHal_sDepAdditionalInfo_t  *psDepAdditionalInfo;
    uint8_t                     *pSendBuf;
    uint16_t                     SendLength;
} phFriNfc_OvrHalCmdSnd_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Ioctl
 *
 */
typedef struct phFriNfc_OvrHalCmdIoc
{
    phHal_sHwReference_t     *psHwReference;
    uint16_t                  IoctlCode;
    uint8_t                  *pInBuf;
    uint16_t                  InLength;
    uint8_t                  *pOutBuf;
    uint16_t                 *pOutLength;
} phFriNfc_OvrHalCmdIoc_t;

/** \brief Parameter compound internally used by \ref phFriNfc_OvrHal_Test
 *
 */
typedef struct phFriNfc_OvrHalCmdTst
{
    phHal_sHwReference_t     *psHwReference;
    void                     *pTestParam;
} phFriNfc_OvrHalCmdTst_t;


#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */
/** \brief Parameter compound internally used by \ref phFriNfc_OvrHalCmdMockup_t
 *
 */
typedef struct phFriNfc_OvrHalCmdMockup
{
    phHal_sHwReference_t     *psHwReference;
    uint16_t                  IoctlCode;
    uint8_t                  *pInBuf;
    uint16_t                  InLength;
    uint8_t                  *pOutBuf;
    uint16_t                 *pOutLength;
} phFriNfc_OvrHalCmdMockup_t;
#endif /* PHFRINFC_OVRHAL_MOCKUP */

/** \brief Placeholder for all parameter structures
 *
 */
typedef union phFriNfc_OvrHalCmd
{
    phFriNfc_OvrHalCmdVoid_t    CmdVoid;

    phFriNfc_OvrHalCmdEnu_t     CmdEnu;
    phFriNfc_OvrHalCmdOpe_t     CmdOpe;
    phFriNfc_OvrHalCmdClo_t     CmdClo;
    phFriNfc_OvrHalCmdGdc_t     CmdGdc;
    phFriNfc_OvrHalCmdPol_t     CmdPol;
    phFriNfc_OvrHalCmdCon_t     CmdCon;
    phFriNfc_OvrHalCmdDis_t     CmdDis;
    phFriNfc_OvrHalCmdTrx_t     CmdTrx;
    phFriNfc_OvrHalCmdIoc_t     CmdIoc;
    phFriNfc_OvrHalCmdStm_t     CmdStm;
    phFriNfc_OvrHalCmdSnd_t     CmdSnd;
    phFriNfc_OvrHalCmdRcv_t     CmdRcv;
    phFriNfc_OvrHalCmdTst_t     CmdTst;
} phFriNfc_OvrHalCmd_t;


/*@}*/
#endif /* PHFRINFC_OVRHALCMD_H */
