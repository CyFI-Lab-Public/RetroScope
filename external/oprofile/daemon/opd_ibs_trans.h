/**
 * @file daemon/opd_ibs_trans.h
 * AMD Family10h Instruction Based Sampling (IBS) translation.
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jason Yeh <jason.yeh@amd.com>
 * @author Paul Drongowski <paul.drongowski@amd.com>
 * @author Suravee Suthikulpanit <suravee.suthikulpanit@amd.com>
 * Copyright (c) 2008 Advanced Micro Devices, Inc.
 */

#ifndef OPD_IBS_TRANS_H
#define OPD_IBS_TRANS_H

struct ibs_fetch_sample;
struct ibs_op_sample;
struct transient;

struct ibs_translation_table {
	unsigned int event;
	void (*translator)(struct transient *);
};


extern void trans_ibs_fetch (struct transient * trans, unsigned int selected_flag);
extern void trans_ibs_op (struct transient * trans, unsigned int selected_flag);
extern void trans_ibs_op_ls (struct transient * trans, unsigned int selected_flag);
extern void trans_ibs_op_nb (struct transient * trans, unsigned int selected_flag);
extern int  trans_ibs_op_rip_invalid (struct transient * trans);
extern void trans_ibs_op_mask_reserved (unsigned int family, struct transient * trans);
extern void trans_ibs_op_ls_memaccess(struct transient * trans);
extern void trans_ibs_op_bta (struct transient * trans);
#endif // OPD_IBS_TRANS_H
