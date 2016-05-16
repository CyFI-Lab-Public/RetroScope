/**
 * @file opreport.cpp
 * Implement opreport utility
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <iostream>
#include <iomanip>
#include <vector>
#include <algorithm>
#include <sstream>
#include <numeric>

#include "op_exception.h"
#include "stream_util.h"
#include "string_manip.h"
#include "file_manip.h"
#include "opreport_options.h"
#include "op_header.h"
#include "profile.h"
#include "populate.h"
#include "arrange_profiles.h"
#include "profile_container.h"
#include "callgraph_container.h"
#include "diff_container.h"
#include "symbol_sort.h"
#include "format_output.h"
#include "xml_utils.h"
#include "image_errors.h"

using namespace std;

namespace {

static size_t nr_classes;

/// storage for a merged file summary
struct summary {
	count_array_t counts;
	string lib_image;

	bool operator<(summary const & rhs) const {
		return options::reverse_sort
		    ? counts[0] < rhs.counts[0] : rhs.counts[0] < counts[0];
	}

	/// add a set of files to a summary
	count_type add_files(list<profile_sample_files> const & files,
	                     size_t pclass);
};


count_type summary::
add_files(list<profile_sample_files> const & files, size_t pclass)
{
	count_type subtotal = 0;

	list<profile_sample_files>::const_iterator it = files.begin();
	list<profile_sample_files>::const_iterator const end = files.end();

	for (; it != end; ++it) {
		count_type count = profile_t::sample_count(it->sample_filename);
		counts[pclass] += count;
		subtotal += count;

		if (!it->cg_files.empty()) {
			throw op_runtime_error("opreport.cpp::add_files(): "
			       "unxpected non empty cg file set");
		}
	}

	return subtotal;
}


/**
 * Summary of an application: a set of image summaries
 * for one application, i.e. an application image and all
 * dependent images such as libraries.
 */
struct app_summary {
	/// total count of us and all dependents
	count_array_t counts;
	/// the main image
	string image;
	/// our dependent images
	vector<summary> deps;

	/// construct and fill in the data
	count_type add_profile(profile_set const & profile, size_t pclass);

	bool operator<(app_summary const & rhs) const {
		return options::reverse_sort 
		    ? counts[0] < rhs.counts[0] : rhs.counts[0] < counts[0];
	}

private:
	/// find a matching summary (including main app summary)
	summary & find_summary(string const & image);
};


summary & app_summary::find_summary(string const & image)
{
	vector<summary>::iterator sit = deps.begin();
	vector<summary>::iterator const send = deps.end();
	for (; sit != send; ++sit) {
		if (sit->lib_image == image)
			return *sit;
	}

	summary summ;
	summ.lib_image = image;
	deps.push_back(summ);
	return deps.back();
}


count_type app_summary::add_profile(profile_set const & profile,
                                size_t pclass)
{
	count_type group_total = 0;

	// first the main image
	summary & summ = find_summary(profile.image);
	count_type app_count = summ.add_files(profile.files, pclass);
	counts[pclass] += app_count;
	group_total += app_count;

	// now all dependent images if any
	list<profile_dep_set>::const_iterator it = profile.deps.begin();
	list<profile_dep_set>::const_iterator const end = profile.deps.end();

	for (; it != end; ++it) {
		summary & summ = find_summary(it->lib_image);
		count_type lib_count = summ.add_files(it->files, pclass);
		counts[pclass] += lib_count;
		group_total += lib_count;
	}

	return group_total;
}


/// all the summaries
struct summary_container {
	summary_container(vector<profile_class> const & pclasses);

	/// all map summaries
	vector<app_summary> apps;
	/// total count of samples for all summaries
	count_array_t total_counts;
};


