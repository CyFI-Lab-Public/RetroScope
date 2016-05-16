/* Compute hash value for given string according to ELF standard.
   Copyright (C) 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 1995.

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
   under an Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) and to distribute linked
   combinations including the two.  Non-GPL Code permitted under this
   exception must only link to the code of Red Hat elfutils through those
   well defined interfaces identified in the file named EXCEPTION found in
   the source code files (the "Approved Interfaces").  The files of Non-GPL
   Code may instantiate templates or use macros or inline functions from
   the Approved Interfaces without causing the resulting work to be covered
   by the GNU General Public License.  Only Red Hat, Inc. may make changes
   or additions to the list of Approved Interfaces.  Red Hat's grant of
   this exception is conditioned upon your not adding any new exceptions.
   If you wish to add a new Approved Interface or exception, please contact
   Red Hat.  You must obey the GNU General Public License in all respects
   for all of the Red Hat elfutils code and other code used in conjunction
   with Red Hat elfutils except the Non-GPL Code covered by this exception.
   If you modify this file, you may extend this exception to your version
   of the file, but you are not obligated to do so.  If you do not wish to
   provide this exception without modification, you must delete this
   exception statement from your version and license this file solely under
   the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifndef _DL_HASH_H
#define _DL_HASH_H	1


/* This is the hashing function specified by the ELF ABI.  In the
   first five operations no overflow is possible so we optimized it a
   bit.  */
static inline unsigned int
__attribute__ ((__pure__))
_dl_elf_hash (const char *name)
{
  const unsigned char *iname = (const unsigned char *) name;
  unsigned int hash = (unsigned int) *iname++;
  if (*iname != '\0')
    {
      hash = (hash << 4) + (unsigned int) *iname++;
      if (*iname != '\0')
	{
	  hash = (hash << 4) + (unsigned int) *iname++;
	  if (*iname != '\0')
	    {
	      hash = (hash << 4) + (unsigned int) *iname++;
	      if (*iname != '\0')
		{
		  hash = (hash << 4) + (unsigned int) *iname++;
		  while (*iname != '\0')
		    {
		      unsigned int hi;
		      hash = (hash << 4) + (unsigned int) *iname++;
		      hi = hash & 0xf0000000;

		      /* The algorithm specified in the ELF ABI is as
			 follows:

			 if (hi != 0)
			 hash ^= hi >> 24;

			 hash &= ~hi;

			 But the following is equivalent and a lot
			 faster, especially on modern processors.  */

		      hash ^= hi;
		      hash ^= hi >> 24;
		    }
		}
	    }
	}
    }
  return hash;
}

#endif /* dl-hash.h */
