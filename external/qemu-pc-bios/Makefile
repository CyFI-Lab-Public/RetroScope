# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# this is a set of definitions that allow the usage of Makefile.android
# even if we're not using the Android build system.
#

CC           := gcc
BCC          := bcc
AS86         ;= as86
MAKE         := make
CP           := cp

all: bios.bin vgabios-cirrus.bin

bochs/bios/Makefile:
		cd bochs && ./configure

bios.bin: bochs/bios/Makefile
		$(MAKE) -C bochs/bios
		$(CP) bochs/bios/BIOS-bochs-latest $@

vgabios-cirrus.bin:
		$(MAKE) -C vgabios
		$(CP) vgabios/VGABIOS-lgpl-latest.cirrus.bin $@

clean:
		rm -rf bochs/Makefile bochs/build bochs/config.h bochs/config.log \
			bochs/config.status bochs/instrument bochs/ltdlconf.h
		$(MAKE) -C bochs/bios bios-clean
		$(MAKE) -C bochs/bios dist-clean
		$(MAKE) -C vgabios bios-clean
		$(MAKE) -C vgabios dist-clean
		rm -rf bios.bin vgabios-cirrus.bin




