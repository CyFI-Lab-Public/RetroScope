/* Conversion functions for versioning information.
   Copyright (C) 1998, 1999, 2000, 2002, 2003 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 1998.

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
elf_cvt_Verdef (void *dest, const void *src, size_t len, int encode)
{
  /* We have two different record types: ElfXX_Verndef and ElfXX_Verdaux.
     To recognize them we have to walk the data structure and convert
     them one after the other.  The ENCODE parameter specifies whether
     we are encoding or decoding.  When we are encoding we can immediately
     use the data in the buffer; if not, we have to decode the data before
     using it.  */
  size_t def_offset = 0;
  GElf_Verdef *ddest;
  GElf_Verdef *dsrc;

  /* We rely on the types being all the same size.  */
  assert (sizeof (GElf_Verdef) == sizeof (Elf32_Verdef));
  assert (sizeof (GElf_Verdaux) == sizeof (Elf32_Verdaux));
  assert (sizeof (GElf_Verdef) == sizeof (Elf64_Verdef));
  assert (sizeof (GElf_Verdaux) == sizeof (Elf64_Verdaux));

  if (len == 0)
    return;

  do
    {
      size_t aux_offset;
      GElf_Verdaux *asrc;

      /* Test for correct offset.  */
      if (def_offset + sizeof (GElf_Verdef) > len)
	return;

      /* Work the tree from the first record.  */
      ddest = (GElf_Verdef *) ((char *) dest + def_offset);
      dsrc = (GElf_Verdef *) ((char *) src + def_offset);

      /* Decode first if necessary.  */
      if (! encode)
	{
	  ddest->vd_version = bswap_16 (dsrc->vd_version);
	  ddest->vd_flags = bswap_16 (dsrc->vd_flags);
	  ddest->vd_ndx = bswap_16 (dsrc->vd_ndx);
	  ddest->vd_cnt = bswap_16 (dsrc->vd_cnt);
	  ddest->vd_hash = bswap_32 (dsrc->vd_hash);
	  ddest->vd_aux = bswap_32 (dsrc->vd_aux);
	  ddest->vd_next = bswap_32 (dsrc->vd_next);

	  aux_offset = def_offset + ddest->vd_aux;
	}
      else
	aux_offset = def_offset + dsrc->vd_aux;

      /* Handle all the auxiliary records belonging to this definition.  */
      do
	{
	  GElf_Verdaux *adest;

	  /* Test for correct offset.  */
	  if (aux_offset + sizeof (GElf_Verdaux) > len)
	    return;

	  adest = (GElf_Verdaux *) ((char *) dest + aux_offset);
	  asrc = (GElf_Verdaux *) ((char *) src + aux_offset);

	  if (encode)
	    aux_offset += asrc->vda_next;

	  adest->vda_name = bswap_32 (asrc->vda_name);
	  adest->vda_next = bswap_32 (asrc->vda_next);

	  if (! encode)
	    aux_offset += adest->vda_next;
	}
      while (asrc->vda_next != 0);

      /* Encode now if necessary.  */
      if (encode)
	{
	  def_offset += dsrc->vd_next;

	  ddest->vd_version = bswap_16 (dsrc->vd_version);
	  ddest->vd_flags = bswap_16 (dsrc->vd_flags);
	  ddest->vd_ndx = bswap_16 (dsrc->vd_ndx);
	  ddest->vd_cnt = bswap_16 (dsrc->vd_cnt);
	  ddest->vd_hash = bswap_32 (dsrc->vd_hash);
	  ddest->vd_aux = bswap_32 (dsrc->vd_aux);
	  ddest->vd_next = bswap_32 (dsrc->vd_next);
	}
      else
	def_offset += ddest->vd_next;
    }
  while (dsrc->vd_next != 0);
}


static void
elf_cvt_Verneed (void *dest, const void *src, size_t len, int encode)
{
  /* We have two different record types: ElfXX_Verndef and ElfXX_Verdaux.
     To recognize them we have to walk the data structure and convert
     them one after the other.  The ENCODE parameter specifies whether
     we are encoding or decoding.  When we are encoding we can immediately
     use the data in the buffer; if not, we have to decode the data before
     using it.  */
  size_t need_offset = 0;
  GElf_Verneed *ndest;
  GElf_Verneed *nsrc;

  /* We rely on the types being all the same size.  */
  assert (sizeof (GElf_Verneed) == sizeof (Elf32_Verneed));
  assert (sizeof (GElf_Vernaux) == sizeof (Elf32_Vernaux));
  assert (sizeof (GElf_Verneed) == sizeof (Elf64_Verneed));
  assert (sizeof (GElf_Vernaux) == sizeof (Elf64_Vernaux));

  if (len == 0)
    return;

  do
    {
      size_t aux_offset;
      GElf_Vernaux *asrc;

      /* Test for correct offset.  */
      if (need_offset + sizeof (GElf_Verneed) > len)
	return;

      /* Work the tree from the first record.  */
      ndest = (GElf_Verneed *) ((char *) dest + need_offset);
      nsrc = (GElf_Verneed *) ((char *) src + need_offset);

      /* Decode first if necessary.  */
      if (! encode)
	{
	  ndest->vn_version = bswap_16 (nsrc->vn_version);
	  ndest->vn_cnt = bswap_16 (nsrc->vn_cnt);
	  ndest->vn_file = bswap_32 (nsrc->vn_file);
	  ndest->vn_aux = bswap_32 (nsrc->vn_aux);
	  ndest->vn_next = bswap_32 (nsrc->vn_next);

	  aux_offset = need_offset + ndest->vn_aux;
	}
      else
	aux_offset = need_offset + nsrc->vn_aux;

      /* Handle all the auxiliary records belonging to this requirement.  */
      do
	{
	  GElf_Vernaux *adest;

	  /* Test for correct offset.  */
	  if (aux_offset + sizeof (GElf_Vernaux) > len)
	    return;

	  adest = (GElf_Vernaux *) ((char *) dest + aux_offset);
	  asrc = (GElf_Vernaux *) ((char *) src + aux_offset);

	  if (encode)
	    aux_offset += asrc->vna_next;

	  adest->vna_hash = bswap_32 (asrc->vna_hash);
	  adest->vna_flags = bswap_16 (asrc->vna_flags);
	  adest->vna_other = bswap_16 (asrc->vna_other);
	  adest->vna_name = bswap_32 (asrc->vna_name);
	  adest->vna_next = bswap_32 (asrc->vna_next);

	  if (! encode)
	    aux_offset += adest->vna_next;
	}
      while (asrc->vna_next != 0);

      /* Encode now if necessary.  */
      if (encode)
	{
	  need_offset += nsrc->vn_next;

	  ndest->vn_version = bswap_16 (nsrc->vn_version);
	  ndest->vn_cnt = bswap_16 (nsrc->vn_cnt);
	  ndest->vn_file = bswap_32 (nsrc->vn_file);
	  ndest->vn_aux = bswap_32 (nsrc->vn_aux);
	  ndest->vn_next = bswap_32 (nsrc->vn_next);
	}
      else
	need_offset += ndest->vn_next;
    }
  while (nsrc->vn_next != 0);
}
