package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Strict v2 additions layered after the frozen v1 Mob wire shape. */
public final class MobEditorV2Data {
    public static final int VERSION = 2;
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 48 * 1024;
    public static final int MAX_CATALOG_ENTRIES = 128;
    public static final int MAX_ABILITY_IDS = 64;
    private static final Pattern ABILITY_ID = Pattern.compile(
            "[a-z][a-z0-9._-]*:[a-z][a-z0-9._/-]*");

    private MobEditorV2Data() {
    }

    public static Mob readMob(FriendlyByteBuf buffer) {
        MobEditorData.Mob base = readStrictMob(buffer);
        validateMobForV2(base);
        int count = buffer.readUnsignedByte();
        if (count > MAX_ABILITY_IDS) throw new IllegalArgumentException("Too many abilities");
        ArrayList<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) ids.add(readAbilityId(buffer));
        requireUnique(ids, "Duplicate ability");
        return new Mob(base, ids);
    }

    public static void writeMob(FriendlyByteBuf buffer, Mob mob) {
        if (mob == null) throw new IllegalArgumentException("Missing mob");
        validateMobForV2(mob.base());
        MobEditorData.writeMob(buffer, mob.base());
        if (mob.abilityIds().size() > MAX_ABILITY_IDS) {
            throw new IllegalArgumentException("Too many abilities");
        }
        buffer.writeByte(mob.abilityIds().size());
        for (String id : mob.abilityIds()) writeAbilityId(buffer, id);
    }

    public static void writeHead(FriendlyByteBuf buffer, MobEditorData.Head head) {
        if (head == null || head.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported head schema");
        }
        if (head.tags().size() > 32) throw new IllegalArgumentException("Too many head tags");
        requireUnique(head.tags(), "Duplicate head tag");
        MobEditorData.writeHead(buffer, head);
    }

    public static List<CatalogEntry> readCatalog(FriendlyByteBuf buffer) {
        int count = buffer.readUnsignedShort();
        if (count > MAX_CATALOG_ENTRIES) throw new IllegalArgumentException("Too many catalog entries");
        ArrayList<CatalogEntry> entries = new ArrayList<>(count);
        HashSet<String> seen = new HashSet<>();
        String previous = null;
        for (int index = 0; index < count; index++) {
            String id = readAbilityId(buffer);
            String displayName = readString(buffer, 128);
            if (displayName.isBlank() || !seen.add(id)
                    || (previous != null && previous.compareTo(id) >= 0)) {
                throw new IllegalArgumentException("Invalid catalog");
            }
            entries.add(new CatalogEntry(id, displayName));
            previous = id;
        }
        return List.copyOf(entries);
    }

    public static MobEditorStatePayload.MobSummary readMobSummary(FriendlyByteBuf buffer) {
        String id = readString(buffer, 64);
        String name = readString(buffer, 128);
        String entityType = readString(buffer, 64);
        MobEditorData.Category category = readEnum(buffer, MobEditorData.Category.class);
        boolean enabled = buffer.readBoolean();
        long revision = buffer.readLong();
        List<String> tags = readStrings(buffer, 8, 32, "Duplicate mob tag");
        return new MobEditorStatePayload.MobSummary(
                id, name, entityType, category, enabled, revision, tags);
    }

    public static MobEditorStatePayload.HeadSummary readHeadSummary(FriendlyByteBuf buffer) {
        String id = readString(buffer, 64);
        String name = readString(buffer, 128);
        MobEditorData.HeadSource source = readEnum(buffer, MobEditorData.HeadSource.class);
        boolean favorite = buffer.readBoolean();
        List<String> tags = readStrings(buffer, 8, 32, "Duplicate head tag");
        return new MobEditorStatePayload.HeadSummary(id, name, source, favorite, tags);
    }

    public static MobEditorData.Head readHead(FriendlyByteBuf buffer) {
        int schema = buffer.readUnsignedByte();
        if (schema != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported head schema");
        long revision = buffer.readLong();
        String id = readString(buffer, 64);
        String name = readString(buffer, 128);
        MobEditorData.HeadSource source = readEnum(buffer, MobEditorData.HeadSource.class);
        String playerName = readString(buffer, 64);
        String textureValue = readString(buffer, 16_384);
        String projectsItemId = readString(buffer, 64);
        List<String> tags = readStrings(buffer, 32, 32, "Duplicate head tag");
        boolean favorite = buffer.readBoolean();
        String sourceNote = readString(buffer, 256);
        return new MobEditorData.Head(schema, revision, id, name, source, playerName,
                textureValue, projectsItemId, tags, favorite, sourceNote);
    }

    public static String readString(FriendlyByteBuf buffer, int maximumBytes) {
        int length = buffer.readUnsignedShort();
        if (length > maximumBytes || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("String is too long");
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Malformed UTF-8", exception);
        }
    }

    public static void writeAbilityId(FriendlyByteBuf buffer, String id) {
        if (!isAbilityId(id)) throw new IllegalArgumentException("Malformed ability id");
        MobEditorData.writeString(buffer, id, 96);
    }

    public static boolean isAbilityId(String id) {
        return id != null && id.getBytes(StandardCharsets.UTF_8).length <= 96
                && ABILITY_ID.matcher(id).matches() && !id.contains("..")
                && !id.contains("//") && !id.endsWith("/");
    }

    private static MobEditorData.Mob readStrictMob(FriendlyByteBuf buffer) {
        int schema = buffer.readUnsignedByte();
        if (schema != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported mob schema");
        long revision = buffer.readLong();
        String id = readString(buffer, 64);
        String name = readString(buffer, 128);
        String entityType = readString(buffer, 64);
        MobEditorData.Category category = readEnum(buffer, MobEditorData.Category.class);
        boolean enabled = buffer.readBoolean();
        int level = buffer.readUnsignedShort();
        MobEditorData.NameplateMode nameplate = readEnum(buffer, MobEditorData.NameplateMode.class);
        List<String> tags = readStrings(buffer, 32, 32, "Duplicate mob tag");
        MobEditorData.Stats stats = new MobEditorData.Stats(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
        MobEditorData.BasicAttack attack = new MobEditorData.BasicAttack(
                readEnum(buffer, MobEditorData.DamageType.class), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readBoolean());
        MobEditorData.Ai ai = new MobEditorData.Ai(
                readEnum(buffer, MobEditorData.AiPreset.class),
                readEnum(buffer, MobEditorData.TargetPriority.class),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        double scale = buffer.readDouble();
        MobEditorData.Age age = readEnum(buffer, MobEditorData.Age.class);
        boolean glowing = buffer.readBoolean();
        String glowingColor = readString(buffer, 32);
        int variantCount = buffer.readUnsignedByte();
        if (variantCount > 16) throw new IllegalArgumentException("Too many variants");
        Map<String, String> variants = new LinkedHashMap<>();
        for (int index = 0; index < variantCount; index++) {
            String key = readString(buffer, 32);
            String value = readString(buffer, 64);
            if (variants.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate variant");
            }
        }
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(MobEditorData.Slot.class);
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            equipment.put(slot, new MobEditorData.Equipment(
                    readEnum(buffer, MobEditorData.EquipmentSource.class),
                    readString(buffer, 64), readString(buffer, 64),
                    readString(buffer, 16), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean()));
        }
        return new MobEditorData.Mob(schema, revision, id, name, entityType, category,
                enabled, level, nameplate, tags, stats, attack, ai,
                new MobEditorData.Appearance(scale, age, glowing, glowingColor,
                        variants, equipment));
    }

    private static void validateMobForV2(MobEditorData.Mob mob) {
        if (mob == null || mob.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported mob schema");
        }
        if (!MobEditorData.finite(mob)) {
            throw new IllegalArgumentException("Non-finite mob field");
        }
        requireUnique(mob.tags(), "Duplicate mob tag");
        if (mob.tags().size() > 32 || mob.appearance().variants().size() > 16) {
            throw new IllegalArgumentException("Too many mob fields");
        }
        requireUnique(mob.appearance().variants().keySet(), "Duplicate variant");
    }

    private static String readAbilityId(FriendlyByteBuf buffer) {
        String id = readString(buffer, 96);
        if (!isAbilityId(id)) throw new IllegalArgumentException("Malformed ability id");
        return id;
    }

    private static <T extends Enum<T>> T readEnum(
            FriendlyByteBuf buffer, Class<T> type
    ) {
        try {
            return Enum.valueOf(type, readString(buffer, 32));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid enum", exception);
        }
    }

    private static List<String> readStrings(
            FriendlyByteBuf buffer, int maximumCount, int maximumBytes, String duplicateError
    ) {
        int count = buffer.readUnsignedByte();
        if (count > maximumCount) throw new IllegalArgumentException("Too many strings");
        ArrayList<String> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(readString(buffer, maximumBytes));
        }
        requireUnique(values, duplicateError);
        return List.copyOf(values);
    }

    private static void requireUnique(Iterable<String> values, String error) {
        HashSet<String> seen = new HashSet<>();
        for (String value : values) {
            if (!seen.add(value)) throw new IllegalArgumentException(error);
        }
    }

    public record Mob(MobEditorData.Mob base, List<String> abilityIds) {
        public Mob {
            if (base == null) throw new IllegalArgumentException("Missing base");
            abilityIds = List.copyOf(abilityIds);
            if (abilityIds.size() > MAX_ABILITY_IDS) {
                throw new IllegalArgumentException("Too many abilities");
            }
            requireUnique(abilityIds, "Duplicate ability");
            for (String id : abilityIds) {
                if (!isAbilityId(id)) throw new IllegalArgumentException("Malformed ability id");
            }
        }

        public Mob withAbilities(List<String> ids) {
            return new Mob(base, ids);
        }
    }

    public record CatalogEntry(String id, String displayName) {
        public CatalogEntry {
            if (!isAbilityId(id) || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Invalid catalog entry");
            }
        }
    }
}
