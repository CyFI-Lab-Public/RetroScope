target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"

declare i8* @rsOffset([1 x i32] %a.coerce, i32 %sizeOf, i32 %x, i32 %y, i32 %z)

; The loads and stores in this file are annotated with RenderScript-specific
; information for the type based alias analysis, such that the TBAA analysis
; understands that loads and stores from two allocations with different types
; can never access the same memory element. This is different from C, where
; a char or uchar load/store is special as it can alias with about everything.
;
; The TBAA tree in this file has the the node "RenderScript TBAA" as its root.
; This means all loads/stores that share this common root can be proven to not
; alias. However, the alias analysis still has to assume MayAlias between
; memory accesses in this file and memory accesses annotated with the C/C++
; TBAA metadata.
; If we can ensure that all accesses to elements loaded from RenderScript
; allocations are either annotated with the RenderScript TBAA information or
; not annotated at all, but never annotated with the C/C++ metadata, we
; can add the RenderScript TBAA tree under the C/C++ TBAA tree. This enables
; then the TBAA to prove that an access to data from the RenderScript allocation
; does not alias with a load/store accessing something not part of a RenderScript
; allocation.


!14 = metadata !{metadata !"RenderScript TBAA"}
!15 = metadata !{metadata !"allocation", metadata !14}

!21 = metadata !{metadata !"char", metadata !15}
define void @rsSetElementAtImpl_char([1 x i32] %a.coerce, i8 signext %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 1, i32 %x, i32 %y, i32 %z) #10
  store i8 %val, i8* %1, align 1, !tbaa !21
  ret void
}

define signext i8 @rsGetElementAtImpl_char([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 1, i32 %x, i32 %y, i32 %z) #10
  %2 = load i8* %1, align 1, !tbaa !21
  ret i8 %2
}

!22 = metadata !{metadata !"char2", metadata !15}
define void @rsSetElementAtImpl_char2([1 x i32] %a.coerce, <2 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i8>*
  store <2 x i8> %val, <2 x i8>* %2, align 2, !tbaa !22
  ret void
}

define <2 x i8> @rsGetElementAtImpl_char2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i8>*
  %3 = load <2 x i8>* %2, align 2, !tbaa !22
  ret <2 x i8> %3
}

!23 = metadata !{metadata !"char3", metadata !15}
define void @rsSetElementAtImpl_char3([1 x i32] %a.coerce, <3 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i8> %val, <3 x i8> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i8>*
  store <4 x i8> %2, <4 x i8>* %3, align 4, !tbaa !23
  ret void
}

define <3 x i8> @rsGetElementAtImpl_char3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  %3 = load <4 x i8>* %2, align 4, !tbaa !23
  %4 = shufflevector <4 x i8> %3, <4 x i8> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i8> %4
}

!24 = metadata !{metadata !"char4", metadata !15}
define void @rsSetElementAtImpl_char4([1 x i32] %a.coerce, <4 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  store <4 x i8> %val, <4 x i8>* %2, align 4, !tbaa !24
  ret void
}

define <4 x i8> @rsGetElementAtImpl_char4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  %3 = load <4 x i8>* %2, align 4, !tbaa !24
  ret <4 x i8> %3
}

!25 = metadata !{metadata !"uchar", metadata !15}
define void @rsSetElementAtImpl_uchar([1 x i32] %a.coerce, i8 zeroext %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 1, i32 %x, i32 %y, i32 %z) #10
  store i8 %val, i8* %1, align 1, !tbaa !25
  ret void
}

define zeroext i8 @rsGetElementAtImpl_uchar([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 1, i32 %x, i32 %y, i32 %z) #10
  %2 = load i8* %1, align 1, !tbaa !25
  ret i8 %2
}

!26 = metadata !{metadata !"uchar2", metadata !15}
define void @rsSetElementAtImpl_uchar2([1 x i32] %a.coerce, <2 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i8>*
  store <2 x i8> %val, <2 x i8>* %2, align 2, !tbaa !26
  ret void
}

define <2 x i8> @rsGetElementAtImpl_uchar2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i8>*
  %3 = load <2 x i8>* %2, align 2, !tbaa !26
  ret <2 x i8> %3
}

