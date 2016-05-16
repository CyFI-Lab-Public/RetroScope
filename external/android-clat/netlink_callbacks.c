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
 * netlink_callbacks.c - generic callbacks for netlink responses
 */
#include <netinet/in.h>
#include <net/if.h>

#include <linux/rtnetlink.h>
#include <netlink/handlers.h>
#include <netlink/msg.h>

/* function: ack_handler
 * generic netlink callback for ack messages
 * msg  - netlink message
 * data - pointer to an int, stores the success code
 */
static int ack_handler(struct nl_msg *msg, void *data) {
  int *retval = data;
  *retval = 0;
  return NL_OK;
}

/* function: error_handler
 * generic netlink callback for error messages
 * nla  - error source
 * err  - netlink error message
 * arg  - pointer to an int, stores the error code
 */
static int error_handler(struct sockaddr_nl *nla, struct nlmsgerr *err, void *arg) {
  int *retval = arg;
  if(err->error < 0) {
    *retval = err->error;
  } else {
    *retval = 0; // NLMSG_ERROR used as reply type on no error
  }
  return NL_OK;
}

/* function: alloc_ack_callbacks
 * allocates a set of netlink callbacks.  returns NULL on failure.  callbacks will modify retval with <0 meaning failure
 * retval - shared state between caller and callback functions
 */
struct nl_cb *alloc_ack_callbacks(int *retval) {
  struct nl_cb *callbacks;

  callbacks = nl_cb_alloc(NL_CB_DEFAULT);
  if(!callbacks) {
    return NULL;
  }
  nl_cb_set(callbacks, NL_CB_ACK, NL_CB_CUSTOM, ack_handler, retval);
  nl_cb_err(callbacks, NL_CB_CUSTOM, error_handler, retval);
  return callbacks;
}
