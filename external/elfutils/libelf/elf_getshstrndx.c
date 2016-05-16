/* Return section index of section header string table.
   Copyright (C) 2002, 2005 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2002.

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
#include <gelf.h>
#include <stddef.h>
#include <unistd.h>

#include <system.h>
#include "libelfP.h"
#include "common.h"


int
elf_getshstrndx (elf, dst)
     Elf *elf;
     size_t *dst;
{
  int result = 0;

  if (elf == NULL)
    return -1;

  if (unlikely (elf->kind != ELF_K_ELF))
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return -1;
    }

  rwlock_rdlock (elf->lock);

  /* We rely here on the fact that the `elf' element is a common prefix
     of `elf32' and `elf64'.  */
  assert (offsetof (struct Elf, state.elf.ehdr)
	  == offsetof (struct Elf, state.elf32.ehdr));
  assert (sizeof (elf->state.elf.ehdr)
	  == sizeof (elf->state.elf32.ehdr));
  assert (offsetof (struct Elf, state.elf.ehdr)
	  == offsetof (struct Elf, state.elf64.ehdr));
  assert (sizeof (elf->state.elf.ehdr)
	  == sizeof (elf->state.elf64.ehdr));

  if (unlikely (elf->state.elf.ehdr == NULL))
    {
      __libelf_seterrno (ELF_E_WRONG_ORDER_EHDR);
      result = -1;
    }
  else
    {
      Elf32_Word num;

      num = (elf->class == ELFCLASS32
	     ? elf->state.elf32.ehdr->e_shstrndx
	     : elf->state.elf64.ehdr->e_shstrndx);

      /* Determine whether the index is too big to fit in the ELF
	 header.  */
      if (unlikely (num == SHN_XINDEX))
	{
	  /* Yes.  Search the zeroth section header.  */
	  if (elf->class == ELFCLASS32)
	    {
	      size_t offset;

	      if (elf->state.elf32.scns.data[0].shdr.e32 != NULL)
		{
		  num = elf->state.elf32.scns.data[0].shdr.e32->sh_link;
		  goto success;
		}

	      offset = elf->state.elf32.ehdr->e_shoff;

	      if (elf->map_address != NULL
		  && elf->state.elf32.ehdr->e_ident[EI_DATA] == MY_ELFDATA
		  && (ALLOW_UNALIGNED
		      || (((size_t) ((char *) elf->map_address + offset))
			  & (__alignof__ (Elf32_Shdr) - 1)) == 0))
		/* We can directly access the memory.  */
		num = ((Elf32_Shdr *) (elf->map_address + offset))->sh_link;
	      else
		{
		  /* We avoid reading in all the section headers.  Just read
		     the first one.  */
		  Elf32_Shdr shdr_mem;

		  if (unlikely (pread_retry (elf->fildes, &shdr_mem,
					     sizeof (Elf32_Shdr), offset)
				!= sizeof (Elf32_Shdr)))
		    {
		      /* We must be able to read this ELF section header.  */
		      __libelf_seterrno (ELF_E_INVALID_FILE);
		      result = -1;
		      goto out;
		    }

		  if (elf->state.elf32.ehdr->e_ident[EI_DATA] != MY_ELFDATA)
		    CONVERT (shdr_mem.sh_link);
		  num = shdr_mem.sh_link;
		}
	    }
	  else
	    {
	      if (elf->state.elf64.scns.data[0].shdr.e64 != NULL)
		{
		  num = elf->state.elf64.scns.data[0].shdr.e64->sh_link;
		  goto success;
		}

	      size_t offset = elf->state.elf64.ehdr->e_shoff;

	      if (elf->map_address != NULL
		  && elf->state.elf64.ehdr->e_ident[EI_DATA] == MY_ELFDATA
		  && (ALLOW_UNALIGNED
		      || (((size_t) ((char *) elf->map_address + offset))
			  & (__alignof__ (Elf64_Shdr) - 1)) == 0))
		/* We can directly access the memory.  */
		num = ((Elf64_Shdr *) (elf->map_address + offset))->sh_link;
	      else
		{
		  /* We avoid reading in all the section headers.  Just read
		     the first one.  */
		  Elf64_Shdr shdr_mem;

		  if (unlikely (pread_retry (elf->fildes, &shdr_mem,
					     sizeof (Elf64_Shdr), offset)
				!= sizeof (Elf64_Shdr)))
		    {
		      /* We must be able to read this ELF section header.  */
		      __libelf_seterrno (ELF_E_INVALID_FILE);
		      result = -1;
		      goto out;
		    }

		  if (elf->state.elf64.ehdr->e_ident[EI_DATA] != MY_ELFDATA)
		    CONVERT (shdr_mem.sh_link);
		  num = shdr_mem.sh_link;
		}
	    }
	}

      /* Store the result.  */
    success:
      *dst = num;
    }

 out:
  rwlock_unlock (elf->lock);

  return result;
}
INTDEF(elf_getshstrndx)
