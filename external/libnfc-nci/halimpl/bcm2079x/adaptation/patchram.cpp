/******************************************************************************
 *
 *  Copyright (C) 1999-2012 Broadcom Corporation
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
#include "OverrideLog.h"
#include "config.h"
#include "nfc_hal_int.h"
#include "userial.h"
extern "C"
{
    #include "nfc_hal_post_reset.h"
}
#include <string>
#include <cutils/properties.h>
#include "spdhelper.h"
#include "StartupConfig.h"

#define LOG_TAG "NfcNciHal"

#define FW_PRE_PATCH                        "FW_PRE_PATCH"
#define FW_PATCH                            "FW_PATCH"
#define MAX_RF_DATA_CREDITS                 "MAX_RF_DATA_CREDITS"

#define MAX_BUFFER      (512)
static char sPrePatchFn[MAX_BUFFER+1];
static char sPatchFn[MAX_BUFFER+1];
static void * sPrmBuf = NULL;
static void * sI2cFixPrmBuf = NULL;

#define CONFIG_MAX_LEN 256
static UINT8 sConfig [CONFIG_MAX_LEN];
static StartupConfig sStartupConfig;
static StartupConfig sLptdConfig;
static StartupConfig sPreDiscoveryConfig;
extern UINT8 *p_nfc_hal_dm_start_up_cfg; //defined in the HAL
static UINT8 nfa_dm_start_up_vsc_cfg[CONFIG_MAX_LEN];
extern UINT8 *p_nfc_hal_dm_start_up_vsc_cfg; //defined in the HAL
extern UINT8 *p_nfc_hal_dm_lptd_cfg; //defined in the HAL
extern UINT8 *p_nfc_hal_pre_discover_cfg; //defined in the HAL

extern tSNOOZE_MODE_CONFIG gSnoozeModeCfg;
extern tNFC_HAL_CFG *p_nfc_hal_cfg;
static void mayDisableSecureElement (StartupConfig& config);

/* Default patchfile (in NCD format) */
#ifndef NFA_APP_DEFAULT_PATCHFILE_NAME
#define NFA_APP_DEFAULT_PATCHFILE_NAME      "\0"
#endif

/* Default patchfile (in NCD format) */
#ifndef NFA_APP_DEFAULT_I2C_PATCHFILE_NAME
#define NFA_APP_DEFAULT_I2C_PATCHFILE_NAME  "\0"
#endif

tNFC_POST_RESET_CB nfc_post_reset_cb =
{
    /* Default Patch & Pre-Patch */
    NFA_APP_DEFAULT_PATCHFILE_NAME,
    NULL,
    NFA_APP_DEFAULT_I2C_PATCHFILE_NAME,
    NULL,

    /* Default UART baud rate */
    NFC_HAL_DEFAULT_BAUD,

    /* Default tNFC_HAL_DEV_INIT_CFG (flags, num_xtal_cfg, {brcm_hw_id, xtal-freq, xtal-index} ) */
    {
        2, /* number of valid entries */
        {
            {0x43341000, 37400, NFC_HAL_XTAL_INDEX_37400},      // All revisions of 43341 use 37,400
            {0x20795000, 26000, NFC_HAL_XTAL_INDEX_26000},
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0},
        }
    },

    /* Default low power mode settings */
    NFC_HAL_LP_SNOOZE_MODE_NONE,    /* Snooze Mode          */
    NFC_HAL_LP_IDLE_THRESHOLD_HOST, /* Idle Threshold Host  */
    NFC_HAL_LP_IDLE_THRESHOLD_HC,   /* Idle Threshold HC    */
    NFC_HAL_LP_ACTIVE_LOW,          /* NFC_WAKE Active Mode */
    NFC_HAL_LP_ACTIVE_HIGH,         /* DH_WAKE Active Mode  */

    NFA_APP_MAX_NUM_REINIT,         /* max retry to get NVM type */
    0,                              /* current retry count */
    TRUE,                           /* debug mode for downloading patchram */
    FALSE                           /* skip downloading patchram after reinit because of patch download failure */
};


