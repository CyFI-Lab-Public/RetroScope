#ifndef MARISA_PROGRESS_H_
#define MARISA_PROGRESS_H_

#include "base.h"

namespace marisa {

class Progress {
 public:
  explicit Progress(int flags);

  bool is_valid() const;
  bool is_last() const {
    return (trie_id_ + 1) >= num_tries();
  }

  Progress &operator++() {
    ++trie_id_;
    return *this;
  }

  void test_total_size(std::size_t total_size) {
    MARISA_THROW_IF(total_size_ > (MARISA_UINT32_MAX - total_size),
        MARISA_SIZE_ERROR);
    total_size_ += total_size;
  }

  int flags() const {
    return flags_;
  }
  int trie_id() const {
    return trie_id_;
  }
  std::size_t total_size() const {
    return total_size_;
  }

  int num_tries() const {
    return flags_ & MARISA_NUM_TRIES_MASK;
  }
  int trie() const {
    return flags_ & MARISA_TRIE_MASK;
  }
  int tail() const {
    return flags_ & MARISA_TAIL_MASK;
  }
  int order() const {
    return flags_ & MARISA_ORDER_MASK;
  }

 private:
  int flags_;
  int trie_id_;
  std::size_t total_size_;

  // Disallows copy and assignment.
  Progress(const Progress &);
  Progress &operator=(const Progress &);
};

}  // namespace marisa

#endif  // MARISA_PROGRESS_H_
