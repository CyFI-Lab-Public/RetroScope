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

LOCAL_JAVA_LIBRARIES := android.test.runner bouncycastle conscrypt
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_DEX_PREOPT := false

# don't include these packages in any target
LOCAL_MODULE_TAGS := optional
# and when installed explicitly put them in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
# Don't delete META-INF from the core-tests jar
LOCAL_DONT_DELETE_JAR_META_INF := true

LOCAL_JNI_SHARED_LIBRARIES := libjavacoretests

include $(BUILD_PACKAGE)
