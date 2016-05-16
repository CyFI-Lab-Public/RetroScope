/* Find matching source locations in a module.
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

#include "libdwflP.h"
#include "../libdw/libdwP.h"

int
dwfl_module_getsrc_file (Dwfl_Module *mod,
			 const char *fname, int lineno, int column,
			 Dwfl_Line ***srcsp, size_t *nsrcs)
{
  if (mod == NULL)
    return -1;

  if (mod->dw == NULL)
    {
      Dwarf_Addr bias;
      if (INTUSE(dwfl_module_getdwarf) (mod, &bias) == NULL)
	return -1;
    }

  bool is_basename = strchr (fname, '/') == NULL;

  size_t max_match = *nsrcs ?: ~0u;
  size_t act_match = *nsrcs;
  size_t cur_match = 0;
  Dwfl_Line **match = *nsrcs == 0 ? NULL : *srcsp;

  struct dwfl_cu *cu = NULL;
  Dwfl_Error error;
  while ((error = __libdwfl_nextcu (mod, cu, &cu)) == DWFL_E_NOERROR
	 && cu != NULL
	 && (error = __libdwfl_cu_getsrclines (cu)) == DWFL_E_NOERROR)
    {
      inline const char *INTUSE(dwarf_line_file) (const Dwarf_Line *line)
	{
	  return line->files->info[line->file].name;
	}
      inline Dwarf_Line *dwfl_line (const Dwfl_Line *line)
	{
	  return &dwfl_linecu (line)->die.cu->lines->info[line->idx];
	}
      inline const char *dwfl_line_file (const Dwfl_Line *line)
	{
	  return INTUSE(dwarf_line_file) (dwfl_line (line));
	}

      /* Search through all the line number records for a matching
	 file and line/column number.  If any of the numbers is zero,
	 no match is performed.  */
      const char *lastfile = NULL;
      bool lastmatch = false;
      for (size_t cnt = 0; cnt < cu->die.cu->lines->nlines; ++cnt)
	{
	  Dwarf_Line *line = &cu->die.cu->lines->info[cnt];

	  if (unlikely (line->file >= line->files->nfiles))
	    {
	      __libdwfl_seterrno (DWFL_E (LIBDW, DWARF_E_INVALID_DWARF));
	      return -1;
	    }
	  else
	    {
	      const char *file = INTUSE(dwarf_line_file) (line);
	      if (file != lastfile)
		{
		  /* Match the name with the name the user provided.  */
		  lastfile = file;
		  lastmatch = !strcmp (is_basename ? basename (file) : file,
				       fname);
		}
	    }
	  if (!lastmatch)
	    continue;

	  /* See whether line and possibly column match.  */
	  if (lineno != 0
	      && (lineno > line->line
		  || (column != 0 && column > line->column)))
	    /* Cannot match.  */
	    continue;

	  /* Determine whether this is the best match so far.  */
	  size_t inner;
	  for (inner = 0; inner < cur_match; ++inner)
	    if (dwfl_line_file (match[inner])
		== INTUSE(dwarf_line_file) (line))
	      break;
	  if (inner < cur_match
	      && (dwfl_line (match[inner])->line != line->line
		  || dwfl_line (match[inner])->line != lineno
		  || (column != 0
		      && (dwfl_line (match[inner])->column != line->column
			  || dwfl_line (match[inner])->column != column))))
	    {
	      /* We know about this file already.  If this is a better
		 match for the line number, use it.  */
	      if (dwfl_line (match[inner])->line >= line->line
		  && (dwfl_line (match[inner])->line != line->line
		      || dwfl_line (match[inner])->column >= line->column))
		/* Use the new line.  Otherwise the old one.  */
		match[inner] = &cu->lines->idx[cnt];
	      continue;
	    }

	  if (cur_match < max_match)
	    {
	      if (cur_match == act_match)
		{
		  /* Enlarge the array for the results.  */
		  act_match += 10;
		  Dwfl_Line **newp = realloc (match,
					      act_match
					      * sizeof (Dwfl_Line *));
		  if (newp == NULL)
		    {
		      free (match);
		      __libdwfl_seterrno (DWFL_E_NOMEM);
		      return -1;
		    }
		  match = newp;
		}

	      match[cur_match++] = &cu->lines->idx[cnt];
	    }
	}
    }

  if (cur_match > 0)
    {
      assert (*nsrcs == 0 || *srcsp == match);

      *nsrcs = cur_match;
      *srcsp = match;

      return 0;
    }

  __libdwfl_seterrno (DWFL_E_NO_MATCH);
  return -1;
}
