.source all_opcodes.java
.class public dasm.test.all_opcodes
.super java/lang/Object
.implements java/lang/Runnable 

.field public pub_field I
.field public static static_field J

.method public <init>()V
    return-void
.end method

.method public run()I
.throws java/lang/NullPointerException
    .limit regs 4
nop
move v1, v2
move/from16 v11, v222
move/16 v111, v222
move-wide v1, v2
move-wide/from16 v11, v222
move-wide/16 v111, v222
move-object v1, v2
move-object/from16 v11, v222
move-object/16 v111, v222
move-result v11
move-result-wide v11
move-result-object v11
move-exception v11
return-void
return v11
return-wide v11
return-object v11
const/4 v1, 1
const/16 v11, 0x1234
const v11, 0x12345678
const/high16 v11, 0x12340000
const-wide/16 v11, 0x1234
const-wide/32 v11, 0x12345678
const-wide v11, 3.1415
const-wide/high16 v11, 0x1234000000000000
const-string v11, "abc"
const-string/jumbo v11, "abcd"
const-class v11, java/lang/Object
monitor-enter v11
monitor-exit v11
check-cast v11, java/lang/Object
instance-of v1, v2, java/lang/Object
array-length v1, v2
new-instance v11, java/lang/Object
new-array v1, v2, java/lang/Object
filled-new-array {v1, v2, v3, v4, v5}, I
filled-new-array/range {v3..v7}, D
fill-array-data v11 I
        1
        2
        3
        4
fill-array-data-end
throw v11
goto $+1
goto/16 Label1
Label1:
goto/32 Label2
Label2:
packed-switch v11, 1
        Label1
        Label3
packed-switch-end
Label3:
sparse-switch v11
        1 : Label2
        33 : Label4
sparse-switch-end
Label4:

cmpl-float v11, v22, v33
cmpg-float v11, v22, v33  
cmpl-double v11, v22, v33  
cmpg-double v11, v22, v33  
cmp-long v11, v22, v33
if-eq v1, v2, Label1  
if-ne v1, v2, $+1  
if-lt v1, v2, Label1  
if-ge v1, v2, $-1  
if-gt v1, v2, Label1 
if-eqz v11, Label1 
if-nez v11, $+1
if-ltz v11, Label1 
if-gez v11, $+1
if-gtz v11, Label1 
if-lez v11, $-1
aget v11, v22, v33
aget-wide v11, v22, v33 
aget-object v11, v22, v33 
aget-boolean v11, v22, v33 
aget-byte v11, v22, v33 
aget-char v11, v22, v33 
aget-short v11, v22, v33 
aput v11, v22, v33 
aput-wide v11, v22, v33 
aput-object v11, v22, v33 
aput-boolean v11, v22, v33 
aput-byte v11, v22, v33 
aput-char v11, v22, v33 
aput-short v11, v22, v33
iget v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-wide v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-object v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-boolean v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-byte v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-char v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iget-short v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-wide v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-object v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-boolean v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-byte v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-char v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
iput-short v1, v2, dxc.junit.opcodes.nop.jm.T_nop_1.pub_field I
sget v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-wide v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-object v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-boolean v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-byte v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-char v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sget-short v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-wide v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-object v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-boolean v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-byte v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-char v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I
sput-short v1, dxc.junit.opcodes.nop.jm.T_nop_1.static_field I

invoke-virtual {v1}, java/lang/Math/sqrt(D)D
invoke-super {v1}, java/lang/Math/sqrt(D)D
invoke-direct {v1}, java/lang/Math/sqrt(D)D 
invoke-static {v1}, java/lang/Math/sqrt(D)D 
;invoke-interface {v1}, java/lang/Math/sqrt(D)D

invoke-virtual/range {v1..v4}, java/lang/Math/sqrt(D)D 
invoke-super/range {v1..v4}, java/lang/Math/sqrt(D)D
invoke-direct/range {v1..v4}, java/lang/Math/sqrt(D)D
invoke-static/range {v1..v4}, java/lang/Math/sqrt(D)D
;invoke-interface/range {v1..v4}, java/lang/Math/sqrt(D)D

