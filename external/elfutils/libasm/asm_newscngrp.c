/* Create new section group.
   Copyright (C) 2002 Red Hat, Inc.
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
#include <stdlib.h>
#include <string.h>

#include "libasmP.h"
#include <system.h>



AsmScnGrp_t *
asm_newscngrp (ctx, grpname, signature, flags)
     AsmCtx_t *ctx;
     const char *grpname;
     AsmSym_t *signature;
     Elf32_Word flags;
{
  AsmScnGrp_t *result;
  size_t grpname_len = strlen (grpname) + 1;

  if (ctx == NULL)
    return NULL;

  if ((flags & ~GRP_COMDAT) != 0)
    {
      /* This is not a supported flag.  */
      __libasm_seterrno (ASM_E_INVALID);
      return NULL;
    }

  result = (AsmScnGrp_t *) malloc (sizeof (AsmScnGrp_t) + grpname_len);
  if (result == NULL)
    return NULL;

  result->signature = signature;
  result->members = NULL;
  result->nmembers = 0;
  result->flags = flags;

  memcpy (result->name, grpname, grpname_len);
  result->strent = ebl_strtabadd (ctx->section_strtab, result->name,
				  grpname_len);

  if (unlikely (ctx->textp))
    // XXX TBI.  What is the format?
    abort ();
  else
    {
      result->scn = elf_newscn (ctx->out.elf);
      if (result->scn == NULL)
	{
	  /* Couldn't allocate a new section.  */
	  __libasm_seterrno (ASM_E_LIBELF);
	  free (result);
	  return NULL;
	}
    }

  /* Enqueue is the context data structure.  */
  if (ctx->ngroups == 0)
    {
      assert (ctx->groups == NULL);
      ctx->groups = result->next = result;
    }
  else
    {
      result->next = ctx->groups->next;
      ctx->groups = ctx->groups->next = result;
    }
  ++ctx->ngroups;

  return result;
}
