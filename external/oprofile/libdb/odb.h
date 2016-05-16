/**
 * @file odb.h
 * This file contains various definitions and interface for management
 * of in-memory, through mmaped file, growable hash table, that stores
 * sample files.
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef ODB_HASH_H
#define ODB_HASH_H

#include <stddef.h>
#include <stdint.h>

#include "op_list.h"

/** the type of key. 64-bit because CG needs 32-bit pair {from,to} */
typedef uint64_t odb_key_t;
/** the type of an information in the database */
typedef unsigned int odb_value_t;
/** the type of index (node number), list are implemented through index */
typedef unsigned int odb_index_t;
/** the type store node number */
typedef odb_index_t odb_node_nr_t;
/** store the hash mask, hash table size are always power of two */
typedef odb_index_t odb_hash_mask_t;

/* there is (bucket factor * nr node) entry in hash table, this can seem
 * excessive but hash coding eip don't give a good distributions and our
 * goal is to get a O(1) amortized insert time. bucket factor must be a
 * power of two. FIXME: see big comment in odb_hash_add_node, you must
 * re-enable zeroing hash table if BUCKET_FACTOR > 2 (roughly exact, you
 * want to read the comment in odb_hash_add_node() if you tune this define)
 */
#define BUCKET_FACTOR 1

/** a db hash node */
typedef struct {
	odb_key_t key;			/**< eip */
	odb_value_t value;		/**< samples count */
	odb_index_t next;		/**< next entry for this bucket */
} odb_node_t;

/** the minimal information which must be stored in the file to reload
 * properly the data base, following this header is the node array then
 * the hash table (when growing we avoid to copy node array)
 */
typedef struct {
	odb_node_nr_t size;		/**< in node nr (power of two) */
	odb_node_nr_t current_size;	/**< nr used node + 1, node 0 unused */
	int padding[6];			/**< for padding and future use */
} odb_descr_t;

/** a "database". this is an in memory only description.
 *
 * We allow to manage a database inside a mapped file with an "header" of
 * unknown size so odb_open get a parameter to specify the size of this header.
 * A typical use is:
 *
 * struct header { int etc; ... };
 * odb_open(&hash, filename, ODB_RW, sizeof(header));
 * so on this library have no dependency on the header type.
 *
 * the internal memory layout from base_memory is:
 *  the unknown header (sizeof_header)
 *  odb_descr_t
 *  the node array: (descr->size * sizeof(odb_node_t) entries
 *  the hash table: array of odb_index_t indexing the node array 
 *    (descr->size * BUCKET_FACTOR) entries
 */
typedef struct odb_data {
	odb_node_t * node_base;		/**< base memory area of the page */
	odb_index_t * hash_base;	/**< base memory of hash table */
	odb_descr_t * descr;		/**< the current state of database */
	odb_hash_mask_t hash_mask;	/**< == descr->size - 1 */
	unsigned int sizeof_header;	/**< from base_memory to odb header */
	unsigned int offset_node;	/**< from base_memory to node array */
	void * base_memory;		/**< base memory of the maped memory */
	int fd;				/**< mmaped memory file descriptor */
	char * filename;                /**< full path name of sample file */
	int ref_count;                  /**< reference count */
	struct list_head list;          /**< hash bucket list */
} odb_data_t;

typedef struct {
	odb_data_t * data;
} odb_t;

