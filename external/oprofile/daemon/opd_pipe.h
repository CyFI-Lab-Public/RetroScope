/**
 * @file daemon/opd_pipe.h
 * Functions handling the $SESSIONDIR/opd_pipe FIFO special file.
 * NOTE: This code is dealing with potencially insecure input.
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Daniel Hansel
 */

#ifndef OPD_PIPE_H_
#define OPD_PIPE_H_
 
/**
 * opd_create_pipe - creates the oprofiled fifo file
 *
 * Creates the Oprofile daemon fifo pipe to enable communication between
 * the daemon and the 'opcontrol --dump' command. Failure to create the pipe
 * is a fatal error.
 */
void opd_create_pipe(void);

/**
 * opd_open_pipe - opens the oprofiled fifo file
 */
void opd_open_pipe(void);

/**
 * opd_close_pipe - closes the oprofiled fifo file
 *
 * Closes the Oprofile daemon fifo pipe.
 */
void opd_close_pipe(void);

/**
 * is_jitconv_requested - check for request to jit conversion
 *
 * Checks the Oprofile daemon fifo pipe for do_jitconv request.
 * If jit conversion is requested ('do_jitconv' is sent) the check returns 1.
 * Otherwise it returns 0.
 */
int is_jitconv_requested(void);

#endif /*OPD_PIPE_H_*/
