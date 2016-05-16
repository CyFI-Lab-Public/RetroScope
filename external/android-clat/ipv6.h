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
 * ipv6.h - takes an ipv6 packet and hands it off to the proper translate function
 */
#ifndef __IPV6_H__
#define __IPV6_H__

#include "translate.h"

int ipv6_packet(clat_packet out, int pos, const char *packet, size_t len);

#endif /* __IPV6_H__ */
