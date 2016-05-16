.L_ZMB_LOP_SGET_WIDE:
    @ (Possibly Old) field ptr in r0 
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_WIDE_finish:

.L_ZMB_LOP_SGET_OBJECT:
    @ (Possibly Old) field ptr in r0 
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_OBJECT_finish:

.L_ZMB_LOP_SGET_BOOLEAN:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_BOOLEAN_finish

.L_ZMB_LOP_SGET_BYTE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_BYTE_finish

.L_ZMB_LOP_SGET_CHAR:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_CHAR_finish


.L_ZMB_LOP_SGET_SHORT:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_SHORT_finish


.L_ZMB_LOP_SPUT:      
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_finish:


.L_ZMB_LOP_SPUT_WIDE:      
    @ (Possibly Old) field ptr in r2
    push    {r0, r1, r3, r6, r9, r10, lr}
    mov     r0, r2
    bl      zmb_Static_SwitchFieldC
    mov     r2, r0
    pop     {r0, r1, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r2
    b .L_ZMB_LOP_SPUT_WIDE_finish


.L_ZMB_LOP_SPUT_OBJECT:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_OBJECT_finish


.L_ZMB_LOP_SPUT_BOOLEAN:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_BOOLEAN_finish



.L_ZMB_LOP_SPUT_BYTE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_BYTE_finish:     



.L_ZMB_LOP_SPUT_CHAR:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_CHAR_finish


.L_ZMB_LOP_SPUT_SHORT:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_SHORT_finish


.L_ZMB_LOP_INVOKE_DIRECT:
    @ (Original) Method is r0, "this" is r9
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r9
    mov     r2, #0
    bl      zmb_Invoke_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) Method is r0
    b .L_ZMB_LOP_INVOKE_DIRECT_finish


.L_ZMB_OP_INVOKE_INTERFACE_1:
    @ (Original) this->Clazz is r0, method is r2
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r2
    bl      zmb_Invoke_Interface_SwitchClazzC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match method's history) this->clazz is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_1_finish



.L_ZMB_OP_INVOKE_INTERFACE_2:
    @ (Original) Method is r0, "this" is r9
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r9
    mov     r2, #0
    bl      zmb_Invoke_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) Method is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_2_finish


.L_ZMB_OP_INVOKE_INTERFACE_1:
    @ (Original) this->Clazz is r0, method is r2
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r2
    bl      zmb_Invoke_Interface_SwitchClazzC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match method's history) this->clazz is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_1_finish



.L_ZMB_OP_INVOKE_INTERFACE_2:
    @ (Original) Method is r0, "this" is r9
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r9
    mov     r2, #0
    bl      zmb_Invoke_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) Method is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_2_finish



.L_ZMB_LOP_INVOKE_DIRECT_RANGE:
    @ (Original) Method is r0, "this" is r9
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r9
    mov     r2, #0
    bl      zmb_Invoke_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) Method is r0
    b .L_ZMB_LOP_INVOKE_DIRECT_RANGE_finish


.L_ZMB_OP_INVOKE_INTERFACE_RANGE_1:
    @ (Original) this->Clazz is r0, method is r2
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r2
    bl      zmb_Invoke_Interface_SwitchClazzC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match method's history) this->clazz is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_RANGE_1_finish



.L_ZMB_OP_INVOKE_INTERFACE_RANGE_2:
    @ (Original) Method is r0, "this" is r9
    push    {r1, r2, r3, r6, r9, r10, lr}
    mov     r1, r9
    mov     r2, #0
    bl      zmb_Invoke_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) Method is r0
    b .L_ZMB_OP_INVOKE_INTERFACE_RANGE_2_finish




.L_ZMB_LOP_SGET_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_VOLATILE_finish


.L_ZMB_LOP_SPUT_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_VOLATILE_finish


.L_ZMB_LOP_SGET_WIDE_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_WIDE_VOLATILE_finish


