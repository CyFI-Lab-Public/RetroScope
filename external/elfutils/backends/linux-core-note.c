/* Common core note type descriptions for Linux.
   Copyright (C) 2007, 2008 Red Hat, Inc.
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

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

/* The including CPU_corenote.c file provides prstatus_regs and
   defines macros ULONG, [PUG]ID_T, and ALIGN_*, TYPE_*.

   Here we describe the common layout used in <linux/elfcore.h>.  */

#define	CHAR			int8_t
#define	ALIGN_CHAR		1
#define	TYPE_CHAR		ELF_T_BYTE
#define	SHORT			uint16_t
#define ALIGN_SHORT		2
#define TYPE_SHORT		ELF_T_HALF
#define	INT			int32_t
#define ALIGN_INT		4
#define TYPE_INT		ELF_T_SWORD

#define FIELD(type, name) type name __attribute__ ((aligned (ALIGN_##type)))

struct EBLHOOK(siginfo)
{
  FIELD (INT, si_signo);
  FIELD (INT, si_code);
  FIELD (INT, si_errno);
};

struct EBLHOOK(timeval)
{
  FIELD (ULONG, tv_sec);
  FIELD (ULONG, tv_usec);
};

/* On sparc64, tv_usec (suseconds_t) is actually 32 bits with 32 bits padding.
   The 'T'|0x80 value for .format indicates this as a special kludge.  */
#if SUSECONDS_HALF
# define TIMEVAL_FIELD(name)	FIELD (time, ULONG, name, 'T'|0x80, .count = 2)
#else
# define TIMEVAL_FIELD(name)	FIELD (time, ULONG, name, 'T', .count = 2)
#endif


struct EBLHOOK(prstatus)
{
  struct EBLHOOK(siginfo) pr_info;
  FIELD (SHORT, pr_cursig);
  FIELD (ULONG, pr_sigpend);
  FIELD (ULONG, pr_sighold);
  FIELD (PID_T, pr_pid);
  FIELD (PID_T, pr_ppid);
  FIELD (PID_T, pr_pgrp);
  FIELD (PID_T, pr_sid);
  struct EBLHOOK(timeval) pr_utime;
  struct EBLHOOK(timeval) pr_stime;
  struct EBLHOOK(timeval) pr_cutime;
  struct EBLHOOK(timeval) pr_cstime;
  FIELD (ULONG, pr_reg[PRSTATUS_REGS_SIZE / sizeof (ULONG)]);
  FIELD (INT, pr_fpvalid);
};

#define	FNAMESZ	16
#define	PRARGSZ	80

struct EBLHOOK(prpsinfo)
{
  FIELD (CHAR, pr_state);
  FIELD (CHAR, pr_sname);
  FIELD (CHAR, pr_zomb);
  FIELD (CHAR, pr_nice);
  FIELD (ULONG, pr_flag);
  FIELD (UID_T, pr_uid);
  FIELD (GID_T, pr_gid);
  FIELD (PID_T, pr_pid);
  FIELD (PID_T, pr_ppid);
  FIELD (PID_T, pr_pgrp);
  FIELD (PID_T, pr_sid);
  FIELD (CHAR, pr_fname[FNAMESZ]);
  FIELD (CHAR, pr_psargs[PRARGSZ]);
};

#undef	FIELD

