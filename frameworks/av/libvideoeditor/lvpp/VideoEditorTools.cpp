/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "VideoEditorTools.h"
#include "PreviewRenderer.h"
/*+ Handle the image files here */
#include <utils/Log.h>
/*- Handle the image files here */

const M4VIFI_UInt8   M4VIFI_ClipTable[1256]
= {
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03,
0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13,
0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b,
0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23,
0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b,
0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33,
0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b,
0x3c, 0x3d, 0x3e, 0x3f, 0x40, 0x41, 0x42, 0x43,
0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b,
0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53,
0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b,
0x5c, 0x5d, 0x5e, 0x5f, 0x60, 0x61, 0x62, 0x63,
0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b,
0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0x73,
0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x7b,
0x7c, 0x7d, 0x7e, 0x7f, 0x80, 0x81, 0x82, 0x83,
0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x8b,
0x8c, 0x8d, 0x8e, 0x8f, 0x90, 0x91, 0x92, 0x93,
0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0x9b,
0x9c, 0x9d, 0x9e, 0x9f, 0xa0, 0xa1, 0xa2, 0xa3,
0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab,
0xac, 0xad, 0xae, 0xaf, 0xb0, 0xb1, 0xb2, 0xb3,
0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xbb,
0xbc, 0xbd, 0xbe, 0xbf, 0xc0, 0xc1, 0xc2, 0xc3,
0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xcb,
0xcc, 0xcd, 0xce, 0xcf, 0xd0, 0xd1, 0xd2, 0xd3,
0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xdb,
0xdc, 0xdd, 0xde, 0xdf, 0xe0, 0xe1, 0xe2, 0xe3,
0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xeb,
0xec, 0xed, 0xee, 0xef, 0xf0, 0xf1, 0xf2, 0xf3,
0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb,
0xfc, 0xfd, 0xfe, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
};

/* Division table for ( 65535/x ); x = 0 to 512 */
const M4VIFI_UInt16  M4VIFI_DivTable[512]
= {
0, 65535, 32768, 21845, 16384, 13107, 10922, 9362,
8192, 7281, 6553, 5957, 5461, 5041, 4681, 4369,
4096, 3855, 3640, 3449, 3276, 3120, 2978, 2849,
2730, 2621, 2520, 2427, 2340, 2259, 2184, 2114,
2048, 1985, 1927, 1872, 1820, 1771, 1724, 1680,
1638, 1598, 1560, 1524, 1489, 1456, 1424, 1394,
1365, 1337, 1310, 1285, 1260, 1236, 1213, 1191,
1170, 1149, 1129, 1110, 1092, 1074, 1057, 1040,
1024, 1008, 992, 978, 963, 949, 936, 923,
910, 897, 885, 873, 862, 851, 840, 829,
819, 809, 799, 789, 780, 771, 762, 753,
744, 736, 728, 720, 712, 704, 697, 689,
682, 675, 668, 661, 655, 648, 642, 636,
630, 624, 618, 612, 606, 601, 595, 590,
585, 579, 574, 569, 564, 560, 555, 550,
546, 541, 537, 532, 528, 524, 520, 516,
512, 508, 504, 500, 496, 492, 489, 485,
481, 478, 474, 471, 468, 464, 461, 458,
455, 451, 448, 445, 442, 439, 436, 434,
431, 428, 425, 422, 420, 417, 414, 412,
409, 407, 404, 402, 399, 397, 394, 392,
390, 387, 385, 383, 381, 378, 376, 374,
372, 370, 368, 366, 364, 362, 360, 358,
356, 354, 352, 350, 348, 346, 344, 343,
341, 339, 337, 336, 334, 332, 330, 329,
327, 326, 324, 322, 321, 319, 318, 316,
315, 313, 312, 310, 309, 307, 306, 304,
303, 302, 300, 299, 297, 296, 295, 293,
292, 291, 289, 288, 287, 286, 284, 283,
282, 281, 280, 278, 277, 276, 275, 274,
273, 271, 270, 269, 268, 267, 266, 265,
264, 263, 262, 261, 260, 259, 258, 257,
256, 255, 254, 253, 252, 251, 250, 249,
248, 247, 246, 245, 244, 243, 242, 241,
240, 240, 239, 238, 237, 236, 235, 234,
234, 233, 232, 231, 230, 229, 229, 228,
227, 226, 225, 225, 224, 223, 222, 222,
221, 220, 219, 219, 218, 217, 217, 216,
215, 214, 214, 213, 212, 212, 211, 210,
210, 209, 208, 208, 207, 206, 206, 205,
204, 204, 203, 202, 202, 201, 201, 200,
199, 199, 198, 197, 197, 196, 196, 195,
195, 194, 193, 193, 192, 192, 191, 191,
190, 189, 189, 188, 188, 187, 187, 186,
186, 185, 185, 184, 184, 183, 183, 182,
182, 181, 181, 180, 180, 179, 179, 178,
178, 177, 177, 176, 176, 175, 175, 174,
174, 173, 173, 172, 172, 172, 171, 171,
170, 170, 169, 169, 168, 168, 168, 167,
167, 166, 166, 165, 165, 165, 164, 164,
163, 163, 163, 162, 162, 161, 161, 161,
160, 160, 159, 159, 159, 158, 158, 157,
157, 157, 156, 156, 156, 155, 155, 154,
154, 154, 153, 153, 153, 152, 152, 152,
151, 151, 151, 150, 150, 149, 149, 149,
148, 148, 148, 147, 147, 147, 146, 146,
146, 145, 145, 145, 144, 144, 144, 144,
143, 143, 143, 142, 142, 142, 141, 141,
141, 140, 140, 140, 140, 139, 139, 139,
138, 138, 138, 137, 137, 137, 137, 136,
136, 136, 135, 135, 135, 135, 134, 134,
134, 134, 133, 133, 133, 132, 132, 132,
132, 131, 131, 131, 131, 130, 130, 130,
130, 129, 129, 129, 129, 128, 128, 128
};

const M4VIFI_Int32  const_storage1[8]
= {
0x00002568, 0x00003343,0x00000649,0x00000d0f, 0x0000D86C, 0x0000D83B, 0x00010000, 0x00010000
};

const M4VIFI_Int32  const_storage[8]
= {
0x00002568, 0x00003343, 0x1BF800, 0x00000649, 0x00000d0f, 0x110180, 0x40cf, 0x22BE00
};


const M4VIFI_UInt16  *M4VIFI_DivTable_zero
 = &M4VIFI_DivTable[0];

const M4VIFI_UInt8   *M4VIFI_ClipTable_zero
 = &M4VIFI_ClipTable[500];

M4VIFI_UInt8 M4VIFI_YUV420PlanarToYUV420Semiplanar(void *user_data,
    M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut ) {

    M4VIFI_UInt32 i;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest, *p_buf_src_u, *p_buf_src_v;
    M4VIFI_UInt8    return_code = M4VIFI_OK;

    /* the filter is implemented with the assumption that the width is equal to stride */
    if(PlaneIn[0].u_width != PlaneIn[0].u_stride)
        return M4VIFI_INVALID_PARAM;

    /* The input Y Plane is the same as the output Y Plane */
    p_buf_src = &(PlaneIn[0].pac_data[PlaneIn[0].u_topleft]);
    p_buf_dest = &(PlaneOut[0].pac_data[PlaneOut[0].u_topleft]);
    memcpy((void *)p_buf_dest,(void *)p_buf_src ,
        PlaneOut[0].u_width * PlaneOut[0].u_height);

    /* The U and V components are planar. The need to be made interleaved */
    p_buf_src_u = &(PlaneIn[1].pac_data[PlaneIn[1].u_topleft]);
    p_buf_src_v = &(PlaneIn[2].pac_data[PlaneIn[2].u_topleft]);
    p_buf_dest  = &(PlaneOut[1].pac_data[PlaneOut[1].u_topleft]);

    for(i = 0; i < PlaneOut[1].u_width*PlaneOut[1].u_height; i++)
    {
        *p_buf_dest++ = *p_buf_src_u++;
        *p_buf_dest++ = *p_buf_src_v++;
    }
    return return_code;
}

M4VIFI_UInt8 M4VIFI_SemiplanarYUV420toYUV420(void *user_data,
    M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut ) {

     M4VIFI_UInt32 i;
     M4VIFI_UInt8 *p_buf_src, *p_buf_dest, *p_buf_src_u, *p_buf_src_v;
     M4VIFI_UInt8 *p_buf_dest_u,*p_buf_dest_v,*p_buf_src_uv;
     M4VIFI_UInt8     return_code = M4VIFI_OK;

     /* the filter is implemented with the assumption that the width is equal to stride */
     if(PlaneIn[0].u_width != PlaneIn[0].u_stride)
        return M4VIFI_INVALID_PARAM;

     /* The input Y Plane is the same as the output Y Plane */
     p_buf_src = &(PlaneIn[0].pac_data[PlaneIn[0].u_topleft]);
     p_buf_dest = &(PlaneOut[0].pac_data[PlaneOut[0].u_topleft]);
     memcpy((void *)p_buf_dest,(void *)p_buf_src ,
         PlaneOut[0].u_width * PlaneOut[0].u_height);

     /* The U and V components are planar. The need to be made interleaved */
     p_buf_src_uv = &(PlaneIn[1].pac_data[PlaneIn[1].u_topleft]);
     p_buf_dest_u  = &(PlaneOut[1].pac_data[PlaneOut[1].u_topleft]);
     p_buf_dest_v  = &(PlaneOut[2].pac_data[PlaneOut[2].u_topleft]);

     for(i = 0; i < PlaneOut[1].u_width*PlaneOut[1].u_height; i++)
     {
        *p_buf_dest_u++ = *p_buf_src_uv++;
        *p_buf_dest_v++ = *p_buf_src_uv++;
     }
     return return_code;
}


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext,
 *                                                  M4VIFI_ImagePlane *PlaneIn,
 *                                                  M4VIFI_ImagePlane *PlaneOut,
 *                                                  M4VSS3GPP_ExternalProgress *pProgress,
 *                                                  M4OSA_UInt32 uiEffectKind)
 *
 * @brief   This function apply a color effect on an input YUV420 planar frame
 * @note
 * @param   pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param   PlaneIn         (IN) Input YUV420 planar
 * @param   PlaneOut        (IN/OUT) Output YUV420 planar
 * @param   pProgress       (IN/OUT) Progress indication (0-100)
 * @param   uiEffectKind    (IN) Unused
 *
 * @return  M4VIFI_OK:  No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext,
            M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut,
            M4VSS3GPP_ExternalProgress *pProgress, M4OSA_UInt32 uiEffectKind) {

    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i,j;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest;
    M4xVSS_ColorStruct* ColorContext = (M4xVSS_ColorStruct*)pFunctionContext;

    for (plane_number = 0; plane_number < 3; plane_number++)
    {
        p_buf_src =
         &(PlaneIn[plane_number].pac_data[PlaneIn[plane_number].u_topleft]);

        p_buf_dest =
         &(PlaneOut[plane_number].pac_data[PlaneOut[plane_number].u_topleft]);
        for (i = 0; i < PlaneOut[plane_number].u_height; i++)
        {
            /**
             * Chrominance */
            if(plane_number==1 || plane_number==2)
            {
                //switch ((M4OSA_UInt32)pFunctionContext) // commented because a structure for the effects context exist
                switch (ColorContext->colorEffectType)
                {
                case M4xVSS_kVideoEffectType_BlackAndWhite:
                    memset((void *)p_buf_dest,128,
                     PlaneIn[plane_number].u_width);
                    break;
                case M4xVSS_kVideoEffectType_Pink:
                    memset((void *)p_buf_dest,255,
                     PlaneIn[plane_number].u_width);
                    break;
                case M4xVSS_kVideoEffectType_Green:
                    memset((void *)p_buf_dest,0,
                     PlaneIn[plane_number].u_width);
                    break;
                case M4xVSS_kVideoEffectType_Sepia:
                    if(plane_number==1)
                    {
                        memset((void *)p_buf_dest,117,
                         PlaneIn[plane_number].u_width);
                    }
                    else
                    {
                        memset((void *)p_buf_dest,139,
                         PlaneIn[plane_number].u_width);
                    }
                    break;
                case M4xVSS_kVideoEffectType_Negative:
                    memcpy((void *)p_buf_dest,
                     (void *)p_buf_src ,PlaneOut[plane_number].u_width);
                    break;

                case M4xVSS_kVideoEffectType_ColorRGB16:
                    {
                        M4OSA_UInt16 r = 0,g = 0,b = 0,y = 0,u = 0,v = 0;

                        /*first get the r, g, b*/
                        b = (ColorContext->rgb16ColorData &  0x001f);
                        g = (ColorContext->rgb16ColorData &  0x07e0)>>5;
                        r = (ColorContext->rgb16ColorData &  0xf800)>>11;

                        /*keep y, but replace u and v*/
                        if(plane_number==1)
                        {
                            /*then convert to u*/
                            u = U16(r, g, b);
                            memset((void *)p_buf_dest,(M4OSA_UInt8)u,
                             PlaneIn[plane_number].u_width);
                        }
                        if(plane_number==2)
                        {
                            /*then convert to v*/
                            v = V16(r, g, b);
                            memset((void *)p_buf_dest,(M4OSA_UInt8)v,
                             PlaneIn[plane_number].u_width);
                        }
                    }
                    break;
                case M4xVSS_kVideoEffectType_Gradient:
                    {
                        M4OSA_UInt16 r = 0,g = 0,b = 0,y = 0,u = 0,v = 0;

                        /*first get the r, g, b*/
                        b = (ColorContext->rgb16ColorData &  0x001f);
                        g = (ColorContext->rgb16ColorData &  0x07e0)>>5;
                        r = (ColorContext->rgb16ColorData &  0xf800)>>11;

                        /*for color gradation*/
                        b = (M4OSA_UInt16)( b - ((b*i)/PlaneIn[plane_number].u_height));
                        g = (M4OSA_UInt16)(g - ((g*i)/PlaneIn[plane_number].u_height));
                        r = (M4OSA_UInt16)(r - ((r*i)/PlaneIn[plane_number].u_height));

                        /*keep y, but replace u and v*/
                        if(plane_number==1)
                        {
                            /*then convert to u*/
                            u = U16(r, g, b);
                            memset((void *)p_buf_dest,(M4OSA_UInt8)u,
                             PlaneIn[plane_number].u_width);
                        }
                        if(plane_number==2)
                        {
                            /*then convert to v*/
                            v = V16(r, g, b);
                            memset((void *)p_buf_dest,(M4OSA_UInt8)v,
                             PlaneIn[plane_number].u_width);
                        }
                    }
                    break;
                default:
                    return M4VIFI_INVALID_PARAM;
                }
            }
            /**
             * Luminance */
            else
            {
                //switch ((M4OSA_UInt32)pFunctionContext)// commented because a structure for the effects context exist
                switch (ColorContext->colorEffectType)
                {
                case M4xVSS_kVideoEffectType_Negative:
                    for(j=0;j<PlaneOut[plane_number].u_width;j++)
                    {
                            p_buf_dest[j] = 255 - p_buf_src[j];
                    }
                    break;
                default:
                    memcpy((void *)p_buf_dest,
                     (void *)p_buf_src ,PlaneOut[plane_number].u_width);
                    break;
                }
            }
            p_buf_src += PlaneIn[plane_number].u_stride;
            p_buf_dest += PlaneOut[plane_number].u_stride;
        }
    }

    return M4VIFI_OK;
}

