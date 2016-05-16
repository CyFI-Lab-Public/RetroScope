/**
 * @file string_manip_tests.cpp
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>

#include <algorithm>
#include <iterator>
#include <iostream>
#include <utility>

#include "string_manip.h"

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
		cerr << fct_name << ": \n"
		     << "for:\n\"" << input << "\"\n"
		     << "expect:\n\"" << output << "\"\n"
		     << "found:\n\"" << result << "\"\n";
		exit(EXIT_FAILURE);
	}
}


static input_output<char const*, char const*> expect_erase[] =
{
	{ "", "" },
	{ ";;;", "" },
	{ "ab;;;cd", "cd" },
	{ ";;;cd", "cd" },
	{ "ab;;;", "" },
	{ 0, 0 }
};

static void erase_to_last_of_tests()
{
	input_output<char const *, char const*> const * cur;
	for (cur = expect_erase; cur->input; ++cur) {
		string result = erase_to_last_of(cur->input, ';');
		check_result("erase_to_last_of()", cur->input, cur->output,
			     result);
	}
}


static input_output<char const *, pair<string, string> > expect_split[] =
{
#define MAKE_PAIR(a, b)  make_pair(string(a), string(b))
	{ "ab;cd", MAKE_PAIR("ab", "cd") },
	{ ";cd",   MAKE_PAIR("",   "cd") },
	{ "ab;",   MAKE_PAIR("ab", "")   },
	{ "b;d",   MAKE_PAIR("b",  "d")  },
	{ ";d",    MAKE_PAIR("",   "d")  },
	{ "a;",    MAKE_PAIR("a",  "")   },
	{ ";",     MAKE_PAIR("",   "")   },
	{ "",      MAKE_PAIR("",   "")   },
	{ 0,       MAKE_PAIR("",   "")   }
#undef MAKE_PAIR
};

static void split_tests()
{
	input_output<char const *, pair<string, string> > const * cur;
	for (cur = expect_split; cur->input; ++cur) {
		string temp = cur->input;
		string result = split(temp, ';');
		check_result("split()", cur->input, cur->output.first, temp);
		check_result("split()", cur->input, cur->output.second, result);
	}
}

static input_output<char const *, pair<string, bool> > expect_is_prefix[] =
{
#define MAKE_PAIR(a, b)  make_pair(string(a), b)
	{ "abcd", MAKE_PAIR("abc", true) },
	{ "abcd", MAKE_PAIR("ac", false) },
	{ "babcd", MAKE_PAIR("abc", false) },
	// these invoke undefined behavior from is_prefix, we keep them
	// for the record.
//	{ "babcd", MAKE_PAIR("", false) },
//	{ "", MAKE_PAIR("", false) },
	{ 0, MAKE_PAIR("", true) }
#undef MAKE_PAIR
};

static void is_prefix_tests()
{
	input_output<char const *, pair<string, bool> > const * cur;
	for (cur = expect_is_prefix; cur->input; ++cur) {
		bool result = is_prefix(cur->input, cur->output.first);
		if (result != cur->output.second) {
			cerr << "is_prefix(" << cur->input << ", "
			     << cur->output.first << ") "
			     << "return " << result << endl;
			exit(EXIT_FAILURE);
		}
	}
}


static const size_t max_token = 8;
static input_output<char const *, char const *[max_token]> expect_separate_token[] =
{
	{ "aa", { "aa" } },
	{ "a\\c", { "a\\c" } },
	{ "a\\\\c", { "a\\\\c" } },
	{ "a\\\\c\\", { "a\\\\c\\" } },
	{ "ab;cd;ef;gh", { "ab", "cd", "ef", "gh" } },
	{ "ab\\;cd", { "ab;cd" } },
	{ "a;a", { "a", "a" } },
	{ ";a", { "", "a" } },
	{ ";", { "", "" } },
	{ ";;", { "", "", "" } },
	{ 0, { 0, } }
};


static void separate_token_tests()
{
	input_output<char const *, char const *[max_token]> const * cur;
	for (cur = expect_separate_token; cur->input; ++cur) {
		vector<string> result = separate_token(cur->input, ';');
		if (result.size() > max_token) {
			cerr << "separate_token(): too many token\n" 
			     << "input:\n"
			     << '"' << cur->input << "\"\n"
			     << "output\n";
			copy(result.begin(), result.end(),
			     ostream_iterator<string>(cerr, "\n"));
			exit(EXIT_FAILURE);
		}
		for (size_t i = 0; i < result.size(); ++i) {
			if (result[i] != cur->output[i]) {
				cerr << "separate_token():\n" 
				     << "input:\n"
				     << cur->input << endl;
				cerr << "expect:\n";
				for (size_t i = 0; i < max_token; ++i) {
					if (!cur->output[i])
						break;
					cerr << cur->output[i] << endl;
				}
				cerr << "output:\n";
				copy(result.begin(), result.end(),
				     ostream_iterator<string>(cerr, "\n"));
				exit(EXIT_FAILURE);
			}
		}
	}
}


static input_output<char const *, char const *> expect_rtrim[] =
{
	{ "abc", "abc" },
	{ "abc  ", "abc" },
	{ " abc  ", " abc" },
	{ " abc \t \t", " abc" },
	{ " ", "" },
	{ "\t \t", "" },
	{ "", "" },
	{ 0, 0 }
};

static void rtrim_tests()
{
	input_output<char const *, char const*> const * cur;
	for (cur = expect_rtrim; cur->input; ++cur) {
		string result = rtrim(cur->input);
		check_result("rtrim()", cur->input, cur->output, result);
	}
}


static input_output<char const *, char const *> expect_ltrim[] =
{
	{ "abc", "abc" },
	{ "abc ", "abc " },
	{ " abc ", "abc " },
	{ "\t  \tabc ", "abc " },
	{ " ", "" },
	{ "\t \t", "" },
	{ "", "" },
	{ 0, 0 }
};

static void ltrim_tests()
{
	input_output<char const *, char const*> const * cur;
	for (cur = expect_ltrim; cur->input; ++cur) {
		string result = ltrim(cur->input);
		check_result("ltrim()", cur->input, cur->output, result);
	}
}


static input_output<char const *, char const *> expect_trim[] =
{
	{ "abc", "abc" },
	{ "abc ", "abc" },
	{ " abc ", "abc" },
	{ "\t  \tabc \t", "abc" },
	{ " ", "" },
	{ "\t \t", "" },
	{ "", "" },
	{ 0, 0 }
};

static void trim_tests()
{
	input_output<char const *, char const*> const * cur;
	for (cur = expect_trim; cur->input; ++cur) {
		string result = trim(cur->input);
		check_result("trim()", cur->input, cur->output, result);
	}
}


static input_output<double, char const *> expect_format_percent[] =
{
	{ 2.2,        " 2.2000" },
	{ 0,          "      0" },
	{ 100.00,     "100.000" },
	{ 99.99999,   "100.000" },
	{ 0.00000344, "3.4e-06" },
	// FIXME, must be 3.e-124 but output is 3.4e-124
//	{ 0.34e-123,  "3.e-124" },
	{ -1.0, 0 }
};

static void format_percent_tests()
{
	input_output<double, char const*> const * cur;
	for (cur = expect_format_percent; cur->input != -1.0; ++cur) {
		string result = format_percent(cur->input, percent_int_width,
		      percent_fract_width);
		check_result("format_percent()", cur->input, cur->output,
			     result);
	}
}


static input_output<unsigned int, char const *> expect_from_str_to_uint[] =
{
	{ 123, "123" },
	{ 33, "33" },
	{ 0, "0" },
	{ 0, 0 }
};

static void tostr_tests()
{
	input_output<unsigned int, char const *> const * cur;
	for (cur = expect_from_str_to_uint; cur->output; ++cur) {
		string result = op_lexical_cast<string>(cur->input);
		check_result("op_lexical_cast()", cur->input,
		     cur->output, result);
	}
}

static void touint_tests()
{
	// reversed input/output of the previous tests
	input_output<unsigned int, char const *> const * cur;
	for (cur = expect_from_str_to_uint; cur->output; ++cur) {
		unsigned int result =
			op_lexical_cast<unsigned int>(cur->output);
		check_result("op_lexical_cast()", cur->output, cur->input,
		     result);
	}
}


static input_output<char const*, bool> expect_from_str_to_bool[] =
{
	{ "0", false },
	{ "1", true },
	{ 0, 0 }
};

static void tobool_tests()
{
	input_output<char const *, bool> const * cur;
	for (cur = expect_from_str_to_bool; cur->input; ++cur) {
		bool result = op_lexical_cast<bool>(cur->input);
		check_result("op_lexical_cast()", cur->input, cur->output,
		     result);
	}
}

// FIXME: more op_lexical_cast<> tests

int main()
{
	erase_to_last_of_tests();
	tostr_tests();
	touint_tests();
	tobool_tests();
	split_tests();
	is_prefix_tests();
	separate_token_tests();
	rtrim_tests();
	ltrim_tests();
	trim_tests();
	format_percent_tests();
	return EXIT_SUCCESS;
}
