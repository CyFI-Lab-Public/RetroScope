/* Return section header.
   Copyright (C) 1998, 1999, 2000, 2001, 2002, 2005, 2007 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <errno.h>
#include <stdbool.h>
#include <unistd.h>

#include <system.h>
#include "libelfP.h"
#include "common.h"

#ifndef LIBELFBITS
# define LIBELFBITS 32
#endif


static ElfW2(LIBELFBITS,Shdr) *
load_shdr_wrlock (Elf_Scn *scn)
{
  ElfW2(LIBELFBITS,Shdr) *result;

  /* Read the section header table.  */
  Elf *elf = scn->elf;
  ElfW2(LIBELFBITS,Ehdr) *ehdr = elf->state.ELFW(elf,LIBELFBITS).ehdr;

  /* Try again, maybe the data is there now.  */
  result = scn->shdr.ELFW(e,LIBELFBITS);
  if (result != NULL)
    goto out;

  size_t shnum;
  if (__elf_getshnum_rdlock (elf, &shnum) != 0)
    goto out;
  size_t size = shnum * sizeof (ElfW2(LIBELFBITS,Shdr));

  /* Allocate memory for the section headers.  We know the number
     of entries from the ELF header.  */
  ElfW2(LIBELFBITS,Shdr) *shdr = elf->state.ELFW(elf,LIBELFBITS).shdr =
    (ElfW2(LIBELFBITS,Shdr) *) malloc (size);
  if (elf->state.ELFW(elf,LIBELFBITS).shdr == NULL)
    {
      __libelf_seterrno (ELF_E_NOMEM);
      goto out;
    }
  elf->state.ELFW(elf,LIBELFBITS).shdr_malloced = 1;

  if (elf->map_address != NULL)
    {
      ElfW2(LIBELFBITS,Shdr) *notcvt;

      /* All the data is already mapped.  If we could use it
	 directly this would already have happened.  */
      void *file_shdr = ((char *) elf->map_address
			 + elf->start_offset + ehdr->e_shoff);

      assert (ehdr->e_ident[EI_DATA] != MY_ELFDATA
	      || (! ALLOW_UNALIGNED
		  && ((uintptr_t) file_shdr
		      & (__alignof__ (ElfW2(LIBELFBITS,Shdr)) - 1)) != 0));

      /* Now copy the data and at the same time convert the byte order.  */
      if (ehdr->e_ident[EI_DATA] == MY_ELFDATA)
	{
	  assert (! ALLOW_UNALIGNED);
	  memcpy (shdr, file_shdr, size);
	}
      else
	{
	  if (ALLOW_UNALIGNED
	      || ((uintptr_t) file_shdr
		  & (__alignof__ (ElfW2(LIBELFBITS,Shdr)) - 1)) == 0)
	    notcvt = (ElfW2(LIBELFBITS,Shdr) *)
	      ((char *) elf->map_address
	       + elf->start_offset + ehdr->e_shoff);
	  else
	    {
	      notcvt = (ElfW2(LIBELFBITS,Shdr) *) alloca (size);
	      memcpy (notcvt, ((char *) elf->map_address
			       + elf->start_offset + ehdr->e_shoff),
		      size);
	    }

	  for (size_t cnt = 0; cnt < shnum; ++cnt)
	    {
	      CONVERT_TO (shdr[cnt].sh_name, notcvt[cnt].sh_name);
	      CONVERT_TO (shdr[cnt].sh_type, notcvt[cnt].sh_type);
	      CONVERT_TO (shdr[cnt].sh_flags, notcvt[cnt].sh_flags);
	      CONVERT_TO (shdr[cnt].sh_addr, notcvt[cnt].sh_addr);
	      CONVERT_TO (shdr[cnt].sh_offset, notcvt[cnt].sh_offset);
	      CONVERT_TO (shdr[cnt].sh_size, notcvt[cnt].sh_size);
	      CONVERT_TO (shdr[cnt].sh_link, notcvt[cnt].sh_link);
	      CONVERT_TO (shdr[cnt].sh_info, notcvt[cnt].sh_info);
	      CONVERT_TO (shdr[cnt].sh_addralign,
			  notcvt[cnt].sh_addralign);
	      CONVERT_TO (shdr[cnt].sh_entsize, notcvt[cnt].sh_entsize);

	      /* If this is a section with an extended index add a
		 reference in the section which uses the extended
		 index.  */
	      if (shdr[cnt].sh_type == SHT_SYMTAB_SHNDX
		  && shdr[cnt].sh_link < shnum)
		elf->state.ELFW(elf,LIBELFBITS).scns.data[shdr[cnt].sh_link].shndx_index
		  = cnt;

	      /* Set the own shndx_index field in case it has not yet
		 been set.  */
	      if (elf->state.ELFW(elf,LIBELFBITS).scns.data[cnt].shndx_index == 0)
		elf->state.ELFW(elf,LIBELFBITS).scns.data[cnt].shndx_index
		  = -1;
	    }
	}
    }
  else if (likely (elf->fildes != -1))
    {
      /* Read the header.  */
      ssize_t n = pread_retry (elf->fildes,
			       elf->state.ELFW(elf,LIBELFBITS).shdr, size,
			       elf->start_offset + ehdr->e_shoff);
      if (unlikely ((size_t) n != size))
	{
	  /* Severe problems.  We cannot read the data.  */
	  __libelf_seterrno (ELF_E_READ_ERROR);
	  goto free_and_out;
	}

      /* If the byte order of the file is not the same as the one
	 of the host convert the data now.  */
      if (ehdr->e_ident[EI_DATA] != MY_ELFDATA)
	for (size_t cnt = 0; cnt < shnum; ++cnt)
	  {
	    CONVERT (shdr[cnt].sh_name);
	    CONVERT (shdr[cnt].sh_type);
	    CONVERT (shdr[cnt].sh_flags);
	    CONVERT (shdr[cnt].sh_addr);
	    CONVERT (shdr[cnt].sh_offset);
	    CONVERT (shdr[cnt].sh_size);
	    CONVERT (shdr[cnt].sh_link);
	    CONVERT (shdr[cnt].sh_info);
	    CONVERT (shdr[cnt].sh_addralign);
	    CONVERT (shdr[cnt].sh_entsize);
	  }
    }
  else
    {
      /* The file descriptor was already enabled and not all data was
	 read.  Undo the allocation.  */
      __libelf_seterrno (ELF_E_FD_DISABLED);

    free_and_out:
      free (shdr);
      elf->state.ELFW(elf,LIBELFBITS).shdr = NULL;
      elf->state.ELFW(elf,LIBELFBITS).shdr_malloced = 0;

      goto out;
    }

  /* Set the pointers in the `scn's.  */
  for (size_t cnt = 0; cnt < shnum; ++cnt)
    elf->state.ELFW(elf,LIBELFBITS).scns.data[cnt].shdr.ELFW(e,LIBELFBITS)
      = &elf->state.ELFW(elf,LIBELFBITS).shdr[cnt];

  result = scn->shdr.ELFW(e,LIBELFBITS);
  assert (result != NULL);

