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

.source T_new_instance_12.java
.class public dot.junit.opcodes.new_instance.d.T_new_instance_12
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()V
.limit regs 6

    const v0, 0
Label1:
    new-instance v1,        java/lang/Integer
    if-nez v0, INIT
    move-object v2, v1
    const v0, 1
    goto Label1
INIT:
        
    invoke-direct {v1, v0}, java/lang/Integer/<init>(I)V
    invoke-virtual {v2}, java/lang/Integer/toString()Ljava/lang/String;

    return-void
.end method


