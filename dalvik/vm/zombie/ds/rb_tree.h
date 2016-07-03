#ifndef __BDS_RBTREE_H
#define __BDS_RBTREE_H

#ifdef __cplusplus
extern "C"
{
#endif

/*
  Red Black Tree based on Linux kernel's <linux/include/linux/rbtree.h>
*/

#include "common.h"

enum rb_color {RB_RED, RB_BLACK};

struct rb_node
{
  struct rb_node * rb_parent;
  enum rb_color rb_color;
  struct rb_node * rb_right;
  struct rb_node * rb_left;
};

struct rb_tree
{
  int (*compare) (const struct rb_tree *, const struct rb_node *, const struct rb_node *);
  struct rb_node * root;
};

#define INIT_RB_TREE(compare_func) { compare_func, NULL }
static inline void
rb_init(struct rb_tree * root, int (*compare) (const struct rb_tree *, const struct rb_node *, const struct rb_node *))
{
  root->compare = compare;
  root->root = NULL;
}

/**
 * Re-init this tree to empty.
 *
 * This is meant to be used after a full (postfix) clearing as such:
 *
 * struct rb_node * p = rb_first_postfix(&tree);
 * while(p != NULL) {
 *   struct rb_node * p_next = rb_next_postfix(p);
 *   struct entry_type * entry = container_of(p, struct entry_type, node_field);
 *   free(entry);
 *   p = p_next;
 * }
 * rb_reinit(tree);
 *
 * This allows for O(n) clearing without the O(log n) multiple to remove each item gracefully
 */
static inline void
rb_reinit(struct rb_tree * root)
{
  root->root = NULL;
}

static inline void
__rb_rotate_left(struct rb_node * node, struct rb_tree * root)
{
  struct rb_node * right = node->rb_right;

  if ((node->rb_right = right->rb_left))
    right->rb_left->rb_parent = node;
  right->rb_left = node;

  if ((right->rb_parent = node->rb_parent))
  {
    if (node == node->rb_parent->rb_left)
      node->rb_parent->rb_left = right;
    else
      node->rb_parent->rb_right = right;
  }
  else
    root->root = right;
  node->rb_parent = right;
}

static inline void
__rb_rotate_right(struct rb_node * node, struct rb_tree * root)
{
  struct rb_node * left = node->rb_left;

  if ((node->rb_left = left->rb_right))
    left->rb_right->rb_parent = node;
  left->rb_right = node;

  if ((left->rb_parent = node->rb_parent))
  {
    if (node == node->rb_parent->rb_right)
      node->rb_parent->rb_right = left;
    else
      node->rb_parent->rb_left = left;
  }
  else
    root->root = left;
  node->rb_parent = left;
}

static inline void 
__rb_insert_color(struct rb_node * node, struct rb_tree * root)
{
  struct rb_node * parent, * gparent;

  while ((parent = node->rb_parent) && parent->rb_color == RB_RED)
  {
    gparent = parent->rb_parent;
    if (parent == gparent->rb_left)
    {
      {
        register struct rb_node * uncle = gparent->rb_right;
        if (uncle && uncle->rb_color == RB_RED)
        {
          uncle->rb_color = RB_BLACK;
          parent->rb_color = RB_BLACK;
          gparent->rb_color = RB_RED;
          node = gparent;
          continue;
        }
      }

      if (parent->rb_right == node)
      {
        register struct rb_node * tmp;
        __rb_rotate_left(parent, root);
        tmp = parent;
        parent = node;
        node = tmp;
      }

      parent->rb_color = RB_BLACK;
      gparent->rb_color = RB_RED;
      __rb_rotate_right(gparent, root);
    } else {
      {
        register struct rb_node * uncle = gparent->rb_left;
        if (uncle && uncle->rb_color == RB_RED)
        {
          uncle->rb_color = RB_BLACK;
          parent->rb_color = RB_BLACK;
          gparent->rb_color = RB_RED;
          node = gparent;
          continue;
        }
      }

      if (parent->rb_left == node)
      {
        register struct rb_node * tmp;
        __rb_rotate_right(parent, root);
        tmp = parent;
        parent = node;
        node = tmp;
      }

      parent->rb_color = RB_BLACK;
      gparent->rb_color = RB_RED;
      __rb_rotate_left(gparent, root);
    }
  }

  root->root->rb_color = RB_BLACK;
}

/**
 * Return the parent of the given node. If the node already exists in the tree, then
 * exists is set to point to that node.
 */
static inline struct rb_node *
rb_find_parent_exists(const struct rb_tree * root, const struct rb_node * node, struct rb_node ** exists)
{
  struct rb_node * p = root->root;
  struct rb_node * parent = p;
  register int cmp = 0;
  
  while (p)
  {
    cmp = root->compare(root, p, node);
    parent = p;
    if (cmp < 0)      p = p->rb_right;
    else if (cmp > 0) p = p->rb_left;
    else
    {
      *exists = p;
      break;
    }
  }
  return parent;
}

/**
 * Insert the given node below the given parent.
 * The given parent node MUST be the accurate lead node which
 * should be the new node's parent (or NULL if tree is empty).
 * e.g. see rb_insert
 */
static inline void
rb_insert_on_parent(struct rb_tree * root, struct rb_node * node, struct rb_node * parent)
{
  if(parent == NULL)
  {
    root->root = node;
  }
  else
  {
    if (root->compare(root, parent, node) < 0)
      parent->rb_right = node;
    else // cmp > 0
      parent->rb_left = node;
  }
  node->rb_parent = parent;
  node->rb_color = RB_RED;
  node->rb_left = node->rb_right = NULL;
  __rb_insert_color(node, root);
}

static inline void
rb_insert(struct rb_tree * root, struct rb_node * node)
{
   struct rb_node * exists = NULL;
   struct rb_node * parent = rb_find_parent_exists(root, node, &exists);
   if(exists == NULL)
     rb_insert_on_parent(root, node, parent); 
}

static inline void 
__rb_erase_color(struct rb_node * node, struct rb_node * parent, struct rb_tree * root)
{
  struct rb_node * other;

  while ((!node || node->rb_color == RB_BLACK) && node != root->root)
  {
    if (parent->rb_left == node)
    {
      other = parent->rb_right;
      if (other->rb_color == RB_RED)
      {
        other->rb_color = RB_BLACK;
        parent->rb_color = RB_RED;
        __rb_rotate_left(parent, root);
        other = parent->rb_right;
      }
    if ((!other->rb_left ||
           other->rb_left->rb_color == RB_BLACK)
          && (!other->rb_right ||
        other->rb_right->rb_color == RB_BLACK))
      {
        other->rb_color = RB_RED;
        node = parent;
        parent = node->rb_parent;
      }
      else
      {
        if (!other->rb_right ||
            other->rb_right->rb_color == RB_BLACK)
        {
          register struct rb_node * o_left;
          if ((o_left = other->rb_left))
            o_left->rb_color = RB_BLACK;
          other->rb_color = RB_RED;
          __rb_rotate_right(other, root);
          other = parent->rb_right;
        }
        other->rb_color = parent->rb_color;
        parent->rb_color = RB_BLACK;
        if (other->rb_right)
          other->rb_right->rb_color = RB_BLACK;
        __rb_rotate_left(parent, root);
        node = root->root;
        break;
      }
    }
    else
    {
      other = parent->rb_left;
      if (other->rb_color == RB_RED)
      {
        other->rb_color = RB_BLACK;
        parent->rb_color = RB_RED;
        __rb_rotate_right(parent, root);
        other = parent->rb_left;
      }
      if ((!other->rb_left ||
           other->rb_left->rb_color == RB_BLACK)
          && (!other->rb_right ||
        other->rb_right->rb_color == RB_BLACK))
      {
        other->rb_color = RB_RED;
        node = parent;
        parent = node->rb_parent;
      }
      else
      {
        if (!other->rb_left ||
            other->rb_left->rb_color == RB_BLACK)
        {
          register struct rb_node * o_right;
          if ((o_right = other->rb_right))
            o_right->rb_color = RB_BLACK;
          other->rb_color = RB_RED;
          __rb_rotate_left(other, root);
          other = parent->rb_left;
        }
        other->rb_color = parent->rb_color;
        parent->rb_color = RB_BLACK;
        if (other->rb_left)
          other->rb_left->rb_color = RB_BLACK;
        __rb_rotate_right(parent, root);
        node = root->root;
        break;
      }
    }
  }
  if (node)
    node->rb_color = RB_BLACK;
}

static inline void
rb_remove(struct rb_tree * root, struct rb_node * node)
{
  struct rb_node * child, * parent;
  int color;

  if (!node->rb_left)
    child = node->rb_right;
  else if (!node->rb_right)
    child = node->rb_left;
  else
  {
    struct rb_node * old = node, * left;

    node = node->rb_right;
    while ((left = node->rb_left))
      node = left;
    child = node->rb_right;
    parent = node->rb_parent;
    color = node->rb_color;

    if (child)
      child->rb_parent = parent;
    if (parent)
    {
      if (parent->rb_left == node)
        parent->rb_left = child;
      else
        parent->rb_right = child;
    }
    else
      root->root = child;

    if (node->rb_parent == old)
      parent = node;
    node->rb_parent = old->rb_parent;
    node->rb_color = old->rb_color;
    node->rb_right = old->rb_right;
    node->rb_left = old->rb_left;

    if (old->rb_parent)
    {
      if (old->rb_parent->rb_left == old)
        old->rb_parent->rb_left = node;
      else
        old->rb_parent->rb_right = node;
    } else
      root->root = node;

    old->rb_left->rb_parent = node;
    if (old->rb_right)
      old->rb_right->rb_parent = node;
    goto color;
  }

  parent = node->rb_parent;
  color = node->rb_color;

  if (child)
    child->rb_parent = parent;
  if (parent)
  {
    if (parent->rb_left == node)
      parent->rb_left = child;
    else
      parent->rb_right = child;
  }
  else
    root->root = child;

color:
  if (color == RB_BLACK)
    __rb_erase_color(child, parent, root);
}

/**
 * rb_empty - tests whether a tree is empty
 * @root: the tree to test.
 */
static inline int
rb_empty(const struct rb_tree *root)
{
  return root->root == NULL;
}

/**
 * The following functions perform in-order traversal.
 */
static inline struct rb_node *
rb_first(const struct rb_tree *root)
{
  struct rb_node *n = root->root;
  if (!n)
    return NULL;
  while (n->rb_left)
    n = n->rb_left;
  return n;
}
#define rb_first_entry(tree, type, member) (container_of_safe(rb_first(tree), type, member))

static inline struct rb_node *
rb_last(const struct rb_tree *root)
{
  struct rb_node *n = root->root;
  if (!n)
    return NULL;
  while (n->rb_right)
    n = n->rb_right;
  return n;
}
#define rb_last_entry(tree, type, member) (container_of_safe(rb_last(tree), type, member))

static inline struct rb_node *
rb_next(const struct rb_node *node)
{
  /* If we have a right-hand child, go down and then left as far
     as we can. */
  if (node->rb_right) {
    node = node->rb_right; 
    while (node->rb_left)
      node=node->rb_left;
    return (struct rb_node *)node;
  }

  /* No right-hand children.  Everything down and left is
     smaller than us, so any 'next' node must be in the general
     direction of our parent. Go up the tree; any time the
     ancestor is a right-hand child of its parent, keep going
     up. First time it's a left-hand child of its parent, said
     parent is our 'next' node. */
  while (node->rb_parent && node == node->rb_parent->rb_right)
    node = node->rb_parent;

  return node->rb_parent;
}
#define rb_next_entry(tree, type, member) (container_of_safe(rb_next(tree), type, member))

static inline struct rb_node *
rb_prev(const struct rb_node *node)
{
  /* If we have a left-hand child, go down and then right as far
     as we can. */
  if (node->rb_left) {
    node = node->rb_left; 
    while (node->rb_right)
      node=node->rb_right;
    return (struct rb_node *)node;
  }

  /* No left-hand children. Go up till we find an ancestor which
     is a right-hand child of its parent */
  while (node->rb_parent && node == node->rb_parent->rb_left)
    node = node->rb_parent;

  return node->rb_parent;
}
#define rb_prev_entry(tree, type, member) (container_of_safe(rb_prev(tree), type, member))

/* Walk tree in order */
#define rb_for_each(pos, tree) \
  for(pos = rb_first(tree); pos != NULL; pos = rb_next(pos))

/* Walk tree in order by entries */
#define rb_for_each_entry(pos, tree, type, member) \
  for(pos = rb_first_entry(tree, type, member); \
      pos != NULL; \
      pos = rb_next_entry(&(pos->member), type, member))

/* Walk tree in reverse order */
#define rb_for_each_prev(pos, tree) \
  for(pos = rb_last(tree); pos != NULL; pos = rb_prev(pos))

/* Walk tree in reverse order by entries */
#define rb_for_each_prev_entry(pos, tree, type, member) \
  for(pos = rb_last_entry(tree, type, member); \
      pos != NULL; \
      pos = rb_prev_entry(&(pos->member), type, member))


/**
 * The following functions perform post-order traversal.
 *
 * Using these functions only make sence to quickly deallocate
 * the tree --- traverse via postfix and free each node as
 * you pass, this avoids the repeated rebalancing done by
 * removing every node.
 */
static inline struct rb_node *
rb_first_postfix(const struct rb_tree *root)
{
  return rb_first(root);
}
#define rb_first_postfix_entry(tree, type, member) (container_of_safe(rb_first_postfix(tree), type, member))

static inline struct rb_node *
rb_last_postfix(const struct rb_tree *root)
{
  return root->root;
}
#define rb_last_postfix_entry(tree, type, member) (container_of_safe(rb_last_postfix(tree), type, member))

static inline struct rb_node *
rb_next_postfix(const struct rb_node *node)
{
  struct rb_node * parent = node->rb_parent;
  if (parent == NULL) return NULL;

  struct rb_node * right_sibling = parent->rb_right;
  if (right_sibling == NULL || right_sibling == node) return parent;

  /* I am the left child & I do have a sibling, so my sibling hasn't been done. */
  node = right_sibling;
  /* Go right until we can go left */
  while (node->rb_left == NULL && node->rb_right != NULL)
    node = node->rb_right;
  /* Then go all the way left */
  while (node->rb_left)
    node = node->rb_left;

  return (struct rb_node *)node;
}
#define rb_next_postfix_entry(tree, type, member) (container_of_safe(rb_next_postfix(tree), type, member))

static inline struct rb_node *
rb_prev_postfix(const struct rb_node *node)
{
  if (node->rb_right != NULL) return node->rb_right;
  if (node->rb_left != NULL) return node->rb_left;

  /* I am a leaf, go up until we find a left sibling */
  struct rb_node * parent = node->rb_parent;
  while(parent && (parent->rb_right != node || parent->rb_left == NULL))
  {
    node = parent;
    parent = node->rb_parent;
  }

  /* Got up to the root and couldn't find a left! */
  if (parent == NULL) return NULL;

  return parent->rb_left;
}
#define rb_prev_postfix_entry(tree, type, member) (container_of_safe(rb_prev_postfix(tree), type, member))

/* Walk tree in postfix order */
#define rb_for_each_postfix(pos, tree) \
  for(pos = rb_first_postfix(tree); pos != NULL; pos = rb_next_postfix(pos))

/* Walk tree in postfix order by entries */
#define rb_for_each_postfix_entry(pos, tree, type, member) \
  for(pos = rb_first_postfix_entry(tree, type, member); \
      pos != NULL; \
      pos = rb_next_postfix_entry(&(pos->member), type, member))

/* Walk tree in reverse postfix order */
#define rb_for_each_prev_postfix(pos, tree) \
  for(pos = rb_last_postfix(tree); pos != NULL; pos = rb_prev_postfix(pos))

/* Walk tree in reverse postfix order by entries */
#define rb_for_each_prev_postfix_entry(pos, tree, type, member) \
  for(pos = rb_last_postfix_entry(tree, type, member); \
      pos != NULL; \
      pos = rb_prev_postfix_entry(&(pos->member), type, member))


/**
 * Returns the first element in the tree whose value is considered to go
 * after find. If a matching value exists then exists is set to that node.
 */
static inline struct rb_node *
__rb_upper_bound_exists(const struct rb_tree *root, const struct rb_node *find, struct rb_node **exists)
{
  struct rb_node * p = root->root;
  struct rb_node * last_greater = NULL;
  
  while (p)
  {
    register int cmp = root->compare(root, p, find);
    if (cmp > 0)
    {
      last_greater = p;
      p = p->rb_left;
    }
    else
    {
      if (cmp == 0)
        *exists = p; 
      p = p->rb_right;
    }
  }
  
  return last_greater;
}

/**
 * Returns the first element in the tree whose value is considered equal
 * to the search node.
 */
static inline struct rb_node *
rb_search(const struct rb_tree *root, const struct rb_node *find)
{
  struct rb_node * exists = NULL;
  rb_find_parent_exists(root, find, &exists);
  return exists; 
}
#define rb_search_entry(tree, find, type, member) \
    (container_of_safe(rb_search(tree, find), type, member))

/**
 * Returns the first element in the tree whose value is not considered to go
 * before search (i.e., either it is equivalent or goes after).
 */
static inline struct rb_node *
rb_lower_bound(const struct rb_tree *tree, const struct rb_node *search)
{
  struct rb_node * exists;
  struct rb_node * upper = __rb_upper_bound_exists(tree, search, &exists);
  if(exists) return exists;
  else return upper;
}
#define rb_lower_bound_entry(tree, search, type, member) \
    (container_of_safe(rb_lower_bound(tree, search), type, member))

/**
 * Returns the first element in the tree whose value is considered to go
 * after search.
 */
static inline struct rb_node *
rb_upper_bound(const struct rb_tree *tree, const struct rb_node *search)
{
  struct rb_node * exists;
  return __rb_upper_bound_exists(tree, search, &exists);
}
#define rb_upper_bound_entry(tree, search, type, member) \
    (container_of_safe(rb_upper_bound(tree, search), type, member))

#ifdef __cplusplus
}

