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

.source T_sparse_switch_8.java
.class public dot.junit.opcodes.sparse_switch.d.T_sparse_switch_8
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)I
.limit regs 5

       sparse-switch v4
            -1 : Label9
            10 : Label12
            15 : Label12
        sparse-switch-end
Label6:
       const v4, -1
       return v4

Label9:
       const v4, 2
       return v4

Label12:
       const v4, 20
       return v4

.end method
