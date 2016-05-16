/**
 * @file format_output.h
 * outputting format for symbol lists
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef FORMAT_OUTPUT_H
#define FORMAT_OUTPUT_H

#include "config.h"

#include <string>
#include <map>
#include <iosfwd>

#include "format_flags.h"
#include "symbol.h"
#include "string_filter.h"
#include "xml_output.h"

class symbol_entry;
class sample_entry;
class callgraph_container;
class profile_container;
class diff_container;
class extra_images;
class op_bfd;

struct profile_classes;
// FIXME: should be passed to the derived class formatter ctor
extern profile_classes classes;

namespace format_output {

/// base class for formatter, handle common options to formatter
class formatter {
public:
	formatter(extra_images const & extra);
	virtual ~formatter();

	/// add a given column
	void add_format(format_flags flag);

	/// set the need_header boolean to false
	void show_header(bool);
	/// format for 64 bit wide VMAs
	void vma_format_64bit(bool);
	/// show long (full path) filenames
	void show_long_filenames(bool);
	/// use global count rather symbol count for details percent
	void show_global_percent(bool);

	/**
	 * Set the number of collected profile classes. Each class
	 * will output sample count and percentage in extra columns.
	 *
	 * This class assumes that the profile information has been
	 * populated with the right number of classes.
	 */
	void set_nr_classes(size_t nr_classes);

	/// output table header, implemented by calling the virtual function
	/// output_header_field()
	void output_header(std::ostream & out);

protected:
	struct counts_t {
		/// total sample count
		count_array_t total;
		/// samples so far
		count_array_t cumulated_samples;
		/// percentage so far
		count_array_t cumulated_percent;
		/// detailed percentage so far
		count_array_t cumulated_percent_details;
	};

	/// data passed for output
	struct field_datum {
		field_datum(symbol_entry const & sym,
		            sample_entry const & s,
			    size_t pc, counts_t & c,
			    extra_images const & extra, double d = 0.0)
			: symbol(sym), sample(s), pclass(pc),
			  counts(c), extra(extra), diff(d) {}
		symbol_entry const & symbol;
		sample_entry const & sample;
		size_t pclass;
		counts_t & counts;
		extra_images const & extra;
		double diff;
	};
 
	/// format callback type
	typedef std::string (formatter::*fct_format)(field_datum const &);
 
	/** @name format functions.
	 * The set of formatting functions, used internally by output().
	 */
	//@{
	std::string format_vma(field_datum const &);
	std::string format_symb_name(field_datum const &);
	std::string format_image_name(field_datum const &);
	std::string format_app_name(field_datum const &);
	std::string format_linenr_info(field_datum const &);
	std::string format_nr_samples(field_datum const &);
	std::string format_nr_cumulated_samples(field_datum const &);
	std::string format_percent(field_datum const &);
	std::string format_cumulated_percent(field_datum const &);
	std::string format_percent_details(field_datum const &);
	std::string format_cumulated_percent_details(field_datum const &);
	std::string format_diff(field_datum const &);
	//@}
 
	/// decribe one field of the colummned output.
	struct field_description {
		field_description() {}
		field_description(std::size_t w, std::string h,
				  fct_format f)
			: width(w), header_name(h), formatter(f) {}
 
		std::size_t width;
		std::string header_name;
		fct_format formatter;
	};
 
	typedef std::map<format_flags, field_description> format_map_t;

	/// actually do output
	void do_output(std::ostream & out, symbol_entry const & symbol,
		      sample_entry const & sample, counts_t & c,
	              diff_array_t const & = diff_array_t(),
	              bool hide_immutable_field = false);
 
	/// returns the nr of char needed to pad this field
	size_t output_header_field(std::ostream & out, format_flags fl,
	                           size_t padding);

	/// returns the nr of char needed to pad this field
	size_t output_field(std::ostream & out, field_datum const & datum,
			   format_flags fl, size_t padding,
			   bool hide_immutable);
 
	/// stores functors for doing actual formatting
	format_map_t format_map;

	/// number of profile classes
	size_t nr_classes;

	/// total counts
	counts_t counts;

	/// formatting flags set
	format_flags flags;
	/// true if we need to format as 64 bits quantities
	bool vma_64;
	/// false if we use basename(filename) in output rather filename
	bool long_filenames;
	/// true if we need to show header before the first output
	bool need_header;
	/// bool if details percentage are relative to total count rather to
	/// symbol count
	bool global_percent;

	/// To retrieve the real image location, usefull when acting on
	/// an archive and for 2.6 kernel modules
	extra_images const & extra_found_images;
};
 

