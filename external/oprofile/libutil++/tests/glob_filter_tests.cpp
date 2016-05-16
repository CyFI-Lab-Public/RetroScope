/**
 * @file glob_filter_tests.cpp
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>

#include <iostream>

#include "glob_filter.h"

using namespace std;

#define check(filter, str, result) \
	if (filter.match(str) != result) { \
		cerr << "\"" << str << "\" matched with " #filter \
		     << " did not return " #result << endl; \
		exit(EXIT_FAILURE); \
	}

int main()
{
	glob_filter f1("foo,*bar", "foobar");
	check(f1, "foo/barfoobar", true);
	check(f1, "foo/bar", true);
	check(f1, "/foo/foobar/foo", false);
	check(f1, "fooobar1", false);
	check(f1, "foo1", false);
	check(f1, "foobar", false);
	check(f1, "bar1", false);

	glob_filter f2("foo", "");
	check(f2, "foo", true);
	check(f2, "foo1", false);
	check(f2, "foo/foo", false);

	glob_filter f3("", "foo");
	check(f3, "foo", false);
	check(f3, "foo1", true);
	check(f3, "foo/foo", true);

	glob_filter f4("foo", "foo");
	check(f4, "foo", false);
	check(f4, "foo1", false);
	check(f4, "foo/foo", false);

	return EXIT_SUCCESS;
}
