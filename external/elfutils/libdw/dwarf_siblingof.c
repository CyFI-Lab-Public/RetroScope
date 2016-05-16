/* Return sibling of given DIE.
   Copyright (C) 2003, 2004, 2005, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2003.

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
#include <string.h>


int
dwarf_siblingof (die, result)
     Dwarf_Die *die;
     Dwarf_Die *result;
{
  /* Ignore previous errors.  */
  if (die == NULL)
    return -1;

  if (result == NULL)
    return -1;

  if (result != die)
    result->addr = NULL;

  unsigned int level = 0;

  /* Copy of the current DIE.  */
  Dwarf_Die this_die = *die;
  /* Temporary attributes we create.  */
  Dwarf_Attribute sibattr;
  /* Copy of the CU in the request.  */
  sibattr.cu = this_die.cu;
  /* That's the address we start looking.  */
  unsigned char *addr = this_die.addr;
  /* End of the buffer.  */
  unsigned char *endp
    = ((unsigned char *) sibattr.cu->dbg->sectiondata[IDX_debug_info]->d_buf
       + sibattr.cu->end);

  /* Search for the beginning of the next die on this level.  We
     must not return the dies for children of the given die.  */
  do
    {
      /* Find the end of the DIE or the sibling attribute.  */
      addr = __libdw_find_attr (&this_die, DW_AT_sibling, &sibattr.code,
				&sibattr.form);
      if (sibattr.code == DW_AT_sibling)
	{
	  Dwarf_Off offset;
	  sibattr.valp = addr;
	  if (unlikely (__libdw_formref (&sibattr, &offset) != 0))
	    /* Something went wrong.  */
	    return -1;

	  /* Compute the next address.  */
	  addr = ((unsigned char *)
		  sibattr.cu->dbg->sectiondata[IDX_debug_info]->d_buf
		  + sibattr.cu->start + offset);
	}
      else if (unlikely (addr == NULL)
	       || unlikely (this_die.abbrev == DWARF_END_ABBREV))
	return -1;
      else if (this_die.abbrev->has_children)
	/* This abbreviation has children.  */
	++level;


      while (1)
	{
	  /* Make sure we are still in range.  Some producers might skip
	     the trailing NUL bytes.  */
	  if (addr >= endp)
	    return 1;

	  if (*addr != '\0')
	    break;

	  if (level-- == 0)
	    {
	      if (result != die)
		result->addr = addr;
	      /* No more sibling at all.  */
	      return 1;
	    }

	  ++addr;
	}

      /* Initialize the 'current DIE'.  */
      this_die.addr = addr;
      this_die.abbrev = NULL;
    }
  while (level > 0);

  /* Maybe we reached the end of the CU.  */
  if (addr >= endp)
    return 1;

  /* Clear the entire DIE structure.  This signals we have not yet
     determined any of the information.  */
  memset (result, '\0', sizeof (Dwarf_Die));

  /* We have the address.  */
  result->addr = addr;

  /* Same CU as the parent.  */
  result->cu = sibattr.cu;

  return 0;
}
INTDEF(dwarf_siblingof)
