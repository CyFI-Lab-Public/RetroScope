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
 * @file        M4PSW_Trace.c
 * @brief        Trace function for trace macros
 * @note        This file gives the implementation of the trace function used
 *                in the trace instrumentation macros
 ************************************************************************
*/

#include <stdio.h>  /*for printf */
#include <stdarg.h> /* ANSI C macros and defs for variable args */
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Mutex.h"
/**
 ************************************************************************
 * @fn         M4OSA_ERR M4OSA_randInit()
 * @brief      this function initialize the number generator
 *               this function must be called once before any call to M4OSA_rand()
 *               need the stdlib and time libraries
 * @note
 * @param
 * @return     M4NO_ERROR
 ************************************************************************
*/

M4OSA_ERR M4OSA_randInit()
{
    int i;

    srand(time(NULL));

    /* Windows' rand is rotten, the first generated value after the init
    above is not random enough, so let's shake things a little... */

    for (i=0; i<100; i++) rand();

    return M4NO_ERROR;
}
/**
 ************************************************************************
 * @fn           M4OSA_ERR M4OSA_rand(M4OSA_Int32* out_value, M4OSA_UInt32 max_value)
 * @brief       This function gives a random number between 1 and max_value
 *               (inclusive) with approximately equal probability, and
 *               returns this number in out_value. For instance, a max_value
 *             of 6 will simulate a fair 6-sided dice roll.
 * @note
 * @param      out_value (OUT): on return, points to random result
 * @param       max_value (IN): max expected value
 * @return     M4NO_ERROR
 ************************************************************************
*/

M4OSA_ERR M4OSA_rand(M4OSA_Int32* out_value, M4OSA_UInt32 max_value)
{
    if( (out_value == M4OSA_NULL) || (max_value < 1) )
    {
        return M4ERR_PARAMETER;
    }

    (*out_value) = rand();
    /* notice this algorithm will only work for max_values such that the multiplication
    won't overflow, which means that max_value typically shouldn't go over the range of
    an Int16. */
    (*out_value) = (((*out_value) * max_value) / ((M4OSA_UInt32)RAND_MAX + 1)) + 1;

    return M4NO_ERROR;
}


