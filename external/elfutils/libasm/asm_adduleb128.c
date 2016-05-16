/* Add integer to a section.
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
#include <string.h>

#include "libasmP.h"


int
asm_adduleb128 (asmscn, num)
     AsmScn_t *asmscn;
     uint32_t num;
{
  if (asmscn == NULL)
    return -1;

  if (asmscn->type == SHT_NOBITS && unlikely (num != 0))
    {
      __libasm_seterrno (ASM_E_TYPE);
      return -1;
    }

  if (unlikely (asmscn->ctx->textp))
    fprintf (asmscn->ctx->out.file, "\t.uleb128\t%" PRIu32 "\n", num);
  else
    {
      char tmpbuf[(sizeof (num) * 8 + 6) / 7];
      char *dest = tmpbuf;
      uint32_t byte;

      while (1)
	{
	  byte = num & 0x7f;

	  num >>= 7;
	  if (num == 0)
	    /* This is the last byte.  */
	    break;

	  *dest++ = byte | 0x80;
	}

      *dest++ = byte;

      /* Number of bytes produced.  */
      size_t nbytes = dest - tmpbuf;

      /* Make sure we have enough room.  */
      if (__libasm_ensure_section_space (asmscn, nbytes) != 0)
	return -1;

      /* Copy the bytes.  */
      if (likely (asmscn->type != SHT_NOBITS))
	memcpy (&asmscn->content->data[asmscn->content->len], tmpbuf, nbytes);

      /* Adjust the pointer in the data buffer.  */
      asmscn->content->len += nbytes;

      /* Increment the offset in the (sub)section.  */
      asmscn->offset += nbytes;
    }

  return 0;
}