#define FIELD(igroup, itype, item, fmt, ...)			\
    {								\
      .name = #item,						\
      .group = #igroup,					\
      .offset = offsetof (struct EBLHOOK(prstatus), pr_##item),	\
      .type = TYPE_##itype,					\
      .format = fmt,						\
      __VA_ARGS__						\
    }

static const Ebl_Core_Item prstatus_items[] =
  {
    FIELD (signal, INT, info.si_signo, 'd'),
    FIELD (signal, INT, info.si_code, 'd'),
    FIELD (signal, INT, info.si_errno, 'd'),
    FIELD (signal, SHORT, cursig, 'd'),
    FIELD (signal, ULONG, sigpend, 'B'),
    FIELD (signal, ULONG, sighold, 'B'),
    FIELD (identity, PID_T, pid, 'd', .thread_identifier = true),
    FIELD (identity, PID_T, ppid, 'd'),
    FIELD (identity, PID_T, pgrp, 'd'),
    FIELD (identity, PID_T, sid, 'd'),
    TIMEVAL_FIELD (utime),
    TIMEVAL_FIELD (stime),
    TIMEVAL_FIELD (cutime),
    TIMEVAL_FIELD (cstime),
#ifdef PRSTATUS_REGSET_ITEMS
    PRSTATUS_REGSET_ITEMS,
#endif
    FIELD (register, INT, fpvalid, 'd'),
  };

#undef	FIELD

#define FIELD(igroup, itype, item, fmt, ...)			\
    {								\
      .name = #item,						\
      .group = #igroup,					\
      .offset = offsetof (struct EBLHOOK(prpsinfo), pr_##item),	\
      .type = TYPE_##itype,					\
      .format = fmt,						\
      __VA_ARGS__						\
    }

static const Ebl_Core_Item prpsinfo_items[] =
  {
    FIELD (state, CHAR, state, 'd'),
    FIELD (state, CHAR, sname, 'c'),
    FIELD (state, CHAR, zomb, 'd'),
    FIELD (state, CHAR, nice, 'd'),
    FIELD (state, ULONG, flag, 'x'),
    FIELD (identity, UID_T, uid, 'd'),
    FIELD (identity, GID_T, gid, 'd'),
    FIELD (identity, PID_T, pid, 'd'),
    FIELD (identity, PID_T, ppid, 'd'),
    FIELD (identity, PID_T, pgrp, 'd'),
    FIELD (identity, PID_T, sid, 'd'),
    FIELD (command, CHAR, fname, 's', .count = FNAMESZ),
    FIELD (command, CHAR, psargs, 's', .count = PRARGSZ),
  };

#undef	FIELD

int
EBLHOOK(core_note) (n_type, descsz,
		    regs_offset, nregloc, reglocs, nitems, items)
     GElf_Word n_type;
     GElf_Word descsz;
     GElf_Word *regs_offset;
     size_t *nregloc;
     const Ebl_Register_Location **reglocs;
     size_t *nitems;
     const Ebl_Core_Item **items;
{
  switch (n_type)
    {
    case NT_PRSTATUS:
      if (descsz != sizeof (struct EBLHOOK(prstatus)))
	return 0;
      *regs_offset = offsetof (struct EBLHOOK(prstatus), pr_reg);
      *nregloc = sizeof prstatus_regs / sizeof prstatus_regs[0];
      *reglocs = prstatus_regs;
      *nitems = sizeof prstatus_items / sizeof prstatus_items[0];
      *items = prstatus_items;
      return 1;

    case NT_PRPSINFO:
      if (descsz != sizeof (struct EBLHOOK(prpsinfo)))
	return 0;
      *regs_offset = 0;
      *nregloc = 0;
      *reglocs = NULL;
      *nitems = sizeof prpsinfo_items / sizeof prpsinfo_items[0];
      *items = prpsinfo_items;
      return 1;

#define EXTRA_REGSET(type, size, table)					      \
    case type:								      \
      if (descsz != size)						      \
	return 0;							      \
      *regs_offset = 0;							      \
      *nregloc = sizeof table / sizeof table[0];			      \
      *reglocs = table;							      \
      *nitems = 0;							      \
      *items = NULL;							      \
      return 1;

#ifdef FPREGSET_SIZE
    EXTRA_REGSET (NT_FPREGSET, FPREGSET_SIZE, fpregset_regs)
#endif

#ifdef EXTRA_NOTES
    EXTRA_NOTES
#endif
    }

  return 0;
}
