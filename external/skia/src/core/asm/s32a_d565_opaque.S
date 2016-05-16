/* s32a_d565_opaque.S
**
** Copyright 2009, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


        .text
        .align

        .global s32a_d565_opaque_arm

// void s32a_d565_opaque_arm(uint16_t*, uint32_t*, size_t)
// r0: dst ptr
// r1: src prt
// r2: count

s32a_d565_opaque_arm:
    stmdb sp!, {r4, r5, r6, r7, lr}
    subs r4, r2, #0
    mov r7, r1
    ble 1f
4:  ldr r1, [r7], #4 // r1=*src; src++
    cmp r1, #0
    mov lr, r1, lsr #24
    beq 2f // get next src

    cmp lr, #255 // 0xff
    mov r2, r1, lsl #8
    mov r6, r2
    moveq r2, r1, lsr #5
    andeq r3, r6, #63488 // 0xf800
    andeq r2, r2, #2016 // 0x7e0
    orreq r3, r3, r2
    orreq r3, r3, r6, lsr #27
    streqh r3, [r0], #2 // *dst = r3; dst++
    beq 3f // go to count--

    mov r3, r1, lsl #16
    rsb ip, lr, #255 // 0xff
    mov r5, r3, lsr #24
    ldrh r3, [r0]    // r3 = *dst
    mov lr, r6, lsr #24
    and r6, r1, #255 // 0xff

    mov r1, r3, lsr #5
    and r2, r1, #63
    smulbb r1, r2, ip
    add r2, r1, #32
    mov r1, r3, lsr #11
    add r2, r2, r2, lsr #6

    smulbb r1, r1, ip
    add r2, r5, r2, lsr #6
    add r1, r1, #16
    and r3, r3, #31 // 0x1f
    add r1, r1, r1, lsr #5

    smulbb r3, r3, ip
    add r1, r6, r1, lsr #5
    mov ip, r2, lsr #2
    add r3, r3, #16 // 0x10
    mov r1, r1, lsr #3

    add r3, r3, r3, lsr #5
    mov r2, ip, lsl #5
    add r3, lr, r3, lsr #5
    orr ip, r2, r1, lsl #11
    orr r1, ip, r3, lsr #3
    strh r1, [r0]

2:  add r0, r0, #2  // dst++
3:  subs r4, r4, #1 // r4 = --count
    bne 4b

1:  ldmia sp!, {r4, r5, r6, r7, pc}

