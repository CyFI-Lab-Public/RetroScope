/* i386 specific auxv handling.
   Copyright (C) 2007 Red Hat, Inc.
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

#define BACKEND i386_
#include "libebl_CPU.h"

int
EBLHOOK(auxv_info) (GElf_Xword a_type, const char **name, const char **format)
{
  if (a_type != AT_HWCAP)
    return 0;

  *name = "HWCAP";
  *format = "b"
    "fpu\0" "vme\0" "de\0" "pse\0" "tsc\0" "msr\0" "pae\0" "mce\0"
    "cx8\0" "apic\0" "10\0" "sep\0" "mtrr\0" "pge\0" "mca\0" "cmov\0"
    "pat\0" "pse36\0" "pn\0" "clflush\0" "20\0" "dts\0" "acpi\0" "mmx\0"
    "fxsr\0" "sse\0" "sse2\0" "ss\0" "ht\0" "tm\0" "ia64\0" "pbe\0" "\0";
  return 1;
}

__typeof (i386_auxv_info) x86_64_auxv_info
			  __attribute__ ((alias ("i386_auxv_info")));
