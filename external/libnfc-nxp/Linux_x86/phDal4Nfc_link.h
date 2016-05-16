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
 * \file phDalNfc_link.h
 * \brief DAL generic link interface for linux
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 10 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */

/**< Basic type definitions */
#include <phNfcTypes.h>
/**< Generic Interface Layer Function Definitions */
#include <phNfcInterface.h>
#include <phDal4Nfc.h>

typedef void      (*phDal4Nfc_link_initialize_CB_t)           (void);
typedef void      (*phDal4Nfc_link_set_open_from_handle_CB_t) (phHal_sHwReference_t * pDalHwContext);
typedef int       (*phDal4Nfc_link_is_opened_CB_t)            (void);
typedef void      (*phDal4Nfc_link_flush_CB_t)                (void);
typedef void      (*phDal4Nfc_link_close_CB_t)                (void);
typedef NFCSTATUS (*phDal4Nfc_link_open_and_configure_CB_t)   (pphDal4Nfc_sConfig_t pConfig, void ** pLinkHandle);
typedef int       (*phDal4Nfc_link_read_CB_t)                 (uint8_t * pBuffer, int nNbBytesToRead);
typedef int       (*phDal4Nfc_link_write_CB_t)                (uint8_t * pBuffer, int nNbBytesToWrite);
typedef int       (*phDal4Nfc_link_download_CB_t)             (long level);
typedef int       (*phDal4Nfc_link_reset_CB_t)                (long level);


typedef struct
{
   phDal4Nfc_link_initialize_CB_t              init;
   phDal4Nfc_link_set_open_from_handle_CB_t    open_from_handle;
   phDal4Nfc_link_is_opened_CB_t               is_opened;
   phDal4Nfc_link_flush_CB_t                   flush;
   phDal4Nfc_link_close_CB_t                   close;
   phDal4Nfc_link_open_and_configure_CB_t      open_and_configure;
   phDal4Nfc_link_read_CB_t                    read;
   phDal4Nfc_link_write_CB_t                   write;
   phDal4Nfc_link_download_CB_t                download;
   phDal4Nfc_link_reset_CB_t                   reset;
} phDal4Nfc_link_cbk_interface_t;


