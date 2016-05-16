/**
 * @file file_manip_tests.cpp
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <unistd.h>
#include <stdlib.h>

#include <string>
#include <iostream>
#include <list>

#include "file_manip.h"

using namespace std;

template <typename Input, typename Output>
struct input_output {
	Input input;
	Output output;
};


template <typename Input, typename Output, typename Result>
static void check_result(char const * fct_name, Input const & input,
		  Output const & output, Result const & result)
{
	if (result != output) {
		cerr << fct_name << " "
		     << "for:\n\"" << input << "\"\n"
		     << "expect:\n\"" << output << "\"\n"
		     << "found:\n\"" << result << "\"\n";
		exit(EXIT_FAILURE);
	}
}

template <typename Input, typename Output, typename Result>
static void check_result(char const * fct_name, Input const & input1,
	Input input2, Output const & output, Result const & result)
{
	if (result != output) {
		cerr << fct_name << ": \n"
		     << "for:\n\"" << input1 << "\"\n"
		     << "\"" << input2 << "\"\n"
		     << "expect:\n\"" << output << "\"\n"
		     << "found:\n\"" << result << "\"\n";
		exit(EXIT_FAILURE);
	}
}


static input_output<char const *, char const *> expect_dirname[] =
{
	{ "/", "/" },
	{ "//////", "/" },
	{ "/usr", "/" },
	{ "///usr", "/" },
	// suprising but conform to dirname(1)
	{ "///usr/dir", "///usr" },
	{ "usr/dir", "usr" },
	{ "usr", "." },
	{ "n", "." },
	{ "../..", ".." },
	{ "/../..", "/.." },
	{ "./..", "." },
	{ "./.", "." },
	{ "..", "." },
	{ ".", "." },
	{ "", "." },
	{ 0, 0 }
};

static void dirname_tests()
{
	input_output<char const *, char const *> const * cur;
	for (cur = expect_dirname; cur->input; ++cur) {
		string result = op_dirname(cur->input);
		check_result("dirname", cur->input, cur->output, result);
	}
}


static input_output<char const *, char const*> expect_basename[] =
{
	{ "/", "/" },
	{ "//////", "/" },
	{ "/usr", "usr" },
	{ "///usr", "usr" },
	{ "///usr/dir", "dir" },
	{ "///usr//dir", "dir" },
	{ "usr/dir", "dir" },
	{ "usr", "usr" },
	{ "../..", ".." },
	{ "/../..", ".." },
	{ "./..", ".." },
	{ "./.", "." },
	{ ".", "." },
	{ 0, 0 }
};

static void basename_tests()
{
	input_output<char const *, char const *> const * cur;
	for (cur = expect_basename; cur->input; ++cur) {
		string result = op_basename(cur->input);
		check_result("basename", cur->input, cur->output, result);
	}
}


static input_output<char const *, bool> expect_is_directory[] =
{
	{ ".", true },
	{ "/.", true },
	{ "./", true },
	{ "/", true },
	{ "../", true },
	{ "../.", true },
	{ "non_existing_dir", false },
	{ 0, 0 }
};

static void is_directory_tests()
{
	input_output<char const *, bool> const * cur;
	for (cur = expect_is_directory; cur->input; ++cur) {
		bool result = is_directory(cur->input);
		check_result("is_directory", cur->input, cur->output, result);
	}
}


static input_output<pair<string, string>, bool>
expect_is_files_identical[] = {
#define MAKE_PAIR(a, b) make_pair(string(a), string(b))
	{ MAKE_PAIR(__FILE__, __FILE__), true },
	{ MAKE_PAIR(__FILE__, "not_existing"), false },
	{ MAKE_PAIR("not_exisiting", __FILE__), false },
	{ MAKE_PAIR("not_exisiting", "not_existing"), false },
	{ MAKE_PAIR("", ""), false }
#undef MAKE_PAIR
};

void is_files_identical_tests(char const * prog_name)
{
	check_result("is_files_identical", prog_name, prog_name,
	             is_files_identical(prog_name, prog_name), true);

	input_output<pair<string, string>, bool> const * cur;
	for (cur = expect_is_files_identical; !cur->input.first.empty(); ++cur) {
		bool result = is_files_identical(cur->input.first,
		                                 cur->input.second);
		check_result("is_files_identical", cur->input.first,
		             cur->input.second, cur->output, result);
	}
}


static input_output<char const *, bool> expect_op_file_readable[] =
{
	{ __FILE__, true },
	{ "./" __FILE__, true },
	{ ".", false },
	{ "/.", false },
	{ "./", false },
	{ "/", false },
	{ "../", false },
	{ "../.", false },
	{ "non_existing_file", false },
	{ 0, 0 }
};

static void op_file_readable_tests()
{
	input_output<char const *, bool> const * cur;
	for (cur = expect_op_file_readable; cur->input; ++cur) {
		bool result = op_file_readable(cur->input);
		check_result("op_file_readable", cur->input, cur->output, result);
	}
}


static input_output<string, string> expect_realpath[] =
{
	// realpath() file argument must exists.
	{ "file_manip_tests.o", "file_manip_tests.o" },
	{ "../tests/" "file_manip_tests.o", "file_manip_tests.o" },
	{ ".//.//" "file_manip_tests.o", "file_manip_tests.o" },
	// POSIX namespaces ignored by realpath(3)
	{ "//", "/" },
	{ "//usr", "/usr" },
	{ "///", "/" },
	{ "", "" }
};


// FIXME: useful to test symlinks too
static void realpath_tests()
{
	input_output<string, string> const * cur;
	for (cur = expect_realpath; !cur->input.empty(); ++cur) {
		string result = op_realpath(cur->input);
		string expect = cur->output;
		if (cur->input[0] != '/')
			expect = SRCDIR + expect;
		check_result("op_realpath", cur->input,
		            expect, result);
	}
}


void create_file_list_tests()
{
	list<string> result;
	if (!create_file_list(result, ".")) {
		cerr << "create_file_list() fail\n";
		exit(EXIT_FAILURE);
	}
	if (result.empty()) {
		cerr << "create_file_list(); empty result\n";
		exit(EXIT_FAILURE);
	}
}


int main(int, char * argv[])
{
	dirname_tests();
	basename_tests();
	is_directory_tests();
	is_files_identical_tests(argv[0]);
	op_file_readable_tests();
	realpath_tests();
	create_file_list_tests();
	return EXIT_SUCCESS;
}
