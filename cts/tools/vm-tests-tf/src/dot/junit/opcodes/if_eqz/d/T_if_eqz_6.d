.source T_ifne_6.java
.class public dot.junit.opcodes.if_eqz.d.T_if_eqz_6
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(D)I
.limit regs 8

       if-eqz v6, Label9
       const/16 v6, 1234
       return v6

Label9:
       const/4 v6, 1
       return v6
.end method
