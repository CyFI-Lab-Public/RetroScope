#include <sstream>

#include <marisa_alpha.h>

#include "assert.h"

namespace {

class FindCallback {
 public:
  FindCallback(std::vector<marisa_alpha::UInt32> *key_ids,
      std::vector<std::size_t> *key_lengths)
      : key_ids_(key_ids), key_lengths_(key_lengths) {}
  FindCallback(const FindCallback &callback)
      : key_ids_(callback.key_ids_), key_lengths_(callback.key_lengths_) {}

  bool operator()(marisa_alpha::UInt32 key_id, std::size_t key_length) const {
    key_ids_->push_back(key_id);
    key_lengths_->push_back(key_length);
    return true;
  }

 private:
  std::vector<marisa_alpha::UInt32> *key_ids_;
  std::vector<std::size_t> *key_lengths_;

  // Disallows assignment.
  FindCallback &operator=(const FindCallback &);
};

class PredictCallback {
 public:
  PredictCallback(std::vector<marisa_alpha::UInt32> *key_ids,
      std::vector<std::string> *keys)
      : key_ids_(key_ids), keys_(keys) {}
  PredictCallback(const PredictCallback &callback)
      : key_ids_(callback.key_ids_), keys_(callback.keys_) {}

  bool operator()(marisa_alpha::UInt32 key_id, const std::string &key) const {
    key_ids_->push_back(key_id);
    keys_->push_back(key);
    return true;
  }

 private:
  std::vector<marisa_alpha::UInt32> *key_ids_;
  std::vector<std::string> *keys_;

  // Disallows assignment.
  PredictCallback &operator=(const PredictCallback &);
};

void TestTrie() {
  TEST_START();

  marisa_alpha::Trie trie;

  ASSERT(trie.num_tries() == 0);
  ASSERT(trie.num_keys() == 0);
  ASSERT(trie.num_nodes() == 0);
  ASSERT(trie.total_size() == (sizeof(marisa_alpha::UInt32) * 23));

  std::vector<std::string> keys;
  trie.build(keys);
  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 0);
  ASSERT(trie.num_nodes() == 1);

  keys.push_back("apple");
  keys.push_back("and");
  keys.push_back("Bad");
  keys.push_back("apple");
  keys.push_back("app");

  std::vector<marisa_alpha::UInt32> key_ids;
  trie.build(keys, &key_ids,
      1 | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_LABEL_ORDER);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 11);

  ASSERT(key_ids.size() == 5);
  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 0);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 2);

  char key_buf[256];
  std::size_t key_length;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.clear();

  ASSERT(trie.num_tries() == 0);
  ASSERT(trie.num_keys() == 0);
  ASSERT(trie.num_nodes() == 0);
  ASSERT(trie.total_size() == (sizeof(marisa_alpha::UInt32) * 23));

  trie.build(keys, &key_ids,
      1 | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_WEIGHT_ORDER);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 11);

  ASSERT(key_ids.size() == 5);
  ASSERT(key_ids[0] == 3);
  ASSERT(key_ids[1] == 1);
  ASSERT(key_ids[2] == 2);
  ASSERT(key_ids[3] == 3);
  ASSERT(key_ids[4] == 0);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
  }

  ASSERT(trie["appl"] == trie.notfound());
  ASSERT(trie["applex"] == trie.notfound());
  ASSERT(trie.find_first("ap") == trie.notfound());
  ASSERT(trie.find_first("applex") == trie["app"]);
  ASSERT(trie.find_last("ap") == trie.notfound());
  ASSERT(trie.find_last("applex") == trie["apple"]);

  std::vector<marisa_alpha::UInt32> ids;
  ASSERT(trie.find("ap", &ids) == 0);
  ASSERT(trie.find("applex", &ids) == 2);
  ASSERT(ids.size() == 2);
  ASSERT(ids[0] == trie["app"]);
  ASSERT(ids[1] == trie["apple"]);

  std::vector<std::size_t> lengths;
  ASSERT(trie.find("Baddie", &ids, &lengths) == 1);
  ASSERT(ids.size() == 3);
  ASSERT(ids[2] == trie["Bad"]);
  ASSERT(lengths.size() == 1);
  ASSERT(lengths[0] == 3);

  ASSERT(trie.find_callback("anderson", FindCallback(&ids, &lengths)) == 1);
  ASSERT(ids.size() == 4);
  ASSERT(ids[3] == trie["and"]);
  ASSERT(lengths.size() == 2);
  ASSERT(lengths[1] == 3);

  ASSERT(trie.predict("") == 4);
  ASSERT(trie.predict("a") == 3);
  ASSERT(trie.predict("ap") == 2);
  ASSERT(trie.predict("app") == 2);
  ASSERT(trie.predict("appl") == 1);
  ASSERT(trie.predict("apple") == 1);
  ASSERT(trie.predict("appleX") == 0);
  ASSERT(trie.predict("X") == 0);

  ids.clear();
  ASSERT(trie.predict("a", &ids) == 3);
  ASSERT(ids.size() == 3);
  ASSERT(ids[0] == trie["app"]);
  ASSERT(ids[1] == trie["and"]);
  ASSERT(ids[2] == trie["apple"]);

  std::vector<std::string> strs;
  ASSERT(trie.predict("a", &ids, &strs) == 3);
  ASSERT(ids.size() == 6);
  ASSERT(ids[3] == trie["app"]);
  ASSERT(ids[4] == trie["apple"]);
  ASSERT(ids[5] == trie["and"]);
  ASSERT(strs[0] == "app");
  ASSERT(strs[1] == "apple");
  ASSERT(strs[2] == "and");

  TEST_END();
}

