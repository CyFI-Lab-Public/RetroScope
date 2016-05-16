#include <string.h>

#include <marisa.h>

#include "assert.h"

void TestHandle(void) {
  marisa_trie *trie = NULL;

  TEST_START();

  ASSERT(marisa_init(&trie) == MARISA_OK);
  ASSERT(marisa_init(&trie) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_end(trie) == MARISA_OK);
  ASSERT(marisa_end(NULL) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_build(NULL, NULL, 0, NULL, NULL, NULL, 0) ==
      MARISA_HANDLE_ERROR);

  ASSERT(marisa_mmap(NULL, NULL, 0, 0) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_map(NULL, NULL, 0) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_load(NULL, NULL, 0, 0) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_fread(NULL, NULL) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_read(NULL, 0) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_save(NULL, NULL, 0, 0, 0) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_fwrite(NULL, NULL) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_write(NULL, 0) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_restore(NULL, 0, NULL, 0, NULL) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_lookup(NULL, NULL, 0, NULL) == MARISA_HANDLE_ERROR);

  ASSERT(marisa_find(NULL, NULL, 0, NULL, NULL, 0, NULL) ==
      MARISA_HANDLE_ERROR);
  ASSERT(marisa_find_first(NULL, NULL, 0, NULL, NULL) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_find_last(NULL, NULL, 0, NULL, NULL) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_find_callback(NULL, NULL, 0, NULL, NULL) ==
      MARISA_HANDLE_ERROR);

  ASSERT(marisa_predict(NULL, NULL, 0, NULL, 0, NULL) == MARISA_HANDLE_ERROR);
  ASSERT(marisa_predict_breadth_first(NULL, NULL, 0, NULL, 0, NULL) ==
      MARISA_HANDLE_ERROR);
  ASSERT(marisa_predict_depth_first(NULL, NULL, 0, NULL, 0, NULL) ==
      MARISA_HANDLE_ERROR);
  ASSERT(marisa_predict_callback(NULL, NULL, 0, NULL, NULL) ==
      MARISA_HANDLE_ERROR);

  ASSERT(marisa_get_num_tries(NULL) == 0);
  ASSERT(marisa_get_num_keys(NULL) == 0);
  ASSERT(marisa_get_num_nodes(NULL) == 0);
  ASSERT(marisa_get_total_size(NULL) == 0);

  ASSERT(marisa_clear(NULL) == MARISA_HANDLE_ERROR);

  TEST_END();
}

int callback_for_find(void *num_keys,
    marisa_uint32 key_id, size_t key_length) {
  ASSERT(*(size_t *)num_keys == 0);
  ASSERT(key_id == 1);
  ASSERT(key_length == 3);
  ++*(size_t *)num_keys;
  return 1;
}

int callback_for_predict(void *num_keys,
    marisa_uint32 key_id, const char *key, size_t key_length) {
  ASSERT(*(size_t *)num_keys < 2);
  switch (*(size_t *)num_keys) {
    case 0: {
      ASSERT(key_id == 0);
      ASSERT(key_length == 3);
      ASSERT(strcmp(key, "app") == 0);
      break;
    }
    case 1: {
      ASSERT(key_id == 3);
      ASSERT(key_length == 5);
      ASSERT(strcmp(key, "apple") == 0);
      break;
    }
  }
  ++*(size_t *)num_keys;
  return 1;
}

