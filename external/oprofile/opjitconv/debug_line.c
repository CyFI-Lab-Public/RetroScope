/**
 * @file debug_line.c
 * DWARF 2 debug line info creation helper
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdio.h>
#include <bfd.h>
#include <limits.h>

#include "opjitconv.h"
#include "jitdump.h"
#include "opagent.h"
#include "op_libiberty.h"
#include "op_growable_buffer.h"

/*
 * Terminology comes from the TIS DWARF Debugging Information Format
 * version 2.0
 */

typedef uint32_t uword;
typedef uint16_t uhalf;
typedef int32_t  sword;
typedef int16_t  shalf;
typedef uint8_t  ubyte;
typedef int8_t   sbyte;

/*
 * Many of the following enum are incomplete and define only the subset
 * of DWARF we use.
 */
enum lns_opcode {
	DW_LNS_copy=1,
	DW_LNS_advance_pc,
	DW_LNS_advance_line,
	DW_LNS_set_file,
	DW_LNS_set_column,
	DW_LNS_negate_stmt,
	DW_LNS_set_basic_block,
	DW_LNS_const_add_pc,
	DW_LNS_fixed_advance_pc,

	/* Adding new opcode needs an update of the standard_opcode_length
	 * array */

	DW_LNS_max_opcode,
};

enum lne_opcode {
	DW_LNE_end_sequence = 1,
	DW_LNE_set_address,
	DW_LNE_define_file
};

enum dw_tag {
	DW_TAG_compile_unit = 0x11,
};

enum dw_at {
	DW_AT_name = 0x03,
	DW_AT_stmt_list = 0x10,
	DW_AT_low_pc,
	DW_AT_high_pc,
	DW_AT_language,
	DW_AT_compdir = 0x1b,
	DW_AT_producer = 0x25,
};

enum dw_children {
	DW_CHILDREN_no,
	DW_CHILDREN_yes
};

enum dw_form {
	DW_FORM_data4 = 0x06,
};

struct debug_line_header {
	// Not counting this field
	uword total_length;
	// version number (2 currently)
	uhalf version;
	// relative offset from next field to
	// program statement
	uword prolog_length;
	ubyte minimum_instruction_length;
	ubyte default_is_stmt;
	// line_base - see DWARF 2 specs
	sbyte line_base;
	// line_range - see DWARF 2 specs
	ubyte line_range;
	// number of opcode + 1
	ubyte opcode_base;
	/* follow the array of opcode args nr: ubytes [nr_opcode_base] */
	/* follow the search directories index, zero terminated string
	 * terminated by an empty string.
	 */
	/* follow an array of { filename, LEB128, LEB128, LEB128 }, first is
	 * the directory index entry, 0 means current directory, then mtime
	 * and filesize, last entry is followed by en empty string.
	 */
	/* follow the first program statement */
} __attribute__((packed));

/* DWARF 2 spec talk only about one possible compilation unit header while
 * binutils can handle two flavours of dwarf 2, 32 and 64 bits, this is not
 * related to the used arch, an ELF 32 can hold more than 4 Go of debug
 * information. For now we handle only DWARF 2 32 bits comp unit. It'll only
 * become a problem if we generate more than 4GB of debug information.
 */
struct compilation_unit_header {
	uword total_length;
	uhalf version;
	uword debug_abbrev_offset;
	ubyte pointer_size;
} __attribute__((packed));

/* field filled at run time are marked with -1 */
static struct debug_line_header const default_debug_line_header = {
	-1,
	2,
	-1,
	1,	/* could be better when min instruction size != 1 */
	1,	/* we don't take care about basic block */
	-5,	/* sensible value for line base ... */
	14,     /* ... and line range are guessed statically */
	DW_LNS_max_opcode
};

static ubyte const standard_opcode_length[DW_LNS_max_opcode - 1] =
{
	0, 1, 1, 1, 1, 0, 0, 0, 1
};

