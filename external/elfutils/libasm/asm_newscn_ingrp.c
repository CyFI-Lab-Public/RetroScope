/* Create new section, which is member of a group, in output file.
   Copyright (C) 2002, 2005 Red Hat, Inc.
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

#include "libasmP.h"


AsmScn_t *
asm_newscn_ingrp (ctx, scnname, type, flags, grp)
     AsmCtx_t *ctx;
     const char *scnname;
     GElf_Word type;
     GElf_Xword flags;
     AsmScnGrp_t *grp;
{
  AsmScn_t *result = INTUSE (asm_newscn) (ctx, scnname, type, flags);

  if (likely (result != NULL))
    {
      /* We managed to create a section group.  Add it to the section
	 group.  */
      if (grp->nmembers == 0)
	{
	  assert (grp->members == NULL);
	  grp->members = result->data.main.next_in_group = result;
	}
      else
	{
	  result->data.main.next_in_group
	    = grp->members->data.main.next_in_group;
	  grp->members = grp->members->data.main.next_in_group = result;
	}

      ++grp->nmembers;

      /* Set the SHF_GROUP flag.  */
      if (likely (! ctx->textp))
	{
	  GElf_Shdr shdr_mem;
	  GElf_Shdr *shdr = gelf_getshdr (result->data.main.scn, &shdr_mem);

	  assert (shdr != NULL);
	  shdr->sh_flags |= SHF_GROUP;

	  (void) gelf_update_shdr (result->data.main.scn, shdr);
	}
    }

  return result;
}
