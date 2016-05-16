.source T_shl_long_2.java
.class public dot.junit.opcodes.shl_long.d.T_shl_long_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(JI)J
.limit regs 11

       shl-long v0, v8, v11
       return-wide v0
.end method
