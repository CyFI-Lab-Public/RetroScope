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

#include <math.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

static double sinc(double x) {
    if (fabs(x) == 0.0f) return 1.0f;
    return sin(x) / x;
}

static double sqr(double x) {
    return x*x;
}

static double I0(double x) {
    // from the Numerical Recipes in C p. 237
    double ax,ans,y;
    ax=fabs(x);
    if (ax < 3.75) {
        y=x/3.75;
        y*=y;
        ans=1.0+y*(3.5156229+y*(3.0899424+y*(1.2067492
                +y*(0.2659732+y*(0.360768e-1+y*0.45813e-2)))));
    } else {
        y=3.75/ax;
        ans=(exp(ax)/sqrt(ax))*(0.39894228+y*(0.1328592e-1
                +y*(0.225319e-2+y*(-0.157565e-2+y*(0.916281e-2
                        +y*(-0.2057706e-1+y*(0.2635537e-1+y*(-0.1647633e-1
                                +y*0.392377e-2))))))));
    }
    return ans;
}

static double kaiser(int k, int N, double beta) {
    if (k < 0 || k > N)
        return 0;
    return I0(beta * sqrt(1.0 - sqr((2.0*k)/N - 1.0))) / I0(beta);
}


static void usage(char* name) {
    fprintf(stderr,
            "usage: %s [-h] [-d] [-s sample_rate] [-c cut-off_frequency] [-n half_zero_crossings] [-f {float|fixed}] [-b beta] [-v dBFS] [-l lerp]\n"
            "       %s [-h] [-d] [-s sample_rate] [-c cut-off_frequency] [-n half_zero_crossings] [-f {float|fixed}] [-b beta] [-v dBFS] -p M/N\n"
            "    -h    this help message\n"
            "    -d    debug, print comma-separated coefficient table\n"
            "    -p    generate poly-phase filter coefficients, with sample increment M/N\n"
            "    -s    sample rate (48000)\n"
            "    -c    cut-off frequency (20478)\n"
            "    -n    number of zero-crossings on one side (8)\n"
            "    -l    number of lerping bits (4)\n"
            "    -f    output format, can be fixed-point or floating-point (fixed)\n"
            "    -b    kaiser window parameter beta (7.865 [-80dB])\n"
            "    -v    attenuation in dBFS (0)\n",
            name, name
    );
    exit(0);
}

