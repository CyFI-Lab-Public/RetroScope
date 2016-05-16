/**
 * @file daemon/opd_ibs_macro.h
 * AMD Instruction Based Sampling (IBS) related macro.
 *
 * @remark Copyright 2008-2010 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jason Yeh <jason.yeh@amd.com>
 * @author Paul Drongowski <paul.drongowski@amd.com>
 * @author Suravee Suthikulpanit <suravee.suthikulpanit@amd.com>
 * Copyright (c) 2008 Advanced Micro Devices, Inc.
 */

#ifndef OPD_IBS_MACRO_H
#define OPD_IBS_MACRO_H

/**
 * The following defines are bit masks that are used to select
 * IBS fetch event flags and values at the
 * MSRC001_1030 IBS Fetch Control Register (IbsFetchCtl)
 */
#define FETCH_MASK_LATENCY  0x0000ffff
#define FETCH_MASK_COMPLETE 0x00040000
#define FETCH_MASK_IC_MISS  0x00080000
#define FETCH_MASK_PHY_ADDR 0x00100000
#define FETCH_MASK_PG_SIZE  0x00600000
#define FETCH_MASK_L1_MISS  0x00800000
#define FETCH_MASK_L2_MISS  0x01000000
#define FETCH_MASK_KILLED   \
		(FETCH_MASK_L1_MISS|FETCH_MASK_L2_MISS|FETCH_MASK_PHY_ADDR|\
		FETCH_MASK_COMPLETE|FETCH_MASK_IC_MISS)


/**
 * The following defines are bit masks that are used to select
 * IBS op event flags and values at the MSR level.
 */

/* MSRC001_1035 IBS Op Data Register (IbsOpData) */
#define BR_MASK_RETIRE           0x0000ffff
#define MASK_RIP_INVALID         0x00000040
#define BR_MASK_BRN_RET          0x00000020
#define BR_MASK_BRN_MISP         0x00000010
#define BR_MASK_BRN_TAKEN        0x00000008
#define BR_MASK_RETURN           0x00000004
#define BR_MASK_MISP_RETURN      0x00000002
#define BR_MASK_BRN_RESYNC       0x00000001

/* MSRC001_1036 IBS Op Data Register (IbsOpData2) */
#define NB_MASK_L3_STATE         0x00000020
#define NB_MASK_REQ_DST_PROC     0x00000010
#define NB_MASK_REQ_DATA_SRC     0x00000007

/* MSRC001_1037 IBS Op Data Register (IbsOpData3) */
#define DC_MASK_L2_HIT_1G        0x00080000
#define DC_MASK_PHY_ADDR_VALID   0x00040000
#define DC_MASK_LIN_ADDR_VALID   0x00020000
#define DC_MASK_MAB_HIT          0x00010000
#define DC_MASK_LOCKED_OP        0x00008000
#define DC_MASK_UC_MEM_ACCESS    0x00004000
#define DC_MASK_WC_MEM_ACCESS    0x00002000
#define DC_MASK_ST_TO_LD_CANCEL  0x00001000
#define DC_MASK_ST_TO_LD_FOR     0x00000800
#define DC_MASK_ST_BANK_CONFLICT 0x00000400
#define DC_MASK_LD_BANK_CONFLICT 0x00000200
#define DC_MASK_MISALIGN_ACCESS  0x00000100
#define DC_MASK_DC_MISS          0x00000080
#define DC_MASK_L2_HIT_2M        0x00000040
#define DC_MASK_L1_HIT_1G        0x00000020
#define DC_MASK_L1_HIT_2M        0x00000010
#define DC_MASK_L2_TLB_MISS      0x00000008
#define DC_MASK_L1_TLB_MISS      0x00000004
#define DC_MASK_STORE_OP         0x00000002
#define DC_MASK_LOAD_OP          0x00000001


/**
 * IBS derived events:
 *
 * IBS derived events are identified by event select values which are
 * similar to the event select values that identify performance monitoring
 * counter (PMC) events. Event select values for IBS derived events begin
 * at 0xf000.
 *
 * The definitions in this file *must* match definitions
 * of IBS derived events. More information
 * about IBS derived events is given in the Software Oprimization
 * Guide.
 */

