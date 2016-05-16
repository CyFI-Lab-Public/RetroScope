/* Return section type name.
   Copyright (C) 2001, 2002, 2006, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <stdio.h>
#include <libeblP.h>


const char *
ebl_section_type_name (ebl, section, buf, len)
     Ebl *ebl;
     int section;
     char *buf;
     size_t len;
{
  const char *res = ebl->section_type_name (section, buf, len);

  if (res == NULL)
    {
      static const char *knowntypes[] =
	{
#define KNOWNSTYPE(name) [SHT_##name] = #name
	  KNOWNSTYPE (NULL),
	  KNOWNSTYPE (PROGBITS),
	  KNOWNSTYPE (SYMTAB),
	  KNOWNSTYPE (STRTAB),
	  KNOWNSTYPE (RELA),
	  KNOWNSTYPE (HASH),
	  KNOWNSTYPE (DYNAMIC),
	  KNOWNSTYPE (NOTE),
	  KNOWNSTYPE (NOBITS),
	  KNOWNSTYPE (REL),
	  KNOWNSTYPE (SHLIB),
	  KNOWNSTYPE (DYNSYM),
	  KNOWNSTYPE (INIT_ARRAY),
	  KNOWNSTYPE (FINI_ARRAY),
	  KNOWNSTYPE (PREINIT_ARRAY),
	  KNOWNSTYPE (GROUP),
	  KNOWNSTYPE (SYMTAB_SHNDX)
	};

      /* Handle standard names.  */
      if ((size_t) section < sizeof (knowntypes) / sizeof (knowntypes[0])
	  && knowntypes[section] != NULL)
	res = knowntypes[section];
      /* The symbol versioning/Sun extensions.  */
      else if (section >= SHT_LOSUNW && section <= SHT_HISUNW)
	{
	  static const char *sunwtypes[] =
	    {
#undef KNOWNSTYPE
#define KNOWNSTYPE(name) [SHT_##name - SHT_LOSUNW] = #name
	      KNOWNSTYPE (SUNW_move),
	      KNOWNSTYPE (SUNW_COMDAT),
	      KNOWNSTYPE (SUNW_syminfo),
	      KNOWNSTYPE (GNU_verdef),
	      KNOWNSTYPE (GNU_verneed),
	      KNOWNSTYPE (GNU_versym)
	    };
	  res = sunwtypes[section - SHT_LOSUNW];
	}
      else
	/* A few GNU additions.  */
	switch (section)
	  {
	  case SHT_CHECKSUM:
	    res = "CHECKSUM";
	    break;
	  case SHT_GNU_LIBLIST:
	    res = "GNU_LIBLIST";
	    break;
	  case SHT_GNU_HASH:
	    res = "GNU_HASH";
	    break;
	  case SHT_GNU_ATTRIBUTES:
	    res = "GNU_ATTRIBUTES";
	    break;

	  default:
	    /* Handle OS-specific section names.  */
	    if (section >= SHT_LOOS && section <= SHT_HIOS)
	      snprintf (buf, len, "SHT_LOOS+%x", section - SHT_LOOS);
	    /* Handle processor-specific section names.  */
	    else if (section >= SHT_LOPROC && section <= SHT_HIPROC)
	      snprintf (buf, len, "SHT_LOPROC+%x", section - SHT_LOPROC);
	    else if ((unsigned int) section >= SHT_LOUSER
		     && (unsigned int) section <= SHT_HIUSER)
	      snprintf (buf, len, "SHT_LOUSER+%x", section - SHT_LOUSER);
	    else
	      snprintf (buf, len, "%s: %d", gettext ("<unknown>"), section);

	    res = buf;
	    break;
	  }
    }

  return res;
}
