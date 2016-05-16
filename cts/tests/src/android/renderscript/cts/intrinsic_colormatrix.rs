/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "shared.rsh"


void reference(rs_matrix4x4 m, float4 add, rs_allocation in, rs_allocation out) {
    uint32_t w = rsAllocationGetDimX(in);
    uint32_t h = rsAllocationGetDimY(in);

    rs_element ein = rsAllocationGetElement(in);
    rs_element eout = rsAllocationGetElement(out);
    rs_data_type dtin = rsElementGetDataType(ein);
    rs_data_type dtout = rsElementGetDataType(eout);
    uint32_t vsin = rsElementGetVectorSize(ein);
    uint32_t vsout = rsElementGetVectorSize(eout);

    for (uint32_t y = 0; y < h; y++) {
        for (uint32_t x = 0; x < w; x++) {
            float4 pin = 0.f;

            if (dtin == RS_TYPE_FLOAT_32) {
                switch(vsin) {
                case 4:
                    pin.xyzw = rsGetElementAt_float4(in, x, y);
                    break;
                case 3:
                    pin.xyz = rsGetElementAt_float3(in, x, y);
                    break;
                case 2:
                    pin.xy = rsGetElementAt_float2(in, x, y);
                    break;
                case 1:
                    pin.x = rsGetElementAt_float(in, x, y);
                    break;
                }
            }

            if (dtin == RS_TYPE_UNSIGNED_8) {
                uchar4 u = 0;
                switch(vsin) {
                case 4:
                    u.xyzw = rsGetElementAt_uchar4(in, x, y);
                    break;
                case 3:
                    u.xyz = rsGetElementAt_uchar3(in, x, y);
                    break;
                case 2:
                    u.xy = rsGetElementAt_uchar2(in, x, y);
                    break;
                case 1:
                    u.x = rsGetElementAt_uchar(in, x, y);
                    break;
                }
                pin = rsUnpackColor8888(u);
            }

            pin = rsMatrixMultiply(&m, pin);
            pin += add;

            if (dtout == RS_TYPE_FLOAT_32) {
                switch(vsout) {
                case 4:
                    rsSetElementAt_float4(out, pin, x, y);
                    break;
                case 3:
                    rsSetElementAt_float3(out, pin.xyz, x, y);
                    break;
                case 2:
                    rsSetElementAt_float2(out, pin.xy, x, y);
                    break;
                case 1:
                    rsSetElementAt_float(out, pin.x, x, y);
                    break;
                }
            }

            if (dtout == RS_TYPE_UNSIGNED_8) {
                uchar4 u = rsPackColorTo8888(pin);
                switch(vsout) {
                case 4:
                    rsSetElementAt_uchar4(out, u, x, y);
                    break;
                case 3:
                    rsSetElementAt_uchar3(out, u.xyz, x, y);
                    break;
                case 2:
                    rsSetElementAt_uchar2(out, u.xy, x, y);
                    break;
                case 1:
                    rsSetElementAt_uchar(out, u.x, x, y);
                    break;
                }
            }

        }
    }
}


