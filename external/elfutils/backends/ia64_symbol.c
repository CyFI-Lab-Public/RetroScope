/* IA-64 specific symbolic name handling.
   Copyright (C) 2002, 2003, 2005, 2006, 2007 Red Hat, Inc.
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

#include <elf.h>
#include <stddef.h>
#include <assert.h>

#define BACKEND		ia64_
#include "libebl_CPU.h"


const char *
ia64_segment_type_name (int segment, char *buf __attribute__ ((unused)),
			size_t len __attribute__ ((unused)))
{
  switch (segment)
    {
    case PT_IA_64_ARCHEXT:
      return "IA_64_ARCHEXT";
    case PT_IA_64_UNWIND:
      return "IA_64_UNWIND";
    case PT_IA_64_HP_OPT_ANOT:
      return "IA_64_HP_OPT_ANOT";
    case PT_IA_64_HP_HSL_ANOT:
      return "IA_64_HP_HSL_ANOT";
    case PT_IA_64_HP_STACK:
      return "IA_64_HP_STACK";
    default:
      break;
    }
  return NULL;
}

const char *
ia64_dynamic_tag_name (int64_t tag, char *buf __attribute__ ((unused)),
		       size_t len __attribute__ ((unused)))
{
  switch (tag)
    {
    case DT_IA_64_PLT_RESERVE:
      return "IA_64_PLT_RESERVE";
    default:
      break;
    }
  return NULL;
}

/* Check dynamic tag.  */
bool
ia64_dynamic_tag_check (int64_t tag)
{
  return tag == DT_IA_64_PLT_RESERVE;
}

/* Check whether machine flags are valid.  */
bool
ia64_machine_flag_check (GElf_Word flags)
{
  return ((flags &~ EF_IA_64_ABI64) == 0);
}

/* Check whether SHF_MASKPROC flags are valid.  */
bool
ia64_machine_section_flag_check (GElf_Xword sh_flags)
{
  return (sh_flags &~ (SHF_IA_64_SHORT | SHF_IA_64_NORECOV)) == 0;
}

/* Return symbolic representation of section type.  */
const char *
ia64_section_type_name (int type,
			char *buf __attribute__ ((unused)),
			size_t len __attribute__ ((unused)))
{
  switch (type)
    {
    case SHT_IA_64_EXT:
      return "SHT_IA_64_EXT";
    case SHT_IA_64_UNWIND:
      return "SHT_IA_64_UNWIND";
    }

  return NULL;
}

/* Check for the simple reloc types.  */
Elf_Type
ia64_reloc_simple_type (Ebl *ebl, int type)
{
  switch (type)
    {
      /* The SECREL types when used with non-allocated sections
	 like .debug_* are the same as direct absolute relocs
	 applied to those sections, since a 0 section address is assumed.
	 So we treat them the same here.  */

    case R_IA64_SECREL32MSB:
    case R_IA64_DIR32MSB:
      if (ebl->data == ELFDATA2MSB)
	return ELF_T_WORD;
      break;
    case R_IA64_SECREL32LSB:
    case R_IA64_DIR32LSB:
      if (ebl->data == ELFDATA2LSB)
	return ELF_T_WORD;
      break;
    case R_IA64_DIR64MSB:
    case R_IA64_SECREL64MSB:
      if (ebl->data == ELFDATA2MSB)
	return ELF_T_XWORD;
      break;
    case R_IA64_SECREL64LSB:
    case R_IA64_DIR64LSB:
      if (ebl->data == ELFDATA2LSB)
	return ELF_T_XWORD;
      break;
    }

  return ELF_T_NUM;
}
