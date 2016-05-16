#!/bin/sh

# Use this script if you need to regenerate the digest values
# in test.sh, or if you don't care about digests and you just
# want to see some fsck results. Should be run as root.

set -e

. ./test-gen.lib

test_cleanup () {
	umount mnt 2>/dev/null || true
	rm -rf mnt fout lsout
}

fail () {
	echo FAILED
	test_cleanup
	gen_cleanup
	exit 1
}

pass () {
	md5=`calc_digest`
	echo PASSED
	echo $@ $md5
	test_cleanup
	gen_cleanup
}

# dtest-mount - Exercise the -d directory option of genext2fs
# Creates an image with a file of given size, verifies it
# and returns the command line with which to invoke dtest()
# Usage: dtest-mount file-size number-of-blocks 
dtest_mount () {
	size=$1; blocks=$2
	echo Testing with file of size $size
	dgen $size $blocks
	/sbin/e2fsck -fn ext2.img || fail
	mkdir -p mnt
	mount -t ext2 -o ro,loop ext2.img mnt || fail
	if (! [ -f mnt/file.$size ]) || \
	      [ $size != "`ls -al mnt | grep file.$size |
	                                awk '{print $5}'`" ] ; then
		fail
	fi
	pass dtest $size $blocks
}

# ftest-mount - Exercise the -f spec-file option of genext2fs
# Creates an image with the devices mentioned in the given spec 
# file, verifies it, and returns the command line with which to
# invoke ftest()
# Usage: ftest-mount spec-file number-of-blocks 
ftest_mount () {
	fname=$1; blocks=$2 
	echo Testing with devices file $fname
	fgen $fname $blocks
	/sbin/e2fsck -fn ext2.img || fail
	mkdir -p mnt
	mount -t ext2 -o ro,loop ext2.img mnt || fail
	[ -d mnt/dev ] || fail
	# Exclude those devices that have interpolated
	# minor numbers, as being too hard to match.
	egrep -v "(hda|hdb|tty|loop|ram|ubda)" $fname | \
		grep '^[^	#]*	[bc]' | \
		awk '{print $1,$4,$5,$6","$7}'| \
		sort -d -k3.6 > fout
	ls -aln mnt/dev | \
		egrep -v "(hda|hdb|tty|loop|ram|ubda)" | \
		grep ^[bc] | \
		awk '{ print "/dev/"$10,$3,$4,$5$6}' | \
		sort -d -k3.6 > lsout
	diff fout lsout || fail
	pass ftest $fname $blocks
}

dtest_mount 0 4096 
dtest_mount 0 8193
dtest_mount 0 8194
dtest_mount 1 4096 
dtest_mount 12288 4096 
dtest_mount 274432 4096 
dtest_mount 8388608 9000 
dtest_mount 16777216 20000

ftest_mount device_table.txt 4096 