summary_container::
summary_container(vector<profile_class> const & pclasses)
{
	typedef map<string, app_summary> app_map_t;
	app_map_t app_map;

	for (size_t i = 0; i < pclasses.size(); ++i) {
		list<profile_set>::const_iterator it
			= pclasses[i].profiles.begin();
		list<profile_set>::const_iterator const end
			= pclasses[i].profiles.end();

		for (; it != end; ++it) {
			app_map_t::iterator ait = app_map.find(it->image);
			if (ait == app_map.end()) {
				app_summary app;
				app.image = it->image;
				total_counts[i] += app.add_profile(*it, i);
				app_map[app.image] = app;
			} else {
				total_counts[i]
					+= ait->second.add_profile(*it, i);
			}
		}
	}

	app_map_t::const_iterator it = app_map.begin();
	app_map_t::const_iterator const end = app_map.end();

	for (; it != end; ++it)
		apps.push_back(it->second);

	// sort by count
	stable_sort(apps.begin(), apps.end());
	vector<app_summary>::iterator ait = apps.begin();
	vector<app_summary>::iterator const aend = apps.end();
	for (; ait != aend; ++ait)
		stable_sort(ait->deps.begin(), ait->deps.end());
}


void output_header()
{
	if (!options::show_header)
		return;

	cout << classes.cpuinfo << endl;
	if (!classes.event.empty())
		cout << classes.event << endl;

	for (vector<profile_class>::size_type i = 0;
	     i < classes.v.size(); ++i) {
		cout << classes.v[i].longname << endl;
	}
}


string get_filename(string const & filename)
{
	return options::long_filenames ? filename : op_basename(filename);
}


/// Output a count and a percentage
void output_count(count_type total_count, count_type count)
{
	cout << setw(9) << count << ' ';
	double ratio = op_ratio(count, total_count);
	cout << format_percent(ratio * 100, percent_int_width,
			      percent_fract_width) << ' ';
}


void output_col_headers(bool indent)
{
	if (!options::show_header)
		return;

	if (indent)
		cout << '\t';

	size_t colwidth = 9 + 1 + percent_width;

	for (size_t i = 0; i < classes.v.size(); ++i) {
		string name = classes.v[i].name;
		if (name.length() > colwidth)
			name = name.substr(0, colwidth - 3)
				+ "...";
		io_state state(cout);
		// gcc 2.95 doesn't know right io manipulator
		cout.setf(ios::right, ios::adjustfield);
		// gcc 2.95 doesn't honor setw() for std::string
		cout << setw(colwidth) << name.c_str();
		cout << '|';
	}
	cout << '\n';

	if (indent)
		cout << '\t';

	for (size_t i = 0; i < classes.v.size(); ++i) {
		cout << "  samples| ";
		io_state state(cout);
		// gcc 2.95 doesn't know right io manipulator
		cout.setf(ios::right, ios::adjustfield);
		cout << setw(percent_width) << "%|";
	}

	cout << '\n';

	if (indent)
		cout << '\t';

	for (size_t i = 0; i < classes.v.size(); ++i) {
		cout << "-----------";
		string str(percent_width, '-');
		cout << str;
	}

	cout << '\n';
}


void
output_deps(summary_container const & summaries,
	    app_summary const & app)
{
	// the app summary itself is *always* present
	// (perhaps with zero counts) so this test
	// is correct
	if (app.deps.size() == 1)
		return;

	output_col_headers(true);

	for (size_t j = 0 ; j < app.deps.size(); ++j) {
		summary const & summ = app.deps[j];

		if (summ.counts.zero())
			continue;

		cout << '\t';

		for (size_t i = 0; i < nr_classes; ++i) {
			count_type tot_count = options::global_percent
				? summaries.total_counts[i] : app.counts[i];

			output_count(tot_count, summ.counts[i]);
		}

		cout << get_filename(summ.lib_image);
		cout << '\n';
	}
}


/**
 * Display all the given summary information
 */
