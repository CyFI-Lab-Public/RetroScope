/* Return location expression list.
   Copyright (C) 2000, 2001, 2002, 2004, 2005, 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

#include <dwarf.h>
#include <search.h>
#include <stdlib.h>

#include <libdwP.h>


static bool
attr_ok (Dwarf_Attribute *attr)
{
  if (attr == NULL)
    return false;

  /* Must be one of the attributes listed below.  */
  switch (attr->code)
    {
    case DW_AT_location:
    case DW_AT_data_member_location:
    case DW_AT_vtable_elem_location:
    case DW_AT_string_length:
    case DW_AT_use_location:
    case DW_AT_frame_base:
    case DW_AT_return_addr:
    case DW_AT_static_link:
      break;

    default:
      __libdw_seterrno (DWARF_E_NO_LOCLIST);
      return false;
    }

  return true;
}


struct loclist
{
  uint8_t atom;
  Dwarf_Word number;
  Dwarf_Word number2;
  Dwarf_Word offset;
  struct loclist *next;
};


static int
loc_compare (const void *p1, const void *p2)
{
  const struct loc_s *l1 = (const struct loc_s *) p1;
  const struct loc_s *l2 = (const struct loc_s *) p2;

  if ((uintptr_t) l1->addr < (uintptr_t) l2->addr)
    return -1;
  if ((uintptr_t) l1->addr > (uintptr_t) l2->addr)
    return 1;

  return 0;
}

