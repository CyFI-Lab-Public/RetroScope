/* Conversion functions for versioning information.
   Copyright (C) 2006, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2006.

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

#include <assert.h>
#include <gelf.h>

#include "libelfP.h"


static void
elf_cvt_gnuhash (void *dest, const void *src, size_t len, int encode)
{
  /* The GNU hash table format on 64 bit machines mixes 32 bit and 64 bit
     words.  We must detangle them here.   */
  Elf32_Word *dest32 = dest;
  const Elf32_Word *src32 = src;

  /* First four control words, 32 bits.  */
  for (unsigned int cnt = 0; cnt < 4; ++cnt)
    {
      if (len < 4)
	return;
      dest32[cnt] = bswap_32 (src32[cnt]);
      len -= 4;
    }

  Elf32_Word bitmask_words = encode ? src32[2] : dest32[2];

  /* Now the 64 bit words.  */
  Elf64_Xword *dest64 = (Elf64_Xword *) &dest32[4];
  const Elf64_Xword *src64 = (const Elf64_Xword *) &src32[4];
  for (unsigned int cnt = 0; cnt < bitmask_words; ++cnt)
    {
      if (len < 8)
	return;
      dest64[cnt] = bswap_64 (src64[cnt]);
      len -= 8;
    }

  /* The rest are 32 bit words again.  */
  src32 = (const Elf32_Word *) &src64[bitmask_words];
  dest32 = (Elf32_Word *) &dest64[bitmask_words];
  while (len >= 4)
    {
      *dest32++ = bswap_32 (*src32++);
      len -= 4;
    }
}
