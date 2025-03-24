package com.hibiscusmc.hmcleaves.config;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.hibiscusmc.hmcleaves.block.BlockProperties;
import com.hibiscusmc.hmcleaves.block.LeavesBlock;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class LeavesConfigImplementation implements LeavesConfig<BlockData> {

    private final Map<String, LeavesBlock> blocksById;
    private final Map<Material, LeavesBlock> blocksByMaterial;
    private Collection<String> whitelistedWorlds;

    public LeavesConfigImplementation(Map<String, LeavesBlock> blocksById, Map<Material, LeavesBlock> blocksByMaterial) {
        this.blocksById = blocksById;
        this.blocksByMaterial = blocksByMaterial;
    }

    @Override
    public @Nullable LeavesBlock getBlock(String id) {
        return this.blocksById.get(id);
    }

    @Override
    public void load() {
        for (Material material : Material.values()) {
            if (!Tag.LOGS.isTagged(material)) {
                continue;
            }
            this.blocksByMaterial.put(
                    material,
                    new LeavesBlock(
                            material.toString(),
                            BlockProperties.builder().build(),
                            WrappedBlockState.getDefaultState(StateTypes.REDSTONE_BLOCK)::clone
                            //                            WrappedBlockState.getDefaultState(
//                                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
//                                    SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getType()
//                            )::clone
                    )
            );
        }

        for (Material material : Material.values()) {
            if (!Tag.LEAVES.isTagged(material)) {
                continue;
            }
            this.blocksByMaterial.put(
                    material,
                    new LeavesBlock(
                            material.toString(),
                            BlockProperties.builder().build(),
                            WrappedBlockState.getDefaultState(StateTypes.GLOWSTONE)::clone
                            //                            WrappedBlockState.getDefaultState(
//                                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
//                                    SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getType()
//                            )::clone
                    )
            );
        }
    }

    private void loadWhitelistedWorlds() {
        this.whitelistedWorlds = new ArrayList<>();
    }

    @Override
    public boolean isWorldWhitelisted(UUID world) {
        return true; // TODO
    }

    @Override
    public @Nullable LeavesBlock getLeavesBlockFromWorldBlockData(BlockData blockData) {
        return this.blocksByMaterial.get(blockData.getMaterial());
    }
}
