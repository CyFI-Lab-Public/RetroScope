LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := name/fraser/neil/plaintext/diff_match_patch.java

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := diff_match_patch

include $(BUILD_STATIC_JAVA_LIBRARY)