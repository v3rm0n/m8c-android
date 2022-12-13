package io.maido.m8client;

import java.util.Set;

enum M8Keys {
    UP(1 << 6),
    DOWN(1 << 5),
    LEFT(1 << 7),
    RIGHT(1 << 2),
    SHIFT(1 << 4),
    PLAY(1 << 3),
    OPTION(1 << 1),
    EDIT(1);

    private final int code;

    M8Keys(int code) {
        this.code = code;
    }

    public char getCode() {
        return (char) code;
    }

    public char getCode(Set<M8Keys> modifiers) {
        if (modifiers.isEmpty()) {
            return (char) code;
        }
        int initial = code;
        for (M8Keys modifier : modifiers) {
            initial |= modifier.code;
        }
        return (char) initial;
    }
}