!27 = metadata !{metadata !"uchar3", metadata !15}
define void @rsSetElementAtImpl_uchar3([1 x i32] %a.coerce, <3 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i8> %val, <3 x i8> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i8>*
  store <4 x i8> %2, <4 x i8>* %3, align 4, !tbaa !27
  ret void
}

define <3 x i8> @rsGetElementAtImpl_uchar3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  %3 = load <4 x i8>* %2, align 4, !tbaa !27
  %4 = shufflevector <4 x i8> %3, <4 x i8> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i8> %4
}

!28 = metadata !{metadata !"uchar4", metadata !15}
define void @rsSetElementAtImpl_uchar4([1 x i32] %a.coerce, <4 x i8> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  store <4 x i8> %val, <4 x i8>* %2, align 4, !tbaa !28
  ret void
}

define <4 x i8> @rsGetElementAtImpl_uchar4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i8>*
  %3 = load <4 x i8>* %2, align 4, !tbaa !28
  ret <4 x i8> %3
}

!29 = metadata !{metadata !"short", metadata !15}
define void @rsSetElementAtImpl_short([1 x i32] %a.coerce, i16 signext %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i16*
  store i16 %val, i16* %2, align 2, !tbaa !29
  ret void
}

define signext i16 @rsGetElementAtImpl_short([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i16*
  %3 = load i16* %2, align 2, !tbaa !29
  ret i16 %3
}

!30 = metadata !{metadata !"short2", metadata !15}
define void @rsSetElementAtImpl_short2([1 x i32] %a.coerce, <2 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i16>*
  store <2 x i16> %val, <2 x i16>* %2, align 4, !tbaa !30
  ret void
}

define <2 x i16> @rsGetElementAtImpl_short2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i16>*
  %3 = load <2 x i16>* %2, align 4, !tbaa !30
  ret <2 x i16> %3
}

!31 = metadata !{metadata !"short3", metadata !15}
define void @rsSetElementAtImpl_short3([1 x i32] %a.coerce, <3 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i16> %val, <3 x i16> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i16>*
  store <4 x i16> %2, <4 x i16>* %3, align 8, !tbaa !31
  ret void
}

define <3 x i16> @rsGetElementAtImpl_short3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  %3 = load <4 x i16>* %2, align 8, !tbaa !31
  %4 = shufflevector <4 x i16> %3, <4 x i16> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i16> %4
}

!32 = metadata !{metadata !"short4", metadata !15}
define void @rsSetElementAtImpl_short4([1 x i32] %a.coerce, <4 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  store <4 x i16> %val, <4 x i16>* %2, align 8, !tbaa !32
  ret void
}

define <4 x i16> @rsGetElementAtImpl_short4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  %3 = load <4 x i16>* %2, align 8, !tbaa !32
  ret <4 x i16> %3
}

!33 = metadata !{metadata !"ushort", metadata !15}
define void @rsSetElementAtImpl_ushort([1 x i32] %a.coerce, i16 zeroext %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i16*
  store i16 %val, i16* %2, align 2, !tbaa !33
  ret void
}

define zeroext i16 @rsGetElementAtImpl_ushort([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 2, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i16*
  %3 = load i16* %2, align 2, !tbaa !33
  ret i16 %3
}

!34 = metadata !{metadata !"ushort2", metadata !15}
define void @rsSetElementAtImpl_ushort2([1 x i32] %a.coerce, <2 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i16>*
  store <2 x i16> %val, <2 x i16>* %2, align 4, !tbaa !34
  ret void
}

define <2 x i16> @rsGetElementAtImpl_ushort2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i16>*
  %3 = load <2 x i16>* %2, align 4, !tbaa !34
  ret <2 x i16> %3
}

!35 = metadata !{metadata !"ushort3", metadata !15}
define void @rsSetElementAtImpl_ushort3([1 x i32] %a.coerce, <3 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i16> %val, <3 x i16> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i16>*
  store <4 x i16> %2, <4 x i16>* %3, align 8, !tbaa !35
  ret void
}

define <3 x i16> @rsGetElementAtImpl_ushort3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  %3 = load <4 x i16>* %2, align 8, !tbaa !35
  %4 = shufflevector <4 x i16> %3, <4 x i16> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i16> %4
}

