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

.source T_invoke_interface_14.java
.class public dot.junit.opcodes.invoke_interface.d.T_invoke_interface_14
.super java/lang/Object


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(Ldot/junit/opcodes/invoke_interface/ITest;)I
.limit regs 9
    const v1, 123
    const v2, 345

    const v4, 64
    const v5, 2
    invoke-interface {v8, v4, v5}, dot/junit/opcodes/invoke_interface/ITest/testArgsOrder(II)I
    move-result v4
    const v5, 32
    if-ne v4, v5, Label0

    const v5, 123
    if-ne v5, v1, Label0

    const v5, 345
    if-ne v5, v2, Label0


    const v0, 1
    return v0

Label0:
    const v0, 0
    return v0
.end method


