#ifndef MARISA_POPCOUNT_H_
#define MARISA_POPCOUNT_H_

#include "base.h"

namespace marisa {

class PopCount {
 public:
  PopCount(UInt32 x) : value_() {
    x = (x & 0x55555555U) + ((x & 0xAAAAAAAAU) >> 1);
    x = (x & 0x33333333U) + ((x & 0xCCCCCCCCU) >> 2);
    x = (x + (x >> 4)) & 0x0F0F0F0FU;
    x += x << 8;
    x += x << 16;
    value_ = x;
  }

  UInt32 lo8() const {
    return value_ & 0xFFU;
  }
  UInt32 lo16() const {
    return (value_ >> 8) & 0xFFU;
  }
  UInt32 lo24() const {
    return (value_ >> 16) & 0xFFU;
  }
  UInt32 lo32() const {
    return value_ >> 24;
  }

 private:
  UInt32 value_;
};

}  // namespace marisa

#endif  // MARISA_POPCOUNT_H_
