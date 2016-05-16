/**
 * @file java_test.cpp
 *
 * A simple test for java demangling. Run it through:
 * $ java_test
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include "demangle_java_symbol.h"

#include "op_regex.h"

#include <iostream>
#include <fstream>

#include <cstdlib>

using namespace std;

namespace {

void check_result(string const & input, string const & output,
			 string const & result)
{
	if (result != output) {
		cerr << "for:\n\"" << input << "\"\n"
		     << "expect:\n\"" << output << "\"\n"
		     << "found:\n\"" << result << "\"\n";
		exit(EXIT_FAILURE);
	}
}

struct input_output {
	char const * mangled;
	char const * expect;
};

input_output mangle_tests[] = {
	{ "Ltest$test_1;f2(I)V", "void test$test_1.f2(int)" },
	{ "Ltest;f4()V", "void test.f4()" },
	{ "Ltest;f2(II)V", "void test.f2(int, int)" },
	{ "Ltest$HelloThread;run()V~1", "void test$HelloThread.run()~1" },
	{ "Lsun/security/provider/SHA;implCompress([BI)V", "void sun.security.provider.SHA.implCompress(byte[], int)" },
	{ "Ljava/lang/String;equals(Ljava/lang/Object;)Z", "boolean java.lang.String.equals(java.lang.Object)" },
	{ "Lorg/eclipse/swt/graphics/ImageData;blit(I[BIIIIIIIIIII[BIII[BIIIIIIIIIIZZ)V", "void org.eclipse.swt.graphics.ImageData.blit(int, byte[], int, int, int, int, int, int, int, int, int, int, int, byte[], int, int, int, byte[], int, int, int, int, int, int, int, int, int, int, boolean, boolean)" },
	{ 0, 0 }
};

} // anonymous namespace

int main(void)
{
	input_output const * cur;
	for (cur = mangle_tests; cur->mangled; ++cur) {
		string result = demangle_java_symbol(cur->mangled);
		check_result(cur->mangled, cur->expect, result);
	}

	return 0;
}

