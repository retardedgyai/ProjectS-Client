package io.github.gyai.projects.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MobPreviewEntity implements AutoCloseable {
    public enum Animation { IDLE, WALK, ATTACK, HURT }

    private LivingEntity entity;
    private String entityType = "";
    private MobEditorData.Mob lastDefinition;
    private MobEditorData.Head lastHead;
    private Animation animation = Animation.IDLE;
    private boolean closed;

    public LivingEntity update(
            MobEditorData.Mob definition,
            MobEditorData.Head selectedHead
    ) {
        if (closed) closed = false;
        if (entity == null || !entityType.equals(definition.entityType())) {
            create(definition.entityType());
        }
        if (entity == null) return null;
        if (definition.equals(lastDefinition)
                && java.util.Objects.equals(selectedHead, lastHead)) return entity;
        entity.setCustomName(Component.literal(definition.displayName()));
        entity.setCustomNameVisible(
                definition.nameplate() == MobEditorData.NameplateMode.ALWAYS);
        entity.setGlowingTag(definition.appearance().glowing());
        var scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(definition.appearance().scale());
        if (entity instanceof AgeableMob ageable) {
            ageable.setBaby(definition.appearance().age() == MobEditorData.Age.BABY);
        }
        applyVariants(definition.appearance().variants());
        applyEquipment(definition.appearance(), selectedHead);
        lastDefinition = definition;
        lastHead = selectedHead;
        return entity;
    }

    public static boolean knownEntityType(String typeName) {
        if (typeName == null || typeName.isBlank()) return false;
        Identifier id = Identifier.tryBuild(
                "minecraft", typeName.toLowerCase(Locale.ROOT));
        if (id == null) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Minecraft client = Minecraft.getInstance();
        if (type == null || client.level == null) return false;
        try {
            return type.create(client.level, EntitySpawnReason.COMMAND) instanceof Mob;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public LivingEntity entity() {
        return entity;
    }

    public void setAnimation(Animation value) {
        animation = value;
    }

    public void tick() {
        if (entity == null || closed) return;
        entity.tickCount++;
        switch (animation) {
            case IDLE -> entity.walkAnimation.stop();
            case WALK -> entity.walkAnimation.update(1, .35f, 1);
            case ATTACK -> {
                if (!entity.swinging) entity.swing(InteractionHand.MAIN_HAND);
            }
            case HURT -> entity.hurtTime = 6;
        }
    }

    private void create(String typeName) {
        entity = null;
        lastDefinition = null;
        lastHead = null;
        entityType = typeName;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Identifier id = Identifier.tryBuild(
                "minecraft", typeName.toLowerCase(Locale.ROOT));
        if (id == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Entity created = type == null ? null
                : type.create(client.level, EntitySpawnReason.COMMAND);
        if (created instanceof LivingEntity living) {
            entity = living;
            entity.setYRot(0);
            entity.setXRot(0);
        }
    }

    private void applyVariants(Map<String, String> variants) {
        if (entity instanceof Slime slime && variants.containsKey("size")) {
            try {
                slime.setSize(Math.clamp(
                        Integer.parseInt(variants.get("size")), 1, 127), false);
            } catch (NumberFormatException ignored) {
                slime.setSize(1, false);
            }
        }
        if (entity instanceof Sheep sheep) {
            String color = variants.get("color");
            if (color != null) {
                try {
                    sheep.setColor(net.minecraft.world.item.DyeColor.valueOf(
                            color.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) { }
            }
            if (variants.containsKey("sheared")) {
                sheep.setSheared(Boolean.parseBoolean(variants.get("sheared")));
            }
        }
        if (entity instanceof Wolf wolf) {
            dye(variants.get("collar-color"), color ->
                    wolf.setComponent(DataComponents.WOLF_COLLAR, color));
            dynamicVariant(Registries.WOLF_VARIANT, variants.get("variant"), value ->
                    wolf.setComponent(DataComponents.WOLF_VARIANT, value));
            if (Boolean.parseBoolean(variants.getOrDefault("angry", "false"))) {
                wolf.setPersistentAngerEndTime(
                        wolf.level().getGameTime() + 20 * 60);
            } else {
                wolf.setPersistentAngerEndTime(0);
            }
        }
        if (entity instanceof Cat cat) {
            dye(variants.get("collar-color"), color ->
                    cat.setComponent(DataComponents.CAT_COLLAR, color));
            dynamicVariant(Registries.CAT_VARIANT, variants.get("variant"), value ->
                    cat.setComponent(DataComponents.CAT_VARIANT, value));
        }
        if (entity instanceof Horse horse) {
            enumValue(Variant.class, variants.get("color"), value ->
                    horse.setComponent(DataComponents.HORSE_VARIANT, value));
        }
        if (entity instanceof Villager villager) {
            String profession = variants.get("profession");
            String type = variants.get("villager-type");
            var data = villager.getVillagerData();
            if (profession != null) {
                Identifier id = Identifier.tryBuild(
                        "minecraft", profession.toLowerCase(Locale.ROOT));
                if (id != null) {
                    var selected = BuiltInRegistries.VILLAGER_PROFESSION.get(id);
                    if (selected.isPresent()) data = data.withProfession(selected.get());
                }
            }
            if (type != null) {
                Identifier id = Identifier.tryBuild(
                        "minecraft", type.toLowerCase(Locale.ROOT));
                if (id != null) {
                    var selected = BuiltInRegistries.VILLAGER_TYPE.get(id);
                    if (selected.isPresent()) data = data.withType(selected.get());
                }
            }
            villager.setVillagerData(data);
        }
    }

    private <T> void dynamicVariant(
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey,
            String value,
            java.util.function.Consumer<net.minecraft.core.Holder<T>> setter
    ) {
        if (value == null || value.isBlank() || entity == null) return;
        Identifier id = Identifier.tryBuild(
                "minecraft", value.toLowerCase(Locale.ROOT));
        if (id != null) {
            entity.registryAccess().lookupOrThrow(registryKey).get(id)
                    .ifPresent(setter);
        }
    }

    private static void dye(
            String value,
            java.util.function.Consumer<net.minecraft.world.item.DyeColor> setter
    ) {
        enumValue(net.minecraft.world.item.DyeColor.class, value, setter);
    }

    private static <T extends Enum<T>> void enumValue(
            Class<T> type,
            String value,
            java.util.function.Consumer<T> setter
    ) {
        if (value == null || value.isBlank()) return;
        try {
            setter.accept(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            // The server validator reports unsupported values.
        }
    }

    private void applyEquipment(
            MobEditorData.Appearance appearance,
            MobEditorData.Head selectedHead
    ) {
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            MobEditorData.Equipment entry = appearance.equipment().get(slot);
            ItemStack item = createItem(entry,
                    slot == MobEditorData.Slot.HEAD ? selectedHead : null);
            entity.setItemSlot(switch (slot) {
                case HEAD -> EquipmentSlot.HEAD;
                case CHEST -> EquipmentSlot.CHEST;
                case LEGS -> EquipmentSlot.LEGS;
                case FEET -> EquipmentSlot.FEET;
                case MAIN_HAND -> EquipmentSlot.MAINHAND;
                case OFF_HAND -> EquipmentSlot.OFFHAND;
            }, item);
        }
    }

    private static ItemStack createItem(
            MobEditorData.Equipment entry,
            MobEditorData.Head selectedHead
    ) {
        if (entry == null || !entry.visible()
                || entry.source() == MobEditorData.EquipmentSource.NONE) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = switch (entry.source()) {
            case NONE -> ItemStack.EMPTY;
            case VANILLA_ITEM -> vanilla(entry.material());
            case PROJECTS_ITEM -> projectItem(entry.referenceId());
            case CUSTOM_HEAD -> customHead(entry.referenceId(), selectedHead);
        };
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!entry.color().isBlank() && entry.color().matches("#[0-9a-fA-F]{6}")) {
            stack.set(DataComponents.DYED_COLOR,
                    new DyedItemColor(Integer.parseInt(entry.color().substring(1), 16)));
        }
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, entry.glint());
        return stack;
    }

    private static ItemStack vanilla(String material) {
        Identifier id = Identifier.tryBuild(
                "minecraft", material.toLowerCase(Locale.ROOT));
        if (id == null) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack projectItem(String id) {
        return switch (id) {
            case "starter_sword" -> new ItemStack(Items.IRON_SWORD);
            case "painter_staff" -> new ItemStack(Items.BLAZE_ROD);
            case "starter_bow" -> new ItemStack(Items.BOW);
            default -> new ItemStack(Items.BARRIER);
        };
    }

    private static ItemStack customHead(String expectedId, MobEditorData.Head head) {
        if (head == null || !head.id().equals(expectedId)) {
            return new ItemStack(Items.PLAYER_HEAD);
        }
        if (head.source() == MobEditorData.HeadSource.PROJECTS_ITEM) {
            return projectItem(head.projectsItemId());
        }
        if (head.source() != MobEditorData.HeadSource.TEXTURE_VALUE
                || head.textureValue().isBlank()) return new ItemStack(Items.PLAYER_HEAD);
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(
                head.id().getBytes(StandardCharsets.UTF_8)), head.id());
        profile.properties().put(
                "textures", new Property("textures", head.textureValue()));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return stack;
    }

    @Override
    public void close() {
        closed = true;
        entity = null;
        lastDefinition = null;
        lastHead = null;
    }

    public static void clearGlobalReference() {
        // Preview entities are instance-owned; retained for disconnect call compatibility.
    }
}
