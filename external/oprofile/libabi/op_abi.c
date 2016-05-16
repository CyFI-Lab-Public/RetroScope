/**
 * @file op_abi.c
 * This file contains a simple C interface to the ABI-describing functionality,
 * the majority of which is implemented in C++. This is the file which is 
 * intended for use in files outside the /libabi directory.
 *
 * @remark Copyright 2002, 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 * @author Philippe Elie
 */

#include "op_abi.h"
#include "odb.h"
#include "op_sample_file.h"

#include <stdio.h>
#include <stddef.h>
#include <assert.h>

static struct op_abi_entry const abi_entries[] = {
	{ "sizeof_double", sizeof(double) },
	{ "sizeof_time_t", sizeof(time_t) },
	{ "sizeof_u8", sizeof(u8) },
	{ "sizeof_u32", sizeof(u32) },
	{ "sizeof_int", sizeof(int) },
	{ "sizeof_unsigned_int", sizeof(unsigned int) },
	{ "sizeof_odb_key_t", sizeof(odb_key_t) },
	{ "sizeof_odb_index_t", sizeof(odb_index_t) },
	{ "sizeof_odb_value_t", sizeof(odb_value_t) },
	{ "sizeof_odb_node_nr_t", sizeof(odb_node_nr_t) },
	{ "sizeof_odb_descr_t", sizeof(odb_descr_t) },
	{ "sizeof_odb_node_t", sizeof(odb_node_t) },
	{ "sizeof_struct_opd_header", sizeof(struct opd_header) },
	
	{ "offsetof_node_key", offsetof(odb_node_t, key) },
	{ "offsetof_node_value", offsetof(odb_node_t, value) },
	{ "offsetof_node_next", offsetof(odb_node_t, next) },
	
	{ "offsetof_descr_size", offsetof(odb_descr_t, size) },
	{ "offsetof_descr_current_size", offsetof(odb_descr_t, current_size) },
	
	{ "offsetof_header_magic", offsetof(struct opd_header, magic) },
	{ "offsetof_header_version", offsetof(struct opd_header, version) },
	{ "offsetof_header_cpu_type", offsetof(struct opd_header, cpu_type) },
	{ "offsetof_header_ctr_event", offsetof(struct opd_header, ctr_event) },
	{ "offsetof_header_ctr_um", offsetof(struct opd_header, ctr_um) },
	{ "offsetof_header_ctr_count", offsetof(struct opd_header, ctr_count) },
	{ "offsetof_header_is_kernel", offsetof(struct opd_header, is_kernel) },
	{ "offsetof_header_cpu_speed", offsetof(struct opd_header, cpu_speed) },
	{ "offsetof_header_mtime", offsetof(struct opd_header, mtime) },
	{ "offsetof_header_cg_to_is_kernel", offsetof(struct opd_header, cg_to_is_kernel), },
	{ "offsetof_header_anon_start", offsetof(struct opd_header, anon_start) },
	{ "offsetof_header_cg_to_anon_start", offsetof(struct opd_header, cg_to_anon_start) },
	
	{ NULL, 0 },
};


struct op_abi_entry const * get_abi(void)
{
	return abi_entries;
}


int op_little_endian(void)
{
	unsigned int probe = 0xff;
	size_t sz = sizeof(unsigned int);
	unsigned char * probe_byte = (unsigned char *)&probe;

	assert(probe_byte[0] == 0xff || probe_byte[sz - 1] == 0xff);

	return probe_byte[0] == 0xff;
}


int op_write_abi_to_file(char const * abi_file)
{
	FILE * fp;
	struct op_abi_entry const * abi_entry;

	if ((fp = fopen(abi_file, "w")) == NULL)
		return 0;

	for (abi_entry = get_abi() ; abi_entry->name != NULL; ++abi_entry)
		fprintf(fp, "%s %u\n", abi_entry->name, abi_entry->offset);
	fprintf(fp, "little_endian %d\n", op_little_endian());

	fclose(fp);

	return 1;
}
