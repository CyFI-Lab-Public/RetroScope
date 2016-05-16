/**
 * @file op_xml_out.c
 * C utility routines for writing XML
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "op_xml_out.h"

char const * xml_tag_map[] = {
	"NONE",
	"id",
	"profile",
		"processor",
		"cputype",
		"title",
		"schemaversion",
		"mhz",
	"setup",
	"timersetup",
		"rtcinterrupts",
	"eventsetup",
		"eventname",
		"unitmask",
		"setupcount",
		"separatedcpus",
	"options",
		"session", "debuginfo", "details", "excludedependent",
		"excludesymbols", "imagepath", "includesymbols", "merge",
	"classes",
	"class",
		"cpu",
		"event",
		"mask",
	"process",
		"pid",
	"thread",
		"tid",
	"binary",
	"module",
		"name",
	"callers",
	"callees",
	"symbol",
		"idref",
		"self",
		"detaillo",
		"detailhi",
	"symboltable",
	"symboldata",
		"startingaddr",
		"file",
		"line",
		"codelength",
	"summarydata",
	"sampledata",
	"count",
	"detailtable",
	"symboldetails",
	"detaildata",
		"vmaoffset",
	"bytestable",
	"bytes",
	"help_events",
	"header",
		"title",
		"doc",
	"event",
		"event_name",
		"group",
		"desc",
		"counter_mask",
		"min_count",
		"ext",
	"unit_masks",
		"default",
		"category",
	"unit_mask",
		"mask",
		"desc"
};

#define MAX_BUF_LEN 2048
char const * xml_tag_name(tag_t tag)
{
	return xml_tag_map[tag];
}


void open_xml_element(tag_t tag, int with_attrs, char *buffer, size_t max)
{
	char *buf;
	int size, ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	ret = snprintf(buf, size, "<%s%s", xml_tag_name(tag),
		       (with_attrs ? " " : ">\n"));

	if (ret < 0 || ret >= size) {
		fprintf(stderr,"open_xml_element: snprintf failed\n");
		exit(EXIT_FAILURE);
	}
}


void close_xml_element(tag_t tag, int has_nested, char *buffer, size_t max)
{
	char *buf;
	int size, ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	if (tag == NONE)
		ret = snprintf(buf, size, "%s\n", (has_nested ? ">" : "/>"));
	else
		ret = snprintf(buf, size, "</%s>\n", xml_tag_name(tag));

	if (ret < 0 || ret >= size) {
		fprintf(stderr, "close_xml_element: snprintf failed\n");
		exit(EXIT_FAILURE);
	}
}


void init_xml_int_attr(tag_t attr, int value, char *buffer, size_t max)
{
	char *buf;
	int size, ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	ret = snprintf(buf, size, " %s=\"%d\"", xml_tag_name(attr), value);

	if (ret < 0 || ret >= size) {
		fprintf(stderr,"init_xml_int_attr: snprintf failed\n");
		exit(EXIT_FAILURE);
	}
}


void init_xml_dbl_attr(tag_t attr, double value, char *buffer, size_t max)
{
	char *buf;
	int size, ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	ret = snprintf(buf, size, " %s=\"%.2f\"", xml_tag_name(attr), value);

	if (ret < 0 || ret >= size) {
		fprintf(stderr, "init_xml_dbl_attr: snprintf failed\n");
		exit(EXIT_FAILURE);
	}
}


static void xml_quote(char const *str, char *buffer, size_t max)
{
	char *buf;
	char *quote;
	char *pos = (char*)str;
	size_t size;
	int ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	if (size < strlen(pos) + 2)
		goto Error;

	*buf = '"';
	buf++;
	size--;

	while (*pos) {
		switch(*pos) {
		case '&':
			quote = "&amp;";
			break;
		case '<':
			quote = "&lt;";
			break;
		case '>':
                        quote = "&gt;";
			break;
		case '"':
			quote = "&quot;";
			break;
		default:
			*buf = *pos;
			pos++;
			buf++;
			size--;
			continue;
		}

		pos++;
		ret = snprintf(buf, size, "%s", quote);
		if (ret < 0 || ret >= (int)size)
			goto Error;
		buf += ret;
		size -= ret;
		if (size < strlen(pos))
			goto Error;
	}

	if (!size)
		goto Error;

	*buf = '"';
	buf++;
	*buf = '\0';

	return;

Error:
	fprintf(stderr,"quote_str: buffer overflow\n");
	exit(EXIT_FAILURE);
}


void init_xml_str_attr(tag_t attr, char const *str, char *buffer, size_t max)
{
	char *buf;
	int size, ret;

	buffer[max - 1] = '\0';
	size = strlen(buffer);
	buf = &buffer[size];
	size = max - 1 - size;

	ret = snprintf(buf, size, " %s=", xml_tag_name(attr));
	if (ret < 0 || ret >= size)
		goto Error;

	buf += ret;
	size -= ret;

	if (!size)
		goto Error;

	xml_quote(str, buf, size);
	return;
Error:
	fprintf(stderr,"init_xml_str_attr: snprintf failed\n");
	exit(EXIT_FAILURE);
}
