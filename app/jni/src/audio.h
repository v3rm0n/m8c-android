#ifndef M8CLIENT_AUDIO_H
#define M8CLIENT_AUDIO_H
#include <libusb.h>
void set_audio_device(int device_id);
int audio_setup(libusb_device_handle *devh);
int audio_destroy(libusb_device_handle *devh);
int audio_loop();
#endif //M8CLIENT_AUDIO_H
