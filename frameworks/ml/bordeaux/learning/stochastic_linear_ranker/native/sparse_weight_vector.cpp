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

#include "sparse_weight_vector.h"

#include <algorithm>
#include <list>
#include <vector>
#include <math.h>

using std::vector;
using std::list;
using std::max;

namespace learning_stochastic_linear {

// Max/Min permitted values of normalizer_ for preventing under/overflows.
static double kNormalizerMin = 1e-20;
static double kNormalizerMax = 1e20;

template<class Key, class Hash>
bool SparseWeightVector<Key, Hash>::IsValid() const {
  if (isnan(normalizer_) || __isinff(normalizer_))
    return false;
  for (Witer_const iter = w_.begin();
       iter != w_.end();
       ++iter) {
    if (isnanf(iter->second) || __isinff(iter->second))
      return false;
  }
  return true;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::AdditiveWeightUpdate(
    const double multiplier,
    const SparseWeightVector<Key, Hash> &w1,
    const double additive_const) {
  for (Witer_const iter = w1.w_.begin();
      iter != w1.w_.end();
      ++iter) {
    w_[iter->first] += ((multiplier * iter->second) / w1.normalizer_
                        + additive_const) * normalizer_;
  }
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::AdditiveSquaredWeightUpdate(
    const double multiplier,
    const SparseWeightVector<Key, Hash> &w1,
    const double additive_const) {
  for (Witer_const iter = w1.w_.begin();
      iter != w1.w_.end();
      ++iter) {
    w_[iter->first] += ((multiplier * iter->second * iter->second) /
                          (w1.normalizer_ * w1.normalizer_)
                        + additive_const) * normalizer_;
  }
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::AdditiveInvSqrtWeightUpdate(
    const double multiplier,
    const SparseWeightVector<Key, Hash> &w1,
    const double additive_const) {
  for (Witer_const iter = w1.w_.begin();
      iter != w1.w_.end();
      ++iter) {
    if(iter->second > 0.0) {
      w_[iter->first] += ((multiplier * sqrt(w1.normalizer_)) /
                          (sqrt(iter->second))
                          + additive_const) * normalizer_;
    }
  }
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::AdditiveWeightUpdateBounded(
    const double multiplier,
    const SparseWeightVector<Key, Hash> &w1,
    const double additive_const) {
  double min_bound = 0;
  double max_bound = 0;
  for (Witer_const iter = w1.w_.begin();
      iter != w1.w_.end();
      ++iter) {
    w_[iter->first] += ((multiplier * iter->second) / w1.normalizer_
                        + additive_const) * normalizer_;
    bool is_min_bounded = GetValue(wmin_, iter->first, &min_bound);
    if (is_min_bounded) {
      if ((w_[iter->first] / normalizer_) < min_bound) {
        w_[iter->first] = min_bound*normalizer_;
        continue;
      }
    }
    bool is_max_bounded = GetValue(wmax_, iter->first, &max_bound);
    if (is_max_bounded) {
      if ((w_[iter->first] / normalizer_) > max_bound)
        w_[iter->first] = max_bound*normalizer_;
    }
  }
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::MultWeightUpdate(
    const SparseWeightVector<Key, Hash> &w1) {
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    iter->second *= w1.GetElement(iter->first);
  }
  normalizer_ *= w1.normalizer_;
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::MultWeightUpdateBounded(
    const SparseWeightVector<Key, Hash> &w1) {
  double min_bound = 0;
  double max_bound = 0;

  normalizer_ *= w1.normalizer_;
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    iter->second *= w1.GetElement(iter->first);
    bool is_min_bounded = GetValue(wmin_, iter->first, &min_bound);
    if (is_min_bounded) {
      if ((iter->second / normalizer_) < min_bound) {
        iter->second = min_bound*normalizer_;
        continue;
      }
    }
    bool is_max_bounded = GetValue(wmax_, iter->first, &max_bound);
    if (is_max_bounded) {
      if ((iter->second / normalizer_) > max_bound)
        iter->second = max_bound*normalizer_;
    }
  }
  return;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::ResetNormalizer() {
  for (Witer iter = w_.begin();
       iter != w_.end();
       ++iter) {
    iter->second /= normalizer_;
  }
  normalizer_ = 1.0;
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::ReprojectToBounds() {
  double min_bound = 0;
  double max_bound = 0;

  for (Witer iter = w_.begin();
       iter != w_.end();
       ++iter) {
    bool is_min_bounded = GetValue(wmin_, iter->first, &min_bound);
    if (is_min_bounded) {
      if ((iter->second/normalizer_) < min_bound) {
        iter->second = min_bound*normalizer_;
        continue;
      }
    }
    bool is_max_bounded = GetValue(wmax_, iter->first, &max_bound);
    if (is_max_bounded) {
      if ((iter->second/normalizer_) > max_bound)
        iter->second = max_bound*normalizer_;
    }
  }
}

template<class Key, class Hash>
double SparseWeightVector<Key, Hash>::DotProduct(
    const SparseWeightVector<Key, Hash> &w1) const {
  double result = 0;
  if (w_.size() > w1.w_.size()) {
    for (Witer_const iter = w1.w_.begin();
        iter != w1.w_.end();
        ++iter) {
      result += iter->second * GetElement(iter->first);
    }
    result /= (this->normalizer_ * w1.normalizer_);
  } else {
    for (Witer_const iter = w_.begin();
        iter != w_.end();
        ++iter) {
      result += iter->second * w1.GetElement(iter->first);
    }
    result /= (this->normalizer_ * w1.normalizer_);
  }
  return result;
}

template<class Key, class Hash>
double SparseWeightVector<Key, Hash>::LxNorm(const double x) const {
  double result = 0;
  CHECK_GT(x, 0);
  for (Witer_const iter = w_.begin();
      iter != w_.end();
      ++iter) {
    result += pow(iter->second, x);
  }
  return (pow(result, 1.0 / x) / normalizer_);
}

template<class Key, class Hash>
double SparseWeightVector<Key, Hash>::L2Norm() const {
  double result = 0;
  for (Witer_const iter = w_.begin();
      iter != w_.end();
      ++iter) {
    result += iter->second * iter->second;
  }
  return sqrt(result)/normalizer_;
}

template<class Key, class Hash>
double SparseWeightVector<Key, Hash>::L1Norm() const {
  double result = 0;
  for (Witer_const iter = w_.begin();
      iter != w_.end();
      ++iter) {
    result += fabs(iter->second);
  }
  return result / normalizer_;
}

template<class Key, class Hash>
double SparseWeightVector<Key, Hash>::L0Norm(
    const double epsilon) const {
  double result = 0;
  for (Witer_const iter = w_.begin();
      iter != w_.end();
      ++iter) {
    if (fabs(iter->second / normalizer_) > epsilon)
      ++result;
  }
  return result;
}

// Algorithm for L0 projection which takes O(n log(n)), where n is
// the number of non-zero elements in the vector.
template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::ReprojectL0(const double l0_norm) {
// First calculates the order-statistics of the sparse vector
// and then reprojects to the L0 orthant with the requested norm.
  CHECK_GT(l0_norm, 0);
  uint64 req_l0_norm = static_cast<uint64>(l0_norm);
  // Compute order statistics and the current L0 norm.
  vector<double> abs_val_vec;
  uint64 curr_l0_norm = 0;
  const double epsilone = 1E-05;
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    if (fabs(iter->second/normalizer_) > epsilone) {
      abs_val_vec.push_back(fabs(iter->second/normalizer_));
      ++curr_l0_norm;
    }
  }
  // check if a projection is necessary
  if (curr_l0_norm < req_l0_norm) {
    return;
  }
  std::nth_element(&abs_val_vec[0],
              &abs_val_vec[curr_l0_norm - req_l0_norm],
              &abs_val_vec[curr_l0_norm]);
  const double theta = abs_val_vec[curr_l0_norm - req_l0_norm];
  // compute the final projection.
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    if ((fabs(iter->second/normalizer_) - theta) < 0) {
      iter->second = 0;
    }
  }
}

// Slow algorithm for accurate L1 projection which takes O(n log(n)), where n is
// the number of non-zero elements in the vector.
template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::ReprojectL1(const double l1_norm) {
// First calculates the order-statistics of the sparse vector
// applies a probability simplex projection to the abs(vector)
// and reprojects back to the original with the appropriate sign.
// For ref. see "Efficient Projections into the l1-ball for Learning
// in High Dimensions"
  CHECK_GT(l1_norm, 0);
  // Compute order statistics and the current L1 norm.
  list<double> abs_val_list;
  double curr_l1_norm = 0;
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    abs_val_list.push_back(fabs(iter->second/normalizer_));
    curr_l1_norm += fabs(iter->second/normalizer_);
  }
  // check if a projection is necessary
  if (curr_l1_norm < l1_norm) {
    return;
  }
  abs_val_list.sort();
  abs_val_list.reverse();
  // Compute projection on the probability simplex.
  double curr_index = 1;
  double theta = 0;
  double cum_sum = 0;
  for (list<double>::iterator val_iter = abs_val_list.begin();
       val_iter != abs_val_list.end();
       ++val_iter) {
    cum_sum += *val_iter;
    theta = (cum_sum - l1_norm)/curr_index;
    if (((*val_iter) - theta) <= 0) {
      break;
    }
    ++curr_index;
  }
  // compute the final projection.
  for (Witer iter = w_.begin();
      iter != w_.end();
      ++iter) {
    int sign_mul = iter->second > 0;
    iter->second = max(sign_mul * normalizer_ *
                           (fabs(iter->second/normalizer_) - theta),
                       0.0);
  }
}

template<class Key, class Hash>
void SparseWeightVector<Key, Hash>::ReprojectL2(const double l2_norm) {
  CHECK_GT(l2_norm, 0);
  double curr_l2_norm = L2Norm();
  // Check if a projection is necessary.
  if (curr_l2_norm > l2_norm) {
    normalizer_ *= curr_l2_norm / l2_norm;
  }
}

template<class Key, class Hash>
int32 SparseWeightVector<Key, Hash>::Reproject(const double norm,
                                               const RegularizationType r) {
  CHECK_GT(norm, 0);
  if (r == L0) {
    ReprojectL0(norm);
  } else if (r == L1) {
    ReprojectL1(norm);
  } else if (r == L2) {
    ReprojectL2(norm);
  } else {
    // This else is just to ensure that if other RegularizationTypes are
    // supported in the enum later which require manipulations not related
    // to SparseWeightVector then we catch the accidental argument here.
    ALOGE("Unsupported regularization type requested");
    return -1;
  }
  // If the normalizer gets dangerously large or small, normalize the
  // entire vector. This stops projections from sending the vector
  // weights and the normalizer simultaneously all very small or
  // large, causing under/over flows. But if you hit this too often
  // it's a sign you've chosen a bad lambda.
  if (normalizer_ < kNormalizerMin) {
    ALOGE("Resetting normalizer to 1.0 to prevent underflow. "
          "Is lambda too large?");
    ResetNormalizer();
  }
  if (normalizer_ > kNormalizerMax) {
    ALOGE("Resetting normalizer to 1.0 to prevent overflow. "
          "Is lambda too small?");
    ResetNormalizer();
  }
  return 0;
}

template class SparseWeightVector<std::string, std::hash_map<std::string, double> >;
template class SparseWeightVector<int, std::hash_map<int, double> >;
template class SparseWeightVector<uint64, std::hash_map<uint64, double> >;
}  // namespace learning_stochastic_linear
