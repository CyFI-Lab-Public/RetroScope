/* Functions to handle creation of Linux archives.
   Copyright (C) 2007 Red Hat, Inc.
   Written by Ulrich Drepper <drepper@redhat.com>, 2007.

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

#include <error.h>
#include <libintl.h>
#include <limits.h>
#include <string.h>
#include <sys/param.h>

#include "arlib.h"


/* Add long file name FILENAME of length FILENAMELEN to the symbol table
   SYMTAB.  Return the offset into the long file name table.  */
long int
arlib_add_long_name (const char *filename, size_t filenamelen)
{
  size_t size = obstack_object_size (&symtab.longnamesob);

  obstack_grow (&symtab.longnamesob, filename, filenamelen);
  obstack_grow (&symtab.longnamesob, "/\n", 2);

  return size - sizeof (struct ar_hdr);
}
