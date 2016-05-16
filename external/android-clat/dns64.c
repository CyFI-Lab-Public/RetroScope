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
 * dns64.c - find the nat64 prefix with a dns64 lookup
 */

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <strings.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "dns64.h"
#include "logging.h"

/* function: plat_prefix
 * looks up an ipv4-only hostname and looks for a nat64 /96 prefix, returns 1 on success, 0 on temporary failure, -1 on permanent failure
 * ipv4_name - name to lookup
 * prefix    - the plat /96 prefix
 */
int plat_prefix(const char *ipv4_name, struct in6_addr *prefix) {
  struct addrinfo hints, *result, *p;
  int status, plat_addr_set, ipv4_records, ipv6_records;
  struct in6_addr plat_addr, this_plat_addr;
  struct sockaddr_in6 *this_addr;
  char plat_addr_str[INET6_ADDRSTRLEN];

  logmsg(ANDROID_LOG_INFO, "Detecting NAT64 prefix from DNS...");

  result = NULL;
  plat_addr_set = 0;
  ipv4_records = ipv6_records = 0;

  bzero(&hints, sizeof(hints));
  hints.ai_family = AF_UNSPEC;
  status = getaddrinfo(ipv4_name, NULL, &hints, &result);
  if(status != 0) {
    logmsg(ANDROID_LOG_ERROR,"plat_prefix/dns(%s) status = %d/%s\n", ipv4_name, status, gai_strerror(status));
    return 0;
  }

  for(p = result; p; p = p->ai_next) {
    if(p->ai_family == AF_INET) {
      ipv4_records++;
      continue;
    }
    if(p->ai_family != AF_INET6) {
      logmsg(ANDROID_LOG_WARN,"plat_prefix/unexpected address family: %d\n", p->ai_family);
      continue;
    }
    ipv6_records++;
    this_addr = (struct sockaddr_in6 *)p->ai_addr;
    this_plat_addr = this_addr->sin6_addr;
    this_plat_addr.s6_addr32[3] = 0;

    if(!plat_addr_set) {
      plat_addr = this_plat_addr;
      plat_addr_set = 1;
      continue;
    }

    inet_ntop(AF_INET6, &plat_addr, plat_addr_str, sizeof(plat_addr_str));
    if(!IN6_ARE_ADDR_EQUAL(&plat_addr, &this_plat_addr)) {
      char this_plat_addr_str[INET6_ADDRSTRLEN];
      inet_ntop(AF_INET6, &this_plat_addr, this_plat_addr_str, sizeof(this_plat_addr_str));
      logmsg(ANDROID_LOG_ERROR,"plat_prefix/two different plat addrs = %s,%s",
             plat_addr_str,this_plat_addr_str);
    }
  }
  if(result != NULL) {
    freeaddrinfo(result);
  }
  if(ipv4_records > 0 && ipv6_records == 0) {
    logmsg(ANDROID_LOG_WARN,"plat_prefix/no dns64 detected\n");
    return -1;
  }

  logmsg(ANDROID_LOG_INFO, "Detected NAT64 prefix %s/96", plat_addr_str);
  *prefix = plat_addr;
  return 1;
}
