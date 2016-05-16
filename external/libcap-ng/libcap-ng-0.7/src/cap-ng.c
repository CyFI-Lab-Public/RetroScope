/* libcap-ng.c --
 * Copyright 2009-10 Red Hat Inc., Durham, North Carolina.
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
#include "cap-ng.h"
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#if !defined(ANDROID)
#include <stdio_ext.h>
#endif
#include <stdlib.h>
#include <sys/prctl.h>
#include <grp.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <errno.h>
#include <byteswap.h>
#ifdef HAVE_SYSCALL_H
#include <sys/syscall.h>
#endif
#ifdef HAVE_LINUX_SECUREBITS_H
#include <linux/securebits.h>
#endif

/*
 * Some milestones of when things became available:
 * 2.6.24 kernel	XATTR_NAME_CAPS
 * 2.6.25 kernel	PR_CAPBSET_DROP, CAPABILITY_VERSION_2
 * 2.6.26 kernel	PR_SET_SECUREBITS, SECURE_*_LOCKED, VERSION_3
 */

/* External syscall prototypes */
extern int capset(cap_user_header_t header, cap_user_data_t data);
extern int capget(cap_user_header_t header, const cap_user_data_t data);

// Local defines
#define MASK(x) (1U << (x))
#ifdef PR_CAPBSET_DROP
#define UPPER_MASK ~(unsigned)((~0U)<<(CAP_LAST_CAP-31))
#else
// For v1 systems UPPER_MASK will never be used
#define UPPER_MASK (unsigned)(~0U)
#endif

// Re-define cap_valid so its uniform between V1 and V3
#undef cap_valid
#define cap_valid(x) ((x) <= CAP_LAST_CAP)

// If we don't have the xattr library, then we can't
// compile-in file system capabilities
#ifndef HAVE_ATTR_XATTR_H
#undef VFS_CAP_U32
#endif

#ifdef VFS_CAP_U32
 #include <attr/xattr.h>
 #if __BYTE_ORDER == __BIG_ENDIAN
  #define FIXUP(x) bswap_32(x)
 #else
  #define FIXUP(x) (x)
 #endif
#endif

#ifndef _LINUX_CAPABILITY_VERSION_1
#define _LINUX_CAPABILITY_VERSION_1 0x19980330
#endif
#ifndef _LINUX_CAPABILITY_VERSION_2
#define _LINUX_CAPABILITY_VERSION_2 0x20071026
#endif
#ifndef _LINUX_CAPABILITY_VERSION_3
#define _LINUX_CAPABILITY_VERSION_3 0x20080522
#endif

// This public API went private in the 2.6.36 kernel - hope it never changes
#ifndef XATTR_CAPS_SUFFIX
#define XATTR_CAPS_SUFFIX "capability"
#endif
#ifndef XATTR_SECURITY_PREFIX
#define XATTR_SECURITY_PREFIX "security."
#endif
#ifndef XATTR_NAME_CAPS
#define XATTR_NAME_CAPS XATTR_SECURITY_PREFIX XATTR_CAPS_SUFFIX
#endif


/* Child processes can't get caps back */
#ifndef SECURE_NOROOT
#define SECURE_NOROOT                   0
#endif
#ifndef SECURE_NOROOT_LOCKED
#define SECURE_NOROOT_LOCKED            1  /* make bit-0 immutable */
#endif
/* Setuid apps run by uid 0 don't get caps back */
#ifndef SECURE_NO_SETUID_FIXUP
#define SECURE_NO_SETUID_FIXUP          2  
#endif
#ifndef SECURE_NO_SETUID_FIXUP_LOCKED
#define SECURE_NO_SETUID_FIXUP_LOCKED   3  /* make bit-2 immutable */
#endif

// States: new, allocated, initted, updated, applied
typedef enum { CAPNG_NEW, CAPNG_ERROR, CAPNG_ALLOCATED, CAPNG_INIT,
	CAPNG_UPDATED, CAPNG_APPLIED } capng_states_t;

// Create an easy data struct out of the kernel definitions
typedef union {
	struct __user_cap_data_struct v1;
	struct __user_cap_data_struct v3[2];
} cap_data_t;

// This struct keeps all state info
struct cap_ng
{
	int cap_ver;
	struct __user_cap_header_struct hdr;
	cap_data_t data;
	capng_states_t state;
	__u32 bounds[2];
};

