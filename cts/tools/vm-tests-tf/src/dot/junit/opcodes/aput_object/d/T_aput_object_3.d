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
.interface abstract dot.junit.opcodes.aput_object.d.SuperInterface

.source TestStubs.java
.interface public dot.junit.opcodes.aput_object.d.SuperInterface2
    
.source TestStubs.java    
.class public dot.junit.opcodes.aput_object.d.SuperClass 
.super java/lang/Object
.implements dot.junit.opcodes.aput_object.d.SuperInterface
.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method
    
.source TestStubs.java
.class public dot.junit.opcodes.aput_object.d.SubClass 
.super dot.junit.opcodes.aput_object.d.SuperClass
.method public <init>()V
.limit regs 1

       invoke-direct {v0}, dot/junit/opcodes/aput_object/d/SuperClass/<init>()V
       return-void
.end method


.source T_aput_object_3.java
.class public dot.junit.opcodes.aput_object.d.T_aput_object_3
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()I
.limit regs 32

    const v1, 0

    const v0, 1
    ; v2 = SubClass[]
    new-array v2, v0, [Ldot/junit/opcodes/aput_object/d/SubClass;
    
    ; v3 =  SuperClass[]
    new-array v3, v0, [Ldot/junit/opcodes/aput_object/d/SuperClass;

    ; v4 =     SubClass
    new-instance v4, dot/junit/opcodes/aput_object/d/SubClass
    invoke-direct {v4}, dot/junit/opcodes/aput_object/d/SubClass/<init>()V

    ; v5 =     SuperClass
    new-instance v5, dot/junit/opcodes/aput_object/d/SuperClass
    invoke-direct {v5}, dot/junit/opcodes/aput_object/d/SuperClass/<init>()V

    ; v6 = SuperInterface[]
    new-array v6, v0, [Ldot/junit/opcodes/aput_object/d/SuperInterface;

    ; v7 =     Object[]
    new-array v7, v0, [Ljava/lang/Object;
    
    ; v8 = SuperInterface2[]
    new-array v8, v0, [Ldot/junit/opcodes/aput_object/d/SuperInterface2;
    
    const/4 v0, 0
    
; (SubClass -> SuperClass[])
    aput-object v4, v3, v0
    
; (SubClass -> SuperInterface[])
    aput-object v4, v6, v0
    
; (SubClass -> Object[])
    aput-object v4, v7, v0
        
; !(SuperClass -> SubClass[])    
Label10:        
    aput-object v5, v2, v0
Label11:    
    goto Label2
Label12:
    add-int/lit8 v1, v1, 1
    goto Label2
        
; !(SuperClass -> SuperInterface2[])    
Label2:
Label20:    
    aput-object v5, v8, v0
Label21:    
    goto Label3
Label22:
    add-int/lit8 v1, v1, 1
    goto Label3

; !(SubClass[] -> SuperInterface[])    
Label3:
Label30:    
    aput-object v2, v6, v0
Label31:    
    goto Label4
Label32:    
    add-int/lit8 v1, v1, 1
    goto Label4

Label4:
    return v1
    
.catch java/lang/ArrayStoreException from Label10 to Label11 using Label12
.catch java/lang/ArrayStoreException from Label20 to Label21 using Label22
.catch java/lang/ArrayStoreException from Label30 to Label31 using Label32
.end method


