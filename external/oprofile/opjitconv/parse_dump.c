/**
 * @file parse_dump.c
 * parse a jit dump file
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jens Wilke
 * @Modifications Maynard Johnson
 * @Modifications Philippe Elie
 * @Modifications Daniel Hansel
 *
 * Copyright IBM Corporation 2007
 *
 */

#include "opjitconv.h"
#include "jitdump.h"
#include "opd_printf.h"
#include "op_libiberty.h"

#include <string.h>
#include <stdio.h>

/* parse a code load record and add the entry to the jitentry list */
static int parse_code_load(void const * ptr_arg, int size,
			   unsigned long long end_time)
{
	struct jitentry * entry;
	int rc = OP_JIT_CONV_OK;
	char const * ptr = ptr_arg;
	struct jr_code_load const * rec = ptr_arg;
	char const * end;
	size_t padding_count, rec_totalsize;
	end = rec->code_addr ? ptr + size : NULL;

	entry = xcalloc(1, sizeof(struct jitentry));

	// jitentry constructor
	entry->next = NULL;
	ptr += sizeof(*rec);
	/* symbol_name can be malloced so we cast away the constness. */
	entry->symbol_name = (char *)ptr;
	entry->sym_name_malloced = 0;
	ptr += strlen(ptr) + 1;
	entry->code = rec->code_addr ? ptr : NULL;
	entry->vma = rec->vma;
	entry->code_size = rec->code_size;
	entry->section = NULL;
	entry->life_start = rec->timestamp;
	// if nothing else is known the symbol lives till the end of the
	// sampling run, this value may be overwritten by an unload record1
	// later
	entry->life_end = end_time;

	// build list
	entry->next = jitentry_list;
	jitentry_list = entry;

	/* padding bytes are calculated over the complete record
	 * (i.e. header + symbol name + code)
	 */
	rec_totalsize = sizeof(*rec) + strlen(entry->symbol_name) + 1 + entry->code_size;
	padding_count = PADDING_8ALIGNED(rec_totalsize);

	verbprintf(debug, "record0: name=%s, vma=%llx, code_size=%i, "
		   "padding_count=%llu, life_start=%lli, life_end=%lli\n", entry->symbol_name,
		   entry->vma, entry->code_size, (unsigned long long)padding_count, entry->life_start,
		   entry->life_end);
	/* If end == NULL, the dump does not include code, and this sanity
	 * check is skipped.
	 */
	if (end && (ptr + entry->code_size + padding_count != end)) {
		verbprintf(debug, "record total size mismatch\n");
		rc = OP_JIT_CONV_FAIL;
	}
	return rc;
}


/*
 * parse a code unload record. Search for existing record with this code
 * address and fill life_end field with the timestamp. linear search not very
 * efficient. FIXME: inefficient
 */
static void parse_code_unload(void const * ptr, unsigned long long end_time)
{
	struct jr_code_unload const * rec = ptr;
	struct jitentry * entry;

	verbprintf(debug,"record1: vma=%llx, life_end=%lli\n",
		   rec->vma, rec->timestamp);
	/**
	 * Normally we won't get a jr_code_unload with a zero time stamp or
	 * a zero code address. The code address is directly provided by the JVMTI.
	 * The documentation of JVMTI does not say anything about the address value if
	 * it could be zero or not. Therefore it is only a sanity check at the moment.
	 */
	if (rec->timestamp > 0 && rec->vma != 0) {
		for (entry = jitentry_list; entry; entry = entry->next) {
			if (entry->vma == rec->vma &&
			    entry->life_end == end_time) {
				entry->life_end = rec->timestamp;
				verbprintf(debug,"matching record found\n");
				break;
			}
		}
	}
}


/*
 * There is no real parsing here, we just record a pointer to the data,
 * we will interpret on the fly the record when building the bfd file.
 */
static void parse_code_debug_info(void const * ptr, void const * end,
				  unsigned long long end_time)
{
	struct jr_code_debug_info const * rec = ptr;
	struct jitentry_debug_line * debug_line =
		xmalloc(sizeof(struct jitentry_debug_line));

	debug_line->data = rec;
	debug_line->end = end;
	debug_line->life_start = rec->timestamp;
	debug_line->life_end = end_time;

	debug_line->next = jitentry_debug_line_list;
	jitentry_debug_line_list = debug_line;
}


/* parse all entries in the jit dump file and build jitentry_list.
 * the code needs to check always whether there is enough
 * to read remaining. this is because the file may be written to
 * concurrently. */
static int parse_entries(void const * ptr, void const * end,
			 unsigned long long end_time)
{
	int rc = OP_JIT_CONV_OK;
	struct jr_prefix const * rec = ptr;

	while ((void *)rec + sizeof(struct jr_prefix) < end) {
		if (((void *) rec + rec->total_size) > end) {
			verbprintf(debug, "record past end of file\n");
			rc = OP_JIT_CONV_FAIL;
			break;
		}

		switch (rec->id) {
		case JIT_CODE_LOAD:
			if (parse_code_load(rec, rec->total_size, end_time)) {
				rc = OP_JIT_CONV_FAIL;
				break;
			}
			break;

		case JIT_CODE_UNLOAD:
			parse_code_unload(rec, end_time);
			break;

		// end of VM live time, no action
		case JIT_CODE_CLOSE:
			break;

		case JIT_CODE_DEBUG_INFO:
			if (rec->total_size == 0) {
				/* op_write_debug_line_info() ensures to write records with
				 * totalsize > 0.
				 */
				rc = OP_JIT_CONV_FAIL;
				break;
			}

			parse_code_debug_info(rec, end, end_time);
			break;

		default:
			verbprintf(debug, "unknown record type\n");
			rc = OP_JIT_CONV_FAIL;
			break;
		}

		/* advance to next record (incl. possible padding bytes) */
		rec = (void *)rec + rec->total_size;
	}

	return rc;
}


/* parse the jit dump header information 
 * The ptr arg is the address of the pointer to the mmapped
 * file, which we modify below.
 */
static int parse_header(char const ** ptr, char const * end)
{
	int rc = OP_JIT_CONV_OK;
	struct jitheader const * header;

	if (*ptr + sizeof(struct jitheader) >= end) {
		verbprintf(debug,
			   "opjitconv: EOF in jitdump file, no header\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	header = (struct jitheader *)*ptr;
	if (header->magic != JITHEADER_MAGIC) {
		verbprintf(debug, "opjitconv: Wrong jitdump file magic\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (header->version != JITHEADER_VERSION) {
		verbprintf(debug, "opjitconv: Wrong jitdump file version\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (*ptr + header->totalsize > end) {
		verbprintf(debug, "opjitconv: EOF in jitdump file, not enough "
			   "data for header\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	dump_bfd_arch = header->bfd_arch;
	dump_bfd_mach = header->bfd_mach;
	dump_bfd_target_name = header->bfd_target;
	verbprintf(debug, "header: bfd-arch=%i, bfd-mach=%i,"
		   " bfd_target_name=%s\n", dump_bfd_arch, dump_bfd_mach,
		   dump_bfd_target_name);
	*ptr = *ptr + header->totalsize;
out:
	return rc;
}


/* Read in the memory mapped jitdump file.
 * Build up jitentry structure and set global variables.
*/
int parse_all(void const * start, void const * end,
	      unsigned long long end_time)
{
	char const * ptr = start;
	if (!parse_header(&ptr, end))
		return parse_entries(ptr, end, end_time);
	else
		return OP_JIT_CONV_FAIL;
}
