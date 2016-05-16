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

.source T_sput_1.java
.class public dot.junit.opcodes.sput.d.T_sput_1
.super java/lang/Object

.field public static st_i1 I
.field protected static st_p1 I
.field private static st_pvt1 I

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public static getPvtField()I
.limit regs 2

       sget v0, dot.junit.opcodes.sput.d.T_sput_1.st_pvt1 I
       return v0
.end method

.method public run()V
.limit regs 3

       const v1, 1000000
       sput v1, dot.junit.opcodes.sput.d.T_sput_1.st_i1 I

       return-void
.end method


