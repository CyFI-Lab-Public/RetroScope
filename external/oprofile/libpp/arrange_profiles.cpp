/**
 * @file arrange_profiles.cpp
 * Classify and process a list of candidate sample files
 * into merged sets and classes.
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#include <algorithm>
#include <cstdlib>
#include <iostream>
#include <iterator>
#include <map>
#include <set>

#include "string_manip.h"
#include "op_header.h"
#include "op_exception.h"

#include "arrange_profiles.h"
#include "format_output.h"
#include "xml_utils.h"
#include "parse_filename.h"
#include "locate_images.h"

using namespace std;

namespace {

int numeric_compare(string const & lhs, string const & rhs)
{
	if (lhs == "all" && rhs == "all")
		return 0;
	// we choose an order arbitrarily
	if (lhs == "all")
		return 1;
	if (rhs == "all")
		return -1;
	unsigned int lhsval = op_lexical_cast<unsigned int>(lhs);
	unsigned int rhsval = op_lexical_cast<unsigned int>(rhs);
	if (lhsval == rhsval)
		return 0;
	if (lhsval < rhsval)
		return -1;
	return 1;
}


} // anonymous namespace


// global to fix some C++ obscure corner case.
bool operator<(profile_class const & lhs,
               profile_class const & rhs)
{
	profile_template const & lt = lhs.ptemplate;
	profile_template const & rt = rhs.ptemplate;
	int comp;

	// The profile classes are used to traverse the sample data
	// arrays.  We create XML elements for <process> and <thread>
	// that contain the sample data that can then be divided amongst
	// CPU, event, mask axes so it is more convenient to have the
	// process and thread classes be the outermost nesting level of
	// the sample data arrays
	if (!want_xml) {
		comp = numeric_compare(lt.cpu, rt.cpu);
		if (comp)
			return comp < 0;
	}

	comp = numeric_compare(lt.tgid, rt.tgid);
	if (comp)
		return comp < 0;

	comp = numeric_compare(lt.tid, rt.tid);
	if (comp)
		return comp < 0;

	comp = numeric_compare(lt.unitmask, rt.unitmask);
	if (comp)
		return comp < 0;

	if (want_xml) {
		if (lt.event != rt.event)
			return lt.event < rt.event;
		if (lt.count != rt.count)
			return lt.count < rt.count;

		return numeric_compare(lt.cpu, rt.cpu) < 0;
	} else {
		if (lt.event == rt.event)
			return lt.count < rt.count;
		return lt.event < rt.event;
	}
}

namespace {

struct axis_t {
	string name;
	string suggestion;
} axes[AXIS_MAX] = {
	{ "event", "specify event:, count: or unitmask: (see also --merge=unitmask)" },
	{ "tgid", "specify tgid: or --merge tgid" },
	{ "tid", "specify tid: or --merge tid" },
	{ "cpu", "specify cpu: or --merge cpu" },
};

} // anonymous namespace


bool profile_classes::matches(profile_classes const & classes)
{
	if (v.size() != classes.v.size())
		return false;

	axis_types const axis2 = classes.axis;

	switch (axis) {
		case AXIS_EVENT:
			break;
		case AXIS_TGID:
		case AXIS_TID:
			return axis2 == AXIS_TID || axis2 == AXIS_TGID;
		case AXIS_CPU:
			return axis2 == AXIS_CPU;
		case AXIS_MAX:
			return false;
	}

	// check that the events match (same event, count)

	vector<profile_class>::const_iterator it1 = v.begin();
	vector<profile_class>::const_iterator end1 = v.end();
	vector<profile_class>::const_iterator it2 = classes.v.begin();

	while (it1 != end1) {
		if (it1->ptemplate.event != it2->ptemplate.event)
			return false;
		if (it1->ptemplate.count != it2->ptemplate.count)
			return false;
		// differing unit mask is considered comparable
		++it1;
		++it2;
	}

	return true;
}

namespace {

typedef growable_vector<string> event_array_t;
typedef growable_vector<string>::size_type event_index_t;

bool new_event_index(string event, event_array_t & events, event_index_t & index)
{
	event_index_t sz = events.size();
	for (event_index_t i = 0; i != sz; ++i) {
		if (events[i] == event) {
			index = i;
			return false;
		}
	}

	index = sz;
	events[sz] = event;
	return true;
}


/// We have more than one axis of classification, tell the user.
void report_error(profile_classes const & classes, axis_types newaxis)
{
	string str = "Already displaying results for parameter ";
	str += axes[classes.axis].name;
	str += " with values:\n";
	vector<profile_class>::const_iterator it = classes.v.begin();
	vector<profile_class>::const_iterator const end = classes.v.end();

	// We show error for the first conflicting axis but on this
	// axis we can get only a few different it->name, we display only
	// these different name.
	set <string> name_seen;
	size_t i = 5;
	for (; it != end && i; ++it) {
		if (name_seen.find(it->name) == name_seen.end()) {
			name_seen.insert(it->name);
			str += it->name + ",";
			--i;
		}
	}

	if (!i) {
		str += " and ";
		str += op_lexical_cast<string>(classes.v.size() - 5);
		str += " more,";
	}

	str += "\nwhich conflicts with parameter ";
	str += axes[newaxis].name += ".\n";
	str += "Suggestion: ";
	str += axes[classes.axis].suggestion;
	throw op_fatal_error(str);
}


/**
 * check that two different axes are OK - this is only
 * allowed if they are TGID,TID and for each class,
 * tid == tgid
 */
