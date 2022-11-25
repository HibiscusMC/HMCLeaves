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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.LeafUpdater;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WorldEditHook {

    private final HMCLeaves plugin;

    public WorldEditHook(HMCLeaves plugin) {
        this.plugin = plugin;
    }

    public void load() {
        WorldEdit.getInstance().getEventBus().register(this);
    }

    public void trySaveSchematic(Player player) {
        try {
            final LeafCache leafCache = this.plugin.getLeafCache();
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
                        final FakeLeafState fakeLeafState = leafCache.getAt(world, x, y, z);
                        if (fakeLeafState == null) continue;
                        final BlockState state = clipboard.getBlock(BlockVector3.at(x, y, z));
                        final BlockData blockData = BukkitAdapter.adapt(state);
                        if (!(blockData instanceof final Leaves leaves)) continue;
                        leaves.setDistance(fakeLeafState.state().getDistance());
                        leaves.setPersistent(fakeLeafState.state().isPersistent());
                        clipboard.setBlock(BlockVector3.at(x, y, z), BukkitAdapter.adapt(leaves));
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
        final LeafCache leafCache = this.plugin.getLeafCache();
        final Config config = this.plugin.config();
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) return;
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                BlockData blockData = BukkitAdapter.adapt(block);
                final Location location = new Location(BukkitAdapter.adapt(event.getWorld()), pos.getX(), pos.getY(), pos.getZ());
                if (config.isLogBlock(blockData)) {
                    leafCache.setLogAt(location);
                    LeafUpdater.scheduleTick(location);
                    return super.setBlock(pos, block);
                }
                if (!(blockData instanceof Leaves leaves)) return super.setBlock(pos, block);
                final FakeLeafState state = config.getDefaultState(blockData.getMaterial());
                state.actuallyPersistent(false);
                state.state().setPersistent(leaves.isPersistent());
                state.state().setDistance(leaves.getDistance());
                leafCache.set(location, state);
                LeafUpdater.scheduleTick(location);
                return getExtent().setBlock(pos, block);
            }
        });
    }

}
