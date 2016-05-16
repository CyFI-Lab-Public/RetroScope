/* Test program for libdwfl basic module tracking, relocation.
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

#include <config.h>
#include <assert.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <error.h>
#include <locale.h>
#include ELFUTILS_HEADER(dwfl)


static const Dwfl_Callbacks offline_callbacks =
  {
    .find_debuginfo = INTUSE(dwfl_standard_find_debuginfo),
    .section_address = INTUSE(dwfl_offline_section_address),
  };


int
main (void)
{
  /* We use no threads here which can interfere with handling a stream.  */
  (void) __fsetlocking (stdout, FSETLOCKING_BYCALLER);

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  Dwfl *dwfl = dwfl_begin (&offline_callbacks);
  assert (dwfl != NULL);

  Dwfl_Module *high = dwfl_report_module (dwfl, "high",
					  UINT64_C (0xffffffff00010000),
					  UINT64_C (0xffffffff00020000));
  assert (high);
  Dwfl_Module *low = dwfl_report_module (dwfl, "low",
					 UINT64_C (0x00010000),
					 UINT64_C (0x00020000));
  assert (low);
  Dwfl_Module *middle = dwfl_report_module (dwfl, "middle",
					    UINT64_C (0xffff00010000),
					    UINT64_C (0xffff00020000));
  assert (middle);

  int ret = dwfl_report_end (dwfl, NULL, NULL);
  assert (ret == 0);

  Dwfl_Module *mod = dwfl_addrmodule (dwfl, UINT64_C (0xffffffff00010123));
  assert (mod == high);
  mod = dwfl_addrmodule (dwfl, UINT64_C (0x00010123));
  assert (mod == low);
  mod = dwfl_addrmodule (dwfl, UINT64_C (0xffff00010123));
  assert (mod == middle);

  dwfl_end (dwfl);

  return 0;
}
