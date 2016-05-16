BASE_PATH := $(call my-dir)
LOCAL_PATH:= $(call my-dir)

###############################################################################
#
# PROBLEMS WITH SKIA DEBUGGING?? READ THIS...
#
# The debug build results in changes to the Skia headers. This means that those
# using libskia must also be built with the debug version of the Skia headers.
# There are a few scenarios where this comes into play:
#
# (1) You're building debug code that depends on libskia.
#   (a) If libskia is built in release, then define SK_RELEASE when building
#       your sources.
#   (b) If libskia is built with debugging (see step 2), then no changes are
#       needed since your sources and libskia have been built with SK_DEBUG.
# (2) You're building libskia in debug mode.
#   (a) RECOMMENDED: You can build the entire system in debug mode. Do this by
#       updating your build/config.mk to include -DSK_DEBUG on the line that
#       defines COMMON_GLOBAL_CFLAGS
#   (b) You can update all the users of libskia to define SK_DEBUG when they are
#       building their sources.
#
# NOTE: If neither SK_DEBUG or SK_RELEASE are defined then Skia checks NDEBUG to
#       determine which build type to use.
###############################################################################


#############################################################
#   build the skia+fretype+png+jpeg+zlib+gif+webp library
#

include $(CLEAR_VARS)

LOCAL_ARM_MODE := thumb

# need a flag to tell the C side when we're on devices with large memory
# budgets (i.e. larger than the low-end devices that initially shipped)

ifeq ($(ARCH_ARM_HAVE_VFP),true)
	LOCAL_CFLAGS += -DANDROID_LARGE_MEMORY_DEVICE
endif

ifeq ($(TARGET_ARCH),x86)
	LOCAL_CFLAGS += -DANDROID_LARGE_MEMORY_DEVICE
endif

ifneq ($(ARCH_ARM_HAVE_VFP),true)
	LOCAL_CFLAGS += -DSK_SOFTWARE_FLOAT
endif

ifeq ($(ARCH_ARM_HAVE_NEON),true)
	LOCAL_CFLAGS += -D__ARM_HAVE_NEON
endif

LOCAL_CFLAGS += -DDCT_IFAST_SUPPORTED

# using freetype's embolden allows us to adjust fake bold settings at
# draw-time, at which point we know which SkTypeface is being drawn
LOCAL_CFLAGS += -DSK_USE_FREETYPE_EMBOLDEN

#Android provides at least FreeType 2.4.0 at runtime.
LOCAL_CFLAGS += -DSK_FONTHOST_FREETYPE_RUNTIME_VERSION=0x020400

#Skia should not use dlopen on Android.
LOCAL_CFLAGS += -DSK_CAN_USE_DLOPEN=0

# used for testing
#LOCAL_CFLAGS += -g -O0

ifeq ($(NO_FALLBACK_FONT),true)
	LOCAL_CFLAGS += -DNO_FALLBACK_FONT
endif

# IMPORTANT: The build order of the SkImageDecoder_*.cpp files
# defines the order image decoders are tested when decoding a
# stream. The last decoder is the first one tested, so the .cpp
# files should be in listed in order from the least likely to be
# used, to the most likely (jpeg and png should be the last two
# for instance.)
# START IMAGE DECODERS

