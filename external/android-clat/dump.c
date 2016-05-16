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
 * dump.c - print various headers for debugging
 */
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>

#include <arpa/inet.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <netinet/udp.h>
#include <netinet/tcp.h>
#include <netinet/ip6.h>
#include <netinet/icmp6.h>
#include <linux/icmp.h>

#include "debug.h"
#include "checksum.h"
#include "clatd.h"
#include "logging.h"

#if CLAT_DEBUG

/* print ip header */
void dump_ip(struct iphdr *header) {
  u_int16_t frag_flags;
  char addrstr[INET6_ADDRSTRLEN];

  frag_flags = ntohs(header->frag_off);

  printf("IP packet\n");
  printf("header_len = %x\n",header->ihl);
  printf("version = %x\n",header->version);
  printf("tos = %x\n",header->tos);
  printf("tot_len = %x\n",ntohs(header->tot_len));
  printf("id = %x\n",ntohs(header->id));
  printf("frag: ");
  if(frag_flags & IP_RF) {
    printf("(RF) ");
  }
  if(frag_flags & IP_DF) {
    printf("DF ");
  }
  if(frag_flags & IP_MF) {
    printf("MF ");
  }
  printf("offset = %x\n",frag_flags & IP_OFFMASK);
  printf("ttl = %x\n",header->ttl);
  printf("protocol = %x\n",header->protocol);
  printf("checksum = %x\n",ntohs(header->check));
  inet_ntop(AF_INET, &header->saddr, addrstr, sizeof(addrstr));
  printf("saddr = %s\n",addrstr);
  inet_ntop(AF_INET, &header->daddr, addrstr, sizeof(addrstr));
  printf("daddr = %s\n",addrstr);
}

/* print ip6 header */
void dump_ip6(struct ip6_hdr *header) {
  char addrstr[INET6_ADDRSTRLEN];

  printf("ipv6\n");
  printf("version = %x\n",header->ip6_vfc >> 4);
  printf("traffic class = %x\n",header->ip6_flow >> 20);
  printf("flow label = %x\n",ntohl(header->ip6_flow & 0x000fffff));
  printf("payload len = %x\n",ntohs(header->ip6_plen));
  printf("next header = %x\n",header->ip6_nxt);
  printf("hop limit = %x\n",header->ip6_hlim);

  inet_ntop(AF_INET6, &header->ip6_src, addrstr, sizeof(addrstr));
  printf("source = %s\n",addrstr);

  inet_ntop(AF_INET6, &header->ip6_dst, addrstr, sizeof(addrstr));
  printf("dest = %s\n",addrstr);
}

/* print icmp header */
void dump_icmp(struct icmphdr *icmp) {
  printf("ICMP\n");

  printf("icmp.type = %x ",icmp->type);
  if(icmp->type == ICMP_ECHOREPLY) {
    printf("echo reply");
  } else if(icmp->type == ICMP_ECHO) {
    printf("echo request");
  } else {
    printf("other");
  }
  printf("\n");
  printf("icmp.code = %x\n",icmp->code);
  printf("icmp.checksum = %x\n",ntohs(icmp->checksum));
  if(icmp->type == ICMP_ECHOREPLY || icmp->type == ICMP_ECHO) {
    printf("icmp.un.echo.id = %x\n",ntohs(icmp->un.echo.id));
    printf("icmp.un.echo.sequence = %x\n",ntohs(icmp->un.echo.sequence));
  }
}

/* print icmp6 header */
void dump_icmp6(struct icmp6_hdr *icmp6) {
  printf("ICMP6\n");
  printf("type = %x",icmp6->icmp6_type);
  if(icmp6->icmp6_type == ICMP6_ECHO_REQUEST) {
    printf("(echo request)");
  } else if(icmp6->icmp6_type == ICMP6_ECHO_REPLY) {
    printf("(echo reply)");
  }
  printf("\n");
  printf("code = %x\n",icmp6->icmp6_code);

  printf("checksum = %x\n",icmp6->icmp6_cksum);

  if((icmp6->icmp6_type == ICMP6_ECHO_REQUEST) || (icmp6->icmp6_type == ICMP6_ECHO_REPLY)) {
    printf("icmp6_id = %x\n",icmp6->icmp6_id);
    printf("icmp6_seq = %x\n",icmp6->icmp6_seq);
  }
}

