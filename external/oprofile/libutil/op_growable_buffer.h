/**
 * @file op_growable_buffer.h
 * a growable buffer interface
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef OP_GROWABLE_BUFFER_H
#define OP_GROWABLE_BUFFER_H

#include <stddef.h>

struct growable_buffer {
	void * p;
	size_t size;
	size_t max_size;
};

/**
 * init_buffer - initialize an empty buffer
 * @param buffer the buffer to initialize
 *
 * init_buffer do not do any allocation, the first allocation will occur
 * when add_data() with a non zero len param will be called.
 */
void init_buffer(struct growable_buffer * buffer);

/**
 * free_buffer - free the memory allocated for this buffer
 * @param buffer the buffer to free
 */
void free_buffer(struct growable_buffer * buffer);

/**
 * add_data - add data to this buffer
 * @param b the buffer where to add data
 * @param data a pointer to the data to add
 * @param len number of byte to add to the buffer
 */
void add_data(struct growable_buffer * b, void const * data, size_t len);

#endif /* !OP_GROWABLE_BUFFER_H */
