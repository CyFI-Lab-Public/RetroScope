/**
 * @file xml_output.h
 * utility routines for writing XML
 *
 * @remark Copyright 2006 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#ifndef XML_OUTPUT_H
#define XML_OUTPUT_H
#include "op_xml_out.h"

std::string tag_name(tag_t tag);
std::string open_element(tag_t tag, bool with_attrs = false);
std::string close_element(tag_t tag = NONE, bool has_nested = false);
std::string init_attr(tag_t attr, size_t value);
std::string init_attr(tag_t attr, double value);
std::string init_attr(tag_t attr, std::string const & str);

#endif /* XML_OUTPUT_H */