#include <functional> 	// for std::less
#include <memory>	// for std::allocator
#include <utility>      // for std::pair

// C++ Class version
template<class T, class Compare = std::less<T>, class Alloc = std::allocator<T> >
class BDS_RB_Tree {
private:
  typedef BDS_RB_Tree<T, Compare, Alloc> This_Type;
  
  struct This_Node {
    T t;
    struct rb_node node;

    This_Node(T& new_t) : t(new_t) {}
  };

  struct rb_tree tree;

  typename Alloc::template rebind<This_Node>::other Node_Allocator;
  Alloc Key_Allocator;
  Compare Key_Compare;

  inline This_Node* make_node(const T& t) {
    This_Node* n = Node_Allocator.allocate(1);
    Key_Allocator.construct(&(n->t), t);
    return n;
  }

  inline void kill_node(This_Node* n) {
    Key_Allocator.destroy(&(n->t));
    Node_Allocator.deallocate(n, 1);
  }

  static int compareNodes(const struct rb_tree *tree, const struct rb_node *a, const struct rb_node *b)
  {
    This_Type* _this = container_of(tree, This_Type, tree);
    T *ma = &(container_of(a, This_Node, node)->t);
    T *mb = &(container_of(b, This_Node, node)->t);
    if(_this->Key_Compare(*ma, *mb)) // ma < mb 
      return -1;
    else if(_this->Key_Compare(*mb, *ma)) // mb < ma 
      return 1;
    else // ma = mb
      return 0;
  }