bool allow_axes(profile_classes const & classes, axis_types newaxis)
{
	// No previous axis - OK
	if (classes.axis == AXIS_MAX)
		return true;

	if (classes.axis != AXIS_TID && classes.axis != AXIS_TGID)
		return false;

	if (newaxis != AXIS_TID && newaxis != AXIS_TGID)
		return false;

	vector<profile_class>::const_iterator it = classes.v.begin();
	vector<profile_class>::const_iterator const end = classes.v.end();

	for (; it != end; ++it) {
		if (it->ptemplate.tgid != it->ptemplate.tid)
			return false;
	}

	return true;
}


/// find the first sample file header in the class
opd_header const get_first_header(profile_class const & pclass)
{
	profile_set const & profile = *(pclass.profiles.begin());

	string file;

	// could be only one main app, with no samples for the main image
	if (profile.files.empty()) {
		profile_dep_set const & dep = *(profile.deps.begin());
		list<profile_sample_files> const & files = dep.files;
		profile_sample_files const & sample_files = *(files.begin());
		if (!sample_files.sample_filename.empty())
			file = sample_files.sample_filename;
		else
			file = *sample_files.cg_files.begin();
	} else {
		profile_sample_files const & sample_files 
			= *(profile.files.begin());
		if (!sample_files.sample_filename.empty())
			file = sample_files.sample_filename;
		else
			file = *sample_files.cg_files.begin();
	}

	return read_header(file);
}

/// merge sample file header in the profile_sample_files
void merge_header(profile_sample_files const & files, opd_header & header)
{
	if (!files.sample_filename.empty()) {
		opd_header const temp = read_header(files.sample_filename);
		header.ctr_um |=  temp.ctr_um;
	}

	list<string>::const_iterator it = files.cg_files.begin();
	list<string>::const_iterator const end = files.cg_files.end();
	for ( ; it != end; ++it) {
		opd_header const temp = read_header(*it);
		header.ctr_um |= temp.ctr_um;
	}
}

/// merge sample file header in the class
opd_header const get_header(profile_class const & pclass,
                            merge_option const & merge_by)
{
	opd_header header = get_first_header(pclass);

	if (!merge_by.unitmask)
		return header;

	profile_set const & profile = *(pclass.profiles.begin());

	typedef list<profile_sample_files>::const_iterator citerator;

	citerator it = profile.files.begin();
	citerator const end = profile.files.end();
	for ( ; it != end; ++it)
		merge_header(*it, header);

	list<profile_dep_set>::const_iterator dep_it = profile.deps.begin();
	list<profile_dep_set>::const_iterator dep_end = profile.deps.end();
	for ( ; dep_it != dep_end; ++dep_it) {
		citerator it = dep_it->files.begin();
		citerator const end = dep_it->files.end();
		for ( ; it != end; ++it)
			merge_header(*it, header);
	}

	return header;
}