/// class to output in a columned format symbols and associated samples
class opreport_formatter : public formatter {
public:
	/// build a ready to use formatter
	opreport_formatter(profile_container const & profile);

	/** output a vector of symbols to out according to the output format
	 * specifier previously set by call(s) to add_format() */
	void output(std::ostream & out, symbol_collection const & syms);

	/// set the output_details boolean
	void show_details(bool);

private:
 
	/** output one symbol symb to out according to the output format
	 * specifier previously set by call(s) to add_format() */
	void output(std::ostream & out, symbol_entry const * symb);

	/// output details for the symbol
	void output_details(std::ostream & out, symbol_entry const * symb);
 
	/// container we work from
	profile_container const & profile;
 
	/// true if we need to show details for each symbols
	bool need_details;
};


/// class to output in a columned format caller/callee and associated samples
class cg_formatter : public formatter {
public:
	/// build a ready to use formatter
	cg_formatter(callgraph_container const & profile);

	/** output callgraph information according to the previously format
	 * specifier set by call(s) to add_format() */
	void output(std::ostream & out, symbol_collection const & syms);
};

/// class to output a columned format symbols plus diff values
class diff_formatter : public formatter {
public:
	/// build a ready to use formatter
	diff_formatter(diff_container const & profile,
		       extra_images const & extra);

	/**
	 * Output a vector of symbols to out according to the output
	 * format specifier previously set by call(s) to add_format()
	 */
	void output(std::ostream & out, diff_collection const & syms);

private:
	/// output a single symbol
	void output(std::ostream & out, diff_symbol const & sym);

};


/// class to output in XML format
class xml_formatter : public formatter {
public:
	/// build a ready to use formatter
	xml_formatter(profile_container const * profile,
		      symbol_collection & symbols, extra_images const & extra,
		      string_filter const & symbol_filter);

	// output body of XML output
	void output(std::ostream & out);

	/** output one symbol symb to out according to the output format
	 * specifier previously set by call(s) to add_format() */
	virtual void output_symbol(std::ostream & out,
		symbol_entry const * symb, size_t lo, size_t hi,
		bool is_module);

	/// output details for the symbol
	std::string output_symbol_details(symbol_entry const * symb,
		size_t & detail_index, size_t const lo, size_t const hi);

	/// set the output_details boolean
	void show_details(bool);

	// output SymbolData XML elements
	void output_symbol_data(std::ostream & out);

private:
	/// container we work from
	profile_container const * profile;
 
	// ordered collection of symbols associated with this profile
	symbol_collection & symbols;

	/// true if we need to show details for each symbols
	bool need_details;

	// count of DetailData items output so far
	size_t detail_count;

	/// with --details we need to reopen the bfd object for each symb to
	/// get it's contents, hence we store the filter used by the bfd ctor.
	string_filter const & symbol_filter;

	void output_sample_data(std::ostream & out,
		sample_entry const & sample, size_t count);

	/// output attribute in XML
	void output_attribute(std::ostream & out, field_datum const & datum,
			      format_flags fl, tag_t tag);

	/// Retrieve a bfd object for this symbol, reopening a new bfd object
	/// only if necessary
	bool get_bfd_object(symbol_entry const * symb, op_bfd * & abfd) const;

	void output_the_symbol_data(std::ostream & out,
		symbol_entry const * symb, op_bfd * & abfd);

	void output_cg_children(std::ostream & out,
		cg_symbol::children const cg_symb, op_bfd * & abfd);
};

// callgraph XML output version
class xml_cg_formatter : public xml_formatter {
public:
	/// build a ready to use formatter
	xml_cg_formatter(callgraph_container const & callgraph,
		symbol_collection & symbols, string_filter const & sf);

	/** output one symbol symb to out according to the output format
	 * specifier previously set by call(s) to add_format() */
	virtual void output_symbol(std::ostream & out,
		symbol_entry const * symb, size_t lo, size_t hi, bool is_module);

private:
	/// container we work from
	callgraph_container const & callgraph;

	void output_symbol_core(std::ostream & out,
		cg_symbol::children const cg_symb,
		std::string const selfname, std::string const qname,
		size_t lo, size_t hi, bool is_module, tag_t tag);
};

} // namespace format_output 


#endif /* !FORMAT_OUTPUT_H */
