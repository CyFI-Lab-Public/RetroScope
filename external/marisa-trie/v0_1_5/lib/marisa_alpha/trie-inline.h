#ifndef MARISA_ALPHA_TRIE_INLINE_H_
#define MARISA_ALPHA_TRIE_INLINE_H_

#include <stdexcept>

#include "cell.h"

namespace marisa_alpha {

inline std::string Trie::operator[](UInt32 key_id) const {
  std::string key;
  restore(key_id, &key);
  return key;
}

inline UInt32 Trie::operator[](const char *str) const {
  return lookup(str);
}

inline UInt32 Trie::operator[](const std::string &str) const {
  return lookup(str);
}

inline UInt32 Trie::lookup(const std::string &str) const {
  return lookup(str.c_str(), str.length());
}

inline std::size_t Trie::find(const std::string &str,
    UInt32 *key_ids, std::size_t *key_lengths,
    std::size_t max_num_results) const {
  return find(str.c_str(), str.length(),
      key_ids, key_lengths, max_num_results);
}

inline std::size_t Trie::find(const std::string &str,
    std::vector<UInt32> *key_ids, std::vector<std::size_t> *key_lengths,
    std::size_t max_num_results) const {
  return find(str.c_str(), str.length(),
      key_ids, key_lengths, max_num_results);
}

inline UInt32 Trie::find_first(const std::string &str,
    std::size_t *key_length) const {
  return find_first(str.c_str(), str.length(), key_length);
}

inline UInt32 Trie::find_last(const std::string &str,
    std::size_t *key_length) const {
  return find_last(str.c_str(), str.length(), key_length);
}

template <typename T>
inline std::size_t Trie::find_callback(const char *str,
    T callback) const {
  MARISA_ALPHA_THROW_IF(empty(), MARISA_ALPHA_STATE_ERROR);
  MARISA_ALPHA_THROW_IF(str == NULL, MARISA_ALPHA_PARAM_ERROR);
  return find_callback_<CQuery>(CQuery(str), callback);
}

template <typename T>
inline std::size_t Trie::find_callback(const char *ptr, std::size_t length,
    T callback) const {
  MARISA_ALPHA_THROW_IF(empty(), MARISA_ALPHA_STATE_ERROR);
  MARISA_ALPHA_THROW_IF((ptr == NULL) && (length != 0),
      MARISA_ALPHA_PARAM_ERROR);
  return find_callback_<const Query &>(Query(ptr, length), callback);
}

template <typename T>
inline std::size_t Trie::find_callback(const std::string &str,
    T callback) const {
  return find_callback(str.c_str(), str.length(), callback);
}

inline std::size_t Trie::predict(const std::string &str,
    UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const {
  return predict(str.c_str(), str.length(), key_ids, keys, max_num_results);
}

inline std::size_t Trie::predict(const std::string &str,
    std::vector<UInt32> *key_ids, std::vector<std::string> *keys,
    std::size_t max_num_results) const {
  return predict(str.c_str(), str.length(), key_ids, keys, max_num_results);
}

inline std::size_t Trie::predict_breadth_first(const std::string &str,
    UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const {
  return predict_breadth_first(str.c_str(), str.length(),
      key_ids, keys, max_num_results);
}

inline std::size_t Trie::predict_breadth_first(const std::string &str,
    std::vector<UInt32> *key_ids, std::vector<std::string> *keys,
    std::size_t max_num_results) const {
  return predict_breadth_first(str.c_str(), str.length(),
      key_ids, keys, max_num_results);
}

inline std::size_t Trie::predict_depth_first(const std::string &str,
    UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const {
  return predict_depth_first(str.c_str(), str.length(),
      key_ids, keys, max_num_results);
}

inline std::size_t Trie::predict_depth_first(const std::string &str,
    std::vector<UInt32> *key_ids, std::vector<std::string> *keys,
    std::size_t max_num_results) const {
  return predict_depth_first(str.c_str(), str.length(),
      key_ids, keys, max_num_results);
}

template <typename T>
inline std::size_t Trie::predict_callback(
    const char *str, T callback) const {
  return predict_callback_<CQuery>(CQuery(str), callback);
}

template <typename T>
inline std::size_t Trie::predict_callback(
    const char *ptr, std::size_t length,
    T callback) const {
  return predict_callback_<const Query &>(Query(ptr, length), callback);
}

template <typename T>
inline std::size_t Trie::predict_callback(
    const std::string &str, T callback) const {
  return predict_callback(str.c_str(), str.length(), callback);
}

inline bool Trie::empty() const {
  return louds_.empty();
}

inline std::size_t Trie::num_keys() const {
  return num_keys_;
}

inline UInt32 Trie::notfound() {
  return MARISA_ALPHA_NOT_FOUND;
}

inline std::size_t Trie::mismatch() {
  return MARISA_ALPHA_MISMATCH;
}

template <typename T>
inline bool Trie::find_child(UInt32 &node, T query,
    std::size_t &pos) const {
  UInt32 louds_pos = get_child(node);
  if (!louds_[louds_pos]) {
    return false;
  }
  node = louds_pos_to_node(louds_pos, node);
  UInt32 link_id = MARISA_ALPHA_UINT32_MAX;
  do {
    if (has_link(node)) {
      if (link_id == MARISA_ALPHA_UINT32_MAX) {
        link_id = get_link_id(node);
      } else {
        ++link_id;
      }
      std::size_t next_pos = has_trie() ?
          trie_->trie_match<T>(get_link(node, link_id), query, pos) :
          tail_match<T>(node, link_id, query, pos);
      if (next_pos == mismatch()) {
        return false;
      } else if (next_pos != pos) {
        pos = next_pos;
        return true;
      }
    } else if (labels_[node] == query[pos]) {
      ++pos;
      return true;
    }
    ++node;
    ++louds_pos;
  } while (louds_[louds_pos]);
  return false;
}

template <typename T, typename U>
std::size_t Trie::find_callback_(T query, U callback) const try {
  std::size_t count = 0;
  UInt32 node = 0;
  std::size_t pos = 0;
  do {
    if (terminal_flags_[node]) {
      ++count;
      if (!callback(node_to_key_id(node), pos)) {
        return count;
      }
    }
  } while (!query.ends_at(pos) && find_child<T>(node, query, pos));
  return count;
} catch (const std::bad_alloc &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_MEMORY_ERROR);
} catch (const std::length_error &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_SIZE_ERROR);
}

template <typename T>
inline bool Trie::predict_child(UInt32 &node, T query, std::size_t &pos,
    std::string *key) const {
  UInt32 louds_pos = get_child(node);
  if (!louds_[louds_pos]) {
    return false;
  }
  node = louds_pos_to_node(louds_pos, node);
  UInt32 link_id = MARISA_ALPHA_UINT32_MAX;
  do {
    if (has_link(node)) {
      if (link_id == MARISA_ALPHA_UINT32_MAX) {
        link_id = get_link_id(node);
      } else {
        ++link_id;
      }
      std::size_t next_pos = has_trie() ?
          trie_->trie_prefix_match<T>(
              get_link(node, link_id), query, pos, key) :
          tail_prefix_match<T>(node, link_id, query, pos, key);
      if (next_pos == mismatch()) {
        return false;
      } else if (next_pos != pos) {
        pos = next_pos;
        return true;
      }
    } else if (labels_[node] == query[pos]) {
      ++pos;
      return true;
    }
    ++node;
    ++louds_pos;
  } while (louds_[louds_pos]);
  return false;
}

template <typename T, typename U>
std::size_t Trie::predict_callback_(T query, U callback) const try {
  std::string key;
  UInt32 node = 0;
  std::size_t pos = 0;
  while (!query.ends_at(pos)) {
    if (!predict_child<T>(node, query, pos, &key)) {
      return 0;
    }
  }
  query.insert(&key);
  std::size_t count = 0;
  if (terminal_flags_[node]) {
    ++count;
    if (!callback(node_to_key_id(node), key)) {
      return count;
    }
  }
  Cell cell;
  cell.set_louds_pos(get_child(node));
  if (!louds_[cell.louds_pos()]) {
    return count;
  }
  cell.set_node(louds_pos_to_node(cell.louds_pos(), node));
  cell.set_key_id(node_to_key_id(cell.node()));
  cell.set_length(key.length());
  Vector<Cell> stack;
  stack.push_back(cell);
  std::size_t stack_pos = 1;
  while (stack_pos != 0) {
    Cell &cur = stack[stack_pos - 1];
    if (!louds_[cur.louds_pos()]) {
      cur.set_louds_pos(cur.louds_pos() + 1);
      --stack_pos;
      continue;
    }
    cur.set_louds_pos(cur.louds_pos() + 1);
    key.resize(cur.length());
    if (has_link(cur.node())) {
      if (has_trie()) {
        trie_->trie_restore(get_link(cur.node()), &key);
      } else {
        tail_restore(cur.node(), &key);
      }
    } else {
      key += labels_[cur.node()];
    }
    if (terminal_flags_[cur.node()]) {
      ++count;
      if (!callback(cur.key_id(), key)) {
        return count;
      }
      cur.set_key_id(cur.key_id() + 1);
    }
    if (stack_pos == stack.size()) {
      cell.set_louds_pos(get_child(cur.node()));
      cell.set_node(louds_pos_to_node(cell.louds_pos(), cur.node()));
      cell.set_key_id(node_to_key_id(cell.node()));
      stack.push_back(cell);
    }
    stack[stack_pos].set_length(key.length());
    stack[stack_pos - 1].set_node(stack[stack_pos - 1].node() + 1);
    ++stack_pos;
  }
  return count;
} catch (const std::bad_alloc &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_MEMORY_ERROR);
} catch (const std::length_error &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_SIZE_ERROR);
}

