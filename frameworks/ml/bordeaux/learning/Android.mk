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
#

LEARNING_LOCAL_PATH:= $(call my-dir)
include $(call all-subdir-makefiles)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

LOCAL_MODULE := libbordeaux

LOCAL_WHOLE_STATIC_LIBRARIES := libmulticlass_pa libstochastic_linear
LOCAL_SHARED_LIBRARIES := libstlport libcutils liblog

LOCAL_PRELINK_MODULE := false

LOCAL_CFLAGS := -DANDROID

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../native

include external/stlport/libstlport.mk

include $(BUILD_SHARED_LIBRARY)

##
# Build java lib
##
LOCAL_PATH:= $(LEARNING_LOCAL_PATH)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, multiclass_pa stochastic_linear_ranker ) \
                   $(call all-java-files-under, predictor_histogram)

LOCAL_MODULE :=  bordeaux_learners

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_JNI_SHARED_LIBRARIES := libbordeaux

include $(BUILD_STATIC_JAVA_LIBRARY)
