/**
 * @file format_output.cpp
 * outputting format for symbol lists
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

/* older glibc has C99 INFINITY in _GNU_SOURCE */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <cassert>
#include <sstream>
#include <iomanip>
#include <iostream>
#include <cmath>

#include "string_manip.h"
#include "string_filter.h"

#include "format_output.h"
#include "profile_container.h"
#include "callgraph_container.h"
#include "diff_container.h"
#include "arrange_profiles.h"
#include "xml_output.h"
#include "xml_utils.h"
#include "cverb.h"

using namespace std;

namespace {


string const get_linenr_info(file_location const floc, bool lf)
{
	ostringstream out;

	string const & filename = lf
		? debug_names.name(floc.filename)
		: debug_names.basename(floc.filename);

	if (!filename.empty())
		out << filename << ":" << floc.linenr;
	else 
		out << "(no location information)";

	return out.str();
}

string get_vma(bfd_vma vma, bool vma_64)
{
	ostringstream out;
	int width = vma_64 ? 16 : 8;

	out << hex << setw(width) << setfill('0') << vma;

	return out.str();
}

string get_percent(count_type dividend, count_type divisor)
{
	double ratio = op_ratio(dividend, divisor);

	return ::format_percent(ratio * 100, percent_int_width,
	                     percent_fract_width);
}

bool extract_linenr_info(string const & info, string & file, size_t & line)
{
	line = 0;
	file = "";
	string::size_type colon_pos = info.find(":");

	if (colon_pos == string::npos)
		return false;

	file = info.substr(0, colon_pos);
	istringstream is_info(info.substr(colon_pos+1));
	is_info >> line;
	return true;
}


} // anonymous namespace

