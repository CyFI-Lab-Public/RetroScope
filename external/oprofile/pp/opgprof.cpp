/**
 * @file opgprof.cpp
 * Implement opgprof utility
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <iostream>
#include <cstdio>

#include "op_header.h"
#include "profile.h"
#include "op_libiberty.h"
#include "op_fileio.h"
#include "string_filter.h"
#include "profile_container.h"
#include "arrange_profiles.h"
#include "image_errors.h"
#include "opgprof_options.h"
#include "cverb.h"
#include "op_file.h"

using namespace std;

extern profile_classes classes;

namespace {

#define GMON_VERSION 1
#define GMON_TAG_TIME_HIST 0
#define GMON_TAG_CG_ARC 1

struct gmon_hdr {
	char cookie[4];
	u32 version;
	u32 spare[3];
};


void op_write_vma(FILE * fp, op_bfd const & abfd, bfd_vma vma)
{
	// bfd vma write size is a per binary property not a bfd
	// configuration property
	switch (abfd.bfd_arch_bits_per_address()) {
		case 32:
			op_write_u32(fp, vma);
			break;
		case 64:
			op_write_u64(fp, vma);
			break;
		default:
			cerr << "oprofile: unknown vma size for this binary\n";
			exit(EXIT_FAILURE);
	}
}


void get_vma_range(bfd_vma & min, bfd_vma & max,
                   profile_container const & samples)
{
	min = bfd_vma(-1);
	max = 0;

	sample_container::samples_iterator it  = samples.begin();
	sample_container::samples_iterator end = samples.end();
	for (; it != end ; ++it) {
		if (it->second.vma < min)
			min = it->second.vma;
		if (it->second.vma > max)
			max = it->second.vma;
	}

	if (min == bfd_vma(-1))
		min = 0;
	// we must return a range [min, max) not a range [min, max]
	if (max != 0)
		max += 1;
}


/**
 * @param abfd  bfd object
 * @param samples_files  profile container to act on
 * @param gap  a power of 2
 *
 * return true if all sample in samples_files are at least aligned on gap. This
 * function is used to get at runtime the right size of gprof bin size
 * reducing gmon.out on arch with fixed size instruction length
 *
 */
bool aligned_samples(profile_container const & samples, int gap)
{
	sample_container::samples_iterator it  = samples.begin();
	sample_container::samples_iterator end = samples.end();
	for (; it != end ; ++it) {
		if (it->second.vma % gap)
			return false;
	}

	return true;
}


void output_cg(FILE * fp, op_bfd const & abfd, profile_t const & cg_db)
{
	opd_header const & header = cg_db.get_header();
	bfd_vma offset = 0;
	if (header.is_kernel)
		offset = abfd.get_start_offset(0);
	else
		offset = header.anon_start;
 
	profile_t::iterator_pair p_it = cg_db.samples_range();
	for (; p_it.first != p_it.second; ++p_it.first) {
		bfd_vma from = p_it.first.vma() >> 32;
		bfd_vma to = p_it.first.vma() & 0xffffffff;

		op_write_u8(fp, GMON_TAG_CG_ARC);
		op_write_vma(fp, abfd, abfd.offset_to_pc(from + offset));
		op_write_vma(fp, abfd, abfd.offset_to_pc(to + offset));
		u32 count = p_it.first.count();
		if (count != p_it.first.count()) {
			count = (u32)-1;
			cerr << "Warning: capping sample count by "
			     << p_it.first.count() - count << endl;
		}
		op_write_u32(fp, p_it.first.count());
	}
}