/* field filled at run time are marked with -1 */
static struct compilation_unit_header const default_comp_unit_header = {
	-1,
	2,
	0,     /* we reuse the same abbrev entries for all comp unit */
	-1
};


static void emit_uword(struct growable_buffer * b, uword data)
{
	add_data(b, &data, sizeof(uword));
}


static void emit_string(struct growable_buffer * b, char const * s)
{
	add_data(b, s, strlen(s) + 1);
}


static void emit_unsigned_LEB128(struct growable_buffer * b,
				 unsigned long data)
{
	do {
		ubyte cur = data & 0x7F;
		data >>= 7;
		if (data)
			cur |= 0x80;
		add_data(b, &cur, 1);
	} while (data);
}


static void emit_signed_LEB128(struct growable_buffer * b, long data)
{
	int more = 1;
	int negative = data < 0;
	int size = sizeof(long) * CHAR_BIT;
	while (more) {
		ubyte cur = data & 0x7F;
		data >>= 7;
		if (negative)
			data |= - (1 << (size - 7));
		if ((data == 0 && !(cur & 0x40)) ||
		    (data == -1l && (cur & 0x40)))
			more = 0;
		else
			cur |= 0x80;
		add_data(b, &cur, 1);
	}
}


static void emit_extended_opcode(struct growable_buffer * b, ubyte opcode,
				 void * data, size_t data_len)
{
	add_data(b, "", 1);
	emit_unsigned_LEB128(b, data_len + 1);
	add_data(b, &opcode, 1);
	add_data(b, data, data_len);
}


static void emit_opcode(struct growable_buffer * b, ubyte opcode)
{
	add_data(b, &opcode, 1);
}


static void emit_opcode_signed(struct growable_buffer * b,
			       ubyte opcode, long data)
{
	add_data(b, &opcode, 1);
	emit_signed_LEB128(b, data);
}


static void emit_opcode_unsigned(struct growable_buffer * b, ubyte opcode, 
				 unsigned long data)
{
	add_data(b, &opcode, 1);
	emit_unsigned_LEB128(b, data);
}


static void emit_advance_pc(struct growable_buffer * b, unsigned long delta_pc)
{
	emit_opcode_unsigned(b, DW_LNS_advance_pc, delta_pc);
}


static void emit_advance_lineno(struct growable_buffer * b, long delta_lineno)
{
	emit_opcode_signed(b, DW_LNS_advance_line, delta_lineno);
}


static void emit_lne_end_of_sequence(struct growable_buffer * b)
{
	emit_extended_opcode(b, DW_LNE_end_sequence, NULL, 0);
}


static void emit_set_file(struct growable_buffer * b, unsigned long index)
{
	emit_opcode_unsigned(b, DW_LNS_set_file, index);
}


static void emit_lne_define_filename(struct growable_buffer * b,
				     char const * filename)
{
	/* emit_extended_opcode() can't be used here, we have additional
	 * data to output and the len field will be miscalculated. */
	add_data(b, "", 1);
	/* strlen(filename) + zero terminator + len field + 3 bytes for the dir
	 * entry, timestamp and filesize */
	emit_unsigned_LEB128(b, strlen(filename) + 5);
	emit_opcode(b, DW_LNE_define_file);
	emit_string(b, filename);
	add_data(b, "\0\0\0", 3);
}


static void emit_lne_set_address(struct growable_buffer * b,
				 void const * address)
{
	emit_extended_opcode(b, DW_LNE_set_address, &address, sizeof(address));
}


