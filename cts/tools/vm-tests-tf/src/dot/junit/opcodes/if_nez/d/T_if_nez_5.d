.source T_ifne_5.java
.class public dot.junit.opcodes.if_nez.d.T_if_nez_5
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)I
.limit regs 6

       if-nez v6, Label9
       const/16 v6, 1234
       return v6

Label9:
       const/4 v6, 1
       return v6
.end method
