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

.source T_sget_byte_1.java
.class public dot.junit.opcodes.sget_byte.d.T_sget_byte_1
.super java/lang/Object

.field public static i1 B
.field protected static p1 B
.field private static pvt1 B

.method static <clinit>()V
.limit regs 1
       const v0, 77
       sput-byte v0, dot.junit.opcodes.sget_byte.d.T_sget_byte_1.i1 B

       const v0, 77
       sput-byte v0, dot.junit.opcodes.sget_byte.d.T_sget_byte_1.p1 B

       const v0, 77
       sput-byte v0, dot.junit.opcodes.sget_byte.d.T_sget_byte_1.pvt1 B

       return-void
.end method

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()B
.limit regs 3

       sget-byte v1, dot.junit.opcodes.sget_byte.d.T_sget_byte_1.i1 B
       return v1
.end method