void output_summaries(summary_container const & summaries)
{
	output_col_headers(false);

	for (size_t i = 0; i < summaries.apps.size(); ++i) {
		app_summary const & app = summaries.apps[i];

		if ((app.counts[0] * 100.0) / summaries.total_counts[0]
		    < options::threshold) {
			continue;
		}

		for (size_t j = 0; j < nr_classes; ++j)
			output_count(summaries.total_counts[j], app.counts[j]);

		cout << get_filename(app.image) << '\n';

		output_deps(summaries, app);
	}
}


format_flags get_format_flags(column_flags const & cf)
{
	format_flags flags(ff_none);
	flags = format_flags(flags | ff_nr_samples);
	flags = format_flags(flags | ff_percent | ff_symb_name);

	if (options::show_address)
		flags = format_flags(flags | ff_vma);

	if (options::debug_info)
		flags = format_flags(flags | ff_linenr_info);

	if (options::accumulated) {
		flags = format_flags(flags | ff_nr_samples_cumulated);
		flags = format_flags(flags | ff_percent_cumulated);
	}

	if (classes2.v.size())
		flags = format_flags(flags | ff_diff);

	if (cf & cf_image_name)
		flags = format_flags(flags | ff_image_name);

	return flags;
}


void output_symbols(profile_container const & pc, bool multiple_apps)
{
	profile_container::symbol_choice choice;
	choice.threshold = options::threshold;
	symbol_collection symbols = pc.select_symbols(choice);
	options::sort_by.sort(symbols, options::reverse_sort,
	                      options::long_filenames);
	format_output::formatter * out;
	format_output::xml_formatter * xml_out = 0;
	format_output::opreport_formatter * text_out = 0;

	if (options::xml) {
		xml_out = new format_output::xml_formatter(&pc, symbols,
			pc.extra_found_images, options::symbol_filter);
		xml_out->show_details(options::details);
		out = xml_out;
		// for XML always output long filenames
		out->show_long_filenames(true);
	} else {
		text_out = new format_output::opreport_formatter(pc);
		text_out->show_details(options::details);
		out = text_out;
		out->show_long_filenames(options::long_filenames);
	}

	out->set_nr_classes(nr_classes);
	out->show_header(options::show_header);
	out->vma_format_64bit(choice.hints & cf_64bit_vma);
	out->show_global_percent(options::global_percent);

	format_flags flags = get_format_flags(choice.hints);
	if (multiple_apps)
		flags = format_flags(flags | ff_app_name);

	out->add_format(flags);

	if (options::xml) {
		xml_support = new xml_utils(xml_out, symbols, nr_classes,
			pc.extra_found_images);
		xml_out->output(cout);
	} else {
		text_out->output(cout, symbols);
	}
}


void output_diff_symbols(profile_container const & pc1,
                         profile_container const & pc2, bool multiple_apps)
{
	diff_container dc(pc1, pc2);

	profile_container::symbol_choice choice;
	choice.threshold = options::threshold;

	diff_collection symbols = dc.get_symbols(choice);

	format_flags flags = get_format_flags(choice.hints);
	if (multiple_apps)
		flags = format_flags(flags | ff_app_name);

	// With diff profile we output only filename coming from the first
	// profile session, internally we use only name derived from the sample
	// filename so image name can match.
	format_output::diff_formatter out(dc, pc1.extra_found_images);

	out.set_nr_classes(nr_classes);
	out.show_long_filenames(options::long_filenames);
	out.show_header(options::show_header);
	out.show_global_percent(options::global_percent);
	out.vma_format_64bit(choice.hints & cf_64bit_vma);
	out.add_format(flags);

	options::sort_by.sort(symbols, options::reverse_sort,
	                      options::long_filenames);

	out.output(cout, symbols);
}


