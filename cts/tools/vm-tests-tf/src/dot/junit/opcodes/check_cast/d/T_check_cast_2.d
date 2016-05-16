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
.interface public dot.junit.opcodes.check_cast.d.T_check_cast_2.SuperInterface

.source TestStubs.java    
.interface public dot.junit.opcodes.check_cast.d.T_check_cast_2.SuperInterface2

.source TestStubs.java
.class public dot.junit.opcodes.check_cast.d.T_check_cast_2.SuperClass 
.super java/lang/Object
.implements dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperInterface
    
.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method    

.source TestStubs.java
.class public dot.junit.opcodes.check_cast.d.T_check_cast_2.SubClass 
.super dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass/<init>()V
       return-void
.end method


.source T_check_cast_2.java
.class public dot.junit.opcodes.check_cast.d.T_check_cast_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()I
.limit regs 20

    const v1, 0
    
; (SubClass instanceof SuperClass)    
    new-instance v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass/<init>()V
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass
    
; (SubClass[] instanceof SuperClass[])    
    const v11, 1
    new-array v10, v11, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass;
    check-cast v10, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass;

    
; (SubClass[] instanceof Object)    
    new-array v10, v11, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass;
    check-cast v10, java/lang/Object
    
; (SubClass instanceof SuperInterface)    
    new-instance v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass/<init>()V    
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperInterface
        

; !(SuperClass instanceof SubClass)    
Label1:
    new-instance v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass
    invoke-direct {v10}, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass/<init>()V
Label10:    
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass
Label11:    
    goto Label2
Label12:
    add-int/lit16 v1, v1, 1
    goto Label2
        
; !(SubClass instanceof SuperInterface2)    
Label2:
    new-instance v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass
    invoke-direct {v10}, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass/<init>()V    
Label20:    
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperInterface2
Label21:    
    goto Label3
Label22:
    add-int/lit16 v1, v1, 1
    goto Label3

; !(SubClass[] instanceof SuperInterface)    
Label3:
    new-array v10, v11, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass;
Label30:    
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SuperInterface
Label31:    
    goto Label4
Label32:    
    add-int/lit16 v1, v1, 1
    goto Label4

; !(SubClass[] instanceof SubClass)    
Label4:
    new-array v10, v11, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass;
Label40:    
    check-cast v10, dot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass
Label41:    
    goto Label5
Label42:
    add-int/lit16 v1, v1, 1
    goto Label5    
    
; !(SuperClass[] instanceof SubClass[])    
Label5:
    new-array v10, v11, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SuperClass;
Label50:    
    check-cast v10, [Ldot/junit/opcodes/check_cast/d/T_check_cast_2/SubClass;
Label51:    
    goto Label6
Label52:
    add-int/lit16 v1, v1, 1
    
Label6:        
    return v1
    
.catch java/lang/ClassCastException from Label10 to Label11 using Label12
.catch java/lang/ClassCastException from Label20 to Label21 using Label22
.catch java/lang/ClassCastException from Label30 to Label31 using Label32
.catch java/lang/ClassCastException from Label40 to Label41 using Label42
.catch java/lang/ClassCastException from Label50 to Label51 using Label52
.end method