/// Give human-readable names to each class.
void name_classes(profile_classes & classes, merge_option const & merge_by)
{
	opd_header header = get_header(classes.v[0], merge_by);

	classes.event = describe_header(header);
	classes.cpuinfo = describe_cpu(header);

	// If we're splitting on event anyway, clear out the
	// global event name
	if (classes.axis == AXIS_EVENT)
		classes.event.erase();

	vector<profile_class>::iterator it = classes.v.begin();
	vector<profile_class>::iterator const end = classes.v.end();

	for (; it != end; ++it) {
		it->name = axes[classes.axis].name + ":";
		switch (classes.axis) {
		case AXIS_EVENT:
			it->name = it->ptemplate.event
				+ ":" + it->ptemplate.count;
			header = get_header(*it, merge_by);
			it->longname = describe_header(header);
			break;
		case AXIS_TGID:
			it->name += it->ptemplate.tgid;
			it->longname = "Processes with a thread group ID of ";
			it->longname += it->ptemplate.tgid;
			break;
		case AXIS_TID:
			it->name += it->ptemplate.tid;
			it->longname = "Processes with a thread ID of ";
			it->longname += it->ptemplate.tid;
			break;
		case AXIS_CPU:
			it->name += it->ptemplate.cpu;
			it->longname = "Samples on CPU " + it->ptemplate.cpu;
			break;
		case AXIS_MAX:;
		}
	}
}


/**
 * Name and verify classes.
 */
void identify_classes(profile_classes & classes,
                      merge_option const & merge_by)
{
	profile_template & ptemplate = classes.v[0].ptemplate;
	bool changed[AXIS_MAX] = { false, };

	vector<profile_class>::iterator it = classes.v.begin();
	++it;
	vector<profile_class>::iterator end = classes.v.end();

	// only one class, name it after the event
	if (it == end)
		changed[AXIS_EVENT] = true;

	for (; it != end; ++it) {
		if (it->ptemplate.event != ptemplate.event
		    ||  it->ptemplate.count != ptemplate.count
		    // unit mask are mergeable
		    || (!merge_by.unitmask
			&& it->ptemplate.unitmask != ptemplate.unitmask))
			changed[AXIS_EVENT] = true;

		// we need the merge checks here because each
		// template is filled in from the first non
		// matching profile, so just because they differ
		// doesn't mean it's the axis we care about
		if (!merge_by.tgid && it->ptemplate.tgid != ptemplate.tgid)
			changed[AXIS_TGID] = true;

		if (!merge_by.tid && it->ptemplate.tid != ptemplate.tid)
			changed[AXIS_TID] = true;

		if (!merge_by.cpu && it->ptemplate.cpu != ptemplate.cpu)
			changed[AXIS_CPU] = true;
	}

	classes.axis = AXIS_MAX;

	for (size_t i = 0; i < AXIS_MAX; ++i) {
		if (!changed[i])
			continue;

		if (!allow_axes(classes, axis_types(i)))
			report_error(classes, axis_types(i));
		classes.axis = axis_types(i);
		/* do this early for report_error */
		name_classes(classes, merge_by);
	}

	if (classes.axis == AXIS_MAX) {
		cerr << "Internal error - no equivalence class axis" << endl;
		abort();
	}
}

void identify_xml_classes(profile_classes & classes, merge_option const & merge_by)
{
	opd_header header = get_header(classes.v[0], merge_by);

	vector<profile_class>::iterator it = classes.v.begin();
	vector<profile_class>::iterator end = classes.v.end();

	event_index_t event_num;
	event_index_t event_max = 0;
	event_array_t event_array;
	size_t nr_cpus = 0;
	bool has_nonzero_mask = false;

	ostringstream event_setup;

	// fill in XML identifying each event, and replace event name by event_num
	for (; it != end; ++it) {
		string mask = it->ptemplate.unitmask;
		if (mask.find_first_of("x123456789abcdefABCDEF") != string::npos)
			has_nonzero_mask = true;
		if (new_event_index(it->ptemplate.event, event_array, event_num)) {
			// replace it->ptemplate.event with the event_num string
			// this is the first time we've seen this event
			header = get_header(*it, merge_by);
			event_setup << describe_header(header);
			event_max = event_num;
		}
		if (it->ptemplate.cpu != "all") {
			size_t cpu = atoi(it->ptemplate.cpu.c_str());
			if (cpu > nr_cpus) nr_cpus = cpu;
		}

		ostringstream str;
		str << event_num;
		it->ptemplate.event = str.str();
	}
	xml_utils::set_nr_cpus(++nr_cpus);
	xml_utils::set_nr_events(event_max+1);
	if (has_nonzero_mask)
		xml_utils::set_has_nonzero_masks();
	classes.event = event_setup.str();
	classes.cpuinfo = describe_cpu(header);
}

