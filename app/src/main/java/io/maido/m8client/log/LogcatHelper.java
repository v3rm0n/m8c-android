package io.maido.m8client.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LogcatHelper {
    private static final long THREAD_DELAY = 1000;
    private static final String[] PROC_ARRAY = new String[]{"logcat", "-v",
            LogOutputFormat.THREADTIME.getOutputFormat()};
    private final AtomicBoolean saveSystemLog = new AtomicBoolean(false);
    private final AtomicBoolean prepareNewLogFile = new AtomicBoolean(false);
    private final AtomicReference<String> packageFilter = new AtomicReference<>();
    private Thread mLogCheckThread;
    private final Runnable logcatRunnable = new Runnable() {

        private final byte[] buf = new byte[LogSaveHelper.TEN_K];
        private InputStream mLogInputStream = null;
        private Process logcatProc;

        @Override
        public void run() {

            try {
                if (prepareNewLogFile.getAndSet(false)) {
                    LogSaveHelper.prepareLogFile();
                }
                String pkg = packageFilter.get();
                if (pkg != null) {
                    String[] customProcArray = new String[]{"logcat", "-v",
                            LogOutputFormat.THREADTIME.getOutputFormat(),
                            pkg + ":V"};
                    logcatProc = Runtime.getRuntime().exec(customProcArray);
                } else {
                    logcatProc = Runtime.getRuntime().exec(PROC_ARRAY);
                }
                mLogInputStream = logcatProc.getInputStream();

                while (true) {
                    LogSaveHelper.handleFilesState(prepareNewLogFile
                            .getAndSet(false));
                    int numRead;
                    int totalReadLeft = LogSaveHelper.TEN_K;
                    FileOutputStream logFileStream;
                    while (saveSystemLog.get()
                            && (logFileStream = LogSaveHelper
                            .getLogFileStream()) != null
                            && (numRead = mLogInputStream.read(buf)) >= 0) {
                        logFileStream.write(buf, 0, numRead);
                        logFileStream.flush();
                        totalReadLeft -= numRead;
                        if (totalReadLeft < 0) {
                            break;
                        }
                    }
                    if (totalReadLeft < 0) {
                        continue;
                    }
                    try {
                        Thread.sleep(THREAD_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mLogInputStream != null) {
                    try {
                        mLogInputStream.close();
                        mLogInputStream = null;
                    } catch (IOException e) {
                        mLogInputStream = null;
                        e.printStackTrace();
                    }
                }
                LogSaveHelper.closeLogFileStream();
                mLogCheckThread = null;
            }
        }
    };

    public void prepareNewLogFile() {
        prepareNewLogFile.set(true);
    }

    public void start(String filter) {
        packageFilter.set(filter);
        saveSystemLog.set(true);
        if (mLogCheckThread == null) {
            mLogCheckThread = new Thread(logcatRunnable);
            mLogCheckThread.setPriority(Thread.NORM_PRIORITY - 2);
            mLogCheckThread.start();
        }
    }


    public void stop() {
        saveSystemLog.set(false);
    }

}