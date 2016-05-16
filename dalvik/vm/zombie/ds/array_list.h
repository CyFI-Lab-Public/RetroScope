#ifndef __BDS_LIST_H
#define __BDS_LIST_H

#ifdef __cplusplus
extern "C"
{
#endif

/* This file is from Linux Kernel (include/linux/list.h) 
 * and modified by simply removing hardware prefetching of list items. 
 * Here by copyright, credits attributed to wherever they belong.
 */

#include "common.h"

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
	struct list_head *next, *prev;
};

#define INIT_LIST(name) { &(name), &(name) }

static inline void list_init(struct list_head *ptr)
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
static inline void __list_add(struct list_head *new_elem,
			      struct list_head *prev,
			      struct list_head *next)
{
	next->prev = new_elem;
	new_elem->next = next;
	new_elem->prev = prev;
	prev->next = new_elem;
}

/**
 * list_add - add a new entry
 * @new_elem: new entry to be added
 * @head: list head to add it after
 *
 * Insert a new entry after the specified head.
 * This is good for implementing stacks.
 */
static inline void list_add(struct list_head *head, struct list_head *new_elem)
{
	__list_add(new_elem, head, head->next);
}

/**
 * list_add_tail - add a new entry
 * @new_elem: new entry to be added
 * @head: list head to add it before
 *
 * Insert a new entry before the specified head.
 * This is useful for implementing queues.
 */
static inline void list_add_tail(struct list_head *head, struct list_head *new_elem)
{
	__list_add(new_elem, head->prev, head);
}

/*
 * Delete a list entry by making the prev/next entries
 * point to each other.
 *
 * This is only for internal list manipulation where we know
 * the prev/next entries already!
 */
static inline void __list_del(struct list_head *prev, struct list_head *next)
{
	next->prev = prev;
	prev->next = next;
}

/**
 * list_del - deletes entry from list.
 * @entry: the element to delete from the list.
 * Note: list_empty on entry does not return true after this, the entry is in an undefined state.
 */
static inline void list_del(struct list_head *entry)
{
	__list_del(entry->prev, entry->next);
	entry->next = NULL;
	entry->prev = NULL;
}

/**
 * list_del_init - deletes entry from list and reinitialize it.
 * @entry: the element to delete from the list.
 */
static inline void list_del_init(struct list_head *entry)
{
	__list_del(entry->prev, entry->next);
	list_init(entry); 
}

/**
 * list_move - delete from one list and add to head's list
 * @list: the entry to move
 * @head: the head that will precede our entry
 */
static inline void list_move_to(struct list_head *list, struct list_head *head)
{
        __list_del(list->prev, list->next);
        list_add(head, list);
}

/**
 * list_move_tail - delete from one list and add as another's tail
 * @list: the entry to move
 * @head: the head that will follow our entry
 */
static inline void list_move_to_tail(struct list_head *list,
				  struct list_head *head)
{
        __list_del(list->prev, list->next);
        list_add_tail(head, list);
}

/**
 * list_empty - tests whether a list is empty
 * @head: the list to test.
 */
static inline int list_empty(const struct list_head *head)
{
	return head->next == head;
}

static inline void __list_splice(struct list_head *list,
				 struct list_head *head)
{
	struct list_head *first = list->next;
	struct list_head *last = list->prev;
	struct list_head *at = head->next;

	first->prev = head;
	head->next = first;

	last->next = at;
	at->prev = last;
}

/**
 * list_splice - join two lists
 * @old: the old list to add to new
 * @new_list: the place to add it in the first list.
 */
static inline void list_splice(struct list_head *old, struct list_head *new_list)
{
	if (!list_empty(old))
		__list_splice(old, new_list);
}

/**
 * list_splice_init - join two lists and reinitialise the emptied list.
 * @old: the old list to add to new
 * @new_list: the place to add it in the first list.
 *
 * The list at @old is reinitialised
 */
static inline void list_splice_init(struct list_head *old,
				    struct list_head *new_list)
{
	if (!list_empty(old)) {
		__list_splice(old, new_list);
		list_init(old);
	}
}

