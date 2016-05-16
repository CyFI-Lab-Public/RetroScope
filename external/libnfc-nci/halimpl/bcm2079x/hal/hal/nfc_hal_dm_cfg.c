/******************************************************************************
 *
 *  Copyright (C) 2011-2013 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/


/******************************************************************************
 *
 *  This file contains compile-time configurable constants for BRCM HAL
 *  modules
 *
 ******************************************************************************/
#include "nfc_hal_int.h"
#include "nci_defs.h"
#include "nfc_brcm_defs.h"

/* the SetConfig at start up*/
UINT8 nfc_hal_start_up_cfg[] = {
    /* TLV len */   31,
    /* B0 */        NCI_PARAM_ID_EMVCO_ENABLE,
    /* B1 */        1,
    /* B2 */        1,     /* (1 = enable emvco mode, 0 = disable emvco mode) Default = 0.*/
    /* B3 */        NCI_PARAM_ID_CONTINUE_MODE, /* NFCC will restart discovery after deactivated */
    /* B4 */        1,
    /* B5 */        1,     /* (1 = enable, 0 = disable) Default = 0.*/
    /* B6 */        NCI_PARAM_ID_RFU_CONFIG,
    /* B7 */        0x14,
    /* B8 */        0x00,
    /* B9 */        0x00,
    /* B10*/        0x00,
    /* B11*/        0x00,
    /* B12*/        0x0E,
    /* B13*/        0xE8,
    /* B14*/        0xF0,
    /* B15*/        0x55,
    /* B16*/        0x00,
    /* B17*/        0x0F, /* CE3 SO delay in sec */
    /* B18*/        0x00,
    /* B19*/        0x00,
    /* B20*/        0x00,
    /* B21*/        0x00,
    /* B22*/        0x00,
    /* B23*/        0x00,
    /* B24*/        0x00,
    /* B25*/        0x00,
    /* B26*/        0x00,
    /* B27*/        0x00,
    /* B28*/        NCI_PARAM_ID_ACT_ORDER, /* polling sequence */
    /* B29*/        1,
    /* B30*/        1,     /* (1 = active mode polling before passive, 0 = passive polling first) Default = 0.*/
};

UINT8 *p_nfc_hal_dm_start_up_cfg = (UINT8 *) nfc_hal_start_up_cfg;

/* the VSCs at start up:
 * The VSCs are specified in TLV format similar to nfa_start_up_cfg[]
 * first byte is the TLV total len.
 * B0 is the first T; i.e. the opcode for the VSC
 * B1 is the len of the VSC parameters/payload
 * */
UINT8 nfc_hal_dm_start_up_vsc_cfg[] = {
    /* TLV len */   5,
    /* B0 */        NCI_MTS_CMD|NCI_GID_PROP,
    /* B1 */        NCI_MSG_FRAME_LOG,
    /* B2 */        2,
    /* B3 */        0,  /* 1 to enable RF frames */
    /* B4 */        1   /* 1 to enable SWP frames */
};

UINT8 *p_nfc_hal_dm_start_up_vsc_cfg = NULL;

/* the SetConfig at HAL_NfcPreDiscover. This is done once after HAL_NfcOpen */
UINT8 nfc_hal_pre_discover_cfg[] = {
    /* TLV len */   0x0A,
    /* B0 */        NCI_PARAM_ID_SWPCFG,
    /* B1 */        0x08,
    /* B2 */        0x01,
    /* B3 */        0x08,
    /* B4 */        0x00,
    /* B5 */        0x04,
    /* B6 */        0x80,
    /* B7 */        0xC3,
    /* B8 */        0xC9,
    /* B9 */        0x01
};

UINT8 *p_nfc_hal_pre_discover_cfg = NULL;

/* LPTD parameters (LowPowerTagDetection)
 * This is typical values for 20791B2
 * The timing and threshold parameters used for a customer handset/hardware may vary
 * depending on antenna and should be verified during a customer testing phase.
 * the data fields without comments are too complicated. Please see ""
 * */
const UINT8 nfc_hal_dm_lptd_cfg[] =
{
    21,             /* total TLV length excluding itself */
    NCI_PARAM_ID_TAGSNIFF_CFG,  /* type */
    19,             /* length */
    0x01,           /* B0 enable: 0/disable, 1/enable*/
    0x02,           /* B1 poll count: number of full power poll before starting lptd poll */
    0xFF,           /* B2 sniff count lsb: number of lptd poll before switching to full power poll */
    0xFF,           /* B3 sniff count msb */
    0x80,           /* B4 threshold: Bigger thresholds give a smaller LPTD range but more immunity to false detections. Smaller thresholds increase LPTD range at the cost of greater likelihood of false detections. */
    0x40,           /* B5 delay lsb: delay (us) to sampling power */
    0x00,           /* B6 delay msb */
    0x40,           /* B7 carrier threshold lsb */
    0x00,           /* B8 carrier threshold msb */
    0x80,           /* B9 mode: Bitwise variable used to enable various algorithm modes.*/
    0x80,           /* B10 0-offset lsb */
    0x00,           /* B11 0-offset msb */
    0x10,           /* B12 field sense time lsb */
    0x00,           /* B13 field sense time msb */
    0x00,           /* B14 false detect threshold lsb: 0x00 to disable LPTD NTF. The number of false tag detections to resport LPTD NTF. */
    0x00,           /* B15 false detect threshold msb. A false tag detect - full poll results in no tag being detected.*/
    0x75,           /* B16 mode1; Bitwise variable used to enable various algorithm modes. */
    0x0D,           /* B17 lptd ant cfg rx */
    0x30,           /* B18 lptd rdr cfg ve */
};

UINT8 *p_nfc_hal_dm_lptd_cfg = (UINT8 *) &nfc_hal_dm_lptd_cfg[0];

/*
** NFCC has a table which has 9 XTAL frequencies: 9.6, 13, 16.2,  19.2, 24, 26, 38.4, 52 and 37.4 in MHz.
** For these 9 xtal frequencies, host doesn't need to configure PLL325.
** For 43341, host doesn't need to configure it at all.
*/
UINT8 *p_nfc_hal_dm_pll_325_cfg = NULL;

tNFC_HAL_CFG nfc_hal_cfg =
{
    FALSE,                                  /* set nfc_hal_prm_nvm_required to TRUE, if the platform wants to abort PRM process without NVM */
    (UINT16) NFC_HAL_NFCC_ENABLE_TIMEOUT,   /* max time to wait for RESET NTF after setting REG_PU to high
                                            ** If NFCC doesn't have NVM or cannot load patch from NVM without Xtal setting
                                            ** then set it to short to optimize bootup time because NFCC cannot send RESET NTF.
                                            ** Otherwise, it depends on NVM type and size of patchram.
                                            */
    (UINT16) NFC_HAL_NFCC_ENABLE_TIMEOUT,   /* max time to wait for RESET NTF after setting Xtal frequency
                                            ** It depends on NVM type and size of patchram.
                                            */
    (HAL_NFC_HCI_UICC0_HOST | HAL_NFC_HCI_UICC1_HOST)  /* Set bit(s) for supported UICC(s) */
};

tNFC_HAL_CFG *p_nfc_hal_cfg= (tNFC_HAL_CFG *) &nfc_hal_cfg;
