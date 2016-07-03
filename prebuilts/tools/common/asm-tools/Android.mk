# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# Note: common/cglib 2.2 requires the old asm 3.3.x.

LOCAL_PREBUILT_JAVA_LIBRARIES := \
    asm-3-tools:asm-3.3.1$(COMMON_JAVA_PACKAGE_SUFFIX) \
    asm-tools:asm-4.0$(COMMON_JAVA_PACKAGE_SUFFIX) \
    asm-tree-tools:asm-tree-4.0$(COMMON_JAVA_PACKAGE_SUFFIX) \
    asm-analysis-tools:asm-analysis-4.0$(COMMON_JAVA_PACKAGE_SUFFIX)

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_PREBUILT)
