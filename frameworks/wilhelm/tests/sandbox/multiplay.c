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

// multiplay is a command-line test app that plays multiple files randomly

#include <SLES/OpenSLES.h>
#include <assert.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

// Describes the state of one player

typedef struct {
    SLObjectItf mPlayerObject;
    SLPlayItf mPlayerPlay;
    SLSeekItf mPlayerSeek;
    SLPrefetchStatusItf mPlayerPrefetchStatus;
    SLVolumeItf mPlayerVolume;
    SLmillisecond mPlayerDuration;
    SLboolean mPlayerErrorInCallback;
    SLboolean mPlayerErrorReported;
} Player;

// Strings corresponding to result codes; FIXME should move to a common test library

static const char *result_strings[] = {
    "SUCCESS",
    "PRECONDITIONS_VIOLATED",
    "PARAMETER_INVALID",
    "MEMORY_FAILURE",
    "RESOURCE_ERROR",
    "RESOURCE_LOST",
    "IO_ERROR",
    "BUFFER_INSUFFICIENT",
    "CONTENT_CORRUPTED",
    "CONTENT_UNSUPPORTED",
    "CONTENT_NOT_FOUND",
    "PERMISSION_DENIED",
    "FEATURE_UNSUPPORTED",
    "INTERNAL_ERROR",
    "UNKNOWN_ERROR",
    "OPERATION_ABORTED",
    "CONTROL_LOST"
};

// Convert result to string; FIXME should move to common test library

static const char *result_to_string(SLresult result)
{
    static char buffer[32];
    if ( /* result >= 0 && */ result < sizeof(result_strings) / sizeof(result_strings[0]))
        return result_strings[result];
    sprintf(buffer, "%d", (int) result);
    return buffer;
}

// Compare result against expected and exit suddenly if wrong

void check2(SLresult result, int line)
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stderr, "error %s at line %d\n", result_to_string(result), line);
        exit(EXIT_FAILURE);
    }
}

// Same as above but automatically adds the source code line number

#define check(result) check2(result, __LINE__)

// Prefetch status callback

#define PREFETCHEVENT_ERROR_CANDIDATE \
            (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE)

