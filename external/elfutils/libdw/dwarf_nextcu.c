/* Advance to next CU header.
   Copyright (C) 2002, 2003, 2004, 2005, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2002.

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

#include <libdwP.h>
#include <dwarf.h>


int
dwarf_nextcu (dwarf, off, next_off, header_sizep, abbrev_offsetp,
	      address_sizep, offset_sizep)
     Dwarf *dwarf;
     Dwarf_Off off;
     Dwarf_Off *next_off;
     size_t *header_sizep;
     Dwarf_Off *abbrev_offsetp;
     uint8_t *address_sizep;
     uint8_t *offset_sizep;
{
  /* Maybe there has been an error before.  */
  if (dwarf == NULL)
    return -1;

  /* If we reached the end before don't do anything.  */
  if (off == (Dwarf_Off) -1l
      /* Make sure there is enough space in the .debug_info section
	 for at least the initial word.  We cannot test the rest since
	 we don't know yet whether this is a 64-bit object or not.  */
      || unlikely (off + 4 >= dwarf->sectiondata[IDX_debug_info]->d_size))
    {
      *next_off = (Dwarf_Off) -1l;
      return 1;
    }

  /* This points into the .debug_info section to the beginning of the
     CU entry.  */
  char *bytes = (char *) dwarf->sectiondata[IDX_debug_info]->d_buf + off;

  /* The format of the CU header is described in dwarf2p1 7.5.1:

     1.  A 4-byte or 12-byte unsigned integer representing the length
	 of the .debug_info contribution for that compilation unit, not
	 including the length field itself. In the 32-bit DWARF format,
	 this is a 4-byte unsigned integer (which must be less than
	 0xfffffff0); in the 64-bit DWARF format, this consists of the
	 4-byte value 0xffffffff followed by an 8-byte unsigned integer
	 that gives the actual length (see Section 7.2.2).

      2. A 2-byte unsigned integer representing the version of the
	 DWARF information for that compilation unit. For DWARF Version
	 2.1, the value in this field is 2.

      3. A 4-byte or 8-byte unsigned offset into the .debug_abbrev
	 section. This offset associates the compilation unit with a
	 particular set of debugging information entry abbreviations. In
	 the 32-bit DWARF format, this is a 4-byte unsigned length; in
	 the 64-bit DWARF format, this is an 8-byte unsigned length (see
	 Section 7.4).

      4. A 1-byte unsigned integer representing the size in bytes of
	 an address on the target architecture. If the system uses
	 segmented addressing, this value represents the size of the
	 offset portion of an address.  */
  uint64_t length = read_4ubyte_unaligned_inc (dwarf, bytes);
  size_t offset_size = 4;
  /* Lengths of 0xfffffff0 - 0xffffffff are escape codes.  Oxffffffff is
     used to indicate that 64-bit dwarf information is being used, the
     other values are currently reserved.  */
  if (length == DWARF3_LENGTH_64_BIT)
    offset_size = 8;
  else if (unlikely (length >= DWARF3_LENGTH_MIN_ESCAPE_CODE
		     && length <= DWARF3_LENGTH_MAX_ESCAPE_CODE))
    {
      __libdw_seterrno (DWARF_E_INVALID_DWARF);
      return -1;
    }

  /* Now we know how large the header is.  */
  if (unlikely (DIE_OFFSET_FROM_CU_OFFSET (off, offset_size)
		>= dwarf->sectiondata[IDX_debug_info]->d_size))
    {
      *next_off = -1;
      return 1;
    }

  if (length == DWARF3_LENGTH_64_BIT)
    /* This is a 64-bit DWARF format.  */
    length = read_8ubyte_unaligned_inc (dwarf, bytes);

  /* Read the version stamp.  Always a 16-bit value.
     XXX Do we need the value?  */
  read_2ubyte_unaligned_inc (dwarf, bytes);

  /* Get offset in .debug_abbrev.  Note that the size of the entry
     depends on whether this is a 32-bit or 64-bit DWARF definition.  */
  uint64_t abbrev_offset;
  if (offset_size == 4)
    abbrev_offset = read_4ubyte_unaligned_inc (dwarf, bytes);
  else
    abbrev_offset = read_8ubyte_unaligned_inc (dwarf, bytes);
  if (abbrev_offsetp != NULL)
    *abbrev_offsetp = abbrev_offset;

  /* The address size.  Always an 8-bit value.  */
  uint8_t address_size = *bytes++;
  if (address_sizep != NULL)
    *address_sizep = address_size;

  /* Store the offset size.  */
  if (offset_sizep != NULL)
    *offset_sizep = offset_size;

  /* Store the header length.  */
  if (header_sizep != NULL)
    *header_sizep = (bytes
		     - ((char *) dwarf->sectiondata[IDX_debug_info]->d_buf
			+ off));

  /* See definition of DIE_OFFSET_FROM_CU_OFFSET macro
     for an explanation of the trick in this expression.  */
  *next_off = off + 2 * offset_size - 4 + length;

  return 0;
}
INTDEF(dwarf_nextcu)