LOCAL_SRC_FILES:= \
	src/core/Sk64.cpp \
	src/core/SkAnnotation.cpp \
	src/core/SkAAClip.cpp \
	src/core/SkAdvancedTypefaceMetrics.cpp \
	src/core/SkAlphaRuns.cpp \
	src/core/SkBBoxHierarchy.cpp \
	src/core/SkBBoxRecord.cpp \
	src/core/SkBBoxHierarchyRecord.cpp \
	src/core/SkBitmap.cpp \
	src/core/SkBitmapFilter.cpp \
	src/core/SkBitmapHeap.cpp \
	src/core/SkBitmapProcShader.cpp \
	src/core/SkBitmapProcState.cpp \
	src/core/SkBitmapProcState_matrixProcs.cpp \
	src/core/SkBitmapScaler.cpp \
	src/core/SkBitmap_scroll.cpp \
	src/core/SkBlitMask_D32.cpp \
	src/core/SkBlitRow_D16.cpp \
	src/core/SkBlitRow_D32.cpp \
	src/core/SkBlitter.cpp \
	src/core/SkBlitter_A1.cpp \
	src/core/SkBlitter_A8.cpp \
	src/core/SkBlitter_ARGB32.cpp \
	src/core/SkBlitter_RGB16.cpp \
	src/core/SkBlitter_Sprite.cpp \
	src/core/SkBuffer.cpp \
	src/core/SkCanvas.cpp \
	src/core/SkChunkAlloc.cpp \
	src/core/SkClipStack.cpp \
	src/core/SkColor.cpp \
	src/core/SkColorFilter.cpp \
	src/core/SkColorTable.cpp \
	src/core/SkComposeShader.cpp \
	src/core/SkConfig8888.cpp \
	src/core/SkConvolver.cpp \
	src/core/SkCordic.cpp \
	src/core/SkCubicClipper.cpp \
	src/core/SkData.cpp \
	src/core/SkDataTable.cpp \
	src/core/SkDebug.cpp \
	src/core/SkDeque.cpp \
	src/core/SkDevice.cpp \
        src/core/SkDeviceLooper.cpp \
	src/core/SkDeviceProfile.cpp \
	src/core/SkDither.cpp \
	src/core/SkDraw.cpp \
	src/core/SkDrawLooper.cpp \
	src/core/SkEdgeBuilder.cpp \
	src/core/SkEdgeClipper.cpp \
	src/core/SkEdge.cpp \
	src/core/SkError.cpp \
	src/core/SkFilterProc.cpp \
	src/core/SkFilterShader.cpp \
	src/core/SkFlattenable.cpp \
	src/core/SkFlattenableBuffers.cpp \
	src/core/SkFlattenableSerialization.cpp \
	src/core/SkFloat.cpp \
	src/core/SkFloatBits.cpp \
	src/core/SkFontDescriptor.cpp \
	src/core/SkFontStream.cpp \
	src/core/SkFontHost.cpp \
	src/core/SkGeometry.cpp \
	src/core/SkGlyphCache.cpp \
	src/core/SkGraphics.cpp \
	src/core/SkInstCnt.cpp \
	src/core/SkImageFilter.cpp \
	src/core/SkImageFilterUtils.cpp \
	src/core/SkLineClipper.cpp \
	src/core/SkMallocPixelRef.cpp \
	src/core/SkMask.cpp \
	src/core/SkMaskFilter.cpp \
	src/core/SkMaskGamma.cpp \
	src/core/SkMath.cpp \
	src/core/SkMatrix.cpp \
	src/core/SkMetaData.cpp \
	src/core/SkMipMap.cpp \
	src/core/SkOrderedReadBuffer.cpp \
	src/core/SkOrderedWriteBuffer.cpp \
	src/core/SkPackBits.cpp \
	src/core/SkPaint.cpp \
	src/core/SkPaintOptionsAndroid.cpp \
	src/core/SkPaintPriv.cpp \
	src/core/SkPath.cpp \
	src/core/SkPathEffect.cpp \
	src/core/SkPathHeap.cpp \
	src/core/SkPathMeasure.cpp \
	src/core/SkPicture.cpp \
	src/core/SkPictureFlat.cpp \
	src/core/SkPicturePlayback.cpp \
	src/core/SkPictureRecord.cpp \
	src/core/SkPictureStateTree.cpp \
	src/core/SkPixelRef.cpp \
	src/core/SkPoint.cpp \
	src/core/SkProcSpriteBlitter.cpp \
	src/core/SkPtrRecorder.cpp \
	src/core/SkQuadClipper.cpp \
	src/core/SkRasterClip.cpp \
	src/core/SkRasterizer.cpp \
	src/core/SkRect.cpp \
	src/core/SkRefCnt.cpp \
	src/core/SkRefDict.cpp \
	src/core/SkRegion.cpp \
	src/core/SkRegion_path.cpp \
	src/core/SkRRect.cpp \
	src/core/SkRTree.cpp \
	src/core/SkScaledImageCache.cpp \
	src/core/SkScalar.cpp \
	src/core/SkScalerContext.cpp \
	src/core/SkScan.cpp \
	src/core/SkScan_AntiPath.cpp \
	src/core/SkScan_Antihair.cpp \
	src/core/SkScan_Hairline.cpp \
	src/core/SkScan_Path.cpp \
	src/core/SkShader.cpp \
	src/core/SkSpriteBlitter_ARGB32.cpp \
	src/core/SkSpriteBlitter_RGB16.cpp \
	src/core/SkStream.cpp \
	src/core/SkString.cpp \
	src/core/SkStringUtils.cpp \
	src/core/SkStroke.cpp \
	src/core/SkStrokeRec.cpp \
	src/core/SkStrokerPriv.cpp \
	src/core/SkTileGrid.cpp \
	src/core/SkTileGridPicture.cpp \
	src/core/SkTLS.cpp \
	src/core/SkTSearch.cpp \
	src/core/SkTypeface.cpp \
	src/core/SkTypefaceCache.cpp \
	src/core/SkUnPreMultiply.cpp \
	src/core/SkUtils.cpp \
	src/core/SkFlate.cpp \
	src/core/SkWriter32.cpp \
	src/core/SkXfermode.cpp \
	src/doc/SkDocument.cpp \
	src/doc/SkDocument_PDF.cpp \
	src/effects/Sk1DPathEffect.cpp \
	src/effects/Sk2DPathEffect.cpp \
	src/effects/SkArithmeticMode.cpp \
	src/effects/SkAvoidXfermode.cpp \
	src/effects/SkBicubicImageFilter.cpp \
	src/effects/SkBitmapSource.cpp \
	src/effects/SkBlurDrawLooper.cpp \
	src/effects/SkBlurImageFilter.cpp \
	src/effects/SkBlurMask.cpp \
	src/effects/SkBlurMaskFilter.cpp \
	src/effects/SkColorFilterImageFilter.cpp \
	src/effects/SkColorFilters.cpp \
	src/effects/SkColorMatrix.cpp \
	src/effects/SkColorMatrixFilter.cpp \
	src/effects/SkComposeImageFilter.cpp \
	src/effects/SkCornerPathEffect.cpp \
	src/effects/SkDashPathEffect.cpp \
	src/effects/SkDiscretePathEffect.cpp \
	src/effects/SkDisplacementMapEffect.cpp \
	src/effects/SkDropShadowImageFilter.cpp \
	src/effects/SkEmbossMask.cpp \
	src/effects/SkEmbossMaskFilter.cpp \
	src/effects/SkGpuBlurUtils.cpp \
	src/effects/SkKernel33MaskFilter.cpp \
	src/effects/SkLayerDrawLooper.cpp \
	src/effects/SkLayerRasterizer.cpp \
	src/effects/SkLerpXfermode.cpp \
	src/effects/SkLightingImageFilter.cpp \
	src/effects/SkMagnifierImageFilter.cpp \
	src/effects/SkMatrixConvolutionImageFilter.cpp \
	src/effects/SkMergeImageFilter.cpp \
	src/effects/SkMorphologyImageFilter.cpp \
	src/effects/SkOffsetImageFilter.cpp \
	src/effects/SkPaintFlagsDrawFilter.cpp \
	src/effects/SkPerlinNoiseShader.cpp \
	src/effects/SkPixelXorXfermode.cpp \
	src/effects/SkPorterDuff.cpp \
	src/effects/SkRectShaderImageFilter.cpp \
	src/effects/SkStippleMaskFilter.cpp \
	src/effects/SkTableColorFilter.cpp \
	src/effects/SkTableMaskFilter.cpp \
	src/effects/SkTestImageFilters.cpp \
	src/effects/SkTransparentShader.cpp \
	src/effects/SkXfermodeImageFilter.cpp \
	src/effects/gradients/SkBitmapCache.cpp \
	src/effects/gradients/SkClampRange.cpp \
	src/effects/gradients/SkGradientShader.cpp \
	src/effects/gradients/SkLinearGradient.cpp \
	src/effects/gradients/SkRadialGradient.cpp \
	src/effects/gradients/SkTwoPointRadialGradient.cpp \
	src/effects/gradients/SkTwoPointConicalGradient.cpp \
	src/effects/gradients/SkSweepGradient.cpp \
	src/image/SkDataPixelRef.cpp \
	src/image/SkImage.cpp \
	src/image/SkImagePriv.cpp \
	src/image/SkImage_Codec.cpp \
	src/image/SkImage_Gpu.cpp \
	src/image/SkImage_Picture.cpp \
	src/image/SkImage_Raster.cpp \
	src/image/SkSurface.cpp \
	src/image/SkSurface_Gpu.cpp \
	src/image/SkSurface_Picture.cpp \
	src/image/SkSurface_Raster.cpp \
	src/images/bmpdecoderhelper.cpp \
	src/images/SkImageDecoder_FactoryDefault.cpp \
	src/images/SkImageDecoder_FactoryRegistrar.cpp \
	src/images/SkFlipPixelRef.cpp \
	src/images/SkImages.cpp \
	src/images/SkForceLinking.cpp \
	src/images/SkImageDecoder.cpp \
	src/images/SkImageDecoder_libbmp.cpp \
	src/images/SkImageDecoder_wbmp.cpp \
	src/images/SkImageDecoder_libico.cpp \
	src/images/SkImageDecoder_libwebp.cpp \
	src/images/SkImageDecoder_libgif.cpp \
	src/images/SkImageDecoder_libjpeg.cpp \
	src/images/SkImageDecoder_libpng.cpp \
	src/images/SkImageEncoder.cpp \
	src/images/SkImageEncoder_Factory.cpp \
	src/images/SkImageEncoder_argb.cpp \
	src/images/SkImageRef.cpp \
	src/images/SkImageRefPool.cpp \
	src/images/SkImageRef_ashmem.cpp \
	src/images/SkImageRef_GlobalPool.cpp \
	src/images/SkJpegUtility.cpp \
	src/images/SkMovie.cpp \
	src/images/SkMovie_gif.cpp \
	src/images/SkPageFlipper.cpp \
	src/images/SkScaledBitmapSampler.cpp \
	src/images/SkStreamHelpers.cpp \
	src/pathops/SkAddIntersections.cpp \
	src/pathops/SkDCubicIntersection.cpp \
	src/pathops/SkDCubicLineIntersection.cpp \
	src/pathops/SkDCubicToQuads.cpp \
	src/pathops/SkDLineIntersection.cpp \
	src/pathops/SkDQuadImplicit.cpp \
	src/pathops/SkDQuadIntersection.cpp \
	src/pathops/SkDQuadLineIntersection.cpp \
	src/pathops/SkIntersections.cpp \
	src/pathops/SkOpAngle.cpp \
	src/pathops/SkOpContour.cpp \
	src/pathops/SkOpEdgeBuilder.cpp \
	src/pathops/SkOpSegment.cpp \
	src/pathops/SkPathOpsBounds.cpp \
	src/pathops/SkPathOpsCommon.cpp \
	src/pathops/SkPathOpsCubic.cpp \
	src/pathops/SkPathOpsDebug.cpp \
	src/pathops/SkPathOpsLine.cpp \
	src/pathops/SkPathOpsOp.cpp \
	src/pathops/SkPathOpsPoint.cpp \
	src/pathops/SkPathOpsQuad.cpp \
	src/pathops/SkPathOpsRect.cpp \
	src/pathops/SkPathOpsSimplify.cpp \
	src/pathops/SkPathOpsTriangle.cpp \
	src/pathops/SkPathOpsTypes.cpp \
	src/pathops/SkPathWriter.cpp \
	src/pathops/SkQuarticRoot.cpp \
	src/pathops/SkReduceOrder.cpp \
	src/pipe/SkGPipeRead.cpp \
	src/pipe/SkGPipeWrite.cpp \
	src/ports/SkDebug_android.cpp \
	src/ports/SkGlobalInitialization_default.cpp \
	src/ports/SkFontConfigInterface_android.cpp \
	src/ports/SkFontConfigParser_android.cpp \
	src/ports/SkFontHost_FreeType.cpp \
	src/ports/SkFontHost_FreeType_common.cpp \
	src/ports/SkFontHost_fontconfig.cpp \
	src/ports/SkMemory_malloc.cpp \
	src/ports/SkOSFile_posix.cpp \
	src/ports/SkOSFile_stdio.cpp \
	src/ports/SkPurgeableMemoryBlock_android.cpp \
	src/ports/SkThread_pthread.cpp \
	src/ports/SkTime_Unix.cpp \
	src/ports/SkTLS_pthread.cpp \
	src/pdf/SkPDFCatalog.cpp \
	src/pdf/SkPDFDevice.cpp \
	src/pdf/SkPDFDeviceFlattener.cpp \
	src/pdf/SkPDFDocument.cpp \
	src/pdf/SkPDFFont.cpp \
	src/pdf/SkPDFFormXObject.cpp \
	src/pdf/SkPDFGraphicState.cpp \
	src/pdf/SkPDFImage.cpp \
	src/pdf/SkPDFImageStream.cpp \
	src/pdf/SkPDFPage.cpp \
	src/pdf/SkPDFResourceDict.cpp \
	src/pdf/SkPDFShader.cpp \
	src/pdf/SkPDFStream.cpp \
	src/pdf/SkPDFTypes.cpp \
	src/pdf/SkPDFUtils.cpp \
	src/sfnt/SkOTTable_name.cpp \
	src/sfnt/SkOTUtils.cpp \
	src/utils/android/ashmem.cpp \
	src/utils/debugger/SkDebugCanvas.cpp \
	src/utils/debugger/SkDrawCommand.cpp \
	src/utils/debugger/SkObjectParser.cpp \
	src/utils/SkBase64.cpp \
	src/utils/SkBitSet.cpp \
	src/utils/SkBoundaryPatch.cpp \
	src/utils/SkCamera.cpp \
	src/utils/SkCanvasStack.cpp \
	src/utils/SkCanvasStateUtils.cpp \
	src/utils/SkCubicInterval.cpp \
	src/utils/SkCullPoints.cpp \
	src/utils/SkDeferredCanvas.cpp \
	src/utils/SkDumpCanvas.cpp \
	src/utils/SkFrontBufferedStream.cpp \
	src/utils/SkInterpolator.cpp \
	src/utils/SkLayer.cpp \
	src/utils/SkMatrix44.cpp \
	src/utils/SkMD5.cpp \
	src/utils/SkMeshUtils.cpp \
	src/utils/SkNinePatch.cpp \
	src/utils/SkNWayCanvas.cpp \
	src/utils/SkNullCanvas.cpp \
	src/utils/SkOSFile.cpp \
	src/utils/SkParse.cpp \
	src/utils/SkParseColor.cpp \
	src/utils/SkParsePath.cpp \
	src/utils/SkPictureUtils.cpp \
	src/utils/SkPathUtils.cpp \
	src/utils/SkProxyCanvas.cpp \
	src/utils/SkSHA1.cpp \
	src/utils/SkRTConf.cpp \
	src/utils/SkThreadUtils_pthread.cpp \
	src/utils/SkThreadUtils_pthread_other.cpp \
	src/utils/SkUnitMappers.cpp \
	src/lazy/SkBitmapFactory.cpp \
	src/lazy/SkLazyPixelRef.cpp \
	src/lazy/SkLruImageCache.cpp \
	src/lazy/SkPurgeableMemoryBlock_common.cpp \
	src/lazy/SkPurgeableImageCache.cpp \

