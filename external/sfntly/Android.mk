BASE_PATH := $(call my-dir)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# We default to release for the Android build system. Developers debugging
# code can build with "Debug"
GYP_CONFIGURATION := Release

LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_MODULE := libsfntly
LOCAL_MODULE_SUFFIX := .a
LOCAL_MODULE_TAGS := optional

LOCAL_CPP_EXTENSION := .cc
LOCAL_GENERATED_SOURCES :=

LOCAL_SRC_FILES := \
	cpp/src/sfntly/data/byte_array.cc \
	cpp/src/sfntly/data/font_data.cc \
	cpp/src/sfntly/data/font_input_stream.cc \
	cpp/src/sfntly/data/font_output_stream.cc \
	cpp/src/sfntly/data/growable_memory_byte_array.cc \
	cpp/src/sfntly/data/memory_byte_array.cc \
	cpp/src/sfntly/data/readable_font_data.cc \
	cpp/src/sfntly/data/writable_font_data.cc \
	cpp/src/sfntly/font.cc \
	cpp/src/sfntly/font_factory.cc \
	cpp/src/sfntly/port/file_input_stream.cc \
	cpp/src/sfntly/port/lock.cc \
	cpp/src/sfntly/port/memory_input_stream.cc \
	cpp/src/sfntly/port/memory_output_stream.cc \
	cpp/src/sfntly/table/bitmap/big_glyph_metrics.cc \
	cpp/src/sfntly/table/bitmap/bitmap_glyph.cc \
	cpp/src/sfntly/table/bitmap/bitmap_glyph_info.cc \
	cpp/src/sfntly/table/bitmap/bitmap_size_table.cc \
	cpp/src/sfntly/table/bitmap/composite_bitmap_glyph.cc \
	cpp/src/sfntly/table/bitmap/ebdt_table.cc \
	cpp/src/sfntly/table/bitmap/eblc_table.cc \
	cpp/src/sfntly/table/bitmap/ebsc_table.cc \
	cpp/src/sfntly/table/bitmap/glyph_metrics.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table_format1.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table_format2.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table_format3.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table_format4.cc \
	cpp/src/sfntly/table/bitmap/index_sub_table_format5.cc \
	cpp/src/sfntly/table/bitmap/simple_bitmap_glyph.cc \
	cpp/src/sfntly/table/bitmap/small_glyph_metrics.cc \
	cpp/src/sfntly/table/byte_array_table_builder.cc \
	cpp/src/sfntly/table/core/cmap_table.cc \
	cpp/src/sfntly/table/core/font_header_table.cc \
	cpp/src/sfntly/table/core/horizontal_device_metrics_table.cc \
	cpp/src/sfntly/table/core/horizontal_header_table.cc \
	cpp/src/sfntly/table/core/horizontal_metrics_table.cc \
	cpp/src/sfntly/table/core/maximum_profile_table.cc \
	cpp/src/sfntly/table/core/name_table.cc \
	cpp/src/sfntly/table/core/os2_table.cc \
	cpp/src/sfntly/table/font_data_table.cc \
	cpp/src/sfntly/table/generic_table_builder.cc \
	cpp/src/sfntly/table/header.cc \
	cpp/src/sfntly/table/subtable.cc \
	cpp/src/sfntly/table/table.cc \
	cpp/src/sfntly/table/table_based_table_builder.cc \
	cpp/src/sfntly/table/truetype/glyph_table.cc \
	cpp/src/sfntly/table/truetype/loca_table.cc \
	cpp/src/sfntly/tag.cc \
	cpp/src/sample/chromium/font_subsetter.cc \
	cpp/src/sample/chromium/subsetter_impl.cc


