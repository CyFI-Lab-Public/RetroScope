/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _NETD_CONSTANTS_H
#define _NETD_CONSTANTS_H

#include <string>
#include <list>
#include <stdarg.h>

extern const char * const IPTABLES_PATH;
extern const char * const IP6TABLES_PATH;
extern const char * const IP_PATH;
extern const char * const TC_PATH;
extern const char * const OEM_SCRIPT_PATH;
extern const char * const ADD;
extern const char * const DEL;

enum IptablesTarget { V4, V6, V4V6 };

int execIptables(IptablesTarget target, ...);
int execIptablesSilently(IptablesTarget target, ...);
int writeFile(const char *path, const char *value, int size);
int readFile(const char *path, char *buf, int *sizep);

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(*(a)))

#endif
