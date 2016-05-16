// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "proxy_resolver_js_bindings.h"
#include "proxy_resolver_v8.h"

#include <netdb.h>
#include <unistd.h>
#include <cstddef>
#include <memory>
#include <string>

#include "net_util.h"

namespace net {

// ProxyResolverJSBindings implementation.
class DefaultJSBindings : public ProxyResolverJSBindings {
 public:
  DefaultJSBindings() {
  }

  // Handler for "myIpAddress()".
  // TODO: Perhaps enumerate the interfaces directly, using
  // getifaddrs().
  virtual bool MyIpAddress(std::string* first_ip_address) {
    return MyIpAddressImpl(first_ip_address);
  }

  // Handler for "myIpAddressEx()".
  virtual bool MyIpAddressEx(std::string* ip_address_list) {
    return MyIpAddressExImpl(ip_address_list);
  }

  // Handler for "dnsResolve(host)".
  virtual bool DnsResolve(const std::string& host,
                          std::string* first_ip_address) {
    return DnsResolveImpl(host, first_ip_address);
  }

  // Handler for "dnsResolveEx(host)".
  virtual bool DnsResolveEx(const std::string& host,
                            std::string* ip_address_list) {
    return DnsResolveExImpl(host, ip_address_list);
  }

 private:
  bool MyIpAddressImpl(std::string* first_ip_address) {
    std::string my_hostname = GetHostName();
    if (my_hostname.empty())
      return false;
    return DnsResolveImpl(my_hostname, first_ip_address);
  }

  bool MyIpAddressExImpl(std::string* ip_address_list) {
    std::string my_hostname = GetHostName();
    if (my_hostname.empty())
      return false;
    return DnsResolveExImpl(my_hostname, ip_address_list);
  }

  bool DnsResolveImpl(const std::string& host,
                      std::string* first_ip_address) {
    struct hostent* he = gethostbyname(host.c_str());

    if (he == NULL) {
      return false;
    }
    *first_ip_address = std::string(he->h_addr);
    return true;
  }

  bool DnsResolveExImpl(const std::string& host,
                        std::string* ip_address_list) {
    struct hostent* he = gethostbyname(host.c_str());

    if (he == NULL) {
      return false;
    }
    std::string address_list_str;
    for (char** addr = &he->h_addr; *addr != NULL; ++addr) {
      if (!address_list_str.empty())
        address_list_str += ";";
      const std::string address_string = std::string(*addr);
      if (address_string.empty())
        return false;
      address_list_str += address_string;
    }
    *ip_address_list = std::string(he->h_addr);
    return true;
  }

  std::string GetHostName() {
    char buffer[256];
    if (gethostname(buffer, 256) != 0) {
      buffer[0] = '\0';
    }
    return std::string(buffer);
  }
};

// static
ProxyResolverJSBindings* ProxyResolverJSBindings::CreateDefault() {
  return new DefaultJSBindings();
}

}  // namespace net