/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectFraming(M4OSA_Void *pFunctionContext,
 *                                                  M4VIFI_ImagePlane *PlaneIn,
 *                                                  M4VIFI_ImagePlane *PlaneOut,
 *                                                  M4VSS3GPP_ExternalProgress *pProgress,
 *                                                  M4OSA_UInt32 uiEffectKind)
 *
 * @brief   This function add a fixed or animated image on an input YUV420 planar frame
 * @note
 * @param   pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param   PlaneIn         (IN) Input YUV420 planar
 * @param   PlaneOut        (IN/OUT) Output YUV420 planar
 * @param   pProgress       (IN/OUT) Progress indication (0-100)
 * @param   uiEffectKind    (IN) Unused
 *
 * @return  M4VIFI_OK:  No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectFraming(
            M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn[3],
            M4VIFI_ImagePlane *PlaneOut, M4VSS3GPP_ExternalProgress *pProgress,
            M4OSA_UInt32 uiEffectKind ) {

    M4VIFI_UInt32 x,y;

    M4VIFI_UInt8 *p_in_Y = PlaneIn[0].pac_data;
    M4VIFI_UInt8 *p_in_U = PlaneIn[1].pac_data;
    M4VIFI_UInt8 *p_in_V = PlaneIn[2].pac_data;

    M4xVSS_FramingStruct* Framing = M4OSA_NULL;
    M4xVSS_FramingStruct* currentFraming = M4OSA_NULL;
    M4VIFI_UInt8 *FramingRGB = M4OSA_NULL;

    M4VIFI_UInt8 *p_out0;
    M4VIFI_UInt8 *p_out1;
    M4VIFI_UInt8 *p_out2;

    M4VIFI_UInt32 topleft[2];

    M4OSA_UInt8 transparent1 =
     (M4OSA_UInt8)((TRANSPARENT_COLOR & 0xFF00)>>8);
    M4OSA_UInt8 transparent2 = (M4OSA_UInt8)TRANSPARENT_COLOR;

#ifndef DECODE_GIF_ON_SAVING
    Framing = (M4xVSS_FramingStruct *)userData;
    currentFraming = (M4xVSS_FramingStruct *)Framing->pCurrent;
    FramingRGB = Framing->FramingRgb->pac_data;
#endif /*DECODE_GIF_ON_SAVING*/

#ifdef DECODE_GIF_ON_SAVING
    M4OSA_ERR err;
    Framing =
     (M4xVSS_FramingStruct *)((M4xVSS_FramingContext*)userData)->aFramingCtx;
    if(Framing == M4OSA_NULL)
    {
        ((M4xVSS_FramingContext*)userData)->clipTime = pProgress->uiOutputTime;
        err = M4xVSS_internalDecodeGIF(userData);
        if(M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4VSS3GPP_externalVideoEffectFraming: \
                Error in M4xVSS_internalDecodeGIF: 0x%x", err);
            return err;
        }
        Framing =
         (M4xVSS_FramingStruct *)((M4xVSS_FramingContext*)userData)->aFramingCtx;
        /* Initializes first GIF time */
        ((M4xVSS_FramingContext*)userData)->current_gif_time =
          pProgress->uiOutputTime;
    }
    currentFraming = (M4xVSS_FramingStruct *)Framing;
    FramingRGB = Framing->FramingRgb->pac_data;
#endif /*DECODE_GIF_ON_SAVING*/

    /**
     * Initialize input / output plane pointers */
    p_in_Y += PlaneIn[0].u_topleft;
    p_in_U += PlaneIn[1].u_topleft;
    p_in_V += PlaneIn[2].u_topleft;

    p_out0 = PlaneOut[0].pac_data;
    p_out1 = PlaneOut[1].pac_data;
    p_out2 = PlaneOut[2].pac_data;

    /**
     * Depending on time, initialize Framing frame to use */
    if(Framing->previousClipTime == -1)
    {
        Framing->previousClipTime = pProgress->uiOutputTime;
    }

    /**
     * If the current clip time has reach the duration of one frame of the framing picture
     * we need to step to next framing picture */
#ifdef DECODE_GIF_ON_SAVING
    if(((M4xVSS_FramingContext*)userData)->b_animated == M4OSA_TRUE)
    {
        while((((M4xVSS_FramingContext*)userData)->current_gif_time + currentFraming->duration) < pProgress->uiOutputTime)
        {
            ((M4xVSS_FramingContext*)userData)->clipTime =
             pProgress->uiOutputTime;

            err = M4xVSS_internalDecodeGIF(userData);
            if(M4NO_ERROR != err)
            {
                M4OSA_TRACE1_1("M4VSS3GPP_externalVideoEffectFraming: Error in M4xVSS_internalDecodeGIF: 0x%x", err);
                return err;
            }
            if(currentFraming->duration != 0)
            {
                ((M4xVSS_FramingContext*)userData)->current_gif_time += currentFraming->duration;
            }
            else
            {
                ((M4xVSS_FramingContext*)userData)->current_gif_time +=
                 pProgress->uiOutputTime - Framing->previousClipTime;
            }
            Framing = (M4xVSS_FramingStruct *)((M4xVSS_FramingContext*)userData)->aFramingCtx;
            currentFraming = (M4xVSS_FramingStruct *)Framing;
            FramingRGB = Framing->FramingRgb->pac_data;
        }
    }
#else
            Framing->pCurrent = currentFraming->pNext;
            currentFraming = (M4xVSS_FramingStruct*)Framing->pCurrent;
#endif /*DECODE_GIF_ON_SAVING*/

    Framing->previousClipTime = pProgress->uiOutputTime;
    FramingRGB = currentFraming->FramingRgb->pac_data;
    topleft[0] = currentFraming->topleft_x;
    topleft[1] = currentFraming->topleft_y;

    for( x=0 ;x < PlaneIn[0].u_height ; x++)
    {
        for( y=0 ;y < PlaneIn[0].u_width ; y++)
        {
            /**
             * To handle framing with input size != output size
             * Framing is applyed if coordinates matches between framing/topleft and input plane */
            if( y < (topleft[0] + currentFraming->FramingYuv[0].u_width)  &&
                y >= topleft[0] &&
                x < (topleft[1] + currentFraming->FramingYuv[0].u_height) &&
                x >= topleft[1])
            {

                /*Alpha blending support*/
                M4OSA_Float alphaBlending = 1;
#ifdef DECODE_GIF_ON_SAVING
                M4xVSS_internalEffectsAlphaBlending* alphaBlendingStruct =
                 (M4xVSS_internalEffectsAlphaBlending*)((M4xVSS_FramingContext*)userData)->alphaBlendingStruct;
#else
                M4xVSS_internalEffectsAlphaBlending* alphaBlendingStruct =
                 (M4xVSS_internalEffectsAlphaBlending*)((M4xVSS_FramingStruct*)userData)->alphaBlendingStruct;
#endif //#ifdef DECODE_GIF_ON_SAVING

                if(alphaBlendingStruct != M4OSA_NULL)
                {
                    if(pProgress->uiProgress < (M4OSA_UInt32)(alphaBlendingStruct->m_fadeInTime*10))
                    {
                        alphaBlending = ((M4OSA_Float)(alphaBlendingStruct->m_middle - alphaBlendingStruct->m_start)*pProgress->uiProgress/(alphaBlendingStruct->m_fadeInTime*10));
                        alphaBlending += alphaBlendingStruct->m_start;
                        alphaBlending /= 100;
                    }
                    else if(pProgress->uiProgress >= (M4OSA_UInt32)(alphaBlendingStruct->m_fadeInTime*10) && pProgress->uiProgress < 1000 - (M4OSA_UInt32)(alphaBlendingStruct->m_fadeOutTime*10))
                    {
                        alphaBlending = (M4OSA_Float)((M4OSA_Float)alphaBlendingStruct->m_middle/100);
                    }
                    else if(pProgress->uiProgress >= 1000 - (M4OSA_UInt32)(alphaBlendingStruct->m_fadeOutTime*10))
                    {
                        alphaBlending = ((M4OSA_Float)(alphaBlendingStruct->m_middle - alphaBlendingStruct->m_end))*(1000 - pProgress->uiProgress)/(alphaBlendingStruct->m_fadeOutTime*10);
                        alphaBlending += alphaBlendingStruct->m_end;
                        alphaBlending /= 100;
                    }
                }

                /**/

                if((*(FramingRGB)==transparent1) && (*(FramingRGB+1)==transparent2))
                {
                    *( p_out0+y+x*PlaneOut[0].u_stride)=(*(p_in_Y+y+x*PlaneIn[0].u_stride));
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=(*(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride));
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=(*(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride));
                }
                else
                {
                    *( p_out0+y+x*PlaneOut[0].u_stride)=(*(currentFraming->FramingYuv[0].pac_data+(y-topleft[0])+(x-topleft[1])*currentFraming->FramingYuv[0].u_stride))*alphaBlending;
                    *( p_out0+y+x*PlaneOut[0].u_stride)+=(*(p_in_Y+y+x*PlaneIn[0].u_stride))*(1-alphaBlending);
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=(*(currentFraming->FramingYuv[1].pac_data+((y-topleft[0])>>1)+((x-topleft[1])>>1)*currentFraming->FramingYuv[1].u_stride))*alphaBlending;
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)+=(*(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride))*(1-alphaBlending);
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=(*(currentFraming->FramingYuv[2].pac_data+((y-topleft[0])>>1)+((x-topleft[1])>>1)*currentFraming->FramingYuv[2].u_stride))*alphaBlending;
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)+=(*(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride))*(1-alphaBlending);
                }
                if( PlaneIn[0].u_width < (topleft[0] + currentFraming->FramingYuv[0].u_width) &&
                    y == PlaneIn[0].u_width-1)
                {
                    FramingRGB = FramingRGB + 2 * (topleft[0] + currentFraming->FramingYuv[0].u_width - PlaneIn[0].u_width + 1);
                }
                else
                {
                    FramingRGB = FramingRGB + 2;
                }
            }
            /**
             * Just copy input plane to output plane */
            else
            {
                *( p_out0+y+x*PlaneOut[0].u_stride)=*(p_in_Y+y+x*PlaneIn[0].u_stride);
                *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=*(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride);
                *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=*(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride);
            }
        }
    }

#ifdef DECODE_GIF_ON_SAVING
    if(pProgress->bIsLast == M4OSA_TRUE
        && (M4OSA_Bool)((M4xVSS_FramingContext*)userData)->b_IsFileGif == M4OSA_TRUE)
    {
        M4xVSS_internalDecodeGIF_Cleaning((M4xVSS_FramingContext*)userData);
    }
#endif /*DECODE_GIF_ON_SAVING*/
    return M4VIFI_OK;
}


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectFifties(M4OSA_Void *pFunctionContext,
 *                                                  M4VIFI_ImagePlane *PlaneIn,
 *                                                  M4VIFI_ImagePlane *PlaneOut,
 *                                                  M4VSS3GPP_ExternalProgress *pProgress,
 *                                                  M4OSA_UInt32 uiEffectKind)
 *
 * @brief   This function make a video look as if it was taken in the fifties
 * @note
 * @param   pUserData       (IN) Context
 * @param   pPlaneIn        (IN) Input YUV420 planar
 * @param   pPlaneOut       (IN/OUT) Output YUV420 planar
 * @param   pProgress       (IN/OUT) Progress indication (0-100)
 * @param   uiEffectKind    (IN) Unused
 *
 * @return  M4VIFI_OK:          No error
 * @return  M4ERR_PARAMETER:    pFiftiesData, pPlaneOut or pProgress are NULL (DEBUG only)
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectFifties(
    M4OSA_Void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
    M4VIFI_ImagePlane *pPlaneOut, M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind )
{
    M4VIFI_UInt32 x, y, xShift;
    M4VIFI_UInt8 *pInY = pPlaneIn[0].pac_data;
    M4VIFI_UInt8 *pOutY, *pInYbegin;
    M4VIFI_UInt8 *pInCr,* pOutCr;
    M4VIFI_Int32 plane_number;

    /* Internal context*/
    M4xVSS_FiftiesStruct* p_FiftiesData = (M4xVSS_FiftiesStruct *)pUserData;

    /* Initialize input / output plane pointers */
    pInY += pPlaneIn[0].u_topleft;
    pOutY = pPlaneOut[0].pac_data;
    pInYbegin  = pInY;

    /* Initialize the random */
    if(p_FiftiesData->previousClipTime < 0)
    {
        M4OSA_randInit();
        M4OSA_rand((M4OSA_Int32*)&(p_FiftiesData->shiftRandomValue), (pPlaneIn[0].u_height) >> 4);
        M4OSA_rand((M4OSA_Int32*)&(p_FiftiesData->stripeRandomValue), (pPlaneIn[0].u_width)<< 2);
        p_FiftiesData->previousClipTime = pProgress->uiOutputTime;
    }

    /* Choose random values if we have reached the duration of a partial effect */
    else if( (pProgress->uiOutputTime - p_FiftiesData->previousClipTime) > p_FiftiesData->fiftiesEffectDuration)
    {
        M4OSA_rand((M4OSA_Int32*)&(p_FiftiesData->shiftRandomValue), (pPlaneIn[0].u_height) >> 4);
        M4OSA_rand((M4OSA_Int32*)&(p_FiftiesData->stripeRandomValue), (pPlaneIn[0].u_width)<< 2);
        p_FiftiesData->previousClipTime = pProgress->uiOutputTime;
    }

    /* Put in Sepia the chrominance */
    for (plane_number = 1; plane_number < 3; plane_number++)
    {
        pInCr  = pPlaneIn[plane_number].pac_data  + pPlaneIn[plane_number].u_topleft;
        pOutCr = pPlaneOut[plane_number].pac_data + pPlaneOut[plane_number].u_topleft;

        for (x = 0; x < pPlaneOut[plane_number].u_height; x++)
        {
            if (1 == plane_number)
                memset((void *)pOutCr, 117,pPlaneIn[plane_number].u_width); /* U value */
            else
                memset((void *)pOutCr, 139,pPlaneIn[plane_number].u_width); /* V value */

            pInCr  += pPlaneIn[plane_number].u_stride;
            pOutCr += pPlaneOut[plane_number].u_stride;
        }
    }

    /* Compute the new pixels values */
    for( x = 0 ; x < pPlaneIn[0].u_height ; x++)
    {
        M4VIFI_UInt8 *p_outYtmp, *p_inYtmp;

        /* Compute the xShift (random value) */
        if (0 == (p_FiftiesData->shiftRandomValue % 5 ))
            xShift = (x + p_FiftiesData->shiftRandomValue ) % (pPlaneIn[0].u_height - 1);
        else
            xShift = (x + (pPlaneIn[0].u_height - p_FiftiesData->shiftRandomValue) ) % (pPlaneIn[0].u_height - 1);

        /* Initialize the pointers */
        p_outYtmp = pOutY + 1;                                    /* yShift of 1 pixel */
        p_inYtmp  = pInYbegin + (xShift * pPlaneIn[0].u_stride);  /* Apply the xShift */

        for( y = 0 ; y < pPlaneIn[0].u_width ; y++)
        {
            /* Set Y value */
            if (xShift > (pPlaneIn[0].u_height - 4))
                *p_outYtmp = 40;        /* Add some horizontal black lines between the two parts of the image */
            else if ( y == p_FiftiesData->stripeRandomValue)
                *p_outYtmp = 90;        /* Add a random vertical line for the bulk */
            else
                *p_outYtmp = *p_inYtmp;


            /* Go to the next pixel */
            p_outYtmp++;
            p_inYtmp++;

            /* Restart at the beginning of the line for the last pixel*/
            if (y == (pPlaneIn[0].u_width - 2))
                p_outYtmp = pOutY;
        }

        /* Go to the next line */
        pOutY += pPlaneOut[0].u_stride;
    }

    return M4VIFI_OK;
}

unsigned char M4VFL_modifyLumaWithScale(M4ViComImagePlane *plane_in,
                                        M4ViComImagePlane *plane_out,
                                        unsigned long lum_factor,
                                        void *user_data)
{
    unsigned short *p_src, *p_dest, *p_src_line, *p_dest_line;
    unsigned char *p_csrc, *p_cdest, *p_csrc_line, *p_cdest_line;
    unsigned long pix_src;
    unsigned long u_outpx, u_outpx2;
    unsigned long u_width, u_stride, u_stride_out,u_height, pix;
    long i, j;

    /* copy or filter chroma */
    u_width = plane_in[1].u_width;
    u_height = plane_in[1].u_height;
    u_stride = plane_in[1].u_stride;
    u_stride_out = plane_out[1].u_stride;
    p_cdest_line = (unsigned char *) &plane_out[1].pac_data[plane_out[1].u_topleft];
    p_csrc_line = (unsigned char *) &plane_in[1].pac_data[plane_in[1].u_topleft];

    if (lum_factor > 256)
    {
        p_cdest = (unsigned char *) &plane_out[2].pac_data[plane_out[2].u_topleft];
        p_csrc = (unsigned char *) &plane_in[2].pac_data[plane_in[2].u_topleft];
        /* copy chroma */
        for (j = u_height; j != 0; j--)
        {
            for (i = u_width; i != 0; i--)
            {
                memcpy((void *)p_cdest_line, (void *)p_csrc_line, u_width);
                memcpy((void *)p_cdest, (void *)p_csrc, u_width);
            }
            p_cdest_line += u_stride_out;
            p_cdest += u_stride_out;
            p_csrc_line += u_stride;
            p_csrc += u_stride;
        }
    }
    else
    {
        /* filter chroma */
        pix = (1024 - lum_factor) << 7;
        for (j = u_height; j != 0; j--)
        {
            p_cdest = p_cdest_line;
            p_csrc = p_csrc_line;
            for (i = u_width; i != 0; i--)
            {
                *p_cdest++ = ((pix + (*p_csrc++ & 0xFF) * lum_factor) >> LUM_FACTOR_MAX);
            }
            p_cdest_line += u_stride_out;
            p_csrc_line += u_stride;
        }
        p_cdest_line = (unsigned char *) &plane_out[2].pac_data[plane_out[2].u_topleft];
        p_csrc_line = (unsigned char *) &plane_in[2].pac_data[plane_in[2].u_topleft];
        for (j = u_height; j != 0; j--)
        {
            p_cdest = p_cdest_line;
            p_csrc = p_csrc_line;
            for (i = u_width; i != 0; i--)
            {
                *p_cdest++ = ((pix + (*p_csrc & 0xFF) * lum_factor) >> LUM_FACTOR_MAX);
            }
            p_cdest_line += u_stride_out;
            p_csrc_line += u_stride;
        }
    }
    /* apply luma factor */
    u_width = plane_in[0].u_width;
    u_height = plane_in[0].u_height;
    u_stride = (plane_in[0].u_stride >> 1);
    u_stride_out = (plane_out[0].u_stride >> 1);
    p_dest = (unsigned short *) &plane_out[0].pac_data[plane_out[0].u_topleft];
    p_src = (unsigned short *) &plane_in[0].pac_data[plane_in[0].u_topleft];
    p_dest_line = p_dest;
    p_src_line = p_src;

    for (j = u_height; j != 0; j--)
    {
        p_dest = p_dest_line;
        p_src = p_src_line;
        for (i = (u_width >> 1); i != 0; i--)
        {
            pix_src = (unsigned long) *p_src++;
            pix = pix_src & 0xFF;
            u_outpx = ((pix * lum_factor) >> LUM_FACTOR_MAX);
            pix = ((pix_src & 0xFF00) >> 8);
            u_outpx2 = (((pix * lum_factor) >> LUM_FACTOR_MAX)<< 8) ;
            *p_dest++ = (unsigned short) (u_outpx2 | u_outpx);
        }
        p_dest_line += u_stride_out;
        p_src_line += u_stride;
    }

    return 0;
}

/******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx)
 * @brief   This function converts an RGB565 plane to YUV420 planar
 * @note    It is used only for framing effect
 *          It allocates output YUV planes
 * @param   framingCtx  (IN) The framing struct containing input RGB565 plane
 *
 * @return  M4NO_ERROR: No error
 * @return  M4ERR_PARAMETER: At least one of the function parameters is null
 * @return  M4ERR_ALLOC: Allocation error (no more memory)
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx)
{
    M4OSA_ERR err;

    /**
     * Allocate output YUV planes */
    framingCtx->FramingYuv = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane), M4VS, (M4OSA_Char*)"M4xVSS_internalConvertRGBtoYUV: Output plane YUV");
    if(framingCtx->FramingYuv == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[0].u_width = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].u_height = framingCtx->FramingRgb->u_height;
    framingCtx->FramingYuv[0].u_topleft = 0;
    framingCtx->FramingYuv[0].u_stride = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc((framingCtx->FramingYuv[0].u_width*framingCtx->FramingYuv[0].u_height*3)>>1, M4VS, (M4OSA_Char*)"Alloc for the Convertion output YUV");;
    if(framingCtx->FramingYuv[0].pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[1].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[1].u_topleft = 0;
    framingCtx->FramingYuv[1].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].pac_data = framingCtx->FramingYuv[0].pac_data + framingCtx->FramingYuv[0].u_width * framingCtx->FramingYuv[0].u_height;
    framingCtx->FramingYuv[2].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[2].u_topleft = 0;
    framingCtx->FramingYuv[2].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].pac_data = framingCtx->FramingYuv[1].pac_data + framingCtx->FramingYuv[1].u_width * framingCtx->FramingYuv[1].u_height;

    /**
     * Convert input RGB 565 to YUV 420 to be able to merge it with output video in framing effect */
    err = M4VIFI_xVSS_RGB565toYUV420(M4OSA_NULL, framingCtx->FramingRgb, framingCtx->FramingYuv);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalConvertRGBtoYUV: error when converting from RGB to YUV: 0x%x\n", err);
    }

    framingCtx->duration = 0;
    framingCtx->previousClipTime = -1;
    framingCtx->previewOffsetClipTime = -1;

    /**
     * Only one element in the chained list (no animated image with RGB buffer...) */
    framingCtx->pCurrent = framingCtx;
    framingCtx->pNext = framingCtx;

    return M4NO_ERROR;
}

