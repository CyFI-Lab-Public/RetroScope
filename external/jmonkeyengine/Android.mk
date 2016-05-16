LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, \
    engine/src/android \
    engine/src/core \
    engine/src/core-plugins \
    engine/src/ogre)

LOCAL_MODULE := jmonkeyengine
LOCAL_SDK_VERSION := 9
LOCAL_MODULE_TAGS := optional

# jMonkeyEngine needs these resources, but they will eventually get
# stripped out even if they're compiled into the jar. You will need
# to duplicate them into your project's assets. See the README for
# more info.
# LOCAL_JAVA_RESOURCE_DIRS := engine/src/core-data

include $(BUILD_STATIC_JAVA_LIBRARY)