# Flags passed to both C and C++ files.
MY_CFLAGS_Debug := \
	-fstack-protector \
	--param=ssp-buffer-size=4 \
	-Werror \
	-fno-exceptions \
	-fno-strict-aliasing \
	-Wall \
	-Wno-unused-parameter \
	-Wno-missing-field-initializers \
	-fvisibility=hidden \
	-pipe \
	-fPIC \
	-fno-tree-sra \
	-fuse-ld=gold \
	-Wno-psabi \
	-ffunction-sections \
	-funwind-tables \
	-g \
	-fstack-protector \
	-fno-short-enums \
	-finline-limit=64 \
	-Wa,--noexecstack \
	-U_FORTIFY_SOURCE \
	-Wno-extra \
	-Wno-ignored-qualifiers \
	-Wno-type-limits \
	-Os \
	-g \
	-fomit-frame-pointer \
	-fdata-sections \
	-ffunction-sections

MY_DEFS_Debug := \
	'-DANGLE_DX11' \
	'-D_FILE_OFFSET_BITS=64' \
	'-DNO_TCMALLOC' \
	'-DDISCARDABLE_MEMORY_ALWAYS_SUPPORTED_NATIVELY' \
	'-DSYSTEM_NATIVELY_SIGNALS_MEMORY_PRESSURE' \
	'-DDISABLE_NACL' \
	'-DCHROMIUM_BUILD' \
	'-DUSE_LIBJPEG_TURBO=1' \
	'-DUSE_PROPRIETARY_CODECS' \
	'-DENABLE_CONFIGURATION_POLICY' \
	'-DENABLE_GPU=1' \
	'-DUSE_OPENSSL=1' \
	'-DENABLE_EGLIMAGE=1' \
	'-DSFNTLY_NO_EXCEPTION' \
	'-DU_USING_ICU_NAMESPACE=0' \
	'-D__STDC_CONSTANT_MACROS' \
	'-D__STDC_FORMAT_MACROS' \
	'-DANDROID' \
	'-D__GNU_SOURCE=1' \
	'-DUSE_STLPORT=1' \
	'-D_STLP_USE_PTR_SPECIALIZATIONS=1' \
	'-DCHROME_BUILD_ID=""' \
	'-DDYNAMIC_ANNOTATIONS_ENABLED=1' \
	'-DWTF_USE_DYNAMIC_ANNOTATIONS=1' \
	'-D_DEBUG'

# Include paths placed before CFLAGS/CPPFLAGS
LOCAL_C_INCLUDES_Debug := \
	$(LOCAL_PATH)/cpp/src \
	$(LOCAL_PATH) \
	external/icu4c/common \
	external/icu4c/i18n \
	frameworks/wilhelm/include \


# Flags passed to only C++ (and not C) files.
LOCAL_CPPFLAGS_Debug := \
	-fno-rtti \
	-fno-threadsafe-statics \
	-fvisibility-inlines-hidden \
	-Wsign-compare \
	-Wno-abi \
	-Wno-error=c++0x-compat \
	-Wno-non-virtual-dtor \
	-Wno-sign-promo


# Flags passed to both C and C++ files.
MY_CFLAGS_Release := \
	-fstack-protector \
	--param=ssp-buffer-size=4 \
	-Werror \
	-fno-exceptions \
	-fno-strict-aliasing \
	-Wall \
	-Wno-unused-parameter \
	-Wno-missing-field-initializers \
	-fvisibility=hidden \
	-pipe \
	-fPIC \
	-fno-tree-sra \
	-fuse-ld=gold \
	-Wno-psabi \
	-ffunction-sections \
	-funwind-tables \
	-g \
	-fstack-protector \
	-fno-short-enums \
	-finline-limit=64 \
	-Wa,--noexecstack \
	-U_FORTIFY_SOURCE \
	-Wno-extra \
	-Wno-ignored-qualifiers \
	-Wno-type-limits \
	-Os \
	-fno-ident \
	-fdata-sections \
	-ffunction-sections \
	-fomit-frame-pointer

