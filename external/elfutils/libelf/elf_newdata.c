/* Create new, empty section data.
   Copyright (C) 1998, 1999, 2000, 2001, 2002 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Contributed by Ulrich Drepper <drepper@redhat.com>, 1998.

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

#include <stddef.h>
#include <stdlib.h>

#include "libelfP.h"


Elf_Data *
elf_newdata (Elf_Scn *scn)
{
  Elf_Data_List *result = NULL;

  if (scn == NULL)
    return NULL;

  if (unlikely (scn->index == 0))
    {
      /* It is not allowed to add something to the 0th section.  */
      __libelf_seterrno (ELF_E_NOT_NUL_SECTION);
      return NULL;
    }

  if (scn->elf->class == ELFCLASS32
      || (offsetof (struct Elf, state.elf32.ehdr)
	  == offsetof (struct Elf, state.elf64.ehdr))
      ? scn->elf->state.elf32.ehdr == NULL
      : scn->elf->state.elf64.ehdr == NULL)
    {
      __libelf_seterrno (ELF_E_WRONG_ORDER_EHDR);
      return NULL;
    }

  rwlock_wrlock (scn->elf->lock);

  if (scn->data_read && scn->data_list_rear == NULL)
    {
      /* This means the section was created by the user and this is the
	 first data.  */
      result = &scn->data_list;
      result->flags = ELF_F_DIRTY;
    }
  else
    {
      /* Create a new, empty data descriptor.  */
      result = (Elf_Data_List *) calloc (1, sizeof (Elf_Data_List));
      if (result == NULL)
	{
	  __libelf_seterrno (ELF_E_NOMEM);
	  goto out;
	}

      result->flags = ELF_F_DIRTY | ELF_F_MALLOCED;

      if (scn->data_list_rear == NULL)
	/* We create new data without reading/converting the data from the
	   file.  That is fine but we have to remember this.  */
	scn->data_list_rear = &scn->data_list;
    }

  /* Set the predefined values.  */
  result->data.d.d_version = __libelf_version;

  result->data.s = scn;

  /* Add to the end of the list.  */
  if (scn->data_list_rear != NULL)
    scn->data_list_rear->next = result;

  scn->data_list_rear = result;

 out:
  rwlock_unlock (scn->elf->lock);

  /* Please note that the following is thread safe and is also defined
     for RESULT == NULL since it still return NULL.  */
  return &result->data.d;
}
