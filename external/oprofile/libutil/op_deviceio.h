/**
 * @file op_deviceio.h
 * Reading from a special device
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_DEVICEIO_H
#define OP_DEVICEIO_H

#ifdef __cplusplus
extern "C" {
#endif

#include "op_types.h"

#include <unistd.h>

/**
 * op_open_device - open a special char device for reading
 * @param name  file name of device file
 *
 * Open the special file name. Returns the file descriptor
 * for the file or -1 on error.
 */
fd_t op_open_device(char const * name);

/**
 * op_read_device - read from a special char device
 * @param devfd  file descriptor of device
 * @param buf  buffer
 * @param size  size of buffer
 *
 * Read size bytes from a device into buffer buf.
 * A seek to the start of the device file is done first
 * then a read is requested in one go of size bytes.
 *
 * It is the caller's responsibility to do further op_read_device()
 * calls if the number of bytes read is not what is requested
 * (where this is applicable).
 *
 * The number of bytes read is returned, or a negative number
 * on failure (in which case errno will be set). If the call is
 * interrupted, then errno will be EINTR, and the client should
 * arrange for re-starting the read if necessary.
 */
ssize_t op_read_device(fd_t devfd, void * buf, size_t size);

#ifdef __cplusplus
}
#endif

#endif /* OP_DEVICEIO_H */
