/**
 * @file name_storage.cpp
 * Storage of global names (filenames and symbols)
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "name_storage.h"
#include "demangle_symbol.h"
#include "file_manip.h"
#include "string_manip.h"
#include "locate_images.h"
#include "op_exception.h"

using namespace std;

image_name_storage image_names;
debug_name_storage debug_names;
symbol_name_storage symbol_names;


string const & image_name_storage::basename(image_name_id id) const
{
	stored_filename const & n = get(id);
	if (n.base_filename.empty())
		n.base_filename = op_basename(n.filename);
	return n.base_filename;
}


string const & image_name_storage::get_name(image_name_id id,
					    image_name_type type,
					    extra_images const & extra) const
{
	stored_filename const & n = get(id);
	if (type == int_filename) {
		return n.filename;
	} else if (type == int_basename) {
		return basename(id);
	} else if (type == int_real_basename) {
		if (n.extra_images_uid == 0) {
			// recursive call to init real_filename.
			get_name(id, int_real_filename, extra);
			n.real_base_filename = op_basename(n.real_filename);
		}
		return n.real_base_filename;
	} else if (type == int_real_filename) {
		if (n.extra_images_uid == 0) {
			// We ignore error here, the real path will be
			// identical to the name derived from the sample
			// filename in this case. FIXME: this mean than the
			// archive path will be ignored if an error occur.
			image_error error;
			n.real_filename = extra.find_image_path(name(id),
								error, true);
			n.extra_images_uid = extra.get_uid();
		}

		if (n.extra_images_uid == extra.get_uid())
			return n.real_filename;
		throw op_runtime_error("image_name_storage::get_name() called"
				       " with different extra parameter");
	}

	throw op_runtime_error("invalid parameter to"
			       " image_name_storage;;get_name()");
}


string const & debug_name_storage::basename(debug_name_id id) const
{
	stored_name const & n = get(id);
	if (n.name_processed.empty())
		n.name_processed = op_basename(n.name);
	return n.name_processed;
}


string const & symbol_name_storage::demangle(symbol_name_id id) const
{
	stored_name const & n = get(id);
	if (!n.name_processed.empty() || n.name.empty())
		return n.name_processed;

	if (n.name[0] != '?') {
		n.name_processed = demangle_symbol(n.name);
		return n.name_processed;
	}

	if (n.name.length() < 2 || n.name[1] != '?') {
		n.name_processed = "(no symbols)";
		return n.name_processed;
	}
	
	n.name_processed = "anonymous symbol from section ";
	n.name_processed += ltrim(n.name, "?");
	return n.name_processed;
}
