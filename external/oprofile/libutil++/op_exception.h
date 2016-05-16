/**
 * @file op_exception.h
 * exception base class
 *
 * This provide simple base class for exception object. All
 * exception are derived from directly or indirectly from
 * std::exception. This class are not itended to be catched
 * in your code except at top level, derive what you want
 * and catch derived class rather.
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef OP_EXCEPTION_H
#define OP_EXCEPTION_H

#include <stdexcept>
#include <string>


/**
 * exception abstract base class
 */
class op_exception : public std::exception {
public:
	explicit op_exception(std::string const& msg);
	~op_exception() throw() = 0;

	char const * what() const throw();
private:
	std::string message;
};


/**
 * fatal exception, never catch it except at top level (likely main or
 * gui). Intended to replace cerr << "blah"; exit(EXIT_FAILURE); by a
 * throw op_fatal_error("blah") when returning error code is not an option
 */
struct op_fatal_error : op_exception
{
	explicit op_fatal_error(std::string const & msg);
};

/**
 * Encapsulate a runtime error with or w/o a valid errno
 */
struct op_runtime_error : std::runtime_error
{
	explicit op_runtime_error(std::string const & err);
	op_runtime_error(std::string const & err, int cerrno);
	~op_runtime_error() throw();
};


#endif /* !OP_EXCEPTION_H */
