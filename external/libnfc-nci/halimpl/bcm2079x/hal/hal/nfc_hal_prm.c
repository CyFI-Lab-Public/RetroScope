/******************************************************************************
 *
 *  Copyright (C) 2012-2013 Broadcom Corporation
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

#include <string.h>
#include "nfc_hal_int.h"
#include "userial.h"

/*****************************************************************************
* Definitions
*****************************************************************************/

/* Internal flags */
#define NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF  0x01    /* Application provided patchram in a single buffer */
#define NFC_HAL_PRM_FLAGS_RFU               0x02    /* Reserved for future use */
#define NFC_HAL_PRM_FLAGS_SIGNATURE_SENT    0x04    /* Signature sent to NFCC */
#define NFC_HAL_PRM_FLAGS_I2C_FIX_REQUIRED  0x08    /* PreI2C patch required */
#define NFC_HAL_PRM_FLAGS_BCM20791B3        0x10    /* B3 Patch (no RESET_NTF after patch download) */

/* Secure patch download definitions */
#define NFC_HAL_PRM_NCD_PATCHFILE_HDR_LEN  7       /* PRJID + MAJORVER + MINORVER + COUNT */

/* Enumeration of power modes IDs */
#define NFC_HAL_PRM_SPD_POWER_MODE_LPM     0
#define NFC_HAL_PRM_SPD_POWER_MODE_FPM     1

/* Version string for BCM20791B3 */
const UINT8 NFC_HAL_PRM_BCM20791B3_STR[]   = "20791B3";
#define NFC_HAL_PRM_BCM20791B3_STR_LEN     (sizeof (NFC_HAL_PRM_BCM20791B3_STR)-1)

#define NFC_HAL_PRM_SPD_TOUT                   (6000)  /* timeout for SPD events (in ms)   */
#define NFC_HAL_PRM_END_DELAY                  (250)   /* delay before sending any new command (ms)*/

#if (NFC_HAL_PRM_DEBUG == TRUE)
#define NFC_HAL_PRM_STATE(str)  HAL_TRACE_DEBUG2 ("%s st: %d", str, nfc_hal_cb.prm.state)
#else
#define NFC_HAL_PRM_STATE(str)
#endif

void nfc_hal_prm_post_baud_update (tHAL_NFC_STATUS status);

/*****************************************************************************
** Extern variable from nfc_hal_dm_cfg.c
*****************************************************************************/
extern tNFC_HAL_CFG *p_nfc_hal_cfg;

/*******************************************************************************
**
** Function         nfc_hal_prm_spd_handle_download_complete
**
** Description      Patch download complete (for secure patch download)
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_handle_download_complete (UINT8 event)
{
    nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_IDLE;

    /* Notify application now */
    if (nfc_hal_cb.prm.p_cback)
        (nfc_hal_cb.prm.p_cback) (event);
}

/*******************************************************************************
**
** Function         nfc_hal_prm_spd_send_next_segment
**
** Description      Send next patch segment (for secure patch download)
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_send_next_segment (void)
{
    UINT8   *p_src;
    UINT16  len, offset = nfc_hal_cb.prm.cur_patch_offset;
    UINT8   hcit, oid, hdr0, type;
    UINT8   chipverlen;
    UINT8   chipverstr[NCI_SPD_HEADER_CHIPVER_LEN];
    UINT8   patch_hdr_size = NCI_MSG_HDR_SIZE + 1; /* 1 is for HCIT */

    /* Validate that segment is at least big enought to have NCI_MSG_HDR_SIZE + 1 (hcit) */
    if (nfc_hal_cb.prm.cur_patch_len_remaining < patch_hdr_size)
    {
        HAL_TRACE_ERROR0 ("Unexpected end of patch.");
        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
        return;
    }

    /* Parse NCI command header */
    p_src = (UINT8*) (nfc_hal_cb.prm.p_cur_patch_data + offset);
    STREAM_TO_UINT8 (hcit, p_src);
    STREAM_TO_UINT8 (hdr0, p_src);
    STREAM_TO_UINT8 (oid,  p_src);
    STREAM_TO_UINT8 (len,  p_src);
    STREAM_TO_UINT8 (type, p_src);


    /* Update number of bytes comsumed */
    nfc_hal_cb.prm.cur_patch_offset += (len + patch_hdr_size);
    nfc_hal_cb.prm.cur_patch_len_remaining -=  (len + patch_hdr_size);

    /* Check if sending signature byte */
    if (  (oid == NCI_MSG_SECURE_PATCH_DOWNLOAD )
        &&(type == NCI_SPD_TYPE_SIGNATURE)  )
    {
        nfc_hal_cb.prm.flags |= NFC_HAL_PRM_FLAGS_SIGNATURE_SENT;
    }
    /* Check for header */
    else if (  (oid == NCI_MSG_SECURE_PATCH_DOWNLOAD )
             &&(type == NCI_SPD_TYPE_HEADER)  )
    {
        /* Check if patch is for BCM20791B3 */
        p_src += NCI_SPD_HEADER_OFFSET_CHIPVERLEN;
        STREAM_TO_UINT8 (chipverlen, p_src);
        if (memcmp (nfc_hal_cb.nvm_cb.chip_ver, p_src, chipverlen) != 0)
        {
            HAL_TRACE_ERROR0 ("Unexpected chip ver.");
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
            return;
        }
        STREAM_TO_ARRAY (chipverstr, p_src, NCI_SPD_HEADER_CHIPVER_LEN);

        if (memcmp (NFC_HAL_PRM_BCM20791B3_STR, chipverstr, NFC_HAL_PRM_BCM20791B3_STR_LEN) == 0)
        {
            /* Patch is for BCM2079B3 - do not wait for RESET_NTF after patch download */
            nfc_hal_cb.prm.flags |= NFC_HAL_PRM_FLAGS_BCM20791B3;
        }
        else
        {
            /* Patch is for BCM2079B4 or newer - wait for RESET_NTF after patch download */
            nfc_hal_cb.prm.flags &= ~NFC_HAL_PRM_FLAGS_BCM20791B3;
        }
    }

    /* Send the command (not including HCIT here) */
    nfc_hal_dm_send_nci_cmd ((UINT8*) (nfc_hal_cb.prm.p_cur_patch_data + offset + 1), (UINT8) (len + NCI_MSG_HDR_SIZE),
                             nfc_hal_prm_nci_command_complete_cback);
}

