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


#define LOG_TAG "NfcNciHal"
#include "OverrideLog.h"
#include <errno.h>
#include <hardware/hardware.h>
#include <hardware/nfc.h>
#include "HalAdaptation.h"


/*********************************
 * NCI HAL method implementations.
 *********************************/


static int hal_open (const struct nfc_nci_device *p_dev, nfc_stack_callback_t *p_hal_cback, nfc_stack_data_callback_t *p_hal_data_callback)
{
    int retval = 0;
    bcm2079x_dev_t *dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiOpen (dev, p_hal_cback, p_hal_data_callback);
    return retval;
}


static int hal_write (const struct nfc_nci_device *p_dev,
        uint16_t data_len, const uint8_t *p_data)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiWrite (dev, data_len, p_data);
    return retval;
}


static int hal_core_initialized (const struct nfc_nci_device *p_dev,
        uint8_t* p_core_init_rsp_params)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiCoreInitialized (dev, p_core_init_rsp_params);
    return retval;
}


static int hal_pre_discover (const struct nfc_nci_device *p_dev)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiPreDiscover (dev);
    return retval;
}


static int hal_close (const struct nfc_nci_device *p_dev)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiClose (dev);
    return retval;
}


static int hal_control_granted (const struct nfc_nci_device *p_dev)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiControlGranted (dev);
   return retval;
}


static int hal_power_cycle (const struct nfc_nci_device *p_dev)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiPowerCycle (dev);
    return retval;
}


static int hal_get_max_nfcee (const struct nfc_nci_device *p_dev, uint8_t* maxNfcee)
{
    int retval = 0;
    bcm2079x_dev_t* dev = (bcm2079x_dev_t*) p_dev;

    retval = HaiGetMaxNfcee (dev, maxNfcee);
    return retval;
}

/*************************************
 * Generic device handling.
 *************************************/


/* Close an opened nfc device instance */
static int nfc_close (hw_device_t *dev)
{
    int retval = 0;
    free (dev);
    retval = HaiTerminateLibrary ();
    return retval;
}


static int nfc_open (const hw_module_t* module, const char* name, hw_device_t** device)
{
    ALOGD ("%s: enter; name=%s", __FUNCTION__, name);
    int retval = 0; //0 is ok; -1 is error

    if (strcmp (name, NFC_NCI_CONTROLLER) == 0)
    {
        bcm2079x_dev_t *dev = calloc (1, sizeof(bcm2079x_dev_t));

        // Common hw_device_t fields
        dev->nci_device.common.tag = HARDWARE_DEVICE_TAG;
        dev->nci_device.common.version = 0x00010000; // [31:16] major, [15:0] minor
        dev->nci_device.common.module = (struct hw_module_t*) module;
        dev->nci_device.common.close = nfc_close;

        // NCI HAL method pointers
        dev->nci_device.open = hal_open;
        dev->nci_device.write = hal_write;
        dev->nci_device.core_initialized = hal_core_initialized;
        dev->nci_device.pre_discover = hal_pre_discover;
        dev->nci_device.close = hal_close;
        dev->nci_device.control_granted = hal_control_granted;
        dev->nci_device.power_cycle = hal_power_cycle;
        // TODO maco commented out for binary HAL compatibility
        // dev->nci_device.get_max_ee = hal_get_max_nfcee;


        // Copy in
        *device = (hw_device_t*) dev;

        retval = HaiInitializeLibrary (dev);
    }
    else
    {
        retval = -EINVAL;
    }
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


static struct hw_module_methods_t nfc_module_methods =
{
    .open = nfc_open,
};


struct nfc_nci_module_t HAL_MODULE_INFO_SYM =
{
    .common =
    {
        .tag = HARDWARE_MODULE_TAG, .module_api_version = 0x0100, // [15:8] major, [7:0] minor (1.0)
        .hal_api_version = 0x00, // 0 is only valid value
        .id = NFC_NCI_HARDWARE_MODULE_ID,
        .name = "Default NFC NCI HW HAL",
        .author = "The Android Open Source Project",
        .methods = &nfc_module_methods,
    },
};
