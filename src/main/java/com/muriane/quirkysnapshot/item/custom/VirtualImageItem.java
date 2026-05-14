package com.muriane.quirkysnapshot.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class VirtualImageItem extends Item {
    public VirtualImageItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
        if (data != null){
            String textureId = data.copyTag().getStringOr("texture_id", "");
            if (!textureId.isEmpty()){
                return Component.literal(textureId);
            }
        }
        return super.getName(itemStack);
    }
}
