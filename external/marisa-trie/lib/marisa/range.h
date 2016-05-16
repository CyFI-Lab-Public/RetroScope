#ifndef MARISA_RANGE_H_
#define MARISA_RANGE_H_

#include "base.h"

namespace marisa {

class Range {
 public:
  Range() : begin_(0), end_(0), pos_(0) {}
  Range(UInt32 begin, UInt32 end, UInt32 pos)
      : begin_(begin), end_(end), pos_(pos) {}

  void set_begin(UInt32 begin) {
    begin_ = begin;
  }
  void set_end(UInt32 end) {
    end_ = end;
  }
  void set_pos(UInt32 pos) {
    pos_ = pos;
  }

  UInt32 begin() const {
    return begin_;
  }
  UInt32 end() const {
    return end_;
  }
  UInt32 pos() const {
    return pos_;
  }

 private:
  UInt32 begin_;
  UInt32 end_;
  UInt32 pos_;
};

class WRange {
 public:
  WRange() : range_(), weight_(0.0) {}
  WRange(const Range &range, double weight)
      : range_(range), weight_(weight) {}
  WRange(UInt32 begin, UInt32 end, UInt32 pos, double weight)
      : range_(begin, end, pos), weight_(weight) {}

  void set_begin(UInt32 begin) {
    range_.set_begin(begin);
  }
  void set_end(UInt32 end) {
    range_.set_end(end);
  }
  void set_pos(UInt32 pos) {
    range_.set_pos(pos);
  }
  void set_weight(double weight) {
    weight_ = weight;
  }

  const Range &range() const {
    return range_;
  }
  UInt32 begin() const {
    return range_.begin();
  }
  UInt32 end() const {
    return range_.end();
  }
  UInt32 pos() const {
    return range_.pos();
  }
  double weight() const {
    return weight_;
  }

 private:
  Range range_;
  double weight_;
};

inline bool operator>(const WRange &lhs, const WRange &rhs) {
  return lhs.weight() > rhs.weight();
}

}  // namespace marisa

#endif  // MARISA_RANGE_H_
