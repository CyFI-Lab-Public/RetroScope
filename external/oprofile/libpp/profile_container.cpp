/**
 * @file profile_container.cpp
 * profile file container
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <set>
#include <vector>
#include <string>
#include <iostream>
#include <sstream>
#include <algorithm>
#include <numeric>

#include "symbol.h"
#include "op_header.h"
#include "profile.h"
#include "symbol_functors.h"
#include "profile_container.h"
#include "sample_container.h"
#include "symbol_container.h"
#include "populate_for_spu.h"

using namespace std;

namespace {

struct filename_by_samples {
	filename_by_samples(debug_name_id id, double percent_)
		: filename(id), percent(percent_)
		{}

	bool operator<(filename_by_samples const & lhs) const {
		if (percent != lhs.percent)
			return percent < lhs.percent;
		return filename < lhs.filename;
	}

	debug_name_id filename;
	// ratio of samples which belongs to this filename.
	double percent;
};

}  // anon namespace


profile_container::profile_container(bool debug_info_, bool need_details_,
				     extra_images const & extra_)
	:
	symbols(new symbol_container),
	samples(new sample_container),
	debug_info(debug_info_),
	need_details(need_details_),
	extra_found_images(extra_)
{
}


profile_container::~profile_container()
{
}
 

// Post condition:
//  the symbols/samples are sorted by increasing vma.
//  the range of sample_entry inside each symbol entry are valid
//  the samples_by_file_loc member var is correctly setup.
void profile_container::add(profile_t const & profile,
                            op_bfd const & abfd, string const & app_name,
                            size_t pclass)
{
	string const image_name = abfd.get_filename();
	opd_header header = profile.get_header();

	for (symbol_index_t i = 0; i < abfd.syms.size(); ++i) {

		unsigned long long start = 0, end = 0;
		symbol_entry symb_entry;

		abfd.get_symbol_range(i, start, end);

		profile_t::iterator_pair p_it =
			profile.samples_range(start, end);
		count_type count = accumulate(p_it.first, p_it.second, 0ull);

		// skip entries with no samples
		if (count == 0)
			continue;

		symb_entry.sample.counts[pclass] = count;
		total_count[pclass] += count;

		symb_entry.size = end - start;

		symb_entry.name = symbol_names.create(abfd.syms[i].name());
		symb_entry.sym_index = i;

		symb_entry.sample.file_loc.linenr = 0;
		if (debug_info) {
			string filename;
			if (abfd.get_linenr(i, start, filename,
				symb_entry.sample.file_loc.linenr)) {
				symb_entry.sample.file_loc.filename =
					debug_names.create(filename);
			}
		}

		symb_entry.image_name = image_names.create(image_name);
		symb_entry.app_name = image_names.create(app_name);

		symb_entry.sample.vma = abfd.syms[i].vma();
		if ((header.spu_profile == cell_spu_profile) &&
		    header.embedded_offset) {
			symb_entry.spu_offset = header.embedded_offset;
			symb_entry.embedding_filename =
				image_names.create(abfd.get_embedding_filename());
		} else {
			symb_entry.spu_offset = 0;
		}
		symbol_entry const * symbol = symbols->insert(symb_entry);

		if (need_details)
			add_samples(abfd, i, p_it, symbol, pclass, start);
	}
}


void
profile_container::add_samples(op_bfd const & abfd, symbol_index_t sym_index,
                               profile_t::iterator_pair const & p_it,
                               symbol_entry const * symbol, size_t pclass,
			       unsigned long start)
{
	bfd_vma base_vma = abfd.syms[sym_index].vma();

	profile_t::const_iterator it;
	for (it = p_it.first; it != p_it.second ; ++it) {
		sample_entry sample;

		sample.counts[pclass] = it.count();

		sample.file_loc.linenr = 0;
		if (debug_info) {
			string filename;
			if (abfd.get_linenr(sym_index, it.vma(), filename,
					sample.file_loc.linenr)) {
				sample.file_loc.filename =
					debug_names.create(filename);
			}
		}

		sample.vma = (it.vma() - start) + base_vma;

		samples->insert(symbol, sample);
	}
}


symbol_collection const
profile_container::select_symbols(symbol_choice & choice) const
{
	symbol_collection result;

	double const threshold = choice.threshold / 100.0;

	symbol_container::symbols_t::iterator it = symbols->begin();
	symbol_container::symbols_t::iterator const end = symbols->end();

	for (; it != end; ++it) {
		if (choice.match_image
		    && (image_names.name(it->image_name) != choice.image_name))
			continue;

		double const percent =
			op_ratio(it->sample.counts[0], total_count[0]);

		if (percent >= threshold) {
			result.push_back(&*it);

			choice.hints = it->output_hint(choice.hints);
		}
	}

	return result;
}


vector<debug_name_id> const
profile_container::select_filename(double threshold) const
{
	set<debug_name_id> filename_set;

	threshold /= 100.0;

	// Trying to iterate on symbols to create the set of filenames which
	// contain sample does not work: a symbol can contain samples and this
	// symbol is in a source file that contain zero sample because only
	// inline function in this source file contains samples.
	sample_container::samples_iterator sit = samples->begin();
	sample_container::samples_iterator const send = samples->end();

	for (; sit != send; ++sit) {
		debug_name_id name_id = sit->second.file_loc.filename;
		if (name_id.set())
			filename_set.insert(name_id);
	}

	// Give a sort order on filename for the selected pclass.
	vector<filename_by_samples> file_by_samples;

	set<debug_name_id>::const_iterator it = filename_set.begin();
	set<debug_name_id>::const_iterator const end = filename_set.end();
	for (; it != end; ++it) {
		// FIXME: is samples_count() the right interface now ?
		count_array_t counts = samples_count(*it);

		double const ratio = op_ratio(counts[0], total_count[0]);
		filename_by_samples const f(*it, ratio);

		file_by_samples.push_back(f);
	}

	// now sort the file_by_samples entry.
	sort(file_by_samples.begin(), file_by_samples.end());

	// 2.91.66 doesn't like const_reverse_iterator in this context
	vector<filename_by_samples>::reverse_iterator cit
		= file_by_samples.rbegin();
	vector<filename_by_samples>::reverse_iterator const cend
		= file_by_samples.rend();

	vector<debug_name_id> result;
	for (; cit != cend; ++cit) {
		if (cit->percent >= threshold)
			result.push_back(cit->filename);
	}

	return result;
}


count_array_t profile_container::samples_count() const
{
	return total_count;
}


// Rest here are delegated to our private implementation.

symbol_entry const *
profile_container::find_symbol(string const & image_name, bfd_vma vma) const
{
	return symbols->find_by_vma(image_name, vma);
}


symbol_collection const
profile_container::find_symbol(debug_name_id filename, size_t linenr) const
{
	return symbols->find(filename, linenr);
}


symbol_collection const
profile_container::select_symbols(debug_name_id filename) const
{
	return symbols->find(filename);
}

sample_entry const *
profile_container::find_sample(symbol_entry const * symbol, bfd_vma vma) const
{
	return samples->find_by_vma(symbol, vma);
}


count_array_t profile_container::samples_count(debug_name_id filename_id) const
{
	return samples->accumulate_samples(filename_id);
}


count_array_t profile_container::samples_count(debug_name_id filename,
				    size_t linenr) const
{
	return samples->accumulate_samples(filename, linenr);
}


sample_container::samples_iterator
profile_container::begin(symbol_entry const * symbol) const
{
	return samples->begin(symbol);
}


sample_container::samples_iterator
profile_container::end(symbol_entry const * symbol) const
{
	return samples->end(symbol);
}


sample_container::samples_iterator profile_container::begin() const
{
	return samples->begin();
}


sample_container::samples_iterator profile_container::end() const
{
	return samples->end();
}

symbol_entry const * profile_container::find(symbol_entry const & symbol) const
{
	return symbols->find(symbol);
}

symbol_container::symbols_t::iterator profile_container::begin_symbol() const
{
	return symbols->begin();
}

symbol_container::symbols_t::iterator profile_container::end_symbol() const
{
	return symbols->end();
}
