/**
 * @file op_fileio.h
 * Reading from / writing to files
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_FILEIO_H
#define OP_FILEIO_H

#ifdef __cplusplus
extern "C" {
#endif

#include "op_types.h"

#include <stdio.h>

/**
 * op_try_open_file - open a file
 * @param name  file name
 * @param mode  mode string
 *
 * Open a file name.
 * Returns file handle or %NULL on failure.
 */
FILE * op_try_open_file(char const * name, char const * mode);

/**
 * op_open_file - open a file
 * @param name  file name
 * @param mode  mode string
 *
 * Open a file name.
 * Failure to open is fatal.
 */
FILE * op_open_file(char const * name, char const * mode);

/**
 * op_read_int_from_file - parse an ASCII value from a file into an integer
 * @param filename  name of file to parse integer value from
 * @param fatal  non-zero if any error must be fatal
 *
 * Reads an ASCII integer from the given file. If an error occur and fatal is
 * zero (u32)-1 is returned else the value read in is returned.
 */
u32 op_read_int_from_file(char const * filename, int fatal);

/**
 * op_close_file - close a file
 * @param fp  file pointer
 *
 * Closes a file pointer. A non-fatal
 * error message is produced if the
 * close fails.
 */
void op_close_file(FILE * fp);

/**
 * op_write_file - write to a file
 * @param fp  file pointer
 * @param buf  buffer
 * @param size  nr. of bytes to write
 *
 * Write size bytes of buffer buf to a file.
 * Failure is fatal.
 */
void op_write_file(FILE * fp, void const * buf, size_t size);

/**
 * op_write_u32 - write four bytes to a file
 * @param fp  file pointer
 * @param val  value to write
 *
 * Write an unsigned four-byte value val to a file.
 * Failure is fatal.
 *
 * No byte-swapping is done.
 */
void op_write_u32(FILE * fp, u32 val);

/**
 * op_write_u64 - write eight bytes to a file
 * @param fp  file pointer
 * @param val  value to write
 *
 * Write an unsigned eight-byte value val to a file.
 * Failure is fatal.
 *
 * No byte-swapping is done.
 */
void op_write_u64(FILE * fp, u64 val);

/**
 * op_write_u8 - write a byte to a file
 * @param fp  file pointer
 * @param val  value to write
 *
 * Write an unsigned byte value val to a file.
 * Failure is fatal.
 */
void op_write_u8(FILE * fp, u8 val);

/**
 * op_get_line - read an ASCII line from a file
 * @param fp  file pointer
 *
 * Get a line of ASCII text from a file. The file is read
 * up to the first '\0' or '\n'. A trailing '\n' is deleted.
 *
 * Returns the dynamically-allocated string containing
 * that line. At the end of a file NULL will be returned.
 * be returned.
 *
 * The string returned must be free()d by the caller.
 *
 * getline() is not a proper solution to replace this function
 */
char * op_get_line(FILE * fp);

/**
 * calc_crc32
 * @param crc current value
 * @param buf pointer to buffer
 * @param len
 *
 * Returns current crc computed from the crc argument and the
 * characters in len characters in buf.
 */
unsigned long calc_crc32(unsigned long crc, unsigned char * buf, size_t len);

#ifdef __cplusplus
}
#endif

#endif /* OP_FILEIO_H */
