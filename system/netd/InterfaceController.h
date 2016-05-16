/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _INTERFACE_CONTROLLER_H
#define _INTERFACE_CONTROLLER_H

#include <linux/in.h>
#include <net/if.h>

typedef struct android_wifi_priv_cmd {
	char *buf;
	int used_len;
	int total_len;
} android_wifi_priv_cmd;

#define INTERFACE_MAX_BUFFER_SIZE	256

class InterfaceController {
 public:
	InterfaceController();
	virtual ~InterfaceController();
	int interfaceCommand(int argc, char *argv[], char **rbuf);
	int setEnableIPv6(const char *interface, const int on);
	int setIPv6PrivacyExtensions(const char *interface, const int on);
	int getMtu(const char *interface, int *mtu);
	int setMtu(const char *interface, const char *mtu);

 private:
	void *libh_;
	int (*sendCommand_)(int argc, char *argv[], char **rbuf);
	int (*sendCommandInit_)(void);
	int (*sendCommandFini_)(void);
	int writeIPv6ProcPath(const char *interface, const char *setting,
			      const char *value);
        int isInterfaceName(const char *name);
        int setAcceptRA(const char *value);
};

#endif
