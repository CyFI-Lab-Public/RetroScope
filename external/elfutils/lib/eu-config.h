/* Configuration definitions.
   Copyright (C) 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under an Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) and to distribute linked
   combinations including the two.  Non-GPL Code permitted under this
   exception must only link to the code of Red Hat elfutils through those
   well defined interfaces identified in the file named EXCEPTION found in
   the source code files (the "Approved Interfaces").  The files of Non-GPL
   Code may instantiate templates or use macros or inline functions from
   the Approved Interfaces without causing the resulting work to be covered
   by the GNU General Public License.  Only Red Hat, Inc. may make changes
   or additions to the list of Approved Interfaces.  Red Hat's grant of
   this exception is conditioned upon your not adding any new exceptions.
   If you wish to add a new Approved Interface or exception, please contact
   Red Hat.  You must obey the GNU General Public License in all respects
   for all of the Red Hat elfutils code and other code used in conjunction
   with Red Hat elfutils except the Non-GPL Code covered by this exception.
   If you modify this file, you may extend this exception to your version
   of the file, but you are not obligated to do so.  If you do not wish to
   provide this exception without modification, you must delete this
   exception statement from your version and license this file solely under
   the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifdef USE_TLS
# include <pthread.h>
# include <assert.h>
# define tls_key_t			__thread void *
# define key_create(keyp, freefct)	(1)
# define getspecific(key)		key
# define setspecific(key,val)		key = val
# define once_define(class,name)	class struct { } name
# define once_execute(name,fct)		((void) &name, (void) (fct))
# define rwlock_define(class,name)	class pthread_rwlock_t name
# define RWLOCK_CALL(call)		\
  ({ int _err = pthread_rwlock_ ## call; assert_perror (_err); })
# define rwlock_init(lock)		RWLOCK_CALL (init (&lock, NULL))
# define rwlock_fini(lock)		RWLOCK_CALL (destroy (&lock))
# define rwlock_rdlock(lock)		RWLOCK_CALL (rdlock (&lock))
# define rwlock_wrlock(lock)		RWLOCK_CALL (wrlock (&lock))
# define rwlock_unlock(lock)		RWLOCK_CALL (unlock (&lock))
#else
/* Eventually we will allow multi-threaded applications to use the
   libraries.  Therefore we will add the necessary locking although
   the macros used expand to nothing for now.  */
# define lock_lock(lock) ((void) (lock))
# define rwlock_define(class,name) class int name
# define rwlock_init(lock) ((void) (lock))
# define rwlock_fini(lock) ((void) (lock))
# define rwlock_rdlock(lock) ((void) (lock))
# define rwlock_wrlock(lock) ((void) (lock))
# define rwlock_unlock(lock) ((void) (lock))
# define tls_key_t void *
# define key_create(keyp, freefct) (1)
# define getspecific(key) key
# define setspecific(key,val) key = val
# define once_define(class,name) class int name
# define once_execute(name,fct) \
  do {									      \
    if (name == 0)							      \
      fct ();								      \
    name = 1;								      \
  } while (0)
#endif	/* USE_TLS */

/* gettext helper macro.  */
#define N_(Str) Str

/* Compiler-specific definitions.  */
#define strong_alias(name, aliasname) \
  extern __typeof (name) aliasname __attribute__ ((alias (#name)));

#ifdef __i386__
# define internal_function __attribute__ ((regparm (3), stdcall))
#else
# define internal_function /* nothing */
#endif

#define internal_strong_alias(name, aliasname) \
  extern __typeof (name) aliasname __attribute__ ((alias (#name))) internal_function;

#define attribute_hidden \
  __attribute__ ((visibility ("hidden")))

/* Define ALLOW_UNALIGNED if the architecture allows operations on
   unaligned memory locations.  */
#if defined __i386__ || defined __x86_64__
# define ALLOW_UNALIGNED	1
#else
# define ALLOW_UNALIGNED	0
#endif

#if DEBUGPRED
# ifdef __x86_64__
asm (".section predict_data, \"aw\"; .previous\n"
     ".section predict_line, \"a\"; .previous\n"
     ".section predict_file, \"a\"; .previous");
#  ifndef PIC
#   define debugpred__(e, E) \
  ({ long int _e = !!(e); \
     asm volatile (".pushsection predict_data; ..predictcnt%=: .quad 0; .quad 0\n" \
                   ".section predict_line; .quad %c1\n" \
                   ".section predict_file; .quad %c2; .popsection\n" \
                   "addq $1,..predictcnt%=(,%0,8)" \
                   : : "r" (_e == E), "i" (__LINE__), "i" (__FILE__)); \
    __builtin_expect (_e, E); \
  })
#  endif
# elif defined __i386__
asm (".section predict_data, \"aw\"; .previous\n"
     ".section predict_line, \"a\"; .previous\n"
     ".section predict_file, \"a\"; .previous");
#  ifndef PIC
#   define debugpred__(e, E) \
  ({ long int _e = !!(e); \
     asm volatile (".pushsection predict_data; ..predictcnt%=: .long 0; .long 0\n" \
                   ".section predict_line; .long %c1\n" \
                   ".section predict_file; .long %c2; .popsection\n" \
                   "incl ..predictcnt%=(,%0,8)" \
                   : : "r" (_e == E), "i" (__LINE__), "i" (__FILE__)); \
    __builtin_expect (_e, E); \
  })
#  endif
# endif
# ifdef debugpred__
#  define unlikely(e) debugpred__ (e,0)
#  define likely(e) debugpred__ (e,1)
# endif
#endif
#ifndef likely
# define unlikely(expr) __builtin_expect (!!(expr), 0)
# define likely(expr) __builtin_expect (!!(expr), 1)
#endif

#define obstack_calloc(ob, size) \
  ({ size_t _s = (size); memset (obstack_alloc (ob, _s), '\0', _s); })
#define obstack_strdup(ob, str) \
  ({ const char *_s = (str); obstack_copy0 (ob, _s, strlen (_s)); })
#define obstack_strndup(ob, str, n) \
  ({ const char *_s = (str); obstack_copy0 (ob, _s, strnlen (_s, n)); })

#if __STDC_VERSION__ >= 199901L
# define flexarr_size /* empty */
#else
# define flexarr_size 0
#endif

/* Calling conventions.  */
#ifdef __i386__
# define CALLING_CONVENTION regparm (3), stdcall
# define AND_CALLING_CONVENTION , regparm (3), stdcall
#else
# define CALLING_CONVENTION
# define AND_CALLING_CONVENTION
#endif

/* Avoid PLT entries.  */
#ifdef PIC
# define INTUSE(name) _INTUSE(name)
# define _INTUSE(name) __##name##_internal
# define INTDEF(name) _INTDEF(name)
# define _INTDEF(name) \
  extern __typeof__ (name) __##name##_internal __attribute__ ((alias (#name)));
# define INTDECL(name) _INTDECL(name)
# define _INTDECL(name) \
  extern __typeof__ (name) __##name##_internal attribute_hidden;
#else
# define INTUSE(name) name
# define INTDEF(name) /* empty */
# define INTDECL(name) /* empty */
#endif

/* This macro is used by the tests conditionalize for standalone building.  */
#define ELFUTILS_HEADER(name) <lib##name.h>
