/* Common definitions for handling files in memory or only on disk.
   Copyright (C) 1998, 1999, 2000, 2002, 2005, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 1998.

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

#ifndef _COMMON_H
#define _COMMON_H       1

#include <ar.h>
#include <byteswap.h>
#include <endian.h>
#include <stdlib.h>
#include <string.h>

#include "libelfP.h"

static inline Elf_Kind
__attribute__ ((unused))
determine_kind (void *buf, size_t len)
{
  /* First test for an archive.  */
  if (len >= SARMAG && memcmp (buf, ARMAG, SARMAG) == 0)
    return ELF_K_AR;

  /* Next try ELF files.  */
  if (len >= EI_NIDENT && memcmp (buf, ELFMAG, SELFMAG) == 0)
    {
      /* Could be an ELF file.  */
      int eclass = (int) ((unsigned char *) buf)[EI_CLASS];
      int data = (int) ((unsigned char *) buf)[EI_DATA];
      int version = (int) ((unsigned char *) buf)[EI_VERSION];

      if (eclass > ELFCLASSNONE && eclass < ELFCLASSNUM
	  && data > ELFDATANONE && data < ELFDATANUM
	  && version > EV_NONE && version < EV_NUM)
	return ELF_K_ELF;
    }

  /* We do not know this file type.  */
  return ELF_K_NONE;
}


/* Allocate an Elf descriptor and fill in the generic information.  */
static inline Elf *
__attribute__ ((unused))
allocate_elf (int fildes, void *map_address, off_t offset, size_t maxsize,
              Elf_Cmd cmd, Elf *parent, Elf_Kind kind, size_t extra)
{
  Elf *result = (Elf *) calloc (1, sizeof (Elf) + extra);
  if (result == NULL)
    __libelf_seterrno (ELF_E_NOMEM);
  else
    {
      result->kind = kind;
      result->ref_count = 1;
      result->cmd = cmd;
      result->fildes = fildes;
      result->start_offset = offset;
      result->maximum_size = maxsize;
      result->map_address = map_address;
      result->parent = parent;

      rwlock_init (result->lock);
    }

  return result;
}


/* Acquire lock for the descriptor and all children.  */
static void
__attribute__ ((unused))
libelf_acquire_all (Elf *elf)
{
  rwlock_wrlock (elf->lock);

  if (elf->kind == ELF_K_AR)
    {
      Elf *child = elf->state.ar.children;

      while (child != NULL)
	{
	  if (child->ref_count != 0)
	    libelf_acquire_all (child);
	  child = child->next;
	}
    }
}

/* Release own lock and those of the children.  */
static void
__attribute__ ((unused))
libelf_release_all (Elf *elf)
{
  if (elf->kind == ELF_K_AR)
    {
      Elf *child = elf->state.ar.children;

      while (child != NULL)
	{
	  if (child->ref_count != 0)
	    libelf_release_all (child);
	  child = child->next;
	}
    }

  rwlock_unlock (elf->lock);
}


/* Macro to convert endianess in place.  It determines the function it
   has to use itself.  */
#define CONVERT(Var) \
  (Var) = (sizeof (Var) == 1						      \
	   ? (unsigned char) (Var)					      \
	   : (sizeof (Var) == 2						      \
	      ? bswap_16 (Var)						      \
	      : (sizeof (Var) == 4					      \
		 ? bswap_32 (Var)					      \
		 : bswap_64 (Var))))

#define CONVERT_TO(Dst, Var) \
  (Dst) = (sizeof (Var) == 1						      \
	   ? (unsigned char) (Var)					      \
	   : (sizeof (Var) == 2						      \
	      ? bswap_16 (Var)						      \
	      : (sizeof (Var) == 4					      \
		 ? bswap_32 (Var)					      \
		 : bswap_64 (Var))))


#if __BYTE_ORDER == __LITTLE_ENDIAN
# define MY_ELFDATA	ELFDATA2LSB
#else
# define MY_ELFDATA	ELFDATA2MSB
#endif

#endif	/* common.h */
