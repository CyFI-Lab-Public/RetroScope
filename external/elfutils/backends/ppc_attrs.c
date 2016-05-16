/* Object attribute tags for PowerPC.
   Copyright (C) 2008 Red Hat, Inc.
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

#include <string.h>
#include <dwarf.h>

#define BACKEND ppc_
#include "libebl_CPU.h"

bool
ppc_check_object_attribute (ebl, vendor, tag, value, tag_name, value_name)
     Ebl *ebl __attribute__ ((unused));
     const char *vendor;
     int tag;
     uint64_t value;
     const char **tag_name;
     const char **value_name;
{
  if (!strcmp (vendor, "gnu"))
    switch (tag)
      {
      case 4:
	*tag_name = "GNU_Power_ABI_FP";
	static const char *fp_kinds[] =
	  {
	    "Hard or soft float",
	    "Hard float",
	    "Soft float",
	  };
	if (value < sizeof fp_kinds / sizeof fp_kinds[0])
	  *value_name = fp_kinds[value];
	return true;

      case 8:
	*tag_name = "GNU_Power_ABI_Vector";
	static const char *vector_kinds[] =
	  {
	    "Any", "Generic", "AltiVec", "SPE"
	  };
	if (value < sizeof vector_kinds / sizeof vector_kinds[0])
	  *value_name = vector_kinds[value];
	return true;
      }

  return false;
}

__typeof (ppc_check_object_attribute)
     ppc64_check_object_attribute
     __attribute__ ((alias ("ppc_check_object_attribute")));
