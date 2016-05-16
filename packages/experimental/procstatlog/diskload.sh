#!/bin/sh
#
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

# This is designed to be run on the device to create I/O load.
#
# adb push diskload.sh /data/local/tmp
# adb shell su root sh /data/local/tmp/diskload.sh /data/testfile

dd if=/dev/zero bs=65536 of=$1.0 & pid0=$!; echo start $pid0; sleep 2
dd if=/dev/zero bs=65536 of=$1.1 & pid1=$!; echo start $pid1; sleep 2
dd if=/dev/zero bs=65536 of=$1.2 & pid2=$!; echo start $pid2; sleep 2
dd if=/dev/zero bs=65536 of=$1.3 & pid3=$!; echo start $pid3; sleep 2
dd if=/dev/zero bs=65536 of=$1.4 & pid4=$!; echo start $pid4; sleep 2
dd if=/dev/zero bs=65536 of=$1.5 & pid5=$!; echo start $pid5; sleep 2
dd if=/dev/zero bs=65536 of=$1.6 & pid6=$!; echo start $pid6; sleep 2
dd if=/dev/zero bs=65536 of=$1.7 & pid7=$!; echo start $pid7; sleep 2
dd if=/dev/zero bs=65536 of=$1.8 & pid8=$!; echo start $pid8; sleep 2
dd if=/dev/zero bs=65536 of=$1.9 & pid9=$!; echo start $pid9; sleep 2

kill $pid0; echo kill $pid0; sleep 2
kill $pid1; echo kill $pid1; sleep 2
kill $pid2; echo kill $pid2; sleep 2
kill $pid3; echo kill $pid3; sleep 2
kill $pid4; echo kill $pid4; sleep 2
kill $pid5; echo kill $pid5; sleep 2
kill $pid6; echo kill $pid6; sleep 2
kill $pid7; echo kill $pid7; sleep 2
kill $pid8; echo kill $pid8; sleep 2
kill $pid9; echo kill $pid9; sleep 2

ls -l $1.0 $1.1 $1.2 $1.3 $1.4 $1.5 $1.6 $1.7 $1.8 $1.9
rm $1.0 $1.1 $1.2 $1.3 $1.4 $1.5 $1.6 $1.7 $1.8 $1.9
