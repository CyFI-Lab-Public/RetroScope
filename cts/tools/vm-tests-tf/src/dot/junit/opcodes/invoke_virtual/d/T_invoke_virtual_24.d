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

.source TTestInterface.java
.interface public dot.junit.opcodes.invoke_virtual.d.TTestInterface

.method public abstract test()V
.end method

.source T_invoke_virtual_24.java
.class public dot.junit.opcodes.invoke_virtual.d.T_invoke_virtual_24
.super java/lang/Object
.implements dot.junit.opcodes.invoke_virtual.d.TTestInterface


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method public test()V
    return-void
.end method

.method public run()V
.limit regs 4

       invoke-virtual {v3}, dot/junit/opcodes/invoke_virtual/d/TTestInterface/test()V
       return-void
.end method


