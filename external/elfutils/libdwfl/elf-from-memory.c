/* Reconstruct an ELF file by reading the segments out of remote memory.
   Copyright (C) 2005, 2006, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.

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

#include <config.h>
#include "../libelf/libelfP.h"
#undef _

#include "libdwflP.h"

#include <gelf.h>
#include <sys/types.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

/* Reconstruct an ELF file by reading the segments out of remote memory
   based on the ELF file header at EHDR_VMA and the ELF program headers it
   points to.  If not null, *LOADBASEP is filled in with the difference
   between the addresses from which the segments were read, and the
   addresses the file headers put them at.

   The function READ_MEMORY is called to copy at least MINREAD and at most
   MAXREAD bytes from the remote memory at target address ADDRESS into the
   local buffer at DATA; it should return -1 for errors (with code in
   `errno'), 0 if it failed to read at least MINREAD bytes due to EOF, or
   the number of bytes read if >= MINREAD.  ARG is passed through.  */

Elf *
elf_from_remote_memory (GElf_Addr ehdr_vma,
			GElf_Addr *loadbasep,
			ssize_t (*read_memory) (void *arg, void *data,
						GElf_Addr address,
						size_t minread,
						size_t maxread),
			void *arg)
{
  /* First read in the file header and check its sanity.  */

  const size_t initial_bufsize = 256;
  unsigned char *buffer = malloc (initial_bufsize);
  if (buffer == NULL)
    {
    no_memory:
      __libdwfl_seterrno (DWFL_E_NOMEM);
      return NULL;
    }

  ssize_t nread = (*read_memory) (arg, buffer, ehdr_vma,
				  sizeof (Elf32_Ehdr), initial_bufsize);
  if (nread <= 0)
    {
    read_error:
      free (buffer);
      __libdwfl_seterrno (nread < 0 ? DWFL_E_ERRNO : DWFL_E_TRUNCATED);
      return NULL;
    }

  if (memcmp (buffer, ELFMAG, SELFMAG) != 0)
    {
    bad_elf:
      __libdwfl_seterrno (DWFL_E_BADELF);
      return NULL;
    }

  /* Extract the information we need from the file header.  */

  union
  {
    Elf32_Ehdr e32;
    Elf64_Ehdr e64;
  } ehdr;
  Elf_Data xlatefrom =
    {
      .d_type = ELF_T_EHDR,
      .d_buf = buffer,
      .d_version = EV_CURRENT,
    };
  Elf_Data xlateto =
    {
      .d_type = ELF_T_EHDR,
      .d_buf = &ehdr,
      .d_size = sizeof ehdr,
      .d_version = EV_CURRENT,
    };

  GElf_Off phoff;
  uint_fast16_t phnum;
  uint_fast16_t phentsize;
  GElf_Off shdrs_end;

  switch (buffer[EI_CLASS])
    {
    case ELFCLASS32:
      xlatefrom.d_size = sizeof (Elf32_Ehdr);
      if (elf32_xlatetom (&xlateto, &xlatefrom, buffer[EI_DATA]) == NULL)
	{
	libelf_error:
	  __libdwfl_seterrno (DWFL_E_LIBELF);
	  return NULL;
	}
      phoff = ehdr.e32.e_phoff;
      phnum = ehdr.e32.e_phnum;
      phentsize = ehdr.e32.e_phentsize;
      if (phentsize != sizeof (Elf32_Phdr) || phnum == 0)
	goto bad_elf;
      shdrs_end = ehdr.e32.e_shoff + ehdr.e32.e_shnum * ehdr.e32.e_shentsize;
      break;

    case ELFCLASS64:
      xlatefrom.d_size = sizeof (Elf64_Ehdr);
      if (elf64_xlatetom (&xlateto, &xlatefrom, buffer[EI_DATA]) == NULL)
	goto libelf_error;
      phoff = ehdr.e64.e_phoff;
      phnum = ehdr.e64.e_phnum;
      phentsize = ehdr.e64.e_phentsize;
      if (phentsize != sizeof (Elf64_Phdr) || phnum == 0)
	goto bad_elf;
      shdrs_end = ehdr.e64.e_shoff + ehdr.e64.e_shnum * ehdr.e64.e_shentsize;
      break;

    default:
      goto bad_elf;
    }


  /* The file header tells where to find the program headers.
     These are what we use to actually choose what to read.  */

  xlatefrom.d_type = xlateto.d_type = ELF_T_PHDR;
  xlatefrom.d_size = phnum * phentsize;

  if ((size_t) nread >= phoff + phnum * phentsize)
    /* We already have all the phdrs from the initial read.  */
    xlatefrom.d_buf = buffer + phoff;
  else
    {
      /* Read in the program headers.  */

      if (initial_bufsize < phnum * phentsize)
	{
	  unsigned char *newbuf = realloc (buffer, phnum * phentsize);
	  if (newbuf == NULL)
	    {
	      free (buffer);
	      goto no_memory;
	    }
	  buffer = newbuf;
	}
      nread = (*read_memory) (arg, buffer, ehdr_vma + phoff,
			      phnum * phentsize, phnum * phentsize);
      if (nread <= 0)
	goto read_error;

      xlatefrom.d_buf = buffer;
    }

  union
  {
    Elf32_Phdr p32[phnum];
    Elf64_Phdr p64[phnum];
  } phdrs;

  xlateto.d_buf = &phdrs;
  xlateto.d_size = sizeof phdrs;

  /* Scan for PT_LOAD segments to find the total size of the file image.  */
  size_t contents_size = 0;
  GElf_Off segments_end = 0;
  GElf_Addr loadbase = ehdr_vma;
  bool found_base = false;
  switch (ehdr.e32.e_ident[EI_CLASS])
    {
      inline void handle_segment (GElf_Addr vaddr, GElf_Off offset,
				  GElf_Xword filesz, GElf_Xword align)
	{
	  GElf_Off segment_end = ((offset + filesz + align - 1) & -align);

	  if (segment_end > (GElf_Off) contents_size)
	    contents_size = segment_end;

	  if (!found_base && (offset & -align) == 0)
	    {
	      loadbase = ehdr_vma - (vaddr & -align);
	      found_base = true;
	    }

	  segments_end = offset + filesz;
	}

    case ELFCLASS32:
      if (elf32_xlatetom (&xlateto, &xlatefrom,
			  ehdr.e32.e_ident[EI_DATA]) == NULL)
	goto libelf_error;
      for (uint_fast16_t i = 0; i < phnum; ++i)
	if (phdrs.p32[i].p_type == PT_LOAD)
	  handle_segment (phdrs.p32[i].p_vaddr, phdrs.p32[i].p_offset,
			  phdrs.p32[i].p_filesz, phdrs.p32[i].p_align);
      break;

    case ELFCLASS64:
      if (elf32_xlatetom (&xlateto, &xlatefrom,
			  ehdr.e32.e_ident[EI_DATA]) == NULL)
	goto libelf_error;
      for (uint_fast16_t i = 0; i < phnum; ++i)
	if (phdrs.p32[i].p_type == PT_LOAD)
	  handle_segment (phdrs.p64[i].p_vaddr, phdrs.p64[i].p_offset,
			  phdrs.p64[i].p_filesz, phdrs.p64[i].p_align);
      break;

    default:
      abort ();
      break;
    }

  /* Trim the last segment so we don't bother with zeros in the last page
     that are off the end of the file.  However, if the extra bit in that
     page includes the section headers, keep them.  */
  if ((GElf_Off) contents_size > segments_end
      && (GElf_Off) contents_size >= shdrs_end)
    {
      contents_size = segments_end;
      if ((GElf_Off) contents_size < shdrs_end)
	contents_size = shdrs_end;
    }
  else
    contents_size = segments_end;

  free (buffer);

  /* Now we know the size of the whole image we want read in.  */
  buffer = calloc (1, contents_size);
  if (buffer == NULL)
    goto no_memory;

  switch (ehdr.e32.e_ident[EI_CLASS])
    {
      inline bool handle_segment (GElf_Addr vaddr, GElf_Off offset,
				  GElf_Xword filesz, GElf_Xword align)
	{
	  GElf_Off start = offset & -align;
	  GElf_Off end = (offset + filesz + align - 1) & -align;
	  if (end > (GElf_Off) contents_size)
	    end = contents_size;
	  nread = (*read_memory) (arg, buffer + start,
				  (loadbase + vaddr) & -align,
				  end - start, end - start);
	  return nread <= 0;
	}

    case ELFCLASS32:
      for (uint_fast16_t i = 0; i < phnum; ++i)
	if (phdrs.p32[i].p_type == PT_LOAD)
	  if (handle_segment (phdrs.p32[i].p_vaddr, phdrs.p32[i].p_offset,
			      phdrs.p32[i].p_filesz, phdrs.p32[i].p_align))
	    goto read_error;

      /* If the segments visible in memory didn't include the section
	 headers, then clear them from the file header.  */
      if (contents_size < shdrs_end)
	{
	  ehdr.e32.e_shoff = 0;
	  ehdr.e32.e_shnum = 0;
	  ehdr.e32.e_shstrndx = 0;
	}

      /* This will normally have been in the first PT_LOAD segment.  But it
	 conceivably could be missing, and we might have just changed it.  */
      xlatefrom.d_type = xlateto.d_type = ELF_T_EHDR;
      xlatefrom.d_size = xlateto.d_size = sizeof ehdr.e32;
      xlatefrom.d_buf = &ehdr.e32;
      xlateto.d_buf = buffer;
      if (elf32_xlatetof (&xlateto, &xlatefrom,
			  ehdr.e32.e_ident[EI_DATA]) == NULL)
	goto libelf_error;
      break;

    case ELFCLASS64:
      for (uint_fast16_t i = 0; i < phnum; ++i)
	if (phdrs.p32[i].p_type == PT_LOAD)
	  if (handle_segment (phdrs.p64[i].p_vaddr, phdrs.p64[i].p_offset,
			      phdrs.p64[i].p_filesz, phdrs.p64[i].p_align))
	    goto read_error;

      /* If the segments visible in memory didn't include the section
	 headers, then clear them from the file header.  */
      if (contents_size < shdrs_end)
	{
	  ehdr.e64.e_shoff = 0;
	  ehdr.e64.e_shnum = 0;
	  ehdr.e64.e_shstrndx = 0;
	}

      /* This will normally have been in the first PT_LOAD segment.  But it
	 conceivably could be missing, and we might have just changed it.  */
      xlatefrom.d_type = xlateto.d_type = ELF_T_EHDR;
      xlatefrom.d_size = xlateto.d_size = sizeof ehdr.e64;
      xlatefrom.d_buf = &ehdr.e64;
      xlateto.d_buf = buffer;
      if (elf64_xlatetof (&xlateto, &xlatefrom,
			  ehdr.e64.e_ident[EI_DATA]) == NULL)
	goto libelf_error;
      break;

    default:
      abort ();
      break;
    }

  /* Now we have the image.  Open libelf on it.  */

  Elf *elf = elf_memory ((char *) buffer, contents_size);
  if (elf == NULL)
    {
      free (buffer);
      goto libelf_error;
    }

  elf->flags |= ELF_F_MALLOCED;
  if (loadbasep != NULL)
    *loadbasep = loadbase;
  return elf;
}