#	src/utils/SkBitmapHasher.cpp \

# maps to the 'skgr' gyp target
LOCAL_SRC_FILES += \
	src/gpu/SkGpuDevice.cpp \
	src/gpu/SkGr.cpp \
	src/gpu/SkGrFontScaler.cpp \
	src/gpu/SkGrPixelRef.cpp \
	src/gpu/SkGrTexturePixelRef.cpp \
	src/gpu/gl/SkGLContextHelper.cpp \
	src/gpu/gl/android/SkNativeGLContext_android.cpp \

# null and debug gpu interfaces
LOCAL_SRC_FILES += \
	src/gpu/gl/SkNullGLContext.cpp \
	src/gpu/gl/GrGLCreateNullInterface.cpp \
	src/gpu/gl/debug/SkDebugGLContext.cpp \
	src/gpu/gl/debug/GrGLCreateDebugInterface.cpp \
	src/gpu/gl/debug/GrBufferObj.cpp \
	src/gpu/gl/debug/GrTextureObj.cpp \
	src/gpu/gl/debug/GrTextureUnitObj.cpp \
	src/gpu/gl/debug/GrFrameBufferObj.cpp \
	src/gpu/gl/debug/GrShaderObj.cpp \
	src/gpu/gl/debug/GrProgramObj.cpp \
	src/gpu/gl/debug/GrDebugGL.cpp

