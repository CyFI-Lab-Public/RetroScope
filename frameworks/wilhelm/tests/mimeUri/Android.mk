LOCAL_PATH:= $(call my-dir)

# slesTest_playStates

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTest_playStates.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_playStates

include $(BUILD_EXECUTABLE)

# slesTest_playStreamType

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestPlayStreamType.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_playStreamType

include $(BUILD_EXECUTABLE)

# slesTest_playUri

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestPlayUri.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_playUri

include $(BUILD_EXECUTABLE)

# slesTest_loopUri

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestLoopUri.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_loopUri

include $(BUILD_EXECUTABLE)

# slesTest_playUri2

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestPlayUri2.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_playUri2

include $(BUILD_EXECUTABLE)

# slesTest_slowDownUri

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestSlowDownUri.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_slowDownUri

include $(BUILD_EXECUTABLE)

# slesTest_manyPlayers

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestManyPlayers.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_manyPlayers

include $(BUILD_EXECUTABLE)

# slesTest_getPositionUri

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	slesTestGetPositionUri.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES

LOCAL_CFLAGS += -UNDEBUG

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_MODULE:= slesTest_getPositionUri

include $(BUILD_EXECUTABLE)
