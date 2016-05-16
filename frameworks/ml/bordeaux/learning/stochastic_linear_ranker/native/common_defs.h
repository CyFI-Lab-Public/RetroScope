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

// Contains definitions commom to all classification and ranking algorithms
// in this package.

#ifndef LEARNING_STOCHASTIC_LINEAR_COMMON_DEFS_H_
#define LEARNING_STOCHASTIC_LINEAR_COMMON_DEFS_H_

namespace learning_stochastic_linear {
// Defines standard types of regulariation methods.
enum RegularizationType { L0, L1, L2, L1L2, L1LInf };

// Defines standard adaptation modes for which Stochastic Sub-Gradient
// methods are known to converge.
enum AdaptationMode { CONST, INV_LINEAR, INV_QUADRATIC, INV_SQRT };
enum UpdateType { FULL_CS, CLIP_CS, REG_CS, SL, ADAPTIVE_REG };
enum RankLossType { PAIRWISE, RECIPROCAL_RANK };
enum KernelType { LINEAR, POLY, RBF };
enum MulticlassUpdateType { MAX, RANK };
}  // namespace learning_stochastic_linear

#ifdef ANDROID
#define uint32 uint32_t
#define int32 int32_t
#define uint64 uint64_t
#define int64 int64_t
#include <cutils/log.h>
#define CHECK_GT(x,y) if (x<y) ALOGE("CHECK_GT failed at file %s line %d", \
                                   __FILE__, __LINE__);
#endif

#endif  // LEARNING_STOCHASTIC_LINEAR_COMMON_DEFS_H_
