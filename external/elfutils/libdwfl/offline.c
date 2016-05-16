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
#include <fcntl.h>
#include <unistd.h>

/* Since dwfl_report_elf lays out the sections already, this will only be
   called when the section headers of the debuginfo file are being
   consulted instead, or for the section placed at 0.  With binutils
   strip-to-debug, the symbol table is in the debuginfo file and relocation
   looks there.  */
int
dwfl_offline_section_address (Dwfl_Module *mod,
			      void **userdata __attribute__ ((unused)),
			      const char *modname __attribute__ ((unused)),
			      Dwarf_Addr base __attribute__ ((unused)),
			      const char *secname __attribute__ ((unused)),
			      Elf32_Word shndx,
			      const GElf_Shdr *shdr __attribute__ ((unused)),
			      Dwarf_Addr *addr)
{
  assert (mod->e_type == ET_REL);
  assert (shdr->sh_addr == 0);
  assert (shdr->sh_flags & SHF_ALLOC);

  if (mod->debug.elf == NULL)
    /* We are only here because sh_addr is zero even though layout is complete.
       The first section in the first file under -e is placed at 0.  */
    return 0;

  /* The section numbers might not match between the two files.
     The best we can rely on is the order of SHF_ALLOC sections.  */

  Elf_Scn *ourscn = elf_getscn (mod->debug.elf, shndx);
  Elf_Scn *scn = NULL;
  uint_fast32_t skip_alloc = 0;
  while ((scn = elf_nextscn (mod->debug.elf, scn)) != ourscn)
    {
      assert (scn != NULL);
      GElf_Shdr shdr_mem;
      GElf_Shdr *sh = gelf_getshdr (scn, &shdr_mem);
      if (unlikely (sh == NULL))
	return -1;
      if (sh->sh_flags & SHF_ALLOC)
	++skip_alloc;
    }

  scn = NULL;
  while ((scn = elf_nextscn (mod->main.elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *main_shdr = gelf_getshdr (scn, &shdr_mem);
      if (unlikely (main_shdr == NULL))
	return -1;
      if ((main_shdr->sh_flags & SHF_ALLOC) && skip_alloc-- == 0)
	{
	  assert (main_shdr->sh_flags == shdr->sh_flags);
	  *addr = main_shdr->sh_addr;
	  return 0;
	}
    }

  /* This should never happen.  */
  return -1;
}
INTDEF (dwfl_offline_section_address)

/* Forward declarations.  */
static Dwfl_Module *process_elf (Dwfl *dwfl, const char *name,
				 const char *file_name, int fd, Elf *elf);
static Dwfl_Module *process_archive (Dwfl *dwfl, const char *name,
				     const char *file_name, int fd, Elf *elf,
				     int (*predicate) (const char *module,
						       const char *file));

/* Report one module for an ELF file, or many for an archive.
   Always consumes ELF and FD.  */
static Dwfl_Module *
process_file (Dwfl *dwfl, const char *name, const char *file_name, int fd,
	      Elf *elf, int (*predicate) (const char *module,
					  const char *file))
{
  switch (elf_kind (elf))
    {
    default:
    case ELF_K_NONE:
      __libdwfl_seterrno (elf == NULL ? DWFL_E_LIBELF : DWFL_E_BADELF);
      return NULL;

    case ELF_K_ELF:
      return process_elf (dwfl, name, file_name, fd, elf);

    case ELF_K_AR:
      return process_archive (dwfl, name, file_name, fd, elf, predicate);
    }
}

/* Report the open ELF file as a module.  Always consumes ELF and FD.  */
static Dwfl_Module *
process_elf (Dwfl *dwfl, const char *name, const char *file_name, int fd,
	     Elf *elf)
{
  Dwfl_Module *mod = __libdwfl_report_elf (dwfl, name, file_name, fd, elf,
					   dwfl->offline_next_address);
  if (mod != NULL)
    {
      /* If this is an ET_EXEC file with fixed addresses, the address range
	 it consumed may or may not intersect with the arbitrary range we
	 will use for relocatable modules.  Make sure we always use a free
	 range for the offline allocations.  If this module did use
	 offline_next_address, it may have rounded it up for the module's
	 alignment requirements.  */
      if ((dwfl->offline_next_address >= mod->low_addr
	   || mod->low_addr - dwfl->offline_next_address < OFFLINE_REDZONE)
	  && dwfl->offline_next_address < mod->high_addr + OFFLINE_REDZONE)
	dwfl->offline_next_address = mod->high_addr + OFFLINE_REDZONE;

      /* Don't keep the file descriptor around.  */
      if (mod->main.fd != -1 && elf_cntl (mod->main.elf, ELF_C_FDREAD) == 0)
	{
	  close (mod->main.fd);
	  mod->main.fd = -1;
	}
    }

  return mod;
}

/* Always consumes MEMBER.  Returns elf_next result on success.
   For errors returns ELF_C_NULL with *MOD set to null.  */
static Elf_Cmd
process_archive_member (Dwfl *dwfl, const char *name, const char *file_name,
			int (*predicate) (const char *module, const char *file),
			int fd, Elf *member, Dwfl_Module **mod)
{
  const Elf_Arhdr *h = elf_getarhdr (member);
  if (unlikely (h == NULL))
    {
      __libdwfl_seterrno (DWFL_E_LIBELF);
    fail:
      elf_end (member);
      *mod = NULL;
      return ELF_C_NULL;
    }

  if (!strcmp (h->ar_name, "/") || !strcmp (h->ar_name, "//"))
    {
    skip:;
      /* Skip this and go to the next.  */
      Elf_Cmd result = elf_next (member);
      elf_end (member);
      return result;
    }

  char *member_name;
  if (unlikely (asprintf (&member_name, "%s(%s)", file_name, h->ar_name) < 0))
    {
    nomem:
      __libdwfl_seterrno (DWFL_E_NOMEM);
      elf_end (member);
      *mod = NULL;
      return ELF_C_NULL;
    }

  char *module_name = NULL;
  if (name == NULL || name[0] == '\0')
    name = h->ar_name;
  else if (unlikely (asprintf (&module_name, "%s:%s", name, h->ar_name) < 0))
    {
      free (member_name);
      goto nomem;
    }
  else
    name = module_name;

  if (predicate != NULL)
    {
      /* Let the predicate decide whether to use this one.  */
      int want = (*predicate) (name, member_name);
      if (want <= 0)
	{
	  free (member_name);
	  free (module_name);
	  if (unlikely (want < 0))
	    {
	      __libdwfl_seterrno (DWFL_E_CB);
	      goto fail;
	    }
	  goto skip;
	}
    }

  /* We let __libdwfl_report_elf cache the fd in mod->main.fd,
     though it's the same fd for all the members.
     On module teardown we will close it only on the last Elf reference.  */
  *mod = process_file (dwfl, name, member_name, fd, member, predicate);
  free (member_name);
  free (module_name);

  if (*mod == NULL)		/* process_file called elf_end.  */
    return ELF_C_NULL;

  /* Advance the archive-reading offset for the next iteration.  */
  return elf_next (member);
}

/* Report each member of the archive as its own module.  */
static Dwfl_Module *
process_archive (Dwfl *dwfl, const char *name, const char *file_name, int fd,
		 Elf *archive,
		 int (*predicate) (const char *module, const char *file))

{
  Dwfl_Module *mod = NULL;
  Elf *member = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, archive);
  if (unlikely (member == NULL)) /* Empty archive.  */
    {
      __libdwfl_seterrno (DWFL_E_BADELF);
      return NULL;
    }

  while (process_archive_member (dwfl, name, file_name, predicate,
				 fd, member, &mod) != ELF_C_NULL)
    member = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, archive);

  /* We can drop the archive Elf handle even if we're still using members
     in live modules.  When the last module's elf_end on a member returns
     zero, that module will close FD.  If no modules survived the predicate,
     we are all done with the file right here.  */
  if (mod != NULL		/* If no modules, caller will clean up.  */
      && elf_end (archive) == 0)
    close (fd);

  return mod;
}

Dwfl_Module *
internal_function
__libdwfl_report_offline (Dwfl *dwfl, const char *name,
			  const char *file_name, int fd, bool closefd,
			  int (*predicate) (const char *module,
					    const char *file))
{
  Elf *elf = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, NULL);
  Dwfl_Module *mod = process_file (dwfl, name, file_name, fd, elf, predicate);
  if (mod == NULL)
    {
      elf_end (elf);
      if (closefd)
	close (fd);
    }
  return mod;
}

Dwfl_Module *
dwfl_report_offline (Dwfl *dwfl, const char *name,
		     const char *file_name, int fd)
{
  if (dwfl == NULL)
    return NULL;

  bool closefd = false;
  if (fd < 0)
    {
      closefd = true;
      fd = open64 (file_name, O_RDONLY);
      if (fd < 0)
	{
	  __libdwfl_seterrno (DWFL_E_ERRNO);
	  return NULL;
	}
    }

  return __libdwfl_report_offline (dwfl, name, file_name, fd, closefd, NULL);
}
INTDEF (dwfl_report_offline)
