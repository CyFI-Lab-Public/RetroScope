/**
 * @file callgraph_container.cpp
 * Container associating symbols and caller/caller symbols
 *
 * @remark Copyright 2004 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <cstdlib>

#include <map>
#include <set>
#include <algorithm>
#include <iterator>
#include <string>
#include <iostream>
#include <numeric>

#include "callgraph_container.h"
#include "cverb.h"
#include "parse_filename.h"
#include "profile_container.h"
#include "arrange_profiles.h" 
#include "populate.h"
#include "string_filter.h"
#include "op_bfd.h"
#include "op_sample_file.h"
#include "locate_images.h"

using namespace std;

namespace {

bool operator==(cg_symbol const & lhs, cg_symbol const & rhs)
{
	less_symbol cmp_symb;
	return !cmp_symb(lhs, rhs) && !cmp_symb(rhs, lhs);
}


// we store {caller,callee} inside a single u64
odb_key_t caller_to_key(u32 value)
{
	return odb_key_t(value) << 32;
}


u32 key_to_callee(odb_key_t key)
{
	return key & 0xffffffff;
}


bool compare_by_callee_vma(pair<odb_key_t, count_type> const & lhs,
                           pair<odb_key_t, count_type> const & rhs)
{
	return (key_to_callee(lhs.first)) < (key_to_callee(rhs.first));
}


/*
 * We need 2 comparators for callgraph to get the desired output:
 *
 *	caller_with_few_samples
 *	caller_with_many_samples
 * function_with_many_samples
 *	callee_with_many_samples
 *	callee_with_few_samples
 */

bool
compare_arc_count(symbol_entry const & lhs, symbol_entry const & rhs)
{
	return lhs.sample.counts[0] < rhs.sample.counts[0];
}


bool
compare_arc_count_reverse(symbol_entry const & lhs, symbol_entry const & rhs)
{
	return rhs.sample.counts[0] < lhs.sample.counts[0];
}


// find the nearest bfd symbol for the given file offset and check it's
// in range
op_bfd_symbol const *
get_symbol_by_filepos(op_bfd const & bfd, u32 bfd_offset,
                      vma_t offset, symbol_index_t & i)
{
	offset += bfd_offset;
	op_bfd_symbol tmpsym(offset, 0, string());

	// sorted by filepos so this will find the nearest
	vector<op_bfd_symbol>::const_iterator it =
		upper_bound(bfd.syms.begin(), bfd.syms.end(), tmpsym);

	if (it != bfd.syms.begin())
		--it;

	if (it == bfd.syms.end()) {
		cerr << "get_symbol_by_filepos: no symbols at all?" << endl;
		abort();
	}

	// if the offset is past the end of the symbol, we didn't find one
	u32 const end_offset = it->size() + it->filepos();

	if (offset >= end_offset) {
		// let's be verbose for now
		cerr << "warning: dropping hyperspace sample at offset "
		     << hex << offset << " >= " << end_offset
		     << " for binary " << bfd.get_filename() << dec << endl;
		return NULL;
	}

	i = distance(bfd.syms.begin(), it);
	return &(*it);
}


/// temporary caller and callee data held during processing
class call_data {
public:
	call_data(profile_container const & p, profile_t const & pr,
	          op_bfd const & bfd, u32 boff, image_name_id iid,
	          image_name_id aid, bool debug_info)
		: pc(p), profile(pr), b(bfd), boffset(boff), image(iid),
		  app(aid), debug(debug_info) {}

	/// point to a caller symbol
	void caller_sym(symbol_index_t i) {
		sym = symbol_entry();

		unsigned long long start;
		unsigned long long end;
		b.get_symbol_range(i, start, end);

		samples.clear();

		// see profile_t::samples_range() for why we need this check
		if (start > boffset) {
			profile_t::iterator_pair p_it = profile.samples_range(
				caller_to_key(start - boffset),
				caller_to_key(end - boffset));

			// Our odb_key_t contain (from_eip << 32 | to_eip),
			// the range of keys we selected above contains one
			// caller but different callees, and due to the
			// ordering callee offsets are not consecutive: so
			// we must sort them first.

			for (; p_it.first != p_it.second; ++p_it.first) {
				samples.push_back(make_pair(p_it.first.vma(),
					p_it.first.count()));
			}

			sort(samples.begin(), samples.end(),
			     compare_by_callee_vma);
		}

		sym.size = end - start;
		sym.name = symbol_names.create(b.syms[i].name());
		sym.sample.vma = b.syms[i].vma();

		finish_sym(i, start);

		if (cverb << vdebug) {
			cverb << vdebug << hex << "Caller sym: "
			      << b.syms[i].name() << " filepos " << start
			      << "-" << end << dec << endl;
		}
	}

