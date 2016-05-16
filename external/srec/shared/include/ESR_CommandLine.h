/*---------------------------------------------------------------------------*
 *  ESR_CommandLine.h  *
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

#ifndef __ESR_COMMANDLINE_H
#define __ESR_COMMANDLINE_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup ESR_CommandLineModule ESR_CommandLine API functions
 * ESR_Session interface functions.
 *
 * @{
 */

/**
 * Returns value of command-line argument.
 *
 * @param argc Number of arguments
 * @param argv Value of arguments
 * @param key Name of command-line argument to look up
 * @param value [out] Value of the argument
 * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *                     the required length is returned in this variable.
 * @return ESR_BUFFER_OVERFLOW if value buffer is not large enough to contain result;
 * ESR_NO_MATCH_ERROR if the specified command-line option could not be fonud
 */
ESR_SHARED_API ESR_ReturnCode ESR_CommandLineGetValue(int argc, const LCHAR* argv[],
    LCHAR* key, LCHAR* value,
    size_t* len);
    
/**
 * @}
 */


#endif /* __ESR_COMMANDLINE_H */
