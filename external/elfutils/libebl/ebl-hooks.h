/* Backend hook signatures internal interface for libebl.
   Copyright (C) 2000,2001,2002,2004,2005,2006,2007,2008 Red Hat, Inc.
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

/* Return symbol representaton of object file type.  */
const char *EBLHOOK(object_type_name) (int, char *, size_t);

/* Return symbolic representation of relocation type.  */
const char *EBLHOOK(reloc_type_name) (int, char *, size_t);

/* Check relocation type.  */
bool EBLHOOK(reloc_type_check) (int);

/* Check if relocation type is for simple absolute relocations.  */
Elf_Type EBLHOOK(reloc_simple_type) (Ebl *, int);

/* Check relocation type use.  */
bool EBLHOOK(reloc_valid_use) (Elf *, int);

/* Return true if the symbol type is that referencing the GOT.  */
bool EBLHOOK(gotpc_reloc_check) (Elf *, int);

/* Return symbolic representation of segment type.  */
const char *EBLHOOK(segment_type_name) (int, char *, size_t);

/* Return symbolic representation of section type.  */
const char *EBLHOOK(section_type_name) (int, char *, size_t);

/* Return section name.  */
const char *EBLHOOK(section_name) (int, int, char *, size_t);

/* Return next machine flag name.  */
const char *EBLHOOK(machine_flag_name) (GElf_Word *);

/* Check whether machine flags are valid.  */
bool EBLHOOK(machine_flag_check) (GElf_Word);

/* Check whether SHF_MASKPROC flag bits are valid.  */
bool EBLHOOK(machine_section_flag_check) (GElf_Xword);

/* Check whether the section with the given index, header, and name
   is a special machine section that is valid despite a combination
   of flags or other details that are not generically valid.  */
bool EBLHOOK(check_special_section) (Ebl *, int,
				     const GElf_Shdr *, const char *);

/* Return symbolic representation of symbol type.  */
const char *EBLHOOK(symbol_type_name) (int, char *, size_t);

/* Return symbolic representation of symbol binding.  */
const char *EBLHOOK(symbol_binding_name) (int, char *, size_t);

/* Return symbolic representation of dynamic tag.  */
const char *EBLHOOK(dynamic_tag_name) (int64_t, char *, size_t);

/* Check dynamic tag.  */
bool EBLHOOK(dynamic_tag_check) (int64_t);

/* Combine section header flags values.  */
GElf_Word EBLHOOK(sh_flags_combine) (GElf_Word, GElf_Word);

/* Return symbolic representation of OS ABI.  */
const char *EBLHOOK(osabi_name) (int, char *, size_t);

/* Name of a note entry type for core files.  */
const char *EBLHOOK(core_note_type_name) (uint32_t, char *, size_t);

/* Name of a note entry type for object files.  */
const char *EBLHOOK(object_note_type_name) (uint32_t, char *, size_t);

/* Describe core note format.  */
int EBLHOOK(core_note) (GElf_Word, GElf_Word, GElf_Word *, size_t *,
			const Ebl_Register_Location **,
			size_t *, const Ebl_Core_Item **);

/* Handle object file note.  */
bool EBLHOOK(object_note) (const char *, uint32_t, uint32_t, const char *);

/* Check object attribute.  */
bool EBLHOOK(check_object_attribute) (Ebl *, const char *, int, uint64_t,
				      const char **, const char **);

/* Describe auxv element type.  */
int EBLHOOK(auxv_info) (GElf_Xword, const char **, const char **);

/* Check section name for being that of a debug informatino section.  */
bool EBLHOOK(debugscn_p) (const char *);

/* Check whether given relocation is a copy relocation.  */
bool EBLHOOK(copy_reloc_p) (int);

/* Check whether given relocation is a no-op relocation.  */
bool EBLHOOK(none_reloc_p) (int);

/* Check whether given relocation is a relative relocation.  */
bool EBLHOOK(relative_reloc_p) (int);

/* Check whether given symbol's value is ok despite normal checks.  */
bool EBLHOOK(check_special_symbol) (Elf *, GElf_Ehdr *, const GElf_Sym *,
			      const char *, const GElf_Shdr *);

/* Check if backend uses a bss PLT in this file.  */
bool EBLHOOK(bss_plt_p) (Elf *, GElf_Ehdr *);

/* Return location expression to find return value given the
   DW_AT_type DIE of a DW_TAG_subprogram DIE.  */
int EBLHOOK(return_value_location) (Dwarf_Die *functypedie,
				    const Dwarf_Op **locp);

/* Return register name information.  */
ssize_t EBLHOOK(register_info) (Ebl *ebl,
				int regno, char *name, size_t namelen,
				const char **prefix, const char **setname,
				int *bits, int *type);

/* Return system call ABI registers.  */
int EBLHOOK(syscall_abi) (Ebl *ebl, int *sp, int *pc,
			  int *callno, int args[6]);

/* Disassembler function.  */
int EBLHOOK(disasm) (const uint8_t **startp, const uint8_t *end,
		     GElf_Addr addr, const char *fmt, DisasmOutputCB_t outcb,
		     DisasmGetSymCB_t symcb, void *outcbarg, void *symcbarg);


/* Destructor for ELF backend handle.  */
void EBLHOOK(destr) (struct ebl *);
