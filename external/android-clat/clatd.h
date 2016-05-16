/*
 * Copyright 2011 Daniel Drown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * clatd.h - main system definitions
 */
#ifndef __CLATD_H__
#define __CLATD_H__

#include <linux/if_tun.h>

#define MAXMTU 1500
#define PACKETLEN (MAXMTU+sizeof(struct tun_pi))
#define CLATD_VERSION "1.1"

// how frequently (in seconds) to poll for an address change while traffic is passing
#define INTERFACE_POLL_FREQUENCY 30

// how frequently (in seconds) to poll for an address change while there is no traffic
#define NO_TRAFFIC_INTERFACE_POLL_FREQUENCY 90

#endif /* __CLATD_H__ */
