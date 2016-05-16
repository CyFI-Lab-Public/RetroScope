/**
 * @file symbol_functors.h
 * Functors for symbol/sample comparison
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef SYMBOL_FUNCTORS_H
#define SYMBOL_FUNCTORS_H

#include "symbol.h"

/// compare based on file location
struct less_by_file_loc {
	bool operator()(sample_entry const * lhs,
			sample_entry const * rhs) const {
		return lhs->file_loc < rhs->file_loc;
	}

	bool operator()(symbol_entry const * lhs,
			symbol_entry const * rhs) const {
		return lhs->sample.file_loc < rhs->sample.file_loc;
	}
};


/// compare based on symbol contents
struct less_symbol {
	// implementation compare by id rather than by string
	bool operator()(symbol_entry const & lhs,
			symbol_entry const & rhs) const;
};

#endif /* SYMBOL_FUNCTORS_H */