# maps to the 'gr' gyp target
LOCAL_SRC_FILES += \
	src/gpu/GrAAHairLinePathRenderer.cpp \
	src/gpu/GrAAConvexPathRenderer.cpp \
	src/gpu/GrAARectRenderer.cpp \
	src/gpu/GrAddPathRenderers_default.cpp \
	src/gpu/GrAllocPool.cpp \
	src/gpu/GrAtlas.cpp \
	src/gpu/GrBufferAllocPool.cpp \
	src/gpu/GrCacheID.cpp \
	src/gpu/GrClipData.cpp \
	src/gpu/GrContext.cpp \
	src/gpu/GrDefaultPathRenderer.cpp \
	src/gpu/GrDrawState.cpp \
	src/gpu/GrDrawTarget.cpp \
	src/gpu/GrEffect.cpp \
	src/gpu/GrGeometryBuffer.cpp \
	src/gpu/GrClipMaskCache.cpp \
	src/gpu/GrClipMaskManager.cpp \
	src/gpu/GrGpu.cpp \
	src/gpu/GrGpuFactory.cpp \
	src/gpu/GrInOrderDrawBuffer.cpp \
	src/gpu/GrMemory.cpp \
	src/gpu/GrMemoryPool.cpp \
	src/gpu/GrOvalRenderer.cpp \
	src/gpu/GrPaint.cpp \
	src/gpu/GrPath.cpp \
	src/gpu/GrPathRendererChain.cpp \
	src/gpu/GrPathRenderer.cpp \
	src/gpu/GrPathUtils.cpp \
	src/gpu/GrRectanizer.cpp \
	src/gpu/GrReducedClip.cpp \
	src/gpu/GrRenderTarget.cpp \
	src/gpu/GrResource.cpp \
	src/gpu/GrResourceCache.cpp \
	src/gpu/GrStencil.cpp \
	src/gpu/GrStencilAndCoverPathRenderer.cpp \
	src/gpu/GrStencilBuffer.cpp \
	src/gpu/GrSWMaskHelper.cpp \
	src/gpu/GrSoftwarePathRenderer.cpp \
	src/gpu/GrSurface.cpp \
	src/gpu/GrTextContext.cpp \
	src/gpu/GrTextStrike.cpp \
	src/gpu/GrTexture.cpp \
	src/gpu/GrTextureAccess.cpp \
	src/gpu/gr_unittests.cpp \
	src/gpu/effects/GrConfigConversionEffect.cpp \
	src/gpu/effects/GrConvolutionEffect.cpp \
	src/gpu/effects/GrSimpleTextureEffect.cpp \
	src/gpu/effects/GrSingleTextureEffect.cpp \
	src/gpu/effects/GrTextureDomainEffect.cpp \
	src/gpu/effects/GrTextureStripAtlas.cpp \
	src/gpu/gl/GrGLBufferImpl.cpp \
	src/gpu/gl/GrGLCaps.cpp \
	src/gpu/gl/GrGLContext.cpp \
	src/gpu/gl/GrGLDefaultInterface_native.cpp \
	src/gpu/gl/GrGLEffect.cpp \
	src/gpu/gl/GrGLExtensions.cpp \
	src/gpu/gl/GrGLEffectMatrix.cpp \
	src/gpu/gl/GrGLIndexBuffer.cpp \
	src/gpu/gl/GrGLInterface.cpp \
	src/gpu/gl/GrGLNoOpInterface.cpp \
	src/gpu/gl/GrGLPath.cpp \
	src/gpu/gl/GrGLProgram.cpp \
	src/gpu/gl/GrGLProgramDesc.cpp \
	src/gpu/gl/GrGLRenderTarget.cpp \
	src/gpu/gl/GrGLShaderBuilder.cpp \
	src/gpu/gl/GrGLSL.cpp \
	src/gpu/gl/GrGLStencilBuffer.cpp \
	src/gpu/gl/GrGLTexture.cpp \
	src/gpu/gl/GrGLUtil.cpp \
	src/gpu/gl/GrGLUniformManager.cpp \
	src/gpu/gl/GrGLVertexArray.cpp \
	src/gpu/gl/GrGLVertexBuffer.cpp \
	src/gpu/gl/GrGpuGL.cpp \
	src/gpu/gl/GrGpuGL_program.cpp \
	src/gpu/gl/android/GrGLCreateNativeInterface_android.cpp


