/* Return symbol table of archive.
   Copyright (C) 1998, 1999, 2000, 2002, 2005 Red Hat, Inc.
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
#include <byteswap.h>
#include <endian.h>
#include <errno.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <system.h>
#include <dl-hash.h>
#include "libelfP.h"


Elf_Arsym *
elf_getarsym (elf, ptr)
     Elf *elf;
     size_t *ptr;
{
  if (elf->kind != ELF_K_AR)
    {
      /* This is no archive.  */
      __libelf_seterrno (ELF_E_NO_ARCHIVE);
      return NULL;
    }

  if (ptr != NULL)
    /* In case of an error or when we know the value store the expected
       value now.  Doing this allows us easier exits in an error case.  */
    *ptr = elf->state.ar.ar_sym_num;

  if (elf->state.ar.ar_sym == (Elf_Arsym *) -1l)
    {
      /* There is no index.  */
      __libelf_seterrno (ELF_E_NO_INDEX);
      return NULL;
    }

  Elf_Arsym *result = elf->state.ar.ar_sym;
  if (result == NULL)
    {
      /* We have not yet read the index.  */
      rwlock_wrlock (elf->lock);

      /* In case we find no index remember this for the next call.  */
      elf->state.ar.ar_sym = (Elf_Arsym *) -1l;

      struct ar_hdr *index_hdr;
      if (elf->map_address == NULL)
	{
	  /* We must read index from the file.  */
	  assert (elf->fildes != -1);
	  if (pread_retry (elf->fildes, &elf->state.ar.ar_hdr,
			   sizeof (struct ar_hdr), elf->start_offset + SARMAG)
	      != sizeof (struct ar_hdr))
	    {
	      /* It is not possible to read the index.  Maybe it does not
		 exist.  */
	      __libelf_seterrno (ELF_E_READ_ERROR);
	      goto out;
	    }

	  index_hdr = &elf->state.ar.ar_hdr;
	}
      else
	{
	  if (SARMAG + sizeof (struct ar_hdr) > elf->maximum_size)
	    {
	      /* There is no room for the full archive.  */
	      __libelf_seterrno (ELF_E_NO_INDEX);
	      goto out;
	    }

	  index_hdr = (struct ar_hdr *) (elf->map_address
					 + elf->start_offset + SARMAG);
	}

      /* Now test whether this really is an archive.  */
      if (memcmp (index_hdr->ar_fmag, ARFMAG, 2) != 0)
	{
	  /* Invalid magic bytes.  */
	  __libelf_seterrno (ELF_E_ARCHIVE_FMAG);
	  goto out;
	}

      /* Now test whether this is the index.  It is denoted by the
	 name being "/ ".
	 XXX This is not entirely true.  There are some more forms.
	 Which of them shall we handle?  */
      if (memcmp (index_hdr->ar_name, "/               ", 16) != 0)
	{
	  /* If the index is not the first entry, there is no index.

	     XXX Is this true?  */
	  __libelf_seterrno (ELF_E_NO_INDEX);
	  goto out;
	}

      /* We have an archive.  The first word in there is the number of
	 entries in the table.  */
      uint32_t n;
      if (elf->map_address == NULL)
	{
	  if (pread_retry (elf->fildes, &n, sizeof (n),
			   elf->start_offset + SARMAG + sizeof (struct ar_hdr))
	      != sizeof (n))
	    {
	      /* Cannot read the number of entries.  */
	      __libelf_seterrno (ELF_E_NO_INDEX);
	      goto out;
	    }
	}
      else
	n = *(uint32_t *) (elf->map_address + elf->start_offset
			   + SARMAG + sizeof (struct ar_hdr));

      if (__BYTE_ORDER == __LITTLE_ENDIAN)
	n = bswap_32 (n);

      /* Now we can perform some first tests on whether all the data
	 needed for the index is available.  */
      char tmpbuf[17];
      memcpy (tmpbuf, index_hdr->ar_size, 10);
      tmpbuf[10] = '\0';
      size_t index_size = atol (tmpbuf);

      if (SARMAG + sizeof (struct ar_hdr) + index_size > elf->maximum_size
	  || n * sizeof (uint32_t) > index_size)
	{
	  /* This index table cannot be right since it does not fit into
	     the file.  */
	  __libelf_seterrno (ELF_E_NO_INDEX);
	  goto out;
	}

      /* Now we can allocate the arrays needed to store the index.  */
      size_t ar_sym_len = (n + 1) * sizeof (Elf_Arsym);
      elf->state.ar.ar_sym = (Elf_Arsym *) malloc (ar_sym_len);
      if (elf->state.ar.ar_sym != NULL)
	{
	  uint32_t *file_data;
	  char *str_data;

	  if (elf->map_address == NULL)
	    {
	      file_data = (uint32_t *) alloca (n * sizeof (uint32_t));

	      ar_sym_len += index_size - n * sizeof (uint32_t);
	      Elf_Arsym *newp = (Elf_Arsym *) realloc (elf->state.ar.ar_sym,
						       ar_sym_len);
	      if (newp == NULL)
		{
		  free (elf->state.ar.ar_sym);
		  elf->state.ar.ar_sym = NULL;
		  __libelf_seterrno (ELF_E_NOMEM);
		  goto out;
		}
	      elf->state.ar.ar_sym = newp;

	      char *new_str = (char *) (elf->state.ar.ar_sym + n + 1);

	      /* Now read the data from the file.  */
	      if ((size_t) pread_retry (elf->fildes, file_data,
					n * sizeof (uint32_t),
					elf->start_offset + SARMAG
					+ sizeof (struct ar_hdr)
					+ sizeof (uint32_t))
		  != n * sizeof (uint32_t)
		  || ((size_t) pread_retry (elf->fildes, new_str,
					    index_size - n * sizeof (uint32_t),
					    elf->start_offset
					    + SARMAG + sizeof (struct ar_hdr)
					    + (n + 1) * sizeof (uint32_t))
		      != index_size - n * sizeof (uint32_t)))
		{
		  /* We were not able to read the data.  */
		  free (elf->state.ar.ar_sym);
		  elf->state.ar.ar_sym = NULL;
		  __libelf_seterrno (ELF_E_NO_INDEX);
		  goto out;
		}

	      str_data = (char *) new_str;
	    }
	  else
	    {
	      file_data = (uint32_t *) (elf->map_address + elf->start_offset
					+ SARMAG + sizeof (struct ar_hdr)
					+ sizeof (uint32_t));
	      str_data = (char *) &file_data[n];
	    }

	  /* Now we can build the data structure.  */
	  Elf_Arsym *arsym = elf->state.ar.ar_sym;
	  for (size_t cnt = 0; cnt < n; ++cnt)
	    {
	      arsym[cnt].as_name = str_data;
	      if (__BYTE_ORDER == __LITTLE_ENDIAN)
		arsym[cnt].as_off = bswap_32 (file_data[cnt]);
	      else
		arsym[cnt].as_off = file_data[cnt];
	      arsym[cnt].as_hash = _dl_elf_hash (str_data);
	      str_data = rawmemchr (str_data, '\0') + 1;
	    }
	  /* At the end a special entry.  */
	  arsym[n].as_name = NULL;
	  arsym[n].as_off = 0;
	  arsym[n].as_hash = ~0UL;

	  /* Tell the caller how many entries we have.  */
	  elf->state.ar.ar_sym_num = n + 1;
	}

      result = elf->state.ar.ar_sym;

    out:
      rwlock_unlock (elf->lock);
    }

  if (ptr != NULL)
    *ptr = elf->state.ar.ar_sym_num;

  return result;
}
