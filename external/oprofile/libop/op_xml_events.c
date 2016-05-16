/**
 * @file op_xml_events.c
 * routines for generating event files in XML
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#include <stdio.h>
#include <string.h>
#include "op_events.h"
#include "op_list.h"
#include "op_cpu_type.h"
#include "op_xml_out.h"

static op_cpu cpu_type;
#define MAX_BUFFER 16384
static char buffer[MAX_BUFFER];

void open_xml_events(char const * title, char const * doc, op_cpu the_cpu_type)
{
	char const * schema_version = "1.1";

	buffer[0] = '\0';
	cpu_type = the_cpu_type;
	open_xml_element(HELP_EVENTS, 1, buffer, MAX_BUFFER);
	init_xml_str_attr(SCHEMA_VERSION, schema_version, buffer, MAX_BUFFER);
	close_xml_element(NONE, 1, buffer, MAX_BUFFER);
	open_xml_element(HELP_HEADER, 1, buffer, MAX_BUFFER);
	init_xml_str_attr(HELP_TITLE, title, buffer, MAX_BUFFER);
	init_xml_str_attr(HELP_DOC, doc, buffer, MAX_BUFFER);
	close_xml_element(NONE, 0, buffer, MAX_BUFFER);
	printf("%s", buffer);
}

void close_xml_events(void)
{
	buffer[0] = '\0';
	close_xml_element(HELP_EVENTS, 0, buffer, MAX_BUFFER);
	printf("%s", buffer);
}

static void xml_do_arch_specific_event_help(struct op_event const *event,
					    char *buffer, size_t size)
{
	switch (cpu_type) {
	case CPU_PPC64_CELL:
		init_xml_int_attr(HELP_EVENT_GROUP, event->val / 100, buffer,
				  size);
		break;
	default:
		break;
	}
}


void xml_help_for_event(struct op_event const * event)
{
	uint i;
	int nr_counters;
	int has_nested = strcmp(event->unit->name, "zero");

	buffer[0] = '\0';
	open_xml_element(HELP_EVENT, 1, buffer, MAX_BUFFER);
	init_xml_str_attr(HELP_EVENT_NAME, event->name, buffer, MAX_BUFFER);
	xml_do_arch_specific_event_help(event, buffer, MAX_BUFFER);
	init_xml_str_attr(HELP_EVENT_DESC, event->desc, buffer, MAX_BUFFER);

	nr_counters = op_get_nr_counters(cpu_type);
	init_xml_int_attr(HELP_COUNTER_MASK, event->counter_mask, buffer,
			  MAX_BUFFER);
	if (event->ext)
		init_xml_str_attr(HELP_EXT, event->ext, buffer, MAX_BUFFER);
	init_xml_int_attr(HELP_MIN_COUNT, event->min_count,
			  buffer, MAX_BUFFER);

	if (has_nested) {
		char um_type[10];
		close_xml_element(NONE, 1, buffer, MAX_BUFFER);
		open_xml_element(HELP_UNIT_MASKS, 1, buffer, MAX_BUFFER);
		init_xml_int_attr(HELP_DEFAULT_MASK, event->unit->default_mask,
				  buffer, MAX_BUFFER);
		switch (event->unit->unit_type_mask){
		case utm_bitmask:
			strncpy(um_type, "bitmask", sizeof(um_type));
			break;
		case utm_exclusive:
			strncpy(um_type, "exclusive", sizeof(um_type));
			break;
		case utm_mandatory:
			strncpy(um_type, "mandatory", sizeof(um_type));
			break;
		}
		init_xml_str_attr(HELP_UNIT_MASKS_CATEGORY, um_type, buffer, MAX_BUFFER);
		close_xml_element(NONE, 1, buffer, MAX_BUFFER);
		for (i = 0; i < event->unit->num; i++) {
			open_xml_element(HELP_UNIT_MASK, 1, buffer, MAX_BUFFER);
			init_xml_int_attr(HELP_UNIT_MASK_VALUE,
					  event->unit->um[i].value,
					  buffer, MAX_BUFFER);
			init_xml_str_attr(HELP_UNIT_MASK_DESC,
					  event->unit->um[i].desc,
					  buffer, MAX_BUFFER);
			close_xml_element(NONE, 0, buffer, MAX_BUFFER);
		}
		close_xml_element(HELP_UNIT_MASKS, 0, buffer, MAX_BUFFER);
	}
	close_xml_element(has_nested ? HELP_EVENT : NONE, has_nested,
			  buffer, MAX_BUFFER);
	printf("%s", buffer);
}

