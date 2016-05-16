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
 * \file  phFriNfc_LlcpMacNfcip.h
 * \brief NFC LLCP MAC Mapping for NFCIP.
 *
 * Project: NFC-FRI
 *
 */

#ifndef PHFRINFC_LLCPMACNFCIP_H
#define PHFRINFC_LLCPMACNFCIP_H


/*include files*/
#include <phNfcTypes.h>
#include <phNfcLlcpTypes.h>
#include <phNfcStatus.h>
#include <phFriNfc.h>
 
/** 
 * \name MAC Mapping for NFCIP
 *
 * File: \ref phFriNfc_LlcpMacNfcip.h
 *
 */


/** \defgroup grp_fri_nfc_llcp_macnfcip NFCIP MAC Mapping
 *
 *  TODO
 *
 */
NFCSTATUS phFriNfc_LlcpMac_Nfcip_Register (phFriNfc_LlcpMac_t *LlcpMac);

#endif /* PHFRINFC_LLCPMACNFCIP_H */
