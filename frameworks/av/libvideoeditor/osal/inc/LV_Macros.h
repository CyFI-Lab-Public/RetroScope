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

/*******************************************************************************
* @file        LV_Macros.h
* @par        NXP Software
* @brief    Macros definition for Smartphone team
*******************************************************************************/

#ifndef LV_MACROS_H
#define LV_MACROS_H

/*------------*/
/*    INCLUDES  */
/*------------*/
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"

/******************************************************************************
*
* CHECK_PTR(fct, p, err, errValue)
* @note    This macro checks the value p. If it is NULL, it sets the variable err
*           to errValue and jumps to the label <fct>_cleanUp. A trace is displayed
*           signalling the error, the function name and the line number.
*
******************************************************************************/
#define CHECK_PTR(fct, p, err, errValue) \
{ \
    if(M4OSA_NULL == (p)) \
    { \
        (err) = (errValue) ; \
        M4OSA_TRACE1_1((M4OSA_Char*)"" #fct "(L%d): " #p " is NULL, returning " #errValue "",__LINE__) ; \
        goto fct##_cleanUp; \
    } \
}

/******************************************************************************
*
* CHECK_ERR(fct, err)
* @note    This macro checks the value err. If it is not NULL, a trace is displayed
*           signalling the error, the function name and the line number. The macro
*           jumps to the label <fct>_cleanUp.
*
******************************************************************************/
#define CHECK_ERR(fct, err) \
{ \
    if(M4NO_ERROR != (err)) \
    { \
        M4OSA_TRACE1_2((M4OSA_Char*)"!!! " #fct "(L%d): ERROR 0x%.8x returned",\
                                                               __LINE__,err) ; \
        goto fct##_cleanUp; \
    } \
}


/******************************************************************************
*
* CHECK_ERR(fct, err)
* @note    This macro compares a current state with a state value. If they are different,
*           err is set to M4ERR_STATE.
*           A trace is displayed signalling the error, the function name and the line number.
*           The macro jumps to the label <fct>_cleanUp.
*
******************************************************************************/
#define    CHECK_STATE(fct, stateValue, state) \
{ \
    if((stateValue) != (state)) \
    { \
        M4OSA_TRACE1_1("" #fct " called in bad state %d", state) ; \
        (err) = M4ERR_STATE ; \
        goto fct##_cleanUp; \
    } \
}

/******************************************************************************
*
* SAFE_FREE(p)
* @note    This macro checks the value of p is not NULL. If it is NULL, it does
*           nothing. Else, p is de allocated and set to NULL.
*
******************************************************************************/
#define SAFE_FREE(p) \
{ \
    if(M4OSA_NULL != (p)) \
    { \
        free((p)) ; \
        (p) = M4OSA_NULL ; \
    } \
}



#endif /*---  LV_MACROS_H ---*/

