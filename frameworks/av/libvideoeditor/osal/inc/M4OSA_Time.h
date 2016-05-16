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
 * @file         M4OSA_Time.h
 * @ingroup      OSAL
 * @brief        Time macros
 * @note         This file defines time type and associated macros which must
 *               be used to manipulate time.
 ************************************************************************
*/

/* $Id: M4OSA_Time.h,v 1.2 2007/01/05 13:12:22 thenault Exp $ */

#ifndef M4OSA_TIME_H
#define M4OSA_TIME_H


#include "M4OSA_Types.h"


typedef signed long long  M4OSA_Time;


/** This macro sets the unknown time value */

#define M4OSA_TIME_UNKNOWN 0x80000000

/** This macro converts a time with a time scale to millisecond.
    The result is a M4OSA_Double*/
#define M4OSA_TIME_TO_MS(result, time, timescale)\
      { result = (1000*(M4OSA_Double)time)/((M4OSA_Double)timescale); }

#endif /*M4OSA_TIME_H*/