inline UInt32 Trie::key_id_to_node(UInt32 key_id) const {
  return terminal_flags_.select1(key_id);
}

inline UInt32 Trie::node_to_key_id(UInt32 node) const {
  return terminal_flags_.rank1(node);
}

inline UInt32 Trie::louds_pos_to_node(UInt32 louds_pos,
    UInt32 parent_node) const {
  return louds_pos - parent_node - 1;
}

inline UInt32 Trie::get_child(UInt32 node) const {
  return louds_.select0(node) + 1;
}

inline UInt32 Trie::get_parent(UInt32 node) const {
  return (node > num_first_branches_) ? (louds_.select1(node) - node - 1) : 0;
}

inline bool Trie::has_link(UInt32 node) const {
  return (link_flags_.empty()) ? false : link_flags_[node];
}

inline UInt32 Trie::get_link_id(UInt32 node) const {
  return link_flags_.rank1(node);
}

inline UInt32 Trie::get_link(UInt32 node) const {
  return get_link(node, get_link_id(node));
}

inline UInt32 Trie::get_link(UInt32 node, UInt32 link_id) const {
  return (links_[link_id] * 256) + labels_[node];
}

inline bool Trie::has_link() const {
  return !link_flags_.empty();
}

inline bool Trie::has_trie() const {
  return trie_.get() != NULL;
}

inline bool Trie::has_tail() const {
  return !tail_.empty();
}

}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_TRIE_INLINE_H_
