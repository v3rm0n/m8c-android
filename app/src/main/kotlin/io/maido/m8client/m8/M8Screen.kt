package io.maido.m8client.m8

import android.graphics.*
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.createBitmap
import android.graphics.Paint.EMBEDDED_BITMAP_TEXT_FLAG
import android.graphics.Paint.LINEAR_TEXT_FLAG
import android.graphics.PorterDuff.Mode.SRC_IN
import android.util.Log
import io.maido.m8client.m8.Font.Companion.CHAR_HEIGHT
import io.maido.m8client.m8.Font.Companion.CHAR_WIDTH
import io.maido.m8client.m8.M8Command.*
import java.nio.ByteBuffer

class M8Screen {

    companion object {
        private val font = Font()
        private val waveformBackground = Rect(0, 0, 320, 21)
        private val black = Paint().also { it.setARGB(255, 0, 0, 0) }
    }

    val screen: Bitmap = createBitmap(320, 240, ARGB_8888)
    private val screenCanvas = Canvas(screen)

    fun draw(frame: ByteBuffer) {
        when (M8Command.fromBuffer(frame)) {
            DRAW_WAVEFORM -> drawWaveform(frame)
            DRAW_CHARACTER -> drawCharacter(frame)
            DRAW_RECTANGLE -> drawRectangle(frame)
            else -> {
                Log.d("M8", "Command not found!")
            }
        }
    }

    private fun drawRectangle(frame: ByteBuffer) {
        val (x, y) = frame.getCoordinates()
        val (width, height) = frame.getSize()
        val color = frame.getPaint()

        val rect = Rect(x, y, x + width, y + height)
        screenCanvas.drawRect(rect, color)
    }

    private fun drawCharacter(frame: ByteBuffer) {
        val character = frame.getCharacter()
        val (x, y) = frame.getCoordinates()
        val foreground = frame.getPaint()
        val background = frame.getPaint()

        if (foreground != background) {
            val bitmapRect = Rect(x - 1, y + 2, x + CHAR_WIDTH - 1, y + CHAR_HEIGHT + 1)
            screenCanvas.drawRect(bitmapRect, background)
        }

        val paint = Paint()
        val filter = PorterDuffColorFilter(foreground.color, SRC_IN)
        paint.colorFilter = filter

        screenCanvas.drawBitmap(font.getChar(character), x.toFloat(), y.toFloat() + 2, paint)
    }

    private fun drawWaveform(frame: ByteBuffer) {
        val paint = frame.getPaint()
        val waveform = ByteArray(frame.remaining())
        frame.get(waveform)

        val points = FloatArray(waveform.size * 2)
        waveform.forEachIndexed { i, byte ->
            points[i * 2] = i.toFloat()
            points[i * 2 + 1] = if (byte > 20) 20F else byte.toFloat()
        }
        screenCanvas.drawRect(waveformBackground, black)
        screenCanvas.drawPoints(points, paint)
    }

    private fun ByteBuffer.getPaint(): Paint {
        val paint = Paint(LINEAR_TEXT_FLAG or EMBEDDED_BITMAP_TEXT_FLAG)
        paint.setARGB(
            255, get().toUByte().toInt(), get().toUByte().toInt(), get().toUByte().toInt()
        )
        return paint
    }

    private fun ByteBuffer.getCoordinates() = short.toInt() to short.toInt()

    private fun ByteBuffer.getSize() = short to short

    private fun ByteBuffer.getCharacter() = get().toUByte().toInt()
}


