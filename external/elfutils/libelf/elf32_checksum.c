/* Compute simple checksum from permanent parts of the ELF file.
   Copyright (C) 2002, 2003, 2004, 2005 Red Hat, Inc.
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
#include <endian.h>
#include <stdbool.h>
#include <stddef.h>
#include <string.h>

#include "gelf.h"
#include "libelfP.h"
#include "elf-knowledge.h"

#ifndef LIBELFBITS
# define LIBELFBITS 32
#endif


/* The SECTION_STRIP_P macro wants to call into libebl which we cannot
   do and do not have to do here.  Provide a dummy replacement.  */
#define ebl_debugscn_p(ebl, name) true


#define process_block(crc, data) \
  __libelf_crc32 (crc, data->d_buf, data->d_size)


long int
elfw2(LIBELFBITS,checksum) (elf)
     Elf *elf;
{
  size_t shstrndx;
  Elf_Scn *scn;
  long int result = 0;
  unsigned char *ident;
  bool same_byte_order;

  if (elf == NULL)
    return -1l;

  /* Find the section header string table.  */
  if  (INTUSE(elf_getshstrndx) (elf, &shstrndx) < 0)
    {
      /* This can only happen if the ELF handle is not for real.  */
      __libelf_seterrno (ELF_E_INVALID_HANDLE);
      return -1l;
    }

  /* Determine whether the byte order of the file and that of the host
     is the same.  */
  ident = elf->state.ELFW(elf,LIBELFBITS).ehdr->e_ident;
  same_byte_order = ((ident[EI_DATA] == ELFDATA2LSB
		      && __BYTE_ORDER == __LITTLE_ENDIAN)
		     || (ident[EI_DATA] == ELFDATA2MSB
			 && __BYTE_ORDER == __BIG_ENDIAN));

  /* If we don't have native byte order, we will likely need to
     convert the data with xlate functions.  We do it upfront instead
     of relocking mid-iteration. */
  if (!likely (same_byte_order))
    rwlock_wrlock (elf->lock);
  else
    rwlock_rdlock (elf->lock);

  /* Iterate over all sections to find those which are not strippable.  */
  scn = NULL;
  while ((scn = INTUSE(elf_nextscn) (elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *shdr;
      Elf_Data *data;

      /* Get the section header.  */
      shdr = INTUSE(gelf_getshdr) (scn, &shdr_mem);
      if (shdr == NULL)
	{
	  __libelf_seterrno (ELF_E_INVALID_SECTION_HEADER);
	  result = -1l;
	  goto out;
	}

      if (SECTION_STRIP_P (shdr,
			   INTUSE(elf_strptr) (elf, shstrndx, shdr->sh_name),
			   true))
	/* The section can be stripped.  Don't use it.  */
	continue;

      /* Do not look at NOBITS sections.  */
      if (shdr->sh_type == SHT_NOBITS)
	continue;

      /* To compute the checksum we need to get to the data.  For
	 repeatable results we must use the external format.  The data
	 we get with 'elf'getdata' might be changed for endianess
	 reasons.  Therefore we use 'elf_rawdata' if possible.  But
	 this function can fail if the data was constructed by the
	 program.  In this case we have to use 'elf_getdata' and
	 eventually convert the data to the external format.  */
      data = INTUSE(elf_rawdata) (scn, NULL);
      if (data != NULL)
	{
	  /* The raw data is available.  */
	  result = process_block (result, data);

	  /* Maybe the user added more data.  These blocks cannot be
	     read using 'elf_rawdata'.  Simply proceed with looking
	     for more data block with 'elf_getdata'.  */
	}

      /* Iterate through the list of data blocks.  */
      while ((data = INTUSE(elf_getdata) (scn, data)) != NULL)
	/* If the file byte order is the same as the host byte order
	   process the buffer directly.  If the data is just a stream
	   of bytes which the library will not convert we can use it
	   as well.  */
	if (likely (same_byte_order) || data->d_type == ELF_T_BYTE)
	  result = process_block (result, data);
	else
	  {
	    /* Convert the data to file byte order.  */
	    if (INTUSE(elfw2(LIBELFBITS,xlatetof)) (data, data, ident[EI_DATA])
		== NULL)
	      {
		result = -1l;
		goto out;
	      }

	    result = process_block (result, data);

	    /* And convert it back.  */
	    if (INTUSE(elfw2(LIBELFBITS,xlatetom)) (data, data, ident[EI_DATA])
		== NULL)
	      {
		result = -1l;
		goto out;
	      }
	  }
    }

 out:
  rwlock_unlock (elf->lock);
  return result;
}
INTDEF(elfw2(LIBELFBITS,checksum))
