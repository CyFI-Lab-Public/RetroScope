/* linux/omap_csmi.h
** 
** Copyright 2005-2006, The Android Open Source Project
** Author: Arve Hjønnevåg
**
** This file is dual licensed.  It may be redistributed and/or modified
** under the terms of the Apache 2.0 License OR version 2 of the GNU
** General Public License.
*/

#ifndef _OMAP_CSMI_H_
#define _OMAP_CSMI_H_

#include <asm/ioctl.h>

#ifdef __KERNEL__

typedef struct {
	uint32_t cmd;
	uint32_t arg1;
	uint32_t arg2;
	uint32_t arg3;
} omap_csmi_gc_command_t;

typedef struct {
	void (*ack)(int mailbox);
	int (*data)(int mailbox);
	void (*reset)(int mailbox);
	void (*start)(int mailbox);
	void (*stop)(int mailbox);
} omap_csmi_mailbox_handlers_t;

extern uint8_t *omap_csmi_gsm_data_vaddr;

#define SEND_FLAG_NO_ACK_AFTER_SEND 1
#define SEND_FLAG_REQEST_ACK_IF_BUSY 2

typedef struct {
	uint32_t magic;                // 0-3     0x494d5347
	uint32_t header_size;          // 4-7     64
	uint8_t  mailbox_size;         // 8       8 or 16
	uint8_t  num_channels;         // 9       2-15
	uint8_t  gc_channel;           // 10
	uint8_t  debug_channel;        // 11
	uint16_t stream_channel_mask;  // 12-13
	uint16_t pad;                  // 14-15
	char     vendor_code[16];      // 16-31
	char     product_code[16];     // 32-47
	uint32_t version;              // 48-51
	uint32_t ffs_location;         // 52-55
	uint32_t ffs_size;             // 56-59   0 or size
	char     vendor_data[20];       // 60-79
} omap_csmi_gsm_image_info_t;
// in platform_device->platform_data where platform_device->name == "omap-csmi"

#define OMAP_GSM_IMAGE_HEADER_MAGIC 0x494d5347 /* GSMI */

struct sysdev_class *omap_csmi_get_sysclass(void);
unsigned short omap_csmi_get_vendor_boot_flags(void);
int omap_csmi_gc_command(const omap_csmi_gc_command_t *command, int timeout, omap_csmi_gc_command_t *reply);
int omap_csmi_send_mailbox(int mailbox, void *data, size_t size, unsigned int flags);
void omap_csmi_read_mailbox(int mailbox, void *data, size_t size);
int omap_csmi_install_mailbox_handlers(int mailbox, omap_csmi_mailbox_handlers_t handlers);
int omap_csmi_uninstall_mailbox_handlers(int mailbox, omap_csmi_mailbox_handlers_t handlers);

//mailbox channel 1 commands

#define GC_DSP_PLL_REQ                  0x0001
#define GC_ARM_PLL_REQ                  0x0002
#define GC_READ_MEM_REQ                 0x0003
#define GC_SET_MEM_REQ                  0x0004
#define GC_SERIAL_CONFIG_REQ            0x0005
#define GC_RESET_REQ                    0x0006
#define GC_SW_VERSION_REQ               0x0007
#define GC_HW_VERSION_REQ               0x0008
#define GC_SLEEP_REQ                    0x0009
#define GC_IRQ_REQ                      0x000A
#define GC_BATTERY_STATUS_REQ           0x0011
#define GC_AUDIO_VOICE_REQ              0x0012
#define GC_AUDIO_VOICEBANDCTL_REQ       0x0013
#define GC_AUDIO_OUTPUTCTL_REQ          0x0014
#define GC_AUDIO_SIDETONE_REQ           0x0015
#define GC_AUDIO_ECHOCANC_REQ           0x0016
#define GC_AUDIO_STEREO_CODEC_REQ       0x0017
#define GC_AUDIO_PGA_REQ                0x0018
#define GC_AUDIO_PROFILE_LOAD_REQ       0x0019
#define GC_FFS_INIT_RESTORE_REQ         0x0020
#define GC_FFS_RESTORE_REQ              0x0021
#define GC_FFS_BACKUP_REQ               0x0022
#define GC_DAR_GET_INFO_REQ             0x0023
#define GC_AMR_RECORD_START_REQ         0x0030
#define GC_AMR_RECORD_STOP_REQ          0x0031
#define GC_AMR_PLAY_START_REQ           0x0032
#define GC_AMR_PLAY_STOP_REQ            0x0033
#define GC_AMR_GET_STATUS_REQ           0x0034
#define GC_AMR_PURGE_PLAY_BUFFER_REQ    0x0035
#define GC_ABB_REGISTER_REQ             0x0040
#define GC_AUDIO_ENHECHOCANC_REQ        0x0041

#define GC_READ_MEM_RES                 0x1000
#define GC_SW_VERSION_RES               0x1001
#define GC_HW_VERSION_RES               0x1002
#define GC_BATTERY_STATUS_RES           0x1005
#define GC_AUDIO_VOICEBANDCTL_RES       0x1006
#define GC_AUDIO_OUTPUTCTL_RES          0x1007
#define GC_AUDIO_STEREO_CODEC_RES       0x1008
#define GC_AUDIO_PROFILE_LOAD_RES       0x1019
#define GC_FFS_INIT_RESTORE_RES         0x1020
#define GC_FFS_BACKUP_RES               0x1022
#define GC_DAR_GET_INFO_RES             0x1023
#define GC_AMR_RECORD_START_RES         0x1030
#define GC_AMR_RECORD_STOP_RES          0x1031
#define GC_AMR_PLAY_START_RES           0x1032
#define GC_AMR_PLAY_STOP_RES            0x1033
#define GC_AMR_GET_STATUS_RES           0x1034
#define GC_ABB_REGISTER_RES             0x1040

#endif

/* tty ioctls */

#define OMAP_CSMI_TTY_ENABLE_ACK _IO('c', 0)
#define OMAP_CSMI_TTY_DISABLE_ACK _IO('c', 1)
#define OMAP_CSMI_TTY_READ_UNACKED _IOR('c', 2, int)
#define OMAP_CSMI_TTY_ACK _IOW('c', 3, int)
#define OMAP_CSMI_TTY_WAKEUP_AND_ACK _IOW('c', 4, int)

#endif
