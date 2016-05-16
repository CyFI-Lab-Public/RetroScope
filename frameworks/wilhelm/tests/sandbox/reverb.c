/*
 * Copyright (C) 2010 The Android Open Source Project
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

// Demonstrate environmental reverb and preset reverb on an output mix and audio player

#include <SLES/OpenSLES.h>
#include <assert.h>
#include <pthread.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define bool int
#define false 0
#define true 1

// Table of I3DL2 named environmental reverb settings

typedef struct {
    const char *mName;
    SLEnvironmentalReverbSettings mSettings;
} Pair;

#define _(name) {#name, SL_I3DL2_ENVIRONMENT_PRESET_##name},

Pair pairs[] = {
    _(DEFAULT)
    _(GENERIC)
    _(PADDEDCELL)
    _(ROOM)
    _(BATHROOM)
    _(LIVINGROOM)
    _(STONEROOM)
    _(AUDITORIUM)
    _(CONCERTHALL)
    _(CAVE)
    _(ARENA)
    _(HANGAR)
    _(CARPETEDHALLWAY)
    _(HALLWAY)
    _(STONECORRIDOR)
    _(ALLEY)
    _(FOREST)
    _(CITY)
    _(MOUNTAINS)
    _(QUARRY)
    _(PLAIN)
    _(PARKINGLOT)
    _(SEWERPIPE)
    _(UNDERWATER)
    _(SMALLROOM)
    _(MEDIUMROOM)
    _(LARGEROOM)
    _(MEDIUMHALL)
    _(LARGEHALL)
    _(PLATE)
};

// Parameters for preset reverb on output mix
bool outputMixPresetItfRequested = false;
SLuint16 outputMixPresetNumber = ~0;

// Parameters for environmental reverb on output mix
bool outputMixEnvironmentalItfRequested = false;
char *outputMixEnvironmentalName = NULL;
SLEnvironmentalReverbSettings outputMixEnvironmentalSettings;

// Parameters for preset reverb on audio player (not supported)
bool playerPresetItfRequested = false;
SLuint16 playerPresetNumber = ~0;

// Parameters for environmental reverb on audio player (not supported)
bool playerEnvironmentalItfRequested = false;
char *playerEnvironmentalName = NULL;
SLEnvironmentalReverbSettings playerEnvironmentalSettings;

// Compare two environmental reverb settings structures.
// Returns true if the settings are identical, or false if they are different.

bool slesutCompareEnvironmentalReverbSettings(
        const SLEnvironmentalReverbSettings *settings1,
        const SLEnvironmentalReverbSettings *settings2)
{
    return
        (settings1->roomLevel == settings2->roomLevel) &&
        (settings1->roomHFLevel == settings2->roomHFLevel) &&
        (settings1->decayTime == settings2->decayTime) &&
        (settings1->decayHFRatio == settings2->decayHFRatio) &&
        (settings1->reflectionsLevel == settings2->reflectionsLevel) &&
        (settings1->reflectionsDelay == settings2->reflectionsDelay) &&
        (settings1->reverbLevel == settings2->reverbLevel) &&
        (settings1->reverbDelay == settings2->reverbDelay) &&
        (settings1->diffusion == settings2->diffusion) &&
        (settings1->density == settings2->density);
}

// Print an environmental reverb settings structure.

void slesutPrintEnvironmentalReverbSettings(const SLEnvironmentalReverbSettings *settings)
{
    printf("roomLevel: %d\n", settings->roomLevel);
    printf("roomHFLevel: %d\n", settings->roomHFLevel);
    printf("decayTime: %d\n", settings->decayTime);
    printf("decayHFRatio: %d\n", settings->decayHFRatio);
    printf("reflectionsLevel: %d\n", settings->reflectionsLevel);
    printf("reflectionsDelay: %d\n", settings->reflectionsDelay);
    printf("reverbLevel: %d\n", settings->reverbLevel);
    printf("reverbDelay: %d\n", settings->reverbDelay);
    printf("diffusion: %d\n", settings->diffusion);
    printf("density: %d\n", settings->density);
}

// Lookup environmental reverb settings by name

const SLEnvironmentalReverbSettings *lookupEnvName(const char *name)
{
    unsigned j;
    for (j = 0; j < sizeof(pairs) / sizeof(pairs[0]); ++j) {
        if (!strcasecmp(name, pairs[j].mName)) {
            return &pairs[j].mSettings;
        }
    }
    return NULL;
}

// Print all available environmental reverb names

void printEnvNames(void)
{
    unsigned j;
    bool needSpace = false;
    bool needNewline = false;
    unsigned lineLen = 0;
    for (j = 0; j < sizeof(pairs) / sizeof(pairs[0]); ++j) {
        const char *name = pairs[j].mName;
        unsigned nameLen = strlen(name);
        if (lineLen + (needSpace ? 1 : 0) + nameLen > 72) {
            putchar('\n');
            needSpace = false;
            needNewline = false;
            lineLen = 0;
        }
        if (needSpace) {
            putchar(' ');
            ++lineLen;
        }
        fputs(name, stdout);
        lineLen += nameLen;
        needSpace = true;
        needNewline = true;
    }
    if (needNewline) {
        putchar('\n');
    }
}

// These are extensions to OpenSL ES 1.0.1 values

#define SL_PREFETCHSTATUS_UNKNOWN 0
#define SL_PREFETCHSTATUS_ERROR   (-1)

// Mutex and condition shared with main program to protect prefetch_status

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
SLuint32 prefetch_status = SL_PREFETCHSTATUS_UNKNOWN;

/* used to detect errors likely to have occured when the OpenSL ES framework fails to open
 * a resource, for instance because a file URI is invalid, or an HTTP server doesn't respond.
 */
