// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "net/proxy/proxy_config_service_android.h"

#include "net/proxy/proxy_config.h"

namespace net {

void ProxyConfigServiceAndroid::AddObserver(Observer* observer) {
  observers_.AddObserver(observer);
}

void ProxyConfigServiceAndroid::RemoveObserver(Observer* observer) {
  observers_.RemoveObserver(observer);
}

ProxyConfigService::ConfigAvailability ProxyConfigServiceAndroid::GetLatestProxyConfig(ProxyConfig* config) {
  if (!config)
    return ProxyConfigService::CONFIG_UNSET;

  if (m_proxy.empty()) {
    *config = ProxyConfig::CreateDirect();
  } else {
    config->proxy_rules().ParseFromString(m_proxy);
  }
  return ProxyConfigService::CONFIG_VALID;
}

void ProxyConfigServiceAndroid::UpdateProxySettings(std::string& proxy,
                                                    std::string& exList) {
  if (proxy == m_proxy)
    return;

  m_proxy = proxy;
  ProxyConfig config;
  config.proxy_rules().ParseFromString(m_proxy);

  size_t pos;
  while ( (pos = exList.find(',')) != std::string::npos) {
    config.proxy_rules().bypass_rules.AddRuleFromString(exList.substr(0, pos));
    exList.erase(0, pos + 1);
  }
  config.proxy_rules().bypass_rules.AddRuleFromString(exList);

  FOR_EACH_OBSERVER(Observer, observers_, 
                    OnProxyConfigChanged(config,
                                         ProxyConfigService::CONFIG_VALID));
}

} // namespace net
