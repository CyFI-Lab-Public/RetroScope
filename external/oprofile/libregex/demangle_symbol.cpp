/**
 * @file demangle_symbol.cpp
 * Demangle a C++ symbol
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#include <cstdlib>

#include "config.h"

#include "demangle_symbol.h"
#include "demangle_java_symbol.h"
#include "op_regex.h"

// from libiberty
/*@{\name demangle option parameter */
#ifndef DMGL_PARAMS
# define DMGL_PARAMS     (1 << 0)        /**< Include function args */
#endif
#ifndef DMGL_ANSI
# define DMGL_ANSI       (1 << 1)        /**< Include const, volatile, etc */
#endif
/*@}*/
extern "C" char * cplus_demangle(char const * mangled, int options);

using namespace std;

namespace options {
	extern demangle_type demangle;
}

string const demangle_symbol(string const & name)
{
	if (options::demangle == dmt_none)
		return name;

	// Do not try to strip leading underscore, as this leads to many
	// C++ demangling failures. However we strip off a leading '.'
        // as generated on PPC64
	string const & tmp = (name[0] == '.' ? name.substr(1) : name);
	char * unmangled = cplus_demangle(tmp.c_str(), DMGL_PARAMS | DMGL_ANSI);

	if (!unmangled) {
		string result = demangle_java_symbol(name);
		if (!result.empty())
			return result;
		return name;
	}

	string result(unmangled);
	free(unmangled);

	if (options::demangle == dmt_smart) {
		static bool init = false;
		static regular_expression_replace regex;
		if (init == false) {
			setup_regex(regex, OP_DATADIR "/stl.pat");
			init = true;
		}
		// we don't protect against exception here, pattern must be
		// right and user can easily work-around by using -d
		regex.execute(result);
	}

	return result;
}
