target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                  FLOAT                 ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <2 x float> @_Z14convert_float2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = uitofp <2 x i8> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = uitofp <3 x i8> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = uitofp <4 x i8> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = sitofp <2 x i8> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = sitofp <3 x i8> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = sitofp <4 x i8> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = uitofp <2 x i16> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = uitofp <3 x i16> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = uitofp <4 x i16> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = sitofp <2 x i16> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = sitofp <3 x i16> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = sitofp <4 x i16> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = uitofp <2 x i32> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = uitofp <3 x i32> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = uitofp <4 x i32> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = sitofp <2 x i32> %in to <2 x float>
  ret <2 x float> %1
}

define <3 x float> @_Z14convert_float3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = sitofp <3 x i32> %in to <3 x float>
  ret <3 x float> %1
}

define <4 x float> @_Z14convert_float4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = sitofp <4 x i32> %in to <4 x float>
  ret <4 x float> %1
}

define <2 x float> @_Z14convert_float2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  ret <2 x float> %in
}

define <3 x float> @_Z14convert_float3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  ret <3 x float> %in
}

define <4 x float> @_Z14convert_float4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  ret <4 x float> %in
}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                  CHAR                  ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
define <4 x i8> @_Z13convert_char4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <4 x float> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z13convert_char3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <3 x float> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z13convert_char2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <2 x float> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z13convert_char4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  ret <4 x i8> %in
}

define <3 x i8> @_Z13convert_char3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  ret <3 x i8> %in
}

define <2 x i8> @_Z13convert_char2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  ret <2 x i8> %in
}

define <4 x i8> @_Z13convert_char4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  ret <4 x i8> %in
}

define <3 x i8> @_Z13convert_char3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  ret <3 x i8> %in
}

define <2 x i8> @_Z13convert_char2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  ret <2 x i8> %in
}

define <4 x i8> @_Z13convert_char4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i16> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z13convert_char3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i16> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z13convert_char2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i16> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z13convert_char4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i16> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z13convert_char3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i16> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z13convert_char2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i16> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z13convert_char4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z13convert_char3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z13convert_char2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z13convert_char4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z13convert_char3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z13convert_char2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i8>
  ret <2 x i8> %1
}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                  UCHAR                 ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <4 x i8> @_Z14convert_uchar4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <4 x float> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z14convert_uchar3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <3 x float> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z14convert_uchar2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <2 x float> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z14convert_uchar4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  ret <4 x i8> %in
}

define <3 x i8> @_Z14convert_uchar3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  ret <3 x i8> %in
}

define <2 x i8> @_Z14convert_uchar2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  ret <2 x i8> %in
}

define <4 x i8> @_Z14convert_uchar4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  ret <4 x i8> %in
}

define <3 x i8> @_Z14convert_uchar3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  ret <3 x i8> %in
}

define <2 x i8> @_Z14convert_uchar2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  ret <2 x i8> %in
}

define <4 x i8> @_Z14convert_uchar4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i16> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z14convert_uchar3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i16> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z14convert_uchar2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i16> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z14convert_uchar4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i16> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z14convert_uchar3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i16> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z14convert_uchar2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i16> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z14convert_uchar4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z14convert_uchar3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z14convert_uchar2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i8>
  ret <2 x i8> %1
}

define <4 x i8> @_Z14convert_uchar4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i8>
  ret <4 x i8> %1
}

define <3 x i8> @_Z14convert_uchar3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i8>
  ret <3 x i8> %1
}

define <2 x i8> @_Z14convert_uchar2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i8>
  ret <2 x i8> %1
}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                  SHORT                 ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <4 x i16> @_Z14convert_short4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <4 x float> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z14convert_short3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <3 x float> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z14convert_short2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <2 x float> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z14convert_short4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z14convert_short3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z14convert_short2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z14convert_short4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <4 x i8> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z14convert_short3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <3 x i8> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z14convert_short2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <2 x i8> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z14convert_short4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  ret <4 x i16> %in
}

define <3 x i16> @_Z14convert_short3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  ret <3 x i16> %in
}

define <2 x i16> @_Z14convert_short2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  ret <2 x i16> %in
}

define <4 x i16> @_Z14convert_short4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  ret <4 x i16> %in
}

define <3 x i16> @_Z14convert_short3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  ret <3 x i16> %in
}

define <2 x i16> @_Z14convert_short2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  ret <2 x i16> %in
}

define <4 x i16> @_Z14convert_short4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z14convert_short3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z14convert_short2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z14convert_short4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z14convert_short3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z14convert_short2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i16>
  ret <2 x i16> %1
}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                 USHORT                 ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <4 x i16> @_Z15convert_ushort4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <4 x float> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z15convert_ushort3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <3 x float> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z15convert_ushort2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <2 x float> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z15convert_ushort4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z15convert_ushort3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z15convert_ushort2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z15convert_ushort4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z15convert_ushort3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z15convert_ushort2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z15convert_ushort4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  ret <4 x i16> %in
}

