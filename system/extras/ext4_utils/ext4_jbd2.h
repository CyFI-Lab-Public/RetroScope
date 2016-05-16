/****************************************************************************
 ****************************************************************************
 ***
 ***   This header was automatically generated from a Linux kernel header
 ***   of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef _EXT4_JBD2_H
#define _EXT4_JBD2_H

#include "ext4.h"

#define EXT4_JOURNAL(inode) (EXT4_SB((inode)->i_sb)->s_journal)

#define EXT4_SINGLEDATA_TRANS_BLOCKS(sb)   (EXT4_HAS_INCOMPAT_FEATURE(sb, EXT4_FEATURE_INCOMPAT_EXTENTS)   ? 27U : 8U)

#define EXT4_XATTR_TRANS_BLOCKS 6U

#define EXT4_DATA_TRANS_BLOCKS(sb) (EXT4_SINGLEDATA_TRANS_BLOCKS(sb) +   EXT4_XATTR_TRANS_BLOCKS - 2 +   EXT4_MAXQUOTAS_TRANS_BLOCKS(sb))

#define EXT4_META_TRANS_BLOCKS(sb) (EXT4_XATTR_TRANS_BLOCKS +   EXT4_MAXQUOTAS_TRANS_BLOCKS(sb))

#define EXT4_DELETE_TRANS_BLOCKS(sb) (2 * EXT4_DATA_TRANS_BLOCKS(sb) + 64)

#define EXT4_MAX_TRANS_DATA 64U

#define EXT4_RESERVE_TRANS_BLOCKS 12U

#define EXT4_INDEX_EXTRA_TRANS_BLOCKS 8

#define EXT4_QUOTA_TRANS_BLOCKS(sb) 0
#define EXT4_QUOTA_INIT_BLOCKS(sb) 0
#define EXT4_QUOTA_DEL_BLOCKS(sb) 0
#define EXT4_MAXQUOTAS_TRANS_BLOCKS(sb) (MAXQUOTAS*EXT4_QUOTA_TRANS_BLOCKS(sb))
#define EXT4_MAXQUOTAS_INIT_BLOCKS(sb) (MAXQUOTAS*EXT4_QUOTA_INIT_BLOCKS(sb))
#define EXT4_MAXQUOTAS_DEL_BLOCKS(sb) (MAXQUOTAS*EXT4_QUOTA_DEL_BLOCKS(sb))

#define ext4_journal_get_undo_access(handle, bh)   __ext4_journal_get_undo_access(__func__, (handle), (bh))
#define ext4_journal_get_write_access(handle, bh)   __ext4_journal_get_write_access(__func__, (handle), (bh))
#define ext4_forget(handle, is_metadata, inode, bh, block_nr)   __ext4_forget(__func__, (handle), (is_metadata), (inode), (bh),  (block_nr))
#define ext4_journal_get_create_access(handle, bh)   __ext4_journal_get_create_access(__func__, (handle), (bh))
#define ext4_handle_dirty_metadata(handle, inode, bh)   __ext4_handle_dirty_metadata(__func__, (handle), (inode), (bh))

#define EXT4_NOJOURNAL_MAX_REF_COUNT ((unsigned long) 4096)

#define ext4_journal_stop(handle)   __ext4_journal_stop(__func__, (handle))

#endif

