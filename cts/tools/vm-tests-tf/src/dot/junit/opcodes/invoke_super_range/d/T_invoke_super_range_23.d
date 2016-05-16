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

.source T_invoke_super_range_23.java
.class public dot.junit.opcodes.invoke_super_range.d.T_invoke_super_range_23
.super dot/junit/opcodes/invoke_super_range/d/TSuper2


.method public <init>()V
.limit regs 2

       invoke-direct {v1}, dot/junit/opcodes/invoke_super_range/d/TSuper2/<init>()V
       return-void
.end method

.method public run()I
.limit regs 6

       new-instance v2, dot/junit/opcodes/invoke_super_range/d/TSuper
       invoke-direct {v2}, dot/junit/opcodes/invoke_super_range/d/TSuper/<init>()V

       invoke-super/range {v2}, dot/junit/opcodes/invoke_super_range/d/TSuper2/test()I

       move-result v0
       return v0
.end method


