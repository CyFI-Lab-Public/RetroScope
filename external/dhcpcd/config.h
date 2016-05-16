/* linux */
#define SYSCONFDIR	"/system/etc/dhcpcd"
#define SBINDIR		"/system/etc/dhcpcd"
#define LIBEXECDIR	"/system/etc/dhcpcd"
#define DBDIR		"/data/misc/dhcp"
#define RUNDIR		"/data/misc/dhcp"
#include "compat/arc4random.h"
#include "compat/closefrom.h"
#include "compat/strlcpy.h"
#include "compat/getline.h"

#ifndef MAX
#define MAX(a,b)	((a) >= (b) ? (a) : (b))
#endif

#ifndef MIN
#define MIN(a,b)	((a) <= (b) ? (a) : (b))
#endif
