/* Convenience functions for allocation.
   Copyright (C) 2006 Red Hat, Inc.
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

#include <error.h>
#include <libintl.h>
#include <stddef.h>
#include <stdlib.h>
#include <sys/types.h>
#include "system.h"

#ifndef _
# define _(str) gettext (str)
#endif


/* Allocate N bytes of memory dynamically, with error checking.  */
void *
xmalloc (n)
     size_t n;
{
  void *p;

  p = malloc (n);
  if (p == NULL)
    error (EXIT_FAILURE, 0, _("memory exhausted"));
  return p;
}


/* Allocate memory for N elements of S bytes, with error checking.  */
void *
xcalloc (n, s)
     size_t n, s;
{
  void *p;

  p = calloc (n, s);
  if (p == NULL)
    error (EXIT_FAILURE, 0, _("memory exhausted"));
  return p;
}


/* Change the size of an allocated block of memory P to N bytes,
   with error checking.  */
void *
xrealloc (p, n)
     void *p;
     size_t n;
{
  p = realloc (p, n);
  if (p == NULL)
    error (EXIT_FAILURE, 0, _("memory exhausted"));
  return p;
}
