// Copyright 2021 Jonne Kokkonen
// Released under the MIT licence, https://opensource.org/licenses/MIT

// Contains portions of code from libserialport's examples released to the
// public domain

#include <SDL_log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "serial.h"
#include <android/log.h>

static int ep_in_addr = 0x83;
#define ACM_CTRL_DTR   0x01
#define ACM_CTRL_RTS   0x02

#define ENABLE_DEBUG_LOGGING
#define ENABLE_LOGGING

int nonblocking_read(libusb_device_handle *devh, uint8_t *serial_buf, int serial_read_size) {
    int actual_length;
    int rc = libusb_bulk_transfer(devh, ep_in_addr, serial_buf, serial_read_size, &actual_length,
                                  1000);
    if (rc == LIBUSB_ERROR_TIMEOUT) {
        SDL_Log("timeout (%d)\n", actual_length);
        return -1;
    } else if (rc < 0) {
        SDL_Log("Error while waiting for char: %d\n", rc);
        return -1;
    }

    return actual_length;
}

// Checks for connected devices and whether the specified device still exists
int check_serial_port(libusb_device_handle *devh) {
    int device_found = 1;
    return device_found;
}

static void print_endpoint(const struct libusb_endpoint_descriptor *endpoint) {
    int i, ret;

    SDL_Log("      Endpoint:\n");
    SDL_Log("        bEndpointAddress:    %02xh\n", endpoint->bEndpointAddress);
    SDL_Log("        bmAttributes:        %02xh\n", endpoint->bmAttributes);
    SDL_Log("        wMaxPacketSize:      %u\n", endpoint->wMaxPacketSize);
    SDL_Log("        bInterval:           %u\n", endpoint->bInterval);
    SDL_Log("        bRefresh:            %u\n", endpoint->bRefresh);
    SDL_Log("        bSynchAddress:       %u\n", endpoint->bSynchAddress);

}

static void print_altsetting(const struct libusb_interface_descriptor *interface) {
    uint8_t i;

    SDL_Log("    Interface:\n");
    SDL_Log("      bInterfaceNumber:      %u\n", interface->bInterfaceNumber);
    SDL_Log("      bAlternateSetting:     %u\n", interface->bAlternateSetting);
    SDL_Log("      bNumEndpoints:         %u\n", interface->bNumEndpoints);
    SDL_Log("      bInterfaceClass:       %u\n", interface->bInterfaceClass);
    SDL_Log("      bInterfaceSubClass:    %u\n", interface->bInterfaceSubClass);
    SDL_Log("      bInterfaceProtocol:    %u\n", interface->bInterfaceProtocol);
    SDL_Log("      iInterface:            %u\n", interface->iInterface);

    for (i = 0; i < interface->bNumEndpoints; i++)
        print_endpoint(&interface->endpoint[i]);
}

static void print_interface(const struct libusb_interface *interface) {
    int i;

    for (i = 0; i < interface->num_altsetting; i++)
        print_altsetting(&interface->altsetting[i]);
}

static void print_configuration(struct libusb_config_descriptor *config) {
    uint8_t i;

    SDL_Log("  Configuration:\n");
    SDL_Log("    wTotalLength:            %u\n", config->wTotalLength);
    SDL_Log("    bNumInterfaces:          %u\n", config->bNumInterfaces);
    SDL_Log("    bConfigurationValue:     %u\n", config->bConfigurationValue);
    SDL_Log("    iConfiguration:          %u\n", config->iConfiguration);
    SDL_Log("    bmAttributes:            %02xh\n", config->bmAttributes);
    SDL_Log("    MaxPower:                %u\n", config->MaxPower);

    for (i = 0; i < config->bNumInterfaces; i++)
        print_interface(&config->interface[i]);
}