/******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalConvertRGB888toYUV(M4xVSS_FramingStruct* framingCtx)
 * @brief   This function converts an RGB888 plane to YUV420 planar
 * @note    It is used only for framing effect
 *          It allocates output YUV planes
 * @param   framingCtx  (IN) The framing struct containing input RGB888 plane
 *
 * @return  M4NO_ERROR: No error
 * @return  M4ERR_PARAMETER: At least one of the function parameters is null
 * @return  M4ERR_ALLOC: Allocation error (no more memory)
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_internalConvertRGB888toYUV(M4xVSS_FramingStruct* framingCtx)
{
    M4OSA_ERR err;

    /**
     * Allocate output YUV planes */
    framingCtx->FramingYuv = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane), M4VS, (M4OSA_Char*)"M4xVSS_internalConvertRGBtoYUV: Output plane YUV");
    if(framingCtx->FramingYuv == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[0].u_width = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].u_height = framingCtx->FramingRgb->u_height;
    framingCtx->FramingYuv[0].u_topleft = 0;
    framingCtx->FramingYuv[0].u_stride = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc((framingCtx->FramingYuv[0].u_width*framingCtx->FramingYuv[0].u_height*3)>>1, M4VS, (M4OSA_Char*)"Alloc for the Convertion output YUV");;
    if(framingCtx->FramingYuv[0].pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[1].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[1].u_topleft = 0;
    framingCtx->FramingYuv[1].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].pac_data = framingCtx->FramingYuv[0].pac_data + framingCtx->FramingYuv[0].u_width * framingCtx->FramingYuv[0].u_height;
    framingCtx->FramingYuv[2].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[2].u_topleft = 0;
    framingCtx->FramingYuv[2].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].pac_data = framingCtx->FramingYuv[1].pac_data + framingCtx->FramingYuv[1].u_width * framingCtx->FramingYuv[1].u_height;

    /**
     * Convert input RGB888 to YUV 420 to be able to merge it with output video in framing effect */
    err = M4VIFI_RGB888toYUV420(M4OSA_NULL, framingCtx->FramingRgb, framingCtx->FramingYuv);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalConvertRGBtoYUV: error when converting from RGB to YUV: 0x%x\n", err);
    }

    framingCtx->duration = 0;
    framingCtx->previousClipTime = -1;
    framingCtx->previewOffsetClipTime = -1;

    /**
     * Only one element in the chained list (no animated image with RGB buffer...) */
    framingCtx->pCurrent = framingCtx;
    framingCtx->pNext = framingCtx;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4VIFI_UInt8 M4VIFI_RGB565toYUV420 (void *pUserData,
 *                                   M4VIFI_ImagePlane *pPlaneIn,
 *                                   M4VIFI_ImagePlane *pPlaneOut)
 * @author  Patrice Martinez / Philips Digital Networks - MP4Net
 * @brief   transform RGB565 image to a YUV420 image.
 * @note    Convert RGB565 to YUV420,
 *          Loop on each row ( 2 rows by 2 rows )
 *              Loop on each column ( 2 col by 2 col )
 *                  Get 4 RGB samples from input data and build 4 output Y samples
 *                  and each single U & V data
 *              end loop on col
 *          end loop on row
 * @param   pUserData: (IN) User Specific Data
 * @param   pPlaneIn: (IN) Pointer to RGB565 Plane
 * @param   pPlaneOut: (OUT) Pointer to  YUV420 buffer Plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: YUV Plane height is ODD
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  YUV Plane width is ODD
 ******************************************************************************
*/
M4VIFI_UInt8    M4VIFI_xVSS_RGB565toYUV420(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
                                                      M4VIFI_ImagePlane *pPlaneOut)
{
    M4VIFI_UInt32   u32_width, u32_height;
    M4VIFI_UInt32   u32_stride_Y, u32_stride2_Y, u32_stride_U, u32_stride_V;
    M4VIFI_UInt32   u32_stride_rgb, u32_stride_2rgb;
    M4VIFI_UInt32   u32_col, u32_row;

    M4VIFI_Int32    i32_r00, i32_r01, i32_r10, i32_r11;
    M4VIFI_Int32    i32_g00, i32_g01, i32_g10, i32_g11;
    M4VIFI_Int32    i32_b00, i32_b01, i32_b10, i32_b11;
    M4VIFI_Int32    i32_y00, i32_y01, i32_y10, i32_y11;
    M4VIFI_Int32    i32_u00, i32_u01, i32_u10, i32_u11;
    M4VIFI_Int32    i32_v00, i32_v01, i32_v10, i32_v11;
    M4VIFI_UInt8    *pu8_yn, *pu8_ys, *pu8_u, *pu8_v;
    M4VIFI_UInt8    *pu8_y_data, *pu8_u_data, *pu8_v_data;
    M4VIFI_UInt8    *pu8_rgbn_data, *pu8_rgbn;
    M4VIFI_UInt16   u16_pix1, u16_pix2, u16_pix3, u16_pix4;
    M4VIFI_UInt8 count_null=0;

    /* Check planes height are appropriate */
    if( (pPlaneIn->u_height != pPlaneOut[0].u_height)           ||
        (pPlaneOut[0].u_height != (pPlaneOut[1].u_height<<1))   ||
        (pPlaneOut[0].u_height != (pPlaneOut[2].u_height<<1)))
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }

    /* Check planes width are appropriate */
    if( (pPlaneIn->u_width != pPlaneOut[0].u_width)         ||
        (pPlaneOut[0].u_width != (pPlaneOut[1].u_width<<1)) ||
        (pPlaneOut[0].u_width != (pPlaneOut[2].u_width<<1)))
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;
    }

    /* Set the pointer to the beginning of the output data buffers */
    pu8_y_data = pPlaneOut[0].pac_data + pPlaneOut[0].u_topleft;
    pu8_u_data = pPlaneOut[1].pac_data + pPlaneOut[1].u_topleft;
    pu8_v_data = pPlaneOut[2].pac_data + pPlaneOut[2].u_topleft;

    /* Set the pointer to the beginning of the input data buffers */
    pu8_rgbn_data   = pPlaneIn->pac_data + pPlaneIn->u_topleft;

    /* Get the size of the output image */
    u32_width = pPlaneOut[0].u_width;
    u32_height = pPlaneOut[0].u_height;

    /* Set the size of the memory jumps corresponding to row jump in each output plane */
    u32_stride_Y = pPlaneOut[0].u_stride;
    u32_stride2_Y = u32_stride_Y << 1;
    u32_stride_U = pPlaneOut[1].u_stride;
    u32_stride_V = pPlaneOut[2].u_stride;

    /* Set the size of the memory jumps corresponding to row jump in input plane */
    u32_stride_rgb = pPlaneIn->u_stride;
    u32_stride_2rgb = u32_stride_rgb << 1;


    /* Loop on each row of the output image, input coordinates are estimated from output ones */
    /* Two YUV rows are computed at each pass */
    for (u32_row = u32_height ;u32_row != 0; u32_row -=2)
    {
        /* Current Y plane row pointers */
        pu8_yn = pu8_y_data;
        /* Next Y plane row pointers */
        pu8_ys = pu8_yn + u32_stride_Y;
        /* Current U plane row pointer */
        pu8_u = pu8_u_data;
        /* Current V plane row pointer */
        pu8_v = pu8_v_data;

        pu8_rgbn = pu8_rgbn_data;

        /* Loop on each column of the output image */
        for (u32_col = u32_width; u32_col != 0 ; u32_col -=2)
        {
            /* Get four RGB 565 samples from input data */
            u16_pix1 = *( (M4VIFI_UInt16 *) pu8_rgbn);
            u16_pix2 = *( (M4VIFI_UInt16 *) (pu8_rgbn + CST_RGB_16_SIZE));
            u16_pix3 = *( (M4VIFI_UInt16 *) (pu8_rgbn + u32_stride_rgb));
            u16_pix4 = *( (M4VIFI_UInt16 *) (pu8_rgbn + u32_stride_rgb + CST_RGB_16_SIZE));

            /* Unpack RGB565 to 8bit R, G, B */
            /* (x,y) */
            GET_RGB565(i32_b00,i32_g00,i32_r00,u16_pix1);
            /* (x+1,y) */
            GET_RGB565(i32_b10,i32_g10,i32_r10,u16_pix2);
            /* (x,y+1) */
            GET_RGB565(i32_b01,i32_g01,i32_r01,u16_pix3);
            /* (x+1,y+1) */
            GET_RGB565(i32_b11,i32_g11,i32_r11,u16_pix4);
            /* If RGB is transparent color (0, 63, 0), we transform it to white (31,63,31) */
            if(i32_b00 == 0 && i32_g00 == 63 && i32_r00 == 0)
            {
                i32_b00 = 31;
                i32_r00 = 31;
            }
            if(i32_b10 == 0 && i32_g10 == 63 && i32_r10 == 0)
            {
                i32_b10 = 31;
                i32_r10 = 31;
            }
            if(i32_b01 == 0 && i32_g01 == 63 && i32_r01 == 0)
            {
                i32_b01 = 31;
                i32_r01 = 31;
            }
            if(i32_b11 == 0 && i32_g11 == 63 && i32_r11 == 0)
            {
                i32_b11 = 31;
                i32_r11 = 31;
            }
            /* Convert RGB value to YUV */
            i32_u00 = U16(i32_r00, i32_g00, i32_b00);
            i32_v00 = V16(i32_r00, i32_g00, i32_b00);
            /* luminance value */
            i32_y00 = Y16(i32_r00, i32_g00, i32_b00);

            i32_u10 = U16(i32_r10, i32_g10, i32_b10);
            i32_v10 = V16(i32_r10, i32_g10, i32_b10);
            /* luminance value */
            i32_y10 = Y16(i32_r10, i32_g10, i32_b10);

            i32_u01 = U16(i32_r01, i32_g01, i32_b01);
            i32_v01 = V16(i32_r01, i32_g01, i32_b01);
            /* luminance value */
            i32_y01 = Y16(i32_r01, i32_g01, i32_b01);

            i32_u11 = U16(i32_r11, i32_g11, i32_b11);
            i32_v11 = V16(i32_r11, i32_g11, i32_b11);
            /* luminance value */
            i32_y11 = Y16(i32_r11, i32_g11, i32_b11);

            /* Store luminance data */
            pu8_yn[0] = (M4VIFI_UInt8)i32_y00;
            pu8_yn[1] = (M4VIFI_UInt8)i32_y10;
            pu8_ys[0] = (M4VIFI_UInt8)i32_y01;
            pu8_ys[1] = (M4VIFI_UInt8)i32_y11;
            *pu8_u = (M4VIFI_UInt8)((i32_u00 + i32_u01 + i32_u10 + i32_u11 + 2) >> 2);
            *pu8_v = (M4VIFI_UInt8)((i32_v00 + i32_v01 + i32_v10 + i32_v11 + 2) >> 2);
            /* Prepare for next column */
            pu8_rgbn += (CST_RGB_16_SIZE<<1);
            /* Update current Y plane line pointer*/
            pu8_yn += 2;
            /* Update next Y plane line pointer*/
            pu8_ys += 2;
            /* Update U plane line pointer*/
            pu8_u ++;
            /* Update V plane line pointer*/
            pu8_v ++;
        } /* End of horizontal scanning */

        /* Prepare pointers for the next row */
        pu8_y_data += u32_stride2_Y;
        pu8_u_data += u32_stride_U;
        pu8_v_data += u32_stride_V;
        pu8_rgbn_data += u32_stride_2rgb;


    } /* End of vertical scanning */

    return M4VIFI_OK;
}

/***************************************************************************
Proto:
M4VIFI_UInt8    M4VIFI_RGB888toYUV420(void *pUserData, M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane PlaneOut[3]);
Author:     Patrice Martinez / Philips Digital Networks - MP4Net
Purpose:    filling of the YUV420 plane from a BGR24 plane
Abstract:   Loop on each row ( 2 rows by 2 rows )
                Loop on each column ( 2 col by 2 col )
                    Get 4 BGR samples from input data and build 4 output Y samples and each single U & V data
                end loop on col
            end loop on row

In:         RGB24 plane
InOut:      none
Out:        array of 3 M4VIFI_ImagePlane structures
Modified:   ML: RGB function modified to BGR.
***************************************************************************/
M4VIFI_UInt8 M4VIFI_RGB888toYUV420(void *pUserData, M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane PlaneOut[3])
{

    M4VIFI_UInt32   u32_width, u32_height;
    M4VIFI_UInt32   u32_stride_Y, u32_stride2_Y, u32_stride_U, u32_stride_V, u32_stride_rgb, u32_stride_2rgb;
    M4VIFI_UInt32   u32_col, u32_row;

    M4VIFI_Int32    i32_r00, i32_r01, i32_r10, i32_r11;
    M4VIFI_Int32    i32_g00, i32_g01, i32_g10, i32_g11;
    M4VIFI_Int32    i32_b00, i32_b01, i32_b10, i32_b11;
    M4VIFI_Int32    i32_y00, i32_y01, i32_y10, i32_y11;
    M4VIFI_Int32    i32_u00, i32_u01, i32_u10, i32_u11;
    M4VIFI_Int32    i32_v00, i32_v01, i32_v10, i32_v11;
    M4VIFI_UInt8    *pu8_yn, *pu8_ys, *pu8_u, *pu8_v;
    M4VIFI_UInt8    *pu8_y_data, *pu8_u_data, *pu8_v_data;
    M4VIFI_UInt8    *pu8_rgbn_data, *pu8_rgbn;

    /* check sizes */
    if( (PlaneIn->u_height != PlaneOut[0].u_height)         ||
        (PlaneOut[0].u_height != (PlaneOut[1].u_height<<1)) ||
        (PlaneOut[0].u_height != (PlaneOut[2].u_height<<1)))
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;

    if( (PlaneIn->u_width != PlaneOut[0].u_width)       ||
        (PlaneOut[0].u_width != (PlaneOut[1].u_width<<1))   ||
        (PlaneOut[0].u_width != (PlaneOut[2].u_width<<1)))
        return M4VIFI_ILLEGAL_FRAME_WIDTH;


    /* set the pointer to the beginning of the output data buffers */
    pu8_y_data  = PlaneOut[0].pac_data + PlaneOut[0].u_topleft;
    pu8_u_data  = PlaneOut[1].pac_data + PlaneOut[1].u_topleft;
    pu8_v_data  = PlaneOut[2].pac_data + PlaneOut[2].u_topleft;

    /* idem for input buffer */
    pu8_rgbn_data   = PlaneIn->pac_data + PlaneIn->u_topleft;

    /* get the size of the output image */
    u32_width   = PlaneOut[0].u_width;
    u32_height  = PlaneOut[0].u_height;

    /* set the size of the memory jumps corresponding to row jump in each output plane */
    u32_stride_Y = PlaneOut[0].u_stride;
    u32_stride2_Y= u32_stride_Y << 1;
    u32_stride_U = PlaneOut[1].u_stride;
    u32_stride_V = PlaneOut[2].u_stride;

    /* idem for input plane */
    u32_stride_rgb = PlaneIn->u_stride;
    u32_stride_2rgb = u32_stride_rgb << 1;

    /* loop on each row of the output image, input coordinates are estimated from output ones */
    /* two YUV rows are computed at each pass */
    for (u32_row = u32_height ;u32_row != 0; u32_row -=2)
    {
        /* update working pointers */
        pu8_yn  = pu8_y_data;
        pu8_ys  = pu8_yn + u32_stride_Y;

        pu8_u   = pu8_u_data;
        pu8_v   = pu8_v_data;

        pu8_rgbn= pu8_rgbn_data;

        /* loop on each column of the output image*/
        for (u32_col = u32_width; u32_col != 0 ; u32_col -=2)
        {
            /* get RGB samples of 4 pixels */
            GET_RGB24(i32_r00, i32_g00, i32_b00, pu8_rgbn, 0);
            GET_RGB24(i32_r10, i32_g10, i32_b10, pu8_rgbn, CST_RGB_24_SIZE);
            GET_RGB24(i32_r01, i32_g01, i32_b01, pu8_rgbn, u32_stride_rgb);
            GET_RGB24(i32_r11, i32_g11, i32_b11, pu8_rgbn, u32_stride_rgb + CST_RGB_24_SIZE);

            i32_u00 = U24(i32_r00, i32_g00, i32_b00);
            i32_v00 = V24(i32_r00, i32_g00, i32_b00);
            i32_y00 = Y24(i32_r00, i32_g00, i32_b00);       /* matrix luminance */
            pu8_yn[0]= (M4VIFI_UInt8)i32_y00;

            i32_u10 = U24(i32_r10, i32_g10, i32_b10);
            i32_v10 = V24(i32_r10, i32_g10, i32_b10);
            i32_y10 = Y24(i32_r10, i32_g10, i32_b10);
            pu8_yn[1]= (M4VIFI_UInt8)i32_y10;

            i32_u01 = U24(i32_r01, i32_g01, i32_b01);
            i32_v01 = V24(i32_r01, i32_g01, i32_b01);
            i32_y01 = Y24(i32_r01, i32_g01, i32_b01);
            pu8_ys[0]= (M4VIFI_UInt8)i32_y01;

            i32_u11 = U24(i32_r11, i32_g11, i32_b11);
            i32_v11 = V24(i32_r11, i32_g11, i32_b11);
            i32_y11 = Y24(i32_r11, i32_g11, i32_b11);
            pu8_ys[1] = (M4VIFI_UInt8)i32_y11;

            *pu8_u  = (M4VIFI_UInt8)((i32_u00 + i32_u01 + i32_u10 + i32_u11 + 2) >> 2);
            *pu8_v  = (M4VIFI_UInt8)((i32_v00 + i32_v01 + i32_v10 + i32_v11 + 2) >> 2);

            pu8_rgbn    +=  (CST_RGB_24_SIZE<<1);
            pu8_yn      += 2;
            pu8_ys      += 2;

            pu8_u ++;
            pu8_v ++;
        } /* end of horizontal scanning */

        pu8_y_data      += u32_stride2_Y;
        pu8_u_data      += u32_stride_U;
        pu8_v_data      += u32_stride_V;
        pu8_rgbn_data   += u32_stride_2rgb;


    } /* End of vertical scanning */

    return M4VIFI_OK;
}

