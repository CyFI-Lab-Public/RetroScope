// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


#include <algorithm>
#include <iterator>
#include <map>

#include <fcntl.h>
#include <netdb.h>
#include <net/if.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <string.h>

#include "net_util.h"

namespace net {

#ifndef INET6_ADDRSTRLEN  /* for non IPv6 machines */
#define INET6_ADDRSTRLEN 46
#endif

bool ParseIPLiteralToNumber(const std::string& ip_literal,
                            IPAddressNumber* ip_number) {
  char buf[sizeof(struct in6_addr)];
  int size = sizeof(struct in_addr);
  int mode = AF_INET;
  if (ip_literal.find(':') != std::string::npos) {
    mode = AF_INET6;
    size = sizeof(struct in6_addr);
  }
  if (inet_pton(mode, ip_literal.c_str(), buf) != 1) {
    return false;
  }
  ip_number->resize(size);
  for (int i = 0; i < size; i++) {
    (*ip_number)[i] = buf[i];
  }
  return true;
}

IPAddressNumber ConvertIPv4NumberToIPv6Number(
    const IPAddressNumber& ipv4_number) {
  // IPv4-mapped addresses are formed by:
  // <80 bits of zeros>  + <16 bits of ones> + <32-bit IPv4 address>.
  IPAddressNumber ipv6_number;
  ipv6_number.reserve(16);
  ipv6_number.insert(ipv6_number.end(), 10, 0);
  ipv6_number.push_back(0xFF);
  ipv6_number.push_back(0xFF);
  ipv6_number.insert(ipv6_number.end(), ipv4_number.begin(), ipv4_number.end());
  return ipv6_number;
}

bool ParseCIDRBlock(const std::string& cidr_literal,
                    IPAddressNumber* ip_number,
                    size_t* prefix_length_in_bits) {
  // We expect CIDR notation to match one of these two templates:
  //   <IPv4-literal> "/" <number of bits>
  //   <IPv6-literal> "/" <number of bits>

  std::vector<std::string> parts;
  unsigned int split = cidr_literal.find('/');
  if (split == std::string::npos)
    return false;
  parts.push_back(cidr_literal.substr(0, split));
  parts.push_back(cidr_literal.substr(split + 1));
  if (parts[1].find('/') != std::string::npos)
    return false;

  // Parse the IP address.
  if (!ParseIPLiteralToNumber(parts[0], ip_number))
    return false;

  // Parse the prefix length.
  int number_of_bits = atoi(parts[1].c_str());

  // Make sure the prefix length is in a valid range.
  if (number_of_bits < 0 ||
      number_of_bits > static_cast<int>(ip_number->size() * 8))
    return false;

  *prefix_length_in_bits = static_cast<size_t>(number_of_bits);
  return true;
}

bool IPNumberMatchesPrefix(const IPAddressNumber& ip_number,
                           const IPAddressNumber& ip_prefix,
                           size_t prefix_length_in_bits) {
  // Both the input IP address and the prefix IP address should be
  // either IPv4 or IPv6.

  // In case we have an IPv6 / IPv4 mismatch, convert the IPv4 addresses to
  // IPv6 addresses in order to do the comparison.
  if (ip_number.size() != ip_prefix.size()) {
    if (ip_number.size() == 4) {
      return IPNumberMatchesPrefix(ConvertIPv4NumberToIPv6Number(ip_number),
                                   ip_prefix, prefix_length_in_bits);
    }
    return IPNumberMatchesPrefix(ip_number,
                                 ConvertIPv4NumberToIPv6Number(ip_prefix),
                                 96 + prefix_length_in_bits);
  }

  // Otherwise we are comparing two IPv4 addresses, or two IPv6 addresses.
  // Compare all the bytes that fall entirely within the prefix.
  int num_entire_bytes_in_prefix = prefix_length_in_bits / 8;
  for (int i = 0; i < num_entire_bytes_in_prefix; ++i) {
    if (ip_number[i] != ip_prefix[i])
      return false;
  }

  // In case the prefix was not a multiple of 8, there will be 1 byte
  // which is only partially masked.
  int remaining_bits = prefix_length_in_bits % 8;
  if (remaining_bits != 0) {
    unsigned char mask = 0xFF << (8 - remaining_bits);
    int i = num_entire_bytes_in_prefix;
    if ((ip_number[i] & mask) != (ip_prefix[i] & mask))
      return false;
  }

  return true;
}

}  // namespace net
