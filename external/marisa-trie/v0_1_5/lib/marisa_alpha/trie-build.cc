#include <algorithm>
#include <functional>
#include <queue>
#include <stdexcept>

#include "range.h"
#include "trie.h"

namespace marisa_alpha {

void Trie::build(const char * const *keys, std::size_t num_keys,
    const std::size_t *key_lengths, const double *key_weights,
    UInt32 *key_ids, int flags) {
  MARISA_ALPHA_THROW_IF((keys == NULL) && (num_keys != 0),
      MARISA_ALPHA_PARAM_ERROR);
  Vector<Key<String> > temp_keys;
  temp_keys.resize(num_keys);
  for (std::size_t i = 0; i < temp_keys.size(); ++i) {
    MARISA_ALPHA_THROW_IF(keys[i] == NULL, MARISA_ALPHA_PARAM_ERROR);
    std::size_t length = 0;
    if (key_lengths == NULL) {
      while (keys[i][length] != '\0') {
        ++length;
      }
    } else {
      length = key_lengths[i];
    }
    MARISA_ALPHA_THROW_IF(length > MARISA_ALPHA_MAX_LENGTH,
        MARISA_ALPHA_SIZE_ERROR);
    temp_keys[i].set_str(String(keys[i], length));
    temp_keys[i].set_weight((key_weights != NULL) ? key_weights[i] : 1.0);
  }
  build_trie(temp_keys, key_ids, flags);
}

void Trie::build(const std::vector<std::string> &keys,
    std::vector<UInt32> *key_ids, int flags) {
  Vector<Key<String> > temp_keys;
  temp_keys.resize(keys.size());
  for (std::size_t i = 0; i < temp_keys.size(); ++i) {
    MARISA_ALPHA_THROW_IF(keys[i].length() > MARISA_ALPHA_MAX_LENGTH,
        MARISA_ALPHA_SIZE_ERROR);
    temp_keys[i].set_str(String(keys[i].c_str(), keys[i].length()));
    temp_keys[i].set_weight(1.0);
  }
  build_trie(temp_keys, key_ids, flags);
}

void Trie::build(const std::vector<std::pair<std::string, double> > &keys,
    std::vector<UInt32> *key_ids, int flags) {
  Vector<Key<String> > temp_keys;
  temp_keys.resize(keys.size());
  for (std::size_t i = 0; i < temp_keys.size(); ++i) {
    MARISA_ALPHA_THROW_IF(keys[i].first.length() > MARISA_ALPHA_MAX_LENGTH,
        MARISA_ALPHA_SIZE_ERROR);
    temp_keys[i].set_str(String(
        keys[i].first.c_str(), keys[i].first.length()));
    temp_keys[i].set_weight(keys[i].second);
  }
  build_trie(temp_keys, key_ids, flags);
}

void Trie::build_trie(Vector<Key<String> > &keys,
    std::vector<UInt32> *key_ids, int flags) {
  if (key_ids == NULL) {
    build_trie(keys, static_cast<UInt32 *>(NULL), flags);
    return;
  }
  try {
    std::vector<UInt32> temp_key_ids(keys.size());
    build_trie(keys, temp_key_ids.empty() ? NULL : &temp_key_ids[0], flags);
    key_ids->swap(temp_key_ids);
  } catch (const std::bad_alloc &) {
    MARISA_ALPHA_THROW(MARISA_ALPHA_MEMORY_ERROR);
  } catch (const std::length_error &) {
    MARISA_ALPHA_THROW(MARISA_ALPHA_SIZE_ERROR);
  }
}

void Trie::build_trie(Vector<Key<String> > &keys,
    UInt32 *key_ids, int flags) {
  Trie temp;
  Vector<UInt32> terminals;
  Progress progress(flags);
  MARISA_ALPHA_THROW_IF(!progress.is_valid(), MARISA_ALPHA_PARAM_ERROR);
  temp.build_trie(keys, &terminals, progress);

  typedef std::pair<UInt32, UInt32> TerminalIdPair;
  Vector<TerminalIdPair> pairs;
  pairs.resize(terminals.size());
  for (UInt32 i = 0; i < pairs.size(); ++i) {
    pairs[i].first = terminals[i];
    pairs[i].second = i;
  }
  terminals.clear();
  std::sort(pairs.begin(), pairs.end());

  UInt32 node = 0;
  for (UInt32 i = 0; i < pairs.size(); ++i) {
    while (node < pairs[i].first) {
      temp.terminal_flags_.push_back(false);
      ++node;
    }
    if (node == pairs[i].first) {
      temp.terminal_flags_.push_back(true);
      ++node;
    }
  }
  while (node < temp.labels_.size()) {
    temp.terminal_flags_.push_back(false);
    ++node;
  }
  terminal_flags_.push_back(false);
  temp.terminal_flags_.build();
  temp.terminal_flags_.clear_select0s();
  progress.test_total_size(temp.terminal_flags_.total_size());

  if (key_ids != NULL) {
    for (UInt32 i = 0; i < pairs.size(); ++i) {
      key_ids[pairs[i].second] = temp.node_to_key_id(pairs[i].first);
    }
  }
  MARISA_ALPHA_THROW_IF(progress.total_size() != temp.total_size(),
      MARISA_ALPHA_UNEXPECTED_ERROR);
  temp.swap(this);
}

template <typename T>
void Trie::build_trie(Vector<Key<T> > &keys,
    Vector<UInt32> *terminals, Progress &progress) {
  build_cur(keys, terminals, progress);
  progress.test_total_size(louds_.total_size());
  progress.test_total_size(sizeof(num_first_branches_));
  progress.test_total_size(sizeof(num_keys_));
  if (link_flags_.empty()) {
    labels_.shrink();
    progress.test_total_size(labels_.total_size());
    progress.test_total_size(link_flags_.total_size());
    progress.test_total_size(links_.total_size());
    progress.test_total_size(tail_.total_size());
    return;
  }

  Vector<UInt32> next_terminals;
  build_next(keys, &next_terminals, progress);

  if (has_trie()) {
    progress.test_total_size(trie_->terminal_flags_.total_size());
  } else if (tail_.mode() == MARISA_ALPHA_BINARY_TAIL) {
    labels_.push_back('\0');
    link_flags_.push_back(true);
  }
  link_flags_.build();

  for (UInt32 i = 0; i < next_terminals.size(); ++i) {
    labels_[link_flags_.select1(i)] = (UInt8)(next_terminals[i] % 256);
    next_terminals[i] /= 256;
  }
  link_flags_.clear_select0s();
  if (has_trie() || (tail_.mode() == MARISA_ALPHA_TEXT_TAIL)) {
    link_flags_.clear_select1s();
  }

  links_.build(next_terminals);
  labels_.shrink();
  progress.test_total_size(labels_.total_size());
  progress.test_total_size(link_flags_.total_size());
  progress.test_total_size(links_.total_size());
  progress.test_total_size(tail_.total_size());
}

template <typename T>
void Trie::build_cur(Vector<Key<T> > &keys,
    Vector<UInt32> *terminals, Progress &progress) try {
  num_keys_ = sort_keys(keys);
  louds_.push_back(true);
  louds_.push_back(false);
  labels_.push_back('\0');
  link_flags_.push_back(false);

  Vector<Key<T> > rest_keys;
  std::queue<Range> queue;
  Vector<WRange> wranges;
  queue.push(Range(0, (UInt32)keys.size(), 0));
  while (!queue.empty()) {
    const UInt32 node = (UInt32)(link_flags_.size() - queue.size());
    Range range = queue.front();
    queue.pop();

    while ((range.begin() < range.end()) &&
        (keys[range.begin()].str().length() == range.pos())) {
      keys[range.begin()].set_terminal(node);
      range.set_begin(range.begin() + 1);
    }
    if (range.begin() == range.end()) {
      louds_.push_back(false);
      continue;
    }

    wranges.clear();
    double weight = keys[range.begin()].weight();
    for (UInt32 i = range.begin() + 1; i < range.end(); ++i) {
      if (keys[i - 1].str()[range.pos()] != keys[i].str()[range.pos()]) {
        wranges.push_back(WRange(range.begin(), i, range.pos(), weight));
        range.set_begin(i);
        weight = 0.0;
      }
      weight += keys[i].weight();
    }
    wranges.push_back(WRange(range, weight));
    if (progress.order() == MARISA_ALPHA_WEIGHT_ORDER) {
      std::stable_sort(wranges.begin(), wranges.end(), std::greater<WRange>());
    }
    if (node == 0) {
      num_first_branches_ = wranges.size();
    }
    for (UInt32 i = 0; i < wranges.size(); ++i) {
      const WRange &wrange = wranges[i];
      UInt32 pos = wrange.pos() + 1;
      if ((progress.tail() != MARISA_ALPHA_WITHOUT_TAIL) ||
          !progress.is_last()) {
        while (pos < keys[wrange.begin()].str().length()) {
          UInt32 j;
          for (j = wrange.begin() + 1; j < wrange.end(); ++j) {
            if (keys[j - 1].str()[pos] != keys[j].str()[pos]) {
              break;
            }
          }
          if (j < wrange.end()) {
            break;
          }
          ++pos;
        }
      }
      if ((progress.trie() != MARISA_ALPHA_PATRICIA_TRIE) &&
          (pos != keys[wrange.end() - 1].str().length())) {
        pos = wrange.pos() + 1;
      }
      louds_.push_back(true);
      if (pos == wrange.pos() + 1) {
        labels_.push_back(keys[wrange.begin()].str()[wrange.pos()]);
        link_flags_.push_back(false);
      } else {
        labels_.push_back('\0');
        link_flags_.push_back(true);
        Key<T> rest_key;
        rest_key.set_str(keys[wrange.begin()].str().substr(
            wrange.pos(), pos - wrange.pos()));
        rest_key.set_weight(wrange.weight());
        rest_keys.push_back(rest_key);
      }
      wranges[i].set_pos(pos);
      queue.push(wranges[i].range());
    }
    louds_.push_back(false);
  }
  louds_.push_back(false);
  louds_.build();
  if (progress.trie_id() != 0) {
    louds_.clear_select0s();
  }
  if (rest_keys.empty()) {
    link_flags_.clear();
  }

  build_terminals(keys, terminals);
  keys.swap(&rest_keys);
} catch (const std::bad_alloc &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_MEMORY_ERROR);
} catch (const std::length_error &) {
  MARISA_ALPHA_THROW(MARISA_ALPHA_SIZE_ERROR);
}

