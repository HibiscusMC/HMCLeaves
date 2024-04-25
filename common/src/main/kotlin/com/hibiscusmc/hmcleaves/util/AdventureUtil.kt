package com.hibiscusmc.hmcleaves.util

import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer


val ADVENTURE_SERIALIZER = LegacyComponentSerializer.builder()
    .hexColors()
    .useUnusualXRepeatedCharacterHexFormat()
    .build()

val MINI_MESAGE = MiniMessage.builder()
    .tags(StandardTags.defaults())
    .postProcessor { component -> component.decoration(TextDecoration.ITALIC, false) }
    .build()
