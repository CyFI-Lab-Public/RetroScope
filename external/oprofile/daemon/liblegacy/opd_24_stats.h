/**
 * @file opd_24_stats.h
 * Management of daemon statistics
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_24_STATS_H
#define OPD_24_STATS_H

extern unsigned long opd_24_stats[];

enum {  OPD_KERNEL, /**< nr kernel samples */
	OPD_MODULE, /**< nr module samples */
	OPD_LOST_MODULE, /**< nr samples in module for which modules can not be located */
	OPD_LOST_PROCESS, /**< nr samples for which process info couldn't be accessed */
	OPD_PROCESS, /**< nr userspace samples */
	OPD_LOST_MAP_PROCESS, /**< nr samples for which map info couldn't be accessed */
	OPD_LOST_SAMPLEFILE, /**< nr samples for which sample file can't be opened */
	OPD_PROC_QUEUE_ACCESS, /**< nr accesses of proc queue */
	OPD_PROC_QUEUE_DEPTH, /**< cumulative depth of proc queue accesses */
	OPD_DUMP_COUNT, /**< nr of times buffer is read */
	OPD_MAP_ARRAY_ACCESS, /**< nr accesses of map array */
	OPD_MAP_ARRAY_DEPTH, /**< cumulative depth of map array accesses */
	OPD_IMAGE_HASH_ACCESS,  /**< nr opd_find_image() */
	OPD_IMAGE_HASH_DEPTH,  /**< cumulative depth of image search */
	OPD_SAMPLES, /**< nr distinct samples */
	OPD_NOTIFICATIONS, /**< nr notifications */
	OPD_MAX_STATS /**< end of stats */
	};

/** opd_print_24_stats - print out latest statistics */
void opd_print_24_stats(void);

#endif /* OPD_24_STATS_H */
