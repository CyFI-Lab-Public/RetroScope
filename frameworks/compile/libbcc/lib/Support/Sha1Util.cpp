/*
 * copyright 2012, the android open source project
 *
 * licensed under the apache license, version 2.0 (the "license");
 * you may not use this file except in compliance with the license.
 * you may obtain a copy of the license at
 *
 *     http://www.apache.org/licenses/license-2.0
 *
 * unless required by applicable law or agreed to in writing, software
 * distributed under the license is distributed on an "as is" basis,
 * without warranties or conditions of any kind, either express or implied.
 * see the license for the specific language governing permissions and
 * limitations under the license.
 */

#include "bcc/Support/Sha1Util.h"

#include <sha1.h>

#include <cstring>

#include "bcc/Support/Log.h"
#include "bcc/Support/InputFile.h"

using namespace bcc;

bool Sha1Util::GetSHA1DigestFromFile(uint8_t pResult[SHA1_DIGEST_LENGTH],
                                     const char *pFilename) {
  InputFile file(pFilename);

  if (file.hasError()) {
    ALOGE("Unable to open the file %s before SHA-1 checksum "
          "calculation! (%s)", pFilename, file.getErrorMessage().c_str());
    return false;
  }

  SHA1_CTX sha1_context;
  SHA1Init(&sha1_context);

  char buf[256];
  while (true) {
    ssize_t nread = file.read(buf, sizeof(buf));

    if (nread < 0) {
      // Some errors occurred during file reading.
      return false;
    }

    SHA1Update(&sha1_context,
               reinterpret_cast<unsigned char *>(buf),
               static_cast<unsigned long>(nread));

    if (static_cast<size_t>(nread) < sizeof(buf)) {
      break;
    }
  }

  SHA1Final(pResult, &sha1_context);

  return true;
}

bool Sha1Util::GetSHA1DigestFromBuffer(uint8_t pResult[SHA1_DIGEST_LENGTH],
                                       const uint8_t *pData, size_t pSize) {
  SHA1_CTX sha1_context;

  SHA1Init(&sha1_context);

  SHA1Update(&sha1_context,
             reinterpret_cast<const unsigned char *>(pData),
             static_cast<unsigned long>(pSize));

  SHA1Final(pResult, &sha1_context);

  return true;
}
