.source T_shl_int_2addr_2.java
.class public dot.junit.opcodes.shl_int_2addr.d.T_shl_int_2addr_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(II)I
.limit regs 8

       shl-int/2addr v7, v8
       return v7
.end method
