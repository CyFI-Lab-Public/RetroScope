/*---------------------------------------------------------------------------*
 *  ptypes.h  *
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

#ifndef __PTYPES_H
#define __PTYPES_H



#include <string.h>
#include <ctype.h>
#include "limits.h"
#include "PortPrefix.h"

#ifndef MAX
#define MAX(A,B) ((A)>(B)?(A):(B))
#endif
#ifndef MIN
#define MIN(A,B) ((A)<(B)?(A):(B))
#endif


/**
 * Boolean definition.
 */
typedef enum ESR_BOOL
{
  ESR_FALSE = 0,
  ESR_TRUE = 1
} ESR_BOOL;

/**
 * @addtogroup ESR_PortableModule ESR_Portable API functions
 *
 * @{
 */

#ifdef _WIN32

#pragma warning (disable: 4100 4127)
#pragma warning (error: 4133 4020)

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

/**
 * Portable 32-bit unsigned integer.
 */
typedef unsigned int   asr_uint32_t;

/**
 * Portable 32-bit signed integer.
 */
typedef int            asr_int32_t;

/**
 * Portable 16-bit unsigned integer.
 */
typedef unsigned short asr_uint16_t;

/**
 * Portable 16-bit signed integer.
 */
typedef short          asr_int16_t;

/**
 * Portable 8-bit unsigned integer.
 */
typedef unsigned char asr_uint8_t;

/**
 * Portable 8-bit signed integer.
 */
typedef signed char   asr_int8_t;

#else


/**
 * Portable 32-bit unsigned integer.
 */
typedef unsigned int   asr_uint32_t;

/**
 * Portable 32-bit signed integer.
 */
typedef int            asr_int32_t;

/**
 * Portable 16-bit unsigned integer.
 */
typedef unsigned short asr_uint16_t;

/**
 * Portable 16-bit signed integer.
 */
typedef short          asr_int16_t;

/**
 * Portable 8-bit unsigned integer.
 */
typedef unsigned char asr_uint8_t;

/**
 * Portable 8-bit signed integer.
 */
typedef signed char   asr_int8_t;

///**
// * Boolean definition.
// */
//#ifdef __vxworks
///* VxWorks defines BOOL as: typedef int BOOL in vxTypesOld.h */
//#include <vxWorks.h>
//#define FALSE 0
//#define TRUE  1
//#endif

#ifdef _solaris_
#include <sys/int_types.h>
#elif defined(_decunix_)
#include <inttypes.h>
#elif defined(POSIX)

#include <time.h>
#include <errno.h>

#if (CPU != SIMNT)
typedef void * HANDLE;
#endif /*  (CPU != SIMNT) */

#if defined(__vxworks)  /* VxWorks */
#include <sys/times.h>
#include <types.h>
/* VxWorks does not support recursive mutex in POSIX.4 */
#define OS_NO_RECURSIVE_MUTEX_SUPPORT
#elif defined(_QNX_) /* QNX */
#include <sys/time.h>
#include <inttypes.h>
#elif (OS == OS_UNIX)
#include <string.h>
#include <pthread.h>
#else
#error "New OS support here"
#endif

#ifndef _POSIX_SOURCE
#define _POSIX_SOURCE
#endif

/* Both POSIX.1 and POSIX.4 (POSIX1003.1c) are supported */
#ifndef _POSIX_C_SOURCE
#define _POSIX_C_SOURCE 199309
#endif

#include <unistd.h>
/*
#ifndef _POSIX_VERSION
#error "POSIX is not supported!")
#elif _POSIX_VERSION == 199009
#pragma message("POSIX.1 is supported only")
#elif _POSIX_VERSION == 199309
#pragma message("POSIX.1 and POSIX.4 are supported")
#elif _POSIX_VERSION > 199309
#pragma message("Version is newer than POSIX.4")
#endif // _POSIX_VERSION
*/
#else
/* Linux, maybe others too */
#endif
#endif

/**
 * Minimum value of UINT16_T.
 */
#define UINT16_TMIN 0

/**
 * Maximum value of UINT16_T.
 */
#define UINT16_TMAX 65535

/*
 * These should be platform-dependent.  But for the moment, we will assume
 * narrow character.
 */
#ifndef USE_NARROW_CHAR
#define USE_NARROW_CHAR
#endif

#ifdef USE_NARROW_CHAR
/**
 * Locale-independant character.
 */
typedef char LCHAR;

/**
 * Locale-independant integer-representation of a character. Used by fgetc() and others.
 */
typedef int LINT;

/**
 * LCHAR version of string-constant
 */
#define L(x) x

/**
 * LCHAR version of strcat()
 */
#define LSTRCAT strcat

/**
 * LCHAR version of strchr()
 */
#define LSTRCHR strchr

/**
 * LCHAR version of strrchr()
 */
#define LSTRRCHR strrchr

/**
 * LCHAR version of strcmp()
 */
#define LSTRCMP strcmp

/**
 * LCHAR version of strncmp()
 */
#define LSTRNCMP strncmp

/**
 * LCHAR version of strcpy()
 */
#define LSTRCPY strcpy

