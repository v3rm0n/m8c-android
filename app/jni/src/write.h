// Copyright 2021 Jonne Kokkonen
// Released under the MIT licence, https://opensource.org/licenses/MIT

#ifndef WRITE_H_
#define WRITE_H_

#include <stdint.h>
#include "libusb.h"

int reset_display(libusb_device_handle *devh);
int enable_and_reset_display(libusb_device_handle *devh);
int disconnect(libusb_device_handle *devh);
int send_msg_controller(libusb_device_handle *devh, uint8_t input);
int send_msg_keyjazz(libusb_device_handle *devh, uint8_t note, uint8_t velocity);

#endif