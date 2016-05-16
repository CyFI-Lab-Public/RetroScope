/**
 * @file symbol_sort.cpp
 * Sorting symbols
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "symbol_sort.h"
#include "symbol_functors.h"

#include "name_storage.h"
#include "op_exception.h"

#include <algorithm>
#include <sstream>

using namespace std;

namespace {

bool long_filenames;

int image_compare(image_name_id l, image_name_id r)
{
	if (long_filenames)
		return image_names.name(l).compare(image_names.name(r));
	return image_names.basename(l).compare(image_names.basename(r));
}


int debug_compare(debug_name_id l, debug_name_id r)
{
	if (long_filenames)
		return debug_names.name(l).compare(debug_names.name(r));
	return debug_names.basename(l).compare(debug_names.basename(r));
}


int compare_by(sort_options::sort_order order,
               symbol_entry const & lhs, symbol_entry const & rhs)
{
	switch (order) {
		case sort_options::sample:
			if (lhs.sample.counts[0] < rhs.sample.counts[0])
				return 1;
			if (lhs.sample.counts[0] > rhs.sample.counts[0])
				return -1;
			return 0;

		case sort_options::symbol:
			return symbol_names.demangle(lhs.name).compare(
				symbol_names.demangle(rhs.name));

		case sort_options::image:
			return image_compare(lhs.image_name, rhs.image_name);

		case sort_options::app_name:
			return image_compare(lhs.app_name, rhs.app_name);

		case sort_options::vma:
			if (lhs.sample.vma < rhs.sample.vma)
				return -1;
			if (lhs.sample.vma > rhs.sample.vma)
				return 1;
			return 0;

		case sort_options::debug: {
			file_location const & f1 = lhs.sample.file_loc;
			file_location const & f2 = rhs.sample.file_loc;
			int ret = debug_compare(f1.filename, f2.filename);
			if (ret == 0)
				ret = f1.linenr - f2.linenr;
			return ret;
		}

		default: {
			// static_cast<> to shut up g++ 2.91.66 which warn
			// about ambiguity between <<(int) and <<(long int)
			ostringstream os;
			os << "compare_by(): unknown sort option: "
			   << static_cast<int>(order) << endl;
			throw op_fatal_error(os.str());
		}
	}

	return 0;
}


struct symbol_compare {
	symbol_compare(vector<sort_options::sort_order> const & order,
	               bool reverse)
		: compare_order(order), reverse_sort(reverse) {}

	bool operator()(symbol_entry const * lhs,
			symbol_entry const * rhs) const {
		return operator()(*lhs, *rhs);
	}

	bool operator()(symbol_entry const & lhs,
			symbol_entry const & rhs) const;

protected:
	vector<sort_options::sort_order> const & compare_order;
	bool reverse_sort;
};


bool symbol_compare::operator()(symbol_entry const & lhs,
				symbol_entry const & rhs) const
{
	for (size_t i = 0; i < compare_order.size(); ++i) {
		int ret = compare_by(compare_order[i], lhs, rhs);

		if (reverse_sort)
			ret = -ret;
		if (ret != 0)
			return ret < 0;
	}
	return false;
}


} // anonymous namespace


void sort_options::
sort(symbol_collection & syms, bool reverse_sort, bool lf) const
{
	long_filenames = lf;

	vector<sort_order> sort_option(options);
	for (sort_order cur = first; cur != last; cur = sort_order(cur + 1)) {
		if (find(sort_option.begin(), sort_option.end(), cur) ==
		    sort_option.end())
			sort_option.push_back(cur);
	}

	stable_sort(syms.begin(), syms.end(),
	            symbol_compare(sort_option, reverse_sort));
}


void sort_options::
sort(diff_collection & syms, bool reverse_sort, bool lf) const
{
	long_filenames = lf;

	vector<sort_order> sort_option(options);
	for (sort_order cur = first; cur != last; cur = sort_order(cur + 1)) {
		if (find(sort_option.begin(), sort_option.end(), cur) ==
		    sort_option.end())
			sort_option.push_back(cur);
	}

	stable_sort(syms.begin(), syms.end(),
	            symbol_compare(sort_option, reverse_sort));
}


void sort_options::add_sort_option(string const & name)
{
	if (name == "vma") {
		options.push_back(vma);
	} else if (name == "sample") {
		options.push_back(sample);
	} else if (name == "symbol") {
		options.push_back(symbol);
	} else if (name == "debug") {
		options.push_back(debug);
	} else if (name == "image") {
		options.push_back(image);
	} else if (name == "app-name") {
		options.push_back(app_name);
	} else {
		ostringstream os;
		os << "unknown sort option: " << name << endl;
		throw op_fatal_error(os.str());
	}
}


void sort_options::add_sort_option(sort_options::sort_order order)
{
	options.push_back(order);
}
