# Copyright (C) 2009 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

# include definition of core-junit-files
include $(LOCAL_PATH)/Common.mk

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# note: ideally this should be junit-host, but leave as is for now to avoid
# changing all its dependencies
LOCAL_MODULE := junit
LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES := hamcrest-host
include $(BUILD_HOST_JAVA_LIBRARY)

# ----------------------------------
# build a core-junit target jar that is built into Android system image

include $(CLEAR_VARS)

# TODO: remove extensions once core-tests is no longer dependent on it
LOCAL_SRC_FILES := $(call all-java-files-under, src/junit/extensions)
LOCAL_SRC_FILES += $(core-junit-files)

LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := core
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core-junit
include $(BUILD_JAVA_LIBRARY)

# ----------------------------------
# build a core-junit-hostdex jar that contains exactly the same classes
# as core-junit.

ifeq ($(WITH_HOST_DALVIK),true)
include $(CLEAR_VARS)
# TODO: remove extensions once apache-harmony/luni/ is no longer dependent
# on it
LOCAL_SRC_FILES := $(call all-java-files-under, src/junit/extensions)
LOCAL_SRC_FILES += $(core-junit-files)
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core-junit-hostdex
LOCAL_BUILD_HOST_DEX := true
include $(BUILD_HOST_JAVA_LIBRARY)
endif

#-------------------------------------------------------
# build a junit-runner jar for the host JVM
# (like the junit classes in the frameworks/base android.test.runner.jar)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(junit-runner-files)
LOCAL_MODULE := junit-runner
LOCAL_MODULE_TAGS := optional
include $(BUILD_STATIC_JAVA_LIBRARY)

#-------------------------------------------------------
# build a junit-runner for the host dalvikvm
# (like the junit classes in the frameworks/base android.test.runner.jar)

ifeq ($(WITH_HOST_DALVIK),true)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(junit-runner-files)
LOCAL_MODULE := junit-runner-hostdex
LOCAL_MODULE_TAGS := optional
LOCAL_BUILD_HOST_DEX := true
LOCAL_JAVA_LIBRARIES := core-junit-hostdex
include $(BUILD_HOST_JAVA_LIBRARY)
endif

#-------------------------------------------------------
# build a junit4-target jar representing the
# classes in external/junit that are not in the core public API 4
# Note: 'core' here means excluding the classes that are contained
# in the optional library android.test.runner. Developers who
# build against this jar shouldn't have to also include android.test.runner

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src/org)
LOCAL_SRC_FILES += $(call all-java-files-under, src/junit/extensions)
LOCAL_SRC_FILES += $(call all-java-files-under, src/junit/runner)
LOCAL_SRC_FILES += $(call all-java-files-under, src/junit/textui)
LOCAL_SRC_FILES += \
	src/junit/framework/ComparisonCompactor.java \
	src/junit/framework/JUnit4TestAdapterCache.java \
	src/junit/framework/JUnit4TestAdapter.java \
	src/junit/framework/JUnit4TestCaseFacade.java

LOCAL_MODULE := junit4-target
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := 4
LOCAL_STATIC_JAVA_LIBRARIES := hamcrest
include $(BUILD_STATIC_JAVA_LIBRARY)

