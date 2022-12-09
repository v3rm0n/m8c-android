// Copyright 2021 Jonne Kokkonen
// Released under the MIT licence, https://opensource.org/licenses/MIT

#ifndef _SERIAL_H_
#define _SERIAL_H_

#include "libusb.h"

struct libusb_device_handle *init_serial(int fd, int verbose);

int nonblocking_read(libusb_device_handle *devh, uint8_t *serial_buf, int serial_read_size);

int check_serial_port(libusb_device_handle *devh);

#endif