package io.maido.m8client;

import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class M8Util {

    public static boolean isM8(UsbDevice usbDevice) {
        return usbDevice.getProductName().equals("M8");
    }

    public static void copyConfigurationFiles(Context context) {
        copyFile(context, "gamecontrollerdb.txt", "/data/data/io.maido.m8client/files/gamecontrollerdb.txt");
        copyFile(context, "config.ini", "/data/data/io.maido.m8client/files/config.ini");
    }

    private static boolean copyFile(Context context, String sourceFileName, String destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);

        File destParentDir = destFile.getParentFile();
        destParentDir.mkdir();

        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(sourceFileName);
            out = new FileOutputStream(destFile);


            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
