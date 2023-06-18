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

package io.github.fisher2911.hmcleaves.hook.hmcleaves;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

public class HMCLeavesHook implements ItemHook {

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;

    public HMCLeavesHook(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = this.plugin.getLeavesConfig();
    }

    @Override
    public @Nullable String getId(ItemStack itemStack) {
        return PDCUtil.getItemId(itemStack);
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        final Supplier<ItemStack> supplier = this.leavesConfig.getItemSupplier(id);
        if (supplier == null) return null;
        return supplier.get();
    }

    @Override
    public @Nullable Integer getBlockId(String id) {
        final BlockData blockData = this.leavesConfig.getBlockData(id);
        if (blockData == null) return null;
        return SpigotConversionUtil.fromBukkitBlockData(
                blockData.worldBlockType().createBlockData()
        ).getGlobalId();
    }

    @Override
    public void transferTextures(File file) {
        // do nothing, no hook used for texture packs
    }

    @Override
    public @Nullable String getCustomBlockIdAt(Location location) {
        return null;
    }

}
