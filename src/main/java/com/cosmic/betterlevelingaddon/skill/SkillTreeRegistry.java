package com.cosmic.betterlevelingaddon.skill;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реестр огромного космического дерева навыков (48 узлов), рассчитанного на долгую игру в сборке из 180+ модов.
 * Первые 3 стартовых навыка доступны сразу:
 * 1) Скорость копания (mining_speed_root)
 * 2) Скорость бега (move_speed_root)
 * 3) Повышение урона (damage_root, +0.5% за уровень)
 * Все последующие навыки закрыты, пока не прокачаны предыдущие до нужного уровня.
 * Крутые навыки (Keystones / Слияния) требуют одновременной прокачки сразу нескольких веток!
 */
public class SkillTreeRegistry {
    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    public static void init() {
        NODES.clear();

        // =========================================================================
        // 1. ТРИ Изначально открытых стартовых навыка (вокруг центра космоса 0,0)
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
        // 2. СЕВЕРНАЯ ВЕТВЬ — ШАХТЁРСТВО, РЕМЕСЛО И БОГАТСТВО НЕДР (Y < 0)
        // =========================================================================
        register(new SkillNode(
                "haste_pulse",
                "Импульс Спешки",
                "Дополнительно разгоняет взмах кирки и лопаты при добыче твёрдых пород.",
                "+%.1f%% к скорости копания",
                SkillCategory.MINING,
                -85, -180, 15, 1, 1, 2.0, false, "minecraft:golden_pickaxe"
        ).addPrerequisite("mining_speed_root", 5));

        register(new SkillNode(
                "ore_fortune",
                "Искатель Самородков",
                "Шанс получить дополнительный дроп при вскрытии рудных жил (работает с модовыми рудами).",
                "+%.1f%% шанс удвоения руды",
                SkillCategory.MINING,
                85, -180, 15, 1, 1, 1.5, false, "minecraft:emerald"
        ).addPrerequisite("mining_speed_root", 5));

        register(new SkillNode(
                "stone_warden",
                "Каменный Щит Глубин",
                "Пыль и камень укрепляют вашу кожу, снижая получаемый физический урон.",
                "-%.1f%% получаемого физ. урона",
                SkillCategory.MINING,
                -150, -275, 15, 1, 1, 0.8, false, "minecraft:shield"
        ).addPrerequisite("haste_pulse", 5));

        register(new SkillNode(
                "deep_miner",
                "Мастер Глубинных Пластов",
                "Ускоряет копание на глубине ниже Y=40 и увеличивает общую скорость добычи.",
                "+%.1f%% к скорости копания на глубине",
                SkillCategory.MINING,
                -35, -285, 20, 1, 1, 2.5, false, "minecraft:diamond_pickaxe"
        ).addPrerequisite("haste_pulse", 8));

        register(new SkillNode(
                "smelting_mastery",
                "Плазменная Переплавка",
                "Шанс автоматически переплавить добытую руду и получить бонусный опыт за блоки.",
                "+%.1f%% шанс авто-переплавки и опыта",
                SkillCategory.MINING,
                45, -285, 15, 1, 1, 2.0, false, "minecraft:blaze_powder"
        ).addPrerequisite("ore_fortune", 6));

        register(new SkillNode(
                "gem_prospector",
                "Геолог Звёздных Кристаллов",
                "Повышает шанс найти редкие кристаллы, алмазы и изумруды при добыче камня и руд.",
                "+%.2f%% шанс редкого кристалла из породы",
                SkillCategory.MINING,
                155, -275, 20, 1, 1, 0.4, false, "minecraft:diamond"
        ).addPrerequisite("ore_fortune", 8));

        register(new SkillNode(
                "unbreaking_spirit",
                "Нерушимый Инструмент",
                "Космическая энергия укрепляет ваши инструменты и оружие, давая шанс не потратить прочность.",
                "+%.1f%% шанс сохранить прочность",
                SkillCategory.MINING,
                -105, -390, 20, 2, 1, 1.5, false, "minecraft:anvil"
        ).addPrerequisite("stone_warden", 5).addPrerequisite("deep_miner", 6));

        register(new SkillNode(
                "vein_resonance",
                "Резонанс Рудной Жилы",
                "Увеличивает количество опыта за вскрытие руд и ускоряет добычу обсидиана и плотных руд.",
                "+%.1f%% к опыту за руду и скорости бурения",
                SkillCategory.MINING,
                0, -405, 15, 2, 1, 3.0, false, "minecraft:crying_obsidian"
        ).addPrerequisite("deep_miner", 10).addPrerequisite("smelting_mastery", 6));

        register(new SkillNode(
                "treasure_excavator",
                "Археолог Древних Миров",
                "Повышает шанс выкопать редкие сокровища, слитки и артефакты из земли, песка и гравия.",
                "+%.2f%% шанс клада при раскопках",
                SkillCategory.MINING,
                110, -390, 15, 2, 1, 0.5, false, "minecraft:gold_ingot"
        ).addPrerequisite("smelting_mastery", 8).addPrerequisite("gem_prospector", 8));

        // КРУТОЙ НАВЫК (Keystone Шахтёра) — требует прокачки сразу 3 предыдущих веток шахтёра!
        register(new SkillNode(
                "tectonic_overlord",
                "★ Владыка Тектоники (Кейстоун)",
                "Ультимативный навык недр: колоссальный прирост скорости бурения, шанс тройной руды и иммунитет к усталости.",
                "+%.1f%% к скорости копания и +%.1f%% к удаче руд",
                SkillCategory.MINING,
                0, -530, 10, 3, 2, 5.0, true, "minecraft:netherite_pickaxe"
        ).addPrerequisite("unbreaking_spirit", 10)
         .addPrerequisite("vein_resonance", 10)
         .addPrerequisite("treasure_excavator", 10));

        // =========================================================================
        // 3. ЮГО-ЗАПАДНАЯ ВЕТВЬ — СКОРОСТЬ, ЛОВКОСТЬ И ВЫЖИВАНИЕ (X < 0, Y > 0)
        // =========================================================================
        register(new SkillNode(
                "wind_step",
                "Поступь Солнечного Ветра",
                "Дополнительно ускоряет шаг и спринт, снижая расход голода при беге.",
                "+%.2f%% к скорости передвижения",
                SkillCategory.AGILITY,
                -190, 80, 20, 1, 1, 0.8, false, "minecraft:feather"
        ).addPrerequisite("move_speed_root", 5));

        register(new SkillNode(
                "vitality_star",
                "Звёздная Живучесть",
                "Увеличивает максимальный запас здоровья персонажа для выживания в опасных модовых данжах.",
                "+%.1f ед. максимального здоровья",
                SkillCategory.AGILITY,
                -140, 175, 20, 1, 1, 1.0, false, "minecraft:golden_apple"
        ).addPrerequisite("move_speed_root", 5));

        register(new SkillNode(
                "feather_gravity",
                "Гравитационная Невесомость",
                "Снижает урон от падения с большой высоты и увеличивает высоту прыжка.",
                "-%.1f%% урона от падения",
                SkillCategory.AGILITY,
                -300, 60, 15, 1, 1, 3.0, false, "minecraft:phantom_membrane"
        ).addPrerequisite("wind_step", 6));

        register(new SkillNode(
                "evasion_matrix",
                "Матрица Уклонения",
                "Даёт шанс полностью уклониться от вражеского удара или снаряда в бою.",
                "+%.2f%% шанс полного уклонения",
                SkillCategory.AGILITY,
                -275, 165, 20, 2, 1, 0.75, false, "minecraft:ender_pearl"
        ).addPrerequisite("wind_step", 8).addPrerequisite("vitality_star", 5));

        register(new SkillNode(
                "cosmic_regen",
                "Метаболизм Туманности",
                "Ускоряет естественную регенерацию здоровья и усиливает эффект исцеления.",
                "+%.1f%% к скорости регенерации ХП",
                SkillCategory.AGILITY,
                -185, 270, 15, 2, 1, 2.0, false, "minecraft:glistering_melon_slice"
        ).addPrerequisite("vitality_star", 8));

        register(new SkillNode(
                "swim_jet",
                "Гидродинамика Кометы",
                "Повышает скорость плавания под водой и замедляет расход кислорода.",
                "+%.1f%% к скорости в воде и дыханию",
                SkillCategory.AGILITY,
                -400, 115, 15, 1, 1, 2.5, false, "minecraft:heart_of_the_sea"
        ).addPrerequisite("feather_gravity", 6).addPrerequisite("evasion_matrix", 5));

        register(new SkillNode(
                "iron_skin_cosmos",
                "Панцирь Сверхновой",
                "Даёт процентную прибавку к общей броне и твёрдости брони.",
                "+%.1f%% к эффективности брони",
                SkillCategory.AGILITY,
                -345, 260, 20, 2, 1, 1.25, false, "minecraft:diamond_chestplate"
        ).addPrerequisite("evasion_matrix", 8).addPrerequisite("cosmic_regen", 6));

        register(new SkillNode(
                "second_wind",
                "Второе Дыхание Пустоты",
                "Когда здоровье падает ниже 30%, вы получаете мощный прилив скорости и защиты.",
                "+%.1f%% защиты и скорости при низком ХП",
                SkillCategory.AGILITY,
                -250, 365, 10, 2, 1, 3.0, false, "minecraft:totem_of_undying"
        ).addPrerequisite("cosmic_regen", 10));

        // КРУТОЙ НАВЫК (Keystone Скорости и Выживания)
        register(new SkillNode(
                "warp_phantom",
                "★ Квантовый Фантом (Кейстоун)",
                "Сверхзвуковой бег, высокий шанс уклонения от смертельных атак и неуязвимость к замедлению.",
                "+%.1f%% к скорости бега и +%.1f%% к уклонению",
                SkillCategory.AGILITY,
                -450, 340, 10, 3, 2, 2.0, true, "minecraft:elytra"
        ).addPrerequisite("swim_jet", 8)
         .addPrerequisite("iron_skin_cosmos", 10)
         .addPrerequisite("second_wind", 6));

        // =========================================================================
        // 4. ЮГО-ВОСТОЧНАЯ ВЕТВЬ — БОЙ, КРИТЫ И СОКРУШЕНИЕ БОССОВ (X > 0, Y > 0)
        // =========================================================================
        register(new SkillNode(
                "crit_chance_node",
                "Око Снайпера (Шанс Крита)",
                "Даёт шанс нанести сокрушительный критический удар даже стоя на земле (без прыжка).",
                "+%.2f%% к шансу критического удара",
                SkillCategory.COMBAT,
                195, 75, 25, 1, 1, 0.8, false, "minecraft:flint"
        ).addPrerequisite("damage_root", 5));

        register(new SkillNode(
                "attack_speed_node",
                "Молниеносный Клинок",
                "Повышает скорость взмаха оружием ближнего боя, позволяя наносить удары чаще.",
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
                310, 60, 25, 1, 1, 1.5, false, "minecraft:blaze_rod"
        ).addPrerequisite("crit_chance_node", 6));

        register(new SkillNode(
                "armor_penetration",
                "Пробитие Звёздной Брони",
                "Часть вашего урона игнорирует броню тяжелобронированных мобов и боссов из модов.",
                "+%.1f%% пробития брони врага",
                SkillCategory.COMBAT,
                280, 165, 20, 2, 1, 1.0, false, "minecraft:iron_axe"
        ).addPrerequisite("crit_chance_node", 8).addPrerequisite("attack_speed_node", 5));

        register(new SkillNode(
                "archery_velocity",
                "Баллистика Квазара",
                "Увеличивает урон стрел, болтов и метательного оружия.",
                "+%.1f%% к урону дальнего боя",
                SkillCategory.COMBAT,
                190, 275, 25, 1, 1, 1.0, false, "minecraft:bow"
        ).addPrerequisite("attack_speed_node", 6));

        register(new SkillNode(
                "lifesteal_fang",
                "Вампиризм Чёрной Дыры",
                "Восстанавливает процент от нанесённого врагу урона в виде вашего здоровья.",
                "+%.2f%% вампиризма от урона",
                SkillCategory.COMBAT,
                415, 115, 15, 2, 1, 0.35, false, "minecraft:redstone"
        ).addPrerequisite("crit_damage_node", 8).addPrerequisite("armor_penetration", 6));

        register(new SkillNode(
                "boss_slayer",
                "Убийца Левиафанов и Боссов",
                "Дополнительный процентный урон по боссам и элитным мобам (с запасом ХП > 50).",
                "+%.1f%% урона по боссам и элите",
                SkillCategory.COMBAT,
                355, 260, 20, 2, 1, 1.25, false, "minecraft:nether_star"
        ).addPrerequisite("armor_penetration", 10).addPrerequisite("archery_velocity", 8));

        register(new SkillNode(
                "executioner_strike",
                "Приговор Палача",
                "Резко увеличивает наносимый урон по врагам, у которых осталось менее 35% здоровья.",
                "+%.1f%% урона по раненым врагам",
                SkillCategory.COMBAT,
                255, 365, 15, 2, 1, 2.0, false, "minecraft:diamond_axe"
        ).addPrerequisite("archery_velocity", 10));

        // КРУТОЙ НАВЫК (Keystone Боя)
        register(new SkillNode(
                "cosmic_warlord",
                "★ Разрушитель Галактик (Кейстоун)",
                "Абсолютное боевое превосходство: мощный бонус к урону, шансу крита и вампиризму.",
                "+%.1f%% ко всему урону и критам",
                SkillCategory.COMBAT,
                460, 340, 10, 3, 2, 2.5, true, "minecraft:netherite_sword"
        ).addPrerequisite("lifesteal_fang", 10)
         .addPrerequisite("boss_slayer", 10)
         .addPrerequisite("executioner_strike", 8));

        // =========================================================================
        // 5. ЮЖНАЯ И МЕЖВЕТОЧНАЯ ВЕТВЬ — ДРОП С МОБОВ, РЫБАЛКА (МУЛЬТИ-КРЮЧКИ) И ОХОТА
        //    (Требует прокачки соседних веток для открытия!)
        // =========================================================================
        register(new SkillNode(
                "mob_loot_chance",
                "Ловец Трофеев (Дроп с мобов)",
                "Даёт шанс удвоить выпадающий лут при убийстве монстров (совместимо с мобами из модов).",
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
                -65, 265, 20, 1, 1, 2.5, false, "minecraft:fishing_rod"
        ).addPrerequisite("mob_loot_chance", 5).addPrerequisite("vitality_star", 4));

        register(new SkillNode(
                "hunter_xp_boost",
                "Поглотитель Эссенции Опыта",
                "Увеличивает количество сфер опыта, выпадающих с убитых монстров и боссов.",
                "+%.1f%% к опыту с мобов",
                SkillCategory.FISHING_LOOT,
                65, 265, 25, 1, 1, 2.0, false, "minecraft:experience_bottle"
        ).addPrerequisite("mob_loot_chance", 5).addPrerequisite("attack_speed_node", 4));

        register(new SkillNode(
                "multi_hook_fishing",
                "Многокрючковая Снасть",
                "Позволяет вытаскивать сразу несколько рыб или предметов за один улов (больше крючков на леске)!",
                "+%.1f%% шанс вытянуть +1/+2/+3 улова за раз",
                SkillCategory.FISHING_LOOT,
                -85, 375, 15, 2, 1, 5.0, false, "minecraft:tripwire_hook"
        ).addPrerequisite("angler_reflex", 8));

        register(new SkillNode(
                "abyssal_treasure_fisher",
                "Искатель Сокровищ Океана",
                "При рыбалке даёт шанс выловить редкие сокровища, зачарованные книги, слитки и артефакты.",
                "+%.2f%% шанс морского сокровища на крючке",
                SkillCategory.FISHING_LOOT,
                0, 390, 20, 2, 1, 0.75, false, "minecraft:nautilus_shell"
        ).addPrerequisite("angler_reflex", 6).addPrerequisite("hunter_xp_boost", 6));

        register(new SkillNode(
                "rare_mob_trophy",
                "Инкассатор Реликвий",
                "Убитые монстры с большей вероятностью оставляют редкие ресурсы, жемчуг, черепа и самородки.",
                "+%.2f%% шанс редкого трофея с мобов",
                SkillCategory.FISHING_LOOT,
                85, 375, 20, 2, 1, 0.6, false, "minecraft:ghast_tear"
        ).addPrerequisite("hunter_xp_boost", 8));

        // КРУТОЙ НАВЫК (Keystone Рыбалки и Охоты) — требует все 3 узла охоты/рыбалки!
        register(new SkillNode(
                "poseidon_bounty",
                "★ Тройной Гарпун Бездны (Кейстоун)",
                "На вашей удочке до 4 крючков одновременно! Огромный шанс вытащить мульти-улов и двойной лут с мобов.",
                "+%.1f%% к мульти-крючкам и трофеям с мобов",
                SkillCategory.FISHING_LOOT,
                0, 515, 10, 3, 2, 4.0, true, "minecraft:trident"
        ).addPrerequisite("multi_hook_fishing", 10)
         .addPrerequisite("abyssal_treasure_fisher", 10)
         .addPrerequisite("rare_mob_trophy", 10));

        // =========================================================================
        // 6. ГИБРИДНЫЕ МЕЖВЕТОЧНЫЕ СЛИЯНИЯ (ЗАПАД И ВОСТОК)
        //    Требуют одновременной прокачки Шахтёрства + Скорости ИЛИ Шахтёрства + Боя
        // =========================================================================
        register(new SkillNode(
                "momentum_miner",
                "Инерционный Проходчик (Гибрид)",
                "Слияние Шахтёра и Странника: разрушение блоков временно ускоряет ваш бег и копание.",
                "+%.1f%% к скорости копания и бега",
                SkillCategory.CELESTIAL,
                -215, -75, 15, 2, 1, 1.0, false, "minecraft:sugar"
        ).addPrerequisite("haste_pulse", 6).addPrerequisite("wind_step", 6));

        register(new SkillNode(
                "Astral_strider_hybrid",
                "Звёздный Навигатор",
                "Увеличивает дальность взаимодействия и снижает входящий урон от магии и огня.",
                "-%.1f%% урона от огня, лавы и магии",
                SkillCategory.CELESTIAL,
                -335, -95, 15, 2, 1, 2.0, false, "minecraft:magma_cream"
        ).addPrerequisite("momentum_miner", 8).addPrerequisite("stone_warden", 6).addPrerequisite("feather_gravity", 6));

        register(new SkillNode(
                "heavy_crusher",
                "Тектонический Удар (Гибрид)",
                "Слияние Шахтёра и Воина: тяжёлые руки рудокопа добавляют процентный урон и отбрасывание.",
                "+%.2f%% к общему урону и силе удара",
                SkillCategory.CELESTIAL,
                215, -75, 20, 2, 1, 0.6, false, "minecraft:iron_block"
        ).addPrerequisite("ore_fortune", 6).addPrerequisite("crit_chance_node", 6));

        register(new SkillNode(
                "meteor_forged_blade",
                "Метеоритная Закалка Оружия",
                "Ваши атаки поджигают врагов звёздной плазмой и наносят дополнительный критический урон.",
                "+%.1f%% к крит. урону и поджог врага",
                SkillCategory.CELESTIAL,
                335, -95, 15, 2, 1, 2.0, false, "minecraft:fire_charge"
        ).addPrerequisite("heavy_crusher", 8).addPrerequisite("gem_prospector", 6).addPrerequisite("crit_damage_node", 6));

        // =========================================================================
        // 7. ФИНАЛЬНЫЕ КОСМИЧЕСКИЕ СОЗВЕЗДИЯ (ЭНДГЕЙМ ДЛЯ СБОРКИ НА 181 МОД)
        //    Открываются только при прокачке Кейстоунов разных веток!
        // =========================================================================
        register(new SkillNode(
                "nebula_synthesis_left",
                "✦ Сингулярность Стража (Слияние)",
                "Требует Кейстоуны Шахтёра и Фантома. Даёт мощный бонус к выживаемости, ХП и скорости добычи.",
                "+%.1f%% копания, бега и +%.0f ХП",
                SkillCategory.CELESTIAL,
                -240, -470, 10, 4, 2, 2.0, true, "minecraft:beacon"
        ).addPrerequisite("tectonic_overlord", 5)
         .addPrerequisite("warp_phantom", 5)
         .addPrerequisite("Astral_strider_hybrid", 10));

        register(new SkillNode(
                "nebula_synthesis_right",
                "✦ Сингулярность Разрушителя (Слияние)",
                "Требует Кейстоуны Шахтёра и Воина. Сокрушительный бонус к урону, критам и добыче редких руд.",
                "+%.1f%% к общему урону и удаче руд",
                SkillCategory.CELESTIAL,
                240, -470, 10, 4, 2, 1.5, true, "minecraft:end_crystal"
        ).addPrerequisite("tectonic_overlord", 5)
         .addPrerequisite("cosmic_warlord", 5)
         .addPrerequisite("meteor_forged_blade", 10));

        register(new SkillNode(
                "abyssal_ascension",
                "✦ Владыка Приливов и Битв (Слияние)",
                "Требует Кейстоуны Фантома, Воина и Гарпуна Бездны. Максимальный шанс мульти-улова и дропа с боссов.",
                "+%.1f%% к дропу с мобов, рыбалке и урону",
                SkillCategory.CELESTIAL,
                0, 650, 10, 4, 2, 2.5, true, "minecraft:conduit"
        ).addPrerequisite("warp_phantom", 5)
         .addPrerequisite("cosmic_warlord", 5)
         .addPrerequisite("poseidon_bounty", 5));

        register(new SkillNode(
                "cosmic_transcendence",
                "★ АБСОЛЮТ КОСМОСА (Финал Дерева)",
                "Вершина эволюции в сборке! Требует все три Сингулярности. Усиливает абсолютно все характеристики героя.",
                "+%.1f%% КО ВСЕМ характеристикам героя",
                SkillCategory.CELESTIAL,
                0, -680, 20, 5, 3, 2.0, true, "minecraft:dragon_egg"
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
