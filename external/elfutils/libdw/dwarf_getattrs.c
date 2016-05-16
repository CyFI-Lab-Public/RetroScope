/* Get attributes of the DIE.
   Copyright (C) 2004, 2005, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2004.

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


ptrdiff_t
dwarf_getattrs (Dwarf_Die *die, int (*callback) (Dwarf_Attribute *, void *),
		void *arg, ptrdiff_t offset)
{
  if (die == NULL)
    return -1l;

  const unsigned char *die_addr = die->addr;

  /* Get the abbreviation code.  */
  unsigned int u128;
  get_uleb128 (u128, die_addr);

  if (die->abbrev == NULL)
    /* Find the abbreviation.  */
    die->abbrev = __libdw_findabbrev (die->cu, u128);

  if (unlikely (die->abbrev == DWARF_END_ABBREV))
    {
    invalid_dwarf:
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return -1l;
    }

  /* This is where the attributes start.  */
  const unsigned char *attrp = die->abbrev->attrp + offset;

  /* Go over the list of attributes.  */
  Dwarf *dbg = die->cu->dbg;
  while (1)
    {
      /* Are we still in bounds?  */
      if (unlikely (attrp
		    >= ((unsigned char *) dbg->sectiondata[IDX_debug_abbrev]->d_buf
			+ dbg->sectiondata[IDX_debug_abbrev]->d_size)))
	goto invalid_dwarf;

      /* Get attribute name and form.  */
      Dwarf_Attribute attr;
      const unsigned char *remembered_attrp = attrp;

      // XXX Fix bound checks
      get_uleb128 (attr.code, attrp);
      get_uleb128 (attr.form, attrp);

      /* We can stop if we found the attribute with value zero.  */
      if (attr.code == 0 && attr.form == 0)
	/* Do not return 0 here - there would be no way to
	   distinguish this value from the attribute at offset 0.
	   Instead we return +1 which would never be a valid
	   offset of an attribute.  */
        return 1l;

      /* Fill in the rest.  */
      attr.valp = (unsigned char *) die_addr;
      attr.cu = die->cu;

      /* Now call the callback function.  */
      if (callback (&attr, arg) != DWARF_CB_OK)
	/* Return the offset of the start of the attribute, so that
	   dwarf_getattrs() can be restarted from this point if the
	   caller so desires.  */
	return remembered_attrp - die->abbrev->attrp;

      /* Skip over the rest of this attribute (if there is any).  */
      if (attr.form != 0)
	{
	  size_t len = __libdw_form_val_len (dbg, die->cu, attr.form,
					     die_addr);

	  if (unlikely (len == (size_t) -1l))
	    /* Something wrong with the file.  */
	    return -1l;

	  // XXX We need better boundary checks.
	  die_addr += len;
	}
    }
  /* NOTREACHED */
}
