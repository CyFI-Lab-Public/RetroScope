/******************************************************************************
 *
 *  Copyright (C) 2001-2012 Broadcom Corporation
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
//override any HAL-specific macros
#pragma once


#include "bt_types.h"

//NFC_HAL_TASK=0 is already defined in gki_hal_target.h; it executes the Broadcom HAL
#define USERIAL_HAL_TASK  1  //execute userial's read thread
#define GKI_RUNNER_HAL_TASK 2  //execute GKI_run(), which runs forever
#define GKI_MAX_TASKS  3 //total of 3 tasks

#define GKI_BUF0_MAX                16
#define GKI_BUF1_MAX                16

#define NFC_HAL_PRM_POST_I2C_FIX_DELAY (500)
