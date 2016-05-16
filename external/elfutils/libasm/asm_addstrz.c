/* Add string to a section.
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

#include <ctype.h>
#include <stdio.h>
#include <string.h>

#include <libasmP.h>


/* Add zero terminated string STR of size LEN to (sub)section ASMSCN.  */
int
asm_addstrz (asmscn, str, len)
     AsmScn_t *asmscn;
     const char *str;
     size_t len;
{
  if (asmscn == NULL)
    return -1;

  if (unlikely (asmscn->type == SHT_NOBITS))
    {
      if (len == 0)
	{
	  if (str[0] != '\0')
	    {
	      __libasm_seterrno (ASM_E_TYPE);
	      return -1;
	    }
	}
      else
	{
	  size_t cnt;

	  for (cnt = 0; cnt < len; ++cnt)
	    if (str[cnt] != '\0')
	      {
		__libasm_seterrno (ASM_E_TYPE);
		return -1;
	      }
	}
    }

  if (len == 0)
    len = strlen (str) + 1;

  if (unlikely (asmscn->ctx->textp))
    {
      bool nextline = true;

      do
	{
	  if (nextline)
	    {
	      fputs ("\t.string\t\"", asmscn->ctx->out.file);
	      nextline = false;
	    }

	  if (*str == '\0')
	    fputs ("\\000", asmscn->ctx->out.file);
	  else if (! isascii (*str))
	    fprintf (asmscn->ctx->out.file, "\\%03o",
		     (unsigned int) *((unsigned char *)str));
	  else if (*str == '\\')
	    fputs ("\\\\", asmscn->ctx->out.file);
	  else if (*str == '\n')
	    {
	      fputs ("\\n\"", asmscn->ctx->out.file);
	      nextline = true;
	    }
	  else
	    fputc (*str, asmscn->ctx->out.file);

	  ++str;
	}
      while (--len > 0 && (len > 1 || *str != '\0'));

      if (! nextline)
	fputs ("\"\n", asmscn->ctx->out.file);
    }
  else
    {
      /* Make sure there is enough room.  */
      if (__libasm_ensure_section_space (asmscn, len) != 0)
	return -1;

      /* Copy the string.  */
      memcpy (&asmscn->content->data[asmscn->content->len], str, len);

      /* Adjust the pointer in the data buffer.  */
      asmscn->content->len += len;

      /* Increment the offset in the (sub)section.  */
      asmscn->offset += len;
    }

  return 0;
}
