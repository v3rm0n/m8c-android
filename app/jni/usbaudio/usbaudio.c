/*
 *
 * Dumb userspace USB Audio receiver
 * Copyright 2012 Joel Stanley <joel@jms.id.au>
 *
 * Based on the following:
 *
 * libusb example program to measure Atmel SAM3U isochronous performance
 * Copyright (C) 2012 Harald Welte <laforge@gnumonks.org>
 *
 * Copied with the author's permission under LGPL-2.1 from
 * http://git.gnumonks.org/cgi-bin/gitweb.cgi?p=sam3u-tests.git;a=blob;f=usb-benchmark-project/host/benchmark.c;h=74959f7ee88f1597286cd435f312a8ff52c56b7e
 *
 * An Atmel SAM3U test firmware is also available in the above repository.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <stdbool.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <libusb.h>

#include <jni.h>

#include <android/log.h>

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, "UsbAudioNative", __VA_ARGS__)

#define UNUSED __attribute__((unused))

#define EP_ISO_IN    0x85
#define IFACE_NUM   4

static int do_exit = 1;
static struct libusb_device_handle *devh = NULL;

static unsigned long num_bytes = 0, num_xfer = 0;
static struct timeval tv_start;

static JavaVM *java_vm = NULL;

static jclass au_id_jms_usbaudio_AudioPlayback = NULL;
static jmethodID au_id_jms_usbaudio_AudioPlayback_write;

static void cb_xfr(struct libusb_transfer *xfr) {
    unsigned int i;

    int len = 0;

    // Get an env handle
    JNIEnv *env;
    void *void_env;
    bool had_to_attach = false;
    jint status = (*java_vm)->GetEnv(java_vm, &void_env, JNI_VERSION_1_6);

    if (status == JNI_EDETACHED) {
        (*java_vm)->AttachCurrentThread(java_vm, &env, NULL);
        had_to_attach = true;
    } else {
        env = void_env;
    }

    jbyteArray audioByteArray = (*env)->NewByteArray(env, 192 * xfr->num_iso_packets);

    for (i = 0; i < xfr->num_iso_packets; i++) {
        struct libusb_iso_packet_descriptor *pack = &xfr->iso_packet_desc[i];

        if (pack->status != LIBUSB_TRANSFER_COMPLETED) {
            LOGD("Error (status %d: %s) :", pack->status,
                 libusb_error_name(pack->status));
            /* This doesn't happen, so bail out if it does. */
            exit(EXIT_FAILURE);
        }

        const uint8_t *data = libusb_get_iso_packet_buffer_simple(xfr, i);
        (*env)->SetByteArrayRegion(env, audioByteArray, len, pack->length, data);

        len += pack->length;
    }

    // Call write()
    (*env)->CallStaticVoidMethod(env, au_id_jms_usbaudio_AudioPlayback,
                                 au_id_jms_usbaudio_AudioPlayback_write, audioByteArray);
    (*env)->DeleteLocalRef(env, audioByteArray);
    if ((*env)->ExceptionCheck(env)) {
        LOGD("Exception while trying to pass sound data to java");
        return;
    }

    num_bytes += len;
    num_xfer++;

    if (had_to_attach) {
        (*java_vm)->DetachCurrentThread(java_vm);
    }


    if (libusb_submit_transfer(xfr) < 0) {
        LOGD("error re-submitting URB\n");
        exit(1);
    }
}

#define NUM_TRANSFERS 10
#define PACKET_SIZE 180
#define NUM_PACKETS 10

static int benchmark_in(uint8_t ep) {
    static uint8_t buf[PACKET_SIZE * NUM_PACKETS];
    static struct libusb_transfer *xfr[NUM_TRANSFERS];
    int num_iso_pack = NUM_PACKETS;
    int i;

    /* NOTE: To reach maximum possible performance the program must
     * submit *multiple* transfers here, not just one.
     *
     * When only one transfer is submitted there is a gap in the bus
     * schedule from when the transfer completes until a new transfer
     * is submitted by the callback. This causes some jitter for
     * isochronous transfers and loss of throughput for bulk transfers.
     *
     * This is avoided by queueing multiple transfers in advance, so
     * that the host controller is always kept busy, and will schedule
     * more transfers on the bus while the callback is running for
     * transfers which have completed on the bus.
     */
    for (i = 0; i < NUM_TRANSFERS; i++) {
        xfr[i] = libusb_alloc_transfer(num_iso_pack);
        if (!xfr[i]) {
            LOGD("Could not allocate transfer");
            return -ENOMEM;
        }

        libusb_fill_iso_transfer(xfr[i], devh, ep, buf,
                                 sizeof(buf), num_iso_pack, cb_xfr, NULL, 1000);
        libusb_set_iso_packet_lengths(xfr[i], sizeof(buf) / num_iso_pack);

        libusb_submit_transfer(xfr[i]);
    }

    gettimeofday(&tv_start, NULL);

    return 1;
}