/*******************************************************************************
**
** Function         nfc_hal_prm_spd_handle_next_patch_start
**
** Description      Handle start of next patch (for secure patch download)
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_handle_next_patch_start (void)
{
    UINT32  cur_patch_mask;
    UINT32  cur_patch_len;
    BOOLEAN found_patch_to_download = FALSE;

    while (!found_patch_to_download)
    {
        /* Get length of current patch */
        cur_patch_len = nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].len;

        /* Check if this is a patch we need to download */
        cur_patch_mask = ((UINT32) 1 << nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].power_mode);
        if (nfc_hal_cb.prm.spd_patch_needed_mask & cur_patch_mask)
        {
            found_patch_to_download = TRUE;
        }
        else
        {
            /* Do not need to download this patch. Skip to next patch */
            HAL_TRACE_DEBUG1 ("Skipping patch for power_mode %i.", nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].power_mode);

            nfc_hal_cb.prm.spd_cur_patch_idx++;
            if (nfc_hal_cb.prm.spd_cur_patch_idx >= nfc_hal_cb.prm.spd_patch_count)
            {
                /* No more to download */
                nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_COMPLETE_EVT);
                return;
            }
            else if (!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF))
            {
                /* Notify adaptation layer to call HAL_NfcPrmDownloadContinue with the next patch header */
                (nfc_hal_cb.prm.p_cback) (NFC_HAL_PRM_SPD_GET_NEXT_PATCH);
                return;
            }
            else
            {
                /* Patch in buffer. Skip over current patch. Check next patch */
                nfc_hal_cb.prm.cur_patch_len_remaining -= (UINT16) cur_patch_len;
                nfc_hal_cb.prm.cur_patch_offset += (UINT16) cur_patch_len;
            }
        }
    }


    /* Begin downloading patch */
    HAL_TRACE_DEBUG1 ("Downloading patch for power_mode %i.", nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].power_mode);
    nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_DOWNLOADING;
    nfc_hal_prm_spd_send_next_segment ();
}

#if (defined (NFC_HAL_PRE_I2C_PATCH_INCLUDED) && (NFC_HAL_PRE_I2C_PATCH_INCLUDED == TRUE))
/*******************************************************************************
**
** Function         nfc_hal_prm_spd_download_i2c_fix
**
** Description      Start downloading patch for i2c fix
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_download_i2c_fix (void)
{
    UINT8 *p, *p_start;
    UINT16 patchfile_project_id;
    UINT16 patchfile_ver_major;
    UINT16 patchfile_ver_minor;
    UINT16 patchfile_patchsize;
    UINT8 u8;

    HAL_TRACE_DEBUG0 ("Downloading I2C fix...");

    /* Save pointer and offset of patchfile, so we can resume after downloading the i2c fix */
    nfc_hal_cb.prm.spd_patch_offset = nfc_hal_cb.prm.cur_patch_offset;
    nfc_hal_cb.prm.spd_patch_len_remaining = nfc_hal_cb.prm.cur_patch_len_remaining;

    /* Initialize pointers for downloading i2c fix */
    nfc_hal_cb.prm.p_cur_patch_data = nfc_hal_cb.prm_i2c.p_patch;
    nfc_hal_cb.prm.cur_patch_offset = 0;
    nfc_hal_cb.prm.cur_patch_len_remaining = nfc_hal_cb.prm_i2c.len;

    /* Parse the i2c patchfile */
    if (nfc_hal_cb.prm.cur_patch_len_remaining >= NFC_HAL_PRM_NCD_PATCHFILE_HDR_LEN)
    {
        /* Parse patchfile header */
        p = (UINT8 *) nfc_hal_cb.prm.p_cur_patch_data;
        p_start = p;
        STREAM_TO_UINT16 (patchfile_project_id, p);
        STREAM_TO_UINT16 (patchfile_ver_major, p);
        STREAM_TO_UINT16 (patchfile_ver_minor, p);

        /* RFU */
        p++;

        /* Check how many patches are in the patch file */
        STREAM_TO_UINT8 (u8, p);

        /* Should only be one patch */
        if (u8 > 1)
        {
            HAL_TRACE_ERROR1 ("Invalid i2c fix: invalid number of patches (%i)", u8);
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
            return;
        }


        /* Get info about the i2c patch*/
        STREAM_TO_UINT8 (u8, p);                     /* power mode (not needed for i2c patch)    */
        STREAM_TO_UINT16 (patchfile_patchsize, p);   /* size of patch                            */

        /* 5 byte RFU */
        p += 5;

        /* Adjust length to exclude patchfiloe header */
        nfc_hal_cb.prm.cur_patch_len_remaining -= (UINT16) (p - p_start);       /* Adjust size of patchfile                        */
        nfc_hal_cb.prm.cur_patch_offset += (UINT16) (p - p_start);              /* Bytes of patchfile transmitted/processed so far */

        /* Begin sending patch to the NFCC */
        nfc_hal_prm_spd_send_next_segment ();
    }
    else
    {
        /* ERROR: Bad length for patchfile */
        HAL_TRACE_ERROR0 ("Invalid i2c fix: unexpected end of patch");
        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
    }
}
#endif /* NFC_HAL_PRE_I2C_PATCH_INCLUDED */

