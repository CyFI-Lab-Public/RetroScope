/**
 * @file op_popt.c
 * Wrapper for libpopt - always use this rather
 * than popt.h
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include "op_libiberty.h"
#include "op_popt.h"

poptContext op_poptGetContext(char const * name,
		int argc, char const ** argv,
		struct poptOption const * options, int flags)
{
	poptContext optcon;
	int c;

	xmalloc_set_program_name(argv[0]);

#ifdef CONST_POPT
	optcon = poptGetContext(name, argc, argv, options, flags);
#else
	optcon = poptGetContext((char *)name, argc, (char **)argv, options, flags);
#endif

	c = poptGetNextOpt(optcon);

	if (c < -1) {
		fprintf(stderr, "%s: %s: %s\n", argv[0],
			poptBadOption(optcon, POPT_BADOPTION_NOALIAS),
			poptStrerror(c));
		poptPrintHelp(optcon, stderr, 0);
		exit(EXIT_FAILURE);
	}

	return optcon;
}
