package io.maido.m8client

enum class M8Key(val code: Int) {
    EDIT(1),
    OPTION(1 shl 1),
    RIGHT(1 shl 2),
    PLAY(1 shl 3),
    SHIFT(1 shl 4),
    DOWN(1 shl 5),
    UP(1 shl 6),
    LEFT(1 shl 7);
}