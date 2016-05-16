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

#include <algorithm>
#include <stdlib.h>

#include "stochastic_linear_ranker.h"

namespace learning_stochastic_linear {

template<class Key, class Hash>
void StochasticLinearRanker<Key, Hash>::UpdateSubGradient(
    const SparseWeightVector<Key, Hash> &positive,
    const SparseWeightVector<Key, Hash> &negative,
    const double learning_rate,
    const double positive_score,
    const double negative_score,
    const int32 gradient_l0_norm) {
  SparseWeightVector<Key, Hash> gradient;
  double final_learning_rate;
  gradient.AdditiveWeightUpdate(1.0, positive, 0.0);
  gradient.AdditiveWeightUpdate(-1.0, negative, 0.0);
  if (update_type_ == FULL_CS || update_type_ == REG_CS) {
    const double loss = std::max(0.0, (1 - positive_score + negative_score));
    const double gradient_norm = gradient.L2Norm();
    const double kMinGradientNorm = 1e-8;
    const double kMaxGradientNorm = 1e8;
    if (gradient_norm < kMinGradientNorm || gradient_norm > kMaxGradientNorm)
      return;
    if (update_type_ == FULL_CS)
      final_learning_rate =
          std::min(lambda_, loss / (gradient_norm * gradient_norm));
    else
      final_learning_rate =
          loss / (gradient_norm * gradient_norm + 1 / (2 * lambda_));
  } else {
    gradient.AdditiveWeightUpdate(-lambda_, weight_, 0.0);
    final_learning_rate = learning_rate;
  }
  if (gradient_l0_norm > 0) {
    gradient.ReprojectL0(gradient_l0_norm);
  }

  if (gradient.IsValid())
    weight_.AdditiveWeightUpdate(final_learning_rate, gradient, 0.0);
}

template<class Key, class Hash>
int StochasticLinearRanker<Key, Hash>::UpdateClassifier(
    const SparseWeightVector<Key, Hash> &positive,
    const SparseWeightVector<Key, Hash> &negative) {
  // Create a backup of the weight vector in case the iteration results in
  // unbounded weights.
  SparseWeightVector<Key, Hash> weight_backup;
  weight_backup.CopyFrom(weight_);

  const double positive_score = ScoreSample(positive);
  const double negative_score = ScoreSample(negative);
  if ((positive_score - negative_score) < 1) {
    ++mini_batch_counter_;
    if ((mini_batch_counter_ % mini_batch_size_ == 0) ||
        (iteration_num_ == 0)) {
      ++iteration_num_;
      mini_batch_counter_ = 0;
    }
    learning_rate_controller_.IncrementSample();
    double learning_rate = learning_rate_controller_.GetLearningRate();

    if (rank_loss_type_ == PAIRWISE) {
      UpdateSubGradient(positive, negative, learning_rate,
                        positive_score, negative_score,
                        gradient_l0_norm_);
    } else if (rank_loss_type_ == RECIPROCAL_RANK) {
      const double current_negative_score = ScoreSample(current_negative_);
      if ((negative_score > current_negative_score) ||
          ((rand()/RAND_MAX) < acceptence_probability_)) {
        UpdateSubGradient(positive, negative, learning_rate,
                          positive_score, negative_score,
                          gradient_l0_norm_);
        current_negative_.Clear();
        current_negative_.LoadWeightVector(negative);
      } else {
        UpdateSubGradient(positive, current_negative_, learning_rate,
                          positive_score, negative_score,
                          gradient_l0_norm_);
      }
    } else {
      ALOGE("Unknown rank loss type: %d", rank_loss_type_);
    }

    int return_code;
    if ((mini_batch_counter_ == 0) && (update_type_ == SL)) {
      return_code = 1;
      switch (regularization_type_) {
        case L1:
          weight_.ReprojectL1(norm_constraint_);
          break;
        case L2:
          weight_.ReprojectL2(norm_constraint_);
          break;
        case L0:
          weight_.ReprojectL0(norm_constraint_);
          break;
        default:
          ALOGE("Unsupported optimization type specified");
          return_code = -1;
      }
    } else if (update_type_ == SL) {
      return_code = 2;
    } else {
      return_code = 1;
    }

    if (!weight_.IsValid())
      weight_.CopyFrom(weight_backup);
    return return_code;
  }

  return 0;
}

template class StochasticLinearRanker<std::string, std::hash_map<std::string, double> >;
template class StochasticLinearRanker<int, std::hash_map<int, double> >;
template class StochasticLinearRanker<uint64, std::hash_map<uint64, double> >;

}  // namespace learning_stochastic_linear
