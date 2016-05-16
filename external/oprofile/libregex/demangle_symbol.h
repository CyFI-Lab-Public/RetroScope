/**
 * @file demangle_symbol.h
 * Demangle a C++ symbol
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef DEMANGLE_SYMBOL_H
#define DEMANGLE_SYMBOL_H

#include <string>

/// demangle type: specify what demangling we use
enum demangle_type {
	/// no demangling.
	dmt_none,
	/// use cplus_demangle()
	dmt_normal,
	/// normal plus a pass through the regular expression to simplify
	/// the mangled name
	dmt_smart
};

/**
 * demangle_symbol - demangle a symbol
 * @param name the mangled symbol name
 * @return the demangled name
 *
 * Demangle the symbol name, if the global
 * variable demangle is true.
 *
 * The demangled name lists the parameters and type
 * qualifiers such as "const".
 */
std::string const demangle_symbol(std::string const & name);

#endif // DEMANGLE_SYMBOL_H
