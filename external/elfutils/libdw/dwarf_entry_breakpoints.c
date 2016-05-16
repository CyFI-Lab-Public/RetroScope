/* Find entry breakpoint locations for a function.
   Copyright (C) 2005, 2008 Red Hat, Inc.
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
#include "libdwP.h"
#include <dwarf.h>
#include <stdlib.h>


int
dwarf_entry_breakpoints (die, bkpts)
     Dwarf_Die *die;
     Dwarf_Addr **bkpts;
{
  int nbkpts = 0;
  *bkpts = NULL;

  /* Add one breakpoint location to the result vector.  */
  inline int add_bkpt (Dwarf_Addr pc)
    {
      Dwarf_Addr *newlist = realloc (*bkpts, ++nbkpts * sizeof newlist[0]);
      if (newlist == NULL)
	{
	  free (*bkpts);
	  *bkpts = NULL;
	  __libdw_seterrno (DWARF_E_NOMEM);
	  return -1;
	}
      newlist[nbkpts - 1] = pc;
      *bkpts = newlist;
      return nbkpts;
    }

  /* Fallback result, break at the entrypc/lowpc value.  */
  inline int entrypc_bkpt (void)
    {
      Dwarf_Addr pc;
      return INTUSE(dwarf_entrypc) (die, &pc) < 0 ? -1 : add_bkpt (pc);
    }

  /* Fetch the CU's line records to look for this DIE's addresses.  */
  Dwarf_Die cudie = CUDIE (die->cu);
  Dwarf_Lines *lines;
  size_t nlines;
  if (INTUSE(dwarf_getsrclines) (&cudie, &lines, &nlines) < 0)
    {
      int error = INTUSE (dwarf_errno) ();
      if (error == DWARF_E_NO_DEBUG_LINE)
	return entrypc_bkpt ();
      __libdw_seterrno (error);
      return -1;
    }

  /* Search a contiguous PC range for prologue-end markers.
     If DWARF, look for proper markers.
     Failing that, if ADHOC, look for the ad hoc convention.  */
  inline int search_range (Dwarf_Addr low, Dwarf_Addr high,
			   bool dwarf, bool adhoc)
    {
      size_t l = 0, u = nlines;
      while (l < u)
	{
	  size_t idx = (l + u) / 2;
	  if (lines->info[idx].addr < low)
	    l = idx + 1;
	  else if (lines->info[idx].addr > low)
	    u = idx;
	  else if (lines->info[idx].end_sequence)
	    l = idx + 1;
	  else
	    {
	      l = idx;
	      break;
	    }
	}
      if (l < u)
	{
	  if (dwarf)
	    for (size_t i = l; i < u && lines->info[i].addr < high; ++i)
	      if (lines->info[i].prologue_end
		  && add_bkpt (lines->info[i].addr) < 0)
		return -1;
	  if (adhoc && nbkpts == 0)
	    while (++l < nlines && lines->info[l].addr < high)
	      if (!lines->info[l].end_sequence)
		return add_bkpt (lines->info[l].addr);
	  return nbkpts;
	}
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return -1;
    }

  /* Search each contiguous address range for DWARF prologue_end markers.  */

  Dwarf_Addr base;
  Dwarf_Addr begin;
  Dwarf_Addr end;
  ptrdiff_t offset = INTUSE(dwarf_ranges) (die, 0, &base, &begin, &end);
  if (offset < 0)
    return -1;

  /* Most often there is a single contiguous PC range for the DIE.  */
  if (offset == 1)
    return search_range (begin, end, true, true) ?: entrypc_bkpt ();

  Dwarf_Addr lowpc = (Dwarf_Addr) -1l;
  Dwarf_Addr highpc = (Dwarf_Addr) -1l;
  while (offset > 0)
    {
      /* We have an address range entry.  */
      if (search_range (begin, end, true, false) < 0)
	return -1;

      if (begin < lowpc)
	{
	  lowpc = begin;
	  highpc = end;
	}

      offset = INTUSE(dwarf_ranges) (die, offset, &base, &begin, &end);
    }

  /* If we didn't find any proper DWARF markers, then look in the
     lowest-addressed range for an ad hoc marker.  Failing that,
     fall back to just using the entrypc value.  */
  return (nbkpts
	  ?: (lowpc == (Dwarf_Addr) -1l ? 0
	      : search_range (lowpc, highpc, false, true))
	  ?: entrypc_bkpt ());
}
