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

#ifndef __M4VD_TOOLS_H__
#define __M4VD_TOOLS_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "NXPSW_CompilerSwitches.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Types.h"
/* ----- bitstream parser ----- */

typedef struct
{
    M4OSA_UInt32 stream_byte;
    M4OSA_UInt32 stream_index;
    M4OSA_MemAddr8 in;

} M4VS_Bitstream_ctxt;

M4OSA_UInt32 M4VD_Tools_GetBitsFromMemory(M4VS_Bitstream_ctxt* parsingCtxt,
                                            M4OSA_UInt32 nb_bits);
M4OSA_ERR M4VD_Tools_WriteBitsToMemory(M4OSA_UInt32 bitsToWrite,
                                         M4OSA_MemAddr32 dest_bits,
                                         M4OSA_UInt8 offset, M4OSA_UInt8 nb_bits);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4VD_TOOLS_H__ */