static void print_device(libusb_device *dev, libusb_device_handle *handle) {
    struct libusb_device_descriptor desc;
    unsigned char string[256];
    const char *speed;
    int ret;
    uint8_t i;
    int verbose = 1;

    switch (libusb_get_device_speed(dev)) {
        case LIBUSB_SPEED_LOW:
            speed = "1.5M";
            break;
        case LIBUSB_SPEED_FULL:
            speed = "12M";
            break;
        case LIBUSB_SPEED_HIGH:
            speed = "480M";
            break;
        case LIBUSB_SPEED_SUPER:
            speed = "5G";
            break;
        case LIBUSB_SPEED_SUPER_PLUS:
            speed = "10G";
            break;
        default:
            speed = "Unknown";
    }

    ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0) {
        SDL_Log("failed to get device descriptor");
        return;
    }

    SDL_Log("Dev (bus %u, device %u): %04X - %04X speed: %s\n",
            libusb_get_bus_number(dev), libusb_get_device_address(dev),
            desc.idVendor, desc.idProduct, speed);

    if (handle) {
        if (desc.iManufacturer) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iManufacturer, string,
                                                     sizeof(string));
            if (ret > 0)
                SDL_Log("  Manufacturer:              %s\n", (char *) string);
        }

        if (desc.iProduct) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iProduct, string, sizeof(string));
            if (ret > 0)
                SDL_Log("  Product:                   %s\n", (char *) string);
        }

        if (desc.iSerialNumber && verbose) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iSerialNumber, string,
                                                     sizeof(string));
            if (ret > 0)
                SDL_Log("  Serial Number:             %s\n", (char *) string);
        }
    }

    if (verbose) {
        for (i = 0; i < desc.bNumConfigurations; i++) {
            struct libusb_config_descriptor *config;

            ret = libusb_get_config_descriptor(dev, i, &config);
            if (LIBUSB_SUCCESS != ret) {
                SDL_Log("  Couldn't retrieve descriptors\n");
                continue;
            }

            print_configuration(config);

            libusb_free_config_descriptor(config);
        }
    }
}

static void LIBUSB_CALL log_cb(libusb_context *ctx, enum libusb_log_level level, const char *str) {
    __android_log_write(ANDROID_LOG_INFO, "libusb", str);
}

void unrooted_usb_description(libusb_device_handle **devh, int fileDescriptor) {
    libusb_context *ctx = NULL;
    int r = 0;
    r = libusb_set_option(ctx, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    if (r != LIBUSB_SUCCESS) {
        SDL_Log("libusb_init failed: %d\n", r);
        return;
    }
    libusb_set_log_cb(ctx, log_cb, LIBUSB_LOG_CB_GLOBAL);
    r = libusb_set_option(ctx, LIBUSB_OPTION_LOG_LEVEL, LIBUSB_LOG_LEVEL_DEBUG);
    if (r != LIBUSB_SUCCESS) {
        SDL_Log("libusb_init failed: %d\n", r);
        return;
    }
    r = libusb_init(&ctx);
    if (r < 0) {
        SDL_Log("libusb_init failed: %d\n", r);
        return;
    }
    r = libusb_wrap_sys_device(ctx, (intptr_t) fileDescriptor, devh);
    if (r < 0) {
        SDL_Log("libusb_wrap_sys_device failed: %d\n", r);
        return;
    } else if (devh == NULL) {
        SDL_Log("libusb_wrap_sys_device returned invalid handle\n");
        return;
    }
    SDL_Log("USB device init success");
}

struct libusb_device_handle *init_serial(int fileDescriptor, int verbose) {

    SDL_Log("Initialising USB device for %d", fileDescriptor);

    libusb_device_handle *devh = NULL;
    unrooted_usb_description(&devh, fileDescriptor);
    print_device(libusb_get_device(devh), devh);

    int rc;

    /* Start configuring the device:
     * - set line state
     */
    SDL_Log("Setting line state");
    rc = libusb_control_transfer(devh, 0x21, 0x22, ACM_CTRL_DTR | ACM_CTRL_RTS,
                                 0, NULL, 0, 0);
    if (rc < 0) {
        SDL_Log("Error during control transfer: %s\n",
                libusb_error_name(rc));
    }

    /* - set line encoding: here 115200 8N1
     * 9600 = 0x2580 ~> 0x80, 0x25 in little endian
     * 115200 = 0x01C200 ~> 0x00, 0xC2, 0x01, 0x00 in little endian
     */
    SDL_Log("Set line encoding");
    unsigned char encoding[] = {0x00, 0xC2, 0x01, 0x00, 0x00, 0x00, 0x08};
    rc = libusb_control_transfer(devh, 0x21, 0x20, 0, 0, encoding,
                                 sizeof(encoding), 0);
    if (rc < 0) {
        SDL_Log("Error during control transfer: %s\n",
                libusb_error_name(rc));
    }

    return (devh);
}
