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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <fcntl.h>

#include <OMXAL/OpenMAXAL.h>
#include <OMXAL/OpenMAXAL_Android.h> // for VP8 definitions

#define NUM_ENGINE_INTERFACES 1

char unknown[50];

//-----------------------------------------------------------------
/* Exits the application if an error is encountered */
#define ExitOnError(x) ExitOnErrorFunc(x,__LINE__)

void ExitOnErrorFunc( XAresult result , int line)
{
    if (XA_RESULT_SUCCESS != result) {
        fprintf(stderr, "Error %u encountered at line %d, exiting\n", result, line);
        exit(EXIT_FAILURE);
    }
}

const char* videoCodecIdToString(XAuint32 decoderId) {
    switch(decoderId) {
    case XA_VIDEOCODEC_MPEG2: return "XA_VIDEOCODEC_MPEG2"; break;
    case XA_VIDEOCODEC_H263: return "XA_VIDEOCODEC_H263"; break;
    case XA_VIDEOCODEC_MPEG4: return "XA_VIDEOCODEC_MPEG4"; break;
    case XA_VIDEOCODEC_AVC: return "XA_VIDEOCODEC_AVC"; break;
    case XA_VIDEOCODEC_VC1: return "XA_VIDEOCODEC_VC1"; break;
    case XA_ANDROID_VIDEOCODEC_VP8: return "XA_ANDROID_VIDEOCODEC_VP8"; break;
    default:
        sprintf(unknown, "Video codec %d unknown to OpenMAX AL", decoderId);
        return unknown;
    }
}

// Use a table of [integer, string] entries to map an integer to a string

typedef struct {
    XAuint32 id;
    const char *string;
} id_to_string_t;

const char *id_to_string(XAuint32 id, const id_to_string_t *table, size_t numEntries)
{
    size_t i;
    for (i = 0; i < numEntries; ++i) {
        if (id == table[i].id) {
            return table[i].string;
        }
    }
    return "Unknown";
}

// Use a table of [integer, table] entries to map a pair of integers to a string

typedef struct {
    XAuint32 id1;
    const id_to_string_t *id2_table;
    size_t id2_numEntries;
} id_pair_to_string_t;

const char *id_pair_to_string(XAuint32 id1, XAuint32 id2, const id_pair_to_string_t *table,
        size_t numEntries)
{
    size_t i;
    for (i = 0; i < numEntries; ++i) {
        if (id1 == table[i].id1) {
            return id_to_string(id2, table[i].id2_table, table[i].id2_numEntries);
        }
    }
    return "Unknown";
}

// Map a video codec and profile to string

