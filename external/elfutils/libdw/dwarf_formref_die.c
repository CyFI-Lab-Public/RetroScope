/* Look up the DIE in a reference-form attribute.
   Copyright (C) 2005, 2007 Red Hat, Inc.
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


Dwarf_Die *
dwarf_formref_die (attr, die_mem)
     Dwarf_Attribute *attr;
     Dwarf_Die *die_mem;
{
  if (attr == NULL)
    return NULL;

  Dwarf_Off offset;
  if (attr->form == DW_FORM_ref_addr)
    {
      /* This has an absolute offset.  */

      uint8_t ref_size = (attr->cu->version == 2
			  ? attr->cu->address_size
			  : attr->cu->offset_size);

      if (ref_size == 8)
	offset = read_8ubyte_unaligned (attr->cu->dbg, attr->valp);
      else
	offset = read_4ubyte_unaligned (attr->cu->dbg, attr->valp);
    }
  else
    {
      /* Other forms produce an offset from the CU.  */
      if (unlikely (__libdw_formref (attr, &offset) != 0))
	return NULL;
      offset += attr->cu->start;
    }

  return INTUSE(dwarf_offdie) (attr->cu->dbg, offset, die_mem);
}
INTDEF (dwarf_formref_die)
