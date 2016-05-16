/* Find line information for given file/line/column triple.
   Copyright (C) 2005 Red Hat, Inc.
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

#include <assert.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>

#include "libdwP.h"

int
dwarf_getsrc_file (Dwarf *dbg, const char *fname, int lineno, int column,
		   Dwarf_Line ***srcsp, size_t *nsrcs)
{
  if (dbg == NULL)
    return -1;

  bool is_basename = strchr (fname, '/') == NULL;

  size_t max_match = *nsrcs ?: ~0u;
  size_t act_match = *nsrcs;
  size_t cur_match = 0;
  Dwarf_Line **match = *nsrcs == 0 ? NULL : *srcsp;

  Dwarf_Off off = 0;
  size_t cuhl;
  Dwarf_Off noff;

  while (INTUSE(dwarf_nextcu) (dbg, off, &noff, &cuhl, NULL, NULL, NULL) == 0)
    {
      Dwarf_Die cudie_mem;
      Dwarf_Die *cudie = INTUSE(dwarf_offdie) (dbg, off + cuhl, &cudie_mem);
      if (cudie == NULL)
	continue;

      /* Get the line number information for this file.  */
      Dwarf_Lines *lines;
      size_t nlines;
      if (INTUSE(dwarf_getsrclines) (cudie, &lines, &nlines) != 0)
	return -1;

      /* Search through all the line number records for a matching
	 file and line/column number.  If any of the numbers is zero,
	 no match is performed.  */
      unsigned int lastfile = UINT_MAX;
      bool lastmatch = false;
      for (size_t cnt = 0; cnt < nlines; ++cnt)
	{
	  Dwarf_Line *line = &lines->info[cnt];

	  if (lastfile != line->file)
	    {
	      lastfile = line->file;
	      if (lastfile >= line->files->nfiles)
		{
		  __libdw_seterrno (DWARF_E_INVALID_DWARF);
		  return -1;
		}

	      /* Match the name with the name the user provided.  */
	      const char *fname2 = line->files->info[lastfile].name;
	      if (is_basename)
		lastmatch = strcmp (basename (fname2), fname) == 0;
	      else
		lastmatch = strcmp (fname2, fname) == 0;
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
	    if (match[inner]->files == line->files
		&& match[inner]->file == line->file)
	      break;
	  if (inner < cur_match
	      && (match[inner]->line != line->line
		  || match[inner]->line != lineno
		  || (column != 0
		      && (match[inner]->column != line->column
			  || match[inner]->column != column))))
	    {
	      /* We know about this file already.  If this is a better
		 match for the line number, use it.  */
	      if (match[inner]->line >= line->line
		  && (match[inner]->line != line->line
		      || match[inner]->column >= line->column))
		/*  Use the new line.  Otherwise the old one.  */
		match[inner] = line;
	      continue;
	    }

	  if (cur_match < max_match)
	    {
	      if (cur_match == act_match)
		{
		  /* Enlarge the array for the results.  */
		  act_match += 10;
		  Dwarf_Line **newp = realloc (match,
					       act_match
					       * sizeof (Dwarf_Line *));
		  if (newp == NULL)
		    {
		      free (match);
		      __libdw_seterrno (DWARF_E_NOMEM);
		      return -1;
		    }
		  match = newp;
		}

	      match[cur_match++] = line;
	    }
	}

      /* If we managed to find as many matches as the user requested
	 already, there is no need to go on to the next CU.  */
      if (cur_match == max_match)
	break;

      off = noff;
    }

  if (cur_match > 0)
    {
      assert (*nsrcs == 0 || *srcsp == match);

      *nsrcs = cur_match;
      *srcsp = match;

      return 0;
    }

  __libdw_seterrno (DWARF_E_NO_MATCH);
  return -1;
}