MY_DEFS_Release := \
	'-DANGLE_DX11' \
	'-D_FILE_OFFSET_BITS=64' \
	'-DNO_TCMALLOC' \
	'-DDISCARDABLE_MEMORY_ALWAYS_SUPPORTED_NATIVELY' \
	'-DSYSTEM_NATIVELY_SIGNALS_MEMORY_PRESSURE' \
	'-DDISABLE_NACL' \
	'-DCHROMIUM_BUILD' \
	'-DUSE_LIBJPEG_TURBO=1' \
	'-DUSE_PROPRIETARY_CODECS' \
	'-DENABLE_CONFIGURATION_POLICY' \
	'-DENABLE_GPU=1' \
	'-DUSE_OPENSSL=1' \
	'-DENABLE_EGLIMAGE=1' \
	'-DSFNTLY_NO_EXCEPTION' \
	'-DU_USING_ICU_NAMESPACE=0' \
	'-D__STDC_CONSTANT_MACROS' \
	'-D__STDC_FORMAT_MACROS' \
	'-DANDROID' \
	'-D__GNU_SOURCE=1' \
	'-DUSE_STLPORT=1' \
	'-D_STLP_USE_PTR_SPECIALIZATIONS=1' \
	'-DCHROME_BUILD_ID=""' \
	'-DNDEBUG' \
	'-DNVALGRIND' \
	'-DDYNAMIC_ANNOTATIONS_ENABLED=0' \
	'-D_FORTIFY_SOURCE=2'


# Include paths placed before CFLAGS/CPPFLAGS
LOCAL_C_INCLUDES_Release := \
	$(LOCAL_PATH)/cpp/src \
	$(LOCAL_PATH) \
	external/icu4c/common \
	external/icu4c/i18n \
	frameworks/wilhelm/include \


# Flags passed to only C++ (and not C) files.
LOCAL_CPPFLAGS_Release := \
	-fno-rtti \
	-fno-threadsafe-statics \
	-fvisibility-inlines-hidden \
	-Wsign-compare \
	-Wno-abi \
	-Wno-error=c++0x-compat \
	-Wno-non-virtual-dtor \
	-Wno-sign-promo

LOCAL_CFLAGS := $(MY_CFLAGS_$(GYP_CONFIGURATION)) $(MY_DEFS_$(GYP_CONFIGURATION))
LOCAL_C_INCLUDES := $(LOCAL_C_INCLUDES_$(GYP_CONFIGURATION))
LOCAL_CPPFLAGS := $(LOCAL_CPPFLAGS_$(GYP_CONFIGURATION))
### Rules for final target.

LOCAL_LDFLAGS_Debug := \
	-Wl,-z,now \
	-Wl,-z,relro \
	-Wl,-z,noexecstack \
	-fPIC \
	-Wl,-z,relro \
	-Wl,-z,now \
	-fuse-ld=gold \
	-nostdlib \
	-Wl,--no-undefined \
	-Wl,--exclude-libs=ALL \
	-Wl,--icf=safe \
	-Wl,--gc-sections \
	-Wl,-O1 \
	-Wl,--as-needed


LOCAL_LDFLAGS_Release := \
	-Wl,-z,now \
	-Wl,-z,relro \
	-Wl,-z,noexecstack \
	-fPIC \
	-Wl,-z,relro \
	-Wl,-z,now \
	-fuse-ld=gold \
	-nostdlib \
	-Wl,--no-undefined \
	-Wl,--exclude-libs=ALL \
	-Wl,--icf=safe \
	-Wl,-O1 \
	-Wl,--as-needed \
	-Wl,--gc-sections


LOCAL_LDFLAGS := $(LOCAL_LDFLAGS_$(GYP_CONFIGURATION))

LOCAL_STATIC_LIBRARIES :=

# Enable grouping to fix circular references
LOCAL_GROUP_STATIC_LIBRARIES := true

LOCAL_SHARED_LIBRARIES := \
	libstlport \
	libdl \

include external/stlport/libstlport.mk

include $(BUILD_STATIC_LIBRARY)