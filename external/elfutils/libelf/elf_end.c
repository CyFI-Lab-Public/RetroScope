/* Free resources associated with Elf descriptor.
   Copyright (C) 1998,1999,2000,2001,2002,2004,2005,2007 Red Hat, Inc.
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
#include <stddef.h>
#include <stdlib.h>
#include <sys/mman.h>

#include "libelfP.h"


int
elf_end (elf)
     Elf *elf;
{
  Elf *parent;

  if (elf == NULL)
    /* This is allowed and is a no-op.  */
    return 0;

  /* Make sure we are alone.  */
  rwlock_wrlock (elf->lock);

  if (elf->ref_count != 0 && --elf->ref_count != 0)
    {
      /* Not yet the last activation.  */
      int result = elf->ref_count;
      rwlock_unlock (elf->lock);
      return result;
    }

  if (elf->kind == ELF_K_AR)
    {
      /* We cannot remove the descriptor now since we still have some
	 descriptors which depend on it.  But we can free the archive
	 symbol table since this is only available via the archive ELF
	 descriptor.  The long name table cannot be freed yet since
	 the archive headers for the ELF files in the archive point
	 into this array.  */
      if (elf->state.ar.ar_sym != (Elf_Arsym *) -1l)
	free (elf->state.ar.ar_sym);
      elf->state.ar.ar_sym = NULL;

      if (elf->state.ar.children != NULL)
	return 0;
    }

  /* Remove this structure from the children list.  */
  parent = elf->parent;
  if (parent != NULL)
    {
      /* This is tricky.  Lock must be acquire from the father to
	 the child but here we already have the child lock.  We
	 solve this problem by giving free the child lock.  The
	 state of REF_COUNT==0 is handled all over the library, so
	 this should be ok.  */
      rwlock_unlock (elf->lock);
      rwlock_rdlock (parent->lock);
      rwlock_wrlock (elf->lock);

      if (parent->state.ar.children == elf)
	parent->state.ar.children = elf->next;
      else
	{
	  struct Elf *child = parent->state.ar.children;

	  while (child->next != elf)
	    child = child->next;

	  child->next = elf->next;
	}

      rwlock_unlock (parent->lock);
    }

  /* This was the last activation.  Free all resources.  */
  switch (elf->kind)
    {
    case ELF_K_AR:
      if (elf->state.ar.long_names != NULL)
	free (elf->state.ar.long_names);
      break;

    case ELF_K_ELF:
      {
	Elf_Data_Chunk *rawchunks
	  = (elf->class == ELFCLASS32
	     || (offsetof (struct Elf, state.elf32.rawchunks)
		 == offsetof (struct Elf, state.elf64.rawchunks))
	     ? elf->state.elf32.rawchunks
	     : elf->state.elf64.rawchunks);
	while (rawchunks != NULL)
	  {
	    Elf_Data_Chunk *next = rawchunks->next;
	    if (rawchunks->dummy_scn.flags & ELF_F_MALLOCED)
	      free (rawchunks->data.d.d_buf);
	    free (rawchunks);
	    rawchunks = next;
	  }

	Elf_ScnList *list = (elf->class == ELFCLASS32
			     || (offsetof (struct Elf, state.elf32.scns)
				 == offsetof (struct Elf, state.elf64.scns))
			     ? &elf->state.elf32.scns
			     : &elf->state.elf64.scns);

	do
	  {
	    /* Free all separately allocated section headers.  */
	    size_t cnt = list->max;

	    while (cnt-- > 0)
	      {
		/* These pointers can be NULL; it's safe to use
		   'free' since it will check for this.  */
		Elf_Scn *scn = &list->data[cnt];
		Elf_Data_List *runp;

		if ((scn->shdr_flags & ELF_F_MALLOCED) != 0)
		  /* It doesn't matter which pointer.  */
		  free (scn->shdr.e32);

		/* If the file has the same byte order and the
		   architecture doesn't require overly stringent
		   alignment the raw data buffer is the same as the
		   one used for presenting to the caller.  */
		if (scn->data_base != scn->rawdata_base)
		  free (scn->data_base);

		/* The section data is allocated if we couldn't mmap
		   the file.  */
		if (elf->map_address == NULL)
		  free (scn->rawdata_base);

		/* Free the list of data buffers for the section.
		   We don't free the buffers themselves since this
		   is the users job.  */
		runp = scn->data_list.next;
		while (runp != NULL)
		  {
		    Elf_Data_List *oldp = runp;
		    runp = runp->next;
		    if ((oldp->flags & ELF_F_MALLOCED) != 0)
		      free (oldp);
		  }
	      }

	    /* Free the memory for the array.  */
	    Elf_ScnList *oldp = list;
	    list = list->next;
	    assert (list == NULL || oldp->cnt == oldp->max);
	    if (oldp != (elf->class == ELFCLASS32
			 || (offsetof (struct Elf, state.elf32.scns)
			     == offsetof (struct Elf, state.elf64.scns))
			 ? &elf->state.elf32.scns
			 : &elf->state.elf64.scns))
	      free (oldp);
	  }
	while (list != NULL);
      }

      /* Free the section header.  */
      if (elf->state.elf.shdr_malloced  != 0)
	free (elf->class == ELFCLASS32
	      || (offsetof (struct Elf, state.elf32.shdr)
		  == offsetof (struct Elf, state.elf64.shdr))
	      ? (void *) elf->state.elf32.shdr
	      : (void *) elf->state.elf64.shdr);

      /* Free the program header.  */
      if ((elf->state.elf.phdr_flags & ELF_F_MALLOCED) != 0)
	free (elf->class == ELFCLASS32
	      || (offsetof (struct Elf, state.elf32.phdr)
		  == offsetof (struct Elf, state.elf64.phdr))
	      ? (void *) elf->state.elf32.phdr
	      : (void *) elf->state.elf64.phdr);
      break;

    default:
      break;
    }

  if (elf->map_address != NULL && parent == NULL)
    {
      /* The file was read or mapped for this descriptor.  */
      if ((elf->flags & ELF_F_MALLOCED) != 0)
	free (elf->map_address);
      else if ((elf->flags & ELF_F_MMAPPED) != 0)
	munmap (elf->map_address, elf->maximum_size);
    }

  rwlock_fini (elf->lock);

  /* Finally the descriptor itself.  */
  free (elf);

  return (parent != NULL && parent->ref_count == 0
	  ? INTUSE(elf_end) (parent) : 0);
}
INTDEF(elf_end)
