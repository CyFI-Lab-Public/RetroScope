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
 * getroute.c - get an ip route
 */
#include <string.h>
#include <errno.h>

#include <sys/socket.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <arpa/inet.h>

#include <netlink/handlers.h>
#include <netlink/msg.h>

#include "getroute.h"
#include "netlink_callbacks.h"
#include "netlink_msg.h"

/* function: get_default_route_cb
 * finds the default route with the request family and out interface and saves the gateway
 * msg  - netlink message
 * data - (struct default_route_data) requested filters and response storage
 */
static int get_default_route_cb(struct nl_msg *msg, void *data) {
  struct rtmsg *rt_p;
  struct rtattr *rta_p;
  int rta_len;
  struct default_route_data *default_route = data;
  union anyip *this_gateway = NULL;
  ssize_t this_gateway_size;
  int this_interface_id = -1;

  if(default_route->reply_found_route) { // we already found our route
    return NL_OK;
  }

  rt_p = (struct rtmsg *)nlmsg_data(nlmsg_hdr(msg));
  if(rt_p->rtm_dst_len != 0) { // not a default route
    return NL_OK;
  }
  if((rt_p->rtm_family != default_route->request_family) || (rt_p->rtm_table != RT_TABLE_MAIN)) { // not a route we care about
    return NL_OK;
  }

  rta_p = (struct rtattr *)RTM_RTA(rt_p);
  rta_len = RTM_PAYLOAD(nlmsg_hdr(msg));
  for(; RTA_OK(rta_p, rta_len); rta_p = RTA_NEXT(rta_p, rta_len)) {
    switch(rta_p->rta_type) {
      case RTA_GATEWAY:
        this_gateway = RTA_DATA(rta_p);
        this_gateway_size = RTA_PAYLOAD(rta_p);
        break;
      case RTA_OIF:
        this_interface_id = *(int *)RTA_DATA(rta_p);
        break;
      default:
        break;
    }
  }

  if(this_interface_id == default_route->request_interface_id) {
    default_route->reply_found_route = 1;
    if(this_gateway != NULL) {
      memcpy(&default_route->reply_gateway, this_gateway, this_gateway_size);
      default_route->reply_has_gateway = 1;
    } else {
      default_route->reply_has_gateway = 0;
    }
  }
  return NL_OK;
}

/* function: error_handler
 * error callback for get_default_route
 * nla  - where the message came from
 * err  - netlink message
 * arg  - (int *) storage for the error number
 */
static int error_handler(struct sockaddr_nl *nla, struct nlmsgerr *err, void *arg) {
  int *retval = arg;
  if(err->error < 0) { // error_handler called even on no error (NLMSG_ERROR reply type used)
    *retval = err->error;
  }
  return NL_OK;
}

/* function: get_default_route
 * finds the first default route with the given family and interface, returns the gateway (if it exists) in the struct
 * default_route - requested family and interface, and response storage
 */
int get_default_route(struct default_route_data *default_route) {
  struct rtmsg msg;
  struct nl_cb *callbacks = NULL;
  struct nl_msg *nlmsg = NULL;
  int retval = 0;

  default_route->reply_has_gateway = 0;
  default_route->reply_found_route = 0;

  memset(&msg,'\0',sizeof(msg));
  msg.rtm_family = default_route->request_family;
  msg.rtm_table = RT_TABLE_MAIN;
  msg.rtm_protocol = RTPROT_KERNEL;
  msg.rtm_scope = RT_SCOPE_UNIVERSE;

  callbacks = nl_cb_alloc(NL_CB_DEFAULT);
  if(!callbacks) {
    retval = -ENOMEM;
    goto cleanup;
  }
  // get_default_route_cb sets the response fields in default_route
  nl_cb_set(callbacks, NL_CB_VALID, NL_CB_CUSTOM, get_default_route_cb, default_route);
  nl_cb_err(callbacks, NL_CB_CUSTOM, error_handler, &retval);

  nlmsg = nlmsg_alloc_rtmsg(RTM_GETROUTE, NLM_F_REQUEST | NLM_F_ROOT, &msg);
  if(!nlmsg) {
    retval = -ENOMEM;
    goto cleanup;
  }
  send_netlink_msg(nlmsg, callbacks);

cleanup:
  if(callbacks)
    nl_cb_put(callbacks);
  if(nlmsg)
    nlmsg_free(nlmsg);

  return retval;
}
