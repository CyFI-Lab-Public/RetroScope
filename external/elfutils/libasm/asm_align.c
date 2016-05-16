/* Align section.
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

#include <inttypes.h>
#include <stdlib.h>
#include <sys/param.h>

#include <libasmP.h>
#include <system.h>


int
asm_align (asmscn, value)
     AsmScn_t *asmscn;
     GElf_Word value;
{
  if (asmscn == NULL)
    /* An earlier error.  */
    return -1;

      /* The alignment value must be a power of two.  */
  if (unlikely (! powerof2 (value)))
    {
      __libasm_seterrno (ASM_E_INVALID);
      return -1;
    }

  if (unlikely (asmscn->ctx->textp))
    {
      fprintf (asmscn->ctx->out.file, "\t.align %" PRId32 ", ",
	       (int32_t) value);
      if (asmscn->pattern->len == 1)
	fprintf (asmscn->ctx->out.file, "%02hhx\n", asmscn->pattern->bytes[0]);
      else
	{
	  fputc_unlocked ('"', asmscn->ctx->out.file);

	  for (size_t cnt = 0; cnt < asmscn->pattern->len; ++cnt)
	    fprintf (asmscn->ctx->out.file, "\\x%02hhx",
		     asmscn->pattern->bytes[cnt]);

	  fputs_unlocked ("\"\n", asmscn->ctx->out.file);
	}
      return 0;
    }

  rwlock_wrlock (asmscn->ctx->lock);

  int result = 0;

  /* Fillbytes necessary?  */
  if ((asmscn->offset & (value - 1)) != 0)
    {
      /* Add fillbytes.  */
      size_t cnt = value - (asmscn->offset & (value - 1));

      /* Ensure there is enough room to add the fill bytes.  */
      result = __libasm_ensure_section_space (asmscn, cnt);
      if (result != 0)
	goto out;

      /* Fill in the bytes.  We align the pattern according to the
	 current offset.  */
      size_t byteptr = asmscn->offset % asmscn->pattern->len;

      /* Update the total size.  */
      asmscn->offset += cnt;

      do
	{
	  asmscn->content->data[asmscn->content->len++]
	    = asmscn->pattern->bytes[byteptr++];

	  if (byteptr == asmscn->pattern->len)
	    byteptr = 0;
	}
      while (--cnt > 0);
    }

  /* Remember the maximum alignment for this subsection.  */
  if (asmscn->max_align < value)
    {
      asmscn->max_align = value;

      /* Update the parent as well (if it exists).  */
      if (asmscn->subsection_id != 0)
	{
	  rwlock_wrlock (asmscn->data.up->ctx->lock);

	  if (asmscn->data.up->max_align < value)
	    asmscn->data.up->max_align = value;

	  rwlock_unlock (asmscn->data.up->ctx->lock);
	}
    }

 out:
  rwlock_unlock (asmscn->ctx->lock);

  return result;
}


/* Ensure there are at least LEN bytes available in the output buffer
   for ASMSCN.  */
int
__libasm_ensure_section_space (asmscn, len)
     AsmScn_t *asmscn;
     size_t len;
{
  /* The blocks with the section content are kept in a circular
     single-linked list.  */
  size_t size;

  if (asmscn->content == NULL)
    {
      /* This is the first block.  */
      size = MAX (2 * len, 960);

      asmscn->content = (struct AsmData *) malloc (sizeof (struct AsmData)
						   + size);
      if (asmscn->content == NULL)
	return -1;

      asmscn->content->next = asmscn->content;
    }
  else
    {
      struct AsmData *newp;

      if (asmscn->content->maxlen - asmscn->content->len >= len)
	/* Nothing to do, there is enough space.  */
	return 0;

      size = MAX (2 *len, MIN (32768, 2 * asmscn->offset));

      newp = (struct AsmData *) malloc (sizeof (struct AsmData) + size);
      if (newp == NULL)
	return -1;

      newp->next = asmscn->content->next;
      asmscn->content = asmscn->content->next = newp;
    }

  asmscn->content->len = 0;
  asmscn->content->maxlen = size;

  return 0;
}
