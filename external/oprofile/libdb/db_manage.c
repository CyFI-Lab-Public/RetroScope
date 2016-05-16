/**
 * @file db_manage.c
 * Management of a DB file
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#define _GNU_SOURCE

#include <stdlib.h>
#ifdef ANDROID
#include <fcntl.h>
#else
#include <sys/fcntl.h>
#endif
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>

#include "odb.h"
#include "op_string.h"
#include "op_libiberty.h"

 
static __inline odb_descr_t * odb_to_descr(odb_data_t * data)
{
	return (odb_descr_t *)(((char*)data->base_memory) + data->sizeof_header);
}

 
static __inline odb_node_t * odb_to_node_base(odb_data_t * data)
{
	return (odb_node_t *)(((char *)data->base_memory) + data->offset_node);
}

 
static __inline odb_index_t * odb_to_hash_base(odb_data_t * data)
{
	return (odb_index_t *)(((char *)data->base_memory) + 
				data->offset_node +
				(data->descr->size * sizeof(odb_node_t)));
}

 
/**
 * return the number of bytes used by hash table, node table and header.
 */
static unsigned int tables_size(odb_data_t const * data, odb_node_nr_t node_nr)
{
	size_t size;

	size = node_nr * (sizeof(odb_index_t) * BUCKET_FACTOR);
	size += node_nr * sizeof(odb_node_t);
	size += data->offset_node;

	return size;
}


int odb_grow_hashtable(odb_data_t * data)
{
	unsigned int old_file_size;
	unsigned int new_file_size;
	unsigned int pos;
	void * new_map;

	old_file_size = tables_size(data, data->descr->size);
	new_file_size = tables_size(data, data->descr->size * 2);

	if (ftruncate(data->fd, new_file_size))
		return 1;

#ifdef MISSING_MREMAP
	new_map = mmap(0, new_file_size, PROT_READ | PROT_WRITE,
		MAP_SHARED, data->fd, 0);
#else
	new_map = mremap(data->base_memory,
			 old_file_size, new_file_size, MREMAP_MAYMOVE);
#endif

	if (new_map == MAP_FAILED)
		return 1;

#ifdef MISSING_MREMAP
	munmap(data->base_memory, old_file_size);
#endif

	data->base_memory = new_map;
	data->descr = odb_to_descr(data);
	data->descr->size *= 2;
	data->node_base = odb_to_node_base(data);
	data->hash_base = odb_to_hash_base(data);
	data->hash_mask = (data->descr->size * BUCKET_FACTOR) - 1;

	/* rebuild the hash table, node zero is never used. This works
	 * because layout of file is node table then hash table,
	 * sizeof(node) > sizeof(bucket) and when we grow table we
	 * double size ==> old hash table and new hash table can't
	 * overlap so on the new hash table is entirely in the new
	 * memory area (the grown part) and we know the new hash
	 * hash table is zeroed. That's why we don't need to zero init
	 * the new table */
	/* OK: the above is not exact
	 * if BUCKET_FACTOR < sizeof(bd_node_t) / sizeof(bd_node_nr_t)
	 * all things are fine and we don't need to init the hash
	 * table because in this case the new hash table is completely
	 * inside the new growed part. Avoiding to touch this memory is
	 * useful.
	 */
#if 0
	for (pos = 0 ; pos < data->descr->size*BUCKET_FACTOR ; ++pos)
		data->hash_base[pos] = 0;
#endif

	for (pos = 1; pos < data->descr->current_size; ++pos) {
		odb_node_t * node = &data->node_base[pos];
		size_t index = odb_do_hash(data, node->key);
		node->next = data->hash_base[index];
		data->hash_base[index] = pos;
	}

	return 0;
}


void odb_init(odb_t * odb)
{
	odb->data = NULL;
}


/* the default number of page, calculated to fit in 4096 bytes */
#define DEFAULT_NODE_NR(offset_node)	128
#define FILES_HASH_SIZE                 512

static struct list_head files_hash[FILES_HASH_SIZE];


static void init_hash()
{
	size_t i;
	for (i = 0; i < FILES_HASH_SIZE; ++i)
		list_init(&files_hash[i]);
}


