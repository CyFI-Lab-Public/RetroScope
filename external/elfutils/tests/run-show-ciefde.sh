#! /bin/sh
# Copyright (C) 1999, 2000, 2002, 2005 Red Hat, Inc.
# This file is part of Red Hat elfutils.
# Written by Ulrich Drepper <drepper@redhat.com>, 1999.
#
# Red Hat elfutils is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by the
# Free Software Foundation; version 2 of the License.
#
# Red Hat elfutils is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with Red Hat elfutils; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.
#
# Red Hat elfutils is an included package of the Open Invention Network.
# An included package of the Open Invention Network is a package for which
# Open Invention Network licensees cross-license their patents.  No patent
# license is granted, either expressly or impliedly, by designation as an
# included package.  Should you wish to participate in the Open Invention
# Network licensing program, please visit www.openinventionnetwork.com
# <http://www.openinventionnetwork.com>.

. $srcdir/test-subr.sh

testfiles testfile3 testfile4

testrun_compare ./show-ciefde testfile3 testfile4 <<\EOF
testfile3 has 1 CIEs and 1 FDEs
CIE[0]: bytes_in_cie = 16, version = 1, augmenter = ""
CIE[0]: code_alignment_factor = 1
CIE[0]: data_alignment_factor = fffffffffffffffc
CIE[0]: return_address_register = 8
CIE[0]: bytes = 0c 04 04 88 01 00 00
FDE[0]: low_pc = 0x804842c, length = 41
FDE[0]: bytes = 18 00 00 00 18 00 00 00 2c 84 04 08 29 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[0]: cie_offset = 0, cie_index = 0, fde_offset = 24
FDE[0]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
no FDE at 8048400
FDE[@804842c]: cie_offset = 0, cie_index = 0, fde_offset = 24
FDE[@8048454]: cie_offset = 0, cie_index = 0, fde_offset = 24
no FDE at 8048455
no FDE at 80493fc
testfile4 has 5 CIEs and 61 FDEs
CIE[0]: bytes_in_cie = 20, version = 1, augmenter = "eh"
CIE[0]: code_alignment_factor = 1
CIE[0]: data_alignment_factor = fffffffffffffffc
CIE[0]: return_address_register = 8
CIE[0]: bytes = 0c 04 04 88 01
CIE[1]: bytes_in_cie = 16, version = 1, augmenter = ""
CIE[1]: code_alignment_factor = 1
CIE[1]: data_alignment_factor = fffffffffffffffc
CIE[1]: return_address_register = 8
CIE[1]: bytes = 0c 04 04 88 01 00 00
CIE[2]: bytes_in_cie = 16, version = 1, augmenter = ""
CIE[2]: code_alignment_factor = 1
CIE[2]: data_alignment_factor = fffffffffffffffc
CIE[2]: return_address_register = 8
CIE[2]: bytes = 0c 04 04 88 01 00 00
CIE[3]: bytes_in_cie = 20, version = 1, augmenter = "eh"
CIE[3]: code_alignment_factor = 1
CIE[3]: data_alignment_factor = fffffffffffffffc
CIE[3]: return_address_register = 8
CIE[3]: bytes = 0c 04 04 88 01
CIE[4]: bytes_in_cie = 16, version = 1, augmenter = ""
CIE[4]: code_alignment_factor = 1
CIE[4]: data_alignment_factor = fffffffffffffffc
CIE[4]: return_address_register = 8
CIE[4]: bytes = 0c 04 04 88 01 00 00
FDE[0]: low_pc = 0x80493fc, length = 154
FDE[0]: bytes = 2c 00 00 00 1c 00 00 00 fc 93 04 08 9a 00 00 00 41 0e 08 85 02 42 0d 05 53 2e 08 50 2e 10 48 2e 00 58 2e 10 62 2e 00 63 2e 10 45 2e 00 00 00 00
FDE[0]: cie_offset = 0, cie_index = 0, fde_offset = 28
FDE[0]: instructions = 41 0e 08 85 02 42 0d 05 53 2e 08 50 2e 10 48 2e 00 58 2e 10 62 2e 00 63 2e 10 45 2e 00 00 00 00
FDE[1]: low_pc = 0x8049498, length = 49
FDE[1]: bytes = 18 00 00 00 4c 00 00 00 98 94 04 08 31 00 00 00 41 0e 08 85 02 42 0d 05 4c 2e 10 00
FDE[1]: cie_offset = 0, cie_index = 0, fde_offset = 76
FDE[1]: instructions = 41 0e 08 85 02 42 0d 05 4c 2e 10 00
FDE[2]: low_pc = 0x80494d4, length = 23
FDE[2]: bytes = 18 00 00 00 18 00 00 00 d4 94 04 08 17 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[2]: cie_offset = 100, cie_index = 1, fde_offset = 24
FDE[2]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[3]: low_pc = 0x80494f0, length = 26
FDE[3]: bytes = 18 00 00 00 34 00 00 00 f0 94 04 08 1a 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[3]: cie_offset = 100, cie_index = 1, fde_offset = 52
FDE[3]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[4]: low_pc = 0x8049560, length = 85
FDE[4]: bytes = 24 00 00 00 50 00 00 00 60 95 04 08 55 00 00 00 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 53 2e 10 4e 2e 00 55 2e 10 00
FDE[4]: cie_offset = 100, cie_index = 1, fde_offset = 80
FDE[4]: instructions = 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 53 2e 10 4e 2e 00 55 2e 10 00
FDE[5]: low_pc = 0x80495c0, length = 66
FDE[5]: bytes = 20 00 00 00 78 00 00 00 c0 95 04 08 42 00 00 00 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 5e 2e 10 00 00 00
FDE[5]: cie_offset = 100, cie_index = 1, fde_offset = 120
FDE[5]: instructions = 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 5e 2e 10 00 00 00
FDE[6]: low_pc = 0x8049610, length = 28
FDE[6]: bytes = 18 00 00 00 9c 00 00 00 10 96 04 08 1c 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[6]: cie_offset = 100, cie_index = 1, fde_offset = 156
FDE[6]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[7]: low_pc = 0x8049630, length = 31
FDE[7]: bytes = 18 00 00 00 b8 00 00 00 30 96 04 08 1f 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[7]: cie_offset = 100, cie_index = 1, fde_offset = 184
FDE[7]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[8]: low_pc = 0x80496e0, length = 71
FDE[8]: bytes = 1c 00 00 00 d4 00 00 00 e0 96 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 5c 2e 10 00 00
FDE[8]: cie_offset = 100, cie_index = 1, fde_offset = 212
FDE[8]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 5c 2e 10 00 00
FDE[9]: low_pc = 0x8049730, length = 165
FDE[9]: bytes = 20 00 00 00 f4 00 00 00 30 97 04 08 a5 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 69 2e 10 02 66 2e 00 00 00
FDE[9]: cie_offset = 100, cie_index = 1, fde_offset = 244
FDE[9]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 69 2e 10 02 66 2e 00 00 00
FDE[10]: low_pc = 0x80497e0, length = 89
FDE[10]: bytes = 1c 00 00 00 18 01 00 00 e0 97 04 08 59 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 74 2e 10 00 00
FDE[10]: cie_offset = 100, cie_index = 1, fde_offset = 280
FDE[10]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 74 2e 10 00 00
FDE[11]: low_pc = 0x8049840, length = 89
FDE[11]: bytes = 28 00 00 00 38 01 00 00 40 98 04 08 59 00 00 00 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 55 2e 10 4e 2e 00 52 2e 10 4c 2e 00 00 00
FDE[11]: cie_offset = 100, cie_index = 1, fde_offset = 312
FDE[11]: instructions = 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 55 2e 10 4e 2e 00 52 2e 10 4c 2e 00 00 00
FDE[12]: low_pc = 0x80498a0, length = 176
FDE[12]: bytes = 24 00 00 00 64 01 00 00 a0 98 04 08 b0 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5e 2e 10 4c 2e 00 00
FDE[12]: cie_offset = 100, cie_index = 1, fde_offset = 356
FDE[12]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5e 2e 10 4c 2e 00 00
FDE[13]: low_pc = 0x8049950, length = 116
FDE[13]: bytes = 24 00 00 00 8c 01 00 00 50 99 04 08 74 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 5e 2e 10 00 00 00
FDE[13]: cie_offset = 100, cie_index = 1, fde_offset = 396
FDE[13]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 5e 2e 10 00 00 00
FDE[14]: low_pc = 0x80499d0, length = 31
FDE[14]: bytes = 18 00 00 00 b4 01 00 00 d0 99 04 08 1f 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[14]: cie_offset = 100, cie_index = 1, fde_offset = 436
FDE[14]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[15]: low_pc = 0x80499f0, length = 313
FDE[15]: bytes = 24 00 00 00 d0 01 00 00 f0 99 04 08 39 01 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 d8 2e 10 62 2e 00
FDE[15]: cie_offset = 100, cie_index = 1, fde_offset = 464
FDE[15]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 d8 2e 10 62 2e 00
FDE[16]: low_pc = 0x8049b30, length = 262
FDE[16]: bytes = 24 00 00 00 f8 01 00 00 30 9b 04 08 06 01 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 c8 2e 10 62 2e 00
FDE[16]: cie_offset = 100, cie_index = 1, fde_offset = 504
FDE[16]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 c8 2e 10 62 2e 00
FDE[17]: low_pc = 0x8049c40, length = 95
FDE[17]: bytes = 1c 00 00 00 20 02 00 00 40 9c 04 08 5f 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 6e 2e 10 00 00
FDE[17]: cie_offset = 100, cie_index = 1, fde_offset = 544
FDE[17]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 6e 2e 10 00 00
FDE[18]: low_pc = 0x8049d60, length = 230
FDE[18]: bytes = 20 00 00 00 40 02 00 00 60 9d 04 08 e6 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 02 9a 2e 10 00 00
FDE[18]: cie_offset = 100, cie_index = 1, fde_offset = 576
FDE[18]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 02 9a 2e 10 00 00
FDE[19]: low_pc = 0x8049e50, length = 85
FDE[19]: bytes = 18 00 00 00 64 02 00 00 50 9e 04 08 55 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[19]: cie_offset = 100, cie_index = 1, fde_offset = 612
FDE[19]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[20]: low_pc = 0x8049eb0, length = 144
FDE[20]: bytes = 20 00 00 00 80 02 00 00 b0 9e 04 08 90 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5b 2e 10
FDE[20]: cie_offset = 100, cie_index = 1, fde_offset = 640
FDE[20]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5b 2e 10
FDE[21]: low_pc = 0x8049f40, length = 115
FDE[21]: bytes = 20 00 00 00 a4 02 00 00 40 9f 04 08 73 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 59 2e 10
FDE[21]: cie_offset = 100, cie_index = 1, fde_offset = 676
FDE[21]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 59 2e 10
FDE[22]: low_pc = 0x8049fd0, length = 948
FDE[22]: bytes = 30 00 00 00 c8 02 00 00 d0 9f 04 08 b4 03 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 f7 2e 20 02 64 2e 10 03 15 01 2e 00 02 9f 2e 10 00 00
FDE[22]: cie_offset = 100, cie_index = 1, fde_offset = 712
FDE[22]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 f7 2e 20 02 64 2e 10 03 15 01 2e 00 02 9f 2e 10 00 00
FDE[23]: low_pc = 0x804a390, length = 201
FDE[23]: bytes = 28 00 00 00 fc 02 00 00 90 a3 04 08 c9 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 58 2e 10 52 2e 00 75 2e 10 00
FDE[23]: cie_offset = 100, cie_index = 1, fde_offset = 764
FDE[23]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 58 2e 10 52 2e 00 75 2e 10 00
FDE[24]: low_pc = 0x804a460, length = 206
FDE[24]: bytes = 28 00 00 00 28 03 00 00 60 a4 04 08 ce 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 64 2e 10 52 2e 00 6e 2e 10 00
FDE[24]: cie_offset = 100, cie_index = 1, fde_offset = 808
FDE[24]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 64 2e 10 52 2e 00 6e 2e 10 00
FDE[25]: low_pc = 0x804b970, length = 1274
FDE[25]: bytes = 44 00 00 00 18 00 00 00 70 b9 04 08 fa 04 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 66 2e 10 7b 2e 20 03 7f 01 2e 10 53 2e 08 4c 2e 10 79 2e 20 02 54 2e 10 7e 2e 20 03 6c 01 2e 10 02 45 2e 20 00 00 00
FDE[25]: cie_offset = 948, cie_index = 2, fde_offset = 24
FDE[25]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 66 2e 10 7b 2e 20 03 7f 01 2e 10 53 2e 08 4c 2e 10 79 2e 20 02 54 2e 10 7e 2e 20 03 6c 01 2e 10 02 45 2e 20 00 00 00
FDE[26]: low_pc = 0x804be70, length = 60
FDE[26]: bytes = 1c 00 00 00 60 00 00 00 70 be 04 08 3c 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 5e 2e 10 00 00
FDE[26]: cie_offset = 948, cie_index = 2, fde_offset = 96
FDE[26]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 5e 2e 10 00 00
FDE[27]: low_pc = 0x804c090, length = 85
FDE[27]: bytes = 24 00 00 00 80 00 00 00 90 c0 04 08 55 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 66 2e 04 4d 2e 0c 4c 2e 04 46 2e 20 00
FDE[27]: cie_offset = 948, cie_index = 2, fde_offset = 128
FDE[27]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 66 2e 04 4d 2e 0c 4c 2e 04 46 2e 20 00
FDE[28]: low_pc = 0x804c0f0, length = 75
FDE[28]: bytes = 2c 00 00 00 a8 00 00 00 f0 c0 04 08 4b 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5b 2e 04 4a 2e 0c 4d 2e 04 46 2e 20 00 00 00
FDE[28]: cie_offset = 948, cie_index = 2, fde_offset = 168
FDE[28]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5b 2e 04 4a 2e 0c 4d 2e 04 46 2e 20 00 00 00
FDE[29]: low_pc = 0x804d8e0, length = 71
FDE[29]: bytes = 20 00 00 00 d8 00 00 00 e0 d8 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[29]: cie_offset = 948, cie_index = 2, fde_offset = 216
FDE[29]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[30]: low_pc = 0x804d980, length = 71
FDE[30]: bytes = 20 00 00 00 fc 00 00 00 80 d9 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[30]: cie_offset = 948, cie_index = 2, fde_offset = 252
FDE[30]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[31]: low_pc = 0x804da20, length = 71
FDE[31]: bytes = 20 00 00 00 20 01 00 00 20 da 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[31]: cie_offset = 948, cie_index = 2, fde_offset = 288
FDE[31]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[32]: low_pc = 0x804dac0, length = 71
FDE[32]: bytes = 20 00 00 00 44 01 00 00 c0 da 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[32]: cie_offset = 948, cie_index = 2, fde_offset = 324
FDE[32]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[33]: low_pc = 0x804db60, length = 71
FDE[33]: bytes = 20 00 00 00 68 01 00 00 60 db 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[33]: cie_offset = 948, cie_index = 2, fde_offset = 360
FDE[33]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[34]: low_pc = 0x804dc00, length = 71
FDE[34]: bytes = 20 00 00 00 8c 01 00 00 00 dc 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[34]: cie_offset = 948, cie_index = 2, fde_offset = 396
FDE[34]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[35]: low_pc = 0x804dca0, length = 71
FDE[35]: bytes = 20 00 00 00 b0 01 00 00 a0 dc 04 08 47 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[35]: cie_offset = 948, cie_index = 2, fde_offset = 432
FDE[35]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 60 2e 10
FDE[36]: low_pc = 0x804c5b4, length = 26
FDE[36]: bytes = 18 00 00 00 1c 00 00 00 b4 c5 04 08 1a 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[36]: cie_offset = 1412, cie_index = 3, fde_offset = 28
FDE[36]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[37]: low_pc = 0x804c5d0, length = 23
FDE[37]: bytes = 18 00 00 00 38 00 00 00 d0 c5 04 08 17 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[37]: cie_offset = 1412, cie_index = 3, fde_offset = 56
FDE[37]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[38]: low_pc = 0x804c640, length = 24
FDE[38]: bytes = 18 00 00 00 54 00 00 00 40 c6 04 08 18 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[38]: cie_offset = 1412, cie_index = 3, fde_offset = 84
FDE[38]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[39]: low_pc = 0x804c660, length = 32
FDE[39]: bytes = 18 00 00 00 70 00 00 00 60 c6 04 08 20 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[39]: cie_offset = 1412, cie_index = 3, fde_offset = 112
FDE[39]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[40]: low_pc = 0x804c680, length = 29
FDE[40]: bytes = 18 00 00 00 8c 00 00 00 80 c6 04 08 1d 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[40]: cie_offset = 1412, cie_index = 3, fde_offset = 140
FDE[40]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[41]: low_pc = 0x804c6a0, length = 36
FDE[41]: bytes = 18 00 00 00 a8 00 00 00 a0 c6 04 08 24 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[41]: cie_offset = 1412, cie_index = 3, fde_offset = 168
FDE[41]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[42]: low_pc = 0x804c6d0, length = 98
FDE[42]: bytes = 24 00 00 00 c4 00 00 00 d0 c6 04 08 62 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 43 2e 10 00 00 00
FDE[42]: cie_offset = 1412, cie_index = 3, fde_offset = 196
FDE[42]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 43 2e 10 00 00 00
FDE[43]: low_pc = 0x804c740, length = 107
FDE[43]: bytes = 24 00 00 00 ec 00 00 00 40 c7 04 08 6b 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 53 2e 10 7b 2e 00 00
FDE[43]: cie_offset = 1412, cie_index = 3, fde_offset = 236
FDE[43]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 53 2e 10 7b 2e 00 00
FDE[44]: low_pc = 0x804c7b0, length = 256
FDE[44]: bytes = 24 00 00 00 14 01 00 00 b0 c7 04 08 00 01 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 bf 2e 10 00 00 00
FDE[44]: cie_offset = 1412, cie_index = 3, fde_offset = 276
FDE[44]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 02 bf 2e 10 00 00 00
FDE[45]: low_pc = 0x804c8b0, length = 78
FDE[45]: bytes = 1c 00 00 00 3c 01 00 00 b0 c8 04 08 4e 00 00 00 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 00 00
FDE[45]: cie_offset = 1412, cie_index = 3, fde_offset = 316
FDE[45]: instructions = 41 0e 08 85 02 42 0d 05 41 86 03 41 83 04 00 00
FDE[46]: low_pc = 0x804c900, length = 480
FDE[46]: bytes = 40 00 00 00 5c 01 00 00 00 c9 04 08 e0 01 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 7d 2e 10 4c 2e 00 02 48 2e 10 02 54 2e 00 78 2e 10 4c 2e 00 02 44 2e 10 79 2e 08 49 2e 10 48 2e 00 00 00
FDE[46]: cie_offset = 1412, cie_index = 3, fde_offset = 348
FDE[46]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 7d 2e 10 4c 2e 00 02 48 2e 10 02 54 2e 00 78 2e 10 4c 2e 00 02 44 2e 10 79 2e 08 49 2e 10 48 2e 00 00 00
FDE[47]: low_pc = 0x804cae0, length = 37
FDE[47]: bytes = 1c 00 00 00 a0 01 00 00 e0 ca 04 08 25 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 52 2e 10 00 00
FDE[47]: cie_offset = 1412, cie_index = 3, fde_offset = 416
FDE[47]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 52 2e 10 00 00
FDE[48]: low_pc = 0x804cb10, length = 128
FDE[48]: bytes = 2c 00 00 00 c0 01 00 00 10 cb 04 08 80 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 62 2e 10 56 2e 08 49 2e 10 48 2e 00 6c 2e 10
FDE[48]: cie_offset = 1412, cie_index = 3, fde_offset = 448
FDE[48]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 62 2e 10 56 2e 08 49 2e 10 48 2e 00 6c 2e 10
FDE[49]: low_pc = 0x804cb90, length = 128
FDE[49]: bytes = 2c 00 00 00 f0 01 00 00 90 cb 04 08 80 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 62 2e 10 56 2e 08 49 2e 10 48 2e 00 6c 2e 10
FDE[49]: cie_offset = 1412, cie_index = 3, fde_offset = 496
FDE[49]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 62 2e 10 56 2e 08 49 2e 10 48 2e 00 6c 2e 10
FDE[50]: low_pc = 0x804cc10, length = 45
FDE[50]: bytes = 18 00 00 00 20 02 00 00 10 cc 04 08 2d 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[50]: cie_offset = 1412, cie_index = 3, fde_offset = 544
FDE[50]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[51]: low_pc = 0x804cc40, length = 43
FDE[51]: bytes = 18 00 00 00 3c 02 00 00 40 cc 04 08 2b 00 00 00 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[51]: cie_offset = 1412, cie_index = 3, fde_offset = 572
FDE[51]: instructions = 41 0e 08 85 02 42 0d 05 41 83 03 00
FDE[52]: low_pc = 0x804cde0, length = 89
FDE[52]: bytes = 20 00 00 00 18 00 00 00 e0 cd 04 08 59 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 6d 2e 20 00 00 00
FDE[52]: cie_offset = 2008, cie_index = 4, fde_offset = 24
FDE[52]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 6d 2e 20 00 00 00
FDE[53]: low_pc = 0x804ce40, length = 217
FDE[53]: bytes = 20 00 00 00 3c 00 00 00 40 ce 04 08 d9 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 02 40 2e 20 00 00
FDE[53]: cie_offset = 2008, cie_index = 4, fde_offset = 60
FDE[53]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 02 40 2e 20 00 00
FDE[54]: low_pc = 0x804d010, length = 117
FDE[54]: bytes = 24 00 00 00 60 00 00 00 10 d0 04 08 75 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5c 2e 10 02 48 2e 20
FDE[54]: cie_offset = 2008, cie_index = 4, fde_offset = 96
FDE[54]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 5c 2e 10 02 48 2e 20
FDE[55]: low_pc = 0x804d090, length = 190
FDE[55]: bytes = 24 00 00 00 88 00 00 00 90 d0 04 08 be 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 64 2e 10 02 89 2e 20
FDE[55]: cie_offset = 2008, cie_index = 4, fde_offset = 136
FDE[55]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 64 2e 10 02 89 2e 20
FDE[56]: low_pc = 0x804d150, length = 101
FDE[56]: bytes = 24 00 00 00 b0 00 00 00 50 d1 04 08 65 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 73 2e 20 00
FDE[56]: cie_offset = 2008, cie_index = 4, fde_offset = 176
FDE[56]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 73 2e 20 00
FDE[57]: low_pc = 0x804d1c0, length = 480
FDE[57]: bytes = 28 00 00 00 d8 00 00 00 c0 d1 04 08 e0 01 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 56 2e 10 02 f5 2e 20 02 91 2e 10
FDE[57]: cie_offset = 2008, cie_index = 4, fde_offset = 216
FDE[57]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 56 2e 10 02 f5 2e 20 02 91 2e 10
FDE[58]: low_pc = 0x804d3a0, length = 897
FDE[58]: bytes = 28 00 00 00 04 01 00 00 a0 d3 04 08 81 03 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 03 61 01 2e 20 00 00 00
FDE[58]: cie_offset = 2008, cie_index = 4, fde_offset = 260
FDE[58]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 03 61 01 2e 20 00 00 00
FDE[59]: low_pc = 0x804d730, length = 238
FDE[59]: bytes = 24 00 00 00 30 01 00 00 30 d7 04 08 ee 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 02 8f 2e 20
FDE[59]: cie_offset = 2008, cie_index = 4, fde_offset = 304
FDE[59]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 41 86 04 41 83 05 61 2e 10 02 8f 2e 20
FDE[60]: low_pc = 0x804e220, length = 73
FDE[60]: bytes = 20 00 00 00 58 01 00 00 20 e2 04 08 49 00 00 00 41 0e 08 85 02 42 0d 05 41 87 03 44 86 04 74 2e 20 00 00 00
FDE[60]: cie_offset = 2008, cie_index = 4, fde_offset = 344
FDE[60]: instructions = 41 0e 08 85 02 42 0d 05 41 87 03 44 86 04 74 2e 20 00 00 00
no FDE at 8048400
no FDE at 804842c
no FDE at 8048454
no FDE at 8048455
FDE[@80493fc]: cie_offset = 0, cie_index = 0, fde_offset = 28
EOF

exit 0
