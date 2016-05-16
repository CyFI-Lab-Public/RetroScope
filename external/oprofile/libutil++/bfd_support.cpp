/**
 * @file bfd_support.cpp
 * BFD muck we have to deal with.
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#include "bfd_support.h"

#include "op_bfd.h"
#include "op_fileio.h"
#include "op_config.h"
#include "string_manip.h"
#include "file_manip.h"
#include "cverb.h"
#include "locate_images.h"

#include <cstdlib>
#include <cstring>
#include <cassert>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <cstring>
#include <cstdlib>

using namespace std;

extern verbose vbfd;

namespace {


void check_format(string const & file, bfd ** ibfd)
{
	if (!bfd_check_format_matches(*ibfd, bfd_object, NULL)) {
		cverb << vbfd << "BFD format failure for " << file << endl;
		bfd_close(*ibfd);
		*ibfd = NULL;
	}
}


bool separate_debug_file_exists(string & name, unsigned long const crc, 
                                extra_images const & extra)
{
	unsigned long file_crc = 0;
	// The size of 2 * 1024 elements for the buffer is arbitrary.
	char buffer[2 * 1024];

	image_error img_ok;
	string const image_path = extra.find_image_path(name, img_ok, true);

	if (img_ok != image_ok)
		return false;

	name = image_path;

	ifstream file(image_path.c_str());
	if (!file)
		return false;

	cverb << vbfd << "found " << name;
	while (file) {
		file.read(buffer, sizeof(buffer));
		file_crc = calc_crc32(file_crc, 
				      reinterpret_cast<unsigned char *>(&buffer[0]),
				      file.gcount());
	}
	cverb << vbfd << " with crc32 = " << hex << file_crc << endl;
	return crc == file_crc;
}


bool get_debug_link_info(bfd * ibfd, string & filename, unsigned long & crc32)
{
	asection * sect;

	cverb << vbfd << "fetching .gnu_debuglink section" << endl;
	sect = bfd_get_section_by_name(ibfd, ".gnu_debuglink");
	
	if (sect == NULL)
		return false;
	
	bfd_size_type debuglink_size = bfd_section_size(ibfd, sect);  
	char contents[debuglink_size];
	cverb << vbfd
	      << ".gnu_debuglink section has size " << debuglink_size << endl;
	
	if (!bfd_get_section_contents(ibfd, sect, 
				 reinterpret_cast<unsigned char *>(contents), 
				 static_cast<file_ptr>(0), debuglink_size)) {
		bfd_perror("bfd_get_section_contents:get_debug:");
		exit(2);
	}
	
	/* CRC value is stored after the filename, aligned up to 4 bytes. */
	size_t filename_len = strlen(contents);
	size_t crc_offset = filename_len + 1;
	crc_offset = (crc_offset + 3) & ~3;
	
	crc32 = bfd_get_32(ibfd, 
			       reinterpret_cast<bfd_byte *>(contents + crc_offset));
	filename = string(contents, filename_len);
	cverb << vbfd << ".gnu_debuglink filename is " << filename << endl;
	return true;
}


/**
 * With Objective C, we'll get strings like:
 *
 * _i_GSUnicodeString__rangeOfCharacterSetFromSet_options_range
 *
 * for the symbol name, and:
 * -[GSUnicodeString rangeOfCharacterFromSet:options:range:]
 *
 * for the function name, so we have to do some looser matching
 * than for other languages (unfortunately, it's not possible
 * to demangle Objective C symbols).
 */
