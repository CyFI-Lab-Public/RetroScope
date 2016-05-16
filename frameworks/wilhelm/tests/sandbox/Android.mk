LOCAL_PATH:= $(call my-dir)

# intbufq

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
    intbufq.c \
	getch.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
	#LOCAL_SHARED_LIBRARIES += librt
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_intbufq

include $(BUILD_EXECUTABLE)

# multiplay

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	multiplay.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_multiplay

include $(BUILD_EXECUTABLE)

# engine

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm) \
	$(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES:= \
	engine.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_engine

include $(BUILD_EXECUTABLE)

# object

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm) \
	$(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES:= \
	object.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_object

include $(BUILD_EXECUTABLE)

# configbq

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	configbq.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_configbq

include $(BUILD_EXECUTABLE)

# reverb

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	reverb.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_reverb

include $(BUILD_EXECUTABLE)

# srcsink

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	srcsink.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_srcsink

include $(BUILD_EXECUTABLE)

# outputmix

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm) \
	$(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES:= \
	outputmix.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_outputmix

include $(BUILD_EXECUTABLE)

# urimime

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	urimime.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_urimime

include $(BUILD_EXECUTABLE)

# dim

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	dim.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_dim

include $(BUILD_EXECUTABLE)

# multithread

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	multithread.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_multithread

include $(BUILD_EXECUTABLE)

# playbq

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm) \
	$(call include-path-for, audio-utils)

LOCAL_SRC_FILES:= \
	playbq.cpp

LOCAL_SHARED_LIBRARIES := \
	libaudioutils \
	libnbaio \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    libsndfile \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_playbq

include $(BUILD_EXECUTABLE)

# monkey

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	monkey.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_monkey

include $(BUILD_EXECUTABLE)

# xa

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	xa.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenMAXAL

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_xa

include $(BUILD_EXECUTABLE)

# dual

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	dual.c

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenSLES \
	libOpenMAXAL

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= slesTest_dual

include $(BUILD_EXECUTABLE)

# xaplay

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
	$(call include-path-for, wilhelm)

LOCAL_SRC_FILES:= \
	xaplay.c nativewindow.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	liblog \
	libOpenMAXAL \
    libgui \
    libbinder \
    libandroid

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    liblog

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
endif

LOCAL_CFLAGS += -UNDEBUG

LOCAL_MODULE:= xaplay

include $(BUILD_EXECUTABLE)
