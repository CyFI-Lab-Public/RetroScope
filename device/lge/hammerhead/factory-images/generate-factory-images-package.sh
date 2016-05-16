#!/bin/sh

# Copyright 2013 The Android Open Source Project
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

# start klp-dev
# 741161 = KRS15
# end klp-dev

source ../../../common/clear-factory-images-variables.sh
BUILD=741161
DEVICE=hammerhead
PRODUCT=hammerhead
VERSION=krs15
#SRCPREFIX=signed-
BOOTLOADER=hhz10f
RADIO=m8974a-0.0.19.0.05
source ../../../common/generate-factory-images-common.sh
