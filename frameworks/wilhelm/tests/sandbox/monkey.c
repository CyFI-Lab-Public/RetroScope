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

// single-threaded, single-player monkey test

#include <SLES/OpenSLES.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef enum {
    STATE_UNCHANGED,
    STATE_INITIAL,
    STATE_NONEXISTENT,
    STATE_CREATED,
    STATE_REALIZED,
    STATE_PAUSED,
    STATE_PLAYING,
    STATE_STOPPED,
    STATE_ERROR,
//    STATE_IDLE,         // after Stop, then sleep for 3 seconds
    STATE_TERMINAL
} State_t;

typedef struct {
    SLObjectItf mObject;
    SLPlayItf mPlay;
    SLSeekItf mSeek;
} Player_t, *Player_pt;

typedef State_t (*Action_pt)(Player_pt player);

SLObjectItf engineObject;
SLEngineItf engineEngine;
SLObjectItf outputMixObject;
int countTransitions = 0;
int maxTransitions = 10;

State_t actionPause(Player_pt p)
{
    assert(NULL != p->mPlay);
    SLresult result = (*p->mPlay)->SetPlayState(p->mPlay, SL_PLAYSTATE_PAUSED);
    assert(SL_RESULT_SUCCESS == result);
    return STATE_PAUSED;
}

State_t actionPlay(Player_pt p)
{
    assert(NULL != p->mPlay);
    SLresult result = (*p->mPlay)->SetPlayState(p->mPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);
    return STATE_PLAYING;
}

State_t actionStop(Player_pt p)
{
    assert(NULL != p->mPlay);
    SLresult result = (*p->mPlay)->SetPlayState(p->mPlay, SL_PLAYSTATE_STOPPED);
    assert(SL_RESULT_SUCCESS == result);
    return STATE_STOPPED;
}

State_t actionRewind(Player_pt p)
{
    assert(NULL != p->mSeek);
    SLresult result = (*p->mSeek)->SetPosition(p->mSeek, (SLmillisecond) 0, SL_SEEKMODE_FAST);
    assert(SL_RESULT_SUCCESS == result);
    return STATE_UNCHANGED;
}

State_t actionDestroy(Player_pt p)
{
    assert(NULL != p->mObject);
    (*p->mObject)->Destroy(p->mObject);
    p->mObject = NULL;
    p->mPlay = NULL;
    p->mSeek = NULL;
    return STATE_NONEXISTENT;
}

State_t actionCreate(Player_pt p)
{
    // configure audio source
    SLDataLocator_URI loc_uri;
    loc_uri.locatorType = SL_DATALOCATOR_URI;
    loc_uri.URI = (SLchar *) "wav/frog.wav";
    SLDataFormat_MIME format_mime;
    format_mime.formatType = SL_DATAFORMAT_MIME;
    format_mime.mimeType = NULL;
    format_mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;
    SLDataSource audioSrc;
    audioSrc.pLocator = &loc_uri;
    audioSrc.pFormat = &format_mime;
    // configure audio sink
    SLDataLocator_OutputMix loc_outmix;
    loc_outmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    loc_outmix.outputMix = outputMixObject;
    SLDataSink audioSnk;
    audioSnk.pLocator = &loc_outmix;
    audioSnk.pFormat = NULL;
    // create audio player
    SLInterfaceID ids[1] = {SL_IID_SEEK};
    SLboolean req[1] = {SL_BOOLEAN_TRUE};
    SLresult result = (*engineEngine)->CreateAudioPlayer(engineEngine, &p->mObject, &audioSrc,
            &audioSnk, 1, ids, req);
    if (SL_RESULT_SUCCESS != result)
        return STATE_ERROR;
    return STATE_CREATED;
}

State_t actionRealize(Player_pt p)
{
    assert(NULL != p->mObject);
    // realize the player
    SLresult result = (*p->mObject)->Realize(p->mObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    // get interfaces
    result = (*p->mObject)->GetInterface(p->mObject, SL_IID_PLAY, &p->mPlay);
    assert(SL_RESULT_SUCCESS == result);
    result = (*p->mObject)->GetInterface(p->mObject, SL_IID_SEEK, &p->mSeek);
    assert(SL_RESULT_SUCCESS == result);
    return STATE_REALIZED;
}

State_t actionSleep(Player_pt p)
{
    unsigned us = 1000 + (rand() & 0xFFFFF);
    usleep(us);
    return STATE_UNCHANGED;
}

#if 0
State_t actionSleep3(Player_pt p)
{
    sleep(3);
    return STATE_IDLE;
}
#endif

State_t actionTerminateIfDone(Player_pt p)
{
    if (countTransitions >= maxTransitions) {
        assert(NULL == p->mObject);
        // clean up output mix and engine
        assert(NULL != outputMixObject);
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        assert(NULL != engineObject);
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        return STATE_TERMINAL;
    } else
        return STATE_UNCHANGED;
}

State_t actionInitialize(Player_pt p)
{
    // create engine
    SLresult result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);

    // create output mix
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    return STATE_NONEXISTENT;
}

