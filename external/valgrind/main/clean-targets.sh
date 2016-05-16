#!/bin/bash
#
# Handy script to delete all Valgrind related targets from the build output
# directory.  Useful when working on the Android makefiles.
#
obj=$ANDROID_BUILD_TOP/out/target/product/stingray/obj

rm -r $obj/STATIC_LIBRARIES/libvex-arm-linux_intermediates \
	$obj/STATIC_LIBRARIES/libcoregrind-arm-linux_intermediates \
	$obj/STATIC_LIBRARIES/libreplacemalloc_toolpreload-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/vgpreload_core-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/memcheck-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/vgpreload_memcheck-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/cachegrind-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/callgrind-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/helgrind-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/vgpreload_helgrind-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/drd-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/vgpreload_drd-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/massif-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/vgpreload_massif-arm-linux_intermediates \
	$obj/SHARED_LIBRARIES/none-arm-linux_intermediates \
	$obj/EXECUTABLES/valgrind_intermediates