static int
getlocation (struct Dwarf_CU *cu, const Dwarf_Block *block,
	     Dwarf_Op **llbuf, size_t *listlen)
{
  Dwarf *dbg = cu->dbg;

  /* Check whether we already looked at this list.  */
  struct loc_s fake = { .addr = block->data };
  struct loc_s **found = tfind (&fake, &cu->locs, loc_compare);
  if (found != NULL)
    {
      /* We already saw it.  */
      *llbuf = (*found)->loc;
      *listlen = (*found)->nloc;

      return 0;
    }

  const unsigned char *data = block->data;
  const unsigned char *const end_data = data + block->length;

  struct loclist *loclist = NULL;
  unsigned int n = 0;
  /* Decode the opcodes.  It is possible in some situations to have a
     block of size zero.  */
  while (data < end_data)
    {
      struct loclist *newloc;
      newloc = (struct loclist *) alloca (sizeof (struct loclist));
      newloc->number = 0;
      newloc->number2 = 0;
      newloc->offset = data - block->data;
      newloc->next = loclist;
      loclist = newloc;
      ++n;

      switch ((newloc->atom = *data++))
	{
	case DW_OP_addr:
	  /* Address, depends on address size of CU.  */
	  if (cu->address_size == 4)
	    {
	      if (unlikely (data + 4 > end_data))
		{
		invalid:
		  __libdw_seterrno (DWARF_E_INVALID_DWARF);
		  return -1;
		}

	      newloc->number = read_4ubyte_unaligned_inc (dbg, data);
	    }
	  else
	    {
	      if (unlikely (data + 8 > end_data))
		goto invalid;

	      newloc->number = read_8ubyte_unaligned_inc (dbg, data);
	    }
	  break;

	case DW_OP_deref:
	case DW_OP_dup:
	case DW_OP_drop:
	case DW_OP_over:
	case DW_OP_swap:
	case DW_OP_rot:
	case DW_OP_xderef:
	case DW_OP_abs:
	case DW_OP_and:
	case DW_OP_div:
	case DW_OP_minus:
	case DW_OP_mod:
	case DW_OP_mul:
	case DW_OP_neg:
	case DW_OP_not:
	case DW_OP_or:
	case DW_OP_plus:
	case DW_OP_shl:
	case DW_OP_shr:
	case DW_OP_shra:
	case DW_OP_xor:
	case DW_OP_eq:
	case DW_OP_ge:
	case DW_OP_gt:
	case DW_OP_le:
	case DW_OP_lt:
	case DW_OP_ne:
	case DW_OP_lit0 ... DW_OP_lit31:
	case DW_OP_reg0 ... DW_OP_reg31:
	case DW_OP_nop:
	case DW_OP_push_object_address:
	case DW_OP_call_ref:
	  /* No operand.  */
	  break;

	case DW_OP_const1u:
	case DW_OP_pick:
	case DW_OP_deref_size:
	case DW_OP_xderef_size:
	  if (unlikely (data >= end_data))
	    goto invalid;

	  newloc->number = *data++;
	  break;

	case DW_OP_const1s:
	  if (unlikely (data >= end_data))
	    goto invalid;

	  newloc->number = *((int8_t *) data);
	  ++data;
	  break;

	case DW_OP_const2u:
	  if (unlikely (data + 2 > end_data))
	    goto invalid;

	  newloc->number = read_2ubyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_const2s:
	case DW_OP_skip:
	case DW_OP_bra:
	case DW_OP_call2:
	  if (unlikely (data + 2 > end_data))
	    goto invalid;

	  newloc->number = read_2sbyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_const4u:
	  if (unlikely (data + 4 > end_data))
	    goto invalid;

	  newloc->number = read_4ubyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_const4s:
	case DW_OP_call4:
	  if (unlikely (data + 4 > end_data))
	    goto invalid;

	  newloc->number = read_4sbyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_const8u:
	  if (unlikely (data + 8 > end_data))
	    goto invalid;

	  newloc->number = read_8ubyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_const8s:
	  if (unlikely (data + 8 > end_data))
	    goto invalid;

	  newloc->number = read_8sbyte_unaligned_inc (dbg, data);
	  break;

	case DW_OP_constu:
	case DW_OP_plus_uconst:
	case DW_OP_regx:
	case DW_OP_piece:
	  /* XXX Check size.  */
	  get_uleb128 (newloc->number, data);
	  break;

	case DW_OP_consts:
	case DW_OP_breg0 ... DW_OP_breg31:
	case DW_OP_fbreg:
	  /* XXX Check size.  */
	  get_sleb128 (newloc->number, data);
	  break;

	case DW_OP_bregx:
	  /* XXX Check size.  */
	  get_uleb128 (newloc->number, data);
	  get_sleb128 (newloc->number2, data);
	  break;

	default:
	  goto invalid;
	}
    }

  if (unlikely (n == 0))
    {
      /* This is not allowed.

	 XXX Is it?  */
      goto invalid;
    }

  /* Allocate the array.  */
  Dwarf_Op *result = libdw_alloc (dbg, Dwarf_Op, sizeof (Dwarf_Op), n);

  /* Store the result.  */
  *llbuf = result;
  *listlen = n;

  do
    {
      /* We populate the array from the back since the list is
         backwards.  */
      --n;
      result[n].atom = loclist->atom;
      result[n].number = loclist->number;
      result[n].number2 = loclist->number2;
      result[n].offset = loclist->offset;

      loclist = loclist->next;
    }
  while (n > 0);

  /* Insert a record in the search tree so that we can find it again
     later.  */
  struct loc_s *newp = libdw_alloc (dbg, struct loc_s, sizeof (struct loc_s),
				    1);
  newp->addr = block->data;
  newp->loc = result;
  newp->nloc = *listlen;
  (void) tsearch (newp, &cu->locs, loc_compare);

  /* We did it.  */
  return 0;
}

int
dwarf_getlocation (attr, llbuf, listlen)
     Dwarf_Attribute *attr;
     Dwarf_Op **llbuf;
     size_t *listlen;
{
  if (! attr_ok (attr))
    return -1;

  /* If it has a block form, it's a single location expression.  */
  Dwarf_Block block;
  if (INTUSE(dwarf_formblock) (attr, &block) != 0)
    return -1;

  return getlocation (attr->cu, &block, llbuf, listlen);
}

