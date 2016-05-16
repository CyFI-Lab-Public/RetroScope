/* Definitions for the Linux module syscall interface.
   Copyright 1996, 1997 Linux International.

   Contributed by Richard Henderson <rth@tamu.edu>

   This file is part of the Linux modutils.

   This program is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at your
   option) any later version.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.  */

#ifndef MODUTILS_MODULE_H
#define MODUTILS_MODULE_H 1

/* This file contains the structures used by the 2.0 and 2.1 kernels.
   We do not use the kernel headers directly because we do not wish
   to be dependant on a particular kernel version to compile insmod.  */


/*======================================================================*/
/* The structures used by Linux 2.0.  */

/* The symbol format used by get_kernel_syms(2).  */
struct old_kernel_sym
{
  unsigned long value;
  char name[60];
};

struct old_module_ref
{
  unsigned long module;		/* kernel addresses */
  unsigned long next;
};

struct old_module_symbol
{
  unsigned long addr;
  unsigned long name;
};

struct old_symbol_table
{
  int size;			/* total, including string table!!! */
  int n_symbols;
  int n_refs;
  struct old_module_symbol symbol[0]; /* actual size defined by n_symbols */
  struct old_module_ref ref[0];	/* actual size defined by n_refs */
};

struct old_mod_routines
{
  unsigned long init;
  unsigned long cleanup;
};

struct old_module
{
  unsigned long next;
  unsigned long ref;		/* the list of modules that refer to me */
  unsigned long symtab;
  unsigned long name;
  int size;			/* size of module in pages */
  unsigned long addr;		/* address of module */
  int state;
  unsigned long cleanup;	/* cleanup routine */
};

/* Sent to init_module(2) or'ed into the code size parameter.  */
#define OLD_MOD_AUTOCLEAN 0x40000000 /* big enough, but no sign problems... */

int get_kernel_syms(struct old_kernel_sym *);
int old_sys_init_module(char const * name, char *code, unsigned codesize,
			struct old_mod_routines *, struct old_symbol_table *);

/*======================================================================*/
/* For sizeof() which are related to the module platform and not to the
   environment isnmod is running in, use sizeof_xx instead of sizeof(xx).  */

#define tgt_sizeof_char		sizeof(char)
#define tgt_sizeof_short	sizeof(short)
#define tgt_sizeof_int		sizeof(int)
#define tgt_sizeof_long		sizeof(long)
#define tgt_sizeof_char_p	sizeof(char *)
#define tgt_sizeof_void_p	sizeof(void *)
#define tgt_long		long
#define tgt_long_fmt		"l"

/* This assumes that long long on a 32 bit system is equivalent to long on the
 * equivalent 64 bit system.  Also that void and char pointers are 8 bytes on
 * all 64 bit systems.  Add per system tweaks if it ever becomes necessary.
 */
#if defined(COMMON_3264) && defined(ONLY_64)
#undef tgt_long
#undef tgt_long_fmt
#undef tgt_sizeof_long
#undef tgt_sizeof_char_p
#undef tgt_sizeof_void_p
#define tgt_long                long long
#define tgt_long_fmt		"ll"
#define tgt_sizeof_long         8
#define tgt_sizeof_char_p       8
#define tgt_sizeof_void_p       8
#endif

/*======================================================================*/
/* The structures used in Linux 2.1 onwards.  */

/* Note: module_symbol does not use tgt_long intentionally */
struct module_symbol
{
  unsigned long value;
  unsigned long name;
};

struct module_ref
{
  unsigned tgt_long dep;		/* kernel addresses */
  unsigned tgt_long ref;
  unsigned tgt_long next_ref;
};

struct module
{
  unsigned tgt_long size_of_struct;	/* == sizeof(module) */
  unsigned tgt_long next;
  unsigned tgt_long name;
  unsigned tgt_long size;

  tgt_long usecount;
  unsigned tgt_long flags;		/* AUTOCLEAN et al */

  unsigned nsyms;
  unsigned ndeps;

  unsigned tgt_long syms;
  unsigned tgt_long deps;
  unsigned tgt_long refs;
  unsigned tgt_long init;
  unsigned tgt_long cleanup;
  unsigned tgt_long ex_table_start;
  unsigned tgt_long ex_table_end;
#ifdef __alpha__
  unsigned tgt_long gp;
#endif
  /* Everything after here is extension.  */
  unsigned tgt_long read_start;		/* Read data from existing module */
  unsigned tgt_long read_end;
  unsigned tgt_long can_unload;
  unsigned tgt_long runsize;
  unsigned tgt_long kallsyms_start;
  unsigned tgt_long kallsyms_end;
  unsigned tgt_long archdata_start;
  unsigned tgt_long archdata_end;
  unsigned tgt_long kernel_data;
};

struct module_info
{
  unsigned long addr;
  unsigned long size;
  unsigned long flags;
	   long usecount;
};

/* Bits of module.flags.  */
#define NEW_MOD_RUNNING		1
#define NEW_MOD_DELETED		2
#define NEW_MOD_AUTOCLEAN	4
#define NEW_MOD_VISITED		8
#define NEW_MOD_USED_ONCE	16
#define NEW_MOD_INITIALIZING	64

int sys_init_module(char const * name, const struct module *);
int query_module(char const * name, int which, void *buf, size_t bufsize,
		 size_t *ret);

/* Values for query_module's which.  */

#define QM_MODULES	1
#define QM_DEPS		2
#define QM_REFS		3
#define QM_SYMBOLS	4
#define QM_INFO		5

/*======================================================================*/
/* The system calls unchanged between 2.0 and 2.1.  */

unsigned long create_module(const char *, size_t);
int delete_module(const char *);

/* In safe mode the last parameter is forced to be a module name and meta
 * expansion is not allowed on that name.
 */
extern unsigned int safemode;

#endif /* module.h */
