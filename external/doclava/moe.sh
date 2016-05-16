#!/bin/bash

# Copyright (C) 2010 The Android Open Source Project
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

# A simple script to import the latest code for Doclava and repackage it
# for the Android build system. Repackaging involves:
#   * Removing the lib/ directory- jsilver is built from source, and
#     ant is not used.
#   * Removing the ant DoclavaTask task (so we don't require ant to build)

rm -fr res/ src/ && svn export --force https://doclava.googlecode.com/svn/trunk/ . && rm -fr lib/ samples/ src/com/google/doclava/DoclavaTask.java
