/**
 * @file profile.cpp
 * Encapsulation for samples files over all profile classes
 * belonging to the same binary image
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <unistd.h>
#include <cstring>

#include <iostream>
#include <string>
#include <sstream>
#include <cstring>

#include <cerrno>

#include "op_exception.h"
#include "op_header.h"
#include "op_config.h"
#include "op_sample_file.h"
#include "profile.h"
#include "op_bfd.h"
#include "cverb.h"
#include "populate_for_spu.h"

using namespace std;

profile_t::profile_t()
	: start_offset(0)
{
}


// static member
count_type profile_t::sample_count(string const & filename)
{
	odb_t samples_db;

	open_sample_file(filename, samples_db);

	count_type count = 0;

	odb_node_nr_t node_nr, pos;
	odb_node_t * node = odb_get_iterator(&samples_db, &node_nr);
	for (pos = 0; pos < node_nr; ++pos)
		count += node[pos].value;

	odb_close(&samples_db);

	return count;
}

//static member
enum profile_type profile_t::is_spu_sample_file(string const & filename)
{
	profile_type retval;
	odb_t samples_db;
	open_sample_file(filename, samples_db);
	opd_header const & hdr =
		*static_cast<opd_header *>(odb_get_data(&samples_db));
	retval = hdr.spu_profile ? cell_spu_profile: normal_profile;
	odb_close(&samples_db);
	return retval;
}

//static member
void profile_t::open_sample_file(string const & filename, odb_t & db)
{
	// Check first if the sample file version is ok else odb_open() can
	// fail and the error message will be obscure.
	opd_header head = read_header(filename);

	if (head.version != OPD_VERSION) {
		ostringstream os;
		os << "oprofpp: samples files version mismatch, are you "
		   << "running a daemon and post-profile tools with version "
		   <<  "mismatch ?\n";
		throw op_fatal_error(os.str());
	}

	int rc = odb_open(&db, filename.c_str(), ODB_RDONLY,
		sizeof(struct opd_header));

	if (rc)
		throw op_fatal_error(filename + ": " + strerror(rc));
}

void profile_t::add_sample_file(string const & filename)
{
	odb_t samples_db;

	open_sample_file(filename, samples_db);

	opd_header const & head =
		*static_cast<opd_header *>(odb_get_data(&samples_db));

	// if we already read a sample file header pointer is non null
	if (file_header.get())
		op_check_header(head, *file_header, filename);
	else
		file_header.reset(new opd_header(head));

	odb_node_nr_t node_nr, pos;
	odb_node_t * node = odb_get_iterator(&samples_db, &node_nr);

	for (pos = 0; pos < node_nr; ++pos) {
		ordered_samples_t::iterator it = 
		    ordered_samples.find(node[pos].key);
		if (it != ordered_samples.end()) {
			it->second += node[pos].value;
		} else {
			ordered_samples_t::value_type
				val(node[pos].key, node[pos].value);
			ordered_samples.insert(val);
		}
	}

	odb_close(&samples_db);
}


void profile_t::set_offset(op_bfd const & abfd)
{
	// if no bfd file has been located for this samples file, we can't
	// shift sample because abfd.get_symbol_range() return the whole
	// address space and setting a non zero start_offset will overflow
	// in get_symbol_range() caller.
	if (abfd.valid()) {
		opd_header const & header = get_header();
		if (header.anon_start) {
			start_offset = header.anon_start;
		} else if (header.is_kernel) {
			start_offset = abfd.get_start_offset(0);
		}
	}
	cverb << (vdebug) << "start_offset is now " << start_offset << endl;
}


profile_t::iterator_pair
profile_t::samples_range(odb_key_t start, odb_key_t end) const
{
	// Check the start position isn't before start_offset:
	// this avoids wrapping/underflowing start/end.
	// This can happen on e.g. ARM kernels, where .init is
	// mapped before .text - we just have to skip any such
	// .init symbols.
	if (start < start_offset) {
		return make_pair(const_iterator(ordered_samples.end(), 0), 
			const_iterator(ordered_samples.end(), 0));
	}
	
	start -= start_offset;
	end -= start_offset;

	// sanity check if start > end caller will enter into an infinite loop
	if (start > end) {
		throw op_fatal_error("profile_t::samples_range(): start > end"
			" something wrong with kernel or module layout ?\n"
			"please report problem to "
			"oprofile-list@lists.sourceforge.net");
	}

	ordered_samples_t::const_iterator first = 
		ordered_samples.lower_bound(start);
	ordered_samples_t::const_iterator last =
		ordered_samples.lower_bound(end);

	return make_pair(const_iterator(first, start_offset),
		const_iterator(last, start_offset));
}


profile_t::iterator_pair profile_t::samples_range() const
{
	ordered_samples_t::const_iterator first = ordered_samples.begin();
	ordered_samples_t::const_iterator last = ordered_samples.end();

	return make_pair(const_iterator(first, start_offset),
		const_iterator(last, start_offset));
}
