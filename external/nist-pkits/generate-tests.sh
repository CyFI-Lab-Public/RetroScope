#!/usr/bin/env bash
#
# Copyright (C) 2012 The Android Open Source Project
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
#

#
# This script sets up the execution of the test extraction script
#

STORAGE_DIR="res/tests/resources/nist-pkits"
TARGET="src/libcore/java/security/cert/X509CertificateNistPkitsTest.java"


set -e
trap "echo WARNING: Exiting on non-zero subprocess exit code" ERR;

usage() {
    echo "$0: generates test cases from the NIST PKITS documentation"
    echo ""
    echo "Usage: $0 PKITS.pdf PKITS_data.zip"
    exit 1
}

if [ $# -ne 2 ]; then
    usage
fi

PDF="${1}"
ZIP="${2}"

if [ ! -f "${PDF}" -o "${PDF#${PDF%.pdf}}" != ".pdf" ]; then
    echo "The first argument must point to PKITS.pdf"
    echo ""
    usage
elif [ ! -f "${ZIP}" -o "${ZIP#${ZIP%.zip}}" != ".zip" ]; then
    echo "The second argument must point to PKITS_data.zip"
    echo ""
    usage
fi

if [ ! -f "${TARGET}" ]; then
    echo "Can not file file:"
    echo "    ${TARGET}"
    echo ""
    usage
fi

PDFTOTEXT=$(which pdftotext)
if [ -z "${PDFTOTEXT}" -o ! -x "${PDFTOTEXT}" ]; then
    echo "pdftotext must be installed. Try"
    echo "    apt-get install pdftotext"
    exit 1
fi

TEMP_TEXT=$(mktemp --tmpdir PKITS.txt.XXXXXXXX)
TEMP_JAVA=$(mktemp --tmpdir generated-nist-tests.XXXXXXXXX)
TEMP_FILES=$(mktemp --tmpdir generated-nist-files.XXXXXXXXX)

${PDFTOTEXT} -layout -nopgbrk -eol unix "${PDF}" "${TEMP_TEXT}"

"$(dirname $0)/extract-pkits-tests.pl" "${TEMP_TEXT}" "${TEMP_JAVA}" "${TEMP_FILES}"
sed -i '/DO NOT MANUALLY EDIT -- BEGIN AUTOMATICALLY GENERATED TESTS/,/DO NOT MANUALLY EDIT -- END AUTOMATICALLY GENERATED TESTS/{//!d}' "${TARGET}"
sed -i '/DO NOT MANUALLY EDIT -- BEGIN AUTOMATICALLY GENERATED TESTS/r '"${TEMP_JAVA}" "${TARGET}"

pushd "$(dirname $0)"
mkdir -p "${STORAGE_DIR}"
while IFS= read -r -d $'\n' file; do
    unzip -q -o -d "${STORAGE_DIR}" "${ZIP}" "${file}"
done < ${TEMP_FILES}
popd

shasum_file() {
    declare -r file="$1"

    pushd "$(dirname "${file}")" > /dev/null 2>&1
    sha256sum -b "$(basename "${file}")"
    popd > /dev/null 2>&1
}

echo Writing pkits.version ...
echo "# sha256sum of PKITS" > pkits.version
shasum_file "${PDF}" >> pkits.version
shasum_file "${ZIP}" >> pkits.version

echo Updated tests: ${TARGET}
