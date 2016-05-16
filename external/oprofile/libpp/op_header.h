/**
 * @file op_header.h
 * various free function acting on a sample file header
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_HEADER_H
#define OP_HEADER_H

#include <iosfwd>
#include <string>

#include "op_sample_file.h"

/**
 * @param h1 sample file header
 * @param h2 sample file header
 * @param filename sample filename
 *
 * check that the h1 and h2 are coherent (same size, same mtime etc.)
 * all error are fatal
 */
void op_check_header(opd_header const & h1, opd_header const & h2,
                     std::string const & filename);

bool is_jit_sample(std::string const & filename);

/**
 * check mtime of samples file header against file
 * all error are fatal
 */
void check_mtime(std::string const & file, opd_header const & header);

/**
 * @param sample_filename  the sample to open
 *
 * Return the header of this sample file. Only the magic number is checked
 * the version number is not checked. All error are fatal
 */
opd_header const read_header(std::string const & sample_filename);

/**
 * output a readable form of header, this don't include the cpu type
 * and speed
 */
std::string const describe_header(opd_header const & header);

/// output a readable form of cpu type and speed
std::string const describe_cpu(opd_header const & header);

#endif // OP_HEADER_H
