/* Locate source files and line information for given addresses
   Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2005.

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

#include <argp.h>
#include <assert.h>
#include <errno.h>
#include <error.h>
#include <fcntl.h>
#include <inttypes.h>
#include <libdwfl.h>
#include <dwarf.h>
#include <libintl.h>
#include <locale.h>
#include <mcheck.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>


/* Name and version of program.  */
static void print_version (FILE *stream, struct argp_state *state);
void (*argp_program_version_hook) (FILE *, struct argp_state *) = print_version;

/* Bug report address.  */
const char *argp_program_bug_address = PACKAGE_BUGREPORT;


/* Values for the parameters which have no short form.  */
#define OPT_DEMANGLER 0x100

/* Definitions of arguments for argp functions.  */
static const struct argp_option options[] =
{
  { NULL, 0, NULL, 0, N_("Output selection options:"), 2 },
  { "basenames", 's', NULL, 0, N_("Show only base names of source files"), 0 },
  { "absolute", 'A', NULL, 0,
    N_("Show absolute file names using compilation directory"), 0 },
  { "functions", 'f', NULL, 0, N_("Also show function names"), 0 },
  { "symbols", 'S', NULL, 0, N_("Also show symbol or section names"), 0 },

  { NULL, 0, NULL, 0, N_("Miscellaneous:"), 0 },
  /* Unsupported options.  */
  { "target", 'b', "ARG", OPTION_HIDDEN, NULL, 0 },
  { "demangle", 'C', "ARG", OPTION_HIDDEN | OPTION_ARG_OPTIONAL, NULL, 0 },
  { "demangler", OPT_DEMANGLER, "ARG", OPTION_HIDDEN, NULL, 0 },
  { NULL, 0, NULL, 0, NULL, 0 }
};

/* Short description of program.  */
static const char doc[] = N_("\
Locate source files and line information for ADDRs (in a.out by default).");

/* Strings for arguments in help texts.  */
static const char args_doc[] = N_("[ADDR...]");

/* Prototype for option handler.  */
static error_t parse_opt (int key, char *arg, struct argp_state *state);

static struct argp_child argp_children[2]; /* [0] is set in main.  */

/* Data structure to communicate with argp functions.  */
static const struct argp argp =
{
  options, parse_opt, args_doc, doc, argp_children, NULL, NULL
};


/* Handle ADDR.  */
static int handle_address (const char *addr, Dwfl *dwfl);


/* True if only base names of files should be shown.  */
static bool only_basenames;

/* True if absolute file names based on DW_AT_comp_dir should be shown.  */
static bool use_comp_dir;

/* True if function names should be shown.  */
static bool show_functions;

/* True if ELF symbol or section info should be shown.  */
static bool show_symbols;


int
main (int argc, char *argv[])
{
  int remaining;
  int result = 0;

  /* Make memory leak detection possible.  */
  mtrace ();

  /* We use no threads here which can interfere with handling a stream.  */
  (void) __fsetlocking (stdout, FSETLOCKING_BYCALLER);

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  /* Make sure the message catalog can be found.  */
  (void) bindtextdomain (PACKAGE_TARNAME, LOCALEDIR);

  /* Initialize the message catalog.  */
  (void) textdomain (PACKAGE_TARNAME);

  /* Parse and process arguments.  This includes opening the modules.  */
  argp_children[0].argp = dwfl_standard_argp ();
  argp_children[0].group = 1;
  Dwfl *dwfl = NULL;
  (void) argp_parse (&argp, argc, argv, 0, &remaining, &dwfl);
  assert (dwfl != NULL);

  /* Now handle the addresses.  In case none are given on the command
     line, read from stdin.  */
  if (remaining == argc)
    {
      /* We use no threads here which can interfere with handling a stream.  */
      (void) __fsetlocking (stdin, FSETLOCKING_BYCALLER);

      char *buf = NULL;
      size_t len = 0;
      while (!feof_unlocked (stdin))
	{
	  if (getline (&buf, &len, stdin) < 0)
	    break;

	  result = handle_address (buf, dwfl);
	}

      free (buf);
    }
  else
    {
      do
	result = handle_address (argv[remaining], dwfl);
      while (++remaining < argc);
    }

  return result;
}


/* Print the version information.  */
static void
print_version (FILE *stream, struct argp_state *state __attribute__ ((unused)))
{
  fprintf (stream, "addr2line (%s) %s\n", PACKAGE_NAME, PACKAGE_VERSION);
  fprintf (stream, gettext ("\
Copyright (C) %s Red Hat, Inc.\n\
This is free software; see the source for copying conditions.  There is NO\n\
warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n\
"), "2008");
  fprintf (stream, gettext ("Written by %s.\n"), "Ulrich Drepper");
}


