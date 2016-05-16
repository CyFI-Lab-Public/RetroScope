/**
 * @file op_bfd.h
 * Encapsulation of bfd objects
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef OP_BFD_H
#define OP_BFD_H

#include "config.h"

#include <vector>
#include <string>
#include <list>
#include <map>
#include <set>

#include "bfd_support.h"
#include "locate_images.h"
#include "utility.h"
#include "cached_value.h"
#include "op_types.h"

class op_bfd;
class string_filter;
class extra_images;

/// all symbol vector indexing uses this type
typedef size_t symbol_index_t;

/**
 * A symbol description from a bfd point of view. This duplicate
 * information pointed by an asymbol, we need this duplication in case
 * the symbol is an artificial symbol
 */
class op_bfd_symbol {
public:

	/// ctor for real symbols
	op_bfd_symbol(asymbol const * a);

	/// ctor for artificial symbols
	op_bfd_symbol(bfd_vma vma, size_t size, std::string const & name);

	bfd_vma vma() const { return symb_value + section_vma; }
	unsigned long value() const { return symb_value; }
	unsigned long filepos() const { return symb_value + section_filepos; }
	unsigned long symbol_endpos(void) const;
	asection const * section(void) const { return bfd_symbol->section; }
	std::string const & name() const { return symb_name; }
	asymbol const * symbol() const { return bfd_symbol; }
	size_t size() const { return symb_size; }
	void size(size_t s) { symb_size = s; }
	bool hidden() const { return symb_hidden; }
	bool weak() const { return symb_weak; }
	bool artificial() const { return symb_artificial; }

	/// compare two symbols by their filepos()
	bool operator<(op_bfd_symbol const & lhs) const;

private:
	/// the original bfd symbol, this can be null if the symbol is an
	/// artificial symbol
	asymbol const * bfd_symbol;
	/// the offset of this symbol relative to the begin of the section's
	/// symbol
	unsigned long symb_value;
	/// the section filepos for this symbol
	unsigned long section_filepos;
	/// the section vma for this symbol
	bfd_vma section_vma;
	/// the size of this symbol
	size_t symb_size;
	/// the name of the symbol
	std::string symb_name;
	/// normally not externally visible symbol
	bool symb_hidden;
	/// whether other symbols can override it
	bool symb_weak;
	/// symbol is artificially created
	bool symb_artificial;
	/// code bytes corresponding to symbol -- used for XML generation
	std::string symb_bytes;
};

/**
 * Encapsulation of a bfd object. Simplifies open/close of bfd, enumerating
 * symbols and retrieving informations for symbols or vma.
 *
 * Use of this class relies on a std::ostream cverb
 */
class op_bfd {
public:
	/**
	 * @param filename  the name of the image file
	 * @param symbol_filter  filter to apply to symbols
	 * @param extra_images container where all extra candidate filenames
	 *    are stored
	 * @param ok in-out parameter: on in, if not set, don't
	 * open the bfd (because it's not there or whatever). On out,
	 * it's set to false if the bfd couldn't be loaded.
	 */
	op_bfd(std::string const & filename,
	       string_filter const & symbol_filter,
	       extra_images const & extra_images,
	       bool & ok);

	/**
	 * This constructor is used when processing an SPU profile
	 * where the SPU ELF is embedded within the PPE binary.
	 */
	op_bfd(uint64_t spu_offset,
	       std::string const & filename,
	       string_filter const & symbol_filter,
	       extra_images const & extra_images,
	       bool & ok);

	std::string get_embedding_filename() const { return embedding_filename; }

	/// close an opened bfd image and free all related resources
	~op_bfd();

	/**
	 * @param sym_idx index of the symbol
	 * @param offset fentry number
	 * @param filename output parameter to store filename
	 * @param linenr output parameter to store linenr.
	 *
	 * Retrieve the relevant finename:linenr information for the sym_idx
	 * at offset. If the lookup fails, return false. In some cases this
	 * function can retrieve the filename and return true but fail to
	 * retrieve the linenr and so can return zero in linenr
	 */
	bool get_linenr(symbol_index_t sym_idx, bfd_vma offset,
			std::string & filename, unsigned int & linenr) const;

	/**
	 * @param sym_idx symbol index
	 * @param start reference to start var
	 * @param end reference to end var
	 *
	 * Calculates the range of sample file entries covered by sym. start
	 * and end will be filled in appropriately. If index is the last entry
	 * in symbol table, all entries up to the end of the sample file will
	 * be used.  After calculating start and end they are sanitized
	 *
	 * All errors are fatal.
	 */
	void get_symbol_range(symbol_index_t sym_idx,
			      unsigned long long & start, unsigned long long & end) const;

