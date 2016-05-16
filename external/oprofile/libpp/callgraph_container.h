/**
 * @file callgraph_container.h
 * Container associating symbols and caller/caller symbols
 *
 * @remark Copyright 2004 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef CALLGRAPH_CONTAINER_H
#define CALLGRAPH_CONTAINER_H

#include <set>
#include <vector>
#include <string>

#include "symbol.h"
#include "symbol_functors.h"
#include "string_filter.h"
#include "locate_images.h"

class profile_container;
class inverted_profile;
class profile_t;
class image_set;
class op_bfd;


/**
 * During building a callgraph_container we store all caller/callee
 * relationship in this container.
 *
 * An "arc" is simply a description of a call from one function to
 * another.
 */
class arc_recorder {
public:
	~arc_recorder() {}

	/**
	 * Add a symbol arc.
	 * @param caller  The calling symbol
	 * @param callee  The called symbol
	 * @param arc_count  profile data for the arcs
	 *
	 * If the callee is NULL, only the caller is added to the main
	 * list. This is used to initially populate the recorder with
	 * the symbols.
	 */
	void add(symbol_entry const & caller, symbol_entry const * callee,
	         count_array_t const & arc_count);

	/// return all the cg symbols
	symbol_collection const & get_symbols() const;

	/**
	 * After population, build the final output, and do
	 * thresholding.
	 */
	void process(count_array_t total, double threshold,
	             string_filter const & filter);

private:
	/**
	 * Internal structure used during collation. We use a map to
	 * allow quick lookup of children (we'll do this several times
	 * if we have more than one profile class). Each child maps from
	 * the symbol to the relevant arc data.
	 */
	struct cg_data {
		cg_data() {}

		typedef std::map<symbol_entry, count_array_t, less_symbol> children;

		/// callers of this symbol
		children callers;
		/// callees of this symbol
		children callees;
	};

	/**
	 * Sort and threshold callers and callees.
	 */
	void process_children(cg_symbol & sym, double threshold);

	typedef std::map<symbol_entry, cg_data, less_symbol> map_t;

	/// all the symbols (used during processing)
	map_t sym_map;

	/// symbol objects pointed to by pointers in vector cg_syms
	cg_collection_objs cg_syms_objs;

	/// final output data
	symbol_collection cg_syms;
};


/**
 * Store all callgraph information for the given profiles
 */
class callgraph_container {
public:
	/**
	 * Populate the container, must be called once only.
	 * @param iprofiles  sample file list including callgraph files.
	 * @param extra  extra image list to fixup binary name.
	 * @param debug_info  true if we must record linenr information
	 * @param threshold  ignore sample percent below this threshold
	 * @param merge_lib  merge library samples
	 * @param sym_filter  symbol filter
	 *
	 * Currently all errors core dump.
	 * FIXME: consider if this should be a ctor
	 */
	void populate(std::list<inverted_profile> const & iprofiles,
		      extra_images const & extra, bool debug_info,
		      double threshold, bool merge_lib,
		      string_filter const & sym_filter);

	/// return hint on how data must be displayed.
	column_flags output_hint() const;

	/// return the total number of samples.
	count_array_t samples_count() const;

	// return all the cg symbols
	symbol_collection const & get_symbols() const;

private:
	/**
	 * Record caller/callee for one cg file
	 * @param profile  one callgraph file stored in a profile_t
	 * @param caller_bfd  the caller bfd
	 * @param bfd_caller_ok  true if we succefully open the binary
	 * @param callee_bfd  the callee bfd
	 * @param app_name  the owning application
	 * @param pc  the profile_container holding all non cg samples.
	 * @param debug_info  record linenr debug information
	 * @param pclass  profile class nr
	 */
	void add(profile_t const & profile, op_bfd const & caller_bfd,
	         bool bfd_caller_ok, op_bfd const & callee_bfd,
		 std::string const & app_name, profile_container const & pc,
		 bool debug_info, size_t pclass);

	void populate(std::list<image_set> const & lset,
		      std::string const & app_image,
		      size_t pclass, profile_container const & pc,
		      bool debug_info, bool merge_lib);
	void populate(std::list<std::string> const & cg_files,
		      std::string const & app_image,
		      size_t pclass, profile_container const & pc,
		      bool debug_info, bool merge_lib);

	/// record all main symbols
	void add_symbols(profile_container const & pc);

	/// Cached value of samples count.
	count_array_t total_count;

	/// A structured representation of the callgraph.
	arc_recorder recorder;

public:  // FIXME
	extra_images extra_found_images;
};

#endif /* !CALLGRAPH_CONTAINER_H */
