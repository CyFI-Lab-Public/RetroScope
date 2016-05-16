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

.source T_iget_2.java
.class public dot.junit.opcodes.iget.d.T_iget_2
.super java/lang/Object

.field public  val F

.method  <clinit>()V
.limit regs 2
       return-void
.end method

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       
       const v0, 123.0
       iput v0, v1, dot.junit.opcodes.iget.d.T_iget_2.val F
       
       return-void
.end method

.method public run()F
.limit regs 4

       iget v1, v3, dot.junit.opcodes.iget.d.T_iget_2.val F
       return v1
.end method


