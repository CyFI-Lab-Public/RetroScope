/**
 * @file profile_spec.h
 * Contains a PP profile specification
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef PROFILE_SPEC_H
#define PROFILE_SPEC_H

#include <map>
#include <vector>
#include <list>

#include "filename_spec.h"
#include "comma_list.h"
#include "locate_images.h"

/**
 * Holds a parsed profile spec composed of tag:value pairs, as given in
 * pp_interface documentation.
 *
 *  @internal implemented through a map of string, pointer to function member
 *  indexed by tag_name.
 */
class profile_spec
{
public:
	/**
	 * @param args  a vector of non options strings
	 * @param extra extra image paths to search
	 *
	 * Factory returning a profile_spec instance storing all valid
	 * tag:value contained in args vector doing also alias
	 * substitution, non-valid tag:value options are considered
	 * as image:value
	 */
	static profile_spec create(std::list<std::string> const & args,
	                           std::vector<std::string> const & image_path,
				   std::string const & root_path);

	/**
	 * @param exclude_dependent  whether to exclude dependent sub-images
	 * @param exclude_cg  whether to exclude call graph file
	 *
	 * Use the spec to generate the list of candidate sample files.
	 */
	std::list<std::string>
	generate_file_list(bool exclude_dependent, bool exclude_cg) const;

	/**
	 * @param file_spec  the filename specification to check
	 *
	 * return true if filename match the spec. PP:3.24 internal loop
	 */
	bool match(filename_spec const & file_spec) const;

	/**
	 * return archive name
	 * returns an empty string if not using an archive.
	 */
	std::string get_archive_path() const;

private:
	profile_spec();

	/**
	 * @param tag_value  a "tag:value" to interpret, all error throw an
	 * invalid_argument exception.
	 */
	void parse(std::string const & tag_value);

	/**
	 * @param image an image or a libray name given on command line
	 *
	 * Used for e.g. "opreport /bin/mybinary". We don't know yet
	 * if this is an application or a dependent image.
	 */
	void set_image_or_lib_name(std::string const & image);

	/**
	 * @param str  a "tag:value"
	 *
	 * return true if tag is a valid tag
	 */
	bool is_valid_tag(std::string const & str);

	/**
	 * implement tag parsing: PP:3.3 to 3.16
	 */
	void parse_archive_path(std::string const &);
	void parse_session(std::string const &);
	void parse_session_exclude(std::string const &);
	void parse_image(std::string const &);
	void parse_image_exclude(std::string const &);
	void parse_lib_image(std::string const &);
	void parse_event(std::string const &);
	void parse_count(std::string const &);
	void parse_unitmask(std::string const &);
	void parse_tid(std::string const &);
	void parse_tgid(std::string const &);
	void parse_cpu(std::string const &);

	typedef void (profile_spec::*action_t)(std::string const &);
	typedef std::map<std::string, action_t> parse_table_t;
	parse_table_t parse_table;

	/**
	 * @param tag_value  input "tag:value" string
	 * @param value  if success return the value part of tag_value
	 * helper for set/is_valid_tag public interface
	 *
	 * return null if tag is not valid, else return the pointer to member
	 * function to apply and the value in value parameter
	 */
	action_t get_handler(std::string const & tag_value,
			     std::string & value);

	std::string archive_path;
	std::string binary;
	std::vector<std::string> session;
	std::vector<std::string> session_exclude;
	std::vector<std::string> image;
	std::vector<std::string> image_exclude;
	std::vector<std::string> lib_image;
	comma_list<std::string> event;
	comma_list<int> count;
	comma_list<unsigned int> unitmask;
	comma_list<pid_t> tid;
	comma_list<pid_t> tgid;
	comma_list<int> cpu;
	// specified by user on command like opreport image1 image2 ...
	std::vector<std::string> image_or_lib_image;

public: // FIXME
	/// extra search path for images
	extra_images extra_found_images;
};

#endif /* !PROFILE_SPEC_H */
