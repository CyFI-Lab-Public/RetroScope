/* lookup_table.c -- 
 * Copyright 2009 Red Hat Inc., Durham, North Carolina.
 * All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Authors:
 *      Steve Grubb <sgrubb@redhat.com>
 */

#include "config.h"
#include <stddef.h>
#include <linux/capability.h>
#include <strings.h>


#ifndef CAP_LAST_CAP
#define CAP_LAST_CAP CAP_AUDIT_CONTROL
#endif
#undef cap_valid
#define cap_valid(x) ((x) <= CAP_LAST_CAP)


struct transtab {
    int   value;
    int   offset;
};

#define MSGSTRFIELD(line) MSGSTRFIELD1(line)
#define MSGSTRFIELD1(line) str##line


/* To create the following tables in a DSO-friendly way we split them in
   two separate variables: a long string which is created by concatenating
   all strings referenced in the table and the table itself, which uses
   offsets instead of string pointers.  To do this without increasing
   the maintenance burden we use a lot of preprocessor magic.  All the
   maintainer has to do is to add a new entry to the included file and
   recompile.  */

static const union captab_msgstr_t {
    struct {
#define _S(n, s) char MSGSTRFIELD(__LINE__)[sizeof (s)];
#include "captab.h"
#undef _S
    };
    char str[0];
} captab_msgstr = { {
#define _S(n, s) s,
#include "captab.h"
#undef _S
} };
static const struct transtab captab[] = {
#define _S(n, s) { n, offsetof(union captab_msgstr_t,  \
                               MSGSTRFIELD(__LINE__)) },
#include "captab.h"
#undef _S
};
#define CAP_NG_CAPABILITY_NAMES (sizeof(captab)/sizeof(captab[0]))




static int capng_lookup_name(const struct transtab *table,
		const char *tabstr, size_t length, const char *name)
{
	size_t i;
    
	for (i = 0; i < length; i++) {
		if (!strcasecmp(tabstr + table[i].offset, name))
			return table[i].value;
	}
	return -1;
}

static const char *capng_lookup_number(const struct transtab *table,
                                       const char *tabstr, size_t length,
                                       int number)
{
	size_t i;
    
	for (i = 0; i < length; i++) {
		if (table[i].value == number)
			return tabstr + table[i].offset;
	}
	return NULL;
}

int capng_name_to_capability(const char *name)
{
	return capng_lookup_name(captab, captab_msgstr.str,
                                 CAP_NG_CAPABILITY_NAMES, name);
}

const char *capng_capability_to_name(unsigned int capability)
{
	if (!cap_valid(capability))
		return NULL;

	return capng_lookup_number(captab, captab_msgstr.str,
                                   CAP_NG_CAPABILITY_NAMES, capability);
}

