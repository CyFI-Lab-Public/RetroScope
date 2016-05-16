#!/bin/sh
#
# YAFFS: Yet another FFS. A NAND-flash specific file system.
#
# Copyright (C) 2002 Aleph One Ltd.
# 
# Created by Charles Manning <charles@aleph1.co.uk>
# 
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 2 as
# published by the Free Software Foundation.
#
# Patch YAFFS into the kernel
#
#  args:  kpath  : Full path to kernel sources to be patched
#
#  Somewhat "inspired by" the mtd patchin script
#
#  $Id: patch-ker.sh,v 1.1 2005/07/31 09:04:13 marty Exp $

VERSION=0
PATCHLEVEL=0
SUBLEVEL=0
LINUXDIR=$1

# To be a Linux directory, it must have a Makefile


# Display usage of this script
usage () {
	echo "usage:  $0  kernelpath"
	exit 1
}



if [ -z $LINUXDIR ]
then
    usage;
fi

# Check if kerneldir contains a Makefile
if [ ! -f $LINUXDIR/Makefile ] 
then 
	echo "Directory $LINUXDIR does not exist or is not a kernel source directory";
	exit 1;
fi

# Get kernel version
VERSION=`grep -s VERSION <$LINUXDIR/Makefile | head -n 1 | sed s/'VERSION = '//`
PATCHLEVEL=`grep -s PATCHLEVEL <$LINUXDIR/Makefile | head -n 1 | sed s/'PATCHLEVEL = '//`
SUBLEVEL=`grep -s SUBLEVEL <$LINUXDIR/Makefile | head -n 1 | sed s/'SUBLEVEL = '//`

# Can we handle this version?
if [ $VERSION -ne 2  -o $PATCHLEVEL -lt 6  ]
then 
	echo "Cannot patch kernel version $VERSION.$PATCHLEVEL.$SUBLEVEL, must be 2.6.x or higher"
	exit 1;
fi


KCONFIG=$LINUXDIR/fs/Kconfig
KCONFIGOLD=$LINUXDIR/fs/Kconfig.pre.yaffs
YAFFS_PATCHED_STRING=`grep -s yaffs <$KCONFIG | head -n 1`

MAKEFILE=$LINUXDIR/fs/Makefile
MAKEFILEOLD=$LINUXDIR/fs/Makefile.pre.yaffs

if [ ! -z "$YAFFS_PATCHED_STRING" ]
then
    YAFFS_PATCHED=0
    echo "$KCONFIG already mentions YAFFS, so we will not change it"
else
   # Change the fs/Kconfig file
   # Save the old Kconfig
   # Copy all stuff up to JFFS
   # Insert some YAFFS stuff
   # Copy all the rest of the stuff

    YAFFS_PATCHED=1
    echo "Updating $KCONFIG"
    mv -f $KCONFIG  $KCONFIGOLD
    sed -n -e "/JFFS/,99999 ! p" $KCONFIGOLD >$KCONFIG
    echo "">>$KCONFIG
    echo "# Patched by YAFFS" >>$KCONFIG
    echo "source \"fs/yaffs2/Kconfig\"">>$KCONFIG
    echo "">>$KCONFIG
    sed -n -e "/JFFS/,99999 p" $KCONFIGOLD >>$KCONFIG

   # now do fs/Makefile -- simply add the target at the end
    echo "Updating $MAKEFILE"
    cp -f $MAKEFILE $MAKEFILEOLD
    echo "">>$MAKEFILE
    echo "# Patched by YAFFS" >>$MAKEFILE
    echo "obj-\$(CONFIG_YAFFS_FS)		+= yaffs2/" >>$MAKEFILE

fi

YAFFSDIR=$LINUXDIR/fs/yaffs2

if [ -e $YAFFSDIR ]
then
   echo "$YAFFSDIR exists, not patching"
else
   mkdir $LINUXDIR/fs/yaffs2
   cp Makefile.kernel $LINUXDIR/fs/yaffs2/Makefile
   cp Kconfig $LINUXDIR/fs/yaffs2
   cp *.c *.h  $LINUXDIR/fs/yaffs2
fi
