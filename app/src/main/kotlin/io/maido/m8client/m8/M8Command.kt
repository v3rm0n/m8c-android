package io.maido.m8client.m8

import java.nio.ByteBuffer

enum class M8Command(val cmd: Byte, val lengths: List<Int>) {
    // @formatter:off
    DRAW_RECTANGLE(0xFE.b, listOf(12)),
    DRAW_CHARACTER(0xFD.b, listOf(12)),
    DRAW_WAVEFORM(0xFC.b, listOf(1 + 3, 1 + 3 + 320)),
    JOYPAD_KEYPRESSED_STATE(0xFB.b, listOf(2,3));
    // @formatter:on

    companion object {
        private val valuesMap = values().associateBy { it.cmd }
        fun fromBuffer(buffer: ByteBuffer) = valuesMap[buffer.get()]?.also { cmd ->
            if (!cmd.lengths.contains(buffer.remaining() + 1)) {
                throw IllegalArgumentException("Command ${cmd.cmd.hex} length incorrect: ${buffer.remaining() + 1} expected IN ${cmd.lengths}")
            }
        }
    }
}