bool objc_match(string const & sym, string const & method)
{
	if (method.length() < 3)
		return false;

	string mangled;

	if (is_prefix(method, "-[")) {
		mangled += "_i_";
	} else if (is_prefix(method, "+[")) {
		mangled += "_c_";
	} else {
		return false;
	}

	string::const_iterator it = method.begin() + 2;
	string::const_iterator const end = method.end();

	bool found_paren = false;

	for (; it != end; ++it) {
		switch (*it) {
		case ' ':
			mangled += '_';
			if (!found_paren)
				mangled += '_';
			break;
		case ':':
			mangled += '_';
			break;
		case ')':
		case ']':
			break;
		case '(':
			found_paren = true;
			mangled += '_';
			break;
		default:
			mangled += *it;	
		}
	}

	return sym == mangled;
}


/*
 * With a binary image where some objects are missing debug
 * info, we can end up attributing to a completely different
 * function (#484660): bfd_nearest_line() will happily move from one
 * symbol to the nearest one it can find with debug information.
 * To mitigate this problem, we check that the symbol name
 * matches the returned function name.
 *
 * However, this check fails in some cases it shouldn't:
 * Objective C, and C++ static inline functions (as discussed in
 * GCC bugzilla #11774). So, we have a looser check that
 * accepts merely a substring, plus some magic for Objective C.
 *
 * If even the loose check fails, then we give up.
 */
bool is_correct_function(string const & function, string const & name)
{
	if (name == function)
		return true;

	if (objc_match(name, function))
		return true;

	// warn the user if we had to use the loose check
	if (name.find(function) != string::npos) {
		static bool warned = false;
		if (!warned) {
			cerr << "warning: some functions compiled without "
			     << "debug information may have incorrect source "
			     << "line attributions" << endl;
				warned = true;
		}
		cverb << vbfd << "is_correct_function(" << function << ", "
		      << name << ") fuzzy match." << endl;
		return true;
	}

	return false;
}


/*
 * binutils 2.12 and below have a small bug where functions without a
 * debug entry at the prologue start do not give a useful line number
 * from bfd_find_nearest_line(). This can happen with certain gcc
 * versions such as 2.95.
 *
 * We work around this problem by scanning forward for a vma with valid
 * linenr info, if we can't get a valid line number.  Problem uncovered
 * by Norbert Kaufmann. The work-around decreases, on the tincas
 * application, the number of failure to retrieve linenr info from 835
 * to 173. Most of the remaining are c++ inline functions mainly from
 * the STL library. Fix #529622
 */
void fixup_linenr(bfd * abfd, asection * section, asymbol ** syms,
		  string const & name, bfd_vma pc,
                  char const ** filename, unsigned int * line)
{
	char const * cfilename;
	char const * function;
	unsigned int linenr;

	// FIXME: looking at debug info for all gcc version shows than
	// the same problems can -perhaps- occur for epilog code: find a
	// samples files with samples in epilog and try opreport -l -g
	// on it, check it also with opannotate.

	// first restrict the search on a sensible range of vma, 16 is
	// an intuitive value based on epilog code look
	size_t max_search = 16;
	size_t section_size = bfd_section_size(abfd, section);
	if (pc + max_search > section_size)
		max_search = section_size - pc;

	for (size_t i = 1; i < max_search; ++i) {
		bool ret = bfd_find_nearest_line(abfd, section, syms, pc + i,
						 &cfilename, &function,
						 &linenr);

		if (ret && cfilename && function && linenr != 0
		    && is_correct_function(function, name)) {
			*filename = cfilename;
			*line = linenr;
			return;
		}
	}
}


} // namespace anon


bfd * open_bfd(string const & file)
{
	/* bfd keeps its own reference to the filename char *,
	 * so it must have a lifetime longer than the ibfd */
	bfd * ibfd = bfd_openr(file.c_str(), NULL);
	if (!ibfd) {
		cverb << vbfd << "bfd_openr failed for " << file << endl;
		return NULL;
	}

	check_format(file, &ibfd);

	return ibfd;
}


bfd * fdopen_bfd(string const & file, int fd)
{
	/* bfd keeps its own reference to the filename char *,
	 * so it must have a lifetime longer than the ibfd */
	bfd * ibfd = bfd_fdopenr(file.c_str(), NULL, fd);
	if (!ibfd) {
		cverb << vbfd << "bfd_openr failed for " << file << endl;
		return NULL;
	}

	check_format(file, &ibfd);

	return ibfd;
}


