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
 * @file         M4OSA_OptionID.h
 * @ingroup      OSAL
 * @brief        Option ID macros
 * @note         This file defines macros to generate and analyze option ID.
 *               Option ID is used by M4YYY_ZZsetOption() and
 *               M4YYY_ZZgetOption() functions.
 ************************************************************************
*/

#ifndef M4OSA_OPTIONID_H
#define M4OSA_OPTIONID_H


#include "M4OSA_Types.h"

/** M4OSA_OptionID is a 32 bits unsigned integer.
- Right access (2 bits): Some options may have read only, write only or read
  and write access
- Core ID (14 bits): It is a unique ID for each core component
- SubOption ID (16 bits): To select which option in a specific core component
*/
typedef M4OSA_UInt32 M4OSA_OptionID;
typedef void*        M4OSA_DataOption;

#define M4_READ      0x01
#define M4_WRITE     0x02
#define M4_READWRITE 0x03

/* Macro to process M4OSA_OptionID */

/** This macro creates an optionID given read/write access,
    coreID and SubOptionID*/
#define M4OSA_OPTION_ID_CREATE(right, coreID, errorID)\
   (M4OSA_Int32)((((((M4OSA_UInt32)right)&0x03)<<30))+((((M4OSA_UInt32)coreID)&0x003FFF)<<16)+(((M4OSA_UInt32)errorID)&0x00FFFF))

/** This macro splits an optionID into read/write access,
    coreID and SubOptionID*/
#define M4OSA_OPTION_ID_SPLIT(optionID, right, coreID, errorID)\
   { right=(M4OSA_UInt8)((optionID)>>30);\
     coreID=(M4OSA_UInt16)(((optionID)>>16)&0x00003FFF);\
     errorID=(M4OSA_UInt32)((optionID)&0x0000FFFF); }

/** This macro returns 1 if the optionID is writable, 0 otherwise*/
#define M4OSA_OPTION_ID_IS_WRITABLE(optionID) ((((optionID)>>30)&M4_WRITE)!=0)

/** This macro returns 1 if the optionID is readable, 0 otherwise*/
#define M4OSA_OPTION_ID_IS_READABLE(optionID) ((((optionID)>>30)&M4_READ)!=0)

/** This macro returns 1 if the optionID has its core ID equal to 'coreID', 0 otherwise*/
#define M4OSA_OPTION_ID_IS_COREID(optionID, coreID)\
   (((((optionID)>>16)&0x003FFF) == (coreID)) ? M4OSA_TRUE:M4OSA_FALSE)


#endif   /*M4OSA_OPTIONID_H*/

