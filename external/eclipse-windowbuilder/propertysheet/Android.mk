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
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_JAVA_RESOURCE_DIRS := src

LOCAL_JAVA_LIBRARIES := \
	swt \
	org.eclipse.core.commands_3.6.0.I20100512-1500 \
	org.eclipse.core.expressions_3.4.200.v20100505 \
	org.eclipse.core.runtime_3.6.0.v20100505 \
	org.eclipse.equinox.common_3.6.0.v20100503 \
	org.eclipse.jface_3.6.2.M20110210-1200 \
	org.eclipse.osgi_3.6.2.R36x_v20110210 \
        org.eclipse.ui.workbench.texteditor_3.6.1.r361_v20100714-0800 \
	org.eclipse.ui.workbench_3.6.2.M20110210-1200 \
        asm-tools \
        guava-tools

LOCAL_MODULE := propertysheet

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)
