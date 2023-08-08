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

package io.github.fisher2911.hmcleaves.hook.oraxen;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Location;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OraxenHook implements ItemHook {

    private final HMCLeaves plugin;
    private final LeavesConfig config;
    private final Path texturesPath;

    public OraxenHook(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.getLeavesConfig();
        this.texturesPath = OraxenPlugin.get().getDataFolder()
                .toPath()
                .resolve("pack")
                .resolve("assets")
                .resolve("minecraft")
                .resolve("blockstates");
    }

    @EventHandler
    public void onItemsLoad(OraxenItemsLoadedEvent event) {
        this.plugin.getLeavesConfig().load();
    }

    @Override
    @Nullable
    public String getId(ItemStack itemStack) {
        return OraxenItems.getIdByItem(itemStack);
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        final ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) return null;
        return builder.build();
    }

    @Override
    @Nullable
    public Integer getBlockId(String id) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");
        if (!(mechanicFactory instanceof final NoteBlockMechanicFactory noteBlockMechanicFactory)) {
            return null;
        }
        final NoteBlockMechanic mechanic = (NoteBlockMechanic) noteBlockMechanicFactory.getMechanic(id);
        if (mechanic == null) {
            return null;
        }
        final NoteBlock noteBlock = noteBlockMechanicFactory.createNoteBlockData(id);
        return SpigotConversionUtil.fromBukkitBlockData(noteBlock).getGlobalId();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNoteblockPlace(OraxenNoteBlockPlaceEvent event) {
        if (event.getMechanic() == null) return;
        final String id = event.getMechanic().getItemID();
        if (this.config.getItemSupplier(id) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onNoteblockRemove(OraxenNoteBlockBreakEvent event) {
        if (event.getMechanic() == null) return;
        final String id = event.getMechanic().getItemID();
        if (this.config.getItemSupplier(id) != null) {
            event.setCancelled(true);
        }
    }

    @Override
    public void transferTextures(File file) {
        final File texturesFolder = this.texturesPath.toFile();
        if (!texturesFolder.exists()) {
            this.plugin.getLogger().warning("Oraxen textures folder does not exist, creating it now");
            if (!texturesFolder.mkdirs()) {
                this.plugin.getLogger().warning("Failed to create Oraxen textures folder");
                return;
            }
        }
        try {
            Files.copy(file.toPath(), this.texturesPath.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            this.plugin.getLogger().info("Successfully transferred " + file.getName() + " to Oraxen textures folder");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable String getCustomBlockIdAt(Location location) {
        final BlockMechanic blockMechanic = OraxenBlocks.getBlockMechanic(location.getBlock());
        if (blockMechanic != null) return blockMechanic.getItemID();
        final NoteBlockMechanic noteBlockMechanic = OraxenBlocks.getNoteBlockMechanic(location.getBlock());
        if (noteBlockMechanic != null) return noteBlockMechanic.getItemID();
        return null;
    }

}
