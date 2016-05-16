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
 * checksum.c - ipv4/ipv6 checksum calculation
 */
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <netinet/udp.h>
#include <netinet/tcp.h>
#include <netinet/ip6.h>
#include <netinet/icmp6.h>
#include <linux/icmp.h>

#include "checksum.h"

/* function: ip_checksum_add
 * adds data to a checksum
 * current - the current checksum (or 0 to start a new checksum)
 * data        - the data to add to the checksum
 * len         - length of data
 */
uint32_t ip_checksum_add(uint32_t current, const void *data, int len) {
  uint32_t checksum = current;
  int left = len;
  const uint16_t *data_16 = data;

  while(left > 1) {
    checksum += *data_16;
    data_16++;
    left -= 2;
  }
  if(left) {
    checksum += *(uint8_t *)data_16;
  }

  return checksum;
}

/* function: ip_checksum_finish
 * close the checksum
 * temp_sum - sum from ip_checksum_add
 */
uint16_t ip_checksum_finish(uint32_t temp_sum) {
  while(temp_sum > 0xffff)
    temp_sum = (temp_sum >> 16) + (temp_sum & 0xFFFF);

  temp_sum = (~temp_sum) & 0xffff;

  return temp_sum;
}

/* function: ip_checksum
 * combined ip_checksum_add and ip_checksum_finish
 * data - data to checksum
 * len  - length of data
 */
uint16_t ip_checksum(const void *data, int len) {
  uint32_t temp_sum;

  temp_sum = ip_checksum_add(0,data,len);
  return ip_checksum_finish(temp_sum);
}

/* function: ipv6_pseudo_header_checksum
 * calculate the pseudo header checksum for use in tcp/udp/icmp headers
 * current - the current checksum or 0 to start a new checksum
 * ip6     - the ipv6 header
 * len     - the transport length (transport header + payload)
 */
uint32_t ipv6_pseudo_header_checksum(uint32_t current, const struct ip6_hdr *ip6, uint16_t len) {
  uint32_t checksum_len, checksum_next;

  checksum_len = htonl((uint32_t) len);
  checksum_next = htonl(ip6->ip6_nxt);

  current = ip_checksum_add(current, &(ip6->ip6_src), sizeof(struct in6_addr));
  current = ip_checksum_add(current, &(ip6->ip6_dst), sizeof(struct in6_addr));
  current = ip_checksum_add(current, &checksum_len, sizeof(checksum_len));
  current = ip_checksum_add(current, &checksum_next, sizeof(checksum_next));

  return current;
}

/* function: ipv4_pseudo_header_checksum
 * calculate the pseudo header checksum for use in tcp/udp headers
 * current - the current checksum or 0 to start a new checksum
 * ip      - the ipv4 header
 * len     - the transport length (transport header + payload)
 */
uint32_t ipv4_pseudo_header_checksum(uint32_t current, const struct iphdr *ip, uint16_t len) {
  uint16_t temp_protocol, temp_length;

  temp_protocol = htons(ip->protocol);
  temp_length = htons(len);

  current = ip_checksum_add(current, &(ip->saddr), sizeof(uint32_t));
  current = ip_checksum_add(current, &(ip->daddr), sizeof(uint32_t));
  current = ip_checksum_add(current, &temp_protocol, sizeof(uint16_t));
  current = ip_checksum_add(current, &temp_length, sizeof(uint16_t));

  return current;
}
