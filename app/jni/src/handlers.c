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
    const char *path;
    path = (*env)->GetStringUTFChars(env, audio_driver, NULL);
    SDL_Log("Setting audio driver to %s", path);
    SDL_SetHint(SDL_HINT_AUDIODRIVER, path);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_resetScreen(JNIEnv *env, jobject thiz) {
    reset_display();
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_exit(JNIEnv *env, jobject thiz) {
    SDL_Event sdlevent = {};
    sdlevent.type = SDL_KEYDOWN;
    sdlevent.key.keysym.sym = SDLK_F4;
    sdlevent.key.keysym.mod = KMOD_ALT;
    SDL_PushEvent(&sdlevent);
}