  /**
   * Abstract Iterator Class
   */
  typedef struct rb_node * (* first_func)(const struct rb_tree *);
  typedef struct rb_node * (* last_func) (const struct rb_tree *);
  typedef struct rb_node * (* next_func) (const struct rb_node *);
  typedef struct rb_node * (* prev_func) (const struct rb_node *);

  template<first_func first, last_func last, next_func next, prev_func prev>
  class This_Itr : public std::iterator<std::bidirectional_iterator_tag, T> {
    typedef This_Itr<first, last, next, prev> This_Type;
    friend class BDS_RB_Tree<T, Compare, Alloc>;
  private:
    struct rb_tree * my_tree;
    struct rb_node * pointee;

    This_Itr(struct rb_tree *t, This_Node *p)      : my_tree(t), pointee(&(p->node)) {}
    This_Itr(struct rb_tree *t, struct rb_node *p) : my_tree(t), pointee(p) {}
    This_Itr(struct rb_tree *t)			   : my_tree(t), pointee(NULL) {}

  public:
    This_Itr() : my_tree(NULL), pointee(NULL) {}

    This_Type& operator++() //prefix
    {
      pointee = next(pointee);
      return *this;
    }
    This_Type operator++(int) // postfix
    {
      This_Type it(my_tree, pointee);
      pointee = next(pointee);
      return it;
    }
    This_Type& operator--() //prefix
    {
      if(pointee == NULL)
        pointee = last(my_tree);
      else
        pointee = prev(pointee);
      return *this;
    }
    This_Type operator--(int) //postfix
    {
      This_Type it(my_tree, pointee);
      if(pointee == NULL)
        pointee = last(my_tree);
      else
        pointee = prev(pointee);
      return it;
    }
    bool operator==(const This_Type& rhs) { return my_tree==rhs.my_tree && pointee==rhs.pointee; }
    bool operator!=(const This_Type& rhs) { return my_tree!=rhs.my_tree || pointee!=rhs.pointee; }
    T& operator*() { return container_of(pointee, This_Node, node)->t; }
    T* operator->() { return &(operator*()); }
  };

