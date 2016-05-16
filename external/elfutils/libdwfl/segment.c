/* Manage address space lookup table for libdwfl.
   Copyright (C) 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.

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

#include "libdwflP.h"

static GElf_Addr
segment_start (Dwfl *dwfl, GElf_Addr start)
{
  if (dwfl->segment_align > 1)
    start &= -dwfl->segment_align;
  return start;
}

static GElf_Addr
segment_end (Dwfl *dwfl, GElf_Addr end)
{
  if (dwfl->segment_align > 1)
    end = (end + dwfl->segment_align - 1) & -dwfl->segment_align;
  return end;
}

static bool
insert (Dwfl *dwfl, size_t i, GElf_Addr start, GElf_Addr end, int segndx)
{
  bool need_start = (i == 0 || dwfl->lookup_addr[i - 1] != start);
  bool need_end = (i >= dwfl->lookup_elts || dwfl->lookup_addr[i + 1] != end);
  size_t need = need_start + need_end;
  if (need == 0)
    return false;

  if (dwfl->lookup_alloc - dwfl->lookup_elts < need)
    {
      size_t n = dwfl->lookup_alloc == 0 ? 16 : dwfl->lookup_alloc * 2;
      GElf_Addr *naddr = realloc (dwfl->lookup_addr, sizeof naddr[0] * n);
      if (unlikely (naddr == NULL))
	return true;
      int *nsegndx = realloc (dwfl->lookup_segndx, sizeof nsegndx[0] * n);
      if (unlikely (nsegndx == NULL))
	{
	  if (naddr != dwfl->lookup_addr)
	    free (naddr);
	  return true;
	}
      dwfl->lookup_alloc = n;
      dwfl->lookup_addr = naddr;
      dwfl->lookup_segndx = nsegndx;

      if (dwfl->lookup_module != NULL)
	{
	  /* Make sure this array is big enough too.  */
	  Dwfl_Module **old = dwfl->lookup_module;
	  dwfl->lookup_module = realloc (dwfl->lookup_module,
					 sizeof dwfl->lookup_module[0] * n);
	  if (unlikely (dwfl->lookup_module == NULL))
	    {
	      free (old);
	      return true;
	    }
	}
    }

  if (unlikely (i < dwfl->lookup_elts))
    {
      memcpy (&dwfl->lookup_addr[i + need], &dwfl->lookup_addr[i],
	      need * sizeof dwfl->lookup_addr[0]);
      memcpy (&dwfl->lookup_segndx[i + need], &dwfl->lookup_segndx[i],
	      need * sizeof dwfl->lookup_segndx[0]);
      if (dwfl->lookup_module != NULL)
	memcpy (&dwfl->lookup_module[i + need], &dwfl->lookup_module[i],
		need * sizeof dwfl->lookup_module[0]);
    }

  if (need_start)
    {
      dwfl->lookup_addr[i] = start;
      dwfl->lookup_segndx[i] = segndx;
      ++i;
    }
  else
    dwfl->lookup_segndx[i - 1] = segndx;

  if (need_end)
    {
      dwfl->lookup_addr[i] = end;
      dwfl->lookup_segndx[i] = -1;
    }

  dwfl->lookup_elts += need;

  return false;
}

static int
lookup (Dwfl *dwfl, GElf_Addr address, int hint)
{
  if (hint >= 0
      && address >= dwfl->lookup_addr[hint]
      && ((size_t) hint + 1 == dwfl->lookup_elts
	  || address <= dwfl->lookup_addr[hint + 1]))
    return hint;

  /* Do binary search on the array indexed by module load address.  */
  size_t l = 0, u = dwfl->lookup_elts;
  while (l < u)
    {
      size_t idx = (l + u) / 2;
      if (address < dwfl->lookup_addr[idx])
	u = idx;
      else
	{
	  l = idx + 1;
	  if (l == dwfl->lookup_elts || address < dwfl->lookup_addr[l])
	    return idx;
	}
    }

  return -1;
}

