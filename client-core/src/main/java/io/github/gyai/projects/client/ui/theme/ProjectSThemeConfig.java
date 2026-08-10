package io.github.gyai.projects.client.ui.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class ProjectSThemeConfig {
    public static final int SCHEMA_VERSION = 1;
    static final long MAX_CONFIG_BYTES = 16 * 1024;
    private final Path path;

    public ProjectSThemeConfig(Path path) {
        this.path = path;
    }

    public LoadResult load() {
        if (!Files.isRegularFile(path)) {
            return new LoadResult(ProjectSThemeId.OBSIDIAN, false, "設定ファイルがありません");
        }
        try {
            if (Files.size(path) > MAX_CONFIG_BYTES) {
                throw new IOException("設定ファイルが大きすぎます");
            }
            JsonReader reader = new JsonReader(new StringReader(
                    Files.readString(path, StandardCharsets.UTF_8)));
            reader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("JSON末尾に不正なデータがあります");
            }
            if (!root.isJsonObject()) throw new IOException("JSONオブジェクトではありません");
            JsonObject object = root.getAsJsonObject();
            JsonElement schema = object.get("schemaVersion");
            if (schema == null || !schema.isJsonPrimitive()
                    || !schema.getAsJsonPrimitive().isNumber()
                    || !schema.getAsString().equals(Integer.toString(SCHEMA_VERSION))) {
                throw new IOException("schemaVersionが不正です");
            }
            JsonElement activeTheme = object.get("activeTheme");
            if (activeTheme == null || !activeTheme.isJsonPrimitive()
                    || !activeTheme.getAsJsonPrimitive().isString()) {
                throw new IOException("activeThemeがありません");
            }
            try {
                return new LoadResult(ProjectSThemeId.valueOf(
                        activeTheme.getAsString().toUpperCase(Locale.ROOT)), true, "");
            } catch (IllegalArgumentException exception) {
                return new LoadResult(ProjectSThemeId.OBSIDIAN, false,
                        "不明なテーマIDです");
            }
        } catch (IOException | RuntimeException exception) {
            return new LoadResult(ProjectSThemeId.OBSIDIAN, false,
                    "テーマ設定を読み込めませんでした");
        }
    }

    public boolean save(ProjectSThemeId id) {
        try {
            Files.createDirectories(path.getParent());
            String json = "{\n  \"schemaVersion\": 1,\n  \"activeTheme\": \""
                    + id.name() + "\"\n}\n";
            Path temporary = Files.createTempFile(
                    path.getParent(), path.getFileName().toString(), ".tmp");
            boolean moved = false;
            try {
                Files.writeString(temporary, json, StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, path,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
                moved = true;
            } finally {
                if (!moved) Files.deleteIfExists(temporary);
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public Path path() {
        return path;
    }

    public record LoadResult(ProjectSThemeId themeId, boolean valid, String warning) { }
}
