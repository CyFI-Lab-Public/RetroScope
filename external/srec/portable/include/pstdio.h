/*---------------------------------------------------------------------------*
 *  pstdio.h  *
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

#ifndef PSTDIO_H
#define PSTDIO_H



#include <stdio.h>
#include "PortPrefix.h"
#include "ptypes.h"
#include "PFile.h"
#include "ESR_ReturnCode.h"

/**
 * File table structure for memory FS
 */
typedef struct FileRecord_t
{
  /**
   * file name
      */
  char name[80];
  /**
   * pointer to the file data
   */
  unsigned char *start;
  /**
      * real size of the file
      */
  int size;
  /**
      * total size in memory
      */
  int memsize;
  /**
   * mode: 0/1: text/binary
   */
  int mode;
}
FileRecord;

#ifdef _WIN32

#include "direct.h"
#include "stdlib.h"

/**
 * @addtogroup ESR_PortableModule ESR_Portable API functions
 *
 * @{
 */

/**
 * Platform-independant maximum filename path length.
 */
#define P_PATH_MAX _MAX_PATH

/**
 * Platform-independant maximum command-line length. In reality this value is shell-specific
 * and is around 32k for WindowsNT however we can't spare that much stack-space and we assume
 * such a large value will never actually occur so we settle for 4k instead.
 */
#define P_CMDLINE_MAX 4000
/**
 * @}
 */

#else

#if defined(PATH_MAX)
#define P_PATH_MAX PATH_MAX
#elif defined(MAXPATHLEN)
#define P_PATH_MAX MAXPATHLEN
#else
#error "Cannot determine value for P_PATH_MAX."
#endif /* PATH_MAX */

#endif /* _WIN32 */

#endif 
