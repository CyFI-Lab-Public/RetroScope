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

//
// This file contains the MulticlassPA class which implements a simple
// linear multi-class classifier based on the multi-prototype version of
// passive aggressive.

#include "native/multiclass_pa.h"

using std::vector;
using std::pair;

namespace learningfw {

float RandFloat() {
  return static_cast<float>(rand()) / RAND_MAX;
}

MulticlassPA::MulticlassPA(int num_classes,
                           int num_dimensions,
                           float aggressiveness)
    : num_classes_(num_classes),
      num_dimensions_(num_dimensions),
      aggressiveness_(aggressiveness) {
  InitializeParameters();
}

MulticlassPA::~MulticlassPA() {
}

void MulticlassPA::InitializeParameters() {
  parameters_.resize(num_classes_);
  for (int i = 0; i < num_classes_; ++i) {
    parameters_[i].resize(num_dimensions_);
    for (int j = 0; j < num_dimensions_; ++j) {
      parameters_[i][j] = 0.0;
    }
  }
}

int MulticlassPA::PickAClassExcept(int target) {
  int picked;
  do {
    picked = static_cast<int>(RandFloat() * num_classes_);
    //    picked = static_cast<int>(random_.RandFloat() * num_classes_);
  } while (target == picked);
  return picked;
}

int MulticlassPA::PickAnExample(int num_examples) {
  return static_cast<int>(RandFloat() * num_examples);
}

float MulticlassPA::Score(const vector<float>& inputs,
                          const vector<float>& parameters) const {
  // CHECK_EQ(inputs.size(), parameters.size());
  float result = 0.0;
  for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
    result += inputs[i] * parameters[i];
  }
  return result;
}

float MulticlassPA::SparseScore(const vector<pair<int, float> >& inputs,
                                const vector<float>& parameters) const {
  float result = 0.0;
  for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
    //DCHECK_GE(inputs[i].first, 0);
    //DCHECK_LT(inputs[i].first, parameters.size());
    result += inputs[i].second * parameters[inputs[i].first];
  }
  return result;
}

float MulticlassPA::L2NormSquare(const vector<float>& inputs) const {
  float norm = 0;
  for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
    norm += inputs[i] * inputs[i];
  }
  return norm;
}

float MulticlassPA::SparseL2NormSquare(
    const vector<pair<int, float> >& inputs) const {
  float norm = 0;
  for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
    norm += inputs[i].second * inputs[i].second;
  }
  return norm;
}

float MulticlassPA::TrainOneExample(const vector<float>& inputs, int target) {
  //CHECK_GE(target, 0);
  //CHECK_LT(target, num_classes_);
  float target_class_score = Score(inputs, parameters_[target]);
  //  VLOG(1) << "target class " << target << " score " << target_class_score;
  int other_class = PickAClassExcept(target);
  float other_class_score = Score(inputs, parameters_[other_class]);
  //  VLOG(1) << "other class " << other_class << " score " << other_class_score;
  float loss = 1.0 - target_class_score + other_class_score;
  if (loss > 0.0) {
    // Compute the learning rate according to PA-I.
    float twice_norm_square = L2NormSquare(inputs) * 2.0;
    if (twice_norm_square == 0.0) {
      twice_norm_square = kEpsilon;
    }
    float rate = loss / twice_norm_square;
    if (rate > aggressiveness_) {
      rate = aggressiveness_;
    }
    //    VLOG(1) << "loss = " << loss << " rate = " << rate;
    // Modify the parameter vectors of the correct and wrong classes
    for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
      // First modify the parameter value of the correct class
      parameters_[target][i] += rate * inputs[i];
      // Then modify the parameter value of the wrong class
      parameters_[other_class][i] -= rate * inputs[i];
    }
    return loss;
  }
  return 0.0;
}

