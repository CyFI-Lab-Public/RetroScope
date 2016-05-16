/**
 * @file jitdump.h
 * Header structure of a JIT-dump file.
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jens Wilke
 * @Modifications Daniel Hansel
 *
 * Copyright IBM Corporation 2007
 *
 */

#include <sys/time.h>
#include <time.h>
#include <stdint.h>

#include "op_types.h"

#ifndef JITDUMP_H
#define JITDUMP_H

 /**
  * Magic to do a sanity check that this is a dump file
  * characters "jItO" */
#define JITHEADER_MAGIC 0x4F74496A

/**
 * Macro to calculate count of padding bytes
 * to extend a size to be 8-byte aligned. */
#define PADDING_8ALIGNED(x) ((((x) + 7) & 7) ^ 7)

/**
 * Version number to avoid conflicts, increase
 * this whenever the header is changed */
#define JITHEADER_VERSION 1

struct jitheader {
	/* characters "jItO" */
	u32 magic;
	/* version of the dump */
	u32 version;
	u32 totalsize;
	u32 bfd_arch;
	u32 bfd_mach;
	u64 timestamp;
	char bfd_target[0];
};

enum jit_record_type {
	JIT_CODE_LOAD=0,
	JIT_CODE_UNLOAD=1,
	JIT_CODE_CLOSE=2,
	JIT_CODE_DEBUG_INFO=3
};

/* each record starts always with a id and a total_size */
struct jr_prefix {
	u32 id;
	u32 total_size;
};

/* record0 (id=0) logs a jitted code */
struct jr_code_load {
	u32 id;
	u32 total_size;
	u64 timestamp;
	u64 vma;
	u64 code_addr;
	u32 code_size;
	u32 align;
};

/* record1 (id=1) logs a code unload */
struct jr_code_unload {
	u32 id;
	u32 total_size;
	u64 timestamp;
	u64 vma;
};

/* record2 (id=2) logs end of JVM livetime */
struct jr_code_close {
	u32 id;
	u32 total_size;
	u64 timestamp;
};

/* record3 (id=3) logs debug line information. */
struct jr_code_debug_info {
	u32 id;
	u32 total_size;
	u64 timestamp;
	u64 code_addr;
	u32 nr_entry;
	u32 align;
};

#endif /* !JITDUMP_H */

