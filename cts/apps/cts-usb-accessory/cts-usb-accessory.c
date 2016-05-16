/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <pthread.h>
#include <time.h>

#include <usbhost/usbhost.h>
#include <linux/usb/f_accessory.h>

struct usb_device *sDevice = NULL;

static void* message_thread(void* arg) {
    int *endpoints = (int *)arg;
    int ret = 0;
    int num = 0;
    char message[50];

    while (sDevice && ret >= 0) {
        char buffer[16384];
        ret = usb_device_bulk_transfer(sDevice, endpoints[0], buffer, sizeof(buffer), 1000);
        if (ret < 0 && errno == ETIMEDOUT) {
            ret = 0;
        }
        if (ret > 0) {
            printf("[RECV] ");
            fwrite(buffer, 1, ret, stdout);
            printf("\n");

            // Respond by sending a message back
            sprintf(message, "Message from Android accessory #%d", num++);
            printf("[SENT] %s\n", message);
            fflush(stdout);
            usb_device_bulk_transfer(sDevice, endpoints[1], message, strlen(message), 1000);
        }
    }

    return NULL;
}

static void milli_sleep(int millis) {
    struct timespec tm;

    tm.tv_sec = 0;
    tm.tv_nsec = millis * 1000000;
    nanosleep(&tm, NULL);
}

static void send_string(struct usb_device *device, int index, const char* string) {
    int ret = usb_device_control_transfer(device, USB_DIR_OUT | USB_TYPE_VENDOR,
            ACCESSORY_SEND_STRING, 0, index, (void *)string, strlen(string) + 1, 0);

    // some devices can't handle back-to-back requests, so delay a bit
    milli_sleep(10);
}

static int usb_device_added(const char *devname, void* client_data) {
    struct usb_descriptor_header* desc;
    struct usb_descriptor_iter iter;
    uint16_t vendorId, productId;
    int ret;
    pthread_t th;

    struct usb_device *device = usb_device_open(devname);
    if (!device) {
        fprintf(stderr, "usb_device_open failed\n");
        return 0;
    }

    vendorId = usb_device_get_vendor_id(device);
    productId = usb_device_get_product_id(device);

    if (!sDevice && (vendorId == 0x18D1 && (productId == 0x2D00 || productId == 0x2D01))) {
        struct usb_descriptor_header* desc;
        struct usb_descriptor_iter iter;
        struct usb_interface_descriptor *intf = NULL;
        struct usb_endpoint_descriptor *ep1 = NULL;
        struct usb_endpoint_descriptor *ep2 = NULL;

        printf("Found Android device in accessory mode (%x:%x)...\n",
               vendorId, productId);
        sDevice = device;

        usb_descriptor_iter_init(device, &iter);
        while ((desc = usb_descriptor_iter_next(&iter)) != NULL && (!intf || !ep1 || !ep2)) {
            if (desc->bDescriptorType == USB_DT_INTERFACE) {
                intf = (struct usb_interface_descriptor *)desc;
            } else if (desc->bDescriptorType == USB_DT_ENDPOINT) {
                if (ep1)
                    ep2 = (struct usb_endpoint_descriptor *)desc;
                else
                    ep1 = (struct usb_endpoint_descriptor *)desc;
            }
        }

        if (!intf) {
            fprintf(stderr, "Interface not found\n");
            exit(1);
        }
        if (!ep1 || !ep2) {
            fprintf(stderr, "Endpoints not found\n");
            exit(1);
        }

        if (usb_device_claim_interface(device, intf->bInterfaceNumber)) {
            fprintf(stderr, "usb_device_claim_interface failed errno: %d\n", errno);
            exit(1);
        }

        int endpoints[2];
        if ((ep1->bEndpointAddress & USB_ENDPOINT_DIR_MASK) == USB_DIR_IN) {
            endpoints[0] = ep1->bEndpointAddress;
            endpoints[1] = ep2->bEndpointAddress;
        } else {
            endpoints[0] = ep2->bEndpointAddress;
            endpoints[1] = ep1->bEndpointAddress;
        }
        pthread_create(&th, NULL, message_thread, (void *)endpoints);
    } else {
        printf("Found possible Android device (%x:%x) "
                "- attempting to switch to accessory mode...\n", vendorId, productId);

        uint16_t protocol = 0;
        ret = usb_device_control_transfer(device, USB_DIR_IN | USB_TYPE_VENDOR,
                ACCESSORY_GET_PROTOCOL, 0, 0, &protocol, sizeof(protocol), 0);
        if (ret == 2)
            printf("Device supports protocol version %d\n", protocol);
        else
            fprintf(stderr, "Failed to read protocol version\n");

        send_string(device, ACCESSORY_STRING_MANUFACTURER, "Android CTS");
        send_string(device, ACCESSORY_STRING_MODEL, "CTS USB Accessory");
        send_string(device, ACCESSORY_STRING_DESCRIPTION, "CTS USB Accessory");
        send_string(device, ACCESSORY_STRING_VERSION, "1.0");
        send_string(device, ACCESSORY_STRING_URI,
                "http://source.android.com/compatibility/cts-intro.html");
        send_string(device, ACCESSORY_STRING_SERIAL, "1234567890");

        ret = usb_device_control_transfer(device, USB_DIR_OUT | USB_TYPE_VENDOR,
                ACCESSORY_START, 0, 0, 0, 0, 0);
        return 0;
    }

    if (device != sDevice)
        usb_device_close(device);

    return 0;
}

static int usb_device_removed(const char *devname, void* client_data) {
    if (sDevice && !strcmp(usb_device_get_name(sDevice), devname)) {
        usb_device_close(sDevice);
        sDevice = NULL;
        // exit when we are disconnected
        return 1;
    }
    return 0;
}


int main(int argc, char* argv[]) {
    printf("CTS USB Accessory Tester\n");

    struct usb_host_context* context = usb_host_init();
    if (!context) {
        fprintf(stderr, "usb_host_init failed");
        return 1;
    }

    // this will never return so it is safe to pass thiz directly
    usb_host_run(context, usb_device_added, usb_device_removed, NULL, NULL);
    return 0;
}
