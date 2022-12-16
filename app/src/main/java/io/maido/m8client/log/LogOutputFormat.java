package io.maido.m8client.log;

public enum LogOutputFormat {
    BRIEF(0), PROCESS(1), TAG(2), RAW(3), TIME(4), THREADTIME(5), LONG(6);
    private int value;

    LogOutputFormat(int value) {
        this.value = value;
    }

    public String getOutputFormat() {
        String format = "brief";
        switch (this) {
            case BRIEF:
                format = "brief";
                break;
            case LONG:
                format = "long";
                break;
            case PROCESS:
                format = "process";
                break;
            case RAW:
                format = "raw";
                break;
            case TAG:
                format = "tag";
                break;
            case THREADTIME:
                format = "threadtime";
                break;
            case TIME:
                format = "time";
                break;
        }
        return format;
    }

    public int toInt() {
        return value;
    }
}