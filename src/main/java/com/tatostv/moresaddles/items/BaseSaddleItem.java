package com.tatostv.moresaddles.items;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

public abstract class BaseSaddleItem extends SaddleItem {
    protected final ResourceLocation speedModifierId;
    protected final ResourceLocation armorModifierId;
    protected final ResourceLocation healthModifierId;
    protected final ResourceLocation jumpModifierId;

    public BaseSaddleItem(Item.Properties properties) {
        super(properties);

        // Use registry name for stable IDs (safe fallback to class name if not
        // registered yet)
        String saddleName;
        try {
            ResourceLocation registryKey = BuiltInRegistries.ITEM.getKey(this);
            saddleName = registryKey != null ? registryKey.getPath() : this.getClass().getSimpleName().toLowerCase();
        } catch (Exception e) {
            // Fallback during construction before registration
            saddleName = this.getClass().getSimpleName().toLowerCase();
        }

        this.speedModifierId = ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_speed");
        this.armorModifierId = ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_armor");
        this.healthModifierId = ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_health");
        this.jumpModifierId = ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_jump");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof AbstractHorse horse))
            return;

        // Read the actual saddle stack from the horse’s saddle slot (don’t hardcode
        // index 0).
        ItemStack saddleStack = horse.getInventory().getItem(AbstractHorse.INV_SLOT_SADDLE);
        if (!saddleStack.isEmpty() && saddleStack.is(this)) {
            applyBuffs(horse);
        } else {
            removeBuffs(horse);
        }
    }

    public abstract void applyBuffs(AbstractHorse horse);

    public void removeBuffs(AbstractHorse horse) {
        removeAttributeModifier(horse, Attributes.MOVEMENT_SPEED, speedModifierId);
        removeAttributeModifier(horse, Attributes.ARMOR, armorModifierId);
        removeAttributeModifier(horse, Attributes.MAX_HEALTH, healthModifierId);
        removeAttributeModifier(horse, Attributes.JUMP_STRENGTH, jumpModifierId);
    }

    protected void applySpeedModifier(AbstractHorse horse, double amount) {
        applySpeedModifier(horse, speedModifierId, amount);
    }

    protected void applySpeedModifier(AbstractHorse horse, ResourceLocation id, double amount) {
        AttributeInstance a = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null && !a.hasModifier(id)) {
            a.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    protected void applyArmorModifier(AbstractHorse horse, double amount) {
        applyArmorModifier(horse, armorModifierId, amount);
    }

    protected void applyArmorModifier(AbstractHorse horse, ResourceLocation id, double amount) {
        AttributeInstance a = horse.getAttribute(Attributes.ARMOR);
        if (a != null && !a.hasModifier(id)) {
            a.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    protected void applyHealthModifier(AbstractHorse horse, double amount) {
        applyHealthModifier(horse, healthModifierId, amount);
    }

    protected void applyHealthModifier(AbstractHorse horse, ResourceLocation id, double amount) {
        AttributeInstance a = horse.getAttribute(Attributes.MAX_HEALTH);
        if (a != null && !a.hasModifier(id)) {
            a.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            double newMax = a.getValue();
            if (horse.getHealth() < newMax)
                horse.setHealth((float) newMax);
        }
    }

    protected void applyJumpModifier(AbstractHorse horse, double amount) {
        applyJumpModifier(horse, jumpModifierId, amount);
    }

    protected void applyJumpModifier(AbstractHorse horse, ResourceLocation id, double amount) {
        AttributeInstance a = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (a != null && !a.hasModifier(id)) {
            a.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeAttributeModifier(AbstractHorse horse, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance a = horse.getAttribute(attr);
        if (a != null && a.hasModifier(id))
            a.removeModifier(id);
    }

    protected abstract void addCustomTooltips(List<Component> tooltipComponents);

    protected abstract String getTooltipKey();

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, out, flag);
        out.add(Component.translatable(getTooltipKey()));
        addCustomTooltips(out);
    }
}
