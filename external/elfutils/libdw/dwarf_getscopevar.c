/* Find a named variable or parameter within given scopes.
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

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

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

#include <stdbool.h>
#include <string.h>
#include "libdwP.h"
#include <dwarf.h>


/* Find the containing CU's files.  */
static int
getfiles (Dwarf_Die *die, Dwarf_Files **files)
{
  return INTUSE(dwarf_getsrcfiles) (&CUDIE (die->cu), files, NULL);
}

/* Fetch an attribute that should have a constant integer form.  */
static int
getattr (Dwarf_Die *die, int search_name, Dwarf_Word *value)
{
  Dwarf_Attribute attr_mem;
  return INTUSE(dwarf_formudata) (INTUSE(dwarf_attr) (die, search_name,
						      &attr_mem), value);
}

/* Search SCOPES[0..NSCOPES-1] for a variable called NAME.
   Ignore the first SKIP_SHADOWS scopes that match the name.
   If MATCH_FILE is not null, accept only declaration in that source file;
   if MATCH_LINENO or MATCH_LINECOL are also nonzero, accept only declaration
   at that line and column.

   If successful, fill in *RESULT with the DIE of the variable found,
   and return N where SCOPES[N] is the scope defining the variable.
   Return -1 for errors or -2 for no matching variable found.  */

int
dwarf_getscopevar (Dwarf_Die *scopes, int nscopes,
		   const char *name, int skip_shadows,
		   const char *match_file, int match_lineno, int match_linecol,
		   Dwarf_Die *result)
{
  /* Match against the given file name.  */
  size_t match_file_len = match_file == NULL ? 0 : strlen (match_file);
  bool lastfile_matches = false;
  const char *lastfile = NULL;
  inline bool file_matches (Dwarf_Files *files, size_t idx)
    {
      if (idx >= files->nfiles)
	return false;

      const char *file = files->info[idx].name;
      if (file != lastfile)
	{
	  size_t len = strlen (file);
	  lastfile_matches = (len >= match_file_len
			      && !memcmp (match_file, file, match_file_len)
			      && (len == match_file_len
				  || file[len - match_file_len - 1] == '/'));
	}
      return lastfile_matches;
    }

  /* Start with the innermost scope and move out.  */
  for (int out = 0; out < nscopes; ++out)
    if (INTUSE(dwarf_haschildren) (&scopes[out]))
      {
	if (INTUSE(dwarf_child) (&scopes[out], result) != 0)
	  return -1;
	do
	  {
	    switch (INTUSE(dwarf_tag) (result))
	      {
	      case DW_TAG_variable:
	      case DW_TAG_formal_parameter:
		break;

	      default:
		continue;
	      }

	    /* Only get here for a variable or parameter.  Check the name.  */
	    Dwarf_Attribute attr_mem;
	    const char *diename = INTUSE(dwarf_formstring)
	      (INTUSE(dwarf_attr_integrate) (result, DW_AT_name, &attr_mem));
	    if (diename != NULL && !strcmp (name, diename))
	      {
		/* We have a matching name.  */

		if (skip_shadows > 0)
		  {
		    /* Punt this scope for the one it shadows.  */
		    --skip_shadows;
		    break;
		  }

		if (match_file != NULL)
		  {
		    /* Check its decl_file.  */

		    Dwarf_Word i;
		    Dwarf_Files *files;
		    if (getattr (result, DW_AT_decl_file, &i) != 0
			|| getfiles (&scopes[out], &files) != 0)
		      break;

		    if (!file_matches (files, i))
		      break;

		    if (match_lineno > 0
			&& (getattr (result, DW_AT_decl_line, &i) != 0
			    || (int) i != match_lineno))
		      break;
		    if (match_linecol > 0
			&& (getattr (result, DW_AT_decl_column, &i) != 0
			    || (int) i != match_linecol))
		      break;
		  }

		/* We have a winner!  */
		return out;
	      }
	  }
	while (INTUSE(dwarf_siblingof) (result, result) == 0);
      }

  return -2;
}