!36 = metadata !{metadata !"ushort4", metadata !15}
define void @rsSetElementAtImpl_ushort4([1 x i32] %a.coerce, <4 x i16> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  store <4 x i16> %val, <4 x i16>* %2, align 8, !tbaa !36
  ret void
}

define <4 x i16> @rsGetElementAtImpl_ushort4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i16>*
  %3 = load <4 x i16>* %2, align 8, !tbaa !36
  ret <4 x i16> %3
}

!37 = metadata !{metadata !"int", metadata !15}
define void @rsSetElementAtImpl_int([1 x i32] %a.coerce, i32 %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i32*
  store i32 %val, i32* %2, align 4, !tbaa !37
  ret void
}

define i32 @rsGetElementAtImpl_int([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i32*
  %3 = load i32* %2, align 4, !tbaa !37
  ret i32 %3
}

!38 = metadata !{metadata !"int2", metadata !15}
define void @rsSetElementAtImpl_int2([1 x i32] %a.coerce, <2 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i32>*
  store <2 x i32> %val, <2 x i32>* %2, align 8, !tbaa !38
  ret void
}

define <2 x i32> @rsGetElementAtImpl_int2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i32>*
  %3 = load <2 x i32>* %2, align 8, !tbaa !38
  ret <2 x i32> %3
}

!39 = metadata !{metadata !"int3", metadata !15}
define void @rsSetElementAtImpl_int3([1 x i32] %a.coerce, <3 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i32> %val, <3 x i32> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i32>*
  store <4 x i32> %2, <4 x i32>* %3, align 16, !tbaa !39
  ret void
}

define <3 x i32> @rsGetElementAtImpl_int3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  %3 = load <4 x i32>* %2, align 8, !tbaa !39
  %4 = shufflevector <4 x i32> %3, <4 x i32> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i32> %4
}

!40 = metadata !{metadata !"int4", metadata !15}
define void @rsSetElementAtImpl_int4([1 x i32] %a.coerce, <4 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  store <4 x i32> %val, <4 x i32>* %2, align 16, !tbaa !40
  ret void
}

define <4 x i32> @rsGetElementAtImpl_int4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  %3 = load <4 x i32>* %2, align 16, !tbaa !40
  ret <4 x i32> %3
}

!41 = metadata !{metadata !"uint", metadata !15}
define void @rsSetElementAtImpl_uint([1 x i32] %a.coerce, i32 %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i32*
  store i32 %val, i32* %2, align 4, !tbaa !41
  ret void
}

define i32 @rsGetElementAtImpl_uint([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i32*
  %3 = load i32* %2, align 4, !tbaa !41
  ret i32 %3
}

!42 = metadata !{metadata !"uint2", metadata !15}
define void @rsSetElementAtImpl_uint2([1 x i32] %a.coerce, <2 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i32>*
  store <2 x i32> %val, <2 x i32>* %2, align 8, !tbaa !42
  ret void
}

define <2 x i32> @rsGetElementAtImpl_uint2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i32>*
  %3 = load <2 x i32>* %2, align 8, !tbaa !42
  ret <2 x i32> %3
}

!43 = metadata !{metadata !"uint3", metadata !15}
define void @rsSetElementAtImpl_uint3([1 x i32] %a.coerce, <3 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i32> %val, <3 x i32> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i32>*
  store <4 x i32> %2, <4 x i32>* %3, align 16, !tbaa !43
  ret void
}

define <3 x i32> @rsGetElementAtImpl_uint3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  %3 = load <4 x i32>* %2, align 8, !tbaa !43
  %4 = shufflevector <4 x i32> %3, <4 x i32> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x i32> %4
}

!44 = metadata !{metadata !"uint4", metadata !15}
define void @rsSetElementAtImpl_uint4([1 x i32] %a.coerce, <4 x i32> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  store <4 x i32> %val, <4 x i32>* %2, align 16, !tbaa !44
  ret void
}

define <4 x i32> @rsGetElementAtImpl_uint4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i32>*
  %3 = load <4 x i32>* %2, align 16, !tbaa !44
  ret <4 x i32> %3
}

!45 = metadata !{metadata !"long", metadata !15}
define void @rsSetElementAtImpl_long([1 x i32] %a.coerce, i64 %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i64*
  store i64 %val, i64* %2, align 8, !tbaa !45
  ret void
}

