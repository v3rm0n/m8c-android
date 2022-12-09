// Copyright 2021 Jonne Kokkonen
// Released under the MIT licence, https://opensource.org/licenses/MIT

#include "SDL_timer.h"
#include <SDL_log.h>
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>
#include "libusb.h"

static int ep_out_addr = 0x03;

int blocking_write(libusb_device_handle *devh, void *buf,
                   size_t count, unsigned int timeout_ms) {
    int actual_length;
    if (libusb_bulk_transfer(devh, ep_out_addr, buf, count,
                             &actual_length, timeout_ms) < 0) {
        SDL_Log("Error while sending char\n");
        return -1;
    }
    return actual_length;
}

int reset_display(libusb_device_handle *devh) {
    SDL_Log("Reset display\n");
    uint8_t buf[2];
    int result;

    buf[0] = 0x45;
    buf[1] = 0x52;

    result = blocking_write(devh, buf, 2, 5);
    if (result != 2) {
        SDL_LogError(SDL_LOG_CATEGORY_SYSTEM, "Error resetting M8 display, code %d",
                     result);
        return 0;
    }
    return 1;
}

int enable_and_reset_display(libusb_device_handle *devh) {
    uint8_t buf[1];
    int result;

    SDL_Log("Enabling and resetting M8 display\n");

    buf[0] = 0x44;
    result = blocking_write(devh, buf, 1, 5);
    if (result != 1) {
        SDL_LogError(SDL_LOG_CATEGORY_SYSTEM, "Error enabling M8 display, code %d",
                     result);
        return 0;
    }

    SDL_Delay(5);
    result = reset_display(devh);
    if (result == 1)
        return 1;
    else
        return 0;
}

int disconnect(libusb_device_handle *devh) {
    char buf[1] = {'D'};
    int result;

    SDL_Log("Disconnecting M8\n");

    result = blocking_write(devh, buf, 1, 5);
    if (result != 1) {
        SDL_LogError(SDL_LOG_CATEGORY_SYSTEM, "Error sending disconnect, code %d",
                     result);
        return -1;
    }
    return 1;
}

int send_msg_controller(libusb_device_handle *devh, uint8_t input) {
    char buf[2] = {'C', input};
    size_t nbytes = 2;
    int result;
    result = blocking_write(devh, buf, nbytes, 5);
    if (result != nbytes) {
        SDL_LogError(SDL_LOG_CATEGORY_SYSTEM, "Error sending input, code %d",
                     result);
        return -1;
    }
    return 1;
}

int send_msg_keyjazz(libusb_device_handle *devh, uint8_t note, uint8_t velocity) {
    if (velocity > 0x7F)
        velocity = 0x7F;
    char buf[3] = {'K', note, velocity};
    size_t nbytes = 3;
    int result;
    result = blocking_write(devh, buf, nbytes, 5);
    if (result != nbytes) {
        SDL_LogError(SDL_LOG_CATEGORY_SYSTEM, "Error sending keyjazz, code %d",
                     result);
        return -1;
    }

    return 1;
}