/** YUV420 to YUV420 */
/**
 *******************************************************************************************
 * M4VIFI_UInt8 M4VIFI_YUV420toYUV420 (void *pUserData,
 *                                     M4VIFI_ImagePlane *pPlaneIn,
 *                                     M4VIFI_ImagePlane *pPlaneOut)
 * @brief   Transform YUV420 image to a YUV420 image.
 * @param   pUserData: (IN) User Specific Data (Unused - could be NULL)
 * @param   pPlaneIn: (IN) Pointer to YUV plane buffer
 * @param   pPlaneOut: (OUT) Pointer to YUV Plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: Error in plane height
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  Error in plane width
 *******************************************************************************************
 */

M4VIFI_UInt8 M4VIFI_YUV420toYUV420(void *user_data, M4VIFI_ImagePlane PlaneIn[3], M4VIFI_ImagePlane *PlaneOut )
{
    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest;

    for (plane_number = 0; plane_number < 3; plane_number++)
    {
        p_buf_src = &(PlaneIn[plane_number].pac_data[PlaneIn[plane_number].u_topleft]);
        p_buf_dest = &(PlaneOut[plane_number].pac_data[PlaneOut[plane_number].u_topleft]);
        for (i = 0; i < PlaneOut[plane_number].u_height; i++)
        {
            memcpy((void *)p_buf_dest, (void *)p_buf_src ,PlaneOut[plane_number].u_width);
            p_buf_src += PlaneIn[plane_number].u_stride;
            p_buf_dest += PlaneOut[plane_number].u_stride;
        }
    }
    return M4VIFI_OK;
}

/**
 ***********************************************************************************************
 * M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
 *                                                                  M4VIFI_ImagePlane *pPlaneOut)
 * @author  David Dana (PHILIPS Software)
 * @brief   Resizes YUV420 Planar plane.
 * @note    Basic structure of the function
 *          Loop on each row (step 2)
 *              Loop on each column (step 2)
 *                  Get four Y samples and 1 U & V sample
 *                  Resize the Y with corresponing U and V samples
 *                  Place the YUV in the ouput plane
 *              end loop column
 *          end loop row
 *          For resizing bilinear interpolation linearly interpolates along
 *          each row, and then uses that result in a linear interpolation down each column.
 *          Each estimated pixel in the output image is a weighted
 *          combination of its four neighbours. The ratio of compression
 *          or dilatation is estimated using input and output sizes.
 * @param   pUserData: (IN) User Data
 * @param   pPlaneIn: (IN) Pointer to YUV420 (Planar) plane buffer
 * @param   pPlaneOut: (OUT) Pointer to YUV420 (Planar) plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: Error in height
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  Error in width
 ***********************************************************************************************
*/
M4VIFI_UInt8    M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
                                                                M4VIFI_ImagePlane *pPlaneIn,
                                                                M4VIFI_ImagePlane *pPlaneOut)
{
    M4VIFI_UInt8    *pu8_data_in, *pu8_data_out, *pu8dum;
    M4VIFI_UInt32   u32_plane;
    M4VIFI_UInt32   u32_width_in, u32_width_out, u32_height_in, u32_height_out;
    M4VIFI_UInt32   u32_stride_in, u32_stride_out;
    M4VIFI_UInt32   u32_x_inc, u32_y_inc;
    M4VIFI_UInt32   u32_x_accum, u32_y_accum, u32_x_accum_start;
    M4VIFI_UInt32   u32_width, u32_height;
    M4VIFI_UInt32   u32_y_frac;
    M4VIFI_UInt32   u32_x_frac;
    M4VIFI_UInt32   u32_temp_value;
    M4VIFI_UInt8    *pu8_src_top;
    M4VIFI_UInt8    *pu8_src_bottom;

    M4VIFI_UInt8    u8Wflag = 0;
    M4VIFI_UInt8    u8Hflag = 0;
    M4VIFI_UInt32   loop = 0;


    /*
     If input width is equal to output width and input height equal to
     output height then M4VIFI_YUV420toYUV420 is called.
    */
    if ((pPlaneIn[0].u_height == pPlaneOut[0].u_height) &&
              (pPlaneIn[0].u_width == pPlaneOut[0].u_width))
    {
        return M4VIFI_YUV420toYUV420(pUserData, pPlaneIn, pPlaneOut);
    }

    /* Check for the YUV width and height are even */
    if ((IS_EVEN(pPlaneIn[0].u_height) == FALSE)    ||
        (IS_EVEN(pPlaneOut[0].u_height) == FALSE))
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }

    if ((IS_EVEN(pPlaneIn[0].u_width) == FALSE) ||
        (IS_EVEN(pPlaneOut[0].u_width) == FALSE))
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;
    }

    /* Loop on planes */
    for(u32_plane = 0;u32_plane < PLANES;u32_plane++)
    {
        /* Set the working pointers at the beginning of the input/output data field */
        pu8_data_in     = pPlaneIn[u32_plane].pac_data + pPlaneIn[u32_plane].u_topleft;
        pu8_data_out    = pPlaneOut[u32_plane].pac_data + pPlaneOut[u32_plane].u_topleft;

        /* Get the memory jump corresponding to a row jump */
        u32_stride_in   = pPlaneIn[u32_plane].u_stride;
        u32_stride_out  = pPlaneOut[u32_plane].u_stride;

        /* Set the bounds of the active image */
        u32_width_in    = pPlaneIn[u32_plane].u_width;
        u32_height_in   = pPlaneIn[u32_plane].u_height;

        u32_width_out   = pPlaneOut[u32_plane].u_width;
        u32_height_out  = pPlaneOut[u32_plane].u_height;

        /*
        For the case , width_out = width_in , set the flag to avoid
        accessing one column beyond the input width.In this case the last
        column is replicated for processing
        */
        if (u32_width_out == u32_width_in) {
            u32_width_out = u32_width_out-1;
            u8Wflag = 1;
        }

        /* Compute horizontal ratio between src and destination width.*/
        if (u32_width_out >= u32_width_in)
        {
            u32_x_inc   = ((u32_width_in-1) * MAX_SHORT) / (u32_width_out-1);
        }
        else
        {
            u32_x_inc   = (u32_width_in * MAX_SHORT) / (u32_width_out);
        }

        /*
        For the case , height_out = height_in , set the flag to avoid
        accessing one row beyond the input height.In this case the last
        row is replicated for processing
        */
        if (u32_height_out == u32_height_in) {
            u32_height_out = u32_height_out-1;
            u8Hflag = 1;
        }

        /* Compute vertical ratio between src and destination height.*/
        if (u32_height_out >= u32_height_in)
        {
            u32_y_inc   = ((u32_height_in - 1) * MAX_SHORT) / (u32_height_out-1);
        }
        else
        {
            u32_y_inc = (u32_height_in * MAX_SHORT) / (u32_height_out);
        }

        /*
        Calculate initial accumulator value : u32_y_accum_start.
        u32_y_accum_start is coded on 15 bits, and represents a value
        between 0 and 0.5
        */
        if (u32_y_inc >= MAX_SHORT)
        {
        /*
        Keep the fractionnal part, assimung that integer  part is coded
        on the 16 high bits and the fractional on the 15 low bits
        */
            u32_y_accum = u32_y_inc & 0xffff;

            if (!u32_y_accum)
            {
                u32_y_accum = MAX_SHORT;
            }

            u32_y_accum >>= 1;
        }
        else
        {
            u32_y_accum = 0;
        }


        /*
        Calculate initial accumulator value : u32_x_accum_start.
        u32_x_accum_start is coded on 15 bits, and represents a value
        between 0 and 0.5
        */
        if (u32_x_inc >= MAX_SHORT)
        {
            u32_x_accum_start = u32_x_inc & 0xffff;

            if (!u32_x_accum_start)
            {
                u32_x_accum_start = MAX_SHORT;
            }

            u32_x_accum_start >>= 1;
        }
        else
        {
            u32_x_accum_start = 0;
        }

        u32_height = u32_height_out;

        /*
        Bilinear interpolation linearly interpolates along each row, and
        then uses that result in a linear interpolation donw each column.
        Each estimated pixel in the output image is a weighted combination
        of its four neighbours according to the formula:
        F(p',q')=f(p,q)R(-a)R(b)+f(p,q-1)R(-a)R(b-1)+f(p+1,q)R(1-a)R(b)+
        f(p+&,q+1)R(1-a)R(b-1) with  R(x) = / x+1  -1 =< x =< 0 \ 1-x
        0 =< x =< 1 and a (resp. b)weighting coefficient is the distance
        from the nearest neighbor in the p (resp. q) direction
        */

        do { /* Scan all the row */

            /* Vertical weight factor */
            u32_y_frac = (u32_y_accum>>12)&15;

            /* Reinit accumulator */
            u32_x_accum = u32_x_accum_start;

            u32_width = u32_width_out;

            do { /* Scan along each row */
                pu8_src_top = pu8_data_in + (u32_x_accum >> 16);
                pu8_src_bottom = pu8_src_top + u32_stride_in;
                u32_x_frac = (u32_x_accum >> 12)&15; /* Horizontal weight factor */

                /* Weighted combination */
                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                /* Update horizontal accumulator */
                u32_x_accum += u32_x_inc;
            } while(--u32_width);

            /*
               This u8Wflag flag gets in to effect if input and output
               width is same, and height may be different. So previous
               pixel is replicated here
            */
            if (u8Wflag) {
                *pu8_data_out = (M4VIFI_UInt8)u32_temp_value;
            }

            pu8dum = (pu8_data_out-u32_width_out);
            pu8_data_out = pu8_data_out + u32_stride_out - u32_width_out;

            /* Update vertical accumulator */
            u32_y_accum += u32_y_inc;
            if (u32_y_accum>>16) {
                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * u32_stride_in;
                u32_y_accum &= 0xffff;
            }
        } while(--u32_height);

        /*
        This u8Hflag flag gets in to effect if input and output height
        is same, and width may be different. So previous pixel row is
        replicated here
        */
        if (u8Hflag) {
            for(loop =0; loop < (u32_width_out+u8Wflag); loop++) {
                *pu8_data_out++ = (M4VIFI_UInt8)*pu8dum++;
            }
        }
    }

    return M4VIFI_OK;
}

M4OSA_ERR applyRenderingMode(M4VIFI_ImagePlane* pPlaneIn, M4VIFI_ImagePlane* pPlaneOut, M4xVSS_MediaRendering mediaRendering)
{
    M4OSA_ERR err = M4NO_ERROR;

    if(mediaRendering == M4xVSS_kResizing)
    {
        /**
         * Call the resize filter. From the intermediate frame to the encoder image plane */
        err = M4VIFI_ResizeBilinearYUV420toYUV420(M4OSA_NULL, pPlaneIn, pPlaneOut);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("applyRenderingMode: M4ViFilResizeBilinearYUV420toYUV420 returns 0x%x!", err);
            return err;
        }
    }
    else
    {
        M4AIR_Params Params;
        M4OSA_Context m_air_context;
        M4VIFI_ImagePlane pImagePlanesTemp[3];
        M4VIFI_ImagePlane* pPlaneTemp;
        M4OSA_UInt8* pOutPlaneY = pPlaneOut[0].pac_data + pPlaneOut[0].u_topleft;
        M4OSA_UInt8* pOutPlaneU = pPlaneOut[1].pac_data + pPlaneOut[1].u_topleft;
        M4OSA_UInt8* pOutPlaneV = pPlaneOut[2].pac_data + pPlaneOut[2].u_topleft;
        M4OSA_UInt8* pInPlaneY = NULL;
        M4OSA_UInt8* pInPlaneU = NULL;
        M4OSA_UInt8* pInPlaneV = NULL;
        M4OSA_UInt32 i;

        /*to keep media aspect ratio*/
        /*Initialize AIR Params*/
        Params.m_inputCoord.m_x = 0;
        Params.m_inputCoord.m_y = 0;
        Params.m_inputSize.m_height = pPlaneIn->u_height;
        Params.m_inputSize.m_width = pPlaneIn->u_width;
        Params.m_outputSize.m_width = pPlaneOut->u_width;
        Params.m_outputSize.m_height = pPlaneOut->u_height;
        Params.m_bOutputStripe = M4OSA_FALSE;
        Params.m_outputOrientation = M4COMMON_kOrientationTopLeft;

        /**
        Media rendering: Black borders*/
        if(mediaRendering == M4xVSS_kBlackBorders)
        {
            memset((void *)pPlaneOut[0].pac_data,Y_PLANE_BORDER_VALUE,(pPlaneOut[0].u_height*pPlaneOut[0].u_stride));
            memset((void *)pPlaneOut[1].pac_data,U_PLANE_BORDER_VALUE,(pPlaneOut[1].u_height*pPlaneOut[1].u_stride));
            memset((void *)pPlaneOut[2].pac_data,V_PLANE_BORDER_VALUE,(pPlaneOut[2].u_height*pPlaneOut[2].u_stride));

            pImagePlanesTemp[0].u_width = pPlaneOut[0].u_width;
            pImagePlanesTemp[0].u_height = pPlaneOut[0].u_height;
            pImagePlanesTemp[0].u_stride = pPlaneOut[0].u_width;
            pImagePlanesTemp[0].u_topleft = 0;
            pImagePlanesTemp[0].pac_data = M4OSA_NULL;

            pImagePlanesTemp[1].u_width = pPlaneOut[1].u_width;
            pImagePlanesTemp[1].u_height = pPlaneOut[1].u_height;
            pImagePlanesTemp[1].u_stride = pPlaneOut[1].u_width;
            pImagePlanesTemp[1].u_topleft = 0;
            pImagePlanesTemp[1].pac_data = M4OSA_NULL;

            pImagePlanesTemp[2].u_width = pPlaneOut[2].u_width;
            pImagePlanesTemp[2].u_height = pPlaneOut[2].u_height;
            pImagePlanesTemp[2].u_stride = pPlaneOut[2].u_width;
            pImagePlanesTemp[2].u_topleft = 0;
            pImagePlanesTemp[2].pac_data = M4OSA_NULL;

            /* Allocates plan in local image plane structure */
            pImagePlanesTemp[0].pac_data = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(pImagePlanesTemp[0].u_width * pImagePlanesTemp[0].u_height, M4VS, (M4OSA_Char*)"applyRenderingMode: temporary plane bufferY") ;
            if(pImagePlanesTemp[0].pac_data == M4OSA_NULL)
            {
                M4OSA_TRACE1_0("Error alloc in applyRenderingMode");
                return M4ERR_ALLOC;
            }
            pImagePlanesTemp[1].pac_data = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(pImagePlanesTemp[1].u_width * pImagePlanesTemp[1].u_height, M4VS, (M4OSA_Char*)"applyRenderingMode: temporary plane bufferU") ;
            if(pImagePlanesTemp[1].pac_data == M4OSA_NULL)
            {

                M4OSA_TRACE1_0("Error alloc in applyRenderingMode");
                return M4ERR_ALLOC;
            }
            pImagePlanesTemp[2].pac_data = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(pImagePlanesTemp[2].u_width * pImagePlanesTemp[2].u_height, M4VS, (M4OSA_Char*)"applyRenderingMode: temporary plane bufferV") ;
            if(pImagePlanesTemp[2].pac_data == M4OSA_NULL)
            {

                M4OSA_TRACE1_0("Error alloc in applyRenderingMode");
                return M4ERR_ALLOC;
            }

            pInPlaneY = pImagePlanesTemp[0].pac_data ;
            pInPlaneU = pImagePlanesTemp[1].pac_data ;
            pInPlaneV = pImagePlanesTemp[2].pac_data ;

            memset((void *)pImagePlanesTemp[0].pac_data,Y_PLANE_BORDER_VALUE,(pImagePlanesTemp[0].u_height*pImagePlanesTemp[0].u_stride));
            memset((void *)pImagePlanesTemp[1].pac_data,U_PLANE_BORDER_VALUE,(pImagePlanesTemp[1].u_height*pImagePlanesTemp[1].u_stride));
            memset((void *)pImagePlanesTemp[2].pac_data,V_PLANE_BORDER_VALUE,(pImagePlanesTemp[2].u_height*pImagePlanesTemp[2].u_stride));

            if((M4OSA_UInt32)((pPlaneIn->u_height * pPlaneOut->u_width) /pPlaneIn->u_width) <= pPlaneOut->u_height)//Params.m_inputSize.m_height < Params.m_inputSize.m_width)
            {
                /*it is height so black borders will be on the top and on the bottom side*/
                Params.m_outputSize.m_width = pPlaneOut->u_width;
                Params.m_outputSize.m_height = (M4OSA_UInt32)((pPlaneIn->u_height * pPlaneOut->u_width) /pPlaneIn->u_width);
                /*number of lines at the top*/
                pImagePlanesTemp[0].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[0].u_height-Params.m_outputSize.m_height)>>1))*pImagePlanesTemp[0].u_stride;
                pImagePlanesTemp[0].u_height = Params.m_outputSize.m_height;
                pImagePlanesTemp[1].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[1].u_height-(Params.m_outputSize.m_height>>1)))>>1)*pImagePlanesTemp[1].u_stride;
                pImagePlanesTemp[1].u_height = Params.m_outputSize.m_height>>1;
                pImagePlanesTemp[2].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[2].u_height-(Params.m_outputSize.m_height>>1)))>>1)*pImagePlanesTemp[2].u_stride;
                pImagePlanesTemp[2].u_height = Params.m_outputSize.m_height>>1;
            }
            else
            {
                /*it is width so black borders will be on the left and right side*/
                Params.m_outputSize.m_height = pPlaneOut->u_height;
                Params.m_outputSize.m_width = (M4OSA_UInt32)((pPlaneIn->u_width * pPlaneOut->u_height) /pPlaneIn->u_height);

                pImagePlanesTemp[0].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[0].u_width-Params.m_outputSize.m_width)>>1));
                pImagePlanesTemp[0].u_width = Params.m_outputSize.m_width;
                pImagePlanesTemp[1].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[1].u_width-(Params.m_outputSize.m_width>>1)))>>1);
                pImagePlanesTemp[1].u_width = Params.m_outputSize.m_width>>1;
                pImagePlanesTemp[2].u_topleft = (M4xVSS_ABS((M4OSA_Int32)(pImagePlanesTemp[2].u_width-(Params.m_outputSize.m_width>>1)))>>1);
                pImagePlanesTemp[2].u_width = Params.m_outputSize.m_width>>1;
            }

            /*Width and height have to be even*/
            Params.m_outputSize.m_width = (Params.m_outputSize.m_width>>1)<<1;
            Params.m_outputSize.m_height = (Params.m_outputSize.m_height>>1)<<1;
            Params.m_inputSize.m_width = (Params.m_inputSize.m_width>>1)<<1;
            Params.m_inputSize.m_height = (Params.m_inputSize.m_height>>1)<<1;
            pImagePlanesTemp[0].u_width = (pImagePlanesTemp[0].u_width>>1)<<1;
            pImagePlanesTemp[1].u_width = (pImagePlanesTemp[1].u_width>>1)<<1;
            pImagePlanesTemp[2].u_width = (pImagePlanesTemp[2].u_width>>1)<<1;
            pImagePlanesTemp[0].u_height = (pImagePlanesTemp[0].u_height>>1)<<1;
            pImagePlanesTemp[1].u_height = (pImagePlanesTemp[1].u_height>>1)<<1;
            pImagePlanesTemp[2].u_height = (pImagePlanesTemp[2].u_height>>1)<<1;

            /*Check that values are coherent*/
            if(Params.m_inputSize.m_height == Params.m_outputSize.m_height)
            {
                Params.m_inputSize.m_width = Params.m_outputSize.m_width;
            }
            else if(Params.m_inputSize.m_width == Params.m_outputSize.m_width)
            {
                Params.m_inputSize.m_height = Params.m_outputSize.m_height;
            }
            pPlaneTemp = pImagePlanesTemp;


        }

        /**
        Media rendering: Cropping*/
        if(mediaRendering == M4xVSS_kCropping)
        {
            Params.m_outputSize.m_height = pPlaneOut->u_height;
            Params.m_outputSize.m_width = pPlaneOut->u_width;
            if((Params.m_outputSize.m_height * Params.m_inputSize.m_width) /Params.m_outputSize.m_width<Params.m_inputSize.m_height)
            {
                /*height will be cropped*/
                Params.m_inputSize.m_height = (M4OSA_UInt32)((Params.m_outputSize.m_height * Params.m_inputSize.m_width) /Params.m_outputSize.m_width);
                Params.m_inputSize.m_height = (Params.m_inputSize.m_height>>1)<<1;
                Params.m_inputCoord.m_y = (M4OSA_Int32)((M4OSA_Int32)((pPlaneIn->u_height - Params.m_inputSize.m_height))>>1);
            }
            else
            {
                /*width will be cropped*/
                Params.m_inputSize.m_width = (M4OSA_UInt32)((Params.m_outputSize.m_width * Params.m_inputSize.m_height) /Params.m_outputSize.m_height);
                Params.m_inputSize.m_width = (Params.m_inputSize.m_width>>1)<<1;
                Params.m_inputCoord.m_x = (M4OSA_Int32)((M4OSA_Int32)((pPlaneIn->u_width - Params.m_inputSize.m_width))>>1);
            }
            pPlaneTemp = pPlaneOut;
        }

        /**
         * Call AIR functions */
        err = M4AIR_create(&m_air_context, M4AIR_kYUV420P);
        if(err != M4NO_ERROR)
        {

            M4OSA_TRACE1_1("applyRenderingMode: Error when initializing AIR: 0x%x", err);
            for(i=0; i<3; i++)
            {
                if(pImagePlanesTemp[i].pac_data != M4OSA_NULL)
                {
                    free(pImagePlanesTemp[i].pac_data);
                    pImagePlanesTemp[i].pac_data = M4OSA_NULL;
                }
            }
            return err;
        }


        err = M4AIR_configure(m_air_context, &Params);
        if(err != M4NO_ERROR)
        {

            M4OSA_TRACE1_1("applyRenderingMode: Error when configuring AIR: 0x%x", err);
            M4AIR_cleanUp(m_air_context);
            for(i=0; i<3; i++)
            {
                if(pImagePlanesTemp[i].pac_data != M4OSA_NULL)
                {
                    free(pImagePlanesTemp[i].pac_data);
                    pImagePlanesTemp[i].pac_data = M4OSA_NULL;
                }
            }
            return err;
        }

        err = M4AIR_get(m_air_context, pPlaneIn, pPlaneTemp);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("applyRenderingMode: Error when getting AIR plane: 0x%x", err);
            M4AIR_cleanUp(m_air_context);
            for(i=0; i<3; i++)
            {
                if(pImagePlanesTemp[i].pac_data != M4OSA_NULL)
                {
                    free(pImagePlanesTemp[i].pac_data);
                    pImagePlanesTemp[i].pac_data = M4OSA_NULL;
                }
            }
            return err;
        }

        if(mediaRendering == M4xVSS_kBlackBorders)
        {
            for(i=0; i<pPlaneOut[0].u_height; i++)
            {
                memcpy((void *)pOutPlaneY, (void *)pInPlaneY, pPlaneOut[0].u_width);
                pInPlaneY += pPlaneOut[0].u_width;
                pOutPlaneY += pPlaneOut[0].u_stride;
            }
            for(i=0; i<pPlaneOut[1].u_height; i++)
            {
                memcpy((void *)pOutPlaneU, (void *)pInPlaneU, pPlaneOut[1].u_width);
                pInPlaneU += pPlaneOut[1].u_width;
                pOutPlaneU += pPlaneOut[1].u_stride;
            }
            for(i=0; i<pPlaneOut[2].u_height; i++)
            {
                memcpy((void *)pOutPlaneV, (void *)pInPlaneV, pPlaneOut[2].u_width);
                pInPlaneV += pPlaneOut[2].u_width;
                pOutPlaneV += pPlaneOut[2].u_stride;
            }

            for(i=0; i<3; i++)
            {
                if(pImagePlanesTemp[i].pac_data != M4OSA_NULL)
                {
                    free(pImagePlanesTemp[i].pac_data);
                    pImagePlanesTemp[i].pac_data = M4OSA_NULL;
                }
            }
        }

        if (m_air_context != M4OSA_NULL) {
            M4AIR_cleanUp(m_air_context);
            m_air_context = M4OSA_NULL;
        }
    }

    return err;
}