bool find_separate_debug_file(bfd * ibfd, string const & filepath_in, 
                              string & debug_filename, extra_images const & extra)
{
	string filepath(filepath_in);
	string basename;
	unsigned long crc32;
	
	if (!get_debug_link_info(ibfd, basename, crc32))
		return false;

	// Work out the image file's directory prefix
	string filedir = op_dirname(filepath);
	// Make sure it starts with /
	if (filedir.size() > 0 && filedir.at(filedir.size() - 1) != '/')
		filedir += '/';

	string first_try(filedir + ".debug/" + basename);
	string second_try(DEBUGDIR + filedir + basename);
	string third_try(filedir + basename);

	cverb << vbfd << "looking for debugging file " << basename 
	      << " with crc32 = " << hex << crc32 << endl;

	if (separate_debug_file_exists(first_try, crc32, extra)) 
		debug_filename = first_try; 
	else if (separate_debug_file_exists(second_try, crc32, extra))
		debug_filename = second_try;
	else if (separate_debug_file_exists(third_try, crc32, extra))
		debug_filename = third_try;
	else
		return false;
	
	return true;
}


bool interesting_symbol(asymbol * sym)
{
	// #717720 some binutils are miscompiled by gcc 2.95, one of the
	// typical symptom can be catched here.
	if (!sym->section) {
		ostringstream os;
		os << "Your version of binutils seems to have a bug.\n"
		   << "Read http://oprofile.sf.net/faq/#binutilsbug\n";
		throw op_runtime_error(os.str());
	}

	if (!(sym->section->flags & SEC_CODE))
		return false;

	// returning true for fix up in op_bfd_symbol()
	if (!sym->name || sym->name[0] == '\0')
		return true;
	/* ARM assembler internal mapping symbols aren't interesting */
	if ((strcmp("$a", sym->name) == 0) ||
	    (strcmp("$t", sym->name) == 0) ||
	    (strcmp("$d", sym->name) == 0))
		return false;

	// C++ exception stuff
	if (sym->name[0] == '.' && sym->name[1] == 'L')
		return false;

	/* This case cannot be moved to boring_symbol(),
	 * because that's only used for duplicate VMAs,
	 * and sometimes this symbol appears at an address
	 * different from all other symbols.
	 */
	if (!strcmp("gcc2_compiled.", sym->name))
		return false;

        if (sym->flags & BSF_SECTION_SYM)
                return false;

	if (!(sym->section->flags & SEC_LOAD))
		return false;

	return true;
}


bool boring_symbol(op_bfd_symbol const & first, op_bfd_symbol const & second)
{
	if (first.name() == "Letext")
		return true;
	else if (second.name() == "Letext")
		return false;

	if (first.name().substr(0, 2) == "??")
		return true;
	else if (second.name().substr(0, 2) == "??")
		return false;

	if (first.hidden() && !second.hidden())
		return true;
	else if (!first.hidden() && second.hidden())
		return false;

	if (first.name()[0] == '_' && second.name()[0] != '_')
		return true;
	else if (first.name()[0] != '_' && second.name()[0] == '_')
		return false;

	if (first.weak() && !second.weak())
		return true;
	else if (!first.weak() && second.weak())
		return false;

	return false;
}


bool bfd_info::has_debug_info() const
{
	if (!valid())
		return false;

	for (asection const * sect = abfd->sections; sect; sect = sect->next) {
		if (sect->flags & SEC_DEBUGGING)
			return true;
	}

	return false;
}


bfd_info::~bfd_info()
{
	free(synth_syms);
	close();
}


void bfd_info::close()
{
	if (abfd)
		bfd_close(abfd);
}

