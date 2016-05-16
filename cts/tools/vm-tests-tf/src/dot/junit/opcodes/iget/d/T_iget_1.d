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

.source T_iget_1.java
.class public dot.junit.opcodes.iget.d.T_iget_1
.super java/lang/Object

.field public  i1 I
.field protected  p1 I
.field private  pvt1 I

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       
       const/4 v0, 5
       iput v0, v1, dot.junit.opcodes.iget.d.T_iget_1.i1 I

       const/16 v0, 10
       iput v0, v1, dot.junit.opcodes.iget.d.T_iget_1.p1 I

       const/16 v0, 20
       iput v0, v1, dot.junit.opcodes.iget.d.T_iget_1.pvt1 I
       
       return-void
.end method

.method public run()I
.limit regs 3

       iget v1, v2, dot.junit.opcodes.iget.d.T_iget_1.i1 I
       return v1
.end method


