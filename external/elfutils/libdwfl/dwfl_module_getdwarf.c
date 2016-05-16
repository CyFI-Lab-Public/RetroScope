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
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include "../libdw/libdwP.h"	/* DWARF_E_* values are here.  */

/* Open libelf FILE->fd and compute the load base of ELF as loaded in MOD.
   When we return success, FILE->elf and FILE->bias are set up.  */
static inline Dwfl_Error
open_elf (Dwfl_Module *mod, struct dwfl_file *file)
{
  if (file->elf == NULL)
    {
      /* If there was a pre-primed file name left that the callback left
	 behind, try to open that file name.  */
      if (file->fd < 0 && file->name != NULL)
	file->fd = TEMP_FAILURE_RETRY (open64 (file->name, O_RDONLY));

      if (file->fd < 0)
	return CBFAIL;

      file->elf = elf_begin (file->fd, ELF_C_READ_MMAP_PRIVATE, NULL);
    }

  if (unlikely (elf_kind (file->elf) != ELF_K_ELF))
    {
      close (file->fd);
      file->fd = -1;
      return DWFL_E_BADELF;
    }

  GElf_Ehdr ehdr_mem, *ehdr = gelf_getehdr (file->elf, &ehdr_mem);
  if (ehdr == NULL)
    {
    elf_error:
      close (file->fd);
      file->fd = -1;
      return DWFL_E (LIBELF, elf_errno ());
    }

  /* The addresses in an ET_EXEC file are absolute.  The lowest p_vaddr of
     the main file can differ from that of the debug file due to prelink.
     But that doesn't not change addresses that symbols, debuginfo, or
     sh_addr of any program sections refer to.  */
  file->bias = 0;
  if (mod->e_type != ET_EXEC)
    for (uint_fast16_t i = 0; i < ehdr->e_phnum; ++i)
      {
	GElf_Phdr ph_mem;
	GElf_Phdr *ph = gelf_getphdr (file->elf, i, &ph_mem);
	if (ph == NULL)
	  goto elf_error;
	if (ph->p_type == PT_LOAD)
	  {
	    file->bias = ((mod->low_addr & -ph->p_align)
			  - (ph->p_vaddr & -ph->p_align));
	    break;
	  }
      }

  mod->e_type = ehdr->e_type;

  /* Relocatable Linux kernels are ET_EXEC but act like ET_DYN.  */
  if (mod->e_type == ET_EXEC && file->bias != 0)
    mod->e_type = ET_DYN;

  return DWFL_E_NOERROR;
}

/* Find the main ELF file for this module and open libelf on it.
   When we return success, MOD->main.elf and MOD->main.bias are set up.  */
static void
find_file (Dwfl_Module *mod)
{
  if (mod->main.elf != NULL	/* Already done.  */
      || mod->elferr != DWFL_E_NOERROR)	/* Cached failure.  */
    return;

  mod->main.fd = (*mod->dwfl->callbacks->find_elf) (MODCB_ARGS (mod),
						    &mod->main.name,
						    &mod->main.elf);
  mod->elferr = open_elf (mod, &mod->main);

  if (mod->elferr == DWFL_E_NOERROR && !mod->main.valid)
    {
      /* Clear any explicitly reported build ID, just in case it was wrong.
	 We'll fetch it from the file when asked.  */
      free (mod->build_id_bits);
      mod->build_id_bits = NULL;
      mod->build_id_len = 0;
    }
}

