/* Append new section.
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

#include <assert.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#include "libelfP.h"


Elf_Scn *
elf_newscn (elf)
     Elf *elf;
{
  Elf_Scn *result = NULL;
  bool first = false;

  if (elf == NULL)
    return NULL;

  /* We rely on the prefix of the `elf', `elf32', and `elf64' element
     being the same.  */
  assert (offsetof (Elf, state.elf.scns_last)
	  == offsetof (Elf, state.elf32.scns_last));
  assert (offsetof (Elf, state.elf.scns_last)
	  == offsetof (Elf, state.elf64.scns_last));
  assert (offsetof (Elf, state.elf32.scns)
	  == offsetof (Elf, state.elf64.scns));

  rwlock_wrlock (elf->lock);

 again:
  if (elf->state.elf.scns_last->cnt < elf->state.elf.scns_last->max)
    {
      result = &elf->state.elf.scns_last->data[elf->state.elf.scns_last->cnt];

      if (++elf->state.elf.scns_last->cnt == 1
	  && (elf->state.elf.scns_last
	      == (elf->class == ELFCLASS32
		  || (offsetof (Elf, state.elf32.scns)
		      == offsetof (Elf, state.elf64.scns))
		  ? &elf->state.elf32.scns : &elf->state.elf64.scns)))
	/* This is zeroth section.  */
	first = true;
      else
	{
	  assert (elf->state.elf.scns_last->cnt > 1);
	  result->index = result[-1].index + 1;
	}
    }
  else
    {
      /* We must allocate a new element.  */
      Elf_ScnList *newp;

      assert (elf->state.elf.scnincr > 0);

      newp = (Elf_ScnList *) calloc (sizeof (Elf_ScnList)
				     + ((elf->state.elf.scnincr *= 2)
					* sizeof (Elf_Scn)), 1);
      if (newp == NULL)
	{
	  __libelf_seterrno (ELF_E_NOMEM);
	  goto out;
	}

      result = &newp->data[0];

      /* One section used.  */
      ++newp->cnt;

      /* This is the number of sections we allocated.  */
      newp->max = elf->state.elf.scnincr;

      /* Remember the index for the first section in this block.  */
      newp->data[0].index
	= 1 + elf->state.elf.scns_last->data[elf->state.elf.scns_last->max - 1].index;

      /* Enqueue the new list element.  */
      elf->state.elf.scns_last = elf->state.elf.scns_last->next = newp;
    }

  /* Create a section header for this section.  */
  if (elf->class == ELFCLASS32)
    {
      result->shdr.e32 = (Elf32_Shdr *) calloc (1, sizeof (Elf32_Shdr));
      if (result->shdr.e32 == NULL)
	{
	  __libelf_seterrno (ELF_E_NOMEM);
	  goto out;
	}
    }
  else
    {
      result->shdr.e64 = (Elf64_Shdr *) calloc (1, sizeof (Elf64_Shdr));
      if (result->shdr.e64 == NULL)
	{
	  __libelf_seterrno (ELF_E_NOMEM);
	  goto out;
	}
    }

  result->elf = elf;
  result->shdr_flags = ELF_F_DIRTY | ELF_F_MALLOCED;
  result->list = elf->state.elf.scns_last;

  /* Initialize the data part.  */
  result->data_read = 1;
  if (unlikely (first))
    {
      /* For the first section we mark the data as already available.  */
      //result->data_list_rear = &result->data_list;
      first = false;
      goto again;
    }

  result->flags |= ELF_F_DIRTY;

 out:
  rwlock_unlock (elf->lock);

  return result;
}
