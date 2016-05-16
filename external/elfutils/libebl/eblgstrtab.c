/* Generic string table handling.
   Copyright (C) 2000, 2001, 2002, 2005 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <inttypes.h>
#include <libelf.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/param.h>

#include "libebl.h"

#ifndef MIN
# define MIN(a, b) ((a) < (b) ? (a) : (b))
#endif


struct Ebl_GStrent
{
  const char *string;
  size_t len;
  struct Ebl_GStrent *next;
  struct Ebl_GStrent *left;
  struct Ebl_GStrent *right;
  size_t offset;
  unsigned int width;
  char reverse[0];
};


struct memoryblock
{
  struct memoryblock *next;
  char memory[0];
};


struct Ebl_GStrtab
{
  struct Ebl_GStrent *root;
  struct memoryblock *memory;
  char *backp;
  size_t left;
  size_t total;
  unsigned int width;
  bool nullstr;

  struct Ebl_GStrent null;
};


/* Cache for the pagesize.  We correct this value a bit so that `malloc'
   is not allocating more than a page.  */
static size_t ps;


struct Ebl_GStrtab *
ebl_gstrtabinit (unsigned int width, bool nullstr)
{
  struct Ebl_GStrtab *ret;

  if (ps == 0)
    {
      ps = sysconf (_SC_PAGESIZE) - 2 * sizeof (void *);
      assert (sizeof (struct memoryblock) < ps);
    }

  ret = (struct Ebl_GStrtab *) calloc (1, sizeof (struct Ebl_GStrtab));
  if (ret != NULL)
    {
      ret->width = width;
      ret->nullstr = nullstr;

      if (nullstr)
	{
	  ret->null.len = 1;
	  ret->null.string = (char *) calloc (1, width);
	}
    }

  return ret;
}


static void
morememory (struct Ebl_GStrtab *st, size_t len)
{
  struct memoryblock *newmem;

  if (len < ps)
    len = ps;
  newmem = (struct memoryblock *) malloc (len);
  if (newmem == NULL)
    abort ();

  newmem->next = st->memory;
  st->memory = newmem;
  st->backp = newmem->memory;
  st->left = len - offsetof (struct memoryblock, memory);
}


void
ebl_gstrtabfree (struct Ebl_GStrtab *st)
{
  struct memoryblock *mb = st->memory;

  while (mb != NULL)
    {
      void *old = mb;
      mb = mb->next;
      free (old);
    }

  if (st->null.string != NULL)
    free ((char *) st->null.string);

  free (st);
}


static struct Ebl_GStrent *
newstring (struct Ebl_GStrtab *st, const char *str, size_t len)
{
  /* Compute the amount of padding needed to make the structure aligned.  */
  size_t align = ((__alignof__ (struct Ebl_GStrent)
		   - (((uintptr_t) st->backp)
		      & (__alignof__ (struct Ebl_GStrent) - 1)))
		  & (__alignof__ (struct Ebl_GStrent) - 1));

  /* Make sure there is enough room in the memory block.  */
  if (st->left < align + sizeof (struct Ebl_GStrent) + len * st->width)
    {
      morememory (st, sizeof (struct Ebl_GStrent) + len * st->width);
      align = 0;
    }

  /* Create the reserved string.  */
  struct Ebl_GStrent *newstr = (struct Ebl_GStrent *) (st->backp + align);
  newstr->string = str;
  newstr->len = len;
  newstr->width = st->width;
  newstr->next = NULL;
  newstr->left = NULL;
  newstr->right = NULL;
  newstr->offset = 0;
  for (int i = len - 2; i >= 0; --i)
    for (int j = st->width - 1; j >= 0; --j)
      newstr->reverse[i * st->width + j] = str[(len - 2 - i) * st->width + j];
  for (size_t j = 0; j < st->width; ++j)
    newstr->reverse[(len - 1) * st->width + j] = '\0';
  st->backp += align + sizeof (struct Ebl_GStrent) + len * st->width;
  st->left -= align + sizeof (struct Ebl_GStrent) + len * st->width;

  return newstr;
}


/* XXX This function should definitely be rewritten to use a balancing
   tree algorith (AVL, red-black trees).  For now a simple, correct
   implementation is enough.  */
static struct Ebl_GStrent **
searchstring (struct Ebl_GStrent **sep, struct Ebl_GStrent *newstr)
{
  int cmpres;

  /* More strings?  */
  if (*sep == NULL)
    {
      *sep = newstr;
      return sep;
    }

  /* Compare the strings.  */
  cmpres = memcmp ((*sep)->reverse, newstr->reverse,
		   (MIN ((*sep)->len, newstr->len) - 1) * (*sep)->width);
  if (cmpres == 0)
    /* We found a matching string.  */
    return sep;
  else if (cmpres > 0)
    return searchstring (&(*sep)->left, newstr);
  else
    return searchstring (&(*sep)->right, newstr);
}


