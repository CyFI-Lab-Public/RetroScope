/*
 *  User Mode Init manager - For shared transport
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

#ifndef UIM_H
#define UIM_H

/* Paramaters to set the baud rate*/
#define  FLOW_CTL       0x0001
#define  BOTHER         0x00001000
#define  ARM_NCCS       19

#define TCGETS2      _IOR('T',0x2A, struct termios2)
#define TCSETS2      _IOW('T',0x2B, struct termios2)

/*HCI Command and Event information*/
#define HCI_HDR_OPCODE          0xff36
#define WRITE_BD_ADDR_OPCODE    0xFC06
#define RESP_PREFIX             0x04
#define MAX_TRY                 10

/* HCI Packet types */
#define HCI_COMMAND_PKT         0x01
#define HCI_EVENT_PKT           0x04

/* HCI command macros*/
#define HCI_EVENT_HDR_SIZE              2
#define HCI_COMMAND_HDR_SIZE            3
#define UIM_WRITE_BD_ADDR_CP_SIZE       6


/* HCI event macros*/
#define EVT_CMD_COMPLETE_SIZE   3
#define EVT_CMD_STATUS_SIZE     4
#define EVT_CMD_COMPLETE        0x0E
#define EVT_CMD_STATUS          0x0F


#define VERBOSE
#ifndef ANDROID
#define ALOGE printf
#endif /* ANDROID */
#define LOG_TAG "uim-rfkill: "
#define UIM_ERR(fmt, arg...)  ALOGE("uim:"fmt"\n" , ##arg)
#if defined(UIM_DEBUG)          /* limited debug messages */
#define UIM_START_FUNC()      ALOGE("uim: Inside %s\n", __FUNCTION__)
#define UIM_DBG(fmt, arg...)  ALOGE("uim:"fmt"\n" , ## arg)
#define UIM_VER(fmt, arg...)
#elif defined(VERBOSE)          /* very verbose */
#define UIM_START_FUNC()      ALOGE("uim: Inside %s\n", __FUNCTION__)
#define UIM_DBG(fmt, arg...)  ALOGE("uim:"fmt"\n" , ## arg)
#define UIM_VER(fmt, arg...)  ALOGE("uim:"fmt"\n" , ## arg)
#else /* error msgs only */
#define UIM_START_FUNC()
#define UIM_DBG(fmt, arg...)
#define UIM_VER(fmt, arg...)
#endif

/*Termios2 structure for setting the Custom baud rate*/
struct termios2 {
    tcflag_t c_iflag;       /* input mode flags */
    tcflag_t c_oflag;       /* output mode flags */
    tcflag_t c_cflag;       /* control mode flags */
    tcflag_t c_lflag;       /* local mode flags */
    cc_t c_line;            /* line discipline */
    cc_t c_cc[ARM_NCCS];    /* control characters */
    speed_t c_ispeed;       /* input speed */
    speed_t c_ospeed;       /* output speed */
};

/* HCI command header*/
typedef struct {
    uint16_t        opcode;         /* OCF & OGF */
    uint8_t         plen;
} __attribute__ ((packed))      hci_command_hdr;

/* HCI event header*/
typedef struct {
    uint8_t         evt;
    uint8_t         plen;
} __attribute__ ((packed))      hci_event_hdr;

/* HCI command complete event*/
typedef struct {
    uint8_t         ncmd;
    uint16_t        opcode;
} __attribute__ ((packed)) evt_cmd_complete;

/* HCI event status*/
typedef struct {
    uint8_t         status;
    uint8_t         ncmd;
    uint16_t        opcode;
} __attribute__ ((packed)) evt_cmd_status;

/* HCI Event structure to set the cusrom baud rate*/
typedef struct {
    uint8_t uart_prefix;
    hci_event_hdr hci_hdr;
    evt_cmd_complete cmd_complete;
    uint8_t status;
    uint8_t data[16];
} __attribute__ ((packed)) command_complete_t;

/* HCI Command structure to set the cusrom baud rate*/
typedef struct {
    uint8_t uart_prefix;
    hci_command_hdr hci_hdr;
    uint32_t speed;
} __attribute__ ((packed)) uim_speed_change_cmd;

/* BD address structure to set the uim BD address*/
typedef struct {
    unsigned char b[6];
} __attribute__((packed)) bdaddr_t;

/* HCI Command structure to set the uim BD address*/
typedef struct {
    uint8_t uart_prefix;
    hci_command_hdr hci_hdr;
    bdaddr_t addr;
} __attribute__ ((packed)) uim_bdaddr_change_cmd;

/* Signal received from KIM will install line discipline at first,
 * the next signal received from KIM will un-install the
 * line discipline*/
enum {
    /* expecting signal from KIM to setup uart fd for ST */
    INSTALL_N_TI_WL,

    /* expecting signal from KIM to close uart fd */
    UNINSTALL_N_TI_WL,
};

/* Functions to insert and remove the kernel modules from the system*/
extern int init_module(void *, unsigned int, const char *);
extern int delete_module(const char *, unsigned int);
extern int load_file(const char *, unsigned int *);

#endif /* UIM_H */
