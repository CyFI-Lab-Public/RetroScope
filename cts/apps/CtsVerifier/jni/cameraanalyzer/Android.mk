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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
include external/stlport/libstlport.mk

LOCAL_MODULE := libcameraanalyzer

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := com_android_cts_verifier_camera_analyzer_CameraTests.cpp \
                com_android_cts_verifier_camera_analyzer_ColorCheckerTest.cpp \
                com_android_cts_verifier_camera_analyzer_ExposureCompensationTest.cpp \
                com_android_cts_verifier_camera_analyzer_AutoLockTest.cpp \
                com_android_cts_verifier_camera_analyzer_MeteringTest.cpp \
                com_android_cts_verifier_camera_analyzer_WhiteBalanceTest.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../include/colorchecker $(JNI_H_INCLUDE)

LOCAL_STATIC_LIBRARIES := libcolorchecker
LOCAL_SHARED_LIBRARIES := libjnigraphics \
                          libstlport \
                          libcutils \
                          libutils liblog

include $(BUILD_SHARED_LIBRARY)
