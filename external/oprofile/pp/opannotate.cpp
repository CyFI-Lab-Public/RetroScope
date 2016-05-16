/**
 * @file opannotate.cpp
 * Implement opannotate utility
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <iostream>
#include <sstream>
#include <algorithm>
#include <iomanip>
#include <fstream>
#include <utility>

#include "op_exception.h"
#include "op_header.h"
#include "profile.h"
#include "populate.h"
#include "op_sample_file.h"
#include "cverb.h"
#include "string_manip.h"
#include "demangle_symbol.h"
#include "child_reader.h"
#include "op_file.h"
#include "file_manip.h"
#include "arrange_profiles.h"
#include "opannotate_options.h"
#include "profile_container.h"
#include "symbol_sort.h"
#include "image_errors.h"

using namespace std;
using namespace options;

namespace {

size_t nr_events;

scoped_ptr<profile_container> samples;

/// how opannotate was invoked
string cmdline;

/// empty annotation fill string
string annotation_fill;

/// string used as start / end comment to annotate source
string const begin_comment("/* ");
string const in_comment(" * ");
string const end_comment(" */");

/// field width for the sample count
unsigned int const count_width = 6;

string get_annotation_fill()
{
	string str;

	for (size_t i = 0; i < nr_events; ++i) {
		str += string(count_width, ' ') + ' ';
		str += string(percent_width, ' ');
	}

	for (size_t i = 1; i < nr_events; ++i)
		str += "  ";

	str += " :";
	return str;
}


symbol_entry const * find_symbol(string const & image_name,
				 string const & str_vma)
{
	// do not use the bfd equivalent:
	//  - it does not skip space at begin
	//  - we does not need cross architecture compile so the native
	// strtoull must work, assuming unsigned long long can contain a vma
	// and on 32/64 bits box bfd_vma is 64 bits
	bfd_vma vma = strtoull(str_vma.c_str(), NULL, 16);

	return samples->find_symbol(image_name, vma);
}


void output_info(ostream & out)
{
	out << begin_comment << '\n';

	out << in_comment << "Command line: " << cmdline << '\n'
	    << in_comment << '\n';

	out << in_comment << "Interpretation of command line:" << '\n';

	if (!assembly) {
		out << in_comment
		    << "Output annotated source file with samples" << '\n';

		if (options::threshold != 0) {
			out << in_comment
			    << "Output files where samples count reach "
			    << options::threshold << "% of the samples\n";
		} else {
			out << in_comment << "Output all files" << '\n';
		}
	} else {
		out << in_comment
		    << "Output annotated assembly listing with samples"
		    << '\n';

		if (!objdump_params.empty()) {
			out << in_comment << "Passing the following "
				"additional arguments to objdump ; \"";
			for (size_t i = 0 ; i < objdump_params.size() ; ++i)
				out << objdump_params[i] << " ";
			out << "\"" << '\n';
		}
	}

	out << in_comment << '\n';

	out << in_comment << classes.cpuinfo << endl;
	if (!classes.event.empty())
		out << in_comment << classes.event << endl;

	for (size_t i = 0; i < classes.v.size(); ++i)
		out << in_comment << classes.v[i].longname << endl;

	out << end_comment << '\n';
}


string count_str(count_array_t const & count,
		   count_array_t const & total)
{
	ostringstream os;
	for (size_t i = 0; i < nr_events; ++i) {
		os << setw(count_width) << count[i] << ' ';

		os << format_percent(op_ratio(count[i], total[i]) * 100.0,
				    percent_int_width, percent_fract_width);
	}
	return os.str();
}