/**
 * The following defines associate a 16-bit select value with an IBS
 * derived fetch event.
 */
#define DE_IBS_FETCH_ALL         0xf000
#define DE_IBS_FETCH_KILLED      0xf001
#define DE_IBS_FETCH_ATTEMPTED   0xf002
#define DE_IBS_FETCH_COMPLETED   0xf003
#define DE_IBS_FETCH_ABORTED     0xf004
#define DE_IBS_L1_ITLB_HIT       0xf005
#define DE_IBS_ITLB_L1M_L2H      0xf006
#define DE_IBS_ITLB_L1M_L2M      0xf007
#define DE_IBS_IC_MISS           0xf008
#define DE_IBS_IC_HIT            0xf009
#define DE_IBS_FETCH_4K_PAGE     0xf00a
#define DE_IBS_FETCH_2M_PAGE     0xf00b
#define DE_IBS_FETCH_1G_PAGE     0xf00c
#define DE_IBS_FETCH_XX_PAGE     0xf00d
#define DE_IBS_FETCH_LATENCY     0xf00e

#define IBS_FETCH_BASE           0xf000
#define IBS_FETCH_END            0xf00e
#define IBS_FETCH_MAX            (IBS_FETCH_END - IBS_FETCH_BASE + 1)
#define IS_IBS_FETCH(x)          (IBS_FETCH_BASE <= x && x <= IBS_FETCH_END)
#define IBS_FETCH_OFFSET(x)      (x - IBS_FETCH_BASE)
#define CHECK_FETCH_SELECTED_FLAG(x)	if ( selected_flag & (1 << IBS_FETCH_OFFSET(x)))


/**
 * The following defines associate a 16-bit select value with an IBS
 * derived branch/return macro-op event.
 */
#define DE_IBS_OP_ALL             0xf100
#define DE_IBS_OP_TAG_TO_RETIRE   0xf101
#define DE_IBS_OP_COMP_TO_RETIRE  0xf102
#define DE_IBS_BRANCH_RETIRED     0xf103
#define DE_IBS_BRANCH_MISP        0xf104
#define DE_IBS_BRANCH_TAKEN       0xf105
#define DE_IBS_BRANCH_MISP_TAKEN  0xf106
#define DE_IBS_RETURN             0xf107
#define DE_IBS_RETURN_MISP        0xf108
#define DE_IBS_RESYNC             0xf109

#define IBS_OP_BASE               0xf100
#define IBS_OP_END                0xf109
#define IBS_OP_MAX                (IBS_OP_END - IBS_OP_BASE + 1)
#define IS_IBS_OP(x)              (IBS_OP_BASE <= x && x <= IBS_OP_END)
#define IBS_OP_OFFSET(x)          (x - IBS_OP_BASE)
#define CHECK_OP_SELECTED_FLAG(x)	if ( selected_flag & (1 << IBS_OP_OFFSET(x)))


/**
 * The following defines associate a 16-bit select value with an IBS
 * derived load/store event.
 */
#define DE_IBS_LS_ALL_OP         0xf200
#define DE_IBS_LS_LOAD_OP        0xf201
#define DE_IBS_LS_STORE_OP       0xf202
#define DE_IBS_LS_DTLB_L1H       0xf203
#define DE_IBS_LS_DTLB_L1M_L2H   0xf204
#define DE_IBS_LS_DTLB_L1M_L2M   0xf205
#define DE_IBS_LS_DC_MISS        0xf206
#define DE_IBS_LS_DC_HIT         0xf207
#define DE_IBS_LS_MISALIGNED     0xf208
#define DE_IBS_LS_BNK_CONF_LOAD  0xf209
#define DE_IBS_LS_BNK_CONF_STORE 0xf20a
#define DE_IBS_LS_STL_FORWARDED  0xf20b
#define DE_IBS_LS_STL_CANCELLED  0xf20c
#define DE_IBS_LS_UC_MEM_ACCESS  0xf20d
#define DE_IBS_LS_WC_MEM_ACCESS  0xf20e
#define DE_IBS_LS_LOCKED_OP      0xf20f
#define DE_IBS_LS_MAB_HIT        0xf210
#define DE_IBS_LS_L1_DTLB_4K     0xf211
#define DE_IBS_LS_L1_DTLB_2M     0xf212
#define DE_IBS_LS_L1_DTLB_1G     0xf213
#define DE_IBS_LS_L1_DTLB_RES    0xf214
#define DE_IBS_LS_L2_DTLB_4K     0xf215
#define DE_IBS_LS_L2_DTLB_2M     0xf216
#define DE_IBS_LS_L2_DTLB_1G     0xf217
#define DE_IBS_LS_L2_DTLB_RES2   0xf218
#define DE_IBS_LS_DC_LOAD_LAT    0xf219

