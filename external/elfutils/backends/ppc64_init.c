/* Initialization of PPC64 specific backend library.
   Copyright (C) 2004, 2005, 2006, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2004.

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

#define BACKEND		ppc64_
#define RELOC_PREFIX	R_PPC64_
#include "libebl_CPU.h"

/* This defines the common reloc hooks based on ppc64_reloc.def.  */
#include "common-reloc.c"


const char *
ppc64_init (elf, machine, eh, ehlen)
     Elf *elf __attribute__ ((unused));
     GElf_Half machine __attribute__ ((unused));
     Ebl *eh;
     size_t ehlen;
{
  /* Check whether the Elf_BH object has a sufficent size.  */
  if (ehlen < sizeof (Ebl))
    return NULL;

  /* We handle it.  */
  eh->name = "PowerPC 64-bit";
  ppc64_init_reloc (eh);
  HOOK (eh, reloc_simple_type);
  HOOK (eh, dynamic_tag_name);
  HOOK (eh, dynamic_tag_check);
  HOOK (eh, copy_reloc_p);
  HOOK (eh, check_special_symbol);
  HOOK (eh, bss_plt_p);
  HOOK (eh, return_value_location);
  HOOK (eh, register_info);
  HOOK (eh, syscall_abi);
  HOOK (eh, core_note);
  HOOK (eh, auxv_info);

  return MODVERSION;
}
