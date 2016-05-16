/**
 * @file symbol_container.h
 * Internal container for symbols
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef SYMBOL_CONTAINER_H
#define SYMBOL_CONTAINER_H

#include <string>
#include <set>

#include "symbol.h"
#include "symbol_functors.h"

/**
 * An arbitrary container of symbols. Supports lookup
 * by name, by VMA, and by file location.
 *
 * Lookup by name or by VMA is O(n). Lookup by file location
 * is O(log(n)).
 */
class symbol_container {
public:
	/// container type
	typedef std::set<symbol_entry, less_symbol> symbols_t;

	typedef symbols_t::size_type size_type;

	/// return the number of symbols stored
	size_type size() const;

	/**
	 * Insert a new symbol. If the symbol already exists in the container,
	 * then the sample counts are accumulated.
	 * Returns the newly created symbol or the existing one. This pointer
	 * remains valid during the whole life time of a symbol_container
	 * object and is warranted unique according to less_symbol comparator.
	 * Can only be done before any file-location based lookups, since the
	 * two lookup methods are not synchronised.
	 */
	symbol_entry const * insert(symbol_entry const &);

	/// find the symbols at the given filename and line number, if any
	symbol_collection const find(debug_name_id filename, size_t linenr) const;

	/// find the symbols defined in the given filename, if any
	symbol_collection const find(debug_name_id filename) const;

	/// find the symbol with the given image_name vma if any
	symbol_entry const * find_by_vma(std::string const & image_name,
					 bfd_vma vma) const;

	/// Search a symbol. Return NULL if not found.
	symbol_entry const * find(symbol_entry const & symbol) const;

	/// return start of symbols
	symbols_t::iterator begin();

	/// return end of symbols
	symbols_t::iterator end();

private:
	/// build the symbol by file-location cache
	void build_by_loc() const;

	/**
	 * The main container of symbols. Multiple symbols with the same
	 * name are allowed.
	 */
	symbols_t symbols;

	/**
	 * Differently-named symbol at same file location are allowed e.g.
	 * template instantiation.
	 */
	typedef std::multiset<symbol_entry const *, less_by_file_loc>
		symbols_by_loc_t;

	// must be declared after the set to ensure a correct life-time.

	/**
	 * Symbols sorted by location order. Lazily built on request,
	 * so mutable.
	 */
	mutable symbols_by_loc_t symbols_by_loc;
};

#endif /* SYMBOL_CONTAINER_H */
