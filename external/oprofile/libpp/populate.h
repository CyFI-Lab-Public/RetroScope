/**
 * @file populate.h
 * Fill up a profile_container from inverted profiles
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef POPULATE_H
#define POPULATE_H

class profile_container;
class inverted_profile;
class string_filter;


/// Load all sample file information for exactly one binary image.
void
populate_for_image(profile_container & samples, inverted_profile const & ip,
   string_filter const & symbol_filter, bool * has_debug_info);

#endif /* POPULATE_H */
