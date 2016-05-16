/* Unaligned memory access functionality.
   Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Red Hat, Inc.
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

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifndef _MEMORY_ACCESS_H
#define _MEMORY_ACCESS_H 1

#include <byteswap.h>
#include <limits.h>
#include <stdint.h>


/* Number decoding macros.  See 7.6 Variable Length Data.  */

#define get_uleb128_step(var, addr, nth, break)				      \
    __b = *(addr)++;							      \
    var |= (uintmax_t) (__b & 0x7f) << (nth * 7);			      \
    if (likely ((__b & 0x80) == 0))					      \
      break

#define get_uleb128(var, addr)						      \
  do {									      \
    unsigned char __b;							      \
    var = 0;								      \
    get_uleb128_step (var, addr, 0, break);				      \
    var = __libdw_get_uleb128 (var, 1, &(addr));			      \
  } while (0)

#define get_uleb128_rest_return(var, i, addrp)				      \
  do {									      \
    for (; i < 10; ++i)							      \
      {									      \
	get_uleb128_step (var, *addrp, i, return var);			      \
      }									      \
    /* Other implementations set VALUE to UINT_MAX in this		      \
       case.  So we better do this as well.  */				      \
    return UINT64_MAX;							      \
  } while (0)

/* The signed case is similar, but we sign-extend the result.  */

#define get_sleb128_step(var, addr, nth, break)				      \
    __b = *(addr)++;							      \
    _v |= (uint64_t) (__b & 0x7f) << (nth * 7);				      \
    if (likely ((__b & 0x80) == 0))					      \
      {									      \
	var = (_v << (64 - (nth * 7) - 7) >> (64 - (nth * 7) - 7));	      \
        break;					 			      \
      }									      \
    else do {} while (0)

#define get_sleb128(var, addr)						      \
  do {									      \
    unsigned char __b;							      \
    int64_t _v = 0;							      \
    get_sleb128_step (var, addr, 0, break);				      \
    var = __libdw_get_sleb128 (_v, 1, &(addr));				      \
  } while (0)

#define get_sleb128_rest_return(var, i, addrp)				      \
  do {									      \
    for (; i < 9; ++i)							      \
      {									      \
	get_sleb128_step (var, *addrp, i, return var);			      \
      }									      \
    /* Other implementations set VALUE to INT_MAX in this		      \
       case.  So we better do this as well.  */				      \
    return INT64_MAX;							      \
  } while (0)

#ifdef IS_LIBDW
extern uint64_t __libdw_get_uleb128 (uint64_t acc, unsigned int i,
				     const unsigned char **addrp)
     internal_function attribute_hidden;
extern int64_t __libdw_get_sleb128 (int64_t acc, unsigned int i,
				    const unsigned char **addrp)
     internal_function attribute_hidden;
#else
static uint64_t
__attribute__ ((unused))
__libdw_get_uleb128 (uint64_t acc, unsigned int i, const unsigned char **addrp)
{
  unsigned char __b;
  get_uleb128_rest_return (acc, i, addrp);
}
static int64_t
__attribute__ ((unused))
__libdw_get_sleb128 (int64_t acc, unsigned int i, const unsigned char **addrp)
{
  unsigned char __b;
  int64_t _v = acc;
  get_sleb128_rest_return (acc, i, addrp);
}
#endif


/* We use simple memory access functions in case the hardware allows it.
   The caller has to make sure we don't have alias problems.  */
#if ALLOW_UNALIGNED

# define read_2ubyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? bswap_16 (*((const uint16_t *) (Addr)))				      \
   : *((const uint16_t *) (Addr)))
# define read_2sbyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? (int16_t) bswap_16 (*((const int16_t *) (Addr)))			      \
   : *((const int16_t *) (Addr)))

# define read_4ubyte_unaligned_noncvt(Addr) \
   *((const uint32_t *) (Addr))
# define read_4ubyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? bswap_32 (*((const uint32_t *) (Addr)))				      \
   : *((const uint32_t *) (Addr)))
# define read_4sbyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? (int32_t) bswap_32 (*((const int32_t *) (Addr)))			      \
   : *((const int32_t *) (Addr)))

# define read_8ubyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? bswap_64 (*((const uint64_t *) (Addr)))				      \
   : *((const uint64_t *) (Addr)))
# define read_8sbyte_unaligned(Dbg, Addr) \
  (unlikely ((Dbg)->other_byte_order)					      \
   ? (int64_t) bswap_64 (*((const int64_t *) (Addr)))			      \
   : *((const int64_t *) (Addr)))

#else

union unaligned
  {
    void *p;
    uint16_t u2;
    uint32_t u4;
    uint64_t u8;
    int16_t s2;
    int32_t s4;
    int64_t s8;
  } __attribute__ ((packed));

static inline uint16_t
read_2ubyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return bswap_16 (up->u2);
  return up->u2;
}
static inline int16_t
read_2sbyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return (int16_t) bswap_16 (up->u2);
  return up->s2;
}

static inline uint32_t
read_4ubyte_unaligned_noncvt (const void *p)
{
  const union unaligned *up = p;
  return up->u4;
}
static inline uint32_t
read_4ubyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return bswap_32 (up->u4);
  return up->u4;
}
static inline int32_t
read_4sbyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return (int32_t) bswap_32 (up->u4);
  return up->s4;
}

static inline uint64_t
read_8ubyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return bswap_64 (up->u8);
  return up->u8;
}
static inline int64_t
read_8sbyte_unaligned (Dwarf *dbg, const void *p)
{
  const union unaligned *up = p;
  if (dbg->other_byte_order)
    return (int64_t) bswap_64 (up->u8);
  return up->s8;
}

#endif	/* allow unaligned */


#define read_2ubyte_unaligned_inc(Dbg, Addr) \
  ({ uint16_t t_ = read_2ubyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 2);		      \
     t_; })
#define read_2sbyte_unaligned_inc(Dbg, Addr) \
  ({ int16_t t_ = read_2sbyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 2);		      \
     t_; })

#define read_4ubyte_unaligned_inc(Dbg, Addr) \
  ({ uint32_t t_ = read_4ubyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 4);		      \
     t_; })
#define read_4sbyte_unaligned_inc(Dbg, Addr) \
  ({ int32_t t_ = read_4sbyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 4);		      \
     t_; })

#define read_8ubyte_unaligned_inc(Dbg, Addr) \
  ({ uint64_t t_ = read_8ubyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 8);		      \
     t_; })
#define read_8sbyte_unaligned_inc(Dbg, Addr) \
  ({ int64_t t_ = read_8sbyte_unaligned (Dbg, Addr);			      \
     Addr = (__typeof (Addr)) (((uintptr_t) (Addr)) + 8);		      \
     t_; })

#endif	/* memory-access.h */
