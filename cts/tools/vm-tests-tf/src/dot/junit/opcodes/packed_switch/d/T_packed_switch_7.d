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

.source T_packed_switch_7.java
.class public dot.junit.opcodes.packed_switch.d.T_packed_switch_7
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)I
.limit regs 5
       packed-switch v4, -1
            Label9    ; -1
            Label6    ; 0
            Label6    ; 1
            Label12    ; 2
            Label12    ; 3
        packed-switch-end
Label6:
       const/4 v2, -1
       return v2
Label9:
       const/4 v2, 2
       return v2
Label12:
       const/16 v2, 20
       return v2

.end method
