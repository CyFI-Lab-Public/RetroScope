/* Maintenance of module list in libdwfl.
   Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
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
#include <search.h>
#include <unistd.h>

static void
free_cu (struct dwfl_cu *cu)
{
  if (cu->lines != NULL)
    free (cu->lines);
  free (cu);
}

static void
nofree (void *arg __attribute__ ((unused)))
{
}

static void
free_file (struct dwfl_file *file)
{
  free (file->name);

  /* Close the fd only on the last reference.  */
  if (file->elf != NULL && elf_end (file->elf) == 0 && file->fd != -1)
    close (file->fd);
}

void
internal_function
__libdwfl_module_free (Dwfl_Module *mod)
{
  if (mod->lazy_cu_root != NULL)
    tdestroy (mod->lazy_cu_root, nofree);

  if (mod->aranges != NULL)
    free (mod->aranges);

  if (mod->cu != NULL)
    {
      for (size_t i = 0; i < mod->ncu; ++i)
	free_cu (mod->cu[i]);
      free (mod->cu);
    }

  if (mod->dw != NULL)
    INTUSE(dwarf_end) (mod->dw);

  if (mod->ebl != NULL)
    ebl_closebackend (mod->ebl);

  if (mod->debug.elf != mod->main.elf)
    free_file (&mod->debug);
  free_file (&mod->main);

  if (mod->build_id_bits != NULL)
    free (mod->build_id_bits);

  free (mod->name);
  free (mod);
}

void
dwfl_report_begin_add (Dwfl *dwfl __attribute__ ((unused)))
{
  /* The lookup table will be cleared on demand, there is nothing we need
     to do here.  */
}
INTDEF (dwfl_report_begin_add)

void
dwfl_report_begin (Dwfl *dwfl)
{
  /* Clear the segment lookup table.  */
  dwfl->lookup_elts = 0;

  for (Dwfl_Module *m = dwfl->modulelist; m != NULL; m = m->next)
    m->gc = true;

  dwfl->offline_next_address = OFFLINE_REDZONE;
}
INTDEF (dwfl_report_begin)

/* Report that a module called NAME spans addresses [START, END).
   Returns the module handle, either existing or newly allocated,
   or returns a null pointer for an allocation error.  */
Dwfl_Module *
dwfl_report_module (Dwfl *dwfl, const char *name,
		    GElf_Addr start, GElf_Addr end)
{
  Dwfl_Module **tailp = &dwfl->modulelist, **prevp = tailp;

  inline Dwfl_Module *use (Dwfl_Module *mod)
  {
    mod->next = *tailp;
    *tailp = mod;

    if (unlikely (dwfl->lookup_module != NULL))
      {
	free (dwfl->lookup_module);
	dwfl->lookup_module = NULL;
      }

    return mod;
  }

  for (Dwfl_Module *m = *prevp; m != NULL; m = *(prevp = &m->next))
    {
      if (m->low_addr == start && m->high_addr == end
	  && !strcmp (m->name, name))
	{
	  /* This module is still here.  Move it to the place in the list
	     after the last module already reported.  */
	  *prevp = m->next;
	  m->gc = false;
	  return use (m);
	}

      if (! m->gc)
	tailp = &m->next;
    }

  Dwfl_Module *mod = calloc (1, sizeof *mod);
  if (mod == NULL)
    goto nomem;

  mod->name = strdup (name);
  if (mod->name == NULL)
    {
      free (mod);
    nomem:
      __libdwfl_seterrno (DWFL_E_NOMEM);
      return NULL;
    }

  mod->low_addr = start;
  mod->high_addr = end;
  mod->dwfl = dwfl;

  return use (mod);
}
INTDEF (dwfl_report_module)


/* Finish reporting the current set of modules to the library.
   If REMOVED is not null, it's called for each module that
   existed before but was not included in the current report.
   Returns a nonzero return value from the callback.
   DWFL cannot be used until this function has returned zero.  */
int
dwfl_report_end (Dwfl *dwfl,
		 int (*removed) (Dwfl_Module *, void *,
				 const char *, Dwarf_Addr,
				 void *arg),
		 void *arg)
{
  Dwfl_Module **tailp = &dwfl->modulelist;
  while (*tailp != NULL)
    {
      Dwfl_Module *m = *tailp;
      if (m->gc && removed != NULL)
	{
	  int result = (*removed) (MODCB_ARGS (m), arg);
	  if (result != 0)
	    return result;
	}
      if (m->gc)
	{
	  *tailp = m->next;
	  __libdwfl_module_free (m);
	}
      else
	tailp = &m->next;
    }

  return 0;
}
INTDEF (dwfl_report_end)
