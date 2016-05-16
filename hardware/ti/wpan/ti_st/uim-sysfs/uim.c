/*
 *  User Mode Init manager - For TI shared transport
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program;if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <poll.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include "uim.h"
#ifdef ANDROID
#include <private/android_filesystem_config.h>
#include <cutils/log.h>
#endif


/* Maintains the exit state of UIM*/
static int exiting;

/* BD address as string and a pointer to array of hex bytes */
char uim_bd_address[17];
bdaddr_t *bd_addr;

/* File descriptor for the UART device*/
int dev_fd;

static inline void cleanup(int failed)
{
	/* for future use */
	(void)failed;

	if (dev_fd == -1)
		return;

	UIM_DBG("%s", __func__);

	close(dev_fd);
	dev_fd = -1;
	/* unused failed for future reference */
}

/*****************************************************************************/
#ifdef UIM_DEBUG
/*  Function to Read the firmware version
 *  module into the system. Currently used for
 *  debugging purpose, whenever the baud rate is changed
 */
void read_firmware_version()
{
	int index = 0;
	char resp_buffer[20] = { 0 };
	unsigned char buffer[] = { 0x01, 0x01, 0x10, 0x00 };

	UIM_START_FUNC();
	UIM_VER(" wrote %d bytes", (int)write(dev_fd, buffer, 4));
	UIM_VER(" reading %d bytes", (int)read(dev_fd, resp_buffer, 15));

	for (index = 0; index < 15; index++)
		UIM_VER(" %x ", resp_buffer[index]);

	printf("\n");
}
#endif /* UIM_DEBUG */

/*****************************************************************************/
#ifdef ANDROID                 /* library for android to do insmod/rmmod  */

/* Function to insert the kernel module into the system*/
static int insmod(const char *filename, const char *args)
{
	void *module;
	unsigned int size;
	int ret = -1;

	UIM_START_FUNC();

	module = (void *)load_file(filename, &size);
	if (!module)
		return ret;

	ret = init_module(module, size, args);
	free(module);

	return ret;
}

/* Function to remove the kernel module from the system*/
static int rmmod(const char *modname)
{
	int ret = -1;
	int maxtry = MAX_TRY;

	UIM_START_FUNC();

	/* Retry MAX_TRY number of times in case of
	 * failure
	 */
	while (maxtry-- > 0) {
		ret = delete_module(modname, O_NONBLOCK | O_EXCL);
		if (ret < 0 && errno == EAGAIN)
			sleep(1);
		else
			break;
	}

	/* Failed to remove the module
	*/
	if (ret != 0)
		UIM_ERR("Unable to unload driver module \"%s\": %s",
				modname, strerror(errno));
	return ret;
}
#endif /* ANDROID */

/*****************************************************************************/
/* Function to read the HCI event from the given file descriptor
 *
 * This will parse the response received and returns error
 * if the required response is not received
 */
int read_hci_event(int fd, unsigned char *buf, int size)
{
	int remain, rd;
	int count = 0;
	int reading = 1;
	int rd_retry_count = 0;
	struct timespec tm = {0, 50*1000*1000};

	UIM_START_FUNC();

	UIM_VER(" read_hci_event");
	if (size <= 0)
		return -1;

	/* The first byte identifies the packet type. For HCI event packets, it
	 * should be 0x04, so we read until we get to the 0x04. */
	while (reading) {
		rd = read(fd, buf, 1);
		if (rd <= 0 && rd_retry_count++ < 4) {
			nanosleep(&tm, NULL);
			continue;
		} else if (rd_retry_count >= 4) {
			return -1;
		}

		if (buf[0] == RESP_PREFIX) {
			break;
		}
	}
	count++;

	/* The next two bytes are the event code and parameter total length. */
	while (count < 3) {
		rd = read(fd, buf + count, 3 - count);
		if (rd <= 0)
			return -1;
		count += rd;
	}

	/* Now we read the parameters. */
	if (buf[2] < (size - 3))
		remain = buf[2];
	else
		remain = size - 3;

	while ((count - 3) < remain) {
		rd = read(fd, buf + count, remain - (count - 3));
		if (rd <= 0)
			return -1;
		count += rd;
	}

	return count;
}

