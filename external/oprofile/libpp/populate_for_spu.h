/**
 * @file libpp/populate_for_spu.h
 * Fill up a profile_container from inverted profiles for
 * a Cell BE SPU profile
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Maynard Johnson
 * (C) Copyright IBM Corporation 2007
 */

#ifndef POPULATE_FOR_SPU_H
#define POPULATE_FOR_SPU_H

class profile_container;
class inverted_profile;
class string_filter;

/*
 * When profiling SPUs on Cell Broadband Engine, all sample file
 * headers get a flag set indicating "spu_profile".  This function
 * checks the first sample file for this indicator.
 */
bool is_spu_profile(inverted_profile const & ip);

/*
 * This is a special-purpose function for CELL BE SPU profiling.
 * See populate_spu_profile_from_files prologue for more details.
 */
void populate_for_spu_image(profile_container & samples,
			    inverted_profile const & ip,
			    string_filter const & symbol_filter,
			    bool * has_debug_info);

enum profile_type {
	unknown_profile = -1,
	normal_profile,
	cell_spu_profile
};

#endif /* POPULATE_FOR_SPU_H */