#define IBS_OP_LS_BASE           0xf200
#define IBS_OP_LS_END            0xf219
#define IBS_OP_LS_MAX            (IBS_OP_LS_END - IBS_OP_LS_BASE + 1)
#define IS_IBS_OP_LS(x)          (IBS_OP_LS_BASE <= x && x <= IBS_OP_LS_END)
#define IBS_OP_LS_OFFSET(x)      (x - IBS_OP_LS_BASE)
#define CHECK_OP_LS_SELECTED_FLAG(x)	if ( selected_flag & (1 << IBS_OP_LS_OFFSET(x)))


/**
 * The following defines associate a 16-bit select value with an IBS
 * derived Northbridge (NB) event.
 */
#define DE_IBS_NB_LOCAL          0xf240
#define DE_IBS_NB_REMOTE         0xf241
#define DE_IBS_NB_LOCAL_L3       0xf242
#define DE_IBS_NB_LOCAL_CACHE    0xf243
#define DE_IBS_NB_REMOTE_CACHE   0xf244
#define DE_IBS_NB_LOCAL_DRAM     0xf245
#define DE_IBS_NB_REMOTE_DRAM    0xf246
#define DE_IBS_NB_LOCAL_OTHER    0xf247
#define DE_IBS_NB_REMOTE_OTHER   0xf248
#define DE_IBS_NB_CACHE_STATE_M  0xf249
#define DE_IBS_NB_CACHE_STATE_O  0xf24a
#define DE_IBS_NB_LOCAL_LATENCY  0xf24b
#define DE_IBS_NB_REMOTE_LATENCY 0xf24c

#define IBS_OP_NB_BASE           0xf240
#define IBS_OP_NB_END            0xf24c
#define IBS_OP_NB_MAX            (IBS_OP_NB_END - IBS_OP_NB_BASE + 1)
#define IS_IBS_OP_NB(x)          (IBS_OP_NB_BASE <= x && x <= IBS_OP_NB_END)
#define IBS_OP_NB_OFFSET(x)      (x - IBS_OP_NB_BASE)
#define CHECK_OP_NB_SELECTED_FLAG(x)	if ( selected_flag & (1 << IBS_OP_NB_OFFSET(x)))


#define OP_MAX_IBS_COUNTERS      (IBS_FETCH_MAX + IBS_OP_MAX + IBS_OP_LS_MAX + IBS_OP_NB_MAX)


/**
 * These macro decodes IBS hardware-level event flags and fields.
 * Translation results are either zero (false) or non-zero (true), except
 * the fetch latency, which is a 16-bit cycle count, and the fetch page size
 * field, which is a 2-bit unsigned integer.
 */

/** Bits 47:32 IbsFetchLat: instruction fetch latency */
#define IBS_FETCH_FETCH_LATENCY(x)              ((unsigned short)(x->ibs_fetch_ctl_high & FETCH_MASK_LATENCY))

/** Bit 50 IbsFetchComp: instruction fetch complete. */
#define IBS_FETCH_FETCH_COMPLETION(x)           ((x->ibs_fetch_ctl_high & FETCH_MASK_COMPLETE) != 0)

