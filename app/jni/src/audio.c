#include <libusb.h>
#include <errno.h>
#include <SDL.h>
#include <serial.h>

#define EP_ISO_IN 0x85
#define IFACE_NUM 4

#define NUM_TRANSFERS 10
#define PACKET_SIZE 180
#define NUM_PACKETS 64

SDL_AudioDeviceID sdl_audio_device_id = 0;

static int do_exit = 1;

static void cb_xfr(struct libusb_transfer *xfr) {
    unsigned int i;

    for (i = 0; i < xfr->num_iso_packets; i++) {
        struct libusb_iso_packet_descriptor *pack = &xfr->iso_packet_desc[i];

        if (pack->status != LIBUSB_TRANSFER_COMPLETED) {
            SDL_Log("XFR callback error (status %d: %s)", pack->status,
                    libusb_error_name(pack->status));
            /* This doesn't happen, so bail out if it does. */
            return;
        }

        const uint8_t *data = libusb_get_iso_packet_buffer_simple(xfr, i);

        if (sdl_audio_device_id != 0) {
            SDL_QueueAudio(sdl_audio_device_id, data, pack->actual_length);
        }
    }

    if (libusb_submit_transfer(xfr) < 0) {
        SDL_Log("error re-submitting URB\n");
    }
}

static struct libusb_transfer *xfr[NUM_TRANSFERS];

static int benchmark_in(libusb_device_handle *devh, uint8_t ep) {
    static uint8_t buf[PACKET_SIZE * NUM_PACKETS];
    int num_iso_pack = NUM_PACKETS;
    int i;

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

    return 1;
}

int audio_device_id = 0;
int audio_buffer_size = 2048;

void set_audio_device(int device_id, int buffer_size) {
    audio_device_id = device_id;
    audio_buffer_size = buffer_size;
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
    audio_spec.samples = 2 * audio_buffer_size;

    SDL_AudioSpec _obtained;
    SDL_zero(_obtained);

    SDL_Log("Current audio driver is %s and device %d", SDL_GetCurrentAudioDriver(),
            audio_device_id);

    if (SDL_strcasecmp(SDL_GetCurrentAudioDriver(), "openslES") == 0 || audio_device_id == 0) {
        SDL_Log("Using default audio device");
        sdl_audio_device_id = SDL_OpenAudioDevice(NULL, 0, &audio_spec, &_obtained, 0);
    } else {
        int n = (int) (log10(audio_device_id) + 1);
        char audio_device_name[n];
        SDL_itoa(audio_device_id, audio_device_name, 10);
        sdl_audio_device_id = SDL_OpenAudioDevice(audio_device_name, 0, &audio_spec, &_obtained, 0);
    }

    SDL_Log("Obtained audio spec. Sample rate: %d, channels: %d, samples: %d, size: %d",
            _obtained.freq,
            _obtained.channels,
            _obtained.samples, +_obtained.size);

    SDL_PauseAudioDevice(sdl_audio_device_id, 0);

    // Good to go
    do_exit = 0;
    SDL_Log("Starting capture");
    if ((rc = benchmark_in(devh, EP_ISO_IN)) < 0) {
        SDL_Log("Capture failed to start: %d", rc);
        return rc;
    }

    SDL_Log("Successful init");
    return 1;
}

int audio_destroy(libusb_device_handle *devh) {
    SDL_Log("Closing audio");

    int rc;

    do_exit = 1;

    SDL_Log("Freeing interface %d", IFACE_NUM);

    rc = libusb_release_interface(devh, IFACE_NUM);
    if (rc < 0) {
        SDL_Log("Error releasing interface: %s\n", libusb_error_name(rc));
        return rc;
    }

    if (sdl_audio_device_id != 0) {
        SDL_Log("Closing audio device %d", sdl_audio_device_id);
        SDL_AudioDeviceID device = sdl_audio_device_id;
        sdl_audio_device_id = 0;
        SDL_CloseAudioDevice(device);
    }

    SDL_Log("Audio closed");
    return 1;
}

void audio_loop() {
    while (!do_exit) {
        int rc = libusb_handle_events(NULL);
        if (rc != LIBUSB_SUCCESS) {
            SDL_Log("Audio loop error: %s\n", libusb_error_name(rc));
            break;
        }
    }
}