/*******************************************************************************
**
** Function         getFileLength
**
** Description      return the size of a file
**
** Returns          file size in number of bytes
**
*******************************************************************************/
static long getFileLength(FILE* fp)
{
    long sz;
    fseek(fp, 0L, SEEK_END);
    sz = ftell(fp);
    fseek(fp, 0L, SEEK_SET);

    return (sz > 0) ? sz : 0;
}

/*******************************************************************************
**
** Function         isFileExist
**
** Description      Check if file name exists (android does not support fexists)
**
** Returns          TRUE if file exists
**
*******************************************************************************/
static BOOLEAN isFileExist(const char *pFilename)
{
    FILE *pf;

    if ((pf = fopen(pFilename, "r")) != NULL)
    {
        fclose(pf);
        return TRUE;
    }
    return FALSE;
}

/*******************************************************************************
**
** Function         findPatchramFile
**
** Description      Find the patchram file name specified in the .conf
**
** Returns          pointer to the file name
**
*******************************************************************************/
static const char* findPatchramFile(const char * pConfigName, char * pBuffer, int bufferLen)
{
    ALOGD("%s: config=%s", __FUNCTION__, pConfigName);

    if (pConfigName == NULL)
    {
        ALOGD("%s No patchfile defined\n", __FUNCTION__);
        return NULL;
    }

    if (GetStrValue(pConfigName, &pBuffer[0], bufferLen))
    {
        ALOGD("%s found patchfile %s\n", __FUNCTION__, pBuffer);
        return (pBuffer[0] == '\0') ? NULL : pBuffer;
    }

    ALOGD("%s Cannot find patchfile '%s'\n", __FUNCTION__, pConfigName);
    return NULL;
}

/*******************************************************************************
**
** Function:    continueAfterSetSnoozeMode
**
** Description: Called after Snooze Mode is enabled.
**
** Returns:     none
**
*******************************************************************************/
static void continueAfterSetSnoozeMode(tHAL_NFC_STATUS status)
{
    ALOGD("%s: status=%u", __FUNCTION__, status);
    //let stack download firmware during next initialization
    nfc_post_reset_cb.spd_skip_on_power_cycle = FALSE;
    if (status == NCI_STATUS_OK)
        HAL_NfcPreInitDone (HAL_NFC_STATUS_OK);
    else
        HAL_NfcPreInitDone (HAL_NFC_STATUS_FAILED);
}

/*******************************************************************************
**
** Function:    postDownloadPatchram
**
** Description: Called after patch download
**
** Returns:     none
**
*******************************************************************************/
static void postDownloadPatchram(tHAL_NFC_STATUS status)
{
    ALOGD("%s: status=%i", __FUNCTION__, status);
    GetStrValue (NAME_SNOOZE_MODE_CFG, (char*)&gSnoozeModeCfg, sizeof(gSnoozeModeCfg));
    if (status != HAL_NFC_STATUS_OK)
    {
        ALOGE("%s: Patch download failed", __FUNCTION__);
        if (status == HAL_NFC_STATUS_REFUSED)
        {
            SpdHelper::setPatchAsBad();
        }
        else
            SpdHelper::incErrorCount();

        /* If in SPD Debug mode, fail immediately and obviously */
        if (SpdHelper::isSpdDebug())
            HAL_NfcPreInitDone (HAL_NFC_STATUS_FAILED);
        else
        {
            /* otherwise, power cycle the chip and let the stack startup normally */
            ALOGD("%s: re-init; don't download firmware", __FUNCTION__);
            //stop stack from downloading firmware during next initialization
            nfc_post_reset_cb.spd_skip_on_power_cycle = TRUE;
            USERIAL_PowerupDevice(0);
            HAL_NfcReInit ();
        }
    }
    /* Set snooze mode here */
    else if (gSnoozeModeCfg.snooze_mode != NFC_HAL_LP_SNOOZE_MODE_NONE)
    {
        status = HAL_NfcSetSnoozeMode(gSnoozeModeCfg.snooze_mode,
                                       gSnoozeModeCfg.idle_threshold_dh,
                                       gSnoozeModeCfg.idle_threshold_nfcc,
                                       gSnoozeModeCfg.nfc_wake_active_mode,
                                       gSnoozeModeCfg.dh_wake_active_mode,
                                       continueAfterSetSnoozeMode);
        if (status != NCI_STATUS_OK)
        {
            ALOGE("%s: Setting snooze mode failed, status=%i", __FUNCTION__, status);
            HAL_NfcPreInitDone(HAL_NFC_STATUS_FAILED);
        }
    }
    else
    {
        ALOGD("%s: Not using Snooze Mode", __FUNCTION__);
        HAL_NfcPreInitDone(HAL_NFC_STATUS_OK);
    }
}


