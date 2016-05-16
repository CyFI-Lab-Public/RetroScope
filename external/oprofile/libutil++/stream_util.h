/**
 * @file stream_util.h
 * C++ stream utility
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef STREAM_UTIL_H
#define STREAM_UTIL_H

#include <iostream>

/// class which save a stream state and restore it at dtor time
class io_state {
public:
	/**
	 * save the stream flags, precision and fill char.
	 *
	 * width is restored at end of expression, there is no need to save it.
	 * tie and locale are not saved currently
	 *
	 * error state shouldn't be saved.
	 */
	io_state(std::ios & stream);
	/// restore the stream state
	~io_state();
private:
	std::ios & stream;

	std::ios::fmtflags format;
	std::streamsize precision;
	char fill;
};

#endif /* !STREAM_UTIL_H */