/**
 * list_for_each	-	iterate over a list
 * @pos:	the &struct list_head to use as a loop counter.
 * @head:	the head for your list.
 */
#define list_for_each(pos, head) \
	for (pos = (head)->next; pos != (head); \
        	pos = pos->next)
/**
 * list_for_each_prev	-	iterate over a list backwards
 * @pos:	the &struct list_head to use as a loop counter.
 * @head:	the head for your list.
 */
#define list_for_each_prev(pos, head) \
	for (pos = (head)->prev; pos != (head); \
        	pos = pos->prev)
        	
/**
 * list_for_each_safe	-	iterate over a list safe against removal of list entry
 * @pos:	the &struct list_head to use as a loop counter.
 * @n:		another &struct list_head to use as temporary storage
 * @head:	the head for your list.
 */
#define list_for_each_safe(pos, n, head) \
	for (pos = (head)->next, n = pos->next; pos != (head); \
		pos = n, n = pos->next)

/**
 * list_for_each_entry	-	iterate over list of given type
 * @pos:	the type * to use as a loop counter.
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 */
#define list_for_each_entry(pos, head, type, member)  \
	for (pos = container_of((head)->next, type, member);	\
	     &pos->member != (head); 					\
	     pos = container_of(pos->member.next, type, member))

/**
 * list_for_each_entry_safe - iterate over list of given type safe against removal of list entry
 * @pos:	the type * to use as a loop counter.
 * @n:		another type * to use as temporary storage
 * @head:	the head for your list.
 * @member:	the name of the list_struct within the struct.
 */
#define list_for_each_entry_safe(pos, n, head, type, member)	\
	for (pos = container_of((head)->next, type, member),	\
         n = container_of(pos->member.next, type, member);	\
	     &pos->member != (head); 					\
	     pos = n, n = container_of(n->member.next, type, member))

#ifdef __cplusplus
}

#include <memory>	// for std::allocator

// C++ Class version
template<class T, class Alloc = std::allocator<T> >
class BDS_List {
private:
  typedef BDS_List<T, Alloc> This_Type;
  
  struct This_Node {
    T t;
    struct list_head node;

    This_Node(T& new_t) : t(new_t) {}
  };

  struct list_head head;

  typename Alloc::template rebind<This_Node>::other Node_Allocator;
  Alloc Key_Allocator;

  inline This_Node* make_node(const T& t) {
    This_Node* n = Node_Allocator.allocate(1);
    Key_Allocator.construct(&(n->t), t);
    return n;
  }

  inline void kill_node(This_Node* n) {
    Key_Allocator.destroy(&(n->t));
    Node_Allocator.deallocate(n, 1);
  }

  inline void remove_elem(struct list_head * p)
  {
    list_del(p);
    This_Node* n = container_of(p, This_Node, node);
    kill_node(n);
  }

  inline void remove_elems_behind(struct list_head * start)
  {
    while(start != &head)
    {
      struct list_head * next = start->next;
      remove_elem(start);
      start = next;
    }
  }

  static inline void
  overwrite_range_from_other(struct list_head ** other_start_p,
                             const struct list_head * other_end,
                             struct list_head ** self_start_p,
                             const struct list_head * self_end)
  {
    struct list_head * other_start = *other_start_p;
    struct list_head * self_start = *self_start_p;
    while(other_start != other_end)
    {
      if(self_start == self_end) break;
      This_Node * source_n = container_of(other_start, This_Node, node);
      This_Node * dest_n   = container_of(self_start, This_Node, node);
      dest_n->t = source_n->t;
      self_start = self_start->next;
      other_start = other_start->next;
    }
    *other_start_p = other_start;
    *self_start_p = self_start;
  }

  inline void copy_other_range_to_self(const struct list_head * start,
                                       const struct list_head * end,
                                       struct list_head * self)
  {
    while(start != end)
    {
      This_Node * source_n = container_of(start, This_Node, node);
      This_Node * copied = make_node(source_n->t);
      struct list_head * new_list_node = &(copied->node);
      list_add(self, new_list_node);
      self = new_list_node;
      start = start->next;
    }
  }

public:
  class This_Itr : public std::iterator<std::bidirectional_iterator_tag, T> {
  friend class BDS_List<T, Alloc>;

