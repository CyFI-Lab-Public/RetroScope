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

.source T_return_object_6.java
.class public dot.junit.opcodes.return_object.d.T_return_object_6
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method private static test()Ljava/lang/String;
.limit regs 5

       const-string v0, "aaa"
       const-string v1, "bbb"
       const-string v2, "ccc"
       const-string v3, "ddd"
       return-object v3
.end method

.method public run()Ljava/lang/String;
.limit regs 10
       const-string v1, "a"
       const-string v2, "b"
       const-string v3, "c"
       invoke-static {}, dot/junit/opcodes/return_object/d/T_return_object_6/test()Ljava/lang/String;
       move-result-object v7

       const-string v8, "ddd"
       if-ne v7, v8, Label43

       const-string v7, "a"
       if-ne v1, v7, Label43

       const-string v7, "b"
       if-ne v2, v7, Label43

       const-string v7, "c"
       if-ne v3, v7, Label43

       const-string v0, "hello"
       return-object v0
Label43:
       const-string v0, "a"
       return-object v0
.end method


