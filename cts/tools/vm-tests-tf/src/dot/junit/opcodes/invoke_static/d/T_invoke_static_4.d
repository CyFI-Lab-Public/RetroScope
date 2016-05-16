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

.source T_invoke_static_4.java
.class public dot.junit.opcodes.invoke_static.d.T_invoke_static_4
.super java/lang/Object

.field static val J

.method static <clinit>()V
.limit regs 2

       const-wide/32 v0, 123456789
       sput-wide v0, dot.junit.opcodes.invoke_static.d.T_invoke_static_4.val J
       return-void
.end method

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public static run()J
.limit regs 4
Label0:
       sget-wide v2, dot.junit.opcodes.invoke_static.d.T_invoke_static_4.val J
       return-wide v2
.end method