/*******************************************************************************
**
** Function:    prmCallback
**
** Description: Patchram callback (for static patchram mode)
**
** Returns:     none
**
*******************************************************************************/
void prmCallback(UINT8 event)
{
    ALOGD("%s: event=0x%x", __FUNCTION__, event);
    switch (event)
    {
    case NFC_HAL_PRM_CONTINUE_EVT:
        /* This event does not occur if static patchram buf is used */
        break;

    case NFC_HAL_PRM_COMPLETE_EVT:
        postDownloadPatchram(HAL_NFC_STATUS_OK);
        break;

    case NFC_HAL_PRM_ABORT_EVT:
        postDownloadPatchram(HAL_NFC_STATUS_FAILED);
        break;

    case NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT:
        ALOGD("%s: invalid patch...skipping patch download", __FUNCTION__);
        postDownloadPatchram(HAL_NFC_STATUS_REFUSED);
        break;

    case NFC_HAL_PRM_ABORT_BAD_SIGNATURE_EVT:
        ALOGD("%s: patch authentication failed", __FUNCTION__);
        postDownloadPatchram(HAL_NFC_STATUS_REFUSED);
        break;

    case NFC_HAL_PRM_ABORT_NO_NVM_EVT:
        ALOGD("%s: No NVM detected", __FUNCTION__);
        HAL_NfcPreInitDone(HAL_NFC_STATUS_FAILED);
        break;

    default:
        ALOGD("%s: not handled event=0x%x", __FUNCTION__, event);
        break;
    }
}


/*******************************************************************************
**
** Function         getNfaValues
**
** Description      Get configuration values needed by NFA layer
**
** Returns:         None
**
*******************************************************************************/
static void getNfaValues()
{
    unsigned long num = 0;
    int actualLen = 0;

    p_nfc_hal_cfg->nfc_hal_prm_nvm_required = TRUE; //don't download firmware if controller cannot detect EERPOM
    sStartupConfig.initialize ();
    sLptdConfig.initialize ();
    sPreDiscoveryConfig.initialize();


    actualLen = GetStrValue (NAME_NFA_DM_START_UP_CFG, (char*)sConfig, sizeof(sConfig));
    if (actualLen)
        sStartupConfig.append (sConfig, actualLen);

    // Set antenna tuning configuration if configured.
    actualLen = GetStrValue(NAME_PREINIT_DSP_CFG, (char*)sConfig, sizeof(sConfig));
    if (actualLen)
        sStartupConfig.append (sConfig, actualLen);

    if ( GetStrValue ( NAME_NFA_DM_START_UP_VSC_CFG, (char*)nfa_dm_start_up_vsc_cfg, sizeof (nfa_dm_start_up_vsc_cfg) ) )
    {
        p_nfc_hal_dm_start_up_vsc_cfg = &nfa_dm_start_up_vsc_cfg[0];
        ALOGD ( "START_UP_VSC_CFG[0] = %02x:%02x:%02x:%02x:%02x:%02x:%02x:%02x\n",
                                                                            nfa_dm_start_up_vsc_cfg[0],
                                                                            nfa_dm_start_up_vsc_cfg[1],
                                                                            nfa_dm_start_up_vsc_cfg[2],
                                                                            nfa_dm_start_up_vsc_cfg[3],
                                                                            nfa_dm_start_up_vsc_cfg[4],
                                                                            nfa_dm_start_up_vsc_cfg[5],
                                                                            nfa_dm_start_up_vsc_cfg[6],
                                                                            nfa_dm_start_up_vsc_cfg[7] );
    }

    actualLen = GetStrValue(NAME_LPTD_CFG, (char*)sConfig, sizeof(sConfig));
    if (actualLen)
    {
        sLptdConfig.append (sConfig, actualLen);
        p_nfc_hal_dm_lptd_cfg = const_cast<UINT8*> (sLptdConfig.getInternalBuffer ());
    }

    mayDisableSecureElement (sStartupConfig);
    p_nfc_hal_dm_start_up_cfg = const_cast<UINT8*> (sStartupConfig.getInternalBuffer ());

    actualLen = GetStrValue(NAME_NFA_DM_PRE_DISCOVERY_CFG, (char*)sConfig, sizeof(sConfig));
    if (actualLen)
    {
        sPreDiscoveryConfig.append (sConfig, actualLen);
        mayDisableSecureElement (sPreDiscoveryConfig);
        p_nfc_hal_pre_discover_cfg = const_cast<UINT8*> (sPreDiscoveryConfig.getInternalBuffer ());
    }
}

