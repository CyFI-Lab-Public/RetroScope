/*---------------------------------------------------------------------------*
 *  pendian.h  *
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

#ifndef PENDIAN_H
#define PENDIAN_H



#include "PortPrefix.h"
#include "ptypes.h"

#ifdef __sgi
/*	*/#include <sys/endian.h>

#elif defined(__sparc)
/*	*/#include <sys/isa_defs.h>
/*	*/#ifdef __LITTLE_ENDIAN
/*	*//*	*/#define __LITTLE_ENDIAN 1234
/*	*//*	*/#define __BYTE_ORDER __LITTLE_ENDIAN
/*	*/#elif defined(_BIG_ENDIAN)
/*	*//*	*/#define __BIG_ENDIAN 4321
/*	*//*	*/#define __BYTE_ORDER __BIG_ENDIAN
/*	*/#endif

#elif defined(ANDROID)

/*  */#ifdef HAVE_ENDIAN
/*  */#include <endian.h>

/*  */#elif defined(HAVE_LITTLE_ENDIAN)
/*  *//*   */#define __LITTLE_ENDIAN 1234
/*  *//*   */#define __BYTE_ORDER __LITTLE_ENDIAN

/*  */#elif defined(HAVE_BIG_ENDIAN)
/*  *//*   */#define __BIG_ENDIAN 4321
/*  *//*   */#define __BYTE_ORDER __BIG_ENDIAN

/*  */#endif

#elif defined (__linux)
/*	    */#include <endian.h>

#elif defined(__FreeBSD__) || defined(_decunix_)
/*	*/#include <machine/endian.h>

#elif defined(__i386) || defined(_M_IX86)
/*	*/#undef  __LITTLE_ENDIAN
/*	*/#define __LITTLE_ENDIAN 1234
/*	*/#define __BYTE_ORDER __LITTLE_ENDIAN

#elif defined(_sh4_)||defined(SH4)
/*	*/#if defined (__vxworks)
/*	*//*	*/#if _BYTE_ORDER == _LITTLE_ENDIAN   /* VxWorks defines _BYTE_ORDER and _LITTLE_ENDIAN */
/*	*//*	*//*	*/#undef __LITTLE_ENDIAN
/*	*//*	*//*	*/#define __LITTLE_ENDIAN  1234
/*	*//*	*//*	*/#define __BYTE_ORDER  __LITTLE_ENDIAN
/*	*//*	*/#elif _BYTE_ORDER == _BIG_ENDIAN    /* VxWorks defines _BYTE_ORDER and _BIG_ENDIAN */
/*	*//*	*//*	*/#undef __BIG_ENDIAN
/*	*//*	*//*	*/#define __BIG_ENDIAN  4321
/*	*//*	*//*	*/#define __BYTE_ORDER  __BIG_ENDIAN
/*	*//*	*/#else
/*	*//*	*//*	*/#error
/*	*//*	*/#endif
/*	*/#else
/*	*//*	*/#error "Could not determine endianness of the machine Unknown OS for SH4 Chip."
/*	*/#endif

#else
/*	*/#error "Could not determine endianness of the machine Chip Not Known."
#endif


/**
 * @addtogroup ESR_PortableModule ESR_Portable API functions
 *
 * @{
 */

/**
 * Swaps bytes of each item in buffer.
 *
 * @param buffer Buffer containing items to swap.
 * @param count Number of items to swap.
 * @param itemSize Size of each items.
 */
PORTABLE_API void swap_byte_order(void *buffer,
                                  size_t count,
                                  size_t itemSize);
                                  
/**
 * @}
 */

#endif 