float MulticlassPA::SparseTrainOneExample(
    const vector<pair<int, float> >& inputs, int target) {
  // CHECK_GE(target, 0);
  // CHECK_LT(target, num_classes_);
  float target_class_score = SparseScore(inputs, parameters_[target]);
  //  VLOG(1) << "target class " << target << " score " << target_class_score;
  int other_class = PickAClassExcept(target);
  float other_class_score = SparseScore(inputs, parameters_[other_class]);
  //  VLOG(1) << "other class " << other_class << " score " << other_class_score;
  float loss = 1.0 - target_class_score + other_class_score;
  if (loss > 0.0) {
    // Compute the learning rate according to PA-I.
    float twice_norm_square = SparseL2NormSquare(inputs) * 2.0;
    if (twice_norm_square == 0.0) {
      twice_norm_square = kEpsilon;
    }
    float rate = loss / twice_norm_square;
    if (rate > aggressiveness_) {
      rate = aggressiveness_;
    }
    //    VLOG(1) << "loss = " << loss << " rate = " << rate;
    // Modify the parameter vectors of the correct and wrong classes
    for (int i = 0; i < static_cast<int>(inputs.size()); ++i) {
      // First modify the parameter value of the correct class
      parameters_[target][inputs[i].first] += rate * inputs[i].second;
      // Then modify the parameter value of the wrong class
      parameters_[other_class][inputs[i].first] -= rate * inputs[i].second;
    }
    return loss;
  }
  return 0.0;
}

float MulticlassPA::Train(const vector<pair<vector<float>, int> >& data,
                          int num_iterations) {
  int num_examples = data.size();
  float total_loss = 0.0;
  for (int t = 0; t < num_iterations; ++t) {
    int index = PickAnExample(num_examples);
    float loss_t = TrainOneExample(data[index].first, data[index].second);
    total_loss += loss_t;
  }
  return total_loss / static_cast<float>(num_iterations);
}

float MulticlassPA::SparseTrain(
    const vector<pair<vector<pair<int, float> >, int> >& data,
    int num_iterations) {
  int num_examples = data.size();
  float total_loss = 0.0;
  for (int t = 0; t < num_iterations; ++t) {
    int index = PickAnExample(num_examples);
    float loss_t = SparseTrainOneExample(data[index].first, data[index].second);
    total_loss += loss_t;
  }
  return total_loss / static_cast<float>(num_iterations);
}

int MulticlassPA::GetClass(const vector<float>& inputs) {
  int best_class = -1;
  float best_score = -10000.0;
  // float best_score = -MathLimits<float>::kMax;
  for (int i = 0; i < num_classes_; ++i) {
    float score_i = Score(inputs, parameters_[i]);
    if (score_i > best_score) {
      best_score = score_i;
      best_class = i;
    }
  }
  return best_class;
}

int MulticlassPA::SparseGetClass(const vector<pair<int, float> >& inputs) {
  int best_class = -1;
  float best_score = -10000.0;
  //float best_score = -MathLimits<float>::kMax;
  for (int i = 0; i < num_classes_; ++i) {
    float score_i = SparseScore(inputs, parameters_[i]);
    if (score_i > best_score) {
      best_score = score_i;
      best_class = i;
    }
  }
  return best_class;
}

float MulticlassPA::Test(const vector<pair<vector<float>, int> >& data) {
  int num_examples = data.size();
  float total_error = 0.0;
  for (int t = 0; t < num_examples; ++t) {
    int best_class = GetClass(data[t].first);
    if (best_class != data[t].second) {
      ++total_error;
    }
  }
  return total_error / num_examples;
}

float MulticlassPA::SparseTest(
    const vector<pair<vector<pair<int, float> >, int> >& data) {
  int num_examples = data.size();
  float total_error = 0.0;
  for (int t = 0; t < num_examples; ++t) {
    int best_class = SparseGetClass(data[t].first);
    if (best_class != data[t].second) {
      ++total_error;
    }
  }
  return total_error / num_examples;
}
}  // namespace learningfw
