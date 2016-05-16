/* Return child of current DIE.
   Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008 Red Hat, Inc.
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
#include <string.h>

/* Some arbitrary value not conflicting with any existing code.  */
#define INVALID 0xffffe444


unsigned char *
internal_function
__libdw_find_attr (Dwarf_Die *die, unsigned int search_name,
		   unsigned int *codep, unsigned int *formp)
{
  Dwarf *dbg = die->cu->dbg;
  const unsigned char *readp = (unsigned char *) die->addr;

  /* First we have to get the abbreviation code so that we can decode
     the data in the DIE.  */
  unsigned int abbrev_code;
  get_uleb128 (abbrev_code, readp);

  /* Find the abbreviation entry.  */
  Dwarf_Abbrev *abbrevp = die->abbrev;
  if (abbrevp == NULL)
    {
      abbrevp = __libdw_findabbrev (die->cu, abbrev_code);
      die->abbrev = abbrevp ?: DWARF_END_ABBREV;
    }
  if (unlikely (die->abbrev == DWARF_END_ABBREV))
    {
    invalid_dwarf:
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return NULL;
    }

  /* Search the name attribute.  */
  unsigned char *const endp
    = ((unsigned char *) dbg->sectiondata[IDX_debug_abbrev]->d_buf
       + dbg->sectiondata[IDX_debug_abbrev]->d_size);

  const unsigned char *attrp = die->abbrev->attrp;
  while (1)
    {
      /* Are we still in bounds?  This test needs to be refined.  */
      if (unlikely (attrp + 1 >= endp))
	goto invalid_dwarf;

      /* Get attribute name and form.

	 XXX We don't check whether this reads beyond the end of the
	 section.  */
      unsigned int attr_name;
      get_uleb128 (attr_name, attrp);
      unsigned int attr_form;
      get_uleb128 (attr_form, attrp);

      /* We can stop if we found the attribute with value zero.  */
      if (attr_name == 0 && attr_form == 0)
	break;

      /* Is this the name attribute?  */
      if (attr_name == search_name && search_name != INVALID)
	{
	  if (codep != NULL)
	    *codep = attr_name;
	  if (formp != NULL)
	    *formp = attr_form;

	  return (unsigned char *) readp;
	}

      /* Skip over the rest of this attribute (if there is any).  */
      if (attr_form != 0)
	{
	  size_t len = __libdw_form_val_len (dbg, die->cu, attr_form, readp);

	  if (unlikely (len == (size_t) -1l))
	    {
	      readp = NULL;
	      break;
	    }

	  // XXX We need better boundary checks.
	  readp += len;
	}
    }

  // XXX Do we need other values?
  if (codep != NULL)
    *codep = INVALID;
  if (formp != NULL)
    *formp = INVALID;

  return (unsigned char *) readp;
}


int
dwarf_child (die, result)
     Dwarf_Die *die;
     Dwarf_Die *result;
{
  /* Ignore previous errors.  */
  if (die == NULL)
    return -1;

  /* Skip past the last attribute.  */
  void *addr = NULL;

  /* If we already know there are no children do not search.  */
  if (die->abbrev != DWARF_END_ABBREV
      && (die->abbrev == NULL || die->abbrev->has_children))
    addr = __libdw_find_attr (die, INVALID, NULL, NULL);
  if (unlikely (die->abbrev == (Dwarf_Abbrev *) -1l))
    return -1;

  /* Make sure the DIE really has children.  */
  if (! die->abbrev->has_children)
    /* There cannot be any children.  */
    return 1;

  if (addr == NULL)
    return -1;

  /* It's kosher (just suboptimal) to have a null entry first thing (7.5.3).
     So if this starts with ULEB128 of 0 (even with silly encoding of 0),
     it is a kosher null entry and we do not really have any children.  */
  const unsigned char *code = addr;
  while (unlikely (*code == 0x80))
    ++code;
  if (unlikely (*code == '\0'))
    return 1;

  /* RESULT can be the same as DIE.  So preserve what we need.  */
  struct Dwarf_CU *cu = die->cu;

  /* Clear the entire DIE structure.  This signals we have not yet
     determined any of the information.  */
  memset (result, '\0', sizeof (Dwarf_Die));

  /* We have the address.  */
  result->addr = addr;

  /* Same CU as the parent.  */
  result->cu = cu;

  return 0;
}
INTDEF(dwarf_child)