static bool
reify_segments (Dwfl *dwfl)
{
  int hint = -1;
  for (Dwfl_Module *mod = dwfl->modulelist; mod != NULL; mod = mod->next)
    if (! mod->gc)
      {
	const GElf_Addr start = segment_start (dwfl, mod->low_addr);
	const GElf_Addr end = segment_end (dwfl, mod->high_addr);

	int idx = lookup (dwfl, start, hint);
	if (unlikely (idx < 0))
	  {
	    /* Module starts below any segment.  Insert a low one.  */
	    if (unlikely (insert (dwfl, 0, start, end, -1)))
	      return true;
	    idx = 0;
	  }
	else if (dwfl->lookup_addr[idx] > start)
	  {
	    /* The module starts in the middle of this segment.  Split it.  */
	    if (unlikely (insert (dwfl, idx + 1, start, end,
				  dwfl->lookup_segndx[idx])))
	      return true;
	    ++idx;
	  }
	else if (dwfl->lookup_addr[idx] < start)
	  {
	    /* The module starts past the end of this segment.
	       Add a new one.  */
	    if (unlikely (insert (dwfl, idx + 1, start, end, -1)))
	      return true;
	    ++idx;
	  }

	if ((size_t) idx + 1 < dwfl->lookup_elts
	    && end < dwfl->lookup_addr[idx + 1]
	    /* The module ends in the middle of this segment.  Split it.  */
	    && unlikely (insert (dwfl, idx + 1,
				 end, dwfl->lookup_addr[idx + 1], -1)))
	  return true;

	if (dwfl->lookup_module == NULL)
	  {
	    dwfl->lookup_module = calloc (dwfl->lookup_alloc,
					  sizeof dwfl->lookup_module[0]);
	    if (unlikely (dwfl->lookup_module == NULL))
	      return true;
	  }

	/* Cache a backpointer in the module.  */
	mod->segment = idx;

	/* Put MOD in the table for each segment that's inside it.  */
	do
	  dwfl->lookup_module[idx++] = mod;
	while ((size_t) idx < dwfl->lookup_elts
	       && dwfl->lookup_addr[idx] < end);
	hint = (size_t) idx < dwfl->lookup_elts ? idx : -1;
      }

  return false;
}

int
dwfl_addrsegment (Dwfl *dwfl, Dwarf_Addr address, Dwfl_Module **mod)
{
  if (unlikely (dwfl == NULL))
    return -1;

  if (unlikely (dwfl->lookup_module == NULL)
      && mod != NULL
      && unlikely (reify_segments (dwfl)))
    {
      __libdwfl_seterrno (DWFL_E_NOMEM);
      return -1;
    }

  int idx = lookup (dwfl, address, -1);
  if (likely (mod != NULL))
    {
      if (unlikely (idx < 0) || unlikely (dwfl->lookup_module == NULL))
	*mod = NULL;
      else
	{
	  *mod = dwfl->lookup_module[idx];

	  /* If this segment does not have a module, but the address is
	     the upper boundary of the previous segment's module, use that.  */
	  if (*mod == NULL && idx > 0 && dwfl->lookup_addr[idx] == address)
	    {
	      *mod = dwfl->lookup_module[idx - 1];
	      if (*mod != NULL && (*mod)->high_addr != address)
		*mod = NULL;
	    }
	}
    }

  if (likely (idx >= 0))
    /* Translate internal segment table index to user segment index.  */
    idx = dwfl->lookup_segndx[idx];

  return idx;
}
INTDEF (dwfl_addrsegment)

int
dwfl_report_segment (Dwfl *dwfl, int ndx, const GElf_Phdr *phdr, GElf_Addr bias,
		     const void *ident)
{
  if (dwfl == NULL)
    return -1;

  if (ndx < 0)
    ndx = dwfl->lookup_tail_ndx;

  if (phdr->p_align > 1 && (dwfl->segment_align <= 1 ||
			    phdr->p_align < dwfl->segment_align))
    dwfl->segment_align = phdr->p_align;

  if (unlikely (dwfl->lookup_module != NULL))
    {
      free (dwfl->lookup_module);
      dwfl->lookup_module = NULL;
    }

  GElf_Addr start = segment_start (dwfl, bias + phdr->p_vaddr);
  GElf_Addr end = segment_end (dwfl, bias + phdr->p_vaddr + phdr->p_memsz);

  /* Coalesce into the last one if contiguous and matching.  */
  if (ndx != dwfl->lookup_tail_ndx
      || ident == NULL
      || ident != dwfl->lookup_tail_ident
      || start != dwfl->lookup_tail_vaddr
      || phdr->p_offset != dwfl->lookup_tail_offset)
    {
      /* Normally just appending keeps us sorted.  */

      size_t i = dwfl->lookup_elts;
      while (i > 0 && unlikely (start < dwfl->lookup_addr[i - 1]))
	--i;

      if (unlikely (insert (dwfl, i, start, end, ndx)))
	{
	  __libdwfl_seterrno (DWFL_E_NOMEM);
	  return -1;
	}
    }

  dwfl->lookup_tail_ident = ident;
  dwfl->lookup_tail_vaddr = end;
  dwfl->lookup_tail_offset = end - bias - phdr->p_vaddr + phdr->p_offset;
  dwfl->lookup_tail_ndx = ndx + 1;

  return ndx;
}
INTDEF (dwfl_report_segment)