/// construct a class template from a profile
profile_template const
template_from_profile(parsed_filename const & parsed,
                      merge_option const & merge_by)
{
	profile_template ptemplate;

	ptemplate.event = parsed.event;
	ptemplate.count = parsed.count;

	if (!merge_by.unitmask)
		ptemplate.unitmask = parsed.unitmask;
	if (!merge_by.tgid)
		ptemplate.tgid = parsed.tgid;
	if (!merge_by.tid)
		ptemplate.tid = parsed.tid;
	if (!merge_by.cpu)
		ptemplate.cpu = parsed.cpu;
	return ptemplate;
}


/**
 * Find a matching class the sample file could go in, or generate
 * a new class if needed.
 * This is the heart of the merging and classification process.
 * The returned value is non-const reference but the ptemplate member
 * must be considered as const
 */
profile_class & find_class(set<profile_class> & classes,
                           parsed_filename const & parsed,
                           merge_option const & merge_by)
{
	profile_class cls;
	cls.ptemplate = template_from_profile(parsed, merge_by);

	pair<set<profile_class>::iterator, bool> ret = classes.insert(cls);

	return const_cast<profile_class &>(*ret.first);
}

/**
 * Sanity check : we can't overwrite sample_filename, if we abort here it means
 * we fail to detect that parsed sample filename for two distinct samples
 * filename must go in two distinct profile_sample_files. This assumption is
 * false for callgraph samples files so this function is only called for non cg
 * files.
 */
void sanitize_profile_sample_files(profile_sample_files const & sample_files,
    parsed_filename const & parsed)
{
	// We can't allow to overwrite sample_filename.
	if (!sample_files.sample_filename.empty()) {
		ostringstream out;
		out << "sanitize_profile_sample_files(): sample file "
		    << "parsed twice ?\nsample_filename:\n"
		    << sample_files.sample_filename << endl
		    << parsed << endl;
		throw op_fatal_error(out.str());
	}
}


/**
 * Add a sample filename (either cg or non cg files) to this profile.
 */
void
add_to_profile_sample_files(profile_sample_files & sample_files,
    parsed_filename const & parsed)
{
	if (parsed.cg_image.empty()) {
		// We can't allow to overwrite sample_filename.
		sanitize_profile_sample_files(sample_files, parsed);

		sample_files.sample_filename = parsed.filename;
	} else {
		sample_files.cg_files.push_back(parsed.filename);
	}
}


/**
 * we need to fix cg filename: a callgraph filename can occur before the binary
 * non callgraph samples filename occur so we must search.
 */
profile_sample_files &
find_profile_sample_files(list<profile_sample_files> & files,
			  parsed_filename const & parsed,
			  extra_images const & extra)
{
	list<profile_sample_files>::iterator it;
	list<profile_sample_files>::iterator const end = files.end();
	for (it = files.begin(); it != end; ++it) {
		if (!it->sample_filename.empty()) {
			parsed_filename psample_filename =
			  parse_filename(it->sample_filename, extra);
			if (psample_filename.lib_image == parsed.lib_image &&
			    psample_filename.image == parsed.image &&
			    psample_filename.profile_spec_equal(parsed))
				return *it;
		}

		list<string>::const_iterator cit;
		list<string>::const_iterator const cend = it->cg_files.end();
		for (cit = it->cg_files.begin(); cit != cend; ++cit) {
			parsed_filename pcg_filename =
				parse_filename(*cit, extra);
			if (pcg_filename.lib_image == parsed.lib_image &&
			    pcg_filename.image == parsed.image &&
			    pcg_filename.profile_spec_equal(parsed))
				return *it;
		}
	}

	// not found, create a new one
	files.push_back(profile_sample_files());
	return files.back();
}


