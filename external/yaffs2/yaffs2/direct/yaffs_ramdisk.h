/*
 * YAFFS: Yet another FFS. A NAND-flash specific file system. 
 * yaffs_ramdisk.h: yaffs ram disk component
 *
 * Copyright (C) 2002 Aleph One Ltd.
 *
 * Created by Charles Manning <charles@aleph1.co.uk>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * $Id: yaffs_ramdisk.h,v 1.1 2004/11/03 08:29:28 charles Exp $
 */

// This provides a rma disk under yaffs.
// NB this is not intended for NAND emulation.
// Use this with dev->useNANDECC enabled, then ECC overheads are not required.

#ifndef __YAFFS_RAMDISK_H__
#define __YAFFS_RAMDISK_H__


#include "yaffs_guts.h"
int yramdisk_EraseBlockInNAND(yaffs_Device *dev, int blockNumber);
int yramdisk_WriteChunkWithTagsToNAND(yaffs_Device *dev,int chunkInNAND,const __u8 *data, yaffs_ExtendedTags *tags);
int yramdisk_ReadChunkWithTagsFromNAND(yaffs_Device *dev,int chunkInNAND, __u8 *data, yaffs_ExtendedTags *tags);
int yramdisk_EraseBlockInNAND(yaffs_Device *dev, int blockNumber);
int yramdisk_InitialiseNAND(yaffs_Device *dev);
int yramdisk_MarkNANDBlockBad(yaffs_Device *dev,int blockNumber);
int yramdisk_QueryNANDBlock(yaffs_Device *dev, int blockNo, yaffs_BlockState *state, int *sequenceNumber);
#endif
