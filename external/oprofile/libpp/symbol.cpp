/**
 * @file symbol.cpp
 * Symbol containers
 *
 * @remark Copyright 2002, 2004 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */


#include "symbol.h"
#include "locate_images.h"

#include <iostream>
#include <string>

using namespace std;

column_flags symbol_entry::output_hint(column_flags fl) const
{
	if (app_name != image_name)
		fl = column_flags(fl | cf_image_name);

	// FIXME: see comment in symbol.h: why we don't use sample.vma + size ?
	if (sample.vma & ~0xffffffffLLU)
		fl = column_flags(fl | cf_64bit_vma);

	return fl;
}



bool has_sample_counts(count_array_t const & counts, size_t lo, size_t hi)
{
	for (size_t i = lo; i <= hi; ++i)
		if (counts[i] != 0)
			return true;
	return false;
}


string const & get_image_name(image_name_id id,
			      image_name_storage::image_name_type type,
			      extra_images const & extra)
{
	return image_names.get_name(id, type, extra);
}