.L_ZMB_LOP_SPUT_WIDE_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r0, r1, r3, r6, r9, r10, lr}
    mov     r0, r2
    bl      zmb_Static_SwitchFieldC
    mov     r2, r0
    pop     {r0, r1, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_WIDE_VOLATILE_finish


.L_ZMB_L_OP_IGET_QUICK:
    @ (Original) field Offset in r1, Object in r3
    push    {r0, r2, r3, r6, r9, r10, lr}
    mov     r0, r1
    mov     r1, r3
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     r1, r0
    pop     {r0, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in r1
    b .L_ZMB_L_OP_IGET_QUICK_finish



.L_ZMB_L_OP_IGET_WIDE_QUICK:
    @ (Original) field Offset in ip, Object in r3
    push    {r0, r1, r2, r3, r6, r9, r10, lr}
    mov     r0, ip
    mov     r1, r3
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     ip, r0
    pop     {r0, r1, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in ip
    b .L_ZMB_L_OP_IGET_WIDE_QUICK_finish



.L_ZMB_L_OP_IGET_OBJECT_QUICK:
    @ (Original) field Offset in r1, Object in r3
    push    {r0, r2, r3, r6, r9, r10, lr}
    mov     r0, r1
    mov     r1, r3
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     r1, r0
    pop     {r0, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in r1
    b .L_ZMB_L_OP_IGET_OBJECT_QUICK_finish


.L_ZMB_L_OP_IPUT_QUICK:       
    @ (Original) field Offset in r1, Object in r3
    push    {r0, r2, r3, r6, r9, r10, lr}
    mov     r0, r1
    mov     r1, r3
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     r1, r0
    pop     {r0, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in r1
    b .L_ZMB_L_OP_IPUT_QUICK_finish



.L_ZMB_L_OP_IPUT_WIDE_QUICK:
    @ (Original) field Offset in r3, Object in r2
    push    {r0, r1, r2, r6, r9, r10, lr}
    mov     r0, r3
    mov     r1, r2
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     r3, r0
    pop     {r0, r1, r2, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in r3
    b .L_ZMB_L_OP_IPUT_WIDE_QUICK_finish



.L_ZMB_L_OP_IPUT_OBJECT_QUICK:
    @ (Original) field Offset in r1, Object in r3
    push    {r0, r2, r3, r6, r9, r10, lr}
    mov     r0, r1
    mov     r1, r3
    mov     r2, rPC
    bl      zmb_Inst_Quick_SwitchFieldOffsetC
    mov     r1, r0
    pop     {r0, r2, r3, r6, r9, r10, lr}
    @ (Possibly switched to match object's history) field Offset in r1
    b .L_ZMB_L_OP_IPUT_OBJECT_QUICK_finish


.L_ZMB_L_OP_INVOKE_VIRTUAL_QUICK:
    push    {r1, r2, r3, r6, r9, r10, lr}
    /* r0 = Existing Looked up function, r1 is offset in vtable, r2 is this, r3 is pc */
    mov     r2, r9
    ldr     r2, [r2, #offObject_clazz]  @ r2<- thisPtr->clazz
    mov     r3, rPC
    bl      zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    b .L_ZMB_L_OP_INVOKE_VIRTUAL_QUICK_finish



.L_ZMB_L_OP_INVOKE_VIRTUAL_QUICK_RANGE:
    push    {r1, r2, r3, r6, r9, r10, lr}
    /* r0 = Existing Looked up function, r1 is offset in vtable, r2 is this, r3 is pc */
    mov     r2, r9
    ldr     r2, [r2, #offObject_clazz]  @ r2<- thisPtr->clazz
    mov     r3, rPC
    bl      zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    b .L_ZMB_L_OP_INVOKE_VIRTUAL_QUICK_RANGE_finish



.L_ZMB_L_OP_INVOKE_SUPER_QUICK:
    push    {r1, r2, r3, r6, r9, r10, lr}
    /* r0 = Existing Looked up function, r1 is offset in vtable, r2 is super, r3 is pc */
    ldr     r2, [rSELF, #offThread_method]    @ r2<- current method
    ldr     r2, [r2, #offMethod_clazz]  @ r2<- method->clazz
    ldr     r2, [r2, #offClassObject_super]     @ r2<- method->clazz->super
    mov     r3, rPC
    bl      zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    b .L_ZMB_L_OP_INVOKE_SUPER_QUICK_finish


.L_ZMB_L_OP_INVOKE_SUPER_QUICK_RANGE:
    push    {r1, r2, r3, r6, r9, r10, lr}
    /* r0 = Existing Looked up function, r1 is offset in vtable, r2 is super, r3 is pc */
    ldr     r2, [rSELF, #offThread_method]    @ r2<- current method
    ldr     r2, [r2, #offMethod_clazz]  @ r2<- method->clazz
    ldr     r2, [r2, #offClassObject_super]     @ r2<- method->clazz->super
    mov     r3, rPC
    bl      zmb_INVOKE_VIRTUAL_QUICK_SwitchMethodC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    b .L_ZMB_L_OP_INVOKE_SUPER_QUICK_RANGE_finish


.L_ZMB_LOP_SGET_OBJECT_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SGET_OBJECT_VOLATILE_finish


.L_ZMB_LOP_SPUT_OBJECT_VOLATILE:
    @ (Possibly Old) field ptr in r0
    push    {r1, r2, r3, r6, r9, r10, lr}
    bl      zmb_Static_SwitchFieldC
    pop     {r1, r2, r3, r6, r9, r10, lr}
    @ (Non-Whitelisted Old/New) field Ptr in r0
    b .L_ZMB_LOP_SPUT_OBJECT_VOLATILE_finish








