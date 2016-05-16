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

.source T_invoke_direct_range_21.java
.class public dot.junit.opcodes.invoke_direct_range.d.T_invoke_direct_range_21
.super java/lang/Object


.method public <init>()V
.limit regs 2

       invoke-direct/range {v1}, java/lang/Object/<init>()V
       return-void
.end method

.method private test(II)I
.limit regs 7
       const v0, 999
       const v1, 888
       const v2, 777
       
       div-int v4, v5, v6
       
       return v4
.end method

.method public run()I
.limit regs 7
       const v0, 111
       const v1, 222
       const v2, 333
       
       const v4, 50
       const v5, 25
         move-object v3, v6
       invoke-direct/range {v3..v5}, dot/junit/opcodes/invoke_direct_range/d/T_invoke_direct_range_21/test(II)I
       move-result v3
       
       const v4, 2
       if-ne v3, v4, Label30
       
       const v4, 111
       if-ne v0, v4, Label30
       
       const v4, 222
       if-ne v1, v4, Label30
       
       const v4, 333
       if-ne v2, v4, Label30

       const v0, 1       
       return v0
Label30:
        const v0, 0
        return v0
        
.end method


