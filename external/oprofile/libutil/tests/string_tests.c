/**
 * @file string_tests.c
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_string.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

void error(char const * str)
{
	fprintf(stderr, "%s\n", str);
	exit(EXIT_FAILURE);
}


int main()
{
	if (!strisprefix("", ""))
		error("\"\" is not a prefix of \"\"");
	if (!strisprefix("a", ""))
		error("\"\" is not a prefix of a");
	if (!strisprefix("a", "a"))
		error("a is not a prefix of a");
	if (!strisprefix("aa", "a"))
		error("a is not a prefix of aa");
	if (strisprefix("a", "b"))
		error("b is a prefix of a");

	if (strcmp(skip_ws(""), ""))
		error("skip_ws of \"\" is not \"\"");
	if (strcmp(skip_ws("\na"), "a"))
		error("skip_ws of \\na is not a");
	if (strcmp(skip_ws("\n\na"), "a"))
		error("skip_ws of \\n\\na is not a");
	if (strcmp(skip_ws("\n a"), "a"))
		error("skip_ws of \\n a is not a");
	if (strcmp(skip_ws("\n \ta"), "a"))
		error("skip_ws of \\n \\ta is not a");
	if (strcmp(skip_ws("\n \t"), ""))
		error("skip_ws of \\n \\t is not \"\"");
	if (strcmp(skip_ws(" "), ""))
		error("skip_ws of \" \" is not \"\"");

	if (strcmp(skip_nonws(""), ""))
		error("skip_nonws of \"\" is not \"\"");
	if (strcmp(skip_nonws("a"), ""))
		error("skip_nonws of a is not \"\"");
	if (strcmp(skip_nonws("\n"), "\n"))
		error("skip_nonws of \\n is not \\n");
	if (strcmp(skip_nonws(" "), " "))
		error("skip_nonws of \" \" is not \" \"");
	if (strcmp(skip_nonws("\t"), "\t"))
		error("skip_nonws of \\t is not \\t");
	if (strcmp(skip_nonws("a\n"), "\n"))
		error("skip_nonws of a\\n is not \\n");
	if (strcmp(skip_nonws("ab"), ""))
		error("skip_nonws of ab is not \"\"");

	if (!empty_line(""))
		error("empty_line is false for \"\"");
	if (!empty_line("\n\t "))
		error("empty_line is false for \"\\n\\n \"");
	if (!empty_line(" "))
		error("empty_line is false for \" \"");
	if (empty_line("\r"))
		error("empty_line is true for \\r");

	if (comment_line(""))
		error("comment_line is true for \"\"");
	if (comment_line("\n"))
		error("comment_line is true for \n");
	if (!comment_line("#"))
		error("comment_line is false for #");
	if (!comment_line(" #"))
		error("comment_line is false for \" #\"");
	/* this is what the spec says */
	if (!comment_line("\n#"))
		error("comment_line is false for \\n#");
	if (!comment_line("\t#"))
		error("comment_line is false for \\t#");

	return EXIT_SUCCESS;
}