/* Function to read the Command complete event
 *
 * This will read the response for the change speed
 * command that was sent to configure the UART speed
 * with the custom baud rate
 */
static int read_command_complete(int fd, unsigned short opcode)
{
	command_complete_t resp;

	UIM_START_FUNC();

	UIM_VER(" Command complete started");
	if (read_hci_event(fd, (unsigned char *)&resp, sizeof(resp)) < 0) {
		UIM_ERR(" Invalid response");
		return -1;
	}

	/* Response should be an event packet */
	if (resp.uart_prefix != HCI_EVENT_PKT) {
		UIM_ERR
			(" Error in response: not an event packet, but 0x%02x!",
			 resp.uart_prefix);
		return -1;
	}

	/* Response should be a command complete event */
	if (resp.hci_hdr.evt != EVT_CMD_COMPLETE) {
		/* event must be event-complete */
		UIM_ERR
			(" Error in response: not a cmd-complete event,but 0x%02x!",
			 resp.hci_hdr.evt);
		return -1;
	}

	if (resp.hci_hdr.plen < 4) {
		/* plen >= 4 for EVT_CMD_COMPLETE */
		UIM_ERR(" Error in response: plen is not >= 4, but 0x%02x!",
				resp.hci_hdr.plen);
		return -1;
	}

	if (resp.cmd_complete.opcode != (unsigned short)opcode) {
		UIM_ERR(" Error in response: opcode is 0x%04x, not 0x%04x!",
				resp.cmd_complete.opcode, opcode);
		return -1;
	}

	UIM_DBG(" Command complete done");
	return resp.status == 0 ? 0 : -1;
}

/* Function to set the default baud rate
 *
 * The default baud rate of 115200 is set to the UART from the host side
 * by making a call to this function.This function is also called before
 * making a call to set the custom baud rate
 */
static int set_baud_rate()
{
	struct termios ti;

	UIM_START_FUNC();

	tcflush(dev_fd, TCIOFLUSH);

	/* Get the attributes of UART */
	if (tcgetattr(dev_fd, &ti) < 0) {
		UIM_ERR(" Can't get port settings");
		return -1;
	}

	/* Change the UART attributes before
	 * setting the default baud rate*/
	cfmakeraw(&ti);

	ti.c_cflag |= 1;
	ti.c_cflag |= CRTSCTS;

	/* Set the attributes of UART after making
	 * the above changes
	 */
	tcsetattr(dev_fd, TCSANOW, &ti);

	/* Set the actual default baud rate */
	cfsetospeed(&ti, B115200);
	cfsetispeed(&ti, B115200);
	tcsetattr(dev_fd, TCSANOW, &ti);

	tcflush(dev_fd, TCIOFLUSH);
	UIM_DBG(" set_baud_rate() done");

	return 0;
}

/* Function to set the UART custom baud rate.
 *
 * The UART baud rate has already been
 * set to default value 115200 before calling this function.
 * The baud rate is then changed to custom baud rate by this function*/
static int set_custom_baud_rate(int cust_baud_rate, unsigned char flow_ctrl)
{
	UIM_START_FUNC();

	struct termios ti;
	struct termios2 ti2;

	/* Get the attributes of UART */
	if (tcgetattr(dev_fd, &ti) < 0) {
		UIM_ERR(" Can't get port settings");
		return -1;
	}

	UIM_VER(" Changing baud rate to %u, flow control to %u",
			cust_baud_rate, flow_ctrl);

	/* Flush non-transmitted output data,
	 * non-read input data or both*/
	tcflush(dev_fd, TCIOFLUSH);

	/*Set the UART flow control */
	if (flow_ctrl)
		ti.c_cflag |= CRTSCTS;
	else
		ti.c_cflag &= ~CRTSCTS;

	/*
	 * Set the parameters associated with the UART
	 * The change will occur immediately by using TCSANOW
	 */
	if (tcsetattr(dev_fd, TCSANOW, &ti) < 0) {
		UIM_ERR(" Can't set port settings");
		return -1;
	}

	tcflush(dev_fd, TCIOFLUSH);

	/*Set the actual baud rate */
	ioctl(dev_fd, TCGETS2, &ti2);
	ti2.c_cflag &= ~CBAUD;
	ti2.c_cflag |= BOTHER;
	ti2.c_ospeed = cust_baud_rate;
	ioctl(dev_fd, TCSETS2, &ti2);

	UIM_DBG(" set_custom_baud_rate() done");
	return 0;
}

