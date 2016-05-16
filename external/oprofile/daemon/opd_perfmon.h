/**
 * @file opd_perfmon.h
 * perfmonctl() handling
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef OPD_PERFMON_H
#define OPD_PERFMON_H

#ifdef __ia64__

#include <stdlib.h>

void perfmon_init(void);
void perfmon_exit(void);
void perfmon_start(void);
void perfmon_stop(void);

/* The following is from asm/perfmon.h. When it's installed on
 * enough boxes, we can remove this and include the platform
 * perfmon.h
 */

typedef unsigned char pfm_uuid_t[16];	/* custom sampling buffer identifier type */

/*
 * Request structure used to define a context
 */
typedef struct {
	pfm_uuid_t     ctx_smpl_buf_id;	 /* which buffer format to use (if needed) */
	unsigned long  ctx_flags;	 /* noblock/block */
	unsigned short ctx_nextra_sets;	 /* number of extra event sets (you always get 1) */
	unsigned short ctx_reserved1;	 /* for future use */
	int	       ctx_fd;		 /* return arg: unique identification for context */
	void	       *ctx_smpl_vaddr;	 /* return arg: virtual address of sampling buffer, is used */
	unsigned long  ctx_reserved2[11];/* for future use */
} pfarg_context_t;

/*
 * Request structure used to write/read a PMC or PMD
 */
typedef struct {
	unsigned int	reg_num;	   /* which register */
	unsigned short	reg_set;	   /* event set for this register */
	unsigned short	reg_reserved1;	   /* for future use */

	unsigned long	reg_value;	   /* initial pmc/pmd value */
	unsigned long	reg_flags;	   /* input: pmc/pmd flags, return: reg error */

	unsigned long	reg_long_reset;	   /* reset after buffer overflow notification */
	unsigned long	reg_short_reset;   /* reset after counter overflow */

	unsigned long	reg_reset_pmds[4]; /* which other counters to reset on overflow */
	unsigned long	reg_random_seed;   /* seed value when randomization is used */
	unsigned long	reg_random_mask;   /* bitmask used to limit random value */
	unsigned long   reg_last_reset_val;/* return: PMD last reset value */

	unsigned long	reg_smpl_pmds[4];  /* which pmds are accessed when PMC overflows */
	unsigned long	reg_smpl_eventid;  /* opaque sampling event identifier */

	unsigned long   reg_reserved2[3];   /* for future use */
} pfarg_reg_t;

typedef struct {
	pid_t		load_pid;	   /* process to load the context into */
	unsigned short	load_set;	   /* first event set to load */
	unsigned short	load_reserved1;	   /* for future use */
	unsigned long	load_reserved2[3]; /* for future use */
} pfarg_load_t;

#define PFM_WRITE_PMCS      0x01
#define PFM_WRITE_PMDS      0x02
#define PFM_STOP            0x04
#define PFM_START           0x05
#define PFM_CREATE_CONTEXT  0x08
#define PFM_LOAD_CONTEXT    0x10
#define PFM_FL_SYSTEM_WIDE  0x02

#else

void perfmon_init(void)
{
}


void perfmon_exit(void)
{
}


void perfmon_start(void)
{
}


void perfmon_stop(void)
{
}

#endif /* __ia64__ */

#endif /* OPD_PERFMON_H */
