/**
 * @file regex_test.cpp
 *
 * A simple test for libregex. Run it through:
 * $ regex_test
 * or
 * $ regex_test filename(s)
 * when no argument is provided "mangled-name" is used,
 * see it for the input file format
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include "string_manip.h"

#include "op_regex.h"

#include <iostream>
#include <fstream>

#include <cstdlib>

using namespace std;

static int nr_error = 0;

static void do_test(istream& fin)
{
	regular_expression_replace rep;

	setup_regex(rep, "../stl.pat");

	string test, expect, last;
	bool first = true;
	while (getline(fin, last)) {
		last = trim(last);
		if (last.length() == 0 || last[0] == '#')
			continue;

		if (first) {
			test = last;
			first = false;
		} else {
			expect = last;
			first = true;
			string str(test);
			rep.execute(str);
			if (str != expect) {
				cerr << "mistmatch: test, expect, returned\n"
				     << '"' << test << '"' << endl
				     << '"' << expect << '"' << endl
				     << '"' << str << '"' << endl;
				++nr_error;
			}
		}
	}

	if (!first)
		cerr << "input file ill formed\n";
}

int main(int argc, char * argv[])
{
	try {
		if (argc > 1) {
			for (int i = 1; i < argc; ++i) {
				ifstream fin(argv[i]);
				do_test(fin);
			}
		} else {
			ifstream fin("mangled-name");
			if (!fin) {
				cerr << "Unable to open input test "
				     << "\"mangled_name\"\n" << endl;
				exit(EXIT_FAILURE);
			}
			do_test(fin);
		}
	}
	catch (bad_regex const & e) {
		cerr << "bad_regex " << e.what() << endl;
		return EXIT_FAILURE;
	}
	catch (exception const & e) {
		cerr << "exception: " << e.what() << endl;
		return EXIT_FAILURE;
	}

	return nr_error ? EXIT_FAILURE : EXIT_SUCCESS;
}