unsigned int measure(void) {
    struct timeval tv_stop;
    unsigned int diff_msec;

    gettimeofday(&tv_stop, NULL);

    diff_msec = (tv_stop.tv_sec - tv_start.tv_sec) * 1000;
    diff_msec += (tv_stop.tv_usec - tv_start.tv_usec) / 1000;

    LOGD("%lu transfers (total %lu bytes) in %u miliseconds => %lu bytes/sec\n",
         num_xfer, num_bytes, diff_msec, (num_bytes * 1000) / diff_msec);

    return num_bytes;
}

JNIEXPORT jint JNICALL
Java_au_id_jms_usbaudio_UsbAudio_measure(JNIEnv *env UNUSED, jobject foo UNUSED) {
    return measure();
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved UNUSED) {
    LOGD("libusbaudio: loaded");
    java_vm = vm;

    return JNI_VERSION_1_6;
}


JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved UNUSED) {
    JNIEnv *env;
    void *void_env;
    (*java_vm)->GetEnv(vm, &void_env, JNI_VERSION_1_6);
    env = void_env;

    (*env)->DeleteGlobalRef(env, au_id_jms_usbaudio_AudioPlayback);

    LOGD("libusbaudio: unloaded");
}

JNIEXPORT jboolean JNICALL
Java_au_id_jms_usbaudio_UsbAudio_setup(JNIEnv *env UNUSED, jobject foo UNUSED, jint fd) {
    LOGD("UsbAudio setup");

    int rc;

    rc = libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    if (rc != LIBUSB_SUCCESS) {
        LOGD("libusb_set_option failed: %s\n", libusb_error_name(rc));
        return rc;
    }

    rc = libusb_init(NULL);
    if (rc < 0) {
        LOGD("Error initializing libusb: %s\n", libusb_error_name(rc));
        return false;
    }

    rc = libusb_wrap_sys_device(NULL, (intptr_t) fd, &devh);
    if (rc < 0) {
        LOGD("libusb_wrap_sys_device failed: %d\n", rc);
        return rc;
    } else if (devh == NULL) {
        LOGD("libusb_wrap_sys_device returned invalid handle\n");
        return rc;
    }

    LOGD("Detaching kernel driver");

    rc = libusb_kernel_driver_active(devh, IFACE_NUM);
    if (rc == 1) {
        rc = libusb_detach_kernel_driver(devh, IFACE_NUM);
        if (rc < 0) {
            LOGD("Could not detach kernel driver: %s\n",
                 libusb_error_name(rc));
            libusb_close(devh);
            libusb_exit(NULL);
            return false;
        }
    }

    rc = libusb_claim_interface(devh, IFACE_NUM);
    if (rc < 0) {
        LOGD("Error claiming interface: %s\n", libusb_error_name(rc));
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }

    rc = libusb_set_interface_alt_setting(devh, IFACE_NUM, 1);
    if (rc < 0) {
        LOGD("Error setting alt setting: %s\n", libusb_error_name(rc));
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }

    // Get write callback handle
    jclass clazz = (*env)->FindClass(env, "au/id/jms/usbaudio/AudioPlayback");
    if (!clazz) {
        LOGD("Could not find au.id.jms.usbaudio.AudioPlayback");
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }
    au_id_jms_usbaudio_AudioPlayback = (*env)->NewGlobalRef(env, clazz);

    au_id_jms_usbaudio_AudioPlayback_write = (*env)->GetStaticMethodID(env,
                                                                       au_id_jms_usbaudio_AudioPlayback,
                                                                       "write", "([B)V");
    if (!au_id_jms_usbaudio_AudioPlayback_write) {
        LOGD("Could not find au.id.jms.usbaudio.AudioPlayback");
        (*env)->DeleteGlobalRef(env, au_id_jms_usbaudio_AudioPlayback);
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }


    // Good to go
    do_exit = 0;
    LOGD("Starting capture");
    if ((rc = benchmark_in(EP_ISO_IN)) < 0) {
        LOGD("Capture failed to start: %d", rc);
        return false;
    }

    LOGD("Successful init");
    return true;
}


JNIEXPORT void JNICALL
Java_au_id_jms_usbaudio_UsbAudio_stop(JNIEnv *env UNUSED, jobject foo UNUSED) {
    do_exit = 1;
    measure();
}

JNIEXPORT bool JNICALL
Java_au_id_jms_usbaudio_UsbAudio_close(JNIEnv *env UNUSED, jobject foo UNUSED) {
    if (do_exit == 0) {
        return false;
    }
    libusb_release_interface(devh, IFACE_NUM);
    if (devh)
        libusb_close(devh);
    libusb_exit(NULL);
    return true;
}


JNIEXPORT void JNICALL
Java_au_id_jms_usbaudio_UsbAudio_loop(JNIEnv *env UNUSED, jobject foo UNUSED) {
    while (!do_exit) {
        int rc = libusb_handle_events(NULL);
        if (rc != LIBUSB_SUCCESS) {
            break;
        }
    }
}
