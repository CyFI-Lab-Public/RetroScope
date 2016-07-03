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
#

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# $(1): sdk version
define _copy_prebuilt_sdk_to_the_right_place
_cpsttrp_src_jar := $(LOCAL_PATH)/$(1)/android.jar
_cpsttrp_sdk_intermediates := $(call intermediates-dir-for,JAVA_LIBRARIES,sdk_v$(1),,COMMON)
$$(_cpsttrp_sdk_intermediates)/classes.jar : $$(_cpsttrp_src_jar) | $(ACP)
	$$(call copy-file-to-target)

$$(_cpsttrp_sdk_intermediates)/javalib.jar : $$(_cpsttrp_sdk_intermediates)/classes.jar | $(ACP)
	$$(call copy-file-to-target)

# The uiautomator.jar
_cpsttrp_src_jar := $(LOCAL_PATH)/$(1)/uiautomator.jar
# The uiautomator library should be referenced as "LOCAL_JAVA_LIBRARIES += uiautomator_sdk_v<version>".
_cpsttrp_sdk_intermediates := $(call intermediates-dir-for,JAVA_LIBRARIES,uiautomator_sdk_v$(1),,COMMON)
$$(_cpsttrp_sdk_intermediates)/classes.jar : $$(_cpsttrp_src_jar) | $(ACP)
	$$(call copy-file-to-target)

$$(_cpsttrp_sdk_intermediates)/javalib.jar : $$(_cpsttrp_sdk_intermediates)/classes.jar | $(ACP)
	$$(call copy-file-to-target)
endef

$(foreach s,$(TARGET_AVAILABLE_SDK_VERSIONS),$(eval $(call _copy_prebuilt_sdk_to_the_right_place,$(s))))

include $(call all-makefiles-under,$(LOCAL_PATH))
