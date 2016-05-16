/**
 * @file mangle_tests.c
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "op_libiberty.h"
#include "op_mangle.h"
#include "op_config.h"

struct test_input {
	struct mangle_values values;
	char const * result;
};

static struct test_input const tests[] = {
	{ { MANGLE_NONE, "foo", "", "bar", NULL, "EVENT", 0, 0, 0, 0, 0 },
	  "{root}/bar/{dep}/{root}/foo/EVENT.0.0.all.all.all" },
	{ { MANGLE_CPU, "foo", "", "bar", NULL, "EVENT", 0, 0, 0, 0, 2 },
	  "{root}/bar/{dep}/{root}/foo/EVENT.0.0.all.all.2" },
	{ { MANGLE_TID, "foo", "", "bar", NULL, "EVENT", 0, 0, 0, 33, 0 },
	  "{root}/bar/{dep}/{root}/foo/EVENT.0.0.all.33.all" },
	{ { MANGLE_TGID, "foo", "", "bar", NULL, "EVENT", 0, 0, 34, 0, 0 },
	  "{root}/bar/{dep}/{root}/foo/EVENT.0.0.34.all.all" },
	{ { MANGLE_KERNEL, "foo", "", "bar", NULL, "EVENT", 0, 0, 0, 0, 0 },
	  "{kern}/bar/{dep}/{kern}/foo/EVENT.0.0.all.all.all" },
	{ { MANGLE_CALLGRAPH, "foo-from", "", "bar-from", "foo-to", "EVENT", 0, 0, 0, 0, 0 },
	  "{root}/bar-from/{dep}/{root}/foo-from/{cg}/{root}/foo-to/EVENT.0.0.all.all.all" },
	{ { MANGLE_CPU|MANGLE_TID|MANGLE_TID|MANGLE_TGID|MANGLE_KERNEL, "foo", "", "bar", NULL, "EVENT", 1234, 8192, 34, 35, 2 },
	  "{kern}/bar/{dep}/{kern}/foo/EVENT.1234.8192.34.35.2" },
	{ { MANGLE_CPU|MANGLE_TID|MANGLE_TID|MANGLE_TGID|MANGLE_KERNEL, "foo1/foo2", "", "bar1/bar2", NULL, "EVENT", 1234, 8192, 34, 35, 2 },
	  "{root}/bar1/bar2/{dep}/{root}/foo1/foo2/EVENT.1234.8192.34.35.2" },
	{ { MANGLE_CALLGRAPH|MANGLE_CPU|MANGLE_TID|MANGLE_TID|MANGLE_TGID|MANGLE_KERNEL, "bar1/bar2", "", "bar1/bar2", "bar1/bar2-to", "EVENT", 1234, 8192, 34, 35, 2 },
	  "{root}/bar1/bar2/{dep}/{root}/bar1/bar2/{cg}/{root}/bar1/bar2-to/EVENT.1234.8192.34.35.2" },

	{ { 0, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 0, 0 }, NULL }
};


int main(void)
{
	struct test_input const * test;
	for (test = tests; test->result; ++test) {
		char * result = op_mangle_filename(&test->values);
		char * expect = xmalloc(strlen(test->result) +
					strlen(op_samples_current_dir) + 1);
		strcpy(expect, op_samples_current_dir);
		strcat(expect, test->result);
		if (strcmp(result, expect)) {
			fprintf(stderr, "test %d:\nfound: %s\nexpect: %s\n",
				(int)(test - tests), result, expect);
			exit(EXIT_FAILURE);
		}
		free(expect);
		free(result);
	}

	return EXIT_SUCCESS;
}
