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

#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"

#include "M4VD_Tools.h"

/**
 ************************************************************************
 * @file   M4VD_Tools.c
 * @brief
 * @note   This file implements helper functions for Bitstream parser
 ************************************************************************
 */

M4OSA_UInt32 M4VD_Tools_GetBitsFromMemory(M4VS_Bitstream_ctxt* parsingCtxt,
     M4OSA_UInt32 nb_bits)
{
    M4OSA_UInt32    code;
    M4OSA_UInt32    i;
    code = 0;
    for (i = 0; i < nb_bits; i++)
    {
        if (parsingCtxt->stream_index == 8)
        {
            //M4OSA_memcpy( (M4OSA_MemAddr8)&(parsingCtxt->stream_byte), parsingCtxt->in,
            //     sizeof(unsigned char));
            parsingCtxt->stream_byte = (unsigned char)(parsingCtxt->in)[0];
            parsingCtxt->in++;
            //fread(&stream_byte, sizeof(unsigned char),1,in);
            parsingCtxt->stream_index = 0;
        }
        code = (code << 1);
        code |= ((parsingCtxt->stream_byte & 0x80) >> 7);

        parsingCtxt->stream_byte = (parsingCtxt->stream_byte << 1);
        parsingCtxt->stream_index++;
    }

    return code;
}

M4OSA_ERR M4VD_Tools_WriteBitsToMemory(M4OSA_UInt32 bitsToWrite,
                                     M4OSA_MemAddr32 dest_bits,
                                     M4OSA_UInt8 offset, M4OSA_UInt8 nb_bits)
{
    M4OSA_UInt8 i,j;
    M4OSA_UInt32 temp_dest = 0, mask = 0, temp = 1;
    M4OSA_UInt32 input = bitsToWrite;
    input = (input << (32 - nb_bits - offset));

    /* Put destination buffer to 0 */
    for(j=0;j<3;j++)
    {
        for(i=0;i<8;i++)
        {
            if((j*8)+i >= offset && (j*8)+i < nb_bits + offset)
            {
                mask |= (temp << ((7*(j+1))-i+j));
            }
        }
    }
    mask = ~mask;
    *dest_bits &= mask;

    /* Parse input bits, and fill output buffer */
    for(j=0;j<3;j++)
    {
        for(i=0;i<8;i++)
        {
            if((j*8)+i >= offset && (j*8)+i < nb_bits + offset)
            {
                temp = ((input & (0x80000000 >> offset)) >> (31-offset));
                //*dest_bits |= (temp << (31 - i));
                *dest_bits |= (temp << ((7*(j+1))-i+j));
                input = (input << 1);
            }
        }
    }

    return M4NO_ERROR;
}



