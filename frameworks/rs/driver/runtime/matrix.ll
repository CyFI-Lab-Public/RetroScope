target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"


%struct.rs_matrix4x4 = type { [16 x float] }
%struct.rs_matrix3x3 = type { [9 x float] }
%struct.rs_matrix2x2 = type { [4 x float] }

define internal <4 x float> @smear_f(float %in) nounwind readnone alwaysinline {
  %1 = insertelement <4 x float> undef, float %in, i32 0
  %2 = insertelement <4 x float> %1, float %in, i32 1
  %3 = insertelement <4 x float> %2, float %in, i32 2
  %4 = insertelement <4 x float> %3, float %in, i32 3
  ret <4 x float> %4
}


define <3 x float> @_Z16rsMatrixMultiplyPK12rs_matrix3x3Dv3_f(%struct.rs_matrix3x3* nocapture %m, <3 x float> %in) nounwind readonly {
  %x0 = extractelement <3 x float> %in, i32 0
  %x = tail call <4 x float> @smear_f(float %x0) nounwind readnone
  %y0 = extractelement <3 x float> %in, i32 1
  %y = tail call <4 x float> @smear_f(float %y0) nounwind readnone
  %z0 = extractelement <3 x float> %in, i32 2
  %z = tail call <4 x float> @smear_f(float %z0) nounwind readnone

  %px = getelementptr inbounds %struct.rs_matrix3x3* %m, i32 0, i32 0, i32 0
  %px2 = bitcast float* %px to <4 x float>*
  %xm = load <4 x float>* %px2, align 4
  %py = getelementptr inbounds %struct.rs_matrix3x3* %m, i32 0, i32 0, i32 3
  %py2 = bitcast float* %py to <4 x float>*
  %ym = load <4 x float>* %py2, align 4
  %pz = getelementptr inbounds %struct.rs_matrix3x3* %m, i32 0, i32 0, i32 6
  %pz2 = bitcast float* %pz to <3 x float>*
  %zm2 = load <3 x float>* %pz2, align 4
  %zm = shufflevector <3 x float> %zm2, <3 x float> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 3>

  %a1 = fmul <4 x float> %x, %xm
  %a2 = fmul <4 x float> %y, %ym
  %a3 = fadd <4 x float> %a1, %a2
  %a4 = fmul <4 x float> %z, %zm
  %a5 = fadd <4 x float> %a4, %a3
  %a6 = shufflevector <4 x float> %a5, <4 x float> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x float> %a6
}

define <3 x float> @_Z16rsMatrixMultiplyP12rs_matrix3x3Dv3_f(%struct.rs_matrix3x3* nocapture %m, <3 x float> %in) nounwind readonly {
  %r = tail call <3 x float> @_Z16rsMatrixMultiplyPK12rs_matrix3x3Dv3_f(%struct.rs_matrix3x3* nocapture %m, <3 x float> %in) nounwind
  ret <3 x float> %r
}

define <3 x float> @_Z16rsMatrixMultiplyPK12rs_matrix3x3Dv2_f(%struct.rs_matrix3x3* nocapture %m, <2 x float> %in) nounwind readonly {
  %x0 = extractelement <2 x float> %in, i32 0
  %x = tail call <4 x float> @smear_f(float %x0) nounwind readnone
  %y0 = extractelement <2 x float> %in, i32 1
  %y = tail call <4 x float> @smear_f(float %y0) nounwind readnone

  %px = getelementptr inbounds %struct.rs_matrix3x3* %m, i32 0, i32 0, i32 0
  %px2 = bitcast float* %px to <4 x float>*
  %xm = load <4 x float>* %px2, align 4
  %py = getelementptr inbounds %struct.rs_matrix3x3* %m, i32 0, i32 0, i32 3
  %py2 = bitcast float* %py to <4 x float>*
  %ym = load <4 x float>* %py2, align 4

  %a1 = fmul <4 x float> %x, %xm
  %a2 = fmul <4 x float> %y, %ym
  %a3 = fadd <4 x float> %a1, %a2
  %a4 = shufflevector <4 x float> %a3, <4 x float> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x float> %a4
}

define <3 x float> @_Z16rsMatrixMultiplyP12rs_matrix3x3Dv2_f(%struct.rs_matrix3x3* nocapture %m, <2 x float> %in) nounwind readonly {
  %r = tail call <3 x float> @_Z16rsMatrixMultiplyPK12rs_matrix3x3Dv2_f(%struct.rs_matrix3x3* nocapture %m, <2 x float> %in) nounwind
  ret <3 x float> %r
}

