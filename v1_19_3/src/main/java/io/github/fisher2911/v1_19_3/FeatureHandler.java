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

package io.github.fisher2911.v1_19_3;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Lifecycle;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.v1_19_3.trunk.NMSStraightTrunkPlacer;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FeatureHandler extends io.github.fisher2911.hmcleaves.nms.FeatureHandler {

    private final WritableRegistry<Feature<?>> featureWritableRegistry;
    private final WritableRegistry<ConfiguredFeature<?, ?>> configuredFeatureWritableRegistry;
    private final Field frozenField;

    public FeatureHandler(LeavesConfig config, BlockCache blockCache) {
        super(config, blockCache);
        try {
            CraftServer craftServer = (CraftServer) Bukkit.getServer();
            MinecraftServer server = craftServer.getServer();
            this.featureWritableRegistry = (WritableRegistry<Feature<?>>) server.registryAccess().registryOrThrow(Registries.FEATURE);
            this.configuredFeatureWritableRegistry = (WritableRegistry<ConfiguredFeature<?, ?>>) server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
            this.frozenField = MappedRegistry.class.getDeclaredField("l");
            this.frozenField.setAccessible(true);
            this.frozenField.set(featureWritableRegistry, false);
            this.frozenField.set(configuredFeatureWritableRegistry, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void register() {
        try {
            this.loadFeatures();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFeatures() throws IOException {
        final File[] worldFiles = Bukkit.getWorldContainer().listFiles();
        if (worldFiles == null) return;
        for (final File file : worldFiles) {
            final File datapackFolder = file.toPath().resolve("datapacks").toFile();
            final File[] datapackFiles = datapackFolder.listFiles();
            if (datapackFiles == null) continue;
            for (final File datapackFile : datapackFiles) {
                this.loadFeatures(datapackFile);
            }
        }
    }

    private void loadFeatures(File datapackFolder) throws IOException {
        final File folder = datapackFolder.toPath()
                .resolve("data")
                .toFile();
        final File[] files = folder.listFiles();
        if (files == null) return;
        for (final File file : files) {
            final File worldGenFile = file.toPath()
                    .resolve("worldgen")
                    .resolve("configured_feature")
                    .toFile();
            this.loadConfiguredFeatures(file.getName(), worldGenFile);
        }
    }

    private static final String FEATURE_ID = "hmcleaves_tree_feature";
    private static final AtomicInteger FEATURE_ID_COUNTER = new AtomicInteger(0);

    private void loadConfiguredFeatures(String dataPackName, File folder) throws IOException {
        final File[] files = folder.listFiles();
        if (files == null) return;
        for (final File file : files) {
            final List<File> jsonFiles = new ArrayList<>();
            this.recursiveFindJson(file, jsonFiles);
            for (final File jsonFile : jsonFiles) {
                final JsonReader jsonReader = new JsonReader(new FileReader(jsonFile));
                final Gson gson = new Gson();
                final JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);
                final TreeConfiguration configuration = this.loadConfiguration(jsonObject);
                if (configuration == null) continue;
                final StringBuilder builder = new StringBuilder();
                File currentParent = jsonFile.getParentFile();
                while (currentParent != null) {
                    if (currentParent.getName().equals("configured_feature")) break;
                    builder.insert(0, currentParent.getName() + "/");
                    currentParent = currentParent.getParentFile();
                }
                builder.insert(0, dataPackName + ":");
                builder.append(jsonFile.getName().replace(".json", ""));
                final String name = builder.toString();
                final String featureId = FEATURE_ID + "_" + FEATURE_ID_COUNTER.getAndIncrement();
                final Feature<TreeConfiguration> feature = new NMSFeature(
                        TreeConfiguration.CODEC,
                        this.config,
                        this.blockCache
                );
                registerFeature(name, new NMSFeature(
                        TreeConfiguration.CODEC,
                        this.config,
                        this.blockCache
                ));
                System.out.println("registering: " + name + " " + featureId);
                this.featureWritableRegistry.register(
                        ResourceKey.create(
                                Registries.FEATURE, new ResourceLocation(featureId)
                        ),
                        feature,
                        Lifecycle.stable()
                );
                this.configuredFeatureWritableRegistry.register(
                        ResourceKey.create(
                                Registries.CONFIGURED_FEATURE, new ResourceLocation(name)
                        ),
                        new ConfiguredFeature<>(feature, configuration),
                        Lifecycle.stable()
                );
            }
        }
    }

    private void recursiveFindJson(File parent, List<File> files) {
        if (!parent.isDirectory() && parent.getName().endsWith(".json")) {
            files.add(parent);
            return;
        }
        final File[] listFiles = parent.listFiles();
        if (listFiles == null) return;
        for (final File file : listFiles) {
            if (file.isDirectory()) {
                this.recursiveFindJson(file, files);
            } else if (file.getName().endsWith(".json")) {
                files.add(file);
            }
        }
    }

    private static final String CONFIG_PATH = "config";
    private static final String DIRT_PROVIDER_PATH = "dirt_provider";
    private static final String TRUNK_PROVIDER_PATH = "trunk_provider";
    private static final String FOLIAGE_PROVIDER_PATH = "foliage_provider";
    private static final String MINIMUM_SIZE_PATH = "minimum_size";
    private static final String TRUNK_PLACER_PATH = "trunk_placer";
    private static final String FOLIAGE_PLACER_PATH = "foliage_placer";
    private static final String DECORATORS_PATH = "decorators";
    private static final String FORCE_DIRT_PATH = "force_dirt";
    private static final String IGNORE_VINES_PATH = "ignore_vines";

    private static final String TYPE_PATH = "type";
    private static final String STATE_PATH = "state";
    private static final String NAME_PATH = "Name";
    private static final String PROPERTIES_PATH = "Properties";

    private @Nullable TreeConfiguration loadConfiguration(JsonObject jsonObject) {
        final String type = jsonObject.get(TYPE_PATH).getAsString();
        if (!type.contains("tree")) return null;
        final JsonObject config = jsonObject.getAsJsonObject(CONFIG_PATH);
        final BlockStateProvider dirtProvider = this.blockStateProvider(config.getAsJsonObject(DIRT_PROVIDER_PATH));
        final BlockStateProvider trunkProvider = this.blockStateProvider(config.getAsJsonObject(TRUNK_PROVIDER_PATH));
        final BlockStateProvider foliageProvider = this.blockStateProvider(config.getAsJsonObject(FOLIAGE_PROVIDER_PATH));
        final TrunkPlacer trunkPlacer = this.loadTrunkPlacer(config.getAsJsonObject(TRUNK_PLACER_PATH));
        final FoliagePlacer foliagePlacer = this.loadFoliagePlacer(config.getAsJsonObject(FOLIAGE_PLACER_PATH));
        final FeatureSize minimumSize = this.featureSize(config.getAsJsonObject(MINIMUM_SIZE_PATH));
        if (trunkPlacer == null) return null;
        if (foliagePlacer == null) return null;
        if (minimumSize == null) return null;
        final TreeConfiguration.TreeConfigurationBuilder builder = new TreeConfiguration.TreeConfigurationBuilder(
                trunkProvider,
                trunkPlacer,
                foliageProvider,
                foliagePlacer,
                minimumSize
        )
                .dirt(dirtProvider);
        if (jsonObject.has(FORCE_DIRT_PATH) && jsonObject.get(FORCE_DIRT_PATH).getAsBoolean()) {
            builder.forceDirt();
        }
        if (jsonObject.has(IGNORE_VINES_PATH) && jsonObject.get(IGNORE_VINES_PATH).getAsBoolean()) {
            builder.ignoreVines();
        }
        return builder.build();
    }

    private static final String LIMIT_PATH = "limit";
    private static final String LOWER_SIZE_PATH = "lower_size";
    private static final String UPPER_SIZE_PATH = "upper_size";

    private FeatureSize featureSize(JsonObject object) {
        return switch (object.get(TYPE_PATH).getAsString()) {
            case "two_layers_feature_size", "minecraft:two_layers_feature_size" -> this.twoLayersFeatureSize(object);
            default -> null;
        };
    }

    private FeatureSize twoLayersFeatureSize(JsonObject object) {
        final int limit;
        if (!object.has(LIMIT_PATH)) {
            limit = 0;
        } else {
            limit = object.get(LIMIT_PATH).getAsInt();
        }
        return new TwoLayersFeatureSize(
                limit,
                object.get(UPPER_SIZE_PATH).getAsInt(),
                object.get(UPPER_SIZE_PATH).getAsInt()
        );
    }

    private TrunkPlacer loadTrunkPlacer(JsonObject object) {
        final String type = object.get(TYPE_PATH).getAsString();
        return switch (type) {
            case "straight_trunk_placer" -> this.loadStraightTrunkPlacer(object);
            default -> null;
        };
    }

    private static final String RADIUS_PATH = "radius";
    private static final String OFFSET_PATH = "offset";
    private static final String HEIGHT_PATH = "height";
    private static final String TRUNK_HEIGHT_PATH = "trunk_height";

    private FoliagePlacer loadFoliagePlacer(JsonObject object) {
        return switch (object.get(TYPE_PATH).getAsString()) {
            case "minecraft:fancy_foliage_placer", "fancy_foliage_placer" -> this.loadFancyFoliagePlacer(object);
            case "minecraft:blob_foliage_placer", "blob_foliage_placer" -> this.loadBlockFoliagePlacer(object);
            case "minecraft:spruce_foliage_placer", "spruce_foliage_placer" -> this.loadSpruceFoliagePlacer(object);
            default -> null;
        };
    }

    private FoliagePlacer loadFancyFoliagePlacer(JsonObject object) {
        return new FancyFoliagePlacer(
                ConstantInt.of(object.get(RADIUS_PATH).getAsInt()),
                ConstantInt.of(object.get(OFFSET_PATH).getAsInt()),
                object.get(HEIGHT_PATH).getAsInt()
        );
    }

    private FoliagePlacer loadBlockFoliagePlacer(JsonObject object) {
        return new BlobFoliagePlacer(
                ConstantInt.of(object.get(RADIUS_PATH).getAsInt()),
                ConstantInt.of(object.get(OFFSET_PATH).getAsInt()),
                object.get(HEIGHT_PATH).getAsInt()
        );
    }

    private FoliagePlacer loadSpruceFoliagePlacer(JsonObject object) {
        return new SpruceFoliagePlacer(
                ConstantInt.of(object.get(RADIUS_PATH).getAsInt()),
                ConstantInt.of(object.get(OFFSET_PATH).getAsInt()),
                ConstantInt.of(object.get(TRUNK_HEIGHT_PATH).getAsInt())
        );
    }

    private static final String BASE_HEIGHT_PATH = "base_height";
    private static final String HEIGHT_RAND_A_PATH = "height_rand_a";
    private static final String HEIGHT_RAND_B_PATH = "height_rand_b";

    private NMSStraightTrunkPlacer loadStraightTrunkPlacer(JsonObject object) {
        return new NMSStraightTrunkPlacer(
                object.get(BASE_HEIGHT_PATH).getAsInt(),
                object.get(HEIGHT_RAND_A_PATH).getAsInt(),
                object.get(HEIGHT_RAND_B_PATH).getAsInt()
        );
    }

    private BlockStateProvider blockStateProvider(JsonObject jsonObject) {
        final String type = jsonObject.get(TYPE_PATH).getAsString();
        if (type.equals("minecraft:simple_state_provider")) {
            final JsonObject state = jsonObject.getAsJsonObject(STATE_PATH);
            final String name = state.get(NAME_PATH).getAsString();
            final JsonObject properties = state.getAsJsonObject(PROPERTIES_PATH);
            final Block block = BuiltInRegistries.BLOCK.get(
                    ResourceKey.create(Registries.BLOCK, new ResourceLocation(name))
            );
            if (properties == null) {
                return BlockStateProvider.simple(block);
            }
            BlockState blockState = block.defaultBlockState();
            for (final Property<?> property : blockState.getProperties()) {
                final String propertyName = property.getName();
                final JsonElement propertyValue = properties.getAsJsonPrimitive(propertyName);
                if (propertyValue == null) continue;
                final Object value = this.get(propertyValue, property.getValueClass());
                if (value == null) continue;
                blockState = blockState.setValue(
                        property,
                        this.get(propertyValue, property.getValueClass())
                );
            }
            return BlockStateProvider.simple(blockState);
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    private <T> T get(JsonElement object, Class<?> clazz) {
        if (clazz == Integer.class) return (T) clazz.cast(object.getAsInt());
        if (clazz == String.class) return (T) clazz.cast(object.getAsString());
        if (clazz == Boolean.class) return (T) clazz.cast(object.getAsBoolean());
        if (clazz == Double.class) return (T) clazz.cast(object.getAsDouble());
        if (clazz == Float.class) return (T) clazz.cast(object.getAsFloat());
        if (clazz == Long.class) return (T) clazz.cast(object.getAsLong());
        if (clazz == Short.class) return (T) clazz.cast(object.getAsShort());
        if (clazz == Byte.class) return (T) clazz.cast(object.getAsByte());
        if (clazz == NoteBlockInstrument.class)
            return (T) NoteBlockInstrument.valueOf(object.getAsString().toUpperCase());
        return null;
    }

    private static <C extends FeatureConfiguration, F extends Feature<C>> F registerFeature(String name, F feature) {
        return Registry.register(BuiltInRegistries.FEATURE, name, feature);
    }

}
