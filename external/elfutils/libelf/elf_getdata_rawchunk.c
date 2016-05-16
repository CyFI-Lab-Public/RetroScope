/* Return converted data from raw chunk of ELF file.
   Copyright (C) 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.

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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <system.h>
#include "libelfP.h"
#include "common.h"

Elf_Data *
elf_getdata_rawchunk (elf, offset, size, type)
     Elf *elf;
     /* ANDROID_CHANGE_BEGIN */
#if 0
     off64_t offset;
#else
     off_t offset;
#endif
     /* ANDROID_CHANGE_END */
     size_t size;
     Elf_Type type;
{
  if (unlikely (elf == NULL))
    return NULL;

  if (unlikely (elf->kind != ELF_K_ELF))
    {
      /* No valid descriptor.  */
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return NULL;
    }

   /* ANDROID_CHANGE_BEGIN */
#if 0
  if (unlikely (size > elf->maximum_size
		|| (off64_t) (elf->maximum_size - size) < offset))
#else
  if (unlikely (size > elf->maximum_size
		|| (off_t) (elf->maximum_size - size) < offset))
#endif
   /* ANDROID_CHANGE_END */
    {
      /* Invalid request.  */
      __libelf_seterrno (ELF_E_INVALID_OP);
      return NULL;
    }

  if (type >= ELF_T_NUM)
    {
      __libelf_seterrno (ELF_E_UNKNOWN_TYPE);
      return NULL;
    }

  /* Get the raw bytes from the file.  */
  void *rawchunk;
  int flags = 0;
  Elf_Data *result = NULL;

  rwlock_rdlock (elf->lock);

  /* If the file is mmap'ed we can use it directly.  */
  if (elf->map_address != NULL)
    rawchunk = elf->map_address + elf->start_offset + offset;
  else
    {
      /* We allocate the memory and read the data from the file.  */
      rawchunk = malloc (size);
      if (rawchunk == NULL)
	{
	nomem:
	  __libelf_seterrno (ELF_E_NOMEM);
	  goto out;
	}

      /* Read the file content.  */
      if (unlikely ((size_t) pread_retry (elf->fildes, rawchunk, size,
					  elf->start_offset + offset)
		    != size))
	{
	  /* Something went wrong.  */
	  free (rawchunk);
	  __libelf_seterrno (ELF_E_READ_ERROR);
	  goto out;
	}

      flags = ELF_F_MALLOCED;
    }

  /* Copy and/or convert the data as needed for aligned native-order access.  */
  size_t align = __libelf_type_align (elf->class, type);
  void *buffer;
  if (elf->state.elf32.ehdr->e_ident[EI_DATA] == MY_ELFDATA)
    {
      if (((uintptr_t) rawchunk & (align - 1)) == 0)
	/* No need to copy, we can use the raw data.  */
	buffer = rawchunk;
      else
	{
	  /* A malloc'd block is always sufficiently aligned.  */
	  assert (flags == 0);

	  buffer = malloc (size);
	  if (unlikely (buffer == NULL))
	    goto nomem;
	  flags = ELF_F_MALLOCED;

	  /* The copy will be appropriately aligned for direct access.  */
	  memcpy (buffer, rawchunk, size);
	}
    }
  else
    {
      if (flags)
	buffer = rawchunk;
      else
	{
	  buffer = malloc (size);
	  if (unlikely (buffer == NULL))
	    goto nomem;
	  flags = ELF_F_MALLOCED;
	}

      /* Call the conversion function.  */
      (*__elf_xfctstom[LIBELF_EV_IDX][LIBELF_EV_IDX][elf->class - 1][type])
	(buffer, rawchunk, size, 0);
    }

  /* Allocate the dummy container to point at this buffer.  */
  Elf_Data_Chunk *chunk = calloc (1, sizeof *chunk);
  if (chunk == NULL)
    {
      if (flags)
	free (buffer);
      goto nomem;
    }

  chunk->dummy_scn.elf = elf;
  chunk->dummy_scn.flags = flags;
  chunk->data.s = &chunk->dummy_scn;
  chunk->data.d.d_buf = buffer;
  chunk->data.d.d_size = size;
  chunk->data.d.d_type = type;
  chunk->data.d.d_align = align;
  chunk->data.d.d_version = __libelf_version;

  rwlock_unlock (elf->lock);
  rwlock_wrlock (elf->lock);

  chunk->next = elf->state.elf.rawchunks;
  elf->state.elf.rawchunks = chunk;
  result = &chunk->data.d;

 out:
  rwlock_unlock (elf->lock);
  return result;
}
