package io.maido.m8client.m8

inline val Int.b get() = this.toByte()
inline val String.b get() = this.single().b
inline val Char.b get() = this.code.toByte()

inline val Char.hex get() = code.toString(16)
inline val Byte.hex get() = "0x${toUByte().toString(16)}"