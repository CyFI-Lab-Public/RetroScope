# RetroScope

The majority of RetroScope's code is in the dalvik/vm/zombie directory.

Please be sure to read the [RetroScope paper](https://www.usenix.org/system/files/conference/usenixsecurity16/sec16_paper_saltaformaggio.pdf) before working with RetroScope.

This code is provided as is. If you extend it in any way/fix any bugs, then please reach out to me. I will be glad to incorporate any upgrades and give you due credit! :) 

To build RetroScope,<br>
1) Setup a build environment as described here:
https://source.android.com/source/initializing.html

2) Clone the RetroScope repo (we have had some problems building from the downloaded zip file).

3) Build RetroScope with the typical Android build commands:<br>
$ source build/envsetup.sh<br>
$ lunch aosp_arm-eng<br>
$ make -j4 RetroScope<br>
$ lunch aosp_arm-eng<br>
$ make -j4<br>

4) Use the emulator-MAGIC binary. This will require overwriting the prebuilt emulator-arm or emulator64-arm binary with emulator-MAGIC.<br>
Do not use the prebuilt emulator binaries. They fail to boot. I do not know why, if you figure out then please email me!

5) Push a memory image:<br>
If using pmd format (pmd code is in tools/pmd):<br>
1) Push the map file to the emulator's /system/usr/data/ directory<br>
2) Push the mem file to the emulator's sdcard<br>
RetroScope can be extended to handle any other memory image formats.

6) Run the 'emulator' command. Hardware rendering should be enabled. The size of the emulator's screen should be at least as large as the subject device.
