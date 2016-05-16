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
 * \file phDalNfc_i2c.h
 * \brief DAL I2C port implementation for linux
 *
 * Project: Trusted NFC Linux
 *
 */

/**< Basic type definitions */
#include <phNfcTypes.h>
/**< Generic Interface Layer Function Definitions */
#include <phNfcInterface.h>
#include <phDal4Nfc.h>

void      phDal4Nfc_i2c_initialize(void);
void      phDal4Nfc_i2c_set_open_from_handle(phHal_sHwReference_t * pDalHwContext);
int       phDal4Nfc_i2c_is_opened(void);
void      phDal4Nfc_i2c_flush(void);
void      phDal4Nfc_i2c_close(void);
NFCSTATUS phDal4Nfc_i2c_open_and_configure(pphDal4Nfc_sConfig_t pConfig, void ** pLinkHandle);
int       phDal4Nfc_i2c_read(uint8_t * pBuffer, int nNbBytesToRead);
int       phDal4Nfc_i2c_write(uint8_t * pBuffer, int nNbBytesToWrite);
int 	  phDal4Nfc_i2c_reset(long level);
