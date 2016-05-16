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

.source T_sget_2.java
.class public dot.junit.opcodes.sget.d.T_sget_2
.super java/lang/Object

.field public static val F

.method static <clinit>()V
.limit regs 2
       const v0, 123.0
       sput v0, dot.junit.opcodes.sget.d.T_sget_2.val F
       return-void
.end method

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()F
.limit regs 4

       sget v1, dot.junit.opcodes.sget.d.T_sget_2.val F
       return v1
.end method


