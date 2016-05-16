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

.source T_move_16_2.java
.class public dot.junit.opcodes.move_16.d.T_move_16_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public static run()Z
.limit regs 5000
       const v1, 5678
       
       move/16 v4001, v1
       move/16 v0, v4001
       
       const/4 v4, 5678

       if-ne v0, v4, Label0
       if-ne v1, v4, Label0
       
       const v1, 1
       return v1

Label0:
       const v1, 0
       return v1
.end method