  /**
   * Infix iterator
   */
private:
  // fix the stupid linkage issue
  static inline struct rb_node * (first_infix)(const struct rb_tree * t) { return rb_first(t); }
  static inline struct rb_node * (last_infix) (const struct rb_tree * t) { return rb_last(t); }
  static inline struct rb_node * (next_infix) (const struct rb_node * n) { return rb_next(n); }
  static inline struct rb_node * (prev_infix) (const struct rb_node * n) { return rb_prev(n); }
public:
  typedef This_Itr<This_Type::first_infix, This_Type::last_infix, This_Type::next_infix, This_Type::prev_infix> iterator;
  iterator begin() { return iterator(&tree, rb_first(&tree)); }
  iterator end() { return iterator(&tree); }
  
  /**
   * Postfix iterator
   */
private:
  // fix the stupid linkage issue
  static inline struct rb_node * (first_postfix)(const struct rb_tree * t) { return rb_first_postfix(t); }
  static inline struct rb_node * (last_postfix) (const struct rb_tree * t) { return rb_last_postfix(t); }
  static inline struct rb_node * (next_postfix) (const struct rb_node * n) { return rb_next_postfix(n); }
  static inline struct rb_node * (prev_postfix) (const struct rb_node * n) { return rb_prev_postfix(n); }
public:
  typedef This_Itr<This_Type::first_postfix, This_Type::last_postfix, This_Type::next_postfix, This_Type::prev_postfix> iterator_postfix;
  iterator_postfix begin_postfix() { return iterator_postfix(&tree, rb_first_postfix(&tree)); }
  iterator_postfix end_postfix() { return iterator_postfix(&tree); }


