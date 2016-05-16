/**
 * @file opd_mapping.h
 * Management of process mappings
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_MAPPING_H
#define OPD_MAPPING_H

#include "op_list.h"

struct opd_image;
struct opd_proc;
struct op_note;

/**
 * represent a mmap'ed area, we create such area only for vma area with exec
 * access right
 */
struct opd_map {
	/** next mapping for this image */
	struct list_head next;
	/** owning image */
	struct opd_image * image;
	/** mapping start vma */
	unsigned long start;
	/** mapping offset */
	unsigned long offset;
	/** mapping end vma */
	unsigned long end;
};

/**
 * opd_init_hash_map - initialise the hashmap
 */
void opd_init_hash_map(void);

/**
 * op_cleanup_hash_name
 *
 * release resource owned by hash_name array
 */
void opd_cleanup_hash_name(void);

/**
 * opd_handle_mapping - deal with mapping notification
 * @param note  mapping notification
 *
 * Deal with one notification that a process has mapped
 * in a new executable file. The mapping information is
 * added to the process structure.
 */
void opd_handle_mapping(struct op_note const * note);

/**
 * opd_put_mapping - add a mapping to a process
 * @param proc  process to add map to
 * @param image  mapped image pointer
 * @param start  start of mapping
 * @param offset  file offset of mapping
 * @param end  end of mapping
 *
 * Add the mapping specified to the process proc growing the maps array
 * if necessary.
 */
void opd_add_mapping(struct opd_proc * proc, struct opd_image * image,
		unsigned long start, unsigned long offset, unsigned long end);

/**
 * opd_kill_maps - delete mapping information for a process
 * @param proc  process to work on
 *
 * Frees structures holding mapping information
 */
void opd_kill_maps(struct opd_proc * proc);

/**
 * opd_is_in_map - check whether an EIP is within a mapping
 * @param map  map to check
 * @param eip  EIP value
 *
 * Return %1 if the EIP value @eip is within the boundaries
 * of the map @map, %0 otherwise.
 */
inline static int opd_is_in_map(struct opd_map * map, unsigned long eip)
{
	return (eip >= map->start && eip < map->end);
}


/*
 * opd_map_offset - return offset of sample against map
 * @param map  map to use
 * @param eip  EIP value to use
 *
 * Returns the offset of the EIP value @eip into
 * the map @map, which is the same as the file offset
 * for the relevant binary image.
 */
inline static unsigned long opd_map_offset(struct opd_map * map,
					   unsigned long eip)
{
	return (eip - map->start) + map->offset;
}

#endif /* OPD_MAPPING_H */