void prefetch_callback(SLPrefetchStatusItf caller, void *context, SLuint32 event)
{
    SLresult result;
    assert(context != NULL);
    Player *p = (Player *) context;
    assert(p->mPlayerPrefetchStatus == caller);
    SLpermille level;
    result = (*caller)->GetFillLevel(caller, &level);
    check(result);
    SLuint32 status;
    result = (*caller)->GetPrefetchStatus(caller, &status);
    check(result);
    //fprintf(stderr, "PrefetchEventCallback: received event %u, level %u, status %u\n",
    //      event, level, status);
    if ((PREFETCHEVENT_ERROR_CANDIDATE == (event & PREFETCHEVENT_ERROR_CANDIDATE))
            && (level == 0) && (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
        p->mPlayerErrorInCallback = SL_BOOLEAN_TRUE;
    }
}

// Main program

int main(int argc, char **argv)
{
    int i;
    const char *arg;
    int numPlayers = 0;
    int playTimeInMilliseconds = 0; // default to run forever
    SLmillibel mixVolumeLevel = 0;
    for (i = 1; i < argc; ++i) {
        arg = argv[i];
        if (arg[0] != '-')
            break;
        if (!strncmp(arg, "-n", 2))
            numPlayers = atoi(&arg[2]);
        else if (!strncmp(arg, "-v", 2))
            mixVolumeLevel = atoi(&arg[2]);
        else if (!strncmp(arg, "-t", 2))
            playTimeInMilliseconds = atoi(&arg[2]) * 1000;
        else
            fprintf(stderr, "unknown option: %s\n", arg);
    }
    int numPathnames = argc - i;
    if (numPathnames <= 0) {
        fprintf(stderr, "usage: %s file.wav ...\n", argv[0]);
        return EXIT_FAILURE;
    }
    if (numPlayers <= 0) {
        numPlayers = numPathnames;
    }
    Player *players = (Player *) calloc(numPlayers, sizeof(Player));
    assert(NULL != players);
    char **pathnames = &argv[i];
    SLresult result;

    // engine
    const SLInterfaceID engine_ids[] = {SL_IID_ENGINE};
    const SLboolean engine_req[] = {SL_BOOLEAN_TRUE};
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 1, engine_ids, engine_req);
    check(result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    check(result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    check(result);

    // mixer
    const SLInterfaceID mix_ids[] = {SL_IID_VOLUME};
    const SLboolean mix_req[] = {SL_BOOLEAN_TRUE};
    SLObjectItf mixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &mixObject, 0, mix_ids, mix_req);
    check(result);
    result = (*mixObject)->Realize(mixObject, SL_BOOLEAN_FALSE);
    check(result);
#if 0
    SLVolumeItf mixVolume;
    result = (*mixObject)->GetInterface(mixObject, SL_IID_VOLUME, &mixVolume);
    check(result);
    SLmillibel mixVolumeLevelDefault;
    result = (*mixVolume)->GetVolumeLevel(mixVolume, &mixVolumeLevelDefault);
    check(result);
    printf("default mix volume level = %d\n", mixVolumeLevelDefault);
#endif

    printf("numPathnames=%d\n", numPathnames);
    printf("numPlayers=%d\n", numPlayers);
    Player *p;

    // create all the players
    for (i = 0; i < numPlayers; ++i) {
        const SLInterfaceID player_ids[] =
                {SL_IID_PLAY, SL_IID_VOLUME, SL_IID_SEEK, SL_IID_PREFETCHSTATUS};
        const SLboolean player_req[] =
                {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
        p = &players[i];
        SLDataLocator_URI locURI = {SL_DATALOCATOR_URI, (SLchar *) pathnames[i % numPathnames]};
        SLDataFormat_MIME dfMIME = {SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED};
        SLDataSource audioSrc = {&locURI, &dfMIME};
        SLDataLocator_OutputMix locOutputMix = {SL_DATALOCATOR_OUTPUTMIX, mixObject};
        SLDataSink audioSnk = {&locOutputMix, NULL};
        result = (*engineEngine)->CreateAudioPlayer(engineEngine, &p->mPlayerObject, &audioSrc,
            &audioSnk, sizeof(player_ids)/sizeof(player_ids[0]), player_ids, player_req);
        check(result);
        result = (*p->mPlayerObject)->Realize(p->mPlayerObject, SL_BOOLEAN_FALSE);
        check(result);
        result = (*p->mPlayerObject)->GetInterface(p->mPlayerObject, SL_IID_PLAY, &p->mPlayerPlay);
        check(result);
        result = (*p->mPlayerObject)->GetInterface(p->mPlayerObject, SL_IID_VOLUME,
            &p->mPlayerVolume);
        check(result);
        result = (*p->mPlayerObject)->GetInterface(p->mPlayerObject, SL_IID_SEEK, &p->mPlayerSeek);
        check(result);
        result = (*p->mPlayerObject)->GetInterface(p->mPlayerObject, SL_IID_PREFETCHSTATUS,
                &p->mPlayerPrefetchStatus);
        check(result);
        result = (*p->mPlayerPrefetchStatus)->RegisterCallback(p->mPlayerPrefetchStatus,
                prefetch_callback, p);
        check(result);
        result = (*p->mPlayerPrefetchStatus)->SetCallbackEventsMask(p->mPlayerPrefetchStatus,
                SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE);
        check(result);
    }

    // now loop randomly doing things to the players
    for (;;) {
        SLmillisecond delay = 100 + (rand() & 1023);
        printf("sleep %u\n", (unsigned) delay);
        usleep(delay * 1000);
        i = (rand() & 0x7FFFFFFF) % numPlayers;
        p = &players[i];
        if (p->mPlayerErrorReported)
            continue;
        printf("player %d (%s): ", i, pathnames[i]);
        if (p->mPlayerErrorInCallback && !p->mPlayerErrorReported) {
            printf("error, ");
            p->mPlayerErrorReported = SL_BOOLEAN_TRUE;
        }
        result = (*p->mPlayerPlay)->GetDuration(p->mPlayerPlay, &p->mPlayerDuration);
        check(result);
        if (p->mPlayerDuration == SL_TIME_UNKNOWN) {
            printf("duration unknown, ");
        } else {
            printf("duration %d ms, ", (int) p->mPlayerDuration);
        }
        SLuint32 state;
        result = (*p->mPlayerPlay)->GetPlayState(p->mPlayerPlay, &state);
        check(result);
        printf("state = ");
        switch (state) {
        case SL_PLAYSTATE_STOPPED:
            printf("STOPPED");
            break;
        case SL_PLAYSTATE_PAUSED:
            printf("PAUSED");
            break;
        case SL_PLAYSTATE_PLAYING:
            printf("PLAYING");
            break;
        default:
            printf("%u", (unsigned) state);
            break;
        }
        printf("\n");
        if (state == SL_PLAYSTATE_STOPPED || state == SL_PLAYSTATE_PAUSED) {
            SLmillibel volumeLevel = -((rand() & 0x7FFFFFFF) % ((SL_MILLIBEL_MIN + 1) / 10));
            printf("volume %d\n", volumeLevel);
            result = (*p->mPlayerVolume)->SetVolumeLevel(p->mPlayerVolume, volumeLevel);
            check(result);
            result = (*p->mPlayerVolume)->EnableStereoPosition(p->mPlayerVolume, SL_BOOLEAN_TRUE);
            check(result);
            SLpermille stereoPosition = ((rand() & 0x7FFFFFFF) % 2001) - 1000;
            printf("position %d\n", stereoPosition);
            result = (*p->mPlayerVolume)->SetStereoPosition(p->mPlayerVolume, stereoPosition);
            check(result);
            if (state != SL_PLAYSTATE_STOPPED) {
                result = (*p->mPlayerSeek)->SetPosition(p->mPlayerSeek, 0, SL_SEEKMODE_FAST);
                check(result);
            }
            result = (*p->mPlayerPlay)->SetPlayState(p->mPlayerPlay, SL_PLAYSTATE_PLAYING);
            check(result);
        }
        if ((playTimeInMilliseconds > 0) && ((playTimeInMilliseconds -= delay) < 0))
            break;
    }

    for (i = 0; i < numPlayers; ++i) {
        SLObjectItf playerObject = players[i].mPlayerObject;
        (*playerObject)->Destroy(playerObject);
    }
    (*mixObject)->Destroy(mixObject);
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
