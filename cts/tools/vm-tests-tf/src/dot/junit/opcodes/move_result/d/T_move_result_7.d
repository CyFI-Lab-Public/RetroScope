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

.source T_move_result_7.java
.class public dot.junit.opcodes.move_result.d.T_move_result_7
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public static run()V
.limit regs 16

    goto Label1
    invoke-static {} dot/junit/opcodes/move_result/d/T_move_result_7/foo()I
Label1:
    move-result v1
    
    return-void
.end method

.method private static foo()I
.limit regs 1

     const v0, 12345
     return v0
.end method


