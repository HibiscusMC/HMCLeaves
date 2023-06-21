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

package io.github.fisher2911.hmcleaves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.SaplingData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TextureFileGenerator {

    private final HMCLeaves plugin;
    private final Path folderPath;

    public TextureFileGenerator(HMCLeaves plugin) {
        this.plugin = plugin;
        this.folderPath = this.plugin.getDataFolder().toPath().resolve("textures");
    }

    private static final String VARIANTS_PATH = "variants";
    private static final String MODEL_PATH = "model";
    private static final String LEAVES_PATH = "blocks/leaves";
    private static final String SAPLINGS_PATH = "blocks/saplings";

    public void generateFile(Material material, Collection<BlockData> data) {
        if (data.isEmpty()) return;
        final GsonBuilder gsonBuilder = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Variants.class, new VariantsAdapter());
        final Gson gson = gsonBuilder.create();
        final Variants variants = new Variants(
                data.stream()
                        .map(blockData -> {
                            if (blockData instanceof LeafData) {
                                return new JSONData(
                                        convertDataToString(blockData),
                                        blockData.id(),
                                        blockData.modelPath()
                                );
                            }
                            if (blockData instanceof SaplingData) {
                                return new JSONData(
                                        convertDataToString(blockData),
                                        blockData.id(),
                                        blockData.modelPath()
                                );
                            }
                            throw new IllegalArgumentException("Cannot convert data type: " + blockData.getClass().getName());
                        })
                        .collect(Collectors.toList())
        );
        final String json = gson.toJson(variants);
        final File file = this.folderPath.resolve(material.name().toLowerCase() + ".json").toFile();
        try {
            if (!this.folderPath.toFile().exists()) {
                Files.createDirectory(this.folderPath);
            }
            file.delete();
            if (!file.exists()) {
                file.createNewFile();
            }
            Files.writeString(file.toPath(), json, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Hooks.transferTextures(file);
    }

    // leaves
    private static final String DISTANCE_KEY = "distance";
    private static final String PERSISTENT_KEY = "persistent";

    // saplings
    private static final String STAGE = "stage";

    private static String convertDataToString(BlockData blockData) {
        if (blockData instanceof final LeafData leafData) {
            return DISTANCE_KEY + "=" + leafData.displayDistance() + "," + PERSISTENT_KEY + "=" + leafData.displayPersistence();
        }
        if (blockData instanceof final SaplingData saplingData) {
            return STAGE + "=" + saplingData.getNewState(null).getStage();
        }
        throw new IllegalArgumentException(blockData.getClass().getSimpleName() + " cannot be converted to a string for texture file!");
    }

    private static class Variants {

        private final List<JSONData> jsonData;

        public Variants(List<JSONData> jsonData) {
            this.jsonData = jsonData;
        }

    }

    private static class JSONData {

        private final String key;
        private final String id;
        private final String path;

        public JSONData(String key, String id, String path) {
            this.key = key;
            this.id = id;
            this.path = path;
        }

    }

    private static class VariantsAdapter extends TypeAdapter<Variants> {

        @Override
        public void write(JsonWriter jsonWriter, Variants variants) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name(VARIANTS_PATH);
            jsonWriter.beginObject();
            for (JSONData data : variants.jsonData) {
                jsonWriter.name(data.key);
                jsonWriter.beginArray();
                jsonWriter.beginObject();
                jsonWriter.name(MODEL_PATH).value(data.path);
                jsonWriter.endObject();
                jsonWriter.endArray();
            }
            jsonWriter.endObject();
            jsonWriter.endObject();
        }

        @Override
        public Variants read(JsonReader jsonReader) throws IOException {
            return null;
        }

    }

}