/*******************************************************************************
**
** Function         nfc_hal_prm_spd_check_version
**
** Description      Check patchfile version with current downloaded version
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_check_version (void)
{
    UINT8 *p, *p_start, i;
    UINT32 nvm_patch_present_mask = 0;
    UINT32 patchfile_patch_present_mask;
    UINT16 patchfile_project_id = 0;
    UINT16 patchfile_ver_major = 0;
    UINT16 patchfile_ver_minor = 0;
    UINT16 patchfile_patchsize;

    UINT8  return_code = NFC_HAL_PRM_COMPLETE_EVT;

    /* Initialize patchfile offset pointers */
    p = p_start = NULL;
    patchfile_patchsize = 0;

    /* the good patches in NVM */
    if (nfc_hal_cb.nvm_cb.lpm_size && !(nfc_hal_cb.nvm_cb.flags & (NFC_HAL_NVM_FLAGS_LPM_BAD)))
        nvm_patch_present_mask  |= (1 << NFC_HAL_PRM_SPD_POWER_MODE_LPM);

    if (nfc_hal_cb.nvm_cb.fpm_size && !(nfc_hal_cb.nvm_cb.flags & (NFC_HAL_NVM_FLAGS_FPM_BAD)))
        nvm_patch_present_mask  |= (1 << NFC_HAL_PRM_SPD_POWER_MODE_FPM);

    /* Get patchfile version */
    if (nfc_hal_cb.prm.cur_patch_len_remaining >= NFC_HAL_PRM_NCD_PATCHFILE_HDR_LEN)
    {
        /* Parse patchfile header */
        p       = (UINT8 *) nfc_hal_cb.prm.p_cur_patch_data;
        p_start = p;
        STREAM_TO_UINT16 (patchfile_project_id, p);
        STREAM_TO_UINT16 (patchfile_ver_major, p);
        STREAM_TO_UINT16 (patchfile_ver_minor, p);

        /* RFU */
        p++;

        /* Check how many patches are in the patch file */
        STREAM_TO_UINT8 (nfc_hal_cb.prm.spd_patch_count, p);

        if (nfc_hal_cb.prm.spd_patch_count > NFC_HAL_PRM_MAX_PATCH_COUNT)
        {
            HAL_TRACE_ERROR2 ("Unsupported patchfile (number of patches (%i) exceeds maximum (%i)",
                               nfc_hal_cb.prm.spd_patch_count, NFC_HAL_PRM_MAX_PATCH_COUNT);
        }

        /* Mask of patches that are present in the patchfile */
        patchfile_patch_present_mask = 0;

        /* Get lengths for each patch */
        for (i = 0; i < nfc_hal_cb.prm.spd_patch_count; i++)
        {
            /* Get power mode for this patch */
            STREAM_TO_UINT8 (nfc_hal_cb.prm.spd_patch_desc[i].power_mode, p);

            /* Update mask of power-modes present in the patchfile */
            patchfile_patch_present_mask |= ((UINT32) 1 << nfc_hal_cb.prm.spd_patch_desc[i].power_mode);

            /* Get length of patch */
            STREAM_TO_UINT16 (nfc_hal_cb.prm.spd_patch_desc[i].len, p);

            /* Add total size of patches */
            patchfile_patchsize += nfc_hal_cb.prm.spd_patch_desc[i].len;

            /* 5 byte RFU */
            p += 5;
        }

        /* Adjust offset to after the patch file header */
        nfc_hal_cb.prm.cur_patch_offset += (UINT16) (p - p_start);              /* Bytes of patchfile transmitted/processed so far */
        nfc_hal_cb.prm.cur_patch_len_remaining -= (UINT16) (p - p_start);       /* Adjust size of patchfile                        */


        HAL_TRACE_DEBUG4 ("NVM Patch info: flags=0x%04x,   Ver=%i.%i, PatchMask=0x%08x",
            nfc_hal_cb.nvm_cb.flags, nfc_hal_cb.nvm_cb.ver_major, nfc_hal_cb.nvm_cb.ver_minor, nvm_patch_present_mask );
        HAL_TRACE_DEBUG6 ("Patchfile info: ProjID=0x%04x,  Ver=%i.%i, Num patches=%i, PatchMask=0x%08x, PatchSize=%i",
                           patchfile_project_id, patchfile_ver_major, patchfile_ver_minor,
                           nfc_hal_cb.prm.spd_patch_count, patchfile_patch_present_mask, patchfile_patchsize);

        /*********************************************************************
        * Version check of patchfile against NVM
        ********************************************************************
        /* Download the patchfile if no patches in NVM */
        if ((nfc_hal_cb.nvm_cb.project_id == 0) || !(nfc_hal_cb.nvm_cb.flags & NFC_HAL_NVM_FLAGS_PATCH_PRESENT))
        {
            /* No patch in NVM, need to download all */
            nfc_hal_cb.prm.spd_patch_needed_mask = patchfile_patch_present_mask;

            HAL_TRACE_DEBUG2 ("No previous patch detected. Downloading patch %i.%i",
                              patchfile_ver_major, patchfile_ver_minor);
        }
        /* Skip download if project ID of patchfile does not match NVM */
        else if (nfc_hal_cb.nvm_cb.project_id != patchfile_project_id)
        {
            /* Project IDs mismatch */
            HAL_TRACE_DEBUG2 ("Patch download skipped: Mismatched Project ID (NVM ProjId: 0x%04x, Patchfile ProjId: 0x%04x)",
                              nfc_hal_cb.nvm_cb.project_id, patchfile_project_id);

            return_code = NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT;
        }
        /* Skip download if version of patchfile is equal to version in NVM */
        /*                  and patches of the power modes are the same as the good patches in NVM */
        else if (  (nfc_hal_cb.nvm_cb.ver_major == patchfile_ver_major)
                 &&(nfc_hal_cb.nvm_cb.ver_minor == patchfile_ver_minor)
                 &&((nvm_patch_present_mask | patchfile_patch_present_mask) == nvm_patch_present_mask)  ) /* if the NVM patch include all the patched in file */
        {
            HAL_TRACE_DEBUG2 ("Patch download skipped. NVM patch (version %i.%i) is the same than the patchfile ",
                              nfc_hal_cb.nvm_cb.ver_major, nfc_hal_cb.nvm_cb.ver_minor);

            return_code = NFC_HAL_PRM_COMPLETE_EVT;
        }
        /* Remaining cases: Download all patches in the patchfile */
        else
        {
            nfc_hal_cb.prm.spd_patch_needed_mask = patchfile_patch_present_mask;

            HAL_TRACE_DEBUG4 ("Downloading patch version: %i.%i (previous version in NVM: %i.%i)...",
                              patchfile_ver_major, patchfile_ver_minor,
                              nfc_hal_cb.nvm_cb.ver_major, nfc_hal_cb.nvm_cb.ver_minor);
        }

    }
    else
    {
        /* Invalid patch file header */
        HAL_TRACE_ERROR0 ("Invalid patch file header.");

        return_code = NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT;
    }

    /* If we need to download anything, get the first patch to download */
    if (nfc_hal_cb.prm.spd_patch_needed_mask)
    {
        HAL_TRACE_ERROR4 ("Downloading patch version: %i.%i (previous version in NVM: %i.%i)...",
                            patchfile_ver_major, patchfile_ver_minor,
                            nfc_hal_cb.nvm_cb.ver_major, nfc_hal_cb.nvm_cb.ver_minor);
#if (defined (NFC_HAL_PRE_I2C_PATCH_INCLUDED) && (NFC_HAL_PRE_I2C_PATCH_INCLUDED == TRUE))
        /* Check if I2C patch is needed: if                                     */
        /*      - I2C patch file was provided using HAL_NfcPrmSetI2cPatch, and        */
        /*      -   current patch in NVM has ProjectID=0, or                    */
        /*          FPM is not present or corrupted, or                         */
        /*          or patchfile is major-ver 76+                               */
        /*          or patchfile is not for B3 (always download for B4 onward)  */
        if (  (nfc_hal_cb.prm_i2c.p_patch)
            &&(  (nfc_hal_cb.nvm_cb.project_id == 0)
               ||(nfc_hal_cb.nvm_cb.fpm_size == 0)
               ||(nfc_hal_cb.nvm_cb.flags & NFC_HAL_NVM_FLAGS_FPM_BAD)
               ||(patchfile_ver_major >= 76)
               ||(!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_BCM20791B3)) ))
        {
            HAL_TRACE_DEBUG0 ("I2C patch fix required.");
            nfc_hal_cb.prm.flags |= NFC_HAL_PRM_FLAGS_I2C_FIX_REQUIRED;

            /* Download i2c fix first */
            nfc_hal_prm_spd_download_i2c_fix ();
            return;
        }
