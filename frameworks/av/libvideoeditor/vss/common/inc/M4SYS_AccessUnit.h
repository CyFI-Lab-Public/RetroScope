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
 * @file         M4SYS_AccessUnit.h
 * @brief        Access unit manipulation
 * @note         This file defines the access unit structure,
 *               and declares functions to manipulate it.
 ************************************************************************
*/

#ifndef M4SYS_ACCESSUNIT_H
#define M4SYS_ACCESSUNIT_H

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Time.h"
#include "M4SYS_Stream.h"

/** The attribute of a fragment*/
typedef enum {
  M4SYS_kFragAttrOk        = 01, /**< The fragment is correct, there is no error
                                         (size cannot be 0)*/
  M4SYS_kFragAttrCorrupted = 02, /**< The fragment is corrupted (there is at least a bit or byte
                                        error somewhere in the fragment (size cannot be 0)*/
  M4SYS_kFragAttrLost      = 03  /**< The fragment is lost, so the size must be 0.*/
} M4SYS_FragAttr;


/** A Fragment is a piece of access unit. It can be decoded without decoding the others*/
typedef struct {
  M4OSA_MemAddr8  fragAddress;   /**< The data pointer. All fragments of the same access unit
                                        must be contiguous in memory*/
  M4OSA_UInt32    size;          /**< The size of the fragment. It must be 0 if fragment is
                                        flagged 'lost'*/
  M4SYS_FragAttr  isCorrupted;   /**< The attribute of this fragment*/
} M4SYS_Frag;

/**< The attribute of an access unit*/
typedef M4OSA_UInt8 M4SYS_AU_Attr;

#define AU_Corrupted   0x01 /**< At least one fragment of the access unit is flagged corrupted.*/
#define AU_P_Frame     0x02 /**< The access unit is a P_frame*/
#define AU_RAP         0x04 /**< The access unit is a random access point.*/


/** An access unit is the smallest piece of data with timing information.*/
typedef struct {
  M4SYS_StreamDescription*    stream ;
  M4OSA_MemAddr32             dataAddress; /**< The data pointer. The size of this block
                                            (allocated size) must be a 32-bits integer multiple*/
  M4OSA_UInt32                size;        /**< The size in bytes of the dataAddress. The size may
                                                 not match a 32-bits word boundary.*/
  M4OSA_Time                  CTS;         /**< The Composition Time Stamp*/
  M4OSA_Time                  DTS;         /**< The Decoded Time Stamp*/
  M4SYS_AU_Attr               attribute;   /**< The attribute of the access unit*/
  M4OSA_UInt8                 nbFrag;      /**< The number of fragments. It can be 0 if there is
                                                no fragment.*/
  M4SYS_Frag**                frag;        /**< An array of 'nbFrag' fragments. It stores the
                                                fragments structure. The original definition
                                              < of frag has been changed from M4SYS_Frag* frag[]
                                                to M4SYS_Frag** frag since the support
                                              < of such syntax is only a Microsoft extension of
                                                the C compiler. */
} M4SYS_AccessUnit;

/* Error codes */
#define M4ERR_AU_NO_MORE_FRAG      M4OSA_ERR_CREATE(M4_ERR,M4SYS_CMAPI,0x000001)
#define M4ERR_AU_BUFFER_OVERFLOW   M4OSA_ERR_CREATE(M4_ERR,M4SYS_CMAPI,0x000002)
#define M4ERR_AU_BAD_INDEX         M4OSA_ERR_CREATE(M4_ERR,M4SYS_CMAPI,0x000003)
#define M4ERR_NOT_ENOUGH_FRAG      M4OSA_ERR_CREATE(M4_ERR,M4SYS_CMAPI,0x000004)



#endif /*M4SYS_ACCESSUNIT_H*/