/*
 * Handling the Signals sent from the Kernel Init Manager.
 * After receiving the indication from rfkill subsystem, configure the
 * baud rate, flow control and Install the N_TI_WL line discipline
 */
int st_uart_config(unsigned char install)
{
	int ldisc, len, fd;
	unsigned char uart_dev_name[32];
	unsigned char buf[32];
	unsigned long cust_baud_rate;
	unsigned int flow_ctrl;

	uim_speed_change_cmd cmd;
	uim_bdaddr_change_cmd addr_cmd;

	UIM_START_FUNC();

	if (install == '1') {
		UIM_DBG("install set to 1");
		memset(buf, 0, 32);
		fd = open(DEV_NAME_SYSFS, O_RDONLY);
		if (fd < 0) {
			UIM_ERR("Can't open %s", DEV_NAME_SYSFS);
			return -1;
		}
		len = read(fd, buf, 32);
		if (len < 0) {
			UIM_ERR("read err (%s)", strerror(errno));
			close(fd);
			return len;
		}
		sscanf((const char*)buf, "%s", uart_dev_name);
		close(fd);

		memset(buf, 0, 32);
		fd = open(BAUD_RATE_SYSFS, O_RDONLY);
		if (fd < 0) {
			UIM_ERR("Can't open %s", BAUD_RATE_SYSFS);
			return -1;
		}
		len = read(fd, buf, 32);
		if (len < 0) {
			UIM_ERR("read err (%s)", strerror(errno));
			close(fd);
			return len;
		}
		close(fd);
		sscanf((const char*)buf, "%ld", &cust_baud_rate);

		memset(buf, 0, 32);
		fd = open(FLOW_CTRL_SYSFS, O_RDONLY);
		if (fd < 0) {
			UIM_ERR("Can't open %s", FLOW_CTRL_SYSFS);
			close(fd);
			return -1;
		}
		len = read(fd, buf, 32);
		if (len < 0) {
			UIM_ERR("read err (%s)", strerror(errno));
			close(fd);
			return len;
		}
		close(fd);
		sscanf((const char*)buf, "%u", &flow_ctrl);

		if (dev_fd != -1) {
			UIM_ERR("opening %s, while already open", uart_dev_name);
			cleanup(-1);
		}

		dev_fd = open((const char*) uart_dev_name, O_RDWR);
		if (dev_fd < 0) {
			UIM_ERR(" Can't open %s", uart_dev_name);
			return -1;
		}
		/*
		 * Set only the default baud rate.
		 * This will set the baud rate to default 115200
		 */
		if (set_baud_rate() < 0) {
			UIM_ERR(" set_baudrate() failed");
			cleanup(-1);
			return -1;
		}

		fcntl(dev_fd, F_SETFL,fcntl(dev_fd, F_GETFL) | O_NONBLOCK);
		/* Set only thecustom baud rate */
		if (cust_baud_rate != 115200) {

			/* Forming the packet for Change speed command */
			cmd.uart_prefix = HCI_COMMAND_PKT;
			cmd.hci_hdr.opcode = HCI_HDR_OPCODE;
			cmd.hci_hdr.plen = sizeof(unsigned long);
			cmd.speed = cust_baud_rate;

			/* Writing the change speed command to the UART
			 * This will change the UART speed at the controller
			 * side
			 */
			UIM_VER(" Setting speed to %ld", cust_baud_rate);
			len = write(dev_fd, &cmd, sizeof(cmd));
			if (len < 0) {
				UIM_ERR(" Failed to write speed-set command");
				cleanup(-1);
				return -1;
			}

			/* Read the response for the Change speed command */
			if (read_command_complete(dev_fd, HCI_HDR_OPCODE) < 0) {
				cleanup(-1);
				return -1;
			}

			UIM_VER(" Speed changed to %ld", cust_baud_rate);

			/* Set the actual custom baud rate at the host side */
			if (set_custom_baud_rate(cust_baud_rate, flow_ctrl) < 0) {
				UIM_ERR(" set_custom_baud_rate() failed");
				cleanup(-1);
				return -1;
			}

			/* Set the uim BD address */
			if (uim_bd_address[0] != 0) {

				memset(&addr_cmd, 0, sizeof(addr_cmd));
				/* Forming the packet for change BD address command*/
				addr_cmd.uart_prefix = HCI_COMMAND_PKT;
				addr_cmd.hci_hdr.opcode = WRITE_BD_ADDR_OPCODE;
				addr_cmd.hci_hdr.plen = sizeof(bdaddr_t);
				memcpy(&addr_cmd.addr, bd_addr, sizeof(bdaddr_t));

				/* Writing the change BD address command to the UART
				 * This will change the change BD address  at the controller
				 * side
				 */
				len = write(dev_fd, &addr_cmd, sizeof(addr_cmd));
				if (len < 0) {
					UIM_ERR(" Failed to write BD address command");
					cleanup(-1);
					return -1;
				}

				/* Read the response for the change BD address command */
				if (read_command_complete(dev_fd, WRITE_BD_ADDR_OPCODE) < 0) {
					cleanup(-1);
					return -1;
				}

				UIM_VER(" BD address changed to %s", uim_bd_address);
			}
#ifdef UIM_DEBUG
			read_firmware_version();
#endif
		}

		/* After the UART speed has been changed, the IOCTL is
		 * is called to set the line discipline to N_TI_WL
		 */
		ldisc = 22;
		if (ioctl(dev_fd, TIOCSETD, &ldisc) < 0) {
			UIM_ERR(" Can't set line discipline");
			cleanup(-1);
			return -1;
		}
		UIM_DBG(" Installed N_TI_WL Line displine");
	}
	else {
		UIM_DBG(" Un-Installed N_TI_WL Line displine");
		/* UNINSTALL_N_TI_WL - When the Signal is received from KIM */
		/* closing UART fd */
		cleanup(0);
		dev_fd = -1;
	}
	return 0;
}

