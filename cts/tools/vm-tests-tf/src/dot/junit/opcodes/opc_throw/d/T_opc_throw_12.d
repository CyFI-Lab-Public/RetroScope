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

.source T_opc_throw_12.java
.class public dot.junit.opcodes.opc_throw.d.T_opc_throw_12
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()Z
.limit regs 2

Label0:
    invoke-direct {v1}, dot/junit/opcodes/opc_throw/d/T_opc_throw_12/test()Z
    move-result v0
    return v0
Label1:
    const v0, 0
    return v0

.catch java/lang/RuntimeException from Label0 to Label1 using Label1
.end method


.method private test()Z
.limit regs 2
Label0:
    new-instance v0, java/lang/RuntimeException
    invoke-direct {v0}, java/lang/RuntimeException/<init>()V
    throw  v0
Label1:
    const v1, 1
    return v1

.catch java/lang/RuntimeException from Label0 to Label1 using Label1
.end method
