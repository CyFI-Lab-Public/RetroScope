/**
 * @file op_list.h
 * Kernel-style lists
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Linux kernel authors
 */

#ifndef OP_LIST_H
#define OP_LIST_H

/*
 * Simple doubly linked list implementation.
 *
 * Some of the internal functions ("__xxx") are useful when
 * manipulating whole lists rather than single entries, as
 * sometimes we already know the next/prev entries and we can
 * generate better code by using them directly rather than
 * using the generic single-entry routines.
 */

struct list_head {
	struct list_head * next, * prev;
};

/**
 * list_init - init a new entry
 * @param ptr  the list to init
 *
 * Init a list head to create an empty list from it
 */
static __inline__ void list_init(struct list_head * ptr)
{
	ptr->next = ptr;
	ptr->prev = ptr;
}

/*
 * Insert a new entry between two known consecutive entries.
 *
 * This is only for internal list manipulation where we know
 * the prev/next entries already!
 */
static __inline__ void __list_add(struct list_head * new_entry,
	struct list_head * prev,
	struct list_head * next)
{
	next->prev = new_entry;
	new_entry->next = next;
	new_entry->prev = prev;
	prev->next = new_entry;
}

/**
 * list_add - add a new entry
 * @param new  new entry to be added
 * @param head  list head to add it after
 *
 * Insert a new entry after the specified head.
 * This is good for implementing stacks.
 */
static __inline__ void list_add(struct list_head * new_entry, struct list_head * head)
{
	__list_add(new_entry, head, head->next);
}

/**
 * list_add_tail - add a new entry
 * @param new  new entry to be added
 * @param head  list head to add it before
 *
 * Insert a new entry before the specified head.
 * This is useful for implementing queues.
 */
static __inline__ void list_add_tail(struct list_head * new_entry, struct list_head * head)
{
	__list_add(new_entry, head->prev, head);
}

/*
 * Delete a list entry by making the prev/next entries
 * point to each other.
 *
 * This is only for internal list manipulation where we know
 * the prev/next entries already!
 */
static __inline__ void __list_del(struct list_head * prev,
				  struct list_head * next)
{
	next->prev = prev;
	prev->next = next;
}

/**
 * list_del - deletes entry from list.
 * @param entry  the element to delete from the list.
 * Note: list_empty on entry does not return true after this, the entry is in an undefined state.
 */
static __inline__ void list_del(struct list_head * entry)
{
	__list_del(entry->prev, entry->next);
}

/**
 * list_del_init - deletes entry from list and reinitialize it.
 * @param entry  the element to delete from the list.
 */
static __inline__ void list_del_init(struct list_head * entry)
{
	__list_del(entry->prev, entry->next);
	list_init(entry);
}

/**
 * list_empty - tests whether a list is empty
 * @param head  the list to test.
 */
static __inline__ int list_empty(struct list_head const * head)
{
	return head->next == head;
}

/**
 * list_splice - join two lists
 * @param list  the new list to add.
 * @param head  the place to add it in the first list.
 */
static __inline__ void list_splice(struct list_head * list, struct list_head * head)
{
	struct list_head * first = list->next;

	if (first != list) {
		struct list_head * last = list->prev;
		struct list_head * at = head->next;

		first->prev = head;
		head->next = first;

		last->next = at;
		at->prev = last;
	}
}

/**
 * list_entry - get the struct for this entry
 * @param ptr 	the &struct list_head pointer.
 * @param type 	the type of the struct this is embedded in.
 * @param member 	the name of the list_struct within the struct.
 */
#define list_entry(ptr, type, member) \
	((type *)((char *)(ptr)-(unsigned long)(&((type *)0)->member)))

/**
 * list_for_each - iterate over a list
 * @param pos 	the &struct list_head to use as a loop counter.
 * @param head 	the head for your list.
 */
#define list_for_each(pos, head) \
	for (pos = (head)->next; pos != (head); pos = pos->next)

/**
 * list_for_each_safe - iterate over a list safe against removal of list entry
 * @param pos 	the &struct list_head to use as a loop counter.
 * @param n 		another &struct list_head to use as temporary storage
 * @param head 	the head for your list.
 */
#define list_for_each_safe(pos, n, head) \
	for (pos = (head)->next, n = pos->next; pos != (head); \
		pos = n, n = pos->next)

#define LIST_HEAD_INIT(name) { &(name), &(name) }

#define LIST_HEAD(name) struct list_head name = LIST_HEAD_INIT(name)

#endif /* OP_LIST_H */
