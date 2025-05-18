package com.hibiscusmc.hmcleaves.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class AdventureUtil {

    private AdventureUtil() {
        throw new UnsupportedOperationException();
    }

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component parse(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

}
