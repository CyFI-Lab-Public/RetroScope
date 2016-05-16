/**
 * @file xml_utils.h
 * utility routines for generating XML
 *
 * @remark Copyright 2006 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#ifndef XML_UTILS_H
#define XML_UTILS_H

#include "symbol.h"
#include "format_output.h"
#include "xml_output.h"

typedef symbol_collection::const_iterator sym_iterator;
extern bool want_xml;

class extra_images;
class op_bfd;

class xml_utils {
public:
	xml_utils(format_output::xml_formatter * xo,
		symbol_collection const & s, size_t nc,
		extra_images const & extra);
	// these members are static because they are invoked before
	// the xml_utils object has been created
	static std::string get_timer_setup(size_t count);
	static std::string get_event_setup(std::string event, size_t count,
					   std::string unit_mask);
	static std::string get_profile_header(std::string cpu_name,
	                                      double const speed);
	static void set_nr_cpus(size_t cpus);
	static void set_nr_events(size_t events);
	static void set_has_nonzero_masks();
	static void add_option(tag_t tag, bool value);
	static void add_option(tag_t tag, std::string const & value);
	static void add_option(tag_t tag, std::vector<std::string> const & value);
	static void add_option(tag_t tag, std::list<std::string> const & value);

	static void output_xml_header(std::string const & command_options,
						   std::string const & cpu_info,
						   std::string const & events);
	void output_symbol_bytes(std::ostream & out, symbol_entry const * symb,
	                         size_t sym_id, op_bfd const & abfd);
	bool output_summary_data(std::ostream & out, count_array_t const & summary,
							 size_t pclass);
	size_t get_symbol_index(sym_iterator const it);
	void output_program_structure(std::ostream & out);
	void build_subclasses(std::ostream & out);
private:
	bool multiple_events;
	bool has_subclasses;
	size_t bytes_index;
	extra_images const & extra_found_images;
	static bool has_nonzero_masks;
	static size_t events_index;
};

extern xml_utils * xml_support;

#endif /* !XML_UTILS_H */
