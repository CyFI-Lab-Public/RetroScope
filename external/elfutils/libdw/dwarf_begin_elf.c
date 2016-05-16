/* Create descriptor from ELF descriptor for processing file.
   Copyright (C) 2002, 2003, 2004, 2005, 2007 Red Hat, Inc.
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

#include <stdbool.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include "libdwP.h"


/* Section names.  */
static const char dwarf_scnnames[IDX_last][17] =
{
  [IDX_debug_info] = ".debug_info",
  [IDX_debug_abbrev] = ".debug_abbrev",
  [IDX_debug_aranges] = ".debug_aranges",
  [IDX_debug_line] = ".debug_line",
  [IDX_debug_frame] = ".debug_frame",
  [IDX_eh_frame] = ".eh_frame",
  [IDX_debug_loc] = ".debug_loc",
  [IDX_debug_pubnames] = ".debug_pubnames",
  [IDX_debug_str] = ".debug_str",
  [IDX_debug_funcnames] = ".debug_funcnames",
  [IDX_debug_typenames] = ".debug_typenames",
  [IDX_debug_varnames] = ".debug_varnames",
  [IDX_debug_weaknames] = ".debug_weaknames",
  [IDX_debug_macinfo] = ".debug_macinfo",
  [IDX_debug_ranges] = ".debug_ranges"
};
#define ndwarf_scnnames (sizeof (dwarf_scnnames) / sizeof (dwarf_scnnames[0]))


static Dwarf *
check_section (Dwarf *result, GElf_Ehdr *ehdr, Elf_Scn *scn, bool inscngrp)
{
  GElf_Shdr shdr_mem;
  GElf_Shdr *shdr;

  /* Get the section header data.  */
  shdr = gelf_getshdr (scn, &shdr_mem);
  if (shdr == NULL)
    /* This should never happen.  If it does something is
       wrong in the libelf library.  */
    abort ();

  /* Ignore any SHT_NOBITS sections.  Debugging sections should not
     have been stripped, but in case of a corrupt file we won't try
     to look at the missing data.  */
  if (unlikely (shdr->sh_type == SHT_NOBITS))
    return result;

  /* Make sure the section is part of a section group only iff we
     really need it.  If we are looking for the global (= non-section
     group debug info) we have to ignore all the info in section
     groups.  If we are looking into a section group we cannot look at
     a section which isn't part of the section group.  */
  if (! inscngrp && (shdr->sh_flags & SHF_GROUP) != 0)
    /* Ignore the section.  */
    return result;


  /* We recognize the DWARF section by their names.  This is not very
     safe and stable but the best we can do.  */
  const char *scnname = elf_strptr (result->elf, ehdr->e_shstrndx,
				    shdr->sh_name);
  if (scnname == NULL)
    {
      /* The section name must be valid.  Otherwise is the ELF file
	 invalid.  */
      __libdw_seterrno (DWARF_E_INVALID_ELF);
      free (result);
      return NULL;
    }


  /* Recognize the various sections.  Most names start with .debug_.  */
  size_t cnt;
  for (cnt = 0; cnt < ndwarf_scnnames; ++cnt)
    if (strcmp (scnname, dwarf_scnnames[cnt]) == 0)
      {
	/* Found it.  Remember where the data is.  */
	if (unlikely (result->sectiondata[cnt] != NULL))
	  /* A section appears twice.  That's bad.  We ignore the section.  */
	  break;

	/* Get the section data.  */
	Elf_Data *data = elf_getdata (scn, NULL);
	if (data != NULL && data->d_size != 0)
	  /* Yep, there is actually data available.  */
	  result->sectiondata[cnt] = data;

	break;
      }

  return result;
}


/* Check whether all the necessary DWARF information is available.  */
static Dwarf *
valid_p (Dwarf *result)
{
  /* We looked at all the sections.  Now determine whether all the
     sections with debugging information we need are there.

     XXX Which sections are absolutely necessary?  Add tests if
     necessary.  For now we require only .debug_info.  Hopefully this
     is correct.  */
  if (likely (result != NULL)
      && unlikely (result->sectiondata[IDX_debug_info] == NULL))
    {
      __libdw_seterrno (DWARF_E_NO_DWARF);
      free (result);
      result = NULL;
    }

  return result;
}


