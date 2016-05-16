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
 * netlink_msg.c - send an ifaddrmsg/ifinfomsg/rtmsg via netlink
 */

#include <netinet/in.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <string.h>
#include <errno.h>

#include <netlink-types.h>
#include <netlink/socket.h>
#include <netlink/netlink.h>
#include <netlink/msg.h>

#include "netlink_msg.h"
#include "netlink_callbacks.h"

/* function: family_size
 * returns the size of the address structure for the given family, or 0 on error
 * family - AF_INET or AF_INET6
 */
size_t inet_family_size(int family) {
  if(family == AF_INET) {
    return sizeof(struct in_addr);
  } else if(family == AF_INET6) {
    return sizeof(struct in6_addr);
  } else {
    return 0;
  }
}

/* function: nlmsg_alloc_generic
 * allocates a netlink message with the given struct inside of it. returns NULL on failure
 * type           - netlink message type
 * flags          - netlink message flags
 * payload_struct - pointer to a struct to add to netlink message
 * payload_len    - bytelength of structure
 */
struct nl_msg *nlmsg_alloc_generic(uint16_t type, uint16_t flags, void *payload_struct, size_t payload_len) {
  struct nl_msg *msg;

  msg = nlmsg_alloc();
  if(!msg) {
    return NULL;
  }

  if ((sizeof(struct nl_msg) + payload_len) > msg->nm_size) {
    nlmsg_free(msg);
    return NULL;
  }

  msg->nm_nlh->nlmsg_len = NLMSG_LENGTH(payload_len);
  msg->nm_nlh->nlmsg_flags = flags;
  msg->nm_nlh->nlmsg_type = type;

  memcpy(nlmsg_data(msg->nm_nlh), payload_struct, payload_len);

  return msg;
}

/* function: nlmsg_alloc_ifaddr
 * allocates a netlink message with a struct ifaddrmsg inside of it. returns NULL on failure
 * type  - netlink message type
 * flags - netlink message flags
 * ifa   - ifaddrmsg to copy into the new netlink message
 */
struct nl_msg *nlmsg_alloc_ifaddr(uint16_t type, uint16_t flags, struct ifaddrmsg *ifa) {
  return nlmsg_alloc_generic(type, flags, ifa, sizeof(*ifa));
}

/* function: nlmsg_alloc_ifinfo
 * allocates a netlink message with a struct ifinfomsg inside of it. returns NULL on failure
 * type  - netlink message type
 * flags - netlink message flags
 * ifi   - ifinfomsg to copy into the new netlink message
 */
struct nl_msg *nlmsg_alloc_ifinfo(uint16_t type, uint16_t flags, struct ifinfomsg *ifi) {
  return nlmsg_alloc_generic(type, flags, ifi, sizeof(*ifi));
}

/* function: nlmsg_alloc_rtmsg
 * allocates a netlink message with a struct rtmsg inside of it. returns NULL on failure
 * type  - netlink message type
 * flags - netlink message flags
 * rt    - rtmsg to copy into the new netlink message
 */
struct nl_msg *nlmsg_alloc_rtmsg(uint16_t type, uint16_t flags, struct rtmsg *rt) {
  return nlmsg_alloc_generic(type, flags, rt, sizeof(*rt));
}

/* function: netlink_set_kernel_only
 * sets a socket to receive messages only from the kernel
 * sock - socket to connect
 */
int netlink_set_kernel_only(struct nl_sock *nl_sk) {
  struct sockaddr_nl addr = { AF_NETLINK, 0, 0, 0 };

  if (!nl_sk) {
    return -EFAULT;
  }

  int sockfd = nl_socket_get_fd(nl_sk);
  return connect(sockfd, (struct sockaddr *) &addr, sizeof(addr));
}

/* function: send_netlink_msg
 * sends a netlink message, reads a response, and hands the response(s) to the callbacks
 * msg       - netlink message to send
 * callbacks - callbacks to use on responses
 */
void send_netlink_msg(struct nl_msg *msg, struct nl_cb *callbacks) {
  struct nl_sock *nl_sk;

  nl_sk = nl_socket_alloc();
  if(!nl_sk)
    goto cleanup;

  if(nl_connect(nl_sk, NETLINK_ROUTE) != 0)
    goto cleanup;

  if(nl_send_auto_complete(nl_sk, msg) < 0)
    goto cleanup;

  if(netlink_set_kernel_only(nl_sk) < 0)
    goto cleanup;

  nl_recvmsgs(nl_sk, callbacks);

cleanup:
  if(nl_sk)
    nl_socket_free(nl_sk);
}

/* function: send_ifaddrmsg
 * sends a netlink/ifaddrmsg message and hands the responses to the callbacks
 * type      - netlink message type
 * flags     - netlink message flags
 * ifa       - ifaddrmsg to send
 * callbacks - callbacks to use with the responses
 */
void send_ifaddrmsg(uint16_t type, uint16_t flags, struct ifaddrmsg *ifa, struct nl_cb *callbacks) {
  struct nl_msg *msg = NULL;

  msg = nlmsg_alloc_ifaddr(type, flags, ifa);
  if(!msg)
    return;

  send_netlink_msg(msg, callbacks);

  nlmsg_free(msg);
}

/* function: netlink_sendrecv
 * send a nl_msg and return an int status - only supports OK/ERROR responses
 * msg - msg to send
 */
int netlink_sendrecv(struct nl_msg *msg) {
  struct nl_cb *callbacks = NULL;
  int retval = -EIO;

  callbacks = alloc_ack_callbacks(&retval);
  if(!callbacks) {
    return -ENOMEM;
  }

  send_netlink_msg(msg, callbacks);

  nl_cb_put(callbacks);

  return retval;
}
