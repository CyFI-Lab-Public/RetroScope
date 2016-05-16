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
 * \file phDalNfc_uart.h
 * \brief DAL com port implementation for linux
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 07 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */

/**< Basic type definitions */
#include <phNfcTypes.h>
/**< Generic Interface Layer Function Definitions */
#include <phNfcInterface.h>
#include <phDal4Nfc.h>

void phDal4Nfc_uart_initialize(void);
void phDal4Nfc_uart_set_open_from_handle(phHal_sHwReference_t * pDalHwContext);
int phDal4Nfc_uart_is_opened(void);
void phDal4Nfc_uart_flush(void);
void phDal4Nfc_uart_close(void);
NFCSTATUS phDal4Nfc_uart_open_and_configure(pphDal4Nfc_sConfig_t pConfig, void ** pLinkHandle);
int phDal4Nfc_uart_read(uint8_t * pBuffer, int nNbBytesToRead);
int phDal4Nfc_uart_write(uint8_t * pBuffer, int nNbBytesToWrite);
int phDal4Nfc_uart_reset();
int phDal4Nfc_uart_download();
