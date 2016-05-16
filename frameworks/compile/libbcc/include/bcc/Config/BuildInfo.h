/*
 * Copyright 2012, The Android Open Source Project
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

#ifndef BCC_CONFIG_BUILD_INFO_H
#define BCC_CONFIG_BUILD_INFO_H

namespace bcc {

class BuildInfo {
private:
  // Disable constructor since this is an utility class.
  BuildInfo();

public:
  // The implementation of these functions is generated during build. See
  // libbcc-gen-build-info.mk and tools/build/gen-build-info.py for detail.
  static const char *GetBuildTime();
  static const char *GetBuildRev();
  static const char *GetBuildSourceBlob();
};

} // end namespace bcc

#endif // BCC_CONFIG_BUILD_INFO_H
