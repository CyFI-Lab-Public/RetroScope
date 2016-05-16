LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_SIMULATOR),true)
  $(error This makefile must not be included when building the simulator)
endif

ifeq ($(WPA_SUPPLICANT_VERSION),VER_0_6_X)
    WPA_SUPPL_DIR = external/wpa_supplicant_6/wpa_supplicant
endif

ifeq ($(WPA_SUPPLICANT_VERSION),VER_0_8_X)
    WPA_SUPPL_DIR = external/wpa_supplicant_8/wpa_supplicant
endif

include $(WPA_SUPPL_DIR)/android.config

ifneq ($(BOARD_WPA_SUPPLICANT_DRIVER),)
  CONFIG_DRIVER_$(BOARD_WPA_SUPPLICANT_DRIVER) := y
endif

L_CFLAGS = -DCONFIG_DRIVER_CUSTOM -DWPA_SUPPLICANT_$(WPA_SUPPLICANT_VERSION)
L_SRC :=

ifdef CONFIG_NO_STDOUT_DEBUG
L_CFLAGS += -DCONFIG_NO_STDOUT_DEBUG
endif

ifdef CONFIG_DEBUG_FILE
L_CFLAGS += -DCONFIG_DEBUG_FILE
endif

ifdef CONFIG_ANDROID_LOG
L_CFLAGS += -DCONFIG_ANDROID_LOG
endif

ifdef CONFIG_IEEE8021X_EAPOL
L_CFLAGS += -DIEEE8021X_EAPOL
endif

ifdef CONFIG_WPS
L_CFLAGS += -DCONFIG_WPS
endif

ifdef CONFIG_DRIVER_WEXT
L_SRC += driver_mac80211.c
endif

ifdef CONFIG_DRIVER_NL80211
L_SRC += driver_mac80211_nl.c
endif

INCLUDES = $(WPA_SUPPL_DIR) \
    $(WPA_SUPPL_DIR)/src \
    $(WPA_SUPPL_DIR)/src/common \
    $(WPA_SUPPL_DIR)/src/drivers \
    $(WPA_SUPPL_DIR)/src/l2_packet \
    $(WPA_SUPPL_DIR)/src/utils \
    $(WPA_SUPPL_DIR)/src/wps \
    external/libnl-headers

include $(CLEAR_VARS)
LOCAL_MODULE := lib_driver_cmd_wl12xx
LOCAL_MODULE_TAGS := eng
LOCAL_SHARED_LIBRARIES := libc libcutils
LOCAL_STATIC_LIBRARIES := libnl_2
LOCAL_CFLAGS := $(L_CFLAGS)
LOCAL_SRC_FILES := $(L_SRC)
LOCAL_C_INCLUDES := $(INCLUDES)
include $(BUILD_STATIC_LIBRARY)
