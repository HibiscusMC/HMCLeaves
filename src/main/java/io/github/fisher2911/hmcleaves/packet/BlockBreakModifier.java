/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.packet;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public record BlockBreakModifier(
        double hardness,
        boolean requiresToolToDrop,
        Set<ToolType> toolTypes,
        Set<Enchantment> requiredEnchantments
) {

    public boolean hasToolType(Material material) {
        for (ToolType toolType : this.toolTypes) {
            if (toolType.isType(material)) return true;
        }
        return false;
    }

    public boolean hasEnchantment(ItemStack itemStack) {
        if (this.requiredEnchantments.isEmpty()) return true;
        for (Enchantment enchantment : this.requiredEnchantments) {
            if (itemStack.containsEnchantment(enchantment)) return true;
        }
        return false;
    }

    public enum ToolType {

        AXE(Set.of(
                Material.WOODEN_AXE,
                Material.STONE_AXE,
                Material.IRON_AXE,
                Material.GOLDEN_AXE,
                Material.DIAMOND_AXE,
                Material.NETHERITE_AXE
        )),
        PICKAXE(Set.of(
                Material.WOODEN_PICKAXE,
                Material.STONE_PICKAXE,
                Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE,
                Material.DIAMOND_PICKAXE,
                Material.NETHERITE_PICKAXE
        )),
        SHOVEL(Set.of(
                Material.WOODEN_SHOVEL,
                Material.STONE_SHOVEL,
                Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL,
                Material.DIAMOND_SHOVEL,
                Material.NETHERITE_SHOVEL
        )),
        HOE(Set.of(
                Material.WOODEN_HOE,
                Material.STONE_HOE,
                Material.IRON_HOE,
                Material.GOLDEN_HOE,
                Material.DIAMOND_HOE,
                Material.NETHERITE_HOE
        )),
        SWORD(Set.of(
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD,
                Material.NETHERITE_SWORD
        )),
        SHEARS(Set.of(
                Material.SHEARS
        ));

        private final Set<Material> tools;

        ToolType(Set<Material> tools) {
            this.tools = tools;
        }

        public boolean isType(Material material) {
            return this.tools.contains(material);
        }
    }

}
