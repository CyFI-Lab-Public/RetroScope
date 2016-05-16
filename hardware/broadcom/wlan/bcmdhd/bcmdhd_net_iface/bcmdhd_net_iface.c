#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <net/if.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/types.h>

#include <netinet/in.h>
#include <arpa/inet.h>

#include <cutils/log.h>
#include <netutils/ifc.h>
#include <private/android_filesystem_config.h>

#define INTERFACE_MAX_BUFFER_SIZE 4096

typedef struct android_wifi_priv_cmd {
	char *buf;
	int used_len;
	int total_len;
} android_wifi_priv_cmd;

static struct net_if_snd_cmd_state {
	int sock;
	char cmd[INTERFACE_MAX_BUFFER_SIZE];
	char ibuf[INTERFACE_MAX_BUFFER_SIZE];
} state;

int net_iface_send_command_init(void) {
	state.sock = socket(AF_INET, SOCK_DGRAM, 0);
	if (state.sock < 0) {
		return -ENOTCONN;
	} else {
		return 0;
	}
}

/*
 * Arguments:
 *	  argv[2] - wlan interface
 *	  argv[3] - command
 *	  argv[4] - argument
 */
int net_iface_send_command(int argc, char *argv[], char **rbuf) {
	int ret, i;
	size_t bc = 0;
	struct ifreq ifr;
	android_wifi_priv_cmd priv_cmd;

	if (argc < 3) {
		return -EINVAL;
	}
	for (i = 3; i < argc; i++) {
		bc += snprintf(&state.cmd[bc], sizeof(state.cmd) - bc, "%s ", argv[i]);
		if (bc >= sizeof(state.cmd)) {
			return -ENOBUFS;
		}
	}
	state.cmd[bc] = '\0';

	memset(&ifr, 0, sizeof(ifr));
	strncpy(ifr.ifr_name, argv[2], IFNAMSIZ);
	strncpy(state.ibuf, state.cmd, INTERFACE_MAX_BUFFER_SIZE);

	priv_cmd.buf = state.ibuf;
	priv_cmd.used_len = INTERFACE_MAX_BUFFER_SIZE;
	priv_cmd.total_len = INTERFACE_MAX_BUFFER_SIZE;

	ifr.ifr_data = &priv_cmd;

	if ((ret = ioctl(state.sock, SIOCDEVPRIVATE + 1, &ifr)) < 0) {
		return ret;
	}

	if (rbuf)
		*rbuf = state.ibuf;
	else
		ret = -EFAULT;

	return ret;
}