/* Handle program arguments.  */
static error_t
parse_opt (int key, char *arg __attribute__ ((unused)),
	   struct argp_state *state)
{
  switch (key)
    {
    case ARGP_KEY_INIT:
      state->child_inputs[0] = state->input;
      break;

    case 'b':
    case 'C':
    case OPT_DEMANGLER:
      /* Ignored for compatibility.  */
      break;

    case 's':
      only_basenames = true;
      break;

    case 'A':
      use_comp_dir = true;
      break;

    case 'f':
      show_functions = true;
      break;

    case 'S':
      show_symbols = true;
      break;

    default:
      return ARGP_ERR_UNKNOWN;
    }
  return 0;
}


static bool
print_dwarf_function (Dwfl_Module *mod, Dwarf_Addr addr)
{
  Dwarf_Addr bias = 0;
  Dwarf_Die *cudie = dwfl_module_addrdie (mod, addr, &bias);

  Dwarf_Die *scopes;
  int nscopes = dwarf_getscopes (cudie, addr - bias, &scopes);
  if (nscopes <= 0)
    return false;

  for (int i = 0; i < nscopes; ++i)
    switch (dwarf_tag (&scopes[i]))
      {
      case DW_TAG_subprogram:
	{
	  const char *name = dwarf_diename (&scopes[i]);
	  if (name == NULL)
	    return false;
	  puts (name);
	  return true;
	}

      case DW_TAG_inlined_subroutine:
	{
	  const char *name = dwarf_diename (&scopes[i]);
	  if (name == NULL)
	    return false;
	  printf ("%s inlined", name);

	  Dwarf_Files *files;
	  if (dwarf_getsrcfiles (cudie, &files, NULL) == 0)
	    {
	      Dwarf_Attribute attr_mem;
	      Dwarf_Word val;
	      if (dwarf_formudata (dwarf_attr (&scopes[i],
					       DW_AT_call_file,
					       &attr_mem), &val) == 0)
		{
		  const char *file = dwarf_filesrc (files, val, NULL, NULL);
		  unsigned int lineno = 0;
		  unsigned int colno = 0;
		  if (dwarf_formudata (dwarf_attr (&scopes[i],
						   DW_AT_call_line,
						   &attr_mem), &val) == 0)
		    lineno = val;
		  if (dwarf_formudata (dwarf_attr (&scopes[i],
						   DW_AT_call_column,
						   &attr_mem), &val) == 0)
		    colno = val;
		  if (lineno == 0)
		    {
		      if (file != NULL)
			printf (" from %s", file);
		    }
		  else if (colno == 0)
		    printf (" at %s:%u", file, lineno);
		  else
		    printf (" at %s:%u:%u", file, lineno, colno);
		}
	    }
	  printf (" in ");
	  continue;
	}
      }

  return false;
}

static void
print_addrsym (Dwfl_Module *mod, GElf_Addr addr)
{
  GElf_Sym s;
  GElf_Word shndx;
  const char *name = dwfl_module_addrsym (mod, addr, &s, &shndx);
  if (name == NULL)
    {
      /* No symbol name.  Get a section name instead.  */
      int i = dwfl_module_relocate_address (mod, &addr);
      if (i >= 0)
	name = dwfl_module_relocation_info (mod, i, NULL);
      if (name == NULL)
	puts ("??");
      else
	printf ("(%s)+%#" PRIx64 "\n", name, addr);
    }
  else if (addr == s.st_value)
    puts (name);
  else
    printf ("%s+%#" PRIx64 "\n", name, addr - s.st_value);
}

static int
see_one_module (Dwfl_Module *mod,
		void **userdata __attribute__ ((unused)),
		const char *name __attribute__ ((unused)),
		Dwarf_Addr start __attribute__ ((unused)),
		void *arg)
{
  Dwfl_Module **result = arg;
  if (*result != NULL)
    return DWARF_CB_ABORT;
  *result = mod;
  return DWARF_CB_OK;
}