/**
 * This function is only called when processing symbols retrieved from a
 * debuginfo file that is separate from the actual runtime binary image.
 * Separate debuginfo files may be needed in two different cases:
 *   1) the real image is completely stripped, where there is no symbol
	information at all
 *   2) the real image has debuginfo stripped, and the user is requesting "-g"
 *   (src file/line num info)
 * After all symbols are gathered up, there will be some filtering/removal of
 * unnecessary symbols.  In particular, the bfd_info::interesting_symbol()
 * function filters out symbols whose section's flag value does not include
 * SEC_LOAD.  This filtering is required, so it must be retained.  However,
 * we run into a problem with symbols from debuginfo files, since the
 * section flag does NOT include SEC_LOAD.  To solve this problem, the
 * translate_debuginfo_syms function maps the debuginfo symbol's sections to
 * that of their corresponding real image.
*/
void bfd_info::translate_debuginfo_syms(asymbol ** dbg_syms, long nr_dbg_syms)
{
	unsigned int img_sect_cnt = 0;
	bfd * image_bfd = image_bfd_info->abfd;
	multimap<string, bfd_section *> image_sections;

	for (bfd_section * sect = image_bfd->sections;
	     sect && img_sect_cnt < image_bfd->section_count;
	     sect = sect->next) {
		// A comment section marks the end of the needed sections
		if (strstr(sect->name, ".comment") == sect->name)
			break;
		image_sections.insert(pair<string, bfd_section *>(sect->name, sect));
		img_sect_cnt++;
	}

	asymbol * sym = dbg_syms[0];
	string prev_sect_name = "";
	bfd_section * matched_section = NULL;
	for (int i = 0; i < nr_dbg_syms; sym = dbg_syms[++i]) {
		bool section_switch;

		if (strcmp(prev_sect_name.c_str(), sym->section->name)) {
			section_switch = true;
			prev_sect_name = sym->section->name;
		} else {
			section_switch = false;
		}
		if (sym->section->owner && sym->section->owner == abfd) {
			if (section_switch ) {
				matched_section = NULL;
				multimap<string, bfd_section *>::iterator it;
				pair<multimap<string, bfd_section *>::iterator,
				     multimap<string, bfd_section *>::iterator> range;

				range = image_sections.equal_range(sym->section->name);
				for (it = range.first; it != range.second; it++) {
					if ((*it).second->vma == sym->section->vma) {
						matched_section = (*it).second;
						break;
					}
				}
			}
			if (matched_section) {
				sym->section = matched_section;
				sym->the_bfd = image_bfd;
			}
		}
	}
}

#if SYNTHESIZE_SYMBOLS
bool bfd_info::get_synth_symbols()
{
	extern const bfd_target bfd_elf64_powerpc_vec;
	extern const bfd_target bfd_elf64_powerpcle_vec;
	bool is_elf64_powerpc_target = (abfd->xvec == &bfd_elf64_powerpc_vec)
		|| (abfd->xvec == &bfd_elf64_powerpcle_vec);

	if (!is_elf64_powerpc_target)
		return false;

	void * buf;
	uint tmp;
	long nr_mini_syms = bfd_read_minisymbols(abfd, 0, &buf, &tmp);
	if (nr_mini_syms < 1)
		return false;

	asymbol ** mini_syms = (asymbol **)buf;
	buf = NULL;
	bfd * synth_bfd;

	/* For ppc64, a debuginfo file by itself does not hold enough symbol
	 * information for us to properly attribute samples to symbols.  If
	 * the image file's bfd has no symbols (as in a super-stripped library),
	 * then we need to do the extra processing in translate_debuginfo_syms.
	 */
	if (image_bfd_info && image_bfd_info->nr_syms == 0) {
		translate_debuginfo_syms(mini_syms, nr_mini_syms);
		synth_bfd = image_bfd_info->abfd;
	} else
		synth_bfd = abfd;
	
	long nr_synth_syms = bfd_get_synthetic_symtab(synth_bfd,
	                                              nr_mini_syms,
	                                              mini_syms, 0,
	                                              NULL, &synth_syms);

	if (nr_synth_syms < 0) {
		free(mini_syms);
		return false;
	}

	cverb << vbfd << "mini_syms: " << dec << nr_mini_syms << hex << endl;
	cverb << vbfd << "synth_syms: " << dec << nr_synth_syms << hex << endl;

	nr_syms = nr_mini_syms + nr_synth_syms;
	syms.reset(new asymbol *[nr_syms + 1]);

	for (size_t i = 0; i < (size_t)nr_mini_syms; ++i)
		syms[i] = mini_syms[i];


	for (size_t i = 0; i < (size_t)nr_synth_syms; ++i)
		syms[nr_mini_syms + i] = synth_syms + i;
	

	free(mini_syms);

	// bfd_canonicalize_symtab does this, so shall we
	syms[nr_syms] = NULL;

	return true;
}
#else
bool bfd_info::get_synth_symbols()
{
	return false;
}
#endif /* SYNTHESIZE_SYMBOLS */


