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

.source StubInitError.java
.class public dot.junit.opcodes.sget_wide.d.StubInitError
.super java/lang/Object

.field public static value J

.method static <clinit>()V
.limit regs 2

       const/4 v0, 0
       const/4 v1, 5
       div-int/2addr v1, v0

       const-wide v0, 1    
       sput-wide v0, dot.junit.opcodes.sget_wide.d.StubInitError.value J
       return-void
.end method


.source T_sget_wide_9.java
.class public dot.junit.opcodes.sget_wide.d.T_sget_wide_9
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()J
.limit regs 3

       sget-wide v1, dot.junit.opcodes.sget_wide.d.StubInitError.value J
       return-wide v1
.end method


