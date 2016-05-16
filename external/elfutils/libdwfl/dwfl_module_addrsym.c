/* Find debugging and symbol information for a module in libdwfl.
   Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
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

/* Returns the name of the symbol "closest" to ADDR.
   Never returns symbols at addresses above ADDR.  */

const char *
dwfl_module_addrsym (Dwfl_Module *mod, GElf_Addr addr,
		     GElf_Sym *closest_sym, GElf_Word *shndxp)
{
  int syments = INTUSE(dwfl_module_getsymtab) (mod);
  if (syments < 0)
    return NULL;

  /* Return true iff we consider ADDR to lie in the same section as SYM.  */
  GElf_Word addr_shndx = SHN_UNDEF;
  inline bool same_section (const GElf_Sym *sym, GElf_Word shndx)
    {
      /* For absolute symbols and the like, only match exactly.  */
      if (shndx >= SHN_LORESERVE)
	return sym->st_value == addr;

      /* Figure out what section ADDR lies in.  */
      if (addr_shndx == SHN_UNDEF)
	{
	  GElf_Addr mod_addr = addr - mod->symfile->bias;
	  Elf_Scn *scn = NULL;
	  addr_shndx = SHN_ABS;
	  while ((scn = elf_nextscn (mod->symfile->elf, scn)) != NULL)
	    {
	      GElf_Shdr shdr_mem;
	      GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
	      if (likely (shdr != NULL)
		  && mod_addr >= shdr->sh_addr
		  && mod_addr < shdr->sh_addr + shdr->sh_size)
		{
		  addr_shndx = elf_ndxscn (scn);
		  break;
		}
	    }
	}

      return shndx == addr_shndx;
    }

  /* Keep track of the closest symbol we have seen so far.
     Here we store only symbols with nonzero st_size.  */
  const char *closest_name = NULL;
  GElf_Word closest_shndx = SHN_UNDEF;

  /* Keep track of an eligible symbol with st_size == 0 as a fallback.  */
  const char *sizeless_name = NULL;
  GElf_Sym sizeless_sym = { 0, 0, 0, 0, 0, SHN_UNDEF };
  GElf_Word sizeless_shndx = SHN_UNDEF;

  /* Keep track of the lowest address a relevant sizeless symbol could have.  */
  GElf_Addr min_label = 0;

  /* Look through the symbol table for a matching symbol.  */
  for (int i = 1; i < syments; ++i)
    {
      GElf_Sym sym;
      GElf_Word shndx;
      const char *name = INTUSE(dwfl_module_getsym) (mod, i, &sym, &shndx);
      if (name != NULL && name[0] != '\0'
	  && sym.st_shndx != SHN_UNDEF
	  && sym.st_value <= addr
	  && GELF_ST_TYPE (sym.st_info) != STT_SECTION
	  && GELF_ST_TYPE (sym.st_info) != STT_FILE
	  && GELF_ST_TYPE (sym.st_info) != STT_TLS)
	{
	  /* Even if we don't choose this symbol, its existence excludes
	     any sizeless symbol (assembly label) that is below its upper
	     bound.  */
	  if (sym.st_value + sym.st_size > min_label)
	    min_label = sym.st_value + sym.st_size;

	  if (sym.st_size == 0 || addr - sym.st_value < sym.st_size)
	    {
	      /* This symbol is a better candidate than the current one
		 if it's closer to ADDR or is global when it was local.  */
	      if (closest_name == NULL
		  || closest_sym->st_value < sym.st_value
		  || (GELF_ST_BIND (closest_sym->st_info)
		      < GELF_ST_BIND (sym.st_info)))
		{
		  if (sym.st_size != 0)
		    {
		      *closest_sym = sym;
		      closest_shndx = shndx;
		      closest_name = name;
		    }
		  else if (same_section (&sym, shndx))
		    {
		      /* Handwritten assembly symbols sometimes have no
			 st_size.  If no symbol with proper size includes
			 the address, we'll use the closest one that is in
			 the same section as ADDR.  */
		      sizeless_sym = sym;
		      sizeless_shndx = shndx;
		      sizeless_name = name;
		    }
		}
	      /* When the beginning of its range is no closer,
		 the end of its range might be.  But do not
		 replace a global symbol with a local!  */
	      else if (sym.st_size != 0
		       && closest_sym->st_value == sym.st_value
		       && closest_sym->st_size > sym.st_size
		       && (GELF_ST_BIND (closest_sym->st_info)
			   <= GELF_ST_BIND (sym.st_info)))
		{
		  *closest_sym = sym;
		  closest_shndx = shndx;
		  closest_name = name;
		}
	    }
	}
    }

  /* If we found no proper sized symbol to use, fall back to the best
     candidate sizeless symbol we found, if any.  */
  if (closest_name == NULL
      && sizeless_name != NULL && sizeless_sym.st_value >= min_label)
    {
      *closest_sym = sizeless_sym;
      closest_shndx = sizeless_shndx;
      closest_name = sizeless_name;
    }

  if (shndxp != NULL)
    *shndxp = closest_shndx;
  return closest_name;
}
INTDEF (dwfl_module_addrsym)