/* print udp header */
void dump_udp_generic(const struct udphdr *udp, uint32_t temp_checksum, const char *payload, size_t payload_size) {
  uint16_t my_checksum;

  temp_checksum = ip_checksum_add(temp_checksum, udp, sizeof(struct udphdr));
  temp_checksum = ip_checksum_add(temp_checksum, payload, payload_size);
  my_checksum = ip_checksum_finish(temp_checksum);

  printf("UDP\n");
  printf("source = %x\n",ntohs(udp->source));
  printf("dest = %x\n",ntohs(udp->dest));
  printf("len = %x\n",ntohs(udp->len));
  printf("check = %x (mine %x)\n",udp->check,my_checksum);
}

/* print ipv4/udp header */
void dump_udp(const struct udphdr *udp, const struct iphdr *ip, const char *payload, size_t payload_size) {
  uint32_t temp_checksum;
  temp_checksum = ipv4_pseudo_header_checksum(0, ip, sizeof(*udp) + payload_size);
  dump_udp_generic(udp, temp_checksum, payload, payload_size);
}

/* print ipv6/udp header */
void dump_udp6(const struct udphdr *udp, const struct ip6_hdr *ip6, const char *payload, size_t payload_size) {
  uint32_t temp_checksum;
  temp_checksum = ipv6_pseudo_header_checksum(0, ip6, sizeof(*udp) + payload_size);
  dump_udp_generic(udp, temp_checksum, payload, payload_size);
}

/* print tcp header */
void dump_tcp_generic(const struct tcphdr *tcp, const char *options, size_t options_size, uint32_t temp_checksum, const char *payload, size_t payload_size) {
  uint16_t my_checksum;

  temp_checksum = ip_checksum_add(temp_checksum, tcp, sizeof(struct tcphdr));
  if(options) {
    temp_checksum = ip_checksum_add(temp_checksum, options, options_size);
  }
  temp_checksum = ip_checksum_add(temp_checksum, payload, payload_size);
  my_checksum = ip_checksum_finish(temp_checksum);

  printf("TCP\n");
  printf("source = %x\n",ntohs(tcp->source));
  printf("dest = %x\n",ntohs(tcp->dest));
  printf("seq = %x\n",ntohl(tcp->seq));
  printf("ack = %x\n",ntohl(tcp->ack_seq));
  printf("d_off = %x\n",tcp->doff);
  printf("res1 = %x\n",tcp->res1);
#ifdef __BIONIC__
  printf("CWR = %x\n",tcp->cwr);
  printf("ECE = %x\n",tcp->ece);
#else
  printf("CWR/ECE = %x\n",tcp->res2);
#endif
  printf("urg = %x  ack = %x  psh = %x  rst = %x  syn = %x  fin = %x\n",
      tcp->urg, tcp->ack, tcp->psh, tcp->rst, tcp->syn, tcp->fin);
  printf("window = %x\n",ntohs(tcp->window));
  printf("check = %x [mine %x]\n",tcp->check,my_checksum);
  printf("urgent = %x\n",tcp->urg_ptr);

  if(options) {
    size_t i;

    printf("options: ");
    for(i=0; i<options_size; i++) {
      printf("%x ",*(options+i));
    }
    printf("\n");
  }
}

/* print ipv4/tcp header */
void dump_tcp(const struct tcphdr *tcp, const struct iphdr *ip, const char *payload, size_t payload_size, const char *options, size_t options_size) {
  uint32_t temp_checksum;

  temp_checksum = ipv4_pseudo_header_checksum(0, ip, sizeof(*tcp) + options_size + payload_size);
  dump_tcp_generic(tcp, options, options_size, temp_checksum, payload, payload_size);
}

/* print ipv6/tcp header */
void dump_tcp6(const struct tcphdr *tcp, const struct ip6_hdr *ip6, const char *payload, size_t payload_size, const char *options, size_t options_size) {
  uint32_t temp_checksum;

  temp_checksum = ipv6_pseudo_header_checksum(0, ip6, sizeof(*tcp) + options_size + payload_size);
  dump_tcp_generic(tcp, options, options_size, temp_checksum, payload, payload_size);
}

/* generic hex dump */
void logcat_hexdump(const char *info, const char *data, size_t len) {
  char output[PACKETLEN*3+2];
  size_t i;

  for(i = 0; i < len && i < PACKETLEN; i++) {
    snprintf(output + i*3, 4, " %02x", (uint8_t)data[i]);
  }
  output[len*3+3] = '\0';

  logmsg(ANDROID_LOG_WARN,"info %s len %d data%s", info, len, output);
}

void dump_iovec(const struct iovec *iov, int iov_len) {
  int i;
  char *str;
  for (i = 0; i < iov_len; i++) {
    asprintf(&str, "iov[%d]: ", i);
    logcat_hexdump(str, iov[i].iov_base, iov[i].iov_len);
    free(str);
  }
}
#endif  // CLAT_DEBUG
