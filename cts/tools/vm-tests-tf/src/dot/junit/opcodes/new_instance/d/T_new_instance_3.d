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

.source T_new_instance_3.java
.class public dot.junit.opcodes.new_instance.d.T_new_instance_3
.super java/lang/Object

.field static i I

.method static <clinit>()V
.limit regs 2

       const/16 v0, 123
       const/4 v1, 0
       div-int/lit8 v0, v0, 0
       sput v0, dot.junit.opcodes.new_instance.d.T_new_instance_3.i I
       return-void
.end method

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
Label5:
       return-void
.end method

.method public static run()I
.limit regs 4

       new-instance v1, dot/junit/opcodes/new_instance/d/T_new_instance_3
       invoke-direct {v1}, dot/junit/opcodes/new_instance/d/T_new_instance_3/<init>()V

       sget v1, dot.junit.opcodes.new_instance.d.T_new_instance_3.i I
       return v1
.end method


