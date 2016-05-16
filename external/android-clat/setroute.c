/*
 * Copyright 2012 Daniel Drown <dan-android@drown.org>
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
 * setroute.c - network route configuration
 */
#include <errno.h>
#include <netinet/in.h>
#include <net/if.h>

#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <netlink/handlers.h>
#include <netlink/msg.h>
#include <netlink-types.h>

#include "netlink_msg.h"
#include "setroute.h"
#include "logging.h"
#include "getroute.h"

/* function: if_route
 * create/replace/delete a route
 * ifname      - name of the outbound interface
 * family      - AF_INET or AF_INET6
 * destination - pointer to a struct in_addr or in6_addr for the destination network
 * prefixlen   - bitlength of the network address (example: 24 for AF_INET's 255.255.255.0)
 * gateway     - pointer to a struct in_addr or in6_addr for the gateway to use or NULL for an interface route
 * metric      - route metric (lower is better)
 * mtu         - route-specific mtu or 0 for the interface mtu
 * change_type - ROUTE_DELETE, ROUTE_REPLACE, or ROUTE_CREATE
 */
int if_route(const char *ifname, int family, const void *destination, int prefixlen, const void *gateway, int metric, int mtu, int change_type) {
  int retval;
  struct nl_msg *msg = NULL;
  struct rtmsg rt;
  uint16_t type, flags = 0;
  size_t addr_size;
  uint32_t ifindex;

  addr_size = inet_family_size(family);
  if(addr_size == 0) {
    retval = -EAFNOSUPPORT;
    goto cleanup;
  }

  if (!(ifindex = if_nametoindex(ifname))) {
    retval = -ENODEV;
    goto cleanup;
  }

  memset(&rt, 0, sizeof(rt));
  rt.rtm_family = family;
  rt.rtm_table = RT_TABLE_MAIN;
  rt.rtm_dst_len = prefixlen;
  switch(change_type) {
    case ROUTE_DELETE:
      rt.rtm_scope = RT_SCOPE_NOWHERE;
      type = RTM_DELROUTE;
      break;

    case ROUTE_REPLACE:
      flags = NLM_F_REPLACE;
    case ROUTE_CREATE:
      type = RTM_NEWROUTE;
      flags |= NLM_F_CREATE;
      if(gateway == NULL) {
        rt.rtm_scope = RT_SCOPE_LINK;
      } else {
        rt.rtm_scope = RT_SCOPE_UNIVERSE;
      }
      rt.rtm_type = RTN_UNICAST;
      //RTPROT_STATIC = from administrator's configuration
      //RTPROT_BOOT = from an automatic process
      rt.rtm_protocol = RTPROT_BOOT;
      break;

    default:
      retval = -EINVAL;
      goto cleanup;
  }

  flags |= NLM_F_REQUEST | NLM_F_ACK;

  msg = nlmsg_alloc_rtmsg(type, flags, &rt);
  if(!msg) {
    retval = -ENOMEM;
    goto cleanup;
  }

  if(nla_put(msg, RTA_DST, addr_size, destination) < 0) {
    retval = -ENOMEM;
    goto cleanup;
  }
  if(gateway != NULL)
    if(nla_put(msg, RTA_GATEWAY, addr_size, gateway) < 0) {
      retval = -ENOMEM;
      goto cleanup;
    }
  if(nla_put(msg, RTA_OIF, 4, &ifindex) < 0) {
    retval = -ENOMEM;
    goto cleanup;
  }
  if(nla_put(msg, RTA_PRIORITY, 4, &metric) < 0) {
    retval = -ENOMEM;
    goto cleanup;
  }
  if(mtu > 0 && change_type != ROUTE_DELETE) {
    // MTU is inside an RTA_METRICS nested message
    struct nlattr *metrics = nla_nest_start(msg, RTA_METRICS);
    if(metrics == NULL) {
      retval = -ENOMEM;
      goto cleanup;
    }

    if(nla_put(msg, RTAX_MTU, 4, &mtu) < 0) {
      retval = -ENOMEM;
      goto cleanup;
    }

    nla_nest_end(msg, metrics);
  }

  retval = netlink_sendrecv(msg);

cleanup:
  if(msg)
    nlmsg_free(msg);

  return retval;
}
