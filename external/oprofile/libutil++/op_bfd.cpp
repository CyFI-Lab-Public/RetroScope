/**
 * @file op_bfd.cpp
 * Encapsulation of bfd objects
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include "op_file.h"
#include "op_config.h"
#include "config.h"

#include <fcntl.h>
#include <cstring>

#include <sys/stat.h>

#include <cstdlib>

#include <algorithm>
#include <iostream>
#include <iomanip>
#include <sstream>

#include "op_bfd.h"
#include "locate_images.h"
#include "string_filter.h"
#include "stream_util.h"
#include "cverb.h"

using namespace std;


verbose vbfd("bfd");


namespace {

/// function object for filtering symbols to remove
struct remove_filter {
	remove_filter(string_filter const & filter)
		: filter_(filter) {}

	bool operator()(op_bfd_symbol const & symbol) {
		return !filter_.match(symbol.name());
	}

	string_filter filter_;
};


} // namespace anon


op_bfd_symbol::op_bfd_symbol(asymbol const * a)
	: bfd_symbol(a), symb_value(a->value),
	  section_filepos(a->section->filepos),
	  section_vma(a->section->vma),
	  symb_size(0), symb_hidden(false), symb_weak(false),
	  symb_artificial(false)
{
	// Some sections have unnamed symbols in them. If
	// we just ignore them then we end up sticking
	// things like .plt hits inside of _init. So instead
	// we name the symbol after the section.
	if (a->name && a->name[0] != '\0') {
		symb_name = a->name;
		symb_weak = a->flags & BSF_WEAK;
		symb_hidden = (a->flags & BSF_LOCAL)
 			&& !(a->flags & BSF_GLOBAL);
	} else {
		symb_name = string("??") + a->section->name;
	}
}


op_bfd_symbol::op_bfd_symbol(bfd_vma vma, size_t size, string const & name)
	: bfd_symbol(0), symb_value(vma),
	  section_filepos(0), section_vma(0),
	  symb_size(size), symb_name(name),
	  symb_artificial(true)
{
}


bool op_bfd_symbol::operator<(op_bfd_symbol const & rhs) const
{
	return filepos() < rhs.filepos();
}

unsigned long op_bfd_symbol::symbol_endpos(void) const
{
	return bfd_symbol->section->filepos + bfd_symbol->section->size;
}


op_bfd::op_bfd(string const & fname, string_filter const & symbol_filter,
	       extra_images const & extra_images, bool & ok)
	:
	filename(fname),
	archive_path(extra_images.get_archive_path()),
	extra_found_images(extra_images),
	file_size(-1),
	anon_obj(false)
{
	int fd;
	struct stat st;
	// after creating all symbol it's convenient for user code to access
	// symbols through a vector. We use an intermediate list to avoid a
	// O(N²) behavior when we will filter vector element below
	symbols_found_t symbols;
	asection const * sect;
	string suf = ".jo";

	image_error img_ok;
	string const image_path =
		extra_images.find_image_path(filename, img_ok, true);

	cverb << vbfd << "op_bfd ctor for " << image_path << endl;

	// if there's a problem already, don't try to open it
	if (!ok || img_ok != image_ok) {
		cverb << vbfd << "can't locate " << image_path << endl;
		goto out_fail;
	}

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

	ibfd.abfd = fdopen_bfd(image_path, fd);

	if (!ibfd.valid()) {
		cverb << vbfd << "fdopen_bfd failed for " << image_path << endl;
		ok = false;
		goto out_fail;
	}

	string::size_type pos;
	pos = filename.rfind(suf);
	if (pos != string::npos && pos == filename.size() - suf.size())
		anon_obj = true;


	// find .text and use it
	for (sect = ibfd.abfd->sections; sect; sect = sect->next) {
		if (sect->flags & SEC_CODE) {
			if (filepos_map[sect->name] != 0) {
				cerr << "Found section \"" << sect->name
				     << "\" twice for " << get_filename()
				     << endl;
				abort();
			}

			filepos_map[sect->name] = sect->filepos;

			if (sect->vma == 0 && strcmp(sect->name, ".text"))
				filtered_section.push_back(sect);
		}
	}

	get_symbols(symbols);

out:
	add_symbols(symbols, symbol_filter);
	return;
out_fail:
	ibfd.close();
	dbfd.close();
	// make the fake symbol fit within the fake file
	file_size = -1;
	goto out;
}


op_bfd::~op_bfd()
{
}


unsigned long op_bfd::get_start_offset(bfd_vma vma) const
{
	if (!vma || !ibfd.valid()) {
		filepos_map_t::const_iterator it = filepos_map.find(".text");
		if (it != filepos_map.end())
			return it->second;
		return 0;
	}

	return 0;
}


void op_bfd::get_symbols(op_bfd::symbols_found_t & symbols)
{
	ibfd.get_symbols();

	// On separate debug file systems, the main bfd has no symbols,
	// so even for non -g reports, we want to process the dbfd.
	// This hurts us pretty badly (the CRC), but we really don't
	// have much choice at the moment.
	has_debug_info();

	dbfd.set_image_bfd_info(&ibfd);
	dbfd.get_symbols();

	size_t i;
	for (i = 0; i < ibfd.nr_syms; ++i) {
		if (!interesting_symbol(ibfd.syms[i]))
			continue;
		if (find(filtered_section.begin(), filtered_section.end(),
			 ibfd.syms[i]->section) != filtered_section.end())
			continue;
		symbols.push_back(op_bfd_symbol(ibfd.syms[i]));
	}

	for (i = 0; i < dbfd.nr_syms; ++i) {
		if (!interesting_symbol(dbfd.syms[i]))
			continue;

		// need to use filepos of original file's section for
		// debug file symbols. We probably need to be more
		// careful for special symbols which have ->section from
		// .rodata like *ABS*
		u32 filepos = filepos_map[dbfd.syms[i]->section->name];
		if (filepos != 0)
			dbfd.syms[i]->section->filepos = filepos;
		symbols.push_back(op_bfd_symbol(dbfd.syms[i]));
	}

	symbols.sort();

	symbols_found_t::iterator it = symbols.begin();

	// we need to ensure than for a given vma only one symbol exist else
	// we read more than one time some samples. Fix #526098
	while (it != symbols.end()) {
		symbols_found_t::iterator temp = it;
		++temp;
		if (temp != symbols.end() && (it->vma() == temp->vma()) &&
			(it->filepos() == temp->filepos())) {
			if (boring_symbol(*it, *temp)) {
				it = symbols.erase(it);
			} else {
				symbols.erase(temp);
			}
		} else {
			++it;
		}
	}

	// now we can calculate the symbol size, we can't first include/exclude
	// symbols because the size of symbol is calculated from the difference
	// between the vma of a symbol and the next one.
	for (it = symbols.begin() ; it != symbols.end(); ++it) {
		op_bfd_symbol const * next = 0;
		symbols_found_t::iterator temp = it;
		++temp;
		if (temp != symbols.end())
			next = &*temp;
		it->size(symbol_size(*it, next));
	}
}


void op_bfd::add_symbols(op_bfd::symbols_found_t & symbols,
                         string_filter const & symbol_filter)
{
	// images with no symbols debug info available get a placeholder symbol
	if (symbols.empty())
		symbols.push_back(create_artificial_symbol());

	cverb << vbfd << "number of symbols before filtering "
	      << dec << symbols.size() << hex << endl;

	symbols_found_t::iterator it;
	it = remove_if(symbols.begin(), symbols.end(),
	               remove_filter(symbol_filter));

	copy(symbols.begin(), it, back_inserter(syms));

	cverb << vbfd << "number of symbols now "
	      << dec << syms.size() << hex << endl;
}


bfd_vma op_bfd::offset_to_pc(bfd_vma offset) const
{
	asection const * sect = ibfd.abfd->sections;

	for (; sect; sect = sect->next) {
		if (offset >= bfd_vma(sect->filepos) &&
		    (!sect->next || offset < bfd_vma(sect->next->filepos))) {
			return sect->vma + (offset - sect->filepos);
		}
	}

	return 0;
}

bool op_bfd::
symbol_has_contents(symbol_index_t sym_idx)
{
	op_bfd_symbol const & bfd_sym = syms[sym_idx];
	string const name = bfd_sym.name();
	if (name.size() == 0 || bfd_sym.artificial() || !ibfd.valid())
		return false;
	else
		return true;
}

bool op_bfd::
get_symbol_contents(symbol_index_t sym_index, unsigned char * contents) const
{
	op_bfd_symbol const & bfd_sym = syms[sym_index];
	size_t size = bfd_sym.size();

	if (!bfd_get_section_contents(ibfd.abfd, bfd_sym.symbol()->section, 
				 contents, 
				 static_cast<file_ptr>(bfd_sym.value()), size)) {
		return false;
	}
	return true;
}

bool op_bfd::has_debug_info() const
{
	if (debug_info.cached())
		return debug_info.get();

	if (!ibfd.valid())
		return debug_info.reset(false);

	if (ibfd.has_debug_info())
		return debug_info.reset(true);

	// check to see if there is an .debug file

	if (find_separate_debug_file(ibfd.abfd, filename, debug_filename, extra_found_images)) {
		cverb << vbfd << "now loading: " << debug_filename << endl;
		dbfd.abfd = open_bfd(debug_filename);
		if (dbfd.has_debug_info())
			return debug_info.reset(true);
	}

	// .debug is optional, so will not fail if there's a problem
	cverb << vbfd << "failed to process separate debug file "
	      << debug_filename << endl;

	return debug_info.reset(false);
}


bool op_bfd::get_linenr(symbol_index_t sym_idx, bfd_vma offset,
			string & source_filename, unsigned int & linenr) const
{
	if (!has_debug_info())
		return false;

	bfd_info const & b = dbfd.valid() ? dbfd : ibfd;
	op_bfd_symbol const & sym = syms[sym_idx];

	linenr_info const info = find_nearest_line(b, sym, offset, anon_obj);

	if (!info.found)
		return false;

	source_filename = info.filename;
	linenr = info.line;
	return true;
}


size_t op_bfd::symbol_size(op_bfd_symbol const & sym,
			   op_bfd_symbol const * next) const
{
	unsigned long long start = sym.filepos();
	unsigned long long end;

	if (next && (sym.section() != next->section()))
		end = sym.symbol_endpos();
	else
		end = next ? next->filepos() : file_size;

	return end - start;
}


void op_bfd::get_symbol_range(symbol_index_t sym_idx,
			      unsigned long long & start, unsigned long long & end) const
{
	op_bfd_symbol const & sym = syms[sym_idx];

	bool const verbose = cverb << (vbfd & vlevel1);

	if (anon_obj)
		start = sym.vma();
	else
		start = sym.filepos();
	end = start + sym.size();

	if (!verbose)
		return;

	io_state state(cverb << (vbfd & vlevel1));

	cverb << (vbfd & vlevel1) << "symbol " << sym.name()
	      << ", value " << hex << sym.value() << endl;
	cverb << (vbfd & vlevel1)
	      << "start " << hex << start << ", end " << end << endl;

	if (sym.symbol()) {
		cverb << (vbfd & vlevel1) << "in section "
		      << sym.symbol()->section->name << ", filepos "
		      << hex << sym.symbol()->section->filepos << endl;
	}
}


void op_bfd::get_vma_range(bfd_vma & start, bfd_vma & end) const
{
	if (!syms.empty()) {
		// syms are sorted by vma so vma of the first symbol and vma +
		// size of the last symbol give the vma range for gprof output
		op_bfd_symbol const & last_symb = syms[syms.size() - 1];
		start = syms[0].vma();
		// end is excluded from range so + 1 *if* last_symb.size() != 0
		end = last_symb.vma() + last_symb.size() + (last_symb.size() != 0);
	} else {
		start = 0;
		end = file_size;
	}
}


op_bfd_symbol const op_bfd::create_artificial_symbol()
{

	bfd_vma start, end;
	get_vma_range(start, end);
	return op_bfd_symbol(start, end - start, get_filename());
}


string op_bfd::get_filename() const
{
	return filename;
}


size_t op_bfd::bfd_arch_bits_per_address() const
{
	if (ibfd.valid())
		return ::bfd_arch_bits_per_address(ibfd.abfd);
	// FIXME: this function should be called only if the underlined ibfd
	// is ok, must we throw ?
	return sizeof(bfd_vma);
}
