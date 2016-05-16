
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  benchmain.cpp \
  SkBenchmark.cpp \
  BenchTimer.cpp \
  BenchSysTimer_posix.cpp \
  BenchGpuTimer_gl.cpp \
  SkBenchLogger.cpp \
  TimerData.cpp \
  ../tools/flags/SkCommandLineFlags.cpp

LOCAL_SRC_FILES += \
  AAClipBench.cpp \
  BicubicBench.cpp \
  BitmapBench.cpp \
  BitmapRectBench.cpp \
  BitmapScaleBench.cpp \
  BlurBench.cpp \
  BlurImageFilterBench.cpp \
  BlurRectBench.cpp \
  ChartBench.cpp \
  ChromeBench.cpp \
  CmapBench.cpp \
  ColorFilterBench.cpp \
  DashBench.cpp \
  DecodeBench.cpp \
  DeferredCanvasBench.cpp \
  DisplacementBench.cpp \
  FontCacheBench.cpp \
  FontScalerBench.cpp \
  FSRectBench.cpp \
  GameBench.cpp \
  GradientBench.cpp \
  GrMemoryPoolBench.cpp \
  ImageCacheBench.cpp \
  ImageDecodeBench.cpp \
  InterpBench.cpp \
  HairlinePathBench.cpp \
  LineBench.cpp \
  LightingBench.cpp \
  MagnifierBench.cpp \
  MathBench.cpp \
  Matrix44Bench.cpp \
  MatrixBench.cpp \
  MatrixConvolutionBench.cpp \
  MemoryBench.cpp \
  MemsetBench.cpp \
  MergeBench.cpp \
  MorphologyBench.cpp \
  MutexBench.cpp \
  PathBench.cpp \
  PathIterBench.cpp \
  PathUtilsBench.cpp \
  PerlinNoiseBench.cpp \
  PicturePlaybackBench.cpp \
  PictureRecordBench.cpp \
  ReadPixBench.cpp \
  PremulAndUnpremulAlphaOpsBench.cpp \
  RectBench.cpp \
  RectoriBench.cpp \
  RefCntBench.cpp \
  RegionBench.cpp \
  RegionContainBench.cpp \
  RepeatTileBench.cpp \
  RTreeBench.cpp \
  ScalarBench.cpp \
  ShaderMaskBench.cpp \
  SortBench.cpp \
  StrokeBench.cpp \
  TableBench.cpp \
  TextBench.cpp \
  TileBench.cpp \
  VertBench.cpp \
  WriterBench.cpp \
  XfermodeBench.cpp

# Disabling this bench since it depends on recent
# changes to bench.
# SkipZeroesBench.cpp \

# Files that are missing dependencies
#LOCAL_SRC_FILES += \
#  ChecksumBench.cpp \
#  DeferredSurfaceCopyBench.cpp \

LOCAL_SHARED_LIBRARIES := libcutils libskia libGLESv2 libEGL 

LOCAL_STATIC_LIBRARIES := libstlport_static

LOCAL_C_INCLUDES := \
  external/skia/src/core \
  external/skia/src/effects \
  external/skia/src/utils \
  external/skia/src/gpu \
  external/skia/tools/flags

LOCAL_MODULE := skia_bench

LOCAL_MODULE_TAGS := optional

#include stlport headers
include external/stlport/libstlport.mk

include $(BUILD_EXECUTABLE)
