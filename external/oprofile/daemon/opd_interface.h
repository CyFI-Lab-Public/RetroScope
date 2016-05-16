/**
 * @file opd_interface.h
 *
 * Module / user space interface for 2.6 kernels and above
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * Modified by Aravind Menon for Xen
 * These modifications are:
 * Copyright (C) 2005 Hewlett-Packard Co.
 */

#ifndef OPD_INTERFACE_H
#define OPD_INTERFACE_H

#define CTX_SWITCH_CODE			1
#define CPU_SWITCH_CODE			2
#define COOKIE_SWITCH_CODE		3
#define KERNEL_ENTER_SWITCH_CODE	4
#define USER_ENTER_SWITCH_CODE		5
#define MODULE_LOADED_CODE		6
#define CTX_TGID_CODE			7
#define TRACE_BEGIN_CODE		8
/* Code 9 used to be TRACE_END_CODE which is not used anymore  */
/* Code 9 is now considered an unknown escape code             */
#define XEN_ENTER_SWITCH_CODE		10
/*
 * Ugly work-around for the unfortunate collision between Xenoprof's
 * DOMAIN_SWITCH_CODE (in use on x86) and Cell's SPU_PROFILING_CODE
 * (in use with Power):
 */
#if defined(__powerpc__)
#define SPU_PROFILING_CODE		11
#define SPU_CTX_SWITCH_CODE		12
#else
#define DOMAIN_SWITCH_CODE		11
/* Code 12 is now considered an unknown escape code */
#endif

/* AMD's Instruction-Based Sampling (IBS) escape code */
#define IBS_FETCH_SAMPLE		13
#define IBS_OP_SAMPLE			14
#define LAST_CODE			15
 
#endif /* OPD_INTERFACE_H */