/** Bit 51 IbsIcMiss: instruction cache miss. */
#define IBS_FETCH_INST_CACHE_MISS(x)            ((x->ibs_fetch_ctl_high & FETCH_MASK_IC_MISS) != 0)

/** Bit 52 IbsPhyAddrValid: instruction fetch physical address valid. */
#define IBS_FETCH_PHYS_ADDR_VALID(x)            ((x->ibs_fetch_ctl_high & FETCH_MASK_PHY_ADDR) != 0)

enum IBSL1PAGESIZE {
	L1TLB4K = 0,
	L1TLB2M,
	L1TLB1G,
	L1TLB_INVALID
};

/** Bits 54:53 IbsL1TlbPgSz: instruction cache L1TLB page size. */
#define IBS_FETCH_TLB_PAGE_SIZE(x)              ((unsigned short)((x->ibs_fetch_ctl_high >> 21) & 0x3))
#define IBS_FETCH_TLB_PAGE_SIZE_4K(x)           (IBS_FETCH_TLB_PAGE_SIZE(x) == L1TLB4K)
#define IBS_FETCH_TLB_PAGE_SIZE_2M(x)           (IBS_FETCH_TLB_PAGE_SIZE(x) == L1TLB2M)
#define IBS_FETCH_TLB_PAGE_SIZE_1G(x)           (IBS_FETCH_TLB_PAGE_SIZE(x) == L1TLB1G)

/** Bit 55 IbsL1TlbMiss: instruction cache L1TLB miss. */
#define IBS_FETCH_M_L1_TLB_MISS(x)              ((x->ibs_fetch_ctl_high & FETCH_MASK_L1_MISS) != 0)

/** Bit 56 IbsL2TlbMiss: instruction cache L2TLB miss. */
#define IBS_FETCH_L2_TLB_MISS(x)                ((x->ibs_fetch_ctl_high & FETCH_MASK_L2_MISS) != 0)

/** A fetch is a killed fetch if all the masked bits are clear */
#define IBS_FETCH_KILLED(x)                     ((x->ibs_fetch_ctl_high & FETCH_MASK_KILLED) == 0)

#define IBS_FETCH_INST_CACHE_HIT(x)             (IBS_FETCH_FETCH_COMPLETION(x) && !IBS_FETCH_INST_CACHE_MISS(x))

#define IBS_FETCH_L1_TLB_HIT(x)                 (!IBS_FETCH_M_L1_TLB_MISS(x) && IBS_FETCH_PHYS_ADDR_VALID(x))

#define IBS_FETCH_ITLB_L1M_L2H(x)               (IBS_FETCH_M_L1_TLB_MISS(x) && !IBS_FETCH_L2_TLB_MISS(x))

#define IBS_FETCH_ITLB_L1M_L2M(x)               (IBS_FETCH_M_L1_TLB_MISS(x) && IBS_FETCH_L2_TLB_MISS(x))


/**
 * These macros translates IBS op event data from its hardware-level
 * representation .It hides the MSR layout of IBS op data.
 */

/**
 * MSRC001_1035 IBS OP Data Register (IbsOpData)
 *
 * 15:0 IbsCompToRetCtr: macro-op completion to retire count
 */
#define IBS_OP_COM_TO_RETIRE_CYCLES(x)          ((unsigned short)(x->ibs_op_data1_low & BR_MASK_RETIRE))

/** 31:16 tag_to_retire_cycles : macro-op tag to retire count. */
#define IBS_OP_TAG_TO_RETIRE_CYCLES(x)          ((unsigned short)((x->ibs_op_data1_low >> 16) & BR_MASK_RETIRE))

/** 32 op_branch_resync : resync macro-op. */
#define IBS_OP_BRANCH_RESYNC(x)                 ((x->ibs_op_data1_high & BR_MASK_BRN_RESYNC) != 0)

/** 33 op_mispredict_return : mispredicted return macro-op. */
#define IBS_OP_MISPREDICT_RETURN(x)             ((x->ibs_op_data1_high & BR_MASK_MISP_RETURN) != 0)

/** 34 IbsOpReturn: return macro-op. */
#define IBS_OP_RETURN(x)                        ((x->ibs_op_data1_high & BR_MASK_RETURN) != 0)

