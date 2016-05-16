/*
 * Copyright 2011-2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_VERSION_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_VERSION_H_

// API levels used by the standard Android SDK.
// MR -> Maintenance Release
// HC -> Honeycomb
// ICS -> Ice Cream Sandwich
// JB -> Jelly Bean
enum SlangTargetAPI {
  SLANG_MINIMUM_TARGET_API = 11,
  SLANG_HC_TARGET_API = 11,
  SLANG_HC_MR1_TARGET_API = 12,
  SLANG_HC_MR2_TARGET_API = 13,
  SLANG_ICS_TARGET_API = 14,
  SLANG_ICS_MR1_TARGET_API = 15,
  SLANG_JB_TARGET_API = 16,
  SLANG_JB_MR1_TARGET_API = 17,
  SLANG_JB_MR2_TARGET_API = 18,
  SLANG_MAXIMUM_TARGET_API = RS_VERSION
};
// Note that RS_VERSION is defined at build time (see Android.mk for details).

// SlangVersion refers to the released compiler version (for which certain
// behaviors could change - i.e. critical bugs fixed that may require
// additional workarounds in the backend compiler).
namespace SlangVersion {
enum {
  LEGACY = 0,
  ICS = 1400,
  JB = 1600,
  JB_MR1 = 1700,
  JB_MR2 = 1800,
  KLP = 1900,
  CURRENT = KLP
};
}  // namespace SlangVersion

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_VERSION_H_  NOLINT
