// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef NET_PROXY_PROXY_CONFIG_SERVICE_ANDROID_H_
#define NET_PROXY_PROXY_CONFIG_SERVICE_ANDROID_H_
#pragma once

#include "net/base/net_export.h"
#include "net/proxy/proxy_config_service.h"

#include <string>

#include "base/observer_list.h"

namespace net {

class NET_EXPORT ProxyConfigServiceAndroid : public ProxyConfigService {
 public:
  // ProxyConfigService implementation:
  virtual void AddObserver(Observer* observer);
  virtual void RemoveObserver(Observer* observer);
  virtual ConfigAvailability GetLatestProxyConfig(ProxyConfig* config);
  virtual void OnLazyPoll() {}

  // For Android to update the proxy service config
  void UpdateProxySettings(std::string& host, std::string& exclusionList);

private:
  ObserverList<Observer> observers_;
  std::string m_proxy;
};

} // namespace net

#endif // NET_PROXY_PROXY_CONFIG_SERVICE_ANDROID_H_
