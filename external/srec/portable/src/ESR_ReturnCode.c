/*---------------------------------------------------------------------------*
 *  ESR_ReturnCode.c  *
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



#include "ESR_ReturnCode.h"

const LCHAR* ESR_rc2str(const ESR_ReturnCode rc)
{
    switch (rc) {
        case ESR_SUCCESS:                  return L("ESR_SUCCESS");
        case ESR_CONTINUE_PROCESSING:      return L("ESR_CONTINUE_PROCESSING");
        case ESR_FATAL_ERROR:              return L("ESR_FATAL_ERROR");
        case ESR_BUFFER_OVERFLOW:          return L("ESR_BUFFER_OVERFLOW");
        case ESR_OPEN_ERROR:               return L("ESR_OPEN_ERROR");
        case ESR_ALREADY_OPEN:             return L("ESR_ALREADY_OPEN");
        case ESR_CLOSE_ERROR:              return L("ESR_CLOSE_ERROR");
        case ESR_ALREADY_CLOSED:           return L("ESR_ALREADY_CLOSED");
        case ESR_READ_ERROR:               return L("ESR_READ_ERROR");
        case ESR_WRITE_ERROR:              return L("ESR_WRITE_ERROR");
        case ESR_FLUSH_ERROR:              return L("ESR_FLUSH_ERROR");
        case ESR_SEEK_ERROR:               return L("ESR_SEEK_ERROR");
        case ESR_OUT_OF_MEMORY:            return L("ESR_OUT_OF_MEMORY");
        case ESR_ARGUMENT_OUT_OF_BOUNDS:   return L("ESR_ARGUMENT_OUT_OF_BOUNDS");
        case ESR_NO_MATCH_ERROR:           return L("ESR_NO_MATCH_ERROR");
        case ESR_INVALID_ARGUMENT:         return L("ESR_INVALID_ARGUMENT");
        case ESR_NOT_SUPPORTED:            return L("ESR_NOT_SUPPORTED");
        case ESR_INVALID_STATE:            return L("ESR_INVALID_STATE");
        case ESR_THREAD_CREATION_ERROR:    return L("ESR_THREAD_CREATION_ERROR");
        case ESR_IDENTIFIER_COLLISION:     return L("ESR_IDENTIFIER_COLLISION");
        case ESR_TIMED_OUT:                return L("ESR_TIMED_OUT");
        case ESR_INVALID_RESULT_TYPE:      return L("ESR_INVALID_RESULT_TYPE");
        case ESR_NOT_IMPLEMENTED:          return L("ESR_NOT_IMPLEMENTED");
        case ESR_CONNECTION_RESET_BY_PEER: return L("ESR_CONNECTION_RESET_BY_PEER");
        case ESR_PROCESS_CREATE_ERROR:     return L("ESR_PROCESS_CREATE_ERROR");
        case ESR_TTS_NO_ENGINE:            return L("ESR_TTS_NO_ENGINE");
        case ESR_MUTEX_CREATION_ERROR:     return L("ESR_MUTEX_CREATION_ERROR");
        case ESR_DEADLOCK:                 return L("ESR_DEADLOCK");
    };
    return L("invalid return code");
}
