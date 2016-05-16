/* Update section header.
   Copyright (C) 2000, 2001, 2002 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

#include "libelfP.h"


int
gelf_update_shdr (Elf_Scn *scn, GElf_Shdr *src)
{
  int result = 0;
  Elf *elf;

  if (scn == NULL || src == NULL)
    return 0;

  elf = scn->elf;
  rwlock_wrlock (elf->lock);

  if (elf->class == ELFCLASS32)
    {
      Elf32_Shdr *shdr
	= scn->shdr.e32 ?: __elf32_getshdr_wrlock (scn);

      if (shdr == NULL)
	{
	  __libelf_seterrno (ELF_E_INVALID_OPERAND);
	  goto out;
	}

      if (unlikely (src->sh_flags > 0xffffffffull)
	  || unlikely (src->sh_addr > 0xffffffffull)
	  || unlikely (src->sh_offset > 0xffffffffull)
	  || unlikely (src->sh_size > 0xffffffffull)
	  || unlikely (src->sh_addralign > 0xffffffffull)
	  || unlikely (src->sh_entsize > 0xffffffffull))
	{
	  __libelf_seterrno (ELF_E_INVALID_DATA);
	  goto out;
	}

#define COPY(name) \
      shdr->name = src->name
      COPY (sh_name);
      COPY (sh_type);
      COPY (sh_flags);
      COPY (sh_addr);
      COPY (sh_offset);
      COPY (sh_size);
      COPY (sh_link);
      COPY (sh_info);
      COPY (sh_addralign);
      COPY (sh_entsize);
    }
  else
    {
      Elf64_Shdr *shdr
	= scn->shdr.e64 ?: __elf64_getshdr_wrlock (scn);

      if (shdr == NULL)
	{
	  __libelf_seterrno (ELF_E_INVALID_OPERAND);
	  goto out;
	}

      /* We only have to copy the data.  */
      (void) memcpy (shdr, src, sizeof (GElf_Shdr));
    }

  result = 1;

 out:
  rwlock_unlock (elf->lock);

  return result;
}
