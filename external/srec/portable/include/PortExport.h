/*---------------------------------------------------------------------------*
 *  PortExport.h  *
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

#ifndef __PORT_EXPORT_H
#define __PORT_EXPORT_H



/* (1) Platform specific macro which handles symbol exports & imports.*/

/* These macros are used if defining DLL import/export in the source file
 * rather than through a .def file. */

/**
 * @addtogroup ESR_PortableModule ESR_Portable API functions
 *
 * @{
 */

#ifndef DOXYGEN_SHOULD_SKIP_THIS

#ifdef _WIN32

#ifndef HAS_INLINE
#define HAS_INLINE
#endif

#ifdef __cplusplus

#define PORT_EXPORT_DECL extern "C" __declspec(dllexport)
#define PORT_IMPORT_DECL extern "C" __declspec(dllimport)

#else /* not __cplusplus */

#define PORT_EXPORT_DECL __declspec(dllexport)
#define PORT_IMPORT_DECL __declspec(dllimport)
#endif /* __cplusplus */

#else /* not _WIN32 */

#ifdef __cplusplus
#define PORT_EXPORT_DECL extern "C"
#define PORT_IMPORT_DECL extern "C"
#else
#define PORT_EXPORT_DECL extern
#define PORT_IMPORT_DECL extern
#endif /* __cplusplus */

#endif /* _WIN32 */

#if !defined(PORT_EXPORT_DECL) || !defined(PORT_IMPORT_DECL)
#error Symbol import/export pair not defined.
#endif

#endif /* DOXYGEN_SHOULD_SKIP_THIS */

/* If using a .def file on win32, use these macros. */
#ifdef __cplusplus

/**
 * Exports C-style symbols; avoids name-mangling.
 */
#define EXTERN extern "C"
#else

/**
* Exports C-style symbols; avoids name-mangling.
*/
#define EXTERN extern
#endif

#ifdef __cplusplus

/**
 * Portable 'inline' keyword
 */
#define PINLINE inline
#elif defined(_WIN32)

/**
* Portable 'inline' keyword
*/
#define PINLINE _inline
#elif defined(__GNUC__)

/**
* Portable 'inline' keyword
*/
#ifdef __vxworks
#define PINLINE __inline__
#else
#define PINLINE	__inline__ 
#endif

#elif !defined(PINLINE)

/**
* Portable 'inline' keyword
*/
#define PINLINE
#endif

/**
 * inlining causes problems for the Xcode 4.3 and 4.4 command line tools,
 * so this is needed to ensure the methods aren't inlined on those compilers
 */

#if defined(__APPLE_CC__)
#if __APPLE_CC__ >= 5621
#undef PINLINE
#define PINLINE
#endif
#endif

/**
 * @}
 */

#endif 
