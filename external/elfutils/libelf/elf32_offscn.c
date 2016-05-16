/* Get section at specific index.
   Copyright (C) 2005, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Contributed by Ulrich Drepper <drepper@redhat.com>, 2005.

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
#include <stddef.h>
#include <stdlib.h>

#include "libelfP.h"

#ifndef LIBELFBITS
# define LIBELFBITS 32
#endif


Elf_Scn *
elfw2(LIBELFBITS,offscn) (elf, offset)
     Elf *elf;
     ElfW2(LIBELFBITS,Off) offset;
{
  if (elf == NULL)
    return NULL;

  if (unlikely (elf->kind != ELF_K_ELF))
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return NULL;
    }

  Elf_ScnList *runp = &elf->state.ELFW(elf,LIBELFBITS).scns;

  /* If we have not looked at section headers before,
     we might need to read them in first.  */
  if (runp->cnt > 0
      && unlikely (runp->data[0].shdr.ELFW(e,LIBELFBITS) == NULL)
      && unlikely (elfw2(LIBELFBITS,getshdr) (&runp->data[0]) == NULL))
    return NULL;

  rwlock_rdlock (elf->lock);

  Elf_Scn *result = NULL;

  /* Find the section in the list.  */
  while (1)
    {
      for (unsigned int i = 0; i < runp->cnt; ++i)
	if (runp->data[i].shdr.ELFW(e,LIBELFBITS)->sh_offset == offset)
	  {
	    result = &runp->data[i];

	    /* If this section is empty, the following one has the same
	       sh_offset.  We presume the caller is looking for a nonempty
	       section, so keep looking if this one is empty.  */
	    if (runp->data[i].shdr.ELFW(e,LIBELFBITS)->sh_size != 0)
	      goto out;
	  }

      runp = runp->next;
      if (runp == NULL)
	{
	  __libelf_seterrno (ELF_E_INVALID_OFFSET);
	  break;
	}
    }

 out:
  rwlock_unlock (elf->lock);

  return result;
}
INTDEF(elfw2(LIBELFBITS,offscn))
