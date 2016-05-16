/* Copyright (C) 2005, 2006 Red Hat, Inc.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <error.h>
#include <locale.h>
#include <argp.h>
#include <assert.h>
#include ELFUTILS_HEADER(dwfl)
#include <dwarf.h>


static const char *
dwarf_encoding_string (unsigned int code)
{
  static const char *known[] =
    {
      [DW_ATE_void] = "void",
      [DW_ATE_address] = "address",
      [DW_ATE_boolean] = "boolean",
      [DW_ATE_complex_float] = "complex_float",
      [DW_ATE_float] = "float",
      [DW_ATE_signed] = "signed",
      [DW_ATE_signed_char] = "signed_char",
      [DW_ATE_unsigned] = "unsigned",
      [DW_ATE_unsigned_char] = "unsigned_char",
      [DW_ATE_imaginary_float] = "imaginary_float",
      [DW_ATE_packed_decimal] = "packed_decimal",
      [DW_ATE_numeric_string] = "numeric_string",
      [DW_ATE_edited] = "edited",
      [DW_ATE_signed_fixed] = "signed_fixed",
      [DW_ATE_unsigned_fixed] = "unsigned_fixed",
      [DW_ATE_decimal_float] = "decimal_float",
    };

  if (code < sizeof (known) / sizeof (known[0]))
    return known[code];

  if (code >= DW_ATE_lo_user && code <= DW_ATE_hi_user)
    {
      static char buf[30];
      snprintf (buf, sizeof (buf), "lo_user+%u", code - DW_ATE_lo_user);
      return buf;
    }

  return "???";
}

static int
first_module (Dwfl_Module *mod,
	      void **userdatap __attribute__ ((unused)),
	      const char *name __attribute__ ((unused)),
	      Dwarf_Addr low_addr __attribute__ ((unused)),
	      void *arg)
{
  Dwarf_Addr bias;
  if (dwfl_module_getelf (mod, &bias) == NULL) /* Not really a module.  */
    return DWARF_CB_OK;

  *(Dwfl_Module **) arg = mod;
  return DWARF_CB_ABORT;
}


struct state
{
  struct reginfo *info;
  int nregs;
};

struct reginfo
{
  const char *set, *pfx;
  int regno;
  int bits;
  int type;
  char name[32];
};

static int
compare (const void *r1, const void *r2)
{
  const struct reginfo *a = r1, *b = r2;
  if (a->set == b->set)
    return a->regno - b->regno;
  if (a->set == NULL)
    return 1;
  if (b->set == NULL)
    return -1;
  if (!strcmp (a->set, "integer"))
    return -1;
  if (!strcmp (b->set, "integer"))
    return 1;
  return strcmp (a->set, b->set);
}

static int
one_register (void *arg,
	      int regno,
	      const char *setname,
	      const char *prefix,
	      const char *regname,
	      int bits, int type)
{
  struct state *state = arg;

  if (regno >= state->nregs)
    {
      state->info = realloc (state->info, (regno + 1) * sizeof state->info[0]);
      memset (&state->info[state->nregs], 0,
	      ((void *) &state->info[regno + 1]
	       - (void *) &state->info[state->nregs]));
      state->nregs = regno + 1;
    }

  state->info[regno].regno = regno;
  state->info[regno].set = setname;
  state->info[regno].pfx = prefix;
  state->info[regno].bits = bits;
  state->info[regno].type = type;
  assert (strlen (regname) < sizeof state->info[regno].name);
  strcpy (state->info[regno].name, regname);

  return DWARF_CB_OK;
}


static int
match_register (void *arg,
		int regno,
		const char *setname,
		const char *prefix,
		const char *regname,
		int bits, int type)
{
  if (regno == *(int *) arg)
    printf ("%5d => %s register %s%s %s %d bits\n",
	    regno, setname, prefix, regname,
	    dwarf_encoding_string (type), bits);

  return DWARF_CB_ABORT;
}


int
main (int argc, char **argv)
{
  int remaining;

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  Dwfl *dwfl = NULL;
  (void) argp_parse (dwfl_standard_argp (), argc, argv, 0, &remaining, &dwfl);
  assert (dwfl != NULL);

  Dwfl_Module *mod = NULL;
  if (dwfl_getmodules (dwfl, &first_module, &mod, 0) < 0)
    error (EXIT_FAILURE, 0, "dwfl_getmodules: %s", dwfl_errmsg (-1));

  if (remaining == argc)
    {
      struct state state = { NULL, 0 };
      int result = dwfl_module_register_names (mod, &one_register, &state);
      if (result != 0 || state.nregs == 0)
	error (EXIT_FAILURE, 0, "dwfl_module_register_names: %s",
	       result ? dwfl_errmsg (-1) : "no backend registers known");

      qsort (state.info, state.nregs, sizeof state.info[0], &compare);

      const char *set = NULL;
      for (int i = 0; i < state.nregs; ++i)
	if (state.info[i].set != NULL)
	  {
	    if (set != state.info[i].set)
	      printf ("%s registers:\n", state.info[i].set);
	    set = state.info[i].set;

	    printf ("\t%3d: %s%s (%s), %s %d bits\n",
		    state.info[i].regno,
		    state.info[i].pfx ?: "", state.info[i].name,
		    state.info[i].name,
		    dwarf_encoding_string (state.info[i].type),
		    state.info[i].bits);
	  }
    }
  else
    do
      {
	const char *arg = argv[remaining++];
	int regno = atoi (arg);
	int result = dwfl_module_register_names (mod, &match_register, &regno);
	if (result != DWARF_CB_ABORT)
	  error (EXIT_FAILURE, 0, "dwfl_module_register_names: %s",
		 result ? dwfl_errmsg (-1) : "no backend registers known");
      }
    while (remaining < argc);

  dwfl_end (dwfl);

  return 0;
}
