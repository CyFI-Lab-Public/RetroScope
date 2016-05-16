/* Fixed size hash table with internal linking.
   Copyright (C) 2000, 2001, 2002, 2004, 2005 Red Hat, Inc.
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

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/cdefs.h>
#include <sys/param.h>

#include <system.h>

#define CONCAT(t1,t2) __CONCAT (t1,t2)

/* Before including this file the following macros must be defined:

   TYPE           data type of the hash table entries
   HASHFCT        name of the hashing function to use
   HASHTYPE       type used for the hashing value
   COMPARE        comparison function taking two pointers to TYPE objects
   CLASS          can be defined to `static' to avoid exporting the functions
   PREFIX         prefix to be used for function and data type names
   STORE_POINTER  if defined the table stores a pointer and not an element
                  of type TYPE
   INSERT_HASH    if defined alternate insert function which takes a hash
                  value is defined
   NO_FINI_FCT    if defined the fini function is not defined
*/


/* Defined separately.  */
extern size_t next_prime (size_t seed);


/* Set default values.  */
#ifndef HASHTYPE
# define HASHTYPE size_t
#endif

#ifndef CLASS
# define CLASS
#endif

#ifndef PREFIX
# define PREFIX
#endif


/* The data structure.  */
struct CONCAT(PREFIX,fshash)
{
  size_t nslots;
  struct CONCAT(PREFIX,fshashent)
  {
    HASHTYPE hval;
#ifdef STORE_POINTER
# define ENTRYP(el) (el).entry
    TYPE *entry;
#else
# define ENTRYP(el) &(el).entry
    TYPE entry;
#endif
  } table[0];
};


/* Constructor for the hashing table.  */
CLASS struct CONCAT(PREFIX,fshash) *
CONCAT(PREFIX,fshash_init) (size_t nelems)
{
  struct CONCAT(PREFIX,fshash) *result;
  /* We choose a size for the hashing table 150% over the number of
     entries.  This will guarantee short medium search lengths.  */
  const size_t max_size_t = ~((size_t) 0);

  if (nelems >= (max_size_t / 3) * 2)
    {
      errno = EINVAL;
      return NULL;
    }

  /* Adjust the size to be used for the hashing table.  */
  nelems = next_prime (MAX ((nelems * 3) / 2, 10));

  /* Allocate the data structure for the result.  */
  result = (struct CONCAT(PREFIX,fshash) *)
    xcalloc (sizeof (struct CONCAT(PREFIX,fshash))
	     + (nelems + 1) * sizeof (struct CONCAT(PREFIX,fshashent)), 1);
  if (result == NULL)
    return NULL;

  result->nslots = nelems;

  return result;
}


#ifndef NO_FINI_FCT
CLASS void
CONCAT(PREFIX,fshash_fini) (struct CONCAT(PREFIX,fshash) *htab)
{
  free (htab);
}
#endif


static struct CONCAT(PREFIX,fshashent) *
CONCAT(PREFIX,fshash_lookup) (struct CONCAT(PREFIX,fshash) *htab,
			      HASHTYPE hval, TYPE *data)
{
  size_t idx = 1 + hval % htab->nslots;

  if (htab->table[idx].hval != 0)
    {
      HASHTYPE hash;

      /* See whether this is the same entry.  */
      if (htab->table[idx].hval == hval
	  && COMPARE (data, ENTRYP (htab->table[idx])) == 0)
	return &htab->table[idx];

      /* Second hash function as suggested in [Knuth].  */
      hash = 1 + hval % (htab->nslots - 2);

      do
	{
	  if (idx <= hash)
	    idx = htab->nslots + idx - hash;
	  else
	    idx -= hash;

	  if (htab->table[idx].hval == hval
	      && COMPARE (data, ENTRYP(htab->table[idx])) == 0)
	    return &htab->table[idx];
	}
      while (htab->table[idx].hval != 0);
    }

  return &htab->table[idx];
}


CLASS int
__attribute__ ((unused))
CONCAT(PREFIX,fshash_insert) (struct CONCAT(PREFIX,fshash) *htab,
			      const char *str,
			      size_t len __attribute__ ((unused)), TYPE *data)
{
  HASHTYPE hval = HASHFCT (str, len ?: strlen (str));
  struct CONCAT(PREFIX,fshashent) *slot;

  slot = CONCAT(PREFIX,fshash_lookup) (htab, hval, data);
 if (slot->hval != 0)
    /* We don't want to overwrite the old value.  */
    return -1;

  slot->hval = hval;
#ifdef STORE_POINTER
  slot->entry = data;
#else
  slot->entry = *data;
#endif

  return 0;
}


#ifdef INSERT_HASH
CLASS int
__attribute__ ((unused))
CONCAT(PREFIX,fshash_insert_hash) (struct CONCAT(PREFIX,fshash) *htab,
				   HASHTYPE hval, TYPE *data)
{
  struct CONCAT(PREFIX,fshashent) *slot;

  slot = CONCAT(PREFIX,fshash_lookup) (htab, hval, data);
  if (slot->hval != 0)
    /* We don't want to overwrite the old value.  */
    return -1;

  slot->hval = hval;
#ifdef STORE_POINTER
  slot->entry = data;
#else
  slot->entry = *data;
#endif

  return 0;
}
#endif


CLASS int
__attribute__ ((unused))
CONCAT(PREFIX,fshash_overwrite) (struct CONCAT(PREFIX,fshash) *htab,
				 const char *str,
				 size_t len __attribute__ ((unused)),
				 TYPE *data)
{
  HASHTYPE hval = HASHFCT (str, len ?: strlen (str));
  struct CONCAT(PREFIX,fshashent) *slot;

  slot = CONCAT(PREFIX,fshash_lookup) (htab, hval, data);
  slot->hval = hval;
#ifdef STORE_POINTER
  slot->entry = data;
#else
  slot->entry = *data;
#endif

  return 0;
}


CLASS const TYPE *
CONCAT(PREFIX,fshash_find) (const struct CONCAT(PREFIX,fshash) *htab,
			    const char *str,
			    size_t len __attribute__ ((unused)), TYPE *data)
{
  HASHTYPE hval = HASHFCT (str, len ?: strlen (str));
  struct CONCAT(PREFIX,fshashent) *slot;

  slot = CONCAT(PREFIX,fshash_lookup) ((struct CONCAT(PREFIX,fshash) *) htab,
				       hval, data);
  if (slot->hval == 0)
    /* Not found.  */
    return NULL;

  return ENTRYP(*slot);
}


/* Unset the macros we expect.  */
#undef TYPE
#undef HASHFCT
#undef HASHTYPE
#undef COMPARE
#undef CLASS
#undef PREFIX
#undef INSERT_HASH
#undef STORE_POINTER
#undef NO_FINI_FCT