#ifdef __cplusplus
extern "C" {
#endif

/* db_manage.c */

/** how to open the DB file */
enum odb_rw {
	ODB_RDONLY = 0,	/**< open for read only */
	ODB_RDWR = 1	/**< open for read and/or write */
};

/**
 * odb_init - initialize a DB file
 * @param odb the DB file to init
 */
void odb_init(odb_t * odb);

/**
 * odb_open - open a DB file
 * @param odb the data base object to setup
 * @param filename the filename where go the maped memory
 * @param rw \enum ODB_RW if opening for writing, else \enum ODB_RDONLY
 * @param sizeof_header size of the file header if any
 *
 * The sizeof_header parameter allows the data file to have a header
 * at the start of the file which is skipped.
 * odb_open() always preallocate a few number of pages.
 * returns 0 on success, errno on failure
 */
int odb_open(odb_t * odb, char const * filename,
             enum odb_rw rw, size_t sizeof_header);

/** Close the given ODB file */
void odb_close(odb_t * odb);

/** return the number of times this sample file is open */
int odb_open_count(odb_t const * odb);

/** return the start of the mapped data */
void * odb_get_data(odb_t * odb);

/** issue a msync on the used size of the mmaped file */
void odb_sync(odb_t const * odb);

/**
 * grow the hashtable in such way current_size is the index of the first free
 * node. Take care all node pointer can be invalidated by this call.
 *
 * Node allocation is done in a two step way 1st) ensure a free node exist
 * eventually, caller can setup it, 2nd) commit the node allocation with
 * odb_commit_reservation().
 * This is done in this way to ensure node setup is visible from another
 * process like pp tools in an atomic way.
 *
 * returns 0 on success, non zero on failure in this case this function do
 * nothing and errno is set by the first libc call failure allowing to retry
 * after cleanup some program resource.
 */
int odb_grow_hashtable(odb_data_t * data);
/**
 * commit a previously successfull node reservation. This can't fail.
 */
static __inline void odb_commit_reservation(odb_data_t * data)
{
	++data->descr->current_size;
}

/** "immpossible" node number to indicate an error from odb_hash_add_node() */
#define ODB_NODE_NR_INVALID ((odb_node_nr_t)-1)

/* db_debug.c */
/** check that the hash is well built */
int odb_check_hash(odb_t const * odb);

/* db_stat.c */
typedef struct odb_hash_stat_t odb_hash_stat_t;
odb_hash_stat_t * odb_hash_stat(odb_t const * odb);
void odb_hash_display_stat(odb_hash_stat_t const * stats);
void odb_hash_free_stat(odb_hash_stat_t * stats);

/* db_insert.c */
/** update info at key by incrementing its associated value by one, 
 * if the key does not exist a new node is created and the value associated
 * is set to one.
 *
 * returns EXIT_SUCCESS on success, EXIT_FAILURE on failure
 */
int odb_update_node(odb_t * odb, odb_key_t key);

/**
 * odb_update_node_with_offset
 * @param odb the data base object to setup
 * @param key the hash key
 * @param offset the offset to be added
 *
 * update info at key by adding the specified offset to its associated value,
 * if the key does not exist a new node is created and the value associated
 * is set to offset.
 *
 * returns EXIT_SUCCESS on success, EXIT_FAILURE on failure
 */
int odb_update_node_with_offset(odb_t * odb, 
				odb_key_t key, 
				unsigned long int offset);

/** Add a new node w/o regarding if a node with the same key already exists
 *
 * returns EXIT_SUCCESS on success, EXIT_FAILURE on failure
 */
int odb_add_node(odb_t * odb, odb_key_t key, odb_value_t value);

/* db_travel.c */
/**
 * return a base pointer to the node array and number of node in this array
 * caller then will iterate through:
 *
 * odb_node_nr_t node_nr, pos;
 * odb_node_t * node = odb_get_iterator(odb, &node_nr);
 *	for ( pos = 0 ; pos < node_nr ; ++pos)
 *		// do something
 *
 *  note than caller does not need to filter nil key as it's a valid key,
 * The returned range is all valid (i.e. should never contain zero value).
 */
odb_node_t * odb_get_iterator(odb_t const * odb, odb_node_nr_t * nr);

static __inline unsigned int
odb_do_hash(odb_data_t const * data, odb_key_t value)
{
	/* FIXME: better hash for eip value, needs to instrument code
	 * and do a lot of tests ... */
	/* trying to combine high order bits his a no-op: inside a binary image
	 * high order bits don't vary a lot, hash table start with 7 bits mask
	 * so this hash coding use bits 0-7, 8-15. Hash table is stored in
	 * files avoiding to rebuilding them at profiling re-start so
	 * on changing do_hash() change the file format!
	 */
	uint32_t temp = value & 0xffffffff;
	return ((temp << 0) ^ (temp >> 8)) & data->hash_mask;
}

#ifdef __cplusplus
}
#endif

#endif /* !ODB_H */