static int
find_symbol (Dwfl_Module *mod,
	     void **userdata __attribute__ ((unused)),
	     const char *name __attribute__ ((unused)),
	     Dwarf_Addr start __attribute__ ((unused)),
	     void *arg)
{
  const char *looking_for = ((void **) arg)[0];
  GElf_Sym *symbol = ((void **) arg)[1];

  int n = dwfl_module_getsymtab (mod);
  for (int i = 1; i < n; ++i)
    {
      const char *symbol_name = dwfl_module_getsym (mod, i, symbol, NULL);
      if (symbol_name == NULL)
	continue;
      switch (GELF_ST_TYPE (symbol->st_info))
	{
	case STT_SECTION:
	case STT_FILE:
	case STT_TLS:
	  break;
	default:
	  if (!strcmp (symbol_name, looking_for))
	    {
	      ((void **) arg)[0] = NULL;
	      return DWARF_CB_ABORT;
	    }
	}
    }

  return DWARF_CB_OK;
}

static int
handle_address (const char *string, Dwfl *dwfl)
{
  char *endp;
  uintmax_t addr = strtoumax (string, &endp, 0);
  if (endp == string)
    {
      bool parsed = false;
      int n;
      char *name = NULL;
      if (sscanf (string, "(%m[^)])%" PRIiMAX "%n", &name, &addr, &n) == 2
	  && string[n] == '\0')
	{
	  /* It was (section)+offset.  This makes sense if there is
	     only one module to look in for a section.  */
	  Dwfl_Module *mod = NULL;
	  if (dwfl_getmodules (dwfl, &see_one_module, &mod, 0) != 0
	      || mod == NULL)
	    error (EXIT_FAILURE, 0, gettext ("Section syntax requires"
					     " exactly one module"));

	  int nscn = dwfl_module_relocations (mod);
	  for (int i = 0; i < nscn; ++i)
	    {
	      GElf_Word shndx;
	      const char *scn = dwfl_module_relocation_info (mod, i, &shndx);
	      if (unlikely (scn == NULL))
		break;
	      if (!strcmp (scn, name))
		{
		  /* Found the section.  */
		  GElf_Shdr shdr_mem;
		  GElf_Addr shdr_bias;
		  GElf_Shdr *shdr = gelf_getshdr
		    (elf_getscn (dwfl_module_getelf (mod, &shdr_bias), shndx),
		     &shdr_mem);
		  if (unlikely (shdr == NULL))
		    break;

		  if (addr >= shdr->sh_size)
		    error (0, 0,
			   gettext ("offset %#" PRIxMAX " lies outside"
				    " section '%s'"),
			   addr, scn);

		  addr += shdr->sh_addr + shdr_bias;
		  parsed = true;
		  break;
		}
	    }
	}
      else if (sscanf (string, "%m[^-+]%" PRIiMAX "%n", &name, &addr, &n) == 2
	       && string[n] == '\0')
	{
	  /* It was symbol+offset.  */
	  GElf_Sym sym;
	  void *arg[2] = { name, &sym };
	  (void) dwfl_getmodules (dwfl, &find_symbol, arg, 0);
	  if (arg[0] != NULL)
	    error (0, 0, gettext ("cannot find symbol '%s'"), name);
	  else
	    {
	      if (sym.st_size != 0 && addr >= sym.st_size)
		error (0, 0,
		       gettext ("offset %#" PRIxMAX " lies outside"
				" contents of '%s'"),
		       addr, name);
	      addr += sym.st_value;
	      parsed = true;
	    }
	}

      free (name);
      if (!parsed)
	return 1;
    }

  Dwfl_Module *mod = dwfl_addrmodule (dwfl, addr);

  if (show_functions)
    {
      /* First determine the function name.  Use the DWARF information if
	 possible.  */
      if (! print_dwarf_function (mod, addr) && !show_symbols)
	puts (dwfl_module_addrname (mod, addr) ?: "??");
    }

  if (show_symbols)
    print_addrsym (mod, addr);

  Dwfl_Line *line = dwfl_module_getsrc (mod, addr);

  const char *src;
  int lineno, linecol;
  if (line != NULL && (src = dwfl_lineinfo (line, &addr, &lineno, &linecol,
					    NULL, NULL)) != NULL)
    {
      const char *comp_dir = "";
      const char *comp_dir_sep = "";

      if (only_basenames)
	src = basename (src);
      else if (use_comp_dir && src[0] != '/')
	{
	  comp_dir = dwfl_line_comp_dir (line);
	  if (comp_dir != NULL)
	    comp_dir_sep = "/";
	}

      if (linecol != 0)
	printf ("%s%s%s:%d:%d\n",
		comp_dir, comp_dir_sep, src, lineno, linecol);
      else
	printf ("%s%s%s:%d\n",
		comp_dir, comp_dir_sep, src, lineno);
    }
  else
    puts ("??:0");

  return 0;
}


#include "debugpred.h"
