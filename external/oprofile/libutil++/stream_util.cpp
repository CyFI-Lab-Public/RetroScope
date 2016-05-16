/**
 * @file stream_util.cpp
 * C++ stream utility
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "stream_util.h"

using namespace std;

io_state::io_state(ios & stream_)
	:
	stream(stream_),
	format(stream.flags()),
	precision(stream.precision()),
	fill(stream.fill())
{
}


io_state::~io_state()
{
	stream.flags(format);
	stream.precision(precision);
	stream.fill(fill);
}