// Global variables with per thread uniqueness
static __thread struct cap_ng m =	{ 1,
					{0, 0},
					{ {0, 0, 0} },
					CAPNG_NEW,
					{0, 0} };


static void init(void)
{
	if (m.state != CAPNG_NEW)
		return;

	memset(&m.hdr, 0, sizeof(m.hdr));
	(void)capget(&m.hdr, NULL); // Returns -EINVAL
	if (m.hdr.version == _LINUX_CAPABILITY_VERSION_3 ||
		m.hdr.version == _LINUX_CAPABILITY_VERSION_2) {
		m.cap_ver = 3;
	} else if (m.hdr.version == _LINUX_CAPABILITY_VERSION_1) {
		m.cap_ver = 1;
	} else {
		m.state = CAPNG_ERROR;
		return;
	}

	memset(&m.data, 0, sizeof(cap_data_t));
#ifdef HAVE_SYSCALL_H
	m.hdr.pid = (unsigned)syscall(__NR_gettid);
#else
	m.hdr.pid = (unsigned)getpid();
#endif
	m.state = CAPNG_ALLOCATED;
}

void capng_clear(capng_select_t set)
{
	if (m.state == CAPNG_NEW)
		init();
	if (m.state == CAPNG_ERROR)
		return;

	if (set & CAPNG_SELECT_CAPS)
		memset(&m.data, 0, sizeof(cap_data_t));
#ifdef PR_CAPBSET_DROP
	if (set & CAPNG_SELECT_BOUNDS)
		memset(m.bounds, 0, sizeof(m.bounds));
#endif
	m.state = CAPNG_INIT;
}

void capng_fill(capng_select_t set)
{
	if (m.state == CAPNG_NEW)
		init();
	if (m.state == CAPNG_ERROR)
		return;

	if (set & CAPNG_SELECT_CAPS) {
		if (m.cap_ver == 1) {
			m.data.v1.effective = 0x7FFFFFFFU;
			m.data.v1.permitted = 0x7FFFFFFFU;
			m.data.v1.inheritable = 0;
		} else {
			m.data.v3[0].effective = 0xFFFFFFFFU;
			m.data.v3[0].permitted = 0xFFFFFFFFU;
			m.data.v3[0].inheritable = 0;
			m.data.v3[1].effective = 0xFFFFFFFFU;
			m.data.v3[1].permitted = 0xFFFFFFFFU;
			m.data.v3[1].inheritable = 0;
		}
	}
#ifdef PR_CAPBSET_DROP
	if (set & CAPNG_SELECT_BOUNDS) {
		unsigned i;
		for (i=0; i<sizeof(m.bounds)/sizeof(__u32); i++)
			m.bounds[i] = 0xFFFFFFFFU;
	}
#endif
	m.state = CAPNG_INIT;
}

void capng_setpid(int pid)
{
	if (m.state == CAPNG_NEW)
		init();
	if (m.state == CAPNG_ERROR)
		return;

	m.hdr.pid = pid;
}

#ifdef PR_CAPBSET_DROP
static int get_bounding_set(void)
{
	char buf[64];
	FILE *f;

	snprintf(buf, sizeof(buf), "/proc/%u/status", m.hdr.pid ? m.hdr.pid :
#ifdef HAVE_SYSCALL_H
		(unsigned)syscall(__NR_gettid));
#else
		(unsigned)getpid();
#endif
	f = fopen(buf, "re");
	if (f == NULL)
		return -1;
#if !defined(ANDROID)
	__fsetlocking(f, FSETLOCKING_BYCALLER);
#endif
	while (fgets(buf, sizeof(buf), f)) {
		if (strncmp(buf, "CapB", 4))
			continue;
		sscanf(buf, "CapBnd:  %08x%08x", &m.bounds[1], &m.bounds[0]);
		fclose(f);
		return 0;
	}
	fclose(f);
	return -1;
}
#endif

int capng_get_caps_process(void)
{
	int rc;

	if (m.state == CAPNG_NEW)
		init();
	if (m.state == CAPNG_ERROR)
		return -1;

	rc = capget((cap_user_header_t)&m.hdr, (cap_user_data_t)&m.data);
	if (rc == 0) {
		m.state = CAPNG_INIT;
#ifdef PR_CAPBSET_DROP
		rc = get_bounding_set();
		if (rc < 0)
			m.state = CAPNG_ERROR;
#endif
	}

	return rc;
}

