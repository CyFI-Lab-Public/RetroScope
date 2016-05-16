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
*************************************************************************
 * @file   M4PSW_MemoryInterface.c
 * @brief  Memory Interface
 * @note   Implementation of the osal memory functions
 *************************************************************************
*/

#include <stdlib.h>
#include <memory.h>

#include <time.h>
#include "M4OSA_Memory.h"
#ifndef M4VPS_ADVANCED_MEMORY_MANAGER
/**
 ************************************************************************
 * @fn         M4OSA_MemAddr32 M4OSA_32bitAlignedMalloc(M4OSA_UInt32 size,
 *                                          M4OSA_CoreID coreID,
 *                                          M4OSA_Char* string)
 * @brief      this function allocates a memory block (at least 32 bits aligned)
 * @note
 * @param      size (IN): size of allocated block in bytes
 * @param      coreID (IN): identification of the caller component
 * @param      string (IN): description of the allocated block (null terminated)
 * @return     address of the allocated block, M4OSA_NULL if no memory available
 ************************************************************************
*/

M4OSA_MemAddr32 M4OSA_32bitAlignedMalloc(M4OSA_UInt32 size,
                             M4OSA_CoreID coreID,
                             M4OSA_Char* string)
{
    M4OSA_MemAddr32 Address = M4OSA_NULL;

    /**
     * If size is 0, malloc on WIN OS allocates a zero-length item in
     * the heap and returns a valid pointer to that item.
     * On other platforms, malloc could returns an invalid pointer
     * So, DON'T allocate memory of 0 byte */
    if (size == 0)
    {
        return Address;
    }

    if (size%4 != 0)
    {
        size = size + 4 - (size%4);
    }

    Address = (M4OSA_MemAddr32) malloc(size);

    return Address;
}

#endif

