#include <SDL3/SDL.h>
#include <jni.h>
#include "src/backends/m8.h"
#include "src/backends/audio.h"

extern int init_serial_with_file_descriptor(int file_descriptor);

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
        m8_send_msg_controller(event);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_hintAudioDriver(JNIEnv *env, jobject thiz,
                                                     jstring audio_driver) {
    if (audio_driver != NULL) {
        const char *path;
        path = (*env)->GetStringUTFChars(env, audio_driver, NULL);
        SDL_Log("Setting audio driver to %s", path);
        SDL_SetHint(SDL_HINT_AUDIO_DRIVER, path);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_hintAudioOutputDevice(JNIEnv *env, jobject thiz,
                                                           jint device_id) {
    if (device_id > 0) {
        char buf[32];
        SDL_snprintf(buf, sizeof(buf), "%d", device_id);
        SDL_Log("Setting audio output device id to %s", buf);
        SDL_SetHint("SDL_ANDROID_AUDIO_DEVICE_ID", buf);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_restartAudioOutput(JNIEnv *env, jobject thiz,
                                                        jint buffer_size) {
    SDL_Log("Restarting audio output, buffer_size=%d", buffer_size);
    audio_close();
    audio_initialize(NULL, (unsigned int)buffer_size);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_00024Companion_resetScreen(JNIEnv *env, jobject thiz) {
    if (device_active) {
        m8_reset_display();
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8TouchListener_00024Companion_exit(JNIEnv *env, jobject thiz) {
    device_active = 0;
    SDL_Log("Sending Alt+F4 to M8");
    SDL_Event sdlevent = {};
    sdlevent.type = SDL_EVENT_KEY_DOWN;
    sdlevent.key.key = SDLK_F4;
    sdlevent.key.mod = SDL_KMOD_ALT;
    SDL_PushEvent(&sdlevent);
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_hintAudioInputDevice(JNIEnv *env, jobject thiz,
                                                           jint device_id) {
    if (device_id > 0) {
        char buf[32];
        SDL_snprintf(buf, sizeof(buf), "%d", device_id);
        SDL_Log("Setting audio input device id to %s", buf);
        SDL_SetHint("SDL_ANDROID_AUDIO_CAPTURE_DEVICE_ID", buf);
    }
}

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_lockOrientation(JNIEnv *env, jobject thiz, jstring lock) {
    if (lock == NULL) {
        SDL_Log("Don't lock orientation");
        SDL_SetHint(SDL_HINT_ORIENTATIONS,
                    "LandscapeLeft LandscapeRight Portrait PortraitUpsideDown");
    } else {
        const char *lockOrientation;
        lockOrientation = (*env)->GetStringUTFChars(env, lock, NULL);
        SDL_Log("Lock orientation to %s", lockOrientation);
        SDL_SetHint(SDL_HINT_ORIENTATIONS, lockOrientation);
    }
}