void bfd_info::get_symbols()
{
	if (!abfd)
		return;

	cverb << vbfd << "bfd_info::get_symbols() for "
	      << bfd_get_filename(abfd) << endl;

	if (get_synth_symbols())
		return;

	if (bfd_get_file_flags(abfd) & HAS_SYMS)
		nr_syms = bfd_get_symtab_upper_bound(abfd);

	cverb << vbfd << "bfd_get_symtab_upper_bound: " << dec
	      << nr_syms << hex << endl;

	nr_syms /= sizeof(asymbol *);

	if (nr_syms < 1)
		return;

	syms.reset(new asymbol *[nr_syms]);

	nr_syms = bfd_canonicalize_symtab(abfd, syms.get());

	if (image_bfd_info)
		translate_debuginfo_syms(syms.get(), nr_syms);

	cverb << vbfd << "bfd_canonicalize_symtab: " << dec
	      << nr_syms << hex << endl;
}


linenr_info const
find_nearest_line(bfd_info const & b, op_bfd_symbol const & sym,
                  bfd_vma offset, bool anon_obj)
{
	char const * function = "";
	char const * cfilename = "";
	unsigned int linenr = 0;
	linenr_info info;
	bfd * abfd;
	asymbol ** syms;
	asection * section;
	bfd_vma pc;
	bool ret;

	if (!b.valid())
		goto fail;

	// take care about artificial symbol
	if (!sym.symbol())
		goto fail;

	abfd = b.abfd;
	syms = b.syms.get();
	if (!syms)
		goto fail;
	section = sym.symbol()->section;
	if (anon_obj)
		pc = offset - sym.symbol()->section->vma;
	else
		pc = (sym.value() + offset) - sym.filepos();

	if ((bfd_get_section_flags(abfd, section) & SEC_ALLOC) == 0)
		goto fail;

	if (pc >= bfd_section_size(abfd, section))
		goto fail;

	ret = bfd_find_nearest_line(abfd, section, syms, pc, &cfilename,
	                                 &function, &linenr);

	if (!ret || !cfilename || !function)
		goto fail;

	/*
	 * is_correct_function does not handle the case of static inlines,
	 * but if the linenr is non-zero in the inline case, it is the correct
	 * line number.
	 */
	if (linenr == 0 && !is_correct_function(function, sym.name()))
		goto fail;

	if (linenr == 0) {
		fixup_linenr(abfd, section, syms, sym.name(), pc, &cfilename,
		             &linenr);
	}

	info.found = true;
	info.filename = cfilename;
	info.line = linenr;
	return info;

fail:
	info.found = false;
	// some stl lacks string::clear()
	info.filename.erase(info.filename.begin(), info.filename.end());
	info.line = 0;
	return info;
}
