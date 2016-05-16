.source T_rem_double_2addr_1.java
.class public dot.junit.opcodes.rem_double_2addr.d.T_rem_double_2addr_1
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(DD)D
.limit regs 14

       rem-double/2addr v10, v12
       return-wide v10
.end method