/*******************************************************************************
**
** Function         StartPatchDownload
**
** Description      Reads configuration settings, and begins the download
**                  process if patch files are configured.
**
** Returns:         None
**
*******************************************************************************/
static void StartPatchDownload(UINT32 chipid)
{
    ALOGD ("%s: chipid=%lx",__FUNCTION__, chipid);

    char chipID[30];
    sprintf(chipID, "%lx", chipid);
    ALOGD ("%s: chidId=%s", __FUNCTION__, chipID);

    readOptionalConfig(chipID);     // Read optional chip specific settings
    readOptionalConfig("fime");     // Read optional FIME specific settings
    getNfaValues();                 // Get NFA configuration values into variables


    findPatchramFile(FW_PATCH, sPatchFn, sizeof(sPatchFn));
    findPatchramFile(FW_PRE_PATCH, sPrePatchFn, sizeof(sPatchFn));

    {
        FILE *fd;
        /* If an I2C fix patch file was specified, then tell the stack about it */
        if (sPrePatchFn[0] != '\0')
        {
            if ((fd = fopen(sPrePatchFn, "rb")) != NULL)
            {
                UINT32 lenPrmBuffer = getFileLength(fd);

                if ((sI2cFixPrmBuf = malloc(lenPrmBuffer)) != NULL)
                {
                    size_t actualLen = fread(sI2cFixPrmBuf, 1, lenPrmBuffer, fd);
                    if (actualLen == lenPrmBuffer)
                    {
                        ALOGD("%s Setting I2C fix to %s (size: %lu)", __FUNCTION__, sPrePatchFn, lenPrmBuffer);
                        HAL_NfcPrmSetI2cPatch((UINT8*)sI2cFixPrmBuf, (UINT16)lenPrmBuffer, 0);
                    }
                    else
                        ALOGE("%s fail reading i2c fix; actual len=%u; expected len=%lu", __FUNCTION__, actualLen, lenPrmBuffer);
                }
                else
                {
                    ALOGE("%s Unable to get buffer to i2c fix (%lu bytes)", __FUNCTION__, lenPrmBuffer);
                }

                fclose(fd);
            }
            else
            {
                ALOGE("%s Unable to open i2c fix patchfile %s", __FUNCTION__, sPrePatchFn);
            }
        }
    }

    {
        FILE *fd;

        /* If a patch file was specified, then download it now */
        if (sPatchFn[0] != '\0')
        {
            UINT32 bDownloadStarted = false;

            /* open patchfile, read it into a buffer */
            if ((fd = fopen(sPatchFn, "rb")) != NULL)
            {
                UINT32 lenPrmBuffer = getFileLength(fd);
                ALOGD("%s Downloading patchfile %s (size: %lu) format=%u", __FUNCTION__, sPatchFn, lenPrmBuffer, NFC_HAL_PRM_FORMAT_NCD);
                if ((sPrmBuf = malloc(lenPrmBuffer)) != NULL)
                {
                    size_t actualLen = fread(sPrmBuf, 1, lenPrmBuffer, fd);
                    if (actualLen == lenPrmBuffer)
                    {
                        if (!SpdHelper::isPatchBad((UINT8*)sPrmBuf, lenPrmBuffer))
                        {
                            /* Download patch using static memeory mode */
                            HAL_NfcPrmDownloadStart(NFC_HAL_PRM_FORMAT_NCD, 0, (UINT8*)sPrmBuf, lenPrmBuffer, 0, prmCallback);
                            bDownloadStarted = true;
                        }
                    }
                    else
                        ALOGE("%s fail reading patchram", __FUNCTION__);
                }
                else
                    ALOGE("%s Unable to buffer to hold patchram (%lu bytes)", __FUNCTION__, lenPrmBuffer);

                fclose(fd);
            }
            else
                ALOGE("%s Unable to open patchfile %s", __FUNCTION__, sPatchFn);

            /* If the download never got started */
            if (!bDownloadStarted)
            {
                /* If debug mode, fail in an obvious way, otherwise try to start stack */
                postDownloadPatchram(SpdHelper::isSpdDebug() ? HAL_NFC_STATUS_FAILED :
                        HAL_NFC_STATUS_OK);
            }
        }
        else
        {
            ALOGE("%s: No patchfile specified or disabled. Proceeding to post-download procedure...", __FUNCTION__);
            postDownloadPatchram(HAL_NFC_STATUS_OK);
        }
    }

    ALOGD ("%s: exit", __FUNCTION__);
}

