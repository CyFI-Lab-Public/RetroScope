/* Get macro information.
   Copyright (C) 2002, 2003, 2004, 2005 Red Hat, Inc.
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

#include <dwarf.h>
#include <string.h>

#include <libdwP.h>


ptrdiff_t
dwarf_getmacros (die, callback, arg, offset)
     Dwarf_Die *die;
     int (*callback) (Dwarf_Macro *, void *);
     void *arg;
     ptrdiff_t offset;
{
  /* Get the appropriate attribute.  */
  Dwarf_Attribute attr;
  if (INTUSE(dwarf_attr) (die, DW_AT_macro_info, &attr) == NULL)
    return -1;

  /* Offset into the .debug_macinfo section.  */
  Dwarf_Word macoff;
  if (INTUSE(dwarf_formudata) (&attr, &macoff) != 0)
    return -1;

  const unsigned char *readp
    = die->cu->dbg->sectiondata[IDX_debug_macinfo]->d_buf + offset;
  const unsigned char *readendp
    = readp + die->cu->dbg->sectiondata[IDX_debug_macinfo]->d_size;

  if (readp == readendp)
    return 0;

  if (*readp != DW_MACINFO_start_file)
    goto invalid;

  while (readp < readendp)
    {
      unsigned int opcode = *readp++;
      unsigned int u128;
      unsigned int u128_2 = 0;
      const char *str = NULL;
      const unsigned char *endp;

      switch (opcode)
	{
	case DW_MACINFO_define:
	case DW_MACINFO_undef:
	case DW_MACINFO_vendor_ext:
	  /*  For the first two opcodes the parameters are
	        line, string
	      For the latter
	        number, string.
	      We can treat these cases together.  */
	  get_uleb128 (u128, readp);

	  endp = memchr (readp, '\0', readendp - readp);
	  if (endp == NULL)
	    goto invalid;

	  str = (char *) readp;
	  readp = endp + 1;
	  break;

	case DW_MACINFO_start_file:
	  /* The two parameters are line and file index.  */
	  get_uleb128 (u128, readp);
	  get_uleb128 (u128_2, readp);
	  break;

	case DW_MACINFO_end_file:
	  /* No parameters for this one.  */
	  u128 = 0;
	  break;

	case 0:
	  /* Nothing more to do.  */
	  return 0;

	default:
	  goto invalid;
	}

      Dwarf_Macro mac;
      mac.opcode = opcode;
      mac.param1 = u128;
      if (str == NULL)
	mac.param2.u = u128_2;
      else
	mac.param2.s = str;

      if (callback (&mac, arg) != DWARF_CB_OK)
	return (readp
		- ((unsigned char *) die->cu->dbg->sectiondata[IDX_debug_macinfo]->d_buf
		   + offset));
    }

  /* If we come here the termination of the data for the CU is not
     present.  */
 invalid:
  __libdw_seterrno (DWARF_E_INVALID_DWARF);
  return -1;
}