/** 35 IbsOpBrnTaken: taken branch macro-op. */
#define IBS_OP_BRANCH_TAKEN(x)                  ((x->ibs_op_data1_high & BR_MASK_BRN_TAKEN) != 0)

/** 36 IbsOpBrnMisp: mispredicted branch macro-op.  */
#define IBS_OP_BRANCH_MISPREDICT(x)             ((x->ibs_op_data1_high & BR_MASK_BRN_MISP) != 0)

/** 37 IbsOpBrnRet: branch macro-op retired. */
#define IBS_OP_BRANCH_RETIRED(x)                ((x->ibs_op_data1_high & BR_MASK_BRN_RET) != 0)

/** 38 IbsRipInvalid: RIP invalid. */
#define IBS_OP_RIP_INVALID(x)                   ((x->ibs_op_data1_high & MASK_RIP_INVALID) != 0)

/**
 * MSRC001_1036 IBS Op Data 2 Register (IbsOpData2)
 *
 * 5 NbIbsReqCacheHitSt: IBS L3 cache state
 */
#define IBS_OP_NB_IBS_CACHE_HIT_ST(x)           ((x->ibs_op_data2_low & NB_MASK_L3_STATE) != 0)

/** 4 NbIbsReqDstProc: IBS request destination processor */
#define IBS_OP_NB_IBS_REQ_DST_PROC(x)           ((x->ibs_op_data2_low & NB_MASK_REQ_DST_PROC) != 0)

/** 2:0 NbIbsReqSrc: Northbridge IBS request data source */
#define IBS_OP_NB_IBS_REQ_SRC(x)                ((unsigned char)(x->ibs_op_data2_low & NB_MASK_REQ_DATA_SRC))

#define IBS_OP_NB_IBS_REQ_SRC_01(x)             (IBS_OP_NB_IBS_REQ_SRC(x) == 0x01)

#define IBS_OP_NB_IBS_REQ_SRC_02(x)             (IBS_OP_NB_IBS_REQ_SRC(x) == 0x02)

#define IBS_OP_NB_IBS_REQ_SRC_03(x)             (IBS_OP_NB_IBS_REQ_SRC(x) == 0x03)

#define IBS_OP_NB_IBS_REQ_SRC_07(x)             (IBS_OP_NB_IBS_REQ_SRC(x) == 0x07)

/**
 * MSRC001_1037 IBS Op Data3 Register
 *
 * Bits 47:32   IbsDcMissLat
 */
#define IBS_OP_DC_MISS_LATENCY(x)               ((unsigned short)(x->ibs_op_data3_high & 0xffff))

/** 0 IbsLdOp: Load op */
#define IBS_OP_IBS_LD_OP(x)                     ((x->ibs_op_data3_low & DC_MASK_LOAD_OP) != 0)

/** 1 IbsStOp: Store op */
#define IBS_OP_IBS_ST_OP(x)                     ((x->ibs_op_data3_low & DC_MASK_STORE_OP) != 0)

/** 2 ibs_dc_l1_tlb_miss: Data cache L1TLB miss */
#define IBS_OP_IBS_DC_L1_TLB_MISS(x)            ((x->ibs_op_data3_low & DC_MASK_L1_TLB_MISS) != 0)

/** 3 ibs_dc_l2_tlb_miss: Data cache L2TLB miss */
#define IBS_OP_IBS_DC_L2_TLB_MISS(x)            ((x->ibs_op_data3_low & DC_MASK_L2_TLB_MISS) != 0)

/** 4 IbsDcL1tlbHit2M: Data cache L1TLB hit in 2M page */
#define IBS_OP_IBS_DC_L1_TLB_HIT_2MB(x)         ((x->ibs_op_data3_low & DC_MASK_L1_HIT_2M) != 0)

/** 5 ibs_dc_l1_tlb_hit_1gb: Data cache L1TLB hit in 1G page */
#define IBS_OP_IBS_DC_L1_TLB_HIT_1GB(x)         ((x->ibs_op_data3_low & DC_MASK_L1_HIT_1G) != 0)