void TestPrefixTrie() {
  TEST_START();

  std::vector<std::string> keys;
  keys.push_back("after");
  keys.push_back("bar");
  keys.push_back("car");
  keys.push_back("caster");

  marisa_alpha::Trie trie;
  std::vector<marisa_alpha::UInt32> key_ids;
  trie.build(keys, &key_ids, 1 | MARISA_ALPHA_PREFIX_TRIE
      | MARISA_ALPHA_TEXT_TAIL | MARISA_ALPHA_LABEL_ORDER);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 7);

  char key_buf[256];
  std::size_t key_length;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  key_length = trie.restore(key_ids[0], NULL, 0);

  ASSERT(key_length == keys[0].length());
  EXCEPT(trie.restore(key_ids[0], NULL, 5), MARISA_ALPHA_PARAM_ERROR);

  key_length = trie.restore(key_ids[0], key_buf, 5);

  ASSERT(key_length == keys[0].length());

  key_length = trie.restore(key_ids[0], key_buf, 6);

  ASSERT(key_length == keys[0].length());

  trie.build(keys, &key_ids, 2 | MARISA_ALPHA_PREFIX_TRIE
      | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_WEIGHT_ORDER);

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 16);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  key_length = trie.restore(key_ids[0], NULL, 0);

  ASSERT(key_length == keys[0].length());
  EXCEPT(trie.restore(key_ids[0], NULL, 5), MARISA_ALPHA_PARAM_ERROR);

  key_length = trie.restore(key_ids[0], key_buf, 5);

  ASSERT(key_length == keys[0].length());

  key_length = trie.restore(key_ids[0], key_buf, 6);

  ASSERT(key_length == keys[0].length());

  trie.build(keys, &key_ids, 2 | MARISA_ALPHA_PREFIX_TRIE
      | MARISA_ALPHA_TEXT_TAIL | MARISA_ALPHA_LABEL_ORDER);

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 14);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.save("trie-test.dat");
  trie.clear();
  marisa_alpha::Mapper mapper;
  trie.mmap(&mapper, "trie-test.dat");

  ASSERT(mapper.is_open());
  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 14);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  std::stringstream stream;
  trie.write(stream);
  trie.clear();
  trie.read(stream);

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 14);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.build(keys, &key_ids, 3 | MARISA_ALPHA_PREFIX_TRIE
      | MARISA_ALPHA_WITHOUT_TAIL | MARISA_ALPHA_WEIGHT_ORDER);

  ASSERT(trie.num_tries() == 3);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 19);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  ASSERT(trie["ca"] == trie.notfound());
  ASSERT(trie["card"] == trie.notfound());

  std::size_t length = 0;
  ASSERT(trie.find_first("ca") == trie.notfound());
  ASSERT(trie.find_first("car") == trie["car"]);
  ASSERT(trie.find_first("card", &length) == trie["car"]);
  ASSERT(length == 3);

  ASSERT(trie.find_last("afte") == trie.notfound());
  ASSERT(trie.find_last("after") == trie["after"]);
  ASSERT(trie.find_last("afternoon", &length) == trie["after"]);
  ASSERT(length == 5);

  {
    std::vector<marisa_alpha::UInt32> ids;
    std::vector<std::size_t> lengths;
    ASSERT(trie.find("card", &ids, &lengths) == 1);
    ASSERT(ids.size() == 1);
    ASSERT(ids[0] == trie["car"]);
    ASSERT(lengths.size() == 1);
    ASSERT(lengths[0] == 3);

    ASSERT(trie.predict("ca", &ids) == 2);
    ASSERT(ids.size() == 3);
    ASSERT(ids[1] == trie["car"]);
    ASSERT(ids[2] == trie["caster"]);

    ASSERT(trie.predict("ca", &ids, NULL, 1) == 1);
    ASSERT(ids.size() == 4);
    ASSERT(ids[3] == trie["car"]);

    std::vector<std::string> strs;
    ASSERT(trie.predict("ca", &ids, &strs, 1) == 1);
    ASSERT(ids.size() == 5);
    ASSERT(ids[4] == trie["car"]);
    ASSERT(strs.size() == 1);
    ASSERT(strs[0] == "car");

    ASSERT(trie.predict_callback("", PredictCallback(&ids, &strs)) == 4);
    ASSERT(ids.size() == 9);
    ASSERT(ids[5] == trie["car"]);
    ASSERT(ids[6] == trie["caster"]);
    ASSERT(ids[7] == trie["after"]);
    ASSERT(ids[8] == trie["bar"]);
    ASSERT(strs.size() == 5);
    ASSERT(strs[1] == "car");
    ASSERT(strs[2] == "caster");
    ASSERT(strs[3] == "after");
    ASSERT(strs[4] == "bar");
  }

  {
    marisa_alpha::UInt32 ids[10];
    std::size_t lengths[10];
    ASSERT(trie.find("card", ids, lengths, 10) == 1);
    ASSERT(ids[0] == trie["car"]);
    ASSERT(lengths[0] == 3);

    ASSERT(trie.predict("ca", ids, NULL, 10) == 2);
    ASSERT(ids[0] == trie["car"]);
    ASSERT(ids[1] == trie["caster"]);

    ASSERT(trie.predict("ca", ids, NULL, 1) == 1);
    ASSERT(ids[0] == trie["car"]);

    std::string strs[10];
    ASSERT(trie.predict("ca", ids, strs, 1) == 1);
    ASSERT(ids[0] == trie["car"]);
    ASSERT(strs[0] == "car");

    ASSERT(trie.predict("", ids, strs, 10) == 4);
    ASSERT(ids[0] == trie["car"]);
    ASSERT(ids[1] == trie["caster"]);
    ASSERT(ids[2] == trie["after"]);
    ASSERT(ids[3] == trie["bar"]);
    ASSERT(strs[0] == "car");
    ASSERT(strs[1] == "caster");
    ASSERT(strs[2] == "after");
    ASSERT(strs[3] == "bar");
  }

  std::string trie_data = stream.str();
  trie.map(trie_data.c_str(), trie_data.length());

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 14);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  TEST_END();
}

