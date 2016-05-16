/**
 * @file create_bfd.c
 * Routine to handle elf file creation
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
#include "opd_printf.h"
#include "op_libiberty.h"

#include <bfd.h>
#include <stdint.h>
#include <stdio.h>

/* Create the symbols and fill the syms array for all functions
 * from start_idx to end_idx pointing into entries_address_ascending array */
static int fill_symtab(void)
{
	int rc = OP_JIT_CONV_OK;
	u32 i;
	int r;
	struct jitentry const * e;
	asymbol * s;
	asection * section = NULL;

	/* Check for valid value of entry_count to avoid integer overflow. */
	if (entry_count > UINT32_MAX - 1) {
		bfd_perror("invalid entry_count value");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	
	syms = xmalloc(sizeof(asymbol *) * (entry_count+1));
	syms[entry_count] = NULL;
	for (i = 0; i < entry_count; i++) {
		e = entries_address_ascending[i];
		if (e->section)
			section = e->section;
		s = bfd_make_empty_symbol(cur_bfd);
		if (!s) {
			bfd_perror("bfd_make_empty_symbol");
			rc = OP_JIT_CONV_FAIL;
			goto out;
		}
		s->name = e->symbol_name;
		s->section = section;
		s->flags = BSF_GLOBAL | BSF_FUNCTION;
		s->value = e->vma - section->vma;
		verbprintf(debug,"add sym: name=%s, value=%llx\n", s->name,
			   (unsigned long long)s->value);
		syms[i] = s;
	}
	r = bfd_set_symtab(cur_bfd, syms, entry_count);
	if (r == FALSE) {
		bfd_perror("bfd_set_symtab");
		rc = OP_JIT_CONV_FAIL;
	}
out:
	return rc;
}

/*
 * create a new section.
 */
asection * create_section(bfd * abfd, char const * section_name,
			  size_t size, bfd_vma vma, flagword flags)
{
	asection * section;

	verbprintf(debug, "create_section() %s\n", section_name);
	section = bfd_make_section(abfd, section_name);
	if (section == NULL)  {
		bfd_perror("bfd_make_section");
		goto error;
	}
	if (bfd_set_section_vma(abfd, section, vma) == FALSE) {
		bfd_perror("bfd_set_section_vma");
		goto error;
	}
	if (bfd_set_section_size(abfd, section, size) == FALSE) {
		bfd_perror("bfd_set_section_size");
		goto error;
	}
	if (bfd_set_section_flags(abfd, section, flags) == FALSE) {
		bfd_perror("bfd_set_section_flags");
		goto error;
	}
	return section;
error:
	return NULL;
}


/* create a .text section. end_idx: index last jitentry (inclusive!)  */
static int create_text_section(int start_idx, int end_idx)
{
	int rc = OP_JIT_CONV_OK;

	asection * section;
	char const * section_name;
	int idx = start_idx;
	unsigned long long vma_start =
		entries_address_ascending[start_idx]->vma;
	struct jitentry * ee = entries_address_ascending[end_idx];
	unsigned long long vma_end = ee->vma + ee->code_size;
	int size = vma_end - vma_start;

	section_name = bfd_get_unique_section_name(cur_bfd, ".text", &idx);
	verbprintf(debug, "section idx=%i, name=%s, vma_start=%llx, size=%i\n",
		   idx, section_name, vma_start, size);

	section = create_section(cur_bfd, section_name, size, vma_start,
               SEC_ALLOC|SEC_LOAD|SEC_READONLY|SEC_CODE|SEC_HAS_CONTENTS);
	if (section)
		entries_address_ascending[start_idx]->section = section;
	else
		rc = OP_JIT_CONV_FAIL;

	return rc;
}

/* fill a section contents at a given offset from the start of the section */
int fill_section_content(bfd * abfd, asection * section,
			 void const * b, file_ptr offset, size_t sz)
{
	if (bfd_set_section_contents(abfd, section, b, offset, sz) == FALSE) {
		bfd_perror("bfd_set_section_contents");
		return OP_JIT_CONV_FAIL;
	}
	return OP_JIT_CONV_OK;
}

/*
 * Copy all code of the functions that are within start_idx and end_idx to 
 * the section.
 */
static int fill_text_section_content(asection * section, int start_idx,
				     int end_idx)
{
	int rc = OP_JIT_CONV_OK;
	unsigned long long vma_start =
		entries_address_ascending[start_idx]->vma;
	struct jitentry const * e;
	int i;

	for (i = start_idx; i <= end_idx; i++) {
		e = entries_address_ascending[i];
		verbprintf(debug, "section = %s, i = %i, code = %llx,"
			   " vma = %llx, offset = %llx,"
			   "size = %i, name = %s\n",
			   section->name, i,
			   (unsigned long long) (uintptr_t) e->code,
			   e->vma, e->vma - vma_start,
			   e->code_size, e->symbol_name);
		/* the right part that is created by split_entry may 
		 * have no code; also, the agent may have passed NULL
		 * for the code location.
		 */
		if (e->code) {
			rc = fill_section_content(cur_bfd, section,
				e->code, (file_ptr) (e->vma - vma_start),
				(bfd_size_type)e->code_size);
			if (rc != OP_JIT_CONV_OK)
				break;
		}
	}
	return rc;
}


/* Walk over the symbols sorted by address and create ELF sections. Whenever we
 * have a gap greater or equal to 4096 make a new section.
 */
int partition_sections(void)
{
	int rc = OP_JIT_CONV_OK;
	u32 i, j;
	struct jitentry const * pred;
	struct jitentry const * entry;
	unsigned long long end_addr;

	// i: start index of the section
	i = 0;
	for (j = 1; j < entry_count; j++) {
		entry = entries_address_ascending[j];
		pred = entries_address_ascending[j - 1];
		end_addr = pred->vma + pred->code_size;
		// calculate gap between code, if it is more than one page
		// create an additional section
		if ((entry->vma - end_addr) >= 4096) {
			rc = create_text_section(i, j - 1);
			if (rc == OP_JIT_CONV_FAIL)
				goto out;
			i = j;
		}
	}
	// this holds always if we have at least one jitentry
	if (i < entry_count)
		rc = create_text_section(i, entry_count - 1);
out:
	return rc;
}


/* Fill the code content into the sections created by partition_sections() */
int fill_sections(void)
{
	int rc = OP_JIT_CONV_OK;
	u32 i, j;
	asection * section;

	rc = fill_symtab();
	if (rc == OP_JIT_CONV_FAIL)
		goto out;

	verbprintf(debug, "opjitconv: fill_sections\n");
	i = 0;
	for (j = 1; j < entry_count; j++) {
		if (entries_address_ascending[j]->section) {
			section = entries_address_ascending[i]->section;
			rc = fill_text_section_content(section, i,
						       j - 1);
			if (rc == OP_JIT_CONV_FAIL)
				goto out;
			i = j;
		}
	}
	// this holds always if we have at least one jitentry
	if (i < entry_count) {
		section = entries_address_ascending[i]->section;
		rc = fill_text_section_content(section,
					       i, entry_count - 1);
	}
out:
	return rc;
}


/* create the elf file */
bfd * open_elf(char const * filename)
{
	bfd * abfd;

	abfd = bfd_openw(filename, dump_bfd_target_name);
	if (!abfd) {
		bfd_perror("bfd_openw");
		goto error1;
	}
	if (bfd_set_format(abfd, bfd_object) == FALSE) {
		bfd_perror("bfd_set_format");
		goto error;
	}
	if (bfd_set_arch_mach(abfd, dump_bfd_arch, dump_bfd_mach) == FALSE) {
		bfd_perror("bfd_set_format");
		goto error;
	}
	return abfd;
error:
	bfd_close(abfd);
error1:
	return NULL;
}