//TODO: remove this code after link with videoartist lib
/* M4AIR code*/
#define M4AIR_YUV420_FORMAT_SUPPORTED
#define M4AIR_YUV420A_FORMAT_SUPPORTED

/************************* COMPILATION CHECKS ***************************/
#ifndef M4AIR_YUV420_FORMAT_SUPPORTED
#ifndef M4AIR_BGR565_FORMAT_SUPPORTED
#ifndef M4AIR_RGB565_FORMAT_SUPPORTED
#ifndef M4AIR_BGR888_FORMAT_SUPPORTED
#ifndef M4AIR_RGB888_FORMAT_SUPPORTED
#ifndef M4AIR_JPG_FORMAT_SUPPORTED

#error "Please define at least one input format for the AIR component"

#endif
#endif
#endif
#endif
#endif
#endif

/************************ M4AIR INTERNAL TYPES DEFINITIONS ***********************/

/**
 ******************************************************************************
 * enum         M4AIR_States
 * @brief       The following enumeration defines the internal states of the AIR.
 ******************************************************************************
*/
typedef enum
{
    M4AIR_kCreated,         /**< State after M4AIR_create has been called */
    M4AIR_kConfigured           /**< State after M4AIR_configure has been called */
}M4AIR_States;


/**
 ******************************************************************************
 * struct       M4AIR_InternalContext
 * @brief       The following structure is the internal context of the AIR.
 ******************************************************************************
*/
typedef struct
{
    M4AIR_States                m_state;            /**< Internal state */
    M4AIR_InputFormatType   m_inputFormat;      /**< Input format like YUV420Planar, RGB565, JPG, etc ... */
    M4AIR_Params            m_params;           /**< Current input Parameter of  the processing */
    M4OSA_UInt32            u32_x_inc[4];       /**< ratio between input and ouput width for YUV */
    M4OSA_UInt32            u32_y_inc[4];       /**< ratio between input and ouput height for YUV */
    M4OSA_UInt32            u32_x_accum_start[4];   /**< horizontal initial accumulator value */
    M4OSA_UInt32            u32_y_accum_start[4];   /**< Vertical initial accumulator value */
    M4OSA_UInt32            u32_x_accum[4];     /**< save of horizontal accumulator value */
    M4OSA_UInt32            u32_y_accum[4];     /**< save of vertical accumulator value */
    M4OSA_UInt8*            pu8_data_in[4];         /**< Save of input plane pointers in case of stripe mode */
    M4OSA_UInt32            m_procRows;         /**< Number of processed rows, used in stripe mode only */
    M4OSA_Bool              m_bOnlyCopy;            /**< Flag to know if we just perform a copy or a bilinear interpolation */
    M4OSA_Bool              m_bFlipX;               /**< Depend on output orientation, used during processing to revert processing order in X coordinates */
    M4OSA_Bool              m_bFlipY;               /**< Depend on output orientation, used during processing to revert processing order in Y coordinates */
    M4OSA_Bool              m_bRevertXY;            /**< Depend on output orientation, used during processing to revert X and Y processing order (+-90° rotation) */
}M4AIR_InternalContext;

/********************************* MACROS *******************************/
#define M4ERR_CHECK_NULL_RETURN_VALUE(retval, pointer) if ((pointer) == M4OSA_NULL) return ((M4OSA_ERR)(retval));


/********************** M4AIR PUBLIC API IMPLEMENTATION ********************/
/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_create(M4OSA_Context* pContext,M4AIR_InputFormatType inputFormat)
 * @author  Arnaud Collard
 * @brief       This function initialize an instance of the AIR.
 * @param   pContext:   (IN/OUT) Address of the context to create
 * @param   inputFormat:    (IN) input format type.
 * @return  M4NO_ERROR: there is no error
 * @return  M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only). Invalid formatType
 * @return  M4ERR_ALLOC: No more memory is available
 ******************************************************************************
*/
M4OSA_ERR M4AIR_create(M4OSA_Context* pContext,M4AIR_InputFormatType inputFormat)
{
    M4OSA_ERR err = M4NO_ERROR ;
    M4AIR_InternalContext* pC = M4OSA_NULL ;
    /* Check that the address on the context is not NULL */
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    *pContext = M4OSA_NULL ;

    /* Internal Context creation */
    pC = (M4AIR_InternalContext*)M4OSA_32bitAlignedMalloc(sizeof(M4AIR_InternalContext), M4AIR, (M4OSA_Char*)"AIR internal context") ;
    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_ALLOC, pC) ;


    /* Check if the input format is supported */
    switch(inputFormat)
    {
#ifdef M4AIR_YUV420_FORMAT_SUPPORTED
        case M4AIR_kYUV420P:
        break ;
#endif
#ifdef M4AIR_YUV420A_FORMAT_SUPPORTED
        case M4AIR_kYUV420AP:
        break ;
#endif
        default:
            err = M4ERR_AIR_FORMAT_NOT_SUPPORTED;
            goto M4AIR_create_cleanup ;
    }

    /**< Save input format and update state */
    pC->m_inputFormat = inputFormat;
    pC->m_state = M4AIR_kCreated;

    /* Return the context to the caller */
    *pContext = pC ;

    return M4NO_ERROR ;

M4AIR_create_cleanup:
    /* Error management : we destroy the context if needed */
    if(M4OSA_NULL != pC)
    {
        free(pC) ;
    }

    *pContext = M4OSA_NULL ;

    return err ;
}



/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_cleanUp(M4OSA_Context pContext)
 * @author  Arnaud Collard
 * @brief       This function destroys an instance of the AIR component
 * @param   pContext:   (IN) Context identifying the instance to destroy
 * @return  M4NO_ERROR: there is no error
 * @return  M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 * @return  M4ERR_STATE: Internal state is incompatible with this function call.
******************************************************************************
*/
M4OSA_ERR M4AIR_cleanUp(M4OSA_Context pContext)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    /**< Check state */
    if((M4AIR_kCreated != pC->m_state)&&(M4AIR_kConfigured != pC->m_state))
    {
        return M4ERR_STATE;
    }
    free(pC) ;

    return M4NO_ERROR ;

}


/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_configure(M4OSA_Context pContext, M4AIR_Params* pParams)
 * @brief       This function will configure the AIR.
 * @note    It will set the input and output coordinates and sizes,
 *          and indicates if we will proceed in stripe or not.
 *          In case a M4AIR_get in stripe mode was on going, it will cancel this previous processing
 *          and reset the get process.
 * @param   pContext:               (IN) Context identifying the instance
 * @param   pParams->m_bOutputStripe:(IN) Stripe mode.
 * @param   pParams->m_inputCoord:  (IN) X,Y coordinates of the first valid pixel in input.
 * @param   pParams->m_inputSize:   (IN) input ROI size.
 * @param   pParams->m_outputSize:  (IN) output size.
 * @return  M4NO_ERROR: there is no error
 * @return  M4ERR_ALLOC: No more memory space to add a new effect.
 * @return  M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 * @return  M4ERR_AIR_FORMAT_NOT_SUPPORTED: the requested input format is not supported.
 ******************************************************************************
*/
M4OSA_ERR M4AIR_configure(M4OSA_Context pContext, M4AIR_Params* pParams)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;
    M4OSA_UInt32    i,u32_width_in, u32_width_out, u32_height_in, u32_height_out;
    M4OSA_UInt32    nb_planes;

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    if(M4AIR_kYUV420AP == pC->m_inputFormat)
    {
        nb_planes = 4;
    }
    else
    {
        nb_planes = 3;
    }

    /**< Check state */
    if((M4AIR_kCreated != pC->m_state)&&(M4AIR_kConfigured != pC->m_state))
    {
        return M4ERR_STATE;
    }

    /** Save parameters */
    pC->m_params = *pParams;

    /* Check for the input&output width and height are even */
        if( ((pC->m_params.m_inputSize.m_height)&0x1)    ||
         ((pC->m_params.m_inputSize.m_height)&0x1))
        {
         return M4ERR_AIR_ILLEGAL_FRAME_SIZE;
        }

    if( ((pC->m_params.m_inputSize.m_width)&0x1)    ||
         ((pC->m_params.m_inputSize.m_width)&0x1))
        {
            return M4ERR_AIR_ILLEGAL_FRAME_SIZE;
        }
    if(((pC->m_params.m_inputSize.m_width) == (pC->m_params.m_outputSize.m_width))
        &&((pC->m_params.m_inputSize.m_height) == (pC->m_params.m_outputSize.m_height)))
    {
        /**< No resize in this case, we will just copy input in output */
        pC->m_bOnlyCopy = M4OSA_TRUE;
    }
    else
    {
        pC->m_bOnlyCopy = M4OSA_FALSE;

        /**< Initialize internal variables used for resize filter */
        for(i=0;i<nb_planes;i++)
        {

            u32_width_in = ((i==0)||(i==3))?pC->m_params.m_inputSize.m_width:(pC->m_params.m_inputSize.m_width+1)>>1;
            u32_height_in = ((i==0)||(i==3))?pC->m_params.m_inputSize.m_height:(pC->m_params.m_inputSize.m_height+1)>>1;
            u32_width_out = ((i==0)||(i==3))?pC->m_params.m_outputSize.m_width:(pC->m_params.m_outputSize.m_width+1)>>1;
            u32_height_out = ((i==0)||(i==3))?pC->m_params.m_outputSize.m_height:(pC->m_params.m_outputSize.m_height+1)>>1;

                /* Compute horizontal ratio between src and destination width.*/
                if (u32_width_out >= u32_width_in)
                {
                    pC->u32_x_inc[i]   = ((u32_width_in-1) * 0x10000) / (u32_width_out-1);
                }
                else
                {
                    pC->u32_x_inc[i]   = (u32_width_in * 0x10000) / (u32_width_out);
                }

                /* Compute vertical ratio between src and destination height.*/
                if (u32_height_out >= u32_height_in)
                {
                    pC->u32_y_inc[i]   = ((u32_height_in - 1) * 0x10000) / (u32_height_out-1);
                }
                else
                {
                    pC->u32_y_inc[i] = (u32_height_in * 0x10000) / (u32_height_out);
                }

                /*
                Calculate initial accumulator value : u32_y_accum_start.
                u32_y_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
                */
                if (pC->u32_y_inc[i] >= 0x10000)
                {
                    /*
                        Keep the fractionnal part, assimung that integer  part is coded
                        on the 16 high bits and the fractionnal on the 15 low bits
                    */
                    pC->u32_y_accum_start[i] = pC->u32_y_inc[i] & 0xffff;

                    if (!pC->u32_y_accum_start[i])
                    {
                        pC->u32_y_accum_start[i] = 0x10000;
                    }

                    pC->u32_y_accum_start[i] >>= 1;
                }
                else
                {
                    pC->u32_y_accum_start[i] = 0;
                }
                /**< Take into account that Y coordinate can be odd
                    in this case we have to put a 0.5 offset
                    for U and V plane as there a 2 times sub-sampled vs Y*/
                if((pC->m_params.m_inputCoord.m_y&0x1)&&((i==1)||(i==2)))
                {
                    pC->u32_y_accum_start[i] += 0x8000;
                }

                /*
                    Calculate initial accumulator value : u32_x_accum_start.
                    u32_x_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
                */

                if (pC->u32_x_inc[i] >= 0x10000)
                {
                    pC->u32_x_accum_start[i] = pC->u32_x_inc[i] & 0xffff;

                    if (!pC->u32_x_accum_start[i])
                    {
                        pC->u32_x_accum_start[i] = 0x10000;
                    }

                    pC->u32_x_accum_start[i] >>= 1;
                }
                else
                {
                    pC->u32_x_accum_start[i] = 0;
                }
                /**< Take into account that X coordinate can be odd
                    in this case we have to put a 0.5 offset
                    for U and V plane as there a 2 times sub-sampled vs Y*/
                if((pC->m_params.m_inputCoord.m_x&0x1)&&((i==1)||(i==2)))
                {
                    pC->u32_x_accum_start[i] += 0x8000;
                }
        }
    }

    /**< Reset variable used for stripe mode */
    pC->m_procRows = 0;

    /**< Initialize var for X/Y processing order according to orientation */
    pC->m_bFlipX = M4OSA_FALSE;
    pC->m_bFlipY = M4OSA_FALSE;
    pC->m_bRevertXY = M4OSA_FALSE;
    switch(pParams->m_outputOrientation)
    {
        case M4COMMON_kOrientationTopLeft:
            break;
        case M4COMMON_kOrientationTopRight:
            pC->m_bFlipX = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationBottomRight:
            pC->m_bFlipX = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationBottomLeft:
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationLeftTop:
            pC->m_bRevertXY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationRightTop:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
        break;
        case M4COMMON_kOrientationRightBottom:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipX = M4OSA_TRUE;
            pC->m_bFlipY = M4OSA_TRUE;
            break;
        case M4COMMON_kOrientationLeftBottom:
            pC->m_bRevertXY = M4OSA_TRUE;
            pC->m_bFlipX = M4OSA_TRUE;
            break;
        default:
        return M4ERR_PARAMETER;
    }
    /**< Update state */
    pC->m_state = M4AIR_kConfigured;

    return M4NO_ERROR ;
}


