package io.github.gyai.projects.client;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

public final class MobEditorData {
    public static final int VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 48 * 1024;

    private MobEditorData() {
    }

    public enum Category { NORMAL, ELITE, BOSS }
    public enum NameplateMode { ALWAYS, COMBAT_ONLY, HIDDEN }
    public enum DamageType { PHYSICAL, MAGICAL, TRUE }
    public enum AiPreset { PASSIVE, NEUTRAL, AGGRESSIVE, RANGED, GUARD, BOSS }
    public enum TargetPriority { NEAREST, LOWEST_HEALTH }
    public enum Age { ADULT, BABY }
    public enum Slot { HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND }
    public enum EquipmentSource { NONE, VANILLA_ITEM, PROJECTS_ITEM, CUSTOM_HEAD }
    public enum HeadSource { VANILLA_HEAD, PLAYER_PROFILE, TEXTURE_VALUE, SAVED_HEAD, PROJECTS_ITEM }

    public record Stats(
            double maxHealth, double physicalAttack, double magicalAttack,
            double physicalDefense, double magicalDefense,
            double movementSpeed, double attackSpeed,
            double criticalChance, double criticalDamage,
            double damageReduction
    ) { }

    public record BasicAttack(
            DamageType damageType, double fixedDamage, double coefficient,
            double intervalSeconds, double range, double knockback,
            boolean criticalAllowed
    ) { }

    public record Ai(
            AiPreset preset, TargetPriority priority,
            double aggroRange, double chaseRange, double leashRange,
            double attackRange, double refreshSeconds,
            boolean returnHome, boolean resetHealth,
            boolean avoidFalls, boolean avoidWater
    ) { }

    public record Equipment(
            EquipmentSource source, String referenceId, String material,
            String color, boolean glint, boolean visible, boolean visualOnly
    ) {
        public static Equipment empty() {
            return new Equipment(
                    EquipmentSource.NONE, "", "", "",
                    false, true, true);
        }
    }

    public record Appearance(
            double scale, Age age, boolean glowing, String glowingColor,
            Map<String, String> variants,
            Map<Slot, Equipment> equipment
    ) {
        public Appearance {
            variants = variants == null ? Map.of() : Map.copyOf(variants);
            EnumMap<Slot, Equipment> safe = new EnumMap<>(Slot.class);
            Map<Slot, Equipment> supplied = equipment == null ? Map.of() : equipment;
            for (Slot slot : Slot.values()) {
                Equipment value = supplied.get(slot);
                safe.put(slot, value == null ? Equipment.empty() : value);
            }
            equipment = Map.copyOf(safe);
        }
    }

    public record Mob(
            int schemaVersion, long revision, String id, String displayName,
            String entityType, Category category, boolean enabled, int level,
            NameplateMode nameplate, List<String> tags, Stats stats,
            BasicAttack attack, Ai ai, Appearance appearance
    ) {
        public Mob {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }

        public static Mob create(String id) {
            return new Mob(1, 0, id, "新しいモブ", "ZOMBIE",
                    Category.NORMAL, true, 1, NameplateMode.ALWAYS,
                    List.of(), new Stats(20, 4, 0, 0, 0,
                    1, 1, .05, 1.75, 0),
                    new BasicAttack(DamageType.PHYSICAL,
                            0, 1, 1.2, 2.2, .2, true),
                    new Ai(AiPreset.AGGRESSIVE, TargetPriority.NEAREST,
                            12, 24, 32, 2.2, 1,
                            true, true, true, false),
                    new Appearance(1, Age.ADULT, false, "WHITE",
                            Map.of(), Map.of()));
        }
    }