void TestTrie() {
  marisa_trie *trie = NULL;
  const char *keys[8];
  marisa_uint32 key_ids[8];
  size_t i;
  char key_buf[16];
  size_t key_length;
  marisa_uint32 key_id;
  marisa_uint32 found_key_ids[8];
  size_t found_key_lengths[8];
  size_t num_found_keys;

  TEST_START();

  ASSERT(marisa_init(&trie) == MARISA_OK);

  ASSERT(marisa_get_num_tries(trie) == 0);
  ASSERT(marisa_get_num_keys(trie) == 0);
  ASSERT(marisa_get_num_nodes(trie) == 0);
  ASSERT(marisa_get_total_size(trie) == (sizeof(marisa_uint32) * 23));

  ASSERT(marisa_build(trie, NULL, 0, NULL, NULL, NULL, 0) == MARISA_OK);

  ASSERT(marisa_get_num_tries(trie) == 1);
  ASSERT(marisa_get_num_keys(trie) == 0);
  ASSERT(marisa_get_num_nodes(trie) == 1);

  keys[0] = "apple";
  keys[1] = "and";
  keys[2] = "Bad";
  keys[3] = "apple";
  keys[4] = "app";

  ASSERT(marisa_build(trie, keys, 5, NULL, NULL, key_ids,
      1 | MARISA_WITHOUT_TAIL | MARISA_LABEL_ORDER) == MARISA_OK);

  ASSERT(marisa_get_num_tries(trie) == 1);
  ASSERT(marisa_get_num_keys(trie) == 4);
  ASSERT(marisa_get_num_nodes(trie) == 11);

  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 0);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 2);

  for (i = 0; i < marisa_get_num_tries(trie); ++i) {
    ASSERT(marisa_restore(trie,
        key_ids[i], key_buf, sizeof(key_buf), &key_length) == MARISA_OK);
    ASSERT(key_length == strlen(keys[i]));
    ASSERT(strcmp(key_buf, keys[i]) == 0);

    ASSERT(marisa_lookup(trie,
        keys[i], MARISA_ZERO_TERMINATED, &key_id) == MARISA_OK);
    ASSERT(key_id == key_ids[i]);

    ASSERT(marisa_lookup(trie,
        keys[i], strlen(keys[i]), &key_id) == MARISA_OK);
    ASSERT(key_id == key_ids[i]);
  }

  ASSERT(marisa_clear(trie) == MARISA_OK);

  ASSERT(marisa_get_num_tries(trie) == 0);
  ASSERT(marisa_get_num_keys(trie) == 0);
  ASSERT(marisa_get_num_nodes(trie) == 0);
  ASSERT(marisa_get_total_size(trie) == (sizeof(marisa_uint32) * 23));

  ASSERT(marisa_build(trie, keys, 5, NULL, NULL, key_ids,
      1 | MARISA_WITHOUT_TAIL | MARISA_WEIGHT_ORDER) == MARISA_OK);

  ASSERT(marisa_get_num_tries(trie) == 1);
  ASSERT(marisa_get_num_keys(trie) == 4);
  ASSERT(marisa_get_num_nodes(trie) == 11);

  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 2);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 0);

  ASSERT(marisa_find(trie, "ap", MARISA_ZERO_TERMINATED,
      found_key_ids, found_key_lengths, 8, &num_found_keys) == MARISA_OK);
  ASSERT(num_found_keys == 0);

  ASSERT(marisa_find(trie, "applex", MARISA_ZERO_TERMINATED,
      found_key_ids, found_key_lengths, 8, &num_found_keys) == MARISA_OK);
  ASSERT(num_found_keys == 2);
  ASSERT(found_key_ids[0] == key_ids[4]);
  ASSERT(found_key_lengths[0] == 3);
  ASSERT(found_key_ids[1] == key_ids[0]);
  ASSERT(found_key_lengths[1] == 5);

  num_found_keys = 0;
  ASSERT(marisa_find_callback(trie, "anderson", MARISA_ZERO_TERMINATED,
      callback_for_find, &num_found_keys) == MARISA_OK);
  ASSERT(num_found_keys == 1);

  ASSERT(marisa_predict(trie, "a", MARISA_ZERO_TERMINATED,
      found_key_ids, 8, &num_found_keys) == MARISA_OK);
  ASSERT(num_found_keys == 3);
  ASSERT(found_key_ids[0] == key_ids[4]);
  ASSERT(found_key_ids[1] == key_ids[1]);
  ASSERT(found_key_ids[2] == key_ids[0]);

  num_found_keys = 0;
  ASSERT(marisa_predict_callback(trie, "app", MARISA_ZERO_TERMINATED,
      callback_for_predict, &num_found_keys) == MARISA_OK);

  ASSERT(marisa_end(trie) == MARISA_OK);

  TEST_END();
}

int main(void) {
  TestHandle();
  TestTrie();

  return 0;
}
