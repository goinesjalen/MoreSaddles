package com.tatostv.moresaddles.events;

import com.tatostv.moresaddles.MoreSaddlesMod;
import com.tatostv.moresaddles.items.BaseSaddleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = MoreSaddlesMod.MODID)
public class SaddleHooks {
    
    // Cache of all known BaseSaddleItem modifier IDs for efficient cleanup
    private static final Set<ResourceLocation> ALL_SADDLE_MODIFIER_IDS = new HashSet<>();
    
    // Initialize modifier ID cache on first access
    private static void initializeModifierIds() {
        if (!ALL_SADDLE_MODIFIER_IDS.isEmpty()) return; // Already initialized
        
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BaseSaddleItem saddleItem) {
                ResourceLocation registryKey = BuiltInRegistries.ITEM.getKey(item);
                String saddleName = registryKey != null ? registryKey.getPath() : item.getClass().getSimpleName().toLowerCase();
                
                // Add all four modifier types for this saddle
                ALL_SADDLE_MODIFIER_IDS.add(ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_speed"));
                ALL_SADDLE_MODIFIER_IDS.add(ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_armor"));
                ALL_SADDLE_MODIFIER_IDS.add(ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_health"));
                ALL_SADDLE_MODIFIER_IDS.add(ResourceLocation.fromNamespaceAndPath("moresaddles", saddleName + "_jump"));
            }
        }
    }
    
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // Server-side only, early return for client
        if (event.getEntity().level().isClientSide) return;
        
        // Only process AbstractHorse entities
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        
        // Only handle chest slot changes (where saddles are equipped for horses)
        // Note: Horses use CHEST slot for saddle equipment changes
        if (event.getSlot() != EquipmentSlot.CHEST) return;
        
        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();
        
        // Remove modifiers from old saddle if it was a BaseSaddleItem
        if (!oldStack.isEmpty() && oldStack.getItem() instanceof BaseSaddleItem oldSaddle) {
            oldSaddle.removeBuffs(horse);
        }
        
        // Apply modifiers from new saddle if it's a BaseSaddleItem
        if (!newStack.isEmpty() && newStack.getItem() instanceof BaseSaddleItem newSaddle) {
            newSaddle.applyBuffs(horse);
        }
        
        // If neither old nor new are BaseSaddleItems, ensure all our modifiers are cleaned up
        // This handles edge cases like switching from our saddle to vanilla saddle
        if ((oldStack.isEmpty() || !(oldStack.getItem() instanceof BaseSaddleItem)) &&
            (newStack.isEmpty() || !(newStack.getItem() instanceof BaseSaddleItem))) {
            removeAllSaddleModifiers(horse);
        }
    }
    
    /**
     * Removes all saddle modifiers from all registered BaseSaddleItem instances.
     * Uses registry-driven approach to avoid hardcoded modifier lists.
     * This is a fallback cleanup method for edge cases.
     */
    private static void removeAllSaddleModifiers(AbstractHorse horse) {
        initializeModifierIds(); // Lazy initialization
        
        for (ResourceLocation modifierId : ALL_SADDLE_MODIFIER_IDS) {
            // Remove speed modifiers
            if (modifierId.getPath().endsWith("_speed")) {
                var speedAttr = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.hasModifier(modifierId)) {
                    speedAttr.removeModifier(modifierId);
                }
            }
            // Remove armor modifiers  
            else if (modifierId.getPath().endsWith("_armor")) {
                var armorAttr = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                if (armorAttr != null && armorAttr.hasModifier(modifierId)) {
                    armorAttr.removeModifier(modifierId);
                }
            }
            // Remove health modifiers and clamp current health
            else if (modifierId.getPath().endsWith("_health")) {
                var healthAttr = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (healthAttr != null && healthAttr.hasModifier(modifierId)) {
                    healthAttr.removeModifier(modifierId);
                    // Clamp current health to new max after removing health modifier
                    double newMaxHealth = healthAttr.getValue();
                    if (horse.getHealth() > newMaxHealth) {
                        horse.setHealth((float) newMaxHealth);
                    }
                }
            }
            // Remove jump modifiers - note: JUMP_STRENGTH mapping may vary
            else if (modifierId.getPath().endsWith("_jump")) {
                var jumpAttr = horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
                if (jumpAttr != null && jumpAttr.hasModifier(modifierId)) {
                    jumpAttr.removeModifier(modifierId);
                }
            }
        }
    }
}