int main(int argc, char** argv)
{
    // nc is the number of bits to store the coefficients
    const int nc = 32;

    bool polyphase = false;
    unsigned int polyM = 160;
    unsigned int polyN = 147;
    bool debug = false;
    double Fs = 48000;
    double Fc = 20478;
    double atten = 1;
    int format = 0;


    // in order to keep the errors associated with the linear
    // interpolation of the coefficients below the quantization error
    // we must satisfy:
    //   2^nz >= 2^(nc/2)
    //
    // for 16 bit coefficients that would be 256
    //
    // note that increasing nz only increases memory requirements,
    // but doesn't increase the amount of computation to do.
    //
    //
    // see:
    // Smith, J.O. Digital Audio Resampling Home Page
    // https://ccrma.stanford.edu/~jos/resample/, 2011-03-29
    //
    int nz = 4;

    //         | 0.1102*(A - 8.7)                         A > 50
    //  beta = | 0.5842*(A - 21)^0.4 + 0.07886*(A - 21)   21 <= A <= 50
    //         | 0                                        A < 21
    //   with A is the desired stop-band attenuation in dBFS
    //
    // for eg:
    //
    //    30 dB    2.210
    //    40 dB    3.384
    //    50 dB    4.538
    //    60 dB    5.658
    //    70 dB    6.764
    //    80 dB    7.865
    //    90 dB    8.960
    //   100 dB   10.056
    double beta = 7.865;


    // 2*nzc = (A - 8) / (2.285 * dw)
    //      with dw the transition width = 2*pi*dF/Fs
    //
    int nzc = 8;

    //
    // Example:
    // 44.1 KHz to 48 KHz resampling
    // 100 dB rejection above 28 KHz
    //   (the spectrum will fold around 24 KHz and we want 100 dB rejection
    //    at the point where the folding reaches 20 KHz)
    //  ...___|_____
    //        |     \|
    //        | ____/|\____
    //        |/alias|     \
    //  ------/------+------\---------> KHz
    //       20     24     28

    // Transition band 8 KHz, or dw = 1.0472
    //
    // beta = 10.056
    // nzc  = 20
    //

    int ch;
    while ((ch = getopt(argc, argv, ":hds:c:n:f:l:b:p:v:")) != -1) {
        switch (ch) {
            case 'd':
                debug = true;
                break;
            case 'p':
                if (sscanf(optarg, "%u/%u", &polyM, &polyN) != 2) {
                    usage(argv[0]);
                }
                polyphase = true;
                break;
            case 's':
                Fs = atof(optarg);
                break;
            case 'c':
                Fc = atof(optarg);
                break;
            case 'n':
                nzc = atoi(optarg);
                break;
            case 'l':
                nz = atoi(optarg);
                break;
            case 'f':
                if (!strcmp(optarg,"fixed")) format = 0;
                else if (!strcmp(optarg,"float")) format = 1;
                else usage(argv[0]);
                break;
            case 'b':
                beta = atof(optarg);
                break;
            case 'v':
                atten = pow(10, -fabs(atof(optarg))*0.05 );
                break;
            case 'h':
            default:
                usage(argv[0]);
                break;
        }
    }

    // cut off frequency ratio Fc/Fs
    double Fcr = Fc / Fs;


    // total number of coefficients (one side)
    const int M = (1 << nz);
    const int N = M * nzc;

    // generate the right half of the filter
    if (!debug) {
        printf("// cmd-line: ");
        for (int i=1 ; i<argc ; i++) {
            printf("%s ", argv[i]);
        }
        printf("\n");
        if (!polyphase) {
            printf("const int32_t RESAMPLE_FIR_SIZE           = %d;\n", N);
            printf("const int32_t RESAMPLE_FIR_LERP_INT_BITS  = %d;\n", nz);
            printf("const int32_t RESAMPLE_FIR_NUM_COEF       = %d;\n", nzc);
        } else {
            printf("const int32_t RESAMPLE_FIR_SIZE           = %d;\n", 2*nzc*polyN);
            printf("const int32_t RESAMPLE_FIR_NUM_COEF       = %d;\n", 2*nzc);
        }
        if (!format) {
            printf("const int32_t RESAMPLE_FIR_COEF_BITS      = %d;\n", nc);
        }
        printf("\n");
        printf("static %s resampleFIR[] = {", !format ? "int32_t" : "float");
    }

    if (!polyphase) {
        for (int i=0 ; i<=M ; i++) { // an extra set of coefs for interpolation
            for (int j=0 ; j<nzc ; j++) {
                int ix = j*M + i;
                double x = (2.0 * M_PI * ix * Fcr) / (1 << nz);
                double y = kaiser(ix+N, 2*N, beta) * sinc(x) * 2.0 * Fcr;
                y *= atten;

                if (!debug) {
                    if (j == 0)
                        printf("\n    ");
                }

                if (!format) {
                    int64_t yi = floor(y * ((1ULL<<(nc-1))) + 0.5);
                    if (yi >= (1LL<<(nc-1))) yi = (1LL<<(nc-1))-1;
                    printf("0x%08x, ", int32_t(yi));
                } else {
                    printf("%.9g%s ", y, debug ? "," : "f,");
                }
            }
        }
    } else {
        for (int j=0 ; j<polyN ; j++) {
            // calculate the phase
            double p = ((polyM*j) % polyN) / double(polyN);
            if (!debug) printf("\n    ");
            else        printf("\n");
            // generate a FIR per phase
            for (int i=-nzc ; i<nzc ; i++) {
                double x = 2.0 * M_PI * Fcr * (i + p);
                double y = kaiser(i+N, 2*N, beta) * sinc(x) * 2.0 * Fcr;;
                y *= atten;
                if (!format) {
                    int64_t yi = floor(y * ((1ULL<<(nc-1))) + 0.5);
                    if (yi >= (1LL<<(nc-1))) yi = (1LL<<(nc-1))-1;
                    printf("0x%08x", int32_t(yi));
                } else {
                    printf("%.9g%s", y, debug ? "" : "f");
                }

                if (debug && (i==nzc-1)) {
                } else {
                    printf(", ");
                }
            }
        }
    }

    if (!debug) {
        printf("\n};");
    }
    printf("\n");
    return 0;
}

// http://www.csee.umbc.edu/help/sound/AFsp-V2R1/html/audio/ResampAudio.html


