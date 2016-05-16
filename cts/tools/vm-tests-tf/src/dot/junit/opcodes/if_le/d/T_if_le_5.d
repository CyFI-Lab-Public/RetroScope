.source T_if_le_5.java
.class public dot.junit.opcodes.if_le.d.T_if_le_5
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(ID)I
.limit regs 8

       if-le v5, v6, Label11
       const/16 v5, 1234
       return v5
       
Label11:
       const/4 v5, 1
       return v5
.end method