    public record Head(
            int schemaVersion, long revision, String id, String displayName,
            HeadSource source, String playerName, String textureValue,
            String projectsItemId, List<String> tags,
        boolean favorite, String sourceNote
    ) {
        public Head {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public static Mob readMob(FriendlyByteBuf buffer) {
        int schema = buffer.readUnsignedByte();
        long revision = buffer.readLong();
        String id = readString(buffer, 64);
        String name = readString(buffer, 128);
        String entityType = readString(buffer, 64);
        Category category = readEnum(buffer, Category.class);
        boolean enabled = buffer.readBoolean();
        int level = buffer.readUnsignedShort();
        NameplateMode nameplate = readEnum(buffer, NameplateMode.class);
        List<String> tags = readStrings(buffer, 32, 32);
        Stats stats = new Stats(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
        BasicAttack attack = new BasicAttack(
                readEnum(buffer, DamageType.class), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readBoolean());
        Ai ai = new Ai(
                readEnum(buffer, AiPreset.class),
                readEnum(buffer, TargetPriority.class),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        double scale = buffer.readDouble();
        Age age = readEnum(buffer, Age.class);
        boolean glowing = buffer.readBoolean();
        String glowingColor = readString(buffer, 32);
        int variantCount = buffer.readUnsignedByte();
        if (variantCount > 16) throw new IllegalArgumentException("Too many variants");
        LinkedHashMap<String, String> variants = new LinkedHashMap<>();
        for (int index = 0; index < variantCount; index++) {
            variants.put(readString(buffer, 32), readString(buffer, 64));
        }
        EnumMap<Slot, Equipment> equipment = new EnumMap<>(Slot.class);
        for (Slot slot : Slot.values()) {
            equipment.put(slot, new Equipment(
                    readEnum(buffer, EquipmentSource.class),
                    readString(buffer, 64), readString(buffer, 64),
                    readString(buffer, 16), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean()));
        }
        Mob result = new Mob(schema, revision, id, name, entityType,
                category, enabled, level, nameplate, tags, stats,
                attack, ai, new Appearance(
                scale, age, glowing, glowingColor, variants, equipment));
        if (!finite(result)) throw new IllegalArgumentException("Non-finite mob field");
        return result;
    }

    public static void writeMob(FriendlyByteBuf buffer, Mob value) {
        if (value == null || value.level() < 0 || value.level() > 65_535
                || !finite(value)) {
            throw new IllegalArgumentException("Invalid mob numeric fields");
        }
        buffer.writeByte(value.schemaVersion());
        buffer.writeLong(value.revision());
        writeString(buffer, value.id(), 64);
        writeString(buffer, value.displayName(), 128);
        writeString(buffer, value.entityType(), 64);
        writeEnum(buffer, value.category());
        buffer.writeBoolean(value.enabled());
        buffer.writeShort(value.level());
        writeEnum(buffer, value.nameplate());
        writeStrings(buffer, value.tags(), 32, 32);
        Stats stats = value.stats();
        buffer.writeDouble(stats.maxHealth());
        buffer.writeDouble(stats.physicalAttack());
        buffer.writeDouble(stats.magicalAttack());
        buffer.writeDouble(stats.physicalDefense());
        buffer.writeDouble(stats.magicalDefense());
        buffer.writeDouble(stats.movementSpeed());
        buffer.writeDouble(stats.attackSpeed());
        buffer.writeDouble(stats.criticalChance());
        buffer.writeDouble(stats.criticalDamage());
        buffer.writeDouble(stats.damageReduction());
        BasicAttack attack = value.attack();
        writeEnum(buffer, attack.damageType());
        buffer.writeDouble(attack.fixedDamage());
        buffer.writeDouble(attack.coefficient());
        buffer.writeDouble(attack.intervalSeconds());
        buffer.writeDouble(attack.range());
        buffer.writeDouble(attack.knockback());
        buffer.writeBoolean(attack.criticalAllowed());
        Ai ai = value.ai();
        writeEnum(buffer, ai.preset());
        writeEnum(buffer, ai.priority());
        buffer.writeDouble(ai.aggroRange());
        buffer.writeDouble(ai.chaseRange());
        buffer.writeDouble(ai.leashRange());
        buffer.writeDouble(ai.attackRange());
        buffer.writeDouble(ai.refreshSeconds());
        buffer.writeBoolean(ai.returnHome());
        buffer.writeBoolean(ai.resetHealth());
        buffer.writeBoolean(ai.avoidFalls());
        buffer.writeBoolean(ai.avoidWater());
        Appearance appearance = value.appearance();
        buffer.writeDouble(appearance.scale());
        writeEnum(buffer, appearance.age());
        buffer.writeBoolean(appearance.glowing());
        writeString(buffer, appearance.glowingColor(), 32);
        if (appearance.variants().size() > 16) {
            throw new IllegalArgumentException("Too many variants");
        }
        buffer.writeByte(appearance.variants().size());
        appearance.variants().forEach((key, variant) -> {
            writeString(buffer, key, 32);
            writeString(buffer, variant, 64);
        });
        for (Slot slot : Slot.values()) {
            Equipment entry = appearance.equipment().get(slot);
            writeEnum(buffer, entry.source());
            writeString(buffer, entry.referenceId(), 64);
            writeString(buffer, entry.material(), 64);
            writeString(buffer, entry.color(), 16);
            buffer.writeBoolean(entry.glint());
            buffer.writeBoolean(entry.visible());
            buffer.writeBoolean(entry.visualOnly());
        }
    }

    public static Head readHead(FriendlyByteBuf buffer) {
        return new Head(
                buffer.readUnsignedByte(), buffer.readLong(),
                readString(buffer, 64), readString(buffer, 128),
                readEnum(buffer, HeadSource.class), readString(buffer, 64),
                readString(buffer, 16_384), readString(buffer, 64),
                readStrings(buffer, 32, 32), buffer.readBoolean(),
                readString(buffer, 256));
    }

    public static void writeHead(FriendlyByteBuf buffer, Head value) {
        buffer.writeByte(value.schemaVersion());
        buffer.writeLong(value.revision());
        writeString(buffer, value.id(), 64);
        writeString(buffer, value.displayName(), 128);
        writeEnum(buffer, value.source());
        writeString(buffer, value.playerName(), 64);
        writeString(buffer, value.textureValue(), 16_384);
        writeString(buffer, value.projectsItemId(), 64);
        writeStrings(buffer, value.tags(), 32, 32);
        buffer.writeBoolean(value.favorite());
        writeString(buffer, value.sourceNote(), 256);
    }

    public static String readString(FriendlyByteBuf buffer, int maximumBytes) {
        int length = buffer.readUnsignedShort();
        if (length > maximumBytes || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("String is too long");
        }
        return buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();
    }

    public static void writeString(
            FriendlyByteBuf buffer,
            String value,
            int maximumBytes
    ) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes) throw new IllegalArgumentException("String is too long");
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static <T extends Enum<T>> T readEnum(
            FriendlyByteBuf buffer,
            Class<T> type
    ) {
        try {
            return Enum.valueOf(type, readString(buffer, 32));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid enum", exception);
        }
    }

    private static void writeEnum(FriendlyByteBuf buffer, Enum<?> value) {
        if (value == null) throw new IllegalArgumentException("Missing enum");
        writeString(buffer, value.name(), 32);
    }

    static boolean finite(Mob value) {
        Stats stats = value.stats();
        BasicAttack attack = value.attack();
        Ai ai = value.ai();
        return stats != null
                && DoubleStream.of(stats.maxHealth(), stats.physicalAttack(),
                stats.magicalAttack(), stats.physicalDefense(), stats.magicalDefense(),
                stats.movementSpeed(), stats.attackSpeed(), stats.criticalChance(),
                stats.criticalDamage(), stats.damageReduction()).allMatch(Double::isFinite)
                && attack != null
                && DoubleStream.of(attack.fixedDamage(), attack.coefficient(),
                attack.intervalSeconds(), attack.range(), attack.knockback())
                .allMatch(Double::isFinite)
                && ai != null
                && DoubleStream.of(ai.aggroRange(), ai.chaseRange(), ai.leashRange(),
                ai.attackRange(), ai.refreshSeconds()).allMatch(Double::isFinite)
                && value.appearance() != null
                && Double.isFinite(value.appearance().scale());
    }

    private static List<String> readStrings(
            FriendlyByteBuf buffer,
            int maximumCount,
            int maximumBytes
    ) {
        int count = buffer.readUnsignedByte();
        if (count > maximumCount) throw new IllegalArgumentException("Too many strings");
        ArrayList<String> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(readString(buffer, maximumBytes));
        }
        return List.copyOf(result);
    }

    private static void writeStrings(
            FriendlyByteBuf buffer,
            List<String> values,
            int maximumCount,
            int maximumBytes
    ) {
        if (values.size() > maximumCount) throw new IllegalArgumentException("Too many strings");
        buffer.writeByte(values.size());
        for (String value : values) writeString(buffer, value, maximumBytes);
    }
}
