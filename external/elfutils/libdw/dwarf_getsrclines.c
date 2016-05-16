/* Return line number information of CU.
   Copyright (C) 2004, 2005, 2007, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2004.

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
#include <stdlib.h>
#include <string.h>
#include "dwarf.h"
#include "libdwP.h"


struct filelist
{
  Dwarf_Fileinfo info;
  struct filelist *next;
};

struct linelist
{
  Dwarf_Line line;
  struct linelist *next;
};


/* Compare by Dwarf_Line.addr, given pointers into an array of pointers.  */
static int
compare_lines (const void *a, const void *b)
{
  Dwarf_Line *const *p1 = a;
  Dwarf_Line *const *p2 = b;

  if ((*p1)->addr == (*p2)->addr)
    /* An end_sequence marker precedes a normal record at the same address.  */
    return (*p2)->end_sequence - (*p1)->end_sequence;

  return (*p1)->addr - (*p2)->addr;
}


/* Adds a new line to the matrix.  We cannot define a function because
   we want to use alloca.  */
#define NEW_LINE(end_seq) \
  do {									      \
    /* Add the new line.  */						      \
    new_line = (struct linelist *) alloca (sizeof (struct linelist));	      \
									      \
    /* Set the line information.  */					      \
    new_line->line.addr = address;					      \
    new_line->line.file = file;						      \
    new_line->line.line = line;						      \
    new_line->line.column = column;					      \
    new_line->line.is_stmt = is_stmt;					      \
    new_line->line.basic_block = basic_block;				      \
    new_line->line.end_sequence = end_seq;				      \
    new_line->line.prologue_end = prologue_end;				      \
    new_line->line.epilogue_begin = epilogue_begin;			      \
									      \
    new_line->next = linelist;						      \
    linelist = new_line;						      \
    ++nlinelist;							      \
  } while (0)


