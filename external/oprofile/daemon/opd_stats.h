/**
 * @file daemon/opd_stats.h
 * Management of daemon statistics
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_STATS_H
#define OPD_STATS_H

extern unsigned long opd_stats[];

enum {	OPD_SAMPLES, /**< nr. samples */
	OPD_KERNEL, /**< nr. kernel samples */
	OPD_PROCESS, /**< nr. userspace samples */
	OPD_NO_CTX, /**< nr. samples lost due to not knowing if in the kernel or not */
	OPD_LOST_KERNEL,  /**< nr. kernel samples lost */
	OPD_LOST_SAMPLEFILE, /**< nr samples for which sample file can't be opened */
	OPD_LOST_NO_MAPPING, /**< nr samples lost due to no mapping */
	OPD_DUMP_COUNT, /**< nr. of times buffer is read */
	OPD_DANGLING_CODE, /**< nr. partial code notifications (buffer overflow */
	OPD_MAX_STATS /**< end of stats */
};

void opd_print_stats(void);

#endif /* OPD_STATS_H */