const char *videoProfileToString(XAuint32 codec, XAuint32 profile) {
    // http://en.wikipedia.org/wiki/H.262/MPEG-2_Part_2
    static const id_to_string_t MPEG2[] = {
        {XA_VIDEOPROFILE_MPEG2_SIMPLE,  "Simple"},
        {XA_VIDEOPROFILE_MPEG2_MAIN,    "Main"},
        {XA_VIDEOPROFILE_MPEG2_422,     "4:2:2"},
        {XA_VIDEOPROFILE_MPEG2_SNR,     "SNR Scalable"},
        {XA_VIDEOPROFILE_MPEG2_SPATIAL, "Spatially Scalable"},
        {XA_VIDEOPROFILE_MPEG2_HIGH,    "High"},
    }, H263[] = {
        {XA_VIDEOPROFILE_H263_BASELINE,           "baseline"},
        {XA_VIDEOPROFILE_H263_H320CODING,         "H320 coding"},
        {XA_VIDEOPROFILE_H263_BACKWARDCOMPATIBLE, "backwards compatible"},
        {XA_VIDEOPROFILE_H263_ISWV2,              "isw v2"},
        {XA_VIDEOPROFILE_H263_ISWV3,              "isw v3"},
        {XA_VIDEOPROFILE_H263_HIGHCOMPRESSION,    "high compression"},
        {XA_VIDEOPROFILE_H263_INTERNET,           "internet"},
        {XA_VIDEOPROFILE_H263_INTERLACE,          "interlace"},
        {XA_VIDEOPROFILE_H263_HIGHLATENCY,        "high latency"},
    }, MPEG4[] = {
        {XA_VIDEOPROFILE_MPEG4_SIMPLE,           "simple"},
        {XA_VIDEOPROFILE_MPEG4_SIMPLESCALABLE,   "simple scalable"},
        {XA_VIDEOPROFILE_MPEG4_CORE,             "core"},
        {XA_VIDEOPROFILE_MPEG4_MAIN,             "main"},
        {XA_VIDEOPROFILE_MPEG4_NBIT,             "nbit"},
        {XA_VIDEOPROFILE_MPEG4_SCALABLETEXTURE,  "scalable texture"},
        {XA_VIDEOPROFILE_MPEG4_SIMPLEFACE,       "simple face"},
        {XA_VIDEOPROFILE_MPEG4_SIMPLEFBA,        "simple fba"},
        {XA_VIDEOPROFILE_MPEG4_BASICANIMATED,    "basic animated"},
        {XA_VIDEOPROFILE_MPEG4_HYBRID,           "hybrid"},
        {XA_VIDEOPROFILE_MPEG4_ADVANCEDREALTIME, "advanced realtime"},
        {XA_VIDEOPROFILE_MPEG4_CORESCALABLE,     "core scalable"},
        {XA_VIDEOPROFILE_MPEG4_ADVANCEDCODING,   "advanced coding"},
        {XA_VIDEOPROFILE_MPEG4_ADVANCEDCORE,     "advanced core"},
        {XA_VIDEOPROFILE_MPEG4_ADVANCEDSCALABLE, "advanced scalable"},
        // FIXME OpenMAX AL is out-of-date with respect to OpenMAX IL
        {16,                                     "advanced simple"},
    }, AVC[] = {
        {XA_VIDEOPROFILE_AVC_BASELINE, "Baseline"},
        {XA_VIDEOPROFILE_AVC_MAIN,     "Main"},
        {XA_VIDEOPROFILE_AVC_EXTENDED, "Extended"},
        {XA_VIDEOPROFILE_AVC_HIGH,     "High"},
        {XA_VIDEOPROFILE_AVC_HIGH10,   "High 10"},
        {XA_VIDEOPROFILE_AVC_HIGH422,  "High 4:2:2"},
        {XA_VIDEOPROFILE_AVC_HIGH444,  "High 4:4:4"},
    }, VC1[] = {
        // FIXME sic should be XA_VIDEOPROFILE_*
        {XA_VIDEOLEVEL_VC1_SIMPLE,   "simple"},
        {XA_VIDEOLEVEL_VC1_MAIN,     "main"},
        {XA_VIDEOLEVEL_VC1_ADVANCED, "advanced"},
    };
    static const id_pair_to_string_t table[] = {
        {XA_VIDEOCODEC_MPEG2, MPEG2, sizeof(MPEG2) / sizeof(MPEG2[0])},
        {XA_VIDEOCODEC_H263,  H263,  sizeof(H263)  / sizeof(H263[0])},
        {XA_VIDEOCODEC_MPEG4, MPEG4, sizeof(MPEG4) / sizeof(MPEG4[0])},
        {XA_VIDEOCODEC_AVC,   AVC,   sizeof(AVC)   / sizeof(AVC[0])},
        {XA_VIDEOCODEC_VC1,   VC1,   sizeof(VC1)   / sizeof(VC1[0])},
    };
    return id_pair_to_string(codec, profile, table, sizeof(table) / sizeof(table[0]));
}

// Map a video codec and level to string

