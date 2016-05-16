/**
 * @file diff_container.cpp
 * Container for diffed symbols
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

/* older glibc has C99 INFINITY in _GNU_SOURCE */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include "diff_container.h"

#include <cmath>

using namespace std;


namespace {


/// a comparator suitable for diffing symbols
bool rough_less(symbol_entry const & lhs, symbol_entry const & rhs)
{
	if (lhs.image_name != rhs.image_name)
		return lhs.image_name < rhs.image_name;

	if (lhs.app_name != rhs.app_name)
		return lhs.app_name < rhs.app_name;

	if (lhs.name != rhs.name)
		return lhs.name < rhs.name;

	return false;
}


/// possibly add a diff sym
void
add_sym(diff_collection & syms, diff_symbol const & sym,
        profile_container::symbol_choice & choice)
{
	if (choice.match_image
	    && (image_names.name(sym.image_name) != choice.image_name))
		return;

	if (fabs(sym.diffs[0]) < choice.threshold)
		return;

	choice.hints = sym.output_hint(choice.hints);
	syms.push_back(sym);
}


/// add a symbol not present in the new profile
void
symbol_old(diff_collection & syms, symbol_entry const & sym,
           profile_container::symbol_choice & choice)
{
	diff_symbol symbol(sym);
	symbol.diffs.fill(sym.sample.counts.size(), -INFINITY);
	add_sym(syms, symbol, choice);
}


/// add a symbol not present in the old profile
void
symbol_new(diff_collection & syms, symbol_entry const & sym,
           profile_container::symbol_choice & choice)
{
	diff_symbol symbol(sym);
	symbol.diffs.fill(sym.sample.counts.size(), INFINITY);
	add_sym(syms, symbol, choice);
}


/// add a diffed symbol
void symbol_diff(diff_collection & syms,
                 symbol_entry const & sym1, count_array_t const & total1,
                 symbol_entry const & sym2, count_array_t const & total2,
                 profile_container::symbol_choice & choice)
{
	diff_symbol symbol(sym2);

	size_t size = sym2.sample.counts.size();
	for (size_t i = 0; i != size; ++i) {
		double percent1;
		double percent2;
		percent1 = op_ratio(sym1.sample.counts[i], total1[i]);
		percent2 = op_ratio(sym2.sample.counts[i], total2[i]);
		symbol.diffs[i] = op_ratio(percent2 - percent1, percent1);
		symbol.diffs[i] *= 100.0;
	}

	add_sym(syms, symbol, choice);
}


}; // namespace anon


diff_container::diff_container(profile_container const & c1,
                               profile_container const & c2)
	: pc1(c1), pc2(c2),
	  total1(pc1.samples_count()), total2(pc2.samples_count())
{
}


diff_collection const
diff_container::get_symbols(profile_container::symbol_choice & choice) const
{
	diff_collection syms;

	/*
	 * Do a pairwise comparison of the two symbol sets. We're
	 * relying here on the symbol container being sorted such
	 * that rough_less() is suitable for iterating through the
	 * two lists (see less_symbol).
	 */

	symbol_container::symbols_t::iterator it1 = pc1.begin_symbol();
	symbol_container::symbols_t::iterator end1 = pc1.end_symbol();
	symbol_container::symbols_t::iterator it2 = pc2.begin_symbol();
	symbol_container::symbols_t::iterator end2 = pc2.end_symbol();

	while (it1 != end1 && it2 != end2) {
		if (rough_less(*it1, *it2)) {
			symbol_old(syms, *it1, choice);
			++it1;
		} else if (rough_less(*it2, *it1)) {
			symbol_new(syms, *it2, choice);
			++it2;
		} else {
			symbol_diff(syms, *it1, total1, *it2, total2, choice);
			++it1;
			++it2;
		}
	}

	for (; it1 != end1; ++it1)
		symbol_old(syms, *it1, choice);

	for (; it2 != end2; ++it2)
		symbol_new(syms, *it2, choice);

	return syms;
}


count_array_t const diff_container::samples_count() const
{
	return total2;
}
