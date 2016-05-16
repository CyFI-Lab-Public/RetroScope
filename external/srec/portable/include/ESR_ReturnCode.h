/*---------------------------------------------------------------------------*
 *  ESR_ReturnCode.h  *
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

#ifndef ESR_RETURNCODE_H
#define ESR_RETURNCODE_H



#include "PortPrefix.h"

/**
 * @addtogroup ESR_PortableModule ESR_Portable API functions
 *
 * @{
 */

/**
 * Return-code values.
 */
typedef enum ESR_ReturnCode_t
{
  /*
   * Note: do not forget to modify ESR_rc2str when modifying this enum.
   */
  
  /**
   * Operation completed successfully.
   */
  ESR_SUCCESS,
  
  /**
   * Intermediate stage of operation completed successfully, we wish to indicate
   * that remainig stages of operation may proceed.
   */
  ESR_CONTINUE_PROCESSING,
  
  /**
   * Indicates a fatal error.
   */
  ESR_FATAL_ERROR,
  
  /**
   * Buffer overflow occured.
   */
  ESR_BUFFER_OVERFLOW,
  
  /**
   * Error typing to open an entity or the operation failed because the entity was not opened.
   */
  ESR_OPEN_ERROR,
  
  /**
   * Error trying to open an entity that is already open.
   */
  ESR_ALREADY_OPEN,
  
  /**
   * Error typing to close a entity or the operation failed because the entity was not closed.
   */
  ESR_CLOSE_ERROR,
  
  /**
   * Error trying to close a entity that was already closed.
   */
  ESR_ALREADY_CLOSED,
  
  /**
   * Error trying to read a file.
   */
  ESR_READ_ERROR,
  
  /**
   * Error trying to write to a entity.
   */
  ESR_WRITE_ERROR,
  
  /**
   * Error trying to flush a entity.
   */
  ESR_FLUSH_ERROR,
  
  /**
   * Error trying to seek a entity.
   */
  ESR_SEEK_ERROR,
  
  /**
   * Error trying to allocate memory.
   */
  ESR_OUT_OF_MEMORY,
  
  /**
   * Specified argument is out of bounds.
   */
  ESR_ARGUMENT_OUT_OF_BOUNDS,
  
  /**
   * Failed to locate the specified entity.
   */
  ESR_NO_MATCH_ERROR,
  
  /**
   * Passed in argument contains an invalid value. Such as when a NULL pointer
   * is passed in when when an actual value is expected.
   */
  ESR_INVALID_ARGUMENT,
  
  /**
   * Indicates that request functionality is not supported.
   */
  ESR_NOT_SUPPORTED,
  
  /**
   * Indicates that the object is not in a state such that the operation can
   * be succesfully performed.
   */
  ESR_INVALID_STATE,
  
  /**
   * Indicates that a thread could not be created.
   */
  ESR_THREAD_CREATION_ERROR,
  
  /**
   * Indicates that a resource with the same identifier already exists.
   */
  ESR_IDENTIFIER_COLLISION,
  
  /**
   * Indicates that the operation timed out.
   */
  ESR_TIMED_OUT,
  
  /**
   * Indicates that the object being retrieved isn't of the expected type.
   * For example, when retrieving an integer from a HashMap we find out the
   * value is actually of type float.
   */
  ESR_INVALID_RESULT_TYPE,
  
  /**
   * Indicates that the invoked function has not been implemented.
   */
  ESR_NOT_IMPLEMENTED,
  
  /**
   * A connection was forcibly closed by a peer. This normally results from
   * a loss of the connection on the remote socket due to a timeout or a reboot.
   */
  ESR_CONNECTION_RESET_BY_PEER,
  
  /**
   * Indicates that a process could not be created.
   */
  ESR_PROCESS_CREATE_ERROR,
  
  /**
   * Indicates that no matching TTS engine is available.
   */
  ESR_TTS_NO_ENGINE,
  
  /**
   * Indicates that an attempt to create a mutex failed because the OS is running out of resources.
   */
  ESR_MUTEX_CREATION_ERROR,
  
  /**
   * Indicates a deadlock situation has occured.
   */
  ESR_DEADLOCK
} ESR_ReturnCode;


/**
 * Checks the function return-code and if it is not ESR_SUCCESS, returns it.
 */
#define CHK(rc, x) do { if ((rc = (x)) != ESR_SUCCESS) goto CLEANUP; } while (0)

#include "ptypes.h"

/**
 * Given a return-code, returns its string representation.
 *
 * @param rc Return-code
 * @return String representation of return-code.
 */
PORTABLE_API const LCHAR* ESR_rc2str(const ESR_ReturnCode rc);

#ifdef _WIN32
/**
 * Called before entering any function.
 */
PORTABLE_API void _cdecl _penter(void);
/**
 * Called after exiting any function.
 */
PORTABLE_API void _cdecl _pexit(void);
#endif

/**
 * @}
 */

#endif 
