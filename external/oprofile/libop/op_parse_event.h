/**
 * @file op_parse_event.h
 * event parsing
 *
 * You can have silliness here.
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_PARSE_EVENT_H
#define OP_PARSE_EVENT_H

#include <stddef.h>

struct parsed_event {
	char * name;
	int count;
	int unit_mask;
	int kernel;
	int user;
	int unit_mask_valid;
};

/**
 * @param parsed_events  array of events to fill in
 * @param max_events  size of parsed_events
 * @param events  null terminated array of events string on the form
 *   event_name:count[:unit_mask:kernel:user]
 *
 * parse events given by the nil terminated array events and fill in
 * parsed_events with results. Events validity are not checked except.
 * A fatal error occur if number of events is greater than max_events.
 *
 * Return the number of events parsed.
 */
size_t parse_events(struct parsed_event * parsed_events, size_t max_events,
                    char const * const * events);

#endif /* !OP_PARSE_EVENT_H */