void TestPatriciaTrie() {
  TEST_START();

  std::vector<std::string> keys;
  keys.push_back("bach");
  keys.push_back("bet");
  keys.push_back("chat");
  keys.push_back("check");
  keys.push_back("check");

  marisa_alpha::Trie trie;
  std::vector<marisa_alpha::UInt32> key_ids;
  trie.build(keys, &key_ids, 1);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 7);

  ASSERT(key_ids.size() == 5);
  ASSERT(key_ids[0] == 2);
  ASSERT(key_ids[1] == 3);
  ASSERT(key_ids[2] == 1);
  ASSERT(key_ids[3] == 0);
  ASSERT(key_ids[4] == 0);

  char key_buf[256];
  std::size_t key_length;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.build(keys, &key_ids, 2 | MARISA_ALPHA_WITHOUT_TAIL);

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 17);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.build(keys, &key_ids, 2);

  ASSERT(trie.num_tries() == 2);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 14);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  trie.build(keys, &key_ids, 3 | MARISA_ALPHA_WITHOUT_TAIL);

  ASSERT(trie.num_tries() == 3);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 20);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  std::stringstream stream;
  trie.write(stream);
  trie.clear();
  trie.read(stream);

  ASSERT(trie.num_tries() == 3);
  ASSERT(trie.num_keys() == 4);
  ASSERT(trie.num_nodes() == 20);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    key_length = trie.restore(key_ids[i], key_buf, sizeof(key_buf));

    ASSERT(trie[keys[i]] == key_ids[i]);
    ASSERT(trie[key_ids[i]] == keys[i]);
    ASSERT(key_length == keys[i].length());
    ASSERT(keys[i] == key_buf);
  }

  TEST_END();
}

