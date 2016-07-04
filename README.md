# RetroScope

The majority of RetroScope's code is in the dalvik/vm/zombie directory.

Please be sure to read the RetroScope paper before working with RetroScope.

This code is provided as is. If you extend it in any way/fix any bugs, then please reach out to me. I will be glad to incorporate any upgrades and give you due credit! :) 

To build RetroScope,
1) Setup a build environment as described here:
https://source.android.com/source/initializing.html

2) Clone the RetroScope repo (we have had some problems building from the downloaded zip file).

3) Build RetroScope with the typical Android build commands:
$ source build/envsetup.sh
$ lunch aosp_arm-eng
$ make -j4 RetroScope
$ lunch aosp_arm-eng
$ make -j4

4) Push a memory image:
If using pmd format (pmd code is in tools/pmd):
1) Push the map file to the emulator's /usr/data/ directory
2) Push the mem file to the emulator's sdcard
RetroScope can be extended to handle any other memory image formats.


