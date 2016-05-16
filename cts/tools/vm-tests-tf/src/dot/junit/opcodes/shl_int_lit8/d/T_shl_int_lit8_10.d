.source T_shl_int_lit8_10.java
.class public dot.junit.opcodes.shl_int_lit8.d.T_shl_int_lit8_10
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(J)I
.limit regs 8

       shl-int/lit8 v0, v5, 2
       return v0
.end method
