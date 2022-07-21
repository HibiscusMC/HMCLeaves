package io.github.fisher2911.hmcleaves.listener;

import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ChunkListener implements Listener {

    private final HMCLeaves plugin;
    private final Config config;
    private final LeafCache cache;

    public ChunkListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.cache = plugin.getLeafCache();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        final PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (PDCUtil.hasLeafData(container)) {
            this.loadPDCData(chunk);
            return;
        }
        final ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        final World world = event.getWorld();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();
        final Position2D chunkPosition = new Position2D(event.getWorld().getUID(), chunkX, chunkZ);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (int x = 0; x < 16; x++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        final Material material = snapshot.getBlockType(x, y, z);
                        if (!Tag.LEAVES.isTagged(material)) continue;
                        final var defaultState = this.config.getDefaultState(material);
                        if (defaultState == null) continue;
                        this.cache.addData(chunkPosition, new Position(x, y, z), defaultState);
                    }
                }
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (!world.isChunkLoaded(chunkX, chunkZ)) return;
                PDCUtil.setHasLeafData(chunk.getPersistentDataContainer());
                for (var entry : this.cache.getOrAddChunkData(chunkPosition).entrySet()) {
                    final var position = entry.getKey();
                    final var data = entry.getValue();
                    final PersistentDataContainer blockData = new CustomBlockData(
                            chunk.getBlock(position.x(), position.y(), position.z()),
                            this.plugin
                    );
                    blockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) data.getDistance());
                    blockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, (byte) (data.isPersistent() ? 1 : 0));
                }
            });
        });
    }

    private void loadPDCData(Chunk chunk) {
        final Position2D chunkPos = new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final var blocks = CustomBlockData.getBlocksWithCustomData(this.plugin, chunk);
        for (var block : blocks) {
            final CustomBlockData blockData = new CustomBlockData(block, this.plugin);
            if (!Tag.LEAVES.isTagged(block.getType())) {
                blockData.remove(PDCUtil.PERSISTENCE_KEY);
                blockData.remove(PDCUtil.DISTANCE_KEY);
                continue;
            }
            final Byte distance = blockData.get(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE);
            if (distance == null) continue;
            final Byte persistent = blockData.get(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE);
            if (persistent == null) continue;
            final var state = this.plugin.config().getDefaultState(block.getType()).clone();
            if (state == null) continue;
            state.setDistance(distance);
            state.setPersistent(persistent == 1);
            try {
                this.cache.addData(
                        chunkPos,
                        new Position(PositionUtil.getCoordInChunk(block.getX()), block.getY(), PositionUtil.getCoordInChunk(block.getZ())),
                        state
                );
            } catch (Exception e) {
                this.plugin.getLogger().severe("Block threw error: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                e.printStackTrace();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        this.cache.remove(new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
    }
}
