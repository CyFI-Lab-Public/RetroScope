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
 * mtu.c - get interface mtu
 */

#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/if.h>

#include "mtu.h"

/* function: getifmtu
 * returns the interface mtu or -1 on failure
 * ifname - interface name
 */
int getifmtu(const char *ifname) {
  int fd;
  struct ifreq if_mtu;

  fd = socket(AF_INET, SOCK_STREAM, 0);
  if(fd < 0) {
    return -1;
  }
  strncpy(if_mtu.ifr_name, ifname, IFNAMSIZ);
  if_mtu.ifr_name[IFNAMSIZ - 1] = '\0';
  if(ioctl(fd, SIOCGIFMTU, &if_mtu) < 0) {
    return -1;
  }
  return if_mtu.ifr_mtu;
}
