/* Find CU for given offset.
   Copyright (C) 2003, 2004, 2005, 2007 Red Hat, Inc.
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
#include <search.h>
#include "libdwP.h"


static int
findcu_cb (const void *arg1, const void *arg2)
{
  struct Dwarf_CU *cu1 = (struct Dwarf_CU *) arg1;
  struct Dwarf_CU *cu2 = (struct Dwarf_CU *) arg2;

  /* Find out which of the two arguments is the search value.  It has
     end offset 0.  */
  if (cu1->end == 0)
    {
      if (cu1->start < cu2->start)
	return -1;
      if (cu1->start >= cu2->end)
	return 1;
    }
  else
    {
      if (cu2->start < cu1->start)
	return 1;
      if (cu2->start >= cu1->end)
	return -1;
    }

  return 0;
}


struct Dwarf_CU *
__libdw_findcu (dbg, start)
     Dwarf *dbg;
     Dwarf_Off start;
{
  /* Maybe we already know that CU.  */
  struct Dwarf_CU fake = { .start = start, .end = 0 };
  struct Dwarf_CU **found = tfind (&fake, &dbg->cu_tree, findcu_cb);
  if (found != NULL)
    return *found;

  if (start < dbg->next_cu_offset)
    {
    invalid:
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return NULL;
    }

  /* No.  Then read more CUs.  */
  while (1)
    {
      Dwarf_Off oldoff = dbg->next_cu_offset;
      uint8_t address_size;
      uint8_t offset_size;
      Dwarf_Off abbrev_offset;

      if (INTUSE(dwarf_nextcu) (dbg, oldoff, &dbg->next_cu_offset, NULL,
				&abbrev_offset, &address_size, &offset_size)
	  != 0)
	/* No more entries.  */
	return NULL;

      /* XXX We need the version number but dwarf_nextcu swallows it.  */
      const char *bytes = (dbg->sectiondata[IDX_debug_info]->d_buf + oldoff
			   + (2 * offset_size - 4));
      uint16_t version = read_2ubyte_unaligned (dbg, bytes);

      /* We only know how to handle the DWARF version 2 and 3 formats.  */
      if (unlikely (version != 2) && unlikely (version != 3))
	goto invalid;

      /* Create an entry for this CU.  */
      struct Dwarf_CU *newp = libdw_typed_alloc (dbg, struct Dwarf_CU);

      newp->dbg = dbg;
      newp->start = oldoff;
      newp->end = dbg->next_cu_offset;
      newp->address_size = address_size;
      newp->offset_size = offset_size;
      newp->version = version;
      Dwarf_Abbrev_Hash_init (&newp->abbrev_hash, 41);
      newp->orig_abbrev_offset = newp->last_abbrev_offset = abbrev_offset;
      newp->lines = NULL;
      newp->locs = NULL;

      /* Add the new entry to the search tree.  */
      if (tsearch (newp, &dbg->cu_tree, findcu_cb) == NULL)
	{
	  /* Something went wrong.  Unfo the operation.  */
	  dbg->next_cu_offset = oldoff;
	  __libdw_seterrno (DWARF_E_NOMEM);
	  return NULL;
	}

      /* Is this the one we are looking for?  */
      if (start < dbg->next_cu_offset)
	// XXX Match exact offset.
	return newp;
    }
  /* NOTREACHED */
}
