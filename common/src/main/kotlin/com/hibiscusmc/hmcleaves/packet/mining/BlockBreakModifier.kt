package com.hibiscusmc.hmcleaves.packet.mining

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack


class BlockBreakModifier(
    val hardness: Double,
    val requiresToolToDrop: Boolean,
    private val toolTypes: Set<ToolType>,
    private val requiredEnchantments: Set<Enchantment>,
) {
    fun hasToolType(material: Material?): Boolean {
        for (toolType in this.toolTypes) {
            if (toolType.isType(material)) return true
        }
        return false
    }

    fun hasEnchantment(itemStack: ItemStack): Boolean {
        if (requiredEnchantments.isEmpty()) return true
        for (enchantment in this.requiredEnchantments) {
            if (itemStack.containsEnchantment(enchantment)) return true
        }
        return false
    }
}

enum class ToolType(private val tools: Set<Material?>) {
    AXE(
        java.util.Set.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
        )
    ),
    PICKAXE(
        java.util.Set.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
        )
    ),
    SHOVEL(
        java.util.Set.of(
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL
        )
    ),
    HOE(
        java.util.Set.of(
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE
        )
    ),
    SWORD(
        java.util.Set.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
        )
    ),
    SHEARS(
        java.util.Set.of(
            Material.SHEARS
        )
    );

    fun isType(material: Material?): Boolean {
        return tools.contains(material)
    }
}