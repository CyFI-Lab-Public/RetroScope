/**
 * @file common_option.cpp
 * Contains common options and implementation of entry point of pp tools
 * and some miscelleaneous functions
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <cstdlib>

#include <iostream>
#include <sstream>
#include <iterator>
#include <cstdlib>

#include "op_config.h"
#include "locate_images.h"
#include "op_exception.h"
#include "popt_options.h"
#include "cverb.h"
#include "common_option.h"
#include "file_manip.h"

using namespace std;

namespace options {
	double threshold = 0.0;
	string threshold_opt;
	string session_dir = OP_SESSION_DIR_DEFAULT;
	string command_options;
	vector<string> image_path;
	string root_path;
}

namespace {

vector<string> verbose_strings;

popt::option common_options_array[] = {
	popt::option(verbose_strings, "verbose", 'V',
		     // FIXME help string for verbose level
		     "verbose output", "all,debug,bfd,level1,sfile,stats,xml"),
	popt::option(options::session_dir, "session-dir", '\0',
		     "specify session path to hold samples database and session data (" OP_SESSION_DIR_DEFAULT ")", "path"),
	popt::option(options::image_path, "image-path", 'p',
		     "comma-separated path to search missing binaries", "path"),
	popt::option(options::root_path, "root", 'R',
		     "path to filesystem to search for missing binaries", "path"),
};


double handle_threshold(string threshold)
{
	double value = 0.0;

	if (threshold.length()) {
		istringstream ss(threshold);
		if (!(ss >> value)) {
			cerr << "illegal threshold value: " << threshold
			     << " allowed range: [0-100]" << endl;
			exit(EXIT_FAILURE);
		}

		if (value < 0.0 || value > 100.0) {
			cerr << "illegal threshold value: " << threshold
			     << " allowed range: [0-100]" << endl;
			exit(EXIT_FAILURE);
		}
	}

	cverb << vdebug << "threshold: " << value << endl;;

	return value;
}


vector<string> pre_parse_spec(vector<string> const & non_options)
{
	vector<string> result;

	for (size_t i = 0; i < non_options.size(); ++i) {
		if (non_options[i] == "{}") {
			result.push_back("{");
			result.push_back("}");
		} else {
			result.push_back(non_options[i]);
		}
	}

	return result;
}


options::spec const parse_spec(vector<string> non_options)
{
	bool in_first = false;
	bool in_second = false;
	bool first = false;
	bool second = false;
	options::spec pspec;

	non_options = pre_parse_spec(non_options);

	vector<string>::const_iterator it = non_options.begin();
	vector<string>::const_iterator end = non_options.end();

	for (; it != end; ++it) {
		if (*it == "{") {
			if (in_first || in_second || second)
				goto fail;
			if (first) {
				in_second = true;
				second = true;
			} else {
				in_first = true;
				first = true;
			}
			continue;
		} 

		if (*it == "}") {
			if (in_first) {
				in_first = false;
			} else if (in_second) {
				in_second = false;
			} else {
				goto fail;
			}
			continue;
		}

		if (in_first) {
			pspec.first.push_back(*it);
		} else if (in_second) {
			pspec.second.push_back(*it);
		} else {
			pspec.common.push_back(*it);
		}
	}

	if (in_first || in_second || (first && !second))
		goto fail;

	if (pspec.first.empty() && pspec.second.size())
		goto fail;

	if (first && second) {
		pspec.first.insert(pspec.first.begin(), pspec.common.begin(),
		                   pspec.common.end());
		pspec.second.insert(pspec.second.begin(), pspec.common.begin(),
		                   pspec.common.end());
	}

	return pspec;
fail:
	cerr << "invalid profile specification ";
	copy(non_options.begin(), non_options.end(),
	     ostream_iterator<string>(cerr, " "));
	cerr << endl;
	exit(EXIT_FAILURE);
}


options::spec get_options(int argc, char const * argv[])
{
	vector<string> non_options;
	popt::parse_options(argc, argv, non_options);

	// initialize paths in op_config.h
	init_op_config_dirs(options::session_dir.c_str());

	if (!options::threshold_opt.empty())
		options::threshold = handle_threshold(options::threshold_opt);

	if (!verbose::setup(verbose_strings)) {
		cerr << "unknown --verbose= options\n";
		exit(EXIT_FAILURE);
	}

	// XML generator needs command line options for its header
	ostringstream str;
	for (int i = 1; i < argc; ++i)
		str << argv[i] << " ";
	options::command_options = str.str();

	return parse_spec(non_options);
}

}  // anon namespace


int run_pp_tool(int argc, char const * argv[], pp_fct_run_t fct)
{
	try {
		return fct(get_options(argc, argv));
	}
	catch (op_runtime_error const & e) {
		cerr << argv[0] << " error: " << e.what() << endl;
	}
	catch (op_fatal_error const & e) {
		cerr << argv[0] << " error: " << e.what() << endl;
	}
	catch (op_exception const & e) {
		cerr << argv[0] << " error: " << e.what() << endl;
	}
	catch (invalid_argument const & e) {
		cerr << argv[0] << " error: " << e.what() << endl;
	}
	catch (exception const & e) {
		cerr << argv[0] << " error: " << e.what() << endl;
	}
	catch (...) {
		cerr << argv[0] << " unknown exception" << endl;
	}

	return EXIT_FAILURE;
}


demangle_type handle_demangle_option(string const & option)
{
	if (option == "none")
		return dmt_none;
	if (option == "smart")
		return dmt_smart;
	if (option == "normal")
		return dmt_normal;

	throw op_runtime_error("invalid option --demangle=" + option);
}


merge_option handle_merge_option(vector<string> const & mergespec,
    bool allow_lib, bool exclude_dependent)
{
	using namespace options;
	merge_option merge_by;

	merge_by.cpu = false;
	merge_by.lib = false;
	merge_by.tid = false;
	merge_by.tgid = false;
	merge_by.unitmask = false;

	if (!allow_lib)
		merge_by.lib = true;

	bool is_all = false;

	vector<string>::const_iterator cit = mergespec.begin();
	vector<string>::const_iterator end = mergespec.end();

	for (; cit != end; ++cit) {
		if (*cit == "cpu") {
			merge_by.cpu = true;
		} else if (*cit == "tid") {
			merge_by.tid = true;
		} else if (*cit == "tgid") {
			// PP:5.21 tgid merge imply tid merging.
			merge_by.tgid = true;
			merge_by.tid = true;
		} else if ((*cit == "lib" || *cit == "library") && allow_lib) {
			merge_by.lib = true;
		} else if (*cit == "unitmask") {
			merge_by.unitmask = true;
		} else if (*cit == "all") {
			merge_by.cpu = true;
			merge_by.lib = true;
			merge_by.tid = true;
			merge_by.tgid = true;
			merge_by.unitmask = true;
			is_all = true;
		} else {
			cerr << "unknown merge option: " << *cit << endl;
			exit(EXIT_FAILURE);
		}
	}

	// if --merge all, don't warn about lib merging,
	// it's not user friendly. Behaviour should still
	// be correct.
	if (exclude_dependent && merge_by.lib && allow_lib && !is_all) {
		cerr << "--merge=lib is meaningless "
		     << "with --exclude-dependent" << endl;
		exit(EXIT_FAILURE);
	}

	return merge_by;
}
