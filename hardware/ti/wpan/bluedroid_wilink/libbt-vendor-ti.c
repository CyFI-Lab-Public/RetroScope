/*
 * Copyright 2001-2012 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Bluetooth Vendor Library for TI's WiLink Chipsets
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <dlfcn.h>
#include <utils/Log.h>
#include <pthread.h>
#include <fcntl.h>
#include <errno.h>
#include <bt_vendor_lib.h>
#include <bt_hci_lib.h>
#include <bt_hci_bdroid.h>
#include <utils.h>

bt_vendor_callbacks_t *bt_vendor_cbacks = NULL;
unsigned int hci_tty_fd = -1;
void hw_config_cback(HC_BT_HDR *p_evt_buf);

/*******************************************************************************
 *
 * Function         hw_config_cback
 *
 * Description      Callback function for controller configuration
 *
 * Returns          None
 *
 * *******************************************************************************/
void hw_config_cback(HC_BT_HDR *p_evt_buf)
{
    ALOGI("hw_config_cback");
}

int ti_init(const bt_vendor_callbacks_t* p_cb, unsigned char *local_bdaddr) {
    ALOGI("vendor Init");

    if (p_cb == NULL)
    {
        ALOGE("init failed with no user callbacks!");
        return BT_HC_STATUS_FAIL;
    }

    bt_vendor_cbacks = (bt_vendor_callbacks_t *) p_cb;

    return 0;
}
void ti_cleanup(void) {
    ALOGI("vendor cleanup");

    bt_vendor_cbacks = NULL;
}
int ti_op(bt_vendor_opcode_t opcode, void **param) {
    int fd;
    int *fd_array = (int (*)[]) param;

    ALOGI("vendor op - %d", opcode);
    switch(opcode)
    {
        case BT_VND_OP_USERIAL_OPEN:
            fd = open("/dev/hci_tty", O_RDWR);
            if (fd < 0) {
                ALOGE(" Can't open hci_tty");
                return -1;
            }
            fd_array[CH_CMD] = fd;
            hci_tty_fd = fd; /* for userial_close op */
            return 1;        /* CMD/EVT/ACL on same fd */
        case BT_VND_OP_USERIAL_CLOSE:
            close(hci_tty_fd);
            return 0;
        /* Since new stack expects fwcfg_cb we are returning SUCCESS here
         * in actual, firmware download is already happened when /dev/hci_tty
         * opened.
         */
        case BT_VND_OP_FW_CFG:
            bt_vendor_cbacks->fwcfg_cb(BT_VND_OP_RESULT_SUCCESS);
            return 0;
        default:
            break;
    }

    return 0;
}
const bt_vendor_interface_t BLUETOOTH_VENDOR_LIB_INTERFACE  = {
    .init = ti_init,
    .op = ti_op,
    .cleanup = ti_cleanup,
};

int main()
{
    return 0;
}
