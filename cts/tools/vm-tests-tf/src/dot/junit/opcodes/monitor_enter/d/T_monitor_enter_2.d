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

.source T_monitor_enter_2.java
.class public dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2
.super java/lang/Object

.field private flg I
.field public result Z

.method public <init>()V
.limit regs 4

       invoke-direct {v3}, java/lang/Object/<init>()V

       const/4 v2, 0
       iput v2, v3, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.flg I

       const/4 v2, 1
       iput-boolean v2, v3, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.result Z
       return-void
.end method

.method public run(I)V
.limit regs 10

       monitor-enter v8
Label13:
       monitor-enter v8
Label14:

       iput v9, v8, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.flg I
       
Label16:
       monitor-exit v8
Label20:

       const-wide/16 v4, 500

Label22:
       invoke-static {v4, v5}, java/lang/Thread/sleep(J)V
Label23:

       iget v1, v8, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.flg I

       if-eq v1, v9, Label35

       const/4 v5, 0
       iput-boolean v5, v8, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.result Z
       
Label35:
       monitor-exit v8
Label37:
       return-void
       
       
Label46:
       move-exception v4

       const/4 v6, 0
       iput-boolean v6, v8, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_2.result Z

       monitor-exit v8
Label53:
       throw v4
       
.catch all from Label13 to Label14 using Label46
.catch all from Label16 to Label20 using Label46
.catch all from Label22 to Label23 using Label46
.catch all from Label35 to Label37 using Label46
.catch all from Label46 to Label53 using Label46
.end method


