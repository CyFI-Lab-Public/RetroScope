/* Find debugging and symbol information for a module in libdwfl.
   Copyright (C) 2006,2007 Red Hat, Inc.
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

const char *
dwfl_module_getsym (Dwfl_Module *mod, int ndx,
		    GElf_Sym *sym, GElf_Word *shndxp)
{
  if (unlikely (mod == NULL))
    return NULL;

  if (unlikely (mod->symdata == NULL))
    {
      int result = INTUSE(dwfl_module_getsymtab) (mod);
      if (result < 0)
	return NULL;
    }

  GElf_Word shndx;
  sym = gelf_getsymshndx (mod->symdata, mod->symxndxdata, ndx, sym, &shndx);
  if (unlikely (sym == NULL))
    {
      __libdwfl_seterrno (DWFL_E_LIBELF);
      return NULL;
    }

  if (sym->st_shndx != SHN_XINDEX)
    shndx = sym->st_shndx;

  if (shndxp != NULL)
    *shndxp = shndx;

  switch (shndx)
    {
    case SHN_ABS:
    case SHN_UNDEF:
    case SHN_COMMON:
      break;

    default:
      if (mod->e_type == ET_REL)
	{
	  /* In an ET_REL file, the symbol table values are relative
	     to the section, not to the module's load base.  */
	  size_t symshstrndx = SHN_UNDEF;
	  Dwfl_Error result = __libdwfl_relocate_value (mod, mod->symfile->elf,
							&symshstrndx,
							shndx, &sym->st_value);
	  if (unlikely (result != DWFL_E_NOERROR))
	    {
	      __libdwfl_seterrno (result);
	      return NULL;
	    }
	}
      /* Apply the bias to the symbol value.  */
      sym->st_value += mod->symfile->bias;
      break;
    }

  if (unlikely (sym->st_name >= mod->symstrdata->d_size))
    {
      __libdwfl_seterrno (DWFL_E_BADSTROFF);
      return NULL;
    }
  return (const char *) mod->symstrdata->d_buf + sym->st_name;
}
INTDEF (dwfl_module_getsym)
