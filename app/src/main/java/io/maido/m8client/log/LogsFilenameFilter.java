package io.maido.m8client.log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

public class LogsFilenameFilter implements FilenameFilter {

    protected static Set<String> logFileNamesSet = new HashSet<String>();
    protected static Set<String> notGeneratedSystemFilesNameSet = new HashSet<String>();

    static {
        logFileNamesSet.add(AppFilesHelper.ANDROID_MANUAL_CLIENT_LOG);
        logFileNamesSet.add(AppFilesHelper.APP_INFO_FILE_NAME);
    }

    public Set<String> getFileNamesSet() {
        return logFileNamesSet;
    }

    public Set<String> getNotGeneratedSystemFileNamesSet() {
        return notGeneratedSystemFilesNameSet;
    }

    @Override
    public boolean accept(File dir, String filename) {
        return (logFileNamesSet.contains(filename));
    }

}