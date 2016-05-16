/**
 * @file locate_images.cpp
 * Command-line helper
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "file_manip.h"
#include "locate_images.h"
#include "string_manip.h"

#include <cerrno>
#include <iostream>
#include <sstream>
#include <cstdlib>

using namespace std;


int extra_images::suid;

extra_images::extra_images()
	:
	uid(++suid)
{
}


void extra_images::populate(vector<string> const & paths,
			    string const & prefix_path)
{
	vector<string>::const_iterator cit = paths.begin();
	vector<string>::const_iterator end = paths.end();
	for (; cit != end; ++cit) {
		string const path = op_realpath(prefix_path + *cit);
		list<string> file_list;
		create_file_list(file_list, path, "*", true);
		list<string>::const_iterator lit = file_list.begin();
		list<string>::const_iterator lend = file_list.end();
		for (; lit != lend; ++lit) {
			value_type v(op_basename(*lit), op_dirname(*lit));
			images.insert(v);
		}
	}
}


void extra_images::populate(vector<string> const & paths,
			    string const & archive_path_,
			    string const & root_path_)
{
	archive_path = archive_path_;
	if (!archive_path.empty())
		archive_path = op_realpath(archive_path);

	root_path = op_realpath(root_path_);
	if (!root_path.empty())
		root_path = op_realpath(root_path);

	if (root_path.empty() && archive_path.empty())
		populate(paths, "");
	if (!archive_path.empty())
		populate(paths, archive_path);
	if (!root_path.empty() && root_path != archive_path)
		populate(paths, root_path);
}


vector<string> const extra_images::find(string const & name) const
{
	extra_images::matcher match(name);
	return find(match);
}


vector<string> const
extra_images::find(extra_images::matcher const & match) const
{
	vector<string> matches;

	const_iterator cit = images.begin();
	const_iterator end = images.end();

	for (; cit != end; ++cit) {
		if (match(cit->first))
			matches.push_back(cit->second + '/' + cit->first);
	}

	return matches;
}


namespace {

/**
 * Function object for matching a module filename, which
 * has its own special mangling rules in 2.6 kernels.
 */
struct module_matcher : public extra_images::matcher {
public:
	explicit module_matcher(string const & s)
		: extra_images::matcher(s) {}

	virtual bool operator()(string const & candidate) const {
		if (candidate.length() != value.length())
			return false;

		for (string::size_type i = 0 ; i < value.length() ; ++i) {
			if (value[i] == candidate[i])
				continue;
			if (value[i] == '_' &&
				(candidate[i] == ',' || candidate[i] == '-'))
				continue;
			return false;
		}

		return true;
	}
};

} // anon namespace

string const extra_images::locate_image(string const & image_name,
			   image_error & error, bool fixup) const
{
	// Skip search since root_path can be non empty and we want
	// to lookup only in root_path in this case.
	if (!archive_path.empty()) {
		string image = op_realpath(archive_path + image_name);
		if (op_file_readable(image)) {
			error = image_ok;
			return fixup ? image : image_name;
		}

		if (errno == EACCES) {
			error = image_unreadable;
			return image_name;
		}
	}

	// We catch a case where root_path.empty() since we skipped a
	// search in "/" above when archive_path is empty. The case where
	// root_path.empty() && archive_path.empty() is the normal one, none
	// of --root or archive: as been given on command line.
	if (!root_path.empty() || archive_path.empty()) {
		string image = op_realpath(root_path + image_name);
		if (op_file_readable(image)) {
			error = image_ok;
			return fixup ? image : image_name;
		}
	}

	error = image_not_found;
	return image_name;
}

string const extra_images::find_image_path(string const & image_name,
	image_error & error, bool fixup) const
{
	error = image_ok;

	string const image = locate_image(image_name, error, fixup);
	if (error != image_not_found)
		return image;

	string const base = op_basename(image);

	vector<string> result = find(base);

	// not found, try a module search
	if (result.empty())
		result = find(module_matcher(base + ".ko"));

	if (result.empty()) {
		error = image_not_found;
		return image_name;
	}

	if (result.size() == 1) {
		error = image_ok;
		return fixup ? result[0] : image_name;
	}

#ifdef ANDROID
	// On Android, we often have both stripped and unstripped versions of the same
	// library in the image path.  Choose the first one found instead of issuing a
	// multiple match error.
	error = image_ok;
	return fixup ? result[0] : image_name;
#else
	// We can't get multiple result except if only one result is prefixed
	// by archive_path or by root_path.
	size_t count = 0;
	size_t index = 0;
	for (size_t i = 0; i < result.size() && count < 2; ++i) {
		if (is_prefix(result[i], archive_path)) {
			index = i;
			++count;
		}
	}

	if (count == 0) {
		for (size_t i = 0; i < result.size() && count < 2; ++i) {
			if (is_prefix(result[i], root_path)) {
				index = i;
				++count;
			}
		}
	}

	if (count == 1) {
		error = image_ok;
		return fixup ? result[index] : image_name;
	}

	error = image_multiple_match;
	return image_name;
#endif
}


string extra_images::strip_path_prefix(string const & image) const
{
	if (archive_path.length() && is_prefix(image, archive_path))
		return image.substr(archive_path.size());
	if (root_path.length() && is_prefix(image, root_path))
		return image.substr(root_path.size());
	return image;
}
