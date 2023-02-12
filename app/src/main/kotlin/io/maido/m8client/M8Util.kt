package io.maido.m8client

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object M8Util {

    @SuppressLint("SdCardPath")
    // This is needed, recommendation doesn't work with SDL_GetPrefPath
    private val basePath = "/data/data/io.maido.m8client/files"
    fun isM8(usbDevice: UsbDevice) = usbDevice.productName == "M8"

    fun copyGameControllerDB(context: Context) {
        copyFile("gamecontrollerdb.txt", openFile(context, "gamecontrollerdb.txt"))
    }

    fun openFile(context: Context, fileName: String): InputStream {
        return context.assets.open(fileName)
    }

    fun copyFile(fileName: String, content: InputStream) {
        val destFile = File("$basePath/$fileName")
        destFile.parentFile?.mkdir()
        content.use { input ->
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