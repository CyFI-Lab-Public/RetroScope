# Don't build the library in unbundled branches.
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH:= $(call my-dir)

LOCAL_IS_HOST_MODULE := true

LOCAL_MODULE:= libclang

LOCAL_MODULE_TAGS := optional

LOCAL_WHOLE_STATIC_LIBRARIES := \
	libclangDriver \
	libclangParse \
	libclangSema \
	libclangAnalysis \
	libclangCodeGen \
	libclangAST \
	libclangEdit \
	libclangLex \
	libclangFrontend \
	libclangBasic \
	libclangSerialization

LOCAL_SHARED_LIBRARIES := libLLVM

ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_LDLIBS := -ldl -lpthread
endif

include $(CLANG_HOST_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)

endif # don't build in unbundled branches