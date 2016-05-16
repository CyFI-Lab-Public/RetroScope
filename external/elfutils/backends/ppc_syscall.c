/* Linux/PPC system call ABI in DWARF register numbers.
   Copyright (C) 2008 Red Hat, Inc.
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
ppc_syscall_abi (Ebl *ebl __attribute__ ((unused)),
		 int *sp, int *pc, int *callno, int args[6])
{
  *sp = 1;
  *pc = -1;
  *callno = 0;
  args[0] = 3;
  args[1] = 4;
  args[2] = 5;
  args[3] = 6;
  args[4] = 7;
  args[5] = 8;
  return 0;
}

__typeof (ppc_syscall_abi)
ppc64_syscall_abi __attribute__ ((alias ("ppc_syscall_abi")));
