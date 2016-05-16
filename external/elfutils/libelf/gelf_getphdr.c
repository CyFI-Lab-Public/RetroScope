/* Return program header table entry.
   Copyright (C) 1998, 1999, 2000, 2001, 2002 Red Hat, Inc.
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

#include <gelf.h>
#include <string.h>
#include <stdbool.h>

#include "libelfP.h"


GElf_Phdr *
gelf_getphdr (elf, ndx, dst)
     Elf *elf;
     int ndx;
     GElf_Phdr *dst;
{
  GElf_Phdr *result = NULL;

  if (elf == NULL)
    return NULL;

  if (unlikely (elf->kind != ELF_K_ELF))
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return NULL;
    }

  if (dst == NULL)
    {
      __libelf_seterrno (ELF_E_INVALID_OPERAND);
      return NULL;
    }

  rwlock_rdlock (elf->lock);

  if (elf->class == ELFCLASS32)
    {
      /* Copy the elements one-by-one.  */
      Elf32_Phdr *phdr = elf->state.elf32.phdr;

      if (phdr == NULL)
	{
	  rwlock_unlock (elf->lock);
	  phdr = INTUSE(elf32_getphdr) (elf);
	  if (phdr == NULL)
	    /* The error number is already set.  */
	    return NULL;
	  rwlock_rdlock (elf->lock);
	}

      /* Test whether the index is ok.  */
      if (ndx >= elf->state.elf32.ehdr->e_phnum)
	{
	  __libelf_seterrno (ELF_E_INVALID_INDEX);
	  goto out;
	}

      /* We know the result now.  */
      result = dst;

      /* Now correct the pointer to point to the correct element.  */
      phdr += ndx;

#define COPY(Name) result->Name = phdr->Name
      COPY (p_type);
      COPY (p_offset);
      COPY (p_vaddr);
      COPY (p_paddr);
      COPY (p_filesz);
      COPY (p_memsz);
      COPY (p_flags);
      COPY (p_align);
    }
  else
    {
      /* Copy the elements one-by-one.  */
      Elf64_Phdr *phdr = elf->state.elf64.phdr;

      if (phdr == NULL)
	{
	  rwlock_unlock (elf->lock);
	  phdr = INTUSE(elf64_getphdr) (elf);
	  if (phdr == NULL)
	    /* The error number is already set.  */
	    return NULL;
	  rwlock_rdlock (elf->lock);
	}

      /* Test whether the index is ok.  */
      if (ndx >= elf->state.elf64.ehdr->e_phnum)
	{
	  __libelf_seterrno (ELF_E_INVALID_INDEX);
	  goto out;
	}

      /* We only have to copy the data.  */
      result = memcpy (dst, phdr + ndx, sizeof (GElf_Phdr));
    }

 out:
  rwlock_unlock (elf->lock);

  return result;
}
