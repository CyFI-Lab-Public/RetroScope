.source T_move_exception_3.java
.class public dot.junit.opcodes.move_exception.d.T_move_exception_3
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run()V
.limit regs 6

Label1:
       const v1, 1
       const v2, 0
       div-int v0, v1, v2 
       
Label2:
       goto Label4

Label3:
       move-exception v6

Label4:
       return-void

.catch all from Label1 to Label2 using Label3
.end method
