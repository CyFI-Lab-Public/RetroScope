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

.source T_opc_throw_5.java
.class public dot.junit.opcodes.opc_throw.d.T_opc_throw_5
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public declared_synchronized run()V
.limit regs 6

       new-instance v2, java/lang/Object
       invoke-direct {v2}, java/lang/Object/<init>()V
       monitor-enter v2
       monitor-exit v5

       new-instance v1, java/lang/NullPointerException
       invoke-direct {v1}, java/lang/NullPointerException/<init>()V
       throw v1
.end method


