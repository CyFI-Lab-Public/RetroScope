/**
 * @file op_abi.h
 * This file contains a simple C interface to the ABI-describing functionality,
 * the majority of which is implemented in C++. this is the file which is 
 * intended for use in files outside the /libabi directory.
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 * @author Philippe Elie
 */

#ifndef OP_ABI_H
#define OP_ABI_H

struct op_abi_entry {
	char const * name;
	/// offset or size of the named entry
	int offset;
};

#ifdef __cplusplus
extern "C" {
#endif

/// return array is terminated by a NULL entry in name field
struct op_abi_entry const * get_abi(void);

/// return non zero if the abi is little endian
int op_little_endian(void);

/**
 * Write current abi to file.
 * return 1 on success, 0 on failure
 */
int op_write_abi_to_file(char const * abi_file);

#ifdef __cplusplus
}
#endif

#endif // OP_ABI_H