#endif  /* NFC_HAL_PRE_I2C_PATCH_INCLUDED */

        /* Download first segment */
        nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER;
        if (!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF))
        {
            /* Notify adaptation layer to call HAL_NfcPrmDownloadContinue with the next patch segment */
            (nfc_hal_cb.prm.p_cback) (NFC_HAL_PRM_SPD_GET_NEXT_PATCH);
        }
        else
        {
            nfc_hal_prm_spd_handle_next_patch_start ();
        }
    }
    else
    {
        static BOOLEAN firstTime = TRUE;
        if (firstTime)
        {
            HAL_TRACE_ERROR2 ("NVM patch version is %d.%d",
                              nfc_hal_cb.nvm_cb.ver_major, nfc_hal_cb.nvm_cb.ver_minor);
            firstTime = FALSE;
        }
        /* Download complete */
        nfc_hal_prm_spd_handle_download_complete (return_code);
    }
}

#if (NFC_HAL_TRACE_VERBOSE == TRUE)
/*******************************************************************************
**
** Function         nfc_hal_prm_spd_status_str
**
** Description      Return status string for a given spd status code
**
** Returns          Status string
**
*******************************************************************************/
UINT8 *nfc_hal_prm_spd_status_str (UINT8 spd_status_code)
{
    char *p_str;

    switch (spd_status_code)
    {
    case NCI_STATUS_SPD_ERROR_DEST:
        p_str = "SPD_ERROR_DEST";
        break;

    case NCI_STATUS_SPD_ERROR_PROJECTID:
        p_str = "SPD_ERROR_PROJECTID";
        break;

    case NCI_STATUS_SPD_ERROR_CHIPVER:
        p_str = "SPD_ERROR_CHIPVER";
        break;

    case NCI_STATUS_SPD_ERROR_MAJORVER:
        p_str = "SPD_ERROR_MAJORVER";
        break;

    case NCI_STATUS_SPD_ERROR_INVALID_PARAM:
        p_str = "SPD_ERROR_INVALID_PARAM";
        break;

    case NCI_STATUS_SPD_ERROR_INVALID_SIG:
        p_str = "SPD_ERROR_INVALID_SIG";
        break;

    case NCI_STATUS_SPD_ERROR_NVM_CORRUPTED:
        p_str = "SPD_ERROR_NVM_CORRUPTED";
        break;

    case NCI_STATUS_SPD_ERROR_PWR_MODE:
        p_str = "SPD_ERROR_PWR_MODE";
        break;

    case NCI_STATUS_SPD_ERROR_MSG_LEN:
        p_str = "SPD_ERROR_MSG_LEN";
        break;

    case NCI_STATUS_SPD_ERROR_PATCHSIZE:
        p_str = "SPD_ERROR_PATCHSIZE";
        break;

    default:
        p_str = "Unspecified Error";
        break;

    }

    return ((UINT8*) p_str);
}
#endif  /* (NFC_HAL_TRACE_VERBOSE == TRUE) */

