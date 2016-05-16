/* Helper functions to descend DWARF scope trees.
   Copyright (C) 2005,2006,2007 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include "libdwP.h"
#include <dwarf.h>

enum die_class { ignore, match, match_inline, walk, imported };

static enum die_class
classify_die (Dwarf_Die *die)
{
  switch (INTUSE(dwarf_tag) (die))
    {
      /* DIEs with addresses we can try to match.  */
    case DW_TAG_compile_unit:
    case DW_TAG_module:
    case DW_TAG_lexical_block:
    case DW_TAG_with_stmt:
    case DW_TAG_catch_block:
    case DW_TAG_try_block:
    case DW_TAG_entry_point:
      return match;
    case DW_TAG_inlined_subroutine:
      return match_inline;
    case DW_TAG_subprogram:
      /* This might be a concrete out-of-line instance of an inline, in
	 which case it is not guaranteed to be owned by the right scope and
	 we will search for its origin as for DW_TAG_inlined_subroutine.  */
      return (INTUSE(dwarf_hasattr) (die, DW_AT_abstract_origin)
	      ? match_inline : match);

      /* DIEs without addresses that can own DIEs with addresses.  */
    case DW_TAG_namespace:
    case DW_TAG_class_type:
    case DW_TAG_structure_type:
      return walk;

      /* Special indirection required.  */
    case DW_TAG_imported_unit:
      return imported;

      /* Other DIEs we have no reason to descend.  */
    default:
      break;
    }
  return ignore;
}

int
__libdw_visit_scopes (depth, root, previsit, postvisit, arg)
     unsigned int depth;
     struct Dwarf_Die_Chain *root;
     int (*previsit) (unsigned int depth, struct Dwarf_Die_Chain *, void *);
     int (*postvisit) (unsigned int depth, struct Dwarf_Die_Chain *, void *);
     void *arg;
{
  struct Dwarf_Die_Chain child;

  child.parent = root;
  if (INTUSE(dwarf_child) (&root->die, &child.die) != 0)
    return -1;

  inline int recurse (void)
    {
      return __libdw_visit_scopes (depth + 1, &child,
				   previsit, postvisit, arg);
    }

  do
    {
      child.prune = false;

      if (previsit != NULL)
	{
	  int result = (*previsit) (depth + 1, &child, arg);
	  if (result != DWARF_CB_OK)
	    return result;
	}

      if (!child.prune)
	switch (classify_die (&child.die))
	  {
	  case match:
	  case match_inline:
	  case walk:
	    if (INTUSE(dwarf_haschildren) (&child.die))
	      {
		int result = recurse ();
		if (result != DWARF_CB_OK)
		  return result;
	      }
	    break;

	  case imported:
	    {
	      /* This imports another compilation unit to appear
		 as part of this one, inside the current scope.
		 Recurse to search the referenced unit, but without
		 recording it as an inner scoping level.  */

	      Dwarf_Attribute attr_mem;
	      Dwarf_Attribute *attr = INTUSE(dwarf_attr) (&child.die,
							  DW_AT_import,
							  &attr_mem);
	      if (INTUSE(dwarf_formref_die) (attr, &child.die) != NULL)
		{
		  int result = recurse ();
		  if (result != DWARF_CB_OK)
		    return result;
		}
	    }
	    break;

	  default:
	    break;
	  }

      if (postvisit != NULL)
	{
	  int result = (*postvisit) (depth + 1, &child, arg);
	  if (result != DWARF_CB_OK)
	    return result;
	}
    }
  while (INTUSE(dwarf_siblingof) (&child.die, &child.die) == 0);

  return 0;
}
