package com.cosmic.betterlevelingaddon.compat;

import com.cosmic.betterlevelingaddon.BetterLevelingCosmicAddon;
import net.minecraft.item.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;

/**
 * Автоматически восстанавливает стандартные размеры стаков (1 / 16 / 64),
 * которые уменьшают моды вроде IguanaTweaksReborn (21, 32, 48 и т.д.):
 * - Оружие, броня, инструменты, зелья, книги и предметы со стаком 1 -> остаются 1.
 * - Жемчуг Края, яйца, снежки, таблички, баннеры, пустые вёдра, бутылочки мёда -> 16.
 * - Все остальные обычные блоки, еда, ресурсы и предметы -> 64.
 */
public class StackSizeFixer {

    private static Field maxStackSizeField = null;
    private static boolean fieldLookupFailed = false;

    public static void restoreStandardStackSizes() {
        if (fieldLookupFailed) return;
        try {
            if (maxStackSizeField == null) {
                try {
                    maxStackSizeField = ObfuscationReflectionHelper.findField(Item.class, "field_77777_bU");
                } catch (Exception e) {
                    maxStackSizeField = Item.class.getDeclaredField("maxStackSize");
                    maxStackSizeField.setAccessible(true);
                }
            }

            int restored64 = 0;
            int restored16 = 0;

            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item == null || item == Items.AIR) continue;

                int currentMax = item.getMaxStackSize();
                // Предметы со стаком 1 (оружие, броня, инструменты, зелья, рюкзаки, кровати и т.д.) не трогаем
                if (currentMax <= 1 || item.canBeDepleted()) {
                    continue;
                }

                // Нестакаемая экипировка/зелья, если вдруг они были > 1
                if (item instanceof TieredItem || item instanceof ArmorItem || item instanceof ShieldItem
                        || item instanceof ShootableItem || item instanceof FishingRodItem
                        || item instanceof PotionItem || item instanceof BedItem
                        || item instanceof BoatItem || item instanceof MinecartItem
                        || item instanceof SaddleItem || item instanceof HorseArmorItem
                        || item instanceof EnchantedBookItem || item instanceof WrittenBookItem
                        || item instanceof SoupItem || item instanceof MilkBucketItem) {
                    continue;
                }

                String regName = item.getRegistryName() != null ? item.getRegistryName().getPath().toLowerCase() : "";

                // Предметы, которые в Minecraft традиционно стакаются по 16
                boolean isStack16 = (item instanceof EnderPearlItem)
                        || (item instanceof EggItem)
                        || (item instanceof SnowballItem)
                        || (item instanceof SignItem)
                        || (item instanceof BannerItem)
                        || (item instanceof BucketItem && item == Items.BUCKET)
                        || (item instanceof HoneyBottleItem)
                        || (item instanceof ArmorStandItem)
                        || (item instanceof WritableBookItem)
                        || regName.equals("ender_pearl")
                        || regName.equals("snowball")
                        || regName.equals("egg")
                        || regName.endsWith("_sign")
                        || regName.endsWith("_banner")
                        || regName.equals("bucket")
                        || regName.equals("honey_bottle");

                if (isStack16) {
                    if (currentMax != 16) {
                        maxStackSizeField.setInt(item, 16);
                        restored16++;
                    }
                } else if (currentMax > 1 && currentMax < 64) {
                    // Восстанавливаем все порезанные до 21, 32, 48 и т.д. обычные предметы и блоки обратно до 64!
                    maxStackSizeField.setInt(item, 64);
                    restored64++;
                }
            }

            if (restored64 > 0 || restored16 > 0) {
                BetterLevelingCosmicAddon.LOGGER.info(
                        "[Cosmic Addon] Восстановлены стандартные стаки предметов (до 64: {}, до 16: {}).",
                        restored64, restored16);
            }
        } catch (Exception e) {
            fieldLookupFailed = true;
            BetterLevelingCosmicAddon.LOGGER.warn("[Cosmic Addon] Не удалось изменить maxStackSize: {}", e.getMessage());
        }
    }
}
