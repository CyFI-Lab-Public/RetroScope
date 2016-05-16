#include "progress.h"

namespace marisa {

Progress::Progress(int flags) : flags_(flags), trie_id_(0), total_size_(0) {
  if ((flags_ & MARISA_NUM_TRIES_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_NUM_TRIES_MASK) | MARISA_DEFAULT_NUM_TRIES;
  }
  if ((flags & MARISA_TRIE_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_TRIE_MASK) | MARISA_DEFAULT_TRIE;
  }
  if ((flags & MARISA_TAIL_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_TAIL_MASK) | MARISA_DEFAULT_TAIL;
  }
  if ((flags & MARISA_ORDER_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_ORDER_MASK) | MARISA_DEFAULT_ORDER;
  }
}

bool Progress::is_valid() const {
  if ((flags_ & ~MARISA_FLAGS_MASK) != 0) {
    return false;
  }
  switch (flags_ & MARISA_TRIE_MASK) {
    case MARISA_PATRICIA_TRIE:
    case MARISA_PREFIX_TRIE: {
      break;
    }
    default: {
      return false;
    }
  }
  switch (flags_ & MARISA_TAIL_MASK) {
    case MARISA_WITHOUT_TAIL:
    case MARISA_BINARY_TAIL:
    case MARISA_TEXT_TAIL: {
      break;
    }
    default: {
      return false;
    }
  }
  switch (flags_ & MARISA_ORDER_MASK) {
    case MARISA_LABEL_ORDER:
    case MARISA_WEIGHT_ORDER: {
      break;
    }
    default: {
      return false;
    }
  }
  return true;
}

}  // namespace marisa
