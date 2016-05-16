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
#define LOG_TAG "NfcNciHal"
#include "gki.h"
extern "C"
{
    #include "nfc_hal_nv_co.h"
}
#include "nfc_hal_nv_ci.h"
#include "nfc_hal_int.h"
#include "config.h"
#include "CrcChecksum.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <string>


//directory of HAL's non-volatile storage
static const char* default_location = "/data/nfc";
static const char* filename_prefix = "/halStorage.bin";
static const std::string get_storage_location ();
void delete_hal_non_volatile_store (bool forceDelete);
void verify_hal_non_volatile_store ();


/*******************************************************************************
**
** Function         nfc_hal_nv_co_read
**
** Description      This function is called by NFA to read in data from the
**                  previously opened file.
**
** Parameters       p_buf   - buffer to read the data into.
**                  nbytes  - number of bytes to read into the buffer.
**
** Returns          void
**
**                  Note: Upon completion of the request, nfc_hal_nv_ci_read () is
**                        called with the buffer of data, along with the number
**                        of bytes read into the buffer, and a status.  The
**                        call-in function should only be called when ALL requested
**                        bytes have been read, the end of file has been detected,
**                        or an error has occurred.
**
*******************************************************************************/
void nfc_hal_nv_co_read (UINT8 *p_buf, UINT16 nbytes, UINT8 block)
{
    std::string fn = get_storage_location();
    char filename[256];

    fn.append (filename_prefix);
    if (fn.length() > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }
    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), block);

    ALOGD ("%s: buffer len=%u; file=%s", __FUNCTION__, nbytes, filename);
    int fileStream = open (filename, O_RDONLY);
    if (fileStream >= 0)
    {
        unsigned short checksum = 0;
        size_t actualReadCrc = read (fileStream, &checksum, sizeof(checksum));
        size_t actualReadData = read (fileStream, p_buf, nbytes);
        close (fileStream);
        if (actualReadData > 0)
        {
            ALOGD ("%s: data size=%u", __FUNCTION__, actualReadData);
            nfc_hal_nv_ci_read (actualReadData, NFC_HAL_NV_CO_OK, block);
        }
        else
        {
            ALOGE ("%s: fail to read", __FUNCTION__);
            nfc_hal_nv_ci_read (0, NFC_HAL_NV_CO_FAIL, block);
        }
    }
    else
    {
        ALOGD ("%s: fail to open", __FUNCTION__);
        nfc_hal_nv_ci_read (0, NFC_HAL_NV_CO_FAIL, block);
    }
}


/*******************************************************************************
**
** Function         nfc_hal_nv_co_write
**
** Description      This function is called by io to send file data to the
**                  phone.
**
** Parameters       p_buf   - buffer to read the data from.
**                  nbytes  - number of bytes to write out to the file.
**
** Returns          void
**
**                  Note: Upon completion of the request, nfc_hal_nv_ci_write () is
**                        called with the file descriptor and the status.  The
**                        call-in function should only be called when ALL requested
**                        bytes have been written, or an error has been detected,
**
*******************************************************************************/
void nfc_hal_nv_co_write (const UINT8 *p_buf, UINT16 nbytes, UINT8 block)
{
    std::string fn = get_storage_location();
    char filename[256];
    int fileStream = 0;

    fn.append (filename_prefix);
    if (fn.length() > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }
    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), block);
    ALOGD ("%s: bytes=%u; file=%s", __FUNCTION__, nbytes, filename);

    fileStream = open (filename, O_WRONLY | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
    if (fileStream >= 0)
    {
        unsigned short checksum = crcChecksumCompute (p_buf, nbytes);
        size_t actualWrittenCrc = write (fileStream, &checksum, sizeof(checksum));
        size_t actualWrittenData = write (fileStream, p_buf, nbytes);
        ALOGD ("%s: %d bytes written", __FUNCTION__, actualWrittenData);
        if ((actualWrittenData == nbytes) && (actualWrittenCrc == sizeof(checksum)))
        {
            nfc_hal_nv_ci_write (NFC_HAL_NV_CO_OK);
        }
        else
        {
            ALOGE ("%s: fail to write", __FUNCTION__);
            nfc_hal_nv_ci_write (NFC_HAL_NV_CO_FAIL);
        }
        close (fileStream);
    }
    else
    {
        ALOGE ("%s: fail to open, error = %d", __FUNCTION__, errno);
        nfc_hal_nv_ci_write (NFC_HAL_NV_CO_FAIL);
    }
}


/*******************************************************************************
**
** Function         get_storage_location
**
** Description      Get the absolute directory path of the HAL's storage location.
**
** Parameters       none
**
** Returns          Absolute path.
**
*******************************************************************************/
const std::string get_storage_location ()
{
    char buffer [100];
    memset (buffer, 0, sizeof(buffer));
    if (!GetStrValue (NAME_NFA_STORAGE, buffer, sizeof(buffer)))
        return default_location;
    else
        return std::string (buffer);
}


/*******************************************************************************
**
** Function         delete_hal_non_volatile_store
**
** Description      Delete all the content of the HAL's storage location.
**
** Parameters       forceDelete: unconditionally delete the storage.
**
** Returns          none
**
*******************************************************************************/
void delete_hal_non_volatile_store (bool forceDelete)
{
    static bool firstTime = true;
    std::string fn = get_storage_location();
    char filename[256];
    int stat = 0;

    if ((firstTime == false) && (forceDelete == false))
        return;
    firstTime = false;

    ALOGD ("%s", __FUNCTION__);

    fn.append (filename_prefix);
    if (fn.length() > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }

    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), DH_NV_BLOCK);
    remove (filename);
    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F3_NV_BLOCK);
    remove (filename);
    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F4_NV_BLOCK);
    remove (filename);
    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F2_NV_BLOCK);
    remove (filename);
}


/*******************************************************************************
**
** Function         verify_hal_non_volatile_store
**
** Description      Verify the content of all non-volatile store.
**
** Parameters       none
**
** Returns          none
**
*******************************************************************************/
void verify_hal_non_volatile_store ()
{
    ALOGD ("%s", __FUNCTION__);
    std::string fn = get_storage_location();
    char filename[256];
    bool isValid = false;

    fn.append (filename_prefix);
    if (fn.length() > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }

    snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), DH_NV_BLOCK);
    if (crcChecksumVerifyIntegrity (filename))
    {
        snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F3_NV_BLOCK);
        if (crcChecksumVerifyIntegrity (filename))
        {
            snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F4_NV_BLOCK);
            if (crcChecksumVerifyIntegrity (filename))
            {
                snprintf (filename, sizeof(filename), "%s%u", fn.c_str(), HC_F2_NV_BLOCK);
                if (crcChecksumVerifyIntegrity (filename))
                    isValid = true;
            }
        }
    }

    if (isValid == false)
        delete_hal_non_volatile_store (true);
}
