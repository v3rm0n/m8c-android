#include <SDL.h>
#include <jni.h>
#include <serial.h>
#include "audio.h"

int device_active = 0;

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_connect(JNIEnv *env, jobject thiz, jint fd) {
    device_active = 1;
    SDL_Log("Connecting to the device");
    init_serial_with_file_descriptor(fd);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_00024Companion_sendClickEvent(JNIEnv *env, jobject thiz,
                                                                     jchar event) {
    if (device_active) {
        SDL_Log("Sending message to M8");
        send_msg_controller(event);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_hintAudioDriver(JNIEnv *env, jobject thiz,
                                                     jstring audio_driver) {
    if (audio_driver != NULL) {
        const char *path;
        path = (*env)->GetStringUTFChars(env, audio_driver, NULL);
        SDL_Log("Setting audio driver to %s", path);
        SDL_SetHint(SDL_HINT_AUDIODRIVER, path);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_00024Companion_resetScreen(JNIEnv *env, jobject thiz) {
    if (device_active) {
        enable_and_reset_display();
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_00024Companion_exit(JNIEnv *env, jobject thiz) {
    device_active = 0;
    SDL_Log("Sending Alt+F4 to M8");
    SDL_Event sdlevent = {};
    sdlevent.type = SDL_KEYDOWN;
    sdlevent.key.keysym.sym = SDLK_F4;
    sdlevent.key.keysym.mod = KMOD_ALT;
    SDL_PushEvent(&sdlevent);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_lockOrientation(JNIEnv *env, jobject thiz, jboolean lock) {
    if (lock) {
        SDL_Log("Lock to landscape");
        SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");
    } else {
        SDL_Log("Don't lock orientation");
        SDL_SetHint(SDL_HINT_ORIENTATIONS,
                    "LandscapeLeft LandscapeRight Portrait PortraitUpsideDown");
    }
}