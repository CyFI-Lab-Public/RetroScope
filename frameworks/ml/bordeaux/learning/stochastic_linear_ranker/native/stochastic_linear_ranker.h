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

// Stochastic Linear Ranking algorithms.
// This class will implement a set of incremental algorithms for ranking tasks
// They support both L1 and L2 regularizations.


#ifndef LEARNING_STOCHASTIC_LINEAR_STOCHASTIC_LINEAR_RANKER_H_
#define LEARNING_STOCHASTIC_LINEAR_STOCHASTIC_LINEAR_RANKER_H_

#include <cmath>
#include <hash_map>
#include <string>

#include <sys/types.h>
#include "cutils/log.h"
#include "common_defs.h"
#include "learning_rate_controller-inl.h"
#include "sparse_weight_vector.h"

namespace learning_stochastic_linear {

// NOTE: This Stochastic Linear Ranker supports only the following update types:
// SL: Stochastic Linear
// CS: Constraint Satisfaction
template<class Key = std::string, class Hash = std::hash_map<std::string, double> >
class StochasticLinearRanker {
 public:
  // initialize lambda_ and constraint to a meaningful default. Will give
  // equal weight to the error and regularizer.
  StochasticLinearRanker() {
    iteration_num_ = 0;
    lambda_ = 1.0;
    learning_rate_controller_.SetLambda(lambda_);
    mini_batch_size_ = 1;
    learning_rate_controller_.SetMiniBatchSize(mini_batch_size_);
    adaptation_mode_ = INV_LINEAR;
    learning_rate_controller_.SetAdaptationMode(adaptation_mode_);
    update_type_ = SL;
    regularization_type_ = L2;
    kernel_type_ = LINEAR;
    kernel_param_ = 1.0;
    kernel_gain_ = 1.0;
    kernel_bias_ = 0.0;
    rank_loss_type_ = PAIRWISE;
    acceptence_probability_ = 0.1;
    mini_batch_counter_ = 0;
    gradient_l0_norm_ = -1;
    norm_constraint_ = 1.0;
  }