/**
 ******************************************************************************
 * M4OSA_ERR M4AIR_get(M4OSA_Context pContext, M4VIFI_ImagePlane* pIn, M4VIFI_ImagePlane* pOut)
 * @brief   This function will provide the requested resized area of interest according to settings
 *          provided in M4AIR_configure.
 * @note    In case the input format type is JPEG, input plane(s)
 *          in pIn is not used. In normal mode, dimension specified in output plane(s) structure must be the
 *          same than the one specified in M4AIR_configure. In stripe mode, only the width will be the same,
 *          height will be taken as the stripe height (typically 16).
 *          In normal mode, this function is call once to get the full output picture. In stripe mode, it is called
 *          for each stripe till the whole picture has been retrieved,and  the position of the output stripe in the output picture
 *          is internally incremented at each step.
 *          Any call to M4AIR_configure during stripe process will reset this one to the beginning of the output picture.
 * @param   pContext:   (IN) Context identifying the instance
 * @param   pIn:            (IN) Plane structure containing input Plane(s).
 * @param   pOut:       (IN/OUT)  Plane structure containing output Plane(s).
 * @return  M4NO_ERROR: there is no error
 * @return  M4ERR_ALLOC: No more memory space to add a new effect.
 * @return  M4ERR_PARAMETER: pContext is M4OSA_NULL (debug only).
 ******************************************************************************
*/
M4OSA_ERR M4AIR_get(M4OSA_Context pContext, M4VIFI_ImagePlane* pIn, M4VIFI_ImagePlane* pOut)
{
    M4AIR_InternalContext* pC = (M4AIR_InternalContext*)pContext ;
    M4OSA_UInt32 i,j,k,u32_x_frac,u32_y_frac,u32_x_accum,u32_y_accum,u32_shift;
        M4OSA_UInt8    *pu8_data_in, *pu8_data_in_org, *pu8_data_in_tmp, *pu8_data_out;
        M4OSA_UInt8    *pu8_src_top;
        M4OSA_UInt8    *pu8_src_bottom;
    M4OSA_UInt32    u32_temp_value;
    M4OSA_Int32 i32_tmp_offset;
    M4OSA_UInt32    nb_planes;



    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext) ;

    /**< Check state */
    if(M4AIR_kConfigured != pC->m_state)
    {
        return M4ERR_STATE;
    }

    if(M4AIR_kYUV420AP == pC->m_inputFormat)
    {
        nb_planes = 4;
    }
    else
    {
        nb_planes = 3;
    }

    /**< Loop on each Plane */
    for(i=0;i<nb_planes;i++)
    {

         /* Set the working pointers at the beginning of the input/output data field */

        u32_shift = ((i==0)||(i==3))?0:1; /**< Depend on Luma or Chroma */

        if((M4OSA_FALSE == pC->m_params.m_bOutputStripe)||((M4OSA_TRUE == pC->m_params.m_bOutputStripe)&&(0 == pC->m_procRows)))
        {
            /**< For input, take care about ROI */
            pu8_data_in     = pIn[i].pac_data + pIn[i].u_topleft + (pC->m_params.m_inputCoord.m_x>>u32_shift)
                        + (pC->m_params.m_inputCoord.m_y >> u32_shift) * pIn[i].u_stride;

            /** Go at end of line/column in case X/Y scanning is flipped */
            if(M4OSA_TRUE == pC->m_bFlipX)
            {
                pu8_data_in += ((pC->m_params.m_inputSize.m_width)>>u32_shift) -1 ;
            }
            if(M4OSA_TRUE == pC->m_bFlipY)
            {
                pu8_data_in += ((pC->m_params.m_inputSize.m_height>>u32_shift) -1) * pIn[i].u_stride;
            }

            /**< Initialize accumulators in case we are using it (bilinear interpolation) */
            if( M4OSA_FALSE == pC->m_bOnlyCopy)
            {
                pC->u32_x_accum[i] = pC->u32_x_accum_start[i];
                pC->u32_y_accum[i] = pC->u32_y_accum_start[i];
            }

        }
        else
        {
            /**< In case of stripe mode for other than first stripe, we need to recover input pointer from internal context */
            pu8_data_in = pC->pu8_data_in[i];
        }

        /**< In every mode, output data are at the beginning of the output plane */
        pu8_data_out    = pOut[i].pac_data + pOut[i].u_topleft;

        /**< Initialize input offset applied after each pixel */
        if(M4OSA_FALSE == pC->m_bFlipY)
        {
            i32_tmp_offset = pIn[i].u_stride;
        }
        else
        {
            i32_tmp_offset = -pIn[i].u_stride;
        }

        /**< In this case, no bilinear interpolation is needed as input and output dimensions are the same */
        if( M4OSA_TRUE == pC->m_bOnlyCopy)
        {
            /**< No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {
                /**< No flip on X abscissa */
                if(M4OSA_FALSE == pC->m_bFlipX)
                {
                     M4OSA_UInt32 loc_height = pOut[i].u_height;
                     M4OSA_UInt32 loc_width = pOut[i].u_width;
                     M4OSA_UInt32 loc_stride = pIn[i].u_stride;
                    /**< Loop on each row */
                    for (j=0; j<loc_height; j++)
                    {
                        /**< Copy one whole line */
                        memcpy((void *)pu8_data_out, (void *)pu8_data_in, loc_width);

                        /**< Update pointers */
                        pu8_data_out += pOut[i].u_stride;
                        if(M4OSA_FALSE == pC->m_bFlipY)
                        {
                            pu8_data_in += loc_stride;
                        }
                        else
                        {
                            pu8_data_in -= loc_stride;
                        }
                    }
                }
                else
                {
                    /**< Loop on each row */
                    for(j=0;j<pOut[i].u_height;j++)
                    {
                        /**< Loop on each pixel of 1 row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {
                            *pu8_data_out++ = *pu8_data_in--;
                        }

                        /**< Update pointers */
                        pu8_data_out += (pOut[i].u_stride - pOut[i].u_width);

                        pu8_data_in += pOut[i].u_width + i32_tmp_offset;

                    }
                }
            }
            /**< Here we have a +-90° rotation */
            else
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    pu8_data_in_tmp = pu8_data_in;

                    /**< Loop on each pixel of 1 row */
                    for(k=0;k<pOut[i].u_width;k++)
                    {
                        *pu8_data_out++ = *pu8_data_in_tmp;

                        /**< Update input pointer in order to go to next/past line */
                        pu8_data_in_tmp += i32_tmp_offset;
                    }

                    /**< Update pointers */
                    pu8_data_out += (pOut[i].u_stride - pOut[i].u_width);
                    if(M4OSA_FALSE == pC->m_bFlipX)
                    {
                        pu8_data_in ++;
                    }
                    else
                    {
                        pu8_data_in --;
                    }
                }
            }
        }
        /**< Bilinear interpolation */
        else
        {

        if(3 != i)  /**< other than alpha plane */
        {
            /**No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* Vertical weight factor */
                    u32_y_frac = (pC->u32_y_accum[i]>>12)&15;

                    /* Reinit horizontal weight factor */
                    u32_x_accum = pC->u32_x_accum_start[i];



                        if(M4OSA_TRUE ==  pC->m_bFlipX)
                        {

                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {

                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal weight factor */

                                pu8_src_top = (pu8_data_in - (u32_x_accum >> 16)) -1 ;

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }
                        }

                        else
                        {
                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {
                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal weight factor */

                                pu8_src_top = pu8_data_in + (u32_x_accum >> 16);

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                                    *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }

                        }

                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update vertical accumulator */
                    pC->u32_y_accum[i] += pC->u32_y_inc[i];
                    if (pC->u32_y_accum[i]>>16)
                    {
                        pu8_data_in = pu8_data_in + (pC->u32_y_accum[i] >> 16) * i32_tmp_offset;
                        pC->u32_y_accum[i] &= 0xffff;
                    }
                }
        }
            /** +-90° rotation */
            else
            {
                pu8_data_in_org = pu8_data_in;

                /**< Loop on each output row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* horizontal weight factor */
                    u32_x_frac = (pC->u32_x_accum[i]>>12)&15;

                    /* Reinit accumulator */
                    u32_y_accum = pC->u32_y_accum_start[i];

                    if(M4OSA_TRUE ==  pC->m_bFlipX)
                    {

                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */


                            pu8_src_top = (pu8_data_in - (pC->u32_x_accum[i] >> 16)) - 1;

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                            if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                u32_y_accum &= 0xffff;
                            }

                        }
                    }
                    else
                    {
                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */

                            pu8_src_top = pu8_data_in + (pC->u32_x_accum[i] >> 16);

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                            if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                u32_y_accum &= 0xffff;
                            }
                        }
                    }
                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update horizontal accumulator */
                    pC->u32_x_accum[i] += pC->u32_x_inc[i];

                    pu8_data_in = pu8_data_in_org;
                }

            }
            }/** 3 != i */
            else
            {
            /**No +-90° rotation */
            if(M4OSA_FALSE == pC->m_bRevertXY)
            {

                /**< Loop on each row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* Vertical weight factor */
                    u32_y_frac = (pC->u32_y_accum[i]>>12)&15;

                    /* Reinit horizontal weight factor */
                    u32_x_accum = pC->u32_x_accum_start[i];



                        if(M4OSA_TRUE ==  pC->m_bFlipX)
                        {

                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {

                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal weight factor */

                                pu8_src_top = (pu8_data_in - (u32_x_accum >> 16)) -1 ;

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                                u32_temp_value= (u32_temp_value >> 7)*0xff;

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }
                        }

                        else
                        {
                            /**< Loop on each output pixel in a row */
                            for(k=0;k<pOut[i].u_width;k++)
                            {
                                u32_x_frac = (u32_x_accum >> 12)&15; /* Fraction of Horizontal weight factor */

                                pu8_src_top = pu8_data_in + (u32_x_accum >> 16);

                                pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                                /* Weighted combination */
                                u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                                u32_temp_value= (u32_temp_value >> 7)*0xff;

                                *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                                /* Update horizontal accumulator */
                                u32_x_accum += pC->u32_x_inc[i];
                            }

                        }

                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update vertical accumulator */
                    pC->u32_y_accum[i] += pC->u32_y_inc[i];
                    if (pC->u32_y_accum[i]>>16)
                    {
                        pu8_data_in = pu8_data_in + (pC->u32_y_accum[i] >> 16) * i32_tmp_offset;
                        pC->u32_y_accum[i] &= 0xffff;
                    }
                }

            } /**< M4OSA_FALSE == pC->m_bRevertXY */
            /** +-90° rotation */
            else
            {
                pu8_data_in_org = pu8_data_in;

                /**< Loop on each output row */
                for(j=0;j<pOut[i].u_height;j++)
                {
                    /* horizontal weight factor */
                    u32_x_frac = (pC->u32_x_accum[i]>>12)&15;

                    /* Reinit accumulator */
                    u32_y_accum = pC->u32_y_accum_start[i];

                    if(M4OSA_TRUE ==  pC->m_bFlipX)
                    {

                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */


                            pu8_src_top = (pu8_data_in - (pC->u32_x_accum[i] >> 16)) - 1;

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[1]*(16-u32_x_frac) +
                                                                 pu8_src_top[0]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[1]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[0]*u32_x_frac)*u32_y_frac )>>8);

                            u32_temp_value= (u32_temp_value >> 7)*0xff;

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                            if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                u32_y_accum &= 0xffff;
                            }

                        }
                    }
                    else
                    {
                        /**< Loop on each output pixel in a row */
                        for(k=0;k<pOut[i].u_width;k++)
                        {

                            u32_y_frac = (u32_y_accum >> 12)&15; /* Vertical weight factor */

                            pu8_src_top = pu8_data_in + (pC->u32_x_accum[i] >> 16);

                            pu8_src_bottom = pu8_src_top + i32_tmp_offset;

                            /* Weighted combination */
                            u32_temp_value = (M4VIFI_UInt8)(((pu8_src_top[0]*(16-u32_x_frac) +
                                                                 pu8_src_top[1]*u32_x_frac)*(16-u32_y_frac) +
                                                                (pu8_src_bottom[0]*(16-u32_x_frac) +
                                                                 pu8_src_bottom[1]*u32_x_frac)*u32_y_frac )>>8);

                            u32_temp_value= (u32_temp_value >> 7)*0xff;

                            *pu8_data_out++ = (M4VIFI_UInt8)u32_temp_value;

                            /* Update vertical accumulator */
                            u32_y_accum += pC->u32_y_inc[i];
                            if (u32_y_accum>>16)
                            {
                                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * i32_tmp_offset;
                                u32_y_accum &= 0xffff;
                            }
                        }
                    }
                    pu8_data_out += pOut[i].u_stride - pOut[i].u_width;

                    /* Update horizontal accumulator */
                    pC->u32_x_accum[i] += pC->u32_x_inc[i];

                    pu8_data_in = pu8_data_in_org;

                }
                } /**< M4OSA_TRUE == pC->m_bRevertXY */
        }/** 3 == i */
            }
        /**< In case of stripe mode, save current input pointer */
        if(M4OSA_TRUE == pC->m_params.m_bOutputStripe)
        {
            pC->pu8_data_in[i] = pu8_data_in;
        }
    }

    /**< Update number of processed rows, reset it if we have finished with the whole processing */
    pC->m_procRows += pOut[0].u_height;
    if(M4OSA_FALSE == pC->m_bRevertXY)
    {
        if(pC->m_params.m_outputSize.m_height <= pC->m_procRows)    pC->m_procRows = 0;
    }
    else
    {
        if(pC->m_params.m_outputSize.m_width <= pC->m_procRows) pC->m_procRows = 0;
    }

    return M4NO_ERROR ;

}
/*+ Handle the image files here */

/**
 ******************************************************************************
 * M4OSA_ERR LvGetImageThumbNail(M4OSA_UChar *fileName, M4OSA_Void **pBuffer)
 * @brief   This function gives YUV420 buffer of a given image file (in argb888 format)
 * @Note: The caller of the function is responsible to free the yuv buffer allocated
 * @param   fileName:       (IN) Path to the filename of the image argb data
 * @param   height:     (IN) Height of the image
 * @param     width:             (OUT) pBuffer pointer to the address where the yuv data address needs to be returned.
 * @return  M4NO_ERROR: there is no error
 * @return  M4ERR_ALLOC: No more memory space to add a new effect.
 * @return  M4ERR_FILE_NOT_FOUND: if the file passed does not exists.
 ******************************************************************************
*/
M4OSA_ERR LvGetImageThumbNail(const char *fileName, M4OSA_UInt32 height, M4OSA_UInt32 width, M4OSA_Void **pBuffer) {

    M4VIFI_ImagePlane rgbPlane, *yuvPlane;
    M4OSA_UInt32 frameSize_argb = (width * height * 4); // argb data
    M4OSA_Context lImageFileFp  = M4OSA_NULL;
    M4OSA_ERR err = M4NO_ERROR;

    M4OSA_UInt8 *pTmpData = (M4OSA_UInt8*) M4OSA_32bitAlignedMalloc(frameSize_argb, M4VS, (M4OSA_Char*)"Image argb data");
    if(pTmpData == M4OSA_NULL) {
        ALOGE("Failed to allocate memory for Image clip");
        return M4ERR_ALLOC;
    }

       /** Read the argb data from the passed file. */
    M4OSA_ERR lerr = M4OSA_fileReadOpen(&lImageFileFp, (M4OSA_Void *) fileName, M4OSA_kFileRead);

    if((lerr != M4NO_ERROR) || (lImageFileFp == M4OSA_NULL))
    {
        ALOGE("LVPreviewController: Can not open the file ");
        free(pTmpData);
        return M4ERR_FILE_NOT_FOUND;
    }
    lerr = M4OSA_fileReadData(lImageFileFp, (M4OSA_MemAddr8)pTmpData, &frameSize_argb);
    if(lerr != M4NO_ERROR)
    {
        ALOGE("LVPreviewController: can not read the data ");
        M4OSA_fileReadClose(lImageFileFp);
        free(pTmpData);
        return lerr;
    }
    M4OSA_fileReadClose(lImageFileFp);

    M4OSA_UInt32 frameSize = (width * height * 3); //Size of YUV420 data.
    rgbPlane.pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(frameSize, M4VS, (M4OSA_Char*)"Image clip RGB888 data");
    if(rgbPlane.pac_data == M4OSA_NULL)
    {
        ALOGE("Failed to allocate memory for Image clip");
        free(pTmpData);
        return M4ERR_ALLOC;
    }

    /** Remove the alpha channel */
    for (M4OSA_UInt32 i=0, j = 0; i < frameSize_argb; i++) {
        if ((i % 4) == 0) continue;
        rgbPlane.pac_data[j] = pTmpData[i];
        j++;
    }
    free(pTmpData);

#ifdef FILE_DUMP
    FILE *fp = fopen("/sdcard/Input/test_rgb.raw", "wb");
    if(fp == NULL)
        ALOGE("Errors file can not be created");
    else {
        fwrite(rgbPlane.pac_data, frameSize, 1, fp);
        fclose(fp);
    }
#endif
        rgbPlane.u_height = height;
        rgbPlane.u_width = width;
        rgbPlane.u_stride = width*3;
        rgbPlane.u_topleft = 0;

        yuvPlane = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane),
                M4VS, (M4OSA_Char*)"M4xVSS_internalConvertRGBtoYUV: Output plane YUV");
        yuvPlane[0].u_height = height;
        yuvPlane[0].u_width = width;
        yuvPlane[0].u_stride = width;
        yuvPlane[0].u_topleft = 0;
        yuvPlane[0].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(yuvPlane[0].u_height * yuvPlane[0].u_width * 1.5, M4VS, (M4OSA_Char*)"imageClip YUV data");

        yuvPlane[1].u_height = yuvPlane[0].u_height >>1;
        yuvPlane[1].u_width = yuvPlane[0].u_width >> 1;
        yuvPlane[1].u_stride = yuvPlane[1].u_width;
        yuvPlane[1].u_topleft = 0;
        yuvPlane[1].pac_data = (M4VIFI_UInt8*)(yuvPlane[0].pac_data + yuvPlane[0].u_height * yuvPlane[0].u_width);

        yuvPlane[2].u_height = yuvPlane[0].u_height >>1;
        yuvPlane[2].u_width = yuvPlane[0].u_width >> 1;
        yuvPlane[2].u_stride = yuvPlane[2].u_width;
        yuvPlane[2].u_topleft = 0;
        yuvPlane[2].pac_data = (M4VIFI_UInt8*)(yuvPlane[1].pac_data + yuvPlane[1].u_height * yuvPlane[1].u_width);


        err = M4VIFI_RGB888toYUV420(M4OSA_NULL, &rgbPlane, yuvPlane);
        //err = M4VIFI_BGR888toYUV420(M4OSA_NULL, &rgbPlane, yuvPlane);
        if(err != M4NO_ERROR)
        {
            ALOGE("error when converting from RGB to YUV: 0x%x\n", (unsigned int)err);
        }
        free(rgbPlane.pac_data);

        //ALOGE("RGB to YUV done");