void output_cg_symbols(callgraph_container const & cg, bool multiple_apps)
{
	column_flags output_hints = cg.output_hint();

	symbol_collection symbols = cg.get_symbols();

	options::sort_by.sort(symbols, options::reverse_sort,
	                      options::long_filenames);

	format_output::formatter * out;
	format_output::xml_cg_formatter * xml_out = 0;
	format_output::cg_formatter * text_out = 0;

	if (options::xml) {
		xml_out = new format_output::xml_cg_formatter(cg, symbols,
			options::symbol_filter);
		xml_out->show_details(options::details);
		out = xml_out;
		// for XML always output long filenames
		out->show_long_filenames(true);
	} else {
		text_out = new format_output::cg_formatter(cg);
		out = text_out;
		out->show_long_filenames(options::long_filenames);
	}

	out->set_nr_classes(nr_classes);
	out->show_header(options::show_header);
	out->vma_format_64bit(output_hints & cf_64bit_vma);
	out->show_global_percent(options::global_percent);

	format_flags flags = get_format_flags(output_hints);
	if (multiple_apps)
		flags = format_flags(flags | ff_app_name);

	out->add_format(flags);

	if (options::xml) {
		xml_support = new xml_utils(xml_out, symbols, nr_classes,
			cg.extra_found_images);
		xml_out->output(cout);
	} else {
		text_out->output(cout, symbols);
	}

}


int opreport(options::spec const & spec)
{
	want_xml = options::xml;

	handle_options(spec);

	nr_classes = classes.v.size();

	if (!options::symbols && !options::xml) {
		summary_container summaries(classes.v);
		output_header();
		output_summaries(summaries);
		return 0;
	}

	bool multiple_apps = false;

	for (size_t i = 0; i < classes.v.size(); ++i) {
		if (classes.v[i].profiles.size() > 1)
			multiple_apps = true;
	}

	list<inverted_profile> iprofiles = invert_profiles(classes);

	report_image_errors(iprofiles, classes.extra_found_images);

	if (options::xml) {
		xml_utils::output_xml_header(options::command_options,
		                             classes.cpuinfo, classes.event);
	} else {
		output_header();
	}

	if (classes2.v.size()) {
		for (size_t i = 0; i < classes2.v.size(); ++i) {
			if (classes2.v[i].profiles.size() > 1)
				multiple_apps |= true;
		}

		profile_container pc1(options::debug_info, options::details,
				      classes.extra_found_images);

		list<inverted_profile>::iterator it = iprofiles.begin();
		list<inverted_profile>::iterator const end = iprofiles.end();

		for (; it != end; ++it)
			populate_for_image(pc1, *it,
					   options::symbol_filter, 0);

		list<inverted_profile> iprofiles2 = invert_profiles(classes2);

		report_image_errors(iprofiles2, classes2.extra_found_images);

		profile_container pc2(options::debug_info, options::details,
				      classes2.extra_found_images);

		list<inverted_profile>::iterator it2 = iprofiles2.begin();
		list<inverted_profile>::iterator const end2 = iprofiles2.end();

		for (; it2 != end2; ++it2)
			populate_for_image(pc2, *it2,
					   options::symbol_filter, 0);

		output_diff_symbols(pc1, pc2, multiple_apps);
	} else if (options::callgraph) {
		callgraph_container cg_container;
		cg_container.populate(iprofiles, classes.extra_found_images,
			options::debug_info, options::threshold,
			options::merge_by.lib, options::symbol_filter);

		output_cg_symbols(cg_container, multiple_apps);
	} else {
		profile_container samples(options::debug_info,
			options::details, classes.extra_found_images);

		list<inverted_profile>::iterator it = iprofiles.begin();
		list<inverted_profile>::iterator const end = iprofiles.end();

		for (; it != end; ++it)
			populate_for_image(samples, *it,
					   options::symbol_filter, 0);

		output_symbols(samples, multiple_apps);
	}

	return 0;
}

}  // anonymous namespace


int main(int argc, char const * argv[])
{
	cout.tie(0);
	return run_pp_tool(argc, argv, opreport);
}