/*******************************************************************************
**
** Function         nfc_hal_prm_nci_command_complete_cback
**
** Description      Callback for NCI vendor specific command complete
**                  (for secure patch download)
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_nci_command_complete_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data)
{
    UINT8 status, u8;
    UINT8 *p;
    UINT32 post_signature_delay;

    NFC_HAL_PRM_STATE ("nfc_hal_prm_nci_command_complete_cback");

    /* Stop the command-timeout timer */
    nfc_hal_main_stop_quick_timer (&nfc_hal_cb.prm.timer);

    /* Skip over NCI header */
    p = p_data + NCI_MSG_HDR_SIZE;

    /* Handle SECURE_PATCH_DOWNLOAD Rsp */
    if (event == NFC_VS_SEC_PATCH_DOWNLOAD_EVT)
    {
        /* Status and error code */
        STREAM_TO_UINT8 (status, p);
        STREAM_TO_UINT8 (u8, p);

        if (status != NCI_STATUS_OK)
        {
#if (NFC_HAL_TRACE_VERBOSE == TRUE)
            HAL_TRACE_ERROR2 ("Patch download failed, reason code=0x%X (%s)", status, nfc_hal_prm_spd_status_str (status));
#else
            HAL_TRACE_ERROR1 ("Patch download failed, reason code=0x%X", status);
#endif

            /* Notify application */
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
            return;
        }

        /* If last segment (SIGNATURE) sent */
        if (nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_SIGNATURE_SENT)
        {
            /* Wait for authentication complete (SECURE_PATCH_DOWNLOAD NTF), including time to commit to NVM (for BCM43341B0) */
            int auth_delay = NFC_HAL_PRM_SPD_TOUT;
            if (!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_BCM20791B3))
            {
                /* XXX maco only wait 30 seconds for B4+ revisions to avoid watchdog timeouts */
                auth_delay = NFC_HAL_PRM_COMMIT_DELAY;
            }
            nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_AUTHENTICATING;
            nfc_hal_main_start_quick_timer (&nfc_hal_cb.prm.timer, 0x00,
                                            (auth_delay * QUICK_TIMER_TICKS_PER_SEC) / 1000);
            return;
        }
        /* Download next segment */
        else if (nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF)
        {
            /* If patch is in a buffer, get next patch from buffer */
            nfc_hal_prm_spd_send_next_segment ();
        }
        else
        {
            /* Notify adaptation layer to get next patch segment (via HAL_NfcPrmDownloadContinue) */
            (nfc_hal_cb.prm.p_cback) (NFC_HAL_PRM_CONTINUE_EVT);
        }
    }
    /* Handle SECURE_PATCH_DOWNLOAD NTF */
    else if (event == NFC_VS_SEC_PATCH_AUTH_EVT)
    {
        HAL_TRACE_DEBUG1 ("prm flags:0x%x.", nfc_hal_cb.prm.flags);
        /* Status and error code */
        STREAM_TO_UINT8 (status, p);
        STREAM_TO_UINT8 (u8, p);

        /* Sanity check - should only get this NTF while in AUTHENTICATING stage */
        if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_AUTHENTICATING)
        {
            if (status != NCI_STATUS_OK)
            {
                HAL_TRACE_ERROR0 ("Patch authentication failed");
                nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_BAD_SIGNATURE_EVT);
                return;
            }

#if (defined (NFC_HAL_PRE_I2C_PATCH_INCLUDED) && (NFC_HAL_PRE_I2C_PATCH_INCLUDED == TRUE))
            if (nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_I2C_FIX_REQUIRED)
            {
                HAL_TRACE_DEBUG1 ("PreI2C patch downloaded...waiting %i ms for NFCC to reboot.", nfc_hal_cb.prm_i2c.prei2c_delay);

                /* Restore pointers to patchfile */
                nfc_hal_cb.prm.flags &= ~NFC_HAL_PRM_FLAGS_I2C_FIX_REQUIRED;
                nfc_hal_cb.prm.p_cur_patch_data = nfc_hal_cb.prm.p_spd_patch;
                nfc_hal_cb.prm.cur_patch_offset = nfc_hal_cb.prm.spd_patch_offset;
                nfc_hal_cb.prm.cur_patch_len_remaining = nfc_hal_cb.prm.spd_patch_len_remaining;

                /* Resume normal patch download */
                nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER;
                nfc_hal_cb.prm.flags &= ~NFC_HAL_PRM_FLAGS_SIGNATURE_SENT;

                /* Post PreI2C delay */
                nfc_hal_main_start_quick_timer (&nfc_hal_cb.prm.timer, 0x00, (nfc_hal_cb.prm_i2c.prei2c_delay * QUICK_TIMER_TICKS_PER_SEC) / 1000);

                return;
            }
