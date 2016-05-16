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
 *
 * icmp.c - convenience functions for translating ICMP and ICMPv6 packets.
 */

#ifndef __ICMP_H__
#define __ICMP_H__

#include <stdint.h>

// Guesses the number of hops a received packet has traversed based on its TTL.
uint8_t icmp_guess_ttl(uint8_t ttl);

// Determines whether an ICMP type is an error message.
int is_icmp_error(uint8_t type);

// Determines whether an ICMPv6 type is an error message.
int is_icmp6_error(uint8_t type);

// Maps ICMP types to ICMPv6 types. Partial implementation of RFC 6145, section 4.2.
uint8_t icmp_to_icmp6_type(uint8_t type, uint8_t code);

// Maps ICMP codes to ICMPv6 codes. Partial implementation of RFC 6145, section 4.2.
uint8_t icmp_to_icmp6_code(uint8_t type, uint8_t code);

// Maps ICMPv6 types to ICMP types. Partial implementation of RFC 6145, section 5.2.
uint8_t icmp6_to_icmp_type(uint8_t type, uint8_t code);

// Maps ICMPv6 codes to ICMP codes. Partial implementation of RFC 6145, section 5.2.
uint8_t icmp6_to_icmp_code(uint8_t type, uint8_t code);

#endif /* __ICMP_H__ */
