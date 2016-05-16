/*
 * Copyright 2012 Daniel Drown
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
 * getaddr.c - get a locally configured address
 */
#include <netinet/in.h>
#include <strings.h>
#include <string.h>
#include <net/if.h>

#include <linux/rtnetlink.h>
#include <netlink/handlers.h>
#include <netlink/msg.h>

#include "getaddr.h"
#include "netlink_msg.h"
#include "logging.h"

// shared state between getinterface_ip and getaddr_cb
struct target {
  int family;
  unsigned int ifindex;
  union anyip ip;
  int foundip;
};

/* function: getaddr_cb
 * callback for getinterface_ip
 * msg  - netlink message
 * data - (struct target) info for which address we're looking for
 */
static int getaddr_cb(struct nl_msg *msg, void *data) {
  struct ifaddrmsg *ifa_p;
  struct rtattr *rta_p;
  int rta_len;
  struct target *targ_p = (struct target *)data;

  ifa_p = (struct ifaddrmsg *)nlmsg_data(nlmsg_hdr(msg));
  rta_p = (struct rtattr *)IFA_RTA(ifa_p);

  if(ifa_p->ifa_index != targ_p->ifindex)
    return NL_OK;

  if(ifa_p->ifa_scope != RT_SCOPE_UNIVERSE)
    return NL_OK;

  rta_len = RTM_PAYLOAD(nlmsg_hdr(msg));
  for (; RTA_OK(rta_p, rta_len); rta_p = RTA_NEXT(rta_p, rta_len)) {
    switch(rta_p->rta_type) {
      case IFA_ADDRESS:
        if((targ_p->family == AF_INET6) && !(ifa_p->ifa_flags & IFA_F_SECONDARY)) {
          memcpy(&targ_p->ip.ip6, RTA_DATA(rta_p), rta_p->rta_len - sizeof(struct rtattr));
          targ_p->foundip = 1;
          return NL_OK;
        }
        break;
      case IFA_LOCAL:
        if(targ_p->family == AF_INET) {
          memcpy(&targ_p->ip.ip4, RTA_DATA(rta_p), rta_p->rta_len - sizeof(struct rtattr));
          targ_p->foundip = 1;
          return NL_OK;
        }
        break;
    }
  }

  return NL_OK;
}

/* function: error_handler
 * error callback for getinterface_ip
 * nla  - source of the error message
 * err  - netlink message
 * arg  - (struct target) info for which address we're looking for
 */
static int error_handler(struct sockaddr_nl *nla, struct nlmsgerr *err, void *arg) {
  return NL_OK;
}

/* function: getinterface_ip
 * finds the first global non-privacy IP of the given family for the given interface, or returns NULL.  caller frees pointer
 * interface - interface to look for
 * family    - family
 */
union anyip *getinterface_ip(const char *interface, int family) {
  struct ifaddrmsg ifa;
  struct nl_cb *callbacks = NULL;
  struct target targ;
  union anyip *retval = NULL;

  targ.family = family;
  targ.foundip = 0;
  targ.ifindex = if_nametoindex(interface);
  if(targ.ifindex == 0) {
    return NULL; // interface not found
  }

  memset(&ifa, 0, sizeof(ifa));
  ifa.ifa_family = targ.family;

  callbacks = nl_cb_alloc(NL_CB_DEFAULT);
  if(!callbacks) {
    goto cleanup;
  }
  nl_cb_set(callbacks, NL_CB_VALID, NL_CB_CUSTOM, getaddr_cb, &targ);
  nl_cb_err(callbacks, NL_CB_CUSTOM, error_handler, &targ);

  // sends message and waits for a response
  send_ifaddrmsg(RTM_GETADDR, NLM_F_REQUEST | NLM_F_ROOT, &ifa, callbacks);

  if(targ.foundip) {
    retval = malloc(sizeof(union anyip));
    if(!retval) {
      logmsg(ANDROID_LOG_FATAL,"getinterface_ip/out of memory");
      goto cleanup;
    }
    memcpy(retval, &targ.ip, sizeof(union anyip));
  }

cleanup:
  if(callbacks)
    nl_cb_put(callbacks);

  return retval;
}
