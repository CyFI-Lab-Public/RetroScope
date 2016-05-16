/* Return tag of given DIE.
   Copyright (C) 2003, 2004, 2005, 2006, 2008 Red Hat, Inc.
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


Dwarf_Abbrev *
internal_function
__libdw_findabbrev (struct Dwarf_CU *cu, unsigned int code)
{
  Dwarf_Abbrev *abb;

  /* See whether the entry is already in the hash table.  */
  abb = Dwarf_Abbrev_Hash_find (&cu->abbrev_hash, code, NULL);
  if (abb == NULL)
    while (cu->last_abbrev_offset != (size_t) -1l)
      {
	size_t length;

	/* Find the next entry.  It gets automatically added to the
	   hash table.  */
	abb = __libdw_getabbrev (cu->dbg, cu, cu->last_abbrev_offset, &length,
				 NULL);
	if (abb == NULL || abb == DWARF_END_ABBREV)
	  {
	    /* Make sure we do not try to search for it again.  */
	    cu->last_abbrev_offset = (size_t) -1l;
	    return DWARF_END_ABBREV;
	  }

	cu->last_abbrev_offset += length;

	/* Is this the code we are looking for?  */
	if (abb->code == code)
	  break;
      }

  return abb;
}


int
dwarf_tag (die)
     Dwarf_Die *die;
{
  /* Do we already know the abbreviation?  */
  if (die->abbrev == NULL)
    {
      /* Get the abbreviation code.  */
      unsigned int u128;
      const unsigned char *addr = die->addr;
      get_uleb128 (u128, addr);

      /* Find the abbreviation.  */
      die->abbrev = __libdw_findabbrev (die->cu, u128);
    }

  if (unlikely (die->abbrev == DWARF_END_ABBREV))
    {
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return DW_TAG_invalid;
    }

  return die->abbrev->tag;
}
INTDEF(dwarf_tag)
