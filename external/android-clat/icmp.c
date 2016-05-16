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

#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <netinet/icmp6.h>
#include <linux/icmp.h>

#include "logging.h"
#include "icmp.h"

/* function: icmp_guess_ttl
 * Guesses the number of hops a received packet has traversed based on its TTL.
 * ttl - the ttl of the received packet.
 */
uint8_t icmp_guess_ttl(uint8_t ttl) {
  if (ttl > 128) {
    return 255 - ttl;
  } else if (ttl > 64) {
    return 128 - ttl;
  } else if (ttl > 32) {
    return 64 - ttl;
  } else {
    return 32 - ttl;
  }
}

/* function: is_icmp_error
 * Determines whether an ICMP type is an error message.
 * type: the ICMP type
 */
int is_icmp_error(uint8_t type) {
  return type == 3 || type == 11 || type == 12;
}

/* function: is_icmp6_error
 * Determines whether an ICMPv6 type is an error message.
 * type: the ICMPv6 type
 */
int is_icmp6_error(uint8_t type) {
  return type < 128;
}

/* function: icmp_to_icmp6_type
 * Maps ICMP types to ICMPv6 types. Partial implementation of RFC 6145, section 4.2.
 * type - the ICMPv6 type
 */
uint8_t icmp_to_icmp6_type(uint8_t type, uint8_t code) {
  switch (type) {
    case ICMP_ECHO:
      return ICMP6_ECHO_REQUEST;

    case ICMP_ECHOREPLY:
      return ICMP6_ECHO_REPLY;

    case ICMP_TIME_EXCEEDED:
      return ICMP6_TIME_EXCEEDED;

    case ICMP_DEST_UNREACH:
      // These two types need special translation which we don't support yet.
      if (code != ICMP_UNREACH_PROTOCOL && code != ICMP_UNREACH_NEEDFRAG) {
        return ICMP6_DST_UNREACH;
      }
  }

  // We don't understand this ICMP type. Return parameter problem so the caller will bail out.
  logmsg_dbg(ANDROID_LOG_DEBUG, "icmp_to_icmp6_type: unhandled ICMP type %d", type);
  return ICMP6_PARAM_PROB;
}

/* function: icmp_to_icmp6_code
 * Maps ICMP codes to ICMPv6 codes. Partial implementation of RFC 6145, section 4.2.
 * type - the ICMP type
 * code - the ICMP code
 */
uint8_t icmp_to_icmp6_code(uint8_t type, uint8_t code) {
  switch (type) {
    case ICMP_ECHO:
    case ICMP_ECHOREPLY:
      return 0;

    case ICMP_TIME_EXCEEDED:
      return code;

    case ICMP_DEST_UNREACH:
      switch (code) {
        case ICMP_UNREACH_NET:
        case ICMP_UNREACH_HOST:
          return ICMP6_DST_UNREACH_NOROUTE;

        case ICMP_UNREACH_PORT:
          return ICMP6_DST_UNREACH_NOPORT;

        case ICMP_UNREACH_NET_PROHIB:
        case ICMP_UNREACH_HOST_PROHIB:
        case ICMP_UNREACH_FILTER_PROHIB:
        case ICMP_UNREACH_PRECEDENCE_CUTOFF:
          return ICMP6_DST_UNREACH_ADMIN;

        // Otherwise, we don't understand this ICMP type/code combination. Fall through.
      }
  }
  logmsg_dbg(ANDROID_LOG_DEBUG, "icmp_to_icmp6_code: unhandled ICMP type/code %d/%d", type, code);
  return 0;
}

/* function: icmp6_to_icmp_type
 * Maps ICMPv6 types to ICMP types. Partial implementation of RFC 6145, section 5.2.
 * type - the ICMP type
 */
uint8_t icmp6_to_icmp_type(uint8_t type, uint8_t code) {
  switch (type) {
    case ICMP6_ECHO_REQUEST:
      return ICMP_ECHO;

    case ICMP6_ECHO_REPLY:
      return ICMP_ECHOREPLY;

    case ICMP6_DST_UNREACH:
      return ICMP_DEST_UNREACH;

    case ICMP6_TIME_EXCEEDED:
      return ICMP_TIME_EXCEEDED;
  }

  // We don't understand this ICMP type. Return parameter problem so the caller will bail out.
  logmsg_dbg(ANDROID_LOG_DEBUG, "icmp6_to_icmp_type: unhandled ICMP type %d", type);
  return ICMP_PARAMETERPROB;
}

/* function: icmp6_to_icmp_code
 * Maps ICMPv6 codes to ICMP codes. Partial implementation of RFC 6145, section 5.2.
 * type - the ICMPv6 type
 * code - the ICMPv6 code
 */
uint8_t icmp6_to_icmp_code(uint8_t type, uint8_t code) {
  switch (type) {
    case ICMP6_ECHO_REQUEST:
    case ICMP6_ECHO_REPLY:
    case ICMP6_TIME_EXCEEDED:
      return code;

    case ICMP6_DST_UNREACH:
      switch (code) {
        case ICMP6_DST_UNREACH_NOROUTE:
          return ICMP_UNREACH_HOST;

        case ICMP6_DST_UNREACH_ADMIN:
          return ICMP_UNREACH_HOST_PROHIB;

        case ICMP6_DST_UNREACH_BEYONDSCOPE:
          return ICMP_UNREACH_HOST;

        case ICMP6_DST_UNREACH_ADDR:
          return ICMP_HOST_UNREACH;

        case ICMP6_DST_UNREACH_NOPORT:
          return ICMP_UNREACH_PORT;

        // Otherwise, we don't understand this ICMPv6 type/code combination. Fall through.
      }
  }

  logmsg_dbg(ANDROID_LOG_DEBUG, "icmp6_to_icmp_code: unhandled ICMP type/code %d/%d", type, code);
  return 0;
}
