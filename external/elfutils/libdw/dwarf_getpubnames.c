/* Get public symbol information.
   Copyright (C) 2002, 2003, 2004, 2005, 2008 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <sys/param.h>

#include <libdwP.h>
#include <dwarf.h>

/* ANDROID_CHANGE_BEGIN */
#include <AndroidFixup.h>
/* ANDROID_CHANGE_END */

static int
get_offsets (Dwarf *dbg)
{
  size_t allocated = 0;
  size_t cnt = 0;
  struct pubnames_s *mem = NULL;
  const size_t entsize = sizeof (struct pubnames_s);
  unsigned char *const startp = dbg->sectiondata[IDX_debug_pubnames]->d_buf;
  unsigned char *readp = startp;
  unsigned char *endp = readp + dbg->sectiondata[IDX_debug_pubnames]->d_size;

  while (readp + 14 < endp)
    {
      /* If necessary, allocate more entries.  */
      if (cnt >= allocated)
	{
	  allocated = MAX (10, 2 * allocated);
	  struct pubnames_s *newmem
	    = (struct pubnames_s *) realloc (mem, allocated * entsize);
	  if (newmem == NULL)
	    {
	      __libdw_seterrno (DWARF_E_NOMEM);
	    err_return:
	      free (mem);
	      return -1;
	    }

	  mem = newmem;
	}

      /* Read the set header.  */
      int len_bytes = 4;
      Dwarf_Off len = read_4ubyte_unaligned_inc (dbg, readp);
      if (len == DWARF3_LENGTH_64_BIT)
	{
	  len = read_8ubyte_unaligned_inc (dbg, readp);
	  len_bytes = 8;
	}
      else if (unlikely (len >= DWARF3_LENGTH_MIN_ESCAPE_CODE
			 && len <= DWARF3_LENGTH_MAX_ESCAPE_CODE))
	{
	invalid_dwarf:
	  __libdw_seterrno (DWARF_E_INVALID_DWARF);
	  goto err_return;
	}

      /* Now we know the offset of the first offset/name pair.  */
      mem[cnt].set_start = readp + 2 + 2 * len_bytes - startp;
      mem[cnt].address_len = len_bytes;
      if (mem[cnt].set_start >= dbg->sectiondata[IDX_debug_pubnames]->d_size)
	/* Something wrong, the first entry is beyond the end of
	   the section.  */
	break;

      /* Read the version.  It better be two for now.  */
      uint16_t version = read_2ubyte_unaligned (dbg, readp);
      if (unlikely (version != 2))
	{
	  __libdw_seterrno (DWARF_E_INVALID_VERSION);
	  goto err_return;
	}

      /* Get the CU offset.  */
      if (len_bytes == 4)
	mem[cnt].cu_offset = read_4ubyte_unaligned (dbg, readp + 2);
      else
	mem[cnt].cu_offset = read_8ubyte_unaligned (dbg, readp + 2);

      /* Determine the size of the CU header.  */
      if (unlikely (dbg->sectiondata[IDX_debug_info] == NULL
		    || dbg->sectiondata[IDX_debug_info]->d_buf == NULL
		    || (mem[cnt].cu_offset + 3
			>= dbg->sectiondata[IDX_debug_info]->d_size)))
	goto invalid_dwarf;

      unsigned char *infop
	= ((unsigned char *) dbg->sectiondata[IDX_debug_info]->d_buf
	   + mem[cnt].cu_offset);
      if (read_4ubyte_unaligned_noncvt (infop) == DWARF3_LENGTH_64_BIT)
	mem[cnt].cu_header_size = 23;
      else
	mem[cnt].cu_header_size = 11;

      ++cnt;

      /* Advance to the next set.  */
      readp += len;
    }

  if (mem == NULL)
    {
      __libdw_seterrno (DWARF_E_NO_ENTRY);
      return -1;
    }

  dbg->pubnames_sets = (struct pubnames_s *) realloc (mem, cnt * entsize);
  dbg->pubnames_nsets = cnt;

  return 0;
}


ptrdiff_t
dwarf_getpubnames (dbg, callback, arg, offset)
     Dwarf *dbg;
     int (*callback) (Dwarf *, Dwarf_Global *, void *);
     void *arg;
     ptrdiff_t offset;
{
  if (dbg == NULL)
    return -1l;

  if (unlikely (offset < 0))
    {
      __libdw_seterrno (DWARF_E_INVALID_OFFSET);
      return -1l;
    }

  /* Make sure it is a valid offset.  */
  if (unlikely (dbg->sectiondata[IDX_debug_pubnames] == NULL
		|| ((size_t) offset
		    >= dbg->sectiondata[IDX_debug_pubnames]->d_size)))
    /* No (more) entry.  */
    return 0;

  /* If necessary read the set information.  */
  if (dbg->pubnames_nsets == 0 && unlikely (get_offsets (dbg) != 0))
    return -1l;

  /* Find the place where to start.  */
  size_t cnt;
  if (offset == 0)
    {
      cnt = 0;
      offset = dbg->pubnames_sets[0].set_start;
    }
  else
    {
      for (cnt = 0; cnt + 1 < dbg->pubnames_nsets; ++cnt)
	if ((Dwarf_Off) offset >= dbg->pubnames_sets[cnt].set_start)
	  {
	    assert ((Dwarf_Off) offset
		    < dbg->pubnames_sets[cnt + 1].set_start);
	    break;
	  }
      assert (cnt + 1 < dbg->pubnames_nsets);
    }

  unsigned char *startp
    = (unsigned char *) dbg->sectiondata[IDX_debug_pubnames]->d_buf;
  unsigned char *readp = startp + offset;
  while (1)
    {
      Dwarf_Global gl;

      gl.cu_offset = (dbg->pubnames_sets[cnt].cu_offset
		      + dbg->pubnames_sets[cnt].cu_header_size);

      while (1)
	{
	  /* READP points to the next offset/name pair.  */
	  if (dbg->pubnames_sets[cnt].address_len == 4)
	    gl.die_offset = read_4ubyte_unaligned_inc (dbg, readp);
	  else
	    gl.die_offset = read_8ubyte_unaligned_inc (dbg, readp);

	  /* If the offset is zero we reached the end of the set.  */
	  if (gl.die_offset == 0)
	    break;

	  /* Add the CU offset.  */
	  gl.die_offset += dbg->pubnames_sets[cnt].cu_offset;

	  gl.name = (char *) readp;
	  readp = (unsigned char *) rawmemchr (gl.name, '\0') + 1;

	  /* We found name and DIE offset.  Report it.  */
	  if (callback (dbg, &gl, arg) != DWARF_CB_OK)
	    {
	      /* The user wants us to stop.  Return the offset of the
		 next entry.  */
	      return readp - startp;
	    }
	}

      if (++cnt == dbg->pubnames_nsets)
	/* This was the last set.  */
	break;

      startp = (unsigned char *) dbg->sectiondata[IDX_debug_pubnames]->d_buf;
      readp = startp + dbg->pubnames_sets[cnt].set_start;
    }

  /* We are done.  No more entries.  */
  return 0;
}
