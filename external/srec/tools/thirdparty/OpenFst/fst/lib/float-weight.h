// float-weight.h
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//
// \file
// Float weight set and associated semiring operation definitions.
//

#ifndef FST_LIB_FLOAT_WEIGHT_H__
#define FST_LIB_FLOAT_WEIGHT_H__

#include <limits>

#include "fst/lib/weight.h"

namespace fst {

static const float kPosInfinity = numeric_limits<float>::infinity();
static const float kNegInfinity = -kPosInfinity;

// Single precision floating point weight base class
class FloatWeight {
 public:
  FloatWeight() {}

  FloatWeight(float f) : value_(f) {}

  FloatWeight(const FloatWeight &w) : value_(w.value_) {}

  FloatWeight &operator=(const FloatWeight &w) {
    value_ = w.value_;
    return *this;
  }

  istream &Read(istream &strm) {
    return ReadType(strm, &value_);
  }

  ostream &Write(ostream &strm) const {
    return WriteType(strm, value_);
  }

  ssize_t Hash() const {
    union {
      float f;
      ssize_t s;
    } u = { value_ };
    return u.s;
  }

  const float &Value() const { return value_; }

 protected:
  float value_;
};

inline bool operator==(const FloatWeight &w1, const FloatWeight &w2) {
  // Volatile qualifier thwarts over-aggressive compiler optimizations
  // that lead to problems esp. with NaturalLess().
  volatile float v1 = w1.Value();
  volatile float v2 = w2.Value();
  return v1 == v2;
}

inline bool operator!=(const FloatWeight &w1, const FloatWeight &w2) {
  return !(w1 == w2);
}

inline bool ApproxEqual(const FloatWeight &w1, const FloatWeight &w2,
                        float delta = kDelta) {
  return w1.Value() <= w2.Value() + delta && w2.Value() <= w1.Value() + delta;
}

inline ostream &operator<<(ostream &strm, const FloatWeight &w) {
  if (w.Value() == kPosInfinity)
    return strm << "Infinity";
  else if (w.Value() == kNegInfinity)
    return strm << "-Infinity";
  else if (w.Value() != w.Value())   // Fails for NaN
    return strm << "BadFloat";
  else
    return strm << w.Value();
}

inline istream &operator>>(istream &strm, FloatWeight &w) {
  string s;
  strm >> s;
  if (s == "Infinity") {
    w = FloatWeight(kPosInfinity);
  } else if (s == "-Infinity") {
    w = FloatWeight(kNegInfinity);
  } else {
    char *p;
    float f = strtod(s.c_str(), &p);
    if (p < s.c_str() + s.size())
      strm.clear(std::ios::badbit);
    else
      w = FloatWeight(f);
  }
  return strm;
}


// Tropical semiring: (min, +, inf, 0)
class TropicalWeight : public FloatWeight {
 public:
  typedef TropicalWeight ReverseWeight;

  TropicalWeight() : FloatWeight() {}

  TropicalWeight(float f) : FloatWeight(f) {}

  TropicalWeight(const TropicalWeight &w) : FloatWeight(w) {}

  static const TropicalWeight Zero() { return TropicalWeight(kPosInfinity); }

  static const TropicalWeight One() { return TropicalWeight(0.0F); }

  static const string &Type() {
    static const string type = "tropical";
    return type;
  }

  bool Member() const {
    // First part fails for IEEE NaN
    return Value() == Value() && Value() != kNegInfinity;
  }

  TropicalWeight Quantize(float delta = kDelta) const {
    return TropicalWeight(floor(Value()/delta + 0.5F) * delta);
  }

  TropicalWeight Reverse() const { return *this; }

  static uint64 Properties() {
    return kLeftSemiring | kRightSemiring | kCommutative |
      kPath | kIdempotent;
  }
};

inline TropicalWeight Plus(const TropicalWeight &w1,
                           const TropicalWeight &w2) {
  return w1.Value() < w2.Value() ? w1 : w2;
}

inline TropicalWeight Times(const TropicalWeight &w1,
                            const TropicalWeight &w2) {
  float f1 = w1.Value(), f2 = w2.Value();
  if (f1 == kPosInfinity)
    return w1;
  else if (f2 == kPosInfinity)
    return w2;
  else
    return TropicalWeight(f1 + f2);
}

inline TropicalWeight Divide(const TropicalWeight &w1,
                             const TropicalWeight &w2,
                             DivideType typ = DIVIDE_ANY) {
  float f1 = w1.Value(), f2 = w2.Value();
  if (f2 == kPosInfinity)
    return kNegInfinity;
  else if (f1 == kPosInfinity)
    return kPosInfinity;
  else
    return TropicalWeight(f1 - f2);
}


// Log semiring: (log(e^-x + e^y), +, inf, 0)
class LogWeight : public FloatWeight {
 public:
  typedef LogWeight ReverseWeight;

  LogWeight() : FloatWeight() {}

  LogWeight(float f) : FloatWeight(f) {}

  LogWeight(const LogWeight &w) : FloatWeight(w) {}

  static const LogWeight Zero() {   return LogWeight(kPosInfinity); }

  static const LogWeight One() { return LogWeight(0.0F); }

  static const string &Type() {
    static const string type = "log";
    return type;
  }

  bool Member() const {
    // First part fails for IEEE NaN
    return Value() == Value() && Value() != kNegInfinity;
  }

  LogWeight Quantize(float delta = kDelta) const {
    return LogWeight(floor(Value()/delta + 0.5F) * delta);
  }

  LogWeight Reverse() const { return *this; }

  static uint64 Properties() {
    return kLeftSemiring | kRightSemiring | kCommutative;
  }
};

inline double LogExp(double x) { return log(1.0F + exp(-x)); }

inline LogWeight Plus(const LogWeight &w1, const LogWeight &w2) {
  float f1 = w1.Value(), f2 = w2.Value();
  if (f1 == kPosInfinity)
    return w2;
  else if (f2 == kPosInfinity)
    return w1;
  else if (f1 > f2)
    return LogWeight(f2 - LogExp(f1 - f2));
  else
    return LogWeight(f1 - LogExp(f2 - f1));
}

inline LogWeight Times(const LogWeight &w1, const LogWeight &w2) {
  float f1 = w1.Value(), f2 = w2.Value();
  if (f1 == kPosInfinity)
    return w1;
  else if (f2 == kPosInfinity)
    return w2;
  else
    return LogWeight(f1 + f2);
}

inline LogWeight Divide(const LogWeight &w1,
                             const LogWeight &w2,
                             DivideType typ = DIVIDE_ANY) {
  float f1 = w1.Value(), f2 = w2.Value();
  if (f2 == kPosInfinity)
    return kNegInfinity;
  else if (f1 == kPosInfinity)
    return kPosInfinity;
  else
    return LogWeight(f1 - f2);
}

}  // namespace fst;

#endif  // FST_LIB_FLOAT_WEIGHT_H__
