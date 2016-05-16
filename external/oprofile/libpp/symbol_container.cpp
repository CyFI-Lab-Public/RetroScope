/**
 * @file symbol_container.cpp
 * Internal container for symbols
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <string>
#include <algorithm>
#include <set>
#include <vector>

#include "symbol_container.h"

using namespace std;

symbol_container::size_type symbol_container::size() const
{
	return symbols.size();
}


symbol_entry const * symbol_container::insert(symbol_entry const & symb)
{
	pair<symbols_t::iterator, bool> p = symbols.insert(symb);
	if (!p.second) {
		// safe: count is not used by sorting criteria
		symbol_entry * symbol = const_cast<symbol_entry*>(&*p.first);
		symbol->sample.counts += symb.sample.counts;
	}

	return &*p.first;
}


symbol_collection const
symbol_container::find(debug_name_id filename, size_t linenr) const
{
	build_by_loc();

	symbol_entry symbol;
	symbol.sample.file_loc.filename = filename;
	symbol.sample.file_loc.linenr = linenr;

	symbol_collection result;

	typedef symbols_by_loc_t::const_iterator it;
	pair<it, it> p_it = symbols_by_loc.equal_range(&symbol);
	for ( ; p_it.first != p_it.second; ++p_it.first)
		result.push_back(*p_it.first);

	return result;
}


symbol_collection const
symbol_container::find(debug_name_id filename) const
{
	build_by_loc();

	symbol_entry symbol;
	symbol.sample.file_loc.filename = filename;
	symbol.sample.file_loc.linenr = 0;

	typedef symbols_by_loc_t::const_iterator it;
	it first = symbols_by_loc.lower_bound(&symbol);
	symbol.sample.file_loc.linenr = (unsigned int)size_t(-1);
	it last  = symbols_by_loc.upper_bound(&symbol);

	symbol_collection result;
	for ( ; first != last ; ++first)
		result.push_back(*first);

	return result;
}


void symbol_container::build_by_loc() const
{
	if (!symbols_by_loc.empty())
		return;

	symbols_t::const_iterator cit = symbols.begin();
	symbols_t::const_iterator end = symbols.end();
	for (; cit != end; ++cit)
		symbols_by_loc.insert(&*cit);
}


symbol_entry const * symbol_container::find_by_vma(string const & image_name,
						   bfd_vma vma) const
{
	// FIXME: this is too inefficient probably
	symbols_t::const_iterator it;
	for (it = symbols.begin(); it != symbols.end(); ++it) {
		if (it->sample.vma == vma &&
		    image_names.name(it->image_name) == image_name)
			return &*it;
	}

	return 0;
}


symbol_container::symbols_t::iterator symbol_container::begin()
{
	return symbols.begin();
}


symbol_container::symbols_t::iterator symbol_container::end()
{
	return symbols.end();
}

symbol_entry const * symbol_container::find(symbol_entry const & symbol) const
{
	symbols_t::const_iterator it = symbols.find(symbol);
	return it == symbols.end() ? 0 : &*it;
}