int remove_modules()
{
	int err = 0;

#ifdef ANDROID
	UIM_VER(" Removing gps_drv ");
	if (rmmod("gps_drv") != 0) {
		UIM_ERR(" Error removing gps_drv module");
		err = -1;
	} else {
		UIM_DBG(" Removed gps_drv module");
	}

	UIM_VER(" Removing fm_drv ");
	if (rmmod("fm_drv") != 0) {
		UIM_ERR(" Error removing fm_drv module");
		err = -1;
	} else {
		UIM_DBG(" Removed fm_drv module");
	}
	UIM_DBG(" Removed fm_drv module");

	UIM_VER(" Removing btwilink ");
	if (rmmod("btwilink") != 0) {
		UIM_ERR(" Error removing btwilink module");
		err = -1;
	} else {
		UIM_DBG(" Removed btwilink module");
	}
	UIM_DBG(" Removed btwilink module");

	/*Remove the Shared Transport */
	UIM_VER(" Removing st_drv ");
	if (rmmod("st_drv") != 0) {
		UIM_ERR(" Error removing st_drv module");
		err = -1;
	} else {
		UIM_DBG(" Removed st_drv module ");
	}
	UIM_DBG(" Removed st_drv module ");
#else
	UIM_VER(" Removing btwilink ");
	if (system("rmmod btwilink") != 0) {
		UIM_ERR(" Error removing btwilink module");
		err = -1;
	} else {
		UIM_DBG(" Removed btwilink module");
	}

	UIM_VER(" Removing fm_drv ");
	if (system("rmmod fm_drv") != 0) {
		UIM_ERR(" Error removing fm_drv module");
		err = -1;
	} else {
		UIM_DBG(" Removed fm_drv module ");
	}

	/*Remove the Shared Transport */
	UIM_VER(" Removing st_drv ");
	if (system("rmmod st_drv") != 0) {
		UIM_ERR(" Error removing st_drv module");
		err = -1;
	} else {
		UIM_DBG(" Removed st_drv module ");
	}
#endif
	return err;
}

