LOCAL_PATH:= $(call my-dir)

########################################
# NXP Configuration
########################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
        $(call all-java-files-under, nxp)

LOCAL_PACKAGE_NAME := Nfc
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := NfcLogTags

LOCAL_REQUIRED_MODULES  := libnfc_jni

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

########################################
# NCI Configuration
########################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
        $(call all-java-files-under, nci)

LOCAL_PACKAGE_NAME := NfcNci
LOCAL_OVERRIDES_PACKAGES := Nfc
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := NfcLogTags

LOCAL_REQUIRED_MODULES  := libnfc_nci_jni

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

#####
# static lib for the log tags
#####
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := src/com/android/nfc/EventLogTags.logtags

LOCAL_MODULE:= NfcLogTags

include $(BUILD_STATIC_JAVA_LIBRARY)


include $(call all-makefiles-under,$(LOCAL_PATH))