static ubyte get_special_opcode(struct debug_line_info const * line,
	unsigned int last_lineno, unsigned long last_vma)
{
	unsigned int temp;
	unsigned long delta_addr;

	/* See TIS DWARF Debugging Information Format version 2.0 § 6.2.5.1 */

	temp = (line->lineno - last_lineno) -
		default_debug_line_header.line_base;
	if (temp >= default_debug_line_header.line_range)
		return 0;

	delta_addr = (line->vma - last_vma) /
		default_debug_line_header.minimum_instruction_length;
	/* This is not sufficient to ensure opcode will be in [0-256] but
	 * sufficient to ensure when summing with the delta lineno we will
	 * not overflow the unsigned long opcode */
	if (delta_addr <= 256 / default_debug_line_header.line_range) {
		unsigned long opcode = temp +
			(delta_addr * default_debug_line_header.line_range) +
			default_debug_line_header.opcode_base;

		return opcode <= 255 ? opcode : 0;
	}

	return 0;
}


static void emit_lineno_info(struct growable_buffer * b,
	struct debug_line_info const * line, size_t nr_entry,
	unsigned long code_addr)
{
	size_t i;

	/*
	 * Machine state at start of a statement program
	 * address = 0
	 * file    = 1
	 * line    = 1
	 * column  = 0
	 * is_stmt = default_is_stmt as given in the debug_line_header
	 * basic block = 0
	 * end sequence = 0
	 */

	/* start state of the state machine we take care of */
	unsigned long last_vma = code_addr;
	unsigned int last_lineno = 1;
	char const  * cur_filename = NULL;
	unsigned long cur_file_index = 0;

	/* FIXME: relocatable address? */
	emit_lne_set_address(b, (void const *)code_addr);
	emit_advance_lineno(b, line[0].lineno - last_lineno);
	last_lineno = line[0].lineno;
	emit_lne_define_filename(b, line[0].filename);
	cur_filename = line[0].filename;
	emit_set_file(b, ++cur_file_index);
	emit_opcode(b, DW_LNS_copy);
	

	for (i = 0; i < nr_entry; i++) {
		int need_copy = 0;
		ubyte special_opcode;

		if (!cur_filename || strcmp(cur_filename, line[i].filename)) {
			emit_lne_define_filename(b, line[i].filename);
			cur_filename = line[i].filename;
			emit_set_file(b, ++cur_file_index);
			need_copy = 1;
		}
		if ((special_opcode = get_special_opcode(&line[i],
				last_lineno, last_vma)) != 0) {
			last_lineno = line[i].lineno;
			last_vma = line[i].vma;
			emit_opcode(b, special_opcode);
		} else {
			if (last_lineno != line[i].lineno) {
				emit_advance_lineno(b,
					line[i].lineno - last_lineno);
				last_lineno = line[i].lineno;
				need_copy = 1;
			}
			if (last_vma != line[i].vma) {
				emit_advance_pc(b, line[i].vma - last_vma);
				last_vma = line[i].vma;
				need_copy = 1;
			}
			if (need_copy)
				emit_opcode(b, DW_LNS_copy);
		}
	}
}


static void add_debug_line(struct growable_buffer * b,
	struct debug_line_info const * line, size_t nr_entry,
	unsigned long code_addr)
{
	struct debug_line_header * dbg_header;
	size_t old_size;

	old_size = b->size;

	add_data(b, &default_debug_line_header,
		 sizeof(default_debug_line_header));
	add_data(b, &standard_opcode_length,  sizeof(standard_opcode_length));

	// empty directory entry
	add_data(b, "", 1);

	// empty filename directory
	add_data(b, "", 1);

	dbg_header = b->p + old_size;
	dbg_header->prolog_length = (b->size - old_size) -
		offsetof(struct debug_line_header, minimum_instruction_length);

	emit_lineno_info(b, line, nr_entry, code_addr);

	emit_lne_end_of_sequence(b);

	dbg_header = b->p + old_size;
	dbg_header->total_length = (b->size - old_size) -
		offsetof(struct debug_line_header, version);
}


