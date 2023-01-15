#include <libusb.h>
#include <errno.h>
#include <SDL.h>
#include <serial.h>

#define EP_ISO_IN 0x85
#define IFACE_NUM 4

#define NUM_TRANSFERS 10
#define PACKET_SIZE 180
#define NUM_PACKETS 10

static unsigned long num_bytes = 0, num_xfer = 0;
static struct timeval tv_start;

SDL_AudioDeviceID audio_device_id = 0;

static int do_exit = 1;

static void cb_xfr(struct libusb_transfer *xfr) {
    unsigned int i;

    int len = 0;

    for (i = 0; i < xfr->num_iso_packets; i++) {
        struct libusb_iso_packet_descriptor *pack = &xfr->iso_packet_desc[i];

        if (pack->status != LIBUSB_TRANSFER_COMPLETED) {
            SDL_Log("Error (status %d: %s) :", pack->status,
                    libusb_error_name(pack->status));
            /* This doesn't happen, so bail out if it does. */
            return;
        }

        const uint8_t *data = libusb_get_iso_packet_buffer_simple(xfr, i);

        SDL_QueueAudio(audio_device_id, data, pack->actual_length);

        len += pack->length;
    }

    num_bytes += len;
    num_xfer++;

    if (libusb_submit_transfer(xfr) < 0) {
        SDL_Log("error re-submitting URB\n");
    }
}

static int benchmark_in(libusb_device_handle *devh, uint8_t ep) {
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
            SDL_Log("Could not allocate transfer");
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

int aaudio_device_id = 0;

void set_audio_device(int device_id) {
    aaudio_device_id = device_id;
}

int audio_setup(libusb_device_handle *devh) {
    SDL_Log("USB audio setup");

    int rc;

    rc = libusb_kernel_driver_active(devh, IFACE_NUM);
    if (rc == 1) {
        SDL_Log("Detaching kernel driver");
        rc = libusb_detach_kernel_driver(devh, IFACE_NUM);
        if (rc < 0) {
            SDL_Log("Could not detach kernel driver: %s\n",
                    libusb_error_name(rc));
            return rc;
        }
    }

    rc = libusb_claim_interface(devh, IFACE_NUM);
    if (rc < 0) {
        SDL_Log("Error claiming interface: %s\n", libusb_error_name(rc));
        return rc;
    }

    rc = libusb_set_interface_alt_setting(devh, IFACE_NUM, 1);
    if (rc < 0) {
        SDL_Log("Error setting alt setting: %s\n", libusb_error_name(rc));
        return rc;
    }


// Good to go
    do_exit = 0;
    SDL_Log("Starting capture");
    if ((rc = benchmark_in(devh, EP_ISO_IN)) < 0) {
        SDL_Log("Capture failed to start: %d", rc);
        return rc;
    }

    if (!SDL_WasInit(SDL_INIT_AUDIO)) {
        if (SDL_InitSubSystem(SDL_INIT_AUDIO) < 0) {
            SDL_Log("Init audio failed %s", SDL_GetError());
            return -1;
        }
    } else {
        SDL_Log("Audio was already initialised");
    }

    static SDL_AudioSpec audio_spec;
    audio_spec.format = AUDIO_S16;
    audio_spec.channels = 2;
    audio_spec.freq = 44100;

    SDL_AudioSpec _obtained;
    SDL_zero(_obtained);

    SDL_Log("Current audio driver is %s and device %d", SDL_GetCurrentAudioDriver(),
            aaudio_device_id);

    if (SDL_strcasecmp(SDL_GetCurrentAudioDriver(), "openslES") == 0 || aaudio_device_id == 0) {
        SDL_Log("Using default audio device");
        audio_device_id = SDL_OpenAudioDevice(NULL, 0, &audio_spec, &_obtained, 0);
    } else {
        int n = (int) (log10(aaudio_device_id) + 1);
        char audio_device_name[n];
        SDL_itoa(aaudio_device_id, audio_device_name, 10);

        audio_device_id = SDL_OpenAudioDevice(audio_device_name, 0, &audio_spec, &_obtained, 0);
    }

    SDL_PauseAudioDevice(audio_device_id, 0);

    SDL_Log("Successful init");
    return 1;
}

int audio_destroy(libusb_device_handle *devh) {
    SDL_Log("Closing audio");
    if (audio_device_id != 0) {
        SDL_CloseAudioDevice(audio_device_id);
    }
    SDL_Log("Audio closed");
    return 1;
}

void audio_loop() {
    while (!do_exit) {
        int rc = libusb_handle_events(NULL);
        if (rc != LIBUSB_SUCCESS) {
            break;
        }
    }
}

