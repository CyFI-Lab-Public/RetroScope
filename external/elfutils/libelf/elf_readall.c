/* Read all of the file associated with the descriptor.
   Copyright (C) 1998, 1999, 2000, 2001, 2002, 2005 Red Hat, Inc.
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

#include <errno.h>
#include <unistd.h>

#include <system.h>
#include "libelfP.h"
#include "common.h"


static void
set_address (Elf *elf, size_t offset)
{
  if (elf->kind == ELF_K_AR)
    {
      Elf *child = elf->state.ar.children;

      while (child != NULL)
	{
	  if (child->map_address == NULL)
	    {
	      child->map_address = elf->map_address;
	      child->start_offset -= offset;
	      if (child->kind == ELF_K_AR)
		child->state.ar.offset -= offset;

	      set_address (child, offset);
	    }

	  child = child->next;
	}
    }
}


char *
__libelf_readall (elf)
     Elf *elf;
{
  /* Get the file.  */
  rwlock_wrlock (elf->lock);

  if (elf->map_address == NULL && unlikely (elf->fildes == -1))
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      rwlock_unlock (elf->lock);
      return NULL;
    }

  /* If the file is not mmap'ed and not previously loaded, do it now.  */
  if (elf->map_address == NULL)
    {
      char *mem;

      /* If this is an archive and we have derived descriptors get the
	 locks for all of them.  */
      libelf_acquire_all (elf);

      /* Allocate all the memory we need.  */
      mem = (char *) malloc (elf->maximum_size);
      if (mem != NULL)
	{
	  /* Read the file content.  */
	  if (unlikely ((size_t) pread_retry (elf->fildes, mem,
					      elf->maximum_size,
					      elf->start_offset)
			!= elf->maximum_size))
	    {
	      /* Something went wrong.  */
	      __libelf_seterrno (ELF_E_READ_ERROR);
	      free (mem);
	    }
	  else
	    {
	      /* Remember the address.  */
	      elf->map_address = mem;

	      /* Also remember that we allocated the memory.  */
	      elf->flags |= ELF_F_MALLOCED;

	      /* Propagate the information down to all children and
		 their children.  */
	      set_address (elf, elf->start_offset);

	      /* Correct the own offsets.  */
	      if (elf->kind == ELF_K_AR)
		elf->state.ar.offset -= elf->start_offset;
	      elf->start_offset = 0;
	    }
	}
      else
	__libelf_seterrno (ELF_E_NOMEM);

      /* Free the locks on the children.  */
      libelf_release_all (elf);
    }

  rwlock_unlock (elf->lock);

  return (char *) elf->map_address;
}
