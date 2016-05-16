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

// This file contains the MulticlassPA class which implements a simple
// linear multi-class classifier based on the multi-prototype version of
// passive aggressive.

#ifndef LEARNINGFW_MULTICLASS_PA_H_
#define LEARNINGFW_MULTICLASS_PA_H_

#include <vector>
#include <cmath>

const float kEpsilon = 1.0e-4;

namespace learningfw {

class MulticlassPA {
 public:
  MulticlassPA(int num_classes,
               int num_dimensions,
               float aggressiveness);
  virtual ~MulticlassPA();

  // Initialize all parameters to 0.0.
  void InitializeParameters();

  // Returns a random class that is different from the target class.
  int PickAClassExcept(int target);

  // Returns a random example.
  int PickAnExample(int num_examples);

  // Computes the score of a given input vector for a given parameter
  // vector, by computing the dot product between the two.
  float Score(const std::vector<float>& inputs,
              const std::vector<float>& parameters) const;
  float SparseScore(const std::vector<std::pair<int, float> >& inputs,
                    const std::vector<float>& parameters) const;

  // Returns the square of the L2 norm.
  float L2NormSquare(const std::vector<float>& inputs) const;
  float SparseL2NormSquare(const std::vector<std::pair<int, float> >& inputs) const;

  // Verify if the given example is correctly classified with margin with
  // respect to a random class.  If not, then modifies the corresponding
  // parameters using passive-aggressive.
  virtual float TrainOneExample(const std::vector<float>& inputs, int target);
  virtual float SparseTrainOneExample(
      const std::vector<std::pair<int, float> >& inputs, int target);

  // Iteratively train the model for num_iterations on the given dataset.
  float Train(const std::vector<std::pair<std::vector<float>, int> >& data,
              int num_iterations);
  float SparseTrain(
      const std::vector<std::pair<std::vector<std::pair<int, float> >, int> >& data,
      int num_iterations);

  // Returns the best class for a given input vector.
  virtual int GetClass(const std::vector<float>& inputs);
  virtual int SparseGetClass(const std::vector<std::pair<int, float> >& inputs);

  // Computes the test error of a given test set on the current model.
  float Test(const std::vector<std::pair<std::vector<float>, int> >& data);
  float SparseTest(
      const std::vector<std::pair<std::vector<std::pair<int, float> >, int> >& data);

  // A few accessors used by the sub-classes.
  inline float aggressiveness() const {
    return aggressiveness_;
  }

  inline std::vector<std::vector<float> >& parameters() {
    return parameters_;
  }

  inline std::vector<std::vector<float> >* mutable_parameters() {
    return &parameters_;
  }

  inline int num_classes() const {
    return num_classes_;
  }

  inline int num_dimensions() const {
    return num_dimensions_;
  }

 private:
  // Keeps the current parameter vector.
  std::vector<std::vector<float> > parameters_;

  // The number of classes of the problem.
  int num_classes_;

  // The number of dimensions of the input vectors.
  int num_dimensions_;

  // Controls how "aggressive" training should be.
  float aggressiveness_;

};
}  // namespace learningfw
#endif  // LEARNINGFW_MULTICLASS_PA_H_
