#include "includes.h"
#include <sys/ioctl.h>
#include <net/if_arp.h>
#include <net/if.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <netlink/genl/genl.h>
#include <netlink/genl/family.h>
#include <netlink/genl/ctrl.h>
#include <netlink/msg.h>
#include <netlink/attr.h>

#include "linux_wext.h"
#include "common.h"
#include "driver.h"
#include "eloop.h"
#include "driver_wext.h"
#include "ieee802_11_defs.h"
#include "wpa_common.h"
#include "wpa_ctrl.h"
#include "wpa_supplicant_i.h"
#include "config_ssid.h"
#include "wpa_debug.h"
#include "linux_ioctl.h"
#include "hardware_legacy/driver_nl80211.h"

#define WPA_EVENT_DRIVER_STATE          "CTRL-EVENT-DRIVER-STATE "
#define DRV_NUMBER_SEQUENTIAL_ERRORS     4

#define BLUETOOTH_COEXISTENCE_MODE_ENABLED   0
#define BLUETOOTH_COEXISTENCE_MODE_DISABLED  1
#define BLUETOOTH_COEXISTENCE_MODE_SENSE     2

static int g_drv_errors = 0;

static void wpa_driver_send_hang_msg(struct wpa_driver_nl80211_data *drv)
{
	g_drv_errors++;
	if (g_drv_errors > DRV_NUMBER_SEQUENTIAL_ERRORS) {
		g_drv_errors = 0;
		wpa_msg(drv->ctx, MSG_INFO, WPA_EVENT_DRIVER_STATE "HANGED");
	}
}

static int wpa_driver_toggle_btcoex_state(char state)
{
	int ret;
	int fd = open("/sys/devices/platform/wl1271/bt_coex_state", O_RDWR, 0);
	if (fd == -1)
		return -1;

	ret = write(fd, &state, sizeof(state));
	close(fd);

	wpa_printf(MSG_DEBUG, "%s:  set btcoex state to '%c' result = %d", __func__,
		   state, ret);
	return ret;
}

int wpa_driver_nl80211_driver_cmd(void *priv, char *cmd, char *buf,
				  size_t buf_len )
{
	struct i802_bss *bss = priv;
	struct wpa_driver_nl80211_data *drv = bss->drv;
	struct ifreq ifr;
	int ret = 0;

	if (os_strcasecmp(cmd, "STOP") == 0) {
		linux_set_iface_flags(drv->global->ioctl_sock, bss->ifname, 0);
		wpa_msg(drv->ctx, MSG_INFO, WPA_EVENT_DRIVER_STATE "STOPPED");
	} else if (os_strcasecmp(cmd, "START") == 0) {
		linux_set_iface_flags(drv->global->ioctl_sock, bss->ifname, 1);
		wpa_msg(drv->ctx, MSG_INFO, WPA_EVENT_DRIVER_STATE "STARTED");
	} else if (os_strcasecmp(cmd, "RELOAD") == 0) {
		wpa_msg(drv->ctx, MSG_INFO, WPA_EVENT_DRIVER_STATE "HANGED");
	} else if (os_strncasecmp(cmd, "BTCOEXMODE ", 11) == 0) {
		int mode = atoi(cmd + 11);
		if (mode == BLUETOOTH_COEXISTENCE_MODE_DISABLED) { /* disable BT-coex */
			ret = wpa_driver_toggle_btcoex_state('0');
		} else if (mode == BLUETOOTH_COEXISTENCE_MODE_SENSE) { /* enable BT-coex */
			ret = wpa_driver_toggle_btcoex_state('1');
		} else {
			wpa_printf(MSG_DEBUG, "invalid btcoex mode: %d", mode);
			ret = -1;
		}
	} else if (os_strcasecmp(cmd, "MACADDR") == 0) {
		u8 macaddr[ETH_ALEN] = {};

		ret = linux_get_ifhwaddr(drv->global->ioctl_sock, bss->ifname, macaddr);
		if (!ret)
			ret = os_snprintf(buf, buf_len,
					  "Macaddr = " MACSTR "\n", MAC2STR(macaddr));
	} else {
		wpa_printf(MSG_INFO, "%s: Unsupported command %s", __func__, cmd);
	}
	return ret;
}
