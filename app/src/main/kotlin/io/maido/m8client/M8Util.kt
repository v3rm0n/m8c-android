package io.maido.m8client

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object M8Util {

    @SuppressLint("SdCardPath")
    // This is needed, recommendation doesn't work with SDL_GetPrefPath
    private val basePath = "/data/data/io.maido.m8client/files"
    fun isM8(usbDevice: UsbDevice) = usbDevice.productName == "M8"

    fun copyGameControllerDB(context: Context) {
        Log.d("CONTEXT", context.filesDir.path)
        copyFile(context, "config.ini", "$basePath/config.ini")
        copyFile(
            context,
            "gamecontrollerdb.txt",
            "$basePath/gamecontrollerdb.txt"
        )
    }

    private fun copyFile(context: Context, sourceFileName: String, destFileName: String) {
        val assetManager = context.assets
        val destFile = File(destFileName)
        destFile.parentFile?.mkdir()
        assetManager.open(sourceFileName).use { input ->
            FileOutputStream(destFile).use { out ->
                val buffer = ByteArray(1024)
                var read = input.read(buffer)
                while (read != -1) {
                    out.write(buffer, 0, read)
                    read = input.read(buffer)
                }
                out.flush()
            }
        }
    }
}