#ifdef VFS_CAP_U32
static int load_data(const struct vfs_cap_data *filedata, int size)
{
	unsigned int magic;
	
	if (m.cap_ver == 1)
		return -1;	// Should never get here but just in case

	magic = FIXUP(filedata->magic_etc);
	switch (magic & VFS_CAP_REVISION_MASK)
	{
		case VFS_CAP_REVISION_1:
			m.cap_ver = 1;
			if (size != XATTR_CAPS_SZ_1)
				return -1;
			break;
		case VFS_CAP_REVISION_2:
			m.cap_ver = 2;
			if (size != XATTR_CAPS_SZ_2)
				return -1;
			break;
		default:
			return -1;
	}

	// Now stuff the data structures
	m.data.v3[0].permitted = FIXUP(filedata->data[0].permitted);
	m.data.v3[1].permitted = FIXUP(filedata->data[1].permitted);
	m.data.v3[0].inheritable = FIXUP(filedata->data[0].inheritable);
	m.data.v3[1].inheritable = FIXUP(filedata->data[1].inheritable);
	if (magic & VFS_CAP_FLAGS_EFFECTIVE) {
		m.data.v3[0].effective =
			m.data.v3[0].permitted | m.data.v3[0].inheritable;
		m.data.v3[1].effective =
			m.data.v3[1].permitted | m.data.v3[1].inheritable;
	} else {
		m.data.v3[0].effective = 0;
		m.data.v3[1].effective = 0;
	}
	return 0;
}
#endif

int capng_get_caps_fd(int fd)
{
#ifndef VFS_CAP_U32
	return -1;
#else
	int rc;
	struct vfs_cap_data filedata;

	if (m.state == CAPNG_NEW)
		init();
	if (m.state == CAPNG_ERROR)
		return -1;

	rc = fgetxattr(fd, XATTR_NAME_CAPS, &filedata, sizeof(filedata));
	if (rc <= 0)
		return -1;

	rc = load_data(&filedata, rc);
	if (rc == 0)
		m.state = CAPNG_INIT;

	return rc;
#endif
}

static void v1_update(capng_act_t action, unsigned int capability, __u32 *data)
{
	if (action == CAPNG_ADD)
		*data |= MASK(capability);
	else
		*data &= ~(MASK(capability));
}

static void update_effective(capng_act_t action, unsigned int capability,
	unsigned int idx)
{
	if (action == CAPNG_ADD)
		m.data.v3[idx].effective |= MASK(capability);
	else
		m.data.v3[idx].effective &= ~(MASK(capability));
}

static void update_permitted(capng_act_t action, unsigned int capability,
	unsigned int idx)
{
	if (action == CAPNG_ADD)
		m.data.v3[idx].permitted |= MASK(capability);
	else
		m.data.v3[idx].permitted &= ~(MASK(capability));
}

static void update_inheritable(capng_act_t action, unsigned int capability,
	unsigned int idx)
{
	if (action == CAPNG_ADD)
		m.data.v3[idx].inheritable |= MASK(capability);
	else
		m.data.v3[idx].inheritable &= ~(MASK(capability));
}

static void update_bounding_set(capng_act_t action, unsigned int capability,
	unsigned int idx)
{
#ifdef PR_CAPBSET_DROP
	if (action == CAPNG_ADD)
		m.bounds[idx] |= MASK(capability);
	else
		m.bounds[idx] &= ~(MASK(capability));
#endif
}

