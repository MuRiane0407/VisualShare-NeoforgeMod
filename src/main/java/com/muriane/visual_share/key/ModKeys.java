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
    public static KeyMapping.Category MISC_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "misc"));
    public static KeyMapping MISC_ITEM_SHOW = new KeyMapping(
            "key.visual_share.misc.item_show",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            MISC_KEY_CATEGORY
    );

    public static KeyMapping.Category SCREENSHOT_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "screenshot"));
    public static KeyMapping SCREENSHOT_SCREENSHOT = new KeyMapping(
            "key.visual_share.screenshot.screenshot",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_APOSTROPHE,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_SELECTION = new KeyMapping(
            "key.visual_share.screenshot.selection",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_BRUSH = new KeyMapping(
            "key.visual_share.screenshot.brush",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_CUT = new KeyMapping(
            "key.visual_share.screenshot.cut",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_COLOR_PALETTE = new KeyMapping(
            "key.visual_share.screenshot.color_palette",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_BRUSH_SIZE = new KeyMapping(
            "key.visual_share.screenshot.brush_size",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_UNDO_REDO = new KeyMapping(
            "key.visual_share.screenshot.undo_redo",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_SAVE = new KeyMapping(
            "key.visual_share.screenshot.save",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_S,
            SCREENSHOT_KEY_CATEGORY
    );
    public static KeyMapping SCREENSHOT_SHARE = new KeyMapping(
            "key.visual_share.screenshot.share",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_T,
            SCREENSHOT_KEY_CATEGORY
    );

    public static KeyMapping.Category DRAWING_BOARD_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "drawing_board"));
    public static KeyMapping DRAWING_BOARD_DRAWING_BOARD = new KeyMapping(
            "key.visual_share.drawing_board.drawing_board",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_SEMICOLON,
            DRAWING_BOARD_KEY_CATEGORY
    );

    public static KeyMapping.Category STRUCTURE_VIEW_KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "structure_view"));
    public static KeyMapping STRUCTURE_VIEW_SELECTION = new KeyMapping(
            "key.visual_share.structure_view.selection",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LBRACKET,
            STRUCTURE_VIEW_KEY_CATEGORY
    );
    public static KeyMapping STRUCTURE_VIEW_PREVIEW = new KeyMapping(
            "key.visual_share.structure_view.preview",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_RBRACKET,
            STRUCTURE_VIEW_KEY_CATEGORY
    );

    @EventBusSubscriber
    public static class KeyHolder{
        @SubscribeEvent
        public static void registerKey(RegisterKeyMappingsEvent event){
            event.register(MISC_ITEM_SHOW);

            event.register(SCREENSHOT_SCREENSHOT);
            event.register(SCREENSHOT_SELECTION);
            event.register(SCREENSHOT_BRUSH);
            event.register(SCREENSHOT_COLOR_PALETTE);
            event.register(SCREENSHOT_BRUSH_SIZE);
            event.register(SCREENSHOT_CUT);
            event.register(SCREENSHOT_UNDO_REDO);
            event.register(SCREENSHOT_SAVE);
            event.register(SCREENSHOT_SHARE);

            event.register(DRAWING_BOARD_DRAWING_BOARD);

            event.register(STRUCTURE_VIEW_SELECTION);
            event.register(STRUCTURE_VIEW_PREVIEW);
        }
    }
}