#endif  /* NFC_HAL_PRE_I2C_PATCH_INCLUDED */


            /* Wait for NFCC to save the patch to NVM */
            if (!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_BCM20791B3))
            {
                /* 20791B4 or newer - wait for RESET_NTF; including time to commit to NVM (for BCM20791B4+) */
                post_signature_delay = NFC_HAL_PRM_COMMIT_DELAY;
                HAL_TRACE_DEBUG1 ("Patch downloaded and authenticated. Waiting %i ms for RESET NTF...", post_signature_delay);

            }
            else if (nfc_hal_cb.nvm_cb.flags & NFC_HAL_NVM_FLAGS_NO_NVM)
            {
                /* No NVM. Wait for NFCC to restart */
                post_signature_delay = NFC_HAL_PRM_END_DELAY;
                HAL_TRACE_DEBUG1 ("Patch downloaded and authenticated. Waiting %i ms for NFCC to restart...", post_signature_delay);
            }
            else
            {
                /* Wait for NFCC to save the patch to NVM (need about 1 ms per byte) */
                post_signature_delay = nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].len;
                if (post_signature_delay < nfc_hal_cb.prm.patchram_delay)
                    post_signature_delay = nfc_hal_cb.prm.patchram_delay;
                HAL_TRACE_DEBUG1 ("Patch downloaded and authenticated. Waiting %i ms for NVM update to complete...", post_signature_delay);
            }

            nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_AUTH_DONE;

            nfc_hal_main_start_quick_timer (&nfc_hal_cb.prm.timer, 0x00,
                                            (post_signature_delay * QUICK_TIMER_TICKS_PER_SEC) / 1000);
        }
        else
        {
            HAL_TRACE_ERROR0 ("Got unexpected SECURE_PATCH_DOWNLOAD NTF");
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_EVT);
        }
    }
    /* Handle NCI_MSG_GET_PATCH_VERSION RSP */
    else if (event == NFC_VS_GET_PATCH_VERSION_EVT)
    {
        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_COMPLETE_EVT);
    }
    else
    {
        /* Invalid response from NFCC during patch download */
        HAL_TRACE_ERROR1 ("Invalid response from NFCC during patch download (opcode=0x%02X)", event);
        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_INVALID_PATCH_EVT);
    }

    NFC_HAL_PRM_STATE ("prm_nci_command_complete_cback");
}

/*******************************************************************************
**
** Function         nfc_hal_prm_nfcc_ready_to_continue
**
** Description      Continue to download patch or notify application completition
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_nfcc_ready_to_continue (void)
{
    UINT8 get_patch_version_cmd [NCI_MSG_HDR_SIZE] =
    {
        NCI_MTS_CMD|NCI_GID_PROP,
        NCI_MSG_GET_PATCH_VERSION,
        0x00
    };

    /* Clear the bit for the patch we just downloaded */
    nfc_hal_cb.prm.spd_patch_needed_mask &= ~ ((UINT32) 1 << nfc_hal_cb.prm.spd_patch_desc[nfc_hal_cb.prm.spd_cur_patch_idx].power_mode);

    /* Check if another patch to download */
    nfc_hal_cb.prm.spd_cur_patch_idx++;
    if ((nfc_hal_cb.prm.spd_patch_needed_mask) && (nfc_hal_cb.prm.spd_cur_patch_idx < nfc_hal_cb.prm.spd_patch_count))
    {
        nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER;
        nfc_hal_cb.prm.flags &= ~NFC_HAL_PRM_FLAGS_SIGNATURE_SENT;

        if (nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF)
        {
            /* If patch is in a buffer, get next patch from buffer */
            nfc_hal_prm_spd_handle_next_patch_start ();
        }
        else
        {
            /* Notify adaptation layer to get next patch header (via HAL_NfcPrmDownloadContinue) */
            (nfc_hal_cb.prm.p_cback) (NFC_HAL_PRM_SPD_GET_NEXT_PATCH);
        }

    }
    else
    {
        /* Done downloading */
        HAL_TRACE_DEBUG0 ("Patch downloaded and authenticated. Get new patch version.");
        /* add get patch info again to verify the effective FW version */
        nfc_hal_dm_send_nci_cmd (get_patch_version_cmd, NCI_MSG_HDR_SIZE, nfc_hal_prm_nci_command_complete_cback);
        nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_W4_GET_VERSION;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_prm_spd_reset_ntf
**
** Description      Received RESET NTF from NFCC, indicating it has completed
**                  reset after patch download.
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_spd_reset_ntf (UINT8 reset_reason, UINT8 reset_type)
{
    /* Check if we were expecting a RESET NTF */
    if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_AUTH_DONE)
    {
        HAL_TRACE_DEBUG2 ("Received RESET NTF after patch download (reset_reason=%i, reset_type=%i)", reset_reason, reset_type);

        /* Stop waiting for RESET NTF */
        nfc_hal_main_stop_quick_timer (&nfc_hal_cb.prm.timer);

        {
            /* Continue with patch download */
            nfc_hal_prm_nfcc_ready_to_continue ();
        }
    }
    else if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER)
    {
        HAL_TRACE_DEBUG0 ("Received RESET NTF after pre-I2C patch download. Proceeding with patch download...");

        /* Stop waiting for RESET NTF */
        nfc_hal_main_stop_quick_timer (&nfc_hal_cb.prm.timer);
        nfc_hal_prm_spd_handle_next_patch_start ();
    }
    else
    {
        HAL_TRACE_ERROR2 ("Received unexpected RESET NTF (reset_reason=%i, reset_type=%i)", reset_reason, reset_type);
    }
}