	/**
	 * @param start reference to the start vma
	 * @param end reference to the end vma
	 *
	 * return in start, end the vma range for this binary object.
	 */
	void get_vma_range(bfd_vma & start, bfd_vma & end) const;

	/** return the relocated PC value for the given file offset */
	bfd_vma offset_to_pc(bfd_vma offset) const;

	/**
	 * If passed 0, return the file position of the .text section.
	 * Otherwise, return the filepos of a section with a matching
	 * vma.
	 */
	unsigned long get_start_offset(bfd_vma vma = 0) const;
 
	/**
	 * Return the image name of the underlying binary image. For an
	 * archive, this returns the path *within* the archive, not the
	 * full path of the file.
	 */
	std::string get_filename() const;

	/// sorted vector by vma of interesting symbol.
	std::vector<op_bfd_symbol> syms;

	/// return in bits the bfd_vma size for this binary. This is needed
	/// because gprof output depend on the bfd_vma for *this* binary
	/// and do not depend on sizeof(bfd_vma)
	size_t bfd_arch_bits_per_address() const;

	/// return true if binary contain some debug information
	bool has_debug_info() const;

	/**
	 * @param sym_idx symbol index
	 *
	 * Return true or false, indicating whether or not the
	 * symbol referenced by the passed sym_idx has code available.
	 * Some symbols have no code associated with them; for example,
	 * artificial symbols created for anonymous memory samples or for
	 * stripped binaries with no symbol debug info.  Additionally,
	 * if the bfd object associated with the symbol is not valid,
	 * this function will also return false.
	 *
	 * NOTE:  This call should be made prior to invoking
	 *        get_symbol_contents to avoid unnecessarily allocating
	 *        memory for the symbol contents.
	 */
	bool symbol_has_contents(symbol_index_t sym_idx);

	bool get_symbol_contents(symbol_index_t sym_index,
		unsigned char * contents) const;

	bool valid() const { return ibfd.valid(); }

private:
	/// temporary container type for getting symbols
	typedef std::list<op_bfd_symbol> symbols_found_t;

	/**
	 * Parse and sort in ascending order all symbols
	 * in the file pointed to by abfd that reside in
	 * a %SEC_CODE section.
	 *
	 * The symbols are filtered through
	 * the interesting_symbol() predicate and sorted
	 * with op_bfd_symbol::operator<() comparator.
	 */
	void get_symbols(symbols_found_t & symbols);

	/**
	 * Helper function for get_symbols.
	 * Populates bfd_syms and extracts the "interesting_symbol"s.
	 */
	void get_symbols_from_file(bfd_info & bfd, size_t start,
				   op_bfd::symbols_found_t & symbols,
				   bool debug_file);

	/**
	 * Add the symbols in the binary, applying filtering,
	 * and handling artificial symbols.
	 */
	void add_symbols(symbols_found_t & symbols,
	                 string_filter const & symbol_filter);

	/**
	 * symbol_size - return the size of a symbol
	 * @param sym  symbol to get size
	 * @param next  next symbol in vma order if any
	 */
	size_t symbol_size(op_bfd_symbol const & sym,
			   op_bfd_symbol const * next) const;

	/// create an artificial symbol for a symbolless binary
	op_bfd_symbol const create_artificial_symbol();

        /* Generate symbols using bfd functions for
	 * the image file associated with the ibfd arg.
	 */
	uint process_symtab(bfd_info * bfd, uint start);

	/// filename we open (not including archive path)
	std::string filename;

	/// path to archive
	std::string archive_path;

	/// reference to extra_images
	extra_images const & extra_found_images;

	/// file size in bytes
	off_t file_size;

	/// corresponding debug file name
	mutable std::string debug_filename;

	/// true if at least one section has (flags & SEC_DEBUGGING) != 0
	mutable cached_value<bool> debug_info;

	/// our main bfd object: .bfd may be NULL
	bfd_info ibfd;

	// corresponding debug bfd object, if one is found
	mutable bfd_info dbfd;

	/// sections we will avoid to use symbol from, this is needed
	/// because elf file allows sections with identical vma and we can't
	/// allow overlapping symbols. Such elf layout is used actually by
	/// kernel modules where all code section vma are set to 0.
	std::vector<asection const *> filtered_section;

	typedef std::map<std::string, u32> filepos_map_t;
	// mapping of section names to filepos in the original binary
	filepos_map_t filepos_map;

	/**
	 * If spu_offset is non-zero, embedding_filename is the file containing
	 * the embedded SPU image.
	 */
	std::string embedding_filename;

	bool anon_obj;
};


#endif /* !OP_BFD_H */
