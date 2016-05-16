/**
 * @file op_msr.h
 * x86-specific MSR stuff
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_MSR_H
#define OP_MSR_H
 
/* work-around compiler bug in gcc 2.91.66, just mark all input register as
 * magically cloberred by wrmsr */
#if __GNUC__ == 2 && __GNUC_MINOR__ == 91
	#undef wrmsr
	#define wrmsr(msr, val1, val2)				\
		__asm__ __volatile__("wrmsr"			\
			/* no outputs */			\
			:					\
			: "c" (msr), "a" (val1), "d" (val2)	\
			: "ecx", "eax", "edx")
#endif

#ifndef MSR_IA32_MISC_ENABLE 
#define MSR_IA32_MISC_ENABLE 0x1a0
#endif

/* MSRs */
#ifndef MSR_P6_PERFCTR0
#define MSR_P6_PERFCTR0 0xc1
#endif
#ifndef MSR_P6_PERFCTR1
#define MSR_P6_PERFCTR1 0xc2
#endif
#ifndef MSR_P6_EVNTSEL0
#define MSR_P6_EVNTSEL0 0x186
#endif
#ifndef MSR_P6_EVNTSEL1
#define MSR_P6_EVNTSEL1 0x187
#endif
#ifndef MSR_K7_EVNTSEL0
#define MSR_K7_EVNTSEL0 0xc0010000
#endif
#ifndef MSR_K7_EVNTSEL1
#define MSR_K7_EVNTSEL1 0xc0010001
#endif
#ifndef MSR_K7_EVNTSEL2
#define MSR_K7_EVNTSEL2 0xc0010002
#endif
#ifndef MSR_K7_EVNTSEL3
#define MSR_K7_EVNTSEL3 0xc0010003
#endif
#ifndef MSR_K7_PERFCTR0
#define MSR_K7_PERFCTR0 0xc0010004
#endif
#ifndef MSR_K7_PERFCTR1
#define MSR_K7_PERFCTR1 0xc0010005
#endif
#ifndef MSR_K7_PERFCTR2
#define MSR_K7_PERFCTR2 0xc0010006
#endif
#ifndef MSR_K7_PERFCTR3
#define MSR_K7_PERFCTR3 0xc0010007
#endif

/* There are *82* pentium 4 MSRs:
   
   - 1 misc register

   - 18 counters (PERFCTRs)
   
   - 18 counter configuration control registers (CCCRs)
   
   - 45 event selection control registers (ESCRs). */


#ifndef MSR_P4_BPU_PERFCTR0
#define MSR_P4_BPU_PERFCTR0 0x300
#endif
#ifndef MSR_P4_BPU_PERFCTR1
#define MSR_P4_BPU_PERFCTR1 0x301
#endif
#ifndef MSR_P4_BPU_PERFCTR2
#define MSR_P4_BPU_PERFCTR2 0x302
#endif
#ifndef MSR_P4_BPU_PERFCTR3
#define MSR_P4_BPU_PERFCTR3 0x303
#endif
#ifndef MSR_P4_MS_PERFCTR0
#define MSR_P4_MS_PERFCTR0 0x304
#endif
#ifndef MSR_P4_MS_PERFCTR1
#define MSR_P4_MS_PERFCTR1 0x305
#endif
#ifndef MSR_P4_MS_PERFCTR2
#define MSR_P4_MS_PERFCTR2 0x306
#endif
#ifndef MSR_P4_MS_PERFCTR3
#define MSR_P4_MS_PERFCTR3 0x307
#endif
#ifndef MSR_P4_FLAME_PERFCTR0
#define MSR_P4_FLAME_PERFCTR0 0x308
#endif
#ifndef MSR_P4_FLAME_PERFCTR1
#define MSR_P4_FLAME_PERFCTR1 0x309
#endif
#ifndef MSR_P4_FLAME_PERFCTR2
#define MSR_P4_FLAME_PERFCTR2 0x30a
#endif
#ifndef MSR_P4_FLAME_PERFCTR3
#define MSR_P4_FLAME_PERFCTR3 0x30b
#endif
#ifndef MSR_P4_IQ_PERFCTR0
#define MSR_P4_IQ_PERFCTR0 0x30c
#endif
#ifndef MSR_P4_IQ_PERFCTR1
#define MSR_P4_IQ_PERFCTR1 0x30d
#endif
#ifndef MSR_P4_IQ_PERFCTR2
#define MSR_P4_IQ_PERFCTR2 0x30e
#endif
#ifndef MSR_P4_IQ_PERFCTR3
#define MSR_P4_IQ_PERFCTR3 0x30f
#endif
#ifndef MSR_P4_IQ_PERFCTR4
#define MSR_P4_IQ_PERFCTR4 0x310
#endif
#ifndef MSR_P4_IQ_PERFCTR5
#define MSR_P4_IQ_PERFCTR5 0x311
#endif


