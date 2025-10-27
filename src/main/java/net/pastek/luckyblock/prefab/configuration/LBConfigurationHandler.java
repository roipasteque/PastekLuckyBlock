package net.pastek.luckyblock.prefab.configuration;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class LBConfigurationHandler {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ModConfigSpec> specPair =
                new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ModConfigSpec.IntValue badChance;
        public final ModConfigSpec.IntValue midChance;
        public final ModConfigSpec.IntValue goodChance;
        public final ModConfigSpec.IntValue luckyChance;
        public final ModConfigSpec.ConfigValue<List<? extends String>> lootTables;
        public final ModConfigSpec.BooleanValue debugLogging;
        public final ModConfigSpec.BooleanValue luckyblockTexture;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("pastekluckyblock");

            // ---- Watermelon Texture ----
            luckyblockTexture = builder
                    .comment("If true, the Lucky Block will have a watermelon custom texture.")
                    .define("useWatermelonLuckyBlockTexture", false);

            // ---- Chances Section ----
            badChance = builder
                    .comment(
                            "\nChance out of 100 for BAD loot",
                            "Default: 15"
                    )
                    .defineInRange("badChance", 15, 0, 100);

            midChance = builder
                    .comment(
                            "\nChance out of 100 for MID loot",
                            "Default: 40"
                    )
                    .defineInRange("midChance", 40, 0, 100);

            goodChance = builder
                    .comment(
                            "\nChance out of 100 for GOOD loot",
                            "Default: 40"
                    )
                    .defineInRange("goodChance", 40, 0, 100);

            luckyChance = builder
                    .comment(
                            "\nChance out of 100 for LUCKY loot",
                            "Default: 5"
                    )
                    .defineInRange("luckyChance", 5, 0, 100);

            // ---- Loot Tables Section ----
            lootTables = builder
                    .comment(
                            "\nList of loot table IDs for random chests.",
                            "Accepts both vanilla and modded loot tables.",
                            "Example: \"minecraft:chests/simple_dungeon\""
                    )
                    .defineListAllowEmpty(
                            List.of("lootTables"),
                            () -> List.of(
                                    "minecraft:chests/simple_dungeon",
                                    "minecraft:chests/abandoned_mineshaft"
                            ),
                            obj -> obj instanceof String
                    );

            debugLogging = builder
                    .comment(
                            "\n\n\nEnable detailed debug logs for Pastek Lucky Block loot injections.",
                            "This will show which tables are injected, missing items and other errors.",
                            "Default: false"
                    )
                    .define("debugLogging", false);

            builder.pop();
        }
    }
}