/**
 * Add a profile to particular profile set. If the new profile is
 * a dependent image, it gets added to the dep list, or just placed
 * on the normal list of profiles otherwise.
 */
void
add_to_profile_set(profile_set & set, parsed_filename const & parsed,
		   bool merge_by_lib, extra_images const & extra)
{
	if (parsed.image == parsed.lib_image && !merge_by_lib) {
		profile_sample_files & sample_files =
			find_profile_sample_files(set.files, parsed, extra);
		add_to_profile_sample_files(sample_files, parsed);
		return;
	}

	list<profile_dep_set>::iterator it = set.deps.begin();
	list<profile_dep_set>::iterator const end = set.deps.end();

	for (; it != end; ++it) {
		if (it->lib_image == parsed.lib_image && !merge_by_lib &&
				parsed.jit_dumpfile_exists == false) {
			profile_sample_files & sample_files =
				find_profile_sample_files(it->files, parsed,
							  extra);
			add_to_profile_sample_files(sample_files, parsed);
			return;
		}
	}

	profile_dep_set depset;
	depset.lib_image = parsed.lib_image;
	profile_sample_files & sample_files =
		find_profile_sample_files(depset.files, parsed, extra);
	add_to_profile_sample_files(sample_files, parsed);
	set.deps.push_back(depset);
}


/**
 * Add a profile to a particular equivalence class. The previous matching
 * will have ensured the profile "fits", so now it's just a matter of
 * finding which sample file list it needs to go on.
 */
void add_profile(profile_class & pclass, parsed_filename const & parsed,
		 bool merge_by_lib, extra_images const & extra)
{
	list<profile_set>::iterator it = pclass.profiles.begin();
	list<profile_set>::iterator const end = pclass.profiles.end();

	for (; it != end; ++it) {
		if (it->image == parsed.image) {
			add_to_profile_set(*it, parsed, merge_by_lib, extra);
			return;
		}
	}

	profile_set set;
	set.image = parsed.image;
	add_to_profile_set(set, parsed, merge_by_lib, extra);
	pclass.profiles.push_back(set);
}

}  // anon namespace


profile_classes const
arrange_profiles(list<string> const & files, merge_option const & merge_by,
		 extra_images const & extra)
{
	set<profile_class> temp_classes;

	list<string>::const_iterator it = files.begin();
	list<string>::const_iterator const end = files.end();

	for (; it != end; ++it) {
		parsed_filename parsed = parse_filename(*it, extra);

		if (parsed.lib_image.empty())
			parsed.lib_image = parsed.image;

		// This simplifies the add of the profile later,
		// if we're lib-merging, then the app_image cannot
		// matter. After this, any non-dependent has
		// image == lib_image
		if (merge_by.lib)
			parsed.image = parsed.lib_image;

		profile_class & pclass =
			find_class(temp_classes, parsed, merge_by);
		add_profile(pclass, parsed, merge_by.lib, extra);
	}

	profile_classes classes;
	copy(temp_classes.begin(), temp_classes.end(),
	     back_inserter(classes.v));

	if (classes.v.empty())
		return classes;

	// sort by template for nicely ordered columns
	stable_sort(classes.v.begin(), classes.v.end());

	if (want_xml)
		identify_xml_classes(classes, merge_by);
	else
		identify_classes(classes, merge_by);

	classes.extra_found_images = extra;

	return classes;
}


ostream & operator<<(ostream & out, profile_sample_files const & sample_files)
{
	out << "sample_filename: " << sample_files.sample_filename << endl;
	out << "callgraph filenames:\n";
	copy(sample_files.cg_files.begin(), sample_files.cg_files.end(),
	     ostream_iterator<string>(out, "\n"));
	return out;
}

ostream & operator<<(ostream & out, profile_dep_set const & pdep_set)
{
	out << "lib_image: " << pdep_set.lib_image << endl;

	list<profile_sample_files>::const_iterator it;
	list<profile_sample_files>::const_iterator const end =
		pdep_set.files.end();
	size_t i = 0;
	for (it = pdep_set.files.begin(); it != end; ++it)
		out << "profile_sample_files #" << i++ << ":\n" << *it;

	return out;
}