/*******************************************************************************
**
** Function:    nfc_hal_post_reset_init
**
** Description: Called by the NFC HAL after controller has been reset.
**              Begin to download firmware patch files.
**
** Returns:     none
**
*******************************************************************************/
void nfc_hal_post_reset_init (UINT32 brcm_hw_id, UINT8 nvm_type)
{
    ALOGD("%s: brcm_hw_id=0x%lx, nvm_type=%d", __FUNCTION__, brcm_hw_id, nvm_type);
    tHAL_NFC_STATUS stat = HAL_NFC_STATUS_FAILED;
    UINT8 max_credits = 1;

    if (nvm_type == NCI_SPD_NVM_TYPE_NONE)
    {
        ALOGD("%s: No NVM detected, FAIL the init stage to force a retry", __FUNCTION__);
        USERIAL_PowerupDevice (0);
        stat = HAL_NfcReInit ();
    }
    else
    {
        /* Start downloading the patch files */
        StartPatchDownload(brcm_hw_id);

        if (GetNumValue(MAX_RF_DATA_CREDITS, &max_credits, sizeof(max_credits)) && (max_credits > 0))
        {
            ALOGD("%s : max_credits=%d", __FUNCTION__, max_credits);
            HAL_NfcSetMaxRfDataCredits(max_credits);
        }
    }
}


/*******************************************************************************
**
** Function:        mayDisableSecureElement
**
** Description:     Optionally adjust a TLV to disable secure element.  This feature
**                  is enabled by setting the system property
**                  nfc.disable_secure_element to a bit mask represented by a hex
**                  octet: C0 = do not detect any secure element.
**                         40 = do not detect secure element in slot 0.
**                         80 = do not detect secure element in slot 1.
**
**                  config: a sequence of TLV's.
**
*******************************************************************************/
void mayDisableSecureElement (StartupConfig& config)
{
    unsigned int bitmask = 0;
    char valueStr [PROPERTY_VALUE_MAX] = {0};
    int len = property_get ("nfc.disable_secure_element", valueStr, "");
    if (len > 0)
    {
        sscanf (valueStr, "%x", &bitmask); //read system property as a hex octet
        ALOGD ("%s: disable 0x%02X", __FUNCTION__, (UINT8) bitmask);
        config.disableSecureElement ((UINT8) (bitmask & 0xC0));
    }
}
