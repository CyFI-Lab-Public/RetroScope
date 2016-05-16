/**
 * @file parse_event_tests.c
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "op_parse_event.h"

struct events_test {
	/* second pointer is the null terminating array marker */
	char const * const tests[2];
	struct parsed_event expected;
};

static struct events_test const events[] = 
{
	{ { "FOO:3000:0:0:0", 0 },    { "FOO", 3000, 0, 0, 0, 0 } },
	{ { "BAR:3000", 0 },          { "BAR", 3000, 0, 1, 1, 0 } },
	{ { "FOOBAR:3000:1:1:1", 0 }, { "FOOBAR", 3000, 1, 1, 1, 0 } },
	{ { NULL, NULL },             { 0, 0, 0, 0, 0, 0 } }
};

static void do_test(struct events_test const * ev)
{
	struct parsed_event parsed;

	parse_events(&parsed, 1, ev->tests);

	if (strcmp(ev->expected.name, parsed.name) ||
	    ev->expected.count != parsed.count ||
	    ev->expected.unit_mask != parsed.unit_mask ||
	    ev->expected.kernel != parsed.kernel ||
	    ev->expected.user != parsed.user) {
		printf("for %s expect { %s, %d, %d, %d, %d } found "
		       "{ %s, %d, %d, %d, %d }\n",
                       ev->tests[0], ev->expected.name, ev->expected.count,
                       ev->expected.unit_mask, ev->expected.kernel,
                       ev->expected.user, parsed.name, parsed.count,
                       parsed.unit_mask, parsed.kernel, parsed.user);
		exit(EXIT_FAILURE);
	}
}

int main(void)
{
	struct events_test const * ev;
	for (ev = events; ev->tests[0]; ++ev)
		do_test(ev);

	return 0;
}