/* Add new string.  The actual string is assumed to be permanent.  */
struct Ebl_GStrent *
ebl_gstrtabadd (struct Ebl_GStrtab *st, const char *str, size_t len)
{
  struct Ebl_GStrent *newstr;
  struct Ebl_GStrent **sep;

  /* Compute the string length if the caller doesn't know it.  */
  if (len == 0)
    {
      size_t j;

      do
	for (j = 0; j < st->width; ++j)
	  if (str[len * st->width + j] != '\0')
	    break;
      while (j == st->width && ++len);
    }

  /* Make sure all "" strings get offset 0 but only if the table was
     created with a special null entry in mind.  */
  if (len == 1 && st->null.string != NULL)
    return &st->null;

  /* Allocate memory for the new string and its associated information.  */
  newstr = newstring (st, str, len);

  /* Search in the array for the place to insert the string.  If there
     is no string with matching prefix and no string with matching
     leading substring, create a new entry.  */
  sep = searchstring (&st->root, newstr);
  if (*sep != newstr)
    {
      /* This is not the same entry.  This means we have a prefix match.  */
      if ((*sep)->len > newstr->len)
	{
	  struct Ebl_GStrent *subs;

	  /* Check whether we already know this string.  */
	  for (subs = (*sep)->next; subs != NULL; subs = subs->next)
	    if (subs->len == newstr->len)
	      {
		/* We have an exact match with a substring.  Free the memory
		   we allocated.  */
		st->left += (st->backp - (char *) newstr) * st->width;
		st->backp = (char *) newstr;

		return subs;
	      }

	  /* We have a new substring.  This means we don't need the reverse
	     string of this entry anymore.  */
	  st->backp -= newstr->len;
	  st->left += newstr->len;

	  newstr->next = (*sep)->next;
	  (*sep)->next = newstr;
	}
      else if ((*sep)->len != newstr->len)
	{
	  /* When we get here it means that the string we are about to
	     add has a common prefix with a string we already have but
	     it is longer.  In this case we have to put it first.  */
	  st->total += newstr->len - (*sep)->len;
	  newstr->next = *sep;
	  newstr->left = (*sep)->left;
	  newstr->right = (*sep)->right;
	  *sep = newstr;
	}
      else
	{
	  /* We have an exact match.  Free the memory we allocated.  */
	  st->left += (st->backp - (char *) newstr) * st->width;
	  st->backp = (char *) newstr;

	  newstr = *sep;
	}
    }
  else
    st->total += newstr->len;

  return newstr;
}


static void
copystrings (struct Ebl_GStrent *nodep, char **freep, size_t *offsetp)
{
  struct Ebl_GStrent *subs;

  if (nodep->left != NULL)
    copystrings (nodep->left, freep, offsetp);

  /* Process the current node.  */
  nodep->offset = *offsetp;
  *freep = (char *) mempcpy (*freep, nodep->string, nodep->len * nodep->width);
  *offsetp += nodep->len * nodep->width;

  for (subs = nodep->next; subs != NULL; subs = subs->next)
    {
      assert (subs->len < nodep->len);
      subs->offset = nodep->offset + (nodep->len - subs->len) * nodep->width;
      assert (subs->offset != 0 || subs->string[0] == '\0');
    }

  if (nodep->right != NULL)
    copystrings (nodep->right, freep, offsetp);
}


void
ebl_gstrtabfinalize (struct Ebl_GStrtab *st, Elf_Data *data)
{
  size_t copylen;
  char *endp;
  size_t nulllen = st->nullstr ? st->width : 0;

  /* Fill in the information.  */
  data->d_buf = malloc (st->total + nulllen);
  if (data->d_buf == NULL)
    abort ();

  /* The first byte must always be zero if we created the table with a
     null string.  */
  if (st->nullstr)
    memset (data->d_buf, '\0', st->width);

  data->d_type = ELF_T_BYTE;
  data->d_size = st->total + nulllen;
  data->d_off = 0;
  data->d_align = 1;
  data->d_version = EV_CURRENT;

  /* Now run through the tree and add all the string while also updating
     the offset members of the elfstrent records.  */
  endp = (char *) data->d_buf + nulllen;
  copylen = nulllen;
  copystrings (st->root, &endp, &copylen);
  assert (copylen == st->total * st->width + nulllen);
}


size_t
ebl_gstrtaboffset (struct Ebl_GStrent *se)
{
  return se->offset;
}
