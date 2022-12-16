package io.maido.m8client.log;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.maido.m8client.M8Application;

public class AppFilesHelper {
    public static final String APP_INFO_FILE_NAME = "appInfo.txt";
    public static final String ANDROID_MANUAL_CLIENT_LOG = "app.log";
    public static final String A_NEW_LINE = "\n";
    public static final String SPACE = "_";

    public static String constructAppInfoFile() {
        PackageInfo pInfo = null;
        try {
            pInfo = M8Application.getContext().getPackageManager().getPackageInfo(M8Application.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = pInfo.versionName;
        StringBuilder email = new StringBuilder()
                .append(Build.MODEL)
                .append(AppFilesHelper.A_NEW_LINE)
                .append(Build.VERSION.RELEASE)
                .append(AppFilesHelper.SPACE)
                .append(Build.VERSION.SDK_INT)
                .append(AppFilesHelper.A_NEW_LINE)
                .append(Build.DEVICE)
                .append(AppFilesHelper.A_NEW_LINE)
                .append(Build.CPU_ABI)
                .append(AppFilesHelper.A_NEW_LINE)
                .append(version);

        return email.toString();
    }

    public static void addAppInfoFile(String fileData, String fileName) {
        File reportFile = null;
        FileWriter fileWriter = null;
        final File cacheDir = M8Application.getContext().getCacheDir();
        try {
            reportFile = new File(cacheDir, fileName);
            if (reportFile.exists()) {
                reportFile.delete();
            }
            if (!reportFile.exists()) {
                reportFile.createNewFile();
                fileWriter = new FileWriter(reportFile);
                fileWriter.write(fileData);
                fileWriter.flush();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void collectFiles(String fileName,
                                    LogsFilenameFilter filter) {
        final File cache = M8Application.getContext().getCacheDir();
        final File[] files;
        if (cache != null && cache.exists() && cache.isDirectory()
                && (files = cache.listFiles(filter)) != null
                && files.length > 0) {

            File targetLocation = new File(cache, fileName);
            try {
                zipFiles(files, targetLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void zipFiles(File[] sourceLocation, File targetLocation)
            throws IOException {
        if (targetLocation.exists()) {
            targetLocation.delete();
        }
        if (!targetLocation.exists()) {
            try {
                targetLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ZipOutputStream zos = null;
            FileInputStream in = null;
            try {
                OutputStream os = new FileOutputStream(targetLocation);
                zos = new ZipOutputStream(new BufferedOutputStream(os));
                for (File f : sourceLocation) {
                    if (f.isDirectory()) {
                        File[] logsFiles = f.listFiles();
                        for (File file : logsFiles) {
                            ZipEntry entry = new ZipEntry(file.getName());
                            zos.putNextEntry(entry);
                            in = new FileInputStream(file);
                            byte[] buffer = new byte[1024];
                            int length = 0;
                            while ((length = in.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            in.close();
                            zos.closeEntry();
                        }
                    } else {
                        ZipEntry entry = new ZipEntry(f.getName());
                        zos.putNextEntry(entry);
                        in = new FileInputStream(f);
                        byte[] buffer = new byte[1024];
                        int length = 0;
                        while ((length = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        in.close();
                        zos.closeEntry();
                    }
                }
            } finally {
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException ioe) {

                    }
                }
            }
        }
    }

    public static void copyFile(File sourceLocation, File targetLocation) {
        if (targetLocation.exists()) {
            targetLocation.delete();
        }
        if (!targetLocation.exists()) {
            try {
                targetLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(sourceLocation);
                out = new FileOutputStream(targetLocation);
                byte[] buffer = new byte[1024];
                int length = 0;
                try {
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                } catch (IOException ioe) {

                }
            } catch (FileNotFoundException fnfe) {

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe) {

                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ioe) {

                    }
                }
            }
        }
    }
}