int capng_update(capng_act_t action, capng_type_t type, unsigned int capability)
{
	// Before updating, we expect that the data is initialized to something
	if (m.state < CAPNG_INIT)
		return -1;
	if (!cap_valid(capability)) {
		errno = EINVAL;
		return -1;
	}

	if (m.cap_ver == 1) {
		if (CAPNG_EFFECTIVE & type)
			v1_update(action, capability, &m.data.v1.effective);
		if (CAPNG_PERMITTED & type)
			v1_update(action, capability, &m.data.v1.permitted);
		if (CAPNG_INHERITABLE & type)
			v1_update(action, capability, &m.data.v1.inheritable);
	} else {
		int idx;

		if (capability > 31) {
			idx = capability>>5;
			capability %= 32;
		} else
			idx = 0;

		if (CAPNG_EFFECTIVE & type)
			update_effective(action, capability, idx);
		if (CAPNG_PERMITTED & type)
			update_permitted(action, capability, idx);
		if (CAPNG_INHERITABLE & type)
			update_inheritable(action, capability, idx);
		if (CAPNG_BOUNDING_SET & type)
			update_bounding_set(action, capability, idx);
	}

	m.state = CAPNG_UPDATED;
	return 0;
}

int capng_updatev(capng_act_t action, capng_type_t type,
                unsigned int capability, ...)
{
	int rc;
	unsigned int cap;
	va_list ap;

	rc = capng_update(action, type, capability);
	if (rc)
		return rc;
	va_start(ap, capability);
	cap = va_arg(ap, unsigned int);
	while (cap_valid(cap)) {
		rc = capng_update(action, type, cap);
		if (rc)
			break;
		cap = va_arg(ap, unsigned int);
	}
	va_end(ap);

	// See if planned exit or invalid
	if (cap == (unsigned)-1)
		rc = 0;
	else {
		rc = -1;
		errno = EINVAL;
	}

	return rc;
}

int capng_apply(capng_select_t set)
{
	int rc = -1;

	// Before updating, we expect that the data is initialized to something
	if (m.state < CAPNG_INIT)
		return -1;

	if (set & CAPNG_SELECT_BOUNDS) { 
#ifdef PR_CAPBSET_DROP
		void *s = capng_save_state();
		capng_get_caps_process();
		if (capng_have_capability(CAPNG_EFFECTIVE, CAP_SETPCAP)) {
			int i;
			capng_restore_state(&s);
			rc = 0;
			for (i=0; i <= CAP_LAST_CAP && rc == 0; i++)
				if (capng_have_capability(CAPNG_BOUNDING_SET,
								 i) == 0)
					rc = prctl(PR_CAPBSET_DROP, i, 0, 0, 0);
			if (rc == 0)
				m.state = CAPNG_APPLIED;
		} else
			capng_restore_state(&s);
#else
		rc = 0;
#endif
	}
	if (set & CAPNG_SELECT_CAPS) {
		rc = capset((cap_user_header_t)&m.hdr,
				(cap_user_data_t)&m.data);
		if (rc == 0)
			m.state = CAPNG_APPLIED;
	}
	return rc;
}

#ifdef VFS_CAP_U32
static int save_data(struct vfs_cap_data *filedata, int *size)
{
	// Now stuff the data structures
	if (m.cap_ver == 1) {
		filedata->data[0].permitted = FIXUP(m.data.v1.permitted);
		filedata->data[0].inheritable = FIXUP(m.data.v1.inheritable);
		filedata->magic_etc = FIXUP(VFS_CAP_REVISION_1);
		*size = XATTR_CAPS_SZ_1;
	} else {
		int eff;

		if (m.data.v3[0].effective || m.data.v3[1].effective)
			eff = VFS_CAP_FLAGS_EFFECTIVE;
		else
			eff = 0;
		filedata->data[0].permitted = FIXUP(m.data.v3[0].permitted);
		filedata->data[0].inheritable = FIXUP(m.data.v3[0].inheritable);
		filedata->data[1].permitted = FIXUP(m.data.v3[1].permitted);
		filedata->data[1].inheritable = FIXUP(m.data.v3[1].inheritable);
		filedata->magic_etc = FIXUP(VFS_CAP_REVISION_2 | eff);
		*size = XATTR_CAPS_SZ_2;
	}

	return 0;
}
#endif

int capng_apply_caps_fd(int fd)
{
#ifndef VFS_CAP_U32
	return -1;
#else
	int rc, size;
	struct vfs_cap_data filedata;
	struct stat buf;

	// Before updating, we expect that the data is initialized to something
	if (m.state < CAPNG_INIT)
		return -1;

	if (fstat(fd, &buf) != 0)
		return -1;
	if (S_ISLNK(buf.st_mode) || !S_ISREG(buf.st_mode)) {
		errno = EINVAL;
		return -1;
	}
	if (capng_have_capabilities(CAPNG_SELECT_CAPS) == CAPNG_NONE)
		rc = fremovexattr(fd, XATTR_NAME_CAPS);
	else {
		save_data(&filedata, &size);
		rc = fsetxattr(fd, XATTR_NAME_CAPS, &filedata, size, 0);
	}

	if (rc == 0)
		m.state = CAPNG_APPLIED;

	return rc;
#endif
}