	/// point to a callee symbol
	bool callee_sym(u32 off) {
		sym = symbol_entry();

		symbol_index_t i = 0;
		op_bfd_symbol const * bfdsym =
			get_symbol_by_filepos(b, boffset, off, i);

		if (!bfdsym)
			return false;

		callee_end = bfdsym->size() + bfdsym->filepos() - boffset;

		sym.size = bfdsym->size();
		sym.name = symbol_names.create(bfdsym->name());
		sym.sample.vma = bfdsym->vma();

		finish_sym(i, bfdsym->filepos());

		if (cverb << vdebug) {
			cverb << vdebug << hex << "Callee sym: "
			      << bfdsym->name() << " filepos "
			      << bfdsym->filepos() << "-"
			      << (bfdsym->filepos() + bfdsym->size())
			      << dec << endl;
		}
		return true;
	}

	void verbose_bfd(string const & prefix) const {
		cverb << vdebug << prefix << " " << b.get_filename()
		      << " offset " << boffset << " app "
		      << image_names.name(app) << endl;
	}

	typedef vector<pair<odb_key_t, count_type> > samples_t;

	typedef samples_t::const_iterator const_iterator;

	samples_t samples;
	symbol_entry sym;
	u32 callee_end;

private:
	/// fill in the rest of the sym
	void finish_sym(symbol_index_t i, unsigned long start) {
		sym.image_name = image;
		sym.app_name = app;
		symbol_entry const * self = pc.find(sym);
		if (self)
			sym.sample.counts = self->sample.counts;

		if (debug) {
			string filename;
			file_location & loc = sym.sample.file_loc;
			if (b.get_linenr(i, start, filename, loc.linenr))
				loc.filename = debug_names.create(filename);
		}
	}

	profile_container const & pc;
	profile_t const & profile;
	op_bfd const & b;
	u32 boffset;
	image_name_id image;
	image_name_id app;
	bool debug;
};


/// accumulate all samples for a given caller/callee pair
count_type
accumulate_callee(call_data::const_iterator & it, call_data::const_iterator end,
                  u32 callee_end)
{
	count_type count = 0;
	call_data::const_iterator const start = it;

	while (it != end) {
		u32 offset = key_to_callee(it->first);

		if (cverb << (vdebug & vlevel1)) {
			cverb << (vdebug & vlevel1) << hex << "offset: "
			      << offset << dec << endl;
		}

		// stop if we pass the end of the callee
		if (offset >= callee_end)
			break;

		count += it->second;
		++it;
	}

	// If we haven't advanced at all, then we'll get
	// an infinite loop, so we must abort.
	if (it == start) {
		cerr << "failure to advance iterator\n";
		abort();
	}

	return count;
}


} // anonymous namespace


void arc_recorder::
add(symbol_entry const & caller, symbol_entry const * callee,
    count_array_t const & arc_count)
{
	cg_data & data = sym_map[caller];

	// If we have a callee, add it to the caller's list, then
	// add the caller to the callee's list.
	if (callee) {
		data.callees[*callee] += arc_count;

		cg_data & callee_data = sym_map[*callee];

		callee_data.callers[caller] += arc_count;
	}
}


void arc_recorder::process_children(cg_symbol & sym, double threshold)
{
	// generate the synthetic self entry for the symbol
	symbol_entry self = sym;

	self.name = symbol_names.create(symbol_names.demangle(self.name)
	                                + " [self]");

	sym.total_callee_count += self.sample.counts;
	sym.callees.push_back(self);

	sort(sym.callers.begin(), sym.callers.end(), compare_arc_count);
	sort(sym.callees.begin(), sym.callees.end(), compare_arc_count_reverse);

	// FIXME: this relies on sort always being sample count

	cg_symbol::children::iterator cit = sym.callers.begin();
	cg_symbol::children::iterator cend = sym.callers.end();

	while (cit != cend && op_ratio(cit->sample.counts[0],
	       sym.total_caller_count[0]) < threshold)
		++cit;

	if (cit != cend)
		sym.callers.erase(sym.callers.begin(), cit);

	cit = sym.callees.begin();
	cend = sym.callees.end();

	while (cit != cend && op_ratio(cit->sample.counts[0],
	       sym.total_callee_count[0]) >= threshold)
		++cit;

	if (cit != cend)
		sym.callees.erase(cit, sym.callees.end());
}


