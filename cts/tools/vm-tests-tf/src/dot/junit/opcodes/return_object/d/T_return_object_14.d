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

.source T_return_object_14.java
.class public dot.junit.opcodes.return_object.d.T_return_object_14
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method private test()Ldot/junit/opcodes/return_object/d/TChild;
.limit regs 6

    new-instance v0, dot/junit/opcodes/return_object/d/TSuper
    invoke-direct {v0}, dot/junit/opcodes/return_object/d/TSuper/<init>()V

    return-object v0
.end method


.method public run()Z
.limit regs 6

    invoke-direct {v5}, dot/junit/opcodes/return_object/d/T_return_object_14/test()Ldot/junit/opcodes/return_object/d/TChild;
    move-result-object v1

    instance-of v0, v1, dot/junit/opcodes/return_object/d/TChild

    return v0

.end method


