package io.github.fisher2911.hmcleaves.nms;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.bukkit.Material;

import java.util.List;

public class CustomLeafInfo {

    private final BlockBehaviour.Properties properties;
    private final Block block;
    private final Item leafItem;
    private final String resourceKeyLocation;
    private final List<String> treeFeatureFields;
    private final Material bukkitMaterial;
    private final boolean isAzaleas;
    private final boolean isMangrove;

    public CustomLeafInfo(
            BlockBehaviour.Properties properties,
            Block block,
            Item leafItem,
            String resourceKeyLocation,
            List<String> treeFeatureFields,
            Material bukkitMaterial,
            boolean isAzaleas,
            boolean isMangrove
    ) {
        this.properties = properties;
        this.block = block;
        this.leafItem = leafItem;
        this.resourceKeyLocation = resourceKeyLocation;
        this.treeFeatureFields = treeFeatureFields;
        this.bukkitMaterial = bukkitMaterial;
        this.isAzaleas = isAzaleas;
        this.isMangrove = isMangrove;
    }

    public CustomLeafInfo(BlockBehaviour.Properties properties, Block block, Item leafItem, String resourceKeyLocation, List<String> treeFeatureFields, Material bukkitMaterial) {
        this(properties, block, leafItem, resourceKeyLocation, treeFeatureFields, bukkitMaterial, false, false);
    }

    public BlockBehaviour.Properties properties() {
        return properties;
    }

    public Item leafItem() {
        return leafItem;
    }

    public Block block() {
        return block;
    }

    public String resourceKeyLocation() {
        return resourceKeyLocation;
    }

    public List<String> treeFeatureFields() {
        return treeFeatureFields;
    }

    public Material bukkitMaterial() {
        return bukkitMaterial;
    }

    public boolean isAzaleas() {
        return isAzaleas;
    }

    public boolean isMangrove() {
        return isMangrove;
    }

}