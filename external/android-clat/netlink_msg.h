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
 * netlink_msg.h - send an ifaddrmsg/ifinfomsg via netlink
 */
#ifndef __NETLINK_IFMSG_H__
#define __NETLINK_IFMSG_H__

size_t inet_family_size(int family);
struct nl_msg *nlmsg_alloc_ifaddr(uint16_t type, uint16_t flags, struct ifaddrmsg *ifa);
struct nl_msg *nlmsg_alloc_ifinfo(uint16_t type, uint16_t flags, struct ifinfomsg *ifi);
struct nl_msg *nlmsg_alloc_rtmsg(uint16_t type, uint16_t flags, struct rtmsg *rt);
void send_netlink_msg(struct nl_msg *msg, struct nl_cb *callbacks);
void send_ifaddrmsg(uint16_t type, uint16_t flags, struct ifaddrmsg *ifa, struct nl_cb *callbacks);
int netlink_sendrecv(struct nl_msg *msg);
int netlink_set_kernel_only(struct nl_sock *nl_sk);

#endif
