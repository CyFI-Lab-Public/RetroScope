#
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

LOCAL_SRC_FILES := \
	src/racoon/algorithm.c \
	src/racoon/crypto_openssl.c \
	src/racoon/genlist.c \
	src/racoon/handler.c \
	src/racoon/isakmp.c \
	src/racoon/isakmp_agg.c \
	src/racoon/isakmp_base.c \
	src/racoon/isakmp_cfg.c \
	src/racoon/isakmp_frag.c \
	src/racoon/isakmp_ident.c \
	src/racoon/isakmp_inf.c \
	src/racoon/isakmp_newg.c \
	src/racoon/isakmp_quick.c \
	src/racoon/isakmp_unity.c \
	src/racoon/isakmp_xauth.c \
	src/racoon/ipsec_doi.c \
	src/racoon/nattraversal.c \
	src/racoon/oakley.c \
	src/racoon/pfkey.c \
	src/racoon/policy.c \
	src/racoon/proposal.c \
	src/racoon/remoteconf.c \
	src/racoon/schedule.c \
	src/racoon/sockmisc.c \
	src/racoon/str2val.c \
	src/racoon/strnames.c \
	src/racoon/vendorid.c \
	src/racoon/vmbuf.c \
	main.c \
	setup.c

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/src/include-glibc \
	$(LOCAL_PATH)/src/libipsec \
	$(LOCAL_PATH)/src/racoon \
	$(LOCAL_PATH)/src/racoon/missing \
	external/openssl/include

LOCAL_STATIC_LIBRARIES := libipsec

LOCAL_SHARED_LIBRARIES := libcutils liblog libcrypto

LOCAL_CFLAGS := -DANDROID_CHANGES -DHAVE_CONFIG_H -DHAVE_OPENSSL_ENGINE_H

LOCAL_CFLAGS += -Wno-sign-compare -Wno-missing-field-initializers

LOCAL_MODULE := racoon

include $(BUILD_EXECUTABLE)

##########################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	src/libipsec/pfkey.c \
	src/libipsec/ipsec_strerror.c

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DHAVE_OPENSSL_ENGINE_H

LOCAL_CFLAGS += -Wno-sign-compare -Wno-missing-field-initializers

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/src/include-glibc \
	$(LOCAL_PATH)/src/libipsec

LOCAL_MODULE := libipsec

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)
