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

// Implements learning rate adaptations common to most stochastic algorithms.

#ifndef LEARNING_STOCHASTIC_LINEAR_LEARNING_RATE_CONTROLLER_INL_H_
#define LEARNING_STOCHASTIC_LINEAR_LEARNING_RATE_CONTROLLER_INL_H_

#include <cmath>
#include "common_defs.h"

namespace learning_stochastic_linear {

class LearningRateController {
 public:
  LearningRateController() {
    iteration_num_ = 1;
    lambda_ = 1.0;
    mini_batch_size_ = 1;
    mini_batch_counter_ = 1;
    sample_num_ = 1;
    mode_ = INV_LINEAR;
    is_first_sample_ = true;
  }
  ~LearningRateController() {}
  // Getters and Setters for learning rate parameter lambda_
  double GetLambda() const {
    return lambda_;
  }
  void SetLambda(double lambda) {
    lambda_ = lambda;
  }
  // Operations on current iteration number
  void SetIterationNumber(uint64 num) {
    iteration_num_ = num;
  }
  void IncrementIteration() {
    ++iteration_num_;
  }
  uint64 GetIterationNumber() const {
    return iteration_num_;
  }
  // Mini batch operations
  uint64 GetMiniBatchSize() const {
    return mini_batch_size_;
  }
  void SetMiniBatchSize(uint64 size) {
    //CHECK_GT(size, 0);
    mini_batch_size_ = size;
  }
  void IncrementSample() {
    // If this is the first sample we've already counted it to prevent NaNs
    // in the learning rate computation
    if (is_first_sample_) {
      is_first_sample_ = false;
      return;
    }
    ++sample_num_;
    if (1 == mini_batch_size_) {
      IncrementIteration();
      mini_batch_counter_ = 0;
    } else {
      ++mini_batch_counter_;
      if ((mini_batch_counter_ % mini_batch_size_ == 0)) {
        IncrementIteration();
        mini_batch_counter_ = 0;
      }
    }
  }
  uint64 GetMiniBatchCounter() const {
    return mini_batch_counter_;
  }
  // Getters and setters for adaptation mode
  AdaptationMode GetAdaptationMode() const {
    return mode_;
  }
  void SetAdaptationMode(AdaptationMode m) {
    mode_ = m;
  }
  double GetLearningRate() const {
    if (mode_ == CONST) {
      return (1.0 / (lambda_ * mini_batch_size_));
    } else if (mode_ == INV_LINEAR) {
      return (1.0 / (lambda_ * iteration_num_ * mini_batch_size_));
    } else if (mode_ == INV_QUADRATIC) {
      return (1.0 / (lambda_ *
                     mini_batch_size_ *
                     (static_cast<double>(iteration_num_) * iteration_num_)));
    } else if (mode_ == INV_SQRT) {
      return (1.0 / (lambda_ *
                     mini_batch_size_ *
                     sqrt((double)iteration_num_)));
    }
    return 0;
  }
  void CopyFrom(const LearningRateController &other) {
    iteration_num_ = other.iteration_num_;
    sample_num_ = other.sample_num_;
    mini_batch_size_ = other.mini_batch_size_;
    mini_batch_counter_ = other.mini_batch_counter_;
    mode_ = other.mode_;
    is_first_sample_ = other.is_first_sample_;
  }
 private:
  uint64 iteration_num_;
  uint64 sample_num_;
  uint64 mini_batch_size_;
  uint64 mini_batch_counter_;
  double lambda_;
  AdaptationMode mode_;
  bool is_first_sample_;
};
}  // namespace learning_stochastic_linear
#endif  // LEARNING_STOCHASTIC_LINEAR_LEARNING_RATE_CONTROLLER_INL_H_
