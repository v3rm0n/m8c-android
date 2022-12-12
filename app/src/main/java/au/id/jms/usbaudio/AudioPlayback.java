package au.id.jms.usbaudio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayback {
	private static final String TAG = "AudioPlayback";
	
	private static final int SAMPLE_RATE_HZ = 48000;
	
	private static AudioTrack track = null;
		
	public static void setup() {
		Log.i(TAG, "Audio Playback");
		
		int bufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_HZ, 
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		Log.d(TAG, "Buf size: " + bufSize);
		
		track = new AudioTrack(AudioManager.STREAM_MUSIC,
				SAMPLE_RATE_HZ,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufSize,
				AudioTrack.MODE_STREAM);
		track.play();
	}
	
	public static void write(byte[] decodedAudio) {
		track.write(decodedAudio, 0, decodedAudio.length);
	}
}