void Trie::build_next(Vector<Key<String> > &keys,
    Vector<UInt32> *terminals, Progress &progress) {
  if (progress.is_last()) {
    Vector<String> strs;
    strs.resize(keys.size());
    for (UInt32 i = 0; i < strs.size(); ++i) {
      strs[i] = keys[i].str();
    }
    tail_.build(strs, terminals, progress.tail());
    return;
  }
  Vector<Key<RString> > rkeys;
  rkeys.resize(keys.size());
  for (UInt32 i = 0; i < rkeys.size(); ++i) {
    rkeys[i].set_str(RString(keys[i].str()));
    rkeys[i].set_weight(keys[i].weight());
  }
  keys.clear();
  trie_.reset(new (std::nothrow) Trie);
  MARISA_ALPHA_THROW_IF(!has_trie(), MARISA_ALPHA_MEMORY_ERROR);
  trie_->build_trie(rkeys, terminals, ++progress);
}

void Trie::build_next(Vector<Key<RString> > &rkeys,
    Vector<UInt32> *terminals, Progress &progress) {
  if (progress.is_last()) {
    Vector<String> strs;
    strs.resize(rkeys.size());
    for (UInt32 i = 0; i < strs.size(); ++i) {
      strs[i] = String(rkeys[i].str().ptr(), rkeys[i].str().length());
    }
    tail_.build(strs, terminals, progress.tail());
    return;
  }
  trie_.reset(new (std::nothrow) Trie);
  MARISA_ALPHA_THROW_IF(!has_trie(), MARISA_ALPHA_MEMORY_ERROR);
  trie_->build_trie(rkeys, terminals, ++progress);
}

template <typename T>
UInt32 Trie::sort_keys(Vector<Key<T> > &keys) const {
  if (keys.empty()) {
    return 0;
  }
  for (UInt32 i = 0; i < keys.size(); ++i) {
    keys[i].set_id(i);
  }
  std::sort(keys.begin(), keys.end());
  UInt32 count = 1;
  for (UInt32 i = 1; i < keys.size(); ++i) {
    if (keys[i - 1].str() != keys[i].str()) {
      ++count;
    }
  }
  return count;
}

template <typename T>
void Trie::build_terminals(const Vector<Key<T> > &keys,
    Vector<UInt32> *terminals) const {
  Vector<UInt32> temp_terminals;
  temp_terminals.resize(keys.size());
  for (UInt32 i = 0; i < keys.size(); ++i) {
    temp_terminals[keys[i].id()] = keys[i].terminal();
  }
  temp_terminals.swap(terminals);
}

}  // namespace marisa_alpha
