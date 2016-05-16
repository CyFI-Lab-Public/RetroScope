/**
 * @file path_filter_tests.cpp
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>

#include <iostream>

#include "path_filter.h"

using namespace std;

#define check(filter, str, result) \
	if (filter.match(str) != result) { \
		cerr << "\"" << str << "\" matched with " #filter \
		     << " did not return " #result << endl; \
		exit(EXIT_FAILURE); \
	}

int main()
{
	path_filter f1("foo,*bar", "foobar");
	check(f1, "foo/barfoobar", true);
	check(f1, "foo/bar", true);
	check(f1, "/foo/foobar/foo", false);
	check(f1, "fooobar1", false);
	check(f1, "foo1", false);
	check(f1, "foobar", false);
	check(f1, "bar1", false);

	path_filter f2("foo", "");
	check(f2, "foo", true);
	check(f2, "foo1", false);
	check(f2, "foo/foo", true);

	path_filter f3("", "foo");
	check(f3, "foo", false);
	check(f3, "foo1", true);
	check(f3, "foo/foo", false);

	path_filter f4("foo", "foo");
	check(f4, "foo", false);
	check(f4, "foo1", false);
	check(f4, "foo/foo", false);

	path_filter f5("*foo*", "*bar*");
	check(f5, "foo", true);
	check(f5, "bar", false);
	check(f5, "foobar", false);
	check(f5, "barfoo", false);
	check(f5, "foo/bar", false);

	path_filter f6(" foo,bar", "bar ");
	check(f6, "foo", false);
	check(f6, "foo ", false);
	check(f6, " foo", true);
	check(f6, "bar", true);
	check(f6, "bar ", false);
	check(f6, " bar", false);
	check(f6, "foo ", false);
	check(f6, "foo/ bar", false);

	path_filter f7(".", "");
	check(f7, ".", true);
	// a bit surprising but right IMHO, our implementation use successive
	// dirname(input) to check vs the included path and
	// dirname("foo") == "." so all relative path input match a "."
	// included filter
	check(f7, "foo", true);

	return EXIT_SUCCESS;
}