void TestEmptyString() {
  TEST_START();

  std::vector<std::string> keys;
  keys.push_back("");

  marisa_alpha::Trie trie;
  std::vector<marisa_alpha::UInt32> key_ids;
  trie.build(keys, &key_ids);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 1);
  ASSERT(trie.num_nodes() == 1);

  ASSERT(key_ids.size() == 1);
  ASSERT(key_ids[0] == 0);

  ASSERT(trie[""] == 0);
  ASSERT(trie[(marisa_alpha::UInt32)0] == "");

  ASSERT(trie["x"] == trie.notfound());
  ASSERT(trie.find_first("") == 0);
  ASSERT(trie.find_first("x") == 0);
  ASSERT(trie.find_last("") == 0);
  ASSERT(trie.find_last("x") == 0);

  std::vector<marisa_alpha::UInt32> ids;
  ASSERT(trie.find("xyz", &ids) == 1);
  ASSERT(ids.size() == 1);
  ASSERT(ids[0] == trie[""]);

  std::vector<std::size_t> lengths;
  ASSERT(trie.find("xyz", &ids, &lengths) == 1);
  ASSERT(ids.size() == 2);
  ASSERT(ids[0] == trie[""]);
  ASSERT(ids[1] == trie[""]);
  ASSERT(lengths.size() == 1);
  ASSERT(lengths[0] == 0);

  ASSERT(trie.find_callback("xyz", FindCallback(&ids, &lengths)) == 1);
  ASSERT(ids.size() == 3);
  ASSERT(ids[2] == trie[""]);
  ASSERT(lengths.size() == 2);
  ASSERT(lengths[1] == 0);

  ASSERT(trie.predict("xyz", &ids) == 0);

  ASSERT(trie.predict("", &ids) == 1);
  ASSERT(ids.size() == 4);
  ASSERT(ids[3] == trie[""]);

  std::vector<std::string> strs;
  ASSERT(trie.predict("", &ids, &strs) == 1);
  ASSERT(ids.size() == 5);
  ASSERT(ids[4] == trie[""]);
  ASSERT(strs[0] == "");

  TEST_END();
}

void TestBinaryKey() {
  TEST_START();

  std::string binary_key = "NP";
  binary_key += '\0';
  binary_key += "Trie";

  std::vector<std::string> keys;
  keys.push_back(binary_key);

  marisa_alpha::Trie trie;
  std::vector<marisa_alpha::UInt32> key_ids;
  trie.build(keys, &key_ids, 1 | MARISA_ALPHA_WITHOUT_TAIL);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 1);
  ASSERT(trie.num_nodes() == 8);
  ASSERT(key_ids.size() == 1);

  char key_buf[256];
  std::size_t key_length;
  key_length = trie.restore(0, key_buf, sizeof(key_buf));

  ASSERT(trie[keys[0]] == key_ids[0]);
  ASSERT(trie[key_ids[0]] == keys[0]);
  ASSERT(std::string(key_buf, key_length) == keys[0]);

  trie.build(keys, &key_ids,
      1 | MARISA_ALPHA_PREFIX_TRIE | MARISA_ALPHA_BINARY_TAIL);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 1);
  ASSERT(trie.num_nodes() == 2);
  ASSERT(key_ids.size() == 1);

  key_length = trie.restore(0, key_buf, sizeof(key_buf));

  ASSERT(trie[keys[0]] == key_ids[0]);
  ASSERT(trie[key_ids[0]] == keys[0]);
  ASSERT(std::string(key_buf, key_length) == keys[0]);

  trie.build(keys, &key_ids,
      1 | MARISA_ALPHA_PREFIX_TRIE | MARISA_ALPHA_TEXT_TAIL);

  ASSERT(trie.num_tries() == 1);
  ASSERT(trie.num_keys() == 1);
  ASSERT(trie.num_nodes() == 2);
  ASSERT(key_ids.size() == 1);

  key_length = trie.restore(0, key_buf, sizeof(key_buf));

  ASSERT(trie[keys[0]] == key_ids[0]);
  ASSERT(trie[key_ids[0]] == keys[0]);
  ASSERT(std::string(key_buf, key_length) == keys[0]);

  std::vector<marisa_alpha::UInt32> ids;
  ASSERT(trie.predict_breadth_first("", &ids) == 1);
  ASSERT(ids.size() == 1);
  ASSERT(ids[0] == key_ids[0]);

  std::vector<std::string> strs;
  ASSERT(trie.predict_depth_first("NP", &ids, &strs) == 1);
  ASSERT(ids.size() == 2);
  ASSERT(ids[1] == key_ids[0]);
  ASSERT(strs[0] == keys[0]);

  TEST_END();
}

}  // namespace

int main() {
  TestTrie();
  TestPrefixTrie();
  TestPatriciaTrie();
  TestEmptyString();
  TestBinaryKey();

  return 0;
}
