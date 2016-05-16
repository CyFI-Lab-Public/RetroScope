/* Create new ELF program header table.
   Copyright (C) 1999, 2000, 2002 Red Hat, Inc.
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
#include <stdlib.h>
#include <string.h>

#include "libelfP.h"

#ifndef LIBELFBITS
# define LIBELFBITS 32
#endif


ElfW2(LIBELFBITS,Phdr) *
elfw2(LIBELFBITS,newphdr) (elf, count)
     Elf *elf;
     size_t count;
{
  ElfW2(LIBELFBITS,Phdr) *result;

  if (elf == NULL)
    return NULL;

  if (unlikely (elf->kind != ELF_K_ELF))
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return NULL;
    }

  rwlock_wrlock (elf->lock);

  if (elf->class == 0)
    elf->class = ELFW(ELFCLASS,LIBELFBITS);
  else if (unlikely (elf->class != ELFW(ELFCLASS,LIBELFBITS)))
    {
      __libelf_seterrno (ELF_E_INVALID_CLASS);
      result = NULL;
      goto out;
    }

  if (unlikely (elf->state.ELFW(elf,LIBELFBITS).ehdr == NULL))
    {
      __libelf_seterrno (ELF_E_WRONG_ORDER_EHDR);
      result = NULL;
      goto out;
    }

  /* A COUNT of zero means remove existing table.  */
  if (count == 0)
    {
      /* Free the old program header.  */
      if (elf->state.ELFW(elf,LIBELFBITS).phdr != NULL)
	{
	  if (elf->state.ELFW(elf,LIBELFBITS).phdr_flags & ELF_F_MALLOCED)
	    free (elf->state.ELFW(elf,LIBELFBITS).phdr);

	  /* Set the pointer to NULL.  */
	  elf->state.ELFW(elf,LIBELFBITS).phdr = NULL;
	  /* Set the `e_phnum' member to the new value.  */
	  elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phnum = 0;
	  /* Also set the size.  */
	  elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phentsize =
	    sizeof (ElfW2(LIBELFBITS,Phdr));

	  elf->state.ELFW(elf,LIBELFBITS).phdr_flags |= ELF_F_DIRTY;
	  elf->flags |= ELF_F_DIRTY;
	  __libelf_seterrno (ELF_E_NOERROR);
	}

      result = NULL;
    }
  else if (elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phnum != count
	   || elf->state.ELFW(elf,LIBELFBITS).phdr == NULL)
    {
      /* Allocate a new program header with the appropriate number of
	 elements.  */
      result = (ElfW2(LIBELFBITS,Phdr) *)
	realloc (elf->state.ELFW(elf,LIBELFBITS).phdr,
		 count * sizeof (ElfW2(LIBELFBITS,Phdr)));
      if (result == NULL)
	__libelf_seterrno (ELF_E_NOMEM);
      else
	{
	  /* Now set the result.  */
	  elf->state.ELFW(elf,LIBELFBITS).phdr = result;
	  /* Clear the whole memory.  */
	  memset (result, '\0', count * sizeof (ElfW2(LIBELFBITS,Phdr)));
	  /* Set the `e_phnum' member to the new value.  */
	  elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phnum = count;
	  /* Also set the size.  */
	  elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phentsize =
	    elf_typesize (LIBELFBITS, ELF_T_PHDR, 1);
	  /* Remember we allocated the array and mark the structure is
	     modified.  */
	  elf->state.ELFW(elf,LIBELFBITS).phdr_flags |=
	    ELF_F_DIRTY | ELF_F_MALLOCED;
	  /* We have to rewrite the entire file if the size of the
	     program header is changed.  */
	  elf->flags |= ELF_F_DIRTY;
	}
    }
  else
    {
      /* We have the same number of entries.  Just clear the array.  */
      assert (elf->state.ELFW(elf,LIBELFBITS).ehdr->e_phentsize
	      == elf_typesize (LIBELFBITS, ELF_T_PHDR, 1));

      /* Mark the structure as modified.  */
      elf->state.ELFW(elf,LIBELFBITS).phdr_flags |= ELF_F_DIRTY;

      result = elf->state.ELFW(elf,LIBELFBITS).phdr;
    }

 out:
  rwlock_unlock (elf->lock);

  return result;
}
INTDEF(elfw2(LIBELFBITS,newphdr))