/*******************************************************************************
**
** Function:    nfc_post_final_baud_update
**
** Description: Called after baud rate udate
**
** Returns:     Nothing
**
*******************************************************************************/
void nfc_hal_prm_post_baud_update (tHAL_NFC_STATUS status)
{
    NFC_HAL_PRM_STATE ("nfc_hal_prm_post_baud_update");

    if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_AUTH_DONE)
    {
        /* Proceed with next step of patch download sequence */
        nfc_hal_prm_nfcc_ready_to_continue ();
    }
}

/*******************************************************************************
**
** Function         nfc_hal_prm_process_timeout
**
** Description      Process timer expireation for patch download
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_prm_process_timeout (void *p_tle)
{
    NFC_HAL_PRM_STATE ("nfc_hal_prm_process_timeout");

    if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_AUTH_DONE)
    {
        if (!(nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_BCM20791B3))
        {
            /* Timeout waiting for RESET NTF after signature sent */
            HAL_TRACE_ERROR0 ("Timeout waiting for RESET NTF after patch download");
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_EVT);
        }
        else
        {
            nfc_hal_prm_nfcc_ready_to_continue ();
        }
    }
    else if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER)
    {
        HAL_TRACE_DEBUG0 ("Delay after PreI2C patch download...proceeding to download firmware patch");
        nfc_hal_prm_spd_handle_next_patch_start ();
    }
    else if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_W4_GET_VERSION)
    {
        HAL_TRACE_DEBUG0 ("get patch version timeout???");
        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_COMPLETE_EVT);
    }
    else
    {
        HAL_TRACE_ERROR1 ("Patch download: command timeout (state=%i)", nfc_hal_cb.prm.state);

        nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_EVT);
    }

    NFC_HAL_PRM_STATE ("nfc_hal_prm_process_timeout");
}


/*******************************************************************************
**
** Function         HAL_NfcPrmDownloadStart
**
** Description      Initiate patch download
**
** Input Params
**                  format_type     patch format type
**                                  (NFC_HAL_PRM_FORMAT_BIN, NFC_HAL_PRM_FORMAT_HCD, or
**                                   NFC_HAL_PRM_FORMAT_NCD)
**
**                  dest_address    destination adderess (needed for BIN format only)
**
**                  p_patchram_buf  pointer to patchram buffer. If NULL,
**                                  then app must call HAL_NfcPrmDownloadContinue when
**                                  NFC_HAL_PRM_CONTINUE_EVT is received, to send the next
**                                  segment of patchram
**
**                  patchram_len    size of p_patchram_buf (if non-NULL)
**
**                  patchram_delay  The delay after each patch.
**                                  If the given value is less than the size of the patchram,
**                                  the size of patchram is used instead.
**
**                  p_cback         callback for download status
**
**
** Returns          TRUE if successful, otherwise FALSE
**
**
*******************************************************************************/
BOOLEAN HAL_NfcPrmDownloadStart (tNFC_HAL_PRM_FORMAT format_type,
                                 UINT32              dest_address,
                                 UINT8               *p_patchram_buf,
                                 UINT32              patchram_len,
                                 UINT32              patchram_delay,
                                 tNFC_HAL_PRM_CBACK  *p_cback)
{
    HAL_TRACE_API0 ("HAL_NfcPrmDownloadStart ()");

    memset (&nfc_hal_cb.prm, 0, sizeof (tNFC_HAL_PRM_CB));

    if (p_patchram_buf)
    {
        nfc_hal_cb.prm.p_cur_patch_data = p_patchram_buf;
        nfc_hal_cb.prm.cur_patch_offset = 0;
        nfc_hal_cb.prm.cur_patch_len_remaining = (UINT16) patchram_len;
        nfc_hal_cb.prm.flags |= NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF;

        if (patchram_len == 0)
            return FALSE;
    }

    nfc_hal_cb.prm.p_cback          = p_cback;
    nfc_hal_cb.prm.dest_ram         = dest_address;
    nfc_hal_cb.prm.format           = format_type;
    nfc_hal_cb.prm.patchram_delay   = patchram_delay;

    nfc_hal_cb.prm.timer.p_cback = nfc_hal_prm_process_timeout;

    if (format_type == NFC_HAL_PRM_FORMAT_NCD)
    {
        /* Store patch buffer pointer and length */
        nfc_hal_cb.prm.p_spd_patch             = p_patchram_buf;
        nfc_hal_cb.prm.spd_patch_len_remaining = (UINT16)patchram_len;
        nfc_hal_cb.prm.spd_patch_offset        = 0;

        /* If patch download is required, but no NVM is available, then abort */
        if ((p_nfc_hal_cfg->nfc_hal_prm_nvm_required) && (nfc_hal_cb.nvm_cb.flags & NFC_HAL_NVM_FLAGS_NO_NVM))
        {
            HAL_TRACE_ERROR0 ("This platform requires NVM and the NVM is not available - Abort");
            nfc_hal_prm_spd_handle_download_complete (NFC_HAL_PRM_ABORT_NO_NVM_EVT);
            return FALSE;
        }

        /* Compare patch version in NVM with version in patchfile */
        nfc_hal_cb.prm.state = NFC_HAL_PRM_ST_SPD_COMPARE_VERSION;
        if (nfc_hal_cb.prm.flags & NFC_HAL_PRM_FLAGS_USE_PATCHRAM_BUF)
        {
            /* If patchfile is in a buffer, get patch version from buffer */
            nfc_hal_prm_spd_check_version ();
        }
        else
        {
            /* If patchfile is not in a buffer, then request patchfile header from adaptation layer. */
            (nfc_hal_cb.prm.p_cback) (NFC_HAL_PRM_SPD_GET_PATCHFILE_HDR_EVT);
        }
    }
    else
    {
        HAL_TRACE_ERROR0 ("Unexpected patch format.");
        return FALSE;
    }

    return TRUE;
}