define i64 @rsGetElementAtImpl_long([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i64*
  %3 = load i64* %2, align 8, !tbaa !45
  ret i64 %3
}

!46 = metadata !{metadata !"long2", metadata !15}
define void @rsSetElementAtImpl_long2([1 x i32] %a.coerce, <2 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i64>*
  store <2 x i64> %val, <2 x i64>* %2, align 16, !tbaa !46
  ret void
}

define <2 x i64> @rsGetElementAtImpl_long2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i64>*
  %3 = load <2 x i64>* %2, align 16, !tbaa !46
  ret <2 x i64> %3
}

!47 = metadata !{metadata !"long3", metadata !15}
define void @rsSetElementAtImpl_long3([1 x i32] %a.coerce, <3 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i64> %val, <3 x i64> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i64>*
  store <4 x i64> %2, <4 x i64>* %3, align 32, !tbaa !47
  ret void
}

define void @rsGetElementAtImpl_long3(<3 x i64>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  %3 = load <4 x i64>* %2, align 32
  %4 = bitcast <3 x i64>* %agg.result to <4 x i64>*
  store <4 x i64> %3, <4 x i64>* %4, align 32, !tbaa !47
  ret void
}

!48 = metadata !{metadata !"long4", metadata !15}
define void @rsSetElementAtImpl_long4([1 x i32] %a.coerce, <4 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  store <4 x i64> %val, <4 x i64>* %2, align 32, !tbaa !48
  ret void
}

define void @rsGetElementAtImpl_long4(<4 x i64>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  %3 = load <4 x i64>* %2, align 32, !tbaa !15
  store <4 x i64> %3, <4 x i64>* %agg.result, align 32, !tbaa !48
  ret void
}

!49 = metadata !{metadata !"ulong", metadata !15}
define void @rsSetElementAtImpl_ulong([1 x i32] %a.coerce, i64 %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i64*
  store i64 %val, i64* %2, align 8, !tbaa !49
  ret void
}

define i64 @rsGetElementAtImpl_ulong([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to i64*
  %3 = load i64* %2, align 8, !tbaa !49
  ret i64 %3
}

!50 = metadata !{metadata !"ulong2", metadata !15}
define void @rsSetElementAtImpl_ulong2([1 x i32] %a.coerce, <2 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i64>*
  store <2 x i64> %val, <2 x i64>* %2, align 16, !tbaa !50
  ret void
}

define <2 x i64> @rsGetElementAtImpl_ulong2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x i64>*
  %3 = load <2 x i64>* %2, align 16, !tbaa !50
  ret <2 x i64> %3
}

!51 = metadata !{metadata !"ulong3", metadata !15}
define void @rsSetElementAtImpl_ulong3([1 x i32] %a.coerce, <3 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x i64> %val, <3 x i64> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x i64>*
  store <4 x i64> %2, <4 x i64>* %3, align 32, !tbaa !51
  ret void
}

define void @rsGetElementAtImpl_ulong3(<3 x i64>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  %3 = load <4 x i64>* %2, align 32
  %4 = bitcast <3 x i64>* %agg.result to <4 x i64>*
  store <4 x i64> %3, <4 x i64>* %4, align 32, !tbaa !51
  ret void
}

!52 = metadata !{metadata !"ulong4", metadata !15}
define void @rsSetElementAtImpl_ulong4([1 x i32] %a.coerce, <4 x i64> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  store <4 x i64> %val, <4 x i64>* %2, align 32, !tbaa !52
  ret void
}

define void @rsGetElementAtImpl_ulong4(<4 x i64>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x i64>*
  %3 = load <4 x i64>* %2, align 32, !tbaa !15
  store <4 x i64> %3, <4 x i64>* %agg.result, align 32, !tbaa !52
  ret void
}

!53 = metadata !{metadata !"float", metadata !15}
define void @rsSetElementAtImpl_float([1 x i32] %a.coerce, float %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to float*
  store float %val, float* %2, align 4, !tbaa !53
  ret void
}

define float @rsGetElementAtImpl_float([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 4, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to float*
  %3 = load float* %2, align 4, !tbaa !53
  ret float %3
}

!54 = metadata !{metadata !"float2", metadata !15}
define void @rsSetElementAtImpl_float2([1 x i32] %a.coerce, <2 x float> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x float>*
  store <2 x float> %val, <2 x float>* %2, align 8, !tbaa !54
  ret void
}

define <2 x float> @rsGetElementAtImpl_float2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x float>*
  %3 = load <2 x float>* %2, align 8, !tbaa !54
  ret <2 x float> %3
}

