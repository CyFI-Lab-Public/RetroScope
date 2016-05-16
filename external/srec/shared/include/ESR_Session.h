/*---------------------------------------------------------------------------*
 *  ESR_Session.h  *
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

#ifndef __ESR_SESSION_H
#define __ESR_SESSION_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "pstdio.h"
#include "ptypes.h"
#include "ESR_SessionType.h"

/**
 * @addtogroup ESR_SessionModule ESR_Session API functions
 * Manages ESR session information.
 *
 * @{
 */

/**
 * Hashmap with helper functions for adding primitives and add-if-empty.
 */
typedef struct ESR_SessionSingleton_t
{
  /**
   * Returns session property value.
   *
   * @param name Property name
   * @param value Property value
   * @param type Expected variable type (for strong-typing purposes)
   */
  ESR_ReturnCode(*getProperty)(const LCHAR* name, void** value, VariableTypes type);
  /**
   * Returns the type of a property value.
   *
   * @param name Property name
   * @param type [out] Value type
   */
  ESR_ReturnCode(*getPropertyType)(const LCHAR* name, VariableTypes* type);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*getInt)(const LCHAR* name, int* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*getUint16_t)(const LCHAR* name, asr_uint16_t* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*getSize_t)(const LCHAR* name, size_t* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*getFloat)(const LCHAR* name, float* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*getBool)(const LCHAR* name, ESR_BOOL* value);
  /**
   * Returns copy of session property value.
   *
   * @param name Property name
   * @param value Property value
   * @param len Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   */
  ESR_ReturnCode(*getLCHAR)(const LCHAR* name, LCHAR* value, size_t* len);
  /**
   * Indicates if key exists in the session.
   *
   * @param name Property name
   * @param exists True if key exists, false otherwise
   */
  ESR_ReturnCode(*contains)(const LCHAR* name, ESR_BOOL* exists);
  /**
   * Sets session property value.
   *
   * @param name Property name
   * @param value Property value
   * @param type Type of value being set
   */
  ESR_ReturnCode(*setProperty)(const LCHAR* name, void* value, VariableTypes type);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setInt)(const LCHAR* name, int value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setUint16_t)(const LCHAR* name, asr_uint16_t value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setSize_t)(const LCHAR* name, size_t value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setFloat)(const LCHAR* name, float value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setBool)(const LCHAR* name, ESR_BOOL value);
  /**
   * Sets session property value, storing a copy of the value.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setLCHAR)(const LCHAR* name, LCHAR* value);
  /**
   * If the key does not exist in the session, calls SessionSetInt().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setIntIfEmpty)(const LCHAR* name, int value);
  /**
   * If the key does not exist in the session, calls SessionSetUint16_t().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setUint16_tIfEmpty)(const LCHAR* name, asr_uint16_t value);
  /**
   * If the key does not exist in the session, calls SessionSetSize_t().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setSize_tIfEmpty)(const LCHAR* name, size_t value);
  /**
   * If the key does not exist in the session, calls SessionSetFloat().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setFloatIfEmpty)(const LCHAR* name, float value);
  /**
   * If the key does not exist in the session, calls SessionSetBool().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setBoolIfEmpty)(const LCHAR* name, ESR_BOOL value);
  /**
   * If the key does not exist in the session, calls SessionSetLCHAR().
   *
   * This helper function aids implementation of "default values", overwriting
   * session values only if they have not been set already.
   *
   * @param name Property name
   * @param value Property value
   */
  ESR_ReturnCode(*setLCHARIfEmpty)(const LCHAR* name, LCHAR* value);
  /**
   * Removes property from session.
   *
   * @param name Property name
   */
  ESR_ReturnCode(*removeProperty)(const LCHAR* name);
  /**
   * Removes and deallocates property from session.
   *
   * @param name Property name
   */
  ESR_ReturnCode(*removeAndFreeProperty)(const LCHAR* name);
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
   * @param argc Number of arguments
   * @param argv Argument values
   */
  ESR_ReturnCode(*importCommandLine)(int argc, char* argv[]);
  /**
   * Returns the number of elements in the session.
   *
   * @param size [out] Session size
   */
  ESR_ReturnCode(*getSize)(size_t* size);
  /**
   * Returns the key associated with the specified index.
   *
   * @param index Element index
   * @param key [out] Key name
   */
  ESR_ReturnCode(*getKeyAtIndex)(size_t index, LCHAR** key);
  /**
   * Convert the specified argument to int.
   *
   * @param key Property name
   */
  ESR_ReturnCode(*convertToInt)(const LCHAR* key);
  /**
   * Convert the specified argument to asr_uint16_t.
   *
   * @param key Property name
   */
  ESR_ReturnCode(*convertToUint16_t)(const LCHAR* key);
  
  /**
   * Convert the specified argument to size_t.
   *
   * @param key Property name
   */
  ESR_ReturnCode(*convertToSize_t)(const LCHAR* key);
  
  /**
   * Convert the specified argument to float.
   *
   * @param key Property name
   */
  ESR_ReturnCode(*convertToFloat)(const LCHAR* key);
  
  /**
   * Convert the specified argument to bool.
   *
   * @param key Property name
   */
  ESR_ReturnCode(*convertToBool)(const LCHAR* key);
  /**
   * Destroys the system session.
   */
  ESR_ReturnCode(*destroy)(void);
  /**
   * Import PAR file into session.
   *
   * @param file File to read session from
   */
  ESR_ReturnCode(*importParFile)(const LCHAR* filename);
  /**
   * Import ARG file into session.
   *
   * @param file File to read arguments from
   */
  ESR_ReturnCode(*importArgFile)(const LCHAR* filename);
  
  /**
   * Pointer to session data.
   */
  void* data;
}
ESR_SessionSingleton;

