/* Interfaces for libdwfl.
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

#ifndef _LIBDWFL_H
#define _LIBDWFL_H	1

#include "libdw.h"
#include <stdio.h>

/* Handle for a session using the library.  */
typedef struct Dwfl Dwfl;

/* Handle for a module.  */
typedef struct Dwfl_Module Dwfl_Module;

/* Handle describing a line record.  */
typedef struct Dwfl_Line Dwfl_Line;

/* Callbacks.  */
typedef struct
{
  int (*find_elf) (Dwfl_Module *mod, void **userdata,
		   const char *modname, Dwarf_Addr base,
		   char **file_name, Elf **elfp);

  int (*find_debuginfo) (Dwfl_Module *mod, void **userdata,
			 const char *modname, Dwarf_Addr base,
			 const char *file_name,
			 const char *debuglink_file, GElf_Word debuglink_crc,
			 char **debuginfo_file_name);

  /* Fill *ADDR with the loaded address of the section called SECNAME in
     the given module.  Use (Dwarf_Addr) -1 if this section is omitted from
     accessible memory.  This is called exactly once for each SHF_ALLOC
     section that relocations affecting DWARF data refer to, so it can
     easily be used to collect state about the sections referenced.  */
  int (*section_address) (Dwfl_Module *mod, void **userdata,
			  const char *modname, Dwarf_Addr base,
			  const char *secname,
			  GElf_Word shndx, const GElf_Shdr *shdr,
			  Dwarf_Addr *addr);

  char **debuginfo_path;	/* See dwfl_standard_find_debuginfo.  */
} Dwfl_Callbacks;


