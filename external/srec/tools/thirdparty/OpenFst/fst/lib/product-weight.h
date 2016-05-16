// product-weight.h
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
// Product weight set and associated semiring operation definitions.

#ifndef FST_LIB_PRODUCT_WEIGHT_H__
#define FST_LIB_PRODUCT_WEIGHT_H__

#include "fst/lib/weight.h"

DECLARE_string(fst_product_separator);

namespace fst {

// Product semiring: W1 * W2
template<class W1, class W2>
class ProductWeight {
 public:
  typedef ProductWeight<typename W1::ReverseWeight, typename W2::ReverseWeight>
  ReverseWeight;

  ProductWeight() {}

  ProductWeight(W1 w1, W2 w2) : value1_(w1), value2_(w2) {}

  static const ProductWeight<W1, W2> &Zero() {
    static const ProductWeight<W1, W2> zero(W1::Zero(), W2::Zero());
    return zero;
  }

  static const ProductWeight<W1, W2> &One() {
    static const ProductWeight<W1, W2> one(W1::One(), W2::One());
    return one;
  }

  static const string &Type() {
    static const string type = W1::Type() + "_X_" + W2::Type();
    return type;
  }

  istream &Read(istream &strm) {
    value1_.Read(strm);
    return value2_.Read(strm);
  }

  ostream &Write(ostream &strm) const {
    value1_.Write(strm);
    return value2_.Write(strm);
  }

  ProductWeight<W1, W2> &operator=(const ProductWeight<W1, W2> &w) {
    value1_ = w.Value1();
    value2_ = w.Value2();
    return *this;
  }

  bool Member() const { return value1_.Member() && value2_.Member(); }

  ssize_t Hash() const {
    ssize_t h1 = value1_.Hash();
    ssize_t h2 = value2_.Hash();
    int lshift = 5;
    int rshift = sizeof(ssize_t) - 5;
    return h1 << lshift ^ h1 >> rshift ^ h2;
  }

  ProductWeight<W1, W2> Quantize(float delta = kDelta) const {
    return ProductWeight<W1, W2>(value1_.Quantize(), value2_.Quantize());
  }

  ReverseWeight Reverse() const {
    return ReverseWeight(value1_.Reverse(), value2_.Reverse());
  }

  static uint64 Properties() {
    uint64 props1 = W1::Properties();
    uint64 props2 = W2::Properties();
    return props1 & props2 & (kLeftSemiring | kRightSemiring |
                              kCommutative | kIdempotent);
  }

  W1 Value1() const { return value1_; }

  W2 Value2() const { return value2_; }

 private:
  W1 value1_;
  W2 value2_;
};

template <class W1, class W2>
inline bool operator==(const ProductWeight<W1, W2> &w,
                       const ProductWeight<W1, W2> &v) {
  return w.Value1() == v.Value1() && w.Value2() == v.Value2();
}

template <class W1, class W2>
inline bool operator!=(const ProductWeight<W1, W2> &w1,
                       const ProductWeight<W1, W2> &w2) {
  return w1.Value1() != w2.Value1() || w1.Value2() != w2.Value2();
}


template <class W1, class W2>
inline bool ApproxEqual(const ProductWeight<W1, W2> &w1,
                        const ProductWeight<W1, W2> &w2,
                        float delta = kDelta) {
  return w1 == w2;
}

template <class W1, class W2>
inline ostream &operator<<(ostream &strm, const ProductWeight<W1, W2> &w) {
  CHECK(FLAGS_fst_product_separator.size() == 1);
  char separator = FLAGS_fst_product_separator[0];
  return strm << w.Value1() << separator << w.Value2();
}

template <class W1, class W2>
inline istream &operator>>(istream &strm, ProductWeight<W1, W2> &w) {
  CHECK(FLAGS_fst_product_separator.size() == 1);
  char separator = FLAGS_fst_product_separator[0];
  int c;

  // read any initial whitespapce
  while (true) {
    c = strm.get();
    if (c == EOF || c == separator) {
      strm.clear(std::ios::badbit);
      return strm;
    }
    if (!isspace(c))
      break;
  }

  // read first element
  string s1;
  do {
    s1 += c;
    c = strm.get();
    if (c == EOF || isspace(c)) {
      strm.clear(std::ios::badbit);
      return strm;
    }
  } while (c != separator);
  istringstream strm1(s1);
  W1 w1 = W1::Zero();
  strm1 >> w1;

  // read second element
  W2 w2 = W2::Zero();
  strm >> w2;

  w = ProductWeight<W1, W2>(w1, w2);
  return strm;
}

template <class W1, class W2>
inline ProductWeight<W1, W2> Plus(const ProductWeight<W1, W2> &w,
                                  const ProductWeight<W1, W2> &v) {
  return ProductWeight<W1, W2>(Plus(w.Value1(), v.Value1()),
                               Plus(w.Value2(), v.Value2()));
}

template <class W1, class W2>
inline ProductWeight<W1, W2> Times(const ProductWeight<W1, W2> &w,
                                   const ProductWeight<W1, W2> &v) {
  return ProductWeight<W1, W2>(Times(w.Value1(), v.Value1()),
                               Times(w.Value2(), v.Value2()));
}

template <class W1, class W2>
inline ProductWeight<W1, W2> Divide(const ProductWeight<W1, W2> &w,
                                    const ProductWeight<W1, W2> &v,
                                    DivideType typ = DIVIDE_ANY) {
  return ProductWeight<W1, W2>(Divide(w.Value1(), v.Value1(), typ),
                               Divide(w.Value2(), v.Value2(), typ));
}

}  // namespace fst;

#endif  // FST_LIB_PRODUCT_WEIGHT_H__