void arc_recorder::
process(count_array_t total, double threshold,
        string_filter const & sym_filter)
{
	map_t::const_iterator it;
	map_t::const_iterator end = sym_map.end();

	for (it = sym_map.begin(); it != end; ++it) {
		cg_symbol sym((*it).first);
		cg_data const & data = (*it).second;

		// threshold out the main symbol if needed
		if (op_ratio(sym.sample.counts[0], total[0]) < threshold)
			continue;

		// FIXME: slow?
		if (!sym_filter.match(symbol_names.demangle(sym.name)))
			continue;

		cg_data::children::const_iterator cit;
		cg_data::children::const_iterator cend = data.callers.end();

		for (cit = data.callers.begin(); cit != cend; ++cit) {
			symbol_entry csym = cit->first;
			csym.sample.counts = cit->second;
			sym.callers.push_back(csym);
			sym.total_caller_count += cit->second;
		}

		cend = data.callees.end();

		for (cit = data.callees.begin(); cit != cend; ++cit) {
			symbol_entry csym = cit->first;
			csym.sample.counts = cit->second;
			sym.callees.push_back(csym);
			sym.total_callee_count += cit->second;
		}

		process_children(sym, threshold);

		// insert sym into cg_syms_objs
		// then store pointer to sym in cg_syms
		cg_syms.push_back(&(*cg_syms_objs.insert(cg_syms_objs.end(), sym)));
	}
}


symbol_collection const & arc_recorder::get_symbols() const
{
	return cg_syms;
}


void callgraph_container::populate(list<inverted_profile> const & iprofiles,
   extra_images const & extra, bool debug_info, double threshold,
   bool merge_lib, string_filter const & sym_filter)
{
	this->extra_found_images = extra;
	// non callgraph samples container, we record sample at symbol level
	// not at vma level.
	profile_container pc(debug_info, false, extra_found_images);

	list<inverted_profile>::const_iterator it;
	list<inverted_profile>::const_iterator const end = iprofiles.end();
	for (it = iprofiles.begin(); it != end; ++it) {
		// populate_caller_image take care about empty sample filename
		populate_for_image(pc, *it, sym_filter, 0);
	}

	add_symbols(pc);

	total_count = pc.samples_count();

	for (it = iprofiles.begin(); it != end; ++it) {
		for (size_t i = 0; i < it->groups.size(); ++i) {
			populate(it->groups[i], it->image,
				i, pc, debug_info, merge_lib);
		}
	}

	recorder.process(total_count, threshold / 100.0, sym_filter);
}


void callgraph_container::populate(list<image_set> const & lset,
	string const & app_image, size_t pclass,
	profile_container const & pc, bool debug_info, bool merge_lib)
{
	list<image_set>::const_iterator lit;
	list<image_set>::const_iterator const lend = lset.end();
	for (lit = lset.begin(); lit != lend; ++lit) {
		list<profile_sample_files>::const_iterator pit;
		list<profile_sample_files>::const_iterator pend
			= lit->files.end();
		for (pit = lit->files.begin(); pit != pend; ++pit) {
			populate(pit->cg_files, app_image,
				 pclass, pc, debug_info, merge_lib);
		}
	}
}


