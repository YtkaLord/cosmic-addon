package com.cosmic.betterlevelingaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

/**
 * Клиентский обработчик клавиш (LWJGL 3.2.2 / GLFW).
 * Регистрирует горячую клавишу F7 (GLFW.GLFW_KEY_F7) для открытия Космического Дерева Навыков.
 */
public class ClientModEvents {
    public static KeyBinding OPEN_SKILL_TREE_KEY;

    public static void initClient() {
        OPEN_SKILL_TREE_KEY = new KeyBinding(
                "key.blcosmicaddon.open_tree",
                KeyConflictContext.IN_GAME,
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.categories.blcosmicaddon"
        );
        ClientRegistry.registerKeyBinding(OPEN_SKILL_TREE_KEY);
        MinecraftForge.EVENT_BUS.register(new ClientModEvents());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (OPEN_SKILL_TREE_KEY != null && OPEN_SKILL_TREE_KEY.consumeClick()) {
            mc.setScreen(new CosmicSkillTreeScreen());
        }
    }
}
