/*---------------------------------------------------------------------------*
 *  ESR_SessionType.h  *
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

#ifndef __ESR_SESSIONTYPE_H
#define __ESR_SESSIONTYPE_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ESR_VariableTypes.h"
#include "ESR_SessionTypeListener.h"
#include "pstdio.h"
#include "ptypes.h"

/**
 * @addtogroup ESR_SessionTypeModule ESR_SessionType API functions
 * ESR_Session interface functions.
 *
 * @{
 */

/**
 * Hashmap with helper functions for adding primitives and add-if-empty.
 */
typedef struct ESR_SessionType_t
{
  /**
   * Returns session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @param type Expected variable type (for strong-typing purposes)
   * @return ESR_INVALID_RESULT_TYPE if the property is not of the specified type
   */
  ESR_ReturnCode(*getProperty)(struct ESR_SessionType_t* self, const LCHAR* name, void** value, VariableTypes type);
  /**
   * Returns the type of a property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param type [out] Value type
   * @return ESR_INVALID_ARGUMENT if self is null or property cannot be found
   */
  ESR_ReturnCode(*getPropertyType)(struct ESR_SessionType_t* self, const LCHAR* name, VariableTypes* type);
  /**
   * Returns copy of session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_RESULT_TYPE if the property is not a int
   */
  ESR_ReturnCode(*getInt)(struct ESR_SessionType_t* self, const LCHAR* name, int* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_RESULT_TYPE if the property is not a asr_uint16_t
   */
  ESR_ReturnCode(*getUint16_t)(struct ESR_SessionType_t* self, const LCHAR* name, asr_uint16_t* value);
  /**
   * Returns copy of session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_RESULT_TYPE if the property is not a size_t
   * @return ESR_INVALID_RESULT_TYPE if the property is not a size_t
   */
  ESR_ReturnCode(*getSize_t)(struct ESR_SessionType_t* self, const LCHAR* name, size_t* value);
  /**
   * Returns copy of session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_RESULT_TYPE if the property is not a float
   */
  ESR_ReturnCode(*getFloat)(struct ESR_SessionType_t* self, const LCHAR* name, float* value);
  /**
   * Returns copy of session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_RESULT_TYPE if the property is not a bool
   */
  ESR_ReturnCode(*getBool)(struct ESR_SessionType_t* self, const LCHAR* name, ESR_BOOL* value);
  /**
   * Returns copy of session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @param len Size of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*
   */
  ESR_ReturnCode(*getLCHAR)(struct ESR_SessionType_t* self, const LCHAR* name, LCHAR* value, size_t* len);
  /**
   * Indicates if key exists in the session.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param exists True if key exists, false otherwise
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*contains)(struct ESR_SessionType_t* self, const LCHAR* name, ESR_BOOL* exists);
  /**
   * Sets session property value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @param type Type of value being set
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setProperty)(struct ESR_SessionType_t* self, const LCHAR* name, void* value, VariableTypes type);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setInt)(struct ESR_SessionType_t* self, const LCHAR* name, int value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setUint16_t)(struct ESR_SessionType_t* self, const LCHAR* name, asr_uint16_t value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setSize_t)(struct ESR_SessionType_t* self, const LCHAR* name, size_t value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setFloat)(struct ESR_SessionType_t* self, const LCHAR* name, float value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setBool)(struct ESR_SessionType_t* self, const LCHAR* name, ESR_BOOL value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setLCHAR)(struct ESR_SessionType_t* self, const LCHAR* name, LCHAR* value);
  /**
   * If the key does not exist in the session, calls SessionSetInt().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setIntIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, int value);
  /**
   * If the key does not exist in the session, calls SessionSetUint16_t().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setUint16_tIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, asr_uint16_t value);
  /**
   * If the key does not exist in the session, calls SessionSetSize_t().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setSize_tIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, size_t value);
  /**
   * If the key does not exist in the session, calls SessionSetFloat().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setFloatIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, float value);
  /**
   * If the key does not exist in the session, calls SessionSetBool().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setBoolIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, ESR_BOOL value);
  /**
   * If the key does not exist in the session, calls SessionSetLCHAR().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @param value Property value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*setLCHARIfEmpty)(struct ESR_SessionType_t* self, const LCHAR* name, LCHAR* value);
  /**
   * Removes property from session.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if key cannot be found
   */
  ESR_ReturnCode(*removeProperty)(struct ESR_SessionType_t* self, const LCHAR* name);
  /**
   * Removes and deallocates property from session.
   *
   * @param self ESR_SessionType handle
   * @param name Property name
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if key cannot be found
   */
  ESR_ReturnCode(*removeAndFreeProperty)(struct ESR_SessionType_t* self, const LCHAR* name);
  /**
   * Imports commandline arguments into the system session.
   *
   * Keys are imported as "cmdline.[name]" where [name] is the name of the command-line argument
   * Values are set in char* format.
   *
   * For example, given the argument "-timer=5", the following key will be added to the session:
   * ["cmdline.timer", "5"]
   *
   * Validation is left up to the application.
   *
   * If the session contains a key that is clobbered by the parser, the old [key, value]
   * pair will be deallocated. For example, if the session contained
   * ["cmdline.timer", "value"] before the aforementioned example occured, then the old
   * [key, value] pair will be allocated by the parser.
   *
   * @param self ESR_SessionType handle
   * @param argc Number of arguments
   * @param argv Argument values
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if the system is out of memory;
   */
  ESR_ReturnCode(*importCommandLine)(struct ESR_SessionType_t* self, int argc, char* argv[]);
  /**
   * Returns the number of elements in the session.
   *
   * @param self ESR_SessionType handle
   * @param size [out] Session size
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*getSize)(struct ESR_SessionType_t* self, size_t* size);
  /**
   * Returns the key associated with the specified index.
   *
   * @param self ESR_SessionType handle
   * @param index Element index
   * @param key [out] Key name
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
   */
  ESR_ReturnCode(*getKeyAtIndex)(struct ESR_SessionType_t* self, size_t index, LCHAR** key);
  /**
   * Convert the specified argument to int.
   *
   * @param self ESR_SessionType handle
   * @param key Property name
   * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
   * ESR_INVALID_ARGUMENT if self is null or property cannot be converted to int
   */
  ESR_ReturnCode(*convertToInt)(struct ESR_SessionType_t* self, const LCHAR* key);
  
  /**
   * Convert the specified argument to asr_uint16_t.
   *
   * @param self ESR_SessionType handle
   * @param key Property name
  * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
  * ESR_INVALID_ARGUMENT if self is null or property cannot be converted to asr_uint16_t
   */
  ESR_ReturnCode(*convertToUint16_t)(struct ESR_SessionType_t* self, const LCHAR* key);
  
  /**
   * Convert the specified argument to size_t.
   *
   * @param self ESR_SessionType handle
   * @param key Property name
  * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
  * ESR_INVALID_ARGUMENT if self is null or property cannot be converted to size_t
   */
  ESR_ReturnCode(*convertToSize_t)(struct ESR_SessionType_t* self, const LCHAR* key);
  
  /**
   * Convert the specified argument to float.
   *
   * @param self ESR_SessionType handle
   * @param key Property name
  * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
  * ESR_INVALID_ARGUMENT if self is null or property cannot be converted to float
   */
  ESR_ReturnCode(*convertToFloat)(struct ESR_SessionType_t* self, const LCHAR* key);
  
  /**
   * Convert the specified argument to bool.
   *
   * @param self ESR_SessionType handle
   * @param key Property name
   * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
   * ESR_INVALID_ARGUMENT if self is null or property cannot be converted to bool
   */
  ESR_ReturnCode(*convertToBool)(struct ESR_SessionType_t* self, const LCHAR* key);
  /**
   * Destroys the Session.
   *
   * @param self ESR_SessionType handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*destroy)(struct ESR_SessionType_t* self);
  /**
   * Import PAR file into session.
   *
   * @param self ESR_SessionType handle
   * @param file File to read session from
   * @return ESR_INVALID_STATE if self is null; ESR_OPEN_ERROR if file cannot be opened; ESR_READ_ERROR if file cannot be
  * read; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*importParFile)(struct ESR_SessionType_t* self, const LCHAR* filename);
  /**
   * Import ARG file into session.
   *
   * @param self ESR_SessionType handle
   * @param file File to read arguments from
   */
  ESR_ReturnCode(*importArgFile)(struct ESR_SessionType_t* self, const LCHAR* filename);
  
  /**
   * Adds an event-listener.
   *
   * @param self ESR_SessionType handle
   * @param listener The event-listener to add
    * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*addListener)(struct ESR_SessionType_t* self, ESR_SessionTypeListenerPair* listener);
  
  /**
   * Removes an event-listener.
   *
   * @param self ESR_SessionType handle
   * @param listener The event-listener to remove
    * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*removeListener)(struct ESR_SessionType_t* self, ESR_SessionTypeListenerPair* listener);
  
  /**
   * Pointer to session data.
   */
  void* data;
}
ESR_SessionType;

/**
 * Initializes the system session.
 *
 * @param self ESR_SessionType handle
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionTypeCreate(ESR_SessionType** self);
/**
 * @}
 */


#endif /* __ESR_SESSIONTYPE_H */
