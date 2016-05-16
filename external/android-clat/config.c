/*
 * Copyright 2011 Daniel Drown
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
 * config.c - configuration settings
 */

#include <string.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <limits.h>
#include <errno.h>
#include <unistd.h>

#include <cutils/config_utils.h>

#include "config.h"
#include "dns64.h"
#include "logging.h"
#include "getaddr.h"
#include "clatd.h"
#include "setroute.h"

struct clat_config Global_Clatd_Config;

/* function: config_item_str
 * locates the config item and returns the pointer to a string, or NULL on failure.  Caller frees pointer
 * root       - parsed configuration
 * item_name  - name of config item to locate
 * defaultvar - value to use if config item isn't present
 */
char *config_item_str(cnode *root, const char *item_name, const char *defaultvar) {
  const char *tmp;

  if(!(tmp = config_str(root, item_name, defaultvar))) {
    logmsg(ANDROID_LOG_FATAL,"%s config item needed",item_name);
    return NULL;
  }
  return strdup(tmp);
}

/* function: config_item_int16_t
 * locates the config item, parses the integer, and returns the pointer ret_val_ptr, or NULL on failure
 * root        - parsed configuration
 * item_name   - name of config item to locate
 * defaultvar  - value to use if config item isn't present
 * ret_val_ptr - pointer for return value storage
 */
int16_t *config_item_int16_t(cnode *root, const char *item_name, const char *defaultvar, int16_t *ret_val_ptr) {
  const char *tmp;
  char *endptr;
  long int conf_int;

  if(!(tmp = config_str(root, item_name, defaultvar))) {
    logmsg(ANDROID_LOG_FATAL,"%s config item needed",item_name);
    return NULL;
  }

  errno = 0;
  conf_int = strtol(tmp,&endptr,10);
  if(errno > 0) {
    logmsg(ANDROID_LOG_FATAL,"%s config item is not numeric: %s (error=%s)",item_name,tmp,strerror(errno));
    return NULL;
  }
  if(endptr == tmp || *tmp == '\0') {
    logmsg(ANDROID_LOG_FATAL,"%s config item is not numeric: %s",item_name,tmp);
    return NULL;
  }
  if(*endptr != '\0') {
    logmsg(ANDROID_LOG_FATAL,"%s config item contains non-numeric characters: %s",item_name,endptr);
    return NULL;
  }
  if(conf_int > INT16_MAX || conf_int < INT16_MIN) {
    logmsg(ANDROID_LOG_FATAL,"%s config item is too big/small: %d",item_name,conf_int);
    return NULL;
  }
  *ret_val_ptr = conf_int;
  return ret_val_ptr;
}

/* function: config_item_ip
 * locates the config item, parses the ipv4 address, and returns the pointer ret_val_ptr, or NULL on failure
 * root        - parsed configuration
 * item_name   - name of config item to locate
 * defaultvar  - value to use if config item isn't present
 * ret_val_ptr - pointer for return value storage
 */
struct in_addr *config_item_ip(cnode *root, const char *item_name, const char *defaultvar, struct in_addr *ret_val_ptr) {
  const char *tmp;
  int status;

  if(!(tmp = config_str(root, item_name, defaultvar))) {
    logmsg(ANDROID_LOG_FATAL,"%s config item needed",item_name);
    return NULL;
  }

  status = inet_pton(AF_INET, tmp, ret_val_ptr);
  if(status <= 0) {
    logmsg(ANDROID_LOG_FATAL,"invalid IPv4 address specified for %s: %s", item_name, tmp);
    return NULL;
  }

  return ret_val_ptr;
}

/* function: config_item_ip6
 * locates the config item, parses the ipv6 address, and returns the pointer ret_val_ptr, or NULL on failure
 * root        - parsed configuration
 * item_name   - name of config item to locate
 * defaultvar  - value to use if config item isn't present
 * ret_val_ptr - pointer for return value storage
 */
struct in6_addr *config_item_ip6(cnode *root, const char *item_name, const char *defaultvar, struct in6_addr *ret_val_ptr) {
  const char *tmp;
  int status;

  if(!(tmp = config_str(root, item_name, defaultvar))) {
    logmsg(ANDROID_LOG_FATAL,"%s config item needed",item_name);
    return NULL;
  }

  status = inet_pton(AF_INET6, tmp, ret_val_ptr);
  if(status <= 0) {
    logmsg(ANDROID_LOG_FATAL,"invalid IPv6 address specified for %s: %s", item_name, tmp);
    return NULL;
  }

  return ret_val_ptr;
}

/* function: free_config
 * frees the memory used by the global config variable
 */
void free_config() {
  if(Global_Clatd_Config.plat_from_dns64_hostname) {
    free(Global_Clatd_Config.plat_from_dns64_hostname);
    Global_Clatd_Config.plat_from_dns64_hostname = NULL;
  }
}

/* function: dns64_detection
 * does dns lookups to set the plat subnet or exits on failure, waits forever for a dns response with a query backoff timer
 */
void dns64_detection() {
  int i, backoff_sleep, status;
  struct in6_addr tmp_ptr;

  backoff_sleep = 1;

  while(1) {
    status = plat_prefix(Global_Clatd_Config.plat_from_dns64_hostname,&tmp_ptr);
    if(status > 0) {
      memcpy(&Global_Clatd_Config.plat_subnet, &tmp_ptr, sizeof(struct in6_addr));
      return;
    }
    if(status < 0) {
      logmsg(ANDROID_LOG_FATAL, "dns64_detection/no dns64, giving up\n");
      exit(1);
    }
    logmsg(ANDROID_LOG_WARN, "dns64_detection failed, sleeping for %d seconds", backoff_sleep);
    sleep(backoff_sleep);
    if(backoff_sleep >= 120) {
      backoff_sleep = 120;
    } else {
      backoff_sleep *= 2;
    }
  }
}