typedef struct {
    State_t mEntryState;
    Action_pt mAction;
    unsigned mProbability;
    const char *mActionName;
    unsigned mCount;
} Transition_t;

Transition_t transitionTable[] = {
#define _(entryState, action, probability) {entryState, action, probability, #action, 0},
    _(STATE_INITIAL, actionInitialize, 1)
    _(STATE_CREATED, actionDestroy, 1)
    _(STATE_CREATED, actionRealize, 1)
    _(STATE_CREATED, actionSleep, 1)
    _(STATE_NONEXISTENT, actionCreate, 1)
    _(STATE_NONEXISTENT, actionSleep, 1)
    _(STATE_PAUSED, actionDestroy, 1)
    _(STATE_PAUSED, actionPause, 1)
    _(STATE_PAUSED, actionPlay, 1)
    _(STATE_PAUSED, actionRewind, 1)
    _(STATE_PAUSED, actionSleep, 1)
    _(STATE_PAUSED, actionStop, 1)
    _(STATE_PLAYING, actionDestroy, 1)
    _(STATE_PLAYING, actionPause, 1)
    _(STATE_PLAYING, actionPlay, 1)
    _(STATE_PLAYING, actionRewind, 1)
    _(STATE_PLAYING, actionSleep, 1)
    _(STATE_PLAYING, actionStop, 1)
    _(STATE_REALIZED, actionDestroy, 1)
    _(STATE_REALIZED, actionPause, 1)
    _(STATE_REALIZED, actionPlay, 1)
    _(STATE_REALIZED, actionSleep, 1)
    _(STATE_REALIZED, actionStop, 1)
    _(STATE_STOPPED, actionDestroy, 1)
    _(STATE_STOPPED, actionPause, 1)
    _(STATE_STOPPED, actionPlay, 1)
    _(STATE_STOPPED, actionRewind, 1)
    _(STATE_STOPPED, actionSleep, 1)
    _(STATE_STOPPED, actionStop, 1)
//    _(STATE_STOPPED, actionSleep3, 1)
//    _(STATE_IDLE, actionDestroy, 1)
    _(STATE_NONEXISTENT, actionTerminateIfDone, 1)
};

int main(int argc, char **argv)
{
    int i;
    for (i = 1; i < argc; ++i) {
        char *arg = argv[i];
        if (arg[0] != '-')
            break;
        if (!strncmp(arg, "-m", 2)) {
            maxTransitions = atoi(&arg[2]);
        } else {
            fprintf(stderr, "Unknown option %s\n", arg);
        }
    }
    unsigned possibleTransitions = sizeof(transitionTable) / sizeof(transitionTable[0]);
    Player_t player;
    player.mObject = NULL;
    player.mPlay = NULL;
    player.mSeek = NULL;
    State_t currentState = STATE_INITIAL;
    while (STATE_TERMINAL != currentState) {
        unsigned matchingTransitions = 0;
        unsigned totalProbability = 0;
        for (i = 0; i < (int) possibleTransitions; ++i) {
            if (currentState != transitionTable[i].mEntryState)
                continue;
            ++matchingTransitions;
            totalProbability += transitionTable[i].mProbability;
        }
        if (matchingTransitions == 0) {
            fprintf(stderr, "No matching transitions in state %d\n", currentState);
            assert(SL_BOOLEAN_FALSE);
            break;
        }
        if (totalProbability == 0) {
            fprintf(stderr, "Found at least one matching transition in state %d, "
                    "but with probability 0\n", currentState);
            assert(SL_BOOLEAN_FALSE);
            break;
        }
        unsigned choice = (rand() & 0x7FFFFFFF) % totalProbability;
        totalProbability = 0;
        for (i = 0; i < (int) possibleTransitions; ++i) {
            if (currentState != transitionTable[i].mEntryState)
                continue;
            totalProbability += transitionTable[i].mProbability;
            if (totalProbability <= choice)
                continue;
            ++transitionTable[i].mCount;
            ++countTransitions;
            printf("[%d] Selecting transition %s in state %d for the %u time\n", countTransitions,
                    transitionTable[i].mActionName, currentState, transitionTable[i].mCount);
            State_t nextState = (*transitionTable[i].mAction)(&player);
            if (STATE_UNCHANGED != nextState)
                currentState = nextState;
            goto found;
        }
        fprintf(stderr, "This shouldn't happen\n");
        assert(SL_BOOLEAN_FALSE);
found:
        ;
    }
    for (i = 0; i < (int) possibleTransitions; ++i) {
        printf("state %d action %s count %u\n",
            transitionTable[i].mEntryState,
            transitionTable[i].mActionName,
            transitionTable[i].mCount);
    }
    return EXIT_SUCCESS;
}
