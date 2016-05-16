#!/bin/bash

# Copyright (C) 2013 The Android Open Source Project
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


# Stop if anything goes wrong, and show what we're doing. (This script is slow.)
set -e
set -x

# TODO: extract this from the .dat file's name.
ICU_VERSION=51

ICU_BIN=$ANDROID_BUILD_TOP/prebuilts/misc/linux-x86_64/icu-$ICU_VERSION
ICU4C_DIR=$ANDROID_BUILD_TOP/external/icu4c

# Make a temporary directory.
rm -rf $ICU4C_DIR/tmp
mkdir $ICU4C_DIR/tmp

# TODO: expand this to more than just the curr and region files.
data_kinds="curr region"

for data_kind in $data_kinds ; do
  mkdir $ICU4C_DIR/tmp/$data_kind

  # Compile the .txt files to .res files.
  cd $ICU4C_DIR/data/$data_kind
  for locale in *.txt ; do
    $ICU_BIN/genrb -d $ICU4C_DIR/tmp/$data_kind ../../data/$data_kind/$locale
  done
done

# Create a copy of the .dat file that uses the new .res files.
cp $ICU4C_DIR/stubdata/icudt${ICU_VERSION}l-all.dat $ICU4C_DIR/tmp/icudt${ICU_VERSION}l.dat
cd $ICU4C_DIR/tmp
for data_kind in $data_kinds ; do
  for res in $data_kind/*.res ; do
    $ICU_BIN/icupkg -a $res icudt${ICU_VERSION}l.dat
  done
done

# Make the modified .dat file the canonical copy.
mv $ICU4C_DIR/tmp/icudt${ICU_VERSION}l.dat $ICU4C_DIR/stubdata/icudt${ICU_VERSION}l-all.dat
