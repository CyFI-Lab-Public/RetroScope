/* Recover relocatibility for addresses computed from debug information.
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

struct dwfl_relocation
{
  size_t count;
  struct
  {
    Elf_Scn *scn;
    Elf_Scn *relocs;
    const char *name;
    GElf_Addr start, end;
  } refs[0];
};


struct secref
{
  struct secref *next;
  Elf_Scn *scn;
  Elf_Scn *relocs;
  const char *name;
  GElf_Addr start, end;
};

static int
compare_secrefs (const void *a, const void *b)
{
  struct secref *const *p1 = a;
  struct secref *const *p2 = b;

  /* No signed difference calculation is correct here, since the
     terms are unsigned and could be more than INT64_MAX apart.  */
  if ((*p1)->start < (*p2)->start)
    return -1;
  if ((*p1)->start > (*p2)->start)
    return 1;

  return 0;
}

static int
cache_sections (Dwfl_Module *mod)
{
  struct secref *refs = NULL;
  size_t nrefs = 0;

  size_t shstrndx;
  if (unlikely (elf_getshstrndx (mod->main.elf, &shstrndx) < 0))
    {
    elf_error:
      __libdwfl_seterrno (DWFL_E_LIBELF);
      return -1;
    }

  bool check_reloc_sections = false;
  Elf_Scn *scn = NULL;
  while ((scn = elf_nextscn (mod->main.elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
      if (shdr == NULL)
	goto elf_error;

      if ((shdr->sh_flags & SHF_ALLOC) && shdr->sh_addr == 0)
	{
	  /* This section might not yet have been looked at.  */
	  if (__libdwfl_relocate_value (mod, mod->main.elf, &shstrndx,
					elf_ndxscn (scn),
					&shdr->sh_addr) != DWFL_E_NOERROR)
	    continue;
	  shdr = gelf_getshdr (scn, &shdr_mem);
	  if (unlikely (shdr == NULL))
	    goto elf_error;
	}

      if (shdr->sh_flags & SHF_ALLOC)
	{
	  const char *name = elf_strptr (mod->main.elf, shstrndx,
					 shdr->sh_name);
	  if (unlikely (name == NULL))
	    goto elf_error;

	  struct secref *newref = alloca (sizeof *newref);
	  newref->scn = scn;
	  newref->relocs = NULL;
	  newref->name = name;
	  newref->start = shdr->sh_addr + mod->main.bias;
	  newref->end = newref->start + shdr->sh_size;
	  newref->next = refs;
	  refs = newref;
	  ++nrefs;
	}

      if (mod->e_type == ET_REL
	  && shdr->sh_size != 0
	  && (shdr->sh_type == SHT_REL || shdr->sh_type == SHT_RELA)
	  && mod->dwfl->callbacks->section_address != NULL)
	{
	  if (shdr->sh_info < elf_ndxscn (scn))
	    {
	      /* We've already looked at the section these relocs apply to.  */
	      Elf_Scn *tscn = elf_getscn (mod->main.elf, shdr->sh_info);
	      if (likely (tscn != NULL))
		for (struct secref *sec = refs; sec != NULL; sec = sec->next)
		  if (sec->scn == tscn)
		    {
		      sec->relocs = scn;
		      break;
		    }
	    }
	  else
	    /* We'll have to do a second pass.  */
	    check_reloc_sections = true;
	}
    }

  mod->reloc_info = malloc (offsetof (struct dwfl_relocation, refs[nrefs]));
  if (mod->reloc_info == NULL)
    {
      __libdwfl_seterrno (DWFL_E_NOMEM);
      return -1;
    }

  struct secref **sortrefs = alloca (nrefs * sizeof sortrefs[0]);
  for (size_t i = nrefs; i-- > 0; refs = refs->next)
    sortrefs[i] = refs;
  assert (refs == NULL);

  qsort (sortrefs, nrefs, sizeof sortrefs[0], &compare_secrefs);

  mod->reloc_info->count = nrefs;
  for (size_t i = 0; i < nrefs; ++i)
    {
      mod->reloc_info->refs[i].name = sortrefs[i]->name;
      mod->reloc_info->refs[i].scn = sortrefs[i]->scn;
      mod->reloc_info->refs[i].relocs = sortrefs[i]->relocs;
      mod->reloc_info->refs[i].start = sortrefs[i]->start;
      mod->reloc_info->refs[i].end = sortrefs[i]->end;
    }

  if (unlikely (check_reloc_sections))
    {
      /* There was a reloc section that preceded its target section.
	 So we have to scan again now that we have cached all the
	 possible target sections we care about.  */

      scn = NULL;
      while ((scn = elf_nextscn (mod->main.elf, scn)) != NULL)
	{
	  GElf_Shdr shdr_mem;
	  GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
	  if (shdr == NULL)
	    goto elf_error;

      	  if (shdr->sh_size != 0
	      && (shdr->sh_type == SHT_REL || shdr->sh_type == SHT_RELA))
	    {
	      Elf_Scn *tscn = elf_getscn (mod->main.elf, shdr->sh_info);
	      if (likely (tscn != NULL))
		for (size_t i = 0; i < nrefs; ++i)
		  if (mod->reloc_info->refs[i].scn == tscn)
		    {
		      mod->reloc_info->refs[i].relocs = scn;
		      break;
		    }
	    }
	}
    }

  return nrefs;
}


int
dwfl_module_relocations (Dwfl_Module *mod)
{
  if (mod == NULL)
    return -1;

  if (mod->reloc_info != NULL)
    return mod->reloc_info->count;

  switch (mod->e_type)
    {
    case ET_REL:
      return cache_sections (mod);

    case ET_DYN:
      return 1;

    case ET_EXEC:
      assert (mod->main.bias == 0);
      assert (mod->debug.bias == 0);
      break;
    }

  return 0;
}

const char *
dwfl_module_relocation_info (Dwfl_Module *mod, unsigned int idx,
			     Elf32_Word *shndxp)
{
  if (mod == NULL)
    return NULL;

  switch (mod->e_type)
    {
    case ET_REL:
      break;

    case ET_DYN:
      if (idx != 0)
	return NULL;
      if (shndxp)
	*shndxp = SHN_ABS;
      return "";

    default:
      return NULL;
    }

  if (unlikely (mod->reloc_info == NULL) && cache_sections (mod) < 0)
    return NULL;

  struct dwfl_relocation *sections = mod->reloc_info;

  if (idx >= sections->count)
    return NULL;

  if (shndxp)
    *shndxp = elf_ndxscn (sections->refs[idx].scn);

  return sections->refs[idx].name;
}

/* Check that MOD is valid and make sure its relocation has been done.  */
static bool
check_module (Dwfl_Module *mod)
{
  if (INTUSE(dwfl_module_getsymtab) (mod) < 0)
    {
      Dwfl_Error error = dwfl_errno ();
      if (error != DWFL_E_NO_SYMTAB)
	{
	  __libdwfl_seterrno (error);
	  return true;
	}
    }

  if (mod->dw == NULL)
    {
      Dwarf_Addr bias;
      if (INTUSE(dwfl_module_getdwarf) (mod, &bias) == NULL)
	{
	  Dwfl_Error error = dwfl_errno ();
	  if (error != DWFL_E_NO_DWARF)
	    {
	      __libdwfl_seterrno (error);
	      return true;
	    }
	}
    }

  return false;
}

/* Find the index in MOD->reloc_info.refs containing *ADDR.  */
static int
find_section (Dwfl_Module *mod, Dwarf_Addr *addr)
{
  if (unlikely (mod->reloc_info == NULL) && cache_sections (mod) < 0)
    return -1;

  struct dwfl_relocation *sections = mod->reloc_info;

  /* The sections are sorted by address, so we can use binary search.  */
  size_t l = 0, u = sections->count;
  while (l < u)
    {
      size_t idx = (l + u) / 2;
      if (*addr < sections->refs[idx].start)
	u = idx;
      else if (*addr > sections->refs[idx].end)
	l = idx + 1;
      else
	{
	  /* Consider the limit of a section to be inside it, unless it's
	     inside the next one.  A section limit address can appear in
	     line records.  */
	  if (*addr == sections->refs[idx].end
	      && idx < sections->count
	      && *addr == sections->refs[idx + 1].start)
	    ++idx;

	  *addr -= sections->refs[idx].start;
	  return idx;
	}
    }

  __libdwfl_seterrno (DWFL_E (LIBDW, DWARF_E_NO_MATCH));
  return -1;
}

int
dwfl_module_relocate_address (Dwfl_Module *mod, Dwarf_Addr *addr)
{
  if (unlikely (check_module (mod)))
    return -1;

  switch (mod->e_type)
    {
    case ET_REL:
      return find_section (mod, addr);

    case ET_DYN:
      /* All relative to first and only relocation base: module start.  */
      *addr -= mod->low_addr;
      break;

    default:
      /* Already absolute, dwfl_module_relocations returned zero.  We
	 shouldn't really have been called, but it's a harmless no-op.  */
      break;
    }

  return 0;
}
INTDEF (dwfl_module_relocate_address)

Elf_Scn *
dwfl_module_address_section (Dwfl_Module *mod, Dwarf_Addr *address,
			     Dwarf_Addr *bias)
{
  if (check_module (mod))
    return NULL;

  int idx = find_section (mod, address);
  if (idx < 0)
    return NULL;

  if (mod->reloc_info->refs[idx].relocs != NULL)
    {
      assert (mod->e_type == ET_REL);

      Elf_Scn *tscn = mod->reloc_info->refs[idx].scn;
      Elf_Scn *relocscn = mod->reloc_info->refs[idx].relocs;
      Dwfl_Error result = __libdwfl_relocate_section (mod, mod->main.elf,
						      relocscn, tscn, true);
      if (likely (result == DWFL_E_NOERROR))
	mod->reloc_info->refs[idx].relocs = NULL;
      else
	{
	  __libdwfl_seterrno (result);
	  return NULL;
	}
    }

  *bias = mod->main.bias;
  return mod->reloc_info->refs[idx].scn;
}
INTDEF (dwfl_module_address_section)
