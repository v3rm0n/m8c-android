#include <jni.h>
#include <serial.h>
#include "audio.h"
#include <SDL.h>

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_loop(JNIEnv *env, jobject thiz) {
    audio_loop();
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_connect(JNIEnv *env, jobject thiz,
                                             jint fd) {
    set_file_descriptor(fd);
    set_usb_init_callback(audio_setup);
    set_usb_destroy_callback(audio_destroy);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_sendClickEvent(JNIEnv *env, jobject thiz, jchar event) {
    send_msg_controller(event);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_disconnect(JNIEnv *env, jobject thiz) {
    set_file_descriptor(-1);
}

int android_main(int argc, char *argv[]) {
    SDL_SetHint(SDL_HINT_AUDIODRIVER, "android");
    return SDL_main(argc, argv);
}