  BDS_RB_Tree() {
    rb_init(&tree, This_Type::compareNodes);
  }

  BDS_RB_Tree(const This_Type& source) {
    rb_init(&tree, This_Type::compareNodes);
    This_Node * source_n;
    rb_for_each_entry(source_n, &(source.tree), This_Node, node) 
      rb_insert(&tree, &(make_node(source_n->t)->node));
  }

  ~BDS_RB_Tree() {
    this->clear();
  }

  std::pair<iterator, bool> insert(const T& t) {
    This_Node* n = make_node(t);
    struct rb_node * exists = NULL;
    struct rb_node * parent = rb_find_parent_exists(&tree, &(n->node), &exists);
    if(exists == NULL) {
      rb_insert_on_parent(&tree, &(n->node), parent); 
      return std::make_pair(iterator(&tree, n), true);
    }
    else {
      return std::make_pair(iterator(&tree, exists), false);
    }
  }

  iterator erase(iterator pos) {
    if(pos.pointee == NULL) return pos;
    iterator kill = pos++;
    struct rb_node * p = kill.pointee;
    rb_remove(&tree, p);
    This_Node* n = container_of(p, This_Node, node);
    kill_node(n);
    return pos;
  }

  void clear() {
    // O(n) time because we avoid calling rb_remove ;)
    struct rb_node * p = rb_first_postfix(&tree);
    while(p != NULL) {
      struct rb_node * p_next = rb_next_postfix(p);
      This_Node* n = container_of(p, This_Node, node);
      kill_node(n);
      p = p_next;
    }
    rb_reinit(&tree);
  }

  bool empty() {
    return rb_empty(&tree) != 0;
  }

  This_Type& operator= (const This_Type& source) {
    if(this != &source) {
      this->clear();
      This_Node * source_n;
      rb_for_each_entry(source_n, &(source.tree), This_Node, node) 
        rb_insert(&tree, &(make_node(source_n->t)->node));
    }
    return *this;
  }

  iterator upper_bound(const T& t) {
    // make a virtual node for "search"
    This_Node* search = container_of((&t), This_Node, t);
    return iterator(&tree, rb_upper_bound(&tree, &search->node));
  }

  iterator contains(const T& t) {
    // make a virtual node for "search"
    This_Node* search = container_of((&t), This_Node, t);
    return iterator(&tree, rb_search(&tree, &search->node));
  }

};

#endif // __cplusplus

#endif  // __BDS_RBTREE_H
