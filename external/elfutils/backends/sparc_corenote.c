/* PowerPC specific core note handling.
   Copyright (C) 2007 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <elf.h>
#include <inttypes.h>
#include <stddef.h>
#include <stdio.h>
#include <sys/time.h>

#ifndef BITS
# define BITS 		32
# define BACKEND	sparc_
#else
# define BITS 		64
# define BACKEND	sparc64_
#endif
#include "libebl_CPU.h"

#define GR(at, n, dwreg)						\
    { .offset = at * BITS/8, .regno = dwreg, .count = n, .bits = BITS }

static const Ebl_Register_Location prstatus_regs[] =
  {
    GR (0, 32, 0),		/* %g0-%g7, %o0-%o7, %i0-%i7 */
#if BITS == 32
    GR (32, 1, 65),		/* %psr */
    GR (33, 2, 68),		/* %pc, %npc */
    GR (35, 1, 64),		/* %y */
    GR (36, 1, 66),		/* %wim, %tbr */
#else
    GR (32, 1, 82),		/* %state */
    GR (33, 2, 80),		/* %pc, %npc */
    GR (35, 1, 85),		/* %y */
#endif
  };
#define PRSTATUS_REGS_SIZE	(BITS / 8 * (32 + (BITS == 32 ? 6 : 4)))

static const Ebl_Register_Location fpregset_regs[] =
  {
#if BITS == 32
    GR (0, 32, 32),		/* %f0-%f31 */
    /* 				   padding word */
    GR (33, 1, 70),		/* %fsr */
    /* 	       			   qcnt, q_entrysize, en, q, padding */
# define FPREGSET_SIZE		(34 * 4 + 4 + 64 * 4 + 4)
#else
    GR (0, 32, 32),		/* %f0-%f31 */
    GR (32, 1, 83),		/* %fsr */
    /*  33, 1, 			   %gsr */
    GR (34, 1, 84),		/* %fprs */
# define FPREGSET_SIZE		(35 * 8)
#endif
  };

#if BITS == 32
# define ULONG			uint32_t
# define ALIGN_ULONG		4
# define TYPE_ULONG		ELF_T_WORD
# define TYPE_LONG		ELF_T_SWORD
# define UID_T			uint16_t
# define GID_T			uint16_t
# define ALIGN_UID_T		2
# define ALIGN_GID_T		2
# define TYPE_UID_T		ELF_T_HALF
# define TYPE_GID_T		ELF_T_HALF
#else
# define ULONG			uint64_t
# define ALIGN_ULONG		8
# define TYPE_ULONG		ELF_T_XWORD
# define TYPE_LONG		ELF_T_SXWORD
# define UID_T			uint32_t
# define GID_T			uint32_t
# define ALIGN_UID_T		4
# define ALIGN_GID_T		4
# define TYPE_UID_T		ELF_T_WORD
# define TYPE_GID_T		ELF_T_WORD
# define SUSECONDS_HALF		1
#endif
#define PID_T			int32_t
#define ALIGN_PID_T		4
#define TYPE_PID_T		ELF_T_SWORD

#include "linux-core-note.c"
