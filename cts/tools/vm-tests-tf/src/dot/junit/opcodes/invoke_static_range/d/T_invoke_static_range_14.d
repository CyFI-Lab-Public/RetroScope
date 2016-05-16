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

.source TestClassInitError.java
.class public dot.junit.opcodes.invoke_static_range.d.TestClassInitError
.super java/lang/Object

.method static public <clinit>()V
.limit regs 2

    const v0, 1
    const v1, 0
    div-int v0, v0, v1
.end method

.method static public test()V
    return-void
.end method

.source T_invoke_static_range_14.java
.class public dot.junit.opcodes.invoke_static_range.d.T_invoke_static_range_14
.super java/lang/Object


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()V
.limit regs 1

       invoke-static/range {}, dot/junit/opcodes/invoke_static_range/d/TestClassInitError/test()V
       return-void
.end method


