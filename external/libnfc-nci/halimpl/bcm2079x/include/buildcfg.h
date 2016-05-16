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
#pragma once
#include <string.h>
#include <memory.h>
#include <stdio.h>
#include "data_types.h"


#define BTE_APPL_MAX_USERIAL_DEV_NAME           (256)

#ifdef	__cplusplus
extern "C" {
#endif


extern UINT8 *scru_dump_hex (UINT8 *p, char *p_title, UINT32 len, UINT32 trace_layer, UINT32 trace_type);
void DispNci (UINT8 *p, UINT16 len, BOOLEAN is_recv);
#define DISP_NCI	(DispNci)


#ifdef	__cplusplus
};
#endif
