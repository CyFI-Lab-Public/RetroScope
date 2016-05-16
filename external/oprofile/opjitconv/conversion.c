/**
 * @file conversion.c
 * Convert a jit dump file to an ELF file
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Daniel Hansel
 *
 * Copyright IBM Corporation 2008
 *
 */

#include <stdlib.h>

#include "opjitconv.h"

static void free_jit_records(void)
{
	struct jitentry * entry, * next;

	for (entry = jitentry_list; entry; entry = next) {
		if (entry->sym_name_malloced)
			free(entry->symbol_name);
		next = entry->next;
		free(entry);
	}
	jitentry_list = NULL;
}

static void free_jit_debug_line(void)
{
	struct jitentry_debug_line * entry, * next;

	for (entry = jitentry_debug_line_list; entry; entry = next) {
		next = entry->next;
		free(entry);
	}
	jitentry_debug_line_list = NULL;
}

int op_jit_convert(struct op_jitdump_info file_info, char const * elffile,
                   unsigned long long start_time, unsigned long long end_time)
{
	void const * jitdump = file_info.dmp_file;
	int rc= OP_JIT_CONV_OK;

	entry_count = 0;
	max_entry_count = 0;
	syms = NULL;
	cur_bfd = NULL;
	jitentry_list = NULL;
	jitentry_debug_line_list = NULL;
	entries_symbols_ascending = entries_address_ascending = NULL;

	if ((rc = parse_all(jitdump, jitdump + file_info.dmp_file_stat.st_size,
	                    end_time)) == OP_JIT_CONV_FAIL)
		goto out;

	create_arrays();
	if ((rc = resolve_overlaps(start_time)) == OP_JIT_CONV_FAIL)
		goto out;

	disambiguate_symbol_names();
	if (!entry_count)
		return OP_JIT_CONV_NO_JIT_RECS_IN_DUMPFILE;

	if ((cur_bfd = open_elf(elffile)) == NULL) {
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}

	init_debug_line_info(cur_bfd);

	if ((rc = partition_sections()) == OP_JIT_CONV_FAIL)
		goto out;

	if ((rc = fill_sections()) == OP_JIT_CONV_FAIL)
		goto out;

	finalize_debug_line_info(cur_bfd);

	if (cur_bfd)
		bfd_close(cur_bfd);
	free(syms);
	out: free_jit_records();
	free_jit_debug_line();
	free(entries_symbols_ascending);
	free(entries_address_ascending);
	return rc;
}

