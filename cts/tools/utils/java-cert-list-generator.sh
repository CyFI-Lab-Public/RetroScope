#!/bin/bash

# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [[ -z $ANDROID_BUILD_TOP ]]; then
  echo "Run 'lunch' to set \$ANDROID_BUILD_TOP" >&2
  exit 1
fi

# Output the Java file with the certificate fingerprints
cat <<-STARTCLASS
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions
 * and
 * limitations under the License.
 */

package android.security.cts;

/**
 * Run "./cts/tools/utils/java-cert-list-generator.sh >
 * cts/tests/tests/security/src/android/security/cts/CertificateData.java"
 * to generate this file.
 */
class CertificateData {
  static final String[] CERTIFICATE_DATA = {
STARTCLASS

CERT_DIRECTORY=$ANDROID_BUILD_TOP/libcore/luni/src/main/files/cacerts
for FILE in `ls $CERT_DIRECTORY`; do
  FINGERPRINT=`cat $CERT_DIRECTORY/$FILE | grep "SHA1 Fingerprint=" | cut -d '=' -f 2`
  echo "      \"${FINGERPRINT}\","
done

cat <<-ENDCLASS
  };
}
ENDCLASS
