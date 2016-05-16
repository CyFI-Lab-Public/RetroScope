/**
 * @file profile.h
 * Encapsulation for samples files over all profile classes
 * belonging to the same binary image
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef PROFILE_H
#define PROFILE_H

#include <string>
#include <map>
#include <iterator>

#include "odb.h"
#include "op_types.h"
#include "utility.h"
#include "populate_for_spu.h"

class opd_header;
class op_bfd;

/**
 * Class containing a single sample file contents.
 * i.e. set of count values for VMA offsets for
 * a particular binary.
 */
class profile_t : noncopyable {
public:
	/**
	 * profile_t - construct an empty  profile_t object
	 */
	profile_t();

	/// return true if no sample file has been loaded
	bool empty() const { return !file_header.get(); }
 
	/// return the header of the last opened samples file
	opd_header const & get_header() const {
		return *file_header;
	}

	/**
	 * count samples count w/o recording them
	 * @param filename sample filename
	 *
	 * convenience interface for raw access to sample count w/o recording
	 * them. It's placed here so all access to samples files go through
	 * profile_t static or non static member.
	 */
	static count_type sample_count(std::string const & filename);

	/**
	 * Indicate if given sample file is from a Cell Broadband Engine
	 * SPU profile
	 * @param filename sample filename
	 *
	 * Convenience interface put here so all access to samples files
	 * go through profile_t static or non static member.
	 */
	static enum profile_type is_spu_sample_file(std::string const & filename);

	/**
	 * cumulate sample file to our container of samples
	 * @param filename  sample file name
	 *
	 * store samples for one sample file, sample file header is sanitized.
	 *
	 * all error are fatal
	 */
	void add_sample_file(std::string const & filename);

	/// Set an appropriate start offset, see comments below.
	void set_offset(op_bfd const & abfd);

	class const_iterator;
	typedef std::pair<const_iterator, const_iterator> iterator_pair;

	/**
	 * @param start  start offset
	 * @param end  end offset
	 *
	 * return an iterator pair to [start, end) range
	 */
	iterator_pair
	samples_range(odb_key_t start, odb_key_t end) const;

	/// return a pair of iterator for all samples
	iterator_pair samples_range() const;

private:
	/// helper for sample_count() and add_sample_file(). All error launch
	/// an exception.
	static void
	open_sample_file(std::string const & filename, odb_t &);

	/// copy of the samples file header
	scoped_ptr<opd_header> file_header;

	/// storage type for samples sorted by eip
	typedef std::map<odb_key_t, count_type> ordered_samples_t;

	/**
	 * Samples are stored in hash table, iterating over hash table don't
	 * provide any ordering, the above count() interface rely on samples
	 * ordered by eip. This map is only a temporary storage where samples
	 * are ordered by eip.
	 */
	ordered_samples_t ordered_samples;

	/**
	 * For certain profiles, such as kernel/modules, and anon
	 * regions with a matching binary, this value is non-zero,
	 * and represents the file offset of the relevant section.
	 *
	 * For kernel profiles, this is done because we use the information
	 * provided in /proc/ksyms, which only gives the mapped position of
	 * .text, and the symbol _text from vmlinux. This value is used to fix
	 * up the sample offsets for kernel code as a result of this difference
	 *
	 * In user-space samples, the sample offset is from the start of the
	 * mapped file, as seen in /proc/pid/maps. This is fine for
	 * mappings of permanent files, but with anon mappings, we need
	 * to adjust the key values to be a file offset against the
	 * *binary* (if there is one). This can obviously be different.
	 * So we pass our anon mapping start VMA to op_bfd, which looks
	 * for a section with that VMA, then returns the section's
	 * filepos. So all is good.
	 *
	 * Finally, note that for cg we can't use this inside the
	 * profile_t, as we're storing two offsets in the key value. So
	 * we do it later in that case.
	 *
	 * Phew.
	 */
	u64 start_offset;
};


// It will be easier to derive profile_t::const_iterator from
// std::iterator<std::input_iterator_tag, unsigned int> but this doesn't
// work for gcc <= 2.95 so we provide the neccessary typedef in the hard way.
// See ISO C++ 17.4.3.1 § 1 and 14.7.3 § 9.
namespace std {
	template <>
		struct iterator_traits<profile_t::const_iterator> {
			typedef ptrdiff_t difference_type;
			typedef count_type value_type;
			typedef count_type * pointer;
			typedef count_type & reference;
			typedef input_iterator_tag iterator_category;
		};
}


class profile_t::const_iterator
{
	typedef ordered_samples_t::const_iterator iterator_t;
public:
	const_iterator() : start_offset(0) {}
	const_iterator(iterator_t it_, u64 start_offset_)
		: it(it_), start_offset(start_offset_) {}

	count_type operator*() const { return it->second; }
	const_iterator & operator++() { ++it; return *this; }

	odb_key_t vma() const { return it->first + start_offset; }
	count_type count() const { return **this; }

	bool operator!=(const_iterator const & rhs) const {
		return it != rhs.it;
	}
	bool operator==(const_iterator const & rhs) const {
		return it == rhs.it;
	}

private:
	iterator_t it;
	u64 start_offset;
};

#endif /* !PROFILE_H */
