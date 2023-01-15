#include <jni.h>
#include <serial.h>
#include "audio.h"
#include "SDL.h"

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_loop(JNIEnv *env, jobject thiz) {
    audio_loop();
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_connect(JNIEnv *env, jobject thiz,
                                             jint fd, jint audiodevice) {
    set_audio_device(audiodevice);
    set_usb_init_callback(audio_setup);
    set_usb_destroy_callback(audio_destroy);
    init_serial_with_file_descriptor(fd);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_sendClickEvent(JNIEnv *env, jobject thiz, jchar event) {
    send_msg_controller(event);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_setAudioDriver(JNIEnv *env, jobject thiz,
                                                    jstring audio_driver) {
    SDL_SetHint(SDL_HINT_AUDIODRIVER, audio_driver);
}