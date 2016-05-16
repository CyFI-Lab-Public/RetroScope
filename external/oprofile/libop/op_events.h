/**
 * @file op_events.h
 * Details of PMC profiling events
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_EVENTS_H
#define OP_EVENTS_H

#ifdef __cplusplus
extern "C" {
#endif

#include "op_cpu_type.h"
#include "op_types.h"
#include "op_list.h"

/** Describe an unit mask type. Events can optionally use a filter called
 * the unit mask. the mask type can be a bitmask or a discrete value */
enum unit_mask_type {
	utm_mandatory,		/**< useless but required by the hardware */
	utm_exclusive,		/**< only one of the values is allowed */
	utm_bitmask		/**< bitmask */
};

/** up to thirty two allowed unit masks */
#define MAX_UNIT_MASK 32


/** Describe an unit mask. */
struct op_unit_mask {
	char * name;		/**< name of unit mask type */
	u32 num;		/**< number of possible unit masks */
	enum unit_mask_type unit_type_mask;
	u32 default_mask;	/**< only the gui use it */
	struct op_described_um {
		u32 value;
		char * desc;
	} um[MAX_UNIT_MASK];
	struct list_head um_next; /**< next um in list */
	int used;                 /**< used by events file parser */
};


/** Describe an event. */
struct op_event {
	u32 counter_mask;	/**< bitmask of allowed counter  */
	u32 val;		/**< event number */
	/** which unit mask if any allowed */
	struct op_unit_mask * unit;			
	char * name;		/**< the event name */
	char * desc;      	/**< the event description */
	int min_count;		/**< minimum counter value allowed */
	int filter;		/**< architecture specific filter or -1 */
	char * ext;		/**< extended events */
	struct list_head event_next;   /**< next event in list */
};

/** Return the known events list. Idempotent */
struct list_head * op_events(op_cpu cpu_type);

/** Find a given event, returns NULL on error */
struct op_event * op_find_event(op_cpu cpu_type, u32 nr, u32 um);
struct op_event * op_find_event_any(op_cpu cpu_type, u32 nr);

/** Find a given event by name */
struct op_event * find_event_by_name(char const * name, unsigned um,
                                     int um_valid);

/**
 * Find a mapping for a given event ID for architectures requiring additional information
 * from what is held in the events file. 
 */
char const * find_mapping_for_event(u32 val, op_cpu cpu_type);


/** op_check_events() return code */
enum op_event_check {
	OP_OK_EVENT = 0, /**< event is valid and allowed */
	OP_INVALID_EVENT = 1, /**< event number is invalid */
	OP_INVALID_UM = 2, /**< unit mask is invalid */
	OP_INVALID_COUNTER = 4 /**< event is not allowed for the given counter */
};

/**
 * sanity check event values
 * @param ctr counter number
 * @param event value for counter
 * @param um unit mask for counter
 * @param cpu_type processor type
 *
 * Check that the counter event and unit mask values are allowed.
 *
 * The function returns bitmask of failure cause 0 otherwise
 *
 * \sa op_cpu, OP_EVENTS_OK
 */
int op_check_events(int ctr, u32 event, u32 um, op_cpu cpu_type);

/**
 * free memory used by any call to above function. Need to be called only once
 */
void op_free_events(void);

struct op_default_event_descr {
	char * name;
	unsigned long count;
	unsigned long um;
};

/**
 * op_default_event - return the details of the default event
 * @param cpu_type  cpu type
 * @param descr filled event description
 *
 * Fills in the event description if applicable
 */
void op_default_event(op_cpu cpu_type, struct op_default_event_descr * descr);

#ifdef __cplusplus
}
#endif

#endif /* OP_EVENTS_H */
