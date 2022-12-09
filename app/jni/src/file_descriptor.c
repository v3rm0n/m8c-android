#include <jni.h>
#include <serial.h>

JNIEXPORT void JNICALL
Java_io_maido_m8client_M8SDLActivity_setFileDescriptor(JNIEnv *env, jobject thiz,
                                                       jint fd) {
    set_file_descriptor(fd);
}