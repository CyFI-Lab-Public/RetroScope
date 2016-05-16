/**
 * @file op_growable_buffer.c
 * a growable buffer implementation
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include "op_growable_buffer.h"
#include "op_libiberty.h"

#include <string.h>
#include <stdlib.h>

void init_buffer(struct growable_buffer * b)
{
	b->max_size = 0;
	b->size = 0;
	b->p = NULL;
}


void free_buffer(struct growable_buffer * b)
{
	free(b->p);
}


static void grow_buffer(struct growable_buffer * b)
{
	size_t new_size = (b->max_size + b->size) * 2;
	b->p = xrealloc(b->p, new_size);
	b->max_size = new_size;
}


void add_data(struct growable_buffer * b, void const * data, size_t len)
{
	size_t old_size = b->size;
	b->size += len;
	if (b->size > b->max_size)
		grow_buffer(b);
	memcpy(b->p + old_size, data, len);
}
