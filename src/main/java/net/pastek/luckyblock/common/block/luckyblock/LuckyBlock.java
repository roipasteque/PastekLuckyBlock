package net.pastek.luckyblock.common.block.luckyblock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.prefab.configuration.LBConfigurationHandler;
import net.pastek.luckyblock.registers.LBBlocks;
import net.pastek.luckyblock.registers.LBSounds;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LuckyBlock extends Block {
    public LuckyBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, Variant.DEFAULT));
    }

    public enum Variant implements StringRepresentable {
        DEFAULT("default"),
        WATERMELON("watermelon");
        private final String name;
        Variant(String name) {this.name = name;}
        @Override
        public String getSerializedName() {
            return this.name;
        }
        public static Variant fromName(String name) {
            for (Variant v : values()) {
                if (v.name.equalsIgnoreCase(name)) return v;
            } return DEFAULT;
        }
    }

    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean useCustom = LBConfigurationHandler.COMMON.luckyblockTexture.get();
        Variant variant = useCustom ? Variant.WATERMELON : Variant.DEFAULT;
        return this.defaultBlockState().setValue(VARIANT, variant);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));

        generateRandomLoot(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Chooses one of the four loot categories at random and triggers it.
     */
    private void generateRandomLoot(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        Random random = new Random();

        int bad = LBConfigurationHandler.COMMON.badChance.get();
        int mid = LBConfigurationHandler.COMMON.midChance.get();
        int good = LBConfigurationHandler.COMMON.goodChance.get();
        int lucky = LBConfigurationHandler.COMMON.luckyChance.get();

        int total = bad + mid + good + lucky;
        if (total <= 0) total = 1;

        int roll = random.nextInt(total);

        if (roll < bad) {
            generateBadLoot(level, pos, player, random);
        } else if (roll < bad + mid) {
            generateMidLoot(level, pos, player, random);
        } else if (roll < bad + mid + good) {
            generateGoodLoot(level, pos, player, random);
        } else {
            generateLuckyLoot(level, pos, player, random);
        }
    }


    /** -------------------- BAD LOOT -------------------- */
    private void generateBadLoot(Level level, BlockPos pos, Player player, Random random) {
        level.playSound(null, pos, LBSounds.SOUND_BADLOOT.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
        spawnParticles(level, pos, ParticleTypes.ANGRY_VILLAGER);

        Map<Runnable, Integer> badLootOptions = new LinkedHashMap<>();
        badLootOptions.put(() -> level.explode(player, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, 5f, true, Level.ExplosionInteraction.TNT), 5);
        badLootOptions.put(() -> surroundWithFluid(level, player.blockPosition(), Blocks.LAVA), 2);
        badLootOptions.put(() -> surroundWithFluid(level, player.blockPosition(), Blocks.WATER), 2);
        badLootOptions.put(() -> spawnHostile(level, pos, random, List.of(EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN, EntityType.ILLUSIONER)), 1);
        badLootOptions.put(() -> rainAnvils(level, player.blockPosition().above(15)), 2);
        badLootOptions.put(() -> rideTntChicken(level, player), 5);
        badLootOptions.put(() -> spawnMobColumn(level, player.blockPosition(), EntityType.CREEPER, 10), 1);
        badLootOptions.put(() -> spawnMobColumn(level, player.blockPosition(), EntityType.BLAZE, 10), 1);
        badLootOptions.put(() -> spawnMobColumn(level, player.blockPosition(), EntityType.SILVERFISH, 10), 1);
        badLootOptions.put(() -> giveBadEffects(player), 1);
        badLootOptions.put(() -> player.push(0, 5, 0), 5);
        badLootOptions.put(() -> surroundFloor(level, player.blockPosition(), random), 1);
        badLootOptions.put(() -> trapInObsidianCage(level, player.blockPosition()), 5);
        badLootOptions.put(() -> scrambleBlocks(level, player.blockPosition(), random), 2);
        badLootOptions.put(() -> dripstoneTrap(level, player.blockPosition()), 1);
        badLootOptions.put(() -> giveRandomBadEffect(player, 20*15, 4), 3);
        badLootOptions.put(() -> spawnRaid((ServerLevel) level, player, pos), 5);

        Runnable selected = getWeightedRandom(random, badLootOptions);
        if (selected != null) selected.run();
    }

    /** -------------------- MID LOOT -------------------- */
    private void generateMidLoot(Level level, BlockPos pos, Player player, Random random) {
        level.playSound(null, pos, LBSounds.SOUND_MIDLOOT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        spawnParticles(level, pos, ParticleTypes.SMOKE);

        Map<Runnable, Integer> midLootOptions = new LinkedHashMap<>();
        midLootOptions.put(() -> spawnLootChest(level, pos, "chests/mid_loot"), 10);
        midLootOptions.put(() -> spawnRandomEntity(level, pos), 8);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.ENDER_PEARL, 3), new ItemStack(Items.ENDER_EYE)), 1);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.WATER_BUCKET)), 3);
        midLootOptions.put(() -> dropAllDyes(level, pos), 1);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.TNT, 4)), 3);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.BREAD, 5), new ItemStack(Items.APPLE, 3)), 3);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.REDSTONE, 15), new ItemStack(Items.REPEATER, 2)), 1);
        midLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.FISHING_ROD), new ItemStack(Items.COD, 3)), 3);

        Runnable selected = getWeightedRandom(random, midLootOptions);
        if (selected != null) selected.run();
    }

    /** -------------------- GOOD LOOT -------------------- */
    private void generateGoodLoot(Level level, BlockPos pos, Player player, Random random) {
        level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.BLOCKS, 0.8f, 1.0f);
        spawnParticles(level, pos, ParticleTypes.HAPPY_VILLAGER);

        Map<Runnable, Integer> goodLootOptions = new LinkedHashMap<>();
        goodLootOptions.put(() -> spawnLootChest(level, pos, "chests/good_loot"), 10);
        goodLootOptions.put(() -> { if (!level.isClientSide && level instanceof ServerLevel serverLevel) placeRandomLootChest(serverLevel, pos, random); }, 10);
        goodLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.EMERALD, 3)), 2);
        goodLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.GOLDEN_APPLE, 2)), 2);
        goodLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.IRON_BLOCK, 4)), 2);
        goodLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.EXPERIENCE_BOTTLE, 10)), 3);
        goodLootOptions.put(() -> dropDiamondGear(level, pos), 1);
        goodLootOptions.put(() -> giveRandomGoodEffect(player, 20*15, 4), 1);

        Runnable selected = getWeightedRandom(random, goodLootOptions);
        if (selected != null) selected.run();
    }

    /** -------------------- LUCKY LOOT -------------------- */
    private void generateLuckyLoot(Level level, BlockPos pos, Player player, Random random) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
        spawnParticles(level, pos, ParticleTypes.TOTEM_OF_UNDYING);

        Map<Runnable, Integer> luckyLootOptions = new LinkedHashMap<>();
        luckyLootOptions.put(() -> spawnLootChest(level, pos, "chests/lucky_loot"), 10);
        luckyLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.BEACON)), 2);
        luckyLootOptions.put(() -> dropItems(level, pos, new ItemStack(LBBlocks.LUCKY_BLOCK.get(), 5), new ItemStack(Items.DIAMOND, 10)), 3);
        luckyLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.ENCHANTING_TABLE), new ItemStack(Items.BOOKSHELF, 18)), 3);
        luckyLootOptions.put(() -> dropItems(level, pos, new ItemStack(Items.NETHER_STAR)), 2);
        luckyLootOptions.put(() -> createExperienceShower(level, pos, random), 5);
        luckyLootOptions.put(() -> dropRandomEnchantedItem(level, pos), 6);

        Runnable selected = getWeightedRandom(random, luckyLootOptions);
        if (selected != null) selected.run();
    }


    // -------------------- Functions --------------------

    private <T> T getWeightedRandom(Random random, Map<T, Integer> items) {
        int totalWeight = items.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Map.Entry<T, Integer> entry : items.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void dropItems(Level level, BlockPos pos, ItemStack... items) {
        for (ItemStack item : items) Block.popResource(level, pos, item);
    }

    private void spawnLootChest(Level level, BlockPos pos, String lootTable) {
        if (level.isClientSide) return;

        BlockPos chestPos = pos.above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            ResourceLocation rl = PastekLuckyBlock.rl(lootTable);
            ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, rl);
            chest.setLootTable(lootTableKey, level.getRandom().nextLong());
        }
    }

    private void spawnRandomEntity(Level level, BlockPos pos) {
        List<EntityType<?>> mobs = level.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .stream()
                .filter(t -> t.create(level) instanceof LivingEntity)
                .toList();

        if (mobs.isEmpty()) return;

        EntityType<?> type = mobs.get(level.random.nextInt(mobs.size()));
        Entity entity = type.create(level);

        if (entity != null) {
            entity.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            level.addFreshEntity(entity);
        }
    }

    private void trapInObsidianCage(Level level, BlockPos center) {
        for (int dx=-2; dx<=2; dx++) for (int dy=0; dy<=3; dy++) for (int dz=-2; dz<=2; dz++) {
            if (dx>=-1 && dx<=1 && dy>=1 && dy<=2 && dz>=-1 && dz<=1) continue;
            level.setBlock(center.offset(dx,dy,dz), Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
        for (int dx=-1; dx<=1; dx++) for (int dy=1; dy<=2; dy++) for (int dz=-1; dz<=1; dz++)
            level.setBlock(center.offset(dx,dy,dz), Blocks.WATER.defaultBlockState(), 3);
    }

    private void spawnParticles(Level level, BlockPos pos, ParticleOptions particle) {
        if (level.isClientSide) return;

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    particle,
                    pos.getX() + 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5,
                    20,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    private void createExperienceShower(Level level, BlockPos pos, Random random) {
        for (int i=0;i<20;i++)
            level.addFreshEntity(new ExperienceOrb(level,pos.getX()+.5+(random.nextDouble()-.5)*2,pos.getY()+2+random.nextDouble()*3,pos.getZ()+.5+(random.nextDouble()-.5)*2,random.nextInt(10)+5));
        for (int i=0;i<100;i++)
            level.addParticle(ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,1,2,2);
    }

    private void dropDiamondGear(Level level, BlockPos pos) {
        dropItems(level, pos,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_CHESTPLATE)
        );
    }

    private void dropAllDyes(Level level, BlockPos pos) {
        for (DyeColor color : DyeColor.values()) {
            dropItems(level, pos, new ItemStack(DyeItem.byColor(color), 3));
        }
    }

    private void placeRandomLootChest(ServerLevel level, BlockPos pos, Random random) {
        List<String> configTables = (List<String>) LBConfigurationHandler.COMMON.lootTables.get();
        List<ResourceKey<LootTable>> available = new ArrayList<>();

        for (String tableId : configTables) {
            ResourceLocation rl = ResourceLocation.tryParse(tableId);
            if (rl == null) continue;

            String modid = rl.getNamespace();
            if (!ModList.get().isLoaded(modid)) {
                if (LBConfigurationHandler.COMMON.debugLogging.get()) {
                    PastekLuckyBlock.LOGGER.debug(
                            "Skipping loot table '{}' because mod '{}' is not loaded", rl, modid
                    );
                }
                continue;
            }

            available.add(ResourceKey.create(Registries.LOOT_TABLE, rl));
        }

        if (available.isEmpty()) {
            ResourceKey<LootTable> fallback = ResourceKey.create(
                    Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(PastekLuckyBlock.MOD_ID, "chests/good_loot")
            );
            available.add(fallback);

            if (LBConfigurationHandler.COMMON.debugLogging.get()) {
                PastekLuckyBlock.LOGGER.warn(
                        "No valid configured loot tables for chest at {}. Using fallback: {}",
                        pos, fallback.location()
                );
            }
        }

        ResourceKey<LootTable> chosen = available.get(random.nextInt(available.size()));

        if (LBConfigurationHandler.COMMON.debugLogging.get()) {
            PastekLuckyBlock.LOGGER.info(
                    "Placing loot chest at {} with loot table: {}",
                    pos, chosen.location()
            );
        }

        BlockPos chestPos = pos.above();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(chosen, random.nextLong());
        }
    }



    private void dripstoneTrap(Level level, BlockPos center) {
        for (int i = 1; i <= 10; i++) {
            BlockPos spike = center.above(i);
            level.setBlock(spike, Blocks.POINTED_DRIPSTONE.defaultBlockState(), 3);
        }
    }

    private void spawnRaid(ServerLevel level, Player player, BlockPos pos) {
        BlockPos playerPos = player.blockPosition();

        Villager nitwit = EntityType.VILLAGER.create(level);
        if (nitwit != null) {
            nitwit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            nitwit.setVillagerData(nitwit.getVillagerData().setProfession(VillagerProfession.NITWIT));
            nitwit.setCustomName(Component.literal("Bob").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
            nitwit.setCustomNameVisible(true);
            level.addFreshEntity(nitwit);
        }

        List<EntityType<?>> patrolEntities = List.of(
                EntityType.PILLAGER,
                EntityType.VINDICATOR,
                EntityType.EVOKER,
                EntityType.RAVAGER
        );

        RandomSource random = level.random;

        int patrolSize = 3 + random.nextInt(3);
        for (int i = 0; i < patrolSize; i++) {
            EntityType<?> type = patrolEntities.get(random.nextInt(patrolEntities.size()));
            Mob mob = (Mob) type.create(level);
            if (mob != null) {
                double offsetX = (random.nextDouble() - 0.5) * 6;
                double offsetZ = (random.nextDouble() - 0.5) * 6;
                BlockPos spawnPos = playerPos.offset((int) (-5 + offsetX), 0, (int) offsetZ);

                mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
                level.addFreshEntity(mob);
            }
        }
    }

    private void scrambleBlocks(Level level, BlockPos center, Random random) {
        for (int i = -3; i <= 3; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -3; k <= 3; k++) {
                    if (random.nextFloat() < 0.1f) {
                        BlockPos target = center.offset(i, j, k);
                        level.setBlock(target, Blocks.COBWEB.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void surroundFloor(Level level, BlockPos center, Random random) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos target = center.offset(dx, -1, dz);
                if (random.nextBoolean()) {
                    level.setBlock(target, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                } else {
                    level.setBlock(target, Blocks.SOUL_SAND.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void giveRandomBadEffect(Player player, int duration, int amplifier) {
        List<Holder<MobEffect>> BAD_EFFECTS = List.of(
                MobEffects.BLINDNESS,
                MobEffects.WEAKNESS,
                MobEffects.MOVEMENT_SLOWDOWN,
                MobEffects.HUNGER,
                MobEffects.POISON,
                MobEffects.CONFUSION,
                MobEffects.LEVITATION
        );

        RandomSource random = player.level().getRandom();
        Holder<MobEffect> effect = BAD_EFFECTS.get(random.nextInt(BAD_EFFECTS.size()));
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }


    private void giveBadEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 10));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 10, 1));
    }

    private static void giveRandomGoodEffect(Player player, int duration, int amplifier) {
        List<Holder<MobEffect>> GOOD_EFFECTS = List.of(
                MobEffects.DAMAGE_RESISTANCE,
                MobEffects.DAMAGE_BOOST,
                MobEffects.SATURATION,
                MobEffects.LUCK,
                MobEffects.DOLPHINS_GRACE,
                MobEffects.FIRE_RESISTANCE,
                MobEffects.CONDUIT_POWER,
                MobEffects.NIGHT_VISION,
                MobEffects.HERO_OF_THE_VILLAGE,
                MobEffects.MOVEMENT_SPEED,
                MobEffects.DIG_SPEED
        );

        RandomSource random = player.level().getRandom();
        Holder<MobEffect> effect = GOOD_EFFECTS.get(random.nextInt(GOOD_EFFECTS.size()));
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    private void spawnMobColumn(Level level, BlockPos base, EntityType<?> mob, int height) {
        for (int i = 0; i < height; i++) {
            Entity e = mob.create(level);
            if (e != null) {
                e.moveTo(base.getX() + .5, base.getY() + i, base.getZ() + .5, 0, 0);
                level.addFreshEntity(e);
            }
        }
    }

    private void rideTntChicken(Level level, Player player) {
        Chicken chicken = EntityType.CHICKEN.create(level);
        PrimedTnt tnt = EntityType.TNT.create(level);
        if (chicken != null && tnt != null) {
            BlockPos pos = player.blockPosition().above();
            chicken.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            tnt.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 0, 0);
            level.addFreshEntity(chicken);
            level.addFreshEntity(tnt);
            tnt.startRiding(chicken, true);
        }
    }

    private void rainAnvils(Level level, BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            BlockPos drop = pos.offset(level.random.nextInt(5) - 2, i, level.random.nextInt(5) - 2);
            level.setBlock(drop, Blocks.ANVIL.defaultBlockState(), 3);
        }
    }

    private void spawnHostile(Level level, BlockPos pos, Random random, List<EntityType<?>> mobs) {
        EntityType<?> type = mobs.get(random.nextInt(mobs.size()));
        Entity e = type.create(level);
        if (e != null) {
            e.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 0, 0);
            level.addFreshEntity(e);
        }
    }

    private void surroundWithFluid(Level level, BlockPos center, Block fluid) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos target = center.offset(dx, 0, dz);
                if (!level.getBlockState(target).isAir()) continue;
                level.setBlock(target, fluid.defaultBlockState(), 3);
            }
        }
    }

    private static void dropRandomEnchantedItem(Level level, BlockPos pos) {
        Map<String, ItemStack> ITEMS = new HashMap<>();

        ITEMS.put("helmet", new ItemStack(Items.NETHERITE_HELMET));
        ITEMS.put("chestplate", new ItemStack(Items.NETHERITE_CHESTPLATE));
        ITEMS.put("leggings", new ItemStack(Items.NETHERITE_LEGGINGS));
        ITEMS.put("boots", new ItemStack(Items.NETHERITE_BOOTS));
        ITEMS.put("turtle_helmet", new ItemStack(Items.TURTLE_HELMET));

        ITEMS.put("sword", new ItemStack(Items.NETHERITE_SWORD));
        ITEMS.put("axe", new ItemStack(Items.NETHERITE_AXE));
        ITEMS.put("pickaxe", new ItemStack(Items.NETHERITE_PICKAXE));
        ITEMS.put("shovel", new ItemStack(Items.NETHERITE_SHOVEL));
        ITEMS.put("hoe", new ItemStack(Items.NETHERITE_HOE));

        ITEMS.put("bow", new ItemStack(Items.BOW));
        ITEMS.put("crossbow", new ItemStack(Items.CROSSBOW));
        ITEMS.put("trident", new ItemStack(Items.TRIDENT));
        ITEMS.put("shield", new ItemStack(Items.SHIELD));
        ITEMS.put("elytra", new ItemStack(Items.ELYTRA));

        List<String> keys = new ArrayList<>(ITEMS.keySet());
        RandomSource random = level.getRandom();
        String type = keys.get(random.nextInt(keys.size()));

        ItemStack item = ITEMS.get(type).copy();
        Map<Holder<Enchantment>, Integer> enchants = new HashMap<>();

        RegistryAccess registryAccess = level.registryAccess();
        Registry<Enchantment> enchantmentRegistry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);

        Holder<Enchantment> protection = enchantmentRegistry.getHolderOrThrow(Enchantments.PROTECTION);
        Holder<Enchantment> unbreaking = enchantmentRegistry.getHolderOrThrow(Enchantments.UNBREAKING);
        Holder<Enchantment> mending = enchantmentRegistry.getHolderOrThrow(Enchantments.MENDING);
        Holder<Enchantment> efficiency = enchantmentRegistry.getHolderOrThrow(Enchantments.EFFICIENCY);
        Holder<Enchantment> sharpness = enchantmentRegistry.getHolderOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> fireAspect = enchantmentRegistry.getHolderOrThrow(Enchantments.FIRE_ASPECT);
        Holder<Enchantment> looting = enchantmentRegistry.getHolderOrThrow(Enchantments.LOOTING);
        Holder<Enchantment> loyalty = enchantmentRegistry.getHolderOrThrow(Enchantments.LOYALTY);
        Holder<Enchantment> channeling = enchantmentRegistry.getHolderOrThrow(Enchantments.CHANNELING);
        Holder<Enchantment> riptide = enchantmentRegistry.getHolderOrThrow(Enchantments.RIPTIDE);
        Holder<Enchantment> thorns = enchantmentRegistry.getHolderOrThrow(Enchantments.THORNS);

        switch (type.toLowerCase()) {
            // Armor
            case "helmet", "chestplate", "leggings", "boots", "turtle_helmet" -> {
                enchants.put(protection, 4);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);

                enchants.put(thorns, 3);
                if (type.equalsIgnoreCase("helmet") || type.equalsIgnoreCase("turtle_helmet")) {
                    Holder<Enchantment> respiration = enchantmentRegistry.getHolderOrThrow(Enchantments.RESPIRATION);
                    Holder<Enchantment> aquaAffinity = enchantmentRegistry.getHolderOrThrow(Enchantments.AQUA_AFFINITY);
                    enchants.put(respiration, 3);
                    enchants.put(aquaAffinity, 1);
                }
                if (type.equalsIgnoreCase("boots")) {
                    Holder<Enchantment> fallProt = enchantmentRegistry.getHolderOrThrow(Enchantments.FEATHER_FALLING);
                    Holder<Enchantment> depthStrider = enchantmentRegistry.getHolderOrThrow(Enchantments.DEPTH_STRIDER);
                    Holder<Enchantment> frostWalker = enchantmentRegistry.getHolderOrThrow(Enchantments.FROST_WALKER);
                    enchants.put(fallProt, 4);
                    enchants.put(depthStrider, 3);
                    enchants.put(frostWalker, 2);
                }
            }

            case "sword" -> {

                enchants.put(sharpness, 5);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
                enchants.put(fireAspect, 2);
                enchants.put(looting, 3);
            }
            case "axe" -> {
                enchants.put(efficiency, 5);
                enchants.put(sharpness, 5);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
            }
            case "pickaxe", "shovel", "hoe" -> {
                Holder<Enchantment> fortune = enchantmentRegistry.getHolderOrThrow(Enchantments.FORTUNE);
                enchants.put(efficiency, 5);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
                enchants.put(fortune, 3);
            }

            case "bow" -> {
                Holder<Enchantment> power = enchantmentRegistry.getHolderOrThrow(Enchantments.POWER);
                Holder<Enchantment> punch = enchantmentRegistry.getHolderOrThrow(Enchantments.PUNCH);
                Holder<Enchantment> flamingArrows = enchantmentRegistry.getHolderOrThrow(Enchantments.FLAME);
                enchants.put(power, 5);
                enchants.put(punch, 2);
                enchants.put(flamingArrows, 1);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
            }
            case "crossbow" -> {
                Holder<Enchantment> quickCharge = enchantmentRegistry.getHolderOrThrow(Enchantments.QUICK_CHARGE);
                Holder<Enchantment> multishot = enchantmentRegistry.getHolderOrThrow(Enchantments.MULTISHOT);
                Holder<Enchantment> piercing = enchantmentRegistry.getHolderOrThrow(Enchantments.PIERCING);
                enchants.put(quickCharge, 3);
                enchants.put(multishot, 1);
                enchants.put(piercing, 4);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
            }

            case "trident" -> {

                enchants.put(loyalty, 3);
                enchants.put(channeling, 1);
                enchants.put(riptide, 3);
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
            }
            case "shield", "elytra" -> {
                enchants.put(unbreaking, 3);
                enchants.put(mending, 1);
            }
        }

        ItemEnchantments.Mutable itemEnchants = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(item));
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchants.entrySet()) {
            itemEnchants.set(entry.getKey(), entry.getValue());
        }

        EnchantmentHelper.setEnchantments(item, itemEnchants.toImmutable());
        Block.popResource(level, pos, item);
    }
}