/// NOTE: This function annotates a list<string> containing output from objdump.
/// It uses a list iterator, and a sample_container iterator which iterates
/// from the beginning to the end, and compare sample address
/// against the instruction address on the asm line.
///
/// There are 2 cases of annotation:
/// 1. If sample address matches current line address, annotate the current line.
/// 2. If (previous line address < sample address < current line address),
///    then we annotate previous line.  This case happens when sample address
///    is not aligned with the instruction address, which is seen when profile
///    using the instruction fetch mode of AMD Instruction-Based Sampling (IBS).
///
int asm_list_annotation(symbol_entry const * last_symbol,
			bfd_vma last_symbol_vma,
			list<string>::iterator sit,
			sample_container::samples_iterator & samp_it,
			list<string> & asm_lines)
{
	int ret = 0;

	sample_entry const * sample = NULL;

	if (samp_it != samples->end())
		sample = &samp_it->second;

	// do not use the bfd equivalent:
	//  - it does not skip space at begin
	//  - we does not need cross architecture compile so the native
	// strtoull must work, assuming unsigned long long can contain a vma
	// and on 32/64 bits box bfd_vma is 64 bits
	// gcc 2.91.66 workaround
	bfd_vma vma = strtoull((*sit).c_str(), NULL, 16);

	if (sample 
	    && ((sample->vma < last_symbol_vma) || (sample->vma > vma))) {
		*sit = annotation_fill + *sit;
	} else if (sample && sample->vma == vma) {
		// Case 1 : Sample address match current line address.
		string str = count_str(sample->counts, samples->samples_count());

		// For each events
		for (size_t i = 1; i < nr_events; ++i)
			str += "  ";

		*sit = str + " :" + *sit;
		if (samp_it != samples->end())
			++samp_it;

	} else	if (sample && sample->vma < vma) {
		// Case 2 : vma of the current line is greater than vma of the sample

		// Get the string of previous assembly line
		list<string>::iterator sit_prev = sit;
		string prev_line, prev_vma_str;
		string::size_type loc1 = string::npos, loc2 = string::npos;
		while (sit_prev != asm_lines.begin()) {
			--sit_prev;
			prev_line = *sit_prev;

			loc1 = prev_line.find(":", 0);
			if (loc1 != string::npos) {
				loc2 = prev_line.find(":", loc1+1);
				if (loc2 != string::npos) {
					prev_vma_str = prev_line.substr(loc1+1, loc2);
					break;
				}
			}
		}

		bfd_vma prev_vma = strtoull(prev_vma_str.c_str(), NULL, 16);

		// Need to check if prev_vma < sample->vma
		if (prev_vma != 0 && prev_vma < sample->vma) {
			string str;

			// Get sample for previous line.
			sample_entry * prev_sample = (sample_entry *)samples->
							find_sample(last_symbol, prev_vma);
			if (prev_sample) {
				// Aggregate sample with previous line if it already has samples
				prev_sample->counts += sample->counts;
				str = count_str(prev_sample->counts, samples->samples_count());
			} else {
				str = count_str(sample->counts, samples->samples_count());
			}

			// For each events
			for (size_t i = 1; i < nr_events; ++i)
				str += "  ";

			*sit_prev = str + " :" + prev_line.substr(loc1+1);
			if (samp_it != samples->end())
				++samp_it;
			ret = -1;
		} else {
			// Failed to annotate the previous line. Skip sample.
			*sit = annotation_fill + *sit;
			if (samp_it != samples->end())
				++samp_it;
		}
	} else {
		// In case sample is NULL
		*sit = annotation_fill + *sit;
	}

	return ret;
}


string symbol_annotation(symbol_entry const * symbol)
{
	if (!symbol)
		return string();

	string annot = count_str(symbol->sample.counts,
	                         samples->samples_count());

	string const & symname = symbol_names.demangle(symbol->name);

	string str = " ";
	str += begin_comment + symname + " total: ";
	str += count_str(symbol->sample.counts, samples->samples_count());
	str += end_comment;
	return str;
}


