.source T_if_lt_12.java
.class public dot.junit.opcodes.if_lt.d.T_if_lt_12
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(II)Z
.limit regs 8

       if-lt v6, v7, Label10
       const/4 v6, 0
       return v6

Label10:
       nop
       return v6
.end method
