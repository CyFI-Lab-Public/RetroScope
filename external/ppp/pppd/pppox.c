/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include <unistd.h>
#include "pppd.h"

static int pppox_set(char **);
static int pppox_connect();
static void pppox_disconnect();

static option_t pppox_options[] = {
    {"pppox", o_special, pppox_set, "PPPoX socket", OPT_DEVNAM},
    {NULL},
};

static struct channel pppox_channel = {
    .options = pppox_options,
    .process_extra_options = NULL,
    .check_options = NULL,
    .connect = pppox_connect,
    .disconnect = pppox_disconnect,
    .establish_ppp = generic_establish_ppp,
    .disestablish_ppp = generic_disestablish_ppp,
    .send_config = NULL,
    .recv_config = NULL,
    .cleanup = NULL,
    .close = NULL,
};

static int pppox = -1;

static int pppox_set(char **argv) {
    if (!int_option(*argv, &pppox)) {
        return 0;
    }
    info("Using PPPoX (socket = %d)", pppox);
    the_channel = &pppox_channel;
    return 1;
}

static int pppox_connect() {
    return pppox;
}

static void pppox_disconnect() {
    if (pppox != -1) {
        close(pppox);
        pppox = -1;
    }
}

void pppox_init() {
    add_options(pppox_options);
}
