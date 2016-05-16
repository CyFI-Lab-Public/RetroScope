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

#ifndef BCC_SUPPORT_SHA1_UTIL_H
#define BCC_SUPPORT_SHA1_UTIL_H

#include <stdint.h>

#include <cstddef>

// This guard prevents system sha1.h (such as the one in bionic) has been
// included before this header.
#ifndef SHA1_DIGEST_LENGTH
#define SHA1_DIGEST_LENGTH 20
#endif

namespace bcc {

class Sha1Util {
private:
  Sha1Util(); // DISABLED.
  Sha1Util(Sha1Util &); // DISABLED.

public:
  // Return true on success.
  static bool GetSHA1DigestFromFile(uint8_t pResult[SHA1_DIGEST_LENGTH],
                                    const char *pFilename);

  // Return true on success.
  static bool GetSHA1DigestFromBuffer(uint8_t pResult[SHA1_DIGEST_LENGTH],
                                      const uint8_t *pData, size_t pSize);

  // Useful function when passing buffer of type  "const char *."
  static bool GetSHA1DigestFromBuffer(uint8_t pResult[SHA1_DIGEST_LENGTH],
                                      const char *pData, size_t pSize) {
    return GetSHA1DigestFromBuffer(pResult,
                                   reinterpret_cast<const uint8_t*>(pData),
                                   pSize);
  }
};

} // end namespace bcc

#endif // BCC_SUPPORT_SHA1_UTIL_H
