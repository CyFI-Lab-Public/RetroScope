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
 * setroute.h - network route configuration
 */

#ifndef __SETROUTE_H__
#define __SETROUTE_H__

#define ROUTE_DELETE 0
#define ROUTE_REPLACE 1
#define ROUTE_CREATE 2
int if_route(const char *ifname, int family, const void *destination, int cidr, const void *gateway, int metric, int mtu, int change_type);

#endif
