/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Purpose: A container for sparse weight vectors
// Maintains the sparse vector as a list of (name, value) pairs alongwith
// a normalizer_. All operations assume that (name, value/normalizer_) is the
// true value in question.

#ifndef LEARNING_STOCHASTIC_LINEAR_SPARSE_WEIGHT_VECTOR_H_
#define LEARNING_STOCHASTIC_LINEAR_SPARSE_WEIGHT_VECTOR_H_

#include <hash_map>
#include <iosfwd>
#include <math.h>
#include <sstream>
#include <string>

#include "common_defs.h"

namespace learning_stochastic_linear {

template<class Key = std::string, class Hash = std::hash_map<Key, double> >
class SparseWeightVector {
 public:
  typedef Hash Wmap;
  typedef typename Wmap::iterator Witer;
  typedef typename Wmap::const_iterator Witer_const;
  SparseWeightVector() {
    normalizer_ = 1.0;
  }
  ~SparseWeightVector() {}
  explicit SparseWeightVector(const SparseWeightVector<Key, Hash> &other) {
    CopyFrom(other);
  }
  void operator=(const SparseWeightVector<Key, Hash> &other) {
    CopyFrom(other);
  }
  void CopyFrom(const SparseWeightVector<Key, Hash> &other) {
    w_ = other.w_;
    wmin_ = other.wmin_;
    wmax_ = other.wmax_;
    normalizer_ = other.normalizer_;
  }

  // This function implements checks to prevent unbounded vectors. It returns
  // true if the checks succeed and false otherwise. A vector is deemed invalid
  // if any of these conditions are met:
  // 1. it has no values.
  // 2. its normalizer is nan or inf or close to zero.
  // 3. any of its values are nan or inf.
  // 4. its L0 norm is close to zero.
  bool IsValid() const;

  // Normalizer getters and setters.
  double GetNormalizer() const {
    return normalizer_;
  }
  void SetNormalizer(const double norm) {
    normalizer_ = norm;
  }
  void NormalizerMultUpdate(const double mul) {
    normalizer_ = normalizer_ * mul;
  }
  void NormalizerAddUpdate(const double add) {
    normalizer_ += add;
  }

  // Divides all the values by the normalizer, then it resets it to 1.0
  void ResetNormalizer();

  // Bound getters and setters.
  // True if there is a bound with val containing the bound. false otherwise.
  bool GetElementMinBound(const Key &fname, double *val) const {
    return GetValue(wmin_, fname, val);
  }
  bool GetElementMaxBound(const Key &fname, double *val) const {
    return GetValue(wmax_, fname, val);
  }
  void SetElementMinBound(const Key &fname, const double bound) {
    wmin_[fname] = bound;
  }
  void SetElementMaxBound(const Key &fname, const double bound) {
    wmax_[fname] = bound;
  }
  // Element getters and setters.
  double GetElement(const Key &fname) const {
    double val = 0;
    GetValue(w_, fname, &val);
    return val;
  }
  void SetElement(const Key &fname, const double val) {
    //DCHECK(!isnan(val));
    w_[fname] = val;
  }
  void AddUpdateElement(const Key &fname, const double val) {
    w_[fname] += val;
  }
  void MultUpdateElement(const Key &fname, const double val) {
    w_[fname] *= val;
  }
  // Load another weight vectors. Will overwrite the current vector.
  void LoadWeightVector(const SparseWeightVector<Key, Hash> &vec) {
    w_.clear();
    w_.insert(vec.w_.begin(), vec.w_.end());
    wmax_.insert(vec.wmax_.begin(), vec.wmax_.end());
    wmin_.insert(vec.wmin_.begin(), vec.wmin_.end());
    normalizer_ = vec.normalizer_;
  }
  void Clear() {
    w_.clear();
    wmax_.clear();
    wmin_.clear();
  }
  const Wmap& GetMap() const {
    return w_;
  }
  // Vector Operations.
  void AdditiveWeightUpdate(const double multiplier,
                            const SparseWeightVector<Key, Hash> &w1,
                            const double additive_const);
  void AdditiveSquaredWeightUpdate(const double multiplier,
                                   const SparseWeightVector<Key, Hash> &w1,
                                   const double additive_const);
  void AdditiveInvSqrtWeightUpdate(const double multiplier,
                                   const SparseWeightVector<Key, Hash> &w1,
                                   const double additive_const);
  void MultWeightUpdate(const SparseWeightVector<Key, Hash> &w1);
  double DotProduct(const SparseWeightVector<Key, Hash> &s) const;
  // L-x norm. eg. L1, L2.
  double LxNorm(const double x) const;
  double L2Norm() const;
  double L1Norm() const;
  double L0Norm(const double epsilon) const;
  // Bound preserving updates.
  void AdditiveWeightUpdateBounded(const double multiplier,
                                   const SparseWeightVector<Key, Hash> &w1,
                                   const double additive_const);
  void MultWeightUpdateBounded(const SparseWeightVector<Key, Hash> &w1);
  void ReprojectToBounds();
  void ReprojectL0(const double l0_norm);
  void ReprojectL1(const double l1_norm);
  void ReprojectL2(const double l2_norm);
  // Reproject using the given norm.
  // Will also rescale regularizer_ if it gets too small/large.
  int32 Reproject(const double norm, const RegularizationType r);
  // Convert this vector to a string, simply for debugging.
  std::string DebugString() const {
    std::stringstream stream;
    stream << *this;
    return stream.str();
  }
 private:
  // The weight map.
  Wmap w_;
  // Constraint bounds.
  Wmap wmin_;
  Wmap wmax_;
  // Normalizing constant in magnitude measurement.
  double normalizer_;
  // This function in necessary since by default hash_map inserts an element
  // if it does not find the key through [] operator. It implements a lookup
  // without the space overhead of an add.
  bool GetValue(const Wmap &w1, const Key &fname, double *val) const {
    Witer_const iter = w1.find(fname);
    if (iter != w1.end()) {
      (*val) = iter->second;
      return true;
    } else {
      (*val) = 0;
      return false;
    }
  }
};

// Outputs a SparseWeightVector, for debugging.
template <class Key, class Hash>
std::ostream& operator<<(std::ostream &stream,
                    const SparseWeightVector<Key, Hash> &vector) {
  typename SparseWeightVector<Key, Hash>::Wmap w_map = vector.GetMap();
  stream << "[[ ";
  for (typename SparseWeightVector<Key, Hash>::Witer_const iter = w_map.begin();
       iter != w_map.end();
       ++iter) {
    stream << "<" << iter->first << ", " << iter->second << "> ";
  }
  return stream << " ]]";
};

}  // namespace learning_stochastic_linear
#endif  // LEARNING_STOCHASTIC_LINEAR_SPARSE_WEIGHT_VECTOR_H_
