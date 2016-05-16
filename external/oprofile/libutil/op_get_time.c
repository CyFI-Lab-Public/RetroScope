/**
 * @file op_get_time.c
 * Get current time as a string
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_get_time.h"

#include <time.h>

char * op_get_time(void)
{
	time_t t = time(NULL);

	if (t == -1)
		return "";

	return ctime(&t);
}
