# Makefile to build the Windows SDK Tools under linux.
#
# This makefile is included by development/build/tools/windows_sdk.mk
# to device which tools we want to build from the sdk.git project.

# Define the list of tool-dependent modules to build for the Windows SDK.
# All these will be build using USE_MINGW=1
WIN_SDK_TARGETS := \
	avdlauncher \
	emulator \
	emulator-arm \
	emulator-x86 \
	emulator-mips \
	find_java \
	find_lock \
	mksdcard \
	monitor \
	sdklauncher


# Add OpenGLES emulation host libraries if needed.
ifeq (true,$(BUILD_EMULATOR_OPENGL))
WIN_SDK_TARGETS += \
	libOpenglRender \
	libGLES_CM_translator \
	libGLES_V2_translator \
	libEGL_translator
endif

# Define the list of tool-dependent modules requisites needed
# for the Windows SDK. These will be built using HOST_OS=linux.
WIN_SDK_BUILD_PREREQ := \
	monitor

