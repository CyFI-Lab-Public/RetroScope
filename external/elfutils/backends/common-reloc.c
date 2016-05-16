/* Common code for ebl reloc functions.
   Copyright (C) 2005, 2006 Red Hat, Inc.
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

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#include "libebl_CPU.h"
#include <assert.h>

#define R_TYPE(name)		PASTE (RELOC_PREFIX, name)
#define PASTE(a, b)		PASTE_1 (a, b)
#define PASTE_1(a, b)		a##b
#define R_NAME(name)		R_NAME_1 (RELOC_PREFIX, name)
#define R_NAME_1(prefix, type)	R_NAME_2 (prefix, type)
#define R_NAME_2(prefix, type)	#prefix #type

#define RELOC_TYPES		STRINGIFIED_PASTE (BACKEND, reloc.def)
#define STRINGIFIED_PASTE(a, b)	STRINGIFY (PASTE (a, b))
#define STRINGIFY(x)		STRINGIFY_1 (x)
#define STRINGIFY_1(x)		#x

/* Provide a table of reloc type names, in a PIC-friendly fashion.  */

static const struct EBLHOOK(reloc_nametable)
{
  char zero;
#define	RELOC_TYPE(type, uses) \
  char name_##type[sizeof R_NAME (type)];
#include RELOC_TYPES
#undef RELOC_TYPE
} EBLHOOK(reloc_nametable) =
  {
    '\0',
#define	RELOC_TYPE(type, uses) R_NAME (type),
#include RELOC_TYPES
#undef RELOC_TYPE
  };
#define reloc_namestr (&EBLHOOK(reloc_nametable).zero)

static const uint_fast16_t EBLHOOK(reloc_nameidx)[] =
{
#define	RELOC_TYPE(type, uses) \
  [R_TYPE (type)] = offsetof (struct EBLHOOK(reloc_nametable), name_##type),
#include RELOC_TYPES
#undef RELOC_TYPE
};
#define nreloc \
  ((int) (sizeof EBLHOOK(reloc_nameidx) / sizeof EBLHOOK(reloc_nameidx)[0]))

#define REL	(1 << (ET_REL - 1))
#define EXEC	(1 << (ET_EXEC - 1))
#define DYN	(1 << (ET_DYN - 1))
static const uint8_t EBLHOOK(reloc_valid)[] =
{
#define	RELOC_TYPE(type, uses) [R_TYPE (type)] = uses,
#include RELOC_TYPES
#undef RELOC_TYPE
};
#undef REL
#undef EXEC
#undef DYN

const char *
EBLHOOK(reloc_type_name) (int reloc,
			  char *buf __attribute__ ((unused)),
			  size_t len __attribute__ ((unused)))
{
  if (reloc >= 0 && reloc < nreloc && EBLHOOK(reloc_nameidx)[reloc] != 0)
    return &reloc_namestr[EBLHOOK(reloc_nameidx)[reloc]];
  return NULL;
}

bool
EBLHOOK(reloc_type_check) (int reloc)
{
  return reloc >= 0 && reloc < nreloc && EBLHOOK(reloc_nameidx)[reloc] != 0;
}

bool
EBLHOOK(reloc_valid_use) (Elf *elf, int reloc)
{
  uint8_t uses = EBLHOOK(reloc_valid)[reloc];

  GElf_Ehdr ehdr_mem;
  GElf_Ehdr *ehdr = gelf_getehdr (elf, &ehdr_mem);
  assert (ehdr != NULL);
  uint8_t type = ehdr->e_type;

  return type > ET_NONE && type < ET_CORE && (uses & (1 << (type - 1)));
}


bool
EBLHOOK(copy_reloc_p) (int reloc)
{
  return reloc == R_TYPE (COPY);
}

bool
EBLHOOK(none_reloc_p) (int reloc)
{
  return reloc == R_TYPE (NONE);
}

#ifndef NO_RELATIVE_RELOC
bool
EBLHOOK(relative_reloc_p) (int reloc)
{
  return reloc == R_TYPE (RELATIVE);
}
#endif

static void
EBLHOOK(init_reloc) (Ebl *ebl)
{
  ebl->reloc_type_name = EBLHOOK(reloc_type_name);
  ebl->reloc_type_check = EBLHOOK(reloc_type_check);
  ebl->reloc_valid_use = EBLHOOK(reloc_valid_use);
  ebl->copy_reloc_p = EBLHOOK(copy_reloc_p);
  ebl->none_reloc_p = EBLHOOK(none_reloc_p);
#ifndef NO_RELATIVE_RELOC
  ebl->relative_reloc_p = EBLHOOK(relative_reloc_p);
#endif
}
