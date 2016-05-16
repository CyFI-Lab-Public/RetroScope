/**
 * @file opd_mapping.c
 * Management of process mappings
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "opd_mapping.h"
#include "opd_proc.h"
#include "opd_image.h"
#include "opd_printf.h"

#include "op_interface.h"
#include "op_config_24.h"
#include "op_libiberty.h"

#include <sys/mman.h>
#include <limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

/* hash map device mmap */
static struct op_hash_index * hashmap;
/* already seen mapping name */
static char const * hash_name[OP_HASH_MAP_NR];


void opd_cleanup_hash_name(void)
{
	int i;
	for (i = 0; i < OP_HASH_MAP_NR; ++i)
		free((char *)hash_name[i]);
	
}


void opd_init_hash_map(void)
{
	extern fd_t hashmapdevfd;

	hashmap = mmap(0, OP_HASH_MAP_SIZE, PROT_READ, MAP_SHARED, hashmapdevfd, 0);
	if ((long)hashmap == -1) {
		perror("oprofiled: couldn't mmap hash map");
		exit(EXIT_FAILURE);
	}

}


void opd_kill_maps(struct opd_proc * proc)
{
	struct list_head * pos, * pos2;

	list_for_each_safe(pos, pos2, &proc->maps) {
		struct opd_map * map = list_entry(pos, struct opd_map, next);
		list_del(pos);
		opd_delete_image(map->image);
		free(map);
	}
}


void opd_add_mapping(struct opd_proc * proc, struct opd_image * image,
		unsigned long start, unsigned long offset, unsigned long end)
{
	struct opd_map * map;

	verbprintf(vmisc, "Adding mapping for process %d: 0x%.8lx-0x%.8lx, off 0x%.8lx, \"%s\"\n",
		proc->tid, start, end, offset, image->name);

	map = malloc(sizeof(struct opd_map));

	/* first map is the primary image */
	if (list_empty(&proc->maps)) {
		if (proc->name)
			free((char *)proc->name);
		proc->name = xstrdup(image->name);
	}

	image->ref_count++;

	map->image = image;
	map->start = start;
	map->offset = offset;
	map->end = end;
	list_add_tail(&map->next, &proc->maps);
}


/**
 * get_from_pool - retrieve string from hash map pool
 * @param ind index into pool
 */
inline static char * get_from_pool(uint ind)
{
	return ((char *)(hashmap + OP_HASH_MAP_NR) + ind);
}


/**
 * opg_get_hash_name - find a mapping name from a hash
 * @param hash hash value for this name
 */
static char const * opd_get_hash_name(int hash)
{
	char file[PATH_MAX];
	char * c = &file[PATH_MAX-1];
	int orighash = hash;

	if (hash_name[hash])
		return hash_name[hash];

	*c = '\0';
	while (hash) {
		char * name = get_from_pool(hashmap[hash].name);

		if (strlen(name) + 1 + strlen(c) >= PATH_MAX) {
			fprintf(stderr, "String \"%s\" too large.\n", c);
			exit(EXIT_FAILURE);
		}

		c -= strlen(name) + 1;
		*c = '/';
		strncpy(c + 1, name, strlen(name));

		/* move onto parent */
		hash = hashmap[hash].parent;
	}

	return hash_name[orighash] = xstrdup(c);
}


void opd_handle_mapping(struct op_note const * note)
{
	struct opd_proc * proc;
	struct opd_image * image;
	int hash;
	char const * name;

	proc = opd_get_proc(note->pid, note->tgid);

	if (!proc) {
		verbprintf(vmisc, "Told about mapping for non-existent process %u.\n", note->pid);
		proc = opd_new_proc(note->pid, note->tgid);
	}

	hash = note->hash;

	if (hash == -1) {
		/* possibly deleted file */
		return;
	}

	if (hash < 0 || hash >= OP_HASH_MAP_NR) {
		fprintf(stderr, "hash value %u out of range.\n", hash);
		return;
	}

	name = opd_get_hash_name(hash);
	image = opd_get_image(name, proc->name, 0, note->pid, note->tgid);

	opd_add_mapping(proc, image, note->addr, note->offset,
	                note->addr + note->len);
}
