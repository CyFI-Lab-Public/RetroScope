# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
       FaceDetector_jni.cpp \
       Embedded/common/src/b_APIEm/DCR.c \
       Embedded/common/src/b_APIEm/BFFaceFinder.c \
       Embedded/common/src/b_APIEm/FaceFinder.c \
       Embedded/common/src/b_APIEm/FaceFinderRef.c \
       Embedded/common/src/b_APIEm/Functions.c \
       Embedded/common/src/b_BasicEm/APh.c \
       Embedded/common/src/b_BasicEm/APhArr.c \
       Embedded/common/src/b_BasicEm/Complex.c \
       Embedded/common/src/b_BasicEm/ComplexArr.c \
       Embedded/common/src/b_BasicEm/Context.c \
       Embedded/common/src/b_BasicEm/DynMemManager.c \
       Embedded/common/src/b_BasicEm/Functions.c \
       Embedded/common/src/b_BasicEm/Int16Arr.c \
       Embedded/common/src/b_BasicEm/Int32Arr.c \
       Embedded/common/src/b_BasicEm/Int8Arr.c \
       Embedded/common/src/b_BasicEm/Math.c.arm \
       Embedded/common/src/b_BasicEm/MemSeg.c \
       Embedded/common/src/b_BasicEm/MemTbl.c \
       Embedded/common/src/b_BasicEm/Memory.c \
       Embedded/common/src/b_BasicEm/Phase.c \
       Embedded/common/src/b_BasicEm/String.c \
       Embedded/common/src/b_BasicEm/UInt16Arr.c \
       Embedded/common/src/b_BasicEm/UInt32Arr.c \
       Embedded/common/src/b_BasicEm/UInt8Arr.c \
       Embedded/common/src/b_BitFeatureEm/BitParam.c \
       Embedded/common/src/b_BitFeatureEm/Feature.c \
       Embedded/common/src/b_BitFeatureEm/Functions.c \
       Embedded/common/src/b_BitFeatureEm/I04Dns2x2Ftr.c \
       Embedded/common/src/b_BitFeatureEm/I04Dns2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/I04Tld2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L01Dns2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L01Tld1x1Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L01Tld2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L04Dns2x2Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L04Dns2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L04Dns3x3Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L04Tld2x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L06Dns3x3Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L06Dns4x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/L06DnsNx4x4Ftr.c \
       Embedded/common/src/b_BitFeatureEm/LocalScanDetector.c \
       Embedded/common/src/b_BitFeatureEm/LocalScanner.c \
       Embedded/common/src/b_BitFeatureEm/ScanDetector.c \
       Embedded/common/src/b_BitFeatureEm/Scanner.c \
       Embedded/common/src/b_BitFeatureEm/Sequence.c \
       Embedded/common/src/b_ImageEm/APhImage.c \
       Embedded/common/src/b_ImageEm/ComplexImage.c \
       Embedded/common/src/b_ImageEm/Flt16Image.c \
       Embedded/common/src/b_ImageEm/Functions.c \
       Embedded/common/src/b_ImageEm/HistoEq.c \
       Embedded/common/src/b_ImageEm/UInt16ByteImage.c \
       Embedded/common/src/b_ImageEm/UInt16BytePyrImage.c \
       Embedded/common/src/b_ImageEm/UInt8Image.c \
       Embedded/common/src/b_ImageEm/UInt32Image.c \
       Embedded/common/src/b_ImageEm/UInt8PyramidalImage.c \
       Embedded/common/src/b_TensorEm/Alt.c \
       Embedded/common/src/b_TensorEm/Cluster2D.c \
       Embedded/common/src/b_TensorEm/Cluster3D.c \
       Embedded/common/src/b_TensorEm/CompactAlt.c \
       Embedded/common/src/b_TensorEm/CompactMat.c \
       Embedded/common/src/b_TensorEm/Flt16Alt2D.c \
       Embedded/common/src/b_TensorEm/Flt16Alt3D.c \
       Embedded/common/src/b_TensorEm/Flt16Mat2D.c \
       Embedded/common/src/b_TensorEm/Flt16Mat3D.c \
       Embedded/common/src/b_TensorEm/Flt16Vec.c \
       Embedded/common/src/b_TensorEm/Flt16Vec2D.c \
       Embedded/common/src/b_TensorEm/Flt16Vec3D.c \
       Embedded/common/src/b_TensorEm/Functions.c \
       Embedded/common/src/b_TensorEm/IdCluster2D.c \
       Embedded/common/src/b_TensorEm/Int16Mat2D.c \
       Embedded/common/src/b_TensorEm/Int16Rect.c \
       Embedded/common/src/b_TensorEm/Int16Vec2D.c \
       Embedded/common/src/b_TensorEm/Int16Vec3D.c \
       Embedded/common/src/b_TensorEm/Int32Mat.c \
       Embedded/common/src/b_TensorEm/MapSequence.c \
       Embedded/common/src/b_TensorEm/Mat.c \
       Embedded/common/src/b_TensorEm/Normalizer.c \
       Embedded/common/src/b_TensorEm/RBFMap2D.c \
       Embedded/common/src/b_TensorEm/SubVecMap.c \
       Embedded/common/src/b_TensorEm/Uint32Rect.c \
       Embedded/common/src/b_TensorEm/VectorMap.c \
       FaceRecEm/common/src/b_FDSDK/DCR.c \
       FaceRecEm/common/src/b_FDSDK/FaceFinder.c \
       FaceRecEm/common/src/b_FDSDK/SDK.c
##

LOCAL_CFLAGS += -Depl_LINUX

LOCAL_C_INCLUDES += \
	external/neven/FaceRecEm/common/src/b_FDSDK \
	$(JNI_H_INCLUDE) \
	$(call include-path-for, corecg graphics) \
	$(LOCAL_PATH)/FaceRecEm/common/src \
	$(LOCAL_PATH)/Embedded/common/conf \
	$(LOCAL_PATH)/Embedded/common/src \
	$(LOCAL_PATH)/unix/src

LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libnativehelper \
	libutils \
	liblog \
	libskia \
	libcutils

LOCAL_MODULE:= libFFTEm

ALL_PREBUILT += $(TARGET_OUT)/usr/share/bmd/RFFspeed_501.bmd
$(TARGET_OUT)/usr/share/bmd/RFFspeed_501.bmd : $(LOCAL_PATH)/Embedded/common/data/APIEm/Modules/RFFspeed_501.bmd | $(ACP)
	$(transform-prebuilt-to-target)

ALL_PREBUILT += $(TARGET_OUT)/usr/share/bmd/RFFstd_501.bmd
$(TARGET_OUT)/usr/share/bmd/RFFstd_501.bmd : $(LOCAL_PATH)/Embedded/common/data/APIEm/Modules/RFFstd_501.bmd | $(ACP)
	$(transform-prebuilt-to-target)


include $(BUILD_SHARED_LIBRARY)
