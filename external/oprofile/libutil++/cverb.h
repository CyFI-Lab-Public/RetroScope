/**
 * @file cverb.h
 * verbose output stream
 *
 * @remark Copyright 2002, 2004 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef CVERB_H
#define CVERB_H

#include <iosfwd>
#include <string>
#include <vector>

struct cverb_object { };

/**
 * verbose object, all output through this stream are made only
 * if a verbose object with a true state is injected in the stream.
 */
extern cverb_object cverb;

/**
 * typical use:
 * declare some verbose global object:
 * verbose debug("debug");
 * verbose stats("stats");
 * verbose level2("level2");
 *
 * setup from command line the state of these objects
 *
 * verbose::setup(command_line_args_to'--verbose=');
 *
 * cverb << stats << "stats\n";
 * cverb << (stats&level2) << "very verbose stats\n"
 * cverb << (stats|debug) << "bar\n";
 * these will give a compile time error
 * cverb << stats << "foo" << debug << "bar"; 
 * cout << stats << "foo";
 *
 * In critical code path cverb can be used in the more efficient way:
 * if (cverb << vdebug)
 *    cverb << vdebug << "foo" << "bar";
 * the condition test the fails bit for the returned stream while the later
 * build a sentry object for each << (more efficient even with one level of <<)
 */
class verbose {
	/// The returned stream is either a null stream or cout.
	friend std::ostream & operator<<(cverb_object &, verbose const &);
public:
	/**
	 * create a verbose object named name, the ctor auto-register name
	 * as a verbose object, the set state can be intialized through
	 * verbose::setup(name)
	 */
	verbose(char const * name);

	verbose operator|(verbose const &);
	verbose operator&(verbose const &);

	/// Return false if this named verbose object has not be registred.
	static bool setup(std::string const &);
	/// convenient interface calling the above for string in args
	static bool setup(std::vector<std::string> const & args);
private:
	bool set;
};

/**
 * predefined general purpose verbose object, comment give their names
 */
extern verbose vlevel1; /**< named "level1" */
extern verbose vdebug;  /**< named "debug"  */
extern verbose vstats;  /**< named "stats"  */
// all sample filename manipulation.
extern verbose vsfile;  /**< named "sfile" */
extern verbose vxml;  /**< named "xml" */

#endif /* !CVERB_H */
