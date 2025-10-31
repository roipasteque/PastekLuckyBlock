package net.pastek.luckyblock.common.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.prefab.configuration.LBConfigurationHandler;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.util.*;
public class LootTableModifier extends LootModifier {

    public static final MapCodec<LootTableModifier> CODEC = RecordCodecBuilder.mapCodec(
            (RecordCodecBuilder.Instance<LootTableModifier> inst) ->
                    codecStart(inst)
                            .and(ResourceLocation.CODEC.fieldOf("injected_loot_table")
                                    .forGetter(m -> m.injectedLootTable))
                            .and(ResourceLocation.CODEC.listOf().fieldOf("target_loot_tables")
                                    .forGetter(m -> m.targetLootTables))
                            .and(com.mojang.serialization.Codec.STRING.optionalFieldOf("modid")
                                    .forGetter(m -> m.requiredModid))
                            .apply(inst, LootTableModifier::new)
    );

    private final ResourceLocation injectedLootTable;
    private final List<ResourceLocation> targetLootTables;
    private final Optional<String> requiredModid;

    private static final Map<ResourceLocation, List<WeightedEntry>> ENTRY_CACHE = new HashMap<>();
    private static final Set<String> SKIPPED_LOGGED = new HashSet<>();

    record WeightedEntry(
            Item item,
            int count,
            float damageMin,
            float damageMax,
            boolean enchantRandom,
            boolean enchantWithLevels,
            int minLevel,
            int maxLevel,
            boolean treasureAllowed,
            int weight
    ) {}

    public LootTableModifier(LootItemCondition[] conditionsIn,
                             ResourceLocation injectedLootTable,
                             List<ResourceLocation> targetLootTables,
                             Optional<String> requiredModid) {
        super(conditionsIn);
        this.injectedLootTable = injectedLootTable;
        this.targetLootTables = targetLootTables;
        this.requiredModid = requiredModid;
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        boolean debug = LBConfigurationHandler.COMMON.debugLogging.get();

        // --- Skip injection if required mod is not loaded ---
        if (requiredModid.isPresent() && !ModList.get().isLoaded(requiredModid.get())) {
            String key = injectedLootTable + "|" + targetLootTables;
            if (debug && !SKIPPED_LOGGED.contains(key)) {
                PastekLuckyBlock.LOGGER.info(
                        "[PastekLuckyBlock] Skipped injection '{}' → target loot table '{}' because required mod '{}' is not loaded",
                        injectedLootTable, targetLootTables, requiredModid.get()
                );
                SKIPPED_LOGGED.add(key);
            }
            return generatedLoot;
        }

        // --- Only apply to target loot table ---
        ResourceLocation currentTable = context.getQueriedLootTableId();
        if (currentTable == null || !targetLootTables.contains(currentTable)) return generatedLoot;

        if (debug) {
            PastekLuckyBlock.LOGGER.info("[PastekLuckyBlock] Applying injection '{}' into '{}'",
                    injectedLootTable, targetLootTables);
        }

        ResourceManager resourceManager = context.getLevel().getServer().getResourceManager();
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                injectedLootTable.getNamespace(),
                injectedLootTable.getPath() + ".json"
        );

        var optionalResource = resourceManager.getResource(path);
        if (optionalResource.isEmpty()) {
            if (debug)
                PastekLuckyBlock.LOGGER.debug("[PastekLuckyBlock] Injection resource not found: '{}'", path);
            return generatedLoot;
        }

