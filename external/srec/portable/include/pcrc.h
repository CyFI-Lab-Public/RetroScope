/*---------------------------------------------------------------------------*
 *  pcrc.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#ifndef PCRC_H
#define PCRC_H



#include "PortPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup PCRCModule PCRC API functions
 *
 * @{
 */

/**
 * Computes the CRC-encoding of the block of data.
 *
 * @param data The data on which to compute the CRC
 * @param size the size of the data.
 * @return the CRC of the data.
 */
PORTABLE_API unsigned int pcrcComputeData(const void *data, unsigned int size);


/**
 * Computes the CRC-encoding of a string.
 *
 * @param str The string for which to compute the CRC
 * @return the CRC of the string.
 */
PORTABLE_API unsigned int pcrcComputeString(const LCHAR *str);


/**
 * Initial value to pass to the pcrcUpdateData to ensist consistency with
 * pcrcComputeData.
 */
#define CRC_INITIAL_VALUE (~0U)

/**
 * Updates the CRC when adding a new byte.
 *
 * @param crc The initial crc value.
 * @param data datum to append to the crc
 * @param size the size of the data.
 * @return the new crc value.
 */
PORTABLE_API unsigned int pcrcUpdateData(unsigned int crc,
    const void * data,
    unsigned int size);
    
/**
 * @}
 */


#endif 