static Dwarf *
global_read (Dwarf *result, Elf *elf, GElf_Ehdr *ehdr)
{
  Elf_Scn *scn = NULL;

  while (result != NULL && (scn = elf_nextscn (elf, scn)) != NULL)
    result = check_section (result, ehdr, scn, false);

  return valid_p (result);
}


static Dwarf *
scngrp_read (Dwarf *result, Elf *elf, GElf_Ehdr *ehdr, Elf_Scn *scngrp)
{
  /* SCNGRP is the section descriptor for a section group which might
     contain debug sections.  */
  Elf_Data *data = elf_getdata (scngrp, NULL);
  if (data == NULL)
    {
      /* We cannot read the section content.  Fail!  */
      free (result);
      return NULL;
    }

  /* The content of the section is a number of 32-bit words which
     represent section indices.  The first word is a flag word.  */
  Elf32_Word *scnidx = (Elf32_Word *) data->d_buf;
  size_t cnt;
  for (cnt = 1; cnt * sizeof (Elf32_Word) <= data->d_size; ++cnt)
    {
      Elf_Scn *scn = elf_getscn (elf, scnidx[cnt]);
      if (scn == NULL)
	{
	  /* A section group refers to a non-existing section.  Should
	     never happen.  */
	  __libdw_seterrno (DWARF_E_INVALID_ELF);
	  free (result);
	  return NULL;
	}

      result = check_section (result, ehdr, scn, true);
      if (result == NULL)
	break;
    }

  return valid_p (result);
}


Dwarf *
dwarf_begin_elf (elf, cmd, scngrp)
     Elf *elf;
     Dwarf_Cmd cmd;
     Elf_Scn *scngrp;
{
  GElf_Ehdr *ehdr;
  GElf_Ehdr ehdr_mem;

  /* Get the ELF header of the file.  We need various pieces of
     information from it.  */
  ehdr = gelf_getehdr (elf, &ehdr_mem);
  if (ehdr == NULL)
    {
      if (elf_kind (elf) != ELF_K_ELF)
	__libdw_seterrno (DWARF_E_NOELF);
      else
	__libdw_seterrno (DWARF_E_GETEHDR_ERROR);

      return NULL;
    }


  /* Default memory allocation size.  */
  size_t mem_default_size = sysconf (_SC_PAGESIZE) - 4 * sizeof (void *);

  /* Allocate the data structure.  */
  Dwarf *result = (Dwarf *) calloc (1, sizeof (Dwarf) + mem_default_size);
  if (result == NULL)
    {
      __libdw_seterrno (DWARF_E_NOMEM);
      return NULL;
    }

  /* Fill in some values.  */
  if ((BYTE_ORDER == LITTLE_ENDIAN && ehdr->e_ident[EI_DATA] == ELFDATA2MSB)
      || (BYTE_ORDER == BIG_ENDIAN && ehdr->e_ident[EI_DATA] == ELFDATA2LSB))
    result->other_byte_order = true;

  result->elf = elf;

  /* Initialize the memory handling.  */
  result->mem_default_size = mem_default_size;
  result->oom_handler = __libdw_oom;
  result->mem_tail = (struct libdw_memblock *) (result + 1);
  result->mem_tail->size = (result->mem_default_size
			    - offsetof (struct libdw_memblock, mem));
  result->mem_tail->remaining = result->mem_tail->size;
  result->mem_tail->prev = NULL;


  if (cmd == DWARF_C_READ || cmd == DWARF_C_RDWR)
    {
      /* If the caller provides a section group we get the DWARF
	 sections only from this setion group.  Otherwise we search
	 for the first section with the required name.  Further
	 sections with the name are ignored.  The DWARF specification
	 does not really say this is allowed.  */
      if (scngrp == NULL)
	return global_read (result, elf, ehdr);
      else
	return scngrp_read (result, elf, ehdr, scngrp);
    }
  else if (cmd == DWARF_C_WRITE)
    {
      __libdw_seterrno (DWARF_E_UNIMPL);
      free (result);
      return NULL;
    }

  __libdw_seterrno (DWARF_E_INVALID_CMD);
  free (result);
  return NULL;
}
INTDEF(dwarf_begin_elf)
