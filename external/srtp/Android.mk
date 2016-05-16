# This is the Android makefile for google3/third_party/libsrtp so that we can
# build it with the Android NDK.

LOCAL_PATH:= $(call my-dir)

common_SRC_FILES := \
    srtp/srtp.c \
    srtp/ekt.c \
    crypto/cipher/cipher.c \
    crypto/cipher/null_cipher.c \
    crypto/cipher/aes.c \
    crypto/cipher/aes_icm.c \
    crypto/cipher/aes_cbc.c \
    crypto/hash/null_auth.c \
    crypto/hash/sha1.c \
    crypto/hash/hmac.c \
    crypto/hash/auth.c \
    crypto/math/datatypes.c \
    crypto/math/stat.c \
    crypto/rng/rand_source.c \
    crypto/rng/prng.c \
    crypto/rng/ctr_prng.c \
    crypto/kernel/err.c \
    crypto/kernel/crypto_kernel.c \
    crypto/kernel/alloc.c \
    crypto/kernel/key.c \
    crypto/ae_xfm/xfm.c \
    crypto/replay/rdb.c \
    crypto/replay/rdbx.c

common_CFLAGS := \
    -DPOSIX -iquote$(LOCAL_PATH)/crypto/include \
    -Werror \
    -Wno-ignored-qualifiers \
    -Wno-sign-compare \
    -Wno-missing-field-initializers

common_C_INCLUDES = $(LOCAL_PATH)/include

# For the device
# =====================================================
# Device static library

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),arm)
    LOCAL_SDK_VERSION := 9
endif


LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)

LOCAL_MODULE:= libsrtp_static
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)
