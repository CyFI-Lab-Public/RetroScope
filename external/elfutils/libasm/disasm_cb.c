/* Copyright (C) 2005, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2005.

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

#include <string.h>

#include "libasmP.h"
#include "../libebl/libeblP.h"


struct symtoken
{
  DisasmCtx_t *ctx;
  void *symcbarg;
};


static int
default_elf_getsym (GElf_Addr addr, Elf32_Word scnndx, GElf_Addr value,
		    char **buf, size_t *buflen, void *arg)
{
  struct symtoken *symtoken = (struct symtoken *) arg;

  /* First try the user provided function.  */
  if (symtoken->ctx->symcb != NULL)
    {
      int res = symtoken->ctx->symcb (addr, scnndx, value, buf, buflen,
				      symtoken->symcbarg);
      if (res >= 0)
	return res;
    }

  // XXX Look up in ELF file.

  return -1;
}


struct symaddrpair
{
  GElf_Addr addr;
  const char *name;
};


static void
read_symtab_exec (DisasmCtx_t *ctx)
{
  /* We simply use all we can get our hands on.  This will produce
     some duplicate information but this is no problem, we simply
     ignore the latter definitions.  */
  Elf_Scn *scn= NULL;
  while ((scn = elf_nextscn (ctx->elf, scn)) != NULL)
    {
      GElf_Shdr shdr_mem;
      GElf_Shdr *shdr = gelf_getshdr (scn, &shdr_mem);
      Elf_Data *data;
      if (shdr == NULL || shdr->sh_type != SHT_SYMTAB
	  || (data = elf_getdata (scn, NULL)) == NULL)
	continue;

      int xndxscnidx = elf_scnshndx (scn);
      Elf_Data *xndxdata = NULL;
      if (xndxscnidx > 0)
	xndxdata = elf_getdata (elf_getscn (ctx->elf, xndxscnidx), NULL);

      /* Iterate over all symbols.  Add all defined symbols.  */
      int nsyms = shdr->sh_size / shdr->sh_entsize;
      for (int cnt = 1; cnt < nsyms; ++cnt)
	{
	  Elf32_Word xshndx;
	  GElf_Sym sym_mem;
	  GElf_Sym *sym = gelf_getsymshndx (data, xndxdata, cnt, &sym_mem,
					    &xshndx);
	  if (sym == NULL)
	    continue;

	  /* Undefined symbols are useless here.  */
	  if (sym->st_shndx == SHN_UNDEF)
	    continue;


	}
    }
}


static void
read_symtab (DisasmCtx_t *ctx)
{
  /* Find the symbol table(s).  */
  GElf_Ehdr ehdr_mem;
  GElf_Ehdr *ehdr = gelf_getehdr (ctx->elf, &ehdr_mem);
  if (ehdr == NULL)
    return;

  switch (ehdr->e_type)
    {
    case ET_EXEC:
    case ET_DYN:
      read_symtab_exec (ctx);
      break;

    case ET_REL:
      // XXX  Handle
      break;

    default:
      break;
    }
}


static int
null_elf_getsym (GElf_Addr addr __attribute__ ((unused)),
		 Elf32_Word scnndx __attribute__ ((unused)),
		 GElf_Addr value __attribute__ ((unused)),
		 char **buf __attribute__ ((unused)),
		 size_t *buflen __attribute__ ((unused)),
		 void *arg __attribute__ ((unused)))
{
  return -1;
}


int
disasm_cb (DisasmCtx_t *ctx, const uint8_t **startp, const uint8_t *end,
	   GElf_Addr addr, const char *fmt, DisasmOutputCB_t outcb,
	   void *outcbarg, void *symcbarg)
{
  struct symtoken symtoken;
  DisasmGetSymCB_t getsym = ctx->symcb ?: null_elf_getsym;

  if (ctx->elf != NULL)
    {
      /* Read all symbols of the ELF file and stuff them into a hash
	 table.  The key is the address and the section index.  */
      read_symtab (ctx);

      symtoken.ctx = ctx;
      symtoken.symcbarg = symcbarg;

      symcbarg = &symtoken;

      getsym = default_elf_getsym;
    }

  return ctx->ebl->disasm (startp, end, addr, fmt, outcb, getsym, outcbarg,
			   symcbarg);
}
INTDEF (disasm_cb)
