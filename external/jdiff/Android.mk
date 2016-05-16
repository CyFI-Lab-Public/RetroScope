LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_CLASSPATH := $(HOST_JDK_TOOLS_JAR)

LOCAL_MODULE := jdiff

include $(BUILD_HOST_JAVA_LIBRARY)