/**
 * Initializes the system session.
 *
 * @param filename File to read session information from
 * @return ESR_OPEN_ERROR if file cannot be opened; ESR_READ_ERROR if file cannot be read;
 * ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionCreate(const LCHAR* filename);
/**
 * Returns session property value.
 *
 * @param name Property name
 * @param value Property value
 * @param type Expected variable type (for strong-typing purposes)
 * @return ESR_INVALID_RESULT_TYPE if the property is not of the specified type
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetProperty(const LCHAR* name, void** value,
    VariableTypes type);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_INVALID_RESULT_TYPE if the property is not an int
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetInt(const LCHAR* name, int* value);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_INVALID_RESULT_TYPE if the property is not a asr_uint16_t
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetUint16_t(const LCHAR* name, asr_uint16_t* value);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_INVALID_RESULT_TYPE if the property is not a size_t
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetSize_t(const LCHAR* name, size_t* value);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_INVALID_RESULT_TYPE if the property is not a float
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetFloat(const LCHAR* name, float* value);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_INVALID_RESULT_TYPE if the property is not a bool
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetBool(const LCHAR* name, ESR_BOOL* value);
/**
 * Returns copy of session property value.
 *
 * @param name Property name
 * @param value Property value
 * @param len Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetLCHAR(const LCHAR* name, LCHAR* value, size_t* len);
/**
 * Indicates if key exists in the session.
 *
 * @param name Property name
 * @param exists True if key exists, false otherwise
 * @return ESR_SUCCESS
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionContains(const LCHAR* name, ESR_BOOL* exists);
/**
 * Sets session property value.
 *
 * @param name Property name
 * @param value Property value
 * @param type Type of value being set
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetProperty(const LCHAR* name, void* value,
    VariableTypes type);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetInt(const LCHAR* name, int value);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetUint16_t(const LCHAR* name, asr_uint16_t value);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetSize_t(const LCHAR* name, size_t value);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetFloat(const LCHAR* name, float value);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetBool(const LCHAR* name, ESR_BOOL value);
/**
 * Sets session property value, storing a copy of the value.
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetLCHAR(const LCHAR* name, LCHAR* value);
/**
 * If the key does not exist in the session, calls SessionSetInt().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetIntIfEmpty(const LCHAR* name, int value);
/**
 * If the key does not exist in the session, calls SessionSetSize_t().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetUint16_tIfEmpty(const LCHAR* name, asr_uint16_t value);
/**
 * If the key does not exist in the session, calls SessionSetSize_t().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetSize_tIfEmpty(const LCHAR* name, size_t value);
/**
 * If the key does not exist in the session, calls SessionSetFloat().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetFloatIfEmpty(const LCHAR* name, float value);
/**
 * If the key does not exist in the session, calls SessionSetBool().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetBoolIfEmpty(const LCHAR* name, ESR_BOOL value);
/**
 * If the key does not exist in the session, calls SessionSetLCHAR().
 *
 * @param name Property name
 * @param value Property value
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionSetLCHARIfEmpty(const LCHAR* name, LCHAR* value);
/**
 * Removes property from session.
 *
 * @param name Property name
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionRemoveProperty(const LCHAR* name);
/**
 * Removes and deallocates property from session.
 *
 * @param name Property name
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionRemoveAndFreeProperty(const LCHAR* name);
/**
 * Destroys the system session.
 *
 * @return ESR_SUCCESS
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionDestroy(void);
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
 * @param argc Number of arguments
 * @param argv Argument values
 * @return ESR_OUT_OF_MEMORY if the system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionImportCommandLine(int argc, LCHAR* argv[]);

/**
 * Returns the number of elements in the session.
 *
 * @param size [out] Session size
 * @return ESR_SUCCESS
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetSize(size_t* size);

/**
 * Returns the key associated with the specified index.
 *
 * @param index Element index
 * @param key [out] Key name
 * @return ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetKeyAtIndex(size_t index, LCHAR** key);

/**
 * Convert the specified argument to int.
 *
 * @param key Property name
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_ARGUMENT if property cannot be converted to int
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToInt(const LCHAR* key);

/**
 * Convert the specified argument to int.
 *
 * @param key Property name
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_ARGUMENT if property cannot be converted to asr_uint16_t
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToUint16_t(const LCHAR* key);

/**
 * Convert the specified argument to size_t.
 *
 * @param key Property name
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_ARGUMENT if property cannot be converted to size_t
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToSize_t(const LCHAR* key);

/**
 * Convert the specified argument to float.
 *
 * @param key Property name
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_ARGUMENT if property cannot be converted to float
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToFloat(const LCHAR* key);

/**
 * Convert the specified argument to bool.
 *
 * @param key Property name
 * @return ESR_INVALID_RESULT_TYPE if the property is not a LCHAR*; ESR_OUT_OF_MEMORY if system is out of memory;
 * ESR_INVALID_ARGUMENT if property cannot be converted to bool
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionConvertToBool(const LCHAR* key);
/**
 * Returns the type of a property value.
 *
 * @param name Property name
 * @param type [out] Value type
 * @return ESR_INVALID_ARGUMENT if property cannot be found
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionGetPropertyType(const LCHAR* name, VariableTypes* type);
/**
 * Import PAR file into session.
 *
 * @param filename File to read session from
 * @return ESR_OPEN_ERROR if file cannot be opened; ESR_READ_ERROR if file cannot be read;
 * ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionImportParFile(const LCHAR* filename);
/**
 * Sets val to true if a session object exists (non-null), FALSE otherwise.
 *
 * @param val True if session is non-null, false otherwise
 * @return ESR_SUCCESS
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionExists(ESR_BOOL* val);
/**
 * Prefixes relative paths with the PAR-file base directory. If the path is not relative,
 * it is not changed.
 *
 * @param path Path to be prefixed
 * @param len Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if path is null;
 * ESR_NO_MATCH_ERROR if session property "parFile.baseDirectory" is undefined
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionPrefixWithBaseDirectory(LCHAR* path, size_t* len);
/**
 * Adds an event-listener.
 *
 * @param self ESR_SessionType handle
 * @param listener The event-listener to add
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionAddListener(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener);

/**
 * Removes an event-listener.
 *
 * @param self ESR_SessionType handle
 * @param listener The event-listener to remove
 * @return ESR_SUCCESS
 */
ESR_SHARED_API ESR_ReturnCode ESR_SessionRemoveListener(ESR_SessionType* self, ESR_SessionTypeListenerPair* listener);



/**
 * @}
 */



#endif /* __ESR_SESSION_H */
