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

package io.github.fisher2911.hmcleaves.hook.mythiccrucible;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.lumine.mythiccrucible.MythicCrucible;
import io.lumine.mythiccrucible.items.CrucibleItem;
import io.lumine.mythiccrucible.items.CrucibleItemType;
import io.lumine.mythiccrucible.items.blocks.CustomBlockItemContext;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class MythicCrucibleHook implements ItemHook {

    private final HMCLeaves plugin;
    private final LeavesConfig config;

    public MythicCrucibleHook(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.getLeavesConfig();
        this.plugin.getLeavesConfig().load();
    }

    @Override
    public @Nullable String getId(ItemStack itemStack) {
        return MythicCrucible.inst().getItemManager()
                .getItem(itemStack)
                .map(CrucibleItem::getInternalName)
                .orElse(null);
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        return MythicCrucible.core()
                .getItemManager()
                .getItemStack(id);
    }

    @Override
    public @Nullable Integer getBlockId(String id) {
        return MythicCrucible.inst()
                .getItemManager()
                .getItem(id)
                .filter(i -> i.getType() == CrucibleItemType.BLOCK)
                .map(CrucibleItem::getBlockData)
                .map(CustomBlockItemContext::getBlockData)
                .map(SpigotConversionUtil::fromBukkitBlockData)
                .map(WrappedBlockState::getGlobalId)
                .orElse(null);
    }

    @Override
    public void transferTextures(File file) {

    }

    @Override
    public @Nullable String getCustomBlockIdAt(Location location) {
        return MythicCrucible.inst()
                .getItemManager()
                .getCustomBlockManager()
                .getBlockFromBlock(location.getBlock())
                .map(CustomBlockItemContext::getCrucibleItem)
                .map(CrucibleItem::getInternalName)
                .orElse(null);
    }

}
