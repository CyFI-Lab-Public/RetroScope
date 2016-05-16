/**
 * @file op_libiberty.h
 * Wrapper for libiberty - always use this instead of
 * libiberty.h
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_LIBIBERTY_H
#define OP_LIBIBERTY_H

#include <stddef.h>

#include "config.h"

#ifdef MALLOC_ATTRIBUTE_OK
#define OP_ATTRIB_MALLOC	__attribute__((malloc))
#else
#define OP_ATTRIB_MALLOC
#endif

#ifdef HAVE_LIBIBERTY_H
#include <libiberty.h>
#else

#ifdef __cplusplus
extern "C" {
#endif

/* some system have a libiberty.a but no libiberty.h so we must provide
 * ourself the missing proto */
#ifndef HAVE_LIBIBERTY_H
/* Set the program name used by xmalloc.  */
void xmalloc_set_program_name(char const *);

/* Allocate memory without fail.  If malloc fails, this will print a
   message to stderr (using the name set by xmalloc_set_program_name,
   if any) and then call xexit.  */
void * xmalloc(size_t) OP_ATTRIB_MALLOC;

/* Reallocate memory without fail.  This works like xmalloc.  Note,
   realloc type functions are not suitable for attribute malloc since
   they may return the same address across multiple calls. */
void * xrealloc(void *, size_t);

/* Allocate memory without fail and set it to zero.  This works like xmalloc */
void * xcalloc(size_t, size_t) OP_ATTRIB_MALLOC;

/* Copy a string into a memory buffer without fail.  */
char * xstrdup(char const *) OP_ATTRIB_MALLOC;

/**
 * Duplicates a region of memory without fail.  First, alloc_size bytes
 * are allocated, then copy_size bytes from input are copied into
 * it, and the new memory is returned.  If fewer bytes are copied than were
 * allocated, the remaining memory is zeroed.
 */
void * xmemdup(void const *, size_t, size_t) OP_ATTRIB_MALLOC;

#endif	/* !HAVE_LIBIBERTY_H */

#ifdef ANDROID
#define xmalloc(s)      malloc(s)
#define xrealloc(p,s)   realloc(p,s)
#define xstrdup(str)    strdup(str)
#define xmalloc_set_program_name(n)
#endif

#ifdef __cplusplus
}
#endif

#endif /* !HAVE_LIBIBERTY_H */

#endif /* OP_LIBIBERTY_H */
