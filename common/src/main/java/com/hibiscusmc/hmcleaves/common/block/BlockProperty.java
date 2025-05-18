package com.hibiscusmc.hmcleaves.common.block;

public record BlockProperty<T>(String id, Class<T> type) {

    public boolean matchesType(Object type) {
        return this.type.isInstance(type);
    }

}
