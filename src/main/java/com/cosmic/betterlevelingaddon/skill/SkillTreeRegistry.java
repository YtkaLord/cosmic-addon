package com.cosmic.betterlevelingaddon.skill;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Огромное космическое дерево навыков (60 уникальных узлов, более 1050 суммарных уровней прокачки!),
 * разработанное специально под большую сборку из 181 мода (Project MMO, Better Leveling, Farmer's Delight,
 * Mystical Agriculture, Ars Nouveau, Astral Sorcery, Apotheosis, Champions, Lycanites Mobs, Cold Sweat, Dungeons Arise).
 *
 * Первые 3 стартовых навыка доступны сразу:
 * 1) Скорость копания (mining_speed_root)
 * 2) Скорость бега (move_speed_root)
 * 3) Повышение урона (damage_root, +0.5% за уровень)
 */
public class SkillTreeRegistry {
    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    public static void init() {
        NODES.clear();

        // =========================================================================
        // 1. ТРИ ИЗНАЧАЛЬНО ОТКРЫТЫХ СТАРТОВЫХ НАВЫКА (вокруг центра космоса 0,0)
        // =========================================================================
        register(new SkillNode(
                "mining_speed_root",
                "Космический Бурильщик I",
                "Базовый навык шахтёра. Увеличивает скорость разрушения любых блоков и руд.",
                "+%.1f%% к скорости копания",
                SkillCategory.MINING,
                0, -90, 20, 1, 1, 1.5, false, "minecraft:iron_pickaxe"
        ));

        register(new SkillNode(
                "move_speed_root",
                "Звёздный Странник I",
                "Базовый навык скорости. Повышает скорость бега и передвижения по миру.",
                "+%.2f%% к скорости бега",
                SkillCategory.AGILITY,
                -85, 65, 20, 1, 1, 0.75, false, "minecraft:leather_boots"
        ));

        register(new SkillNode(
                "damage_root",
                "Астральная Мощь I",
                "Базовый навык воина. Увеличивает весь наносимый урон в ближнем и дальнем бою.",
                "+%.1f%% к общему урону",
                SkillCategory.COMBAT,
                85, 65, 30, 1, 1, 0.5, false, "minecraft:iron_sword"
        ));

        // =========================================================================
        // 2. СЕВЕРНАЯ ВЕТВЬ — ШАХТЁРСТВО, НЕДРА И МАГНЕТИЗМ (Y < 0)
        // =========================================================================
        register(new SkillNode(
                "haste_pulse",
                "Импульс Спешки",
                "Дополнительно разгоняет взмах кирки и лопаты при добыче твёрдых пород.",
                "+%.1f%% к скорости копания",
                SkillCategory.MINING,
                -90, -185, 15, 1, 1, 2.0, false, "minecraft:golden_pickaxe"
        ).addPrerequisite("mining_speed_root", 5));

        register(new SkillNode(
                "ore_fortune",
                "Искатель Самородков",
                "Шанс получить дополнительный дроп при вскрытии рудных жил (работает с модовыми рудами).",
                "+%.1f%% шанс удвоения руды",
                SkillCategory.MINING,
                90, -185, 15, 1, 1, 1.5, false, "minecraft:emerald"
        ).addPrerequisite("mining_speed_root", 5));

        register(new SkillNode(
                "reach_extension",
                "Дотягивание Космонавта",
                "Увеличивает дистанцию, с которой вы можете копать и ставить блоки (Reach Distance).",
                "+%.2f блока к дальности рук",
                SkillCategory.MINING,
                0, -205, 15, 1, 1, 0.15, false, "minecraft:piston"
        ).addPrerequisite("mining_speed_root", 8));

        register(new SkillNode(
                "stone_warden",
                "Каменный Щит Глубин",
                "Пыль и камень укрепляют вашу кожу, снижая получаемый физический урон.",
                "-%.1f%% получаемого физ. урона",
                SkillCategory.MINING,
                -165, -285, 15, 1, 1, 0.8, false, "minecraft:shield"
        ).addPrerequisite("haste_pulse", 5));

        register(new SkillNode(
                "deep_miner",
                "Мастер Глубинных Пластов",
                "Ускоряет копание на глубине ниже Y=45 и увеличивает общую скорость добычи.",
                "+%.1f%% к скорости копания на глубине",
                SkillCategory.MINING,
                -55, -295, 20, 1, 1, 2.5, false, "minecraft:diamond_pickaxe"
        ).addPrerequisite("haste_pulse", 8));

        register(new SkillNode(
                "smelting_mastery",
                "Плазменная Переплавка",
                "Даёт дополнительный опыт за вскрытие любых руд и усиливает добычу в глубинах.",
                "+%.1f%% бонусного опыта за руды",
                SkillCategory.MINING,
                55, -295, 15, 1, 1, 3.0, false, "minecraft:blaze_powder"
        ).addPrerequisite("ore_fortune", 6));

        register(new SkillNode(
                "gem_prospector",
                "Геолог Звёздных Кристаллов",
                "Повышает шанс найти редкие кристаллы, алмазы и изумруды при добыче камня и руд.",
                "+%.2f%% шанс редкого кристалла",
                SkillCategory.MINING,
                165, -285, 20, 1, 1, 0.4, false, "minecraft:diamond"
        ).addPrerequisite("ore_fortune", 8));

        register(new SkillNode(
                "void_magnet",
                "Гравитационный Магнит",
                "Притягивает выпадающие предметы и сферы опыта прямо к вам в радиусе действия.",
                "+%.1f блока радиуса притяжения",
                SkillCategory.MINING,
                0, -345, 15, 2, 1, 0.45, false, "minecraft:lodestone"
        ).addPrerequisite("reach_extension", 6).addPrerequisite("deep_miner", 4).addPrerequisite("smelting_mastery", 4));

        register(new SkillNode(
                "unbreaking_spirit",
                "Нерушимый Инструмент",
                "Космическая энергия укрепляет ваши инструменты, давая шанс не потратить прочность.",
                "+%.1f%% шанс сохранить прочность",
                SkillCategory.MINING,
                -115, -415, 20, 2, 1, 1.5, false, "minecraft:anvil"
        ).addPrerequisite("stone_warden", 5).addPrerequisite("deep_miner", 6));

        register(new SkillNode(
                "vein_resonance",
                "Резонанс Рудной Жилы",
                "Увеличивает количество опыта за руды и ускоряет добычу обсидиана и плотных блоков.",
                "+%.1f%% к опыту за руду и бурению",
                SkillCategory.MINING,
                0, -445, 15, 2, 1, 3.0, false, "minecraft:crying_obsidian"
        ).addPrerequisite("void_magnet", 5).addPrerequisite("deep_miner", 10).addPrerequisite("smelting_mastery", 6));

        register(new SkillNode(
                "treasure_excavator",
                "Археолог Древних Миров",
                "Повышает шанс выкопать редкие сокровища и слитки из земли, песка и гравия.",
                "+%.2f%% шанс клада при раскопках",
                SkillCategory.MINING,
                115, -415, 15, 2, 1, 0.5, false, "minecraft:gold_ingot"
        ).addPrerequisite("smelting_mastery", 8).addPrerequisite("gem_prospector", 8));

        register(new SkillNode(
                "cave_night_vision",
                "Зрение Глубоких Пещер",
                "На глубине ниже Y=35 даёт ночное зрение и дополнительно ускоряет работу киркой.",
                "+%.1f%% к скорости в пещерах + Ночное зрение",
                SkillCategory.MINING,
                -195, -495, 10, 2, 1, 2.5, false, "minecraft:golden_carrot"
        ).addPrerequisite("unbreaking_spirit", 8));

        register(new SkillNode(
                "tectonic_overlord",
                "★ Владыка Тектоники (Кейстоун)",
                "Ультимативный навык недр: колоссальная скорость бурения и шанс редчайших кристаллов.",
                "+%.1f%% к скорости копания и удаче руд",
                SkillCategory.MINING,
                0, -565, 10, 3, 2, 5.0, true, "minecraft:netherite_pickaxe"
        ).addPrerequisite("unbreaking_spirit", 10)
         .addPrerequisite("vein_resonance", 10)
         .addPrerequisite("treasure_excavator", 10));

        // =========================================================================
        // 3. СЕВЕРО-ЗАПАДНАЯ ВЕТВЬ — ФЕРМЕРСТВО, ПРИРОДА И УРОЖАЙ (X < 0, Y < 0)
        //    Идеально под Farmer's Delight, Mystical Agriculture, Project MMO Farming!
        // =========================================================================
        register(new SkillNode(
                "momentum_miner",
                "Инерционный Проходчик (Гибрид)",
                "Слияние Шахтёра и Странника: ускоряет ваш бег и скорость разрушения блоков.",
                "+%.1f%% к скорости копания и бега",
                SkillCategory.FARMING,
                -225, -75, 15, 1, 1, 1.0, false, "minecraft:sugar"
        ).addPrerequisite("haste_pulse", 5).addPrerequisite("move_speed_root", 5));

        register(new SkillNode(
                "modded_farming_xp",
                "Звёздный Агроном (Опыт Фермерства)",
                "Увеличивает опыт Фермерства (Project MMO) за сбор любых ванильных и модовых растений!",
                "+%.1f%% к опыту фермерства PMMO",
                SkillCategory.FARMING,
                -335, -135, 20, 1, 1, 4.0, false, "minecraft:iron_hoe"
        ).addPrerequisite("momentum_miner", 4));

        register(new SkillNode(
                "lumberjack_fury",
                "Дровосек Туманности",
                "Ускоряет рубку любых деревьев топором и даёт шанс получить дополнительное бревно.",
                "+%.1f%% к скорости рубки дерева",
                SkillCategory.FARMING,
                -295, -245, 20, 1, 1, 2.5, false, "minecraft:diamond_axe"
        ).addPrerequisite("modded_farming_xp", 5).addPrerequisite("stone_warden", 4));

        register(new SkillNode(
                "harvest_bounty",
                "Щедрый Урожай Галактики",
                "Даёт шанс получить дополнительный урожай при сборе культур (включая Farmer's Delight и Mystical Agriculture).",
                "+%.1f%% шанс двойного урожая",
                SkillCategory.FARMING,
                -455, -125, 20, 2, 1, 2.0, false, "minecraft:wheat"
        ).addPrerequisite("modded_farming_xp", 6));

        register(new SkillNode(
                "gourmet_saturation",
                "Энергия Гурмана",
                "Повышает питательность любой еды: даёт шанс восстановить доп. голод и насыщение.",
                "+%.1f%% шанс доп. сытости от еды",
                SkillCategory.FARMING,
                -445, -245, 15, 2, 1, 3.0, false, "minecraft:cooked_beef"
        ).addPrerequisite("harvest_bounty", 6).addPrerequisite("lumberjack_fury", 5));

        register(new SkillNode(
                "Astral_strider_hybrid",
                "Звёздный Навигатор Стихий",
                "Снижает входящий урон от огня, лавы и магии в опасных измерениях и данжах.",
                "-%.1f%% урона от огня, лавы и магии",
                SkillCategory.FARMING,
                -350, -355, 15, 2, 1, 2.0, false, "minecraft:magma_cream"
        ).addPrerequisite("lumberjack_fury", 8).addPrerequisite("gourmet_saturation", 5));

        register(new SkillNode(
                "world_tree_guardian",
                "★ Хранитель Мирового Древа (Кейстоун)",
                "Ультимативный навык природы: огромный бонус к урожаю, опыту фермерства, рубке и восстановлению ХП.",
                "+%.1f%% к урожаю и опыту фермерства",
                SkillCategory.FARMING,
                -545, -340, 10, 3, 2, 5.0, true, "minecraft:enchanted_golden_apple"
        ).addPrerequisite("harvest_bounty", 10)
         .addPrerequisite("gourmet_saturation", 8)
         .addPrerequisite("Astral_strider_hybrid", 8));

        // =========================================================================
        // 4. СЕВЕРО-ВОСТОЧНАЯ ВЕТВЬ — МАГИЯ, ЧАРЫ И СТИХИИ (X > 0, Y < 0)
        //    Идеально под Ars Nouveau, Astral Sorcery, Apotheosis, Cold Sweat!
        // =========================================================================
        register(new SkillNode(
                "heavy_crusher",
                "Тектонический Удар (Гибрид)",
                "Слияние Шахтёра и Воина: добавляет процентный урон ко всем атакам.",
                "+%.2f%% к общему урону",
                SkillCategory.ARCANE,
                225, -75, 20, 1, 1, 0.6, false, "minecraft:iron_block"
        ).addPrerequisite("ore_fortune", 5).addPrerequisite("damage_root", 5));

        register(new SkillNode(
                "enchanting_wisdom",
                "Мудрость Зачарователя",
                "Увеличивает количество получаемого ванильного опыта из всех источников (мобы, руды, рыбалка).",
                "+%.1f%% ко всему получаемому опыту",
                SkillCategory.ARCANE,
                335, -135, 20, 1, 1, 2.5, false, "minecraft:enchanting_table"
        ).addPrerequisite("heavy_crusher", 5));

        register(new SkillNode(
                "arcane_ward",
                "Астральный Барьер",
                "Снижает получаемый урон от магии, зелий ведьм, шипов и магических боссов.",
                "-%.1f%% входящего магического урона",
                SkillCategory.ARCANE,
                295, -245, 20, 1, 1, 1.5, false, "minecraft:amethyst_shard".replace("amethyst_shard", "ender_eye")
        ).addPrerequisite("enchanting_wisdom", 5).addPrerequisite("gem_prospector", 4));

        register(new SkillNode(
                "thermal_suit",
                "Скафандр Первопроходца",
                "Защищает от экстремальных условий: снижает урон от огня, лавы, удушья и молний.",
                "-%.1f%% урона от стихий и огня",
                SkillCategory.ARCANE,
                455, -125, 15, 2, 1, 2.5, false, "minecraft:blaze_powder"
        ).addPrerequisite("enchanting_wisdom", 6));

        register(new SkillNode(
                "cosmic_shield_absorb",
                "Энергощит Сверхновой",
                "В мирном состоянии или в бою периодически восстанавливает щит поглощения (жёлтые сердца).",
                "+%.1f ед. энергощита поглощения",
                SkillCategory.ARCANE,
                445, -245, 15, 2, 1, 0.5, false, "minecraft:nether_star"
        ).addPrerequisite("thermal_suit", 6).addPrerequisite("arcane_ward", 6));

        register(new SkillNode(
                "meteor_forged_blade",
                "Метеоритная Закалка Оружия",
                "Ваши атаки поджигают врагов звёздной плазмой и усиливают критический урон.",
                "+%.1f%% к крит. урону и поджог врага",
                SkillCategory.ARCANE,
                350, -355, 15, 2, 1, 2.0, false, "minecraft:fire_charge"
        ).addPrerequisite("arcane_ward", 8).addPrerequisite("cosmic_shield_absorb", 5));

        register(new SkillNode(
                "cosmic_archmage",
                "★ Архимаг Созвездий (Кейстоун)",
                "Вершина астральной магии: мощная защита от магии, большой энергощит и усиление всего опыта.",
                "+%.1f%% к опыту, критам и защите от магии",
                SkillCategory.ARCANE,
                545, -340, 10, 3, 2, 3.0, true, "minecraft:dragon_breath"
        ).addPrerequisite("thermal_suit", 8)
         .addPrerequisite("cosmic_shield_absorb", 8)
         .addPrerequisite("meteor_forged_blade", 8));

        // =========================================================================
        // 5. ЮГО-ЗАПАДНАЯ ВЕТВЬ — СКОРОСТЬ, ЛОВКОСТЬ И ТИТАНИЧЕСКАЯ ЖИВУЧЕСТЬ
        // =========================================================================
        register(new SkillNode(
                "wind_step",
                "Поступь Солнечного Ветра",
                "Дополнительно ускоряет шаг и спринт по любым биомам.",
                "+%.2f%% к скорости передвижения",
                SkillCategory.AGILITY,
                -195, 75, 20, 1, 1, 0.8, false, "minecraft:feather"
        ).addPrerequisite("move_speed_root", 5));

        register(new SkillNode(
                "vitality_star",
                "Звёздная Живучесть",
                "Увеличивает максимальный запас здоровья персонажа для выживания в данжах.",
                "+%.1f ед. максимального здоровья",
                SkillCategory.AGILITY,
                -145, 175, 20, 1, 1, 1.0, false, "minecraft:golden_apple"
        ).addPrerequisite("move_speed_root", 5));

        register(new SkillNode(
                "feather_gravity",
                "Гравитационная Невесомость",
                "Снижает урон от падения с большой высоты.",
                "-%.1f%% урона от падения",
                SkillCategory.AGILITY,
                -315, 55, 15, 1, 1, 3.0, false, "minecraft:phantom_membrane"
        ).addPrerequisite("wind_step", 6));

        register(new SkillNode(
                "evasion_matrix",
                "Матрица Уклонения",
                "Даёт шанс полностью уклониться от вражеского удара или снаряда в бою.",
                "+%.2f%% шанс полного уклонения",
                SkillCategory.AGILITY,
                -290, 165, 20, 2, 1, 0.75, false, "minecraft:ender_pearl"
        ).addPrerequisite("wind_step", 8).addPrerequisite("vitality_star", 5));

        register(new SkillNode(
                "cosmic_regen",
                "Метаболизм Туманности",
                "Ускоряет естественную регенерацию здоровья каждые 2 секунды.",
                "+%.1f%% к скорости регенерации ХП",
                SkillCategory.AGILITY,
                -195, 280, 15, 2, 1, 2.0, false, "minecraft:glistering_melon_slice"
        ).addPrerequisite("vitality_star", 8));

        register(new SkillNode(
                "swim_jet",
                "Гидродинамика Кометы",
                "Повышает скорость плавания под водой для быстрого исследования океанов.",
                "+%.1f%% к скорости в воде",
                SkillCategory.AGILITY,
                -430, 95, 15, 1, 1, 2.5, false, "minecraft:heart_of_the_sea"
        ).addPrerequisite("feather_gravity", 6).addPrerequisite("evasion_matrix", 5));

        register(new SkillNode(
                "gravity_anchor",
                "Гравитационный Якорь",
                "Повышает сопротивление отбрасыванию — боссы и взрывы меньше сбивают вас с ног.",
                "+%.1f%% сопротивления отбрасыванию",
                SkillCategory.AGILITY,
                -415, 210, 20, 2, 1, 3.0, false, "minecraft:obsidian"
        ).addPrerequisite("evasion_matrix", 6));

        register(new SkillNode(
                "iron_skin_cosmos",
                "Панцирь Сверхновой",
                "Даёт процентную прибавку к общей броне персонажа.",
                "+%.1f%% к эффективности брони",
                SkillCategory.AGILITY,
                -335, 310, 20, 2, 1, 1.25, false, "minecraft:diamond_chestplate"
        ).addPrerequisite("evasion_matrix", 8).addPrerequisite("cosmic_regen", 6));

        register(new SkillNode(
                "supernova_thorns",
                "Шипы Сверхновой",
                "Отражает процент от полученного вами урона обратно атакующему монстру!",
                "Отражает %.1f%% полученного урона",
                SkillCategory.AGILITY,
                -240, 400, 20, 2, 1, 2.0, false, "minecraft:cactus"
        ).addPrerequisite("cosmic_regen", 8).addPrerequisite("iron_skin_cosmos", 5));

        register(new SkillNode(
                "second_wind",
                "Второе Дыхание Пустоты",
                "Когда здоровье падает ниже 35%, вы получаете мощный прилив защиты.",
                "+%.1f%% защиты при низком ХП",
                SkillCategory.AGILITY,
                -365, 430, 10, 2, 1, 3.0, false, "minecraft:totem_of_undying"
        ).addPrerequisite("iron_skin_cosmos", 8).addPrerequisite("supernova_thorns", 6));

        register(new SkillNode(
                "battle_momentum",
                "Импульс Звёздного Охотника",
                "Убийство монстра даёт кратковременное ускорение бега (Скорость II) для зачистки данжей.",
                "+%.1f сек. ускорения после убийства",
                SkillCategory.AGILITY,
                -530, 195, 15, 2, 1, 0.4, false, "minecraft:rabbit_hide"
        ).addPrerequisite("swim_jet", 6).addPrerequisite("gravity_anchor", 6));

        register(new SkillNode(
                "warp_phantom",
                "★ Квантовый Фантом (Кейстоун)",
                "Сверхзвуковой бег, высокий шанс уклонения от атак и мощное снижение урона от падения.",
                "+%.1f%% к скорости бега и уклонению",
                SkillCategory.AGILITY,
                -520, 365, 10, 3, 2, 2.0, true, "minecraft:elytra"
        ).addPrerequisite("battle_momentum", 6)
         .addPrerequisite("iron_skin_cosmos", 10)
         .addPrerequisite("second_wind", 6));

        // =========================================================================
        // 6. ЮГО-ВОСТОЧНАЯ ВЕТВЬ — ВОИН, КРИТЫ, БЕРСЕРК И СОКРУШЕНИЕ БОССОВ
        // =========================================================================
        register(new SkillNode(
                "crit_chance_node",
                "Око Снайпера (Шанс Крита)",
                "Даёт шанс нанести критический удар даже стоя на земле (без прыжка).",
                "+%.2f%% к шансу критического удара",
                SkillCategory.COMBAT,
                195, 75, 25, 1, 1, 0.8, false, "minecraft:flint"
        ).addPrerequisite("damage_root", 5));

        register(new SkillNode(
                "attack_speed_node",
                "Молниеносный Клинок",
                "Повышает скорость взмаха оружием ближнего боя.",
                "+%.1f%% к скорости атаки",
                SkillCategory.COMBAT,
                145, 175, 20, 1, 1, 1.25, false, "minecraft:golden_sword"
        ).addPrerequisite("damage_root", 5));

        register(new SkillNode(
                "crit_damage_node",
                "Сверхновая Ярость (Урон Крита)",
                "Увеличивает множитель критического урона при срабатывании крита.",
                "+%.1f%% к критическому урону",
                SkillCategory.COMBAT,
                315, 55, 25, 1, 1, 1.5, false, "minecraft:blaze_rod"
        ).addPrerequisite("crit_chance_node", 6));

        register(new SkillNode(
                "armor_penetration",
                "Пробитие Звёздной Брони",
                "Усиливает урон по тяжелобронированным мобам и чемпионам из модов.",
                "+%.1f%% урона по бронированным врагам",
                SkillCategory.COMBAT,
                290, 165, 20, 2, 1, 1.0, false, "minecraft:iron_axe"
        ).addPrerequisite("crit_chance_node", 8).addPrerequisite("attack_speed_node", 5));

        register(new SkillNode(
                "archery_velocity",
                "Баллистика Квазара",
                "Увеличивает урон луков, арбалетов, стрел и любого метательного оружия.",
                "+%.1f%% к урону дальнего боя",
                SkillCategory.COMBAT,
                195, 280, 25, 1, 1, 1.0, false, "minecraft:bow"
        ).addPrerequisite("attack_speed_node", 6));

        register(new SkillNode(
                "lifesteal_fang",
                "Вампиризм Чёрной Дыры",
                "Восстанавливает процент от нанесённого врагу урона в виде вашего здоровья.",
                "+%.2f%% вампиризма от урона",
                SkillCategory.COMBAT,
                430, 95, 15, 2, 1, 0.35, false, "minecraft:redstone"
        ).addPrerequisite("crit_damage_node", 8).addPrerequisite("armor_penetration", 6));

        register(new SkillNode(
                "undead_purger",
                "Инквизитор Нежити",
                "Дополнительный урон по всей нежити (зомби, скелеты, визеры, мобы Fish's Undead Rising).",
                "+%.1f%% урона по нежити",
                SkillCategory.COMBAT,
                415, 210, 20, 1, 1, 1.5, false, "minecraft:wither_skeleton_skull"
        ).addPrerequisite("armor_penetration", 6));

        register(new SkillNode(
                "boss_slayer",
                "Убийца Левиафанов и Боссов",
                "Дополнительный процентный урон по боссам и элитным мобам (с запасом ХП >= 50).",
                "+%.1f%% урона по боссам и элите",
                SkillCategory.COMBAT,
                335, 310, 20, 2, 1, 1.25, false, "minecraft:dragon_head"
        ).addPrerequisite("armor_penetration", 10).addPrerequisite("archery_velocity", 8));

        register(new SkillNode(
                "executioner_strike",
                "Приговор Палача",
                "Резко увеличивает урон по врагам, у которых осталось менее 35% здоровья.",
                "+%.1f%% урона по раненым врагам",
                SkillCategory.COMBAT,
                240, 400, 15, 2, 1, 2.0, false, "minecraft:diamond_sword"
        ).addPrerequisite("archery_velocity", 10));

        register(new SkillNode(
                "berserker_rage",
                "Ярость Берсерка",
                "Когда ваше здоровье падает ниже 50%, вы наносите значительно больше урона.",
                "+%.1f%% урона при ХП ниже 50%%",
                SkillCategory.COMBAT,
                365, 430, 20, 2, 1, 1.5, false, "minecraft:crimson_fungus"
        ).addPrerequisite("boss_slayer", 8).addPrerequisite("executioner_strike", 6));

        register(new SkillNode(
                "champion_breaker",
                "Сокрушитель Чемпионов",
                "Увеличивает урон по редким врагам и монстрам из других измерений (Незер, Энд, Бездна).",
                "+%.1f%% урона в опасных измерениях",
                SkillCategory.COMBAT,
                530, 195, 20, 2, 1, 1.5, false, "minecraft:chorus_fruit"
        ).addPrerequisite("lifesteal_fang", 6).addPrerequisite("undead_purger", 8));

        register(new SkillNode(
                "cosmic_warlord",
                "★ Разрушитель Галактик (Кейстоун)",
                "Абсолютное боевое превосходство: мощный бонус к урону, шансу крита и вампиризму.",
                "+%.1f%% ко всему урону и критам",
                SkillCategory.COMBAT,
                520, 365, 10, 3, 2, 2.5, true, "minecraft:netherite_sword"
        ).addPrerequisite("champion_breaker", 8)
         .addPrerequisite("boss_slayer", 10)
         .addPrerequisite("berserker_rage", 8));

        // =========================================================================
        // 7. ЮЖНАЯ ВЕТВЬ — ДРОП С МОБОВ, МУЛЬТИ-КРЮЧКИ НА РЫБАЛКЕ И СОКРОВИЩА
        // =========================================================================
        register(new SkillNode(
                "mob_loot_chance",
                "Ловец Трофеев (Дроп с мобов)",
                "Даёт шанс удвоить выпадающий лут при убийстве монстров (работает с модовыми мобами).",
                "+%.1f%% шанс доп. дропа с мобов",
                SkillCategory.FISHING_LOOT,
                0, 175, 25, 1, 1, 1.2, false, "minecraft:rabbit_foot"
        ).addPrerequisite("move_speed_root", 4).addPrerequisite("damage_root", 4));

        register(new SkillNode(
                "angler_reflex",
                "Мастер Заброса (Рыбалка)",
                "Повышает удачу рыбака и увеличивает количество опыта за каждый улов.",
                "+%.1f%% к удаче и опыту рыбалки",
                SkillCategory.FISHING_LOOT,
                -70, 275, 20, 1, 1, 2.5, false, "minecraft:fishing_rod"
        ).addPrerequisite("mob_loot_chance", 5).addPrerequisite("vitality_star", 4));

        register(new SkillNode(
                "hunter_xp_boost",
                "Поглотитель Эссенции Опыта",
                "Увеличивает количество сфер опыта, выпадающих с убитых монстров и боссов.",
                "+%.1f%% к опыту с мобов",
                SkillCategory.FISHING_LOOT,
                70, 275, 25, 1, 1, 2.0, false, "minecraft:experience_bottle"
        ).addPrerequisite("mob_loot_chance", 5).addPrerequisite("attack_speed_node", 4));

        register(new SkillNode(
                "multi_hook_fishing",
                "Многокрючковая Снасть",
                "Позволяет вытаскивать сразу несколько рыб или предметов за один улов (больше крючков на леске)!",
                "+%.1f%% шанс доп. улова на крючке",
                SkillCategory.FISHING_LOOT,
                -115, 390, 15, 2, 1, 5.0, false, "minecraft:tripwire_hook"
        ).addPrerequisite("angler_reflex", 8));

        register(new SkillNode(
                "abyssal_treasure_fisher",
                "Искатель Сокровищ Океана",
                "При рыбалке даёт шанс выловить редкие сокровища, раковины, слитки и алмазы.",
                "+%.2f%% шанс морского сокровища",
                SkillCategory.FISHING_LOOT,
                0, 395, 20, 2, 1, 0.75, false, "minecraft:nautilus_shell"
        ).addPrerequisite("angler_reflex", 6).addPrerequisite("hunter_xp_boost", 6));

        register(new SkillNode(
                "rare_mob_trophy",
                "Инкассатор Реликвий",
                "Убитые монстры с большей вероятностью оставляют редкие ресурсы, жемчуг и алмазы.",
                "+%.2f%% шанс редкого трофея с мобов",
                SkillCategory.FISHING_LOOT,
                115, 390, 20, 2, 1, 0.6, false, "minecraft:ghast_tear"
        ).addPrerequisite("hunter_xp_boost", 8));

        register(new SkillNode(
                "dungeon_luck",
                "Фортуна Расхитителя Данжей",
                "Постоянно повышает характеристику Удачи (Luck) — улучшает лут в сундуках данжей и на рыбалке.",
                "+%.2f к общей Удаче (Luck)",
                SkillCategory.FISHING_LOOT,
                -90, 505, 20, 2, 1, 0.2, false, "minecraft:chest"
        ).addPrerequisite("multi_hook_fishing", 6).addPrerequisite("abyssal_treasure_fisher", 6));

        register(new SkillNode(
                "deep_sea_angler",
                "Укротитель Океанских Глубин",
                "При рыбалке даёт шанс выловить ещё один редкий трофей и большой бонус к опыту PMMO.",
                "+%.1f%% к опыту рыбалки и сокровищам",
                SkillCategory.FISHING_LOOT,
                90, 505, 20, 2, 1, 3.0, false, "minecraft:prismarine_crystals"
        ).addPrerequisite("abyssal_treasure_fisher", 6).addPrerequisite("rare_mob_trophy", 6));

        register(new SkillNode(
                "poseidon_bounty",
                "★ Тройной Гарпун Бездны (Кейстоун)",
                "До 4 крючков на удочке одновременно! Колоссальный шанс мульти-улова и двойного лута с мобов.",
                "+%.1f%% к мульти-крючкам и луту с мобов",
                SkillCategory.FISHING_LOOT,
                0, 615, 10, 3, 2, 4.0, true, "minecraft:trident"
        ).addPrerequisite("dungeon_luck", 8)
         .addPrerequisite("deep_sea_angler", 8)
         .addPrerequisite("abyssal_treasure_fisher", 10));

        // =========================================================================
        // 8. ФИНАЛЬНЫЕ КОСМИЧЕСКИЕ СИНГУЛЯРНОСТИ И АБСОЛЮТ КОСМОСА (ЭНДГЕЙМ)
        // =========================================================================
        register(new SkillNode(
                "nebula_synthesis_left",
                "✦ Сингулярность Стража Природы",
                "Слияние Кейстоунов Шахтёра, Хранителя Древа и Фантома. Мощный прирост ХП, бега, копания и урожая.",
                "+%.1f%% копания, бега и +%.0f ХП",
                SkillCategory.CELESTIAL,
                -290, -565, 10, 4, 2, 2.0, true, "minecraft:beacon"
        ).addPrerequisite("tectonic_overlord", 5)
         .addPrerequisite("world_tree_guardian", 5)
         .addPrerequisite("warp_phantom", 5));

        register(new SkillNode(
                "nebula_synthesis_right",
                "✦ Сингулярность Звёздного Разрушителя",
                "Слияние Кейстоунов Шахтёра, Архимага и Воина. Сокрушительный урон, криты и удача руд.",
                "+%.1f%% к общему урону и критам",
                SkillCategory.CELESTIAL,
                290, -565, 10, 4, 2, 1.5, true, "minecraft:end_crystal"
        ).addPrerequisite("tectonic_overlord", 5)
         .addPrerequisite("cosmic_archmage", 5)
         .addPrerequisite("cosmic_warlord", 5));

        register(new SkillNode(
                "abyssal_ascension",
                "✦ Владыка Приливов и Битв",
                "Слияние Кейстоунов Фантома, Воина и Гарпуна Бездны. Максимум дропа с боссов и мульти-крючков.",
                "+%.1f%% к дропу, рыбалке и урону",
                SkillCategory.CELESTIAL,
                0, 745, 10, 4, 2, 2.5, true, "minecraft:conduit"
        ).addPrerequisite("warp_phantom", 5)
         .addPrerequisite("cosmic_warlord", 5)
         .addPrerequisite("poseidon_bounty", 5));

        register(new SkillNode(
                "cosmic_transcendence",
                "★ АБСОЛЮТ КОСМОСА (Финал Дерева)",
                "Вершина эволюции в сборке из 181 мода! Требует все три Сингулярности. Усиливает абсолютно все статы героя.",
                "+%.1f%% КО ВСЕМ характеристикам героя",
                SkillCategory.CELESTIAL,
                0, -725, 25, 5, 3, 2.0, true, "minecraft:dragon_egg"
        ).addPrerequisite("nebula_synthesis_left", 5)
         .addPrerequisite("nebula_synthesis_right", 5)
         .addPrerequisite("abyssal_ascension", 5));
    }

    private static void register(SkillNode node) {
        NODES.put(node.getId(), node);
    }

    public static SkillNode getNode(String id) {
        return NODES.get(id);
    }

    public static Collection<SkillNode> getAllNodes() {
        return Collections.unmodifiableCollection(NODES.values());
    }
}