!55 = metadata !{metadata !"float3", metadata !15}
define void @rsSetElementAtImpl_float3([1 x i32] %a.coerce, <3 x float> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x float> %val, <3 x float> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x float>*
  store <4 x float> %2, <4 x float>* %3, align 16, !tbaa !55
  ret void
}

define <3 x float> @rsGetElementAtImpl_float3([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x float>*
  %3 = load <4 x float>* %2, align 8, !tbaa !55
  %4 = shufflevector <4 x float> %3, <4 x float> undef, <3 x i32> <i32 0, i32 1, i32 2>
  ret <3 x float> %4
}

!56 = metadata !{metadata !"float4", metadata !15}
define void @rsSetElementAtImpl_float4([1 x i32] %a.coerce, <4 x float> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x float>*
  store <4 x float> %val, <4 x float>* %2, align 16, !tbaa !56
  ret void
}

define <4 x float> @rsGetElementAtImpl_float4([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x float>*
  %3 = load <4 x float>* %2, align 16, !tbaa !56
  ret <4 x float> %3
}

!57 = metadata !{metadata !"double", metadata !15}
define void @rsSetElementAtImpl_double([1 x i32] %a.coerce, double %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to double*
  store double %val, double* %2, align 8, !tbaa !57
  ret void
}

define double @rsGetElementAtImpl_double([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 8, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to double*
  %3 = load double* %2, align 8, !tbaa !57
  ret double %3
}

!58 = metadata !{metadata !"double2", metadata !15}
define void @rsSetElementAtImpl_double2([1 x i32] %a.coerce, <2 x double> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x double>*
  store <2 x double> %val, <2 x double>* %2, align 16, !tbaa !58
  ret void
}

define <2 x double> @rsGetElementAtImpl_double2([1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #3 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 16, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <2 x double>*
  %3 = load <2 x double>* %2, align 16, !tbaa !58
  ret <2 x double> %3
}

!59 = metadata !{metadata !"double3", metadata !15}
define void @rsSetElementAtImpl_double3([1 x i32] %a.coerce, <3 x double> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = shufflevector <3 x double> %val, <3 x double> undef, <4 x i32> <i32 0, i32 1, i32 2, i32 undef>
  %3 = bitcast i8* %1 to <4 x double>*
  store <4 x double> %2, <4 x double>* %3, align 32, !tbaa !59
  ret void
}


define void @rsGetElementAtImpl_double3(<3 x double>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x double>*
  %3 = load <4 x double>* %2, align 32
  %4 = bitcast <3 x double>* %agg.result to <4 x double>*
  store <4 x double> %3, <4 x double>* %4, align 32, !tbaa !59
  ret void
}

!60 = metadata !{metadata !"double4", metadata !15}
define void @rsSetElementAtImpl_double4([1 x i32] %a.coerce, <4 x double> %val, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x double>*
  store <4 x double> %val, <4 x double>* %2, align 32, !tbaa !60
  ret void
}
define void @rsGetElementAtImpl_double4(<4 x double>* noalias nocapture sret %agg.result, [1 x i32] %a.coerce, i32 %x, i32 %y, i32 %z) #2 {
  %1 = tail call i8* @rsOffset([1 x i32] %a.coerce, i32 32, i32 %x, i32 %y, i32 %z) #10
  %2 = bitcast i8* %1 to <4 x double>*
  %3 = load <4 x double>* %2, align 32, !tbaa !15
  store <4 x double> %3, <4 x double>* %agg.result, align 32, !tbaa !60
  ret void
}

attributes #0 = { nounwind readonly "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #3 = { nounwind readonly "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #4 = { "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #5 = { nounwind readnone "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf"="true" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #6 = { nounwind readnone }
attributes #7 = { nounwind }
attributes #8 = { alwaysinline nounwind readnone }
attributes #9 = { nounwind readonly }
attributes #10 = { nobuiltin }
attributes #11 = { nobuiltin nounwind }
attributes #12 = { nobuiltin nounwind readnone }

