Android platform gdb is built by the NDK script now. The source lives in the
AOSP-toolchain repository.

cd $(AOSP)/ndk
./build/tools/build-gdbserver.sh --build-out=/tmp/gdbserver-7.6 --gdb-version=7.6 $(AOSP-toolchain)/gdb/gdb-7.6/gdb/gdbserver/ $(NDK) arm-linux-androideabi-4.7

Current prebuilt version is against AOSP-toolchain/gdb/gdb-7.6
commit 1c4be8e861df1416efe1812dfe0c6e7a096d9d60

