#include "progress.h"

namespace marisa_alpha {

Progress::Progress(int flags) : flags_(flags), trie_id_(0), total_size_(0) {
  if ((flags_ & MARISA_ALPHA_NUM_TRIES_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_ALPHA_NUM_TRIES_MASK)
        | MARISA_ALPHA_DEFAULT_NUM_TRIES;
  }
  if ((flags & MARISA_ALPHA_TRIE_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_ALPHA_TRIE_MASK) | MARISA_ALPHA_DEFAULT_TRIE;
  }
  if ((flags & MARISA_ALPHA_TAIL_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_ALPHA_TAIL_MASK) | MARISA_ALPHA_DEFAULT_TAIL;
  }
  if ((flags & MARISA_ALPHA_ORDER_MASK) == 0) {
    flags_ = (flags_ & ~MARISA_ALPHA_ORDER_MASK) | MARISA_ALPHA_DEFAULT_ORDER;
  }
}

bool Progress::is_valid() const {
  if ((flags_ & ~MARISA_ALPHA_FLAGS_MASK) != 0) {
    return false;
  }
  switch (flags_ & MARISA_ALPHA_TRIE_MASK) {
    case MARISA_ALPHA_PATRICIA_TRIE:
    case MARISA_ALPHA_PREFIX_TRIE: {
      break;
    }
    default: {
      return false;
    }
  }
  switch (flags_ & MARISA_ALPHA_TAIL_MASK) {
    case MARISA_ALPHA_WITHOUT_TAIL:
    case MARISA_ALPHA_BINARY_TAIL:
    case MARISA_ALPHA_TEXT_TAIL: {
      break;
    }
    default: {
      return false;
    }
  }
  switch (flags_ & MARISA_ALPHA_ORDER_MASK) {
    case MARISA_ALPHA_LABEL_ORDER:
    case MARISA_ALPHA_WEIGHT_ORDER: {
      break;
    }
    default: {
      return false;
    }
  }
  return true;
}

}  // namespace marisa_alpha
