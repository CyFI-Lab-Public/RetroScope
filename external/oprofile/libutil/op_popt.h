/**
 * @file op_popt.h
 * Wrapper for libpopt - always use this rather
 * than popt.h
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_POPT_H
#define OP_POPT_H

#include <popt.h>

// not in some versions of popt.h
#ifndef POPT_TABLEEND
#define POPT_TABLEEND { NULL, '\0', 0, 0, 0, NULL, NULL }
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * op_poptGetContext - wrapper for popt
 *
 * Use this instead of poptGetContext to cope with
 * different popt versions. This also handle unrecognized
 * options. All error are fatal.
 */
poptContext op_poptGetContext(char const * name,
                int argc, char const ** argv,
                struct poptOption const * options, int flags);

#ifdef __cplusplus
}
#endif

#endif /* OP_POPT_H */