/// return true if  this line contains a symbol name in objdump formatting
/// symbol are on the form 08030434 <symbol_name>:  we need to be strict
/// here to avoid any interpretation of a source line as a symbol line
bool is_symbol_line(string const & str, string::size_type pos)
{
	if (str[pos] != ' ' || str[pos + 1] != '<')
		return false;

	return str[str.length() - 1] == ':';
}


void annotate_objdump_str_list(string const & app_name,
			       symbol_collection const & symbols,
			       list<string> & asm_lines)
{
	symbol_entry const * last_symbol = 0;
	bfd_vma last_symbol_vma = 0;
	int ret = 0;

	// to filter output of symbols (filter based on command line options)
	bool do_output = true;

	// We simultaneously walk the two structures (list and sample_container)
	// which are sorted by address. and do address comparision.
	list<string>::iterator sit  = asm_lines.begin();
	list<string>::iterator send = asm_lines.end();
	sample_container::samples_iterator samp_it = samples->begin();

	for (; sit != send; (!ret? sit++: sit)) {
		// output of objdump is a human readable form and can contain some
		// ambiguity so this code is dirty. It is also optimized a little bit
		// so it is difficult to simplify it without breaking something ...

		// line of interest are: "[:space:]*[:xdigit:]?[ :]", the last char of
		// this regexp dis-ambiguate between a symbol line and an asm line. If
		// source contain line of this form an ambiguity occur and we rely on
		// the robustness of this code.
		string str = *sit;
		size_t pos = 0;
		while (pos < str.length() && isspace(str[pos]))
			++pos;

		if (pos == str.length() || !isxdigit(str[pos])) {
			if (do_output) {
				*sit = annotation_fill + str;
				continue;
			}
		}

		while (pos < str.length() && isxdigit(str[pos]))
			++pos;

		if (pos == str.length() || (!isspace(str[pos]) && str[pos] != ':')) {
			if (do_output) {
				*sit = annotation_fill + str;
				continue;
			}
		}

		if (is_symbol_line(str, pos)) {

			last_symbol = find_symbol(app_name, str);
			last_symbol_vma = strtoull(str.c_str(), NULL, 16);

			// ! complexity: linear in number of symbol must use sorted
			// by address vector and lower_bound ?
			// Note this use a pointer comparison. It work because symbols
			// pointer are unique
			if (find(symbols.begin(), symbols.end(), last_symbol)
			    != symbols.end())
				do_output = true;
			else
				do_output = false;

			if (do_output) {
				*sit += symbol_annotation(last_symbol);

				// Realign the sample iterator to
				// the beginning of this symbols
				samp_it = samples->begin(last_symbol);
			}
		} else {
			// not a symbol, probably an asm line.
			if (do_output)
				ret = asm_list_annotation(last_symbol,
							  last_symbol_vma,
							  sit, samp_it,
							  asm_lines);
		}

		if (!do_output)
			*sit = "";
	}
}


void output_objdump_str_list(symbol_collection const & symbols,
			string const & app_name,
			list<string> & asm_lines)
{

	annotate_objdump_str_list(app_name, symbols, asm_lines);

	// Printing objdump output to stdout
	list<string>::iterator sit  = asm_lines.begin();
	list<string>::iterator send = asm_lines.end();
	sit = asm_lines.begin();
	for (; sit != send; ++sit) {
		string str = *sit;
		if (str.length() != 0)
			cout << str << '\n';
	}
}