#ifndef MSR_P4_BPU_CCCR0
#define MSR_P4_BPU_CCCR0 0x360
#endif
#ifndef MSR_P4_BPU_CCCR1
#define MSR_P4_BPU_CCCR1 0x361
#endif
#ifndef MSR_P4_BPU_CCCR2
#define MSR_P4_BPU_CCCR2 0x362
#endif
#ifndef MSR_P4_BPU_CCCR3
#define MSR_P4_BPU_CCCR3 0x363
#endif
#ifndef MSR_P4_MS_CCCR0
#define MSR_P4_MS_CCCR0 0x364
#endif
#ifndef MSR_P4_MS_CCCR1
#define MSR_P4_MS_CCCR1 0x365
#endif
#ifndef MSR_P4_MS_CCCR2
#define MSR_P4_MS_CCCR2 0x366
#endif
#ifndef MSR_P4_MS_CCCR3
#define MSR_P4_MS_CCCR3 0x367
#endif
#ifndef MSR_P4_FLAME_CCCR0
#define MSR_P4_FLAME_CCCR0 0x368
#endif
#ifndef MSR_P4_FLAME_CCCR1
#define MSR_P4_FLAME_CCCR1 0x369
#endif
#ifndef MSR_P4_FLAME_CCCR2
#define MSR_P4_FLAME_CCCR2 0x36a
#endif
#ifndef MSR_P4_FLAME_CCCR3
#define MSR_P4_FLAME_CCCR3 0x36b
#endif
#ifndef MSR_P4_IQ_CCCR0
#define MSR_P4_IQ_CCCR0 0x36c
#endif
#ifndef MSR_P4_IQ_CCCR1
#define MSR_P4_IQ_CCCR1 0x36d
#endif
#ifndef MSR_P4_IQ_CCCR2
#define MSR_P4_IQ_CCCR2 0x36e
#endif
#ifndef MSR_P4_IQ_CCCR3
#define MSR_P4_IQ_CCCR3 0x36f
#endif
#ifndef MSR_P4_IQ_CCCR4
#define MSR_P4_IQ_CCCR4 0x370
#endif
#ifndef MSR_P4_IQ_CCCR5
#define MSR_P4_IQ_CCCR5 0x371
#endif


