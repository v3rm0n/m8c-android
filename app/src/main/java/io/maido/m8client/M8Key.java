package io.maido.m8client;

import java.util.Set;

enum M8Key {
    EDIT(1),
    OPTION(1 << 1),
    RIGHT(1 << 2),
    PLAY(1 << 3),
    SHIFT(1 << 4),
    DOWN(1 << 5),
    UP(1 << 6),
    LEFT(1 << 7);

    private final int code;

    M8Key(int code) {
        this.code = code;
    }

    public char getCode(Set<M8Key> modifiers) {
        return (char) modifiers.stream()
                .map(t -> t.code)
                .reduce(code, (in, out) -> in | out)
                .intValue();
    }
}