/*******************************************************************************
**
** Function         HAL_NfcPrmDownloadContinue
**
** Description      Send next segment of patchram to controller. Called when
**                  NFC_HAL_PRM_CONTINUE_EVT is received.
**
**                  Only needed if HAL_NfcPrmDownloadStart was called with
**                  p_patchram_buf=NULL
**
** Input Params     p_patch_data    pointer to patch data
**                  patch_data_len  patch data len
**
** Returns          TRUE if successful, otherwise FALSE
**
*******************************************************************************/
BOOLEAN HAL_NfcPrmDownloadContinue (UINT8 *p_patch_data,
                                    UINT16 patch_data_len)
{
    HAL_TRACE_API2 ("HAL_NfcPrmDownloadContinue ():state = %d, patch_data_len=%d",
                     nfc_hal_cb.prm.state, patch_data_len);

    /* Check if we are in a valid state for this API */
    if (  (nfc_hal_cb.prm.state != NFC_HAL_PRM_ST_SPD_COMPARE_VERSION)
        &&(nfc_hal_cb.prm.state != NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER)
        &&(nfc_hal_cb.prm.state != NFC_HAL_PRM_ST_SPD_DOWNLOADING)  )
        return FALSE;

    if (patch_data_len == 0)
        return FALSE;

    nfc_hal_cb.prm.cur_patch_offset = 0;
    nfc_hal_cb.prm.p_cur_patch_data = p_patch_data;
    nfc_hal_cb.prm.cur_patch_len_remaining = patch_data_len;

    /* Call appropriate handler */
    if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_COMPARE_VERSION)
    {
        nfc_hal_prm_spd_check_version ();
    }
    else if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER)
    {
        nfc_hal_prm_spd_handle_next_patch_start ();
    }
    else if (nfc_hal_cb.prm.state == NFC_HAL_PRM_ST_SPD_DOWNLOADING)
    {
        nfc_hal_prm_spd_send_next_segment ();
    }
    else
    {
        HAL_TRACE_ERROR1 ("Unexpected patch state:%d.", nfc_hal_cb.prm.state);
    }

    return TRUE;
}

/*******************************************************************************
**
** Function         HAL_NfcPrmSetI2cPatch
**
** Description      Specify patchfile for BCM20791B3 I2C fix. This fix
**                  must be downloaded prior to initial patch download for I2C
**                  transport
**
** Input Params     p_i2c_patchfile_buf: pointer to patch for i2c fix
**                  i2c_patchfile_len: length of patch
**                  prei2c_delay: the delay before downloading main patch
**                                if 0 is given, NFC_HAL_PRM_POST_I2C_FIX_DELAY is used instead.
**
** Returns          Nothing
**
**
*******************************************************************************/
void HAL_NfcPrmSetI2cPatch (UINT8 *p_i2c_patchfile_buf, UINT16 i2c_patchfile_len, UINT32 prei2c_delay)
{
#if (defined (NFC_HAL_PRE_I2C_PATCH_INCLUDED) && (NFC_HAL_PRE_I2C_PATCH_INCLUDED == TRUE))
    HAL_TRACE_API0 ("HAL_NfcPrmSetI2cPatch ()");

    nfc_hal_cb.prm_i2c.prei2c_delay    = NFC_HAL_PRM_POST_I2C_FIX_DELAY;
    if (prei2c_delay)
        nfc_hal_cb.prm_i2c.prei2c_delay = prei2c_delay;
    nfc_hal_cb.prm_i2c.p_patch = p_i2c_patchfile_buf;
    nfc_hal_cb.prm_i2c.len = i2c_patchfile_len;
#endif  /* NFC_HAL_PRE_I2C_PATCH_INCLUDED */
}

/*******************************************************************************
**
** Function         HAL_NfcPrmSetSpdNciCmdPayloadSize
**
** Description      Set Host-to-NFCC NCI message size for secure patch download
**
**                  This API must be called before calling HAL_NfcPrmDownloadStart.
**                  If the API is not called, then PRM will use the default
**                  message size.
**
**                  Typically, this API is only called for platforms that have
**                  message-size limitations in the transport/driver.
**
**                  Valid message size range: NFC_HAL_PRM_MIN_NCI_CMD_PAYLOAD_SIZE to 255.
**
** Returns          HAL_NFC_STATUS_OK if successful
**                  HAL_NFC_STATUS_FAILED otherwise
**
**
*******************************************************************************/
tHAL_NFC_STATUS HAL_NfcPrmSetSpdNciCmdPayloadSize (UINT8 max_payload_size)
{
    /* Validate: minimum size is NFC_HAL_PRM_MIN_NCI_CMD_PAYLOAD_SIZE */
    if (max_payload_size < NFC_HAL_PRM_MIN_NCI_CMD_PAYLOAD_SIZE)
    {
        HAL_TRACE_ERROR2 ("HAL_NfcPrmSetSpdNciCmdPayloadSize: invalid size (%i). Must be between %i and 255", max_payload_size, NFC_HAL_PRM_MIN_NCI_CMD_PAYLOAD_SIZE);
        return (HAL_NFC_STATUS_FAILED);
    }
    else
    {
        HAL_TRACE_API1 ("HAL_NfcPrmSetSpdNciCmdPayloadSize: new message size during download: %i", max_payload_size);
        nfc_hal_cb.ncit_cb.nci_ctrl_size = max_payload_size;
        return (HAL_NFC_STATUS_OK);
    }
}
