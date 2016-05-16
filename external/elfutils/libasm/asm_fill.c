/* Determine fill pattern for a section.
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

#include <stdlib.h>
#include <string.h>

#include <libasmP.h>
#include <system.h>


int
asm_fill (asmscn, bytes, len)
     AsmScn_t *asmscn;
     void *bytes;
     size_t len;
{
  struct FillPattern *pattern;
  struct FillPattern *old_pattern;

  if (asmscn == NULL)
    /* Some earlier error.  */
    return -1;

  if (bytes == NULL)
    /* Use the default pattern.  */
    pattern = (struct FillPattern *) __libasm_default_pattern;
  else
    {
      /* Allocate appropriate memory.  */
      pattern = (struct FillPattern *) malloc (sizeof (struct FillPattern)
					       + len);
      if (pattern == NULL)
	return -1;

      pattern->len = len;
      memcpy (pattern->bytes, bytes, len);
    }

  old_pattern = asmscn->pattern;
  asmscn->pattern = pattern;

  /* Free the old data structure if we have allocated it.  */
  if (old_pattern != __libasm_default_pattern)
    free (old_pattern);

  return 0;
}
