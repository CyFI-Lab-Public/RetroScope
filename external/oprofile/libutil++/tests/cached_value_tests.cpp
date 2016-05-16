/**
 * @file cached_value_tests.cpp
 * tests cached_value.h
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#include <cstdlib>
#include <iostream>
#include <string>

#include "cached_value.h"

using namespace std;

namespace {


bool check_throw(cached_value<bool> const & boolval)
{
	try {
		bool foo = boolval.get();
		foo = false;
	} catch (op_fatal_error const & e) {
		return true;
	}
	return false;
}


int check_cached(void)
{
	cached_value<bool> boolval;
	cached_value<string> strval;

	if (!check_throw(boolval)) {
		cerr << "get() on no value didn't throw\n";
		return EXIT_FAILURE;
	}

	if (boolval.reset(false) != false || boolval.get() != false) {
		cerr << "reset() of cached value \"false\" didn't work\n";
		return EXIT_FAILURE;
	}

	if (boolval.reset(true) != true || boolval.get() != true) {
		cerr << "reset() of cached value \"true\" didn't work\n";
		return EXIT_FAILURE;
	}

	if (strval.reset("foo") != "foo" || strval.get() != "foo") {
		cerr << "reset() of cached value \"foo\" didn't work\n";
		return EXIT_FAILURE;
	}

	if (strval.reset("") != "" || strval.get() != "") {
		cerr << "reset() of cached value \"\" didn't work\n";
		return EXIT_FAILURE;
	}

	return EXIT_SUCCESS;
}

};

int main()
{
	try {
		check_cached();
	}
	catch (...) {
		cerr << "unknown exception\n";
		return EXIT_FAILURE;
	}

	return EXIT_SUCCESS;
}
