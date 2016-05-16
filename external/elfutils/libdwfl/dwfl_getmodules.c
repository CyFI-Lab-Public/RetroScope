/* Iterate through modules.
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

#include "libdwflP.h"

ptrdiff_t
dwfl_getmodules (Dwfl *dwfl,
		 int (*callback) (Dwfl_Module *, void **,
				  const char *, Dwarf_Addr, void *),
		 void *arg,
		 ptrdiff_t offset)
{
  if (dwfl == NULL)
    return -1;

  /* We iterate through the linked list when it's all we have.
     But continuing from an offset is slow that way.  So when
     DWFL->lookup_module is populated, we can instead keep our
     place by jumping directly into the array.  Since the actions
     of a callback could cause it to get populated, we must
     choose the style of place-holder when we return an offset,
     and we encode the choice in the low bits of that value.  */

  Dwfl_Module *m = dwfl->modulelist;

  if ((offset & 3) == 1)
    {
      offset >>= 2;
      for (ptrdiff_t pos = 0; pos < offset; ++pos)
	if (m == NULL)
	  return -1;
	else
	  m = m->next;
    }
  else if (((offset & 3) == 2) && likely (dwfl->lookup_module != NULL))
    {
      offset >>= 2;

      if ((size_t) offset - 1 == dwfl->lookup_elts)
	return 0;

      if (unlikely ((size_t) offset - 1 > dwfl->lookup_elts))
	return -1;

      m = dwfl->lookup_module[offset - 1];
      if (unlikely (m == NULL))
	return -1;
    }
  else if (offset != 0)
    {
      __libdwfl_seterrno (DWFL_E_BADSTROFF);
      return -1;
    }

  while (m != NULL)
    {
      int ok = (*callback) (MODCB_ARGS (m), arg);
      ++offset;
      m = m->next;
      if (ok != DWARF_CB_OK)
	return ((dwfl->lookup_module == NULL) ? ((offset << 2) | 1)
		: (((m == NULL ? (ptrdiff_t) dwfl->lookup_elts + 1
		     : m->segment + 1) << 2) | 2));
    }
  return 0;
}
INTDEF (dwfl_getmodules)
