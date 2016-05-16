/* SPARC specific symbolic name handling.
   Copyright (C) 2002, 2003, 2005, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Jakub Jelinek <jakub@redhat.com>, 2002.

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

#define BACKEND		sparc_
#include "libebl_CPU.h"

/* Check for the simple reloc types.  */
Elf_Type
sparc_reloc_simple_type (Ebl *ebl __attribute__ ((unused)), int type)
{
  switch (type)
    {
    case R_SPARC_8:
      return ELF_T_BYTE;
    case R_SPARC_16:
    case R_SPARC_UA16:
      return ELF_T_HALF;
    case R_SPARC_32:
    case R_SPARC_UA32:
      return ELF_T_WORD;
    case R_SPARC_64:
    case R_SPARC_UA64:
      return ELF_T_XWORD;
    default:
      return ELF_T_NUM;
    }
}

/* Check whether machine flags are valid.  */
bool
sparc_machine_flag_check (GElf_Word flags)
{
  return ((flags &~ (EF_SPARCV9_MM
		     | EF_SPARC_LEDATA
		     | EF_SPARC_32PLUS
		     | EF_SPARC_SUN_US1
		     | EF_SPARC_SUN_US3)) == 0);
}

bool
sparc_check_special_section (Ebl *ebl,
			     int ndx __attribute__ ((unused)),
			     const GElf_Shdr *shdr,
			     const char *sname __attribute__ ((unused)))
{
  if ((shdr->sh_flags & (SHF_WRITE | SHF_EXECINSTR))
      == (SHF_WRITE | SHF_EXECINSTR))
    {
      /* This is ordinarily flagged, but is valid for a PLT on SPARC.

	 Look for the SHT_DYNAMIC section and the DT_PLTGOT tag in it.
	 Its d_ptr should match the .plt section's sh_addr.  */

      Elf_Scn *scn = NULL;
      while ((scn = elf_nextscn (ebl->elf, scn)) != NULL)
	{
	  GElf_Shdr scn_shdr;
	  if (likely (gelf_getshdr (scn, &scn_shdr) != NULL)
	      && scn_shdr.sh_type == SHT_DYNAMIC
	      && scn_shdr.sh_entsize != 0)
	    {
	      Elf_Data *data = elf_getdata (scn, NULL);
	      if (data != NULL)
		for (size_t i = 0; i < data->d_size / scn_shdr.sh_entsize; ++i)
		  {
		    GElf_Dyn dyn;
		    if (unlikely (gelf_getdyn (data, i, &dyn) == NULL))
		      break;
		    if (dyn.d_tag == DT_PLTGOT)
		      return dyn.d_un.d_ptr == shdr->sh_addr;
		  }
	      break;
	    }
	}
    }

  return false;
}

const char *
sparc_symbol_type_name (int type,
			char *buf __attribute__ ((unused)),
			size_t len __attribute__ ((unused)))
{
  switch (type)
    {
    case STT_SPARC_REGISTER:
      return "SPARC_REGISTER";
    }
  return NULL;
}

const char *
sparc_dynamic_tag_name (int64_t tag,
			char *buf __attribute__ ((unused)),
			size_t len __attribute__ ((unused)))
{
  switch (tag)
    {
    case DT_SPARC_REGISTER:
      return "SPARC_REGISTER";
    }
  return NULL;
}

bool
sparc_dynamic_tag_check (int64_t tag)
{
  switch (tag)
    {
    case DT_SPARC_REGISTER:
      return true;
    }
  return false;
}
