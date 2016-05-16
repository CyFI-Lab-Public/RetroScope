/**
 * @file op_config_24.h
 *
 * Parameters a user may want to change
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_CONFIG_24_H
#define OP_CONFIG_24_H

#define OP_MOUNT "/proc/sys/dev/oprofile/"

extern char op_device[];
extern char op_note_device[];
extern char op_hash_device[];

/*@{\name module default/min/max settings */

/** 65536 * sizeof(op_sample) */
#define OP_DEFAULT_BUF_SIZE 65536
/** 
 * we don't try to wake-up daemon until it remains more than this free entry
 * in eviction buffer 
 */
#define OP_PRE_WATERMARK(buffer_size)			\
	(((buffer_size) / 8) < OP_MIN_PRE_WATERMARK	\
		? OP_MIN_PRE_WATERMARK			\
		: (buffer_size) / 8)
/** minimal buffer water mark before we try to wakeup daemon */
#define OP_MIN_PRE_WATERMARK 8192
/** maximum number of entry in samples eviction buffer */
#define OP_MAX_BUF_SIZE	1048576
/** minimum number of entry in samples eviction buffer */
#define OP_MIN_BUF_SIZE	(32768 + OP_PRE_WATERMARK(32768))

/** 16384 * sizeof(op_note) = 273680 bytes default */
#define OP_DEFAULT_NOTE_SIZE 16384
/** 
 * we don't try to wake-up daemon until it remains more than this free entry
 * in note buffer 
 */
#define OP_PRE_NOTE_WATERMARK(note_size)		\
	(((note_size) / 32) < OP_MIN_NOTE_PRE_WATERMARK	\
		? OP_MIN_NOTE_PRE_WATERMARK		\
		: (note_size) / 32)
/** minimal note buffer water mark before we try to wakeup daemon */
#define OP_MIN_NOTE_PRE_WATERMARK 512
/** maximum number of entry in note buffer */
#define OP_MAX_NOTE_TABLE_SIZE	1048576
/** minimum number of entry in note buffer */
#define OP_MIN_NOTE_TABLE_SIZE	(1024 + OP_PRE_NOTE_WATERMARK(1024))

/** maximum sampling rate when using RTC */
#define OP_MAX_RTC_COUNT	4096
/** minimum sampling rate when using RTC */
#define OP_MIN_RTC_COUNT	2

/*@}*/

/** 
 * nr entries in hash map. This is the maximum number of name components
 * allowed. Must be a prime number 
 */
#define OP_HASH_MAP_NR 4093

/** size of string pool in bytes */
#define POOL_SIZE 65536

#ifndef NR_CPUS
/** maximum number of cpus present in the box */
#define NR_CPUS 32
#endif

#endif /* OP_CONFIG_24_H */
