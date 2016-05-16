.source T_neg_int_2.java
.class public dot.junit.opcodes.neg_int.d.T_neg_int_2
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(I)Z
.limit regs 7

       neg-int v4, v6
       
       not-int v3, v6
       add-int/lit8 v2, v3, 1

       if-eq v4, v2, Label1
       const/4 v1, 0

Label15:
       return v1
Label1:
       const/4 v1, 1
       goto Label15
.end method
