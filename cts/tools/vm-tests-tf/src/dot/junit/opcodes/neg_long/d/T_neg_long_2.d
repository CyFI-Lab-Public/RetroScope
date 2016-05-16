.source T_neg_long_2.java
.class public dot.junit.opcodes.neg_long.d.T_neg_long_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(J)Z
.limit regs 14

       neg-long v10, v12
       
       not-long v8, v12
       const-wide v6, 1
       add-long v4, v6, v8

       cmp-long v3, v4, v10
       const/4 v1, 0
       if-eq v1, v3, Label1
       const/4 v0, 0

Label15:
       return v0
Label1:
       const/4 v0, 1
       goto Label15

.end method
