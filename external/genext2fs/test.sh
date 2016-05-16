#!/bin/sh

# This script generates a variety of filesystems and checks that they
# are identical to ones that are known to be mountable, and pass fsck
# and various other sanity checks.

# Passing these tests is preferable to passing test-mount.sh because
# this script doesn't require root, and because passing these tests
# guarantees byte-for-byte agreement with other builds, ports,
# architectures, times of day etc.

set -e

. ./test-gen.lib

# md5cmp - Calculate MD5 digest and compare it to an expected value.
# Usage: md5cmp expected-digest
md5cmp () {
	checksum=$1
	md5=`calc_digest`
	if [ x$md5 = x$checksum ] ; then
		echo PASSED
	else
		echo FAILED
		exit 1
	fi
}

# dtest - Exercises the -d directory option of genext2fs
# Creates an image with a file of given size and verifies it
# Usage: dtest file-size number-of-blocks correct-checksum
dtest () {
	size=$1; blocks=$2; checksum=$3
	echo Testing with file of size $size
	dgen $size $blocks
	md5cmp $checksum
	gen_cleanup
}

# ftest - Exercises the -f spec-file option of genext2fs
# Creates an image with the devices mentioned in the given spec 
# file and verifies it
# Usage: ftest spec-file number-of-blocks correct-checksum
ftest () {
	fname=$1; blocks=$2; checksum=$3
	echo Testing with devices file $fname
	fgen $fname $blocks
	md5cmp $checksum
	gen_cleanup
}

# NB: to regenerate these values, always use test-mount.sh, that is,
# replace the following lines with the output of
# sudo sh test-mount.sh|grep test

dtest 0 4096 3bc6424b8fcd51a0de34ee59d91d5f16
dtest 0 8193 f174804f6b433b552706cbbfc60c416d
dtest 0 8194 4855a55d0cbdc44584634df49ebd5711
dtest 1 4096 09c569b6bfb45222c729c42d04d5451f
dtest 12288 4096 61febcbfbf32024ef99103fcdc282c39
dtest 274432 4096 0c517803552c55c1806e4220b0a0164f
dtest 8388608 9000 e0e5ea15bced10ab486d8135584b5d8e
dtest 16777216 20000 fdf636eb905ab4dc1bf76dce5ac5d209
ftest device_table.txt 4096 a0af06d944b11d2902dfd705484c64cc