/* Search an ELF file for a ".gnu_debuglink" section.  */
static const char *
find_debuglink (Elf *elf, GElf_Word *crc)
{
  size_t shstrndx;
  if (elf_getshstrndx (elf, &shstrndx) < 0)
    return NULL;

  Elf_Scn *scn = NULL;
  while ((scn = elf_nextscn (elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
      if (shdr == NULL)
	return NULL;

      const char *name = elf_strptr (elf, shstrndx, shdr->sh_name);
      if (name == NULL)
	return NULL;

      if (!strcmp (name, ".gnu_debuglink"))
	break;
    }

  if (scn == NULL)
    return NULL;

  /* Found the .gnu_debuglink section.  Extract its contents.  */
  Elf_Data *rawdata = elf_rawdata (scn, NULL);
  if (rawdata == NULL)
    return NULL;

  Elf_Data crcdata =
    {
      .d_type = ELF_T_WORD,
      .d_buf = crc,
      .d_size = sizeof *crc,
      .d_version = EV_CURRENT,
    };
  Elf_Data conv =
    {
      .d_type = ELF_T_WORD,
      .d_buf = rawdata->d_buf + rawdata->d_size - sizeof *crc,
      .d_size = sizeof *crc,
      .d_version = EV_CURRENT,
    };

  GElf_Ehdr ehdr_mem;
  GElf_Ehdr *ehdr = gelf_getehdr (elf, &ehdr_mem);
  if (ehdr == NULL)
    return NULL;

  Elf_Data *d = gelf_xlatetom (elf, &crcdata, &conv, ehdr->e_ident[EI_DATA]);
  if (d == NULL)
    return NULL;
  assert (d == &crcdata);

  return rawdata->d_buf;
}


/* Find the separate debuginfo file for this module and open libelf on it.
   When we return success, MOD->debug is set up.  */
static Dwfl_Error
find_debuginfo (Dwfl_Module *mod)
{
  if (mod->debug.elf != NULL)
    return DWFL_E_NOERROR;

  GElf_Word debuglink_crc = 0;
  const char *debuglink_file = find_debuglink (mod->main.elf, &debuglink_crc);

  mod->debug.fd = (*mod->dwfl->callbacks->find_debuginfo) (MODCB_ARGS (mod),
							   mod->main.name,
							   debuglink_file,
							   debuglink_crc,
							   &mod->debug.name);
  return open_elf (mod, &mod->debug);
}


/* Try to find a symbol table in FILE.
   Returns DWFL_E_NOERROR if a proper one is found.
   Returns DWFL_E_NO_SYMTAB if not, but still sets results for SHT_DYNSYM.  */
static Dwfl_Error
load_symtab (struct dwfl_file *file, struct dwfl_file **symfile,
	     Elf_Scn **symscn, Elf_Scn **xndxscn,
	     size_t *syments, GElf_Word *strshndx)
{
  bool symtab = false;
  Elf_Scn *scn = NULL;
  while ((scn = elf_nextscn (file->elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem, *shdr = gelf_getshdr (scn, &shdr_mem);
      if (shdr != NULL)
	switch (shdr->sh_type)
	  {
	  case SHT_SYMTAB:
	    symtab = true;
	    *symscn = scn;
	    *symfile = file;
	    *strshndx = shdr->sh_link;
	    *syments = shdr->sh_size / shdr->sh_entsize;
	    if (*xndxscn != NULL)
	      return DWFL_E_NOERROR;
	    break;

	  case SHT_DYNSYM:
	    if (symtab)
	      break;
	    /* Use this if need be, but keep looking for SHT_SYMTAB.  */
	    *symscn = scn;
	    *symfile = file;
	    *strshndx = shdr->sh_link;
	    *syments = shdr->sh_size / shdr->sh_entsize;
	    break;

	  case SHT_SYMTAB_SHNDX:
	    *xndxscn = scn;
	    if (symtab)
	      return DWFL_E_NOERROR;
	    break;

	  default:
	    break;
	  }
    }

  if (symtab)
    /* We found one, though no SHT_SYMTAB_SHNDX to go with it.  */
    return DWFL_E_NOERROR;

  /* We found no SHT_SYMTAB, so any SHT_SYMTAB_SHNDX was bogus.
     We might have found an SHT_DYNSYM and set *SYMSCN et al though.  */
  *xndxscn = NULL;
  return DWFL_E_NO_SYMTAB;
}


/* Translate addresses into file offsets.
   OFFS[*] start out zero and remain zero if unresolved.  */
static void
find_offsets (Elf *elf, const GElf_Ehdr *ehdr, size_t n,
	      GElf_Addr addrs[n], GElf_Off offs[n])
{
  size_t unsolved = n;
  for (uint_fast16_t i = 0; i < ehdr->e_phnum; ++i)
    {
      GElf_Phdr phdr_mem;
      GElf_Phdr *phdr = gelf_getphdr (elf, i, &phdr_mem);
      if (phdr != NULL && phdr->p_type == PT_LOAD && phdr->p_memsz > 0)
	for (size_t j = 0; j < n; ++j)
	  if (offs[j] == 0
	      && addrs[j] >= phdr->p_vaddr
	      && addrs[j] - phdr->p_vaddr < phdr->p_filesz)
	    {
	      offs[j] = addrs[j] - phdr->p_vaddr + phdr->p_offset;
	      if (--unsolved == 0)
		break;
	    }
    }
}

/* Try to find a dynamic symbol table via phdrs.  */
static void
find_dynsym (Dwfl_Module *mod)
{
  GElf_Ehdr ehdr_mem;
  GElf_Ehdr *ehdr = gelf_getehdr (mod->main.elf, &ehdr_mem);

  for (uint_fast16_t i = 0; i < ehdr->e_phnum; ++i)
    {
      GElf_Phdr phdr_mem;
      GElf_Phdr *phdr = gelf_getphdr (mod->main.elf, i, &phdr_mem);
      if (phdr == NULL)
	break;

      if (phdr->p_type == PT_DYNAMIC)
	{
	  /* Examine the dynamic section for the pointers we need.  */

	  Elf_Data *data = elf_getdata_rawchunk (mod->main.elf,
						 phdr->p_offset, phdr->p_filesz,
						 ELF_T_DYN);
	  if (data == NULL)
	    continue;

	  enum
	    {
	      i_symtab,
	      i_strtab,
	      i_hash,
	      i_gnu_hash,
	      i_max
	    };
	  GElf_Addr addrs[i_max] = { 0, };
	  GElf_Xword strsz = 0;
	  size_t n = data->d_size / gelf_fsize (mod->main.elf,
						ELF_T_DYN, 1, EV_CURRENT);
	  for (size_t j = 0; j < n; ++j)
	    {
	      GElf_Dyn dyn_mem;
	      GElf_Dyn *dyn = gelf_getdyn (data, j, &dyn_mem);
	      if (dyn != NULL)
		switch (dyn->d_tag)
		  {
		  case DT_SYMTAB:
		    addrs[i_symtab] = dyn->d_un.d_ptr;
		    continue;

		  case DT_HASH:
		    addrs[i_hash] = dyn->d_un.d_ptr;
		    continue;

		  case DT_GNU_HASH:
		    addrs[i_gnu_hash] = dyn->d_un.d_ptr;
		    continue;

		  case DT_STRTAB:
		    addrs[i_strtab] = dyn->d_un.d_ptr;
		    continue;

		  case DT_STRSZ:
		    strsz = dyn->d_un.d_val;
		    continue;

		  default:
		    continue;

		  case DT_NULL:
		    break;
		  }
	      break;
	    }

	  /* Translate pointers into file offsets.  */
	  GElf_Off offs[i_max] = { 0, };
	  find_offsets (mod->main.elf, ehdr, i_max, addrs, offs);

	  /* Figure out the size of the symbol table.  */
	  if (offs[i_hash] != 0)
	    {
	      /* In the original format, .hash says the size of .dynsym.  */

	      size_t entsz = SH_ENTSIZE_HASH (ehdr);
	      data = elf_getdata_rawchunk (mod->main.elf,
					   offs[i_hash] + entsz, entsz,
					   entsz == 4 ? ELF_T_WORD
					   : ELF_T_XWORD);
	      if (data != NULL)
		mod->syments = (entsz == 4
				? *(const GElf_Word *) data->d_buf
				: *(const GElf_Xword *) data->d_buf);
	    }
	  if (offs[i_gnu_hash] != 0 && mod->syments == 0)
	    {
	      /* In the new format, we can derive it with some work.  */

	      const struct
	      {
		Elf32_Word nbuckets;
		Elf32_Word symndx;
		Elf32_Word maskwords;
		Elf32_Word shift2;
	      } *header;

	      data = elf_getdata_rawchunk (mod->main.elf, offs[i_gnu_hash],
					   sizeof *header, ELF_T_WORD);
	      if (data != NULL)
		{
		  header = data->d_buf;
		  Elf32_Word nbuckets = header->nbuckets;
		  Elf32_Word symndx = header->symndx;
		  GElf_Off buckets_at = (offs[i_gnu_hash] + sizeof *header
					 + (gelf_getclass (mod->main.elf)
					    * sizeof (Elf32_Word)
					    * header->maskwords));

		  data = elf_getdata_rawchunk (mod->main.elf, buckets_at,
					       nbuckets * sizeof (Elf32_Word),
					       ELF_T_WORD);
		  if (data != NULL && symndx < nbuckets)
		    {
		      const Elf32_Word *const buckets = data->d_buf;
		      Elf32_Word maxndx = symndx;
		      for (Elf32_Word bucket = 0; bucket < nbuckets; ++bucket)
			if (buckets[bucket] > maxndx)
			  maxndx = buckets[bucket];

		      GElf_Off hasharr_at = (buckets_at
					     + nbuckets * sizeof (Elf32_Word));
		      hasharr_at += (maxndx - symndx) * sizeof (Elf32_Word);
		      do
			{
			  data = elf_getdata_rawchunk (mod->main.elf,
						       hasharr_at,
						       sizeof (Elf32_Word),
						       ELF_T_WORD);
			  if (data != NULL
			      && (*(const Elf32_Word *) data->d_buf & 1u))
			    {
			      mod->syments = maxndx + 1;
			      break;
			    }
			  ++maxndx;
			  hasharr_at += sizeof (Elf32_Word);
			} while (data != NULL);
		    }
		}
	    }
	  if (offs[i_strtab] > offs[i_symtab] && mod->syments == 0)
	    mod->syments = ((offs[i_strtab] - offs[i_symtab])
			    / gelf_fsize (mod->main.elf,
					  ELF_T_SYM, 1, EV_CURRENT));

	  if (mod->syments > 0)
	    {
	      mod->symdata = elf_getdata_rawchunk (mod->main.elf,
						   offs[i_symtab],
						   gelf_fsize (mod->main.elf,
							       ELF_T_SYM,
							       mod->syments,
							       EV_CURRENT),
						   ELF_T_SYM);
	      if (mod->symdata != NULL)
		{
		  mod->symstrdata = elf_getdata_rawchunk (mod->main.elf,
							  offs[i_strtab],
							  strsz,
							  ELF_T_BYTE);
		  if (mod->symstrdata == NULL)
		    mod->symdata = NULL;
		}
	      if (mod->symdata == NULL)
		mod->symerr = DWFL_E (LIBELF, elf_errno ());
	      else
		{
		  mod->symfile = &mod->main;
		  mod->symerr = DWFL_E_NOERROR;
		}
	      return;
	    }
	}
    }
}

/* Try to find a symbol table in either MOD->main.elf or MOD->debug.elf.  */
static void
find_symtab (Dwfl_Module *mod)
{
  if (mod->symdata != NULL	/* Already done.  */
      || mod->symerr != DWFL_E_NOERROR) /* Cached previous failure.  */
    return;

  find_file (mod);
  mod->symerr = mod->elferr;
  if (mod->symerr != DWFL_E_NOERROR)
    return;

  /* First see if the main ELF file has the debugging information.  */
  Elf_Scn *symscn = NULL, *xndxscn = NULL;
  GElf_Word strshndx = 0;
  mod->symerr = load_symtab (&mod->main, &mod->symfile, &symscn,
			     &xndxscn, &mod->syments, &strshndx);
  switch (mod->symerr)
    {
    default:
      return;

    case DWFL_E_NOERROR:
      break;

    case DWFL_E_NO_SYMTAB:
      /* Now we have to look for a separate debuginfo file.  */
      mod->symerr = find_debuginfo (mod);
      switch (mod->symerr)
	{
	default:
	  return;

	case DWFL_E_NOERROR:
	  mod->symerr = load_symtab (&mod->debug, &mod->symfile, &symscn,
				     &xndxscn, &mod->syments, &strshndx);
	  break;

	case DWFL_E_CB:		/* The find_debuginfo hook failed.  */
	  mod->symerr = DWFL_E_NO_SYMTAB;
	  break;
	}

      switch (mod->symerr)
	{
	default:
	  return;

	case DWFL_E_NOERROR:
	  break;

	case DWFL_E_NO_SYMTAB:
	  if (symscn != NULL)
	    {
	      /* We still have the dynamic symbol table.  */
	      mod->symerr = DWFL_E_NOERROR;
	      break;
	    }

	  /* Last ditch, look for dynamic symbols without section headers.  */
	  find_dynsym (mod);
	  return;
	}
      break;
    }

  /* This does some sanity checks on the string table section.  */
  if (elf_strptr (mod->symfile->elf, strshndx, 0) == NULL)
    {
    elferr:
      mod->symerr = DWFL_E (LIBELF, elf_errno ());
      return;
    }

  /* Cache the data; MOD->syments was set above.  */

  mod->symstrdata = elf_getdata (elf_getscn (mod->symfile->elf, strshndx),
				 NULL);
  if (mod->symstrdata == NULL)
    goto elferr;

  if (xndxscn == NULL)
    mod->symxndxdata = NULL;
  else
    {
      mod->symxndxdata = elf_getdata (xndxscn, NULL);
      if (mod->symxndxdata == NULL)
	goto elferr;
    }

  mod->symdata = elf_getdata (symscn, NULL);
  if (mod->symdata == NULL)
    goto elferr;
}


/* Try to open a libebl backend for MOD.  */
Dwfl_Error
internal_function
__libdwfl_module_getebl (Dwfl_Module *mod)
{
  if (mod->ebl == NULL)
    {
      find_file (mod);
      if (mod->elferr != DWFL_E_NOERROR)
	return mod->elferr;

      mod->ebl = ebl_openbackend (mod->main.elf);
      if (mod->ebl == NULL)
	return DWFL_E_LIBEBL;
    }
  return DWFL_E_NOERROR;
}

/* Try to start up libdw on DEBUGFILE.  */
static Dwfl_Error
load_dw (Dwfl_Module *mod, struct dwfl_file *debugfile)
{
  if (mod->e_type == ET_REL && !debugfile->relocated)
    {
      const Dwfl_Callbacks *const cb = mod->dwfl->callbacks;

      /* The debugging sections have to be relocated.  */
      if (cb->section_address == NULL)
	return DWFL_E_NOREL;

      Dwfl_Error error = __libdwfl_module_getebl (mod);
      if (error != DWFL_E_NOERROR)
	return error;

      find_symtab (mod);
      Dwfl_Error result = mod->symerr;
      if (result == DWFL_E_NOERROR)
	result = __libdwfl_relocate (mod, debugfile->elf, true);
      if (result != DWFL_E_NOERROR)
	return result;

      /* Don't keep the file descriptors around.  */
      if (mod->main.fd != -1 && elf_cntl (mod->main.elf, ELF_C_FDREAD) == 0)
	{
	  close (mod->main.fd);
	  mod->main.fd = -1;
	}
      if (debugfile->fd != -1 && elf_cntl (debugfile->elf, ELF_C_FDREAD) == 0)
	{
	  close (debugfile->fd);
	  debugfile->fd = -1;
	}
    }

  mod->dw = INTUSE(dwarf_begin_elf) (debugfile->elf, DWARF_C_READ, NULL);
  if (mod->dw == NULL)
    {
      int err = INTUSE(dwarf_errno) ();
      return err == DWARF_E_NO_DWARF ? DWFL_E_NO_DWARF : DWFL_E (LIBDW, err);
    }

  /* Until we have iterated through all CU's, we might do lazy lookups.  */
  mod->lazycu = 1;

  return DWFL_E_NOERROR;
}

/* Try to start up libdw on either the main file or the debuginfo file.  */
static void
find_dw (Dwfl_Module *mod)
{
  if (mod->dw != NULL		/* Already done.  */
      || mod->dwerr != DWFL_E_NOERROR) /* Cached previous failure.  */
    return;

  find_file (mod);
  mod->dwerr = mod->elferr;
  if (mod->dwerr != DWFL_E_NOERROR)
    return;

  /* First see if the main ELF file has the debugging information.  */
  mod->dwerr = load_dw (mod, &mod->main);
  switch (mod->dwerr)
    {
    case DWFL_E_NOERROR:
      mod->debug.elf = mod->main.elf;
      mod->debug.bias = mod->main.bias;
      return;

    case DWFL_E_NO_DWARF:
      break;

    default:
      goto canonicalize;
    }

  /* Now we have to look for a separate debuginfo file.  */
  mod->dwerr = find_debuginfo (mod);
  switch (mod->dwerr)
    {
    case DWFL_E_NOERROR:
      mod->dwerr = load_dw (mod, &mod->debug);
      break;

    case DWFL_E_CB:		/* The find_debuginfo hook failed.  */
      mod->dwerr = DWFL_E_NO_DWARF;
      return;

    default:
      break;
    }

 canonicalize:
  mod->dwerr = __libdwfl_canon_error (mod->dwerr);
}


Elf *
dwfl_module_getelf (Dwfl_Module *mod, GElf_Addr *loadbase)
{
  if (mod == NULL)
    return NULL;

  find_file (mod);
  if (mod->elferr == DWFL_E_NOERROR)
    {
      if (mod->e_type == ET_REL && ! mod->main.relocated)
	{
	  /* Before letting them get at the Elf handle,
	     apply all the relocations we know how to.  */

	  mod->main.relocated = true;
	  if (likely (__libdwfl_module_getebl (mod) == DWFL_E_NOERROR))
	    {
	      (void) __libdwfl_relocate (mod, mod->main.elf, false);

	      if (mod->debug.elf == mod->main.elf)
		mod->debug.relocated = true;
	      else if (mod->debug.elf != NULL && ! mod->debug.relocated)
		{
		  mod->debug.relocated = true;
		  (void) __libdwfl_relocate (mod, mod->debug.elf, false);
		}
	    }
	}

      *loadbase = mod->main.bias;
      return mod->main.elf;
    }

  __libdwfl_seterrno (mod->elferr);
  return NULL;
}
INTDEF (dwfl_module_getelf)


Dwarf *
dwfl_module_getdwarf (Dwfl_Module *mod, Dwarf_Addr *bias)
{
  if (mod == NULL)
    return NULL;

  find_dw (mod);
  if (mod->dwerr == DWFL_E_NOERROR)
    {
      /* If dwfl_module_getelf was used previously, then partial apply
	 relocation to miscellaneous sections in the debug file too.  */
      if (mod->e_type == ET_REL
	  && mod->main.relocated && ! mod->debug.relocated)
	{
	  mod->debug.relocated = true;
	  if (mod->debug.elf != mod->main.elf)
	    (void) __libdwfl_relocate (mod, mod->debug.elf, false);
	}

      *bias = mod->debug.bias;
      return mod->dw;
    }

  __libdwfl_seterrno (mod->dwerr);
  return NULL;
}
INTDEF (dwfl_module_getdwarf)

int
dwfl_module_getsymtab (Dwfl_Module *mod)
{
  if (mod == NULL)
    return -1;

  find_symtab (mod);
  if (mod->symerr == DWFL_E_NOERROR)
    return mod->syments;

  __libdwfl_seterrno (mod->symerr);
  return -1;
}
INTDEF (dwfl_module_getsymtab)