  ~StochasticLinearRanker() {}
  // Getters and setters
  double GetIterationNumber() const {
    return iteration_num_;
  }
  double GetNormContraint() const {
    return norm_constraint_;
  }
  RegularizationType GetRegularizationType() const {
    return regularization_type_;
  }
  double GetLambda() const {
    return lambda_;
  }
  uint64 GetMiniBatchSize() const {
    return mini_batch_size_;
  }
  int32 GetGradientL0Norm() const {
    return gradient_l0_norm_;
  }
  UpdateType GetUpdateType() const {
    return update_type_;
  }
  AdaptationMode GetAdaptationMode() const {
    return adaptation_mode_;
  }
  KernelType GetKernelType() const {
    return kernel_type_;
  }
  // This function returns the basic kernel parameter. In case of
  // polynomial kernel, it implies the degree of the polynomial.  In case of
  // RBF kernel, it implies the sigma parameter. In case of linear kernel,
  // it is not used.
  double GetKernelParam() const {
    return kernel_param_;
  }
  double GetKernelGain() const {
    return kernel_gain_;;
  }
  double GetKernelBias() const {
    return kernel_bias_;
  }
  RankLossType GetRankLossType() const {
    return rank_loss_type_;
  }
  double GetAcceptanceProbability() const {
    return acceptence_probability_;
  }
  void SetIterationNumber(uint64 num) {
    iteration_num_=num;
  }
  void SetNormConstraint(const double norm) {
    norm_constraint_ = norm;
  }
  void SetRegularizationType(const RegularizationType r) {
    regularization_type_ = r;
  }
  void SetLambda(double l) {
    lambda_ = l;
    learning_rate_controller_.SetLambda(l);
  }
  void SetMiniBatchSize(const uint64 msize) {
    mini_batch_size_ = msize;
    learning_rate_controller_.SetMiniBatchSize(msize);
  }
  void SetAdaptationMode(AdaptationMode m) {
    adaptation_mode_ = m;
    learning_rate_controller_.SetAdaptationMode(m);
  }
  void SetKernelType(KernelType k ) {
    kernel_type_ = k;
  }
  // This function sets the basic kernel parameter. In case of
  // polynomial kernel, it implies the degree of the polynomial. In case of
  // RBF kernel, it implies the sigma parameter. In case of linear kernel,
  // it is not used.
  void SetKernelParam(double param) {
    kernel_param_ = param;
  }
  // This function sets the kernel gain. NOTE: in most use cases, gain should
  // be set to 1.0.
  void SetKernelGain(double gain) {
    kernel_gain_ = gain;
  }
  // This function sets the kernel bias. NOTE: in most use cases, bias should
  // be set to 0.0.
  void SetKernelBias(double bias) {
    kernel_bias_ = bias;
  }
  void SetUpdateType(UpdateType u) {
    update_type_ = u;
  }
  void SetRankLossType(RankLossType r) {
    rank_loss_type_ = r;
  }
  void SetAcceptanceProbability(double p) {
    acceptence_probability_ = p;
  }
  void SetGradientL0Norm(const int32 gradient_l0_norm) {
    gradient_l0_norm_ = gradient_l0_norm;
  }
  // Load an existing model
  void LoadWeights(const SparseWeightVector<Key, Hash> &model) {
    weight_.LoadWeightVector(model);
  }
  // Save current model
  void SaveWeights(SparseWeightVector<Key, Hash> *model) {
    model->LoadWeightVector(weight_);
  }
  // Scoring
  double ScoreSample(const SparseWeightVector<Key, Hash> &sample) {
    const double dot = weight_.DotProduct(sample);
    double w_square;
    double s_square;
    switch (kernel_type_) {
      case LINEAR:
        return dot;
      case POLY:
        return pow(kernel_gain_ * dot + kernel_bias_, kernel_param_);
      case RBF:
        w_square = weight_.L2Norm();
        s_square = sample.L2Norm();
        return exp(-1 * kernel_param_ * (w_square + s_square - 2 * dot));
      default:
      ALOGE("unsupported kernel: %d", kernel_type_);
    }
    return -1;
  }
  // Learning Functions
  // Return values:
  // 1 :full update went through
  // 2 :partial update went through (for SL only)
  // 0 :no update necessary.
  // -1:error.
  int UpdateClassifier(const SparseWeightVector<Key, Hash> &positive,
                       const SparseWeightVector<Key, Hash> &negative);

 private:
  SparseWeightVector<Key, Hash> weight_;
  double norm_constraint_;
  double lambda_;
  RegularizationType regularization_type_;
  AdaptationMode adaptation_mode_;
  UpdateType update_type_;
  RankLossType rank_loss_type_;
  KernelType kernel_type_;
  // Kernel gain and bias are typically multiplicative and additive factors to
  // the dot product while calculating the kernel function. Kernel param is
  // kernel-specific. In case of polynomial kernel, it is the degree of the
  // polynomial.
  double kernel_param_;
  double kernel_gain_;
  double kernel_bias_;
  double acceptence_probability_;
  SparseWeightVector<Key, Hash> current_negative_;
  LearningRateController learning_rate_controller_;
  uint64 iteration_num_;
  // We average out gradient updates for mini_batch_size_ samples
  // before performing an iteration of the algorithm.
  uint64 mini_batch_counter_;
  uint64 mini_batch_size_;
  // Specifies the number of non-zero entries allowed in a gradient.
  // Default is -1 which means we take the gradient as given by data without
  // adding any new constraints. positive number is treated as an L0 constraint
  int32 gradient_l0_norm_;
  // Sub-Gradient Updates
  // Pure Sub-Gradient update without any reprojection
  // Note that a form of L2 regularization is built into this
  void UpdateSubGradient(const SparseWeightVector<Key, Hash> &positive,
                         const SparseWeightVector<Key, Hash> &negative,
                         const double learning_rate,
                         const double positive_score,
                         const double negative_score,
                         const int32 gradient_l0_norm);

};
}  // namespace learning_stochastic_linear
#endif  // LEARNING_STOCHASTIC_LINEAR_STOCHASTIC_LINEAR_RANKER_H_