void output_gprof(op_bfd const & abfd, profile_container const & samples,
                  profile_t const & cg_db, string const & gmon_filename)
{
	static gmon_hdr hdr = { { 'g', 'm', 'o', 'n' }, GMON_VERSION, {0, 0, 0 } };

	bfd_vma low_pc;
	bfd_vma high_pc;

	/* FIXME worth to try more multiplier ?	*/
	int multiplier = 2;
	if (aligned_samples(samples, 4))
		multiplier = 8;

	cverb << vdebug << "opgrof multiplier: " << multiplier << endl;

	get_vma_range(low_pc, high_pc, samples);

	cverb << vdebug << "low_pc: " << hex << low_pc << " " << "high_pc: "
	      << high_pc << dec << endl;

	// round-down low_pc to ensure bin number is correct in the inner loop
	low_pc = (low_pc / multiplier) * multiplier;
	// round-up high_pc to ensure a correct histsize calculus
	high_pc = ((high_pc + multiplier - 1) / multiplier) * multiplier;

	cverb << vdebug << "low_pc: " << hex << low_pc << " " << "high_pc: "
	      << high_pc << dec << endl;

	size_t histsize = (high_pc - low_pc) / multiplier;

	// FIXME: must we skip the flat profile write if histsize == 0 ?
	// (this can occur with callgraph w/o samples to the binary) but in
	// this case user must gprof --no-flat-profile which is a bit boring
	// and result *seems* weirds.

	FILE * fp = op_open_file(gmon_filename.c_str(), "w");

	op_write_file(fp, &hdr, sizeof(gmon_hdr));
	op_write_u8(fp, GMON_TAG_TIME_HIST);

	op_write_vma(fp, abfd, low_pc);
	op_write_vma(fp, abfd, high_pc);
	/* size of histogram */
	op_write_u32(fp, histsize);
	/* profiling rate */
	op_write_u32(fp, 1);
	op_write_file(fp, "samples\0\0\0\0\0\0\0\0", 15);
	/* abbreviation */
	op_write_u8(fp, '1');

	u16 * hist = (u16*)xcalloc(histsize, sizeof(u16));

	profile_container::symbol_choice choice;
	choice.threshold = options::threshold;
	symbol_collection symbols = samples.select_symbols(choice);

	symbol_collection::const_iterator sit = symbols.begin();
	symbol_collection::const_iterator send = symbols.end();

	for (; sit != send; ++sit) {
		sample_container::samples_iterator it  = samples.begin(*sit);
		sample_container::samples_iterator end = samples.end(*sit);
		for (; it != end ; ++it) {
			u32 pos = (it->second.vma - low_pc) / multiplier;
			count_type count = it->second.counts[0];

			if (pos >= histsize) {
				cerr << "Bogus histogram bin " << pos
				     << ", larger than " << pos << " !\n";
				continue;
			}
	
			if (hist[pos] + count > (u16)-1) {
				hist[pos] = (u16)-1;
				cerr <<	"Warning: capping sample count by "
				     << hist[pos] + count - ((u16)-1) << endl;
			} else {
				hist[pos] += (u16)count;
			}
		}
	}

	op_write_file(fp, hist, histsize * sizeof(u16));

	if (!cg_db.empty())
		output_cg(fp, abfd, cg_db);

	op_close_file(fp);

	free(hist);
}


void
load_samples(op_bfd const & abfd, list<profile_sample_files> const & files,
                  string const & image, profile_container & samples)
{
	list<profile_sample_files>::const_iterator it = files.begin();
	list<profile_sample_files>::const_iterator const end = files.end();

	for (; it != end; ++it) {
		// we can get call graph w/o any samples to the binary
		if (it->sample_filename.empty())
			continue;
			
		cverb << vsfile << "loading flat samples files : "
		      << it->sample_filename << endl;

		profile_t profile;

		profile.add_sample_file(it->sample_filename);
		profile.set_offset(abfd);

		check_mtime(abfd.get_filename(), profile.get_header());

		samples.add(profile, abfd, image, 0);
	}
}


void load_cg(profile_t & cg_db, list<profile_sample_files> const & files)
{
	list<profile_sample_files>::const_iterator it = files.begin();
	list<profile_sample_files>::const_iterator const end = files.end();

	/* the list of non cg files is a super set of the list of cg file
	 * (module always log a samples to non-cg files before logging
	 * call stack) so by using the list of non-cg file we are sure to get
	 * all existing cg files.
	 */
	for (; it != end; ++it) {
		list<string>::const_iterator cit;
		list<string>::const_iterator const cend = it->cg_files.end();
		for (cit = it->cg_files.begin(); cit != cend; ++cit) {
			// FIXME: do we need filtering ?
			/* We can't handle start_offset now but after splitting
			 * data in from/to eip. */
			cverb << vsfile << "loading cg samples file : " 
			      << *cit << endl;
			cg_db.add_sample_file(*cit);
		}
	}
}


int opgprof(options::spec const & spec)
{
	handle_options(spec);

	profile_container samples(false, true, classes.extra_found_images);

	bool ok = image_profile.error == image_ok;
	// FIXME: symbol_filter would be allowed through option
	op_bfd abfd(image_profile.image, string_filter(),
		    classes.extra_found_images, ok);
	if (!ok && image_profile.error == image_ok)
		image_profile.error = image_format_failure;

	if (image_profile.error != image_ok) {
		report_image_error(image_profile, true,
				   classes.extra_found_images);
		exit(EXIT_FAILURE);
	}

	profile_t cg_db;
	
	image_group_set const & groups = image_profile.groups[0];
	image_group_set::const_iterator it;
	for (it = groups.begin(); it != groups.end(); ++it) {
		load_samples(abfd, it->files, image_profile.image, samples);

		load_cg(cg_db, it->files);
	}

	output_gprof(abfd, samples, cg_db, options::gmon_filename);

	return 0;
}


} // anonymous namespace


int main(int argc, char const * argv[])
{
	return run_pp_tool(argc, argv, opgprof);
}
