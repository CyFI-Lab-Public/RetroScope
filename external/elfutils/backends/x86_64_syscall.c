/* Linux/x86-64 system call ABI in DWARF register numbers.
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

#define BACKEND x86_64_
#include "libebl_CPU.h"

int
x86_64_syscall_abi (Ebl *ebl __attribute__ ((unused)),
		    int *sp, int *pc, int *callno, int args[6])
{
  *sp = 7;			/* %rsp */
  *pc = 16;			/* %rip */
  *callno = 0;			/* %rax */
  args[0] = 5;			/* %rdi */
  args[1] = 4;			/* %rsi */
  args[2] = 1;			/* %rdx */
  args[3] = 10;			/* %r10 */
  args[4] = 8;			/* %r8 */
  args[5] = 9;			/* %r9 */
  return 0;
}
