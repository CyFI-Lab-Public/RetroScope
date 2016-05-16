/* Initialization of SPARC specific backend library.
   Copyright (C) 2002, 2005, 2006, 2007, 2008 Red Hat, Inc.
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

#define BACKEND		sparc_
#define RELOC_PREFIX	R_SPARC_
#include "libebl_CPU.h"

/* This defines the common reloc hooks based on sparc_reloc.def.  */
#include "common-reloc.c"

extern __typeof (EBLHOOK (core_note)) sparc64_core_note attribute_hidden;

const char *
sparc_init (elf, machine, eh, ehlen)
     Elf *elf __attribute__ ((unused));
     GElf_Half machine __attribute__ ((unused));
     Ebl *eh;
     size_t ehlen;
{
  /* Check whether the Elf_BH object has a sufficent size.  */
  if (ehlen < sizeof (Ebl))
    return NULL;

  /* We handle it.  */
  if (machine == EM_SPARCV9)
    eh->name = "SPARC v9";
  else if (machine == EM_SPARC32PLUS)
    eh->name = "SPARC v8+";
  else
    eh->name = "SPARC";
  sparc_init_reloc (eh);
  HOOK (eh, reloc_simple_type);
  HOOK (eh, machine_flag_check);
  HOOK (eh, check_special_section);
  HOOK (eh, symbol_type_name);
  HOOK (eh, dynamic_tag_name);
  HOOK (eh, dynamic_tag_check);
  if (eh->class == ELFCLASS64)
    eh->core_note = sparc64_core_note;
  else
    HOOK (eh, core_note);
  HOOK (eh, auxv_info);
  HOOK (eh, register_info);
  HOOK (eh, return_value_location);

  return MODVERSION;
}