ostream & operator<<(ostream & out, profile_set const & pset)
{
	out << "image: " << pset.image << endl;

	list<profile_sample_files>::const_iterator it;
	list<profile_sample_files>::const_iterator const end =
		pset.files.end();
	size_t i = 0;
	for (it = pset.files.begin(); it != end; ++it)
		out << "profile_sample_files #" << i++ << ":\n" << *it;

	list<profile_dep_set>::const_iterator cit;
	list<profile_dep_set>::const_iterator const cend = pset.deps.end();
	i = 0;
	for (cit = pset.deps.begin(); cit != cend; ++cit)
		out << "profile_dep_set #" << i++ << ":\n" << *cit;

	return out;
}

ostream & operator<<(ostream & out, profile_template const & ptemplate)
{
	out << "event: " << ptemplate.event << endl
	    << "count: " << ptemplate.count << endl
	    << "unitmask: " << ptemplate.unitmask << endl
	    << "tgid: " << ptemplate.tgid << endl
	    << "tid: " << ptemplate.tid << endl
	    << "cpu: " << ptemplate.cpu << endl;
	return out;
}

ostream & operator<<(ostream & out, profile_class const & pclass)
{
	out << "name: " << pclass.name << endl
	    << "longname: " << pclass.longname << endl
	    << "ptemplate:\n" << pclass.ptemplate;

	size_t i = 0;
	list<profile_set>::const_iterator it;
	list<profile_set>::const_iterator const end = pclass.profiles.end();
	for (it = pclass.profiles.begin(); it != end; ++it)
		out << "profiles_set #" << i++ << ":\n" << *it;

	return out;
}

ostream & operator<<(ostream & out, profile_classes const & pclasses)
{
	out << "event: " << pclasses.event << endl
	    << "cpuinfo: " << pclasses.cpuinfo << endl;

	for (size_t i = 0; i < pclasses.v.size(); ++i)
		out << "class #" << i << ":\n" << pclasses.v[i];

	return out;
}


namespace {

/// add the files to group of image sets
void add_to_group(image_group_set & group, string const & app_image,
                  list<profile_sample_files> const & files)
{
	image_set set;
	set.app_image = app_image;
	set.files = files;
	group.push_back(set);
}


typedef map<string, inverted_profile> app_map_t;


inverted_profile &
get_iprofile(app_map_t & app_map, string const & image, size_t nr_classes)
{
	app_map_t::iterator ait = app_map.find(image);
	if (ait != app_map.end())
		return ait->second;

	inverted_profile ip;
	ip.image = image;
	ip.groups.resize(nr_classes);
	app_map[image] = ip;
	return app_map[image];
}


/// Pull out all the images, removing any we can't access.
void
verify_and_fill(app_map_t & app_map, list<inverted_profile> & plist,
		extra_images const & extra)
{
	app_map_t::iterator it = app_map.begin();
	app_map_t::iterator const end = app_map.end();

	for (; it != end; ++it) {
		plist.push_back(it->second);
		inverted_profile & ip = plist.back();
		extra.find_image_path(ip.image, ip.error, false);
	}
}

} // anon namespace


list<inverted_profile> const
invert_profiles(profile_classes const & classes)
{
	app_map_t app_map;

	size_t nr_classes = classes.v.size();

	for (size_t i = 0; i < nr_classes; ++i) {
		list<profile_set>::const_iterator pit
			= classes.v[i].profiles.begin();
		list<profile_set>::const_iterator pend
			= classes.v[i].profiles.end();

		for (; pit != pend; ++pit) {
			// files can be empty if samples for a lib image
			// but none for the main image. Deal with it here
			// rather than later.
			if (pit->files.size()) {
				inverted_profile & ip = get_iprofile(app_map,
					pit->image, nr_classes);
				add_to_group(ip.groups[i], pit->image, pit->files);
			}

			list<profile_dep_set>::const_iterator dit
				= pit->deps.begin();
			list<profile_dep_set>::const_iterator const dend
				= pit->deps.end();

			for (;  dit != dend; ++dit) {
				inverted_profile & ip = get_iprofile(app_map,
					dit->lib_image, nr_classes);
				add_to_group(ip.groups[i], pit->image,
				             dit->files);
			}
		}
	}

	list<inverted_profile> inverted_list;

	verify_and_fill(app_map, inverted_list, classes.extra_found_images);

	return inverted_list;
}
