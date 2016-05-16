#ifndef MARISA_ALPHA_CELL_H_
#define MARISA_ALPHA_CELL_H_

#include "base.h"

namespace marisa_alpha {

class Cell {
 public:
  Cell() : louds_pos_(0), node_(0), key_id_(0), length_(0) {}

  void set_louds_pos(UInt32 louds_pos) {
    louds_pos_ = louds_pos;
  }
  void set_node(UInt32 node) {
    node_ = node;
  }
  void set_key_id(UInt32 key_id) {
    key_id_ = key_id;
  }
  void set_length(std::size_t length) {
    length_ = length;
  }

  UInt32 louds_pos() const {
    return louds_pos_;
  }
  UInt32 node() const {
    return node_;
  }
  UInt32 key_id() const {
    return key_id_;
  }
  std::size_t length() const {
    return length_;
  }

 private:
  UInt32 louds_pos_;
  UInt32 node_;
  UInt32 key_id_;
  std::size_t length_;
};
}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_CELL_H_