#ifdef __cplusplus
extern "C" {
#endif

/* Start a new session with the library.  */
extern Dwfl *dwfl_begin (const Dwfl_Callbacks *callbacks)
  __nonnull_attribute__ (1);


/* End a session.  */
extern void dwfl_end (Dwfl *);

/* Return implementation's version string suitable for printing.  */
extern const char *dwfl_version (Dwfl *);

/* Return error code of last failing function call.  This value is kept
   separately for each thread.  */
extern int dwfl_errno (void);

/* Return error string for ERROR.  If ERROR is zero, return error string
   for most recent error or NULL if none occurred.  If ERROR is -1 the
   behaviour is similar to the last case except that not NULL but a legal
   string is returned.  */
extern const char *dwfl_errmsg (int err);


/* Start reporting the current set of segments and modules to the library.
   All existing segments are wiped.  Existing modules are marked to be
   deleted, and will not be found via dwfl_addrmodule et al if they are not
   re-reported before dwfl_report_end is called.  */
extern void dwfl_report_begin (Dwfl *dwfl);

/* Report that segment NDX begins at PHDR->p_vaddr + BIAS.
   If NDX is < 0, the value succeeding the last call's NDX
   is used instead (zero on the first call).

   If nonzero, the smallest PHDR->p_align value seen sets the
   effective page size for the address space DWFL describes.
   This is the granularity at which reported module boundary
   addresses will be considered to fall in or out of a segment.

   Returns -1 for errors, or NDX (or its assigned replacement) on success.

   When NDX is the value succeeding the last call's NDX (or is implicitly
   so as above), IDENT is nonnull and matches the value in the last call,
   and the PHDR and BIAS values reflect a segment that would be contiguous,
   in both memory and file, with the last segment reported, then this
   segment may be coalesced internally with preceding segments.  When given
   an address inside this segment, dwfl_addrsegment may return the NDX of a
   preceding contiguous segment.  To prevent coalesced segments, always
   pass a null pointer for IDENT.

   The values passed are not stored (except to track coalescence).
   The only information that can be extracted from DWFL later is the
   mapping of an address to a segment index that starts at or below
   it.  Reporting segments at all is optional.  Its only benefit to
   the caller is to offer this quick lookup via dwfl_addrsegment,
   or use other segment-based calls.  */
extern int dwfl_report_segment (Dwfl *dwfl, int ndx,
				const GElf_Phdr *phdr, GElf_Addr bias,
				const void *ident);

/* Report that a module called NAME spans addresses [START, END).
   Returns the module handle, either existing or newly allocated,
   or returns a null pointer for an allocation error.  */
extern Dwfl_Module *dwfl_report_module (Dwfl *dwfl, const char *name,
					Dwarf_Addr start, Dwarf_Addr end);

/* Report a module with start and end addresses computed from the ELF
   program headers in the given file, plus BASE.  For an ET_REL file,
   does a simple absolute section layout starting at BASE.
   FD may be -1 to open FILE_NAME.  On success, FD is consumed by the
   library, and the `find_elf' callback will not be used for this module.  */
extern Dwfl_Module *dwfl_report_elf (Dwfl *dwfl, const char *name,
				     const char *file_name, int fd,
				     GElf_Addr base);

/* Similar, but report the module for offline use.  All ET_EXEC files
   being reported must be reported before any relocatable objects.
   If this is used, dwfl_report_module and dwfl_report_elf may not be
   used in the same reporting session.  */
extern Dwfl_Module *dwfl_report_offline (Dwfl *dwfl, const char *name,
					 const char *file_name, int fd);


/* Finish reporting the current set of modules to the library.
   If REMOVED is not null, it's called for each module that
   existed before but was not included in the current report.
   Returns a nonzero return value from the callback.
   The callback may call dwfl_report_module; doing so with the
   details of the module being removed prevents its removal.
   DWFL cannot be used until this function has returned zero.  */
extern int dwfl_report_end (Dwfl *dwfl,
			    int (*removed) (Dwfl_Module *, void *,
					    const char *, Dwarf_Addr,
					    void *arg),
			    void *arg);

/* Start reporting additional modules to the library.  No calls but
   dwfl_report_* can be made on DWFL until dwfl_report_end is called.
   This is like dwfl_report_begin, but all the old modules are kept on.
   More dwfl_report_* calls can follow to add more modules.
   When dwfl_report_end is called, no old modules will be removed.  */
extern void dwfl_report_begin_add (Dwfl *dwfl);


/* Return the name of the module, and for each non-null argument store
   interesting details: *USERDATA is a location for storing your own
   pointer, **USERDATA is initially null; *START and *END give the address
   range covered by the module; *DWBIAS is the address bias for debugging
   information, and *SYMBIAS for symbol table entries (either is -1 if not
   yet accessed); *MAINFILE is the name of the ELF file, and *DEBUGFILE the
   name of the debuginfo file (might be equal to *MAINFILE; either is null
   if not yet accessed).  */
extern const char *dwfl_module_info (Dwfl_Module *mod, void ***userdata,
				     Dwarf_Addr *start, Dwarf_Addr *end,
				     Dwarf_Addr *dwbias, Dwarf_Addr *symbias,
				     const char **mainfile,
				     const char **debugfile);

/* Iterate through the modules, starting the walk with OFFSET == 0.
   Calls *CALLBACK for each module as long as it returns DWARF_CB_OK.
   When *CALLBACK returns another value, the walk stops and the
   return value can be passed as OFFSET to resume it.  Returns 0 when
   there are no more modules, or -1 for errors.  */
extern ptrdiff_t dwfl_getmodules (Dwfl *dwfl,
				  int (*callback) (Dwfl_Module *, void **,
						   const char *, Dwarf_Addr,
						   void *arg),
				  void *arg,
				  ptrdiff_t offset);

/* Find the module containing the given address.  */
extern Dwfl_Module *dwfl_addrmodule (Dwfl *dwfl, Dwarf_Addr address);

/* Find the segment, if any, and module, if any, containing ADDRESS.
   Returns a segment index returned by dwfl_report_segment, or -1
   if no segment matches the address.  Regardless of the return value,
   *MOD is always set to the module containing ADDRESS, or to null.  */
extern int dwfl_addrsegment (Dwfl *dwfl, Dwarf_Addr address, Dwfl_Module **mod);



/* Report the known build ID bits associated with a module.
   If VADDR is nonzero, it gives the absolute address where those
   bits are found within the module.  This can be called at any
   time, but is usually used immediately after dwfl_report_module.
   Once the module's main ELF file is opened, the ID note found
   there takes precedence and cannot be changed.  */
extern int dwfl_module_report_build_id (Dwfl_Module *mod,
					const unsigned char *bits, size_t len,
					GElf_Addr vaddr)
  __nonnull_attribute__ (2);

/* Extract the build ID bits associated with a module.
   Returns -1 for errors, 0 if no ID is known, or the number of ID bytes.
   When an ID is found, *BITS points to it; *VADDR is the absolute address
   at which the ID bits are found within the module, or 0 if unknown.

   This returns 0 when the module's main ELF file has not yet been loaded
   and its build ID bits were not reported.  To ensure the ID is always
   returned when determinable, call dwfl_module_getelf first.  */
extern int dwfl_module_build_id (Dwfl_Module *mod,
				 const unsigned char **bits, GElf_Addr *vaddr)
  __nonnull_attribute__ (2, 3);


/*** Standard callbacks ***/

/* These standard find_elf and find_debuginfo callbacks are
   controlled by a string specifying directories to look in.
   If `debuginfo_path' is set in the Dwfl_Callbacks structure
   and the char * it points to is not null, that supplies the
   string.  Otherwise a default path is used.

   If the first character of the string is + or - that enables or
   disables CRC32 checksum validation when it's necessary.  The
   remainder of the string is composed of elements separated by
   colons.  Each element can start with + or - to override the
   global checksum behavior.  This flag is never relevant when
   working with build IDs, but it's always parsed in the path
   string.  The remainder of the element indicates a directory.

   Searches by build ID consult only the elements naming absolute
   directory paths.  They look under those directories for a link
   named ".build-id/xx/yy" or ".build-id/xx/yy.debug", where "xxyy"
   is the lower-case hexadecimal representation of the ID bytes.

   In searches for debuginfo by name, if the remainder of the
   element is empty, the directory containing the main file is
   tried; if it's an absolute path name, the absolute directory path
   containing the main file is taken as a subdirectory of this path;
   a relative path name is taken as a subdirectory of the directory
   containing the main file.  Hence for /bin/ls, the default string
   ":.debug:/usr/lib/debug" says to look in /bin, then /bin/.debug,
   then /usr/lib/debug/bin, for the file name in the .gnu_debuglink
   section (or "ls.debug" if none was found).  */

/* Standard find_elf callback function working solely on build ID.
   This can be tried first by any find_elf callback, to use the
   bits passed to dwfl_module_report_build_id, if any.  */
extern int dwfl_build_id_find_elf (Dwfl_Module *, void **,
				   const char *, Dwarf_Addr,
				   char **, Elf **);

/* Standard find_debuginfo callback function working solely on build ID.
   This can be tried first by any find_debuginfo callback,
   to use the build ID bits from the main file when present.  */
extern int dwfl_build_id_find_debuginfo (Dwfl_Module *, void **,
					 const char *, Dwarf_Addr,
					 const char *, const char *,
					 GElf_Word, char **);

/* Standard find_debuginfo callback function.
   If a build ID is available, this tries first to use that.
   If there is no build ID or no valid debuginfo found by ID,
   it searches the debuginfo path by name, as described above.
   Any file found in the path is validated by build ID if possible,
   or else by CRC32 checksum if enabled, and skipped if it does not match.  */
extern int dwfl_standard_find_debuginfo (Dwfl_Module *, void **,
					 const char *, Dwarf_Addr,
					 const char *, const char *,
					 GElf_Word, char **);


/* This callback must be used when using dwfl_offline_* to report modules,
   if ET_REL is to be supported.  */
extern int dwfl_offline_section_address (Dwfl_Module *, void **,
					 const char *, Dwarf_Addr,
					 const char *, GElf_Word,
					 const GElf_Shdr *,
					 Dwarf_Addr *addr);


/* Callbacks for working with kernel modules in the running Linux kernel.  */
extern int dwfl_linux_kernel_find_elf (Dwfl_Module *, void **,
				       const char *, Dwarf_Addr,
				       char **, Elf **);
extern int dwfl_linux_kernel_module_section_address (Dwfl_Module *, void **,
						     const char *, Dwarf_Addr,
						     const char *, GElf_Word,
						     const GElf_Shdr *,
						     Dwarf_Addr *addr);

/* Call dwfl_report_elf for the running Linux kernel.
   Returns zero on success, -1 if dwfl_report_module failed,
   or an errno code if opening the kernel binary failed.  */
extern int dwfl_linux_kernel_report_kernel (Dwfl *dwfl);

/* Call dwfl_report_module for each kernel module in the running Linux kernel.
   Returns zero on success, -1 if dwfl_report_module failed,
   or an errno code if reading the list of modules failed.  */
extern int dwfl_linux_kernel_report_modules (Dwfl *dwfl);

/* Report a kernel and its modules found on disk, for offline use.
   If RELEASE starts with '/', it names a directory to look in;
   if not, it names a directory to find under /lib/modules/;
   if null, /lib/modules/`uname -r` is used.
   Returns zero on success, -1 if dwfl_report_module failed,
   or an errno code if finding the files on disk failed.

   If PREDICATE is not null, it is called with each module to be reported;
   its arguments are the module name, and the ELF file name or null if unknown,
   and its return value should be zero to skip the module, one to report it,
   or -1 to cause the call to fail and return errno.  */
extern int dwfl_linux_kernel_report_offline (Dwfl *dwfl, const char *release,
					     int (*predicate) (const char *,
							       const char *));


/* Call dwfl_report_module for each file mapped into the address space of PID.
   Returns zero on success, -1 if dwfl_report_module failed,
   or an errno code if opening the kernel binary failed.  */
extern int dwfl_linux_proc_report (Dwfl *dwfl, pid_t pid);

/* Similar, but reads an input stream in the format of Linux /proc/PID/maps
   files giving module layout, not the file for a live process.  */
extern int dwfl_linux_proc_maps_report (Dwfl *dwfl, FILE *);

/* Trivial find_elf callback for use with dwfl_linux_proc_report.
   This uses the module name as a file name directly and tries to open it
   if it begin with a slash, or handles the magic string "[vdso]".  */
extern int dwfl_linux_proc_find_elf (Dwfl_Module *mod, void **userdata,
				     const char *module_name, Dwarf_Addr base,
				     char **file_name, Elf **);

/* Standard argument parsing for using a standard callback set.  */
struct argp;
extern const struct argp *dwfl_standard_argp (void) __attribute__ ((const));


/*** Relocation of addresses from Dwfl ***/

/* Return the number of relocatable bases associated with the module,
   which is zero for ET_EXEC and one for ET_DYN.  Returns -1 for errors.  */
extern int dwfl_module_relocations (Dwfl_Module *mod);

/* Return the relocation base index associated with the *ADDRESS location,
   and adjust *ADDRESS to be an offset relative to that base.
   Returns -1 for errors.  */
extern int dwfl_module_relocate_address (Dwfl_Module *mod,
					 Dwarf_Addr *address);

/* Return the ELF section name for the given relocation base index;
   if SHNDXP is not null, set *SHNDXP to the ELF section index.
   For ET_DYN, returns "" and sets *SHNDXP to SHN_ABS; the relocation
   base is the runtime start address reported for the module.
   Returns null for errors.  */
extern const char *dwfl_module_relocation_info (Dwfl_Module *mod,
						unsigned int idx,
						GElf_Word *shndxp);

/* Validate that ADDRESS and ADDRESS+OFFSET lie in a known module
   and both within the same contiguous region for relocation purposes.
   Returns zero for success and -1 for errors.  */
extern int dwfl_validate_address (Dwfl *dwfl,
				  Dwarf_Addr address, Dwarf_Sword offset);


/*** ELF access functions ***/

/* Fetch the module main ELF file (where the allocated sections
   are found) for use with libelf.  If successful, fills in *BIAS
   with the difference between addresses within the loaded module
   and those in symbol tables or Dwarf information referring to it.  */
extern Elf *dwfl_module_getelf (Dwfl_Module *, GElf_Addr *bias);

/* Return the number of symbols in the module's symbol table,
   or -1 for errors.  */
extern int dwfl_module_getsymtab (Dwfl_Module *mod);

/* Fetch one entry from the module's symbol table.  On errors, returns
   NULL.  If successful, fills in *SYM and returns the string for st_name.
   This works like gelf_getsym except that st_value is always adjusted
   to an absolute value based on the module's location.  If SHNDXP is
   non-null, it's set with the section index (whether from st_shndx or
   extended index table).  */
extern const char *dwfl_module_getsym (Dwfl_Module *mod, int ndx,
				       GElf_Sym *sym, GElf_Word *shndxp)
  __nonnull_attribute__ (3);

/* Find the symbol that ADDRESS lies inside, and return its name.  */
extern const char *dwfl_module_addrname (Dwfl_Module *mod, GElf_Addr address);

/* Find the symbol that ADDRESS lies inside, and return detailed
   information as for dwfl_module_getsym (above).  */
extern const char *dwfl_module_addrsym (Dwfl_Module *mod, GElf_Addr address,
					GElf_Sym *sym, GElf_Word *shndxp)
  __nonnull_attribute__ (3);

/* Find the ELF section that *ADDRESS lies inside and return it.
   On success, adjusts *ADDRESS to be relative to the section,
   and sets *BIAS to the difference between addresses used in
   the returned section's headers and run-time addresses.  */
extern Elf_Scn *dwfl_module_address_section (Dwfl_Module *mod,
					     Dwarf_Addr *address,
					     Dwarf_Addr *bias)
  __nonnull_attribute__ (2, 3);


/*** Dwarf access functions ***/

/* Fetch the module's debug information for use with libdw.
   If successful, fills in *BIAS with the difference between
   addresses within the loaded module and those  to use with libdw.  */
extern Dwarf *dwfl_module_getdwarf (Dwfl_Module *, Dwarf_Addr *bias)
     __nonnull_attribute__ (2);

/* Get the libdw handle for each module.  */
extern ptrdiff_t dwfl_getdwarf (Dwfl *,
				int (*callback) (Dwfl_Module *, void **,
						 const char *, Dwarf_Addr,
						 Dwarf *, Dwarf_Addr, void *),
				void *arg, ptrdiff_t offset);

/* Look up the module containing ADDR and return its debugging information,
   loading it if necessary.  */
extern Dwarf *dwfl_addrdwarf (Dwfl *dwfl, Dwarf_Addr addr, Dwarf_Addr *bias)
     __nonnull_attribute__ (3);


/* Find the CU containing ADDR and return its DIE.  */
extern Dwarf_Die *dwfl_addrdie (Dwfl *dwfl, Dwarf_Addr addr, Dwarf_Addr *bias)
     __nonnull_attribute__ (3);
extern Dwarf_Die *dwfl_module_addrdie (Dwfl_Module *mod,
				       Dwarf_Addr addr, Dwarf_Addr *bias)
     __nonnull_attribute__ (3);

/* Iterate through the CUs, start with null for LASTCU.  */
extern Dwarf_Die *dwfl_nextcu (Dwfl *dwfl, Dwarf_Die *lastcu, Dwarf_Addr *bias)
     __nonnull_attribute__ (3);
extern Dwarf_Die *dwfl_module_nextcu (Dwfl_Module *mod,
				      Dwarf_Die *lastcu, Dwarf_Addr *bias)
     __nonnull_attribute__ (3);

/* Return the module containing the CU DIE.  */
extern Dwfl_Module *dwfl_cumodule (Dwarf_Die *cudie);


/* Cache the source line information fo the CU and return the
   number of Dwfl_Line entries it has.  */
extern int dwfl_getsrclines (Dwarf_Die *cudie, size_t *nlines);

/* Access one line number entry within the CU.  */
extern Dwfl_Line *dwfl_onesrcline (Dwarf_Die *cudie, size_t idx);

/* Get source for address.  */
extern Dwfl_Line *dwfl_module_getsrc (Dwfl_Module *mod, Dwarf_Addr addr);
extern Dwfl_Line *dwfl_getsrc (Dwfl *dwfl, Dwarf_Addr addr);

/* Get address for source.  */
extern int dwfl_module_getsrc_file (Dwfl_Module *mod,
				    const char *fname, int lineno, int column,
				    Dwfl_Line ***srcsp, size_t *nsrcs);

/* Return the module containing this line record.  */
extern Dwfl_Module *dwfl_linemodule (Dwfl_Line *line);

/* Return the CU containing this line record.  */
extern Dwarf_Die *dwfl_linecu (Dwfl_Line *line);

/* Return the source file name and fill in other information.
   Arguments may be null for unneeded fields.  */
extern const char *dwfl_lineinfo (Dwfl_Line *line, Dwarf_Addr *addr,
				  int *linep, int *colp,
				  Dwarf_Word *mtime, Dwarf_Word *length);

/* Return the compilation directory (AT_comp_dir) from this line's CU.  */
extern const char *dwfl_line_comp_dir (Dwfl_Line *line);


/*** Machine backend access functions ***/

/* Return location expression to find return value given a
   DW_TAG_subprogram, DW_TAG_subroutine_type, or similar DIE describing
   function itself (whose DW_AT_type attribute describes its return type).
   The given DIE must come from the given module.  Returns -1 for errors.
   Returns zero if the function has no return value (e.g. "void" in C).
   Otherwise, *LOCOPS gets a location expression to find the return value,
   and returns the number of operations in the expression.  The pointer is
   permanently allocated at least as long as the module is live.  */
extern int dwfl_module_return_value_location (Dwfl_Module *mod,
					      Dwarf_Die *functypedie,
					      const Dwarf_Op **locops);

/* Enumerate the DWARF register numbers and their names.
   For each register, CALLBACK gets its DWARF number, a string describing
   the register set (such as "integer" or "FPU"), a prefix used in
   assembler syntax (such as "%" or "$", may be ""), and the name for the
   register (contains identifier characters only, possibly all digits).
   The REGNAME string is valid only during the callback. */
extern int dwfl_module_register_names (Dwfl_Module *mod,
				       int (*callback) (void *arg,
							int regno,
							const char *setname,
							const char *prefix,
							const char *regname,
							int bits, int type),
				       void *arg);


#ifdef __cplusplus
}
#endif

#endif	/* libdwfl.h */