// Change uids keeping/removing only certain capabilities 
// flag to drop supp groups
int capng_change_id(int uid, int gid, capng_flags_t flag)
{
	int rc, need_setgid, need_setuid;

	// Before updating, we expect that the data is initialized to something
	if (m.state < CAPNG_INIT)
		return -1;

	// Check the current capabilities
#ifdef PR_CAPBSET_DROP
	// If newer kernel, we need setpcap to change the bounding set
	if (capng_have_capability(CAPNG_EFFECTIVE, CAP_SETPCAP) == 0 && 
					flag & CAPNG_CLEAR_BOUNDING)
		capng_update(CAPNG_ADD,
				CAPNG_EFFECTIVE|CAPNG_PERMITTED, CAP_SETPCAP);
#endif
	if (gid == -1 || capng_have_capability(CAPNG_EFFECTIVE, CAP_SETGID))
		need_setgid = 0;
	else {
		need_setgid = 1;
		capng_update(CAPNG_ADD, CAPNG_EFFECTIVE|CAPNG_PERMITTED,
				CAP_SETGID);
	}
	if (uid == -1 || capng_have_capability(CAPNG_EFFECTIVE, CAP_SETUID))
		need_setuid = 0;
	else {
		need_setuid = 1;
		capng_update(CAPNG_ADD, CAPNG_EFFECTIVE|CAPNG_PERMITTED,
				CAP_SETUID);
	}

	// Tell system we want to keep caps across uid change
	if (prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0))
		return -2;

	// Change to the temp capabilities
	rc = capng_apply(CAPNG_SELECT_CAPS);
	if (rc < 0)
		return -3;

	// Clear bounding set if needed while we have CAP_SETPCAP
	if (flag & CAPNG_CLEAR_BOUNDING) {
		capng_clear(CAPNG_BOUNDING_SET);
		rc = capng_apply(CAPNG_SELECT_BOUNDS);
		if (rc)
			return -8;
	}

	// Change gid
	if (gid != -1) {
		rc = setresgid(gid, gid, gid);
		if (rc)
			return -4;
	}

	// See if we need to unload supplemental groups
	if ((flag & CAPNG_DROP_SUPP_GRP) && gid != -1) {
		if (setgroups(0, NULL))
			return -5;
	}

	// Change uid
	if (uid != -1) {
		rc = setresuid(uid, uid, uid);
		if (rc)
			return -6;
	}

	// Tell it we are done keeping capabilities
	rc = prctl(PR_SET_KEEPCAPS, 0, 0, 0, 0);
	if (rc)
		return -7;

	// Now throw away CAP_SETPCAP so no more changes
	if (need_setgid)
		capng_update(CAPNG_DROP, CAPNG_EFFECTIVE|CAPNG_PERMITTED,
				CAP_SETGID);
	if (need_setuid)
		capng_update(CAPNG_DROP, CAPNG_EFFECTIVE|CAPNG_PERMITTED,
				CAP_SETUID);

	// Now drop setpcap & apply
	capng_update(CAPNG_DROP, CAPNG_EFFECTIVE|CAPNG_PERMITTED,
				CAP_SETPCAP);
	rc = capng_apply(CAPNG_SELECT_CAPS);
	if (rc < 0)
		return -9;

	// Done
	m.state = CAPNG_UPDATED;
	return 0;
}

int capng_lock(void)
{
#ifdef PR_SET_SECUREBITS
	int rc = prctl(PR_SET_SECUREBITS,
			1 << SECURE_NOROOT |
			1 << SECURE_NOROOT_LOCKED |
			1 << SECURE_NO_SETUID_FIXUP |
			1 << SECURE_NO_SETUID_FIXUP_LOCKED, 0, 0, 0);
	if (rc)
		return -1;
#endif

	return 0;
}

