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

package io.github.fisher2911.hmcleaves.api;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class HMCLeavesAPI {

    private static HMCLeavesAPI instance;

    public static HMCLeavesAPI getInstance() {
        if (instance == null) {
            instance = new HMCLeavesAPI(HMCLeaves.getPlugin(HMCLeaves.class));
        }
        return instance;
    }

    private final HMCLeaves plugin;
    private final LeavesConfig config;
    private final BlockCache blockCache;

    public HMCLeavesAPI(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = this.plugin.getLeavesConfig();
        this.blockCache = this.plugin.getBlockCache();
    }


    /**
     * Get the {@link BlockData} at the given {@link Location}.
     * return {@link BlockData#EMPTY} if the block is not a custom block.
     */
    @NotNull
    public BlockData getBlockDataAt(Location location) {
        return this.blockCache.getBlockData(Position.fromLocation(location));
    }

    /**
     * Check if the given {@link Location} is a custom block.
     * @param location The location to check.
     * @return true if the location is a custom block, false if not.
     */
    public boolean isCustomBlock(Location location) {
        return this.blockCache.getBlockData(Position.fromLocation(location)) != BlockData.EMPTY;
    }

    /**
     * Set the {@link BlockData} at the given {@link Location}.
     * @param location The location to set the block at.
     * @param id The id of the block to set.
     * @param setBlockInWorld If true, the block will be set in the world.
     *                        If false, the block will only be set in the cache,
     *                        which may cause issues so be very careful if you do this.
     * @return true if the block was set, false if the block id was not found.
     */
    public boolean setCustomBlock(Location location, String id, boolean setBlockInWorld) {
        final BlockData blockData = this.config.getBlockData(id);
        if (blockData == null) return false;
        this.blockCache.addBlockData(Position.fromLocation(location), blockData);
        if (setBlockInWorld) {
            location.getBlock().setType(blockData.worldBlockType());
        }
        return true;
    }

    /**
     * Remove the {@link BlockData} at the given {@link Location}.
     * @param location The location to remove the block at.
     * @param setBlockInWorld If true, the block will be set to air in the world.
     *                        If false, the block will only be removed from the cache,
     *                        which may cause issues so be very careful if you do this.
     * @return The {@link BlockData} that was removed.
     */
    @NotNull
    public BlockData removeBlockDataAt(Location location, boolean setBlockInWorld) {
        if (setBlockInWorld) {
            location.getBlock().setType(Material.AIR);
        }
        return this.blockCache.removeBlockData(Position.fromLocation(location));
    }

    /**
     *
     * @param world The world to check.
     * @return true if the world is whitelisted, false if not.
     */
    public boolean isWorldWhitelisted(World world) {
        return this.config.isWorldWhitelisted(world);
    }

    /**
     *
     * @param world The world to check.
     * @return true if the world is whitelisted, false if not.
     */
    public boolean isWorldWhitelisted(String world) {
        return this.config.isWorldWhitelisted(world);
    }

    /**
     * Add a world to the whitelist.
     * @param world The world to add.
     */
    public void addWhitelistedWorld(World world) {
        this.config.addWhitelistedWorld(world);
    }

    /**
     * Add a world to the whitelist.
     * @param world The world to add.
     */
    public void addWhitelistedWorld(String world) {
        this.config.addWhitelistedWorld(world);
    }

    /**
     * Remove a world from the whitelist.
     * @param world The world to remove.
     */
    public void removeWhitelistedWorld(World world) {
        this.config.removeWhitelistedWorld(world);
    }

    /**
     * Remove a world from the whitelist.
     * @param world The world to remove.
     */
    public void removeWhitelistedWorld(String world) {
        this.config.removeWhitelistedWorld(world);
    }

    public boolean isWorldWhitelistEnabled() {
        return this.config.isUseWorldWhitelist();
    }

}
