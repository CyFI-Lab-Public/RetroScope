#include <stdlib.h>
#include <stdio.h>
#include "hash_table.h"
#include "tester.h"

struct my_key {
  int i;
  struct ht_key key;
};

int my_key_compare(struct hash_table *this_hash_table,
                   struct ht_key *key1, struct ht_key *key2)
{
  struct my_key* k1 = container_of(key1, struct my_key, key);
  struct my_key* k2 = container_of(key2, struct my_key, key);
  return k1->i - k2->i;
}

unsigned long key_hash(struct ht_key * key)
{
  struct my_key* k = container_of(key, struct my_key, key);
  return k->i;
}


struct my_value {
  int j;
  struct ht_value val;
};

#define NUM_HT_ENTRIES (10)
test_f(init(struct hash_table* ht))
{
  void* ents = malloc(ht_bytes_needed_for_entries(NUM_HT_ENTRIES));

  ht_init(ht, key_hash, my_key_compare, ents, NUM_HT_ENTRIES);
  test(ht->key_compare == my_key_compare);
  test(ht->roots == ents);
  test(ht->num_buckets == NUM_HT_ENTRIES);
  test(ht->hash_func == key_hash);
}

#define MAX 65500
test_f(add(struct hash_table *ht))
{
  struct my_key * key;
  struct my_value * val;
  unsigned int x;

  for(x = 0; x < MAX; x++)
  {
    key = (struct my_key *)malloc(sizeof(struct my_key));
    val = (struct my_value *)malloc(sizeof(struct my_value));
    key->i = x;
    val->j = x;
    ht_insert(ht, &(key->key), &(val->val));
    test(ht_lookup(ht, &(key->key)) == &(val->val));
    test(ht_lookup_entry(ht, &(key->key), struct my_value, val)->j == x);
  }
}

test_f(lookup(struct hash_table *ht))
{
  struct my_key key;
  unsigned int x;
  for(x = 0; x < MAX; x+=2)
  {
    key.i = x;
    test(ht_lookup_entry(ht, &(key.key), struct my_value, val)->j == x);
  }

  for(x = 1; x < MAX; x+=2)
  {
    key.i = x;
    test(ht_lookup_entry(ht, &(key.key), struct my_value, val)->j == x);
  }

  for(x = MAX ; x < MAX+10; x++)
  {
    key.i = x;
    test(ht_lookup_entry(ht, &(key.key), struct my_value, val) == NULL);
  }
}

test_f(remove(struct hash_table *ht))
{
  struct my_key key;
  unsigned int x;
  for(x = 0; x < MAX; x++)
  {
    key.i = x;
    test(ht_remove_entry(ht, &(key.key), struct my_value, val)->j == x);
    test(ht_remove(ht, &(key.key)) == NULL);
  }
  for(x = 0 ; x < MAX+10; x++)
  {
    key.i = x;
    test(ht_lookup(ht, &(key.key)) == NULL);
  }
}

int main ()
{
  struct hash_table ht;
  call_test(init(&ht));
  call_test(add(&ht));
  call_test(lookup(&ht));
  call_test(remove(&ht));
  return 0;
}
