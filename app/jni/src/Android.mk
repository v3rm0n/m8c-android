LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../m8c

LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.c)

LOCAL_SHARED_LIBRARIES := m8c

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -lOpenSLES -llog -landroid

include $(BUILD_SHARED_LIBRARY)