/* function: config_generate_local_ipv6_subnet
 * generates the local ipv6 subnet when given the interface ip
 * requires config.ipv6_host_id
 * interface_ip - in: interface ip, out: local ipv6 host address
 */
void config_generate_local_ipv6_subnet(struct in6_addr *interface_ip) {
  int i;

  for(i = 2; i < 4; i++) {
    interface_ip->s6_addr32[i] = Global_Clatd_Config.ipv6_host_id.s6_addr32[i];
  }
}

/* function: subnet_from_interface
 * finds the ipv6 subnet configured on the specified interface
 * root      - parsed configuration
 * interface - network interface name
 */
int subnet_from_interface(cnode *root, const char *interface) {
  union anyip *interface_ip;

  if(!config_item_ip6(root, "ipv6_host_id", "::200:5E10:0:0", &Global_Clatd_Config.ipv6_host_id))
    return 0;

  interface_ip = getinterface_ip(interface, AF_INET6);
  if(!interface_ip) {
    logmsg(ANDROID_LOG_FATAL,"unable to find an ipv6 ip on interface %s",interface);
    return 0;
  }

  memcpy(&Global_Clatd_Config.ipv6_local_subnet, &interface_ip->ip6, sizeof(struct in6_addr));
  free(interface_ip);

  config_generate_local_ipv6_subnet(&Global_Clatd_Config.ipv6_local_subnet);

  return 1;
}

/* function: read_config
 * reads the config file and parses it into the global variable Global_Clatd_Config. returns 0 on failure, 1 on success
 * file             - filename to parse
 * uplink_interface - interface to use to reach the internet and supplier of address space
 * plat_prefix      - (optional) plat prefix to use, otherwise follow config file
 */
int read_config(const char *file, const char *uplink_interface, const char *plat_prefix) {
  cnode *root = config_node("", "");
  void *tmp_ptr = NULL;

  if(!root) {
    logmsg(ANDROID_LOG_FATAL,"out of memory");
    return 0;
  }

  memset(&Global_Clatd_Config, '\0', sizeof(Global_Clatd_Config));

  config_load_file(root, file);
  if(root->first_child == NULL) {
    logmsg(ANDROID_LOG_FATAL,"Could not read config file %s", file);
    goto failed;
  }

  strncpy(Global_Clatd_Config.default_pdp_interface, uplink_interface, sizeof(Global_Clatd_Config.default_pdp_interface));

  if(!subnet_from_interface(root,Global_Clatd_Config.default_pdp_interface))
    goto failed;

  if(!config_item_int16_t(root, "mtu", "-1", &Global_Clatd_Config.mtu))
    goto failed;

  if(!config_item_int16_t(root, "ipv4mtu", "-1", &Global_Clatd_Config.ipv4mtu))
    goto failed;

  if(!config_item_ip(root, "ipv4_local_subnet", DEFAULT_IPV4_LOCAL_SUBNET, &Global_Clatd_Config.ipv4_local_subnet))
    goto failed;

  if(plat_prefix) { // plat subnet is coming from the command line
    if(inet_pton(AF_INET6, plat_prefix, &Global_Clatd_Config.plat_subnet) <= 0) {
      logmsg(ANDROID_LOG_FATAL,"invalid IPv6 address specified for plat prefix: %s", plat_prefix);
      goto failed;
    }
  } else {
    tmp_ptr = (void *)config_item_str(root, "plat_from_dns64", "yes");
    if(!tmp_ptr || strcmp(tmp_ptr, "no") == 0) {
      free(tmp_ptr);

      if(!config_item_ip6(root, "plat_subnet", NULL, &Global_Clatd_Config.plat_subnet)) {
        logmsg(ANDROID_LOG_FATAL, "plat_from_dns64 disabled, but no plat_subnet specified");
        goto failed;
      }
    } else {
      free(tmp_ptr);

      if(!(Global_Clatd_Config.plat_from_dns64_hostname = config_item_str(root, "plat_from_dns64_hostname", DEFAULT_DNS64_DETECTION_HOSTNAME)))
        goto failed;
      dns64_detection();
    }
  }


  return 1;

failed:
  free(root);
  free_config();
  return 0;
}

/* function; dump_config
 * prints the current config
 */
void dump_config() {
  char charbuffer[INET6_ADDRSTRLEN];

  logmsg(ANDROID_LOG_DEBUG,"mtu = %d",Global_Clatd_Config.mtu);
  logmsg(ANDROID_LOG_DEBUG,"ipv4mtu = %d",Global_Clatd_Config.ipv4mtu);
  logmsg(ANDROID_LOG_DEBUG,"ipv6_local_subnet = %s",inet_ntop(AF_INET6, &Global_Clatd_Config.ipv6_local_subnet, charbuffer, sizeof(charbuffer)));
  logmsg(ANDROID_LOG_DEBUG,"ipv4_local_subnet = %s",inet_ntop(AF_INET, &Global_Clatd_Config.ipv4_local_subnet, charbuffer, sizeof(charbuffer)));
  logmsg(ANDROID_LOG_DEBUG,"plat_subnet = %s",inet_ntop(AF_INET6, &Global_Clatd_Config.plat_subnet, charbuffer, sizeof(charbuffer)));
  logmsg(ANDROID_LOG_DEBUG,"default_pdp_interface = %s",Global_Clatd_Config.default_pdp_interface);
}