static odb_data_t *
find_samples_data(size_t hash, char const * filename)
{
	struct list_head * pos;

	/* FIXME: maybe an initial init routine ? */
	if (files_hash[0].next == NULL) {
		init_hash();
		return NULL;
	}

	list_for_each(pos, &files_hash[hash]) {
		odb_data_t * entry = list_entry(pos, odb_data_t, list);
		if (strcmp(entry->filename, filename) == 0)
			return entry;
	}

	return NULL;
}


int odb_open(odb_t * odb, char const * filename, enum odb_rw rw,
	     size_t sizeof_header)
{
	struct stat stat_buf;
	odb_node_nr_t nr_node;
	odb_data_t * data;
	size_t hash;
	int err = 0;

	int flags = (rw == ODB_RDWR) ? (O_CREAT | O_RDWR) : O_RDONLY;
	int mmflags = (rw == ODB_RDWR) ? (PROT_READ | PROT_WRITE) : PROT_READ;

	hash = op_hash_string(filename) % FILES_HASH_SIZE;
	data = find_samples_data(hash, filename);
	if (data) {
		odb->data = data;
		data->ref_count++;
		return 0;
	}

	data = xmalloc(sizeof(odb_data_t));
	memset(data, '\0', sizeof(odb_data_t));
	list_init(&data->list);
	data->offset_node = sizeof_header + sizeof(odb_descr_t);
	data->sizeof_header = sizeof_header;
	data->ref_count = 1;
	data->filename = xstrdup(filename);

	data->fd = open(filename, flags, 0644);
	if (data->fd < 0) {
		err = errno;
		goto out;
	}

	if (fstat(data->fd, &stat_buf)) {
		err = errno;
		goto fail;
	}

	if (stat_buf.st_size == 0) {
		size_t file_size;

		if (rw == ODB_RDONLY) {
			err = EIO;
			goto fail;
		}

		nr_node = DEFAULT_NODE_NR(data->offset_node);

		file_size = tables_size(data, nr_node);
		if (ftruncate(data->fd, file_size)) {
			err = errno;
			goto fail;
		}
	} else {
		/* Calculate nr node allowing a sanity check later */
		nr_node = (stat_buf.st_size - data->offset_node) /
			((sizeof(odb_index_t) * BUCKET_FACTOR) + sizeof(odb_node_t));
	}

	data->base_memory = mmap(0, tables_size(data, nr_node), mmflags,
				MAP_SHARED, data->fd, 0);

	if (data->base_memory == MAP_FAILED) {
		err = errno;
		goto fail;
	}

	data->descr = odb_to_descr(data);

	if (stat_buf.st_size == 0) {
		data->descr->size = nr_node;
		/* page zero is not used */
		data->descr->current_size = 1;
	} else {
		/* file already exist, sanity check nr node */
		if (nr_node != data->descr->size) {
			err = EINVAL;
			goto fail_unmap;
		}
	}

	data->hash_base = odb_to_hash_base(data);
	data->node_base = odb_to_node_base(data);
	data->hash_mask = (data->descr->size * BUCKET_FACTOR) - 1;

	list_add(&data->list, &files_hash[hash]);
	odb->data = data;
out:
	return err;
fail_unmap:
	munmap(data->base_memory, tables_size(data, nr_node));
fail:
	close(data->fd);
	free(data->filename);
	free(data);
	odb->data = NULL;
	goto out;
}


void odb_close(odb_t * odb)
{
	odb_data_t * data = odb->data;

	if (data) {
		data->ref_count--;
		if (data->ref_count == 0) {
			size_t size = tables_size(data, data->descr->size);
			list_del(&data->list);
			munmap(data->base_memory, size);
			if (data->fd >= 0)
				close(data->fd);
			free(data->filename);
			free(data);
			odb->data = NULL;
		}
	}
}


int odb_open_count(odb_t const * odb)
{
	if (!odb->data)
		return 0;
	return odb->data->ref_count;
}


void * odb_get_data(odb_t * odb)
{
	return odb->data->base_memory;
}


void odb_sync(odb_t const * odb)
{
	odb_data_t * data = odb->data;
	size_t size;

	if (!data)
		return;

	size = tables_size(data, data->descr->size);
	msync(data->base_memory, size, MS_ASYNC);
}