define <3 x i16> @_Z15convert_ushort3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  ret <3 x i16> %in
}

define <2 x i16> @_Z15convert_ushort2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  ret <2 x i16> %in
}

define <4 x i16> @_Z15convert_ushort4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  ret <4 x i16> %in
}

define <3 x i16> @_Z15convert_ushort3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  ret <3 x i16> %in
}

define <2 x i16> @_Z15convert_ushort2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  ret <2 x i16> %in
}

define <4 x i16> @_Z15convert_ushort4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z15convert_ushort3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z15convert_ushort2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i16>
  ret <2 x i16> %1
}

define <4 x i16> @_Z15convert_ushort4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <4 x i32> %in to <4 x i16>
  ret <4 x i16> %1
}

define <3 x i16> @_Z15convert_ushort3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <3 x i32> %in to <3 x i16>
  ret <3 x i16> %1
}

define <2 x i16> @_Z15convert_ushort2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  %1 = trunc <2 x i32> %in to <2 x i16>
  ret <2 x i16> %1
}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                   INT                  ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <4 x i32> @_Z12convert_int4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <4 x float> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z12convert_int3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <3 x float> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z12convert_int2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptosi <2 x float> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z12convert_int4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z12convert_int3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z12convert_int2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z12convert_int4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <4 x i8> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z12convert_int3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <3 x i8> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z12convert_int2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = sext <2 x i8> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z12convert_int4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i16> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z12convert_int3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i16> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z12convert_int2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i16> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z12convert_int4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = sext <4 x i16> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z12convert_int3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = sext <3 x i16> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z12convert_int2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = sext <2 x i16> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z12convert_int4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  ret <4 x i32> %in
}

define <3 x i32> @_Z12convert_int3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  ret <3 x i32> %in
}

define <2 x i32> @_Z12convert_int2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  ret <2 x i32> %in
}

define <4 x i32> @_Z12convert_int4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  ret <4 x i32> %in
}

define <3 x i32> @_Z12convert_int3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  ret <3 x i32> %in
}

define <2 x i32> @_Z12convert_int2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  ret <2 x i32> %in
}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;                  UINT                  ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

define <4 x i32> @_Z13convert_uint4Dv4_f(<4 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <4 x float> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z13convert_uint3Dv3_f(<3 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <3 x float> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z13convert_uint2Dv2_f(<2 x float> %in) nounwind readnone alwaysinline {
  %1 = fptoui <2 x float> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z13convert_uint4Dv4_h(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z13convert_uint3Dv3_h(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z13convert_uint2Dv2_h(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z13convert_uint4Dv4_c(<4 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i8> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z13convert_uint3Dv3_c(<3 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i8> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z13convert_uint2Dv2_c(<2 x i8> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i8> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z13convert_uint4Dv4_t(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i16> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z13convert_uint3Dv3_t(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i16> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z13convert_uint2Dv2_t(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i16> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z13convert_uint4Dv4_s(<4 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <4 x i16> %in to <4 x i32>
  ret <4 x i32> %1
}

define <3 x i32> @_Z13convert_uint3Dv3_s(<3 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <3 x i16> %in to <3 x i32>
  ret <3 x i32> %1
}

define <2 x i32> @_Z13convert_uint2Dv2_s(<2 x i16> %in) nounwind readnone alwaysinline {
  %1 = zext <2 x i16> %in to <2 x i32>
  ret <2 x i32> %1
}

define <4 x i32> @_Z13convert_uint4Dv4_j(<4 x i32> %in) nounwind readnone alwaysinline {
  ret <4 x i32> %in
}

define <3 x i32> @_Z13convert_uint3Dv3_j(<3 x i32> %in) nounwind readnone alwaysinline {
  ret <3 x i32> %in
}

define <2 x i32> @_Z13convert_uint2Dv2_j(<2 x i32> %in) nounwind readnone alwaysinline {
  ret <2 x i32> %in
}

define <4 x i32> @_Z13convert_uint4Dv4_i(<4 x i32> %in) nounwind readnone alwaysinline {
  ret <4 x i32> %in
}

define <3 x i32> @_Z13convert_uint3Dv3_i(<3 x i32> %in) nounwind readnone alwaysinline {
  ret <3 x i32> %in
}

define <2 x i32> @_Z13convert_uint2Dv2_i(<2 x i32> %in) nounwind readnone alwaysinline {
  ret <2 x i32> %in
}
