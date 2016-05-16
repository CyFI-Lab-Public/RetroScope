.source T_if_lez_1.java
.class public dot.junit.opcodes.if_lez.d.T_if_lez_1
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)I
.limit regs 6

       if-lez v5, Label9
       const/16 v5, 1234
       return v5

Label9:
       const/4 v5, 1
       return v5
.end method
