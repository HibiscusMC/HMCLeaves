package com.hibiscusmc.hmcleaves.block

import org.bukkit.Axis
import java.util.EnumMap

class BlockFamily(
    private val family: MutableMap<Type, String>
) {

    constructor() : this(EnumMap(Type::class.java)) {

    }

    constructor(vararg pairs: Pair<Type, String>) : this() {
        this.family.putAll(pairs)
    }

    enum class Type {

        STRIPPED,
        NOT_STRIPPED,
        PLANT,
        NOT_PLANT,
        NO_AXIS,
        AXIS_X,
        AXIS_Y,
        AXIS_Z;

        companion object {

            val AXIS_TYPES = listOf(AXIS_X, AXIS_Y, AXIS_Z)

            fun fromAxis(axis: Axis): Type {
                return when (axis) {
                    Axis.X -> AXIS_X
                    Axis.Y -> AXIS_Y
                    Axis.Z -> AXIS_Z
                }
            }
        }

    }

    fun getFamilyId(type: Type): String? {
        return this.family[type]
    }

    fun addFamilyType(type: Type, id: String) {
        this.family[type] = id
    }

}