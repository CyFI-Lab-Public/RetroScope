/******************************************************************************
 *
 *  Copyright (C) 2012 Broadcom Corporation
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
 *  HAL Adaptation Interface (HAI). This interface regulates the interaction
 *  between standard Android HAL and Broadcom-specific HAL.  It adapts
 *  Broadcom-specific features to the Android framework.
 *
 ******************************************************************************/
#pragma once
#include <hardware/hardware.h>
#include <hardware/nfc.h>


#ifdef __cplusplus
extern "C" {
#endif


typedef struct
{
    struct nfc_nci_device nci_device;
    //below declarations are private variables within Broadcom HAL
    void* data;
}
bcm2079x_dev_t;


/*
All functions return POSIX error codes (see errno):
  0 means success.
  non-zero means failure; for example EACCES.
*/


extern int HaiInitializeLibrary (const bcm2079x_dev_t* device);
extern int HaiTerminateLibrary ();
extern int HaiOpen (const bcm2079x_dev_t* device, nfc_stack_callback_t* halCallbackFunc, nfc_stack_data_callback_t* halDataCallbackFunc);
extern int HaiClose (const bcm2079x_dev_t* device);
extern int HaiCoreInitialized (const bcm2079x_dev_t* device, uint8_t* coreInitResponseParams);
extern int HaiWrite (const bcm2079x_dev_t* dev, uint16_t dataLen, const uint8_t* data);
extern int HaiPreDiscover (const bcm2079x_dev_t* device);
extern int HaiControlGranted (const bcm2079x_dev_t* device);
extern int HaiPowerCycle (const bcm2079x_dev_t* device);
extern int HaiGetMaxNfcee (const bcm2079x_dev_t* device, uint8_t* maxNfcee);


#ifdef __cplusplus
}
#endif
