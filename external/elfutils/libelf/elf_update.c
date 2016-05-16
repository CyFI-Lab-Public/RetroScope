/* Update data structures for changes and write them out.
   Copyright (C) 1999, 2000, 2001, 2002, 2004, 2005, 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Contributed by Ulrich Drepper <drepper@redhat.com>, 1999.

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

#include <libelf.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>

#include "libelfP.h"


static off_t
write_file (Elf *elf, off_t size, int change_bo, size_t shnum)
{
  int class = elf->class;

  /* Check the mode bits now, before modification might change them.  */
  struct stat st;
  if (unlikely (fstat (elf->fildes, &st) != 0))
    {
      __libelf_seterrno (ELF_E_WRITE_ERROR);
      return -1;
    }

  /* Adjust the size in any case.  We do this even if we use `write'.
     We cannot do this if this file is in an archive.  We also don't
     do it *now* if we are shortening the file since this would
     prevent programs to use the data of the file in generating the
     new file.  We truncate the file later in this case.  */
  if (elf->parent == NULL
      && (elf->maximum_size == ~((size_t) 0)
	  || (size_t) size > elf->maximum_size)
      && unlikely (ftruncate (elf->fildes, size) != 0))
    {
      __libelf_seterrno (ELF_E_WRITE_ERROR);
      return -1;
    }

  /* Try to map the file if this isn't done yet.  */
  if (elf->map_address == NULL && elf->cmd == ELF_C_WRITE_MMAP)
    {
#if _MUDFLAP
      /* Mudflap doesn't grok that our mmap'd data is ok.  */
#else
      elf->map_address = mmap (NULL, size, PROT_READ | PROT_WRITE,
			       MAP_SHARED, elf->fildes, 0);
      if (unlikely (elf->map_address == MAP_FAILED))
	elf->map_address = NULL;
#endif
    }

  if (elf->map_address != NULL)
    {
      /* The file is mmaped.  */
      if ((class == ELFCLASS32
	   ? __elf32_updatemmap (elf, change_bo, shnum)
	   : __elf64_updatemmap (elf, change_bo, shnum)) != 0)
	/* Some problem while writing.  */
	size = -1;
    }
  else
    {
      /* The file is not mmaped.  */
      if ((class == ELFCLASS32
	   ? __elf32_updatefile (elf, change_bo, shnum)
	   : __elf64_updatefile (elf, change_bo, shnum)) != 0)
	/* Some problem while writing.  */
	size = -1;
    }

  if (size != -1
      && elf->parent == NULL
      && elf->maximum_size != ~((size_t) 0)
      && (size_t) size < elf->maximum_size
      && unlikely (ftruncate (elf->fildes, size) != 0))
    {
      __libelf_seterrno (ELF_E_WRITE_ERROR);
      size = -1;
    }

  /* POSIX says that ftruncate and write may clear the S_ISUID and S_ISGID
     mode bits.  So make sure we restore them afterwards if they were set.
     This is not atomic if someone else chmod's the file while we operate.  */
  if (size != -1
      && unlikely (st.st_mode & (S_ISUID | S_ISGID))
      /* fchmod ignores the bits we cannot change.  */
      && unlikely (fchmod (elf->fildes, st.st_mode) != 0))
    {
      __libelf_seterrno (ELF_E_WRITE_ERROR);
      size = -1;
    }

  if (size != -1 && elf->parent == NULL)
    elf->maximum_size = size;

  return size;
}


off_t
elf_update (elf, cmd)
     Elf *elf;
     Elf_Cmd cmd;
{
  size_t shnum;
  off_t size;
  int change_bo = 0;

  if (cmd != ELF_C_NULL
      && cmd != ELF_C_WRITE
      && unlikely (cmd != ELF_C_WRITE_MMAP))
    {
      __libelf_seterrno (ELF_E_INVALID_CMD);
      return -1;
    }

  if (elf == NULL)
    return -1;

  if (elf->kind != ELF_K_ELF)
    {
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return -1;
    }

  rwlock_wrlock (elf->lock);

  /* Make sure we have an ELF header.  */
  if (elf->state.elf.ehdr == NULL)
    {
      __libelf_seterrno (ELF_E_WRONG_ORDER_EHDR);
      size = -1;
      goto out;
    }

  /* Determine the number of sections.  */
  shnum = (elf->state.elf.scns_last->cnt == 0
	   ? 0
	   : 1 + elf->state.elf.scns_last->data[elf->state.elf.scns_last->cnt - 1].index);

  /* Update the ELF descriptor.  First, place the program header.  It
     will come right after the ELF header.  The count the size of all
     sections and finally place the section table.  */
  size = (elf->class == ELFCLASS32
	  ? __elf32_updatenull_wrlock (elf, &change_bo, shnum)
	  : __elf64_updatenull_wrlock (elf, &change_bo, shnum));
  if (likely (size != -1)
      /* See whether we actually have to write out the data.  */
      && (cmd == ELF_C_WRITE || cmd == ELF_C_WRITE_MMAP))
    {
      if (elf->cmd != ELF_C_RDWR
	  && elf->cmd != ELF_C_RDWR_MMAP
	  && elf->cmd != ELF_C_WRITE
	  && unlikely (elf->cmd != ELF_C_WRITE_MMAP))
	{
	  __libelf_seterrno (ELF_E_UPDATE_RO);
	  size = -1;
	}
      else if (unlikely (elf->fildes == -1))
	{
	  /* We closed the file already.  */
	  __libelf_seterrno (ELF_E_FD_DISABLED);
	  size = -1;
	}
      else
	size = write_file (elf, size, change_bo, shnum);
    }

 out:
  rwlock_unlock (elf->lock);

  return size;
}
