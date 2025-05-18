package com.hibiscusmc.hmcleaves.common.config;

import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface LeavesConfig<T> {

    @Nullable
    LeavesBlock getBlock(String id);

    void load();

    boolean isWorldWhitelisted(UUID worldId);

    boolean isWorldWhitelisted(String worldName);

    @Nullable
    LeavesBlock getLeavesBlockFromWorldBlockData(T blockData);

    int chunkScanVersion();

}
