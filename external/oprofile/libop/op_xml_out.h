/**
 * @file op_xml_out.h
 * utility routines for writing XML
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#ifndef OP_XML_OUT_H
#define OP_XML_OUT_H

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
	NONE=0, TABLE_ID, PROFILE,
	PROCESSOR, CPU_NAME, TITLE, SCHEMA_VERSION, MHZ,
	SETUP, 
	TIMER_SETUP, RTC_INTERRUPTS,
	EVENT_SETUP, EVENT_NAME, UNIT_MASK, SETUP_COUNT, SEPARATED_CPUS,
	OPTIONS, SESSION, DEBUG_INFO, DETAILS, EXCLUDE_DEPENDENT, EXCLUDE_SYMBOLS,
		IMAGE_PATH, INCLUDE_SYMBOLS, MERGE,
	CLASSES,
	CLASS,
		CPU_NUM,
		EVENT_NUM,
		EVENT_MASK,
	PROCESS, PROC_ID,
	THREAD, THREAD_ID,
	BINARY,
	MODULE, NAME,
	CALLERS, CALLEES,
	SYMBOL, ID_REF, SELFREF, DETAIL_LO, DETAIL_HI,
	SYMBOL_TABLE,
	SYMBOL_DATA, STARTING_ADDR,
		SOURCE_FILE, SOURCE_LINE, CODE_LENGTH,
	SUMMARY, SAMPLE,
	COUNT,
	DETAIL_TABLE, SYMBOL_DETAILS, DETAIL_DATA, VMA,
	BYTES_TABLE, BYTES,
	HELP_EVENTS,
	HELP_HEADER,
	HELP_TITLE,
	HELP_DOC,
	HELP_EVENT,
	HELP_EVENT_NAME,
	HELP_EVENT_GROUP,
	HELP_EVENT_DESC,
	HELP_COUNTER_MASK,
	HELP_MIN_COUNT,
	HELP_EXT,
	HELP_UNIT_MASKS,
	HELP_DEFAULT_MASK,
	HELP_UNIT_MASKS_CATEGORY,
	HELP_UNIT_MASK,
	HELP_UNIT_MASK_VALUE,
	HELP_UNIT_MASK_DESC
	} tag_t;

char const * xml_tag_name(tag_t tag);
void open_xml_element(tag_t tag, int with_attrs, char *buffer, size_t size);
void close_xml_element(tag_t tag, int has_nested, char *buffer, size_t size);
void init_xml_int_attr(tag_t attr, int value, char *buffer, size_t size);
void init_xml_dbl_attr(tag_t attr, double value, char *buffer, size_t size);
void init_xml_str_attr(tag_t attr, char const *str, char *buffer, size_t size);

#ifdef __cplusplus
}
#endif

#endif /* OP_XML_OUT_H */
