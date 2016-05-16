/**
 * @file op_exception.cpp
 * exception base class
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <cstring>

#include "op_exception.h"

using namespace std;

op_exception::op_exception(string const & msg)
	:
	message(msg)
{
}

op_exception::~op_exception() throw()
{
}

char const * op_exception::what() const throw()
{
	return message.c_str();
}


op_fatal_error::op_fatal_error(string const & msg)
	:
	op_exception(msg)
{
}


op_runtime_error::op_runtime_error(string const & msg)
	:
	runtime_error(msg)
{
}

op_runtime_error::op_runtime_error(string const & msg, int cerrno)
	:
	runtime_error(msg + "\ncause: " + strerror(cerrno))
{
}

op_runtime_error::~op_runtime_error() throw()
{
}