/** 6 ibs_dc_l2_tlb_hit_2mb: Data cache L2TLB hit in 2M page */
#define IBS_OP_IBS_DC_L2_TLB_HIT_2MB(x)         ((x->ibs_op_data3_low & DC_MASK_L2_HIT_2M) != 0)

/** 7 ibs_dc_miss: Data cache miss */
#define IBS_OP_IBS_DC_MISS(x)                   ((x->ibs_op_data3_low & DC_MASK_DC_MISS) != 0)

/** 8 ibs_dc_miss_acc: Misaligned access */
#define IBS_OP_IBS_DC_MISS_ACC(x)               ((x->ibs_op_data3_low & DC_MASK_MISALIGN_ACCESS) != 0)

/** 9 ibs_dc_ld_bnk_con: Bank conflict on load operation */
#define IBS_OP_IBS_DC_LD_BNK_CON(x)             ((x->ibs_op_data3_low & DC_MASK_LD_BANK_CONFLICT) != 0)

/** 10 ibs_dc_st_bnk_con: Bank conflict on store operation */
#define IBS_OP_IBS_DC_ST_BNK_CON(x)             ((x->ibs_op_data3_low & DC_MASK_ST_BANK_CONFLICT) != 0)

/** 11 ibs_dc_st_to_ld_fwd : Data forwarded from store to load operation */
#define IBS_OP_IBS_DC_ST_TO_LD_FWD(x)           ((x->ibs_op_data3_low & DC_MASK_ST_TO_LD_FOR) != 0)

/** 12 ibs_dc_st_to_ld_can: Data forwarding from store to load operation cancelled */
#define IBS_OP_IBS_DC_ST_TO_LD_CAN(x)           ((x->ibs_op_data3_low & DC_MASK_ST_TO_LD_CANCEL) != 0)

/** 13 ibs_dc_wc_mem_acc : WC memory access */
#define IBS_OP_IBS_DC_WC_MEM_ACC(x)             ((x->ibs_op_data3_low & DC_MASK_WC_MEM_ACCESS) != 0)

/** 14 ibs_dc_uc_mem_acc : UC memory access */
#define IBS_OP_IBS_DC_UC_MEM_ACC(x)             ((x->ibs_op_data3_low & DC_MASK_UC_MEM_ACCESS) != 0)

/** 15 ibs_locked_op: Locked operation */
#define IBS_OP_IBS_LOCKED_OP(x)                 ((x->ibs_op_data3_low & DC_MASK_LOCKED_OP) != 0)

/** 16 ibs_dc_mab_hit : MAB hit */
#define IBS_OP_IBS_DC_MAB_HIT(x)                ((x->ibs_op_data3_low & DC_MASK_MAB_HIT) != 0)

/** 17 IbsDcLinAddrValid: Data cache linear address valid */
#define IBS_OP_IBS_DC_LIN_ADDR_VALID(x)         ((x->ibs_op_data3_low & DC_MASK_LIN_ADDR_VALID) != 0)

/** 18 ibs_dc_phy_addr_valid: Data cache physical address valid */
#define IBS_OP_IBS_DC_PHY_ADDR_VALID(x)         ((x->ibs_op_data3_low & DC_MASK_PHY_ADDR_VALID) != 0)

/** 19 ibs_dc_l2_tlb_hit_1gb: Data cache L2TLB hit in 1G page */
#define IBS_OP_IBS_DC_L2_TLB_HIT_1GB(x)         ((x->ibs_op_data3_low & DC_MASK_L2_HIT_1G) != 0)


/**
 * Aggregate the IBS derived event. Increase the
 * derived event count by one.
 */
#define AGG_IBS_EVENT(EV)               opd_log_ibs_event(EV, trans)

/**
 * Aggregate the IBS latency/cycle counts. Increase the
 * derived event count by the specified count value.
 */
#define AGG_IBS_COUNT(EV, COUNT)        opd_log_ibs_count(EV, trans, COUNT)

#endif /*OPD_IBS_MACRO_H*/
