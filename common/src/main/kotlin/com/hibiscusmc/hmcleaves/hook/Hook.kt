package com.hibiscusmc.hmcleaves.hook

interface Hook {

    fun id(): String

    fun isEnabled(): Boolean

}