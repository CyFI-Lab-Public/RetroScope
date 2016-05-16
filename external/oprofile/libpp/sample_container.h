/**
 * @file sample_container.h
 * Internal implementation of sample container
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef SAMPLE_CONTAINER_H
#define SAMPLE_CONTAINER_H

#include <map>
#include <set>
#include <string>

#include "symbol.h"
#include "symbol_functors.h"

/**
 * Arbitrary container of sample entries. Can return
 * number of samples for a file or line number and
 * return the particular sample information for a VMA.
 */
class sample_container {
	typedef std::pair<symbol_entry const *, bfd_vma> sample_index_t;
public:
	typedef std::map<sample_index_t, sample_entry> samples_storage;
	typedef samples_storage::const_iterator samples_iterator;

	/// return iterator to the first samples for this symbol
	samples_iterator begin(symbol_entry const *) const;
	/// return iterator to the last samples for this symbol
	samples_iterator end(symbol_entry const *) const;

	/// return iterator to the first samples
	samples_iterator begin() const;
	/// return iterator to the last samples
	samples_iterator end() const;

	/// insert a sample entry by creating a new entry or by cumulating
	/// samples into an existing one. Can only be done before any lookups
	void insert(symbol_entry const * symbol, sample_entry const &);

	/// return nr of samples in the given filename
	count_array_t accumulate_samples(debug_name_id filename_id) const;

	/// return nr of samples at the given line nr in the given file
	count_array_t accumulate_samples(debug_name_id, size_t linenr) const;

	/// return the sample entry for the given image_name and vma if any
	sample_entry const * find_by_vma(symbol_entry const * symbol,
					 bfd_vma vma) const;

private:
	/// build the symbol by file-location cache
	void build_by_loc() const;

	/// main sample entry container
	samples_storage samples;

	typedef std::multiset<sample_entry const *, less_by_file_loc>
		samples_by_loc_t;

	// must be declared after the samples_storage to ensure a
	// correct life-time.

	/**
	 * Sample entries by file location. Lazily built when necessary,
	 * so mutable.
	 */
	mutable samples_by_loc_t samples_by_loc;
};

#endif /* SAMPLE_CONTAINER_H */