const char* videoLevelToString(XAuint32 codec, XAuint32 level) {
    static const id_to_string_t MPEG2[] = {
        {XA_VIDEOLEVEL_MPEG2_LL,  "Low"},
        {XA_VIDEOLEVEL_MPEG2_ML,  "Main"},
        {XA_VIDEOLEVEL_MPEG2_H14, "H-14"},
        {XA_VIDEOLEVEL_MPEG2_HL,  "High"},
    }, H263[]= {
        {XA_VIDEOLEVEL_H263_10, "10"},
        {XA_VIDEOLEVEL_H263_20, "20"},
        {XA_VIDEOLEVEL_H263_30, "30"},
        {XA_VIDEOLEVEL_H263_40, "40"},
        {XA_VIDEOLEVEL_H263_45, "45"},
        {XA_VIDEOLEVEL_H263_50, "50"},
        {XA_VIDEOLEVEL_H263_60, "60"},
        {XA_VIDEOLEVEL_H263_70, "70"},
    }, MPEG4[] = {
        {XA_VIDEOLEVEL_MPEG4_0,  "0"},
        {XA_VIDEOLEVEL_MPEG4_0b, "0b"},
        {XA_VIDEOLEVEL_MPEG4_1,  "1"},
        {XA_VIDEOLEVEL_MPEG4_2,  "2"},
        {XA_VIDEOLEVEL_MPEG4_3,  "3"},
        {XA_VIDEOLEVEL_MPEG4_4,  "4"},
        {XA_VIDEOLEVEL_MPEG4_4a, "4a"},
        // FIXME OpenMAX AL is out-of-date with respect to OpenMAX IL
        {8,                      "5"},
    }, AVC[] = {
        {XA_VIDEOLEVEL_AVC_1,  "1"},
        {XA_VIDEOLEVEL_AVC_1B, "1B"},
        {XA_VIDEOLEVEL_AVC_11, "1.1"},
        {XA_VIDEOLEVEL_AVC_12, "1.2"},
        {XA_VIDEOLEVEL_AVC_13, "1.3"},
        {XA_VIDEOLEVEL_AVC_2,  "2"},
        {XA_VIDEOLEVEL_AVC_21, "2.1"},
        {XA_VIDEOLEVEL_AVC_22, "2.2"},
        {XA_VIDEOLEVEL_AVC_3,  "3"},
        {XA_VIDEOLEVEL_AVC_31, "3.1"},
        {XA_VIDEOLEVEL_AVC_32, "3.2"},
        {XA_VIDEOLEVEL_AVC_4,  "4"},
        {XA_VIDEOLEVEL_AVC_41, "4.1"},
        {XA_VIDEOLEVEL_AVC_42, "4.2"},
        {XA_VIDEOLEVEL_AVC_5,  "5"},
        {XA_VIDEOLEVEL_AVC_51, "5.1"},
    }, VC1[] = {
        {XA_VIDEOLEVEL_VC1_LOW,    "Low"},
        {XA_VIDEOLEVEL_VC1_MEDIUM, "Medium"},
        {XA_VIDEOLEVEL_VC1_HIGH,   "High"},
        {XA_VIDEOLEVEL_VC1_L0,     "L0"},
        {XA_VIDEOLEVEL_VC1_L1,     "L1"},
        {XA_VIDEOLEVEL_VC1_L2,     "L2"},
        {XA_VIDEOLEVEL_VC1_L3,     "L3"},
        {XA_VIDEOLEVEL_VC1_L4,     "L4"},
    };
    static const id_pair_to_string_t table[] = {
        {XA_VIDEOCODEC_MPEG2, MPEG2, sizeof(MPEG2) / sizeof(MPEG2[0])},
        {XA_VIDEOCODEC_H263,  H263,  sizeof(H263)  / sizeof(H263[0])},
        {XA_VIDEOCODEC_MPEG4, MPEG4, sizeof(MPEG4) / sizeof(MPEG4[0])},
        {XA_VIDEOCODEC_AVC,   AVC,   sizeof(AVC)   / sizeof(AVC[0])},
        {XA_VIDEOCODEC_VC1,   VC1,   sizeof(VC1)   / sizeof(VC1[0])},
    };
    return id_pair_to_string(codec, level, table, sizeof(table) / sizeof(table[0]));
}

