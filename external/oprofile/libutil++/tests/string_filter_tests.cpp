/**
 * @file string_filter_tests.cpp
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>

#include <iostream>

#include "string_filter.h"

using namespace std;

#define check(filter, str, result) \
	if (filter.match(str) != result) { \
		cerr << "\"" << str << "\" matched with " #filter \
		     << " did not return " #result << endl; \
		exit(EXIT_FAILURE); \
	}

int main()
{
	string_filter f1;
	check(f1, "", true);
	check(f1, "ok", true);

	string_filter f2("ok", "");
	check(f2, "ok", true);
	check(f2, "no", false);

	string_filter f3("", "no");
	check(f3, "ok", true);
	check(f3, "no", false);

	string_filter f4("ok,ok2,", "");
	check(f4, "ok", true);
	check(f4, "ok2", true);
	check(f4, "no", false);

	string_filter f5("ok,ok2", "no,no2");
	check(f5, "ok", true);
	check(f5, "ok2", true);
	check(f5, "no", false);
	check(f5, "no2", false);

	vector<string> v1;
	vector<string> v2;

	string_filter f6(v1, v2);
	check(f6, "", true);
	check(f6, "ok", true);

	v1.push_back("ok");
	v1.push_back("ok2");

	string_filter f7(v1, v2);
	check(f7, "ok", true);
	check(f7, "ok2", true);
	check(f7, "no", false);

	v1.clear();

	v2.push_back("no");
	v2.push_back("no2");

	string_filter f8(v1, v2);
	check(f8, "ok", true);
	check(f8, "no", false);
	check(f8, "no", false);

	v1.push_back("ok");
	v1.push_back("ok2");

	string_filter f9(v1, v2);
	check(f9, "ok", true);
	check(f9, "no2", false);

	string_filter f10(" foo ", "");
	check(f10, " foo ", true);
	check(f10, " foo", false);
	check(f10, "foo ", false);
	check(f10, "foo", false);

	return EXIT_SUCCESS;
}
