package io.maido.m8client

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.hardware.usb.*
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.media.*
import android.media.AudioTrack.WRITE_BLOCKING
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE
import com.hoho.android.usbserial.driver.UsbSerialPort.STOPBITS_1
import io.maido.m8client.m8.M8Key
import io.maido.m8client.m8.M8Device
import java.nio.ByteBuffer
import java.util.*
import java.util.Arrays.stream
import kotlin.concurrent.thread


class M8ScreenActivity : AppCompatActivity(R.layout.m8) {

    companion object {
        private const val TAG = "M8Activity"

        private val USB_DEVICE = M8ScreenActivity::class.simpleName + ".USB_DEVICE"
        private val AUDIO_DEVICE = M8ScreenActivity::class.simpleName + ".AUDIO_DEVICE"
        private const val audioEndpoint = 0x85
        private const val audioInterface = 4

        fun startM8SDLActivity(
            context: Context,
            usbDevice: UsbDevice,
            audioDeviceId: Int,
        ) {
            val sdlActivity = Intent(context, M8ScreenActivity::class.java)
            sdlActivity.putExtra(USB_DEVICE, usbDevice)
            sdlActivity.putExtra(AUDIO_DEVICE, audioDeviceId)
            context.startActivity(sdlActivity)
        }

        private val buttonMappings = mapOf(
            R.id.left to M8Key.LEFT,
            R.id.right to M8Key.RIGHT,
            R.id.up to M8Key.UP,
            R.id.down to M8Key.DOWN,
            R.id.shift to M8Key.SHIFT,
            R.id.play to M8Key.PLAY,
            R.id.option to M8Key.OPTION,
            R.id.edit to M8Key.EDIT,
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usbDevice = getUsbDevice()
        connectSerial(usbDevice)
        //connectAudio(usbDevice)
    }

    private fun startAudio(): AudioTrack {
        val audioDevice = intent.getIntExtra(AUDIO_DEVICE, 0)
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val sampleSize = 2
        val sampleRate = 44100
        val frameSize = (sampleSize * 2)
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes((minBufferSize + frameSize - 1) / frameSize)
            .build()
        if (audioDevice != 0) {
            audioTrack.preferredDevice = getInputAudioDeviceInfo(audioDevice)
        }
        audioTrack.play()
        return audioTrack
    }

    private fun getInputAudioDeviceInfo(deviceId: Int): AudioDeviceInfo? {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return stream(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS))
            .filter { deviceInfo -> deviceInfo.id == deviceId }
            .findFirst()
            .orElse(null)
    }

    @Suppress("Deprecation")
    private fun getUsbDevice(): UsbDevice {
        val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(USB_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(USB_DEVICE)
        } ?: throw RuntimeException("No device!")
        return usbDevice
    }

    private fun connectSerial(usbDevice: UsbDevice) {
        Log.d(TAG, "usbDevice $usbDevice")
        val manager = getSystemService(USB_SERVICE) as UsbManager
        val connection = manager.openDevice(usbDevice)
        val driver = CdcAcmSerialDriver(usbDevice)
        driver.ports.first().also { port ->
            port.open(connection)
            port.setParameters(115200, 8, STOPBITS_1, PARITY_NONE)
            Log.d(TAG, "Connected to M8")
            val m8 = M8Device(port) { finish() }
            val surface = findViewById<SurfaceView>(R.id.m8Surface)
            surface.holder.addCallback(m8)
            buttonMappings.forEach { (id, key) ->
                findViewById<View>(id).setOnTouchListener(
                    M8TouchListener(
                        key,
                        m8::sendCommand
                    )
                )
            }
        }
    }

    private val transfer = UsbRequest() to ByteBuffer.allocate(1800)

    private fun connectAudio(usbDevice: UsbDevice) {
        val manager = getSystemService(USB_SERVICE) as UsbManager
        val connection = manager.openDevice(usbDevice)
        val audioInterface = getAudioInterface(usbDevice)
        Log.d(TAG, "Audio interface $audioInterface endpoints: ${audioInterface.endpointCount}")
        connection.setInterface(audioInterface)
        if (connection.claimInterface(audioInterface, true)) {
            val endpoint = getAudioOutEndpoint(audioInterface)
            Log.d(TAG, "Audio endpoint type ${endpoint.type}")
            queueRequests(connection, endpoint)
            thread {
                val audioTrack = startAudio()
                while (true) {
                    val request = connection.requestWait()
                    if (request == transfer.first && transfer.second.remaining() > 0) {
                        audioTrack.write(
                            transfer.second,
                            transfer.second.remaining(),
                            WRITE_BLOCKING
                        )
                    }
                }
            }

        }
    }

    private fun getAudioInterface(usbDevice: UsbDevice): UsbInterface {
        return (0 until usbDevice.interfaceCount).map {
            usbDevice.getInterface(it)
        }.first { it.id == audioInterface && it.alternateSetting == 1 }
    }

    private fun getAudioOutEndpoint(usbInterface: UsbInterface): UsbEndpoint {
        return (0 until usbInterface.endpointCount).map {
            usbInterface.getEndpoint(it)
        }.first { it.address == audioEndpoint && it.type == USB_ENDPOINT_XFER_BULK }
    }

    private fun queueRequests(connection: UsbDeviceConnection, endpoint: UsbEndpoint) {
        val (request, buffer) = transfer
        if (request.initialize(connection, endpoint)) {
            request.queue(buffer)
        } else {
            Log.e(TAG, "Could not initialise request")
        }

    }

}