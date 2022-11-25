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

package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.api.HMCLeavesAPI;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class PlaceListener implements Listener {

    private final HMCLeaves plugin;
    private final LeafCache leafCache;
    private final Config config;

    public PlaceListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leafCache = plugin.getLeafCache();
        this.config = plugin.config();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent event) {
        this.handlePlace(event);
        this.handleInfoClick(event);
    }

    private void handlePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block toPlace = event.getClickedBlock().getRelative(event.getBlockFace());
        final World world = toPlace.getWorld();
        if (!(world.getNearbyEntities(toPlace.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty()))
            return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final String id = PDCUtil.getLeafDataItemId(itemStack);/* itemMeta.getPersistentDataContainer().get(PDCUtil.ITEM_KEY, PersistentDataType.STRING);*/
        final Chunk chunk = toPlace.getChunk();
        final Position2D chunkPos = new Position2D(toPlace.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(ChunkUtil.getCoordInChunk(toPlace.getX()), toPlace.getY(), ChunkUtil.getCoordInChunk(toPlace.getZ()));
        final LeafItem leafItem = plugin.config().getItem(id);
        LeafData leafData = leafItem != null ? leafItem.leafData() : PDCUtil.getLeafData(itemMeta.getPersistentDataContainer());
        if (leafData == null) {
            final Material material = itemStack.getType();
            if (!Tag.LEAVES.isTagged(material)) return;
            leafData = new LeafData(material, this.plugin.config().getDefaultDistance(), this.plugin.config().isDefaultPersistent(), true);
        }
        HMCLeavesAPI.getInstance().setLeafAt(
                toPlace.getLocation(),
                leafData
        );
        Bukkit.getScheduler().runTaskAsynchronously(
                this.plugin,
                () -> PacketHelper.sendArmSwing(event.getPlayer(), Bukkit.getOnlinePlayers())
        );
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) itemStack.setAmount(itemStack.getAmount() - 1);
    }

    private void handleInfoClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        if (!Config.DEBUG_TOOL_ID.equals(PDCUtil.getLeafDataItemId(itemStack))) return;

        final Player player = event.getPlayer();
        if (!(clicked.getBlockData() instanceof final Leaves leaves)) {
            if (this.plugin.getLeafCache().isLogAt(clicked.getLocation())) {
                player.sendMessage("Log");
            } else if (this.config.isLogBlock(clicked.getBlockData())) {
                player.sendMessage("Not log but should be");
            }
            return;
        }
        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(
                new Position2D(clicked.getWorld().getUID(), clicked.getChunk().getX(), clicked.getChunk().getZ()),
                new Position(ChunkUtil.getCoordInChunk(clicked.getX()), clicked.getY(), ChunkUtil.getCoordInChunk(clicked.getZ()))
        );
        if (fakeLeafState == null) {
            player.sendMessage("Actual distance: " + leaves.getDistance() + " actual persistence: " + leaves.isPersistent());
            player.sendMessage("The fake leaf state was not found");
            return;
        }
        player.sendMessage("Actual distance: " + fakeLeafState.actualDistance() + " actual persistence: " + fakeLeafState.actuallyPersistent() + " " +
                "server distance: " + leaves.getDistance() + " server persistence: " + leaves.isPersistent());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onActionClick(InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        if (!(event.getWhoClicked() instanceof final Player player)) return;
        final RayTraceResult rayTrace = player.rayTraceBlocks(6);
        if (rayTrace == null) return;
        final Block clicked = rayTrace.getHitBlock();
        if (clicked == null) return;
        final Location location = clicked.getLocation();
        final Position2D chunkPos = new Position2D(location.getWorld().getUID(), location.getChunk().getX(), location.getChunk().getZ());
        final Position position = new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()));
        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(chunkPos, position);
        if (fakeLeafState == null) return;
        final LeafItem leafItem = this.plugin.config().getByState(fakeLeafState);
        if (leafItem == null) return;
        final ItemStack leafItemStack = leafItem.itemStack();
        for (int i = 0; i <= 8; i++) {
            final ItemStack itemStack = player.getInventory().getItem(i);
            if (leafItemStack.equals(itemStack)) {
                player.getInventory().setHeldItemSlot(i);
                event.setCancelled(true);
                return;
            }
        }
        event.setCursor(leafItemStack);
    }

}
