/* Enumerate the PC ranges covered by a DIE.
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
#include <assert.h>


ptrdiff_t
dwarf_ranges (Dwarf_Die *die, ptrdiff_t offset, Dwarf_Addr *basep,
	      Dwarf_Addr *startp, Dwarf_Addr *endp)
{
  if (die == NULL)
    return -1;

  if (offset == 0
      /* Usually there is a single contiguous range.  */
      && INTUSE(dwarf_highpc) (die, endp) == 0
      && INTUSE(dwarf_lowpc) (die, startp) == 0)
    /* A offset into .debug_ranges will never be 1, it must be at least a
       multiple of 4.  So we can return 1 as a special case value to mark
       there are no ranges to look for on the next call.  */
    return 1;

  if (offset == 1)
    return 0;

  /* We have to look for a noncontiguous range.  */

  const Elf_Data *d = die->cu->dbg->sectiondata[IDX_debug_ranges];
  if (d == NULL)
    {
      __libdw_seterrno (DWARF_E_NO_DEBUG_RANGES);
      return -1;
    }

  if (offset == 0)
    {
      Dwarf_Attribute attr_mem;
      Dwarf_Attribute *attr = INTUSE(dwarf_attr) (die, DW_AT_ranges,
						  &attr_mem);
      if (attr == NULL)
	return -1;

      /* Must have the form data4 or data8 which act as an offset.  */
      Dwarf_Word start_offset;
      if (INTUSE(dwarf_formudata) (attr, &start_offset) != 0)
	return -1;

      offset = start_offset;
      assert ((Dwarf_Word) offset == start_offset);

      /* Fetch the CU's base address.  */
      Dwarf_Die cudie = CUDIE (attr->cu);

      /* Find the base address of the compilation unit.  It will
	 normally be specified by DW_AT_low_pc.  In DWARF-3 draft 4,
	 the base address could be overridden by DW_AT_entry_pc.  It's
	 been removed, but GCC emits DW_AT_entry_pc and not DW_AT_lowpc
	 for compilation units with discontinuous ranges.  */
      if (unlikely (INTUSE(dwarf_lowpc) (&cudie, basep) != 0)
	  && INTUSE(dwarf_formaddr) (INTUSE(dwarf_attr) (&cudie,
							 DW_AT_entry_pc,
							 &attr_mem),
				     basep) != 0)
	{
	  if (INTUSE(dwarf_errno) () == 0)
	    {
	    invalid:
	      __libdw_seterrno (DWARF_E_INVALID_DWARF);
	    }
	  return -1;
	}
    }
  else if (offset < 0 || (size_t) offset >= d->d_size)
    {
      __libdw_seterrno (DWARF_E_INVALID_OFFSET);
      return -1l;
    }

  unsigned char *readp = d->d_buf + offset;

 next:
  if ((unsigned char *) d->d_buf + d->d_size - readp
      < die->cu->address_size * 2)
    goto invalid;

  Dwarf_Addr begin;
  Dwarf_Addr end;
  if (die->cu->address_size == 8)
    {
      begin = read_8ubyte_unaligned_inc (die->cu->dbg, readp);
      end = read_8ubyte_unaligned_inc (die->cu->dbg, readp);
      if (begin == (uint64_t) -1l) /* Base address entry.  */
	{
	  *basep = end;
	  goto next;
	}
    }
  else
    {
      begin = read_4ubyte_unaligned_inc (die->cu->dbg, readp);
      end = read_4ubyte_unaligned_inc (die->cu->dbg, readp);
      if (begin == (uint32_t) -1) /* Base address entry.  */
	{
	  *basep = end;
	  goto next;
	}
    }

  if (begin == 0 && end == 0) /* End of list entry.  */
    return 0;

  /* We have an address range entry.  */
  *startp = *basep + begin;
  *endp = *basep + end;
  return readp - (unsigned char *) d->d_buf;
}
INTDEF (dwarf_ranges)
