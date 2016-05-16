/**
 * @file demangle_java_symbol.h
 * Demangle a java symbol
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef DEMANGLE_JAVA_SYMBOL_H
#define DEMANGLE_JAVA_SYMBOL_H

#include <string>

/// Return an empty string on error
std::string const demangle_java_symbol(std::string const & name);

#endif // DEMANGLE_JAVA_SYMBOL_H
