/* Return scope DIEs containing PC address.
   Copyright (C) 2005, 2007 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <stdlib.h>
#include "libdwP.h"
#include <dwarf.h>


struct args
{
  Dwarf_Addr pc;
  Dwarf_Die *scopes;
  unsigned int inlined, nscopes;
  Dwarf_Die inlined_origin;
};

/* Preorder visitor: prune the traversal if this DIE does not contain PC.  */
static int
pc_match (unsigned int depth, struct Dwarf_Die_Chain *die, void *arg)
{
  struct args *a = arg;

  if (a->scopes != NULL)
    die->prune = true;
  else
    {
      /* dwarf_haspc returns an error if there are no appropriate attributes.
	 But we use it indiscriminantly instead of presuming which tags can
	 have PC attributes.  So when it fails for that reason, treat it just
	 as a nonmatching return.  */
      int result = INTUSE(dwarf_haspc) (&die->die, a->pc);
      if (result < 0)
	{
	  int error = INTUSE(dwarf_errno) ();
	  if (error != DWARF_E_NOERROR && error != DWARF_E_NO_DEBUG_RANGES)
	    {
	      __libdw_seterrno (error);
	      return -1;
	    }
	  result = 0;
	}
      if (result == 0)
    	die->prune = true;

      if (!die->prune
	  && INTUSE (dwarf_tag) (&die->die) == DW_TAG_inlined_subroutine)
	a->inlined = depth;
    }

  return 0;
}

/* Preorder visitor for second partial traversal after finding a
   concrete inlined instance.  */
static int
origin_match (unsigned int depth, struct Dwarf_Die_Chain *die, void *arg)
{
  struct args *a = arg;

  if (die->die.addr != a->inlined_origin.addr)
    return 0;

  /* We have a winner!  This is the abstract definition of the inline
     function of which A->scopes[A->nscopes - 1] is a concrete instance.
  */

  unsigned int nscopes = a->nscopes + depth;
  Dwarf_Die *scopes = realloc (a->scopes, nscopes * sizeof scopes[0]);
  if (scopes == NULL)
    {
      free (a->scopes);
      __libdw_seterrno (DWARF_E_NOMEM);
      return -1;
    }

  a->scopes = scopes;
  do
    {
      die = die->parent;
      scopes[a->nscopes++] = die->die;
    }
  while (a->nscopes < nscopes);
  assert (die->parent == NULL);
  return a->nscopes;
}

/* Postorder visitor: first (innermost) call wins.  */
static int
pc_record (unsigned int depth, struct Dwarf_Die_Chain *die, void *arg)
{
  struct args *a = arg;

  if (die->prune)
    return 0;

  if (a->scopes == NULL)
    {
      /* We have hit the innermost DIE that contains the target PC.  */

      a->nscopes = depth + 1 - a->inlined;
      a->scopes = malloc (a->nscopes * sizeof a->scopes[0]);
      if (a->scopes == NULL)
	{
	  __libdw_seterrno (DWARF_E_NOMEM);
	  return -1;
	}

      for (unsigned int i = 0; i < a->nscopes; ++i)
	{
	  a->scopes[i] = die->die;
	  die = die->parent;
	}

      if (a->inlined == 0)
	{
	  assert (die == NULL);
	  return a->nscopes;
	}

      /* This is the concrete inlined instance itself.
	 Record its abstract_origin pointer.  */
      Dwarf_Die *const inlinedie = &a->scopes[depth - a->inlined];

      assert (INTUSE (dwarf_tag) (inlinedie) == DW_TAG_inlined_subroutine);
      Dwarf_Attribute attr_mem;
      Dwarf_Attribute *attr = INTUSE (dwarf_attr) (inlinedie,
						   DW_AT_abstract_origin,
						   &attr_mem);
      if (INTUSE (dwarf_formref_die) (attr, &a->inlined_origin) == NULL)
	return -1;
      return 0;
    }


  /* We've recorded the scopes back to one that is a concrete inlined
     instance.  Now return out of the traversal back to the scope
     containing that instance.  */

  assert (a->inlined);
  if (depth >= a->inlined)
    /* Not there yet.  */
    return 0;

  /* Now we are in a scope that contains the concrete inlined instance.
     Search it for the inline function's abstract definition.
     If we don't find it, return to search the containing scope.
     If we do find it, the nonzero return value will bail us out
     of the postorder traversal.  */
  return __libdw_visit_scopes (depth, die, &origin_match, NULL, a);
}


int
dwarf_getscopes (Dwarf_Die *cudie, Dwarf_Addr pc, Dwarf_Die **scopes)
{
  if (cudie == NULL)
    return -1;

  struct Dwarf_Die_Chain cu = { .parent = NULL, .die = *cudie };
  struct args a = { .pc = pc };

  int result = __libdw_visit_scopes (0, &cu, &pc_match, &pc_record, &a);

  if (result == 0 && a.scopes != NULL)
    result = __libdw_visit_scopes (0, &cu, &origin_match, NULL, &a);

  if (result > 0)
    *scopes = a.scopes;

  return result;
}
