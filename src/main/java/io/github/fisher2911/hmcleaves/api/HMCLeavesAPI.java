package io.github.fisher2911.hmcleaves.api;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.nms.FakeLeafData;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.jetbrains.annotations.Nullable;

public class HMCLeavesAPI {

    private static final BlockFace DIRECTIONS[] = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private final HMCLeaves plugin;

    private HMCLeavesAPI(final HMCLeaves plugin) {
        this.plugin = plugin;
    }

    private static final HMCLeavesAPI INSTANCE = new HMCLeavesAPI(HMCLeaves.getPlugin(HMCLeaves.class));

    public static HMCLeavesAPI getInstance() {
        return INSTANCE;
    }

    public void setLeafAt(
            Location location,
            @Nullable LeafData leafData,
            Material defaultMaterial
    ) {
        setLeafAt(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                leafData,
                defaultMaterial
        );
    }

    public void setLeafAt(
            World world,
            int x,
            int y,
            int z,
            @Nullable LeafData leafData,
            Material defaultMaterial
    ) {
        final FakeLeafData defaultFakeLeafData = this.plugin.config().getDefaultData();
        if (leafData == null) {
            if (!Tag.LEAVES.isTagged(defaultMaterial)) return;
            leafData = new LeafData(defaultMaterial, defaultFakeLeafData.fakeDistance(), defaultFakeLeafData.fakePersistence(), true);
        }
        final FakeLeafData fakeLeafData = new FakeLeafData(
                leafData.fakeDistance(),
                leafData.fakePersistence(),
                7,
                leafData.actuallyPersistent()
        );
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        this.plugin.getLeafCache().addData(chunkPos, position, fakeLeafData);
        final Block toPlace = world.getBlockAt(x, y, z);
        toPlace.setType(leafData.material(), true);
        final @Nullable LeafData finalLeafData = leafData;
        final Leaves leaves = (Leaves) toPlace.getBlockData();
        leaves.setDistance(finalLeafData.fakeDistance());
        leaves.setPersistent(finalLeafData.fakePersistence());
        toPlace.setBlockData(leaves);
        ((CraftWorld) world).getHandle().scheduleTick(
                new BlockPos(x, y, z), ((CraftBlock) toPlace).getNMS().getBlock(), 1
        );
    }

    public void removeLeafAt(World world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        this.plugin.getLeafCache().remove(chunkPos, position);
        world.getBlockAt(x, y, z).setType(Material.AIR);
    }

    public void updateBlocksAroundChangedLeaf(World world, Block block) {
        for (BlockFace direction : DIRECTIONS) {
            final Block relative = block.getRelative(direction);
            final BlockPos blockPos = new BlockPos(relative.getX(), relative.getY(), relative.getZ());
            ((CraftWorld) world).getHandle().scheduleTick(
                    blockPos, ((CraftBlock) relative).getNMS().getBlock(), 1
            );
        }
    }

}