namespace format_output {

formatter::formatter(extra_images const & extra)
	:
	nr_classes(1),
	flags(ff_none),
	vma_64(false),
	long_filenames(false),
	need_header(true),
	extra_found_images(extra)
{
	format_map[ff_vma] = field_description(9, "vma", &formatter::format_vma);
	format_map[ff_nr_samples] = field_description(9, "samples", &formatter::format_nr_samples);
	format_map[ff_nr_samples_cumulated] = field_description(14, "cum. samples", &formatter::format_nr_cumulated_samples);
	format_map[ff_percent] = field_description(9, "%", &formatter::format_percent);
	format_map[ff_percent_cumulated] = field_description(11, "cum. %", &formatter::format_cumulated_percent);
	format_map[ff_linenr_info] = field_description(28, "linenr info", &formatter::format_linenr_info);
	format_map[ff_image_name] = field_description(25, "image name", &formatter::format_image_name);
	format_map[ff_app_name] = field_description(25, "app name", &formatter::format_app_name);
	format_map[ff_symb_name] = field_description(30, "symbol name", &formatter::format_symb_name);
	format_map[ff_percent_details] = field_description(9, "%", &formatter::format_percent_details);
	format_map[ff_percent_cumulated_details] = field_description(10, "cum. %", &formatter::format_cumulated_percent_details);
	format_map[ff_diff] = field_description(10, "diff %", &formatter::format_diff);
}


formatter::~formatter()
{
}


void formatter::set_nr_classes(size_t nr)
{
	nr_classes = nr;
}


void formatter::add_format(format_flags flag)
{
	flags = static_cast<format_flags>(flags | flag);
}


void formatter::show_header(bool on_off)
{
	need_header = on_off;
}
 

void formatter::vma_format_64bit(bool on_off)
{
	vma_64 = on_off;
}


void formatter::show_long_filenames(bool on_off)
{
	long_filenames = on_off;
}


void formatter::show_global_percent(bool on_off)
{
	global_percent = on_off;
}


void formatter::output_header(ostream & out)
{
	if (!need_header)
		return;

	size_t padding = 0;

	// first output the vma field
	if (flags & ff_vma)
		padding = output_header_field(out, ff_vma, padding);

	// the field repeated for each profile class
	for (size_t pclass = 0 ; pclass < nr_classes; ++pclass) {
		if (flags & ff_nr_samples)
			padding = output_header_field(out,
			      ff_nr_samples, padding);

		if (flags & ff_nr_samples_cumulated)
			padding = output_header_field(out, 
			       ff_nr_samples_cumulated, padding);

		if (flags & ff_percent)
			padding = output_header_field(out,
			       ff_percent, padding);

		if (flags & ff_percent_cumulated)
			padding = output_header_field(out,
			       ff_percent_cumulated, padding);

		if (flags & ff_diff)
			padding = output_header_field(out,
				ff_diff, padding);

		if (flags & ff_percent_details)
			padding = output_header_field(out,
			       ff_percent_details, padding);

		if (flags & ff_percent_cumulated_details)
			padding = output_header_field(out,
			       ff_percent_cumulated_details, padding);
	}

	// now the remaining field
	if (flags & ff_linenr_info)
		padding = output_header_field(out, ff_linenr_info, padding);

	if (flags & ff_image_name)
		padding = output_header_field(out, ff_image_name, padding);

	if (flags & ff_app_name)
		padding = output_header_field(out, ff_app_name, padding);

	if (flags & ff_symb_name)
		padding = output_header_field(out, ff_symb_name, padding);

	out << "\n";
}


/// describe each possible field of colummned output.
// FIXME: use % of the screen width here. sum of % equal to 100, then calculate
// ratio between 100 and the selected % to grow non fixed field use also
// lib[n?]curses to get the console width (look info source) (so on add a fixed
// field flags)
size_t formatter::
output_field(ostream & out, field_datum const & datum,
             format_flags fl, size_t padding, bool hide_immutable)
{
	if (!hide_immutable) {
		out << string(padding, ' ');

		field_description const & field(format_map[fl]);
		string str = (this->*field.formatter)(datum);
		out << str;

		// at least one separator char
		padding = 1;
		if (str.length() < field.width)
			padding = field.width - str.length();
	} else {
		field_description const & field(format_map[fl]);
		padding += field.width;
	}

	return padding;
}

 
size_t formatter::
output_header_field(ostream & out, format_flags fl, size_t padding)
{
	out << string(padding, ' ');

	field_description const & field(format_map[fl]);
	out << field.header_name;

	// at least one separator char
	padding = 1;
	if (field.header_name.length() < field.width)
		padding = field.width - field.header_name.length();

	return padding;
}
 

string formatter::format_vma(field_datum const & f)
{
	return get_vma(f.sample.vma, vma_64);
}

 
string formatter::format_symb_name(field_datum const & f)
{
	return symbol_names.demangle(f.symbol.name);
}


string formatter::format_image_name(field_datum const & f)
{
	return get_image_name(f.symbol.image_name, 
		long_filenames 
			? image_name_storage::int_real_filename
			: image_name_storage::int_real_basename,
		extra_found_images);
}

 
string formatter::format_app_name(field_datum const & f)
{
	return get_image_name(f.symbol.app_name,
		long_filenames 
			? image_name_storage::int_real_filename
			: image_name_storage::int_real_basename,
		extra_found_images);
}

 
string formatter::format_linenr_info(field_datum const & f)
{
	return get_linenr_info(f.sample.file_loc, long_filenames);
}

 
string formatter::format_nr_samples(field_datum const & f)
{
	ostringstream out;
	out << f.sample.counts[f.pclass];
	return out.str();
}

 
string formatter::format_nr_cumulated_samples(field_datum const & f)
{
	if (f.diff == -INFINITY)
		return "---";
	ostringstream out;
	f.counts.cumulated_samples[f.pclass] += f.sample.counts[f.pclass];
	out << f.counts.cumulated_samples[f.pclass];
	return out.str();
}

 
string formatter::format_percent(field_datum const & f)
{
	if (f.diff == -INFINITY)
		return "---";
	return get_percent(f.sample.counts[f.pclass], f.counts.total[f.pclass]);
}

 
string formatter::format_cumulated_percent(field_datum const & f)
{
	if (f.diff == -INFINITY)
		return "---";
	f.counts.cumulated_percent[f.pclass] += f.sample.counts[f.pclass];

	return get_percent(f.counts.cumulated_percent[f.pclass],
	                   f.counts.total[f.pclass]);
}

 
string formatter::format_percent_details(field_datum const & f)
{
	return get_percent(f.sample.counts[f.pclass],
		f.counts.total[f.pclass]);
}

 
string formatter::format_cumulated_percent_details(field_datum const & f)
{
	f.counts.cumulated_percent_details[f.pclass] += f.sample.counts[f.pclass];

	return get_percent(f.counts.cumulated_percent_details[f.pclass],
	                   f.counts.total[f.pclass]);
}


string formatter::format_diff(field_datum const & f)
{
	if (f.diff == INFINITY)
		return "+++";
	else if (f.diff == -INFINITY)
		return "---";

	return ::format_percent(f.diff, percent_int_width,
                                percent_fract_width, true);
}


void formatter::
do_output(ostream & out, symbol_entry const & symb, sample_entry const & sample,
          counts_t & c, diff_array_t const & diffs, bool hide_immutable)
{
	size_t padding = 0;

	// first output the vma field
	field_datum datum(symb, sample, 0, c, extra_found_images);
	if (flags & ff_vma)
		padding = output_field(out, datum, ff_vma, padding, false);

	// repeated fields for each profile class
	for (size_t pclass = 0 ; pclass < nr_classes; ++pclass) {
		field_datum datum(symb, sample, pclass, c,
				  extra_found_images, diffs[pclass]);

		if (flags & ff_nr_samples)
			padding = output_field(out, datum,
			       ff_nr_samples, padding, false);

		if (flags & ff_nr_samples_cumulated)
			padding = output_field(out, datum, 
			       ff_nr_samples_cumulated, padding, false);

		if (flags & ff_percent)
			padding = output_field(out, datum,
			       ff_percent, padding, false);

		if (flags & ff_percent_cumulated)
			padding = output_field(out, datum,
			       ff_percent_cumulated, padding, false);

		if (flags & ff_diff)
			padding = output_field(out, datum,
				ff_diff, padding, false);

		if (flags & ff_percent_details)
			padding = output_field(out, datum,
			       ff_percent_details, padding, false);

		if (flags & ff_percent_cumulated_details)
			padding = output_field(out, datum,
			       ff_percent_cumulated_details, padding, false);
	}

	// now the remaining field
	if (flags & ff_linenr_info)
		padding = output_field(out, datum, ff_linenr_info,
		       padding, false);

	if (flags & ff_image_name)
		padding = output_field(out, datum, ff_image_name,
		       padding, hide_immutable);

	if (flags & ff_app_name)
		padding = output_field(out, datum, ff_app_name,
		       padding, hide_immutable);

	if (flags & ff_symb_name)
		padding = output_field(out, datum, ff_symb_name,
		       padding, hide_immutable);

	out << "\n";
}


opreport_formatter::opreport_formatter(profile_container const & p)
	:
	formatter(p.extra_found_images),
	profile(p),
	need_details(false)
{
	counts.total = profile.samples_count();
}

 
void opreport_formatter::show_details(bool on_off)
{
	need_details = on_off;
}


void opreport_formatter::output(ostream & out, symbol_entry const * symb)
{
	do_output(out, *symb, symb->sample, counts);

	if (need_details)
		output_details(out, symb);
}


void opreport_formatter::
output(ostream & out, symbol_collection const & syms)
{
	output_header(out);

	symbol_collection::const_iterator it = syms.begin();
	symbol_collection::const_iterator end = syms.end();
	for (; it != end; ++it)
		output(out, *it);
}


void opreport_formatter::
output_details(ostream & out, symbol_entry const * symb)
{
	counts_t c = counts;

	if (!global_percent)
		c.total = symb->sample.counts;

	// cumulated percent are relative to current symbol.
	c.cumulated_samples = count_array_t();
	c.cumulated_percent = count_array_t();

	sample_container::samples_iterator it = profile.begin(symb);
	sample_container::samples_iterator end = profile.end(symb);
	for (; it != end; ++it) {
		out << "  ";
		do_output(out, *symb, it->second, c, diff_array_t(), true);
	}
}

 
cg_formatter::cg_formatter(callgraph_container const & profile)
	:
	formatter(profile.extra_found_images)
{
	counts.total = profile.samples_count();
}


void cg_formatter::output(ostream & out, symbol_collection const & syms)
{
	// amount of spacing prefixing child and parent lines
	string const child_parent_prefix("  ");

	output_header(out);

	out << string(79, '-') << endl;

	symbol_collection::const_iterator it;
	symbol_collection::const_iterator end = syms.end();

	for (it = syms.begin(); it < end; ++it) {
		cg_symbol const * sym = dynamic_cast<cg_symbol const *>(*it);

		cg_symbol::children::const_iterator cit;
		cg_symbol::children::const_iterator cend = sym->callers.end();

		counts_t c;
		if (global_percent)
			c.total = counts.total;
		else
			c.total = sym->total_caller_count;

		for (cit = sym->callers.begin(); cit != cend; ++cit) {
			out << child_parent_prefix;
			do_output(out, *cit, cit->sample, c);
		}

		do_output(out, *sym, sym->sample, counts);

		c = counts_t();
		if (global_percent)
			c.total = counts.total;
		else
			c.total = sym->total_callee_count;

		cend = sym->callees.end();

		for (cit = sym->callees.begin(); cit != cend; ++cit) {
			out << child_parent_prefix;
			do_output(out, *cit, cit->sample, c);
		}

		out << string(79, '-') << endl;
	}
}


diff_formatter::diff_formatter(diff_container const & profile,
			       extra_images const & extra)
	:
	formatter(extra)
{
	counts.total = profile.samples_count();
}


void diff_formatter::output(ostream & out, diff_collection const & syms)
{
	output_header(out);

	diff_collection::const_iterator it = syms.begin();
	diff_collection::const_iterator end = syms.end();
	for (; it != end; ++it)
		do_output(out, *it, it->sample, counts, it->diffs);
}

// local variables used in generation of XML
// buffer details for output later
ostringstream bytes_out;

// module+symbol table for detecting duplicate symbols
map<string, size_t> symbol_data_table;
size_t symbol_data_index = 0;

/* Return any existing index or add to the table */
size_t xml_get_symbol_index(string const & name)
{
	size_t index = symbol_data_index;
	map<string, size_t>::iterator it = symbol_data_table.find(name);

	if (it == symbol_data_table.end()) {
		symbol_data_table[name] = symbol_data_index++;
		return index;
	}

	return it->second;
}


class symbol_details_t {
public:
	symbol_details_t() { size = index = 0; id = -1; }
	int id;
	size_t size;
	size_t index;
	string details;
};

typedef growable_vector<symbol_details_t> symbol_details_array_t;
symbol_details_array_t symbol_details;
size_t detail_table_index = 0;

xml_formatter::
xml_formatter(profile_container const * p,
	      symbol_collection & s, extra_images const & extra,
	      string_filter const & sf)
	:
	formatter(extra),
	profile(p),
	symbols(s),
	need_details(false),
	symbol_filter(sf)
{
	if (profile)
		counts.total = profile->samples_count();
}


void xml_formatter::
show_details(bool on_off)
{
	need_details = on_off;
}


void xml_formatter::output(ostream & out)
{
	xml_support->build_subclasses(out);

	xml_support->output_program_structure(out);
	output_symbol_data(out);
	if (need_details) {
		out << open_element(DETAIL_TABLE);
		for (size_t i = 0; i < symbol_details.size(); ++i) {
			int id = symbol_details[i].id;

			if (id >= 0) {
				out << open_element(SYMBOL_DETAILS, true);
				out << init_attr(TABLE_ID, (size_t)id);
				out << close_element(NONE, true);
				out << symbol_details[i].details;
				out << close_element(SYMBOL_DETAILS);
			}
		}
		out << close_element(DETAIL_TABLE);

		// output bytesTable
		out << open_element(BYTES_TABLE);
		out << bytes_out.str();
		out << close_element(BYTES_TABLE);
	}

	out << close_element(PROFILE);
}

bool
xml_formatter::get_bfd_object(symbol_entry const * symb, op_bfd * & abfd) const
{
	bool ok = true;

	string const & image_name = get_image_name(symb->image_name,
		image_name_storage::int_filename, extra_found_images);
	if (symb->spu_offset) {
		// FIXME: what about archive:tmp, actually it's not supported
		// for spu since oparchive doesn't archive the real file but
		// in future it would work ?
		string tmp = get_image_name(symb->embedding_filename, 
			image_name_storage::int_filename, extra_found_images);
		if (abfd && abfd->get_filename() == tmp)
			return true;
		delete abfd;
		abfd = new op_bfd(symb->spu_offset, tmp,
				  symbol_filter, extra_found_images, ok);
	} else {
		if (abfd && abfd->get_filename() == image_name)
			return true;
		delete abfd;
		abfd = new op_bfd(image_name, symbol_filter,
				  extra_found_images, ok);

	}

	if (!ok) {
		report_image_error(image_name, image_format_failure,
				   false, extra_found_images);
		delete abfd;
		abfd = 0;
		return false;
	}

	return true;
}

void xml_formatter::
output_the_symbol_data(ostream & out, symbol_entry const * symb, op_bfd * & abfd)
{
	string const name = symbol_names.name(symb->name);
	assert(name.size() > 0);

	string const image = get_image_name(symb->image_name,
		image_name_storage::int_filename, extra_found_images);
	string const qname = image + ":" + name;
	map<string, size_t>::iterator sd_it = symbol_data_table.find(qname);

	if (sd_it != symbol_data_table.end()) {
		// first time we've seen this symbol
		out << open_element(SYMBOL_DATA, true);
		out << init_attr(TABLE_ID, sd_it->second);

		field_datum datum(*symb, symb->sample, 0, counts,
				  extra_found_images);

		output_attribute(out, datum, ff_symb_name, NAME);

		if (flags & ff_linenr_info) {
			output_attribute(out, datum, ff_linenr_info, SOURCE_FILE);
			output_attribute(out, datum, ff_linenr_info, SOURCE_LINE);
		}

		if (name.size() > 0 && name[0] != '?') {
			output_attribute(out, datum, ff_vma, STARTING_ADDR);

			if (need_details) {
				get_bfd_object(symb, abfd);
				if (abfd && abfd->symbol_has_contents(symb->sym_index))
					xml_support->output_symbol_bytes(bytes_out, symb, sd_it->second, *abfd);
			}
		}
		out << close_element();

		// seen so remove (otherwise get several "no symbols")
		symbol_data_table.erase(qname);
	}
}

void xml_formatter::output_cg_children(ostream & out, 
	cg_symbol::children const cg_symb, op_bfd * & abfd)
{
	cg_symbol::children::const_iterator cit;
	cg_symbol::children::const_iterator cend = cg_symb.end();

	for (cit = cg_symb.begin(); cit != cend; ++cit) {
		string const name = symbol_names.name(cit->name);
		string const image = get_image_name(cit->image_name,
			image_name_storage::int_filename, extra_found_images);
		string const qname = image + ":" + name;
		map<string, size_t>::iterator sd_it = symbol_data_table.find(qname);

		if (sd_it != symbol_data_table.end()) {
			symbol_entry const * child = &(*cit);
			output_the_symbol_data(out, child, abfd);
		}
	}
}

void xml_formatter::output_symbol_data(ostream & out)
{
	op_bfd * abfd = NULL;
	sym_iterator it = symbols.begin();
	sym_iterator end = symbols.end();

	out << open_element(SYMBOL_TABLE);
	for ( ; it != end; ++it) {
		symbol_entry const * symb = *it;
		cg_symbol const * cg_symb = dynamic_cast<cg_symbol const *>(symb);
		output_the_symbol_data(out, symb, abfd);
		if (cg_symb) {
			/* make sure callers/callees are included in SYMBOL_TABLE */
			output_cg_children(out, cg_symb->callers, abfd);
			output_cg_children(out, cg_symb->callees, abfd);
		}
	}
	out << close_element(SYMBOL_TABLE);

	delete abfd;
}

string  xml_formatter::
output_symbol_details(symbol_entry const * symb,
    size_t & detail_index, size_t const lo, size_t const hi)
{
	if (!has_sample_counts(symb->sample.counts, lo, hi))
		return "";

	sample_container::samples_iterator it = profile->begin(symb);
	sample_container::samples_iterator end = profile->end(symb);

	ostringstream str;
	for (; it != end; ++it) {
		counts_t c;

		for (size_t p = lo; p <= hi; ++p)  {
			size_t count = it->second.counts[p];

			if (count == 0) continue;

			str << open_element(DETAIL_DATA, true);
			str << init_attr(TABLE_ID, detail_index++);

			// first output the vma field
			field_datum datum(*symb, it->second, 0, c, 
					  extra_found_images, 0.0);
			output_attribute(str, datum, ff_vma, VMA);
			if (ff_linenr_info) {
				string sym_file;
				size_t sym_line;
				string samp_file;
				size_t samp_line;
				string sym_info = get_linenr_info(symb->sample.file_loc, true);
				string samp_info = get_linenr_info(it->second.file_loc, true);

				if (extract_linenr_info(samp_info, samp_file, samp_line)) {
					if (extract_linenr_info(sym_info, sym_file, sym_line)) {
						// only output source_file if it is different than the symbol's 
						// source file.  this can happen with inlined functions in
						// #included header files
						if (sym_file != samp_file)
							str << init_attr(SOURCE_FILE, samp_file);
					}
					str << init_attr(SOURCE_LINE, samp_line);
				}
			}
			str << close_element(NONE, true);

			// output buffered sample data
			output_sample_data(str, it->second, p);

			str << close_element(DETAIL_DATA);
		}
	}
	return str.str();
}

void xml_formatter::
output_symbol(ostream & out,
	symbol_entry const * symb, size_t lo, size_t hi, bool is_module)
{
	ostringstream str;
	// pointless reference to is_module, remove insane compiler warning
	size_t indx = is_module ? 0 : 1;

	// output symbol's summary data for each profile class
	bool got_samples = false;

	for (size_t p = lo; p <= hi; ++p) {
		got_samples |= xml_support->output_summary_data(str,
		    symb->sample.counts, p);
	}

	if (!got_samples)
		return;

	if (cverb << vxml)
		out << "<!-- symbol_ref=" << symbol_names.name(symb->name) <<
			" -->" << endl;

	out << open_element(SYMBOL, true);

	string const name = symbol_names.name(symb->name);
	assert(name.size() > 0);
	
	string const image = get_image_name(symb->image_name,
		image_name_storage::int_filename, extra_found_images);
	string const qname = image + ":" + name;

	indx = xml_get_symbol_index(qname);

	out << init_attr(ID_REF, indx);

	if (need_details) {
		ostringstream details;
		symbol_details_t & sd = symbol_details[indx];
		size_t const detail_lo = sd.index;

		string detail_str = output_symbol_details(symb, sd.index, lo, hi);

		if (detail_str.size() > 0) {
			if (sd.id < 0)
				sd.id = indx;
			details << detail_str;
		}

		if (sd.index > detail_lo) {
			sd.details = sd.details + details.str();
			out << init_attr(DETAIL_LO, detail_lo);
			out << init_attr(DETAIL_HI, sd.index-1);
		}
	}
	out << close_element(NONE, true);
	// output summary
	out << str.str();
	out << close_element(SYMBOL);
}


void xml_formatter::
output_sample_data(ostream & out, sample_entry const & sample, size_t pclass)
{
	out << open_element(COUNT, true);
	out << init_attr(CLASS, classes.v[pclass].name);
	out << close_element(NONE, true);
	out << sample.counts[pclass];
	out << close_element(COUNT);
}


void xml_formatter::
output_attribute(ostream & out, field_datum const & datum,
                 format_flags fl, tag_t tag)
{
	field_description const & field(format_map[fl]);

	string str = (this->*field.formatter)(datum);

	if (!str.empty()) {
		if (fl == ff_linenr_info && (tag == SOURCE_LINE || tag == SOURCE_FILE)) {
			string file;
			size_t line;

			if (extract_linenr_info(str, file, line)) {
				if (tag == SOURCE_LINE)
					out << init_attr(tag, line);
				else
					out << init_attr(tag, file);
			}
		} else
			out << " " << init_attr(tag, str);
	}
}

xml_cg_formatter::
xml_cg_formatter(callgraph_container const & cg, symbol_collection & s,
		 string_filter const & sf)
	:
	xml_formatter(0, s, cg.extra_found_images, sf),
	callgraph(cg)
{
	counts.total = callgraph.samples_count();
}

void xml_cg_formatter::
output_symbol_core(ostream & out, cg_symbol::children const cg_symb,
       string const selfname, string const qname,
       size_t lo, size_t hi, bool is_module, tag_t tag)
{
	cg_symbol::children::const_iterator cit;
	cg_symbol::children::const_iterator cend = cg_symb.end();

	for (cit = cg_symb.begin(); cit != cend; ++cit) {
		string const & module = get_image_name((cit)->image_name,
			image_name_storage::int_filename, extra_found_images);
		bool self = false;
		ostringstream str;
		size_t indx;

		// output symbol's summary data for each profile class
		for (size_t p = lo; p <= hi; ++p)
			xml_support->output_summary_data(str, cit->sample.counts, p);

		if (cverb << vxml)
			out << "<!-- symbol_ref=" << symbol_names.name(cit->name) <<
				" -->" << endl;

		if (is_module) {
			out << open_element(MODULE, true);
			out << init_attr(NAME, module) << close_element(NONE, true);
		}

		out << open_element(SYMBOL, true);

		string const symname = symbol_names.name(cit->name);
		assert(symname.size() > 0);

		string const symqname = module + ":" + symname;

		// Find any self references and handle
		if ((symname == selfname) && (tag == CALLEES)) {
			self = true;
			indx = xml_get_symbol_index(qname);
		} else {
			indx = xml_get_symbol_index(symqname);
		}

		out << init_attr(ID_REF, indx);

		if (self)
			out << init_attr(SELFREF, "true");

		out << close_element(NONE, true);
		out << str.str();
		out << close_element(SYMBOL);

		if (is_module)
			out << close_element(MODULE);
	}
}


void xml_cg_formatter::
output_symbol(ostream & out,
	symbol_entry const * symb, size_t lo, size_t hi, bool is_module)
{
	cg_symbol const * cg_symb = dynamic_cast<cg_symbol const *>(symb);
	ostringstream str;
	size_t indx;

	// output symbol's summary data for each profile class
	for (size_t p = lo; p <= hi; ++p)
		xml_support->output_summary_data(str, symb->sample.counts, p);

	if (cverb << vxml)
		out << "<!-- symbol_ref=" << symbol_names.name(symb->name) <<
			" -->" << endl;

	out << open_element(SYMBOL, true);

	string const name = symbol_names.name(symb->name);
	assert(name.size() > 0);

	string const image = get_image_name(symb->image_name,
		image_name_storage::int_filename, extra_found_images);
	string const qname = image + ":" + name;

	string const selfname = symbol_names.demangle(symb->name) + " [self]";

	indx = xml_get_symbol_index(qname);

	out << init_attr(ID_REF, indx);

	out << close_element(NONE, true);

	out << open_element(CALLERS);
	if (cg_symb)
		output_symbol_core(out, cg_symb->callers, selfname, qname, lo, hi, is_module, CALLERS);
	out << close_element(CALLERS);

	out << open_element(CALLEES);
	if (cg_symb)
		output_symbol_core(out, cg_symb->callees, selfname, qname, lo, hi, is_module, CALLEES);

	out << close_element(CALLEES);

	// output summary
	out << str.str();
	out << close_element(SYMBOL);
}

} // namespace format_output
