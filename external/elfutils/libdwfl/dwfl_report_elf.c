/* Report a module to libdwfl based on ELF program headers.
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

#include "libdwflP.h"
#include <fcntl.h>
#include <unistd.h>


/* We start every ET_REL module at a moderately aligned boundary.
   This keeps the low addresses easy to read compared to a layout
   starting at 0 (as when using -e).  It also makes it unlikely
   that a middle section will have a larger alignment and require
   rejiggering (see below).  */
#define REL_MIN_ALIGN	((GElf_Xword) 0x100)

Dwfl_Module *
internal_function
__libdwfl_report_elf (Dwfl *dwfl, const char *name, const char *file_name,
		      int fd, Elf *elf, GElf_Addr base)
{
  GElf_Ehdr ehdr_mem, *ehdr = gelf_getehdr (elf, &ehdr_mem);
  if (ehdr == NULL)
    {
    elf_error:
      __libdwfl_seterrno (DWFL_E_LIBELF);
      return NULL;
    }

  GElf_Addr start = 0, end = 0, bias = 0;
  switch (ehdr->e_type)
    {
    case ET_REL:
      /* For a relocatable object, we do an arbitrary section layout.
	 By updating the section header in place, we leave the layout
	 information to be found by relocation.  */

      start = end = base = (base + REL_MIN_ALIGN - 1) & -REL_MIN_ALIGN;

      bool first = true;
      Elf_Scn *scn = NULL;
      while ((scn = elf_nextscn (elf, scn)) != NULL)
	{
	  GElf_Shdr shdr_mem;
	  GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
	  if (unlikely (shdr == NULL))
	    goto elf_error;

	  if (shdr->sh_flags & SHF_ALLOC)
	    {
	      const GElf_Xword align = shdr->sh_addralign ?: 1;
	      const GElf_Addr next = (end + align - 1) & -align;
	      if (shdr->sh_addr == 0
		  /* Once we've started doing layout we have to do it all,
		     unless we just layed out the first section at 0 when
		     it already was at 0.  */
		  || (bias == 0 && end > start && end != next))
		{
		  shdr->sh_addr = next;
		  if (end == base)
		    /* This is the first section assigned a location.
		       Use its aligned address as the module's base.  */
		    start = base = shdr->sh_addr;
		  else if (unlikely (base & (align - 1)))
		    {
		      /* If BASE has less than the maximum alignment of
			 any section, we eat more than the optimal amount
			 of padding and so make the module's apparent
			 size come out larger than it would when placed
			 at zero.  So reset the layout with a better base.  */

		      start = end = base = (base + align - 1) & -align;
		      Elf_Scn *prev_scn = NULL;
		      do
			{
			  prev_scn = elf_nextscn (elf, prev_scn);
			  GElf_Shdr prev_shdr_mem;
			  GElf_Shdr *prev_shdr = gelf_getshdr (prev_scn,
							       &prev_shdr_mem);
			  if (unlikely (prev_shdr == NULL))
			    goto elf_error;
			  if (prev_shdr->sh_flags & SHF_ALLOC)
			    {
			      const GElf_Xword prev_align
				= prev_shdr->sh_addralign ?: 1;

			      prev_shdr->sh_addr
				= (end + prev_align - 1) & -prev_align;
			      end = prev_shdr->sh_addr + prev_shdr->sh_size;

			      if (unlikely (! gelf_update_shdr (prev_scn,
								prev_shdr)))
				goto elf_error;
			    }
			}
		      while (prev_scn != scn);
		      continue;
		    }

		  end = shdr->sh_addr + shdr->sh_size;
		  if (likely (shdr->sh_addr != 0)
		      && unlikely (! gelf_update_shdr (scn, shdr)))
		    goto elf_error;
		}
	      else
		{
		  /* The address is already assigned.  Just track it.  */
		  if (first || end < shdr->sh_addr + shdr->sh_size)
		    end = shdr->sh_addr + shdr->sh_size;
		  if (first || bias > shdr->sh_addr)
		    /* This is the lowest address in the module.  */
		    bias = shdr->sh_addr;

		  if ((shdr->sh_addr - bias + base) & (align - 1))
		    /* This section winds up misaligned using BASE.
		       Adjust BASE upwards to make it congruent to
		       the lowest section address in the file modulo ALIGN.  */
		    base = (((base + align - 1) & -align)
			    + (bias & (align - 1)));
		}

	      first = false;
	    }
	}

      if (bias != 0)
	{
	  /* The section headers had nonzero sh_addr values.  The layout
	     was already done.  We've just collected the total span.
	     Now just compute the bias from the requested base.  */
	  start = base;
	  end = end - bias + start;
	  bias = start - bias;
	}
      break;

      /* Everything else has to have program headers.  */

    case ET_EXEC:
    case ET_CORE:
      /* An assigned base address is meaningless for these.  */
      base = 0;

    case ET_DYN:
    default:
      for (uint_fast16_t i = 0; i < ehdr->e_phnum; ++i)
	{
	  GElf_Phdr phdr_mem, *ph = gelf_getphdr (elf, i, &phdr_mem);
	  if (unlikely (ph == NULL))
	    goto elf_error;
	  if (ph->p_type == PT_LOAD)
	    {
	      if ((base & (ph->p_align - 1)) != 0)
		base = (base + ph->p_align - 1) & -ph->p_align;
	      start = base + (ph->p_vaddr & -ph->p_align);
	      break;
	    }
	}
      bias = base;

      for (uint_fast16_t i = ehdr->e_phnum; i-- > 0;)
	{
	  GElf_Phdr phdr_mem, *ph = gelf_getphdr (elf, i, &phdr_mem);
	  if (unlikely (ph == NULL))
	    goto elf_error;
	  if (ph->p_type == PT_LOAD)
	    {
	      end = base + (ph->p_vaddr + ph->p_memsz);
	      break;
	    }
	}

      if (end == 0)
	{
	  __libdwfl_seterrno (DWFL_E_NO_PHDR);
	  return NULL;
	}
      break;
    }

  Dwfl_Module *m = INTUSE(dwfl_report_module) (dwfl, name, start, end);
  if (m != NULL)
    {
      if (m->main.name == NULL)
	{
	  m->main.name = strdup (file_name);
	  m->main.fd = fd;
	}
      else if ((fd >= 0 && m->main.fd != fd)
	       || strcmp (m->main.name, file_name))
	{
	  elf_end (elf);
	overlap:
	  m->gc = true;
	  __libdwfl_seterrno (DWFL_E_OVERLAP);
	  m = NULL;
	}

      /* Preinstall the open ELF handle for the module.  */
      if (m->main.elf == NULL)
	{
	  m->main.elf = elf;
	  m->main.bias = bias;
	  m->e_type = ehdr->e_type;
	}
      else
	{
	  elf_end (elf);
	  if (m->main.bias != base)
	    goto overlap;
	}
    }
  return m;
}

Dwfl_Module *
dwfl_report_elf (Dwfl *dwfl, const char *name,
		 const char *file_name, int fd, GElf_Addr base)
{
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

  Elf *elf = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, NULL);
  Dwfl_Module *mod = __libdwfl_report_elf (dwfl, name, file_name,
					   fd, elf, base);
  if (mod == NULL)
    {
      elf_end (elf);
      if (closefd)
	close (fd);
    }

  return mod;
}
INTDEF (dwfl_report_elf)