void do_one_output_objdump(symbol_collection const & symbols,
			   string const & image_name, string const & app_name,
			   bfd_vma start, bfd_vma end)
{
	vector<string> args;
	list<string> asm_lines;

	args.push_back("-d");
	args.push_back("--no-show-raw-insn");
	if (source)
		args.push_back("-S");

	if (start || end != ~(bfd_vma)0) {
		ostringstream arg1, arg2;
		arg1 << "--start-address=" << start;
		arg2 << "--stop-address=" << end;
		args.push_back(arg1.str());
		args.push_back(arg2.str());
	}

	if (!objdump_params.empty()) {
		for (size_t i = 0 ; i < objdump_params.size() ; ++i)
			args.push_back(objdump_params[i]);
	}

	args.push_back(image_name);
#if defined(ANDROID_TARGET_ARM)
	child_reader reader("arm-eabi-objdump", args);
#elif defined(ANDROID_TARGET_MIPS)
	child_reader reader("mipsel-linux-android-objdump", args);
#else
	child_reader reader("objdump", args);
#endif
	if (reader.error()) {
		cerr << "An error occur during the execution of objdump:\n\n";
		cerr << reader.error_str() << endl;
		return;
	}

	// Read each output line from objdump and store in a list.
	string str;
	while (reader.getline(str))
		asm_lines.push_back(str);

	output_objdump_str_list(symbols, app_name, asm_lines);

	// objdump always returns SUCCESS so we must rely on the stderr state
	// of objdump. If objdump error message is cryptic our own error
	// message will be probably also cryptic
	ostringstream std_err;
	ostringstream std_out;
	reader.get_data(std_out, std_err);
	if (std_err.str().length()) {
		cerr << "An error occur during the execution of objdump:\n\n";
		cerr << std_err.str() << endl;
		return ;
	}

	// force error code to be acquired
	reader.terminate_process();

	// required because if objdump stop by signal all above things suceeed
	// (signal error message are not output through stdout/stderr)
	if (reader.error()) {
		cerr << "An error occur during the execution of objdump:\n\n";
		cerr << reader.error_str() << endl;
		return;
	}
}


void output_objdump_asm(symbol_collection const & symbols,
			string const & app_name)
{
	image_error error;
	string image =
		classes.extra_found_images.find_image_path(app_name, error,
							   true);

	// this is only an optimisation, we can either filter output by
	// directly calling objdump and rely on the symbol filtering or
	// we can call objdump with the right parameter to just disassemble
	// the needed part. This is a real win only when calling objdump
	// a medium number of times, I dunno if the used threshold is optimal
	// but it is a conservative value.
	size_t const max_objdump_exec = 50;
	if (symbols.size() <= max_objdump_exec || error != image_ok) {
		symbol_collection::const_iterator cit = symbols.begin();
		symbol_collection::const_iterator end = symbols.end();
		for (; cit != end; ++cit) {
			bfd_vma start = (*cit)->sample.vma;
			bfd_vma end  = start + (*cit)->size;
			do_one_output_objdump(symbols, image, app_name,
					      start, end);
		}
	} else {
		do_one_output_objdump(symbols, image,
				      app_name, 0, ~bfd_vma(0));
	}
}


bool output_asm(string const & app_name)
{
	profile_container::symbol_choice choice;
	choice.threshold = options::threshold;
	choice.image_name = app_name;
	choice.match_image = true;
	symbol_collection symbols = samples->select_symbols(choice);

	if (!symbols.empty()) {
		sort_options options;
		options.add_sort_option(sort_options::sample);
		options.sort(symbols, false, false);

		output_info(cout);

		output_objdump_asm(symbols, app_name);

		return true;
	}

	return false;
}


string const source_line_annotation(debug_name_id filename, size_t linenr)
{
	string str;

	count_array_t counts = samples->samples_count(filename, linenr);
	if (!counts.zero()) {
		str += count_str(counts, samples->samples_count());
		for (size_t i = 1; i < nr_events; ++i)
			str += "  ";
		str += " :";
	} else {
		str = annotation_fill;
	}

	return str;
}


string source_symbol_annotation(debug_name_id filename, size_t linenr)
{
	symbol_collection const symbols = samples->find_symbol(filename, linenr);

	if (symbols.empty())
		return string();

	string str = " " + begin_comment;

	count_array_t counts;
	for (size_t i = 0; i < symbols.size(); ++i) {
		str += symbol_names.demangle(symbols[i]->name);
		if (symbols.size() == 1)
			str += " total: ";
		else
			str += " ";
		str += count_str(symbols[i]->sample.counts,
		          samples->samples_count());
		if (symbols.size() != 1)
			str += ", ";

		counts += symbols[i]->sample.counts;
	}

	if (symbols.size() > 1)
		str += "total: " + count_str(counts, samples->samples_count());
	str += end_comment;

	return str;
}


