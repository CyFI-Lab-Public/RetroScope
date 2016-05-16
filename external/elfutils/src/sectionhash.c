/* Section hash table implementation.
   Copyright (C) 2001, 2002, 2005 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

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

#include <elf-knowledge.h>
#include <ld.h>


/* Comparison function for sections.  */
static int
scnhead_compare (struct scnhead *one, struct scnhead *two)
{
  int result = strcmp (one->name, two->name);

  if (result == 0)
    {
      result = one->type - two->type;

      if (result == 0)
	{
	  GElf_Sxword diff = (SH_FLAGS_IMPORTANT (one->flags)
			     - SH_FLAGS_IMPORTANT (two->flags));
	  result = diff < 0 ? -1 : diff == 0 ? 0 : 1;

	  if (result == 0)
	    {
	      result = one->entsize - two->entsize;

	      if (result == 0)
		{
		  result = (one->grp_signature == NULL
			    ? (two->grp_signature == NULL ? 0 : -1)
			    : (two->grp_signature == NULL
			       ? 1 : strcmp (one->grp_signature,
					     two->grp_signature)));

		  if (result == 0)
		    result = one->kind - two->kind;
		}
	    }
	}
    }

  return result;
}

/* Definitions for the section hash table.  */
#define TYPE struct scnhead *
#define NAME ld_section_tab
#define ITERATE 1
#define COMPARE(a, b) scnhead_compare (a, b)

#include "../lib/dynamicsizehash.c"
