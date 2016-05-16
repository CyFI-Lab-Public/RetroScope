.source T_rem_double_2addr_5.java
.class public dot.junit.opcodes.rem_double_2addr.d.T_rem_double_2addr_5
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(DD)D
.limit regs 14

       rem-double/2addr v9, v12
       return-wide v9
.end method
