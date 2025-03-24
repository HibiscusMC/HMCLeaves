package com.hibiscusmc.hmcleaves.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class BlockProperties {

    public static final BlockProperty<Integer> DISTANCE = new BlockProperty<>("distance", Integer.class);
    public static final BlockProperty<Boolean> WATERLOGGED = new BlockProperty<>("waterlogged", Boolean.class);



    private final @Unmodifiable Map<BlockProperty<?>, Object> properties;

    private BlockProperties(Map<BlockProperty<?>, Object> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    public @Unmodifiable Map<BlockProperty<?>, Object> properties() {
        return this.properties;
    }

    public <T> @Nullable T getProperty(BlockProperty<T> property) {
        final Object value = this.properties.get(property);
        if (value == null) {
            return null;
        }
        return property.type().cast(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<BlockProperty<?>, Object> properties;

        public Builder() {
            this.properties = new HashMap<>();
        }

        public <T> Builder addProperty(BlockProperty<T> property, T value) {
            this.properties.put(property, value);
            return this;
        }

        public BlockProperties build() {
            return new BlockProperties(this.properties);
        }

    }

}
