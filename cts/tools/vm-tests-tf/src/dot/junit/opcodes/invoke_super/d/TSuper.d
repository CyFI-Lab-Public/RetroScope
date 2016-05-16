; Copyright (C) 2008 The Android Open Source Project
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;      http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

.source TSuper.java
.class public dot.junit.opcodes.invoke_super.d.TSuper
.super  java/lang/Object

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public toInt()I 
.limit regs 3
    const v0, 5
    return v0
.end method
    
.method public toInt(F)I 
.limit regs 3
    float-to-int v0, v2
    return v0
.end method

.method public native toIntNative()I    
.end method
    
.method public static toIntStatic()I 
.limit regs 3
    const v0, 5
    return v0
.end method
  
.method protected toIntP()I 
.limit regs 3
    const v0, 5
    return v0
.end method  

.method private toIntPvt()I 
.limit regs 3
    const v0, 5
    return v0
.end method  
    
.method public testArgsOrder(II)I
.limit regs 4
    const v0, 349
    const v1, 344656
    div-int v2, v2, v3
    return v2
.end method   

.method public testString(Ljava/lang/String;)V
.limit regs 2
    return-void
.end method
