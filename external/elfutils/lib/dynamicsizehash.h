/* Copyright (C) 2000, 2001, 2002 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

#include <stddef.h>

/* Before including this file the following macros must be defined:

   NAME      name of the hash table structure.
   TYPE      data type of the hash table entries

   The following macros if present select features:

   ITERATE   iterating over the table entries is possible
 */


/* Optionally include an entry pointing to the first used entry.  */
#ifdef ITERATE
# define FIRST(name)	name##_ent *first;
# define NEXT(name)	struct name##_ent *next;
#else
# define FIRST(name)
# define NEXT(name)
#endif


/* Defined separately.  */
extern size_t next_prime (size_t seed);


/* Table entry type.  */
#define _DYNHASHENTTYPE(name) \
  typedef struct name##_ent						      \
  {									      \
    unsigned long int hashval;						      \
    TYPE data;								      \
    NEXT (name)								      \
  } name##_ent
#define DYNHASHENTTYPE(name) _DYNHASHENTTYPE (name)
DYNHASHENTTYPE (NAME);


/* Type of the dynamic hash table data structure.  */
#define _DYNHASHTYPE(name) \
typedef struct								      \
{									      \
  unsigned long int size;						      \
  unsigned long int filled;						      \
  name##_ent *table;							      \
  FIRST	(name)								      \
} name
#define DYNHASHTYPE(name) _DYNHASHTYPE (name)
DYNHASHTYPE (NAME);



#define _FUNCTIONS(name) \
/* Initialize the hash table.  */					      \
extern int name##_init (name *htab, unsigned long int init_size);	      \
									      \
/* Free resources allocated for hash table.  */				      \
extern int name##_free (name *htab);					      \
									      \
/* Insert new entry.  */						      \
extern int name##_insert (name *htab, unsigned long int hval, TYPE data);     \
									      \
/* Insert new entry, possibly overwrite old entry.  */			      \
extern int name##_overwrite (name *htab, unsigned long int hval, TYPE data);  \
									      \
/* Find entry in hash table.  */					      \
extern TYPE name##_find (name *htab, unsigned long int hval, TYPE val);
#define FUNCTIONS(name) _FUNCTIONS (name)
FUNCTIONS (NAME)


#ifdef ITERATE
# define _XFUNCTIONS(name) \
/* Get next element in table.  */					      \
extern TYPE name##_iterate (name *htab, void **ptr);
# define XFUNCTIONS(name) _XFUNCTIONS (name)
XFUNCTIONS (NAME)
#endif

#ifndef NO_UNDEF
# undef DYNHASHENTTYPE
# undef DYNHASHTYPE
# undef FUNCTIONS
# undef _FUNCTIONS
# undef XFUNCTIONS
# undef _XFUNCTIONS
# undef NAME
# undef TYPE
# undef ITERATE
# undef COMPARE
# undef FIRST
# undef NEXT
#endif