out:
  return result;
}

static bool
scn_valid (Elf_Scn *scn)
{
  if (scn == NULL)
    return false;

  if (unlikely (scn->elf->state.elf.ehdr == NULL))
    {
      __libelf_seterrno (ELF_E_WRONG_ORDER_EHDR);
      return false;
    }

  if (unlikely (scn->elf->class != ELFW(ELFCLASS,LIBELFBITS)))
    {
      __libelf_seterrno (ELF_E_INVALID_CLASS);
      return false;
    }

  return true;
}

ElfW2(LIBELFBITS,Shdr) *
__elfw2(LIBELFBITS,getshdr_rdlock) (scn)
     Elf_Scn *scn;
{
  ElfW2(LIBELFBITS,Shdr) *result;

  if (!scn_valid (scn))
    return NULL;

  result = scn->shdr.ELFW(e,LIBELFBITS);
  if (result == NULL)
    {
      rwlock_unlock (scn->elf->lock);
      rwlock_wrlock (scn->elf->lock);
      result = scn->shdr.ELFW(e,LIBELFBITS);
      if (result == NULL)
	result = load_shdr_wrlock (scn);
    }

  return result;
}

ElfW2(LIBELFBITS,Shdr) *
__elfw2(LIBELFBITS,getshdr_wrlock) (scn)
     Elf_Scn *scn;
{
  ElfW2(LIBELFBITS,Shdr) *result;

  if (!scn_valid (scn))
    return NULL;

  result = scn->shdr.ELFW(e,LIBELFBITS);
  if (result == NULL)
    result = load_shdr_wrlock (scn);

  return result;
}

ElfW2(LIBELFBITS,Shdr) *
elfw2(LIBELFBITS,getshdr) (scn)
     Elf_Scn *scn;
{
  ElfW2(LIBELFBITS,Shdr) *result;

  if (!scn_valid (scn))
    return NULL;

  rwlock_rdlock (scn->elf->lock);
  result = __elfw2(LIBELFBITS,getshdr_rdlock) (scn);
  rwlock_unlock (scn->elf->lock);

  return result;
}
