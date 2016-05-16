LOCAL_PATH:= $(call my-dir)

# =====================================================
# Static library: libmcldLD
# =====================================================

mcld_ld_SRC_FILES := \
  Archive.cpp \
  ArchiveReader.cpp \
  BranchIsland.cpp  \
  BranchIslandFactory.cpp  \
  DWARFLineInfo.cpp \
  Diagnostic.cpp  \
  DiagnosticEngine.cpp  \
  DiagnosticInfos.cpp \
  DiagnosticLineInfo.cpp  \
  DiagnosticPrinter.cpp \
  DynObjReader.cpp  \
  ELFBinaryReader.cpp  \
  ELFSegment.cpp  \
  ELFSegmentFactory.cpp \
  EhFrame.cpp \
  EhFrameHdr.cpp  \
  EhFrameReader.cpp  \
  GroupReader.cpp \
  LDContext.cpp \
  LDFileFormat.cpp  \
  LDReader.cpp  \
  LDSection.cpp \
  LDSymbol.cpp  \
  MsgHandler.cpp  \
  NamePool.cpp  \
  ObjectWriter.cpp  \
  RelocData.cpp  \
  RelocationFactory.cpp \
  Relocator.cpp \
  ResolveInfo.cpp \
  Resolver.cpp  \
  SectionData.cpp \
  SectionSymbolSet.cpp \
  StaticResolver.cpp  \
  StubFactory.cpp  \
  TextDiagnosticPrinter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_SRC_FILES)
LOCAL_MODULE:= libmcldLD

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_SRC_FILES)
LOCAL_MODULE:= libmcldLD

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

# =====================================================
# Static library: libmcldLDVariant
# =====================================================

mcld_ld_variant_SRC_FILES := \
  BSDArchiveReader.cpp  \
  GNUArchiveReader.cpp  \
  ELFDynObjFileFormat.cpp \
  ELFDynObjReader.cpp \
  ELFExecFileFormat.cpp \
  ELFFileFormat.cpp \
  ELFObjectReader.cpp \
  ELFObjectWriter.cpp \
  ELFReader.cpp \
  ELFReaderIf.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_variant_SRC_FILES)
LOCAL_MODULE:= libmcldLDVariant

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_ld_variant_SRC_FILES)
LOCAL_MODULE:= libmcldLDVariant

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
