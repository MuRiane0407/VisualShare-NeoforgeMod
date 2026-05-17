package com.muriane.visual_share.item;

import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.item.custom.VirtualImageItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VisualShare.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VisualShare.MOD_ID);

    public static final DeferredItem<VirtualImageItem> VIRTUAL_IMAGE_ITEM =
            ITEMS.registerItem("virtual_image",
                    VirtualImageItem::new,
                    properties -> properties
                            .stacksTo(1));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
