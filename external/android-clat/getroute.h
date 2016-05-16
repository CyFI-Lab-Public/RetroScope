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
 * getroute.h - get an ip route
 */
#ifndef __GETROUTE_H__
#define __GETROUTE_H__

// for union anyip
#include "getaddr.h"

struct default_route_data {
  int request_interface_id;
  int request_family;

  union anyip reply_gateway;
  int reply_has_gateway;
  int reply_found_route;
};

int get_default_route(struct default_route_data *default_route);

#endif