#ifndef MSR_P4_ALF_ESCR0
#define MSR_P4_ALF_ESCR0 0x3ca
#endif
#ifndef MSR_P4_ALF_ESCR1
#define MSR_P4_ALF_ESCR1 0x3cb
#endif
#ifndef MSR_P4_BPU_ESCR0
#define MSR_P4_BPU_ESCR0 0x3b2
#endif
#ifndef MSR_P4_BPU_ESCR1
#define MSR_P4_BPU_ESCR1 0x3b3
#endif
#ifndef MSR_P4_BSU_ESCR0
#define MSR_P4_BSU_ESCR0 0x3a0
#endif
#ifndef MSR_P4_BSU_ESCR1
#define MSR_P4_BSU_ESCR1 0x3a1
#endif
#ifndef MSR_P4_CRU_ESCR0
#define MSR_P4_CRU_ESCR0 0x3b8
#endif
#ifndef MSR_P4_CRU_ESCR1
#define MSR_P4_CRU_ESCR1 0x3b9
#endif
#ifndef MSR_P4_CRU_ESCR2
#define MSR_P4_CRU_ESCR2 0x3cc
#endif
#ifndef MSR_P4_CRU_ESCR3
#define MSR_P4_CRU_ESCR3 0x3cd
#endif
#ifndef MSR_P4_CRU_ESCR4
#define MSR_P4_CRU_ESCR4 0x3e0
#endif
#ifndef MSR_P4_CRU_ESCR5
#define MSR_P4_CRU_ESCR5 0x3e1
#endif
#ifndef MSR_P4_DAC_ESCR0
#define MSR_P4_DAC_ESCR0 0x3a8
#endif
#ifndef MSR_P4_DAC_ESCR1
#define MSR_P4_DAC_ESCR1 0x3a9
#endif
#ifndef MSR_P4_FIRM_ESCR0
#define MSR_P4_FIRM_ESCR0 0x3a4
#endif
#ifndef MSR_P4_FIRM_ESCR1
#define MSR_P4_FIRM_ESCR1 0x3a5
#endif
#ifndef MSR_P4_FLAME_ESCR0
#define MSR_P4_FLAME_ESCR0 0x3a6
#endif
#ifndef MSR_P4_FLAME_ESCR1
#define MSR_P4_FLAME_ESCR1 0x3a7
#endif
#ifndef MSR_P4_FSB_ESCR0
#define MSR_P4_FSB_ESCR0 0x3a2
#endif
#ifndef MSR_P4_FSB_ESCR1
#define MSR_P4_FSB_ESCR1 0x3a3
#endif
#ifndef MSR_P4_IQ_ESCR0
#define MSR_P4_IQ_ESCR0 0x3ba
#endif
#ifndef MSR_P4_IQ_ESCR1
#define MSR_P4_IQ_ESCR1 0x3bb
#endif
#ifndef MSR_P4_IS_ESCR0
#define MSR_P4_IS_ESCR0 0x3b4
#endif
#ifndef MSR_P4_IS_ESCR1
#define MSR_P4_IS_ESCR1 0x3b5
#endif
#ifndef MSR_P4_ITLB_ESCR0
#define MSR_P4_ITLB_ESCR0 0x3b6
#endif
#ifndef MSR_P4_ITLB_ESCR1
#define MSR_P4_ITLB_ESCR1 0x3b7
#endif
#ifndef MSR_P4_IX_ESCR0
#define MSR_P4_IX_ESCR0 0x3c8
#endif
#ifndef MSR_P4_IX_ESCR1
#define MSR_P4_IX_ESCR1 0x3c9
#endif
#ifndef MSR_P4_MOB_ESCR0
#define MSR_P4_MOB_ESCR0 0x3aa
#endif
#ifndef MSR_P4_MOB_ESCR1
#define MSR_P4_MOB_ESCR1 0x3ab
#endif
#ifndef MSR_P4_MS_ESCR0
#define MSR_P4_MS_ESCR0 0x3c0
#endif
#ifndef MSR_P4_MS_ESCR1
#define MSR_P4_MS_ESCR1 0x3c1
#endif
#ifndef MSR_P4_PMH_ESCR0
#define MSR_P4_PMH_ESCR0 0x3ac
#endif
#ifndef MSR_P4_PMH_ESCR1
#define MSR_P4_PMH_ESCR1 0x3ad
#endif
#ifndef MSR_P4_RAT_ESCR0
#define MSR_P4_RAT_ESCR0 0x3bc
#endif
#ifndef MSR_P4_RAT_ESCR1
#define MSR_P4_RAT_ESCR1 0x3bd
#endif
#ifndef MSR_P4_SAAT_ESCR0
#define MSR_P4_SAAT_ESCR0 0x3ae
#endif
#ifndef MSR_P4_SAAT_ESCR1
#define MSR_P4_SAAT_ESCR1 0x3af
#endif
#ifndef MSR_P4_SSU_ESCR0
#define MSR_P4_SSU_ESCR0 0x3be
#endif
/* guess: not defined in manual */
#ifndef MSR_P4_SSU_ESCR1
#define MSR_P4_SSU_ESCR1 0x3bf
#endif
#ifndef MSR_P4_TBPU_ESCR0
#define MSR_P4_TBPU_ESCR0 0x3c2
#endif
#ifndef MSR_P4_TBPU_ESCR1
#define MSR_P4_TBPU_ESCR1 0x3c3
#endif
#ifndef MSR_P4_TC_ESCR0
#define MSR_P4_TC_ESCR0 0x3c4
#endif
#ifndef MSR_P4_TC_ESCR1
#define MSR_P4_TC_ESCR1 0x3c5
#endif
#ifndef MSR_P4_U2L_ESCR0
#define MSR_P4_U2L_ESCR0 0x3b0
#endif
#ifndef MSR_P4_U2L_ESCR1
#define MSR_P4_U2L_ESCR1 0x3b1
#endif

/* Hyper-Threading */
#ifndef X86_FEATURE_HT
#define X86_FEATURE_HT		(0*32+28)
#endif

#endif /* OP_MSR_H */
