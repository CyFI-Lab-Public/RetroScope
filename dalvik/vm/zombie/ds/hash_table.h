#ifndef __BDS_HASH_TABLE_H
#define __BDS_HASH_TABLE_H

#include "common.h"
#include "rb_tree.h"

#ifdef __cplusplus
extern "C"
{

/**
 * WARNING: This class REQUIRES C++ with TR1 or later
 * E.g., g++ -std=c++0x ...
 */


#endif

/*
 * Simple hash table implementation.
 */
struct ht_value {
};

struct ht_key {
  struct ht_value *val;
  struct rb_node key_set_node;
  struct rb_node bucket_node;
};

/* 
 * All Keys are kept in one RB Tree for key searches.
 * Each bucket in the hash table is an RB Tree, thus
 * all keys which hash to the same bucket are stored
 * in an RB Tree. Thus lookup(...) is O(1) at best
 * and O(log n) where n = # of collisions at worst.
 */

struct hash_table {
  unsigned long (*hash_func)(const struct hash_table *, const struct ht_key*);
  int (*key_compare)(const struct hash_table *, const struct ht_key*, const struct ht_key*);
  unsigned long num_buckets;
  struct rb_tree buckets;  // We abuse our knowledge of the rb_tree to
  struct rb_node **roots;  // optimize the memory usage. 
  struct rb_tree key_set;
};

/**
 * This does some magic to call the hash_table's specific
 * key compare function given only the rb_nodes for those
 * keys
 */
static inline int
key_set_compare (const struct rb_tree *this_tree, const struct rb_node * node1, const struct rb_node * node2)
{
  const struct ht_key * key1 = container_of(node1, struct ht_key, key_set_node);
  const struct ht_key * key2 = container_of(node2, struct ht_key, key_set_node);
  const struct hash_table * ht = container_of(this_tree, struct hash_table, key_set);
  return ht->key_compare(ht, key1, key2);
}

static inline int
buckets_compare (const struct rb_tree *this_tree, const struct rb_node * node1, const struct rb_node * node2)
{
  const struct ht_key * key1 = container_of(node1, struct ht_key, bucket_node);
  const struct ht_key * key2 = container_of(node2, struct ht_key, bucket_node);
  const struct hash_table * ht = container_of(this_tree, struct hash_table, buckets);
  return ht->key_compare(ht, key1, key2);
}

/**
 * Compute the size to alloc for the "entries" array given to ht_init.
 */
#define ht_bytes_needed_for_entries(x) (sizeof(struct rb_node) * (x))

static inline void
ht_init(struct hash_table *ht,
        unsigned long (*hash_func)(const struct hash_table *, const struct ht_key*), 
        int (*compare)(const struct hash_table *, const struct ht_key*, const struct ht_key*),
        void** /* struct rb_node* [] */ entries_array,
        unsigned long num_entries)
{
  unsigned long i;
  ht->hash_func = hash_func;
  ht->key_compare = compare;
  ht->roots = (struct rb_node **)entries_array;
  ht->num_buckets = num_entries;
  rb_init(&(ht->buckets), buckets_compare);
  for(i = 0; i < num_entries; i++)
    ht->roots[i] = ht->buckets.root;
  rb_init(&(ht->key_set), key_set_compare);
}

/**
 * Re-init this hash table to empty.
 *
 * This is meant to be used after a full clearing as such:
 *
 * struct ht_value * v;
 * unsigned long bucket_num;
 * struct ht_key   * k = ht_first_by_buckets(hash_table_inst, &bucket_num, &v)
 * while(k != NULL)
 * {
 *   struct hash_table_val * val = container_of(v, struct hash_table_val, val_field);
 *   free(val);
 *   struct ht_key * next_k = ht_next_by_buckets(k, &bucket_num, &v);
 *   struct hash_table_key * key = container_of(k, struct hash_table_key, key_field);
 *   free(key);
 *   k = next_k;
 * }
 * ht_reinit(hash_table_inst);
 *
 * This allows for O(n) clearing without the O(log n) multiple to remove each item gracefully
 */
static inline void
ht_reinit(struct hash_table *ht)
{
  rb_reinit(&(ht->buckets));
  for(i = 0; i < ht->num_buckets; i++)
    ht->roots[i] = ht->buckets.root;
  rb_reinit(&(ht->key_set));
}

static inline int
ht_empty(const struct hash_table *ht)
{
  return rb_empty(&(ht->key_set)) != 0;
}

static inline void
ht_insert(struct hash_table *ht, struct ht_key *key, struct ht_value* val)
{
  unsigned long index = ht->hash_func(ht, key) % ht->num_buckets;
  ht->buckets.root = ht->roots[index];
  rb_insert(&(ht->buckets), &(key->bucket_node));
  ht->roots[index] = ht->buckets.root; // may have updated the root!
  key->val = val;
  rb_insert(&(ht->key_set), &(key->key_set_node));
}

static inline struct ht_value*
ht_lookup(const struct hash_table *ht, const struct ht_key *key)
{
  unsigned long index = ht->hash_func(ht, key) % ht->num_buckets;
  ht->buckets.root = ht->roots[index];
  struct ht_key* real_key = rb_search_entry(&(ht->buckets), &(key->bucket_node),
                                            struct ht_key, bucket_node);
  if(real_key == NULL) return NULL;
  else return real_key->val;
}
#define ht_lookup_entry(ht, key, type, member) (container_of_safe(ht_lookup((ht), (key)), type, member))

static inline struct ht_value*
ht_remove(struct hash_table *ht, const struct ht_key *key)
{
  struct ht_value* ret = NULL;
  unsigned long index = ht->hash_func(ht, key) % ht->num_buckets;
  ht->buckets.root = ht->roots[index];
  struct ht_key* real_key = rb_search_entry(&(ht->buckets), &(key->bucket_node),
                                            struct ht_key, bucket_node);
  if(real_key != NULL) {
    rb_remove(&(ht->buckets), &(real_key->bucket_node));
    ht->roots[index] = ht->buckets.root; // may have updated the root!
    rb_remove(&(ht->key_set), &(real_key->key_set_node));
    ret = real_key->val;
    real_key->val = NULL;
  }

  return ret;
}
#define ht_remove_entry(ht, key, type, member) (container_of_safe(ht_remove((ht), (key)), type, member))

static inline struct ht_key *
ht_first(const struct hash_table *ht, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_first(&(ht->key_set));
  if(!next_rb) return NULL;
  struct ht_key * next_key = container_of(next_rb, struct ht_key, key_set_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

static inline struct ht_key *
ht_last(const struct hash_table *ht, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_last(&(ht->key_set));
  if(!next_rb) return NULL;
  struct ht_key * next_key = container_of(next_rb, struct ht_key, key_set_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

static inline struct ht_key *
ht_next(const struct ht_key *key, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_next(&(key->key_set_node));
  if(!next_rb) return NULL;
  struct ht_key * next_key = container_of(next_rb, struct ht_key, key_set_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

static inline struct ht_key *
ht_prev(const struct ht_key *key, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_prev(&(key->key_set_node));
  if(!next_rb) return NULL;
  struct ht_key * next_key = container_of(next_rb, struct ht_key, key_set_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

/* Walk keys in order */
#define ht_for_each(key, value_ptr, ht) \
  for(key = ht_first(ht, value_ptr); key != NULL; key = ht_next(key, value_ptr))

/* Walk keys in reverse order */
#define ht_for_each_prev(key, value_ptr, ht) \
  for(pos = ht_last(tree); pos != NULL; pos = ht_prev(pos))

/**
 * Traverse the HT in bucket order.
 * This is generally useless except for a fast way to
 * traverse all (unordered) keys/values (e.g., for clearing the ht).
 * Note that ret_value may be NULL, but bucket_num CANNOT because it
 * must be given to ht_next_by_buckets and ht_prev_by_buckets.
 */
static inline struct ht_key *
__first_of_next_bucket(const struct hash_table *ht, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  unsigned long bucket = *bucket_num;
  struct rb_node ** root = &(ht->roots[bucket]);
  while(bucket < ht->num_buckets && *root == NULL)
  {
    bucket++;
    root++;
  }
  if(*root == NULL) return NULL;

  *bucket_num = bucket;
  
  struct rb_tree temp_tree = ht->buckets;
  temp_tree.root = *root;
  struct rb_node * next_rb = rb_first_postfix(&(temp_tree)); // must be postfix to allow free on the node
  struct ht_key * next_key = container_of(next_rb, struct ht_key, bucket_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

static inline struct ht_key *
__last_of_prev_bucket(const struct hash_table *ht, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  unsigned long bucket = *bucket_num;
  struct rb_node ** root = &(ht->roots[bucket]);
  while(bucket > 0 && *root == NULL)
  {
    bucket--;
    root--;
  }
  if(*root == NULL) return NULL;

  *bucket_num = bucket;
  
  struct rb_tree temp_tree = ht->buckets;
  temp_tree.root = *root;
  struct rb_node * next_rb = rb_last_postfix(&(temp_tree)); // must be postfix to allow free on the node
  struct ht_key * next_key = container_of(next_rb, struct ht_key, bucket_node);
  if(ret_value) *ret_value = next_key->val;
  return next_key;
}

static inline struct ht_key *
ht_first_by_buckets(const struct hash_table *ht, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  if(ht_empty(ht)) return NULL;

  *bucket_num = 0;
  return __first_of_next_bucket(ht, bucket_num, ret_value);  
}

static inline struct ht_key *
ht_last_by_buckets(const struct hash_table *ht, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  if(ht_empty(ht)) return NULL;

  *bucket_num = ht->num_buckets-1;
  return __last_of_prev_bucket(ht, bucket_num, ret_value);
}

static inline struct ht_key *
ht_next_by_buckets(const struct ht_key *key, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_next_postfix(&(key->bucket_node));
  if(next_rb != NULL)
  {
    struct ht_key * next_key = container_of(next_rb, struct ht_key, bucket_node);
    if(ret_value) *ret_value = next_key->val;
    return next_key;
  }

  *bucket_num = *bucket_num + 1;
  return __first_of_next_bucket(ht, bucket_num, ret_value);  
}

static inline struct ht_key *
ht_prev_by_buckets(const struct ht_key *key, unsigned long * bucket_num, struct ht_value ** ret_value)
{
  struct rb_node * next_rb = rb_prev_postfix(&(key->bucket_node));
  if(next_rb != NULL)
  {
    struct ht_key * next_key = container_of(next_rb, struct ht_key, bucket_node);
    if(ret_value) *ret_value = next_key->val;
    return next_key;
  }

  *bucket_num = *bucket_num - 1;
  return __last_of_prev_bucket(ht, bucket_num, ret_value);  
}

#define ht_for_each_by_bucket(key, bucket_num_ptr, value_ptr, ht) \
  for(key = ht_first_by_buckets(ht, bucket_num_ptr, value_ptr); \
      key != NULL; \
      key = ht_next_by_buckets(key, bucket_num_ptr, value_ptr))

#define ht_for_each_prev_by_bucket(key, bucket_num_ptr, value_ptr, ht) \
  for(key = ht_last_by_buckets(ht, bucket_num_ptr, value_ptr); \
      key != NULL; \
      key = ht_prev_by_buckets(key, bucket_num_ptr, value_ptr))

#ifdef __cplusplus
}

#include <memory>	// for std::allocator
#include <functional>	// for std::hash
#include <utility>      // for std::pair

/**
 * When using this with C++ std code, this class can act as BOTH an std::map and
 * std::unordered_map. It has the same time constraints as std::unordered_map but ALSO
 * has strictly ordered keys as required by std::map. :)
 */

// C++ Class version
template<class Key, class Value,
         class Hash = std::hash<Key>,
         class Compare = std::less<Key>,
         class Alloc = std::allocator<std::pair<const Key, Value> > >
class BDS_Hash_Table {
private:
  typedef BDS_Hash_Table<Key, Value, Hash, Compare, Alloc> This_Type;

  typedef std::pair<const Key, Value> This_KV_Pair;

  struct This_Value {
    This_KV_Pair kv_pair;
    struct ht_value val;

    This_Value(const Key& k, Value& v) : kv_pair(This_KV_Pair(k,v)) {}
  };
 
  static inline This_Value * ht_key2This_Value(struct ht_key* key)
  {
    return container_of(key->val, This_Value, val);
  }

  static inline This_KV_Pair * ht_key2This_KV_Pair(struct ht_key* key)
  {
    return &(ht_key2This_Value(key)->kv_pair);
  }

  typename Alloc::template rebind<This_Value>::other Value_Allocator;
  typename Alloc::template rebind<rb_node *>::other rb_node_ptr_Allocator;

  Compare Key_Compare;
  Hash Key_Hash;

  static int compareKeys(const struct hash_table *this_hash_table,
                         const struct ht_key *key1, const struct ht_key *key2)
  {
    const This_Type* _this = container_of(this_hash_table, This_Type, ht);
    const Key k1 = ht_key2This_KV_Pair(key1)->first;
    const Key k2 = ht_key2This_KV_Pair(key2)->first;

    if(_this->Key_Compare(k1, k2)) // k1 < k2 
      return -1;
    else if(_this->Key_Compare(k1, k2)) // k1 < k2 
      return 1;
    else // k1 = k2
      return 0;
  }

  static unsigned long hashKey(const struct hash_table *this_hash_table, const struct ht_key *key)
  {
    const This_Type* _this = container_of(this_hash_table, This_Type, ht);
    const Key k = ht_key2This_KV_Pair(key)->first;
    return _this->Key_Hash(k);
  }

  inline This_Value* make_entry(const Key& k, Value v) {
    return new(Value_Allocator.allocate(1)) This_Value(k, v);
  }

  inline void kill_entry(This_Value* v) {
    v->~This_Value();
    Value_Allocator.deallocate(v, 1);
  }

  struct hash_table ht;

public:
  class This_Itr : public std::iterator<std::bidirectional_iterator_tag, std::pair<const Key, Value>> {
  friend class BDS_Hash_Table<Key, Value, Hash, Compare, Alloc>;

  private:
    struct hash_table * my_ht;
    struct ht_key * pointee;

    This_Itr(struct hash_table * ht, struct ht_key *k) : my_ht(ht), pointee(k) {}
    This_Itr(struct hash_table * ht)                   : my_ht(ht), pointee(NULL) {}

  public:
    This_Itr() : my_ht(NULL), pointee(NULL) {}

    This_Itr& operator++() //prefix
    {
      pointee = ht_next(pointee, NULL);
      return *this;
    }
    This_Itr operator++(int) // postfix
    {
      This_Itr it(my_ht, pointee);
      pointee = ht_next(pointee, NULL);
      return it;
    }
    This_Itr& operator--() //prefix
    {
      if(pointee == NULL)
        pointee = ht_last(my_ht, NULL);
      else
        pointee = ht_prev(pointee, NULL);
      return *this;
    }
    This_Itr operator--(int) //postfix
    {
      This_Itr it(my_ht, pointee);
      if(pointee == NULL)
        pointee = ht_last(my_ht, NULL);
      else
        pointee = ht_prev(pointee, NULL);
      return it;
    }
    bool operator==(const This_Itr& rhs) { return my_ht==rhs.my_ht && pointee==rhs.pointee; }
    bool operator!=(const This_Itr& rhs) { return my_ht!=rhs.my_ht || pointee!=rhs.pointee; }
    std::pair<const Key, Value>& operator*() { return *(ht_key2This_KV_Pair(pointee)); }
    std::pair<const Key, Value>* operator->() { return &(operator*()); }
  };
  typedef This_Itr iterator;

  BDS_Hash_Table() {
    #define NUM_HT_BUCKETS 1024
    rb_node** buckets = rb_node_ptr_Allocator.allocate(NUM_HT_BUCKETS);
    ht_init(&ht, This_Type::hashKey, This_Type::compareKeys, (void**)buckets, NUM_HT_BUCKETS);
    #undef NUM_HT_BUCKETS
  }

/*
  BDS_Hash_Table(const This_Type& source) {
    rb_init(&tree, This_Type::compareNodes);
    This_Node * source_n;
    rb_for_each_entry(source_n, &(source.tree), This_Node, node) 
      rb_insert(&tree, &(make_node(source_n->t)->node));
  }
*/

  ~BDS_Hash_Table() {
    this->clear();
  }

  
  void clear() {
    struct ht_value * v;
    unsigned long bucket_num;
    struct ht_key * k = ht_first_by_buckets(&ht, &bucket_num, &v)
    while(k != NULL)
    {
      This_Value * val = container_of(v, This_Value, val)
      k = ht_next_by_buckets(k, &bucket_num, &v);
      kill_node(val);
    }
    ht_reinit(&ht);
  }

  bool empty() {
    return ht_empty(&ht) != 0;
  }

  iterator begin() { return iterator(&ht, ht_first(&ht, NULL)); }
  iterator end() { return iterator(&ht); }
};

#endif // __cplusplus

#endif // __BDS_HASH_TABLE_H
