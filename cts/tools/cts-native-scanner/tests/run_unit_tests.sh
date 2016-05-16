#!/bin/bash

# Copyright 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# helper script for running the cts-tradefed unit tests

checkFile() {
    if [ ! -f "$1" ]; then
        echo "Unable to locate $1"
        exit
    fi;
}

JAR_DIR=${ANDROID_HOST_OUT}/framework
JARS="ddmlib-prebuilt.jar tradefed-prebuilt.jar hosttestlib.jar cts-native-scanner.jar cts-native-scanner-tests.jar"

for JAR in $JARS; do
    checkFile ${JAR_DIR}/${JAR}
    JAR_PATH=${JAR_PATH}:${JAR_DIR}/${JAR}
done

java $RDBG_FLAG \
  -cp ${JAR_PATH} com.android.tradefed.command.Console run singleCommand host -n --class com.android.cts.nativescanner.UnitTests "$@"

