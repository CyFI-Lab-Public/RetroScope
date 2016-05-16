/**
 * @file op_string.h
 * general purpose C string handling declarations.
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_STRING_H
#define OP_STRING_H

#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @param  s: input string
 * @param len: len char to copy
 *
 * Allocate and copy len character from s to a newly allocated buffer then
 * append a '\0' terminator. Return the newly allocated string
 */
char * op_xstrndup(char const * s, size_t len);

/**
 * @param  s: string to hash
 *
 * Generate a hash code from a string
 */
size_t op_hash_string(char const * s);

/**
 * @param str: string to test
 * @param prefix: prefix string
 *
 * return non zero if prefix parameters is a prefix of str
 */
int strisprefix(char const * str, char const * prefix);

/**
 * @param c: input string
 *
 * return a pointer to the first location in c which is not a blank space
 * where blank space are in " \t\n"
 */
char const * skip_ws(char const * c);

/**
 * @param c: input string
 *
 * return a pointer to the first location in c which is a blank space
 * where blank space are in " \t\n"
 */
char const * skip_nonws(char const * c);

/**
 * @param c: input string
 *
 * return non zero if c string contains only blank space
 * where blank space are in " \t\n"
 */
int empty_line(char const * c);

/**
 * @param c: input string
 *
 * return non zero if c string is a comment. Comment are lines with optional
 * blank space at left then a '#' character. Blank space are in " \t\n"
 */
int comment_line(char const * c);

#ifdef __cplusplus
}
#endif

#endif /* !OP_STRING_H */