// -1 - error, 0 - no caps, 1 partial caps, 2 full caps
capng_results_t capng_have_capabilities(capng_select_t set)
{
	int empty = 0, full = 0;

	// First, try to init with current set
	if (m.state < CAPNG_INIT)
		capng_get_caps_process();

	// If we still don't have anything, error out
	if (m.state < CAPNG_INIT)
		return CAPNG_FAIL;

	if (set & CAPNG_SELECT_CAPS) {
		if (m.cap_ver == 1) {
			if (m.data.v1.effective == 0)
				empty = 1;
			// after fill, 30 bits starts from upper to lower
			else if (m.data.v1.effective == 0x7FFFFFFFU)
				full = 1;
			// actual capabilities read from system
			else if (m.data.v1.effective == 0xFFFFFEFFU)
				full = 1;
			else
				return CAPNG_PARTIAL;
		} else {
			if (m.data.v3[0].effective == 0)
				empty = 1;
			else if (m.data.v3[0].effective == 0xFFFFFFFFU)
				full = 1;
			else
				return CAPNG_PARTIAL;
			if ((m.data.v3[1].effective & UPPER_MASK) == 0)
				empty = 1;
			else if ((m.data.v3[1].effective & UPPER_MASK) ==
							UPPER_MASK)
				full = 1;
			else
				return CAPNG_PARTIAL;
		}
	}
#ifdef PR_CAPBSET_DROP
	if (set & CAPNG_SELECT_BOUNDS) {
		if (m.bounds[0] == 0)
			empty = 1;
		else if (m.bounds[0] == 0xFFFFFFFFU)
			full = 1;
		else
			return CAPNG_PARTIAL;
		if ((m.bounds[1] & UPPER_MASK) == 0)
			empty = 1;
		else if ((m.bounds[1] & UPPER_MASK) == UPPER_MASK)
			full = 1;
		else
			return CAPNG_PARTIAL;
	}
#endif 

	if (empty == 1 && full == 0)
		return CAPNG_NONE;
	else if (empty == 0 && full == 1)
		return CAPNG_FULL;
	
	return CAPNG_PARTIAL;
}

static int check_effective(unsigned int capability, unsigned int idx)
{
	return MASK(capability) & m.data.v3[idx].effective ? 1 : 0;
}

static int check_permitted(unsigned int capability, unsigned int idx)
{
	return MASK(capability) & m.data.v3[idx].permitted ? 1 : 0;
}

static int check_inheritable(unsigned int capability, unsigned int idx)
{
	return MASK(capability) & m.data.v3[idx].inheritable ? 1 : 0;
}

static int bounds_bit_check(unsigned int capability, unsigned int idx)
{
#ifdef PR_CAPBSET_DROP
	return MASK(capability) & m.bounds[idx] ? 1 : 0;
#else
	return 0;
#endif
}

static int v1_check(unsigned int capability, __u32 data)
{
	return MASK(capability) & data ? 1 : 0;
}

int capng_have_capability(capng_type_t which, unsigned int capability)
{
	// First, try to init with current set
	if (m.state < CAPNG_INIT)
		capng_get_caps_process();

	// If we still don't have anything, error out
	if (m.state < CAPNG_INIT)
		return CAPNG_FAIL;
	if (m.cap_ver == 1 && capability > 31)
		return 0;
	if (!cap_valid(capability))
		return 0;

	if (m.cap_ver == 1) {
		if (which == CAPNG_EFFECTIVE)
			return v1_check(capability, m.data.v1.effective);
		else if (which == CAPNG_PERMITTED)
			return v1_check(capability, m.data.v1.permitted);
		else if (which == CAPNG_INHERITABLE)
			return v1_check(capability, m.data.v1.inheritable);
	} else {
		unsigned int idx;

		if (capability > 31) {
			idx = capability>>5;
			capability %= 32;
		} else
			idx = 0;

		if (which == CAPNG_EFFECTIVE)
			return check_effective(capability, idx);
		else if (which == CAPNG_PERMITTED)
			return check_permitted(capability, idx);
		else if (which == CAPNG_INHERITABLE)
			return check_inheritable(capability, idx);
		else if (which == CAPNG_BOUNDING_SET)
			return bounds_bit_check(capability, idx);
	}
	return 0;
}

