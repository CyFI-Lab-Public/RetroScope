/**
 * @file op_dcache.h
 * Compatibility functions for dcache lookups
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef OP_DCACHE_H
#define OP_DCACHE_H

#include <linux/sched.h>
#include <linux/unistd.h>
#include <linux/mman.h>
#include <linux/file.h>

#include "oprofile.h"

extern uint op_dname_top;
extern struct qstr **op_dname_stack;
extern char * op_pool_pos;
extern char * op_pool_start;
extern char * op_pool_end;

uint do_hash(struct dentry * dentry, struct vfsmount * vfsmnt, struct dentry * root, struct vfsmount * rootmnt);

inline static uint alloc_in_pool(char const * str, uint len);
inline static int add_hash_entry(struct op_hash_index * entry, uint parent, char const * name, uint len);
inline static uint name_hash(char const * name, uint len, uint parent);

inline static uint name_hash(char const * name, uint len, uint parent)
{
	uint hash=0;

	while (len--)
		hash = (hash + (name[len] << 4) + (name[len] >> 4)) * 11;

	return (hash ^ parent) % OP_HASH_MAP_NR;
}

/* empty ascending dname stack */
inline static void push_dname(struct qstr * dname)
{
	op_dname_stack[op_dname_top] = dname;
	if (op_dname_top != DNAME_STACK_MAX)
		op_dname_top++;
	else
		printk(KERN_ERR "oprofile: overflowed dname stack !\n");
}

inline static struct qstr * pop_dname(void)
{
	if (op_dname_top == 0)
		return NULL;

	return op_dname_stack[--op_dname_top];
}

inline static uint alloc_in_pool(char const * str, uint len)
{
	char * place = op_pool_pos;
	if (op_pool_pos + len + 1 >= op_pool_end)
		return 0;

	strcpy(place, str);
	op_pool_pos += len + 1;
	return place - op_pool_start;
}

inline static char * get_from_pool(uint index)
{
	return op_pool_start + index;
}

inline static int add_hash_entry(struct op_hash_index * entry, uint parent, char const * name, uint len)
{
	entry->name = alloc_in_pool(name, len);
	if (!entry->name)
		return -1;
	entry->parent = parent;
	return 0;
}

#endif /* OP_DCACHE_H */
