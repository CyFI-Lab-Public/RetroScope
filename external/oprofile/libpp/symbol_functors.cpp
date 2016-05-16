/**
 * @file symbol_functors.cpp
 * Functors for symbol/sample comparison
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "symbol_functors.h"

bool less_symbol::operator()(symbol_entry const & lhs,
			     symbol_entry const & rhs) const
{
	if (lhs.image_name != rhs.image_name)
		return lhs.image_name < rhs.image_name;

	if (lhs.app_name != rhs.app_name)
		return lhs.app_name < rhs.app_name;

	if (lhs.name != rhs.name)
		return lhs.name < rhs.name;

	if (lhs.sample.vma != rhs.sample.vma)
		return lhs.sample.vma < rhs.sample.vma;

	return lhs.size < rhs.size;
}
