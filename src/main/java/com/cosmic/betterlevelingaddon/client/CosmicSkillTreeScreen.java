package com.cosmic.betterlevelingaddon.client;

import com.cosmic.betterlevelingaddon.capability.ICosmicSkillData;
import com.cosmic.betterlevelingaddon.network.PacketHandler;
import com.cosmic.betterlevelingaddon.network.UpgradeSkillPacket;
import com.cosmic.betterlevelingaddon.skill.SkillNode;
import com.cosmic.betterlevelingaddon.skill.SkillTreeRegistry;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Полноэкранное голубое космическое дерево навыков (открывается по F7).
 * Совместимо с Minecraft Forge 1.16.5 (36.2.39) и LWJGL 3.2.2.
 * Поддерживает панорамирование мышью, масштабирование колесиком, анимацию параллакс-космоса,
 * линии созвездий, иконки предметов и подробные тултипы с процентными бонусами.
 */
public class CosmicSkillTreeScreen extends Screen {

    private double panX = 0.0;
    private double panY = 0.0;
    private float zoom = 1.0F;
    private boolean isDragging = false;

    private static final int NODE_RADIUS = 16;
    private static final int KEYSTONE_RADIUS = 22;

    private final List<CosmicStar> stars = new ArrayList<>();
    private final List<NebulaCloud> nebulae = new ArrayList<>();
    private final Map<String, ItemStack> iconCache = new HashMap<>();

    private static class CosmicStar {
        final float x, y, size, speed, phase;
        final int color;

        CosmicStar(float x, float y, float size, float speed, float phase, int color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.phase = phase;
            this.color = color;
        }
    }

    private static class NebulaCloud {
        final float x, y, width, height;
        final int topColor, bottomColor;