int change_rfkill_perms(void)
{
	int fd, id, sz;
	char path[64];
	char buf[16];
	for (id = 0; id < 50; id++) {
		snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/type", id);
		fd = open(path, O_RDONLY);
		if (fd < 0) {
			UIM_DBG("open(%s) failed: %s (%d)\n", path, strerror(errno), errno);
			continue;
		}
		sz = read(fd, &buf, sizeof(buf));
		close(fd);
		if (sz >= 9 && memcmp(buf, "bluetooth", 9) == 0) {
			UIM_DBG("found bluetooth rfkill entry @ %d\n", id);
			break;
		}
	}
	if (id == 50) {
		return -1;
	}
#ifdef ANDROID
	sprintf(path, "/sys/class/rfkill/rfkill%d/state", id);
	sz = chown(path, AID_BLUETOOTH, AID_BLUETOOTH);
	if (sz < 0) {
		UIM_ERR("change mode failed for %s (%d)\n", path, errno);
		return -1;
	}
#endif
	/*
	 * bluetooth group's user system needs write permission
	 */
	sz = chmod(path, 0660);
	if (sz < 0) {
		UIM_ERR("change mode failed for %s (%d)\n", path, errno);
		return -1;
	}
	UIM_DBG("changed permissions for %s(%d) \n", path, sz);
	/* end of change_perms */

	return 0;
}

void *bt_malloc(size_t size)
{
	return malloc(size);
}

/* Function to convert the BD address from ascii to hex value */
bdaddr_t *strtoba(const char *str)
{
	const char *ptr = str;
	int i;

	uint8_t *ba = bt_malloc(sizeof(bdaddr_t));
	if (!ba)
		return NULL;

	for (i = 0; i < 6; i++) {
		ba[i] = (uint8_t) strtol(ptr, NULL, 16);
		if (i != 5 && !(ptr = strchr(ptr, ':')))
			ptr = ":00:00:00:00:00";
		ptr++;
	}

	return (bdaddr_t *) ba;
}

