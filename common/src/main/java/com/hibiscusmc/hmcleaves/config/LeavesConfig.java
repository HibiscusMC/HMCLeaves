package com.hibiscusmc.hmcleaves.config;

import com.hibiscusmc.hmcleaves.block.LeavesBlock;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface LeavesConfig<T> {

    @Nullable
    LeavesBlock getBlock(String id);

    void load();

    boolean isWorldWhitelisted(UUID world);

    @Nullable
    LeavesBlock getLeavesBlockFromWorldBlockData(T blockData);

}
