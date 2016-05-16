.source T_if_gtz_9.java
.class public dot.junit.opcodes.if_gtz.d.T_if_gtz_9
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)Z
.limit regs 6

       if-gtz v5, Label8
       const/4 v0, 0
       return v0

Label8:
       const v0, 0
       return v0
.end method
