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

.source T_nop_1.java
.class public dot.junit.opcodes.nop.d.T_nop_1
.super java/lang/Object


.method public <init>()V
.limit regs 3
       move-object v1, v2
       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()Z
.limit regs 4
       const v1, 12345678
       nop
       nop
       nop
       nop
       nop
       move v3, v1
       nop
       nop
       nop
       if-ne v1, v3, Label1
       const/4 v1, 1
       return v1
Label1:
       const/4 v1, 0
       return v1
.end method