static void add_compilation_unit(struct growable_buffer * b,
				 size_t offset_debug_line)
{
	struct compilation_unit_header * comp_unit_header;

	size_t old_size = b->size;

	add_data(b, &default_comp_unit_header,
		 sizeof(default_comp_unit_header));

	emit_unsigned_LEB128(b, 1);
	emit_uword(b, offset_debug_line);

	comp_unit_header = b->p + old_size;
	comp_unit_header->total_length = (b->size - old_size) -
		offsetof(struct compilation_unit_header, version);
	comp_unit_header->pointer_size = sizeof(void *);
}


static void create_debug_abbrev(struct growable_buffer * b)
{
	emit_unsigned_LEB128(b, 1);
	emit_unsigned_LEB128(b, DW_TAG_compile_unit);
	emit_unsigned_LEB128(b, DW_CHILDREN_yes);
	emit_unsigned_LEB128(b, DW_AT_stmt_list);
	emit_unsigned_LEB128(b, DW_FORM_data4);
	emit_unsigned_LEB128(b, 0);
}

static struct growable_buffer b_line;
static struct growable_buffer b_debug_info;
static struct growable_buffer b_debug_abbrev;

int init_debug_line_info(bfd * abfd)
{
	asection * line_section, * debug_info, * debug_abbrev;
	struct jitentry_debug_line * debug_line;

	init_buffer(&b_line);
	init_buffer(&b_debug_info);
	init_buffer(&b_debug_abbrev);

	for (debug_line = jitentry_debug_line_list;
	     debug_line;
	     debug_line = debug_line->next) {
		struct jr_code_debug_info const * rec = debug_line->data;
		if (rec->nr_entry) {
			size_t i;
			void const * data = rec + 1;
			struct debug_line_info * dbg_line =
				xmalloc(rec->nr_entry *
					sizeof(struct debug_line_info));
			for (i = 0; i < rec->nr_entry; ++i) {
				dbg_line[i].vma = *(unsigned long *)data;
				data += sizeof(unsigned long);
				dbg_line[i].lineno = *(unsigned int *)data;
				data += sizeof(unsigned int);
				dbg_line[i].filename = data;
				data += strlen(data) + 1;
			}

			add_compilation_unit(&b_debug_info, b_line.size);
			add_debug_line(&b_line, dbg_line,
				       rec->nr_entry, rec->code_addr);
			create_debug_abbrev(&b_debug_abbrev);

			free(dbg_line);
		}
	}
	
	line_section = create_section(abfd, ".debug_line", b_line.size, 0,
		SEC_HAS_CONTENTS|SEC_READONLY|SEC_DEBUGGING);
	if (!line_section)
		return -1;

	debug_info = create_section(abfd, ".debug_info", b_debug_info.size, 0,
		SEC_HAS_CONTENTS|SEC_READONLY|SEC_DEBUGGING);
 	if (!debug_info)
		return -1;

	debug_abbrev = create_section(abfd, ".debug_abbrev",
		b_debug_abbrev.size, 0,
		SEC_HAS_CONTENTS|SEC_READONLY|SEC_DEBUGGING);
	if (!debug_abbrev)
		return -1;

	return 0;
}


int finalize_debug_line_info(bfd * abfd)
{
	asection * line_section, * debug_info, * debug_abbrev;

	line_section = bfd_get_section_by_name(abfd, ".debug_line");
	if (!line_section)
		return -1;

	debug_info = bfd_get_section_by_name(abfd, ".debug_info");
 	if (!debug_info)
		return -1;


	debug_abbrev = bfd_get_section_by_name(abfd, ".debug_abbrev");
	if (!debug_abbrev)
		return -1;

	fill_section_content(abfd, line_section, b_line.p, 0, b_line.size);
	fill_section_content(abfd, debug_info, b_debug_info.p,
			     0, b_debug_info.size);
	fill_section_content(abfd, debug_abbrev, b_debug_abbrev.p, 0,
			     b_debug_abbrev.size);


	free_buffer(&b_line);
	free_buffer(&b_debug_info);
	free_buffer(&b_debug_abbrev);

	return 0;
}
