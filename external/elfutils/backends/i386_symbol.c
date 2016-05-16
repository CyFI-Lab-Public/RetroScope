/* i386 specific symbolic name handling.
   Copyright (C) 2000, 2001, 2002, 2005 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

#include <assert.h>
#include <elf.h>
#include <stddef.h>
#include <string.h>

#define BACKEND i386_
#include "libebl_CPU.h"


/* Return true if the symbol type is that referencing the GOT.  */
bool
i386_gotpc_reloc_check (Elf *elf __attribute__ ((unused)), int type)
{
  return type == R_386_GOTPC;
}

/* Check for the simple reloc types.  */
Elf_Type
i386_reloc_simple_type (Ebl *ebl __attribute__ ((unused)), int type)
{
  switch (type)
    {
    case R_386_32:
      return ELF_T_SWORD;
    case R_386_16:
      return ELF_T_HALF;
    case R_386_8:
      return ELF_T_BYTE;
    default:
      return ELF_T_NUM;
    }
}

/* Check section name for being that of a debug information section.  */
bool (*generic_debugscn_p) (const char *);
bool
i386_debugscn_p (const char *name)
{
  return (generic_debugscn_p (name)
	  || strcmp (name, ".stab") == 0
	  || strcmp (name, ".stabstr") == 0);
}
