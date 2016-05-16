/**
 * @file op_regex.h
 * This file contains various definitions and interface for a
 * lightweight wrapper around libc regex, providing match
 * and replace facility.
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 * @remark Idea comes from TextFilt project <http://textfilt.sourceforge.net>
 *
 * @author Philippe Elie
 */

#ifndef OP_REGEX_H
#define OP_REGEX_H

// required by posix before including regex.h
#include <sys/types.h>
#include <regex.h>

#include <string>
#include <vector>
#include <map>

#include "op_exception.h"

/**
 * ill formed regular expression or expression throw such exception
 */
struct bad_regex : op_exception {
	bad_regex(std::string const & pattern);
};

/**
 * lightweight encapsulation of regex lib search and replace
 *
 * See stl.pat for further details and examples of used syntax.
 */
class regular_expression_replace {
public:
	/**
	 * @param limit limit on number of search and replace done
	 * @param limit_defs_expansion limit on number of expansion done
	 *  during replacement of regular definition name by their expansion
	 *
	 * build an object holding regular defintion and regular expression
	 * & replace, preparing it for substitution ala sed
	 */
	regular_expression_replace(size_t limit = 100,
				   size_t limit_defs_expansion = 100);
	~regular_expression_replace();

	/**
	 * @param name a regular definition name
	 * @param replace the string to subsitute in other regular definition
	 * or regular exepression when this regular defintion name is
	 * encoutered.
	 */
	void add_definition(std::string const & name,
			    std::string const & replace);
	/**
	 * @param pattern a regular expression pattern, POSIX extended notation
	 * @param replace the replace string to use when this regular
	 *  expression is matched
	 *
	 * You can imbed regular definition in pattern but not in replace.
	 */
	void add_pattern(std::string const & pattern,
			 std::string const & replace);

	/**
	 * @param str the input/output string where we search pattern and
	 * replace them.
	 *
	 * Execute loop at max limit time on the set of regular expression
	 *
	 * Return true if too many match occur and replacing has been stopped
	 * due to reach limit_defs_expansion. You can test if some pattern has
	 * been matched by saving the input string and comparing it to the new
	 * value. There is no way to detect s/a/a because the output string
	 * will be identical to the input string.
	 */
	bool execute(std::string & str) const;
private:
	struct replace_t {
		// when this regexp is matched
		regex_t regexp;
		// replace the matched part with this string
		std::string replace;
	};

	// helper to execute
	bool do_execute(std::string & str, replace_t const & regexp) const;
	void do_replace(std::string & str, std::string const & replace,
			regmatch_t const * match) const;

	// helper to add_definition() and add_pattern()
	std::string expand_string(std::string const & input);

	// helper to add_pattern
	std::string substitute_definition(std::string const & pattern);

	// return the match of throw if idx is invalid
	regmatch_t const & get_match(regmatch_t const * match, char idx) const;

	// don't increase too, it have direct impact on performance. This limit
	// the number of grouping expression allowed in a regular expression
	// Note than you can use grouping match operator > 9 only in the
	// replace rule not in match regular expression since POSIX don't allow
	// more than \9 in matching sequence.
	static const size_t max_match = 16;

	size_t limit;
	size_t limit_defs_expansion;
	std::vector<replace_t> regex_replace;
	/// dictionary of regular definition
	typedef std::map<std::string, std::string> defs_dict;
	defs_dict defs;
};

/**
 * @param regex the regular_expression_replace to fill
 * @param filename the filename from where the deifnition and pattern are read
 *
 * add to regex pattern and regular definition read from the given file
 */
void setup_regex(regular_expression_replace& regex,
		 std::string const & filename);

#endif /* !OP_REGEX_H */