//-----------------------------------------------------------------
void TestVideoDecoderCapabilities() {

    XAObjectItf xa;
    XAresult res;

    /* parameters for the OpenMAX AL engine creation */
    XAEngineOption EngineOption[] = {
            {(XAuint32) XA_ENGINEOPTION_THREADSAFE, (XAuint32) XA_BOOLEAN_TRUE}
    };
    XAInterfaceID itfIidArray[NUM_ENGINE_INTERFACES] = { XA_IID_VIDEODECODERCAPABILITIES };
    XAboolean     itfRequired[NUM_ENGINE_INTERFACES] = { XA_BOOLEAN_TRUE };

    /* create OpenMAX AL engine */
    res = xaCreateEngine( &xa, 1, EngineOption, NUM_ENGINE_INTERFACES, itfIidArray, itfRequired);
    ExitOnError(res);

    /* realize the engine in synchronous mode. */
    res = (*xa)->Realize(xa, XA_BOOLEAN_FALSE); ExitOnError(res);

    /* Get the video decoder capabilities interface which was explicitly requested */
    XAVideoDecoderCapabilitiesItf decItf;
    res = (*xa)->GetInterface(xa, XA_IID_VIDEODECODERCAPABILITIES, (void*)&decItf);
    ExitOnError(res);

    /* Query the platform capabilities */
    XAuint32 numDecoders = 0;
    XAuint32 *decoderIds = NULL;

    /* -> Number of decoders */
    res = (*decItf)->GetVideoDecoders(decItf, &numDecoders, NULL); ExitOnError(res);
    fprintf(stdout, "Found %d video decoders\n", numDecoders);
    if (0 == numDecoders) {
        fprintf(stderr, "0 video decoders is not an acceptable number, exiting\n");
        goto destroyRes;
    }

    /* -> Decoder list */
    decoderIds = (XAuint32 *) malloc(numDecoders * sizeof(XAuint32));
    res = (*decItf)->GetVideoDecoders(decItf, &numDecoders, decoderIds); ExitOnError(res);
    fprintf(stdout, "Decoders:\n");
    for(XAuint32 i = 0 ; i < numDecoders ; i++) {
        fprintf(stdout, "decoder %d is %s\n", i, videoCodecIdToString(decoderIds[i]));
    }

    /* -> Decoder capabilities */
    /*       for each decoder  */
    for(XAuint32 i = 0 ; i < numDecoders ; i++) {
        XAuint32 nbCombinations = 0;
        /* get the number of profile / level combinations */
        res = (*decItf)->GetVideoDecoderCapabilities(decItf, decoderIds[i], &nbCombinations, NULL);
        ExitOnError(res);
        fprintf(stdout, "decoder %s has %d profile/level combinations:\n\t",
                videoCodecIdToString(decoderIds[i]), nbCombinations);
        /* display the profile / level combinations */
        for(XAuint32 pl = 0 ; pl < nbCombinations ; pl++) {
            XAVideoCodecDescriptor decDescriptor;
            XAuint32 decoder = decoderIds[i];
            res = (*decItf)->GetVideoDecoderCapabilities(decItf, decoder, &pl, &decDescriptor);
            ExitOnError(res);
            XAuint32 profile = decDescriptor.profileSetting;
            XAuint32 level = decDescriptor.levelSetting;
            fprintf(stdout, "%u/%u ", profile, level);
            ExitOnError(res);
            printf("(%s/%s) ", videoProfileToString(decoder, profile),
                    videoLevelToString(decoder, level));
        }
        fprintf(stdout, "\n");
    }

destroyRes:
    free(decoderIds);

    /* shutdown OpenMAX AL */
    (*xa)->Destroy(xa);
}


//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    XAresult    result;
    XAObjectItf sl;

    fprintf(stdout, "OpenMAX AL test %s: exercises SLAudioDecoderCapabiltiesItf ", argv[0]);
    fprintf(stdout, "and displays the list of supported video decoders, and for each, lists the ");
    fprintf(stdout, "profile / levels combinations, that map to the constants defined in ");
    fprintf(stdout, "\"XA_VIDEOPROFILE and XA_VIDEOLEVEL\" section of the specification\n\n");

    TestVideoDecoderCapabilities();

    return EXIT_SUCCESS;
}
