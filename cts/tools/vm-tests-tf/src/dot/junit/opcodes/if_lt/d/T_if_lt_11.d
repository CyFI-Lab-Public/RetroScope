.source T_if_icmpge_11.java
.class public dot.junit.opcodes.if_lt.d.T_if_lt_11
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(IF)I
.limit regs 8

       if-lt v6, v7, Label11
       const/16 v6, 1234
       return v6

Label11:
       const/4 v6, 1
       return v6
.end method
