package io.maido.m8client.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.maido.m8client.M8Application;

public class LogSaveHelper {
    public static final String ANDROID_MANUAL_CLIENT_LOG = "app.log";

    public static final int ONE_K = 1024;
    public static final int TEN_K = 10 * ONE_K;
    public static final long ONE_MB = 1024 * ONE_K;
    public static final long EIGHT_MB = 8 * ONE_MB;
    private static final String TAG = LogSaveHelper.class.getSimpleName();
    private static FileOutputStream mLogFileStream;
    private static File logFile;

    public static File prepareLogFile() {
        File f = null;
        try {
            f = new File(getCachePath(),
                    ANDROID_MANUAL_CLIENT_LOG);
            if (f.exists()) {
                f.delete();
            }
            f = new File(getCachePath(),
                    ANDROID_MANUAL_CLIENT_LOG);
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    public static File getLogFile() {
        return getCustomLogFile(ANDROID_MANUAL_CLIENT_LOG);
    }

    public static File getCustomLogFile(String fileName) {
        File f = null;
        try {
            f = new File(getCachePath(), fileName);
            if (!f.exists()) {
                f.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    public static FileOutputStream getLogFileStream() {
        if (mLogFileStream == null || logFile == null) {
            initLogWriter();
        }
        return mLogFileStream;
    }

    public static void handleFilesState(boolean prepare) {
        if (prepare || (logFile != null && logFile.length() > EIGHT_MB)) {
            closeLogFileStream();
            prepareLogFile();
            initLogWriter();
        }
    }

    public static File getCachePath() {
        return M8Application.getContext().getCacheDir();
    }

    private static void initLogWriter() {
        try {
            logFile = LogSaveHelper.getLogFile();
            mLogFileStream = new FileOutputStream(logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeLogFileStream() {
        if (mLogFileStream != null) {
            try {
                mLogFileStream.close();
                mLogFileStream = null;
                logFile = null;
            } catch (IOException e) {
                mLogFileStream = null;
                logFile = null;
                e.printStackTrace();
            }
        }
    }
}