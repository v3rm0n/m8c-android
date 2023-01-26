package io.maido.m8client

import android.content.Context
import android.hardware.usb.UsbDevice
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object M8Util {
    fun isM8(usbDevice: UsbDevice): Boolean {
        return usbDevice.productName == "M8"
    }

    fun copyGameControllerDB(context: Context) {
        copyFile(context, "config.ini", "/data/data/io.maido.m8client/files/config.ini")
        copyFile(
            context,
            "gamecontrollerdb.txt",
            "/data/data/io.maido.m8client/files/gamecontrollerdb.txt"
        )
    }

    private fun copyFile(context: Context, sourceFileName: String, destFileName: String): Boolean {
        val assetManager = context.assets
        val destFile = File(destFileName)
        destFile.parentFile?.mkdir()
        val `in`: InputStream
        val out: OutputStream
        try {
            `in` = assetManager.open(sourceFileName)
            out = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            out.flush()
            out.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}