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

.source T_return_void_1.java
.class public dot.junit.opcodes.return_void.d.T_return_void_1
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()I
.limit regs 6
    
    const v1, 1
    const v2, 2
    const v3, 3
        
    invoke-static {}, dot/junit/opcodes/return_void/d/T_return_void_1/test()V

    const v4, 1    
    if-ne v1, v4, Label0
    
    const v4, 2    
    if-ne v2, v4, Label0

    const v4, 3
    if-ne v3, v4, Label0
    
    const v0, 123456
    return v0

Label0:
    const v0, 0
    return v0
.end method

.method private static test()V
.limit regs 5
    
    const v0, 9999
    const v1, 0xaaa
    const v2, 0xbbbb
    const v3, 0xcccc
    const v4, 0xdddd
    
    return-void
.end method


