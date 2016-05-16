/**
 * @file file_tests.c
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_file.h"

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>

static char * tests[][2] = {
	{ "/usr/bin/../bin", "/usr/bin" },
	{ "/../usr/bin/", "/usr/bin" },
	{ "/../../usr/bin/", "/usr/bin" },
	{ "/../../usr/bin/.", "/usr/bin" },
	{ "/../../usr/bin/./", "/usr/bin" },
	{ "/usr/./bin", "/usr/bin" },
	{ "/usr/././bin", "/usr/bin" },
	{ "/usr///", "/usr" },
	{ "../", "/" },
	{ "./", "/usr" },
	{ ".", "/usr" },
	{ "./../", "/" },
	{ "bin/../bin/../", "/usr" },
	{ "../../../../../", "/" },
	{ "/usr/bin/../../..", "/" },
	{ "/usr/bin/../../../", "/" },
	{ "././.", "/usr" },
	/* POSIX namespace ignored by realpath(3) */
	{ "//", "/" },
	{ "//usr", "/usr" },
	{ "///", "/" },
	{ NULL, NULL },
};

int main(void)
{
	char tmp[PATH_MAX];
	size_t i = 0;

	if (chdir("/usr")) {
		fprintf(stderr, "chdir(\"/usr\") failed for %s\n", tests[i][0]);
		exit(EXIT_FAILURE);
	}

	while (tests[i][0]) {
		if (!realpath(tests[i][0], tmp)) {
			fprintf(stderr, "NULL return for %s\n", tests[i][0]);
			exit(EXIT_FAILURE);
		}

		if (strcmp(tmp, tests[i][1])) {
			fprintf(stderr, "%s does not match %s given %s\n",
			        tmp, tests[i][1], tests[i][0]);
			exit(EXIT_FAILURE);
		}
		++i;
	}

	return EXIT_SUCCESS;
}
