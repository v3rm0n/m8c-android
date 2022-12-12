LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../m8c

LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.c)

LOCAL_CFLAGS += -DUSE_LIBUSB

LOCAL_SHARED_LIBRARIES := m8c SDL2 usb-1.0

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -lOpenSLES -llog -landroid

include $(BUILD_SHARED_LIBRARY)
