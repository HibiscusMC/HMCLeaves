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

package io.github.fisher2911.hmcleaves.hook.worldedit;

import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.ChatColor;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class WorldEditHook {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig config;

    public WorldEditHook(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = this.plugin.getBlockCache();
        this.config = this.plugin.getLeavesConfig();
    }

    public void load() {
        WorldEdit.getInstance().getEventBus().register(this);
    }

    private static final String BUKKIT_NBT_TAG = "PublicBukkitValues";
    private static Property<Integer> distanceProperty;
    private static Property<Boolean> persistentProperty;

    public void trySaveSchematic(Player player) {
        try {
            final ClipboardHolder clipboardHolder = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getClipboard();
            final Clipboard clipboard = clipboardHolder.getClipboard();
            final Region region = clipboard.getRegion();
            final BlockVector3 min = region.getMinimumPoint();
            final BlockVector3 max = region.getMaximumPoint();
            final UUID world = BukkitAdapter.adapt(region.getWorld()).getUID();
            int transformedBlocks = 0;
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        final BlockData blockData = this.blockCache.getBlockData(Position.at(world, x, y, z));
                        if (blockData == BlockData.EMPTY) continue;
                        final BlockState state = clipboard.getBlock(BlockVector3.at(x, y, z));
                        final BaseBlock furnaceBlock = BlockTypes.FURNACE.getDefaultState().toBaseBlock();
                        CompoundTag baseNBT = furnaceBlock.getNbtData();
                        if (baseNBT == null) {
                            baseNBT = new CompoundTag(new HashMap<>());
                        }
                        final CompoundTag bukkitTag;
                        if (!(baseNBT.getValue().get(BUKKIT_NBT_TAG) instanceof final CompoundTag tag)) {
                            bukkitTag = new CompoundTag(new HashMap<>());
                        } else {
                            bukkitTag = tag;
                        }
                        final CompoundTag newBukkitValuesTag;
                        if (blockData instanceof final LogData logData) {
                            newBukkitValuesTag = bukkitTag.createBuilder()
                                    .put(PDCUtil.LOG_ID_KEY.toString(), new StringTag(logData.getCurrentId()))
                                    .build();
                        } else if (blockData instanceof final LeafData leafData) {
                            if (persistentProperty == null || distanceProperty == null) {
                                for (Property<?> p : state.getStates().keySet()) {
                                    if (p.getName().equalsIgnoreCase("distance")) {
                                        distanceProperty = (Property<Integer>) p;
                                    }
                                    if (p.getName().equalsIgnoreCase("persistent")) {
                                        persistentProperty = (Property<Boolean>) p;
                                    }
                                }
                            }
                            final int distance = state.getState(distanceProperty);
                            final boolean persistent = state.getState(persistentProperty);
                            newBukkitValuesTag = bukkitTag.createBuilder()
                                    .put(
                                            PDCUtil.LEAF_ID_KEY.toString(),
                                            new StringTag(leafData.id())
                                    )
                                    .put(
                                            PDCUtil.LEAF_WORLD_DISTANCE_KEY.toString(),
                                            new IntTag(distance)
                                    )
                                    .put(
                                            PDCUtil.LEAF_WORLD_PERSISTENCE_KEY.toString(),
                                            new ByteTag((byte) (persistent ? 1 : 0))
                                    )
                                    .build();
                        } else {
                            continue;
                        }
                        clipboard.setBlock(BlockVector3.at(x, y, z), furnaceBlock.toBaseBlock(
                                baseNBT.createBuilder()
                                        .put(BUKKIT_NBT_TAG, newBukkitValuesTag)
                                        .build()
                        ));
                        transformedBlocks++;
                    }
                }
            }
            player.sendMessage(ChatColor.GREEN + "Successfully saved schematic! " + transformedBlocks + " leaves were transformed.");
        } catch (EmptyClipboardException e) {
            player.sendMessage(ChatColor.RED + "You do not have a clipboard selected!");
        } catch (WorldEditException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while transforming the schematic!");
            e.printStackTrace();
        }

    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                final Position position = new Position(BukkitAdapter.adapt(event.getWorld()).getUID(), pos.getX(), pos.getY(), pos.getZ());
                if (!(block instanceof final BaseBlock baseBlock)) return super.setBlock(pos, block);
                if (baseBlock.getBlockType() != BlockTypes.FURNACE) return super.setBlock(pos, block);
                final CompoundTag tag = baseBlock.getNbtData();
                if (tag == null) {
                    return super.setBlock(pos, block);
                }
                if (!(tag.getValue().get(BUKKIT_NBT_TAG) instanceof final CompoundTag bukkitTag)) {
                    return super.setBlock(pos, block);
                }
                if (bukkitTag.containsKey(PDCUtil.LOG_ID_KEY.toString())) {
                    final String logId = bukkitTag.getString(PDCUtil.LOG_ID_KEY.toString());
                    final BlockData blockData = WorldEditHook.this.config.getBlockData(logId);
                    if (!(blockData instanceof final LogData logData)) return super.setBlock(pos, block);
                    final org.bukkit.block.data.BlockData bukkitLogData = logData.worldBlockType().createBlockData();
                    WorldEditHook.this.blockCache.addBlockData(position, logData);
                    return getExtent().setBlock(pos, BukkitAdapter.adapt(bukkitLogData));
                }
                if (bukkitTag.containsKey(PDCUtil.LEAF_ID_KEY.toString())) {
                    final String leafId = bukkitTag.getString(PDCUtil.LEAF_ID_KEY.toString());
                    final BlockData blockData = WorldEditHook.this.config.getBlockData(leafId);
                    if (!(blockData instanceof final LeafData leafData)) return super.setBlock(pos, block);
                    final int distance = bukkitTag.getInt(PDCUtil.LEAF_WORLD_DISTANCE_KEY.toString());
                    final boolean persistent = bukkitTag.getByte(PDCUtil.LEAF_WORLD_PERSISTENCE_KEY.toString()) == 1;
                    final Leaves bukkitLeafData = (Leaves) leafData.realBlockType().createBlockData();
                    bukkitLeafData.setDistance(distance);
                    bukkitLeafData.setPersistent(persistent);
                    WorldEditHook.this.blockCache.addBlockData(position, leafData);
                    return getExtent().setBlock(pos, BukkitAdapter.adapt(bukkitLeafData));
                }
                return super.setBlock(pos, block);
            }
        });
    }

}