#ifdef FILE_DUMP
        FILE *fp1 = fopen("/sdcard/Input/test_yuv.raw", "wb");
        if(fp1 == NULL)
            ALOGE("Errors file can not be created");
        else {
            fwrite(yuvPlane[0].pac_data, yuvPlane[0].u_height * yuvPlane[0].u_width * 1.5, 1, fp1);
            fclose(fp1);
        }
#endif
        *pBuffer = yuvPlane[0].pac_data;
        free(yuvPlane);
        return M4NO_ERROR;

}
M4OSA_Void prepareYUV420ImagePlane(M4VIFI_ImagePlane *plane,
    M4OSA_UInt32 width, M4OSA_UInt32 height, M4VIFI_UInt8 *buffer,
    M4OSA_UInt32 reportedWidth, M4OSA_UInt32 reportedHeight) {

    //Y plane
    plane[0].u_width = width;
    plane[0].u_height = height;
    plane[0].u_stride = reportedWidth;
    plane[0].u_topleft = 0;
    plane[0].pac_data = buffer;

    // U plane
    plane[1].u_width = width/2;
    plane[1].u_height = height/2;
    plane[1].u_stride = reportedWidth >> 1;
    plane[1].u_topleft = 0;
    plane[1].pac_data = buffer+(reportedWidth*reportedHeight);

    // V Plane
    plane[2].u_width = width/2;
    plane[2].u_height = height/2;
    plane[2].u_stride = reportedWidth >> 1;
    plane[2].u_topleft = 0;
    plane[2].pac_data = plane[1].pac_data + ((reportedWidth/2)*(reportedHeight/2));
}

M4OSA_Void prepareYV12ImagePlane(M4VIFI_ImagePlane *plane,
    M4OSA_UInt32 width, M4OSA_UInt32 height, M4OSA_UInt32 stride,
    M4VIFI_UInt8 *buffer) {

    //Y plane
    plane[0].u_width = width;
    plane[0].u_height = height;
    plane[0].u_stride = stride;
    plane[0].u_topleft = 0;
    plane[0].pac_data = buffer;

    // U plane
    plane[1].u_width = width/2;
    plane[1].u_height = height/2;
    plane[1].u_stride = android::PreviewRenderer::ALIGN(plane[0].u_stride/2, 16);
    plane[1].u_topleft = 0;
    plane[1].pac_data = (buffer
                + plane[0].u_height * plane[0].u_stride
                + (plane[0].u_height/2) * android::PreviewRenderer::ALIGN((
                 plane[0].u_stride / 2), 16));

    // V Plane
    plane[2].u_width = width/2;
    plane[2].u_height = height/2;
    plane[2].u_stride = android::PreviewRenderer::ALIGN(plane[0].u_stride/2, 16);
    plane[2].u_topleft = 0;
    plane[2].pac_data = (buffer +
     plane[0].u_height * android::PreviewRenderer::ALIGN(plane[0].u_stride, 16));


}

M4OSA_Void swapImagePlanes(
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2) {

    planeIn[0].u_height = planeOut[0].u_height;
    planeIn[0].u_width = planeOut[0].u_width;
    planeIn[0].u_stride = planeOut[0].u_stride;
    planeIn[0].u_topleft = planeOut[0].u_topleft;
    planeIn[0].pac_data = planeOut[0].pac_data;

    /**
     * U plane */
    planeIn[1].u_width = planeOut[1].u_width;
    planeIn[1].u_height = planeOut[1].u_height;
    planeIn[1].u_stride = planeOut[1].u_stride;
    planeIn[1].u_topleft = planeOut[1].u_topleft;
    planeIn[1].pac_data = planeOut[1].pac_data;
    /**
     * V Plane */
    planeIn[2].u_width = planeOut[2].u_width;
    planeIn[2].u_height = planeOut[2].u_height;
    planeIn[2].u_stride = planeOut[2].u_stride;
    planeIn[2].u_topleft = planeOut[2].u_topleft;
    planeIn[2].pac_data = planeOut[2].pac_data;

    if(planeOut[0].pac_data == (M4VIFI_UInt8*)buffer1)
    {
        planeOut[0].pac_data = (M4VIFI_UInt8*)buffer2;
        planeOut[1].pac_data = (M4VIFI_UInt8*)(buffer2 +
         planeOut[0].u_width*planeOut[0].u_height);

        planeOut[2].pac_data = (M4VIFI_UInt8*)(buffer2 +
         planeOut[0].u_width*planeOut[0].u_height +
         planeOut[1].u_width*planeOut[1].u_height);
    }
    else
    {
        planeOut[0].pac_data = (M4VIFI_UInt8*)buffer1;
        planeOut[1].pac_data = (M4VIFI_UInt8*)(buffer1 +
         planeOut[0].u_width*planeOut[0].u_height);

        planeOut[2].pac_data = (M4VIFI_UInt8*)(buffer1 +
         planeOut[0].u_width*planeOut[0].u_height +
         planeOut[1].u_width*planeOut[1].u_height);
    }

}

M4OSA_Void computePercentageDone(
    M4OSA_UInt32 ctsMs, M4OSA_UInt32 effectStartTimeMs,
    M4OSA_UInt32 effectDuration, M4OSA_Double *percentageDone) {

    M4OSA_Double videoEffectTime =0;

    // Compute how far from the beginning of the effect we are, in clip-base time.
    videoEffectTime =
     (M4OSA_Int32)(ctsMs+ 0.5) - effectStartTimeMs;

    // To calculate %, substract timeIncrement
    // because effect should finish on the last frame
    // which is from CTS = (eof-timeIncrement) till CTS = eof
    *percentageDone =
     videoEffectTime / ((M4OSA_Float)effectDuration);

    if(*percentageDone < 0.0) *percentageDone = 0.0;
    if(*percentageDone > 1.0) *percentageDone = 1.0;

}


M4OSA_Void computeProgressForVideoEffect(
    M4OSA_UInt32 ctsMs, M4OSA_UInt32 effectStartTimeMs,
    M4OSA_UInt32 effectDuration, M4VSS3GPP_ExternalProgress* extProgress) {

    M4OSA_Double percentageDone =0;

    computePercentageDone(ctsMs, effectStartTimeMs, effectDuration, &percentageDone);

    extProgress->uiProgress = (M4OSA_UInt32)( percentageDone * 1000 );
    extProgress->uiOutputTime = (M4OSA_UInt32)(ctsMs + 0.5);
    extProgress->uiClipTime = extProgress->uiOutputTime;
    extProgress->bIsLast = M4OSA_FALSE;
}

M4OSA_ERR prepareFramingStructure(
    M4xVSS_FramingStruct* framingCtx,
    M4VSS3GPP_EffectSettings* effectsSettings, M4OSA_UInt32 index,
    M4VIFI_UInt8* overlayRGB, M4VIFI_UInt8* overlayYUV) {

    M4OSA_ERR err = M4NO_ERROR;

    // Force input RGB buffer to even size to avoid errors in YUV conversion
    framingCtx->FramingRgb = effectsSettings[index].xVSS.pFramingBuffer;
    framingCtx->FramingRgb->u_width = framingCtx->FramingRgb->u_width & ~1;
    framingCtx->FramingRgb->u_height = framingCtx->FramingRgb->u_height & ~1;
    framingCtx->FramingYuv = NULL;

    framingCtx->duration = effectsSettings[index].uiDuration;
    framingCtx->topleft_x = effectsSettings[index].xVSS.topleft_x;
    framingCtx->topleft_y = effectsSettings[index].xVSS.topleft_y;
    framingCtx->pCurrent = framingCtx;
    framingCtx->pNext = framingCtx;
    framingCtx->previousClipTime = -1;

    framingCtx->alphaBlendingStruct =
     (M4xVSS_internalEffectsAlphaBlending*)M4OSA_32bitAlignedMalloc(
      sizeof(M4xVSS_internalEffectsAlphaBlending), M4VS,
      (M4OSA_Char*)"alpha blending struct");

    framingCtx->alphaBlendingStruct->m_fadeInTime =
     effectsSettings[index].xVSS.uialphaBlendingFadeInTime;

    framingCtx->alphaBlendingStruct->m_fadeOutTime =
     effectsSettings[index].xVSS.uialphaBlendingFadeOutTime;

    framingCtx->alphaBlendingStruct->m_end =
     effectsSettings[index].xVSS.uialphaBlendingEnd;

    framingCtx->alphaBlendingStruct->m_middle =
     effectsSettings[index].xVSS.uialphaBlendingMiddle;

    framingCtx->alphaBlendingStruct->m_start =
     effectsSettings[index].xVSS.uialphaBlendingStart;

    // If new Overlay buffer, convert from RGB to YUV
    if((overlayRGB != framingCtx->FramingRgb->pac_data) || (overlayYUV == NULL) ) {

        // If YUV buffer exists, delete it
        if(overlayYUV != NULL) {
           free(overlayYUV);
           overlayYUV = NULL;
        }
    if(effectsSettings[index].xVSS.rgbType == M4VSS3GPP_kRGB565) {
        // Input RGB565 plane is provided,
        // let's convert it to YUV420, and update framing structure
        err = M4xVSS_internalConvertRGBtoYUV(framingCtx);
    }
    else if(effectsSettings[index].xVSS.rgbType == M4VSS3GPP_kRGB888) {
        // Input RGB888 plane is provided,
        // let's convert it to YUV420, and update framing structure
        err = M4xVSS_internalConvertRGB888toYUV(framingCtx);
    }
    else {
        err = M4ERR_PARAMETER;
    }
        overlayYUV = framingCtx->FramingYuv[0].pac_data;
        overlayRGB = framingCtx->FramingRgb->pac_data;

    }
    else {
        ALOGV(" YUV buffer reuse");
        framingCtx->FramingYuv = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(
            3*sizeof(M4VIFI_ImagePlane), M4VS, (M4OSA_Char*)"YUV");

        if(framingCtx->FramingYuv == M4OSA_NULL) {
            return M4ERR_ALLOC;
        }

        framingCtx->FramingYuv[0].u_width = framingCtx->FramingRgb->u_width;
        framingCtx->FramingYuv[0].u_height = framingCtx->FramingRgb->u_height;
        framingCtx->FramingYuv[0].u_topleft = 0;
        framingCtx->FramingYuv[0].u_stride = framingCtx->FramingRgb->u_width;
        framingCtx->FramingYuv[0].pac_data = (M4VIFI_UInt8*)overlayYUV;

        framingCtx->FramingYuv[1].u_width = (framingCtx->FramingRgb->u_width)>>1;
        framingCtx->FramingYuv[1].u_height = (framingCtx->FramingRgb->u_height)>>1;
        framingCtx->FramingYuv[1].u_topleft = 0;
        framingCtx->FramingYuv[1].u_stride = (framingCtx->FramingRgb->u_width)>>1;
        framingCtx->FramingYuv[1].pac_data = framingCtx->FramingYuv[0].pac_data +
            framingCtx->FramingYuv[0].u_width * framingCtx->FramingYuv[0].u_height;

        framingCtx->FramingYuv[2].u_width = (framingCtx->FramingRgb->u_width)>>1;
        framingCtx->FramingYuv[2].u_height = (framingCtx->FramingRgb->u_height)>>1;
        framingCtx->FramingYuv[2].u_topleft = 0;
        framingCtx->FramingYuv[2].u_stride = (framingCtx->FramingRgb->u_width)>>1;
        framingCtx->FramingYuv[2].pac_data = framingCtx->FramingYuv[1].pac_data +
            framingCtx->FramingYuv[1].u_width * framingCtx->FramingYuv[1].u_height;

        framingCtx->duration = 0;
        framingCtx->previousClipTime = -1;
        framingCtx->previewOffsetClipTime = -1;

    }
    return err;
}

M4OSA_ERR applyColorEffect(M4xVSS_VideoEffectType colorEffect,
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2, M4OSA_UInt16 rgbColorData) {

    M4xVSS_ColorStruct colorContext;
    M4OSA_ERR err = M4NO_ERROR;

    colorContext.colorEffectType = colorEffect;
    colorContext.rgb16ColorData = rgbColorData;

    err = M4VSS3GPP_externalVideoEffectColor(
     (M4OSA_Void *)&colorContext, planeIn, planeOut, NULL,
     colorEffect);

    if(err != M4NO_ERROR) {
        ALOGV("M4VSS3GPP_externalVideoEffectColor(%d) error %d",
            colorEffect, err);

        if(NULL != buffer1) {
            free(buffer1);
            buffer1 = NULL;
        }
        if(NULL != buffer2) {
            free(buffer2);
            buffer2 = NULL;
        }
        return err;
    }

    // The out plane now becomes the in plane for adding other effects
    swapImagePlanes(planeIn, planeOut, buffer1, buffer2);

    return err;
}

M4OSA_ERR applyLumaEffect(M4VSS3GPP_VideoEffectType videoEffect,
    M4VIFI_ImagePlane *planeIn, M4VIFI_ImagePlane *planeOut,
    M4VIFI_UInt8 *buffer1, M4VIFI_UInt8 *buffer2, M4OSA_Int32 lum_factor) {

    M4OSA_ERR err = M4NO_ERROR;

    err = M4VFL_modifyLumaWithScale(
         (M4ViComImagePlane*)planeIn,(M4ViComImagePlane*)planeOut,
         lum_factor, NULL);

    if(err != M4NO_ERROR) {
        ALOGE("M4VFL_modifyLumaWithScale(%d) error %d", videoEffect, (int)err);

        if(NULL != buffer1) {
            free(buffer1);
            buffer1= NULL;
        }
        if(NULL != buffer2) {
            free(buffer2);
            buffer2= NULL;
        }
        return err;
    }

    // The out plane now becomes the in plane for adding other effects
    swapImagePlanes(planeIn, planeOut,(M4VIFI_UInt8 *)buffer1,
     (M4VIFI_UInt8 *)buffer2);

    return err;
}

