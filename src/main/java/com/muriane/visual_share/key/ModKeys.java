package com.muriane.visual_share.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.muriane.visual_share.VisualShare;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class ModKeys {
    public static KeyMapping.Category IFIM_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "screenshot"));

    public static KeyMapping screenshot = new KeyMapping(
            "key.visual_share.screenshot.screenshot",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_APOSTROPHE,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping SELECTION = new KeyMapping(
            "key.visual_share.screenshot.selection",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping BRUSH = new KeyMapping(
            "key.visual_share.screenshot.brush",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping CUT = new KeyMapping(
            "key.visual_share.screenshot.cut",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping COLOR_PALETTE = new KeyMapping(
            "key.visual_share.screenshot.color_palette",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping BRUSH_SIZE = new KeyMapping(
            "key.visual_share.screenshot.brush_size",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping UNDO_REDO = new KeyMapping(
            "key.visual_share.screenshot.undo_redo",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping SAVE = new KeyMapping(
            "key.visual_share.screenshot.save",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_S,
            IFIM_KEY_CATEGORY
    );
    public static KeyMapping SHARE = new KeyMapping(
            "key.visual_share.screenshot.share",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_T,
            IFIM_KEY_CATEGORY
    );

    @EventBusSubscriber
    public static class KeyHolder{
        @SubscribeEvent
        public static void registerKey(RegisterKeyMappingsEvent event){
            event.register(screenshot);
            event.register(SELECTION);
            event.register(BRUSH);
            event.register(COLOR_PALETTE);
            event.register(BRUSH_SIZE);
            event.register(CUT);
            event.register(UNDO_REDO);
            event.register(SAVE);
            event.register(SHARE);
        }
    }
}