void output_per_file_info(ostream & out, debug_name_id filename,
			  count_array_t const & total_file_count)
{
	out << begin_comment << '\n'
	     << in_comment << "Total samples for file : "
	     << '"' << debug_names.name(filename) << '"'
	     << '\n';
	out << in_comment << '\n' << in_comment
	    << count_str(total_file_count, samples->samples_count())
	    << '\n';
	out << end_comment << '\n' << '\n';
}


string const line0_info(debug_name_id filename)
{
	string annotation = source_line_annotation(filename, 0);
	if (trim(annotation, " \t:").empty())
		return string();

	string str = "<credited to line zero> ";
	str += annotation;
	return str;
}


void do_output_one_file(ostream & out, istream & in, debug_name_id filename,
                        bool header)
{
	count_array_t count = samples->samples_count(filename);

	if (header) {
		output_per_file_info(out, filename, count);
		out << line0_info(filename) << '\n';
	}


	if (in) {
		string str;

		for (size_t linenr = 1 ; getline(in, str) ; ++linenr) {
			out << source_line_annotation(filename, linenr) << str
			    << source_symbol_annotation(filename, linenr)
			    << '\n';
		}

	} else {
		// source is not available but we can at least output all the
		// symbols belonging to this file. This make more visible the
		// problem of having less samples for a given file than the
		// sum of all symbols samples for this file due to inlining
		symbol_collection const symbols = samples->select_symbols(filename);
		for (size_t i = 0; i < symbols.size(); ++i)
			out << symbol_annotation(symbols[i]) << endl;
	}

	if (!header) {
		output_per_file_info(out, filename, count);
		out << line0_info(filename) << '\n';
	}
}


void output_one_file(istream & in, debug_name_id filename,
                     string const & source)
{
	if (output_dir.empty()) {
		do_output_one_file(cout, in, filename, true);
		return;
	}

	string const out_file = op_realpath(output_dir + source);

	/* Just because you're paranoid doesn't mean they're not out to
	 * get you ...
	 *
	 * This is just a lame final safety check. If we found the
	 * source, then "source" should be canonical already, and
	 * can't escape from the output dir. We can't use op_realpath()
	 * alone as that needs the file to exist already.
	 *
	 * Let's not complain again if we couldn't find the file anyway.
	 */
	if (out_file.find("/../") != string::npos) {
		if (in) {
			cerr << "refusing to create non-canonical filename "
			     << out_file  << endl;
		}
		return;
	} else if (!is_prefix(out_file, output_dir)) {
		if (in) {
			cerr << "refusing to create file " << out_file
			     << " outside of output directory " << output_dir
			     << endl;
		}
		return;
	}

	if (is_files_identical(out_file, source)) {
		cerr << "input and output files are identical: "
		     << out_file << endl;
		return;
	}

	if (create_path(out_file.c_str())) {
		cerr << "unable to create file: "
		     << '"' << op_dirname(out_file) << '"' << endl;
		return;
	}

	ofstream out(out_file.c_str());
	if (!out) {
		cerr << "unable to open output file "
		     << '"' << out_file << '"' << endl;
	} else {
		do_output_one_file(out, in, filename, false);
		output_info(out);
	}
}


