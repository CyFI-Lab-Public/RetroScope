/**
 * @file bfd_support.h
 * BFD muck we have to deal with.
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef BFD_SUPPORT_H
#define BFD_SUPPORT_H

#include "utility.h"
#include "op_types.h"
#include "locate_images.h"

#include <bfd.h>
#include <stdint.h>

#include <string>

class op_bfd_symbol;

/// holder for BFD state we must keep
struct bfd_info {
	bfd_info() : abfd(0), nr_syms(0), synth_syms(0), image_bfd_info(0) {}

	~bfd_info();

	/// close the BFD, setting abfd to NULL
	void close();

	/// return true if BFD is readable
	bool valid() const { return abfd; }

	/// return true if BFD has debug info
	bool has_debug_info() const;

	/// pick out the symbols from the bfd, if we can
	void get_symbols();

	/// the actual BFD
	bfd * abfd;
	/// normal symbols (includes synthesized symbols)
	scoped_array<asymbol *> syms;
	/// nr. symbols
	size_t nr_syms;

	void set_image_bfd_info(bfd_info * ibfd) { image_bfd_info = ibfd; }

private:
	/**
	 * Acquire the synthetic symbols if we need to.
	 */
	bool get_synth_symbols();

	/**
	 * On PPC64, synth_syms points to an array of synthetic asymbol
	 * structs returned from bfd_get_synthetic_symtab.  The syms
	 * member points into this array, so we have to keep it around
	 * until ~bfd_info time.
	 */
	asymbol * synth_syms;

	/**
	 * Under certain circumstances, correct handling of the bfd for a
	 * debuginfo file is not possible without access to the bfd for
	 * the actual image file.  The image_bfd_info field provides access to
	 * that bfd when this bfd_info is for a debuginfo file; otherwise
	 * image_bfd_info is NULL.
	 */ 
	bfd_info * image_bfd_info;

	/* To address a different issue, we discard symbols whose section
	 * flag does not contain SEC_LOAD.  But since this is true for symbols
	 * found in debuginfo files, we must run those debuginfo symbols
	 * through the function below to prevent them from being inadvertently
	 * discarded.  This function maps the sections from the symbols in
	 * the debuginfo bfd to those of the real image bfd.  Then, when
	 * we later do symbol filtering, we see the sections from the real
	 * bfd, which do contain SEC_LOAD in the section flag.
	 */
	void translate_debuginfo_syms(asymbol ** dbg_syms, long nr_dbg_syms);

};


/*
 * find_separate_debug_file - return true if a valid separate debug file found
 * @param ibfd binary file
 * @param dir_in directory holding the binary file
 * @param global_in
 * @param filename path to valid debug file
 *
 * Search order for debug file and use first one found:
 * 1) dir_in directory
 * 2) dir_in/.debug directory
 * 3) global_in/dir_in directory
 *
 * Newer binutils and Linux distributions (e.g. Fedora) allow the
 * creation of debug files that are separate from the binary. The
 * debugging information is stripped out of the binary file, placed in
 * this separate file, and a link to the new file is placed in the
 * binary. The debug files hold the information needed by the debugger
 * (and OProfile) to map machine instructions back to source code.
 */
extern bool
find_separate_debug_file(bfd * ibfd, 
                         std::string const & filepath_in,
                         std::string & debug_filename,
                         extra_images const & extra);

/// open the given BFD
bfd * open_bfd(std::string const & file);

/// open the given BFD from the fd
bfd * fdopen_bfd(std::string const & file, int fd);

/// Return a BFD for an SPU ELF embedded in PPE binary file
bfd * spu_open_bfd(std::string const name, int fd, uint64_t offset_to_spu_elf);

/// Return true if the symbol is worth looking at
bool interesting_symbol(asymbol * sym);

/**
 * return true if the first symbol is less interesting than the second symbol
 * boring symbol are eliminated when multiple symbol exist at the same vma
 */
bool boring_symbol(op_bfd_symbol const & first, op_bfd_symbol const & second);

/// debug info for a given pc
struct linenr_info {
	/// did we find something?
	bool found;
	/// filename
	std::string filename;
	/// line number
	unsigned int line;
};

/**
 * Attempt to locate a filename + line number for the given symbol and
 * offset.
 *
 * The bfd object is either the object associated with the binary or the
 * once associated with the separated debug info file so find_nearest_line()
 * can't lookup the section contents of code section etc. The filepos of
 * debuginfo symbols are different from the original file but we fixed symbol
 * filepos earlier.
 */
linenr_info const
find_nearest_line(bfd_info const & ibfd, op_bfd_symbol const & sym,
                  bfd_vma offset, bool anon_obj);

#endif /* !BFD_SUPPORT_H */
