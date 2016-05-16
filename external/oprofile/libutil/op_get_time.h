/**
 * @file op_get_time.h
 * Get current time as a string
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_GET_TIME_H
#define OP_GET_TIME_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * op_get_time - get current date and time
 *
 * Returns a string representing the current date
 * and time, or an empty string on error.
 *
 * The string is statically allocated and should not be freed.
 */
char * op_get_time(void);

#ifdef __cplusplus
}
#endif

#endif /* OP_GET_TIME_H */
