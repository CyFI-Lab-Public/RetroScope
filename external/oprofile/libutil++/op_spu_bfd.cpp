/**
 * @file libutil++/op_spu_bfd.cpp
 * Encapsulation of bfd objects for Cell BE SPU
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Maynard Johnson
 * (C) Copyright IBM Corporation 2007
 */


#include <fcntl.h>
#include <sys/stat.h>
#include <cstdlib>
#include <cstring>

#include <iostream>
#include <cstring>
#include <cstdlib>

#include "op_bfd.h"
#include "locate_images.h"
#include "op_libiberty.h"
#include "string_filter.h"
#include "cverb.h"

#define OP_SPU_DYN_FLAG		0x10000000	/* kernel module adds this offset */
						/* to SPU code it can't find in the map */
#define OP_SPU_MEMSIZE		0x3ffff		/* Physical memory size on an SPU */

using namespace std;

extern verbose vbfd;

/*
 * This overload of the op_bfd constructor is patterned after the
 * constructor in libutil++/op_bfd.cpp, with the additional processing
 * needed to handle an embedded spu offset.
 */
op_bfd::op_bfd(uint64_t spu_offset, string const & fname,
	       string_filter const & symbol_filter, 
	       extra_images const & extra_images, bool & ok)
	:
	archive_path(extra_images.get_archive_path()),
	extra_found_images(extra_images),
	file_size(-1),
	embedding_filename(fname),
	anon_obj(false)
{
	int fd;
	struct stat st;
	int notes_remaining;
	bool spu_note_found = false;
	size_t sec_size = 0;
	unsigned int oct_per_byte;
	asection * note = NULL;

	symbols_found_t symbols;
	asection const * sect;

	image_error image_ok;
	string const image_path =
		extra_images.find_image_path(fname, image_ok, true);

	cverb << vbfd << "op_bfd ctor for " << image_path << endl;
	if (!ok)
		goto out_fail;

	fd = open(image_path.c_str(), O_RDONLY);
	if (fd == -1) {
		cverb << vbfd << "open failed for " << image_path << endl;
		ok = false;
		goto out_fail;
	}

	if (fstat(fd, &st)) {
		cverb << vbfd << "stat failed for " << image_path << endl;
		ok = false;
		goto out_fail;
	}

	file_size = st.st_size;
	ibfd.abfd = spu_open_bfd(image_path, fd, spu_offset);

	if (!ibfd.valid()) {
		cverb << vbfd << "fdopen_bfd failed for " << image_path << endl;
		ok = false;
		goto out_fail;
	}

	/* For embedded SPU ELF, a note section named '.note.spu_name'
	 * contains the name of the SPU binary image in the description
	 * field.
	 */
	note = bfd_get_section_by_name(ibfd.abfd, ".note.spu_name");
	if (!note) {
		cverb << vbfd << "No .note.spu-name section found" << endl;
		goto find_sec_code;
	}
	cverb << vbfd << "found .note.spu_name section" << endl;

	bfd_byte * sec_contents;
	oct_per_byte = bfd_octets_per_byte(ibfd.abfd);
	sec_size = bfd_section_size(ibfd.abfd, note)/oct_per_byte;

	sec_contents = (bfd_byte *) xmalloc(sec_size);
	if (!bfd_get_section_contents(ibfd.abfd, note, sec_contents,
				      0, sec_size)) {
		cverb << vbfd << "bfd_get_section_contents with size "
		      << sec_size << " returned an error" << endl;
		ok = false;
		goto out_fail;
	}
	notes_remaining = sec_size;
	while (notes_remaining && !spu_note_found) {
		unsigned int  nsize, dsize, type;
		nsize = *((unsigned int *) sec_contents);
		dsize = *((unsigned int *) sec_contents +1);
		type = *((unsigned int *) sec_contents +2);
		int remainder, desc_start, name_pad_length, desc_pad_length;
		name_pad_length = desc_pad_length = 0;
		/* Calculate padding for 4-byte alignment */
		remainder = nsize % 4;
		if (remainder != 0)
			name_pad_length = 4 - remainder;
		desc_start = 12 + nsize + name_pad_length;
		if (type != 1) {
			int note_record_length;
			if ((remainder = (dsize % 4)) != 0)
				desc_pad_length = 4 - remainder;
			note_record_length = 12 + nsize +
				name_pad_length + dsize + desc_pad_length;
			notes_remaining -= note_record_length;
			sec_contents += note_record_length;
			continue;
		} else {
			spu_note_found = true;
			/* Must memcpy the data from sec_contents to a
			 * 'char *' first, then stringify it, since
			 * the type of sec_contents (bfd_byte *) cannot be
			 * used as input for creating a string.
			 */
			char * description = (char *) xmalloc(dsize);
			memcpy(description, sec_contents + desc_start, dsize);
			filename = description;
			free(description);
		}
	}
	free(sec_contents);
	/* Default to app name for the image name */
	if (spu_note_found == false)
		filename = fname;

find_sec_code:
	for (sect = ibfd.abfd->sections; sect; sect = sect->next) {
		if (sect->flags & SEC_CODE) {
			if (filepos_map[sect->name] != 0) {
				cerr << "Found section \"" << sect->name
				     << "\" twice for " << get_filename()
				     << endl;
				abort();
			}

			filepos_map[sect->name] = sect->filepos;
		}
	}

	get_symbols(symbols);

	/* In some cases the SPU library code generates code stubs on the stack. */
	/* The kernel module remaps those addresses so add an entry to catch/report them. */
	symbols.push_back(op_bfd_symbol(OP_SPU_DYN_FLAG, OP_SPU_MEMSIZE,
			  "__send_to_ppe(stack)"));

out:
	add_symbols(symbols, symbol_filter);
	return;
out_fail:
	ibfd.close();
	dbfd.close();
	file_size = -1;
	goto out;
}

