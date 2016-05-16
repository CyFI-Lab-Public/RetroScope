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

.source T_iput_wide_1.java
.class public dot.junit.opcodes.iput_wide.d.T_iput_wide_1
.super java/lang/Object

.field public  st_i1 J
.field protected  st_p1 J
.field private  st_pvt1 J

.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public  getPvtField()J
.limit regs 3

       iget-wide v0, v2, dot.junit.opcodes.iput_wide.d.T_iput_wide_1.st_pvt1 J
       return-wide v0
.end method

.method public run()V
.limit regs 3

       const-wide v0, 778899112233
       iput-wide v0, v2, dot.junit.opcodes.iput_wide.d.T_iput_wide_1.st_i1 J

       return-void
.end method


