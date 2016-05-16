/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "AudioResamplerSinc"
//#define LOG_NDEBUG 0

#include <malloc.h>
#include <string.h>
#include <stdlib.h>
#include <dlfcn.h>

#include <cutils/compiler.h>
#include <cutils/properties.h>

#include <utils/Log.h>

#include "AudioResamplerSinc.h"



#if defined(__arm__) && !defined(__thumb__)
#define USE_INLINE_ASSEMBLY (true)
#else
#define USE_INLINE_ASSEMBLY (false)
#endif

#if USE_INLINE_ASSEMBLY && defined(__ARM_NEON__)
#define USE_NEON (true)
#else
#define USE_NEON (false)
#endif


namespace android {
// ----------------------------------------------------------------------------


/*
 * These coeficients are computed with the "fir" utility found in
 * tools/resampler_tools
 * cmd-line: fir -l 7 -s 48000 -c 20478
 */
const uint32_t AudioResamplerSinc::mFirCoefsUp[] __attribute__ ((aligned (32))) = {
        0x6d374bc7, 0x111c6ba0, 0xf3240e61, 0x07d14a38, 0xfc509e64, 0x0139cee9, 0xffc8c866, 0xfffcc300,
        0x6d35278a, 0x103e8192, 0xf36b9dfd, 0x07bdfaa5, 0xfc5102d0, 0x013d618d, 0xffc663b9, 0xfffd9592,
        0x6d2ebafe, 0x0f62811a, 0xf3b3d8ac, 0x07a9f399, 0xfc51d9a6, 0x0140bea5, 0xffc41212, 0xfffe631e,
        0x6d24069d, 0x0e8875ad, 0xf3fcb43e, 0x07953976, 0xfc53216f, 0x0143e67c, 0xffc1d373, 0xffff2b9f,
        0x6d150b35, 0x0db06a89, 0xf4462690, 0x077fd0ac, 0xfc54d8ae, 0x0146d965, 0xffbfa7d9, 0xffffef10,
        0x6d01c9e3, 0x0cda6ab5, 0xf4902587, 0x0769bdaf, 0xfc56fdda, 0x014997bb, 0xffbd8f40, 0x0000ad6e,
        0x6cea4418, 0x0c0680fe, 0xf4daa718, 0x07530501, 0xfc598f60, 0x014c21db, 0xffbb89a1, 0x000166b6,
        0x6cce7b97, 0x0b34b7f5, 0xf525a143, 0x073bab28, 0xfc5c8ba5, 0x014e782a, 0xffb996f3, 0x00021ae5,
        0x6cae7272, 0x0a6519f4, 0xf5710a17, 0x0723b4b4, 0xfc5ff105, 0x01509b14, 0xffb7b728, 0x0002c9fd,
        0x6c8a2b0f, 0x0997b116, 0xf5bcd7b1, 0x070b2639, 0xfc63bdd3, 0x01528b08, 0xffb5ea31, 0x000373fb,
        0x6c61a823, 0x08cc873c, 0xf609003f, 0x06f20453, 0xfc67f05a, 0x0154487b, 0xffb42ffc, 0x000418e2,
        0x6c34ecb5, 0x0803a60a, 0xf6557a00, 0x06d853a2, 0xfc6c86dd, 0x0155d3e8, 0xffb28876, 0x0004b8b3,
        0x6c03fc1c, 0x073d16e7, 0xf6a23b44, 0x06be18cd, 0xfc717f97, 0x01572dcf, 0xffb0f388, 0x00055371,
        0x6bced9ff, 0x0678e2fc, 0xf6ef3a6e, 0x06a3587e, 0xfc76d8bc, 0x015856b6, 0xffaf7118, 0x0005e921,
        0x6b958a54, 0x05b71332, 0xf73c6df4, 0x06881761, 0xfc7c9079, 0x01594f25, 0xffae010b, 0x000679c5,
        0x6b581163, 0x04f7b037, 0xf789cc61, 0x066c5a27, 0xfc82a4f4, 0x015a17ab, 0xffaca344, 0x00070564,
        0x6b1673c1, 0x043ac276, 0xf7d74c53, 0x06502583, 0xfc89144d, 0x015ab0db, 0xffab57a1, 0x00078c04,
        0x6ad0b652, 0x0380521c, 0xf824e480, 0x06337e2a, 0xfc8fdc9f, 0x015b1b4e, 0xffaa1e02, 0x00080dab,
        0x6a86de48, 0x02c86715, 0xf8728bb3, 0x061668d2, 0xfc96fbfc, 0x015b579e, 0xffa8f641, 0x00088a62,
        0x6a38f123, 0x0213090c, 0xf8c038d0, 0x05f8ea30, 0xfc9e7074, 0x015b666c, 0xffa7e039, 0x00090230,
        0x69e6f4b1, 0x01603f6e, 0xf90de2d1, 0x05db06fc, 0xfca63810, 0x015b485b, 0xffa6dbc0, 0x0009751e,
        0x6990ef0b, 0x00b01162, 0xf95b80cb, 0x05bcc3ed, 0xfcae50d6, 0x015afe14, 0xffa5e8ad, 0x0009e337,
        0x6936e697, 0x000285d0, 0xf9a909ea, 0x059e25b5, 0xfcb6b8c4, 0x015a8843, 0xffa506d2, 0x000a4c85,
        0x68d8e206, 0xff57a35e, 0xf9f67577, 0x057f310a, 0xfcbf6dd8, 0x0159e796, 0xffa43603, 0x000ab112,
        0x6876e855, 0xfeaf706f, 0xfa43bad2, 0x055fea9d, 0xfcc86e09, 0x01591cc0, 0xffa3760e, 0x000b10ec,
        0x681100c9, 0xfe09f323, 0xfa90d17b, 0x0540571a, 0xfcd1b74c, 0x01582878, 0xffa2c6c2, 0x000b6c1d,
        0x67a732f4, 0xfd673159, 0xfaddb10c, 0x05207b2f, 0xfcdb4793, 0x01570b77, 0xffa227ec, 0x000bc2b3,
        0x673986ac, 0xfcc730aa, 0xfb2a513b, 0x05005b82, 0xfce51ccb, 0x0155c678, 0xffa19957, 0x000c14bb,
        0x66c80413, 0xfc29f670, 0xfb76a9dd, 0x04dffcb6, 0xfcef34e1, 0x01545a3c, 0xffa11acb, 0x000c6244,
        0x6652b392, 0xfb8f87bd, 0xfbc2b2e4, 0x04bf6369, 0xfcf98dbe, 0x0152c783, 0xffa0ac11, 0x000cab5c,
        0x65d99dd5, 0xfaf7e963, 0xfc0e6461, 0x049e9433, 0xfd04254a, 0x01510f13, 0xffa04cf0, 0x000cf012,
        0x655ccbd3, 0xfa631fef, 0xfc59b685, 0x047d93a8, 0xfd0ef969, 0x014f31b2, 0xff9ffd2c, 0x000d3075,
        0x64dc46c3, 0xf9d12fab, 0xfca4a19f, 0x045c6654, 0xfd1a0801, 0x014d3029, 0xff9fbc89, 0x000d6c97,
        0x64581823, 0xf9421c9d, 0xfcef1e20, 0x043b10bd, 0xfd254ef4, 0x014b0b45, 0xff9f8ac9, 0x000da486,
        0x63d049b4, 0xf8b5ea87, 0xfd392498, 0x04199760, 0xfd30cc24, 0x0148c3d2, 0xff9f67ae, 0x000dd854,
        0x6344e578, 0xf82c9ce7, 0xfd82adba, 0x03f7feb4, 0xfd3c7d73, 0x01465a9f, 0xff9f52f7, 0x000e0812,
        0x62b5f5b2, 0xf7a636fa, 0xfdcbb25a, 0x03d64b27, 0xfd4860c2, 0x0143d07f, 0xff9f4c65, 0x000e33d3,
        0x622384e8, 0xf722bbb5, 0xfe142b6e, 0x03b4811d, 0xfd5473f3, 0x01412643, 0xff9f53b4, 0x000e5ba7,
        0x618d9ddc, 0xf6a22dcf, 0xfe5c120f, 0x0392a4f4, 0xfd60b4e7, 0x013e5cc0, 0xff9f68a1, 0x000e7fa1,
        0x60f44b91, 0xf6248fb6, 0xfea35f79, 0x0370bafc, 0xfd6d2180, 0x013b74ca, 0xff9f8ae9, 0x000e9fd5,
        0x60579947, 0xf5a9e398, 0xfeea0d0c, 0x034ec77f, 0xfd79b7a1, 0x01386f3a, 0xff9fba47, 0x000ebc54,
        0x5fb79278, 0xf5322b61, 0xff30144a, 0x032ccebb, 0xfd86752e, 0x01354ce7, 0xff9ff674, 0x000ed533,
        0x5f1442dc, 0xf4bd68b6, 0xff756edc, 0x030ad4e1, 0xfd93580d, 0x01320ea9, 0xffa03f2b, 0x000eea84,
        0x5e6db665, 0xf44b9cfe, 0xffba168d, 0x02e8de19, 0xfda05e23, 0x012eb55a, 0xffa09425, 0x000efc5c,
        0x5dc3f93c, 0xf3dcc959, 0xfffe054e, 0x02c6ee7f, 0xfdad855b, 0x012b41d3, 0xffa0f519, 0x000f0ace,
        0x5d1717c4, 0xf370eea9, 0x00413536, 0x02a50a22, 0xfdbacb9e, 0x0127b4f1, 0xffa161bf, 0x000f15ef,
        0x5c671e96, 0xf3080d8c, 0x0083a081, 0x02833506, 0xfdc82edb, 0x01240f8e, 0xffa1d9cf, 0x000f1dd2,
        0x5bb41a80, 0xf2a2265e, 0x00c54190, 0x02617321, 0xfdd5ad01, 0x01205285, 0xffa25cfe, 0x000f228d,
        0x5afe1886, 0xf23f393b, 0x010612eb, 0x023fc85c, 0xfde34403, 0x011c7eb2, 0xffa2eb04, 0x000f2434,
        0x5a4525df, 0xf1df45fd, 0x01460f41, 0x021e3891, 0xfdf0f1d6, 0x011894f0, 0xffa38395, 0x000f22dc,
        0x59894ff3, 0xf1824c3e, 0x01853165, 0x01fcc78f, 0xfdfeb475, 0x0114961b, 0xffa42668, 0x000f1e99,
        0x58caa45b, 0xf1284b58, 0x01c37452, 0x01db7914, 0xfe0c89db, 0x0110830f, 0xffa4d332, 0x000f1781,
        0x580930e1, 0xf0d14267, 0x0200d32c, 0x01ba50d2, 0xfe1a7009, 0x010c5ca6, 0xffa589a6, 0x000f0da8,
        0x5745037c, 0xf07d3043, 0x023d493c, 0x0199526b, 0xfe286505, 0x010823ba, 0xffa6497c, 0x000f0125,
        0x567e2a51, 0xf02c138a, 0x0278d1f2, 0x01788170, 0xfe3666d5, 0x0103d927, 0xffa71266, 0x000ef20b,
        0x55b4b3af, 0xefddea9a, 0x02b368e6, 0x0157e166, 0xfe447389, 0x00ff7dc4, 0xffa7e41a, 0x000ee070,
        0x54e8ae13, 0xef92b393, 0x02ed09d7, 0x013775bf, 0xfe528931, 0x00fb126b, 0xffa8be4c, 0x000ecc69,
        0x541a281e, 0xef4a6c58, 0x0325b0ad, 0x011741df, 0xfe60a5e5, 0x00f697f3, 0xffa9a0b1, 0x000eb60b,
        0x5349309e, 0xef051290, 0x035d5977, 0x00f7491a, 0xfe6ec7c0, 0x00f20f32, 0xffaa8afe, 0x000e9d6b,
        0x5275d684, 0xeec2a3a3, 0x0394006a, 0x00d78eb3, 0xfe7cece2, 0x00ed78ff, 0xffab7ce7, 0x000e829e,
        0x51a028e8, 0xee831cc3, 0x03c9a1e5, 0x00b815da, 0xfe8b1373, 0x00e8d62d, 0xffac7621, 0x000e65ba,
        0x50c83704, 0xee467ae1, 0x03fe3a6f, 0x0098e1b3, 0xfe99399f, 0x00e4278f, 0xffad7662, 0x000e46d3,
        0x4fee1037, 0xee0cbab9, 0x0431c6b5, 0x0079f54c, 0xfea75d97, 0x00df6df7, 0xffae7d5f, 0x000e25fd,
        0x4f11c3fe, 0xedd5d8ca, 0x0464438c, 0x005b53a4, 0xfeb57d92, 0x00daaa34, 0xffaf8acd, 0x000e034f,
        0x4e3361f7, 0xeda1d15c, 0x0495adf2, 0x003cffa9, 0xfec397cf, 0x00d5dd16, 0xffb09e63, 0x000ddedb,
        0x4d52f9df, 0xed70a07d, 0x04c6030d, 0x001efc35, 0xfed1aa92, 0x00d10769, 0xffb1b7d8, 0x000db8b7,
        0x4c709b8e, 0xed424205, 0x04f54029, 0x00014c12, 0xfedfb425, 0x00cc29f7, 0xffb2d6e1, 0x000d90f6,
        0x4b8c56f8, 0xed16b196, 0x052362ba, 0xffe3f1f7, 0xfeedb2da, 0x00c7458a, 0xffb3fb37, 0x000d67ae,
        0x4aa63c2c, 0xecedea99, 0x0550685d, 0xffc6f08a, 0xfefba508, 0x00c25ae8, 0xffb52490, 0x000d3cf1,
        0x49be5b50, 0xecc7e845, 0x057c4ed4, 0xffaa4a5d, 0xff09890f, 0x00bd6ad7, 0xffb652a7, 0x000d10d5,
        0x48d4c4a2, 0xeca4a59b, 0x05a7140b, 0xff8e01f1, 0xff175d53, 0x00b87619, 0xffb78533, 0x000ce36b,
        0x47e98874, 0xec841d68, 0x05d0b612, 0xff7219b3, 0xff252042, 0x00b37d70, 0xffb8bbed, 0x000cb4c8,
        0x46fcb72d, 0xec664a48, 0x05f93324, 0xff5693fe, 0xff32d04f, 0x00ae8198, 0xffb9f691, 0x000c84ff,
        0x460e6148, 0xec4b26a2, 0x0620899e, 0xff3b731b, 0xff406bf8, 0x00a9834e, 0xffbb34d8, 0x000c5422,
        0x451e9750, 0xec32acb0, 0x0646b808, 0xff20b93e, 0xff4df1be, 0x00a4834c, 0xffbc767f, 0x000c2245,
        0x442d69de, 0xec1cd677, 0x066bbd0d, 0xff066889, 0xff5b602c, 0x009f8249, 0xffbdbb42, 0x000bef79,
        0x433ae99c, 0xec099dcf, 0x068f9781, 0xfeec830d, 0xff68b5d5, 0x009a80f8, 0xffbf02dd, 0x000bbbd2,
        0x4247273f, 0xebf8fc64, 0x06b2465b, 0xfed30ac5, 0xff75f153, 0x0095800c, 0xffc04d0f, 0x000b8760,
        0x41523389, 0xebeaebaf, 0x06d3c8bb, 0xfeba0199, 0xff831148, 0x00908034, 0xffc19996, 0x000b5235,
        0x405c1f43, 0xebdf6500, 0x06f41de3, 0xfea16960, 0xff90145e, 0x008b821b, 0xffc2e832, 0x000b1c64,
        0x3f64fb40, 0xebd6617b, 0x0713453d, 0xfe8943dc, 0xff9cf947, 0x0086866b, 0xffc438a3, 0x000ae5fc,
        0x3e6cd85b, 0xebcfda19, 0x07313e56, 0xfe7192bd, 0xffa9bebe, 0x00818dcb, 0xffc58aaa, 0x000aaf0f,
        0x3d73c772, 0xebcbc7a7, 0x074e08e0, 0xfe5a579d, 0xffb66386, 0x007c98de, 0xffc6de09, 0x000a77ac,
        0x3c79d968, 0xebca22cc, 0x0769a4b2, 0xfe439407, 0xffc2e669, 0x0077a845, 0xffc83285, 0x000a3fe5,
        0x3b7f1f23, 0xebcae405, 0x078411c7, 0xfe2d496f, 0xffcf463a, 0x0072bc9d, 0xffc987e0, 0x000a07c9,
        0x3a83a989, 0xebce03aa, 0x079d503b, 0xfe177937, 0xffdb81d6, 0x006dd680, 0xffcadde1, 0x0009cf67,
        0x3987897f, 0xebd379eb, 0x07b56051, 0xfe0224b0, 0xffe79820, 0x0068f687, 0xffcc344c, 0x000996ce,
        0x388acfe9, 0xebdb3ed5, 0x07cc426c, 0xfded4d13, 0xfff38806, 0x00641d44, 0xffcd8aeb, 0x00095e0e,
        0x378d8da8, 0xebe54a4f, 0x07e1f712, 0xfdd8f38b, 0xffff507b, 0x005f4b4a, 0xffcee183, 0x00092535,
        0x368fd397, 0xebf1941f, 0x07f67eec, 0xfdc5192d, 0x000af07f, 0x005a8125, 0xffd037e0, 0x0008ec50,
        0x3591b28b, 0xec0013e8, 0x0809dac3, 0xfdb1befc, 0x00166718, 0x0055bf60, 0xffd18dcc, 0x0008b36e,
        0x34933b50, 0xec10c12c, 0x081c0b84, 0xfd9ee5e7, 0x0021b355, 0x00510682, 0xffd2e311, 0x00087a9c,
        0x33947eab, 0xec23934f, 0x082d1239, 0xfd8c8ecc, 0x002cd44d, 0x004c570f, 0xffd4377d, 0x000841e8,
        0x32958d55, 0xec388194, 0x083cf010, 0xfd7aba74, 0x0037c922, 0x0047b186, 0xffd58ade, 0x0008095d,
        0x319677fa, 0xec4f8322, 0x084ba654, 0xfd696998, 0x004290fc, 0x00431666, 0xffd6dd02, 0x0007d108,
        0x30974f3b, 0xec688f02, 0x08593671, 0xfd589cdc, 0x004d2b0e, 0x003e8628, 0xffd82dba, 0x000798f5,
        0x2f9823a8, 0xec839c22, 0x0865a1f1, 0xfd4854d3, 0x00579691, 0x003a0141, 0xffd97cd6, 0x00076130,
        0x2e9905c1, 0xeca0a156, 0x0870ea7e, 0xfd3891fd, 0x0061d2ca, 0x00358824, 0xffdaca2a, 0x000729c4,
        0x2d9a05f4, 0xecbf9558, 0x087b11de, 0xfd2954c8, 0x006bdf05, 0x00311b41, 0xffdc1588, 0x0006f2bb,
        0x2c9b349e, 0xece06ecb, 0x088419f6, 0xfd1a9d91, 0x0075ba95, 0x002cbb03, 0xffdd5ec6, 0x0006bc21,
        0x2b9ca203, 0xed032439, 0x088c04c8, 0xfd0c6ca2, 0x007f64da, 0x002867d2, 0xffdea5bb, 0x000685ff,
        0x2a9e5e57, 0xed27ac16, 0x0892d470, 0xfcfec233, 0x0088dd38, 0x00242213, 0xffdfea3c, 0x0006505f,
        0x29a079b2, 0xed4dfcc2, 0x08988b2a, 0xfcf19e6b, 0x0092231e, 0x001fea27, 0xffe12c22, 0x00061b4b,
        0x28a30416, 0xed760c88, 0x089d2b4a, 0xfce50161, 0x009b3605, 0x001bc06b, 0xffe26b48, 0x0005e6cb,
        0x27a60d6a, 0xed9fd1a2, 0x08a0b740, 0xfcd8eb17, 0x00a4156b, 0x0017a53b, 0xffe3a788, 0x0005b2e8,
        0x26a9a57b, 0xedcb4237, 0x08a33196, 0xfccd5b82, 0x00acc0da, 0x001398ec, 0xffe4e0bf, 0x00057faa,
        0x25addbf9, 0xedf8545b, 0x08a49cf0, 0xfcc25285, 0x00b537e1, 0x000f9bd2, 0xffe616c8, 0x00054d1a,
        0x24b2c075, 0xee26fe17, 0x08a4fc0d, 0xfcb7cff0, 0x00bd7a1c, 0x000bae3c, 0xffe74984, 0x00051b3e,
        0x23b86263, 0xee573562, 0x08a451c0, 0xfcadd386, 0x00c5872a, 0x0007d075, 0xffe878d3, 0x0004ea1d,
        0x22bed116, 0xee88f026, 0x08a2a0f8, 0xfca45cf7, 0x00cd5eb7, 0x000402c8, 0xffe9a494, 0x0004b9c0,
        0x21c61bc0, 0xeebc2444, 0x089fecbb, 0xfc9b6be5, 0x00d50075, 0x00004579, 0xffeaccaa, 0x00048a2b,
        0x20ce516f, 0xeef0c78d, 0x089c3824, 0xfc92ffe1, 0x00dc6c1e, 0xfffc98c9, 0xffebf0fa, 0x00045b65,
        0x1fd7810f, 0xef26cfca, 0x08978666, 0xfc8b186d, 0x00e3a175, 0xfff8fcf7, 0xffed1166, 0x00042d74,
        0x1ee1b965, 0xef5e32bd, 0x0891dac8, 0xfc83b4fc, 0x00eaa045, 0xfff5723d, 0xffee2dd7, 0x0004005e,
        0x1ded0911, 0xef96e61c, 0x088b38a9, 0xfc7cd4f0, 0x00f16861, 0xfff1f8d2, 0xffef4632, 0x0003d426,
        0x1cf97e8b, 0xefd0df9a, 0x0883a378, 0xfc76779e, 0x00f7f9a3, 0xffee90eb, 0xfff05a60, 0x0003a8d2,
        0x1c072823, 0xf00c14e1, 0x087b1ebc, 0xfc709c4d, 0x00fe53ef, 0xffeb3ab8, 0xfff16a4a, 0x00037e65,
        0x1b1613ff, 0xf0487b98, 0x0871ae0d, 0xfc6b4233, 0x0104772e, 0xffe7f666, 0xfff275db, 0x000354e5,
        0x1a26501b, 0xf0860962, 0x08675516, 0xfc66687a, 0x010a6353, 0xffe4c41e, 0xfff37d00, 0x00032c54,
        0x1937ea47, 0xf0c4b3e0, 0x085c1794, 0xfc620e3d, 0x01101858, 0xffe1a408, 0xfff47fa5, 0x000304b7,
        0x184af025, 0xf10470b0, 0x084ff957, 0xfc5e328c, 0x0115963d, 0xffde9646, 0xfff57db8, 0x0002de0e,
        0x175f6f2b, 0xf1453571, 0x0842fe3d, 0xfc5ad465, 0x011add0b, 0xffdb9af8, 0xfff67729, 0x0002b85f,
        0x1675749e, 0xf186f7c0, 0x08352a35, 0xfc57f2be, 0x011fecd3, 0xffd8b23b, 0xfff76be9, 0x000293aa,
        0x158d0d95, 0xf1c9ad40, 0x0826813e, 0xfc558c7c, 0x0124c5ab, 0xffd5dc28, 0xfff85be8, 0x00026ff2,
        0x14a646f6, 0xf20d4b92, 0x08170767, 0xfc53a07b, 0x012967b1, 0xffd318d6, 0xfff9471b, 0x00024d39,
        0x13c12d73, 0xf251c85d, 0x0806c0cb, 0xfc522d88, 0x012dd30a, 0xffd06858, 0xfffa2d74, 0x00022b7f,
        0x12ddcd8f, 0xf297194d, 0x07f5b193, 0xfc513266, 0x013207e4, 0xffcdcabe, 0xfffb0ee9, 0x00020ac7,
        0x11fc3395, 0xf2dd3411, 0x07e3ddf7, 0xfc50adcc, 0x01360670, 0xffcb4014, 0xfffbeb70, 0x0001eb10,
        0x111c6ba0, 0xf3240e61, 0x07d14a38, 0xfc509e64, 0x0139cee9, 0xffc8c866, 0xfffcc300, 0x0001cc5c,
};

/*
 * These coefficients are optimized for 48KHz -> 44.1KHz
 * cmd-line: fir -l 7 -s 48000 -c 17189
 */
const uint32_t AudioResamplerSinc::mFirCoefsDown[] __attribute__ ((aligned (32))) = {
        0x5bacb6f4, 0x1ded1a1d, 0xf0398d56, 0x0394f674, 0x0193a5f9, 0xfe66dbeb, 0x00791043, 0xfffe6631,
        0x5bab6c81, 0x1d3ddccd, 0xf0421d2c, 0x03af9995, 0x01818dc9, 0xfe6bb63e, 0x0079812a, 0xfffdc37d,
        0x5ba78d37, 0x1c8f2cf9, 0xf04beb1d, 0x03c9a04a, 0x016f8aca, 0xfe70a511, 0x0079e34d, 0xfffd2545,
        0x5ba1194f, 0x1be11231, 0xf056f2c7, 0x03e309fe, 0x015d9e64, 0xfe75a79f, 0x007a36e2, 0xfffc8b86,
        0x5b981122, 0x1b3393f8, 0xf0632fb7, 0x03fbd625, 0x014bc9fa, 0xfe7abd23, 0x007a7c20, 0xfffbf639,
        0x5b8c7530, 0x1a86b9bf, 0xf0709d74, 0x04140449, 0x013a0ee9, 0xfe7fe4db, 0x007ab33d, 0xfffb655b,
        0x5b7e461a, 0x19da8ae5, 0xf07f3776, 0x042b93fd, 0x01286e86, 0xfe851e05, 0x007adc72, 0xfffad8e4,
        0x5b6d84a8, 0x192f0eb7, 0xf08ef92d, 0x044284e6, 0x0116ea22, 0xfe8a67dd, 0x007af7f6, 0xfffa50ce,
        0x5b5a31c6, 0x18844c70, 0xf09fddfe, 0x0458d6b7, 0x01058306, 0xfe8fc1a5, 0x007b0603, 0xfff9cd12,
        0x5b444e81, 0x17da4b37, 0xf0b1e143, 0x046e8933, 0x00f43a74, 0xfe952a9b, 0x007b06d4, 0xfff94da9,
        0x5b2bdc0e, 0x17311222, 0xf0c4fe50, 0x04839c29, 0x00e311a9, 0xfe9aa201, 0x007afaa1, 0xfff8d28c,
        0x5b10dbc2, 0x1688a832, 0xf0d9306d, 0x04980f79, 0x00d209db, 0xfea02719, 0x007ae1a7, 0xfff85bb1,
        0x5af34f18, 0x15e11453, 0xf0ee72db, 0x04abe310, 0x00c12439, 0xfea5b926, 0x007abc20, 0xfff7e910,
        0x5ad337af, 0x153a5d5e, 0xf104c0d2, 0x04bf16e9, 0x00b061eb, 0xfeab576d, 0x007a8a49, 0xfff77a9f,
        0x5ab09748, 0x14948a16, 0xf11c1583, 0x04d1ab0d, 0x009fc413, 0xfeb10134, 0x007a4c5d, 0xfff71057,
        0x5a8b6fc7, 0x13efa12c, 0xf1346c17, 0x04e39f93, 0x008f4bcb, 0xfeb6b5c0, 0x007a029a, 0xfff6aa2b,
        0x5a63c336, 0x134ba937, 0xf14dbfb1, 0x04f4f4a2, 0x007efa29, 0xfebc745c, 0x0079ad3d, 0xfff64812,
        0x5a3993c0, 0x12a8a8bb, 0xf1680b6e, 0x0505aa6a, 0x006ed038, 0xfec23c50, 0x00794c82, 0xfff5ea02,
        0x5a0ce3b2, 0x1206a625, 0xf1834a63, 0x0515c12d, 0x005ecf01, 0xfec80ce8, 0x0078e0a9, 0xfff58ff0,
        0x59ddb57f, 0x1165a7cc, 0xf19f77a0, 0x05253938, 0x004ef782, 0xfecde571, 0x007869ee, 0xfff539cf,
        0x59ac0bba, 0x10c5b3ef, 0xf1bc8e31, 0x053412e4, 0x003f4ab4, 0xfed3c538, 0x0077e891, 0xfff4e794,
        0x5977e919, 0x1026d0b8, 0xf1da891b, 0x05424e9b, 0x002fc98a, 0xfed9ab8f, 0x00775ccf, 0xfff49934,
        0x59415075, 0x0f890437, 0xf1f96360, 0x054feccf, 0x002074ed, 0xfedf97c6, 0x0076c6e8, 0xfff44ea3,
        0x590844c9, 0x0eec5465, 0xf21917ff, 0x055cee03, 0x00114dc3, 0xfee58932, 0x00762719, 0xfff407d2,
        0x58ccc930, 0x0e50c723, 0xf239a1ef, 0x056952c3, 0x000254e8, 0xfeeb7f27, 0x00757da3, 0xfff3c4b7,
        0x588ee0ea, 0x0db6623b, 0xf25afc29, 0x05751baa, 0xfff38b32, 0xfef178fc, 0x0074cac4, 0xfff38542,
        0x584e8f56, 0x0d1d2b5d, 0xf27d219f, 0x0580495c, 0xffe4f171, 0xfef7760c, 0x00740ebb, 0xfff34968,
        0x580bd7f4, 0x0c85281f, 0xf2a00d43, 0x058adc8d, 0xffd6886d, 0xfefd75af, 0x007349c7, 0xfff3111b,
        0x57c6be67, 0x0bee5dff, 0xf2c3ba04, 0x0594d5fa, 0xffc850e6, 0xff037744, 0x00727c27, 0xfff2dc4c,
        0x577f4670, 0x0b58d262, 0xf2e822ce, 0x059e366c, 0xffba4b98, 0xff097a29, 0x0071a61b, 0xfff2aaef,
        0x573573f2, 0x0ac48a92, 0xf30d428e, 0x05a6feb9, 0xffac7936, 0xff0f7dbf, 0x0070c7e1, 0xfff27cf3,
        0x56e94af1, 0x0a318bc1, 0xf333142f, 0x05af2fbf, 0xff9eda6d, 0xff15816a, 0x006fe1b8, 0xfff2524c,
        0x569acf90, 0x099fdb04, 0xf359929a, 0x05b6ca6b, 0xff916fe1, 0xff1b848e, 0x006ef3df, 0xfff22aea,
        0x564a0610, 0x090f7d57, 0xf380b8ba, 0x05bdcfb2, 0xff843a32, 0xff218692, 0x006dfe94, 0xfff206bf,
        0x55f6f2d3, 0x0880779d, 0xf3a88179, 0x05c44095, 0xff7739f7, 0xff2786e1, 0x006d0217, 0xfff1e5bb,
        0x55a19a5c, 0x07f2ce9b, 0xf3d0e7c2, 0x05ca1e1f, 0xff6a6fc1, 0xff2d84e5, 0x006bfea4, 0xfff1c7d0,
        0x554a0148, 0x076686fc, 0xf3f9e680, 0x05cf6965, 0xff5ddc1a, 0xff33800e, 0x006af47b, 0xfff1acef,
        0x54f02c56, 0x06dba551, 0xf42378a0, 0x05d42387, 0xff517f86, 0xff3977cb, 0x0069e3d9, 0xfff19508,
        0x54942061, 0x06522e0f, 0xf44d9912, 0x05d84daf, 0xff455a80, 0xff3f6b8f, 0x0068ccfa, 0xfff1800b,
        0x5435e263, 0x05ca258f, 0xf47842c5, 0x05dbe90f, 0xff396d7f, 0xff455acf, 0x0067b01e, 0xfff16de9,
        0x53d57774, 0x0543900d, 0xf4a370ad, 0x05def6e4, 0xff2db8f2, 0xff4b4503, 0x00668d80, 0xfff15e93,
        0x5372e4c6, 0x04be71ab, 0xf4cf1dbf, 0x05e17873, 0xff223d40, 0xff5129a3, 0x0065655d, 0xfff151f9,
        0x530e2fac, 0x043ace6e, 0xf4fb44f4, 0x05e36f0d, 0xff16faca, 0xff57082e, 0x006437f1, 0xfff1480b,
        0x52a75d90, 0x03b8aa40, 0xf527e149, 0x05e4dc08, 0xff0bf1ed, 0xff5ce021, 0x00630577, 0xfff140b9,
        0x523e73fd, 0x033808eb, 0xf554edbd, 0x05e5c0c6, 0xff0122fc, 0xff62b0fd, 0x0061ce2c, 0xfff13bf3,
        0x51d37897, 0x02b8ee22, 0xf5826555, 0x05e61eae, 0xfef68e45, 0xff687a47, 0x00609249, 0xfff139aa,
        0x5166711c, 0x023b5d76, 0xf5b0431a, 0x05e5f733, 0xfeec340f, 0xff6e3b84, 0x005f520a, 0xfff139cd,
        0x50f76368, 0x01bf5a5e, 0xf5de8218, 0x05e54bcd, 0xfee2149b, 0xff73f43d, 0x005e0da8, 0xfff13c4c,
        0x5086556f, 0x0144e834, 0xf60d1d63, 0x05e41dfe, 0xfed83023, 0xff79a3fe, 0x005cc55c, 0xfff14119,
        0x50134d3e, 0x00cc0a36, 0xf63c1012, 0x05e26f4e, 0xfece86db, 0xff7f4a54, 0x005b7961, 0xfff14821,
        0x4f9e50ff, 0x0054c382, 0xf66b5544, 0x05e0414d, 0xfec518f1, 0xff84e6d0, 0x005a29ed, 0xfff15156,
        0x4f2766f2, 0xffdf171b, 0xf69ae81d, 0x05dd9593, 0xfebbe68c, 0xff8a7905, 0x0058d738, 0xfff15ca8,
        0x4eae9571, 0xff6b07e7, 0xf6cac3c7, 0x05da6dbe, 0xfeb2efcd, 0xff900089, 0x0057817b, 0xfff16a07,
        0x4e33e2ee, 0xfef898ae, 0xf6fae373, 0x05d6cb72, 0xfeaa34d0, 0xff957cf4, 0x005628ec, 0xfff17962,
        0x4db755f3, 0xfe87cc1b, 0xf72b425b, 0x05d2b05c, 0xfea1b5a9, 0xff9aede0, 0x0054cdc0, 0xfff18aab,
        0x4d38f520, 0xfe18a4bc, 0xf75bdbbd, 0x05ce1e2d, 0xfe997268, 0xffa052ec, 0x0053702d, 0xfff19dd1,
        0x4cb8c72e, 0xfdab2501, 0xf78caae0, 0x05c9169d, 0xfe916b15, 0xffa5abb8, 0x00521068, 0xfff1b2c5,
        0x4c36d2eb, 0xfd3f4f3d, 0xf7bdab16, 0x05c39b6a, 0xfe899fb2, 0xffaaf7e6, 0x0050aea5, 0xfff1c976,
        0x4bb31f3c, 0xfcd525a5, 0xf7eed7b4, 0x05bdae57, 0xfe82103f, 0xffb0371c, 0x004f4b17, 0xfff1e1d6,
        0x4b2db31a, 0xfc6caa53, 0xf8202c1c, 0x05b7512e, 0xfe7abcb1, 0xffb56902, 0x004de5f1, 0xfff1fbd5,
        0x4aa69594, 0xfc05df40, 0xf851a3b6, 0x05b085bc, 0xfe73a4fb, 0xffba8d44, 0x004c7f66, 0xfff21764,
        0x4a1dcdce, 0xfba0c64b, 0xf88339f5, 0x05a94dd5, 0xfe6cc909, 0xffbfa38d, 0x004b17a6, 0xfff23473,
        0x499362ff, 0xfb3d6133, 0xf8b4ea55, 0x05a1ab52, 0xfe6628c1, 0xffc4ab8f, 0x0049aee3, 0xfff252f3,
        0x49075c72, 0xfadbb19a, 0xf8e6b059, 0x0599a00e, 0xfe5fc405, 0xffc9a4fc, 0x0048454b, 0xfff272d6,
        0x4879c185, 0xfa7bb908, 0xf9188793, 0x05912dea, 0xfe599aaf, 0xffce8f8a, 0x0046db0f, 0xfff2940b,
        0x47ea99a9, 0xfa1d78e3, 0xf94a6b9b, 0x058856cd, 0xfe53ac97, 0xffd36af1, 0x0045705c, 0xfff2b686,
        0x4759ec60, 0xf9c0f276, 0xf97c5815, 0x057f1c9e, 0xfe4df98e, 0xffd836eb, 0x00440561, 0xfff2da36,
        0x46c7c140, 0xf96626f0, 0xf9ae48af, 0x0575814c, 0xfe48815e, 0xffdcf336, 0x00429a4a, 0xfff2ff0d,
        0x46341fed, 0xf90d1761, 0xf9e03924, 0x056b86c6, 0xfe4343d0, 0xffe19f91, 0x00412f43, 0xfff324fd,
        0x459f101d, 0xf8b5c4be, 0xfa122537, 0x05612f00, 0xfe3e40a6, 0xffe63bc0, 0x003fc478, 0xfff34bf9,
        0x45089996, 0xf8602fdc, 0xfa4408ba, 0x05567bf1, 0xfe39779a, 0xffeac787, 0x003e5a12, 0xfff373f0,
        0x4470c42d, 0xf80c5977, 0xfa75df87, 0x054b6f92, 0xfe34e867, 0xffef42af, 0x003cf03d, 0xfff39cd7,
        0x43d797c7, 0xf7ba422b, 0xfaa7a586, 0x05400be1, 0xfe3092bf, 0xfff3ad01, 0x003b871f, 0xfff3c69f,
        0x433d1c56, 0xf769ea78, 0xfad956ab, 0x053452dc, 0xfe2c7650, 0xfff8064b, 0x003a1ee3, 0xfff3f13a,
        0x42a159dc, 0xf71b52c4, 0xfb0aeef6, 0x05284685, 0xfe2892c5, 0xfffc4e5c, 0x0038b7ae, 0xfff41c9c,
        0x42045865, 0xf6ce7b57, 0xfb3c6a73, 0x051be8dd, 0xfe24e7c3, 0x00008507, 0x003751a7, 0xfff448b7,
        0x4166200e, 0xf683645a, 0xfb6dc53c, 0x050f3bec, 0xfe2174ec, 0x0004aa1f, 0x0035ecf4, 0xfff4757e,
        0x40c6b8fd, 0xf63a0ddf, 0xfb9efb77, 0x050241b6, 0xfe1e39da, 0x0008bd7c, 0x003489b9, 0xfff4a2e5,
        0x40262b65, 0xf5f277d9, 0xfbd00956, 0x04f4fc46, 0xfe1b3628, 0x000cbef7, 0x0033281a, 0xfff4d0de,
        0x3f847f83, 0xf5aca21f, 0xfc00eb1b, 0x04e76da3, 0xfe18696a, 0x0010ae6e, 0x0031c83a, 0xfff4ff5d,
        0x3ee1bda2, 0xf5688c6d, 0xfc319d13, 0x04d997d8, 0xfe15d32f, 0x00148bbd, 0x00306a3b, 0xfff52e57,
        0x3e3dee13, 0xf5263665, 0xfc621b9a, 0x04cb7cf2, 0xfe137304, 0x001856c7, 0x002f0e3f, 0xfff55dbf,
        0x3d991932, 0xf4e59f8a, 0xfc926319, 0x04bd1efb, 0xfe114872, 0x001c0f6e, 0x002db466, 0xfff58d89,
        0x3cf34766, 0xf4a6c748, 0xfcc27008, 0x04ae8000, 0xfe0f52fc, 0x001fb599, 0x002c5cd0, 0xfff5bdaa,
        0x3c4c811c, 0xf469aced, 0xfcf23eec, 0x049fa20f, 0xfe0d9224, 0x0023492f, 0x002b079a, 0xfff5ee17,
        0x3ba4cec9, 0xf42e4faf, 0xfd21cc59, 0x04908733, 0xfe0c0567, 0x0026ca1c, 0x0029b4e4, 0xfff61ec5,
        0x3afc38eb, 0xf3f4aea6, 0xfd5114f0, 0x0481317a, 0xfe0aac3f, 0x002a384c, 0x002864c9, 0xfff64fa8,
        0x3a52c805, 0xf3bcc8d3, 0xfd801564, 0x0471a2ef, 0xfe098622, 0x002d93ae, 0x00271766, 0xfff680b5,
        0x39a884a1, 0xf3869d1a, 0xfdaeca73, 0x0461dda0, 0xfe089283, 0x0030dc34, 0x0025ccd7, 0xfff6b1e4,
        0x38fd774e, 0xf3522a49, 0xfddd30eb, 0x0451e396, 0xfe07d0d3, 0x003411d2, 0x00248535, 0xfff6e329,
        0x3851a8a2, 0xf31f6f0f, 0xfe0b45aa, 0x0441b6dd, 0xfe07407d, 0x0037347d, 0x0023409a, 0xfff7147a,
        0x37a52135, 0xf2ee6a07, 0xfe39059b, 0x0431597d, 0xfe06e0eb, 0x003a442e, 0x0021ff1f, 0xfff745cd,
        0x36f7e9a4, 0xf2bf19ae, 0xfe666dbc, 0x0420cd80, 0xfe06b184, 0x003d40e0, 0x0020c0dc, 0xfff7771a,
        0x364a0a90, 0xf2917c6d, 0xfe937b15, 0x041014eb, 0xfe06b1ac, 0x00402a8e, 0x001f85e6, 0xfff7a857,
        0x359b8c9d, 0xf265908f, 0xfec02ac2, 0x03ff31c3, 0xfe06e0c4, 0x00430137, 0x001e4e56, 0xfff7d97a,
        0x34ec786f, 0xf23b544b, 0xfeec79ec, 0x03ee260d, 0xfe073e2a, 0x0045c4dd, 0x001d1a3f, 0xfff80a7c,
        0x343cd6af, 0xf212c5be, 0xff1865cd, 0x03dcf3ca, 0xfe07c93a, 0x00487582, 0x001be9b7, 0xfff83b52,
        0x338cb004, 0xf1ebe2ec, 0xff43ebac, 0x03cb9cf9, 0xfe08814e, 0x004b132b, 0x001abcd0, 0xfff86bf6,
        0x32dc0d17, 0xf1c6a9c3, 0xff6f08e4, 0x03ba2398, 0xfe0965bc, 0x004d9dde, 0x0019939d, 0xfff89c60,
        0x322af693, 0xf1a3181a, 0xff99badb, 0x03a889a1, 0xfe0a75da, 0x005015a5, 0x00186e31, 0xfff8cc86,
        0x3179751f, 0xf1812bb0, 0xffc3ff0c, 0x0396d10c, 0xfe0bb0f9, 0x00527a8a, 0x00174c9c, 0xfff8fc62,
        0x30c79163, 0xf160e22d, 0xffedd2fd, 0x0384fbd1, 0xfe0d166b, 0x0054cc9a, 0x00162eef, 0xfff92bec,
        0x30155404, 0xf1423924, 0x00173447, 0x03730be0, 0xfe0ea57e, 0x00570be4, 0x00151538, 0xfff95b1e,
        0x2f62c5a7, 0xf1252e0f, 0x00402092, 0x0361032a, 0xfe105d7e, 0x00593877, 0x0013ff88, 0xfff989ef,
        0x2eafeeed, 0xf109be56, 0x00689598, 0x034ee39b, 0xfe123db6, 0x005b5267, 0x0012edea, 0xfff9b85b,
        0x2dfcd873, 0xf0efe748, 0x0090911f, 0x033caf1d, 0xfe144570, 0x005d59c6, 0x0011e06d, 0xfff9e65a,
        0x2d498ad3, 0xf0d7a622, 0x00b81102, 0x032a6796, 0xfe1673f2, 0x005f4eac, 0x0010d71d, 0xfffa13e5,
        0x2c960ea3, 0xf0c0f808, 0x00df1328, 0x03180ee7, 0xfe18c884, 0x0061312e, 0x000fd205, 0xfffa40f8,
        0x2be26c73, 0xf0abda0e, 0x0105958c, 0x0305a6f0, 0xfe1b4268, 0x00630167, 0x000ed130, 0xfffa6d8d,
        0x2b2eaccf, 0xf0984931, 0x012b9635, 0x02f3318a, 0xfe1de0e2, 0x0064bf71, 0x000dd4a7, 0xfffa999d,
        0x2a7ad83c, 0xf086425a, 0x0151133e, 0x02e0b08d, 0xfe20a335, 0x00666b68, 0x000cdc74, 0xfffac525,
        0x29c6f738, 0xf075c260, 0x01760ad1, 0x02ce25ca, 0xfe2388a1, 0x0068056b, 0x000be89f, 0xfffaf01e,
        0x2913123c, 0xf066c606, 0x019a7b27, 0x02bb9310, 0xfe269065, 0x00698d98, 0x000af931, 0xfffb1a84,
        0x285f31b7, 0xf05949fb, 0x01be628c, 0x02a8fa2a, 0xfe29b9c1, 0x006b0411, 0x000a0e2f, 0xfffb4453,
        0x27ab5e12, 0xf04d4ade, 0x01e1bf58, 0x02965cdb, 0xfe2d03f2, 0x006c68f8, 0x000927a0, 0xfffb6d86,
        0x26f79fab, 0xf042c539, 0x02048ff8, 0x0283bce6, 0xfe306e35, 0x006dbc71, 0x00084589, 0xfffb961a,
        0x2643feda, 0xf039b587, 0x0226d2e6, 0x02711c05, 0xfe33f7c7, 0x006efea0, 0x000767f0, 0xfffbbe09,
        0x259083eb, 0xf032182f, 0x024886ad, 0x025e7bf0, 0xfe379fe3, 0x00702fae, 0x00068ed8, 0xfffbe552,
        0x24dd3721, 0xf02be98a, 0x0269a9e9, 0x024bde5a, 0xfe3b65c4, 0x00714fc0, 0x0005ba46, 0xfffc0bef,
        0x242a20b3, 0xf02725dc, 0x028a3b44, 0x023944ee, 0xfe3f48a5, 0x00725f02, 0x0004ea3a, 0xfffc31df,
        0x237748cf, 0xf023c95d, 0x02aa397b, 0x0226b156, 0xfe4347c0, 0x00735d9c, 0x00041eb9, 0xfffc571e,
        0x22c4b795, 0xf021d031, 0x02c9a359, 0x02142533, 0xfe476250, 0x00744bba, 0x000357c2, 0xfffc7ba9,
        0x2212751a, 0xf0213671, 0x02e877b9, 0x0201a223, 0xfe4b978e, 0x0075298a, 0x00029558, 0xfffc9f7e,
        0x21608968, 0xf021f823, 0x0306b586, 0x01ef29be, 0xfe4fe6b3, 0x0075f739, 0x0001d779, 0xfffcc29a,
        0x20aefc79, 0xf0241140, 0x03245bbc, 0x01dcbd96, 0xfe544efb, 0x0076b4f5, 0x00011e26, 0xfffce4fc,
        0x1ffdd63b, 0xf0277db1, 0x03416966, 0x01ca5f37, 0xfe58cf9d, 0x007762f0, 0x0000695e, 0xfffd06a1,
        0x1f4d1e8e, 0xf02c3953, 0x035ddd9e, 0x01b81028, 0xfe5d67d4, 0x0078015a, 0xffffb91f, 0xfffd2787,
        0x1e9cdd43, 0xf0323ff5, 0x0379b790, 0x01a5d1ea, 0xfe6216db, 0x00789065, 0xffff0d66, 0xfffd47ae,
        0x1ded1a1d, 0xf0398d56, 0x0394f674, 0x0193a5f9, 0xfe66dbeb, 0x00791043, 0xfffe6631, 0xfffd6713,
};

// we use 15 bits to interpolate between these samples
// this cannot change because the mul below rely on it.
static const int pLerpBits = 15;

static pthread_once_t once_control = PTHREAD_ONCE_INIT;
static readCoefficientsFn readResampleCoefficients = NULL;

/*static*/ AudioResamplerSinc::Constants AudioResamplerSinc::highQualityConstants;
/*static*/ AudioResamplerSinc::Constants AudioResamplerSinc::veryHighQualityConstants;

void AudioResamplerSinc::init_routine()
{
    // for high quality resampler, the parameters for coefficients are compile-time constants
    Constants *c = &highQualityConstants;
    c->coefsBits = RESAMPLE_FIR_LERP_INT_BITS;
    c->cShift = kNumPhaseBits - c->coefsBits;
    c->cMask = ((1<< c->coefsBits)-1) << c->cShift;
    c->pShift = kNumPhaseBits - c->coefsBits - pLerpBits;
    c->pMask = ((1<< pLerpBits)-1) << c->pShift;
    c->halfNumCoefs = RESAMPLE_FIR_NUM_COEF;

    // for very high quality resampler, the parameters are load-time constants
    veryHighQualityConstants = highQualityConstants;

    // Open the dll to get the coefficients for VERY_HIGH_QUALITY
    void *resampleCoeffLib = dlopen("libaudio-resampler.so", RTLD_NOW);
    ALOGV("Open libaudio-resampler library = %p", resampleCoeffLib);
    if (resampleCoeffLib == NULL) {
        ALOGE("Could not open audio-resampler library: %s", dlerror());
        return;
    }

    readResampleFirNumCoeffFn readResampleFirNumCoeff;
    readResampleFirLerpIntBitsFn readResampleFirLerpIntBits;

    readResampleCoefficients = (readCoefficientsFn)
            dlsym(resampleCoeffLib, "readResamplerCoefficients");
    readResampleFirNumCoeff = (readResampleFirNumCoeffFn)
            dlsym(resampleCoeffLib, "readResampleFirNumCoeff");
    readResampleFirLerpIntBits = (readResampleFirLerpIntBitsFn)
            dlsym(resampleCoeffLib, "readResampleFirLerpIntBits");

    if (!readResampleCoefficients || !readResampleFirNumCoeff || !readResampleFirLerpIntBits) {
        readResampleCoefficients = NULL;
        dlclose(resampleCoeffLib);
        resampleCoeffLib = NULL;
        ALOGE("Could not find symbol: %s", dlerror());
        return;
    }

    c = &veryHighQualityConstants;
    c->coefsBits = readResampleFirLerpIntBits();
    c->cShift = kNumPhaseBits - c->coefsBits;
    c->cMask = ((1<<c->coefsBits)-1) << c->cShift;
    c->pShift = kNumPhaseBits - c->coefsBits - pLerpBits;
    c->pMask = ((1<<pLerpBits)-1) << c->pShift;
    // number of zero-crossing on each side
    c->halfNumCoefs = readResampleFirNumCoeff();
    ALOGV("coefsBits = %d", c->coefsBits);
    ALOGV("halfNumCoefs = %d", c->halfNumCoefs);
    // note that we "leak" resampleCoeffLib until the process exits
}

// ----------------------------------------------------------------------------

static inline
int32_t mulRL(int left, int32_t in, uint32_t vRL)
{
#if USE_INLINE_ASSEMBLY
    int32_t out;
    if (left) {
        asm( "smultb %[out], %[in], %[vRL] \n"
             : [out]"=r"(out)
             : [in]"%r"(in), [vRL]"r"(vRL)
             : );
    } else {
        asm( "smultt %[out], %[in], %[vRL] \n"
             : [out]"=r"(out)
             : [in]"%r"(in), [vRL]"r"(vRL)
             : );
    }
    return out;
#else
    int16_t v = left ? int16_t(vRL) : int16_t(vRL>>16);
    return int32_t((int64_t(in) * v) >> 16);
#endif
}

static inline
int32_t mulAdd(int16_t in, int32_t v, int32_t a)
{
#if USE_INLINE_ASSEMBLY
    int32_t out;
    asm( "smlawb %[out], %[v], %[in], %[a] \n"
         : [out]"=r"(out)
         : [in]"%r"(in), [v]"r"(v), [a]"r"(a)
         : );
    return out;
#else
    return a + int32_t((int64_t(v) * in) >> 16);
#endif
}

static inline
int32_t mulAddRL(int left, uint32_t inRL, int32_t v, int32_t a)
{
#if USE_INLINE_ASSEMBLY
    int32_t out;
    if (left) {
        asm( "smlawb %[out], %[v], %[inRL], %[a] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [v]"r"(v), [a]"r"(a)
             : );
    } else {
        asm( "smlawt %[out], %[v], %[inRL], %[a] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [v]"r"(v), [a]"r"(a)
             : );
    }
    return out;
#else
    int16_t s = left ? int16_t(inRL) : int16_t(inRL>>16);
    return a + int32_t((int64_t(v) * s) >> 16);
#endif
}

// ----------------------------------------------------------------------------

AudioResamplerSinc::AudioResamplerSinc(int bitDepth,
        int inChannelCount, int32_t sampleRate, src_quality quality)
    : AudioResampler(bitDepth, inChannelCount, sampleRate, quality),
    mState(0), mImpulse(0), mRingFull(0), mFirCoefs(0)
{
    /*
     * Layout of the state buffer for 32 tap:
     *
     * "present" sample            beginning of 2nd buffer
     *                 v                v
     *  0              01               2              23              3
     *  0              F0               0              F0              F
     * [pppppppppppppppInnnnnnnnnnnnnnnnpppppppppppppppInnnnnnnnnnnnnnnn]
     *                 ^               ^ head
     *
     * p = past samples, convoluted with the (p)ositive side of sinc()
     * n = future samples, convoluted with the (n)egative side of sinc()
     * r = extra space for implementing the ring buffer
     *
     */

    mVolumeSIMD[0] = 0;
    mVolumeSIMD[1] = 0;

    // Load the constants for coefficients
    int ok = pthread_once(&once_control, init_routine);
    if (ok != 0) {
        ALOGE("%s pthread_once failed: %d", __func__, ok);
    }
    mConstants = (quality == VERY_HIGH_QUALITY) ?
            &veryHighQualityConstants : &highQualityConstants;
}


AudioResamplerSinc::~AudioResamplerSinc() {
    free(mState);
}

void AudioResamplerSinc::init() {
    const Constants& c(*mConstants);
    const size_t numCoefs = 2 * c.halfNumCoefs;
    const size_t stateSize = numCoefs * mChannelCount * 2;
    mState = (int16_t*)memalign(32, stateSize*sizeof(int16_t));
    memset(mState, 0, sizeof(int16_t)*stateSize);
    mImpulse  = mState   + (c.halfNumCoefs-1)*mChannelCount;
    mRingFull = mImpulse + (numCoefs+1)*mChannelCount;
}

void AudioResamplerSinc::setVolume(int16_t left, int16_t right) {
    AudioResampler::setVolume(left, right);
    mVolumeSIMD[0] = int32_t(left)<<16;
    mVolumeSIMD[1] = int32_t(right)<<16;
}

void AudioResamplerSinc::resample(int32_t* out, size_t outFrameCount,
            AudioBufferProvider* provider)
{
    // FIXME store current state (up or down sample) and only load the coefs when the state
    // changes. Or load two pointers one for up and one for down in the init function.
    // Not critical now since the read functions are fast, but would be important if read was slow.
    if (mConstants == &veryHighQualityConstants && readResampleCoefficients) {
        mFirCoefs = readResampleCoefficients( mInSampleRate <= mSampleRate );
    } else {
        mFirCoefs = (const int32_t *) ((mInSampleRate <= mSampleRate) ? mFirCoefsUp : mFirCoefsDown);
    }

    // select the appropriate resampler
    switch (mChannelCount) {
    case 1:
        resample<1>(out, outFrameCount, provider);
        break;
    case 2:
        resample<2>(out, outFrameCount, provider);
        break;
    }
}


template<int CHANNELS>
void AudioResamplerSinc::resample(int32_t* out, size_t outFrameCount,
        AudioBufferProvider* provider)
{
    const Constants& c(*mConstants);
    const size_t headOffset = c.halfNumCoefs*CHANNELS;
    int16_t* impulse = mImpulse;
    uint32_t vRL = mVolumeRL;
    size_t inputIndex = mInputIndex;
    uint32_t phaseFraction = mPhaseFraction;
    uint32_t phaseIncrement = mPhaseIncrement;
    size_t outputIndex = 0;
    size_t outputSampleCount = outFrameCount * 2;
    size_t inFrameCount = (outFrameCount*mInSampleRate)/mSampleRate;

    while (outputIndex < outputSampleCount) {
        // buffer is empty, fetch a new one
        while (mBuffer.frameCount == 0) {
            mBuffer.frameCount = inFrameCount;
            provider->getNextBuffer(&mBuffer,
                                    calculateOutputPTS(outputIndex / 2));
            if (mBuffer.raw == NULL) {
                goto resample_exit;
            }
            const uint32_t phaseIndex = phaseFraction >> kNumPhaseBits;
            if (phaseIndex == 1) {
                // read one frame
                read<CHANNELS>(impulse, phaseFraction, mBuffer.i16, inputIndex);
            } else if (phaseIndex == 2) {
                // read 2 frames
                read<CHANNELS>(impulse, phaseFraction, mBuffer.i16, inputIndex);
                inputIndex++;
                if (inputIndex >= mBuffer.frameCount) {
                    inputIndex -= mBuffer.frameCount;
                    provider->releaseBuffer(&mBuffer);
                } else {
                    read<CHANNELS>(impulse, phaseFraction, mBuffer.i16, inputIndex);
                }
            }
        }
        int16_t const * const in = mBuffer.i16;
        const size_t frameCount = mBuffer.frameCount;

        // Always read-in the first samples from the input buffer
        int16_t* head = impulse + headOffset;
        for (size_t i=0 ; i<CHANNELS ; i++) {
            head[i] = in[inputIndex*CHANNELS + i];
        }

        // handle boundary case
        while (CC_LIKELY(outputIndex < outputSampleCount)) {
            filterCoefficient<CHANNELS>(&out[outputIndex], phaseFraction, impulse, vRL);
            outputIndex += 2;

            phaseFraction += phaseIncrement;
            const size_t phaseIndex = phaseFraction >> kNumPhaseBits;
            for (size_t i=0 ; i<phaseIndex ; i++) {
                inputIndex++;
                if (inputIndex >= frameCount) {
                    goto done;  // need a new buffer
                }
                read<CHANNELS>(impulse, phaseFraction, in, inputIndex);
            }
        }
done:
        // if done with buffer, save samples
        if (inputIndex >= frameCount) {
            inputIndex -= frameCount;
            provider->releaseBuffer(&mBuffer);
        }
    }

resample_exit:
    mImpulse = impulse;
    mInputIndex = inputIndex;
    mPhaseFraction = phaseFraction;
}

template<int CHANNELS>
/***
* read()
*
* This function reads only one frame from input buffer and writes it in
* state buffer
*
**/
void AudioResamplerSinc::read(
        int16_t*& impulse, uint32_t& phaseFraction,
        const int16_t* in, size_t inputIndex)
{
    impulse += CHANNELS;
    phaseFraction -= 1LU<<kNumPhaseBits;

    const Constants& c(*mConstants);
    if (CC_UNLIKELY(impulse >= mRingFull)) {
        const size_t stateSize = (c.halfNumCoefs*2)*CHANNELS;
        memcpy(mState, mState+stateSize, sizeof(int16_t)*stateSize);
        impulse -= stateSize;
    }

    int16_t* head = impulse + c.halfNumCoefs*CHANNELS;
    for (size_t i=0 ; i<CHANNELS ; i++) {
        head[i] = in[inputIndex*CHANNELS + i];
    }
}

template<int CHANNELS>
void AudioResamplerSinc::filterCoefficient(
        int32_t* out, uint32_t phase, const int16_t *samples, uint32_t vRL)
{
    // NOTE: be very careful when modifying the code here. register
    // pressure is very high and a small change might cause the compiler
    // to generate far less efficient code.
    // Always sanity check the result with objdump or test-resample.

    // compute the index of the coefficient on the positive side and
    // negative side
    const Constants& c(*mConstants);
    const int32_t ONE = c.cMask | c.pMask;
    uint32_t indexP = ( phase & c.cMask) >> c.cShift;
    uint32_t lerpP  = ( phase & c.pMask) >> c.pShift;
    uint32_t indexN = ((ONE-phase) & c.cMask) >> c.cShift;
    uint32_t lerpN  = ((ONE-phase) & c.pMask) >> c.pShift;

    const size_t offset = c.halfNumCoefs;
    indexP *= offset;
    indexN *= offset;

    int32_t const* coefsP = mFirCoefs + indexP;
    int32_t const* coefsN = mFirCoefs + indexN;
    int16_t const* sP = samples;
    int16_t const* sN = samples + CHANNELS;

    size_t count = offset;

    if (!USE_NEON) {
        int32_t l = 0;
        int32_t r = 0;
        for (size_t i=0 ; i<count ; i++) {
            interpolate<CHANNELS>(l, r, coefsP++, offset, lerpP, sP);
            sP -= CHANNELS;
            interpolate<CHANNELS>(l, r, coefsN++, offset, lerpN, sN);
            sN += CHANNELS;
        }
        out[0] += 2 * mulRL(1, l, vRL);
        out[1] += 2 * mulRL(0, r, vRL);
    } else if (CHANNELS == 1) {
        int32_t const* coefsP1 = coefsP + offset;
        int32_t const* coefsN1 = coefsN + offset;
        sP -= CHANNELS*3;
        asm (
            "vmov.32        d2[0], %[lerpP]          \n"    // load the positive phase
            "vmov.32        d2[1], %[lerpN]          \n"    // load the negative phase
            "veor           q0, q0, q0               \n"    // result, initialize to 0
            "vshl.s32       d2, d2, #16              \n"    // convert to 32 bits

            "1:                                      \n"
            "vld1.16        { d4}, [%[sP]]           \n"    // load 4 16-bits stereo samples
            "vld1.32        { q8}, [%[coefsP0]:128]! \n"    // load 4 32-bits coefs
            "vld1.32        { q9}, [%[coefsP1]:128]! \n"    // load 4 32-bits coefs for interpolation
            "vld1.16        { d6}, [%[sN]]!          \n"    // load 4 16-bits stereo samples
            "vld1.32        {q10}, [%[coefsN0]:128]! \n"    // load 4 32-bits coefs
            "vld1.32        {q11}, [%[coefsN1]:128]! \n"    // load 4 32-bits coefs for interpolation

            "vrev64.16      d4, d4                   \n"    // reverse 2 frames of the positive side

            "vsub.s32        q9,  q9,  q8            \n"    // interpolate (step1) 1st set of coefs
            "vsub.s32       q11, q11, q10            \n"    // interpolate (step1) 2nd set of coets
            "vshll.s16      q12,  d4, #15            \n"    // extend samples to 31 bits

            "vqrdmulh.s32    q9,  q9, d2[0]          \n"    // interpolate (step2) 1st set of coefs
            "vqrdmulh.s32   q11, q11, d2[1]          \n"    // interpolate (step3) 2nd set of coefs
            "vshll.s16      q14,  d6, #15            \n"    // extend samples to 31 bits

            "vadd.s32        q8,  q8,  q9            \n"    // interpolate (step3) 1st set
            "vadd.s32       q10, q10, q11            \n"    // interpolate (step4) 2nd set
            "subs           %[count], %[count], #4   \n"    // update loop counter

            "vqrdmulh.s32   q12, q12, q8             \n"    // multiply samples by interpolated coef
            "vqrdmulh.s32   q14, q14, q10            \n"    // multiply samples by interpolated coef
            "sub            %[sP], %[sP], #8         \n"    // move pointer to next set of samples

            "vadd.s32       q0, q0, q12              \n"    // accumulate result
            "vadd.s32       q0, q0, q14              \n"    // accumulate result

            "bne            1b                       \n"    // loop

            "vld1.s32       {d2}, [%[vLR]]           \n"    // load volumes
            "vld1.s32       {d3}, %[out]             \n"    // load the output
            "vpadd.s32      d0, d0, d1               \n"    // add all 4 partial sums
            "vpadd.s32      d0, d0, d0               \n"    // together
            "vdup.i32       d0, d0[0]                \n"    // interleave L,R channels
            "vqrdmulh.s32   d0, d0, d2               \n"    // apply volume
            "vadd.s32       d3, d3, d0               \n"    // accumulate result
            "vst1.s32       {d3}, %[out]             \n"    // store result

            : [out]     "=Uv" (out[0]),
              [count]   "+r" (count),
              [coefsP0] "+r" (coefsP),
              [coefsP1] "+r" (coefsP1),
              [coefsN0] "+r" (coefsN),
              [coefsN1] "+r" (coefsN1),
              [sP]      "+r" (sP),
              [sN]      "+r" (sN)
            : [lerpP]   "r" (lerpP),
              [lerpN]   "r" (lerpN),
              [vLR]     "r" (mVolumeSIMD)
            : "cc", "memory",
              "q0", "q1", "q2", "q3",
              "q8", "q9", "q10", "q11",
              "q12", "q14"
        );
    } else if (CHANNELS == 2) {
        int32_t const* coefsP1 = coefsP + offset;
        int32_t const* coefsN1 = coefsN + offset;
        sP -= CHANNELS*3;
        asm (
            "vmov.32        d2[0], %[lerpP]          \n"    // load the positive phase
            "vmov.32        d2[1], %[lerpN]          \n"    // load the negative phase
            "veor           q0, q0, q0               \n"    // result, initialize to 0
            "veor           q4, q4, q4               \n"    // result, initialize to 0
            "vshl.s32       d2, d2, #16              \n"    // convert to 32 bits

            "1:                                      \n"
            "vld2.16        {d4,d5}, [%[sP]]         \n"    // load 4 16-bits stereo samples
            "vld1.32        { q8}, [%[coefsP0]:128]! \n"    // load 4 32-bits coefs
            "vld1.32        { q9}, [%[coefsP1]:128]! \n"    // load 4 32-bits coefs for interpolation
            "vld2.16        {d6,d7}, [%[sN]]!        \n"    // load 4 16-bits stereo samples
            "vld1.32        {q10}, [%[coefsN0]:128]! \n"    // load 4 32-bits coefs
            "vld1.32        {q11}, [%[coefsN1]:128]! \n"    // load 4 32-bits coefs for interpolation

            "vrev64.16      d4, d4                   \n"    // reverse 2 frames of the positive side
            "vrev64.16      d5, d5                   \n"    // reverse 2 frames of the positive side

            "vsub.s32        q9,  q9,  q8            \n"    // interpolate (step1) 1st set of coefs
            "vsub.s32       q11, q11, q10            \n"    // interpolate (step1) 2nd set of coets
            "vshll.s16      q12,  d4, #15            \n"    // extend samples to 31 bits
            "vshll.s16      q13,  d5, #15            \n"    // extend samples to 31 bits

            "vqrdmulh.s32    q9,  q9, d2[0]          \n"    // interpolate (step2) 1st set of coefs
            "vqrdmulh.s32   q11, q11, d2[1]          \n"    // interpolate (step3) 2nd set of coefs
            "vshll.s16      q14,  d6, #15            \n"    // extend samples to 31 bits
            "vshll.s16      q15,  d7, #15            \n"    // extend samples to 31 bits

            "vadd.s32        q8,  q8,  q9            \n"    // interpolate (step3) 1st set
            "vadd.s32       q10, q10, q11            \n"    // interpolate (step4) 2nd set
            "subs           %[count], %[count], #4   \n"    // update loop counter

            "vqrdmulh.s32   q12, q12, q8             \n"    // multiply samples by interpolated coef
            "vqrdmulh.s32   q13, q13, q8             \n"    // multiply samples by interpolated coef
            "vqrdmulh.s32   q14, q14, q10            \n"    // multiply samples by interpolated coef
            "vqrdmulh.s32   q15, q15, q10            \n"    // multiply samples by interpolated coef
            "sub            %[sP], %[sP], #16        \n"    // move pointer to next set of samples

            "vadd.s32       q0, q0, q12              \n"    // accumulate result
            "vadd.s32       q4, q4, q13              \n"    // accumulate result
            "vadd.s32       q0, q0, q14              \n"    // accumulate result
            "vadd.s32       q4, q4, q15              \n"    // accumulate result

            "bne            1b                       \n"    // loop

            "vld1.s32       {d2}, [%[vLR]]           \n"    // load volumes
            "vld1.s32       {d3}, %[out]             \n"    // load the output
            "vpadd.s32      d0, d0, d1               \n"    // add all 4 partial sums from q0
            "vpadd.s32      d8, d8, d9               \n"    // add all 4 partial sums from q4
            "vpadd.s32      d0, d0, d0               \n"    // together
            "vpadd.s32      d8, d8, d8               \n"    // together
            "vtrn.s32       d0, d8                   \n"    // interlace L,R channels
            "vqrdmulh.s32   d0, d0, d2               \n"    // apply volume
            "vadd.s32       d3, d3, d0               \n"    // accumulate result
            "vst1.s32       {d3}, %[out]             \n"    // store result

            : [out]     "=Uv" (out[0]),
              [count]   "+r" (count),
              [coefsP0] "+r" (coefsP),
              [coefsP1] "+r" (coefsP1),
              [coefsN0] "+r" (coefsN),
              [coefsN1] "+r" (coefsN1),
              [sP]      "+r" (sP),
              [sN]      "+r" (sN)
            : [lerpP]   "r" (lerpP),
              [lerpN]   "r" (lerpN),
              [vLR]     "r" (mVolumeSIMD)
            : "cc", "memory",
              "q0", "q1", "q2", "q3", "q4",
              "q8", "q9", "q10", "q11",
              "q12", "q13", "q14", "q15"
        );
    }
}

template<int CHANNELS>
void AudioResamplerSinc::interpolate(
        int32_t& l, int32_t& r,
        const int32_t* coefs, size_t offset,
        int32_t lerp, const int16_t* samples)
{
    int32_t c0 = coefs[0];
    int32_t c1 = coefs[offset];
    int32_t sinc = mulAdd(lerp, (c1-c0)<<1, c0);
    if (CHANNELS == 2) {
        uint32_t rl = *reinterpret_cast<const uint32_t*>(samples);
        l = mulAddRL(1, rl, sinc, l);
        r = mulAddRL(0, rl, sinc, r);
    } else {
        r = l = mulAdd(samples[0], sinc, l);
    }
}
// ----------------------------------------------------------------------------
}; // namespace android
