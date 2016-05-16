/**
 * @file arrange_profiles.h
 * Classify and process a list of candidate sample files
 * into merged sets and classes.
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef ARRANGE_PROFILES_H
#define ARRANGE_PROFILES_H

#include <string>
#include <list>
#include <vector>
#include <iosfwd>

#include "image_errors.h"
#include "locate_images.h"

/**
 * store merging options options used to classify profiles
 */
struct merge_option {
	bool cpu;
	bool lib;
	bool tid;
	bool tgid;
	bool unitmask;
};


/**
 * This describes which parameters are set for each
 * equivalence class.
 */
struct profile_template {
	std::string event;
	std::string count;
	std::string unitmask;
	std::string tgid;
	std::string tid;
	std::string cpu;
};


/**
 * A samples filename + its associated callgraph sample filename.
 */
struct profile_sample_files {
	/**
	 * This member can be empty since it is possible to get callgraph
	 * w/o any samples to the binary. e.g an application which defer all
	 * works to shared library but if arrange_profiles receive a sample
	 * file list filtered from cg file sample_filename can't be empty
	 */
	std::string sample_filename;
	/**
	 * List of callgraph sample filename. If the {dep} part of
	 * cg_filename != {cg} part it's a cross binary samples file.
	 */
	std::list<std::string> cg_files;
};


/**
 * A number of profiles files that are all dependent on
 * the same main (application) profile, for the same
 * dependent image.
 */
struct profile_dep_set {
	/// which dependent image is this set for
	std::string lib_image;

	/// the actual sample files optionnaly including callgraph sample files
	std::list<profile_sample_files> files;
};

/**
 * A number of profile files all for the same binary with the same
 * profile specification (after merging). Includes the set of dependent
 * profile files, if any.
 *
 * For example, we could have image == "/bin/bash", where files
 * contains all profiles against /bin/bash, and deps contains
 * the sample file list for /lib/libc.so, /lib/ld.so etc.
 */
struct profile_set {
	std::string image;

	/// the actual sample files for the main image and the asociated
	/// callgraph files
	std::list<profile_sample_files> files;

	/// all profile files dependent on the main image
	std::list<profile_dep_set> deps;
};


/**
 * A class collection of profiles. This is an equivalence class and
 * will correspond to columnar output of opreport.
 */
struct profile_class {
	std::list<profile_set> profiles;

	/// human-readable column name
	std::string name;

	/// human-readable long name
	std::string longname;

	/// merging matches against this
	profile_template ptemplate;
};


/**
 * The "axis" says what we've used to split the sample
 * files into the classes. Only one is allowed.
 */
enum axis_types {
	AXIS_EVENT,
	AXIS_TGID,
	AXIS_TID,
	AXIS_CPU,
	AXIS_MAX
};


struct profile_classes {
	/**
	 * This is only set if we're not classifying on event/count
	 * anyway - if we're classifying on event/count, then we'll
	 * already output the details of each class's event/count.
	 *
	 * It's only used when classifying by CPU, tgid etc. so the
	 * user can still see what perfctr event was used.
	 */
	std::string event;

	/// CPU info
	std::string cpuinfo;

	/// the actual classes
	std::vector<profile_class> v;

	/// the axis of the classes
	axis_types axis;

	/// the extra images to consider for this profile_classes
	extra_images extra_found_images;

	/// is this class set comparable with another?
	bool matches(profile_classes const & classes);
};


std::ostream & operator<<(std::ostream &, profile_sample_files const &);
std::ostream & operator<<(std::ostream &, profile_dep_set const &);
std::ostream & operator<<(std::ostream &, profile_set const &);
std::ostream & operator<<(std::ostream &, profile_template const &);
std::ostream & operator<<(std::ostream &, profile_class const &);
std::ostream & operator<<(std::ostream &, profile_classes const &);


/**
 * Take a list of sample filenames, and process them into a set of
 * classes containing profile_sets. Merging is done at this stage
 * as well as attaching dependent profiles to the main image.
 *
 * The classes correspond to the columns you'll get in opreport:
 * this can be a number of events, or different CPUs, etc.
 */
profile_classes const
arrange_profiles(std::list<std::string> const & files,
		 merge_option const & merge_by, extra_images const & extra);


/**
 * A set of sample files where the image binary to open
 * are all the same.
 */
struct image_set {
	/// this is main app image, *not* necessarily
	/// the one we need to open
	std::string app_image;

	/// the sample files
	std::list<profile_sample_files> files;
};

typedef std::list<image_set> image_group_set;

/**
 * All sample files where the binary image to open is
 * the same.
 *
 * This is the "inverse" to some degree of profile_set.
 * For example, here we might have image = "/lib/libc.so",
 * with groups being the profile classifications
 * tgid:404, tgid:301, etc.
 *
 * Within each group there's a number of image_sets.
 * All the sample files listed within the image_sets
 * are still for /lib/libc.so, but they may have
 * different app_image values, e.g. /bin/bash.
 * We need to keep track of the app_image values to
 * make opreport give the right info in the "app"
 * column.
 */
struct inverted_profile {
	inverted_profile() : error(image_ok) {}
	/// the image to open
	std::string image;

	/// an error found in reading the image
	mutable image_error error;

	/// all sample files with data for the above image
	std::vector<image_group_set> groups;
};


/**
 * Invert the profile set. For opreport -l, opannotate etc.,
 * processing the profile_classes directly is slow, because
 * we end up opening BFDs multiple times (for each class,
 * dependent images etc.). This function returns an inverted
 * set of sample files, where the primary sort is on the binary
 * image to open.
 *
 * Thus each element in the returned list is for exactly one
 * binary file that we're going to bfd_openr(). Attached to that
 * is the actual sample files we need to process for that binary
 * file. In order to get the output right, these have to be
 * marked with the profile class they're from (hence the groups
 * vector), and the app image that owned the sample file, if
 * applicable (hence image_set).
 */
std::list<inverted_profile> const
invert_profiles(profile_classes const & classes);

#endif /* !ARRANGE_PROFILES_H */
