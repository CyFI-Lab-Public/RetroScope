/**
 * @file opjitconv.h
 * Convert a jit dump file to an ELF file
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

#ifndef OPJITCONV_H
#define OPJITCONV_H

#define OP_JIT_CONV_OK 0
#define OP_JIT_CONV_FAIL -1
#define OP_JIT_CONV_NO_DUMPFILE 1
#define OP_JIT_CONV_NO_ANON_SAMPLES 2
#define OP_JIT_CONV_NO_MATCHING_ANON_SAMPLES 3
#define OP_JIT_CONV_NO_JIT_RECS_IN_DUMPFILE 4
#define OP_JIT_CONV_ALREADY_DONE 5
#define OP_JIT_CONV_TMPDIR_NOT_REMOVED 6

#include <bfd.h>
#include <stddef.h>
#include <sys/stat.h>

#include "op_list.h"
#include "op_types.h"

/* Structure that contains all information
 * for one function entry in the jit dump file.
 * the jit dump file gets mmapped and code and
 * symbol_name point directly into the file */
struct jitentry {
	/* linked list. see jitentry_list */
	struct jitentry * next;
	/* vma */
	unsigned long long vma;
	/* point to code in the memory mapped file */
	void const * code;
	/* size of the jitted code */
	int code_size;
	/* point to the name in memory mapped jitdump file, or
	 * to a malloced string, if we have a disambiguation replacement */
	char * symbol_name;
	/* sym_name_malloced ==1 means we need to free the memory when done. */
	int sym_name_malloced;
	/* seconds since epoch when the code was created */
	unsigned long long life_start;
	/* seconds since epoch when the code was overwritten */
	unsigned long long life_end;
	/* after ordering and partitioning this is the ELF
	 * section we put this code to */
	asection * section;
};

struct jitentry_debug_line {
	struct jitentry_debug_line * next;
	struct jr_code_debug_info const * data;
	/* seconds since epoch when the code was created */
	unsigned long long life_start;
	/* seconds since epoch when the code was overwritten */
	unsigned long long life_end;
	void const * end;
};

struct op_jitdump_info
{
	void * dmp_file;
	struct stat dmp_file_stat;
};

struct pathname
{
	char * name;
	struct list_head neighbor;
};

/* jitsymbol.c */
void create_arrays(void);
int resolve_overlaps(unsigned long long start_time);
void disambiguate_symbol_names(void);

/* parse_dump.c */
int parse_all(void const * start, void const * end,
	      unsigned long long end_time);

/* conversion.c */
int op_jit_convert(struct op_jitdump_info file_info, char const * elffile,
                   unsigned long long start_time, unsigned long long end_time);

/* create_bfd.c */
bfd * open_elf(char const * filename);
int partition_sections(void);
int fill_sections(void);
asection * create_section(bfd * abfd, char const * section_name,
			  size_t size, bfd_vma vma, flagword flags);
int fill_section_content(bfd * abfd, asection * section,
			 void const * b, file_ptr offset, size_t sz);

/* debug_line.c */
int init_debug_line_info(bfd * abfd);
int finalize_debug_line_info(bfd * abfd);

/* jit dump header information */
extern enum bfd_architecture dump_bfd_arch;
extern int dump_bfd_mach;
extern char const * dump_bfd_target_name;
/*
 * list head.  The linked list is used during parsing (parse_all) to
 * hold all jitentry elements. After parsing, the program works on the
 * array structures (entries_symbols_ascending, entries_address_ascending)
 * and the linked list is not used any more.
 */
extern struct jitentry * jitentry_list;
/* count of jitentries in the list */
extern u32 entry_count;
/* list head for debug line information */
extern struct jitentry_debug_line * jitentry_debug_line_list;
/* maximum space in the entry arrays, needed to add entries */
extern u32 max_entry_count;
/* array pointing to all jit entries, sorted by symbol names */
extern struct jitentry ** entries_symbols_ascending;
/* array pointing to all jit entries sorted by address */
extern struct jitentry ** entries_address_ascending;
/* Global variable for asymbols so we can free the storage later. */
extern asymbol ** syms;
/* the bfd handle of the ELF file we write */
extern bfd * cur_bfd;
/* debug flag, print some information */
extern int debug;


#endif /* OPJITCONV_H */
