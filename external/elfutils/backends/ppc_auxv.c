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

#define BACKEND ppc_
#include "libebl_CPU.h"

int
EBLHOOK(auxv_info) (GElf_Xword a_type, const char **name, const char **format)
{
  if (a_type != AT_HWCAP)
    return 0;

  *name = "HWCAP";
  *format = "b"
    "ppcle\0" "truele\0" "3\0" "4\0" "5\0" "6\0" "7\0" "8\0" "9\0"
    "power6x\0" "dfp\0" "pa6t\0" "arch_2_05\0"
    "ic_snoop\0" "smt\0" "booke\0" "cellbe\0"
    "power5+\0" "power5\0" "power4\0" "notb\0"
    "efpdouble\0" "efpsingle\0" "spe\0" "ucache\0"
    "4xxmac\0" "mmu\0" "fpu\0" "altivec\0"
    "ppc601\0" "ppc64\0" "ppc32\0" "\0";
  return 1;
}

__typeof (ppc_auxv_info) ppc64_auxv_info
			 __attribute__ ((alias ("ppc_auxv_info")));
