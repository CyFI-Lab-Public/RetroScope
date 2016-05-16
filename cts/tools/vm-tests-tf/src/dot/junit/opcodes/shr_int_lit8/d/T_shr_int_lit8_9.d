.source T_shr_int_lit8_9.java
.class public dot.junit.opcodes.shr_int_lit8.d.T_shr_int_lit8_9
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(J)I
.limit regs 8

       shr-int/lit8 v0, v6, 2
       return v0
.end method
