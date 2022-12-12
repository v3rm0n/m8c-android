package au.id.jms.usbaudio;

public class UsbAudio {
    static {
        System.loadLibrary("usbaudio");
    }

    public native boolean setup(int fd);

    public native void close();

    public native void loop();

    public native boolean stop();

    public native int measure();

}