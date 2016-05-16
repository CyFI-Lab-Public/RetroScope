.source T_shl_int_2addr_4.java
.class public dot.junit.opcodes.shl_int_2addr.d.T_shl_int_2addr_4
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(JI)I
.limit regs 8

       shl-int/2addr v5, v7
       return v5
.end method
