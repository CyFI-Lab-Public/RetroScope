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

.source T_monitor_enter_1.java
.class public dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1
.super java/lang/Object

.field public counter I

.method public <init>()V
.limit regs 2

       invoke-direct {v1}, java/lang/Object/<init>()V

       const/4 v0, 0

       iput v0, v1, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I
       return-void
.end method

.method public run()V
.limit regs 8
       monitor-enter v7
Label8:
       iget v1, v7, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I

       const-wide/16 v3, 500
       invoke-static {v3, v4}, java/lang/Thread/sleep(J)V
       
       iget v2, v7, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I
       
       if-ne v1, v2, Label0
       
       add-int/lit8 v1, v1, 1
       iput v1, v7, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I
       monitor-exit v7
Label24:
       return-void
Label0:
       const/4 v5, -1
       iput v5, v7, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I
       monitor-exit v7
       return-void
       
Label25:
       move-exception v3
       monitor-exit v7
       const/4 v5, -1
       iput v5, v7, dot.junit.opcodes.monitor_enter.d.T_monitor_enter_1.counter I
       throw v3
.catch all from Label8 to Label24 using Label25
.end method