        try (var inputStream = optionalResource.get().open();
             var reader = new InputStreamReader(inputStream)) {

            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // --- Check trigger chance ---
            double triggerChance = json.has("trigger_chance") ? json.get("trigger_chance").getAsDouble() : 1.0;
            if (context.getRandom().nextDouble() > triggerChance) {
                if (debug)
                    PastekLuckyBlock.LOGGER.info("[PastekLuckyBlock] Skipped injection '{}' (chance {}) into '{}'",
                            injectedLootTable, String.format(Locale.US, "%.2f", triggerChance), targetLootTables);
                return generatedLoot;
            }

            if (!json.has("pools")) return generatedLoot;
            var pools = json.getAsJsonArray("pools");

            for (var poolEl : pools) {
                var poolObj = poolEl.getAsJsonObject();

                // --- Determine number of rolls ---
                int rolls = 1;
                if (poolObj.has("rolls")) {
                    var rollsVal = poolObj.get("rolls");
                    if (rollsVal.isJsonPrimitive()) rolls = rollsVal.getAsInt();
                    else if (rollsVal.isJsonObject() && rollsVal.getAsJsonObject().has("min") && rollsVal.getAsJsonObject().has("max")) {
                        int min = rollsVal.getAsJsonObject().get("min").getAsInt();
                        int max = rollsVal.getAsJsonObject().get("max").getAsInt();
                        rolls = min + context.getRandom().nextInt(max - min + 1);
                    }
                }
                rolls += poolObj.has("bonus_rolls") ? poolObj.get("bonus_rolls").getAsInt() : 0;

                // --- Get or parse weighted entries ---
                List<WeightedEntry> weightedEntries;
                if (ENTRY_CACHE.containsKey(injectedLootTable)) {
                    weightedEntries = ENTRY_CACHE.get(injectedLootTable);
                } else {
                    weightedEntries = new ArrayList<>();
                    if (poolObj.has("entries")) {
                        var entries = poolObj.getAsJsonArray("entries");
                        for (var entryEl : entries) {
                            var entry = entryEl.getAsJsonObject();
                            if (!"minecraft:item".equals(entry.get("type").getAsString())) continue;

                            String itemName = entry.get("name").getAsString();
                            Item item = context.getLevel().registryAccess()
                                    .registryOrThrow(Registries.ITEM)
                                    .get(ResourceLocation.tryParse(itemName));
                            if (item == null) {
                                if (debug)
                                    PastekLuckyBlock.LOGGER.warn("[PastekLuckyBlock] Missing item '{}'", itemName);
                                continue;
                            }

                            int count = 1;
                            float dmgMin = 0f, dmgMax = 0f;
                            boolean enchantRandom = false, enchantWithLevels = false;
                            int minLevel = 0, maxLevel = 0;
                            boolean treasureAllowed = false;
                            int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;

                            if (entry.has("functions")) {
                                var functions = entry.getAsJsonArray("functions");
                                for (var funcEl : functions) {
                                    var funcObj = funcEl.getAsJsonObject();
                                    if (!funcObj.has("function")) continue;

                                    switch (funcObj.get("function").getAsString()) {
                                        case "minecraft:set_count" -> {
                                            if (funcObj.has("count")) {
                                                var countVal = funcObj.get("count");
                                                if (countVal.isJsonPrimitive()) count = countVal.getAsInt();
                                                else if (countVal.isJsonObject() && "minecraft:uniform".equals(countVal.getAsJsonObject().get("type").getAsString())) {
                                                    int min = countVal.getAsJsonObject().get("min").getAsInt();
                                                    int max = countVal.getAsJsonObject().get("max").getAsInt();
                                                    count = min + context.getRandom().nextInt(max - min + 1);
                                                }
                                            }
                                        }
                                        case "minecraft:set_damage" -> {
                                            if (funcObj.has("damage")) {
                                                var dmgVal = funcObj.get("damage");
                                                if (dmgVal.isJsonPrimitive()) dmgMin = dmgMax = dmgVal.getAsFloat();
                                                else if (dmgVal.isJsonObject() && "minecraft:uniform".equals(dmgVal.getAsJsonObject().get("type").getAsString())) {
                                                    dmgMin = dmgVal.getAsJsonObject().get("min").getAsFloat();
                                                    dmgMax = dmgVal.getAsJsonObject().get("max").getAsFloat();
                                                }
                                            }
                                        }
                                        case "minecraft:enchant_randomly" -> enchantRandom = true;
                                        case "minecraft:enchant_with_levels" -> {
                                            enchantWithLevels = true;
                                            if (funcObj.has("levels")) {
                                                var levelVal = funcObj.get("levels");
                                                if (levelVal.isJsonPrimitive()) minLevel = maxLevel = levelVal.getAsInt();
                                                else if (levelVal.isJsonObject() && "minecraft:uniform".equals(levelVal.getAsJsonObject().get("type").getAsString())) {
                                                    minLevel = levelVal.getAsJsonObject().get("min").getAsInt();
                                                    maxLevel = levelVal.getAsJsonObject().get("max").getAsInt();
                                                }
                                            }
                                            treasureAllowed = funcObj.has("treasure") && funcObj.get("treasure").getAsBoolean();
                                        }
                                    }
                                }
                            }

                            weightedEntries.add(new WeightedEntry(item, count, dmgMin, dmgMax,
                                    enchantRandom, enchantWithLevels, minLevel, maxLevel, treasureAllowed, weight));
                        }
                    }
                    ENTRY_CACHE.put(injectedLootTable, weightedEntries);
                }

                // --- Generate new ItemStacks per-roll ---
                for (int i = 0; i < rolls; i++) {
                    if (weightedEntries.isEmpty()) continue;

                    int totalWeight = weightedEntries.stream().mapToInt(e -> e.weight).sum();
                    int choice = context.getRandom().nextInt(totalWeight);

                    for (WeightedEntry e : weightedEntries) {
                        choice -= e.weight;
                        if (choice < 0) {
                            ItemStack stack = new ItemStack(e.item, Math.max(e.count, 1));
                            if (stack.isDamageableItem() && e.damageMax > 0f) {
                                float dmg = e.damageMin + context.getRandom().nextFloat() * (e.damageMax - e.damageMin);
                                stack.setDamageValue((int)(stack.getMaxDamage() * dmg));
                            }
                            if (e.enchantRandom && stack.isEnchantable())
                                EnchantmentHelper.enchantItem(context.getRandom(), stack, 30, context.getLevel().registryAccess(), Optional.empty());
                            if (e.enchantWithLevels && stack.isEnchantable()) {
                                int level = e.minLevel + context.getRandom().nextInt(e.maxLevel - e.minLevel + 1);
                                EnchantmentHelper.enchantItem(context.getRandom(), stack, level, context.getLevel().registryAccess(), Optional.empty());
                            }

                            generatedLoot.add(stack);
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (debug)
                PastekLuckyBlock.LOGGER.error("[PastekLuckyBlock] Failed to load '{}' → {}", injectedLootTable, e.toString());
        }

        return generatedLoot;
    }


    @Override
    public MapCodec<LootTableModifier> codec() {
        return CODEC;
    }
}