void callgraph_container::populate(list<string> const & cg_files,
	string const & app_image, size_t pclass,
	profile_container const & pc, bool debug_info, bool merge_lib)
{
	list<string>::const_iterator it;
	list<string>::const_iterator const end = cg_files.end();
	for (it = cg_files.begin(); it != end; ++it) {
		cverb << vdebug << "samples file : " << *it << endl;

		parsed_filename caller_file =
			parse_filename(*it, extra_found_images);
		string const app_name = caller_file.image;

		image_error error;
		extra_found_images.find_image_path(caller_file.lib_image,
				error, false);

		if (error != image_ok)
			report_image_error(caller_file.lib_image,
					   error, false, extra_found_images);

		bool caller_bfd_ok = true;
		op_bfd caller_bfd(caller_file.lib_image,
			string_filter(), extra_found_images, caller_bfd_ok);
		if (!caller_bfd_ok)
			report_image_error(caller_file.lib_image,
			                   image_format_failure, false,
					   extra_found_images);

		parsed_filename callee_file =
			parse_filename(*it, extra_found_images);

		extra_found_images.find_image_path(callee_file.cg_image,
				error, false);
		if (error != image_ok)
			report_image_error(callee_file.cg_image,
					   error, false, extra_found_images);

		bool callee_bfd_ok = true;
		op_bfd callee_bfd(callee_file.cg_image,
			string_filter(), extra_found_images, callee_bfd_ok);
		if (!callee_bfd_ok)
			report_image_error(callee_file.cg_image,
		                           image_format_failure, false,
					   extra_found_images);

		profile_t profile;
		// We can't use start_offset support in profile_t, give
		// it a zero offset and we will fix that in add()
		profile.add_sample_file(*it);
		add(profile, caller_bfd, caller_bfd_ok, callee_bfd,
		    merge_lib ? app_image : app_name, pc,
		    debug_info, pclass);
	}
}


void callgraph_container::
add(profile_t const & profile, op_bfd const & caller_bfd, bool caller_bfd_ok,
   op_bfd const & callee_bfd, string const & app_name,
   profile_container const & pc, bool debug_info, size_t pclass)
{
	string const image_name = caller_bfd.get_filename();

	opd_header const & header = profile.get_header();

	// We can't use kernel sample file w/o the binary else we will
	// use it with a zero offset, the code below will abort because
	// we will get incorrect callee sub-range and out of range
	// callee vma. FIXME
	if (header.is_kernel && !caller_bfd_ok)
		return;

	// We must handle start_offset, this offset can be different for the
	// caller and the callee: kernel sample traversing the syscall barrier.
	u32 caller_offset;
	if (header.is_kernel)
		caller_offset = caller_bfd.get_start_offset(0);
	else
		caller_offset = header.anon_start;

	u32 callee_offset;
	if (header.cg_to_is_kernel)
		callee_offset = callee_bfd.get_start_offset(0);
	else
		callee_offset = header.cg_to_anon_start;

	image_name_id image_id = image_names.create(image_name);
	image_name_id callee_image_id = image_names.create(callee_bfd.get_filename());
	image_name_id app_id = image_names.create(app_name);

	call_data caller(pc, profile, caller_bfd, caller_offset, image_id,
	                   app_id, debug_info);
	call_data callee(pc, profile, callee_bfd, callee_offset,
	                   callee_image_id, app_id, debug_info);

	if (cverb << vdebug) {
		caller.verbose_bfd("Caller:");
		callee.verbose_bfd("Callee:");
	}

	// For each symbol in the caller bfd, process all arcs to
	// callee bfd symbols

	for (symbol_index_t i = 0; i < caller_bfd.syms.size(); ++i) {

		caller.caller_sym(i);

		call_data::const_iterator dit = caller.samples.begin();
		call_data::const_iterator dend = caller.samples.end();
		while (dit != dend) {
			// if we can't find the callee, skip an arc
			if (!callee.callee_sym(key_to_callee(dit->first))) {
				++dit;
				continue;
			}

			count_array_t arc_count;
			arc_count[pclass] =
				accumulate_callee(dit, dend, callee.callee_end);

			recorder.add(caller.sym, &callee.sym, arc_count);
		}
	}
}


void callgraph_container::add_symbols(profile_container const & pc)
{
	symbol_container::symbols_t::iterator it;
	symbol_container::symbols_t::iterator const end = pc.end_symbol();

	for (it = pc.begin_symbol(); it != end; ++it)
		recorder.add(*it, 0, count_array_t());
}


column_flags callgraph_container::output_hint() const
{
	column_flags output_hints = cf_none;

	// FIXME: costly: must we access directly recorder map ?
	symbol_collection syms = recorder.get_symbols();

	symbol_collection::iterator it;
	symbol_collection::iterator const end = syms.end();
	for (it = syms.begin(); it != end; ++it)
		output_hints = (*it)->output_hint(output_hints);

	return output_hints;
}


count_array_t callgraph_container::samples_count() const
{
	return total_count;
}


symbol_collection const & callgraph_container::get_symbols() const
{
	return recorder.get_symbols();
}
