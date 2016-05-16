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

.source T_invoke_super_14.java
.class public dot.junit.opcodes.invoke_super.d.T_invoke_super_14
.super dot/junit/opcodes/invoke_super/d/TSuper


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, dot/junit/opcodes/invoke_super/d/TSuper/<init>()V
       return-void
.end method

.method public run()Z
.limit regs 7

    const v1, 123
    const v2, 659
    
    const v3, 300
    const v4, 3

    invoke-super {v6, v3, v4}, dot/junit/opcodes/invoke_super/d/T_invoke_super_14/testArgsOrder(II)I

    move-result v3
    const v4, 100
    if-ne v3, v4, Label0

    const v4, 123
    if-ne v1, v4, Label0

    const v4, 659
    if-ne v2, v4, Label0
    
    const v0, 1
    return v0

Label0:
    const v0, 0
    return v0
.end method

.method public testArgsOrder(II)I
.limit regs 4
    const v0, 0
    return v0
.end method