ifeq ($(TARGET_ARCH),arm)

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_SRC_FILES += \
	src/opts/memset16_neon.S \
	src/opts/memset32_neon.S \
	src/opts/SkBitmapProcState_arm_neon.cpp \
	src/opts/SkBitmapProcState_matrixProcs_neon.cpp \
	src/opts/SkBlitRow_opts_arm_neon.cpp
endif

LOCAL_SRC_FILES += \
	src/core/SkUtilsArm.cpp \
	src/opts/opts_check_arm.cpp \
	src/opts/memset.arm.S \
	src/opts/SkBitmapProcState_opts_arm.cpp \
	src/opts/SkBlitRow_opts_arm.cpp

else
LOCAL_SRC_FILES += \
	src/opts/SkBlitRow_opts_none.cpp \
	src/opts/SkBitmapProcState_opts_none.cpp \
	src/opts/SkUtils_opts_none.cpp
endif

LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils \
	libft2 \
	libjpeg \
	libpng \
	libutils \
	libz \
	libexpat \
	libutils \
	libEGL \
	libGLESv2 \
        libdvm \
        libdl

LOCAL_STATIC_LIBRARIES := \
	libgif \
	libwebp-decode \
	libwebp-encode

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include/core \
	$(LOCAL_PATH)/include/config \
	$(LOCAL_PATH)/include/effects \
	$(LOCAL_PATH)/include/gpu \
	$(LOCAL_PATH)/include/images \
	$(LOCAL_PATH)/include/lazy \
	$(LOCAL_PATH)/include/pathops \
	$(LOCAL_PATH)/include/pdf \
	$(LOCAL_PATH)/include/pipe \
	$(LOCAL_PATH)/include/ports \
	$(LOCAL_PATH)/include/utils \
	$(LOCAL_PATH)/include/xml \
	$(LOCAL_PATH)/src/core \
	$(LOCAL_PATH)/src/gpu \
	$(LOCAL_PATH)/src/image \
	$(LOCAL_PATH)/src/lazy \
	$(LOCAL_PATH)/src/pdf \
	$(LOCAL_PATH)/src/sfnt \
	$(LOCAL_PATH)/src/utils \
	external/freetype/include \
	external/zlib \
	external/libpng \
	external/giflib \
	external/jpeg \
	external/webp/include \
	frameworks/base/opengl/include \
	external/expat/lib \
        dalvik \
        dalvik/vm \
        dalvik/vm/zombie \

