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

// This file contains a few utilities for MulticlassPA

#include "learning/multiclass_pa/util.h"

#include <string>

#include "learning/multiclass_pa/multiclass_pa.h"
#include "learning/multiclass_pa/multiclass_pa_max.h"
#include "learning/multiclass_pa/multiclass_pa_opt.h"
#include "learning/multiclass_pa/multiclass_warp.h"
#include "strings/strutil.h"

namespace learning_multiclass_pa {

MulticlassPA* CreateMulticlassPA(const string& version,
                                 int32 num_classes,
                                 int32 num_dimensions,
                                 float aggressiveness) {
  MulticlassPA* machine = NULL;
  if (StringCaseEqual(version, "max")) {
    machine = reinterpret_cast<MulticlassPA*>(
      new MulticlassPAMax(num_classes, num_dimensions, aggressiveness));
  } else if (StringCaseEqual(version, "opt")) {
    machine = reinterpret_cast<MulticlassPA*>(
      new MulticlassPAOpt(num_classes, num_dimensions, aggressiveness));
  } else if (StringCaseEqual(version, "random")) {
    machine = new MulticlassPA(num_classes, num_dimensions, aggressiveness);
  } else if (StringCaseEqual(version, "warp")) {
    machine = reinterpret_cast<MulticlassPA*>(
      new MulticlassWarp(num_classes, num_dimensions, aggressiveness));
  } else {
    LOG(ERROR) << "Machine type (" << version << ") unknown";
  }
  return machine;
}
}  // namespace learning_multiclass_pa
