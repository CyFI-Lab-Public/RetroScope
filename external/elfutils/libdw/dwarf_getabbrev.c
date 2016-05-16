/* Get abbreviation at given offset.
   Copyright (C) 2003, 2004, 2005, 2006 Red Hat, Inc.
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

#include <assert.h>
#include <dwarf.h>
#include "libdwP.h"


Dwarf_Abbrev *
internal_function
__libdw_getabbrev (dbg, cu, offset, lengthp, result)
     Dwarf *dbg;
     struct Dwarf_CU *cu;
     Dwarf_Off offset;
     size_t *lengthp;
     Dwarf_Abbrev *result;
{
  /* Don't fail if there is not .debug_abbrev section.  */
  if (dbg->sectiondata[IDX_debug_abbrev] == NULL)
    return NULL;

  if (offset >= dbg->sectiondata[IDX_debug_abbrev]->d_size)
    {
      __libdw_seterrno (DWARF_E_INVALID_OFFSET);
      return NULL;
    }

  const unsigned char *abbrevp
    = (unsigned char *) dbg->sectiondata[IDX_debug_abbrev]->d_buf + offset;

  if (*abbrevp == '\0')
    /* We are past the last entry.  */
    return DWARF_END_ABBREV;

  /* 7.5.3 Abbreviations Tables

     [...] Each declaration begins with an unsigned LEB128 number
     representing the abbreviation code itself.  [...]  The
     abbreviation code is followed by another unsigned LEB128
     number that encodes the entry's tag.  [...]

     [...] Following the tag encoding is a 1-byte value that
     determines whether a debugging information entry using this
     abbreviation has child entries or not. [...]

     [...] Finally, the child encoding is followed by a series of
     attribute specifications. Each attribute specification
     consists of two parts. The first part is an unsigned LEB128
     number representing the attribute's name. The second part is
     an unsigned LEB128 number representing the attribute's form.  */
  const unsigned char *start_abbrevp = abbrevp;
  unsigned int code;
  get_uleb128 (code, abbrevp);

  /* Check whether this code is already in the hash table.  */
  bool foundit = false;
  Dwarf_Abbrev *abb = NULL;
  if (cu == NULL
      || (abb = Dwarf_Abbrev_Hash_find (&cu->abbrev_hash, code, NULL)) == NULL)
    {
      if (result == NULL)
	abb = libdw_typed_alloc (dbg, Dwarf_Abbrev);
      else
	abb = result;
    }
  else
    {
      foundit = true;

      assert (abb->offset == offset);

      /* If the caller doesn't need the length we are done.  */
      if (lengthp == NULL)
	goto out;
    }

  /* If there is already a value in the hash table we are going to
     overwrite its content.  This must not be a problem, since the
     content better be the same.  */
  abb->code = code;
  get_uleb128 (abb->tag, abbrevp);
  abb->has_children = *abbrevp++ == DW_CHILDREN_yes;
  abb->attrp = (unsigned char *) abbrevp;
  abb->offset = offset;

  /* Skip over all the attributes and count them while doing so.  */
  abb->attrcnt = 0;
  unsigned int attrname;
  unsigned int attrform;
  do
    {
      get_uleb128 (attrname, abbrevp);
      get_uleb128 (attrform, abbrevp);
    }
  while (attrname != 0 && attrform != 0 && ++abb->attrcnt);

  /* Return the length to the caller if she asked for it.  */
  if (lengthp != NULL)
    *lengthp = abbrevp - start_abbrevp;

  /* Add the entry to the hash table.  */
  if (cu != NULL && ! foundit)
    (void) Dwarf_Abbrev_Hash_insert (&cu->abbrev_hash, abb->code, abb);

 out:
  return abb;
}


Dwarf_Abbrev *
dwarf_getabbrev (die, offset, lengthp)
     Dwarf_Die *die;
     Dwarf_Off offset;
     size_t *lengthp;
{
  return __libdw_getabbrev (die->cu->dbg, die->cu,
			    die->cu->orig_abbrev_offset + offset, lengthp,
			    NULL);
}
