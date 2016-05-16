/* Unaligned memory access functionality.
   Copyright (C) 2000, 2001, 2002, 2003, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifndef _UNALIGNED_H
#define _UNALIGNED_H	1

#include <byteswap.h>
#include <endian.h>


#ifndef UNALIGNED_ACCESS_CLASS
# error "UNALIGNED_ACCESS_CLASS must be defined"
#endif


/* Macros to convert from the host byte order to that of the object file.  */
#if UNALIGNED_ACCESS_CLASS == BYTE_ORDER
# define target_bswap_16(n) (n)
# define target_bswap_32(n) (n)
# define target_bswap_64(n) (n)
#else
# define target_bswap_16(n) bswap_16 (n)
# define target_bswap_32(n) bswap_32 (n)
# define target_bswap_64(n) bswap_64 (n)
#endif


union u_2ubyte_unaligned
{
  uint16_t u;
  char c[2];
} __attribute__((packed));

union u_4ubyte_unaligned
{
  uint32_t u;
  char c[4];
} __attribute__((packed));

union u_8ubyte_unaligned
{
  uint64_t u;
  char c[8];
} __attribute__((packed));


/* Macros to store value at unaligned address.  */
#define store_2ubyte_unaligned(ptr, value) \
  (void) (((union u_2ubyte_unaligned *) (ptr))->u = target_bswap_16 (value))
#define store_4ubyte_unaligned(ptr, value) \
  (void) (((union u_4ubyte_unaligned *) (ptr))->u = target_bswap_32 (value))
#define store_8ubyte_unaligned(ptr, value) \
  (void) (((union u_8ubyte_unaligned *) (ptr))->u = target_bswap_64 (value))


/* Macros to add value to unaligned address.  This is a bit more
   complicated since the value must be read from memory and eventually
   converted twice.  */
#if UNALIGNED_ACCESS_CLASS == BYTE_ORDER
# define add_2ubyte_unaligned(ptr, value) \
  (void) (((union u_2ubyte_unaligned *) (ptr))->u += value)
# define add_4ubyte_unaligned(ptr, value) \
  (void) (((union u_4ubyte_unaligned *) (ptr))->u += value)
# define add_8ubyte_unaligned(ptr, value) \
  (void) (((union u_8ubyte_unaligned *) (ptr))->u += value)
#else
# define add_2ubyte_unaligned(ptr, value) \
  do {									      \
    union u_2ubyte_unaligned *_ptr = (void *) (ptr);			      \
    uint16_t _val = bswap_16 (_ptr->u) + (value);			      \
    _ptr->u = bswap_16 (_val);						      \
  } while (0)
# define add_4ubyte_unaligned(ptr, value) \
  do {									      \
    union u_4ubyte_unaligned *_ptr = (void *) (ptr);			      \
    uint32_t _val = bswap_32 (_ptr->u) + (value);			      \
    _ptr->u = bswap_32 (_val);						      \
  } while (0)
# define add_8ubyte_unaligned(ptr, value) \
  do {									      \
    union u_8ubyte_unaligned *_ptr = (void *) (ptr);			      \
    uint64_t _val = bswap_64 (_ptr->u) + (value);			      \
    _ptr->u = bswap_64 (_val);						      \
  } while (0)
#endif

#endif /* unaligned.h */