        NebulaCloud(float x, float y, float width, float height, int topColor, int bottomColor) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.topColor = topColor;
            this.bottomColor = bottomColor;
        }
    }

    public CosmicSkillTreeScreen() {
        super(new TranslationTextComponent("gui.blcosmicaddon.title"));
        generateCosmicField();
    }

    private void generateCosmicField() {
        Random rng = new Random(16536239L);
        stars.clear();
        int[] starPalette = new int[]{
                0xFFE0F7FA, // Светло-голубой
                0xFF80D8FF, // Небесно-голубой
                0xFF40C4FF, // Ярко-лазурный
                0xFFFFFFFF, // Чисто белый
                0xFFB388FF, // Туманно-фиолетовый
                0xFFFFF59D  // Далёкий золотистый гигант
        };

        for (int i = 0; i < 320; i++) {
            float sx = (rng.nextFloat() - 0.5F) * 2600.0F;
            float sy = (rng.nextFloat() - 0.5F) * 2200.0F;
            float size = 1.0F + rng.nextFloat() * 2.2F;
            float speed = 0.03F + rng.nextFloat() * 0.08F;
            float phase = rng.nextFloat() * 6.28F;
            int col = starPalette[rng.nextInt(starPalette.length)];
            stars.add(new CosmicStar(sx, sy, size, speed, phase, col));
        }

        nebulae.clear();
        int[] nebulaColorsTop = new int[]{0x2800B0FF, 0x2400E5FF, 0x223D5AFE, 0x2018FFFF};
        int[] nebulaColorsBot = new int[]{0x05002244, 0x05004466, 0x05051535, 0x05003355};
        for (int i = 0; i < 18; i++) {
            float nx = (rng.nextFloat() - 0.5F) * 1800.0F;
            float ny = (rng.nextFloat() - 0.5F) * 1600.0F;
            float nw = 180.0F + rng.nextFloat() * 260.0F;
            float nh = 120.0F + rng.nextFloat() * 200.0F;
            int idx = rng.nextInt(nebulaColorsTop.length);
            nebulae.add(new NebulaCloud(nx, ny, nw, nh, nebulaColorsTop[idx], nebulaColorsBot[idx]));
        }
    }

    private ItemStack getIconStack(SkillNode node) {
        return iconCache.computeIfAbsent(node.getId(), k -> {
            try {
                ResourceLocation rl = new ResourceLocation(node.getIconItemRegistryName());
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null && item != Items.AIR) {
                    return new ItemStack(item);
                }
            } catch (Exception ignored) {
            }
            return new ItemStack(Items.NETHER_STAR);
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // 1. Отрисовка глубокого голубого космического фона
        renderBlueCosmicBackground(matrixStack, partialTicks);

        ICosmicSkillData skillData = ClientSkillCache.get();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 2. Трансформация холста дерева навыков (панорамирование + зум)
        RenderSystem.pushMatrix();
        RenderSystem.translatef(centerX + (float) panX, centerY + (float) panY, 0.0F);
        RenderSystem.scalef(zoom, zoom, 1.0F);

        MatrixStack canvasStack = new MatrixStack();

        // Отрисовка центрального ядра космоса в (0, 0)
        drawCentralCoreOrb(canvasStack);

        // 3. Отрисовка соединительных линий созвездий между родительскими и дочерними навыками
        renderConstellationConnections(canvasStack, skillData);

        // 4. Отрисовка всех узлов дерева навыков
        SkillNode hoveredNode = null;
        double worldMouseX = (mouseX - centerX - panX) / zoom;
        double worldMouseY = (mouseY - centerY - panY) / zoom;

        for (SkillNode node : SkillTreeRegistry.getAllNodes()) {
            int radius = node.isKeystone() ? KEYSTONE_RADIUS : NODE_RADIUS;
            double dx = worldMouseX - node.getX();
            double dy = worldMouseY - node.getY();
            boolean isHovered = (dx * dx + dy * dy) <= (radius + 3) * (radius + 3);
            if (isHovered) {
                hoveredNode = node;
            }
            renderSkillNode(canvasStack, node, skillData, isHovered);
        }

        RenderSystem.popMatrix();

        // 5. Отрисовка верхней и нижней информационной панели (HUD)
        renderCosmicHudOverlay(matrixStack, skillData);

        // 6. Отрисовка подробного всплывающего окна (Tooltip) при наведении на навык
        if (hoveredNode != null) {
            renderSkillNodeTooltip(matrixStack, hoveredNode, skillData, mouseX, mouseY);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void renderBlueCosmicBackground(MatrixStack matrixStack, float partialTicks) {
        // Глубокий космический сине-голубой градиент
        this.fillGradient(matrixStack, 0, 0, this.width, this.height, 0xFF030B1E, 0xFF0A274E);
        // Дополнительное голубое свечение в нижней половине экрана
        this.fillGradient(matrixStack, 0, this.height / 3, this.width, this.height, 0x00004488, 0x550088CC);

        float time = (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0) + partialTicks;
        float parallaxX = (float) (this.width / 2.0 + panX * 0.25);
        float parallaxY = (float) (this.height / 2.0 + panY * 0.25);

        // Туманности (Nebula Clouds)
        for (NebulaCloud cloud : nebulae) {
            int x1 = (int) (parallaxX + cloud.x - cloud.width / 2);
            int y1 = (int) (parallaxY + cloud.y - cloud.height / 2);
            int x2 = (int) (x1 + cloud.width);
            int y2 = (int) (y1 + cloud.height);
            if (x2 >= 0 && x1 <= this.width && y2 >= 0 && y1 <= this.height) {
                this.fillGradient(matrixStack, x1, y1, x2, y2, cloud.topColor, cloud.bottomColor);
            }
        }

        // Мерцающие звёзды
        float starParallaxX = (float) (this.width / 2.0 + panX * 0.45);
        float starParallaxY = (float) (this.height / 2.0 + panY * 0.45);
        for (CosmicStar star : stars) {
            int sx = (int) (starParallaxX + star.x);
            int sy = (int) (starParallaxY + star.y);
            if (sx < -5 || sx > this.width + 5 || sy < -5 || sy > this.height + 5) continue;

            float twinkle = 0.55F + 0.45F * MathHelper.sin(time * star.speed + star.phase);
            int alpha = MathHelper.clamp((int) (twinkle * 255.0F), 40, 255);
            int rgb = star.color & 0x00FFFFFF;
            int colorWithAlpha = (alpha << 24) | rgb;

            int s = Math.max(1, Math.round(star.size));
            fill(matrixStack, sx, sy, sx + s, sy + s, colorWithAlpha);
        }
    }

    private void drawCentralCoreOrb(MatrixStack stack) {
        // Светящееся ядро в центре (0,0), от которого расходятся первые 3 навыка
        fill(stack, -26, -26, 26, 26, 0x3300E5FF);
        fill(stack, -18, -18, 18, 18, 0x8800B0FF);
        fill(stack, -11, -11, 11, 11, 0xFF00E5FF);
        fill(stack, -6, -6, 6, 6, 0xFFFFFFFF);

        // Линии от центрального ядра к 3 стартовым навыкам
        String[] roots = new String[]{"mining_speed_root", "move_speed_root", "damage_root"};
        for (String rootId : roots) {
            SkillNode root = SkillTreeRegistry.getNode(rootId);
            if (root != null) {
                drawSmoothLine(stack, 0, 0, root.getX(), root.getY(), 2.5F, 0xAA00E5FF);
            }
        }
    }

    private void renderConstellationConnections(MatrixStack stack, ICosmicSkillData data) {
        for (SkillNode node : SkillTreeRegistry.getAllNodes()) {
            int nodeLvl = data.getNodeLevel(node.getId());
            boolean nodeUnlocked = data.arePrerequisitesMet(node);

            for (Map.Entry<String, Integer> prereq : node.getPrerequisites().entrySet()) {
                SkillNode parent = SkillTreeRegistry.getNode(prereq.getKey());
                if (parent == null) continue;

                int parentLvl = data.getNodeLevel(parent.getId());
                int reqLvl = prereq.getValue();
                boolean conditionMet = parentLvl >= reqLvl;

                int lineColor;
                float thickness;
                if (nodeLvl > 0 && conditionMet) {
                    lineColor = 0xFF00E5FF;
                    thickness = 3.2F;
                } else if (conditionMet && nodeUnlocked) {
                    lineColor = 0xCC64FFDA;
                    thickness = 2.4F;
                } else if (parentLvl > 0) {
                    lineColor = 0x883D85C6;
                    thickness = 1.8F;
                } else {
                    lineColor = 0x5526466D;
                    thickness = 1.4F;
                }

                drawSmoothLine(stack, parent.getX(), parent.getY(), node.getX(), node.getY(), thickness, lineColor);
            }
        }
    }

    private void renderSkillNode(MatrixStack stack, SkillNode node, ICosmicSkillData data, boolean isHovered) {
        int x = node.getX();
        int y = node.getY();
        int r = node.isKeystone() ? KEYSTONE_RADIUS : NODE_RADIUS;

        int currentLvl = data.getNodeLevel(node.getId());
        int maxLvl = node.getMaxLevel();
        boolean prereqsMet = data.arePrerequisitesMet(node);
        boolean canUpgrade = data.canUpgradeNode(node);
        boolean isMaxed = currentLvl >= maxLvl;

        int borderColor;
        int innerColor;
        int glowColor;

        if (isMaxed) {
            borderColor = 0xFFFFD700;
            innerColor = 0xFF2B2305;
            glowColor = 0x55FFD700;
        } else if (currentLvl > 0) {
            borderColor = node.getCategory().getPrimaryColor();
            innerColor = 0xFF0A2540;
            glowColor = (node.getCategory().getPrimaryColor() & 0x00FFFFFF) | 0x44000000;
        } else if (canUpgrade) {
            borderColor = 0xFF00E5FF;
            innerColor = 0xFF082038;
            glowColor = 0x4400E5FF;
        } else if (prereqsMet) {
            borderColor = 0xFF5499C7;
            innerColor = 0xFF071628;
            glowColor = 0x225499C7;
        } else {
            borderColor = 0xFF2C3E50;
            innerColor = 0xFF050B14;
            glowColor = 0x00000000;
        }

        if (isHovered) {
            fill(stack, x - r - 5, y - r - 5, x + r + 5, y + r + 5, 0x6600E5FF);
        } else if (glowColor != 0) {
            fill(stack, x - r - 3, y - r - 3, x + r + 3, y + r + 3, glowColor);
        }

        // Внешняя рамка узла
        fill(stack, x - r, y - r, x + r, y + r, borderColor);
        // Внутренний фон узла
        fill(stack, x - r + 2, y - r + 2, x + r - 2, y + r - 2, innerColor);

        // Декоративные уголки для Кейстоунов (крутых навыков)
        if (node.isKeystone()) {
            fill(stack, x - r - 2, y - 3, x - r + 2, y + 3, 0xFFFFD700);
            fill(stack, x + r - 2, y - 3, x + r + 2, y + 3, 0xFFFFD700);
            fill(stack, x - 3, y - r - 2, x + 3, y - r + 2, 0xFFFFD700);
            fill(stack, x - 3, y + r - 2, x + 3, y + r + 2, 0xFFFFD700);
        }

        // Отрисовка иконки предмета в центре узла
        ItemStack icon = getIconStack(node);
        this.itemRenderer.renderGuiItem(icon, x - 8, y - 8);

        // Индикатор текущего уровня под узлом
        String lvlText = currentLvl + "/" + maxLvl;
        int textWidth = this.font.width(lvlText);
        fill(stack, x - textWidth / 2 - 2, y + r - 2, x + textWidth / 2 + 2, y + r + 9, 0xCC040D1A);
        int lvlColor = isMaxed ? 0xFFFFD700 : (currentLvl > 0 ? 0xFF80D8FF : (prereqsMet ? 0xFFCFD8DC : 0xFF607D8B));
        this.font.draw(stack, lvlText, x - textWidth / 2.0F, y + r - 1.0F, lvlColor);
    }

    private void renderCosmicHudOverlay(MatrixStack matrixStack, ICosmicSkillData data) {
        // Верхняя полупрозрачная космическая панель
        fill(matrixStack, 0, 0, this.width, 34, 0xCC040D21);
        fill(matrixStack, 0, 34, this.width, 35, 0xFF00B0FF);

        // Заголовок по центру
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 7, 0xFF80D8FF);

        // Доступные очки навыков слева
        String pointsStr = "✦ Доступные очки навыков: " + data.getAvailablePoints();
        this.font.drawShadow(matrixStack, pointsStr, 14, 11, 0xFF00E676);

        // Информация о рубежах Better Leveling справа
        String milestoneStr = "Рубежей Better Leveling (каждые 5 ур.): " + data.getLastRewardedMilestones();
        int mw = this.font.width(milestoneStr);
        this.font.drawShadow(matrixStack, milestoneStr, this.width - mw - 14, 11, 0xFFFFD54F);

        // Нижняя строка с подсказкой управления
        fill(matrixStack, 0, this.height - 20, this.width, this.height, 0xCC040D21);
        fill(matrixStack, 0, this.height - 21, this.width, this.height - 20, 0x8800B0FF);
        ITextComponent hint = new TranslationTextComponent("gui.blcosmicaddon.hint");
        drawCenteredString(matrixStack, this.font, hint, this.width / 2, this.height - 14, 0xFFB0BEC5);
    }

    private void renderSkillNodeTooltip(MatrixStack matrixStack, SkillNode node, ICosmicSkillData data, int mouseX, int mouseY) {
        List<ITextComponent> lines = new ArrayList<>();
        int currentLvl = data.getNodeLevel(node.getId());
        int maxLvl = node.getMaxLevel();
        boolean prereqsMet = data.arePrerequisitesMet(node);
        boolean canUpgrade = data.canUpgradeNode(node);

        // Название и категория
        String titlePrefix = node.isKeystone() ? "§6§l" : "§b§l";
        lines.add(new StringTextComponent(titlePrefix + node.getName()));
        lines.add(new StringTextComponent("§9Ветвь: §f" + node.getCategory().getDisplayName()));
        lines.add(new StringTextComponent("§7" + node.getDescription()));
        lines.add(new StringTextComponent(""));

        // Уровень и бонус
        lines.add(new StringTextComponent("§fУровень прокачки: §e" + currentLvl + " §7/ §e" + maxLvl));
        double currentBonus = node.getTotalBonus(currentLvl);
        double nextBonus = node.getTotalBonus(Math.min(maxLvl, currentLvl + 1));

        lines.add(new StringTextComponent("§aТекущий бонус: §f" + formatBonusText(node.getBonusFormat(), currentBonus)));
        if (currentLvl < maxLvl) {
            lines.add(new StringTextComponent("§bСледующий уровень: §f" + formatBonusText(node.getBonusFormat(), nextBonus)
                    + " §7(+" + formatSingleValue(node.getValuePerLevel()) + " за ур.)"));
            int cost = node.getCostForNextLevel(currentLvl);
            String costColor = data.getAvailablePoints() >= cost ? "§a" : "§c";
            lines.add(new StringTextComponent("§fСтоимость улучшения: " + costColor + cost + " очк."));
        } else {
            lines.add(new StringTextComponent("§6★ Достигнут максимальный уровень навыка! ★"));
        }

        // Список требуемых предыдущих навыков (пререквизитов)
        if (!node.getPrerequisites().isEmpty()) {
            lines.add(new StringTextComponent(""));
            lines.add(new StringTextComponent("§eТребования для разблокировки:"));
            for (Map.Entry<String, Integer> entry : node.getPrerequisites().entrySet()) {
                SkillNode parent = SkillTreeRegistry.getNode(entry.getKey());
                String parentName = parent != null ? parent.getName() : entry.getKey();
                int haveLvl = data.getNodeLevel(entry.getKey());
                int reqLvl = entry.getValue();
                boolean ok = haveLvl >= reqLvl;
                String statusPrefix = ok ? " §a✔ " : " §c✖ ";
                lines.add(new StringTextComponent(statusPrefix + parentName + " §7(Ур. " + haveLvl + "/" + reqLvl + ")"));
            }
        }

        lines.add(new StringTextComponent(""));
        if (currentLvl >= maxLvl) {
            lines.add(new StringTextComponent("§6Навык полностью изучен"));
        } else if (canUpgrade) {
            lines.add(new StringTextComponent("§a▶ Нажмите ЛКМ, чтобы прокачать навык!"));
        } else if (!prereqsMet) {
            lines.add(new StringTextComponent("§c🔒 Закрыто: сначала прокачайте предыдущие навыки ветки!"));
        } else {
            lines.add(new StringTextComponent("§cНедостаточно очков! Повысьте любой навык в Better Leveling на 5 уровней."));
        }

        this.renderComponentTooltip(matrixStack, lines, mouseX, mouseY);
    }

    private String formatBonusText(String format, double val) {
        try {
            if (format.indexOf('%') != format.lastIndexOf('%')) {
                return String.format(format, val, val);
            }
            return String.format(format, val);
        } catch (Exception e) {
            return "+" + val;
        }
    }

    private String formatSingleValue(double val) {
        if (Math.abs(val - Math.round(val)) < 0.001) {
            return String.valueOf((int) Math.round(val));
        }
        return String.format("%.2f", val);
    }

    private void drawSmoothLine(MatrixStack matrixStack, float x1, float y1, float x2, float y2, float width, int argbColor) {
        float a = ((argbColor >> 24) & 0xFF) / 255.0F;
        float r = ((argbColor >> 16) & 0xFF) / 255.0F;
        float g = ((argbColor >> 8) & 0xFF) / 255.0F;
        float b = (argbColor & 0xFF) / 255.0F;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = MathHelper.sqrt(dx * dx + dy * dy);
        if (len <= 0.001F) return;

        float nx = -dy / len * (width * 0.5F);
        float ny = dx / len * (width * 0.5F);

        Matrix4f pose = matrixStack.last().pose();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.vertex(pose, x1 + nx, y1 + ny, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(pose, x1 - nx, y1 - ny, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(pose, x2 - nx, y2 - ny, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(pose, x2 + nx, y2 + ny, 0.0F).color(r, g, b, a).endVertex();
        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            double worldMouseX = (mouseX - centerX - panX) / zoom;
            double worldMouseY = (mouseY - centerY - panY) / zoom;

            for (SkillNode node : SkillTreeRegistry.getAllNodes()) {
                int radius = node.isKeystone() ? KEYSTONE_RADIUS : NODE_RADIUS;
                double dx = worldMouseX - node.getX();
                double dy = worldMouseY - node.getY();
                if ((dx * dx + dy * dy) <= (radius + 3) * (radius + 3)) {
                    ICosmicSkillData data = ClientSkillCache.get();
                    if (data.canUpgradeNode(node)) {
                        PacketHandler.sendToServer(new UpgradeSkillPacket(node.getId()));
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    } else {
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSound.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.7F));
                    }
                    return true;
                }
            }
            isDragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            panX = MathHelper.clamp(panX + dragX, -1100.0, 1100.0);
            panY = MathHelper.clamp(panY + dragY, -1100.0, 1100.0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float prevZoom = this.zoom;
        if (delta > 0) {
            this.zoom = MathHelper.clamp(this.zoom + 0.1F, 0.45F, 2.2F);
        } else if (delta < 0) {
            this.zoom = MathHelper.clamp(this.zoom - 0.1F, 0.45F, 2.2F);
        }
        if (this.zoom != prevZoom) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Клавиша R центрирует камеру на начало дерева
        if (keyCode == GLFW.GLFW_KEY_R) {
            this.panX = 0.0;
            this.panY = 0.0;
            this.zoom = 1.0F;
            return true;
        }
        // Повторное нажатие F7 закрывает экран
        if (ClientModEvents.OPEN_SKILL_TREE_KEY != null && ClientModEvents.OPEN_SKILL_TREE_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