/**
 * LCHAR version of strftime()
 */
#define LSTRFTIME strftime

/**
 * LCHAR version of strlen()
 */
#define LSTRLEN strlen

/**
 * LCHAR version of strncpy()
 */
#define LSTRNCPY strncpy

/**
 * LCHAR version of memmove()
 */
#define LMEMMOVE memmove

/**
 * LCHAR version of strstr()
 */
#define LSTRSTR strstr

/**
 * LCHAR version of strlwr() which converts a string to lowercase.
 */
#define LSTRLWR lstrlwr

/**
 * LCHAR version of strupr() which converts a string to lowercase.
 */
#define LSTRUPR lstrupr

/**
 * LCHAR version of strtod()
 */
#define LSTRTOD strtod

/**
 * LCHAR version of strtol()
 */
#define LSTRTOL strtol

/**
 * LCHAR version of strtoul()
 */
#define LSTRTOUL strtoul

/**
 * LCHAR version of isspace()
 */
#define LISSPACE(c) isspace((unsigned char) c)

/**
 * LCHAR version of strcspn()
 */
#define LSTRCSPN strcspn

/**
 * LCHAR version of isalpha()
 */
#define LISALPHA isalpha

/**
 * LCHAR version of isalnum()
 */
#define LISALNUM isalnum

/**
 * LCHAR version of isdigit()
 */
#define LISDIGIT isdigit

/**
 * LCHAR version of strtok()
 */
#define LSTRTOK strtok

/**
 * LCHAR version of getenv()
 */
#define LGETENV getenv

/**
 * Converts LCHAR character to uppercase.
 */
#define LTOUPPER toupper

/**
 * Converts LCHAR character to lowercase.
 */
#define LTOLOWER tolower

/**
 * Portable printf().
 */
#define LPRINTF   printf
/**
 * Portable fprintf().
 */
#define LFPRINTF fprintf
/**
 * Portable sprintf().
 */
#define LSPRINTF sprintf

/**
 * Portable sprintf().
 */
#define psprintf sprintf

/**
 * Portable svprintf().
 */
#define pvsprintf vsprintf

#else

#include <wchar.h>
typedef wchar_t LCHAR;
/**
* Locale-independant integer-representation of a character. Used by fgetc() and others.
*/
typedef wint_t LINT;
#define L(x) L ## x
#define LSTRCAT wcscat
#define LSTRCHR wcschr
#define LSTRRCHR wcsrchr
#define LSTRCMP wcscmp
#define LSTRNCMP wcsncmp
#define LSTRCPY wcscpy
#define LSTRFTIME wcsftime

#define LPRINTF   wprintf
#define LFPRINTF fwprintf
#define LSPRINTF swprintf

#ifdef _WIN32

/**
* LCHAR version of getenv()
*/
#define LGETENV wgetenv

/**
* LCHAR version of strlwr() which converts a string to lowercase.
*/
#define LSTRLWR _wcslwr

/**
* LCHAR version of strtok()
*/
#define LSTRTOK wcstok

/**
* LCHAR version of strupr() which converts a string to lowercase.
*/
#define LSTRUPR _wcsupr
#else
#define LSTRCASECMP wcscasecmp
#define LSTRLWR #error LSTRLWR not defined.
#define LSTRUPR #error LSTRUPR not defined.
#endif /* _WIN32 */

#define LSTRLEN wcslen
#define LSTRNCPY wcsncpy
#define LMEMMOVE wmemmove
#define LSTRSTR wcsstr
#define LSTRTOD wcstod
#define LSTRTOL wcstol
#define LSTRTOUL wcstoul
#define LISSPACE iswspace
#define LSTRCSPN wcscspn
#define LISALPHA iswalpha
#define LISALNUM iswalnum
#define LISDIGIT iswdigit

/**
* Converts LCHAR character to uppercase.
*/
#define LTOUPPER towupper

/**
* Converts LCHAR character to lowercase.
*/
#define LTOLOWER towlower

/**
* Portable sprintf().
*/
#define psprintf sprintf

/**
* Portable svprintf().
*/
#define pvsprintf vsprintf

#endif /* USE_NARROW_CHAR */

/**
 * Log of 2 in base 10.
 */
#define LOG_10_2 (0.30102999566398)


/**
 * Maximum number of digits used to represent an unsigned int as a string in
 * base 10.  The +1 is for taking into account the fact that the fractional
 * part is removed and that we really need to take the ceiling.
 */
#define MAX_UINT_DIGITS ((size_t) ((CHAR_BIT * sizeof(int) * LOG_10_2) + 1))

/**
 * Maximum number of digits used to represent an int as a string in base 10.
 * +1 for sign character [+, -]
 */
#define MAX_INT_DIGITS ((size_t) (MAX_UINT_DIGITS + 1))

/**
 * Indicates if text contains a number (and nothing else).
 *
 * @param text String to check
 * @return ESR_TRUE if text is a number, ESR_FALSE otherwise.
 */
PORTABLE_API ESR_BOOL isNumber(const LCHAR* text);

/**
 * @}
 */


#include "ESR_ReturnCode.h"


#include "pstdio.h"

#endif 


