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

.source T_monitor_exit_1.java
.class public dot.junit.opcodes.monitor_exit.d.T_monitor_exit_1
.super java/lang/Object

.field public result Z

.method public <init>()V
.limit regs 4

       invoke-direct {v3}, java/lang/Object/<init>()V

       const/4 v2, 1
       iput-boolean v2, v3, dot.junit.opcodes.monitor_exit.d.T_monitor_exit_1.result Z

       return-void
.end method

.method public run(Ljava/lang/Object;)V
.limit regs 5

       new-instance v2, java/lang/Object
       invoke-direct {v2}, java/lang/Object/<init>()V
       monitor-enter v2
       monitor-exit v3
       return-void
.end method


