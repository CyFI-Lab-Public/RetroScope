/*
 * Copyright (C) 2011 The Android Open Source Project
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
 ************************************************************************
 * @file         M4OSA_Clock.h
 * @ingroup      OSAL
 * @brief        clock API
 ************************************************************************
*/

#ifndef M4OSA_CLOCH_H
#define M4OSA_CLOCK_H

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Time.h"


#define M4WAR_TIMESCALE_TOO_BIG    M4OSA_ERR_CREATE(M4_WAR,M4OSA_CLOCK,0x000001) /**< Time precision too high for the system*/
#define M4ERR_CLOCK_BAD_REF_YEAR   M4OSA_ERR_CREATE(M4_ERR,M4OSA_CLOCK,0x000001) /**< Input year of reference is neither 1900, nor 1970 nor 2000*/

#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_CLOCK_EXPORT_TYPE M4OSA_ERR M4OSA_clockGetTime(M4OSA_Time* pTime,
                                                M4OSA_UInt32 timescale);

#ifdef __cplusplus
}
#endif

#endif /*M4OSA_CLOCK_H*/

