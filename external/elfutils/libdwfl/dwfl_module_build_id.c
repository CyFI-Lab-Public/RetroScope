/* Return build ID information for a module.
   Copyright (C) 2007, 2008 Red Hat, Inc.
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

static int
found_build_id (Dwfl_Module *mod, bool set,
		const void *bits, int len, GElf_Addr vaddr)
{
  if (!set)
    /* When checking bits, we do not compare VADDR because the
       address found in a debuginfo file may not match the main
       file as modified by prelink.  */
    return 1 + (mod->build_id_len == len
		&& !memcmp (bits, mod->build_id_bits, len));

  void *copy = malloc (len);
  if (unlikely (copy == NULL))
    {
      __libdwfl_seterrno (DWFL_E_NOMEM);
      return -1;
    }

  mod->build_id_bits = memcpy (copy, bits, len);
  mod->build_id_vaddr = vaddr;
  mod->build_id_len = len;
  return len;
}

#define NO_VADDR	((GElf_Addr) -1l)

static int
check_notes (Dwfl_Module *mod, bool set, Elf_Data *data, GElf_Addr data_vaddr)
{
  size_t pos = 0;
  GElf_Nhdr nhdr;
  size_t name_pos;
  size_t desc_pos;
  while ((pos = gelf_getnote (data, pos, &nhdr, &name_pos, &desc_pos)) > 0)
    if (nhdr.n_type == NT_GNU_BUILD_ID
	&& nhdr.n_namesz == sizeof "GNU" && !memcmp (data->d_buf + name_pos,
						     "GNU", sizeof "GNU"))
      return found_build_id (mod, set,
			     data->d_buf + desc_pos, nhdr.n_descsz,
			     data_vaddr == NO_VADDR ? 0
			     : data_vaddr + desc_pos);
  return 0;
}

int
internal_function
__libdwfl_find_build_id (Dwfl_Module *mod, bool set, Elf *elf)
{
  int result = 0;

  Elf_Scn *scn = elf_nextscn (elf, NULL);

  if (scn == NULL)
    {
      /* No sections, have to look for phdrs.  */
      GElf_Ehdr ehdr_mem;
      GElf_Ehdr *ehdr = gelf_getehdr (elf, &ehdr_mem);
      if (unlikely (ehdr == NULL))
	{
	  __libdwfl_seterrno (DWFL_E_LIBELF);
	  return -1;
	}
      for (uint_fast16_t i = 0; result == 0 && i < ehdr_mem.e_phnum; ++i)
	{
	  GElf_Phdr phdr_mem;
	  GElf_Phdr *phdr = gelf_getphdr (elf, i, &phdr_mem);
	  if (likely (phdr != NULL) && phdr->p_type == PT_NOTE)
	    result = check_notes (mod, set,
				  elf_getdata_rawchunk (elf,
							phdr->p_offset,
							phdr->p_filesz,
							ELF_T_NHDR),
				  phdr->p_vaddr + mod->main.bias);
	}
    }
  else
    do
      {
	GElf_Shdr shdr_mem;
	GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
	if (likely (shdr != NULL) && shdr->sh_type == SHT_NOTE)
	  result = check_notes (mod, set, elf_getdata (scn, NULL),
				(shdr->sh_flags & SHF_ALLOC)
				? shdr->sh_addr + mod->main.bias : NO_VADDR);
      }
    while (result == 0 && (scn = elf_nextscn (elf, scn)) != NULL);

  return result;
}

/* ANDROID_CHANGE_BEGIN */
#if 0
int
__dwfl_module_build_id (Dwfl_Module *mod,
			const unsigned char **bits, GElf_Addr *vaddr)
#else
int
dwfl_module_build_id (Dwfl_Module *mod,
		      const unsigned char **bits, GElf_Addr *vaddr)
#endif
/* ANDROID_CHANGE_END */
{
  if (mod == NULL)
    return -1;

  if (mod->build_id_len == 0 && mod->main.elf != NULL)
    {
      /* We have the file, but have not examined it yet.  */
      int result = __libdwfl_find_build_id (mod, true, mod->main.elf);
      if (result <= 0)
	{
	  mod->build_id_len = -1;	/* Cache negative result.  */
	  return result;
	}
    }

  if (mod->build_id_len <= 0)
    return 0;

  *bits = mod->build_id_bits;
  *vaddr = mod->build_id_vaddr;
  return mod->build_id_len;
}

/* ANDROID_CHANGE_BEGIN */
#if 0
extern __typeof__ (dwfl_module_build_id) INTUSE(dwfl_module_build_id)
     __attribute__ ((alias ("__dwfl_module_build_id")));
asm (".symver "
     "__dwfl_module_build_id, dwfl_module_build_id@@ELFUTILS_0.138");

int
_BUG_COMPAT_dwfl_module_build_id (Dwfl_Module *mod,
				  const unsigned char **bits, GElf_Addr *vaddr)
{
  int result = INTUSE(dwfl_module_build_id) (mod, bits, vaddr);
  if (result > 0)
    *vaddr += (result + 3) & -4;
  return result;
}
asm (".symver "
     "_BUG_COMPAT_dwfl_module_build_id, dwfl_module_build_id@ELFUTILS_0.130");
#endif
/* ANDROID_CHANGE_END */
