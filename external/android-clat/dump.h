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
 * dump.h - debug functions
 */
#ifndef __DUMP_H__
#define __DUMP_H__

void dump_ip(struct iphdr *header);
void dump_icmp(struct icmphdr *icmp);
void dump_udp(const struct udphdr *udp, const struct iphdr *ip, const char *payload, size_t payload_size);
void dump_tcp(const struct tcphdr *tcp, const struct iphdr *ip, const char *payload, size_t payload_size, const char *options, size_t options_size);

void dump_ip6(struct ip6_hdr *header);
void dump_icmp6(struct icmp6_hdr *icmp6);
void dump_udp6(const struct udphdr *udp, const struct ip6_hdr *ip6, const char *payload, size_t payload_size);
void dump_tcp6(const struct tcphdr *tcp, const struct ip6_hdr *ip6, const char *payload, size_t payload_size, const char *options, size_t options_size);

void logcat_hexdump(const char *info, const char *data, size_t len);
void dump_iovec(const struct iovec *iov, int iov_len);

#endif /* __DUMP_H__ */