define <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv4_f(%struct.rs_matrix4x4* nocapture %m, <4 x float> %in) nounwind readonly {
  %x0 = extractelement <4 x float> %in, i32 0
  %x = tail call <4 x float> @smear_f(float %x0) nounwind readnone
  %y0 = extractelement <4 x float> %in, i32 1
  %y = tail call <4 x float> @smear_f(float %y0) nounwind readnone
  %z0 = extractelement <4 x float> %in, i32 2
  %z = tail call <4 x float> @smear_f(float %z0) nounwind readnone
  %w0 = extractelement <4 x float> %in, i32 3
  %w = tail call <4 x float> @smear_f(float %w0) nounwind readnone

  %px = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 0
  %px2 = bitcast float* %px to <4 x float>*
  %xm = load <4 x float>* %px2, align 4
  %py = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 4
  %py2 = bitcast float* %py to <4 x float>*
  %ym = load <4 x float>* %py2, align 4
  %pz = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 8
  %pz2 = bitcast float* %pz to <4 x float>*
  %zm = load <4 x float>* %pz2, align 4
  %pw = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 12
  %pw2 = bitcast float* %pw to <4 x float>*
  %wm = load <4 x float>* %pw2, align 4

  %a1 = fmul <4 x float> %x, %xm
  %a2 = fmul <4 x float> %y, %ym
  %a3 = fadd <4 x float> %a1, %a2
  %a4 = fmul <4 x float> %z, %zm
  %a5 = fadd <4 x float> %a3, %a4
  %a6 = fmul <4 x float> %w, %wm
  %a7 = fadd <4 x float> %a5, %a6
  ret <4 x float> %a7
}

define <4 x float> @_Z16rsMatrixMultiplyP12rs_matrix4x4Dv4_f(%struct.rs_matrix4x4* nocapture %m, <4 x float> %in) nounwind readonly {
  %r = tail call <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv4_f(%struct.rs_matrix4x4* nocapture %m, <4 x float> %in) nounwind
  ret <4 x float> %r
}

define <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv3_f(%struct.rs_matrix4x4* nocapture %m, <3 x float> %in) nounwind readonly {
  %x0 = extractelement <3 x float> %in, i32 0
  %x = tail call <4 x float> @smear_f(float %x0) nounwind readnone
  %y0 = extractelement <3 x float> %in, i32 1
  %y = tail call <4 x float> @smear_f(float %y0) nounwind readnone
  %z0 = extractelement <3 x float> %in, i32 2
  %z = tail call <4 x float> @smear_f(float %z0) nounwind readnone

  %px = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 0
  %px2 = bitcast float* %px to <4 x float>*
  %xm = load <4 x float>* %px2, align 4
  %py = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 4
  %py2 = bitcast float* %py to <4 x float>*
  %ym = load <4 x float>* %py2, align 4
  %pz = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 8
  %pz2 = bitcast float* %pz to <4 x float>*
  %zm = load <4 x float>* %pz2, align 4
  %pw = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 12
  %pw2 = bitcast float* %pw to <4 x float>*
  %wm = load <4 x float>* %pw2, align 4

  %a1 = fmul <4 x float> %x, %xm
  %a2 = fadd <4 x float> %wm, %a1
  %a3 = fmul <4 x float> %y, %ym
  %a4 = fadd <4 x float> %a2, %a3
  %a5 = fmul <4 x float> %z, %zm
  %a6 = fadd <4 x float> %a4, %a5
  ret <4 x float> %a6
}

define <4 x float> @_Z16rsMatrixMultiplyP12rs_matrix4x4Dv3_f(%struct.rs_matrix4x4* nocapture %m, <3 x float> %in) nounwind readonly {
  %r = tail call <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv3_f(%struct.rs_matrix4x4* nocapture %m, <3 x float> %in) nounwind
  ret <4 x float> %r
}

define <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv2_f(%struct.rs_matrix4x4* nocapture %m, <2 x float> %in) nounwind readonly {
  %x0 = extractelement <2 x float> %in, i32 0
  %x = tail call <4 x float> @smear_f(float %x0) nounwind readnone
  %y0 = extractelement <2 x float> %in, i32 1
  %y = tail call <4 x float> @smear_f(float %y0) nounwind readnone

  %px = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 0
  %px2 = bitcast float* %px to <4 x float>*
  %xm = load <4 x float>* %px2, align 4
  %py = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 4
  %py2 = bitcast float* %py to <4 x float>*
  %ym = load <4 x float>* %py2, align 4
  %pw = getelementptr inbounds %struct.rs_matrix4x4* %m, i32 0, i32 0, i32 12
  %pw2 = bitcast float* %pw to <4 x float>*
  %wm = load <4 x float>* %pw2, align 4

  %a1 = fmul <4 x float> %x, %xm
  %a2 = fadd <4 x float> %wm, %a1
  %a3 = fmul <4 x float> %y, %ym
  %a4 = fadd <4 x float> %a2, %a3
  ret <4 x float> %a4
}

define <4 x float> @_Z16rsMatrixMultiplyP12rs_matrix4x4Dv2_f(%struct.rs_matrix4x4* nocapture %m, <2 x float> %in) nounwind readonly {
  %r = tail call <4 x float> @_Z16rsMatrixMultiplyPK12rs_matrix4x4Dv2_f(%struct.rs_matrix4x4* nocapture %m, <2 x float> %in) nounwind
  ret <4 x float> %r
}