char *capng_print_caps_numeric(capng_print_t where, capng_select_t set)
{
	char *ptr = NULL;

	if (m.state < CAPNG_INIT)
		return ptr;

	if (where == CAPNG_PRINT_STDOUT) {
		if (set & CAPNG_SELECT_CAPS) {
			if (m.cap_ver == 1) {
				printf( "Effective:    %08X\n"
					"Permitted:    %08X\n"
					"Inheritable:  %08X\n",
					m.data.v1.effective,
					m.data.v1.permitted,
					m.data.v1.inheritable);
			} else {
				printf( "Effective:    %08X, %08X\n"
					"Permitted:    %08X, %08X\n"
					"Inheritable:  %08X, %08X\n",
					m.data.v3[1].effective & UPPER_MASK,
					m.data.v3[0].effective,
					m.data.v3[1].permitted & UPPER_MASK,
					m.data.v3[0].permitted,
					m.data.v3[1].inheritable & UPPER_MASK,
					m.data.v3[0].inheritable);
			
			}
		}
#ifdef PR_CAPBSET_DROP
		if (set & CAPNG_SELECT_BOUNDS)
			printf("Bounding Set: %08X, %08X\n",
				m.bounds[1] & UPPER_MASK, m.bounds[0]);
#endif
	} else if (where == CAPNG_PRINT_BUFFER) {
		if (set & CAPNG_SELECT_CAPS) {
			// Make it big enough for bounding set, too
			ptr = malloc(160);
			if (m.cap_ver == 1) {
				snprintf(ptr, 160,
					"Effective:   %08X\n"
					"Permitted:   %08X\n"
					"Inheritable: %08X\n",
					m.data.v1.effective,
					m.data.v1.permitted,
					m.data.v1.inheritable);
			} else {
				snprintf(ptr, 160,
					"Effective:   %08X, %08X\n"
					"Permitted:   %08X, %08X\n"
					"Inheritable: %08X, %08X\n",
					m.data.v3[1].effective & UPPER_MASK,
					m.data.v3[0].effective,
					m.data.v3[1].permitted & UPPER_MASK,
					m.data.v3[0].permitted,
					m.data.v3[1].inheritable & UPPER_MASK,
					m.data.v3[0].inheritable);
			}
		}
		if (set & CAPNG_SELECT_BOUNDS) {
#ifdef PR_CAPBSET_DROP
			char *s;
			if (ptr == NULL ){
				ptr = malloc(40);
				if (ptr == NULL)
					return ptr;
				*ptr = 0;
				s = ptr;
			} else
				s = ptr + strlen(ptr);
			snprintf(s, 40, "Bounding Set: %08X, %08X\n",
					m.bounds[1] & UPPER_MASK, m.bounds[0]);
#endif
		}
	}

	return ptr;
}

char *capng_print_caps_text(capng_print_t where, capng_type_t which)
{
	unsigned int i; 
	int once = 0, cnt = 0;
	char *ptr = NULL;

	if (m.state < CAPNG_INIT)
		return ptr;

	for (i=0; i<=CAP_LAST_CAP; i++) {
		if (capng_have_capability(which, i)) {
			const char *n = capng_capability_to_name(i);
			if (n == NULL)
				n = "unknown";
			if (where == CAPNG_PRINT_STDOUT) {
				if (once == 0) {
					printf("%s", n);
					once++;
				} else
					printf(", %s", n);
			} else if (where == CAPNG_PRINT_BUFFER) {
				int len;
				if (once == 0) {
					ptr = malloc(CAP_LAST_CAP*18);
					if (ptr == NULL)
						return ptr;
					len = sprintf(ptr+cnt, "%s", n);
					once++;
				} else
					len = sprintf(ptr+cnt, ", %s", n);
				if (len > 0)
					cnt+=len;
			}
		}
	}
	if (once == 0) {
		if (where == CAPNG_PRINT_STDOUT)
			printf("none");
		else
			ptr = strdup("none");
	}
	return ptr;
}

void *capng_save_state(void)
{
	void *ptr = malloc(sizeof(m));
	if (ptr)
		memcpy(ptr, &m, sizeof(m));
	return ptr;
}

void capng_restore_state(void **state)
{
	if (state) {
		void *ptr = *state;
		if (ptr)
			memcpy(&m, ptr, sizeof(m));
		free(ptr);
		*state = NULL;
	}
}

