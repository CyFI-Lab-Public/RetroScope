define <3 x float> @convert1_float3(<3 x i8> %u3) nounwind readnone {
  %conv = uitofp <3 x i8> %u3 to <3 x float>
  ret <3 x float> %conv
}

define <3 x i8> @convert1_uchar3(<3 x float> %f3) nounwind readnone {
  %conv = fptoui <3 x float> %f3 to <3 x i8>
  ret <3 x i8> %conv
}

declare float @llvm.powi.f32(float, i32) nounwind readonly

define <3 x float> @_Z4powiDv3_fi(<3 x float> %f3, i32 %exp) nounwind readnone {
  %x = extractelement <3 x float> %f3, i32 0      ; <float> [#uses=1]
  %y = extractelement <3 x float> %f3, i32 1      ; <float> [#uses=1]
  %z = extractelement <3 x float> %f3, i32 2      ; <float> [#uses=1]
  %retx = tail call float @llvm.powi.f32(float %x, i32 %exp) ; <float> [#uses=1]
  %rety = tail call float @llvm.powi.f32(float %y, i32 %exp) ; <float> [#uses=1]
  %retz = tail call float @llvm.powi.f32(float %z, i32 %exp) ; <float> [#uses=1]
  %tmp1 = insertelement <3 x float> %f3, float %retx, i32 0 ; <<3 x float>> [#uses=1]
  %tmp2 = insertelement <3 x float> %tmp1, float %rety, i32 1 ; <<3 x float>> [#uses=1]
  %ret = insertelement <3 x float> %tmp2, float %retz, i32 2 ; <<3 x float>> [#uses=1]
  ret <3 x float> %ret
}

declare float @llvm.pow.f32(float, float) nounwind readonly

define <3 x float> @_Z4pow3Dv3_ff(<3 x float> %f3, float %exp) nounwind readnone {
  %x = extractelement <3 x float> %f3, i32 0      ; <float> [#uses=1]
  %y = extractelement <3 x float> %f3, i32 1      ; <float> [#uses=1]
  %z = extractelement <3 x float> %f3, i32 2      ; <float> [#uses=1]
  %retx = tail call float @llvm.pow.f32(float %x, float %exp) ; <float> [#uses=1]
  %rety = tail call float @llvm.pow.f32(float %y, float %exp) ; <float> [#uses=1]
  %retz = tail call float @llvm.pow.f32(float %z, float %exp) ; <float> [#uses=1]
  %tmp1 = insertelement <3 x float> %f3, float %retx, i32 0 ; <<3 x float>> [#uses=1]
  %tmp2 = insertelement <3 x float> %tmp1, float %rety, i32 1 ; <<3 x float>> [#uses=1]
  %ret = insertelement <3 x float> %tmp2, float %retz, i32 2 ; <<3 x float>> [#uses=1]
  ret <3 x float> %ret
}

declare <4 x i32> @llvm.arm.neon.vmlals.v4i32(<4 x i32>, <4 x i16>, <4 x i16>)

define <4 x i32> @foo(<4 x i32> %a, <4 x i16> %b, <4 x i16> %c) {
  %A = tail call <4 x i32> @llvm.arm.neon.vmlals.v4i32(<4 x i32> %a, <4 x i16> %b, <4 x i16> %c)
  ret <4 x i32> %A
}

define i32 @test4() {
  %ret4 = call <4 x i32> @foo(<4 x i32> undef, <4 x i16> undef, <4 x i16> undef)
  %retx = extractelement <4 x i32> %ret4, i32 0
  %rety = extractelement <4 x i32> %ret4, i32 1
  %retz = extractelement <4 x i32> %ret4, i32 2
  %ret1 = add i32 %retx, %rety
  %ret2 = add i32 %ret1, %retz
  ret i32 %ret2
}