#define PREFETCHEVENT_ERROR_CANDIDATE \
        (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE)

// Prefetch status callback

void prefetch_callback(SLPrefetchStatusItf caller, void *context, SLuint32 event)
{
    SLresult result;
    assert(context == NULL);
    SLpermille level;
    result = (*caller)->GetFillLevel(caller, &level);
    assert(SL_RESULT_SUCCESS == result);
    SLuint32 status;
    result = (*caller)->GetPrefetchStatus(caller, &status);
    assert(SL_RESULT_SUCCESS == result);
    SLuint32 new_prefetch_status;
    if ((event & PREFETCHEVENT_ERROR_CANDIDATE) == PREFETCHEVENT_ERROR_CANDIDATE
            && level == 0 && status == SL_PREFETCHSTATUS_UNDERFLOW) {
        new_prefetch_status = SL_PREFETCHSTATUS_ERROR;
    } else if (event == SL_PREFETCHEVENT_STATUSCHANGE &&
            status == SL_PREFETCHSTATUS_SUFFICIENTDATA) {
        new_prefetch_status = status;
    } else {
        return;
    }
    int ok;
    ok = pthread_mutex_lock(&mutex);
    assert(ok == 0);
    prefetch_status = new_prefetch_status;
    ok = pthread_cond_signal(&cond);
    assert(ok == 0);
    ok = pthread_mutex_unlock(&mutex);
    assert(ok == 0);
}

// Main program

