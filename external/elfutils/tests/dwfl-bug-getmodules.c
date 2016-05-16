/* Test program for dwfl_getmodules bug.
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

#include <config.h>
#include ELFUTILS_HEADER(dwfl)

#include <error.h>

static const Dwfl_Callbacks callbacks =
  {
    .find_elf = dwfl_linux_proc_find_elf,
    .find_debuginfo = dwfl_standard_find_debuginfo,
  };

static int
iterate (Dwfl_Module *mod __attribute__ ((unused)),
	 void **userdata __attribute__ ((unused)),
	 const char *name __attribute__ ((unused)),
	 Dwarf_Addr base, void *arg)
{
  if (base != 0x2000)
    return DWARF_CB_OK;

  if (dwfl_addrmodule (arg, 0x2100) == NULL)
    error (1, 0, "dwfl_addrmodule: %s", dwfl_errmsg (-1));

  return DWARF_CB_ABORT;
}

int
main (void)
{
  Dwfl *dwfl = dwfl_begin (&callbacks);

  dwfl_report_module (dwfl, "m1", 0, 0x1000);
  dwfl_report_module (dwfl, "m2", 0x2000, 0x3000);
  dwfl_report_module (dwfl, "m3", 0x4000, 0x5000);

  dwfl_report_end (dwfl, NULL, NULL);

  ptrdiff_t offset = dwfl_getmodules (dwfl, &iterate, dwfl, 0);
  if (offset <= 0)
    error (1, 0, "dwfl_getmodules: %s", dwfl_errmsg (-1));

  offset = dwfl_getmodules (dwfl, &iterate, NULL, offset);
  if (offset != 0)
    error (1, 0, "dwfl_getmodules (%d): %s", (int) offset, dwfl_errmsg (-1));

  dwfl_end (dwfl);

  return 0;
}