int
dwarf_getsrclines (Dwarf_Die *cudie, Dwarf_Lines **lines, size_t *nlines)
{
  if (unlikely (cudie == NULL
		|| INTUSE(dwarf_tag) (cudie) != DW_TAG_compile_unit))
    return -1;

  int res = -1;

  /* Get the information if it is not already known.  */
  struct Dwarf_CU *const cu = cudie->cu;
  if (cu->lines == NULL)
    {
      /* Failsafe mode: no data found.  */
      cu->lines = (void *) -1l;
      cu->files = (void *) -1l;

      /* The die must have a statement list associated.  */
      Dwarf_Attribute stmt_list_mem;
      Dwarf_Attribute *stmt_list = INTUSE(dwarf_attr) (cudie, DW_AT_stmt_list,
						       &stmt_list_mem);

      /* Get the offset into the .debug_line section.  NB: this call
	 also checks whether the previous dwarf_attr call failed.  */
      Dwarf_Word offset;
      if (INTUSE(dwarf_formudata) (stmt_list, &offset) != 0)
	goto out;

      Dwarf *dbg = cu->dbg;
      if (dbg->sectiondata[IDX_debug_line] == NULL)
	{
	  __libdw_seterrno (DWARF_E_NO_DEBUG_LINE);
	  goto out;
	}
      const uint8_t *linep = dbg->sectiondata[IDX_debug_line]->d_buf + offset;
      const uint8_t *lineendp = (dbg->sectiondata[IDX_debug_line]->d_buf
				 + dbg->sectiondata[IDX_debug_line]->d_size);

      /* Get the compilation directory.  */
      Dwarf_Attribute compdir_attr_mem;
      Dwarf_Attribute *compdir_attr = INTUSE(dwarf_attr) (cudie,
							  DW_AT_comp_dir,
							  &compdir_attr_mem);
      const char *comp_dir = INTUSE(dwarf_formstring) (compdir_attr);

      if (unlikely (linep + 4 > lineendp))
	{
	invalid_data:
	  __libdw_seterrno (DWARF_E_INVALID_DEBUG_LINE);
	  goto out;
	}
      Dwarf_Word unit_length = read_4ubyte_unaligned_inc (dbg, linep);
      unsigned int length = 4;
      if (unlikely (unit_length == DWARF3_LENGTH_64_BIT))
	{
	  if (unlikely (linep + 8 > lineendp))
	    goto invalid_data;
	  unit_length = read_8ubyte_unaligned_inc (dbg, linep);
	  length = 8;
	}

      /* Check whether we have enough room in the section.  */
      if (unit_length < 2 + length + 5 * 1
	  || unlikely (linep + unit_length > lineendp))
	goto invalid_data;
      lineendp = linep + unit_length;

      /* The next element of the header is the version identifier.  */
      uint_fast16_t version = read_2ubyte_unaligned_inc (dbg, linep);
      if (unlikely (version > DWARF_VERSION))
	{
	  __libdw_seterrno (DWARF_E_VERSION);
	  goto out;
	}

      /* Next comes the header length.  */
      Dwarf_Word header_length;
      if (length == 4)
	header_length = read_4ubyte_unaligned_inc (dbg, linep);
      else
	header_length = read_8ubyte_unaligned_inc (dbg, linep);
      const unsigned char *header_start = linep;

      /* Next the minimum instruction length.  */
      uint_fast8_t minimum_instr_len = *linep++;

        /* Then the flag determining the default value of the is_stmt
	   register.  */
      uint_fast8_t default_is_stmt = *linep++;

      /* Now the line base.  */
      int_fast8_t line_base = *((int_fast8_t *) linep);
      ++linep;

      /* And the line range.  */
      uint_fast8_t line_range = *linep++;

      /* The opcode base.  */
      uint_fast8_t opcode_base = *linep++;

      /* Remember array with the standard opcode length (-1 to account for
	 the opcode with value zero not being mentioned).  */
      const uint8_t *standard_opcode_lengths = linep - 1;
      linep += opcode_base - 1;
      if (unlikely (linep >= lineendp))
	goto invalid_data;

      /* First comes the list of directories.  Add the compilation
	 directory first since the index zero is used for it.  */
      struct dirlist
      {
	const char *dir;
	size_t len;
	struct dirlist *next;
      } comp_dir_elem =
	{
	  .dir = comp_dir,
	  .len = comp_dir ? strlen (comp_dir) : 0,
	  .next = NULL
	};
      struct dirlist *dirlist = &comp_dir_elem;
      unsigned int ndirlist = 1;

      // XXX Directly construct array to conserve memory?
      while (*linep != 0)
	{
	  struct dirlist *new_dir =
	    (struct dirlist *) alloca (sizeof (*new_dir));

	  new_dir->dir = (char *) linep;
	  uint8_t *endp = memchr (linep, '\0', lineendp - linep);
	  if (endp == NULL)
	    goto invalid_data;
	  new_dir->len = endp - linep;
	  new_dir->next = dirlist;
	  dirlist = new_dir;
	  ++ndirlist;
	  linep = endp + 1;
	}
      /* Skip the final NUL byte.  */
      ++linep;

      /* Rearrange the list in array form.  */
      struct dirlist **dirarray
	= (struct dirlist **) alloca (ndirlist * sizeof (*dirarray));
      for (unsigned int n = ndirlist; n-- > 0; dirlist = dirlist->next)
	dirarray[n] = dirlist;

      /* Now read the files.  */
      struct filelist null_file =
	{
	  .info =
	  {
	    .name = "???",
	    .mtime = 0,
	    .length = 0
	  },
	  .next = NULL
	};
      struct filelist *filelist = &null_file;
      unsigned int nfilelist = 1;

      if (unlikely (linep >= lineendp))
	goto invalid_data;
      while (*linep != 0)
	{
	  struct filelist *new_file =
	    (struct filelist *) alloca (sizeof (*new_file));

	  /* First comes the file name.  */
	  char *fname = (char *) linep;
	  uint8_t *endp = memchr (fname, '\0', lineendp - linep);
	  if (endp == NULL)
	    goto invalid_data;
	  size_t fnamelen = endp - (uint8_t *) fname;
	  linep = endp + 1;

	  /* Then the index.  */
	  Dwarf_Word diridx;
	  get_uleb128 (diridx, linep);
	  if (unlikely (diridx >= ndirlist))
	    {
	      __libdw_seterrno (DWARF_E_INVALID_DIR_IDX);
	      goto out;
	    }

	  if (*fname == '/')
	    /* It's an absolute path.  */
	    new_file->info.name = fname;
	  else
	    {
	      new_file->info.name = libdw_alloc (dbg, char, 1,
						 dirarray[diridx]->len + 1
						 + fnamelen + 1);
              char *cp = new_file->info.name;

              if (dirarray[diridx]->dir != NULL)
		{
		  /* This value could be NULL in case the DW_AT_comp_dir
		     was not present.  We cannot do much in this case.
		     The easiest thing is to convert the path in an
                   absolute path.  */
		  cp = stpcpy (cp, dirarray[diridx]->dir);
		}
              *cp++ = '/';
              strcpy (cp, fname);
	      assert (strlen (new_file->info.name)
		      < dirarray[diridx]->len + 1 + fnamelen + 1);
            }

	  /* Next comes the modification time.  */
	  get_uleb128 (new_file->info.mtime, linep);

	  /* Finally the length of the file.  */
	  get_uleb128 (new_file->info.length, linep);

	  new_file->next = filelist;
	  filelist = new_file;
	  ++nfilelist;
	}
      /* Skip the final NUL byte.  */
      ++linep;

      /* Consistency check.  */
      if (unlikely (linep != header_start + header_length))
	{
	  __libdw_seterrno (DWARF_E_INVALID_DWARF);
	  goto out;
	}

        /* We are about to process the statement program.  Initialize the
	   state machine registers (see 6.2.2 in the v2.1 specification).  */
      Dwarf_Word address = 0;
      size_t file = 1;
      size_t line = 1;
      size_t column = 0;
      uint_fast8_t is_stmt = default_is_stmt;
      int basic_block = 0;
      int prologue_end = 0;
      int epilogue_begin = 0;

        /* Process the instructions.  */
      struct linelist *linelist = NULL;
      unsigned int nlinelist = 0;
      while (linep < lineendp)
	{
	  struct linelist *new_line;
	  unsigned int opcode;
	  unsigned int u128;
	  int s128;

	  /* Read the opcode.  */
	  opcode = *linep++;

	  /* Is this a special opcode?  */
	  if (likely (opcode >= opcode_base))
	    {
	      /* Yes.  Handling this is quite easy since the opcode value
		 is computed with

		 opcode = (desired line increment - line_base)
		           + (line_range * address advance) + opcode_base
	      */
	      int line_increment = (line_base
				    + (opcode - opcode_base) % line_range);
	      unsigned int address_increment = (minimum_instr_len
						* ((opcode - opcode_base)
						   / line_range));

	      /* Perform the increments.  */
	      line += line_increment;
	      address += address_increment;

	      /* Add a new line with the current state machine values.  */
	      NEW_LINE (0);

	      /* Reset the flags.  */
	      basic_block = 0;
	      prologue_end = 0;
	      epilogue_begin = 0;
	    }
	  else if (opcode == 0)
	    {
	      /* This an extended opcode.  */
	      if (unlikely (linep + 2 > lineendp))
		goto invalid_data;

	      /* The length.  */
	      unsigned int len = *linep++;

	      if (unlikely (linep + len > lineendp))
		goto invalid_data;

	      /* The sub-opcode.  */
	      opcode = *linep++;

	      switch (opcode)
		{
		case DW_LNE_end_sequence:
		  /* Add a new line with the current state machine values.
		     The is the end of the sequence.  */
		  NEW_LINE (1);

		  /* Reset the registers.  */
		  address = 0;
		  file = 1;
		  line = 1;
		  column = 0;
		  is_stmt = default_is_stmt;
		  basic_block = 0;
		  prologue_end = 0;
		  epilogue_begin = 0;
		  break;

		case DW_LNE_set_address:
		  /* The value is an address.  The size is defined as
		     apporiate for the target machine.  We use the
		     address size field from the CU header.  */
		  if (cu->address_size == 4)
		    address = read_4ubyte_unaligned_inc (dbg, linep);
		  else
		    address = read_8ubyte_unaligned_inc (dbg, linep);
		  break;

		case DW_LNE_define_file:
		  {
		    char *fname = (char *) linep;
		    uint8_t *endp = memchr (linep, '\0', lineendp - linep);
		    if (endp == NULL)
		      goto invalid_data;
		    size_t fnamelen = endp - linep;
		    linep = endp + 1;

		    unsigned int diridx;
		    get_uleb128 (diridx, linep);
		    Dwarf_Word mtime;
		    get_uleb128 (mtime, linep);
		    Dwarf_Word filelength;
		    get_uleb128 (filelength, linep);

		    struct filelist *new_file =
		      (struct filelist *) alloca (sizeof (*new_file));
		    if (fname[0] == '/')
		      new_file->info.name = fname;
		    else
		      {
			new_file->info.name =
			  libdw_alloc (dbg, char, 1, (dirarray[diridx]->len + 1
						      + fnamelen + 1));
			char *cp = new_file->info.name;

			if (dirarray[diridx]->dir != NULL)
			  /* This value could be NULL in case the
			     DW_AT_comp_dir was not present.  We
			     cannot do much in this case.  The easiest
			     thing is to convert the path in an
			     absolute path.  */
			  cp = stpcpy (cp, dirarray[diridx]->dir);
			*cp++ = '/';
			strcpy (cp, fname);
		      }

		    new_file->info.mtime = mtime;
		    new_file->info.length = filelength;
		    new_file->next = filelist;
		    filelist = new_file;
		    ++nfilelist;
		  }
		  break;

		default:
		  /* Unknown, ignore it.  */
		  linep += len - 1;
		  break;
		}
	    }
	  else if (opcode <= DW_LNS_set_epilogue_begin)
	    {
	      /* This is a known standard opcode.  */
	      switch (opcode)
		{
		case DW_LNS_copy:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  /* Add a new line with the current state machine values.  */
		  NEW_LINE (0);

		  /* Reset the flags.  */
		  basic_block = 0;
		  /* XXX Whether the following two lines are necessary is
		     unclear.  I guess the current v2.1 specification has
		     a bug in that it says clearing these two registers is
		     not necessary.  */
		  prologue_end = 0;
		  epilogue_begin = 0;
		  break;

		case DW_LNS_advance_pc:
		  /* Takes one uleb128 parameter which is added to the
		     address.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 1))
		    goto invalid_data;

		  get_uleb128 (u128, linep);
		  address += minimum_instr_len * u128;
		  break;

		case DW_LNS_advance_line:
		  /* Takes one sleb128 parameter which is added to the
		     line.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 1))
		    goto invalid_data;

		  get_sleb128 (s128, linep);
		  line += s128;
		  break;

		case DW_LNS_set_file:
		  /* Takes one uleb128 parameter which is stored in file.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 1))
		    goto invalid_data;

		  get_uleb128 (u128, linep);
		  file = u128;
		  break;

		case DW_LNS_set_column:
		  /* Takes one uleb128 parameter which is stored in column.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 1))
		    goto invalid_data;

		  get_uleb128 (u128, linep);
		  column = u128;
		  break;

		case DW_LNS_negate_stmt:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  is_stmt = 1 - is_stmt;
		  break;

		case DW_LNS_set_basic_block:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  basic_block = 1;
		  break;

		case DW_LNS_const_add_pc:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  address += (minimum_instr_len
			      * ((255 - opcode_base) / line_range));
		  break;

		case DW_LNS_fixed_advance_pc:
		  /* Takes one 16 bit parameter which is added to the
		     address.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 1))
		    goto invalid_data;

		  address += read_2ubyte_unaligned_inc (dbg, linep);
		  break;

		case DW_LNS_set_prologue_end:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  prologue_end = 1;
		  break;

		case DW_LNS_set_epilogue_begin:
		  /* Takes no argument.  */
		  if (unlikely (standard_opcode_lengths[opcode] != 0))
		    goto invalid_data;

		  epilogue_begin = 1;
		  break;
		}
	    }
	  else
	    {
	      /* This is a new opcode the generator but not we know about.
		 Read the parameters associated with it but then discard
		 everything.  Read all the parameters for this opcode.  */
	      for (int n = standard_opcode_lengths[opcode]; n > 0; --n)
		get_uleb128 (u128, linep);

	      /* Next round, ignore this opcode.  */
	      continue;
	    }
	}

      /* Put all the files in an array.  */
      Dwarf_Files *files = libdw_alloc (dbg, Dwarf_Files,
					sizeof (Dwarf_Files)
					+ nfilelist * sizeof (Dwarf_Fileinfo)
					+ (ndirlist + 1) * sizeof (char *),
					1);
      const char **dirs = (void *) &files->info[nfilelist];

      files->nfiles = nfilelist;
      while (nfilelist-- > 0)
	{
	  files->info[nfilelist] = filelist->info;
	  filelist = filelist->next;
	}
      assert (filelist == NULL);

      /* Put all the directory strings in an array.  */
      files->ndirs = ndirlist;
      for (unsigned int i = 0; i < ndirlist; ++i)
	dirs[i] = dirarray[i]->dir;
      dirs[ndirlist] = NULL;

      /* Remember the debugging descriptor.  */
      files->dbg = dbg;

      /* Make the file data structure available through the CU.  */
      cu->files = files;

      void *buf = libdw_alloc (dbg, Dwarf_Lines, (sizeof (Dwarf_Lines)
						  + (sizeof (Dwarf_Line)
						     * nlinelist)), 1);

      /* First use the buffer for the pointers, and sort the entries.
	 We'll write the pointers in the end of the buffer, and then
	 copy into the buffer from the beginning so the overlap works.  */
      assert (sizeof (Dwarf_Line) >= sizeof (Dwarf_Line *));
      Dwarf_Line **sortlines = (buf + sizeof (Dwarf_Lines)
				+ ((sizeof (Dwarf_Line)
				    - sizeof (Dwarf_Line *)) * nlinelist));

      /* The list is in LIFO order and usually they come in clumps with
	 ascending addresses.  So fill from the back to probably start with
	 runs already in order before we sort.  */
      unsigned int i = nlinelist;
      while (i-- > 0)
	{
	  sortlines[i] = &linelist->line;
	  linelist = linelist->next;
	}
      assert (linelist == NULL);

      /* Sort by ascending address.  */
      qsort (sortlines, nlinelist, sizeof sortlines[0], &compare_lines);

      /* Now that they are sorted, put them in the final array.
	 The buffers overlap, so we've clobbered the early elements
	 of SORTLINES by the time we're reading the later ones.  */
      cu->lines = buf;
      cu->lines->nlines = nlinelist;
      for (i = 0; i < nlinelist; ++i)
	{
	  cu->lines->info[i] = *sortlines[i];
	  cu->lines->info[i].files = files;
	}

      /* Success.  */
      res = 0;
    }
  else if (cu->lines != (void *) -1l)
    /* We already have the information.  */
    res = 0;

  if (likely (res == 0))
    {
      *lines = cu->lines;
      *nlines = cu->lines->nlines;
    }
 out:

  // XXX Eventually: unlocking here.

  return res;
}
INTDEF(dwarf_getsrclines)
