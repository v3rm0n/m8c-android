LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := usbaudio

LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.c)

LOCAL_SHARED_LIBRARIES := usb-1.0

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -lOpenSLES -llog -landroid

include $(BUILD_SHARED_LIBRARY)