LOCAL_EXPORT_C_INCLUDE_DIRS := \
	$(LOCAL_PATH)/include/config \
	$(LOCAL_PATH)/include/core \
	$(LOCAL_PATH)/include/effects \
	$(LOCAL_PATH)/include/gpu \
	$(LOCAL_PATH)/include/images \
	$(LOCAL_PATH)/include/pdf \
	$(LOCAL_PATH)/include/pipe \
	$(LOCAL_PATH)/include/ports \
	$(LOCAL_PATH)/include/utils \
	$(LOCAL_PATH)/include/lazy

# Add SFTNLY support for PDF (which in turns depends on ICU)
LOCAL_C_INCLUDES += external/sfntly/cpp/src
LOCAL_STATIC_LIBRARIES += libsfntly
LOCAL_SHARED_LIBRARIES += libicuuc libicui18n

# Android's -D_FORTIFY_SOURCE=2 extensions are incompatibile with SkString.
# Revert to -D_FORTIFY_SOURCE=1
LOCAL_CFLAGS += -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=1

LOCAL_LDLIBS += -lpthread -ldl

LOCAL_MODULE:= libskia

#include stlport headers
include external/stlport/libstlport.mk

include $(BUILD_SHARED_LIBRARY)

#############################################################
# Build the skia tools
#

# benchmark (timings)
include $(BASE_PATH)/bench/Android.mk

# golden-master (fidelity / regression test)
#include $(BASE_PATH)/gm/Android.mk

# unit-tests
include $(BASE_PATH)/tests/Android.mk

# pathOps unit-tests
# TODO include those sources!
