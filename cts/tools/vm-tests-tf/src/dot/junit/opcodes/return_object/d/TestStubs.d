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

.source TestStubs.java
.interface dot.junit.opcodes.return_object.d.TInterface

.source TestStubs.java
.class dot.junit.opcodes.return_object.d.TSuper 
.super java/lang/Object
.implements dot.junit.opcodes.return_object.d.TInterface 
    
.method public <init>()V
.limit regs 1
       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.source TestStubs.java
.class dot.junit.opcodes.return_object.d.TChild 
.super dot.junit.opcodes.return_object.d.TSuper 
    
.method public <init>()V
.limit regs 1
       invoke-direct {v0}, dot/junit/opcodes/return_object/d/TSuper/<init>()V
       return-void
.end method

.source TestStubs.java
.class dot.junit.opcodes.return_object.d.TSuper2
.super java/lang/Object
 
    
.method public <init>()V
.limit regs 1
       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.source TestStubs.java
.class dot.junit.opcodes.return_object.d.TestStubs 
