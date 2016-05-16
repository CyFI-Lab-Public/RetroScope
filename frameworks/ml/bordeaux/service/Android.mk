LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
        src/android/bordeaux/services/IBordeauxServiceCallback.aidl \
        src/android/bordeaux/services/IAggregatorManager.aidl \
        src/android/bordeaux/services/ILearning_MulticlassPA.aidl \
        src/android/bordeaux/services/IPredictor.aidl \
        src/android/bordeaux/services/ILearning_StochasticLinearRanker.aidl \
        src/android/bordeaux/services/IBordeauxService.aidl \

LOCAL_STATIC_JAVA_LIBRARIES := bordeaux_learners
LOCAL_JNI_SHARED_LIBRARIES := libbordeaux
LOCAL_REQUIRED_MODULES := libbordeaux

LOCAL_PACKAGE_NAME := bordeaux


include $(BUILD_PACKAGE)

##
# Build java lib
##
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES += \
        src/android/bordeaux/services/IntFloat.java \
        src/android/bordeaux/services/StringFloat.java \
        src/android/bordeaux/services/StringString.java \
        src/android/bordeaux/services/BordeauxClassifier.java \
        src/android/bordeaux/services/BordeauxRanker.java \
        src/android/bordeaux/services/BordeauxPredictor.java \
        src/android/bordeaux/services/BordeauxAggregatorManager.java \
        src/android/bordeaux/services/BordeauxManagerService.java \
        src/android/bordeaux/services/IBordeauxServiceCallback.aidl \
        src/android/bordeaux/services/IAggregatorManager.aidl \
        src/android/bordeaux/services/IPredictor.aidl \
        src/android/bordeaux/services/ILearning_MulticlassPA.aidl \
        src/android/bordeaux/services/ILearning_StochasticLinearRanker.aidl \
        src/android/bordeaux/services/IBordeauxService.aidl \

LOCAL_MODULE :=  bordeaux_service
LOCAL_STATIC_JAVA_LIBRARIES := bordeaux_learners

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_STATIC_JAVA_LIBRARY)

## Building the whole Bordeaux service
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
        src/android/bordeaux/services/IBordeauxServiceCallback.aidl \
        src/android/bordeaux/services/IAggregatorManager.aidl \
        src/android/bordeaux/services/ILearning_MulticlassPA.aidl \
        src/android/bordeaux/services/IPredictor.aidl \
        src/android/bordeaux/services/ILearning_StochasticLinearRanker.aidl \
        src/android/bordeaux/services/IBordeauxService.aidl \

LOCAL_STATIC_JAVA_LIBRARIES := bordeaux_learners
LOCAL_JNI_SHARED_LIBRARIES := libbordeaux

LOCAL_JAVA_RESOURCE_DIRS := res
LOCAL_MODULE := bordeaux_whole_service
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_STATIC_JAVA_LIBRARY)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
