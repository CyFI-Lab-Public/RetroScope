.source T_shr_int_2addr_6.java
.class public dot.junit.opcodes.shr_int_2addr.d.T_shr_int_2addr_6
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(FF)I
.limit regs 8

       shr-int/2addr v6, v7
       return v6
.end method