int main(int argc, char **argv)
{
    SLresult result;
    bool loop = false;

    // process command line parameters
    char *prog = argv[0];
    int i;
    for (i = 1; i < argc; ++i) {
        char *arg = argv[i];
        if (arg[0] != '-')
            break;
        bool bad = false;   // whether the option string is invalid
        if (!strncmp(arg, "--mix-preset", 12)) {
            if ('\0' == arg[12]) {
                outputMixPresetItfRequested = true;
            } else if ('=' == arg[12]) {
                outputMixPresetNumber = atoi(&arg[13]);
                outputMixPresetItfRequested = true;
            } else {
                bad = true;
            }
        } else if (!strncmp(arg, "--mix-name", 10)) {
            if ('\0' == arg[10]) {
                outputMixEnvironmentalItfRequested = true;
            } else if ('=' == arg[10]) {
                outputMixEnvironmentalName = &arg[11];
                outputMixEnvironmentalItfRequested = true;
            } else {
                bad = true;
            }
        } else if (!strncmp(arg, "--player-preset", 15)) {
            if ('\0' == arg[15]) {
                playerPresetItfRequested = true;
            } else if ('=' == arg[15]) {
                playerPresetNumber = atoi(&arg[16]);
                playerPresetItfRequested = true;
            } else {
                bad = true;
            }
        } else if (!strncmp(arg, "--player-name", 13)) {
            if ('\0' == arg[13]) {
                playerEnvironmentalItfRequested = true;
            } else if ('=' == arg[13]) {
                playerEnvironmentalName = &arg[14];
                playerEnvironmentalItfRequested = true;
            } else {
                bad = true;
            }
        } else if (!strcmp(arg, "--loop")) {
            loop = true;
        } else {
            bad = true;
        }
        if (bad) {
            fprintf(stderr, "%s: unknown option %s ignored\n", prog, arg);
        }
    }
    if (argc - i != 1) {
        fprintf(stderr, "usage: %s --mix-preset=# --mix-name=I3DL2 --player-preset=# "
                "--player-name=I3DL2 --loop filename\n", prog);
        return EXIT_FAILURE;
    }
    char *pathname = argv[i];

    const SLEnvironmentalReverbSettings *envSettings;
    if (NULL != outputMixEnvironmentalName) {
        envSettings = lookupEnvName(outputMixEnvironmentalName);
        if (NULL == envSettings) {
            fprintf(stderr, "%s: output mix environmental reverb name %s not found, "
                    "available names are:\n", prog, outputMixEnvironmentalName);
            printEnvNames();
            return EXIT_FAILURE;
        }
        outputMixEnvironmentalSettings = *envSettings;
    }
    if (NULL != playerEnvironmentalName) {
        envSettings = lookupEnvName(playerEnvironmentalName);
        if (NULL == envSettings) {
            fprintf(stderr, "%s: player environmental reverb name %s not found, "
                    "available names are:\n", prog, playerEnvironmentalName);
            printEnvNames();
            return EXIT_FAILURE;
        }
        playerEnvironmentalSettings = *envSettings;
    }

    // create engine
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);

    // create output mix
    SLInterfaceID mix_ids[2];
    SLboolean mix_req[2];
    SLuint32 count = 0;
    if (outputMixPresetItfRequested) {
        mix_req[count] = SL_BOOLEAN_TRUE;
        mix_ids[count++] = SL_IID_PRESETREVERB;
    }
    if (outputMixEnvironmentalItfRequested) {
        mix_req[count] = SL_BOOLEAN_TRUE;
        mix_ids[count++] = SL_IID_ENVIRONMENTALREVERB;
    }
    SLObjectItf mixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &mixObject, count, mix_ids, mix_req);
    assert(SL_RESULT_SUCCESS == result);
    result = (*mixObject)->Realize(mixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // configure preset reverb on output mix
    SLPresetReverbItf outputMixPresetReverb;
    if (outputMixPresetItfRequested) {
        result = (*mixObject)->GetInterface(mixObject, SL_IID_PRESETREVERB, &outputMixPresetReverb);
        assert(SL_RESULT_SUCCESS == result);
        SLuint16 getPresetReverb = 12345;
        result = (*outputMixPresetReverb)->GetPreset(outputMixPresetReverb, &getPresetReverb);
        assert(SL_RESULT_SUCCESS == result);
        printf("Output mix default preset reverb number = %u\n", getPresetReverb);
        if (outputMixPresetNumber != ((SLuint16) ~0)) {
            result = (*outputMixPresetReverb)->SetPreset(outputMixPresetReverb,
                    outputMixPresetNumber);
            if (SL_RESULT_SUCCESS == result) {
                result = (*outputMixPresetReverb)->GetPreset(outputMixPresetReverb,
                        &getPresetReverb);
                assert(SL_RESULT_SUCCESS == result);
                assert(getPresetReverb == outputMixPresetNumber);
                printf("Output mix preset reverb successfully changed to %u\n",
                        outputMixPresetNumber);
            } else {
                printf("Unable to set output mix preset reverb to %u, result=%u\n",
                        outputMixPresetNumber, result);
            }
        }
    }

    // configure environmental reverb on output mix
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb;
    if (outputMixEnvironmentalItfRequested) {
        result = (*mixObject)->GetInterface(mixObject, SL_IID_ENVIRONMENTALREVERB,
                &outputMixEnvironmentalReverb);
        assert(SL_RESULT_SUCCESS == result);
        SLEnvironmentalReverbSettings getSettings;
        result = (*outputMixEnvironmentalReverb)->GetEnvironmentalReverbProperties(
                outputMixEnvironmentalReverb, &getSettings);
        assert(SL_RESULT_SUCCESS == result);
        printf("Output mix default environmental reverb settings\n");
        printf("------------------------------------------------\n");
        slesutPrintEnvironmentalReverbSettings(&getSettings);
        printf("\n");
        if (outputMixEnvironmentalName != NULL) {
            result = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                    outputMixEnvironmentalReverb, &outputMixEnvironmentalSettings);
            assert(SL_RESULT_SUCCESS == result);
            printf("Output mix new environmental reverb settings\n");
            printf("--------------------------------------------\n");
            slesutPrintEnvironmentalReverbSettings(&outputMixEnvironmentalSettings);
            printf("\n");
            result = (*outputMixEnvironmentalReverb)->GetEnvironmentalReverbProperties(
                    outputMixEnvironmentalReverb, &getSettings);
            assert(SL_RESULT_SUCCESS == result);
            printf("Output mix read environmental reverb settings\n");
            printf("--------------------------------------------\n");
            slesutPrintEnvironmentalReverbSettings(&getSettings);
            printf("\n");
            if (!slesutCompareEnvironmentalReverbSettings(&getSettings,
                    &outputMixEnvironmentalSettings)) {
                printf("Warning: new and read are different; check details above\n");
            } else {
                printf("New and read match, life is good\n");
            }
        }
    }

    // create audio player
    SLDataLocator_URI locURI = {SL_DATALOCATOR_URI, (SLchar *) pathname};
    SLDataFormat_MIME dfMIME = {SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED};
    SLDataSource audioSrc = {&locURI, &dfMIME};
    SLDataLocator_OutputMix locOutputMix = {SL_DATALOCATOR_OUTPUTMIX, mixObject};
    SLDataSink audioSnk = {&locOutputMix, NULL};
    SLInterfaceID player_ids[5];
    SLboolean player_req[5];
    count = 0;
    if (playerPresetItfRequested) {
        player_req[count] = SL_BOOLEAN_TRUE;
        player_ids[count++] = SL_IID_PRESETREVERB;
    }
    if (playerEnvironmentalItfRequested) {
        player_req[count] = SL_BOOLEAN_TRUE;
        player_ids[count++] = SL_IID_ENVIRONMENTALREVERB;
    }
    if (outputMixPresetItfRequested || outputMixEnvironmentalItfRequested) {
        player_req[count] = SL_BOOLEAN_TRUE;
        player_ids[count++] = SL_IID_EFFECTSEND;
    }
    if (loop) {
        player_req[count] = SL_BOOLEAN_TRUE;
        player_ids[count++] = SL_IID_SEEK;
    }
    player_req[count] = SL_BOOLEAN_TRUE;
    player_ids[count++] = SL_IID_PREFETCHSTATUS;
    SLObjectItf playerObject;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
        &audioSnk, count, player_ids, player_req);
    assert(SL_RESULT_SUCCESS == result);

    // realize audio player
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // if reverb is on output mix (aux effect), then enable it for this player
    if (outputMixPresetItfRequested || outputMixEnvironmentalItfRequested) {
        SLEffectSendItf playerEffectSend;
        result = (*playerObject)->GetInterface(playerObject, SL_IID_EFFECTSEND, &playerEffectSend);
        assert(SL_RESULT_SUCCESS == result);
        SLboolean enabled;
        SLmillibel directLevel;
        SLmillibel sendLevel;
        if (outputMixPresetItfRequested) {
            result = (*playerEffectSend)->IsEnabled(playerEffectSend, outputMixPresetReverb,
                    &enabled);
            assert(SL_RESULT_SUCCESS == result);
            printf("Output mix preset reverb: player effect send default enabled = %s\n",
                    enabled ? "true" : "false");
            directLevel = 12345;
            result = (*playerEffectSend)->GetDirectLevel(playerEffectSend, &directLevel);
            assert(SL_RESULT_SUCCESS == result);
            printf("Output mix preset reverb: player effect send default direct level = %d\n",
                    directLevel);
            sendLevel = 12345;
            result = (*playerEffectSend)->GetSendLevel(playerEffectSend, outputMixPresetReverb,
                    &sendLevel);
            assert(SL_RESULT_SUCCESS == result);
            printf("Output mix preset reverb: player effect send default send level = %d\n",
                    sendLevel);
            if (outputMixPresetNumber != ((SLuint16) ~0)) {
                result = (*playerEffectSend)->EnableEffectSend(playerEffectSend,
                        outputMixPresetReverb, SL_BOOLEAN_TRUE, (SLmillibel) 0);
                assert(SL_RESULT_SUCCESS == result);
                result = (*playerEffectSend)->IsEnabled(playerEffectSend, outputMixPresetReverb,
                        &enabled);
                assert(SL_RESULT_SUCCESS == result);
                directLevel = 12345;
                result = (*playerEffectSend)->GetDirectLevel(playerEffectSend, &directLevel);
                assert(SL_RESULT_SUCCESS == result);
                sendLevel = 12345;
                result = (*playerEffectSend)->GetSendLevel(playerEffectSend, outputMixPresetReverb,
                        &sendLevel);
                assert(SL_RESULT_SUCCESS == result);
                printf("Output mix preset reverb: player effect send new enabled = %s, direct level"
                    " = %d, send level = %d\n", enabled ? "true" : "false", directLevel, sendLevel);
            }
        }
        if (outputMixEnvironmentalItfRequested) {
            if (outputMixEnvironmentalName != NULL) {
                result = (*playerEffectSend)->IsEnabled(playerEffectSend,
                        outputMixEnvironmentalReverb, &enabled);
                assert(SL_RESULT_SUCCESS == result);
                printf("Output mix environmental reverb: player effect send default enabled = %s\n",
                        enabled ? "true" : "false");
                directLevel = 12345;
                result = (*playerEffectSend)->GetDirectLevel(playerEffectSend, &directLevel);
                assert(SL_RESULT_SUCCESS == result);
                printf("Output mix environmental reverb: player effect send default direct level"
                        " = %d\n", directLevel);
                sendLevel = 12345;
                result = (*playerEffectSend)->GetSendLevel(playerEffectSend,
                        outputMixEnvironmentalReverb, &sendLevel);
                assert(SL_RESULT_SUCCESS == result);
                printf("Output mix environmental reverb: player effect send default send level"
                        " = %d\n", sendLevel);
                result = (*playerEffectSend)->EnableEffectSend(playerEffectSend,
                        outputMixEnvironmentalReverb, SL_BOOLEAN_TRUE, (SLmillibel) 0);
                assert(SL_RESULT_SUCCESS == result);
                result = (*playerEffectSend)->IsEnabled(playerEffectSend,
                        outputMixEnvironmentalReverb, &enabled);
                assert(SL_RESULT_SUCCESS == result);
                directLevel = 12345;
                result = (*playerEffectSend)->GetDirectLevel(playerEffectSend, &directLevel);
                assert(SL_RESULT_SUCCESS == result);
                sendLevel = 12345;
                result = (*playerEffectSend)->GetSendLevel(playerEffectSend,
                        outputMixEnvironmentalReverb, &sendLevel);
                assert(SL_RESULT_SUCCESS == result);
                printf("Output mix environmental reverb: player effect send new enabled = %s, "
                    "direct level = %d, send level = %d\n", enabled ? "true" : "false",
                    directLevel, sendLevel);
            }
        }
    }

    // configure preset reverb on player
    SLPresetReverbItf playerPresetReverb;
    if (playerPresetItfRequested) {
        result = (*playerObject)->GetInterface(playerObject, SL_IID_PRESETREVERB,
                &playerPresetReverb);
        assert(SL_RESULT_SUCCESS == result);
        SLuint16 getPresetReverb = 12345;
        result = (*playerPresetReverb)->GetPreset(playerPresetReverb, &getPresetReverb);
        if (SL_RESULT_SUCCESS == result) {
            printf("Player default preset reverb %u\n", getPresetReverb);
            if (playerPresetNumber != ((SLuint16) ~0)) {
                result = (*playerPresetReverb)->SetPreset(playerPresetReverb, playerPresetNumber);
                if (SL_RESULT_SUCCESS == result) {
                    result = (*playerPresetReverb)->GetPreset(playerPresetReverb, &getPresetReverb);
                    assert(SL_RESULT_SUCCESS == result);
                    assert(getPresetReverb == playerPresetNumber);
                    printf("Player preset reverb successfully changed to %u\n", playerPresetNumber);
                } else {
                    printf("Unable to set player preset reverb to %u, result=%u\n",
                            playerPresetNumber, result);
                }
            }
        } else {
            printf("Unable to get player default preset reverb, result=%u\n", result);
        }
    }

    // configure environmental reverb on player
    SLEnvironmentalReverbItf playerEnvironmentalReverb;
    if (playerEnvironmentalItfRequested) {
        result = (*playerObject)->GetInterface(playerObject, SL_IID_ENVIRONMENTALREVERB,
                &playerEnvironmentalReverb);
        assert(SL_RESULT_SUCCESS == result);
        SLEnvironmentalReverbSettings getSettings;
        memset(&getSettings, 0, sizeof(getSettings));
        result = (*playerEnvironmentalReverb)->GetEnvironmentalReverbProperties(
                playerEnvironmentalReverb, &getSettings);
        if (SL_RESULT_SUCCESS == result) {
            printf("Player default environmental reverb settings\n");
            printf("--------------------------------------------\n");
            slesutPrintEnvironmentalReverbSettings(&getSettings);
            printf("\n");
            if (playerEnvironmentalName != NULL) {
                result = (*playerEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                        playerEnvironmentalReverb, &playerEnvironmentalSettings);
                assert(SL_RESULT_SUCCESS == result);
                printf("Player new environmental reverb settings\n");
                printf("----------------------------------------\n");
                slesutPrintEnvironmentalReverbSettings(&playerEnvironmentalSettings);
                printf("\n");
                result = (*playerEnvironmentalReverb)->GetEnvironmentalReverbProperties(
                        playerEnvironmentalReverb, &getSettings);
                assert(SL_RESULT_SUCCESS == result);
                printf("Player read environmental reverb settings\n");
                printf("-----------------------------------------\n");
                slesutPrintEnvironmentalReverbSettings(&getSettings);
                printf("\n");
                if (!slesutCompareEnvironmentalReverbSettings(&getSettings,
                        &playerEnvironmentalSettings)) {
                    printf("Warning: new and read are different; check details above\n");
                } else {
                    printf("New and read match, life is good\n");
                }
            }
        } else {
            printf("Unable to get player default environmental reverb properties, result=%u\n",
                    result);
        }
    }

    // get the play interface
    SLPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    assert(SL_RESULT_SUCCESS == result);

    // get the prefetch status interface
    SLPrefetchStatusItf playerPrefetchStatus;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PREFETCHSTATUS,
            &playerPrefetchStatus);
    assert(SL_RESULT_SUCCESS == result);

    // enable prefetch status callbacks
    result = (*playerPrefetchStatus)->RegisterCallback(playerPrefetchStatus, prefetch_callback,
            NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*playerPrefetchStatus)->SetCallbackEventsMask(playerPrefetchStatus,
            SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE);
    assert(SL_RESULT_SUCCESS == result);

    // set play state to paused to enable pre-fetch so we can get a more reliable duration
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PAUSED);
    assert(SL_RESULT_SUCCESS == result);

    // wait for prefetch status callback to indicate either sufficient data or error
    pthread_mutex_lock(&mutex);
    while (prefetch_status == SL_PREFETCHSTATUS_UNKNOWN) {
        pthread_cond_wait(&cond, &mutex);
    }
    pthread_mutex_unlock(&mutex);
    if (prefetch_status == SL_PREFETCHSTATUS_ERROR) {
        fprintf(stderr, "Error during prefetch, exiting\n");
        goto destroyRes;
    }

    // get the duration
    SLmillisecond duration;
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(SL_RESULT_SUCCESS == result);
    if (SL_TIME_UNKNOWN == duration) {
        printf("duration: unknown\n");
    } else {
        printf("duration: %.1f seconds\n", duration / 1000.0);
    }

    // enable looping
    if (loop) {
        SLSeekItf playerSeek;
        result = (*playerObject)->GetInterface(playerObject, SL_IID_SEEK, &playerSeek);
        assert(SL_RESULT_SUCCESS == result);
        result = (*playerSeek)->SetLoop(playerSeek, SL_BOOLEAN_TRUE, (SLmillisecond) 0,
                SL_TIME_UNKNOWN);
        assert(SL_RESULT_SUCCESS == result);
    }

    // start audio playing
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);

    // wait for audio to finish playing
    SLuint32 state;
    for (;;) {
        result = (*playerPlay)->GetPlayState(playerPlay, &state);
        assert(SL_RESULT_SUCCESS == result);
        if (SL_PLAYSTATE_PLAYING != state)
            break;
        usleep(1000000);
     }
    assert(SL_PLAYSTATE_PAUSED == state);

destroyRes:
    // cleanup objects
    (*playerObject)->Destroy(playerObject);
    (*mixObject)->Destroy(mixObject);
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