int
dwarf_getlocation_addr (attr, address, llbufs, listlens, maxlocs)
     Dwarf_Attribute *attr;
     Dwarf_Addr address;
     Dwarf_Op **llbufs;
     size_t *listlens;
     size_t maxlocs;
{
  if (! attr_ok (attr))
    return -1;

  if (llbufs == NULL)
    maxlocs = SIZE_MAX;

  /* If it has a block form, it's a single location expression.  */
  Dwarf_Block block;
  if (INTUSE(dwarf_formblock) (attr, &block) == 0)
    {
      if (maxlocs == 0)
	return 0;
      if (llbufs != NULL &&
	  getlocation (attr->cu, &block, &llbufs[0], &listlens[0]) != 0)
	return -1;
      return listlens[0] == 0 ? 0 : 1;
    }

  int error = INTUSE(dwarf_errno) ();
  if (unlikely (error != DWARF_E_NO_BLOCK))
    {
      __libdw_seterrno (error);
      return -1;
    }

  /* Must have the form data4 or data8 which act as an offset.  */
  Dwarf_Word offset;
  if (unlikely (INTUSE(dwarf_formudata) (attr, &offset) != 0))
    return -1;

  const Elf_Data *d = attr->cu->dbg->sectiondata[IDX_debug_loc];
  if (unlikely (d == NULL))
    {
      __libdw_seterrno (DWARF_E_NO_LOCLIST);
      return -1;
    }

  Dwarf_Addr base = (Dwarf_Addr) -1;
  unsigned char *readp = d->d_buf + offset;
  size_t got = 0;
  while (got < maxlocs)
    {
      if ((unsigned char *) d->d_buf + d->d_size - readp
	  < attr->cu->address_size * 2)
	{
	invalid:
	  __libdw_seterrno (DWARF_E_INVALID_DWARF);
	  return -1;
	}

      Dwarf_Addr begin;
      Dwarf_Addr end;
      if (attr->cu->address_size == 8)
	{
	  begin = read_8ubyte_unaligned_inc (attr->cu->dbg, readp);
	  end = read_8ubyte_unaligned_inc (attr->cu->dbg, readp);

	  if (begin == (Elf64_Addr) -1l) /* Base address entry.  */
	    {
	      base = end;
	      if (unlikely (base == (Dwarf_Addr) -1))
		goto invalid;
	      continue;
	    }
	}
      else
	{
	  begin = read_4ubyte_unaligned_inc (attr->cu->dbg, readp);
	  end = read_4ubyte_unaligned_inc (attr->cu->dbg, readp);

	  if (begin == (Elf32_Addr) -1) /* Base address entry.  */
	    {
	      base = end;
	      continue;
	    }
	}

      if (begin == 0 && end == 0) /* End of list entry.  */
	break;

      if ((unsigned char *) d->d_buf + d->d_size - readp < 2)
	goto invalid;

      /* We have a location expression.  */
      block.length = read_2ubyte_unaligned_inc (attr->cu->dbg, readp);
      block.data = readp;
      if ((unsigned char *) d->d_buf + d->d_size - readp
	  < (ptrdiff_t) block.length)
	goto invalid;
      readp += block.length;

      if (base == (Dwarf_Addr) -1)
	{
	  /* Fetch the CU's base address.  */
	  Dwarf_Die cudie = CUDIE (attr->cu);

	  /* Find the base address of the compilation unit.  It will
	     normally be specified by DW_AT_low_pc.  In DWARF-3 draft 4,
	     the base address could be overridden by DW_AT_entry_pc.  It's
	     been removed, but GCC emits DW_AT_entry_pc and not DW_AT_lowpc
	     for compilation units with discontinuous ranges.  */
	  Dwarf_Attribute attr_mem;
	  if (unlikely (INTUSE(dwarf_lowpc) (&cudie, &base) != 0)
	      && INTUSE(dwarf_formaddr) (INTUSE(dwarf_attr) (&cudie,
							     DW_AT_entry_pc,
							     &attr_mem),
					 &base) != 0)
	    {
	      if (INTUSE(dwarf_errno) () != 0)
		return -1;

	      /* The compiler provided no base address when it should
		 have.  Buggy GCC does this when it used absolute
		 addresses in the location list and no DW_AT_ranges.  */
	      base = 0;
	    }
	}

      if (address >= base + begin && address < base + end)
	{
	  /* This one matches the address.  */
	  if (llbufs != NULL
	      && unlikely (getlocation (attr->cu, &block,
					&llbufs[got], &listlens[got]) != 0))
	    return -1;
	  ++got;
	}
    }

  return got;
}