/*****************************************************************************/
int main(int argc, char *argv[])
{
	int st_fd,err;
	struct stat file_stat;
#ifndef ANDROID        /* used on ubuntu */
	char *tist_ko_path;
	struct utsname name;
#endif
	struct pollfd   p;
	unsigned char install;

	UIM_START_FUNC();
	err = 0;

	/* Parse the user input */
	if (argc == 2) {
		memset(&uim_bd_address, 0, sizeof(uim_bd_address));
		/* BD address passed as string in xx:xx:xx:xx:xx:xx format */
		strcpy(uim_bd_address, argv[1]);
		bd_addr = strtoba(uim_bd_address);
	} else if (argc != 1) {
		UIM_ERR(" Invalid arguements");
		UIM_ERR(" Usage: uim <bd address>");
		return -1;
	}

#ifndef ANDROID
	if (uname (&name) == -1) {
		UIM_ERR("cannot get kernel release name");
		return -1;
	}
#else  /* if ANDROID */

	if (0 == lstat("/st_drv.ko", &file_stat)) {
		if (insmod("/st_drv.ko", "") < 0) {
			UIM_ERR(" Error inserting st_drv module");
			return -1;
		} else {
			UIM_DBG(" Inserted st_drv module");
		}
	} else {
		if (0 == lstat(INSTALL_SYSFS_ENTRY, &file_stat)) {
			UIM_DBG("ST built into the kernel ?");
		} else {
			UIM_ERR("BT/FM/GPS would be unavailable on system");
			return -1;
		}
	}

	if (0 == lstat("/btwilink.ko", &file_stat)) {
		if (insmod("/btwilink.ko", "") < 0) {
			UIM_ERR(" Error inserting btwilink module, NO BT? ");
		} else {
			UIM_DBG(" Inserted btwilink module");
		}
	} else {
		UIM_DBG("BT driver module un-available... ");
		UIM_DBG("BT driver built into the kernel ?");
	}

	if (0 == lstat("/fm_drv.ko", &file_stat)) {
		if (insmod("/fm_drv.ko", "") < 0) {
			UIM_ERR(" Error inserting fm_drv module, NO FM? ");
		} else {
			UIM_DBG(" Inserted fm_drv module");
		}
	} else {
		UIM_DBG("FM driver module un-available... ");
		UIM_DBG("FM driver built into the kernel ?");
	}

	if (0 == lstat("/gps_drv.ko", &file_stat)) {
		if (insmod("/gps_drv.ko", "") < 0) {
			UIM_ERR(" Error inserting gps_drv module, NO GPS? ");
		} else {
			UIM_DBG(" Inserted gps_drv module");
		}
	} else {
		UIM_DBG("GPS driver module un-available... ");
		UIM_DBG("GPS driver built into the kernel ?");
	}

	if (0 == lstat("/fm_v4l2_drv.ko", &file_stat)) {
		if (insmod("/fm_v4l2_drv.ko", "") < 0) {
			UIM_ERR(" Error inserting fm_v4l2_drv module, NO FM? ");
		} else {
			UIM_DBG(" Inserted fm_v4l2_drv module");
		}
	} else {
		UIM_DBG("FM V4L2 driver module un-available... ");
		UIM_DBG("FM V4L2 driver built into the kernel ?");
	}
	/* Change the permissions for v4l2 Fm device node */
	if ((0 == lstat("/dev/radio0", &file_stat)) && chmod("/dev/radio0", 0666) < 0) {
		UIM_ERR("unable to chmod /dev/radio0, might not exist");
	}
	if ((0 == lstat("/dev/tifm", &file_stat)) && chmod("/dev/tifm", 0666) < 0) {
		UIM_ERR("unable to chmod /dev/tifm, might not exist");
	}
	/* change rfkill perms after insertion of BT driver which asks
	 * the Bluetooth sub-system to create the rfkill device of type
	 * "bluetooth"
	 */
	if (change_rfkill_perms() < 0) {
		/* possible error condition */
		UIM_ERR("rfkill not enabled in st_drv - BT on from UI might fail\n");
	}

#endif /* ANDROID */
	/* rfkill device's open/poll/read */
	st_fd = open(INSTALL_SYSFS_ENTRY, O_RDONLY);
	if (st_fd < 0) {
		UIM_DBG("unable to open %s (%s)", INSTALL_SYSFS_ENTRY,
				strerror(errno));
		remove_modules();
		return -1;
	}

RE_POLL:
	err = read(st_fd, &install, 1);
	if ((err > 0) && (install == '1')) {
		UIM_DBG("install already set");
		st_uart_config(install);
	}

	memset(&p, 0, sizeof(p));
	p.fd = st_fd;
	/* sysfs entries can only break poll for following events */
	p.events = POLLERR | POLLHUP;

	while (!exiting) {
		p.revents = 0;
		err = poll(&p, 1, -1);
		if (err < 0 && errno == EINTR)
			continue;
		if (err)
			break;
	}

	close(st_fd);
	st_fd = open(INSTALL_SYSFS_ENTRY, O_RDONLY);
	if (st_fd < 0) {
		UIM_ERR("re-opening %s failed: %s", INSTALL_SYSFS_ENTRY,
		strerror(errno));
		return -1;
	}

	if (!exiting)
	{
		err = read(st_fd, &install, 1);
		if (err <= 0) {
			UIM_ERR("reading %s failed: %s", INSTALL_SYSFS_ENTRY,
					strerror(errno));
			goto RE_POLL;
		}
		st_uart_config(install);
		goto RE_POLL;
	}

	if(remove_modules() < 0) {
		UIM_ERR(" Error removing modules ");
		close(st_fd);
		return -1;
	}

	close(st_fd);
	return 0;
}
