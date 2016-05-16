/* Register names and numbers for Alpha DWARF.
   Copyright (C) 2007 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <string.h>
#include <dwarf.h>

#define BACKEND alpha_
#include "libebl_CPU.h"

ssize_t
alpha_register_info (Ebl *ebl __attribute__ ((unused)),
		     int regno, char *name, size_t namelen,
		     const char **prefix, const char **setname,
		     int *bits, int *type)
{
  if (name == NULL)
    return 67;

  if (regno < 0 || regno > 66 || namelen < 7)
    return -1;

  *prefix = "$";

  *bits = 64;
  *type = DW_ATE_signed;
  *setname = "integer";
  if (regno >= 32 && regno < 64)
    {
      *setname = "FPU";
      *type = DW_ATE_float;
    }

  switch (regno)
    {
    case 0:
      name[0] = 'v';
      name[1] = '0';
      namelen = 2;
      break;

    case 1 ... 8:
      name[0] = 't';
      name[1] = regno - 1 + '0';
      namelen = 2;
      break;

    case 9 ... 15:
      name[0] = 's';
      name[1] = regno - 9 + '0';
      namelen = 2;
      break;

    case 16 ... 21:
      name[0] = 'a';
      name[1] = regno - 16 + '0';
      namelen = 2;
      break;

    case 22 ... 23:
      name[0] = 't';
      name[1] = regno - 22 + '8';
      namelen = 2;
      break;

    case 24 ... 25:
      name[0] = 't';
      name[1] = '1';
      name[2] = regno - 24 + '0';
      namelen = 3;
      break;

    case 26:
      *type = DW_ATE_address;
      return stpcpy (name, "ra") + 1 - name;

    case 27:
      return stpcpy (name, "t12") + 1 - name;

    case 28:
      return stpcpy (name, "at") + 1 - name;

    case 29:
      *type = DW_ATE_address;
      return stpcpy (name, "gp") + 1 - name;

    case 30:
      *type = DW_ATE_address;
      return stpcpy (name, "sp") + 1 - name;

    case 31:
      return stpcpy (name, "zero") + 1 - name;

    case 32 ... 32 + 9:
      name[0] = 'f';
      name[1] = regno - 32 + '0';
      namelen = 2;
      break;

    case 32 + 10 ... 32 + 19:
      name[0] = 'f';
      name[1] = '1';
      name[2] = regno - 32 - 10 + '0';
      namelen = 3;
      break;

    case 32 + 20 ... 32 + 29:
      name[0] = 'f';
      name[1] = '2';
      name[2] = regno - 32 - 20 + '0';
      namelen = 3;
      break;

    case 32 + 30:
      return stpcpy (name, "f30") + 1 - name;

    case 32 + 31:
      *type = DW_ATE_unsigned;
      return stpcpy (name, "fpcr") + 1 - name;

    case 64:
      *type = DW_ATE_address;
      return stpcpy (name, "pc") + 1 - name;

    case 66:
      *type = DW_ATE_address;
      return stpcpy (name, "unique") + 1 - name;

    default:
      *setname = NULL;
      return 0;
    }

  name[namelen++] = '\0';
  return namelen;
}
