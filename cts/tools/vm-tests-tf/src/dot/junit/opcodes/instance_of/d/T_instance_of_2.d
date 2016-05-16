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
.interface public dot.junit.opcodes.instance_of.d.T_instance_of_2.SuperInterface

.source TestStubs.java    
.interface public dot.junit.opcodes.instance_of.d.T_instance_of_2.SuperInterface2

.source TestStubs.java
.class public dot.junit.opcodes.instance_of.d.T_instance_of_2.SuperClass 
.super java/lang/Object
.implements dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperInterface
    
.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method    

.source TestStubs.java
.class public dot.junit.opcodes.instance_of.d.T_instance_of_2.SubClass 
.super dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass/<init>()V
       return-void
.end method


.source T_instance_of_2.java
.class public dot.junit.opcodes.instance_of.d.T_instance_of_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()Z
.limit regs 20

    const v0, 0
    
; (SubClass instanceof SuperClass)    
    new-instance v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass/<init>()V
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass
    if-eqz v15, LabelExit
    
; (SubClass[] instanceof SuperClass[])    
    const v11, 1
    new-array v10, v11, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass;
    instance-of v15, v10, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass;
    if-eqz v15, LabelExit

; (SubClass[] instanceof Object)    
    new-array v10, v11, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass;
    instance-of v15, v10, java/lang/Object
    if-eqz v15, LabelExit
    
; (SubClass instanceof SuperInterface)    
    new-instance v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass/<init>()V    
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperInterface
    if-eqz v15, LabelExit

; !(SuperClass instanceof SubClass)    
    new-instance v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass
    invoke-direct {v10}, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass/<init>()V
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass
    if-nez v15, LabelExit
        
; !(SubClass instanceof SuperInterface2)    
    new-instance v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass/<init>()V    
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperInterface2
    if-nez v15, LabelExit

; !(SubClass[] instanceof SuperInterface)    
    new-array v10, v11, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass;
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SuperInterface
    if-nez v15, LabelExit

; !(SubClass[] instanceof SubClass)    
    new-array v10, v11, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass;
    instance-of v15, v10, dot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass
    if-nez v15, LabelExit
    
; !(SuperClass[] instanceof SubClass[])    
    new-array v10, v11, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SuperClass;
    instance-of v15, v10, [Ldot/junit/opcodes/instance_of/d/T_instance_of_2/SubClass;
    if-nez v15, LabelExit
    
    const v0, 1
    
LabelExit:        
    return v0
    
.end method


