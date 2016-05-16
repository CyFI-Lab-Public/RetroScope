#ifndef MARISA_ALPHA_RANK_H_
#define MARISA_ALPHA_RANK_H_

#include "base.h"

namespace marisa_alpha {

class Rank {
 public:
  Rank() : abs_(0), rel_lo_(0), rel_hi_(0) {}

  void set_abs(UInt32 value) {
    abs_ = value;
  }
  void set_rel1(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 64, MARISA_ALPHA_PARAM_ERROR);
    rel_lo_ = (rel_lo_ & ~0x7FU) | (value & 0x7FU);
  }
  void set_rel2(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 128, MARISA_ALPHA_PARAM_ERROR);
    rel_lo_ = (rel_lo_ & ~(0xFFU << 7)) | ((value & 0xFFU) << 7);
  }
  void set_rel3(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 192, MARISA_ALPHA_PARAM_ERROR);
    rel_lo_ = (rel_lo_ & ~(0xFFU << 15)) | ((value & 0xFFU) << 15);
  }
  void set_rel4(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 256, MARISA_ALPHA_PARAM_ERROR);
    rel_lo_ = (rel_lo_ & ~(0x1FFU << 23)) | ((value & 0x1FFU) << 23);
  }
  void set_rel5(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 320, MARISA_ALPHA_PARAM_ERROR);
    rel_hi_ = (rel_hi_ & ~0x1FFU) | (value & 0x1FFU);
  }
  void set_rel6(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 384, MARISA_ALPHA_PARAM_ERROR);
    rel_hi_ = (rel_hi_ & ~(0x1FFU << 9)) | ((value & 0x1FFU) << 9);
  }
  void set_rel7(UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(value > 448, MARISA_ALPHA_PARAM_ERROR);
    rel_hi_ = (rel_hi_ & ~(0x1FFU << 18)) | ((value & 0x1FFU) << 18);
  }

  UInt32 abs() const {
    return abs_;
  }
  UInt32 rel1() const {
    return rel_lo_ & 0x7FU;
  }
  UInt32 rel2() const {
    return (rel_lo_ >> 7) & 0xFFU;
  }
  UInt32 rel3() const {
    return (rel_lo_ >> 15) & 0xFFU;
  }
  UInt32 rel4() const {
    return rel_lo_ >> 23;
  }
  UInt32 rel5() const {
    return rel_hi_ & 0x1FFU;
  }
  UInt32 rel6() const {
    return (rel_hi_ >> 9) & 0x1FFU;
  }
  UInt32 rel7() const {
    return (rel_hi_ >> 18) & 0x1FFU;
  }

 private:
  UInt32 abs_;
  UInt32 rel_lo_;
  UInt32 rel_hi_;
};

}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_RANK_H_
