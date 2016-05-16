#include <string.h>

#include <marisa_alpha.h>

#include "assert.h"

void TestHandle(void) {
  marisa_alpha_trie *trie = NULL;

  TEST_START();

  ASSERT(marisa_alpha_init(&trie) == MARISA_ALPHA_OK);
  ASSERT(marisa_alpha_init(&trie) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_end(trie) == MARISA_ALPHA_OK);
  ASSERT(marisa_alpha_end(NULL) == MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_build(NULL, NULL, 0, NULL, NULL, NULL, 0) ==
      MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_mmap(NULL, NULL, 0, 0) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_map(NULL, NULL, 0) == MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_load(NULL, NULL, 0, 0) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_fread(NULL, NULL) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_read(NULL, 0) == MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_save(NULL, NULL, 0, 0, 0) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_fwrite(NULL, NULL) == MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_write(NULL, 0) == MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_restore(NULL, 0, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_lookup(NULL, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_find(NULL, NULL, 0, NULL, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_find_first(NULL, NULL, 0, NULL, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_find_last(NULL, NULL, 0, NULL, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_find_callback(NULL, NULL, 0, NULL, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_predict(NULL, NULL, 0, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_predict_breadth_first(NULL, NULL, 0, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_predict_depth_first(NULL, NULL, 0, NULL, 0, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);
  ASSERT(marisa_alpha_predict_callback(NULL, NULL, 0, NULL, NULL) ==
      MARISA_ALPHA_HANDLE_ERROR);

  ASSERT(marisa_alpha_get_num_tries(NULL) == 0);
  ASSERT(marisa_alpha_get_num_keys(NULL) == 0);
  ASSERT(marisa_alpha_get_num_nodes(NULL) == 0);
  ASSERT(marisa_alpha_get_total_size(NULL) == 0);

  ASSERT(marisa_alpha_clear(NULL) == MARISA_ALPHA_HANDLE_ERROR);

  TEST_END();
}

int callback_for_find(void *num_keys,
    marisa_alpha_uint32 key_id, size_t key_length) {
  ASSERT(*(size_t *)num_keys == 0);
  ASSERT(key_id == 1);
  ASSERT(key_length == 3);
  ++*(size_t *)num_keys;
  return 1;
}

int callback_for_predict(void *num_keys,
    marisa_alpha_uint32 key_id, const char *key, size_t key_length) {
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
  marisa_alpha_trie *trie = NULL;
  const char *keys[8];
  marisa_alpha_uint32 key_ids[8];
  size_t i;
  char key_buf[16];
  size_t key_length;
  marisa_alpha_uint32 key_id;
  marisa_alpha_uint32 found_key_ids[8];
  size_t found_key_lengths[8];
  size_t num_found_keys;

  TEST_START();

  ASSERT(marisa_alpha_init(&trie) == MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_get_num_tries(trie) == 0);
  ASSERT(marisa_alpha_get_num_keys(trie) == 0);
  ASSERT(marisa_alpha_get_num_nodes(trie) == 0);
  ASSERT(marisa_alpha_get_total_size(trie) ==
      (sizeof(marisa_alpha_uint32) * 23));

  ASSERT(marisa_alpha_build(trie, NULL, 0, NULL, NULL, NULL, 0) ==
      MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_get_num_tries(trie) == 1);
  ASSERT(marisa_alpha_get_num_keys(trie) == 0);
  ASSERT(marisa_alpha_get_num_nodes(trie) == 1);

  keys[0] = "apple";
  keys[1] = "and";
  keys[2] = "Bad";
  keys[3] = "apple";
  keys[4] = "app";

  ASSERT(marisa_alpha_build(trie, keys, 5, NULL, NULL, key_ids,
      1 | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_LABEL_ORDER) ==
      MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_get_num_tries(trie) == 1);
  ASSERT(marisa_alpha_get_num_keys(trie) == 4);
  ASSERT(marisa_alpha_get_num_nodes(trie) == 11);

  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 0);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 2);

  for (i = 0; i < marisa_alpha_get_num_tries(trie); ++i) {
    ASSERT(marisa_alpha_restore(trie,
        key_ids[i], key_buf, sizeof(key_buf), &key_length) == MARISA_ALPHA_OK);
    ASSERT(key_length == strlen(keys[i]));
    ASSERT(strcmp(key_buf, keys[i]) == 0);

    ASSERT(marisa_alpha_lookup(trie,
        keys[i], MARISA_ALPHA_ZERO_TERMINATED, &key_id) == MARISA_ALPHA_OK);
    ASSERT(key_id == key_ids[i]);

    ASSERT(marisa_alpha_lookup(trie,
        keys[i], strlen(keys[i]), &key_id) == MARISA_ALPHA_OK);
    ASSERT(key_id == key_ids[i]);
  }

  ASSERT(marisa_alpha_clear(trie) == MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_get_num_tries(trie) == 0);
  ASSERT(marisa_alpha_get_num_keys(trie) == 0);
  ASSERT(marisa_alpha_get_num_nodes(trie) == 0);
  ASSERT(marisa_alpha_get_total_size(trie) ==
      (sizeof(marisa_alpha_uint32) * 23));

  ASSERT(marisa_alpha_build(trie, keys, 5, NULL, NULL, key_ids,
      1 | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_WEIGHT_ORDER) ==
      MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_get_num_tries(trie) == 1);
  ASSERT(marisa_alpha_get_num_keys(trie) == 4);
  ASSERT(marisa_alpha_get_num_nodes(trie) == 11);

  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 2);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 0);

  ASSERT(marisa_alpha_find(trie, "ap", MARISA_ALPHA_ZERO_TERMINATED,
      found_key_ids, found_key_lengths, 8, &num_found_keys) ==
      MARISA_ALPHA_OK);
  ASSERT(num_found_keys == 0);

  ASSERT(marisa_alpha_find(trie, "applex", MARISA_ALPHA_ZERO_TERMINATED,
      found_key_ids, found_key_lengths, 8, &num_found_keys) ==
      MARISA_ALPHA_OK);
  ASSERT(num_found_keys == 2);
  ASSERT(found_key_ids[0] == key_ids[4]);
  ASSERT(found_key_lengths[0] == 3);
  ASSERT(found_key_ids[1] == key_ids[0]);
  ASSERT(found_key_lengths[1] == 5);

  num_found_keys = 0;
  ASSERT(marisa_alpha_find_callback(trie, "anderson",
      MARISA_ALPHA_ZERO_TERMINATED,
      callback_for_find, &num_found_keys) == MARISA_ALPHA_OK);
  ASSERT(num_found_keys == 1);

  ASSERT(marisa_alpha_predict(trie, "a", MARISA_ALPHA_ZERO_TERMINATED,
      found_key_ids, 8, &num_found_keys) == MARISA_ALPHA_OK);
  ASSERT(num_found_keys == 3);
  ASSERT(found_key_ids[0] == key_ids[4]);
  ASSERT(found_key_ids[1] == key_ids[1]);
  ASSERT(found_key_ids[2] == key_ids[0]);

  num_found_keys = 0;
  ASSERT(marisa_alpha_predict_callback(trie, "app",
      MARISA_ALPHA_ZERO_TERMINATED,
      callback_for_predict, &num_found_keys) == MARISA_ALPHA_OK);

  ASSERT(marisa_alpha_end(trie) == MARISA_ALPHA_OK);

  TEST_END();
}

int main(void) {
  TestHandle();
  TestTrie();

  return 0;
}