neg-int v1, v2
not-int v1, v2
neg-long v1, v2
not-long v1, v2
neg-float v1, v2
neg-double v1, v2
int-to-long v1, v2
int-to-float v1, v2
int-to-double v1, v2
long-to-int v1, v2
long-to-float v1, v2
long-to-double v1, v2
float-to-int v1, v2
float-to-long v1, v2
float-to-double v1, v2
double-to-int v1, v2
double-to-long v1, v2
double-to-float v1, v2
int-to-byte v1, v2
int-to-char v1, v2
int-to-short v1, v2
add-int v11, v22, v33
sub-int v11, v22, v33
mul-int v11, v22, v33
div-int v11, v22, v33
rem-int v11, v22, v33
and-int v11, v22, v33
or-int v11, v22, v33
xor-int v11, v22, v33
shl-int v11, v22, v33
shr-int v11, v22, v33
ushr-int v11, v22, v33
add-long v11, v22, v33
sub-long v11, v22, v33
mul-long v11, v22, v33
div-long v11, v22, v33
rem-long v11, v22, v33
and-long v11, v22, v33
or-long v11, v22, v33
xor-long v11, v22, v33
shl-long v11, v22, v33
shr-long v11, v22, v33
ushr-long v11, v22, v33
add-float v11, v22, v33
sub-float v11, v22, v33
mul-float v11, v22, v33
div-float v11, v22, v33
rem-float v11, v22, v33
add-double v11, v22, v33
sub-double v11, v22, v33
mul-double v11, v22, v33
div-double v11, v22, v33
rem-double v11, v22, v33
add-int/2addr v1, v2 
sub-int/2addr v1, v2
mul-int/2addr v1, v2
div-int/2addr v1, v2
rem-int/2addr v1, v2
and-int/2addr v1, v2
or-int/2addr v1, v2
xor-int/2addr v1, v2
shl-int/2addr v1, v2
shr-int/2addr v1, v2
ushr-int/2addr v1, v2
add-long/2addr v1, v2
sub-long/2addr v1, v2
mul-long/2addr v1, v2
div-long/2addr v1, v2
rem-long/2addr v1, v2
and-long/2addr v1, v2
or-long/2addr v1, v2
xor-long/2addr v1, v2
shl-long/2addr v1, v2
shr-long/2addr v1, v2
ushr-long/2addr v1, v2
add-float/2addr v1, v2
sub-float/2addr v1, v2
mul-float/2addr v1, v2
div-float/2addr v1, v2
rem-float/2addr v1, v2
add-double/2addr v1, v2
sub-double/2addr v1, v2
mul-double/2addr v1, v2
div-double/2addr v1, v2
rem-double/2addr v1, v2
add-int/lit16 v1, v2, 0x1234
rsub-int v1, v2, 0x1234
mul-int/lit16 v1, v2, 0x1234
div-int/lit16 v1, v2, 0x1234
rem-int/lit16 v1, v2, 0x1234
and-int/lit16 v1, v2, 0x1234
or-int/lit16 v1, v2, 0x1234
xor-int/lit16 v1, v2, 0x1234
add-int/lit8 v1, v2, 0x12 
rsub-int/lit8 v1, v2, 0x12
mul-int/lit8 v1, v2, 0x12
div-int/lit8 v1, v2, 0x12
rem-int/lit8 v1, v2, 0x12
and-int/lit8 v1, v2, 0x12
or-int/lit8 v1, v2, 0x12
xor-int/lit8 v1, v2, 0x12
shl-int/lit8 v1, v2, 0x12
shr-int/lit8 v1, v2, 0x12
ushr-int/lit8 v1, v2, 0x12

.catch java/lang/Exception from Label1 to Label2 using Label3
.end method

.source test_interface.java
.interface public dasm.test.interface.test_interface

.method public native test()V
.end method