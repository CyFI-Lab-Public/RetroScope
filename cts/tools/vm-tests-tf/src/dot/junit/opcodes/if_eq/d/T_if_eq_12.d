.source T_if_eq_12.java
.class public dot.junit.opcodes.if_eq.d.T_if_eq_12
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(II)I
.limit regs 8

       if-eq v6, v7, Label11
       const/16 v6, 1234
Label10:
       return v6
       
Label11:
       const/4 v6, 1
       goto Label10
.end method
