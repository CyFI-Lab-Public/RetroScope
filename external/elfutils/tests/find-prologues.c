/* Test program for dwarf_entry_breakpoints.
   Copyright (C) 2005 Red Hat, Inc.
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
#include ELFUTILS_HEADER(dwfl)
#include <dwarf.h>
#include <argp.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <locale.h>
#include <stdlib.h>
#include <error.h>
#include <string.h>
#include <fnmatch.h>


struct args
{
  Dwfl *dwfl;
  Dwarf_Die *cu;
  Dwarf_Addr dwbias;
  char **argv;
};

static int
handle_function (Dwarf_Die *func, void *arg)
{
  struct args *a = arg;

  const char *name = dwarf_diename (func);
  char **argv = a->argv;
  if (argv[0] != NULL)
    {
      bool match;
      do
	match = fnmatch (*argv, name, 0) == 0;
      while (!match && *++argv);
      if (!match)
	return 0;
    }

  if (dwarf_func_inline (func))
    return 0;

  Dwarf_Addr entrypc;
  if (dwarf_entrypc (func, &entrypc) != 0)
    error (EXIT_FAILURE, 0, "dwarf_entrypc: %s: %s",
	   dwarf_diename (func), dwarf_errmsg (-1));
  entrypc += a->dwbias;

  printf ("%-16s %#.16" PRIx64, dwarf_diename (func), entrypc);

  Dwarf_Addr *bkpts = NULL;
  int result = dwarf_entry_breakpoints (func, &bkpts);
  if (result <= 0)
    printf ("\t%s\n", dwarf_errmsg (-1));
  else
    {
      for (int i = 0; i < result; ++i)
	printf (" %#.16" PRIx64 "%s", bkpts[i] + a->dwbias,
		i == result - 1 ? "\n" : "");
      free (bkpts);
    }

  return 0;
}


int
main (int argc, char *argv[])
{
  int remaining;

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  struct args a = { .dwfl = NULL, .cu = NULL };

  (void) argp_parse (dwfl_standard_argp (), argc, argv, 0, &remaining,
		     &a.dwfl);
  assert (a.dwfl != NULL);
  a.argv = &argv[remaining];

  int result = 0;

  while ((a.cu = dwfl_nextcu (a.dwfl, a.cu, &a.dwbias)) != NULL)
    dwarf_getfuncs (a.cu, &handle_function, &a, 0);

  dwfl_end (a.dwfl);

  return result;
}
