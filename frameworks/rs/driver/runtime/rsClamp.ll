target datalayout = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:64:64-f32:32:32-f64:64:64-v64:64:64-v128:64:128-a0:0:64-n32-S64"
target triple = "armv7-none-linux-gnueabi"


define float @_Z7rsClampfff(float %value, float %low, float %high) nounwind readonly {
  %1 = fcmp olt float %value, %high
  %2 = select i1 %1, float %value, float %high
  %3 = fcmp ogt float %2, %low
  %4 = select i1 %3, float %2, float %low
  ret float %4
}

define signext i8 @_Z7rsClampccc(i8 signext %value, i8 signext %low, i8 signext %high) nounwind readonly {
  %1 = icmp slt i8 %value, %high
  %2 = select i1 %1, i8 %value, i8 %high
  %3 = icmp sgt i8 %2, %low
  %4 = select i1 %3, i8 %2, i8 %low
  ret i8 %4
}

define zeroext i8 @_Z7rsClamphhh(i8 zeroext %value, i8 zeroext %low, i8 zeroext %high) nounwind readonly {
  %1 = icmp ult i8 %value, %high
  %2 = select i1 %1, i8 %value, i8 %high
  %3 = icmp ugt i8 %2, %low
  %4 = select i1 %3, i8 %2, i8 %low
  ret i8 %4
}

define signext i16 @_Z7rsClampsss(i16 signext %value, i16 signext %low, i16 signext %high) nounwind readonly {
  %1 = icmp slt i16 %value, %high
  %2 = select i1 %1, i16 %value, i16 %high
  %3 = icmp sgt i16 %2, %low
  %4 = select i1 %3, i16 %2, i16 %low
  ret i16 %4
}

define zeroext i16 @_Z7rsClampttt(i16 zeroext %value, i16 zeroext %low, i16 zeroext %high) nounwind readonly {
  %1 = icmp ult i16 %value, %high
  %2 = select i1 %1, i16 %value, i16 %high
  %3 = icmp ugt i16 %2, %low
  %4 = select i1 %3, i16 %2, i16 %low
  ret i16 %4
}

define i32 @_Z7rsClampiii(i32 %value, i32 %low, i32 %high) nounwind readonly {
  %1 = icmp slt i32 %value, %high
  %2 = select i1 %1, i32 %value, i32 %high
  %3 = icmp sgt i32 %2, %low
  %4 = select i1 %3, i32 %2, i32 %low
  ret i32 %4
}

define i32 @_Z7rsClampjjj(i32 %value, i32 %low, i32 %high) nounwind readonly {
  %1 = icmp ult i32 %value, %high
  %2 = select i1 %1, i32 %value, i32 %high
  %3 = icmp ugt i32 %2, %low
  %4 = select i1 %3, i32 %2, i32 %low
  ret i32 %4
}