  private:
    struct list_head * head;
    struct list_head * pointee;
    
    This_Itr(struct list_head *h, struct list_head *p) : head(h) 
    {
      if(p == head) pointee = NULL;
      else          pointee = p;
    }

    This_Itr(struct list_head *h, This_Node *p) { This_Itr(h, &(p->node)); }
    This_Itr(struct list_head *h) : head(h), pointee(NULL) {}

  public:
    This_Itr() : head(NULL), pointee(NULL) {}

    This_Itr& operator++() //prefix
    {
      pointee = pointee->next;
      if(pointee == head) pointee = NULL;
      return *this;
    }
    This_Itr operator++(int) // postfix
    {
      This_Itr it(head, pointee);
      pointee = pointee->next;
      if(pointee == head) pointee = NULL;
      return it;
    }
    This_Itr& operator--() //prefix
    {
      if(pointee == NULL)
        pointee = head;
      pointee = pointee->prev;
      if(pointee == head) pointee = NULL;
      return *this;
    }
    This_Itr operator--(int) //postfix
    {
      This_Itr it(head, pointee);
      if(pointee == NULL)
        pointee = head;
      pointee = pointee->prev;
      if(pointee == head) pointee = NULL;
      return it;
    }
    bool operator==(const This_Itr& rhs) { return head==rhs.head && pointee==rhs.pointee; }
    bool operator!=(const This_Itr& rhs) { return head!=rhs.head || pointee!=rhs.pointee; }
    T& operator*() { return container_of(pointee, This_Node, node)->t; }
    T* operator->() { return &(operator*()); }
  };
  typedef This_Itr iterator;


  BDS_List() {
    list_init(&head);
  }

  // copy constructor
  BDS_List(const This_Type& source) {
    list_init(&head);
    copy_other_range_to_self(source.head.next, &(source.head), &head);
  }

  ~BDS_List() {
    this->clear();
  }

  iterator push_front(const T& t) {
    This_Node* n = make_node(t);
    list_add(&head, &(n->node));
    return iterator(&head, n); 
  }
  
  void pop_front() {
    if(empty()) return;
    remove_elem(head.next);
  }

  iterator push_back(const T& t) {
    This_Node* n = make_node(t);
    list_add_tail(&head, &(n->node));
    return iterator(&head, n); 
  }

  void pop_back() {
    if(empty()) return;
    remove_elem(head.prev);
  }

  iterator erase(iterator pos) {
    if(pos.pointee == NULL) return pos;
    iterator kill = pos++;
    remove_elem(kill.pointee);
    return pos;
  }

  void clear() {
    remove_elems_behind(head.next);
  }

  bool empty() const {
    return list_empty(&head) != 0;
  }
  
  This_Type& operator= (const This_Type& source) {
    if(this != &source) {
      struct list_head *       other_start = source.head.next;
      const struct list_head * other_end = &(source.head);
      struct list_head *       self_start = head.next;
      const struct list_head * self_end = &head;
      // avoid as much allocation as possible.
      overwrite_range_from_other(&other_start, other_end,
                                 &self_start, self_end);

      if(other_start == other_end) { // we had more space than needed!
        remove_elems_behind(self_start);
      } else { // allocate any remaining
        copy_other_range_to_self(other_start, other_end, head.prev);
      }
    }
    return *this;
  }

  iterator begin() { return iterator(&head, head.next); }
  iterator end() { return iterator(&head); }

  bool operator== (const This_Type& other) const {
    if(this == &other) return true;
    if(empty() && other.empty()) return true;

    This_Node * other_node = container_of(other.head.next, This_Node, node);
    This_Node * my_node;
    list_for_each_entry(my_node, &(head), This_Node, node) {
      if(&(other_node->node) == &(other.head)) break;
      if(!(other_node->t == my_node->t)) return false;
      other_node = container_of(other_node->node.next, This_Node, node);
    }
    return (&(other_node->node) == &(other.head)) &&
           (&(my_node->node) == &(head));
  }
};

#endif // __cplusplus

#endif // __BDS_LIST_H
