/*---------------------------------------------------------------------------*
 *  LCHAR.h  *
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

#ifndef __LCHAR_H
#define __LCHAR_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup LCHARModule LCHAR API functions
 * LCHAR manipulation functions.
 *
 * @{
 */

/**
 * Trims string, removing any leading, trailing whitespace.
 *
 * @param text Text to trim
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode lstrtrim(LCHAR* text);

/**
 * Inserts text into a string.
 *
 * @param source String to insert
 * @param target String to insert into
 * @param offset Offset in target string
 * @param len [in/out] Length of target argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_BUFFER_OVERFLOW is target is too small to insert into
 */
PORTABLE_API ESR_ReturnCode lstrinsert(const LCHAR* source, LCHAR* target, size_t offset, size_t* len);

/**
 * Changes all instances of one character to another in a string.
 *
 * @param text String to process
 * @param source Source character
 * @param target Target character
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode lstrreplace(LCHAR* text, const LCHAR source, const LCHAR target);

/**
 * Converts string to integer.
 *
 * @param text String to parse
 * @param result [out] Resulting value
 * @param base Number base to use
 * @return ESR_INVALID_ARGUMENT is text is null or does not represent a number
 */
PORTABLE_API ESR_ReturnCode lstrtoi(const LCHAR* text, int* result, int base);

/**
 * Converts string to unsigned integer.
 *
 * @param text String to parse
 * @param result [out] Resulting value
 * @param base Number base to use
 * @return ESR_INVALID_ARGUMENT is text is null or does not represent a number
 */
PORTABLE_API ESR_ReturnCode lstrtoui(const LCHAR* text, unsigned int* result, int base);

/**
 * Converts string to float.
 *
 * @param text String to parse
 * @param result [out] Resulting value
 * @return ESR_INVALID_ARGUMENT is text is null or does not represent a number
 */
PORTABLE_API ESR_ReturnCode lstrtof(const LCHAR* text, float* result);

/**
 * Converts string to boolean.
 *
 * @param text String to parse
 * @param result [out] Resulting value
 * @return ESR_INVALID_ARGUMENT is text is null or does not represent a boolean value
 */
PORTABLE_API ESR_ReturnCode lstrtob(const LCHAR* text, ESR_BOOL* result);

/**
 * Returns the first token in the string in the form of an integer.
 *
 * @param text Text containing integers
 * @param value [out] Integer that was read
 * @param finalPosition [out] The first character after the token. A NULL value means this
 *                            argument is ignored.
 * @return ESR_INVALID_ARGUMENT is text is null or does not represent an integer value
 */
PORTABLE_API ESR_ReturnCode LCHARGetInt( LCHAR* text, int* value, LCHAR** finalPosition);

/**
 * Convert string to upper case
 *
 * @param string [in/out] string to be converted
 * @return ESR_INVALID_ARGUMENT is string is null
 */
PORTABLE_API ESR_ReturnCode lstrupr(LCHAR* string);

/**
 * Convert string to lower case
 *
 * @param string [in/out] string to be converted
 * @return ESR_INVALID_ARGUMENT is string is null
 */
PORTABLE_API ESR_ReturnCode lstrlwr(LCHAR* string);

/**
 * Binary safe case-insensitive string comparison
 *
 * @param string1 Text containing integers
 * @param string2 Integer that was read
 * @param result [out] returns
 *        < 0 if str1 is less than str2;
 *        > 0 if str1 is greater than str2, and
 *          0 if they are equal.
 * @return ESR_INVALID_ARGUMENT is string1 or string2 is null
 */
PORTABLE_API ESR_ReturnCode lstrcasecmp(const LCHAR *string1, const LCHAR *string2, int *result);

/**
 * Converts int to string
 *
 * @param value unsigned long to convert
 * @param string [out] String to store
 * @param len [in/out] in: length of the buffer; out: length of the converted string
 * @param radix Base of value; must be in the range 2 - 36
 * @return ESR_INVALID_ARGUMENT is string is null; ESR_BUFFER_OVERFLOW is string is not big enough to contain result
 */
PORTABLE_API ESR_ReturnCode litostr(int value, LCHAR *string, size_t *len, int radix);

/**
 * Converts unsigned long to string
 *
 * @param value unsigned long to convert
 * @param string [out] String to store
 * @param len [in/out] in: length of the buffer; out: length of the converted string
 * @param radix Base of value; must be in the range 2 - 36
 * @return ESR_INVALID_ARGUMENT is string is null; ESR_BUFFER_OVERFLOW is string is not big enough to contain result
 */
PORTABLE_API ESR_ReturnCode lultostr(unsigned long value, LCHAR *string, size_t *len, int radix);

/**
 * @}
 */

#endif /* __LCHAR_H */