/* Locate a source file from debug info, which may be relative */
string const locate_source_file(debug_name_id filename_id)
{
	string const origfile = debug_names.name(filename_id);
	string file = origfile;

	if (file.empty())
		return file;

	/* Allow absolute paths to be relocated to a different directory */
	if (file[0] == '/') {
		vector<string>::const_iterator cit = base_dirs.begin();
		vector<string>::const_iterator end = base_dirs.end();
		for (; cit != end; ++cit) {
			string path = op_realpath(*cit);

			if (is_prefix(file, path)) {
				file = file.substr(path.length());
				break;
			}
		}
	}

	vector<string>::const_iterator cit = search_dirs.begin();
	vector<string>::const_iterator end = search_dirs.end();

	for (; cit != end; ++cit) {
		string const absfile = op_realpath(*cit + "/" + file);

		if (op_file_readable(absfile))
			return absfile;
	}

	/* We didn't find a relocated absolute file, or a relative file,
	 * assume the original is correct, accounting for the
	 * possibility it's relative the cwd
	 */
	return op_realpath(origfile);
}


void output_source(path_filter const & filter)
{
	bool const separate_file = !output_dir.empty();

	if (!separate_file)
		output_info(cout);

	vector<debug_name_id> filenames =
		samples->select_filename(options::threshold);

	for (size_t i = 0 ; i < filenames.size() ; ++i) {
		string const & source = locate_source_file(filenames[i]);

		if (!filter.match(source))
			continue;

		ifstream in(source.c_str());

		// it is common to have empty filename due to the lack
		// of debug info (eg _init function) so warn only
		// if the filename is non empty. The case: no debug
		// info at all has already been checked.
		if (!in && source.length()) {
			cerr << "opannotate (warning): unable to open for "
			     "reading: " << source << endl;
		}

		if (source.length())
			output_one_file(in, filenames[i], source);
	}
}


bool annotate_source(list<string> const & images)
{
	annotation_fill = get_annotation_fill();

	if (!output_dir.empty()) {

		if (create_path(output_dir.c_str())) {
			cerr << "unable to create " << output_dir
			     << " directory: " << endl;
			return false;
		}

		// Make sure we have an absolute path.
		output_dir = op_realpath(output_dir);
		if (output_dir.length() &&
		    output_dir[output_dir.length() - 1] != '/')
			output_dir += '/';

		/* Don't let the user stomp on their sources */
		if (output_dir == "/") {
			cerr << "Output path of / would over-write the "
				"source files" << endl;
			return false;
		}
	}

	if (assembly) {
		bool some_output = false;

		list<string>::const_iterator it = images.begin();
		list<string>::const_iterator const end = images.end();

		for (; it != end; ++it) {
			if (output_asm(*it))
				some_output = true;
		}

		if (!some_output) {
			// It's the only case we must care since we know the
			// selected image set is not empty
			cerr << "selected image set doesn't contain any of "
			     << "the selected symbol\n";
		}
	} else {
		output_source(file_filter);
	}

	return true;
}


int opannotate(options::spec const & spec)
{
	handle_options(spec);

	nr_events = classes.v.size();

	samples.reset(new profile_container(true, true,
					    classes.extra_found_images));

	list<string> images;

	list<inverted_profile> iprofiles = invert_profiles(classes);

	report_image_errors(iprofiles, classes.extra_found_images);

	list<inverted_profile>::iterator it = iprofiles.begin();
	list<inverted_profile>::iterator const end = iprofiles.end();

	bool debug_info = false;
	for (; it != end; ++it) {
		bool tmp = false;
		populate_for_image(*samples, *it,
				   options::symbol_filter, &tmp);
		images.push_back(it->image);
		if (tmp)
			debug_info = true;
	}

	if (!debug_info && !options::assembly) {
		cerr << "opannotate (warning): no debug information available for binary "
		     << it->image << ", and --assembly not requested\n";
	}

	annotate_source(images);

	return 0;
}

} // anonymous namespace


int main(int argc, char const * argv[])
{
	// set the invocation, for the file headers later
	for (int i = 0 ; i < argc ; ++i)
		cmdline += string(argv[i]) + " ";

	return run_pp_tool(argc, argv, opannotate);
}