M4OSA_ERR applyEffectsAndRenderingMode(vePostProcessParams *params,
    M4OSA_UInt32 reportedWidth, M4OSA_UInt32 reportedHeight) {

    M4OSA_ERR err = M4NO_ERROR;
    M4VIFI_ImagePlane planeIn[3], planeOut[3];
    M4VIFI_UInt8 *finalOutputBuffer = NULL, *tempOutputBuffer= NULL;
    M4OSA_Double percentageDone =0;
    M4OSA_Int32 lum_factor;
    M4VSS3GPP_ExternalProgress extProgress;
    M4xVSS_FiftiesStruct fiftiesCtx;
    M4OSA_UInt32 frameSize = 0, i=0;

    frameSize = (params->videoWidth*params->videoHeight*3) >> 1;

    finalOutputBuffer = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(frameSize, M4VS,
     (M4OSA_Char*)("lvpp finalOutputBuffer"));

    if(finalOutputBuffer == NULL) {
        ALOGE("applyEffectsAndRenderingMode: malloc error");
        return M4ERR_ALLOC;
    }

    // allocate the tempOutputBuffer
    tempOutputBuffer = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(
     ((params->videoHeight*params->videoWidth*3)>>1), M4VS, (M4OSA_Char*)("lvpp colorBuffer"));

    if(tempOutputBuffer == NULL) {
        ALOGE("applyEffectsAndRenderingMode: malloc error tempOutputBuffer");
        if(NULL != finalOutputBuffer) {
            free(finalOutputBuffer);
            finalOutputBuffer = NULL;
        }
        return M4ERR_ALLOC;
    }

    // Initialize the In plane
    prepareYUV420ImagePlane(planeIn, params->videoWidth, params->videoHeight,
       params->vidBuffer, reportedWidth, reportedHeight);

    // Initialize the Out plane
    prepareYUV420ImagePlane(planeOut, params->videoWidth, params->videoHeight,
       (M4VIFI_UInt8 *)tempOutputBuffer, params->videoWidth, params->videoHeight);

    // The planeIn contains the YUV420 input data to postprocessing node
    // and planeOut will contain the YUV420 data with effect
    // In each successive if condition, apply filter to successive
    // output YUV frame so that concurrent effects are both applied

    if(params->currentVideoEffect & VIDEO_EFFECT_BLACKANDWHITE) {
        err = applyColorEffect(M4xVSS_kVideoEffectType_BlackAndWhite,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer, 0);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_PINK) {
        err = applyColorEffect(M4xVSS_kVideoEffectType_Pink,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer, 0);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_GREEN) {
        err = applyColorEffect(M4xVSS_kVideoEffectType_Green,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer, 0);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_SEPIA) {
        err = applyColorEffect(M4xVSS_kVideoEffectType_Sepia,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer, 0);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_NEGATIVE) {
        err = applyColorEffect(M4xVSS_kVideoEffectType_Negative,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer, 0);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_GRADIENT) {
        // find the effect in effectSettings array
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             (M4VSS3GPP_VideoEffectType)M4xVSS_kVideoEffectType_Gradient)
                break;
        }
        err = applyColorEffect(M4xVSS_kVideoEffectType_Gradient,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer,
              params->effectsSettings[i].xVSS.uiRgb16InputColor);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_COLOR_RGB16) {
        // Find the effect in effectSettings array
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             (M4VSS3GPP_VideoEffectType)M4xVSS_kVideoEffectType_ColorRGB16)
                break;
        }
        err = applyColorEffect(M4xVSS_kVideoEffectType_ColorRGB16,
              planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
              (M4VIFI_UInt8 *)tempOutputBuffer,
              params->effectsSettings[i].xVSS.uiRgb16InputColor);
        if(err != M4NO_ERROR) {
            return err;
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_FIFTIES) {
        // Find the effect in effectSettings array
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             (M4VSS3GPP_VideoEffectType)M4xVSS_kVideoEffectType_Fifties)
                break;
        }
        if(i < params->numberEffects) {
            computeProgressForVideoEffect(params->timeMs,
             params->effectsSettings[i].uiStartTime,
             params->effectsSettings[i].uiDuration, &extProgress);

            if(params->isFiftiesEffectStarted) {
                fiftiesCtx.previousClipTime = -1;
            }
            fiftiesCtx.fiftiesEffectDuration =
             1000/params->effectsSettings[i].xVSS.uiFiftiesOutFrameRate;

            fiftiesCtx.shiftRandomValue = 0;
            fiftiesCtx.stripeRandomValue = 0;

            err = M4VSS3GPP_externalVideoEffectFifties(
             (M4OSA_Void *)&fiftiesCtx, planeIn, planeOut, &extProgress,
             M4xVSS_kVideoEffectType_Fifties);

            if(err != M4NO_ERROR) {
                ALOGE("M4VSS3GPP_externalVideoEffectFifties error 0x%x", (unsigned int)err);

                if(NULL != finalOutputBuffer) {
                    free(finalOutputBuffer);
                    finalOutputBuffer = NULL;
                }
                if(NULL != tempOutputBuffer) {
                    free(tempOutputBuffer);
                    tempOutputBuffer = NULL;
                }
                return err;
            }

            // The out plane now becomes the in plane for adding other effects
            swapImagePlanes(planeIn, planeOut,(M4VIFI_UInt8 *)finalOutputBuffer,
             (M4VIFI_UInt8 *)tempOutputBuffer);
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_FRAMING) {

        M4xVSS_FramingStruct framingCtx;
        // Find the effect in effectSettings array
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             (M4VSS3GPP_VideoEffectType)M4xVSS_kVideoEffectType_Framing) {
                if((params->effectsSettings[i].uiStartTime <= params->timeMs + params->timeOffset) &&
                   ((params->effectsSettings[i].uiStartTime+
                     params->effectsSettings[i].uiDuration) >= params->timeMs + params->timeOffset))
                {
                        break;
                }
            }
        }
        if(i < params->numberEffects) {
            computeProgressForVideoEffect(params->timeMs,
             params->effectsSettings[i].uiStartTime,
             params->effectsSettings[i].uiDuration, &extProgress);

            err = prepareFramingStructure(&framingCtx,
                  params->effectsSettings, i, params->overlayFrameRGBBuffer,
                  params->overlayFrameYUVBuffer);

            if(err == M4NO_ERROR) {
                err = M4VSS3GPP_externalVideoEffectFraming(
                      (M4OSA_Void *)&framingCtx, planeIn, planeOut, &extProgress,
                      M4xVSS_kVideoEffectType_Framing);
            }

            free(framingCtx.alphaBlendingStruct);

            if(framingCtx.FramingYuv != NULL) {
                free(framingCtx.FramingYuv);
                framingCtx.FramingYuv = NULL;
            }
            //If prepareFramingStructure / M4VSS3GPP_externalVideoEffectFraming
            // returned error, then return from function
            if(err != M4NO_ERROR) {

                if(NULL != finalOutputBuffer) {
                    free(finalOutputBuffer);
                    finalOutputBuffer = NULL;
                }
                if(NULL != tempOutputBuffer) {
                    free(tempOutputBuffer);
                    tempOutputBuffer = NULL;
                }
                return err;
            }

            // The out plane now becomes the in plane for adding other effects
            swapImagePlanes(planeIn, planeOut,(M4VIFI_UInt8 *)finalOutputBuffer,
             (M4VIFI_UInt8 *)tempOutputBuffer);
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_FADEFROMBLACK) {
        /* find the effect in effectSettings array*/
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             M4VSS3GPP_kVideoEffectType_FadeFromBlack)
                break;
        }

        if(i < params->numberEffects) {
            computePercentageDone(params->timeMs,
             params->effectsSettings[i].uiStartTime,
             params->effectsSettings[i].uiDuration, &percentageDone);

            // Compute where we are in the effect (scale is 0->1024)
            lum_factor = (M4OSA_Int32)( percentageDone * 1024 );
            // Apply the darkening effect
            err = applyLumaEffect(M4VSS3GPP_kVideoEffectType_FadeFromBlack,
                  planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
                  (M4VIFI_UInt8 *)tempOutputBuffer, lum_factor);
            if(err != M4NO_ERROR) {
                return err;
            }
        }
    }

    if(params->currentVideoEffect & VIDEO_EFFECT_FADETOBLACK) {
        // Find the effect in effectSettings array
        for(i=0;i<params->numberEffects;i++) {
            if(params->effectsSettings[i].VideoEffectType ==
             M4VSS3GPP_kVideoEffectType_FadeToBlack)
                break;
        }
        if(i < params->numberEffects) {
            computePercentageDone(params->timeMs,
             params->effectsSettings[i].uiStartTime,
             params->effectsSettings[i].uiDuration, &percentageDone);

            // Compute where we are in the effect (scale is 0->1024)
            lum_factor = (M4OSA_Int32)( (1.0-percentageDone) * 1024 );
            // Apply the darkening effect
            err = applyLumaEffect(M4VSS3GPP_kVideoEffectType_FadeToBlack,
                  planeIn, planeOut, (M4VIFI_UInt8 *)finalOutputBuffer,
                  (M4VIFI_UInt8 *)tempOutputBuffer, lum_factor);
            if(err != M4NO_ERROR) {
                return err;
            }
        }
    }

    ALOGV("doMediaRendering CALL getBuffer()");
    // Set the output YUV420 plane to be compatible with YV12 format
    // W & H even
    // YVU instead of YUV
    // align buffers on 32 bits

    // Y plane
    //in YV12 format, sizes must be even
    M4OSA_UInt32 yv12PlaneWidth = ((params->outVideoWidth +1)>>1)<<1;
    M4OSA_UInt32 yv12PlaneHeight = ((params->outVideoHeight+1)>>1)<<1;

    prepareYV12ImagePlane(planeOut, yv12PlaneWidth, yv12PlaneHeight,
     (M4OSA_UInt32)params->outBufferStride, (M4VIFI_UInt8 *)params->pOutBuffer);

    err = applyRenderingMode(planeIn, planeOut, params->renderingMode);

    if(M4OSA_NULL != finalOutputBuffer) {
        free(finalOutputBuffer);
        finalOutputBuffer= M4OSA_NULL;
    }
    if(M4OSA_NULL != tempOutputBuffer) {
        free(tempOutputBuffer);
        tempOutputBuffer = M4OSA_NULL;
    }
    if(err != M4NO_ERROR) {
        ALOGV("doVideoPostProcessing: applyRenderingMode returned err=%d",err);
        return err;
    }
    return M4NO_ERROR;
}

android::status_t getVideoSizeByResolution(
                      M4VIDEOEDITING_VideoFrameSize resolution,
                      uint32_t *pWidth, uint32_t *pHeight) {

    uint32_t frameWidth, frameHeight;

    if (pWidth == NULL) {
        ALOGE("getVideoFrameSizeByResolution invalid pointer for pWidth");
        return android::BAD_VALUE;
    }
    if (pHeight == NULL) {
        ALOGE("getVideoFrameSizeByResolution invalid pointer for pHeight");
        return android::BAD_VALUE;
    }

    switch (resolution) {
        case M4VIDEOEDITING_kSQCIF:
            frameWidth = 128;
            frameHeight = 96;
            break;

        case M4VIDEOEDITING_kQQVGA:
            frameWidth = 160;
            frameHeight = 120;
            break;

        case M4VIDEOEDITING_kQCIF:
            frameWidth = 176;
            frameHeight = 144;
            break;

        case M4VIDEOEDITING_kQVGA:
            frameWidth = 320;
            frameHeight = 240;
            break;

        case M4VIDEOEDITING_kCIF:
            frameWidth = 352;
            frameHeight = 288;
            break;

        case M4VIDEOEDITING_kVGA:
            frameWidth = 640;
            frameHeight = 480;
            break;

        case M4VIDEOEDITING_kWVGA:
            frameWidth = 800;
            frameHeight = 480;
            break;

        case M4VIDEOEDITING_kNTSC:
            frameWidth = 720;
            frameHeight = 480;
            break;

        case M4VIDEOEDITING_k640_360:
            frameWidth = 640;
            frameHeight = 360;
            break;

        case M4VIDEOEDITING_k854_480:
            frameWidth = 854;
            frameHeight = 480;
            break;

        case M4VIDEOEDITING_k1280_720:
            frameWidth = 1280;
            frameHeight = 720;
            break;

        case M4VIDEOEDITING_k1080_720:
            frameWidth = 1080;
            frameHeight = 720;
            break;

        case M4VIDEOEDITING_k960_720:
            frameWidth = 960;
            frameHeight = 720;
            break;

        case M4VIDEOEDITING_k1920_1080:
            frameWidth = 1920;
            frameHeight = 1080;
            break;

        default:
            ALOGE("Unsupported video resolution %d.", resolution);
            return android::BAD_VALUE;
    }

    *pWidth = frameWidth;
    *pHeight = frameHeight;

    return android::OK;
}

M4VIFI_UInt8 M4VIFI_Rotate90LeftYUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut) {

    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i,j, u_stride;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest;

    /**< Loop on Y,U and V planes */
    for (plane_number = 0; plane_number < 3; plane_number++) {
        /**< Get adresses of first valid pixel in input and output buffer */
        /**< As we have a -90° rotation, first needed pixel is the upper-right one */
        p_buf_src =
            &(pPlaneIn[plane_number].pac_data[pPlaneIn[plane_number].u_topleft]) +
             pPlaneOut[plane_number].u_height - 1 ;
        p_buf_dest =
            &(pPlaneOut[plane_number].pac_data[pPlaneOut[plane_number].u_topleft]);
        u_stride = pPlaneIn[plane_number].u_stride;
        /**< Loop on output rows */
        for (i = 0; i < pPlaneOut[plane_number].u_height; i++) {
            /**< Loop on all output pixels in a row */
            for (j = 0; j < pPlaneOut[plane_number].u_width; j++) {
                *p_buf_dest++= *p_buf_src;
                p_buf_src += u_stride;  /**< Go to the next row */
            }

            /**< Go on next row of the output frame */
            p_buf_dest +=
                pPlaneOut[plane_number].u_stride - pPlaneOut[plane_number].u_width;
            /**< Go to next pixel in the last row of the input frame*/
            p_buf_src -=
                pPlaneIn[plane_number].u_stride * pPlaneOut[plane_number].u_width + 1 ;
        }
    }

    return M4VIFI_OK;
}

M4VIFI_UInt8 M4VIFI_Rotate90RightYUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut) {

    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i,j, u_stride;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest;

    /**< Loop on Y,U and V planes */
    for (plane_number = 0; plane_number < 3; plane_number++) {
        /**< Get adresses of first valid pixel in input and output buffer */
        /**< As we have a +90° rotation, first needed pixel is the left-down one */
        p_buf_src =
            &(pPlaneIn[plane_number].pac_data[pPlaneIn[plane_number].u_topleft]) +
             (pPlaneIn[plane_number].u_stride * (pPlaneOut[plane_number].u_width - 1));
        p_buf_dest =
            &(pPlaneOut[plane_number].pac_data[pPlaneOut[plane_number].u_topleft]);
        u_stride = pPlaneIn[plane_number].u_stride;
        /**< Loop on output rows */
        for (i = 0; i < pPlaneOut[plane_number].u_height; i++) {
            /**< Loop on all output pixels in a row */
            for (j = 0; j < pPlaneOut[plane_number].u_width; j++) {
                *p_buf_dest++= *p_buf_src;
                p_buf_src -= u_stride;  /**< Go to the previous row */
            }

            /**< Go on next row of the output frame */
            p_buf_dest +=
                pPlaneOut[plane_number].u_stride - pPlaneOut[plane_number].u_width;
            /**< Go to next pixel in the last row of the input frame*/
            p_buf_src +=
                pPlaneIn[plane_number].u_stride * pPlaneOut[plane_number].u_width +1 ;
        }
    }

    return M4VIFI_OK;
}

M4VIFI_UInt8 M4VIFI_Rotate180YUV420toYUV420(void* pUserData,
    M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut) {
    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i,j;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest, temp_pix1;

    /**< Loop on Y,U and V planes */
    for (plane_number = 0; plane_number < 3; plane_number++) {
        /**< Get adresses of first valid pixel in input and output buffer */
        p_buf_src =
            &(pPlaneIn[plane_number].pac_data[pPlaneIn[plane_number].u_topleft]);
        p_buf_dest =
            &(pPlaneOut[plane_number].pac_data[pPlaneOut[plane_number].u_topleft]);

        /**< If pPlaneIn = pPlaneOut, the algorithm will be different */
        if (p_buf_src == p_buf_dest) {
            /**< Get Address of last pixel in the last row of the frame */
            p_buf_dest +=
                pPlaneOut[plane_number].u_stride*(pPlaneOut[plane_number].u_height-1) +
                 pPlaneOut[plane_number].u_width - 1;

            /**< We loop (height/2) times on the rows.
             * In case u_height is odd, the row at the middle of the frame
             * has to be processed as must be mirrored */
            for (i = 0; i < ((pPlaneOut[plane_number].u_height)>>1); i++) {
                for (j = 0; j < pPlaneOut[plane_number].u_width; j++) {
                    temp_pix1= *p_buf_dest;
                    *p_buf_dest--= *p_buf_src;
                    *p_buf_src++ = temp_pix1;
                }
                /**< Go on next row in top of frame */
                p_buf_src +=
                    pPlaneOut[plane_number].u_stride - pPlaneOut[plane_number].u_width;
                /**< Go to the last pixel in previous row in bottom of frame*/
                p_buf_dest -=
                    pPlaneOut[plane_number].u_stride - pPlaneOut[plane_number].u_width;
            }

            /**< Mirror middle row in case height is odd */
            if ((pPlaneOut[plane_number].u_height%2)!= 0) {
                p_buf_src =
                    &(pPlaneOut[plane_number].pac_data[pPlaneIn[plane_number].u_topleft]);
                p_buf_src +=
                    pPlaneOut[plane_number].u_stride*(pPlaneOut[plane_number].u_height>>1);
                p_buf_dest =
                    p_buf_src + pPlaneOut[plane_number].u_width;

                /**< We loop u_width/2 times on this row.
                 *  In case u_width is odd, the pixel at the middle of this row
                 * remains unchanged */
                for (j = 0; j < (pPlaneOut[plane_number].u_width>>1); j++) {
                    temp_pix1= *p_buf_dest;
                    *p_buf_dest--= *p_buf_src;
                    *p_buf_src++ = temp_pix1;
                }
            }
        } else {
            /**< Get Address of last pixel in the last row of the output frame */
            p_buf_dest +=
                pPlaneOut[plane_number].u_stride*(pPlaneOut[plane_number].u_height-1) +
                 pPlaneIn[plane_number].u_width - 1;

            /**< Loop on rows */
            for (i = 0; i < pPlaneOut[plane_number].u_height; i++) {
                for (j = 0; j < pPlaneOut[plane_number].u_width; j++) {
                    *p_buf_dest--= *p_buf_src++;
                }

                /**< Go on next row in top of input frame */
                p_buf_src +=
                    pPlaneIn[plane_number].u_stride - pPlaneOut[plane_number].u_width;
                /**< Go to last pixel of previous row in bottom of input frame*/
                p_buf_dest -=
                    pPlaneOut[plane_number].u_stride - pPlaneOut[plane_number].u_width;
            }
        }
    }

    return M4VIFI_OK;
}

M4OSA_ERR applyVideoRotation(M4OSA_Void* pBuffer, M4OSA_UInt32 width,
                             M4OSA_UInt32 height, M4OSA_UInt32 rotation) {

    M4OSA_ERR err = M4NO_ERROR;
    M4VIFI_ImagePlane planeIn[3], planeOut[3];

    if (pBuffer == M4OSA_NULL) {
        ALOGE("applyVideoRotation: NULL input frame");
        return M4ERR_PARAMETER;
    }
    M4OSA_UInt8* outPtr = (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(
     (width*height*1.5), M4VS, (M4OSA_Char*)("rotation out ptr"));
    if (outPtr == M4OSA_NULL) {
        return M4ERR_ALLOC;
    }

    // In plane
    prepareYUV420ImagePlane(planeIn, width,
        height, (M4VIFI_UInt8 *)pBuffer, width, height);

    // Out plane
    if (rotation != 180) {
        prepareYUV420ImagePlane(planeOut, height,
            width, outPtr, height, width);
    }

    switch(rotation) {
        case 90:
            M4VIFI_Rotate90RightYUV420toYUV420(M4OSA_NULL, planeIn, planeOut);
            memcpy(pBuffer, (void *)outPtr, (width*height*1.5));
            break;

        case 180:
            // In plane rotation, so planeOut = planeIn
            M4VIFI_Rotate180YUV420toYUV420(M4OSA_NULL, planeIn, planeIn);
            break;

        case 270:
            M4VIFI_Rotate90LeftYUV420toYUV420(M4OSA_NULL, planeIn, planeOut);
            memcpy(pBuffer, (void *)outPtr, (width*height*1.5));
            break;

        default:
            ALOGE("invalid rotation param %d", (int)rotation);
            err = M4ERR_PARAMETER;
            break;
    }

    free((void *)outPtr);
    return err;

}

