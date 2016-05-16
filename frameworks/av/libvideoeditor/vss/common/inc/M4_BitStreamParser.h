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
 * @file   M4_BitStreamParser.h
 * @brief  MPEG-4 File Format bit stream utility
 * @note   This file contains utility functions used to parse MPEG specific
 *         data structures.
 ************************************************************************
*/
#ifndef __M4_BITSTREAMPARSER_H__
#define __M4_BITSTREAMPARSER_H__

#include "M4OSA_Types.h"

/**
* M4_BitStreamParser_Init.
*
* Allocates the context and initializes internal data
*
* @param pContext   : A pointer to the context internally used by the package - ALLOCATED BY THE
*                    FUNCTION (M4OSA_NULL if allocation fails)
* @param bitStream  : A pointer to the bitstream - must be 32 bits as access are 32 bits
* @param size        : The size of the bitstream in bytes
*
*/
void M4_BitStreamParser_Init(void** pContext, void* pBitStream, M4OSA_Int32 size);

/**
 ************************************************************************
 * @brief    Clean up context
 * @param    pContext    (IN/OUT)  M4_BitStreamParser context.
 ************************************************************************
*/
void M4_BitStreamParser_CleanUp(void* pContext);

/**
 ************************************************************************
 * @brief    Read the next <length> bits in the bitstream.
 * @note    The function does not update the bitstream pointer.
 * @param    pContext    (IN/OUT) M4_BitStreamParser context.
 * @param    length        (IN) The number of bits to extract from the bitstream
 * @return    the read bits
 ************************************************************************
*/
M4OSA_UInt32 M4_BitStreamParser_ShowBits(void* pContext, M4OSA_Int32 length);

/**
 ************************************************************************
 * @brief    Increment the bitstream pointer of <length> bits.
 * @param    pContext    (IN/OUT) M4_BitStreamParser context.
 * @param    length        (IN) The number of bit to shift the bitstream
 ************************************************************************
*/
void M4_BitStreamParser_FlushBits(void* pContext, M4OSA_Int32 length);

/**
 ************************************************************************
 * @brief    Get a pointer to the current byte pointed by the bitstream pointer.
 * It does not update the bitstream pointer
 *
 * @param pContext   : A pointer to the context internally used by the package
 * @param length        : The number of bit to extract from the bitstream
 *
 * @returns the read bits
*/
M4OSA_UInt32 M4_BitStreamParser_GetBits(void* pContext,M4OSA_Int32 bitPos, M4OSA_Int32 length);

/**
* M4_BitStreamParser_Restart resets the bitstream indexes.
*
* @param pContext   : A pointer to the context internally used by the package
*
*/
void M4_BitStreamParser_Restart(void* pContext);

/**
 ************************************************************************
 * @brief    Get a pointer to the current byte pointed by the bitstream pointer.
 * @returns pointer to the current location in the bitstream
 * @note    It should be used carefully as the pointer is in the bitstream itself
 *            and no copy is made.
 * @param    pContext    (IN/OUT)  M4_BitStreamParser context.
*/
M4OSA_UInt8*  M4_BitStreamParser_GetCurrentbitStreamPointer(void* pContext);

/**
* M4_BitStreamParser_GetSize gets the size of the bitstream in bytes
*
* @param pContext   : A pointer to the context internally used by the package
*
* @returns the size of the bitstream in bytes
*/
M4OSA_Int32 M4_BitStreamParser_GetSize(void* pContext);

void M4_MPEG4BitStreamParser_Init(void** pContext, void* pBitStream, M4OSA_Int32 size);

/**
* getMpegLengthFromInteger returns a decoded size value from an encoded one (SDL)
*
* @param pContext   : A pointer to the context internally used by the package
* @param val : encoded value
*
* @returns size in a human readable form
*/

M4OSA_Int32 M4_MPEG4BitStreamParser_GetMpegLengthFromInteger(void* pContext, M4OSA_UInt32 val);


/**
 ************************************************************************
 * @brief    Decode an MPEG4 Systems descriptor size from an encoded SDL size data.
 * @note    The value is read from the current bitstream location.
 * @param    pContext    (IN/OUT)  M4_BitStreamParser context.
 * @return    Size in a human readable form
 ************************************************************************
*/
M4OSA_Int32 M4_MPEG4BitStreamParser_GetMpegLengthFromStream(void* pContext);

#endif /*__M4_BITSTREAMPARSER_H__*/

