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

# build only for linux
ifeq ($(HOST_OS),linux)

CTS_AUDIO_TOP:= $(call my-dir)

CTS_AUDIO_INSTALL_DIR := $(HOST_OUT)/cts-audio-quality/android-cts-audio-quality
CTS_AUDIO_QUALITY_ZIP := $(HOST_OUT)/cts-audio-quality/android-cts-audio-quality.zip

cts_audio_quality_client_apk := $(TARGET_OUT_DATA_APPS)/CtsAudioClient.apk
cts_audio_quality_host_bins := $(HOST_OUT)/bin/cts_audio_quality_test $(HOST_OUT)/bin/cts_audio_quality
$(CTS_AUDIO_QUALITY_ZIP): PRIVATE_CLIENT_APK := $(cts_audio_quality_client_apk)
$(CTS_AUDIO_QUALITY_ZIP): PRIVATE_HOST_BINS := $(cts_audio_quality_host_bins)
$(CTS_AUDIO_QUALITY_ZIP): PRIVATE_TEST_DESC := $(CTS_AUDIO_TOP)/test_description
$(CTS_AUDIO_QUALITY_ZIP): $(cts_audio_quality_client_apk) $(cts_audio_quality_host_bins) \
    $(CTS_AUDIO_TOP)/test_description | $(ACP)
	$(hide) mkdir -p $(CTS_AUDIO_INSTALL_DIR)/client
	$(hide) $(ACP) -fp $(PRIVATE_CLIENT_APK) \
        $(CTS_AUDIO_INSTALL_DIR)/client
	$(hide) $(ACP) -fp $(PRIVATE_HOST_BINS) $(CTS_AUDIO_INSTALL_DIR)
	$(hide) $(ACP) -fr $(PRIVATE_TEST_DESC) $(CTS_AUDIO_INSTALL_DIR)
	$(hide) echo "Package cts_audio: $@"
	$(hide) cd $(dir $@) && \
        zip -rq $(notdir $@) android-cts-audio-quality -x android-cts-audio-quality/reports/\*

cts_audio_quality_client_apk :=
cts_audio_quality_host_bins :=

# target to build only this package
.PHONY: cts_audio_quality_package
cts_audio_quality_package: $(CTS_AUDIO_QUALITY_ZIP)

cts: $(CTS_AUDIO_QUALITY_ZIP)

ifneq ($(filter cts, $(MAKECMDGOALS)),)
$(call dist-for-goals, cts, $(CTS_AUDIO_QUALITY_ZIP))
endif # cts

include $(call all-subdir-makefiles)

endif # linux
